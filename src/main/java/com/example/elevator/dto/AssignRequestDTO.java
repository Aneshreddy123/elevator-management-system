package com.example.elevator.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for PUT /api/elevators/{id}/assign (admin manual override). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRequestDTO {

    @NotNull(message = "targetFloor is required")
    private Integer targetFloor;
}
