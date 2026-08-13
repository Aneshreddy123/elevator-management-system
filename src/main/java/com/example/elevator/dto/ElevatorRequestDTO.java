package com.example.elevator.dto;

import com.example.elevator.entity.Direction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for POST /api/elevators/request (a passenger call). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElevatorRequestDTO {

    @NotNull(message = "requestFloor is required")
    private Integer requestFloor;

    @NotNull(message = "destinationFloor is required")
    private Integer destinationFloor;

    @NotNull(message = "direction is required")
    private Direction direction;
}
