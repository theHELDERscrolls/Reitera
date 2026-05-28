# Backend — Reitera API

Spring Boot 4.0.6 REST API for the Reitera spaced repetition application.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16 (via Docker for local development)

## Running locally

```bash
# From the repo root — start the database first
docker compose up -d

# Then start the API
cd backend/reitera-backend
mvn spring-boot:run
# API available at http://localhost:8080
```

See [docs/setup/local-environment.md](../docs/setup/local-environment.md) for the full setup guide including schema initialization.

## Interactive API docs

Swagger UI is available in the `dev` profile only:

```
http://localhost:8080/swagger-ui/index.html
```

The Postman collection with all endpoints pre-configured is at [`docs/api/reitera-postman-collection.json`](../docs/api/reitera-postman-collection.json).

## Tests

```bash
mvn test
```

## Architecture

See [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) for the module structure, security model, and design decisions.
