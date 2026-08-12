package com.example.elevator.exception;

public class ElevatorFaultException extends RuntimeException {
    public ElevatorFaultException(String message) {
        super(message);
    }
}
