# Card Endpoints

Cards are generated automatically from **notes** — they are not created or edited directly. To add or modify cards, create or update a note via `POST/PUT /api/v1/decks/{deckId}/notes` (see [notes.md](notes.md)).

The only direct card endpoint is the cross-deck listing below.

---

### GET `/api/v1/cards`

Returns a paginated list of **all cards** owned by the current user, across all their decks. All filter parameters are optional — omitting them returns everything.

| Query param | Type    | Default        | Description                                                                                        |
| ----------- | ------- | -------------- | -------------------------------------------------------------------------------------------------- |
| `question`  | String  | —              | Partial case-insensitive match on the question text                                                |
| `type`      | String  | —              | Exact card type: `BASIC`, `BASIC_REVERSE`, `CLOZE`, or `MULTIPLE_CHOICE`                          |
| `state`     | Integer | —              | FSRS study state: `-1` = never studied, `0` = New, `1` = Learning, `2` = Review, `3` = Relearning |
| `page`      | Integer | `0`            | Zero-based page number                                                                             |
| `size`      | Integer | `20`           | Items per page                                                                                     |
| `sort`      | String  | `question,asc` | Field and direction (`question,asc` \| `type,desc` \| …)                                           |

Filters are composed dynamically — only non-null params are applied, so any combination works.

Cards with no `StudyProgress` row (never studied) return `state: null`. The `-1` value is used only as a **filter query param** to request that subset — it is never present in the response. States are enriched in a single batch query (N+1 free).

**Response `200 OK`:** `Page<CardResponseDTO>`

---

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

---

### CardResponseDTO shape

```json
{
  "id": 1,
  "deckId": 3,
  "noteId": 7,
  "type": "BASIC",
  "question": "What year did the Spanish Civil War begin?",
  "answerJson": { "answer": "1936" },
  "state": null
}
```

| Field | Description |
|---|---|
| `noteId` | ID of the parent note that generated this card |
| `type` | Card type: `BASIC`, `BASIC_REVERSE`, `CLOZE`, or `MULTIPLE_CHOICE` |
| `answerJson` | Type-specific JSON answer structure (see below) |
| `state` | Requesting user's FSRS state — `null` if never studied, `0` New, `1` Learning, `2` Review, `3` Relearning |

`state` is always `null` for single-card lookups (GET by ID). Populated for paginated list responses.

#### `answerJson` structure by type

**BASIC / BASIC_REVERSE:**
```json
{ "answer": "1936" }
```

**CLOZE:**
```json
{ "clozeIndex": 1, "answer": "1936" }
```
Each cloze deletion becomes a separate card with its own `clozeIndex`.

**MULTIPLE_CHOICE:**
```json
{ "options": ["Bando Nacional", "Bando Republicano", "Neither"], "correctIndex": 0 }
```
