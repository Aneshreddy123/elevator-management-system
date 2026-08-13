package com.example.elevator.service;

import com.example.elevator.entity.Direction;
import com.example.elevator.entity.Elevator;
import com.example.elevator.entity.ElevatorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral tests for the MinHeap dispatch algorithm: proves nearest-wins,
 * direction affinity, maintenance/fault exclusion, and load balancing.
 */
class ElevatorSchedulingServiceTest {

    private ElevatorSchedulingService schedulingService;

    @BeforeEach
    void setUp() {
        schedulingService = new ElevatorSchedulingService();
    }

    @Test
    void picksNearestIdleElevator() {
        Elevator near = Elevator.builder().id(1L).currentFloor(3).state(ElevatorState.IDLE).direction(Direction.NONE).build();
        Elevator far = Elevator.builder().id(2L).currentFloor(10).state(ElevatorState.IDLE).direction(Direction.NONE).build();

        Elevator best = schedulingService.findBestElevator(List.of(near, far), 4, Direction.UP);

        assertThat(best.getId()).isEqualTo(1L);
    }

    @Test
    void prefersElevatorAlreadyMovingTowardRequestOverFartherIdleOne() {
        Elevator movingToward = Elevator.builder().id(1L).currentFloor(2).state(ElevatorState.MOVING).direction(Direction.UP).build();
        Elevator idleFar = Elevator.builder().id(2L).currentFloor(9).state(ElevatorState.IDLE).direction(Direction.NONE).build();

        Elevator best = schedulingService.findBestElevator(List.of(movingToward, idleFar), 5, Direction.UP);

        assertThat(best.getId()).isEqualTo(1L);
    }

    @Test
    void excludesMaintenanceAndFaultElevators() {
        Elevator maintenance = Elevator.builder().id(1L).currentFloor(1).state(ElevatorState.MAINTENANCE).direction(Direction.NONE).build();
        Elevator faulted = Elevator.builder().id(2L).currentFloor(1).state(ElevatorState.FAULT).direction(Direction.NONE).build();
        Elevator healthy = Elevator.builder().id(3L).currentFloor(6).state(ElevatorState.IDLE).direction(Direction.NONE).build();

        Elevator best = schedulingService.findBestElevator(List.of(maintenance, faulted, healthy), 5, Direction.UP);

        assertThat(best.getId()).isEqualTo(3L);
    }

    @Test
    void returnsNullWhenNoElevatorsAvailable() {
        Elevator maintenance = Elevator.builder().id(1L).currentFloor(1).state(ElevatorState.MAINTENANCE).direction(Direction.NONE).build();

        Elevator best = schedulingService.findBestElevator(List.of(maintenance), 5, Direction.UP);

        assertThat(best).isNull();
    }

    @Test
    void balancesLoadBetweenEquallyPositionedElevators() {
        Elevator loaded = Elevator.builder().id(1L).currentFloor(5).state(ElevatorState.IDLE).direction(Direction.NONE)
                .requests(List.of(
                        com.example.elevator.entity.ElevatorRequest.builder().id(1L).build(),
                        com.example.elevator.entity.ElevatorRequest.builder().id(2L).build()))
                .build();
        Elevator free = Elevator.builder().id(2L).currentFloor(5).state(ElevatorState.IDLE).direction(Direction.NONE).build();

        Elevator best = schedulingService.findBestElevator(List.of(loaded, free), 5, Direction.UP);

        assertThat(best.getId()).isEqualTo(2L);
    }
}
