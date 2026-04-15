# API Reference

**Base URL (local):** `http://localhost:8080`

**Interactive docs:** `http://localhost:8080/swagger-ui.html`

---

## Authentication

All endpoints except Auth require a valid access token in every request:

```
Authorization: Bearer <accessToken>
```

Access tokens are valid for **15 minutes**. When expired, call `POST /api/v1/auth/refresh` with your refresh token to obtain a new pair without re-authenticating. The old refresh token is immediately revoked — each one is single-use.

---

## Common Error Codes

| Code | Meaning |
|------|---------|
| `400` | Validation error, bad request body, or business rule violation |
| `401` | Token missing, expired, or invalid — re-authenticate |
| `404` | Resource not found **or** resource exists but belongs to another user (IDOR prevention — both cases return the same 404) |
| `500` | Internal server error — details are intentionally hidden |

---

## Endpoints by Feature

| File | Endpoints |
|------|-----------|
| [auth.md](auth.md) | Register · Login · Refresh token · Logout |
| [decks.md](decks.md) | Deck CRUD · Deck stats |
| [cards.md](cards.md) | Card CRUD · All four card types |
| [users.md](users.md) | Authenticated user profile |
| [categories.md](categories.md) | Category listing (autocomplete) |
| [tags.md](tags.md) | Tag listing · Cards by tag |
| [study.md](study.md) | Due cards · Submit study session (FSRS-6) |
