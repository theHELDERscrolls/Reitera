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
  "lastName": "Testez"
}
```

A verification email is sent automatically after registration. The user cannot log in until the email is verified.

**Errors:** `400` if passwords don't match or validation fails · `409 Conflict` if the email or username is already registered.

---

### GET `/api/v1/auth/verify?token=<token>`
Verifies the user's email address using the token from the verification email. Marks the account as active.

**Response `200 OK`** (no body)

**Errors:** `400` if the token is invalid or has expired (24-hour TTL).

---

### POST `/api/v1/auth/resend-verification`
Resends the verification email. Always returns 200 regardless of whether the email exists — prevents user enumeration.

**Request body:**
```json
{ "email": "test01@test.com" }
```

**Response `200 OK`** (no body)

**Errors:** `400` if the email field is blank or not a valid email format · `429` if rate limit exceeded (5 req/min per IP per endpoint).

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

---

### POST `/api/v1/auth/forgot-password`
Initiates the password reset flow. Sends a reset email with a one-time link (1-hour expiry) if the address is registered.

Always returns `200` regardless of whether the email exists — prevents user enumeration.

**Request body:**
```json
{ "email": "test01@test.com" }
```

**Response `200 OK`** (no body)

**Errors:** `400` if the email field is blank or not a valid email format · `429` if rate limit exceeded (5 req/min per IP per endpoint).

---

### POST `/api/v1/auth/reset-password`
Validates the reset token and sets a new password. The token is single-use — it is nullified immediately after a successful reset.

**Request body:**
```json
{
  "token": "<raw-uuid-token-from-email>",
  "newPassword": "NewPass@1234"
}
```

Password rules: min 8 characters, at least 1 digit, 1 lowercase, 1 uppercase, 1 special character (`@#$%^&+=!`). Max 128 characters.

**Response `200 OK`** (no body)

**Errors:** `400` if the token is invalid or has already been used · `410 Gone` if the token has expired (1-hour TTL) · `429` if rate limit exceeded.
