package com.example.elevator.service;

import com.example.elevator.entity.Direction;
import com.example.elevator.entity.Elevator;
import com.example.elevator.entity.ElevatorState;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Intelligent Elevator Scheduling Algorithm.
 *
 * Uses a MinHeap (PriorityQueue) to select the best elevator for a given
 * passenger request, ranking candidates by a weighted cost combining:
 *   1. Distance from the elevator's current floor to the request floor.
 *   2. Direction affinity (an elevator already moving toward the request
 *      floor in the same direction is preferred over an idle one, which is
 *      preferred over one moving away).
 *   3. Current load (active request count) for load balancing across the fleet.
 *
 * Time complexity: O(n log n) for n elevators, dominated by heap operations.
 */
@Service
public class ElevatorSchedulingService {

    private static final int DIRECTION_MATCH_BONUS = 0;
    private static final int IDLE_PENALTY = 3;
    private static final int OPPOSITE_DIRECTION_PENALTY = 1000; // effectively disqualifies unless nothing else is free
    private static final int LOAD_WEIGHT = 2;

    /**
     * Selects the nearest, most suitable elevator for a request using a MinHeap.
     *
     * @param elevators        candidate elevators (should exclude MAINTENANCE/FAULT elevators upstream)
     * @param requestFloor     the floor the passenger is calling from
     * @param requestDirection the direction the passenger wants to travel
     * @return the best-fit elevator, or null if none are available
     */
    public Elevator findBestElevator(List<Elevator> elevators, int requestFloor, Direction requestDirection) {
        PriorityQueue<ScoredElevator> heap = new PriorityQueue<>(Comparator.comparingInt(se -> se.cost));

        for (Elevator elevator : elevators) {
            if (elevator.getState() == ElevatorState.MAINTENANCE || elevator.getState() == ElevatorState.FAULT) {
                continue;
            }
            int cost = computeCost(elevator, requestFloor, requestDirection);
            heap.offer(new ScoredElevator(elevator, cost));
        }

        ScoredElevator best = heap.poll();
        return best != null ? best.elevator : null;
    }

    private int computeCost(Elevator elevator, int requestFloor, Direction requestDirection) {
        int distance = Math.abs(elevator.getCurrentFloor() - requestFloor);
        int directionPenalty = directionPenalty(elevator, requestFloor, requestDirection);
        int loadPenalty = elevator.activeLoad() * LOAD_WEIGHT;
        return distance + directionPenalty + loadPenalty;
    }

    private int directionPenalty(Elevator elevator, int requestFloor, Direction requestDirection) {
        if (elevator.getState() == ElevatorState.IDLE) {
            return IDLE_PENALTY;
        }
        boolean movingTowardRequest =
                (elevator.getDirection() == Direction.UP && elevator.getCurrentFloor() <= requestFloor && requestDirection == Direction.UP) ||
                (elevator.getDirection() == Direction.DOWN && elevator.getCurrentFloor() >= requestFloor && requestDirection == Direction.DOWN);

        if (movingTowardRequest) {
            return DIRECTION_MATCH_BONUS;
        }
        return OPPOSITE_DIRECTION_PENALTY;
    }

    private record ScoredElevator(Elevator elevator, int cost) {
    }
}
