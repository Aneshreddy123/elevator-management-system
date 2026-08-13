package com.example.elevator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A persisted audit-trail entry for elevator movement events.
 * Written asynchronously by {@link com.example.elevator.service.KafkaMovementConsumer}
 * after it consumes a {@link com.example.elevator.dto.MovementEvent} from Kafka,
 * and read back out (paginated) via GET /api/elevators/logs.
 */
@Entity
@Table(name = "elevator_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevatorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long elevatorId;

    @Column(length = 1000)
    private String event;

    private LocalDateTime timestamp;
}
