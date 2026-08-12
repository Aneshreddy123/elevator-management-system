package com.example.elevator.repository;

import com.example.elevator.entity.ElevatorRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElevatorRequestRepository extends JpaRepository<ElevatorRequest, Long> {
    List<ElevatorRequest> findByElevatorId(Long elevatorId);
}
