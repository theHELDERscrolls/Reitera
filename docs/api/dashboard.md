# Dashboard Endpoints

Analytics endpoints that power the dashboard page. All endpoints require a valid JWT token.

---

### GET `/api/v1/dashboard/stats`

Returns the three summary counters shown in the dashboard stat badges.

**Response `200 OK`:** `DashboardStatsDTO`

```json
{
  "streak": 7,
  "totalDueToday": 42,
  "studiedToday": 18
}
```

| Field | Type | Description |
|---|---|---|
| `streak` | Integer | Consecutive days with ≥ 1 review ending today or yesterday. `0` if the user has never studied or last reviewed more than a day ago |
| `totalDueToday` | Long | Overdue progress-tracked cards + new cards (no `StudyProgress` row) across all user-owned decks |
| `studiedToday` | Long | Distinct cards reviewed today (from `review_logs`) |

---

### GET `/api/v1/dashboard/heatmap`

Returns per-day card review counts for the past 365 days. Only days with at least one review are included — absent dates represent zero activity and should be treated as such by the client.

**Response `200 OK`:** Array of `DailyStudyCountDTO`, ordered by date ascending

```json
[
  { "date": "2026-01-14", "count": 12 },
  { "date": "2026-01-15", "count": 5 }
]
```

| Field | Type | Description |
|---|---|---|
| `date` | String | Calendar date in `YYYY-MM-DD` format |
| `count` | Long | Number of distinct cards reviewed on that date |

---

### GET `/api/v1/dashboard/last-studied`

Returns the most recently studied decks with FSRS state breakdowns and a Study CTA count.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `limit` | Integer | `5` | Maximum number of decks to return |

**Response `200 OK`:** Array of `LastStudiedDeckDTO`, ordered by most recent review descending

```json
[
  {
    "deckId": 2,
    "name": "Historia de España",
    "category": "Historia",
    "newCount": 8,
    "dueCount": 3,
    "relearningCount": 1,
    "lastStudied": "2026-04-23T10:42:00"
  }
]
```

| Field | Type | Description |
|---|---|---|
| `deckId` | Integer | Deck primary key |
| `name` | String | Deck title |
| `category` | String \| null | Category name, or `null` if uncategorised |
| `newCount` | Long | Cards the user has never reviewed in this deck |
| `dueCount` | Long | Due learning/review cards (FSRS states 1 and 2, `nextReview ≤ now`) |
| `relearningCount` | Long | Due relearning cards (FSRS state 3, `nextReview ≤ now`) |
| `lastStudied` | String | ISO-8601 timestamp of the most recent review in this deck |
