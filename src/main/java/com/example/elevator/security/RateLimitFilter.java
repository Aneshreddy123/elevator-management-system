package com.example.elevator.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client-IP token bucket rate limiter for the /api/elevators/** endpoints.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.capacity:30}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens:30}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration-seconds:60}")
    private int refillDurationSeconds;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity,
                io.github.bucket4j.Refill.greedy(refillTokens, Duration.ofSeconds(refillDurationSeconds)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/elevators")) {
            chain.doFilter(request, response);
            return;
        }

        String clientId = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded, try again later.\"}");
        }
    }
}
