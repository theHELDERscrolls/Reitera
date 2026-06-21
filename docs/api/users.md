# User Endpoints

### GET `/api/v1/users/me`

Returns the profile of the currently authenticated user.

**Response `200 OK`:**

```json
{
  "id": "uuid",
  "username": "alumno",
  "email": "alumno@reitera.com",
  "firstName": "Alumno",
  "lastName": "Demo",
  "avatarId": "avatar-03",
  "createdAt": "2026-01-01T10:00:00"
}
```

`avatarId` is `null` when the user has not selected an avatar.

---

### PUT `/api/v1/users/me`

Updates the personal data and avatar of the authenticated user. Username uniqueness is enforced — if the new username is already taken by a different account, the request is rejected with `409 Conflict`.

**Request body:**

```json
{
  "username": "newname",
  "firstName": "New",
  "lastName": "Name",
  "avatarId": "avatar-03"
}
```

| Field | Required | Constraints |
|-------|----------|-------------|
| `username` | yes | 3–50 characters |
| `firstName` | yes | max 50 characters |
| `lastName` | yes | max 100 characters |
| `avatarId` | no | max 50 characters; one of the predefined IDs (`avatar-01` … `avatar-08`), or `null` to clear |

**Response `200 OK`:** same shape as `GET /api/v1/users/me`.

**Errors:**

| Code | Reason |
|------|--------|
| `400` | Validation failure (blank required field, length exceeded) |
| `401` | Missing or invalid token |
| `409` | Username already taken by another account |
