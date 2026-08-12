package com.example.elevator.repository;

import com.example.elevator.entity.ElevatorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElevatorLogRepository extends JpaRepository<ElevatorLog, Long> {
    Page<ElevatorLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
