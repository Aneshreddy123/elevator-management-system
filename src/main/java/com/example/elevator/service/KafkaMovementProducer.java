package com.example.elevator.service;

import com.example.elevator.dto.MovementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes a {@link com.example.elevator.dto.MovementEvent} to Kafka every
 * time {@link ElevatorService#simulateMovement} advances an elevator. This
 * decouples "an elevator moved" from "write that fact to the audit log" -
 * the log write happens asynchronously on the consumer side
 * ({@link KafkaMovementConsumer}), keeping the simulate endpoint fast.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaMovementProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.movement}")
    private String movementTopic;

    public void publishMovementEvent(Long elevatorId, int fromFloor, int toFloor, String state) {
        MovementEvent event = new MovementEvent(elevatorId, fromFloor, toFloor, state, System.currentTimeMillis());
        kafkaTemplate.send(movementTopic, elevatorId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish movement event for elevator {}", elevatorId, ex);
                    } else {
                        log.debug("Published movement event for elevator {}", elevatorId);
                    }
                });
    }
}
