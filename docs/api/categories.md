# Category Endpoints

Categories are a shared global table — there is no dedicated creation endpoint. They are created automatically via the find-or-create logic in `POST /api/v1/decks` and `PUT /api/v1/decks/{id}` when a `categoryName` is supplied. They are deleted automatically when no deck references them anymore (orphan cleanup).

---

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
