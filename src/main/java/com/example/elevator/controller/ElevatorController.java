package com.example.elevator.controller;

import com.example.elevator.dto.AssignRequestDTO;
import com.example.elevator.dto.ElevatorRequestDTO;
import com.example.elevator.dto.ElevatorStatusDTO;
import com.example.elevator.entity.Elevator;
import com.example.elevator.entity.ElevatorLog;
import com.example.elevator.entity.ElevatorRequest;
import com.example.elevator.service.ElevatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elevators")
@RequiredArgsConstructor
@Tag(name = "Elevator Management")
@SecurityRequirement(name = "bearerAuth")
public class ElevatorController {

    private final ElevatorService elevatorService;

    @PostMapping("/request")
    @Operation(summary = "Request an elevator (passenger call)")
    public ResponseEntity<ElevatorRequest> requestElevator(@Valid @RequestBody ElevatorRequestDTO dto) {
        ElevatorRequest request = elevatorService.requestElevator(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/status")
    @Operation(summary = "Get status of all elevators (Redis-cached)")
    public ResponseEntity<List<ElevatorStatusDTO>> getAllStatuses() {
        return ResponseEntity.ok(elevatorService.getAllStatuses());
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Manually assign an elevator (Admin only)")
    public ResponseEntity<Elevator> manualAssign(@PathVariable Long id, @Valid @RequestBody AssignRequestDTO dto) {
        return ResponseEntity.ok(elevatorService.manualAssign(id, dto.getTargetFloor()));
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simulate elevator movement (async)")
    public ResponseEntity<String> simulate(@RequestParam Long elevatorId) {
        elevatorService.simulateMovement(elevatorId);
        return ResponseEntity.accepted().body("Simulation started for elevator " + elevatorId);
    }

    @GetMapping("/logs")
    @Operation(summary = "Get paginated elevator logs")
    public ResponseEntity<Page<ElevatorLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ElevatorLog> logs = elevatorService.getLogs(PageRequest.of(page, size, Sort.by("timestamp").descending()));
        return ResponseEntity.ok(logs);
    }

    @PutMapping("/{id}/repair")
    @Operation(summary = "Fault detection auto-recovery / manual repair (Admin only)")
    public ResponseEntity<Elevator> repair(@PathVariable Long id) {
        return ResponseEntity.ok(elevatorService.repair(id));
    }

    @GetMapping("/optimize")
    @Operation(summary = "Optimize routes based on traffic (Admin only)")
    public ResponseEntity<List<ElevatorStatusDTO>> optimize() {
        return ResponseEntity.ok(elevatorService.optimizeRoutes());
    }
}
