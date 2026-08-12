package com.example.elevator.dto;

import com.example.elevator.entity.Direction;
import com.example.elevator.entity.ElevatorState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
