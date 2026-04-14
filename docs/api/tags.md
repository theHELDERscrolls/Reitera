# Tag Endpoints

Tags are user-scoped — a tag belongs to exactly one user; two users can share the same name independently. Tags have no dedicated creation endpoint: they are created inline during card save via the `newTags` field in `CardRequestDTO`. Tags with no remaining cards are automatically deleted by `CardService` after every card update or delete (orphan cleanup).

---

### GET `/api/v1/tags`
Returns all tags owned by the authenticated user.

**Response `200 OK`:**
```json
[
  { "id": 1, "name": "importante", "hexColor": "#E74C3C" },
  { "id": 2, "name": "difícil",    "hexColor": "#E67E22" }
]
```

Because orphan cleanup deletes unused tags automatically, this list in practice only contains tags that are assigned to at least one card.

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
