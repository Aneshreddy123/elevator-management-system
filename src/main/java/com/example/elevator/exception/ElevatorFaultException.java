package com.example.elevator.exception;

/**
 * Thrown when an elevator can't service a request because it's faulted,
 * under maintenance, or (for the whole-fleet case) none are available at all.
 * Mapped to HTTP 503 and also used to trip the Resilience4j circuit breaker
 * on the simulate-movement path.
 */
public class ElevatorFaultException extends RuntimeException {
    public ElevatorFaultException(String message) {
        super(message);
    }
}
