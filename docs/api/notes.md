# Note Endpoints

Notes are the source content from which cards are generated automatically. Each note belongs to a deck and contains Markdown-formatted content. When a note is created or updated, the backend parses its content and generates the corresponding cards via `NoteParser`.

All note endpoints are nested under their parent deck.

---

## Note types and their Markdown syntax

| Type | Detection rule | Cards generated |
|---|---|---|
| `BASIC` | Contains a `---` separator line | 1 card (front / back) |
| `BASIC_REVERSE` | Contains both `---` and `<->` lines | 2 cards (forward + reverse) |
| `CLOZE` | Content matches `{{c1::…}}` pattern | One card per cloze index |
| `MULTIPLE_CHOICE` | Contains `- [x]` (correct) and `- [ ]` (wrong) items | 1 card |

**BASIC example:**
```
What year did the Spanish Civil War begin?

---

1936
```

**BASIC_REVERSE example:**
```
Spanish Civil War start year

---

1936

<->
```

**CLOZE example:**
```
The Spanish Civil War began in {{c1::1936}} and ended in {{c2::1939}}.
```
Generates two cards: one hiding `1936`, one hiding `1939`.

**MULTIPLE_CHOICE example:**
```
Which side won the Spanish Civil War?

- [x] Bando Nacional
- [ ] Bando Republicano
- [ ] Neither side
```

An optional explanation block can be appended after a `===` separator line and applies to all generated cards:

```
Front content

---

Back content

===

This is the explanation shown after revealing the answer.
```

---

### POST `/api/v1/decks/{deckId}/notes`

Creates a new note inside the specified deck, validates the content format, and auto-generates its child cards.

**Request body:**

```json
{
  "content": "What year did the Spanish Civil War begin?\n\n---\n\n1936",
  "explanation": "The conflict began on 17 July 1936."
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `content` | String | yes | 1–10 000 chars; must match one of the four supported formats |
| `explanation` | String | no | max 2 000 chars; appended after `===` separator in the editor |

**Response `201 Created`:** `NoteResponseDTO`

**Errors:** `400` if content is blank, exceeds limits, does not match any known format, or mixes markers from multiple types.

---

### GET `/api/v1/decks/{deckId}/notes`

Returns a paginated list of notes belonging to the specified deck, sorted by `createdAt DESC`.

| Query param | Type | Default | Description |
|---|---|---|---|
| `page` | Integer | `0` | Zero-based page number |
| `size` | Integer | `20` | Items per page |

**Response `200 OK`:** `Page<NoteResponseDTO>`

---

### GET `/api/v1/decks/{deckId}/notes/{noteId}`

Returns a single note by ID.

**Response `200 OK`:** `NoteResponseDTO`

**Errors:** `404` if not found or note does not belong to the declared deck.

---

### PUT `/api/v1/decks/{deckId}/notes/{noteId}`

Updates an existing note (full content replacement). Uses a **smart merge strategy** to preserve FSRS progress when possible:

- If the note type did **not** change, existing cards are updated in-place by key (`clozeIndex` for CLOZE, `ordinal` for all others) — `StudyProgress` rows survive.
- If the note type **changed**, all child cards are replaced and FSRS state is reset (the card semantics are fundamentally different).

**Request body:** Same as POST.

**Response `200 OK`:** Updated `NoteResponseDTO`

---

### DELETE `/api/v1/decks/{deckId}/notes/{noteId}`

Deletes a note and all its generated cards (`ON DELETE CASCADE`).

**Response `204 No Content`**

---

### NoteResponseDTO shape

```json
{
  "id": 1,
  "deckId": 3,
  "type": "BASIC",
  "content": "What year did the Spanish Civil War begin?\n\n---\n\n1936",
  "explanation": "The conflict began on 17 July 1936.",
  "cardCount": 1,
  "createdAt": "2026-05-28T10:00:00Z"
}
```

| Field | Description |
|---|---|
| `type` | Detected note type: `BASIC`, `BASIC_REVERSE`, `CLOZE`, or `MULTIPLE_CHOICE` |
| `content` | Raw Markdown content as stored (no explanation block) |
| `explanation` | Optional explanation text, or `null` |
| `cardCount` | Number of child cards currently generated from this note |
| `createdAt` | ISO-8601 timestamp (UTC offset) |
