package com.example.elevator.repository;

import com.example.elevator.entity.Elevator;
import com.example.elevator.entity.ElevatorState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElevatorRepository extends JpaRepository<Elevator, Long> {
    List<Elevator> findByState(ElevatorState state);
}
