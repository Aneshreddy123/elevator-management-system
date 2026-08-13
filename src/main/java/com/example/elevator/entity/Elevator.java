package com.example.elevator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single physical elevator car in the building.
 *
 * Implements {@link Serializable} because instances are stored in the Redis
 * status cache (see {@link com.example.elevator.config.RedisConfig}) and
 * carried across Kafka/JSON boundaries via {@link com.example.elevator.dto.ElevatorStatusDTO}.
 */
@Entity
@Table(name = "elevators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Elevator implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int currentFloor;
    private int targetFloor;

    @Enumerated(EnumType.STRING)
    private ElevatorState state; // MOVING, IDLE, MAINTENANCE, FAULT

    @Enumerated(EnumType.STRING)
    private Direction direction; // UP, DOWN, NONE

    private int capacity;

    private LocalDateTime lastMaintenance;

    @Builder.Default
    private boolean healthy = true;

    @OneToMany(mappedBy = "elevator", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<ElevatorRequest> requests = new ArrayList<>();

    /**
     * Number of requests currently queued against this elevator.
     * Used by {@link com.example.elevator.service.ElevatorSchedulingService}
     * to penalize already-busy elevators for load balancing.
     */
    public int activeLoad() {
        return requests == null ? 0 : requests.size();
    }
}
