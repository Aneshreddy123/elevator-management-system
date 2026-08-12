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

    @Cacheable(value = "elevatorStatus", key = "'all'")
    public List<ElevatorStatusDTO> getAllStatuses() {
        log.debug("Cache miss - loading elevator statuses from DB");
        return elevatorRepository.findAll().stream().map(this::toStatusDTO).toList();
    }

    // ---------- Manual admin assignment ----------

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

    @SuppressWarnings("unused")
    private CompletableFuture<Void> simulateMovementFallback(Long elevatorId, Throwable t) {
        log.warn("Circuit breaker fallback triggered for elevator {}: {}", elevatorId, t.getMessage());
        markFault(elevatorId);
        return CompletableFuture.completedFuture(null);
    }

    // ---------- Fault detection & auto recovery ----------

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

    public List<ElevatorStatusDTO> optimizeRoutes() {
        // Batch optimization: group idle elevators toward floors with the most
        // pending requests (highest-traffic floors first) for pre-positioning.
        List<Elevator> elevators = elevatorRepository.findAll();
        List<ElevatorRequest> pending = requestRepository.findAll();

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
