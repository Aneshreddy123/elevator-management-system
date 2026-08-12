package com.example.elevator.service;

import com.example.elevator.dto.MovementEvent;
import com.example.elevator.entity.ElevatorLog;
import com.example.elevator.repository.ElevatorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Consumes movement events asynchronously (decoupling the simulate endpoint
 * from persistence) and writes them to the elevator_logs table for the
 * paginated /api/elevators/logs endpoint.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaMovementConsumer {

    private final ElevatorLogRepository logRepository;

    @KafkaListener(topics = "${app.kafka.topic.movement}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMovementEvent(MovementEvent event) {
        try {
            Long elevatorId = event.getElevatorId();
            String logMessage = String.format("Elevator %s moved from %s to %s [%s]",
                    elevatorId, event.getFromFloor(), event.getToFloor(), event.getState());

            ElevatorLog logEntry = ElevatorLog.builder()
                    .elevatorId(elevatorId)
                    .event(logMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            logRepository.save(logEntry);
            log.info(logMessage);
        } catch (Exception e) {
            log.error("Failed to process movement event: {}", event, e);
        }
    }
}
