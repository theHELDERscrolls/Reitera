-- ============================================================
-- Reitera — Demo Seed Data
-- ============================================================
-- !!! DEV / DEMO ONLY — DO NOT RUN AGAINST PRODUCTION !!!
-- The demo passwords below are public knowledge; running this
-- script in production would expose a valid account.
-- ============================================================
-- Prerequisites: run init.sql first.
-- Password (BCrypt, strength 10): reitera2026.
-- To re-seed: run truncate.sql first, then re-run this script.
-- ============================================================

BEGIN;

-- 0. IDEMPOTENCY GUARD
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'alumno@reitera.com') THEN
        RAISE EXCEPTION 'Seed data is already present. Run truncate.sql first to re-seed.';
    END IF;
END $$;


-- 1. ROLES
INSERT INTO roles (name) VALUES
    ('STUDENT'),
    ('ADMIN')
ON CONFLICT (name) DO NOTHING;


-- 2. USERS
-- Fixed UUID allows study_progress and review_logs to reference
-- this user by a known value without relying on auto-generated IDs.
INSERT INTO users (
        id, username, email, password,
        first_name, last_name, role_id,
        email_verified, verification_token, verification_token_expires_at
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'alumno',
    'alumno@reitera.com',
    crypt('reitera2026', gen_salt('bf', 10)),
    'Alumno', 'Demo',
    (SELECT id FROM roles WHERE name = 'STUDENT'),
    true, null, null
);


-- 3. CATEGORIES
INSERT INTO categories (name, description) VALUES
    ('Historia de España',  'Tarjetas sobre historia española desde la Reconquista hasta el siglo XX'),
    ('Programación Java',   'Conceptos de programación orientada a objetos, patrones de diseño y Spring'),
    ('Inglés B2',           'Vocabulario avanzado, phrasal verbs y gramática inglesa nivel B2');


-- 4. DECKS
-- Two decks share "Historia de España" to demonstrate category-scoped study.
INSERT INTO decks (title, description, owner_id, author_name, category_id) VALUES
    (
        'La Segunda Guerra Mundial',
        'Repaso de los principales eventos, fechas y personajes de la SGMU',
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'alumno',
        (SELECT id FROM categories WHERE name = 'Historia de España')
    ),
    (
        'Patrones de Diseño GoF',
        'Los 23 patrones clásicos del libro Gang of Four aplicados a Java',
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'alumno',
        (SELECT id FROM categories WHERE name = 'Programación Java')
    ),
    (
        'Phrasal Verbs Esenciales',
        'Los 60 phrasal verbs más usados en inglés cotidiano y profesional',
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'alumno',
        (SELECT id FROM categories WHERE name = 'Inglés B2')
    ),
    (
        'La Guerra Civil Española',
        'Causas, desarrollo y consecuencias del conflicto civil español (1936-1939)',
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'alumno',
        (SELECT id FROM categories WHERE name = 'Historia de España')
    );


-- ============================================================
-- 5. NOTES + CARDS
-- Each note is inserted first via CTE, then its child cards are
-- derived using the same logic the NoteParser would apply.
-- Types: BASIC, BASIC_REVERSE, CLOZE, MULTIPLE_CHOICE.
-- ============================================================


-- ---- Deck 1: La Segunda Guerra Mundial ----

-- [BASIC] note 1
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
        'BASIC',
        $$¿En qué fecha comenzó la Segunda Guerra Mundial?

---

El 1 de septiembre de 1939, con la invasión alemana de Polonia$$,
        'Francia y el Reino Unido declararon la guerra a Alemania dos días después. Inicio oficial del conflicto en Europa.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC', 0,
    '¿En qué fecha comenzó la Segunda Guerra Mundial?',
    '{"answer":"El 1 de septiembre de 1939, con la invasión alemana de Polonia"}'::jsonb
FROM n;

-- [BASIC] note 2
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
        'BASIC',
        $$¿Qué fue el Día D?

---

El desembarco aliado en las playas de Normandía el 6 de junio de 1944$$,
        'Operación Overlord: la mayor operación anfibia de la historia. Abrió el frente occidental contra la Alemania nazi.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC', 0,
    '¿Qué fue el Día D?',
    '{"answer":"El desembarco aliado en las playas de Normandía el 6 de junio de 1944"}'::jsonb
FROM n;

-- [MULTIPLE_CHOICE] note 3
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
        'MULTIPLE_CHOICE',
        $$¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?

- [ ] Berlín
- [ ] Tokio
- [x] Hiroshima
- [ ] Shanghai$$,
        'El 6 de agosto de 1945, EE.UU. lanzó la bomba "Little Boy" sobre Hiroshima. El 9 de agosto, "Fat Man" cayó sobre Nagasaki.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'MULTIPLE_CHOICE', 0,
    '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?',
    '{"options":["Berlín","Tokio","Hiroshima","Shanghai"],"correctIndex":2}'::jsonb
FROM n;

-- [MULTIPLE_CHOICE] note 4
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
        'MULTIPLE_CHOICE',
        $$¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?

- [ ] Neville Chamberlain
- [x] Winston Churchill
- [ ] Clement Attlee
- [ ] Anthony Eden$$,
        'Churchill fue PM desde mayo de 1940 hasta julio de 1945, liderando al Reino Unido durante los momentos más críticos del conflicto.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'MULTIPLE_CHOICE', 0,
    '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?',
    '{"options":["Neville Chamberlain","Winston Churchill","Clement Attlee","Anthony Eden"],"correctIndex":1}'::jsonb
FROM n;

-- [BASIC_REVERSE] note 5 — genera 2 cartas (ordinal 0: concepto→definición, ordinal 1: inverso)
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
        'BASIC_REVERSE',
        $$Operación Barbarroja

---

La invasión alemana de la URSS, lanzada el 22 de junio de 1941

<->$$,
        'Hitler rompió el Pacto Molotov–Ribbentrop sorprendiendo a Stalin. Fue la mayor operación terrestre de la historia y resultó decisiva para el desenlace de la guerra.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC_REVERSE', 0,
    'Operación Barbarroja',
    '{"answer":"La invasión alemana de la URSS, lanzada el 22 de junio de 1941"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'BASIC_REVERSE', 1,
    'La invasión alemana de la URSS, lanzada el 22 de junio de 1941',
    '{"answer":"Operación Barbarroja"}'::jsonb
FROM n;

-- [CLOZE] note 6 — genera 2 cartas (una por índice cloze)
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
        'CLOZE',
        $$La batalla de {{c1::Stalingrado}} duró de agosto de 1942 a febrero de {{c2::1943}}, siendo el mayor punto de inflexión en el frente oriental.$$,
        'La rendición del Sexto Ejército alemán marcó el inicio de la retirada alemana en el este. Más de 800.000 bajas en el Eje.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'CLOZE', 0,
    'La batalla de [...] duró de agosto de 1942 a febrero de 1943, siendo el mayor punto de inflexión en el frente oriental.',
    '{"clozeIndex":1,"answer":"Stalingrado"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'CLOZE', 1,
    'La batalla de Stalingrado duró de agosto de 1942 a febrero de [...], siendo el mayor punto de inflexión en el frente oriental.',
    '{"clozeIndex":2,"answer":"1943"}'::jsonb
FROM n;


-- ---- Deck 2: Patrones de Diseño GoF ----

-- [BASIC] note 1
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
        'BASIC',
        $$¿Qué problema resuelve el patrón Singleton?

---

Garantiza que una clase tenga una única instancia y proporciona un punto de acceso global a ella$$,
        'Se usa para recursos compartidos: pools de conexiones, loggers, configuración global. En Spring, todos los beans son Singleton por defecto.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC', 0,
    '¿Qué problema resuelve el patrón Singleton?',
    '{"answer":"Garantiza que una clase tenga una única instancia y proporciona un punto de acceso global a ella"}'::jsonb
FROM n;

-- [BASIC] note 2
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
        'BASIC',
        $$¿Qué es el patrón Strategy y cuándo se usa?

---

Define una familia de algoritmos intercambiables. Se usa cuando queremos seleccionar el algoritmo a ejecutar en tiempo de ejecución sin cambiar el cliente$$,
        'Ejemplo: un sistema de ordenación que puede usar QuickSort o MergeSort según el contexto. En Spring Security, la autenticación usa este patrón.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC', 0,
    '¿Qué es el patrón Strategy y cuándo se usa?',
    '{"answer":"Define una familia de algoritmos intercambiables. Se usa cuando queremos seleccionar el algoritmo a ejecutar en tiempo de ejecución sin cambiar el cliente"}'::jsonb
FROM n;

-- [MULTIPLE_CHOICE] note 3
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
        'MULTIPLE_CHOICE',
        $$¿A qué categoría pertenece el patrón Observer?

- [ ] Creacional
- [ ] Estructural
- [x] De comportamiento
- [ ] Concurrencia$$,
        'Observer es de comportamiento. Define una dependencia uno-a-muchos: cuando un objeto cambia, todos sus suscriptores son notificados automáticamente.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'MULTIPLE_CHOICE', 0,
    '¿A qué categoría pertenece el patrón Observer?',
    '{"options":["Creacional","Estructural","De comportamiento","Concurrencia"],"correctIndex":2}'::jsonb
FROM n;

-- [BASIC_REVERSE] note 4 — genera 2 cartas
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
        'BASIC_REVERSE',
        $$Patrón Factory Method

---

Define una interfaz para crear objetos, pero deja que las subclases decidan qué clase instanciar

<->$$,
        'Patrón creacional. Desacopla el código que usa el objeto del código que lo crea, respetando el principio Open/Closed.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC_REVERSE', 0,
    'Patrón Factory Method',
    '{"answer":"Define una interfaz para crear objetos, pero deja que las subclases decidan qué clase instanciar"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'BASIC_REVERSE', 1,
    'Define una interfaz para crear objetos, pero deja que las subclases decidan qué clase instanciar',
    '{"answer":"Patrón Factory Method"}'::jsonb
FROM n;

-- [CLOZE] note 5 — genera 2 cartas
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
        'CLOZE',
        $$El patrón {{c1::Decorator}} añade responsabilidades a un objeto dinámicamente, a diferencia de la {{c2::herencia}} que actúa en tiempo de compilación.$$,
        'Ejemplo clásico: los flujos de Java (BufferedReader envuelve FileReader). Se pueden apilar múltiples Decorators para combinar comportamientos.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'CLOZE', 0,
    'El patrón [...] añade responsabilidades a un objeto dinámicamente, a diferencia de la herencia que actúa en tiempo de compilación.',
    '{"clozeIndex":1,"answer":"Decorator"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'CLOZE', 1,
    'El patrón Decorator añade responsabilidades a un objeto dinámicamente, a diferencia de la [...] que actúa en tiempo de compilación.',
    '{"clozeIndex":2,"answer":"herencia"}'::jsonb
FROM n;


-- ---- Deck 3: Phrasal Verbs Esenciales ----

-- [BASIC] note 1
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
        'BASIC',
        $$What does "give up" mean?

---

To stop trying; to abandon an effort or habit$$,
        'Examples: "I gave up smoking last year." / "Don''t give up on your dreams!" Synonyms: to quit, to abandon.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC', 0,
    'What does "give up" mean?',
    '{"answer":"To stop trying; to abandon an effort or habit"}'::jsonb
FROM n;

-- [BASIC] note 2
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
        'BASIC',
        $$What does "put off" mean?

---

To postpone or delay something to a later time$$,
        'Example: "Don''t put off until tomorrow what you can do today." Synonyms: to delay, to defer, to postpone.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC', 0,
    'What does "put off" mean?',
    '{"answer":"To postpone or delay something to a later time"}'::jsonb
FROM n;

-- [MULTIPLE_CHOICE] note 3
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
        'MULTIPLE_CHOICE',
        $$Which sentence uses "run out of" correctly?

- [ ] We ran out of the building.
- [x] We ran out of milk, so I need to go shopping.
- [ ] She ran out of the race early.
- [ ] He ran out of his friend.$$,
        '"Run out of" means to have no more of something left. It requires an object. It is NOT the same as "run out" (to exit a place running).'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'MULTIPLE_CHOICE', 0,
    'Which sentence uses "run out of" correctly?',
    '{"options":["We ran out of the building.","We ran out of milk, so I need to go shopping.","She ran out of the race early.","He ran out of his friend."],"correctIndex":1}'::jsonb
FROM n;

-- [BASIC_REVERSE] note 4 — genera 2 cartas
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
        'BASIC_REVERSE',
        $$break up

---

To end a romantic relationship; or to separate something into smaller parts

<->$$,
        'Examples: "They broke up after two years." / "The company broke up into smaller divisions." Context determines the meaning.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC_REVERSE', 0,
    'break up',
    '{"answer":"To end a romantic relationship; or to separate something into smaller parts"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'BASIC_REVERSE', 1,
    'To end a romantic relationship; or to separate something into smaller parts',
    '{"answer":"break up"}'::jsonb
FROM n;

-- [CLOZE] note 5 — genera 2 cartas
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
        'CLOZE',
        $$To {{c1::give up}} means to stop trying, while to {{c2::carry on}} means to continue doing something.$$,
        'Both are extremely common in everyday English. Antonyms in meaning but often appear together as contrasting options in exam exercises.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'CLOZE', 0,
    'To [...] means to stop trying, while to carry on means to continue doing something.',
    '{"clozeIndex":1,"answer":"give up"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'CLOZE', 1,
    'To give up means to stop trying, while to [...] means to continue doing something.',
    '{"clozeIndex":2,"answer":"carry on"}'::jsonb
FROM n;


-- ---- Deck 4: La Guerra Civil Española ----

-- [BASIC] note 1
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
        'BASIC',
        $$¿Cuándo comenzó la Guerra Civil Española y quiénes fueron los dos bandos principales?

---

Comenzó el 17 de julio de 1936. Los dos bandos fueron el bando republicano (gobierno legítimo) y el bando nacional (sublevados liderados por Franco)$$,
        'El detonante fue el golpe de Estado de un sector del ejército contra la Segunda República. La guerra duró hasta el 1 de abril de 1939.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC', 0,
    '¿Cuándo comenzó la Guerra Civil Española y quiénes fueron los dos bandos principales?',
    '{"answer":"Comenzó el 17 de julio de 1936. Los dos bandos fueron el bando republicano (gobierno legítimo) y el bando nacional (sublevados liderados por Franco)"}'::jsonb
FROM n;

-- [MULTIPLE_CHOICE] note 2
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
        'MULTIPLE_CHOICE',
        $$¿Qué país apoyó militarmente al bando republicano durante la Guerra Civil?

- [ ] Alemania
- [ ] Italia
- [x] La Unión Soviética
- [ ] Portugal$$,
        'La URSS suministró armas y asesores militares. Alemania e Italia apoyaron al bando nacional con la Legión Cóndor y el CTV respectivamente.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'MULTIPLE_CHOICE', 0,
    '¿Qué país apoyó militarmente al bando republicano durante la Guerra Civil?',
    '{"options":["Alemania","Italia","La Unión Soviética","Portugal"],"correctIndex":2}'::jsonb
FROM n;

-- [BASIC_REVERSE] note 3 — genera 2 cartas
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
        'BASIC_REVERSE',
        $$Guernica

---

Ciudad vasca bombardeada por la Legión Cóndor el 26 de abril de 1937, inmortalizada en el cuadro de Picasso

<->$$,
        'Guernica era una ciudad sin valor militar estratégico. El cuadro de Picasso es uno de los símbolos antibelicistas más reconocidos del siglo XX.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'BASIC_REVERSE', 0,
    'Guernica',
    '{"answer":"Ciudad vasca bombardeada por la Legión Cóndor el 26 de abril de 1937, inmortalizada en el cuadro de Picasso"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'BASIC_REVERSE', 1,
    'Ciudad vasca bombardeada por la Legión Cóndor el 26 de abril de 1937, inmortalizada en el cuadro de Picasso',
    '{"answer":"Guernica"}'::jsonb
FROM n;

-- [CLOZE] note 4 — genera 2 cartas
WITH n AS (
    INSERT INTO notes (deck_id, type, content, explanation)
    VALUES (
        (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
        'CLOZE',
        $$La Guerra Civil Española comenzó el {{c1::17 de julio de 1936}} y terminó el {{c2::1 de abril de 1939}} con el parte de victoria de Franco.$$,
        'La guerra duró exactamente 2 años y 8 meses. La firma del parte de victoria es la fecha oficial del fin del conflicto.'
    ) RETURNING id, deck_id
)
INSERT INTO cards (note_id, deck_id, type, ordinal, question, answer_json)
SELECT id, deck_id, 'CLOZE', 0,
    'La Guerra Civil Española comenzó el [...] y terminó el 1 de abril de 1939 con el parte de victoria de Franco.',
    '{"clozeIndex":1,"answer":"17 de julio de 1936"}'::jsonb
FROM n
UNION ALL
SELECT id, deck_id, 'CLOZE', 1,
    'La Guerra Civil Española comenzó el 17 de julio de 1936 y terminó el [...] con el parte de victoria de Franco.',
    '{"clozeIndex":2,"answer":"1 de abril de 1939"}'::jsonb
FROM n;


-- ============================================================
-- 6. STUDY PROGRESS
-- Cards from Decks 1–3 with FSRS state. States: 1=Learning,
-- 2=Review, 3=Relearning. Cards with next_review in the past
-- appear immediately in GET /study/due.
--
-- Card lookups join through notes→decks so the query is scoped
-- by deck title. This prevents "more than one row" errors if the
-- same question text ever appears in two different decks.
-- ============================================================

-- Helper: returns the card ID for a given (deck title, question) pair.
-- Used in study_progress and review_logs to avoid ambiguous lookups.
CREATE OR REPLACE FUNCTION seed_card_id(p_deck_title TEXT, p_question TEXT)
RETURNS INTEGER LANGUAGE sql AS $$
    SELECT c.id
    FROM cards c
    JOIN notes n ON c.note_id = n.id
    JOIN decks d ON n.deck_id = d.id
    WHERE d.title = p_deck_title
      AND c.question = p_question
    LIMIT 1;
$$;

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('La Segunda Guerra Mundial', '¿En qué fecha comenzó la Segunda Guerra Mundial?'),
    21.5, 4.2, 7, 21, 3, 0, 2, NOW() - INTERVAL '7 days', NOW() + INTERVAL '14 days');  -- Review, not due

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('La Segunda Guerra Mundial', '¿Qué fue el Día D?'),
    10.3, 5.1, 10, 10, 2, 0, 2, NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 minute');  -- Review, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('La Segunda Guerra Mundial', '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'),
    2.1, 7.4, 3, 3, 4, 1, 3, NOW() - INTERVAL '3 days', NOW() - INTERVAL '30 minutes');  -- Relearning, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('La Segunda Guerra Mundial', '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'),
    1.8, 6.9, 5, 5, 3, 2, 3, NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 hour');  -- Relearning, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('Patrones de Diseño GoF', '¿Qué problema resuelve el patrón Singleton?'),
    15.2, 3.8, 5, 15, 2, 0, 2, NOW() - INTERVAL '5 days', NOW() + INTERVAL '10 days');  -- Review, not due

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('Patrones de Diseño GoF', '¿Qué es el patrón Strategy y cuándo se usa?'),
    0.8, 4.5, 0, 0, 1, 0, 1, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '10 minutes');  -- Learning, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('Phrasal Verbs Esenciales', 'What does "give up" mean?'),
    8.7, 4.1, 8, 8, 2, 0, 2, NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 hours');  -- Review, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    seed_card_id('Phrasal Verbs Esenciales', 'What does "put off" mean?'),
    1.2, 5.3, 0, 0, 1, 0, 1, NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '15 minutes');  -- Learning, DUE


-- 7. REVIEW LOGS (ratings: 1=Again, 2=Hard, 3=Good, 4=Easy)

INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿En qué fecha comenzó la Segunda Guerra Mundial?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿En qué fecha comenzó la Segunda Guerra Mundial?'), 3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿En qué fecha comenzó la Segunda Guerra Mundial?'), 4, 7, 7),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Qué fue el Día D?'), 2, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Qué fue el Día D?'), 3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 4, 3, 3),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 1, 3, 3),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'), 1, 5, 5),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('La Segunda Guerra Mundial',   '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'), 1, 5, 5),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('Patrones de Diseño GoF',      '¿Qué problema resuelve el patrón Singleton?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('Patrones de Diseño GoF',      '¿Qué problema resuelve el patrón Singleton?'), 4, 5, 5),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('Patrones de Diseño GoF',      '¿Qué es el patrón Strategy y cuándo se usa?'), 2, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('Phrasal Verbs Esenciales',    'What does "give up" mean?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('Phrasal Verbs Esenciales',    'What does "give up" mean?'), 4, 8, 8),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', seed_card_id('Phrasal Verbs Esenciales',    'What does "put off" mean?'), 3, 0, 0);

DROP FUNCTION seed_card_id(TEXT, TEXT);


COMMIT;
