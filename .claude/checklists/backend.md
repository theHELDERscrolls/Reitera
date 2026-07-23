# Backend review checklist

- [ ] New endpoints have a corresponding test in `*Test.java`
- [ ] No endpoint bypasses `findOwnedDeck(id, owner)` or `findOwnedCard(id, owner)` (IDOR prevention)
- [ ] `init.sql` updated if schema changed (never Hibernate DDL auto)
- [ ] Custom exceptions used — no raw `RuntimeException` reaching the client
- [ ] `@Transactional` on operations touching category or tag lifecycle
- [ ] New module added to the module structure in `docs/architecture/backend.md`
- [ ] New endpoint documented in the corresponding `docs/api/<feature>.md`
- [ ] New endpoint listed in `docs/api/README.md` endpoint index
- [ ] New file in `docs/setup/` or `docs/core-logic/` listed in the corresponding `README.md` table
- [ ] HTTP status codes match what `docs/api/` describes
- [ ] `docs/CHANGELOG.md` updated if this closes a feature issue
