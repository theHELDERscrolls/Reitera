-- ============================================================
-- Reitera — Demo Seed Data
-- ============================================================
-- Populates the database with realistic demo data for local
-- development and TFG demonstrations.
--
-- Prerequisites:
--   - init.sql must have been executed first.
--   - pgcrypto extension must be enabled (init.sql does this).
--
-- PASSWORDS (BCrypt strength 10, generated via pgcrypto):
--   alumno@reitera.com -> "reitera2026"
--   admin@reitera.com  -> "reitera2026"
--
-- IDEMPOTENCY:
--   This script raises an error if seed data is already present.
--   To re-seed from scratch, run truncate.sql first, then re-run this script.
-- ============================================================

BEGIN;

-- ============================================================
-- 0. IDEMPOTENCY GUARD
-- Stops the script immediately if seed data already exists,
-- preventing duplicate entries on repeated executions.
-- ============================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'alumno@reitera.com') THEN
        RAISE EXCEPTION
            'Seed data is already present. To re-seed, delete demo data manually (see header of this file).';
    END IF;
END $$;


-- ============================================================
-- 1. ROLES
-- Two roles: USER (regular student) and ADMIN (moderator).
-- No explicit IDs — we let the SERIAL generate them to avoid
-- PK conflicts with roles that may already exist in the DB.
-- ON CONFLICT (name) skips the insert if the role already exists.
-- ============================================================
INSERT INTO roles (name) VALUES
    ('USER'),
    ('ADMIN')
ON CONFLICT (name) DO NOTHING;


-- ============================================================
-- 2. USERS
-- Fixed UUIDs are intentional: they allow study_progress and
-- review_logs to reference users by a known, stable value.
--
-- crypt('reitera2026', gen_salt('bf', 10)) generates a valid
-- BCrypt hash at runtime, compatible with Spring Security's
-- BCryptPasswordEncoder.matches() method.
-- ============================================================
INSERT INTO users (id, username, email, password, first_name, last_name, role_id) VALUES
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'alumno',
        'alumno@reitera.com',
        crypt('reitera2026', gen_salt('bf', 10)),
        'Alumno',
        'Demo',
        (SELECT id FROM roles WHERE name = 'USER')
    ),
    (
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'admin',
        'admin@reitera.com',
        crypt('reitera2026', gen_salt('bf', 10)),
        'Admin',
        'Reitera',
        (SELECT id FROM roles WHERE name = 'ADMIN')
    );


-- ============================================================
-- 3. CATEGORIES
-- Three subject areas to demonstrate category-scoped study
-- (studying all decks under "Historia de España" at once).
-- ============================================================
INSERT INTO categories (name, description) VALUES
    (
        'Historia de España',
        'Tarjetas sobre historia española desde la Reconquista hasta el siglo XX'
    ),
    (
        'Programación Java',
        'Conceptos de programación orientada a objetos, patrones de diseño y Spring'
    ),
    (
        'Inglés B2',
        'Vocabulario avanzado, phrasal verbs y gramática inglesa nivel B2'
    );


-- ============================================================
-- 4. TAGS
-- Cross-deck labels. ON CONFLICT handles pre-existing tags.
-- ============================================================
INSERT INTO tags (name, hex_color) VALUES
    ('importante', '#E74C3C'),
    ('difícil',    '#E67E22'),
    ('repaso',     '#3498DB'),
    ('vocabulario','#27AE60')
ON CONFLICT (name) DO NOTHING;


-- ============================================================
-- 5. DECKS
-- Four decks owned by 'alumno'. Deliberately two decks share
-- the same category ("Historia de España") to demonstrate
-- category-scoped study: GET /study/due?categoryId=X returns
-- cards from both decks at once.
--
--   Historia de España → Deck 1 (SGMU)  + Deck 4 (GCE)
--   Programación Java  → Deck 2 (GoF)
--   Inglés B2          → Deck 3 (Phrasal Verbs)
--
-- The admin user has no decks — admin is a moderation role,
-- not a student role.
-- ============================================================
INSERT INTO decks (title, description, is_public, owner_id, author_name, category_id) VALUES
    (
        'La Segunda Guerra Mundial',
        'Repaso de los principales eventos, fechas y personajes de la SGMU',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'Alumno Demo',
        (SELECT id FROM categories WHERE name = 'Historia de España')
    ),
    (
        'Patrones de Diseño GoF',
        'Los 23 patrones clásicos del libro Gang of Four aplicados a Java',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'Alumno Demo',
        (SELECT id FROM categories WHERE name = 'Programación Java')
    ),
    (
        'Phrasal Verbs Esenciales',
        'Los 60 phrasal verbs más usados en inglés cotidiano y profesional',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'Alumno Demo',
        (SELECT id FROM categories WHERE name = 'Inglés B2')
    ),
    (
        'La Guerra Civil Española',
        'Causas, desarrollo y consecuencias del conflicto civil español (1936-1939)',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'Alumno Demo',
        (SELECT id FROM categories WHERE name = 'Historia de España')
    );


-- ============================================================
-- 6. CARDS (16 total — all 4 types covered)
-- deck_id is resolved via subquery to avoid hardcoded IDs.
--
-- JSONB formats by type:
--   BASIC:           {"answer": "texto"}
--   CLOZE:           {"answer": "palabra oculta"}
--   MULTIPLE_CHOICE: {"options": [...], "correctIndex": N}
--   TRUE_FALSE:      {"answer": true|false}
-- ============================================================

-- ---- Deck 1: La Segunda Guerra Mundial (6 cards) ----

-- [BASIC] Inicio de la guerra
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'BASIC',
    '¿En qué fecha comenzó la Segunda Guerra Mundial?',
    '{"answer": "El 1 de septiembre de 1939, con la invasión alemana de Polonia"}',
    'Francia y el Reino Unido declararon la guerra a Alemania dos días después. Es el inicio oficial del conflicto en Europa.'
);

-- [BASIC] El Día D
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'BASIC',
    '¿Qué fue el Día D?',
    '{"answer": "El desembarco aliado en las playas de Normandía el 6 de junio de 1944"}',
    'Operación Overlord: la mayor operación anfibia de la historia. Supuso la apertura del frente occidental contra la Alemania nazi.'
);

-- [TRUE_FALSE] Fin de la guerra
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'TRUE_FALSE',
    'La SGMU finalizó en 1945 con la rendición de Alemania y Japón.',
    '{"answer": true}',
    'Alemania se rindió el 8 de mayo de 1945 (VE Day). Japón se rindió el 2 de septiembre de 1945 (VJ Day) tras los bombardeos de Hiroshima y Nagasaki.'
);

-- [TRUE_FALSE] La URSS en el Eje
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'TRUE_FALSE',
    'La Unión Soviética formó parte del Eje junto con Alemania e Italia.',
    '{"answer": false}',
    'La URSS firmó el Pacto Molotov–Ribbentrop con Alemania, pero se unió a los Aliados tras la Operación Barbarroja en junio de 1941.'
);

-- [MULTIPLE_CHOICE] Bomba atómica
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'MULTIPLE_CHOICE',
    '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?',
    '{"options": ["Berlín", "Tokio", "Hiroshima", "Shanghai"], "correctIndex": 2}',
    'El 6 de agosto de 1945, EE.UU. lanzó la bomba "Little Boy" sobre Hiroshima. El 9 de agosto, "Fat Man" cayó sobre Nagasaki.'
);

-- [MULTIPLE_CHOICE] Primer Ministro británico
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'MULTIPLE_CHOICE',
    '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?',
    '{"options": ["Neville Chamberlain", "Winston Churchill", "Clement Attlee", "Anthony Eden"], "correctIndex": 1}',
    'Churchill fue PM desde mayo de 1940 hasta julio de 1945, liderando al Reino Unido durante los momentos más críticos del conflicto.'
);


-- ---- Deck 2: Patrones de Diseño GoF (5 cards) ----

-- [BASIC] Singleton
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'BASIC',
    '¿Qué problema resuelve el patrón Singleton?',
    '{"answer": "Garantiza que una clase tenga una única instancia y proporciona un punto de acceso global a ella"}',
    'Se usa para recursos compartidos: pools de conexiones, loggers, configuración global. En Spring, todos los beans son Singleton por defecto.'
);

-- [CLOZE] Factory Method
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'CLOZE',
    'El patrón ___ define una interfaz para crear objetos, pero deja que las subclases decidan qué clase instanciar.',
    '{"answer": "Factory Method"}',
    'Factory Method es un patrón creacional. Desacopla el código que usa el objeto del código que lo crea, respetando el principio Open/Closed.'
);

-- [CLOZE] Adapter
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'CLOZE',
    'El patrón ___ convierte la interfaz de una clase en otra que el cliente espera, actuando como intermediario entre incompatibles.',
    '{"answer": "Adapter"}',
    'También llamado Wrapper. Ejemplo clásico: adaptar una librería de terceros a la interfaz de tu aplicación sin modificar su código.'
);

-- [MULTIPLE_CHOICE] Categoría de Observer
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'MULTIPLE_CHOICE',
    '¿A qué categoría pertenece el patrón Observer?',
    '{"options": ["Creacional", "Estructural", "De comportamiento", "Concurrencia"], "correctIndex": 2}',
    'Observer es de comportamiento. Define una dependencia uno-a-muchos: cuando un objeto cambia, todos sus suscriptores son notificados. Base de los sistemas de eventos y RxJS.'
);

-- [BASIC] Strategy
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'BASIC',
    '¿Qué es el patrón Strategy y cuándo se usa?',
    '{"answer": "Define una familia de algoritmos intercambiables. Se usa cuando queremos seleccionar el algoritmo a ejecutar en tiempo de ejecución sin cambiar el cliente"}',
    'Ejemplo: un sistema de ordenación que puede usar QuickSort o MergeSort según el contexto. En Spring Security, la autenticación usa este patrón.'
);


-- ---- Deck 3: Phrasal Verbs Esenciales (5 cards) ----

-- [BASIC] give up
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "give up" mean?',
    '{"answer": "To stop trying; to abandon an effort or habit"}',
    'Examples: "I gave up smoking last year." / "Don''t give up on your dreams!" Synonym: to quit, to abandon.'
);

-- [BASIC] look into
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "look into" mean?',
    '{"answer": "To investigate or examine something carefully"}',
    'Example: "The police are looking into the matter." Synonyms: to investigate, to research, to examine.'
);

-- [BASIC] put off
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "put off" mean?',
    '{"answer": "To postpone or delay something to a later time"}',
    'Example: "Don''t put off until tomorrow what you can do today." Synonyms: to delay, to defer, to postpone.'
);

-- [TRUE_FALSE] turn down
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'TRUE_FALSE',
    '"Turn down" means to increase the volume of something.',
    '{"answer": false}',
    '"Turn down" means to DECREASE volume or to reject an offer. The opposite is "turn up". E.g., "Can you turn down the music, please?"'
);

-- [TRUE_FALSE] carry on
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'TRUE_FALSE',
    '"Carry on" means to continue doing something.',
    '{"answer": true}',
    'Example: "Carry on with your work." Synonyms: to continue, to keep going, to proceed. British English also uses it informally to mean "behave badly".'
);


-- ---- Deck 4: La Guerra Civil Española (5 cards — all NEW, no study_progress) ----
-- These cards have no study_progress records on purpose.
-- Studying by deckId=1 (SGMU) returns only SGMU cards.
-- Studying by categoryId="Historia de España" returns SGMU cards
-- (due/learning/relearning) PLUS these 5 new GCE cards — demonstrating
-- that category scope spans multiple decks under the same category.

-- [BASIC] Inicio de la guerra civil
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'BASIC',
    '¿Cuándo comenzó la Guerra Civil Española y quiénes fueron los dos bandos principales?',
    '{"answer": "Comenzó el 17 de julio de 1936. Los dos bandos fueron el bando republicano (gobierno legítimo) y el bando nacional (sublevados liderados por Franco)"}',
    'El detonante fue el golpe de Estado de un sector del ejército contra la Segunda República. La guerra duró casi tres años, hasta abril de 1939.'
);

-- [TRUE_FALSE] Duración del conflicto
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'TRUE_FALSE',
    'La Guerra Civil Española duró más de cinco años.',
    '{"answer": false}',
    'El conflicto duró exactamente 2 años y 8 meses: desde el 17 de julio de 1936 hasta el 1 de abril de 1939, cuando Franco firmó el parte de victoria.'
);

-- [MULTIPLE_CHOICE] Intervención extranjera
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'MULTIPLE_CHOICE',
    '¿Qué país apoyó militarmente al bando republicano durante la Guerra Civil?',
    '{"options": ["Alemania", "Italia", "La Unión Soviética", "Portugal"], "correctIndex": 2}',
    'La URSS suministró armas, asesores militares y las Brigadas Internacionales apoyaron a la República. Alemania e Italia apoyaron al bando nacional con la Legión Cóndor y el CTV respectivamente.'
);

-- [CLOZE] El bombardeo de Guernica
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'CLOZE',
    'El bombardeo de ___ el 26 de abril de 1937, llevado a cabo por la Legión Cóndor alemana, fue inmortalizado por Picasso en un famoso cuadro.',
    '{"answer": "Guernica"}',
    'Guernica era una ciudad vasca sin valor militar estratégico. Su destrucción fue un ensayo de bombardeo de terror sobre población civil. El cuadro de Picasso es uno de los símbolos antibelicistas más reconocidos del siglo XX.'
);

-- [BASIC] Consecuencias de la guerra
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'BASIC',
    '¿Qué régimen político se instauró en España tras la victoria del bando nacional?',
    '{"answer": "La dictadura franquista, un régimen autoritario liderado por Francisco Franco que se mantuvo hasta su muerte en 1975"}',
    'El franquismo fue un régimen de partido único (Movimiento Nacional) con represión política, censura y aislamiento internacional inicial. Duró 36 años.'
);


-- ============================================================
-- 7. CARD TAGS
-- ON CONFLICT (card_id, tag_id) DO NOTHING = idempotent.
-- We use cross-joins with WHERE filters to attach tags
-- to multiple cards at once without repeating INSERT lines.
-- ============================================================

-- Mark all SGMU history cards as 'importante'
INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id
FROM cards c
CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial')
  AND t.name = 'importante'
ON CONFLICT (card_id, tag_id) DO NOTHING;

-- Mark the two MULTIPLE_CHOICE history cards as 'difícil'
INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id
FROM cards c
CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial')
  AND c.type = 'MULTIPLE_CHOICE'
  AND t.name = 'difícil'
ON CONFLICT (card_id, tag_id) DO NOTHING;

-- Mark all phrasal verb cards as 'vocabulario'
INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id
FROM cards c
CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales')
  AND t.name = 'vocabulario'
ON CONFLICT (card_id, tag_id) DO NOTHING;

-- Mark all GCE history cards as 'importante'
INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id
FROM cards c
CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'La Guerra Civil Española')
  AND t.name = 'importante'
ON CONFLICT (card_id, tag_id) DO NOTHING;

-- Mark the Guernica cloze card as 'difícil'
INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id
FROM cards c
CROSS JOIN tags t
WHERE c.question LIKE '%Guernica%'
  AND t.name = 'difícil'
ON CONFLICT (card_id, tag_id) DO NOTHING;


-- ============================================================
-- 8. STUDY PROGRESS
-- Simulates 'alumno' having already studied all 6 cards of
-- Deck 1 (La Segunda Guerra Mundial).
--
-- State distribution:
--   Cards 1-2: STATE=2 (Review)    — well-established memories
--   Cards 3-4: STATE=1 (Learning)  — first encounters, short intervals
--   Cards 5-6: STATE=3 (Relearning)— forgot a Review card, restarting
--
-- next_review in the past = card appears in GET /study/due.
-- next_review in the future = card is scheduled but not yet due.
-- ============================================================

-- Card 1 (BASIC - inicio guerra): Review, NOT due yet (well-known)
INSERT INTO study_progress (
    user_id, card_id, stability, difficulty,
    elapsed_days, scheduled_days, reps, lapses, state,
    last_review, next_review
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'),
    21.5, 4.2, 7, 21, 3, 0, 2,
    NOW() - INTERVAL '7 days',
    NOW() + INTERVAL '14 days'   -- future: not due
);

-- Card 2 (BASIC - Día D): Review, DUE NOW (interval expired)
INSERT INTO study_progress (
    user_id, card_id, stability, difficulty,
    elapsed_days, scheduled_days, reps, lapses, state,
    last_review, next_review
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Qué fue el Día D?'),
    10.3, 5.1, 10, 10, 2, 0, 2,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '1 minute'  -- past: DUE
);

-- Card 3 (TRUE_FALSE - fin guerra): Learning, DUE NOW
INSERT INTO study_progress (
    user_id, card_id, stability, difficulty,
    elapsed_days, scheduled_days, reps, lapses, state,
    last_review, next_review
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = 'La SGMU finalizó en 1945 con la rendición de Alemania y Japón.'),
    0.9, 5.0, 0, 0, 1, 0, 1,
    NOW() - INTERVAL '20 minutes',
    NOW() - INTERVAL '5 minutes'  -- past: DUE
);

-- Card 4 (TRUE_FALSE - URSS): Learning, DUE NOW
INSERT INTO study_progress (
    user_id, card_id, stability, difficulty,
    elapsed_days, scheduled_days, reps, lapses, state,
    last_review, next_review
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = 'La Unión Soviética formó parte del Eje junto con Alemania e Italia.'),
    0.6, 6.2, 0, 0, 1, 0, 1,
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '2 minutes'  -- past: DUE
);

-- Card 5 (MULTIPLE_CHOICE - bomba): Relearning (lapses=1), DUE NOW
INSERT INTO study_progress (
    user_id, card_id, stability, difficulty,
    elapsed_days, scheduled_days, reps, lapses, state,
    last_review, next_review
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'),
    2.1, 7.4, 3, 3, 4, 1, 3,
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '30 minutes'  -- past: DUE
);

-- Card 6 (MULTIPLE_CHOICE - Churchill): Relearning (lapses=2), DUE NOW
INSERT INTO study_progress (
    user_id, card_id, stability, difficulty,
    elapsed_days, scheduled_days, reps, lapses, state,
    last_review, next_review
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'),
    1.8, 6.9, 5, 5, 3, 2, 3,
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '1 hour'  -- past: DUE
);


-- ============================================================
-- 9. REVIEW LOGS
-- Historical review entries for the 6 cards above.
-- Each row represents one rating given by the user on one card.
-- Ratings: 1=Again, 2=Hard, 3=Good, 4=Easy.
-- ============================================================

-- Card 1: 3 reviews — steady improvement (Good, Good, Easy)
INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'),
     3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'),
     3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'),
     4, 7, 7);

-- Card 2: 2 reviews — Hard first, then Good
INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Qué fue el Día D?'),
     2, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Qué fue el Día D?'),
     3, 1, 1);

-- Card 3: 1 review — first time seen (Good)
INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = 'La SGMU finalizó en 1945 con la rendición de Alemania y Japón.'),
     3, 0, 0);

-- Card 4: 1 review — first time seen (Hard)
INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = 'La Unión Soviética formó parte del Eje junto con Alemania e Italia.'),
     2, 0, 0);

-- Card 5: 4 reviews — learned, then forgot once (Again on 4th review)
INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'),
     3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'),
     3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'),
     4, 3, 3),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'),
     1, 3, 3);

-- Card 6: 3 reviews — learned, forgot twice (Again twice)
INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'),
     3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'),
     1, 5, 5),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'),
     1, 5, 5);


COMMIT;
