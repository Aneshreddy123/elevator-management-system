package com.example.elevator.controller;

import com.example.elevator.dto.AuthRequest;
import com.example.elevator.dto.AuthResponse;
import com.example.elevator.security.AppUserDetailsService;
import com.example.elevator.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Demo authentication endpoint. Issues JWTs for the two seeded accounts:
 *   admin / admin123       -> ROLE_ADMIN
 *   passenger / passenger123 -> ROLE_PASSENGER
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AppUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain a JWT")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        if (!userDetailsService.matches(request.getUsername(), request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
        String role = userDetailsService.getRole(request.getUsername());
        String token = jwtUtil.generateToken(request.getUsername(), role);
        return ResponseEntity.ok(new AuthResponse(token, role, jwtUtil.getExpirationMs()));
    }
}
