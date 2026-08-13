package com.example.elevator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * The message payload published to the {@code elevator-movement-events}
 * Kafka topic by {@link com.example.elevator.service.KafkaMovementProducer}
 * and consumed by {@link com.example.elevator.service.KafkaMovementConsumer}
 * to write an {@link com.example.elevator.entity.ElevatorLog} entry.
 * A dedicated typed class (rather than a raw Map) is required here so
 * Spring Kafka's JSON deserializer can trust and deserialize it - see
 * spring.json.trusted.packages in application.yml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovementEvent implements Serializable {
    private Long elevatorId;
    private int fromFloor;
    private int toFloor;
    private String state;
    private long timestamp;
}
