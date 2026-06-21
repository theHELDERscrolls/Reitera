# Local Environment Setup

This guide covers everything needed to run the Reitera project locally from scratch.

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 21 | Backend runtime |
| Maven | 3.9+ | Backend build tool |
| Node.js | 22 LTS | Frontend runtime |
| npm | 10+ | Frontend package manager |
| Docker + Docker Compose | Latest | PostgreSQL database container |

## 1. Clone the Repository

```bash
git clone https://github.com/theHELDERscrolls/Reitera.git
cd Reitera
```

## 2. Start the Database

The database runs in a Docker container. From the **root** of the repository:

```bash
docker compose up -d
```

Verify the container is healthy:
```bash
docker ps
# reitera_postgres should show status: healthy
```

See `docs/setup/database-setup.md` for full database setup and schema initialization instructions.

## 3. Run the Backend

```bash
cd backend/reitera-backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

To verify it's running, check the health of a public endpoint:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"wrong"}'
# Expected: 400 with {"error": "Invalid credentials"}
```

## 4. Run the Frontend

```bash
cd frontend
npm install
npm start
```

The Angular dev server will be available at `http://localhost:4200`.

## 5. Run the Tests

### Frontend

```bash
cd frontend
ng test
```

Runs all 74 unit tests with Vitest + jsdom in a single pass. No database or backend is required — HTTP calls are intercepted by `HttpTestingController`.

### Backend

```bash
cd backend/reitera-backend
mvn test
```

Requires the PostgreSQL container to be running (step 2 above) because the backend uses `hibernate.ddl-auto: none`, native JSONB columns, and `pgcrypto` — none of which an in-memory database supports.

## 6. Testing the API with Postman

Import the collection at `docs/api/reitera-postman-collection.json` into Postman.

The collection includes:
- Pre-configured `base_url` variable (`http://localhost:8080`)
- Auto-save script on the Login request that stores the JWT into the `token` variable
- Example request bodies for note creation (all 4 types: BASIC, BASIC_REVERSE, CLOZE, MULTIPLE_CHOICE)

**Recommended flow:**
1. Run **Register** to create a test user
2. Run **Login** — token is saved automatically
3. All other requests will use the token from that point on

## Environment Variables

All local configuration lives in `backend/reitera-backend/src/main/resources/application-dev.yml`, which is loaded automatically because `spring.profiles.active: dev` is set as the default in `application.yml`. No environment variables are needed to run locally.

To verify the backend is running correctly, check the health endpoint:

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

For production environment variables and the deployment setup, see [docs/ARCHITECTURE.md — Production Deployment](../ARCHITECTURE.md#production-deployment).
