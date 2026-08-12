package com.example.elevator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
