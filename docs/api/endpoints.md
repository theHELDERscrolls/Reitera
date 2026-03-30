# API Reference

Base URL (local): `http://localhost:8080`

Interactive documentation: `http://localhost:8080/swagger-ui.html`

All endpoints except Auth require a valid **access token** in the `Authorization` header:
```
Authorization: Bearer <accessToken>
```

Access tokens are valid for **15 minutes**. When expired, use `POST /api/v1/auth/refresh` with your refresh token to obtain a new pair without re-authenticating.

---

## Auth

Public endpoints — no token required.

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

**Errors:** `400` if the token is missing · `500` if the token is invalid, expired, or already revoked.

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

**Errors:** `400` if the token is missing · `500` if the token is invalid or already revoked.

---

## Decks

All deck endpoints are owner-scoped: users can only access their own decks.

### POST `/api/v1/decks`
Creates a new deck for the authenticated user.

**Request body:**
```json
{
  "title": "Historia de España",
  "description": "Repaso de eventos clave del siglo XX",
  "isPublic": false,
  "categoryId": null,
  "categoryName": null
}
```

- `title` is required (max 100 characters).
- `categoryId` and `categoryName` are both optional. Use one or the other:
  - `categoryId` — assigns an existing category by ID.
  - `categoryName` — find-or-create: if a category with that name already exists (case-insensitive) it is reused; otherwise a new category row is created automatically.
  - If both are supplied, `categoryId` takes precedence.
  - If neither is supplied, the deck is created without a category.

**Response `201 Created`:** `DeckResponseDTO`

---

### GET `/api/v1/decks`
Returns a paginated list of decks owned by the authenticated user.

| Query param | Type | Default | Description |
|---|---|---|---|
| `page` | Integer | `0` | Zero-based page number |
| `size` | Integer | `20` | Items per page |
| `sort` | String | `createdAt,desc` | Field and direction (e.g. `title,asc`) |

**Response `200 OK`:** `Page<DeckResponseDTO>`
```json
{
  "content": [ ...decks... ],
  "totalElements": 47,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

### GET `/api/v1/decks/{id}`
Returns a single deck by ID.

**Response `200 OK`:** `DeckResponseDTO`
**Errors:** `404` if not found · `400` if not the owner

---

### PUT `/api/v1/decks/{id}`
Updates an existing deck (full replacement of all fields).

**Request body:** Same as POST. The same `categoryId` / `categoryName` find-or-create logic applies. If the category changes, the old category is automatically deleted if no other deck references it.

**Response `200 OK`:** Updated `DeckResponseDTO`

---

### DELETE `/api/v1/decks/{id}`
Deletes a deck by ID. If the deck had a category and no other deck references it after deletion, the category row is automatically deleted.

**Response `204 No Content`**

---

#### DeckResponseDTO shape
```json
{
  "id": 1,
  "title": "Historia de España",
  "description": "...",
  "isPublic": false,
  "authorName": "alumno",
  "categoryId": null,
  "categoryName": null,
  "createdAt": "2026-03-18T10:00:00",
  "updatedAt": "2026-03-18T10:00:00"
}
```

---

## Cards

Cards are nested under their parent deck. All endpoints verify deck ownership before operating on cards.

### POST `/api/v1/decks/{deckId}/cards`
Creates a new card inside the specified deck.

The `answerJson` structure varies by card type:

**BASIC** — simple question / answer:
```json
{
  "type": "BASIC",
  "question": "¿En qué año comenzó la Guerra Civil Española?",
  "answerJson": { "answer": "1936" },
  "explanation": "El conflicto se inició el 17 de julio de 1936.",
  "tagIds": []
}
```

**MULTIPLE_CHOICE** — one correct option among several:
```json
{
  "type": "MULTIPLE_CHOICE",
  "question": "¿Cuál fue el bando vencedor de la Guerra Civil Española?",
  "answerJson": {
    "options": ["Bando Republicano", "Bando Nacional", "Ninguno"],
    "correctIndex": 1
  },
  "explanation": "El bando Nacional venció en 1939.",
  "tagIds": []
}
```

**CLOZE** — fill in the blank (use `___` as the gap marker in the question):
```json
{
  "type": "CLOZE",
  "question": "La Guerra Civil terminó en ___.",
  "answerJson": { "answer": "1939" },
  "explanation": null,
  "tagIds": []
}
```

**TRUE_FALSE** — true or false statement:
```json
{
  "type": "TRUE_FALSE",
  "question": "La Guerra Civil Española comenzó en 1936.",
  "answerJson": { "answer": true },
  "explanation": "El conflicto se inició el 17 de julio de 1936.",
  "tagIds": []
}
```

`tagIds` is optional — send an empty array or omit it if no tags apply.

**Response `201 Created`:** `CardResponseDTO`

---

### GET `/api/v1/decks/{deckId}/cards`
Returns a paginated list of cards belonging to the specified deck.

| Query param | Type | Default | Description |
|---|---|---|---|
| `page` | Integer | `0` | Zero-based page number |
| `size` | Integer | `20` | Items per page |
| `sort` | String | — | Field and direction (e.g. `id,asc`) |

**Response `200 OK`:** `Page<CardResponseDTO>`
```json
{
  "content": [ ...cards... ],
  "totalElements": 120,
  "totalPages": 6,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

### GET `/api/v1/decks/{deckId}/cards/{cardId}`
Returns a single card by ID.

**Response `200 OK`:** `CardResponseDTO`
**Errors:** `404` if not found or card does not belong to the declared deck

---

### PUT `/api/v1/decks/{deckId}/cards/{cardId}`
Updates an existing card (full replacement of all fields).

**Request body:** Same as POST.

**Response `200 OK`:** Updated `CardResponseDTO`

---

### DELETE `/api/v1/decks/{deckId}/cards/{cardId}`
Deletes a card by ID.

**Response `204 No Content`**

---

#### CardResponseDTO shape
```json
{
  "id": 1,
  "deckId": 1,
  "type": "BASIC",
  "question": "¿En qué año comenzó la Guerra Civil Española?",
  "answerJson": { "answer": "1936" },
  "explanation": "El conflicto se inició el 17 de julio de 1936.",
  "tags": [
    { "id": 1, "name": "historia", "hexColor": "#FF5733" }
  ]
}
```

---

## Users

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
  "roleName": "STUDENT"
}
```

---

## Categories

Categories are a shared global table — there is no dedicated creation endpoint. They are created automatically via the find-or-create logic in `POST /api/v1/decks` and `PUT /api/v1/decks/{id}` when a `categoryName` is supplied. They are also deleted automatically when no deck references them anymore (orphan cleanup).

### GET `/api/v1/categories`
Returns the distinct categories assigned to the authenticated user's own decks. Intended for frontend autocomplete when creating or editing a deck.

**Response `200 OK`:**
```json
[
  { "id": 1, "name": "Historia de España", "description": null },
  { "id": 2, "name": "Programación Java",  "description": null }
]
```

Returns an empty array if the user has no decks with a category assigned.

---

## Tags

Returns only the tags used on cards in the authenticated user's own decks.

### GET `/api/v1/tags`
Returns all distinct tags used on the user's cards.

**Response `200 OK`:**
```json
[
  { "id": 1, "name": "importante", "hexColor": "#E74C3C" },
  { "id": 2, "name": "difícil",    "hexColor": "#E67E22" }
]
```

Tags that exist in the system but are not assigned to any of the user's cards will **not** appear.

---

### GET `/api/v1/tags/{tagId}/cards`
Returns a paginated list of cards owned by the authenticated user that have the specified tag.

| Query param | Type | Default | Description |
|---|---|---|---|
| `page` | Integer | `0` | Zero-based page number |
| `size` | Integer | `20` | Items per page |
| `sort` | String | — | Field and direction (e.g. `id,asc`) |

**Response `200 OK`:** `Page<CardResponseDTO>`

Always returns `200` with an empty page for unknown tag IDs — no `404` is thrown to prevent tag ID enumeration.

---

## Study

Core study endpoints powered by the FSRS-6 spaced repetition algorithm.
All endpoints require a valid JWT token.

### GET `/api/v1/study/due`

Returns all cards the user should study now. Provide **exactly one** query parameter:

| Parameter | Type | Description |
|-----------|------|-------------|
| `deckId` | Integer | Study a single deck |
| `categoryId` | Integer | Study all decks in a category (e.g. all topics of "Historia de España") |

The response merges two groups:
1. **Overdue cards** — previously studied cards whose `nextReview` is in the past
2. **New cards** — cards with no study history for this user

Overdue cards appear first so the user revisits pending material before tackling new content.

**Response `200 OK`:** Array of `DueCardDTO`

```
GET /api/v1/study/due?deckId=1
GET /api/v1/study/due?categoryId=2
```

#### DueCardDTO shape
```json
{
  "id": 1,
  "deckId": 1,
  "type": "BASIC",
  "question": "¿En qué año comenzó la Guerra Civil Española?",
  "answerJson": { "answer": "1936" },
  "explanation": "El conflicto se inició el 17 de julio de 1936.",
  "tags": [
    { "id": 1, "name": "historia", "hexColor": "#FF5733" }
  ],
  "state": 0
}
```

`state` values: `0` = New · `1` = Learning · `2` = Review · `3` = Relearning

---

### POST `/api/v1/study/sessions`

Processes a completed study session. Runs the FSRS-6 algorithm for each rated card,
updates `StudyProgress`, and writes an immutable `ReviewLog` entry.
The entire batch runs in a single transaction — if any card fails, the whole session is rolled back.

Provide **exactly one** of `deckId` or `categoryId` to match the scope used to fetch the due cards.

**Request body:**
```json
{
  "deckId": 1,
  "categoryId": null,
  "ratings": [
    { "cardId": 1, "rating": 3 },
    { "cardId": 2, "rating": 1 },
    { "cardId": 3, "rating": 4 }
  ]
}
```

Rating scale: `1` = Again · `2` = Hard · `3` = Good · `4` = Easy

For category sessions, replace `deckId` with `categoryId` and set `deckId` to `null`:
```json
{
  "deckId": null,
  "categoryId": 2,
  "ratings": [
    { "cardId": 5, "rating": 3 }
  ]
}
```

**Response `201 Created`:**
```json
{
  "deckId": 1,
  "categoryId": null,
  "cardsReviewed": 3
}
```

**Errors:** `404` if deck/category/card not found · `400` if both or neither scope provided · `400` if a card does not belong to the declared scope
