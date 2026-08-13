package com.example.elevator.repository;

import com.example.elevator.entity.Elevator;
import com.example.elevator.entity.ElevatorState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** CRUD access to elevators, plus a state-filtered lookup used by the fault watchdog. */
public interface ElevatorRepository extends JpaRepository<Elevator, Long> {

    /** Used by {@link com.example.elevator.service.ElevatorService#watchdogHealthCheck} to find FAULT elevators to auto-recover. */
    List<Elevator> findByState(ElevatorState state);
}
