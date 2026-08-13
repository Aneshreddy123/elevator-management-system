package com.example.elevator.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single passenger call: "I'm at requestFloor, I want to go to
 * destinationFloor, heading direction." Created by
 * {@link com.example.elevator.controller.ElevatorController#requestElevator}
 * and linked to whichever elevator the scheduler assigns.
 */
@Entity
@Table(name = "elevator_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevatorRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int requestFloor;
    private int destinationFloor;

    @Enumerated(EnumType.STRING)
    private Direction direction; // UP, DOWN

    private LocalDateTime requestTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elevator_id")
    @JsonIgnoreProperties({"requests"})
    private Elevator elevator;
}
