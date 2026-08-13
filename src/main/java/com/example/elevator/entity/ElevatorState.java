package com.example.elevator.entity;

/**
 * Lifecycle state of an elevator.
 * MAINTENANCE is a planned/manual outage; FAULT is an unplanned failure
 * detected at runtime (e.g. by the circuit breaker) that the watchdog
 * job will attempt to auto-recover from.
 */
public enum ElevatorState {
    MOVING, IDLE, MAINTENANCE, FAULT
}
