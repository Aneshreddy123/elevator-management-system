package com.example.elevator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard JSON error body returned by {@link com.example.elevator.exception.GlobalExceptionHandler}
 * for every handled exception, so clients always get a consistent shape
 * instead of a raw stack trace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private int status;
    private String error;
    private String message;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
