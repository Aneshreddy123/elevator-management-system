# Elevator Management System

A Spring Boot 3 / Java 17 RESTful API for multi-elevator coordination: intelligent
dispatch (MinHeap scheduling), real-time async movement simulation via Kafka,
Redis-cached status queries, JWT + role-based security, Resilience4j circuit
breaking with a watchdog auto-recovery loop, and full test coverage.

## Architecture

```
Client → RateLimitFilter → JwtAuthFilter → SecurityFilterChain → Controller
                                                                     │
                                                                     ▼
                                        ElevatorService (business logic)
                                        ├─ ElevatorSchedulingService (MinHeap dispatch)
                                        ├─ Redis (@Cacheable status)
                                        ├─ Resilience4j @CircuitBreaker (simulate)
                                        ├─ KafkaMovementProducer → topic → KafkaMovementConsumer → elevator_logs
                                        └─ @Scheduled watchdog → auto-repair FAULT elevators
```

## Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Get a JWT (`admin`/`admin123` or `passenger`/`passenger123`) |
| POST | `/api/elevators/request` | Authenticated | Passenger call; MinHeap picks the best elevator |
| GET | `/api/elevators/status` | Authenticated | Redis-cached fleet status |
| PUT | `/api/elevators/{id}/assign` | Admin | Manually assign a target floor |
| POST | `/api/elevators/simulate?elevatorId=` | Authenticated | Async one-step movement simulation (Kafka event) |
| GET | `/api/elevators/logs?page=&size=` | Authenticated | Paginated movement logs |
| PUT | `/api/elevators/{id}/repair` | Admin | Fault recovery / restart |
| GET | `/api/elevators/optimize` | Admin | Traffic-based batch route optimization |

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Core scheduling algorithm

`ElevatorSchedulingService` ranks all non-maintenance/fault elevators with a
`PriorityQueue` (MinHeap) on a weighted cost:

```
cost = |currentFloor - requestFloor|
     + directionPenalty   (0 if already heading toward the call in the right
                            direction, small penalty if idle, large if moving away)
     + activeLoad * loadWeight   (load balancing across the fleet)
```

The head of the heap is the elevator dispatched. See
`ElevatorSchedulingServiceTest` for behavioral proof (nearest-wins, direction
affinity, maintenance exclusion, load balancing).

## Run locally (dev profile — H2 in-memory DB)

Requires: Java 17, Maven, a local Redis and Kafka (or use `docker-compose` for
just those two and run the app from your IDE).

```bash
mvn spring-boot:run
```

The app seeds 4 elevators on startup (dev profile only). H2 console:
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:elevatordb`).

## Run everything with Docker Compose (prod profile — Postgres + Redis + Kafka)

```bash
docker compose up --build
```

This brings up the app, PostgreSQL, Redis, Zookeeper, and Kafka. The API is
available at `http://localhost:8080`.

## Authentication quickstart

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Use the returned `token` as `Authorization: Bearer <token>` on subsequent calls.
Import `postman_collection.json` into Postman for a ready-made request set.

## Testing

```bash
mvn test
```

- `ElevatorSchedulingServiceTest` — unit tests for the dispatch algorithm.
- `ElevatorControllerTest` — `@WebMvcTest` slice tests with mocked service layer.

## Configuration

Key environment variables (see `application.yml` / `application-prod.yml`):

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | HMAC signing key for JWTs |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker(s) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL (prod profile) |

## CI/CD

`.github/workflows/ci-cd.yml` builds and tests on every push/PR, then builds
and publishes a Docker image to GHCR on merges to `main`.

## Notes / known simplifications

- User accounts are seeded in-memory (`AppUserDetailsService`) for demo
  purposes; swap in a DB-backed `UserRepository` for production use.
- Rate limiting uses an in-memory Bucket4j map keyed by client IP; for a
  multi-instance deployment back it with Redis (Bucket4j has a Redis
  proxy-manager) instead.
- The optimizer performs a lightweight traffic-based pre-positioning pass;
  the ML-based peak-hour prediction mentioned as an optional bonus is not
  implemented.
