# Study Endpoints

Core study endpoints powered by the FSRS-6 spaced repetition algorithm. All endpoints require a valid JWT token.

### GET `/api/v1/study/due`

Returns all cards the user should study now. Provide **exactly one** query parameter:

| Parameter    | Type    | Description                   |
| ------------ | ------- | ----------------------------- |
| `deckId`     | Integer | Study a single deck           |
| `categoryId` | Integer | Study all decks in a category |

The response merges two groups:

1. **Overdue cards** — previously studied cards whose `nextReview` is in the past
2. **New cards** — cards with no study history for this user

Overdue cards appear first so the user revisits pending material before tackling new content.

**Response `200 OK`:** Array of `DueCardDTO`

```
GET /api/v1/study/due?deckId=1
GET /api/v1/study/due?categoryId=2
```

**Errors:** `400` if both or neither scope parameter is provided.

#### DueCardDTO shape

```json
{
  "id": 1,
  "deckId": 1,
  "type": "BASIC",
  "question": "¿En qué año comenzó la Guerra Civil Española?",
  "answerJson": { "answer": "1936" },
  "explanation": "El conflicto se inició el 17 de julio de 1936.",
  "tags": [{ "id": 1, "name": "historia", "hexColor": "#FF5733" }],
  "state": 0
}
```

`state` values: `0` = New · `1` = Learning · `2` = Review · `3` = Relearning

### POST `/api/v1/study/sessions`

Processes a completed study session. Runs the FSRS-6 algorithm for each rated card, updates `StudyProgress`, and writes an immutable `ReviewLog` entry. The entire batch runs in a single transaction — if any card fails, the whole session is rolled back.

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

For category sessions, replace `deckId` with `categoryId`:

```json
{
  "deckId": null,
  "categoryId": 2,
  "ratings": [{ "cardId": 5, "rating": 3 }]
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

**Errors:** `404` if deck/category/card not found · `400` if both or neither scope provided · `400` if a card does not belong to the declared scope.
