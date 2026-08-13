package com.example.elevator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * {@code @EnableAsync} powers the fire-and-forget movement simulation in
 * {@link com.example.elevator.service.ElevatorService#simulateMovement}.
 * {@code @EnableScheduling} powers the watchdog health-check job that
 * auto-recovers faulted elevators.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ElevatorManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElevatorManagementApplication.class, args);
    }
}
