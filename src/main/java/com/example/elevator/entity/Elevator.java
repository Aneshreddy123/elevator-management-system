package com.example.elevator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public int activeLoad() {
        return requests == null ? 0 : requests.size();
    }
}
