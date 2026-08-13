package com.example.elevator.exception;

/** Thrown when an operation references an elevator id that doesn't exist. Mapped to HTTP 404. */
public class ElevatorNotFoundException extends RuntimeException {
    public ElevatorNotFoundException(Long id) {
        super("Elevator not found with id: " + id);
    }
}
