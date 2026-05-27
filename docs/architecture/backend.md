# Backend Architecture

## Layer Architecture

The backend follows a standard **3-tier N-layer architecture**:

```
HTTP Request
     │
     ▼
┌─────────────┐
│  Controller │  Receives HTTP requests, delegates to service, returns ResponseEntity
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Service   │  Business logic, ownership checks, DTO ↔ Entity mapping
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │  Spring Data JPA — translates method names into SQL queries
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  PostgreSQL │  Persistent storage (Docker in local, managed service in production)
└─────────────┘
```

Data flows **inward** as DTOs and is **mapped to entities** only at the service layer. Entities never leave the service layer — controllers always deal with DTOs.

## Module Structure

The backend is organized by **feature module**, not by technical layer:

```
modules/
├── auth/
│   ├── controller/ → AuthController: /register, /login, /refresh, /logout, /verify, /resend-verification, /forgot-password, /reset-password
│   ├── filter/     → JwtAuthenticationFilter (intercepts every request)
│   ├── model/      → RefreshToken entity
│   ├── repository/ → RefreshTokenRepository
│   └── service/    → JwtService, RefreshTokenService
├── user/           → User & Role entities, registration, authentication logic, profile endpoint (UserController)
├── deck/           → Deck, Category entities + CRUD, categories and deck stats APIs
├── card/           → Card entity + CRUD API (nested under decks) + cross-deck search with dynamic JPA Specifications
├── study/          → StudyProgress, ReviewLog entities + FSRS-6 algorithm + study session API
└── dashboard/      → DashboardController, DashboardService — stats, heatmap and last-studied endpoints

core/
├── email/          → EmailService (Resend SDK wrapper), EmailVerificationService (token generation, SHA-256 hashing, expiry, verification), PasswordResetService (forgot-password token generation + reset)
└── exception/      → GlobalExceptionHandler, ResourceNotFoundException, DataConflictException, InvalidRefreshTokenException, TokenExpiredException (→ 410 Gone)
```

## Database Design

- **Schema management:** Manual SQL scripts (`ddl-auto: none`). No Flyway/Liquibase yet.
- **Primary keys:** UUID for `users`, auto-increment Integer for all other entities.
- **Flexible card answers:** `answer_json` is stored as native PostgreSQL `JSONB`, mapped via Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` to a `Map<String, Object>`. This allows different card types (BASIC, MULTIPLE_CHOICE, TRUE_FALSE) to use different answer structures without schema changes.
- **Composite keys:** `StudyProgress` uses a composite PK of `(user_id, card_id)` — one progress record per user per card.
- **Refresh tokens:** the raw UUID token is never stored. Only its SHA-256 hex digest (`token_hash VARCHAR(64)`) is persisted. `ON DELETE CASCADE` on `user_id` ensures cleanup on user deletion. The `revoked` flag preserves the audit trail without physically deleting rows.

## Study Session Flow

Study sessions follow a **batch architecture** — the backend is only hit twice per session:

1. Frontend fetches due cards: `GET /api/v1/study/due?deckId={id}` (or `?categoryId={id}`)
   - Returns overdue cards (previously studied, `nextReview ≤ now`) + new cards (no prior progress)
   - Cards carry their FSRS `state` field for client-side processing
2. User studies the session locally — ratings held in memory (or `localStorage` for resilience)
3. On session completion, a **single batch request** submits all ratings: `POST /api/v1/study/sessions`
4. Backend runs FSRS-6 for each card in one `@Transactional` block and persists `StudyProgress` + `ReviewLog`

**Scope modes:** both endpoints accept either `deckId` (single deck) or `categoryId` (all decks in a category), enabling users to study individual topics or full subjects at once.

**Interval precision:** intervals are stored and applied at minute precision. Low-stability cards (e.g. Forgotten on a new card) receive sub-day intervals (~5 hours) rather than being forced to the next day.

## Testing Strategy

Tests live in `src/test/java/` mirroring the production package structure. Two test types are used — no integration tests exist yet.

### Unit tests (service layer)

```
@ExtendWith(MockitoExtension.class)
class XServiceTest {
    // instance fields (test data)
    @Mock     XRepository xRepository;
    @InjectMocks XService xService;

    @BeforeEach void setUp() { ... }
    @Test void methodName_condition_expectedOutcome() { ... }
}
```

- Framework: JUnit 5 + Mockito + AssertJ
- `@Mock` injects fakes for every repository/service dependency
- `@InjectMocks` instantiates the class under test with those fakes injected
- `@Value`-injected fields set via `ReflectionTestUtils.setField()` in `@BeforeEach`
- Assertions use AssertJ (`assertThat`, `assertThatThrownBy`)
- Side-effect verification uses `verify(repo).method(...)` / `verify(repo, never()).method(...)`

### Controller slice tests (web layer)

```
@WebMvcTest(XController.class)
class XControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean XService xService;
    @MockitoBean JwtService jwtService;   // always required — JwtAuthenticationFilter depends on it

    @Test void endpoint_returns200_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/...").with(user(mockUser)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.field").value(...));
    }
}
```

- Framework: `@WebMvcTest` (Spring MVC slice) + `SecurityMockMvcRequestPostProcessors`
- `@MockitoBean JwtService` is required in every controller test because `JwtAuthenticationFilter` is a `@Component` filter loaded by the slice and depends on `JwtService`
- `.with(user(mockUser))` injects authentication directly into the `SecurityContext`, bypassing the JWT filter entirely — used for all protected endpoints
- POST/PUT/DELETE requests include `.with(csrf())` for CSRF compatibility
- `AuthControllerTest` is the only exception: it adds `@Import(SecurityConfig.class)` + `@MockitoBean AuthenticationProvider` + `@MockitoBean UserDetailsService` to activate the `permitAll()` rules for public auth endpoints

### Coverage

| Module | Unit tests | Slice tests |
|--------|-----------|-------------|
| auth | `JwtServiceTest` (4), `RefreshTokenServiceTest` (7), `PasswordResetServiceTest` (5) | `AuthControllerTest` (17) |
| user | `UserServiceTest` (9) | `UserControllerTest` (2) |
| deck | `DeckServiceTest` (20), `CategoryServiceTest` (1) | `DeckControllerTest` (16), `CategoryControllerTest` (2) |
| card | `CardServiceTest` (7) | `CardControllerTest` (14), `CardListControllerTest` (3) |
| study | `FsrsServiceTest` (3), `StudyServiceTest` (4) | `StudyControllerTest` (5) |
| dashboard | `DashboardServiceTest` (4) | `DashboardControllerTest` (4) |

**Total: ~120 tests** across 17 test classes. Run with `mvn test` from `backend/reitera-backend/`.
