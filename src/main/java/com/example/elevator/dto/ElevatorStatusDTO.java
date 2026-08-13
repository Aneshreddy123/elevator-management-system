package com.example.elevator.dto;

import com.example.elevator.entity.Direction;
import com.example.elevator.entity.ElevatorState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Read-only projection of an {@link com.example.elevator.entity.Elevator}
 * returned by GET /api/elevators/status and GET /api/elevators/optimize.
 * Kept separate from the entity so the Redis-cached response never leaks
 * JPA internals (lazy collections, proxies) and stays cheap to (de)serialize.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElevatorStatusDTO implements Serializable {
    private Long id;
    private int currentFloor;
    private int targetFloor;
    private ElevatorState state;
    private Direction direction;
    private int capacity;
    private int activeLoad;
    private boolean healthy;
}
