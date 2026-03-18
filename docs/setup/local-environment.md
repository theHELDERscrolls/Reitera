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

## 5. Testing the API with Postman

Import the collection at `docs/api/reitera-postman-collection.json` into Postman.

The collection includes:
- Pre-configured `base_url` variable (`http://localhost:8080`)
- Auto-save script on the Login request that stores the JWT into the `token` variable
- Example request bodies for all 3 card types (BASIC, CLOZE, MULTIPLE_CHOICE)

**Recommended flow:**
1. Run **Register** to create a test user
2. Run **Login** — token is saved automatically
3. All other requests will use the token from that point on

## Environment Variables

The backend reads configuration from `application.yml`. For local development, no additional environment variables are needed — defaults work out of the box.

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Backend API port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/reitera_db` | Database URL |
| `api.security.jwt.expiration-time` | `86400000` (24h) | JWT token lifetime in ms |
