package com.example.elevator.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
