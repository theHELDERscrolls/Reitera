# Auth Endpoints

Public endpoints — no token required.

---

### POST `/api/v1/auth/register`
Creates a new user account.

**Request body:**
```json
{
  "username": "test01",
  "email": "test01@test.com",
  "password": "!Test123",
  "confirmPassword": "!Test123",
  "firstName": "Test",
  "lastName": "Testez"
}
```

Password rules: min 8 characters, at least 1 digit, 1 lowercase, 1 uppercase, 1 special character (`@#$%^&+=!`). `confirmPassword` must match `password` exactly.

**Response `201 Created`:**
```json
{
  "id": "uuid",
  "username": "test01",
  "email": "test01@test.com",
  "firstName": "Test",
  "lastName": "Testez",
  "roleName": "STUDENT"
}
```

---

### POST `/api/v1/auth/login`
Authenticates a user and returns an access token (15 min) and a refresh token (7 days).

**Request body:**
```json
{
  "email": "test01@test.com",
  "password": "!Test123"
}
```

**Response `200 OK`:**
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<uuid>",
  "message": "Login successful"
}
```

---

### POST `/api/v1/auth/refresh`
Issues a new access token and a rotated refresh token. The old refresh token is revoked immediately — each token can only be used once.

**Request body:**
```json
{
  "refreshToken": "<uuid>"
}
```

**Response `200 OK`:**
```json
{
  "accessToken": "<new-jwt>",
  "refreshToken": "<new-uuid>",
  "message": "Token refreshed"
}
```

**Errors:** `400` if the token is missing · `401` if the token is invalid, expired, or already revoked.

---

### POST `/api/v1/auth/logout`
Revokes all active refresh tokens for the user identified by the provided refresh token. The access token naturally expires after its remaining TTL (max 15 minutes).

**Request body:**
```json
{
  "refreshToken": "<uuid>"
}
```

**Response `204 No Content`**

**Errors:** `400` if the token is missing · `401` if the token is invalid or already revoked.
