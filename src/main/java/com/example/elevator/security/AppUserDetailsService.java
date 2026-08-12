package com.example.elevator.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Demo in-memory user store. Swap for a UserRepository/DB-backed implementation
 * in a real deployment.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, String[]> users; // username -> [encodedPassword, role]

    public AppUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.users = Map.of(
                "admin", new String[]{passwordEncoder.encode("admin123"), "ADMIN"},
                "passenger", new String[]{passwordEncoder.encode("passenger123"), "PASSENGER"}
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String[] record = users.get(username);
        if (record == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return User.builder()
                .username(username)
                .password(record[0])
                .authorities(new SimpleGrantedAuthority("ROLE_" + record[1]))
                .build();
    }

    public String getRole(String username) {
        String[] record = users.get(username);
        return record != null ? record[1] : null;
    }

    public boolean matches(String username, String rawPassword) {
        String[] record = users.get(username);
        return record != null && passwordEncoder.matches(rawPassword, record[0]);
    }
}
