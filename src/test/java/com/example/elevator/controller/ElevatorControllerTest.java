package com.example.elevator.controller;

import com.example.elevator.dto.AssignRequestDTO;
import com.example.elevator.dto.ElevatorRequestDTO;
import com.example.elevator.entity.Direction;
import com.example.elevator.entity.Elevator;
import com.example.elevator.entity.ElevatorState;
import com.example.elevator.security.JwtAuthFilter;
import com.example.elevator.security.RateLimitFilter;
import com.example.elevator.service.ElevatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ElevatorController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters - this slice tests controller logic only;
                                            // role-based access control itself is covered by manual/integration testing
class ElevatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ElevatorService elevatorService;

    // Security beans referenced by the filter chain are mocked out so the
    // slice test doesn't need the full JWT stack.
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @Test
    @WithMockUser(roles = "PASSENGER")
    void requestElevator_returnsCreated() throws Exception {
        ElevatorRequestDTO dto = new ElevatorRequestDTO(1, 5, Direction.UP);
        when(elevatorService.requestElevator(any())).thenReturn(
                com.example.elevator.entity.ElevatorRequest.builder().id(1L).requestFloor(1).destinationFloor(5).direction(Direction.UP).build());

        mockMvc.perform(post("/api/elevators/request")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void getAllStatuses_returnsOk() throws Exception {
        when(elevatorService.getAllStatuses()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/elevators/status"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void manualAssign_asAdmin_returnsOk() throws Exception {
        AssignRequestDTO dto = new AssignRequestDTO(7);
        Elevator elevator = Elevator.builder().id(1L).currentFloor(1).targetFloor(7).state(ElevatorState.MOVING).direction(Direction.UP).build();
        when(elevatorService.manualAssign(anyLong(), any(Integer.class))).thenReturn(elevator);

        mockMvc.perform(put("/api/elevators/1/assign")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void requestElevator_invalidPayload_returnsBadRequest() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/elevators/request")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
