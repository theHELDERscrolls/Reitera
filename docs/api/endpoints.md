# API Reference

Base URL (local): `http://localhost:8080`

All endpoints except Auth require a valid JWT token in the `Authorization` header:
```
Authorization: Bearer <token>
```

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
  "firstName": "Test",
  "lastName": "Testez"
}
```
Password rules: min 8 characters, at least 1 digit, 1 lowercase, 1 uppercase, 1 special character (`@#$%^&+=!`).

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
Authenticates a user and returns a JWT token (valid 24 hours).

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
  "token": "<jwt>",
  "message": "Login successful"
}
```

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
  "categoryId": null
}
```
`categoryId` is optional. `title` is required (max 100 characters).

**Response `201 Created`:** `DeckResponseDTO`

---

### GET `/api/v1/decks`
Returns all decks owned by the authenticated user.

**Response `200 OK`:** Array of `DeckResponseDTO`

---

### GET `/api/v1/decks/{id}`
Returns a single deck by ID.

**Response `200 OK`:** `DeckResponseDTO`
**Errors:** `404` if not found · `400` if not the owner

---

### PUT `/api/v1/decks/{id}`
Updates an existing deck (full replacement of all fields).

**Request body:** Same as POST.

**Response `200 OK`:** Updated `DeckResponseDTO`

---

### DELETE `/api/v1/decks/{id}`
Deletes a deck by ID.

**Response `204 No Content`**

---

#### DeckResponseDTO shape
```json
{
  "id": 1,
  "title": "Historia de España",
  "description": "...",
  "isPublic": false,
  "authorName": "Test Testez",
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
Returns all cards belonging to the specified deck.

**Response `200 OK`:** Array of `CardResponseDTO`

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
