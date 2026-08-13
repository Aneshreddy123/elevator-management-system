package com.example.elevator.config;

import com.example.elevator.entity.Direction;
import com.example.elevator.entity.Elevator;
import com.example.elevator.entity.ElevatorState;
import com.example.elevator.repository.ElevatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Dev-only convenience: pre-populates 4 idle elevators on startup so you
 * can hit the API immediately without manually inserting rows. Only active
 * under the "dev" profile (H2) - the "prod" profile (docker-compose,
 * Postgres) intentionally starts with an empty fleet.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ElevatorRepository elevatorRepository;

    @Override
    public void run(String... args) {
        if (elevatorRepository.count() > 0) {
            return;
        }
        for (int i = 1; i <= 4; i++) {
            Elevator elevator = Elevator.builder()
                    .currentFloor(1)
                    .targetFloor(1)
                    .state(ElevatorState.IDLE)
                    .direction(Direction.NONE)
                    .capacity(8)
                    .lastMaintenance(LocalDateTime.now())
                    .healthy(true)
                    .build();
            elevatorRepository.save(elevator);
        }
    }
}
