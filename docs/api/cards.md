# Card Endpoints

Cards are nested under their parent deck for create/update/delete operations. A separate cross-deck endpoint allows querying all cards across all decks owned by the authenticated user.

### GET `/api/v1/cards`

Returns a paginated list of **all cards** owned by the current user, across all their decks. All filter parameters are optional — omitting them returns everything.

| Query param | Type    | Default        | Description                                                                                       |
| ----------- | ------- | -------------- | ------------------------------------------------------------------------------------------------- |
| `question`  | String  | —              | Partial case-insensitive match on the question text                                               |
| `type`      | String  | —              | Exact card type: `BASIC`, `MULTIPLE_CHOICE`, or `TRUE_FALSE`                                      |
| `state`     | Integer | —              | FSRS study state: `-1` = never studied, `0` = New, `1` = Learning, `2` = Review, `3` = Relearning |
| `tagId`     | Integer | —              | ID of a tag the card must have assigned                                                           |
| `page`      | Integer | `0`            | Zero-based page number                                                                            |
| `size`      | Integer | `20`           | Items per page                                                                                    |
| `sort`      | String  | `question,asc` | Field and direction (`question,asc` \| `type,desc` \| …)                                          |

Filters are composed dynamically — only non-null params are applied, so any combination works.

Cards with no `StudyProgress` row (never studied) return `state: null`, same as the deck-scoped list. The `-1` value is used only as a **filter query param** to request that subset — it is never present in the response. States are enriched in a single batch query (N+1 free).

**Response `200 OK`:** `Page<CardResponseDTO>`

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
  "tagIds": [1],
  "newTags": [{ "name": "siglo XX", "hexColor": "#3b82f6" }]
}
```

**MULTIPLE_CHOICE** — one correct option among several (min 2, max 8 options):

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

**TRUE_FALSE** — true or false statement:

```json
{
  "type": "TRUE_FALSE",
  "question": "La Guerra Civil Española comenzó en 1936.",
  "answerJson": { "correct": true },
  "explanation": "El conflicto se inició el 17 de julio de 1936.",
  "tagIds": []
}
```

Tags can be attached in two ways (both optional):

- `tagIds` — IDs of existing tags the user owns.
- `newTags` — name + hexColor pairs for inline tag creation; if a tag with that name already exists for the user it is reused (find-or-create); otherwise a new tag row is created and attached.

**Response `201 Created`:** `CardResponseDTO`

### GET `/api/v1/decks/{deckId}/cards`

Returns a paginated list of cards belonging to the specified deck.

| Query param | Type    | Default | Description                               |
| ----------- | ------- | ------- | ----------------------------------------- |
| `page`      | Integer | `0`     | Zero-based page number                    |
| `size`      | Integer | `20`    | Items per page                            |
| `sort`      | String  | —       | Field and direction (e.g. `question,asc`) |

**Response `200 OK`:** `Page<CardResponseDTO>`

```json
{
  "content": ["...cards..."],
  "totalElements": 120,
  "totalPages": 6,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

### GET `/api/v1/decks/{deckId}/cards/{cardId}`

Returns a single card by ID.

**Response `200 OK`:** `CardResponseDTO`

**Errors:** `404` if not found or card does not belong to the declared deck.

### PUT `/api/v1/decks/{deckId}/cards/{cardId}`

Updates an existing card (full replacement of all fields).

**Request body:** Same as POST.

**Response `200 OK`:** Updated `CardResponseDTO`

### DELETE `/api/v1/decks/{deckId}/cards/{cardId}`

Deletes a card by ID. Tags that were assigned exclusively to this card are automatically deleted (orphan cleanup).

**Response `204 No Content`**

### CardResponseDTO shape

```json
{
  "id": 1,
  "deckId": 1,
  "type": "BASIC",
  "question": "¿En qué año comenzó la Guerra Civil Española?",
  "answerJson": { "answer": "1936" },
  "explanation": "El conflicto se inició el 17 de julio de 1936.",
  "tags": [{ "id": 1, "name": "historia", "hexColor": "#FF5733" }],
  "state": null
}
```

`state` — the requesting user's FSRS state for this card:

- `null` = never studied (no `StudyProgress` row; only appears in deck-scoped card lists)
- `0` = New
- `1` = Learning
- `2` = Review
- `3` = Relearning

Always `null` for single-card lookups (GET by ID, create, update). Populated for paginated list responses (`GET /decks/{id}/cards` and `GET /cards`).
