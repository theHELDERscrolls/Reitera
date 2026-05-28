# Security Architecture

## Authentication Model

Authentication uses a **two-token stateless strategy**:

| Token         | Type                         | Expiry     | Storage                |
| ------------- | ---------------------------- | ---------- | ---------------------- |
| Access token  | Signed JWT (HMAC-SHA)        | 15 minutes | `localStorage`         |
| Refresh token | Random UUID (SHA-256 hashed) | 7 days     | `refresh_tokens` table |

**Flow:**

1. `POST /auth/login` → server issues both tokens; refresh token hash persisted in DB
2. Client sends `Authorization: Bearer <accessToken>` on every request
3. `JwtAuthenticationFilter` validates the JWT and populates the `SecurityContext`; if the token is missing, malformed, or expired (`JwtException`) the filter skips authentication and continues the chain — Spring Security then returns `401 Unauthorized` via the configured `AuthenticationEntryPoint`
4. Controllers receive the authenticated `User` via `@AuthenticationPrincipal`
5. When the access token expires, `jwtInterceptor` intercepts the `401`, calls `POST /auth/refresh` transparently, persists the new token pair, and retries the original request — the user never notices the renewal
6. If the refresh token is also expired or revoked, `AuthService.logout()` is called and the user is redirected to the login page
7. `POST /auth/logout` → server revokes all active refresh tokens for the user

Public endpoints (no token required): `/register`, `/login`, `/refresh`, `/logout`, `/verify`, `/resend-verification`, `/forgot-password`, `/reset-password`
All other endpoints are protected.

**Refresh token rotation:** each refresh token is single-use. After being exchanged for a new pair, it is immediately marked `revoked = true`. This limits the damage window if a token is stolen — once used, the old token is worthless.

## Security Decisions

### 404 instead of 403 on unauthorized resource access (IDOR prevention)

When a user requests a resource that exists but belongs to another user, the API returns `404 Not Found` instead of `403 Forbidden`.

Returning `403` would confirm to an attacker that the resource exists, enabling enumeration: by iterating IDs and observing 403 vs 404 responses, an attacker could map out which IDs are valid. This is an IDOR (Insecure Direct Object Reference) vulnerability listed in the OWASP Top 10.

Returning `404` in both cases (resource not found, resource belongs to another user) makes the response indistinguishable. The attacker learns nothing about the existence of resources they do not own.

Applied in: `DeckService.findOwnedDeck()`, `CardService.findOwnedDeck()`.

### `authorName` uses the nickname (`username` field), not `firstName + lastName`

Deck author attribution uses the unique `username` field (the user's chosen nickname) rather than `firstName + lastName`. Full names are not unique — multiple users can share the same name. The `username` column has a unique constraint and unambiguously identifies the author.

**Implementation note:** `User` implements Spring Security's `UserDetails`, which forces an override of `getUsername()` to return the email (the authentication principal). Lombok cannot generate a getter for the `username` field because that method name is taken. A dedicated `getNickname()` method exposes the actual nickname value. Any code that needs the display username must call `getNickname()`, not `getUsername()`.

### Rate limiting on authentication endpoints

Auth endpoints are rate-limited to **5 requests per minute per client IP** using Bucket4j's token-bucket algorithm (`RateLimitFilter`, runs before the JWT filter). When the bucket is empty the server returns `429 Too Many Requests` with a `Retry-After: 60` header so clients know when to retry. Protected paths: `/login`, `/register`, `/resend-verification`, `/forgot-password`, `/reset-password`.

The real IP is read from the `X-Forwarded-For` header first (needed for the Render reverse-proxy), falling back to `request.getRemoteAddr()`. Buckets are stored in a Caffeine `LoadingCache` — one bucket per IP string — with a 10-minute expiration after last access. Inactive IPs are evicted automatically, preventing unbounded memory growth on single-instance deployments. A Redis-backed bucket would be needed for horizontal scaling.

### 409 Conflict on registration duplicates

When registration fails because the email or username is already taken, the API returns `409 Conflict` with the generic message `"The provided data is invalid or already in use"` — the same message regardless of which field caused the conflict.

The 409 status (vs. the previous 400) lets the frontend distinguish a data conflict from a validation error and show a user-friendly warning toast. The message stays generic so an attacker cannot tell whether the email or the username was the duplicate. Enumeration risk at registration is accepted given that the forgot-password flow exposes the same information anyway.

Applied in: `UserService.registerUser()` via `DataConflictException` → `GlobalExceptionHandler.handleDataConflict()` → 409.

### Email verification requirement

Newly registered users cannot log in until they verify their email address. The verification flow:

1. `POST /auth/register` → account created, verification email sent asynchronously via Resend SDK
2. Email contains a signed link: `{app.base-url}/verify?token={rawToken}`
3. The raw token is SHA-256 hashed before storage — the DB never holds the plaintext token
4. `GET /auth/verify?token=` → token hashed, matched against DB, expiry checked (24h TTL), account activated
5. `POST /auth/resend-verification` → always returns 200 regardless of whether the email exists (anti-enumeration); re-sends only if the account is not yet verified

`verifyToken` is `@Transactional` to prevent a race condition where two concurrent verification requests could both pass the expiry check before either marks the account as verified.

### Rate limiting scope

The rate limiter (Bucket4j, 5 req/min per IP) keys buckets on `ip:path` rather than `ip` alone. This prevents a sustained attack on `/login` from exhausting the budget for `/register`, `/resend-verification`, `/forgot-password`, or `/reset-password` and vice versa.

### HTTP security headers

Every response includes a fixed set of security headers applied globally in `SecurityConfig`:

| Header                      | Value                                        | Protects against           |
| --------------------------- | -------------------------------------------- | -------------------------- |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains`        | Protocol downgrade / MITM  |
| `Content-Security-Policy`   | `default-src 'none'; frame-ancestors 'none'` | Content injection, framing |
| `X-Frame-Options`           | `deny`                                       | Clickjacking               |
| `X-Content-Type-Options`    | `nosniff`                                    | MIME-type sniffing         |
| `Referrer-Policy`           | `strict-origin-when-cross-origin`            | Referrer leakage           |

### Input size constraints

All request DTOs carry `@Size` (and `@Pattern` for colors) constraints validated by Spring's `@Valid` pipeline. The limits mirror the database column sizes and prevent oversized payloads from reaching the service layer:

- Auth: email ≤ 100, password ≤ 128, refresh token ≤ 512
- Deck: title ≤ 100, description ≤ 2 000, category ≤ 50
- Card: question ≤ 5 000, explanation ≤ 2 000, `answerJson` ≤ 10 KB (custom validator)
