package com.example.elevator.exception;

public class ElevatorNotFoundException extends RuntimeException {
    public ElevatorNotFoundException(Long id) {
        super("Elevator not found with id: " + id);
    }
}
