package com.example.elevator.repository;

import com.example.elevator.entity.ElevatorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** CRUD + pagination access to the movement-event audit log. */
public interface ElevatorLogRepository extends JpaRepository<ElevatorLog, Long> {

    /** Backs GET /api/elevators/logs?page=&size= - newest events first. */
    Page<ElevatorLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
