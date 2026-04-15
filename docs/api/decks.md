# Deck Endpoints

All deck endpoints are owner-scoped: users can only access their own decks.

---

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
| `categoryId` | Integer | — | Optional. When provided, only decks belonging to that category are returned. Pagination applies after filtering. |

**Response `200 OK`:** `Page<DeckResponseDTO>`
```json
{
  "content": [ "...decks..." ],
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

**Errors:** `404` if not found or not owned by the requesting user.

---

### GET `/api/v1/decks/{id}/stats`
Returns aggregated card counts for a deck, broken down by FSRS state.

**Response `200 OK`:**
```json
{
  "totalCards": 21,
  "newCards": 10,
  "learningCards": 3,
  "reviewCards": 5,
  "relearningCards": 1,
  "dueCards": 4
}
```

- `newCards` — cards with no `StudyProgress` row for the requesting user.
- `dueCards` — cards whose `nextReview ≤ now` (overdue).
- All counts are computed from `StudyProgress` per user — two users studying the same deck have fully independent counts.

**Errors:** `404` if deck not found or not owned by the requesting user.

---

### PUT `/api/v1/decks/{id}`
Updates an existing deck (full replacement of all fields).

**Request body:** Same as POST. The same `categoryId` / `categoryName` find-or-create logic applies. If the category changes, the old category is automatically deleted if no other deck references it (orphan cleanup).

**Response `200 OK`:** Updated `DeckResponseDTO`

---

### DELETE `/api/v1/decks/{id}`
Deletes a deck by ID. If the deck had a category and no other deck references it after deletion, the category row is automatically deleted.

**Response `204 No Content`**

---

### DeckResponseDTO shape
```json
{
  "id": 1,
  "title": "Historia de España",
  "description": "...",
  "isPublic": false,
  "authorName": "alumno",
  "categoryId": 1,
  "categoryName": "Historia de España",
  "createdAt": "2026-03-18T10:00:00",
  "updatedAt": "2026-03-18T10:00:00"
}
```
