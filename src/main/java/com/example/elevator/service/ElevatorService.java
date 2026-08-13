package com.example.elevator.service;

import com.example.elevator.dto.ElevatorRequestDTO;
import com.example.elevator.dto.ElevatorStatusDTO;
import com.example.elevator.entity.*;
import com.example.elevator.exception.ElevatorFaultException;
import com.example.elevator.exception.ElevatorNotFoundException;
import com.example.elevator.repository.ElevatorLogRepository;
import com.example.elevator.repository.ElevatorRepository;
import com.example.elevator.repository.ElevatorRequestRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Core business logic for the elevator system - the orchestrator that ties
 * together the scheduling algorithm, persistence, Redis caching, the
 * Kafka movement-event pipeline, and fault recovery. Each public method
 * here backs one endpoint in {@link com.example.elevator.controller.ElevatorController}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ElevatorService {

    private final ElevatorRepository elevatorRepository;
    private final ElevatorRequestRepository requestRepository;
    private final ElevatorLogRepository logRepository;
    private final ElevatorSchedulingService schedulingService;
    private final KafkaMovementProducer movementProducer;

    // ---------- Request an elevator (passenger call) ----------

    /**
     * Handles a passenger call: runs the MinHeap scheduler over every
     * elevator to pick the best fit, persists the request against it, and
     * (if that elevator was idle) kicks it into MOVING state toward the
     * caller's floor. Invalidates the status cache since fleet state changed.
     */
    @Transactional
    @CacheEvict(value = "elevatorStatus", allEntries = true)
    public ElevatorRequest requestElevator(ElevatorRequestDTO dto) {
        List<Elevator> candidates = elevatorRepository.findAll();
        if (candidates.isEmpty()) {
            throw new ElevatorFaultException("No elevators registered in the system.");
        }

        Elevator chosen = schedulingService.findBestElevator(candidates, dto.getRequestFloor(), dto.getDirection());
        if (chosen == null) {
            throw new ElevatorFaultException("No available (non-maintenance) elevator to service this request.");
        }

        ElevatorRequest request = ElevatorRequest.builder()
                .requestFloor(dto.getRequestFloor())
                .destinationFloor(dto.getDestinationFloor())
                .direction(dto.getDirection())
                .requestTime(LocalDateTime.now())
                .elevator(chosen)
                .build();

        requestRepository.save(request);

        if (chosen.getState() == ElevatorState.IDLE) {
            chosen.setState(ElevatorState.MOVING);
            chosen.setTargetFloor(dto.getRequestFloor());
            chosen.setDirection(dto.getDirection());
            elevatorRepository.save(chosen);
        }

        log.info("Assigned elevator {} to request at floor {} -> {}", chosen.getId(), dto.getRequestFloor(), dto.getDestinationFloor());
        return request;
    }

    // ---------- Status of all elevators (Redis cached) ----------

    /**
     * Returns the whole fleet's status. Cached in Redis under a single
     * "all" key with a short TTL (see application.yml) since this is a
     * hot-path read; any write elsewhere in this class evicts the cache
     * so status never returns stale data for longer than one write cycle.
     */
    @Cacheable(value = "elevatorStatus", key = "'all'")
    public List<ElevatorStatusDTO> getAllStatuses() {
        log.debug("Cache miss - loading elevator statuses from DB");
        return elevatorRepository.findAll().stream().map(this::toStatusDTO).toList();
    }

    // ---------- Manual admin assignment ----------

    /** Admin override: force a specific elevator to a target floor, bypassing the scheduler. */
    @Transactional
    @CacheEvict(value = "elevatorStatus", allEntries = true)
    public Elevator manualAssign(Long elevatorId, int targetFloor) {
        Elevator elevator = getOrThrow(elevatorId);
        if (elevator.getState() == ElevatorState.MAINTENANCE || elevator.getState() == ElevatorState.FAULT) {
            throw new ElevatorFaultException("Cannot assign elevator " + elevatorId + " - currently " + elevator.getState());
        }
        elevator.setTargetFloor(targetFloor);
        elevator.setDirection(targetFloor > elevator.getCurrentFloor() ? Direction.UP
                : targetFloor < elevator.getCurrentFloor() ? Direction.DOWN : Direction.NONE);
        elevator.setState(targetFloor == elevator.getCurrentFloor() ? ElevatorState.IDLE : ElevatorState.MOVING);
        return elevatorRepository.save(elevator);
    }

    // ---------- Simulate movement (async, circuit-breaker protected) ----------

    /**
     * Advances an elevator one floor toward its target, runs off the async
     * executor so the HTTP request returns immediately (see the 202
     * Accepted response in the controller), and publishes a Kafka event
     * for whatever moved. Wrapped in a circuit breaker: if this method
     * keeps failing (e.g. repeated faults), Resilience4j trips the breaker
     * and routes calls to {@link #simulateMovementFallback} instead of
     * hammering a broken elevator.
     */
    @Async
    @CircuitBreaker(name = "elevatorService", fallbackMethod = "simulateMovementFallback")
    @CacheEvict(value = "elevatorStatus", allEntries = true)
    public CompletableFuture<Void> simulateMovement(Long elevatorId) {
        Elevator elevator = getOrThrow(elevatorId);

        if (!elevator.isHealthy() || elevator.getState() == ElevatorState.FAULT) {
            throw new ElevatorFaultException("Elevator " + elevatorId + " is faulted and cannot move.");
        }

        int from = elevator.getCurrentFloor();
        int to = elevator.getTargetFloor();

        if (from == to) {
            elevator.setState(ElevatorState.IDLE);
            elevator.setDirection(Direction.NONE);
        } else {
            int step = to > from ? 1 : -1;
            elevator.setCurrentFloor(from + step);
            elevator.setDirection(step > 0 ? Direction.UP : Direction.DOWN);
            elevator.setState(elevator.getCurrentFloor() == to ? ElevatorState.IDLE : ElevatorState.MOVING);
        }

        elevatorRepository.save(elevator);
        movementProducer.publishMovementEvent(elevatorId, from, elevator.getCurrentFloor(), elevator.getState().name());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Resilience4j fallback for {@link #simulateMovement}: instead of letting
     * the exception propagate, mark the elevator FAULT so the watchdog job
     * can pick it up for auto-recovery later. Signature must mirror the
     * original method plus a trailing Throwable, per Resilience4j convention.
     */
    @SuppressWarnings("unused")
    private CompletableFuture<Void> simulateMovementFallback(Long elevatorId, Throwable t) {
        log.warn("Circuit breaker fallback triggered for elevator {}: {}", elevatorId, t.getMessage());
        markFault(elevatorId);
        return CompletableFuture.completedFuture(null);
    }

    // ---------- Fault detection & auto recovery ----------

    /** Marks an elevator as faulted/unhealthy - stops it being selected by the scheduler until repaired. */
    @Transactional
    @CacheEvict(value = "elevatorStatus", allEntries = true)
    public Elevator markFault(Long elevatorId) {
        Elevator elevator = getOrThrow(elevatorId);
        elevator.setHealthy(false);
        elevator.setState(ElevatorState.FAULT);
        elevator.setDirection(Direction.NONE);
        return elevatorRepository.save(elevator);
    }

    /**
     * Repairs (recovers) a faulted/maintenance elevator - acts as the watchdog
     * "restart" action, either triggered manually by an admin via PUT /repair
     * or automatically by the scheduled health check below.
     */
    @Transactional
    @CacheEvict(value = "elevatorStatus", allEntries = true)
    public Elevator repair(Long elevatorId) {
        Elevator elevator = getOrThrow(elevatorId);
        elevator.setHealthy(true);
        elevator.setState(ElevatorState.IDLE);
        elevator.setDirection(Direction.NONE);
        elevator.setLastMaintenance(LocalDateTime.now());
        Elevator saved = elevatorRepository.save(elevator);
        log.info("Elevator {} repaired and returned to service", elevatorId);
        return saved;
    }

    /**
     * Watchdog process: periodically scans for non-responding (FAULT) elevators
     * and attempts auto-recovery, simulating a restart.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${app.watchdog.interval-ms:30000}")
    public void watchdogHealthCheck() {
        List<Elevator> faulted = elevatorRepository.findByState(ElevatorState.FAULT);
        for (Elevator elevator : faulted) {
            log.info("Watchdog attempting auto-recovery for elevator {}", elevator.getId());
            try {
                repair(elevator.getId());
            } catch (Exception e) {
                log.error("Watchdog auto-recovery failed for elevator {}", elevator.getId(), e);
            }
        }
    }

    // ---------- Traffic-based route optimization ----------

    /**
     * Batch optimization pass (admin-triggered): counts pending requests
     * per floor to find the highest-traffic floors, then pre-positions
     * the currently-idle elevators toward the busiest ones (nearest idle
     * elevator to each hot floor), so the next call there gets served faster.
     */
    public List<ElevatorStatusDTO> optimizeRoutes() {
        List<Elevator> elevators = elevatorRepository.findAll();
        List<ElevatorRequest> pending = requestRepository.findAll();

        // Group pending requests by floor and rank floors by call volume, descending.
        pending.stream()
                .collect(java.util.stream.Collectors.groupingBy(ElevatorRequest::getRequestFloor, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(elevators.stream().filter(e -> e.getState() == ElevatorState.IDLE).count())
                .forEach(entry -> {
                    Elevator idle = elevators.stream()
                            .filter(e -> e.getState() == ElevatorState.IDLE)
                            .min((a, b) -> Integer.compare(
                                    Math.abs(a.getCurrentFloor() - entry.getKey()),
                                    Math.abs(b.getCurrentFloor() - entry.getKey())))
                            .orElse(null);
                    if (idle != null) {
                        idle.setTargetFloor(entry.getKey());
                        idle.setDirection(entry.getKey() > idle.getCurrentFloor() ? Direction.UP
                                : entry.getKey() < idle.getCurrentFloor() ? Direction.DOWN : Direction.NONE);
                        elevatorRepository.save(idle);
                    }
                });

        return elevatorRepository.findAll().stream().map(this::toStatusDTO).toList();
    }

    // ---------- Logs (paginated) ----------

    /** Backs GET /api/elevators/logs - returns the Kafka-consumer-written audit trail, newest first. */
    public Page<ElevatorLog> getLogs(Pageable pageable) {
        return logRepository.findAllByOrderByTimestampDesc(pageable);
    }

    // ---------- Helpers ----------

    private Elevator getOrThrow(Long id) {
        return elevatorRepository.findById(id).orElseThrow(() -> new ElevatorNotFoundException(id));
    }

    private ElevatorStatusDTO toStatusDTO(Elevator e) {
        return ElevatorStatusDTO.builder()
                .id(e.getId())
                .currentFloor(e.getCurrentFloor())
                .targetFloor(e.getTargetFloor())
                .state(e.getState())
                .direction(e.getDirection())
                .capacity(e.getCapacity())
                .activeLoad(e.activeLoad())
                .healthy(e.isHealthy())
                .build();
    }
}
