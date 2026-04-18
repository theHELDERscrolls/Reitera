-- ============================================================
-- Reitera — Demo Seed Data
-- ============================================================
-- Prerequisites: run init.sql first.
-- Passwords (BCrypt, strength 10): reitera2026 for both users.
-- To re-seed: run truncate.sql first, then re-run this script.
-- See docs/setup/database-setup.md for full instructions.
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
-- No explicit IDs: avoids PK conflicts if roles already exist in the DB.
INSERT INTO roles (name) VALUES
    ('STUDENT'),
    ('ADMIN')
ON CONFLICT (name) DO NOTHING;


-- 2. USERS
-- Fixed UUIDs allow study_progress and review_logs to reference
-- users by a known value without relying on auto-generated IDs.
INSERT INTO users (id, username, email, password, first_name, last_name, role_id) VALUES
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'alumno',
        'alumno@reitera.com',
        crypt('reitera2026', gen_salt('bf', 10)),
        'Alumno',
        'Demo',
        (SELECT id FROM roles WHERE name = 'STUDENT')
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


-- 3. CATEGORIES
INSERT INTO categories (name, description) VALUES
    ('Historia de España',  'Tarjetas sobre historia española desde la Reconquista hasta el siglo XX'),
    ('Programación Java',   'Conceptos de programación orientada a objetos, patrones de diseño y Spring'),
    ('Inglés B2',           'Vocabulario avanzado, phrasal verbs y gramática inglesa nivel B2');


-- 4. TAGS
INSERT INTO tags (name, hex_color, owner_id) VALUES
    ('importante', '#E74C3C', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
    ('difícil',    '#E67E22', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
    ('repaso',     '#3498DB', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
    ('vocabulario','#27AE60', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11')
ON CONFLICT (name, owner_id) DO NOTHING;


-- 5. DECKS
-- Two decks share "Historia de España" to demonstrate category-scoped study:
--   GET /study/due?categoryId=X returns cards from both SGMU and GCE decks.
INSERT INTO decks (title, description, is_public, owner_id, author_name, category_id) VALUES
    (
        'La Segunda Guerra Mundial',
        'Repaso de los principales eventos, fechas y personajes de la SGMU',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'alumno',
        (SELECT id FROM categories WHERE name = 'Historia de España')
    ),
    (
        'Patrones de Diseño GoF',
        'Los 23 patrones clásicos del libro Gang of Four aplicados a Java',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'alumno',
        (SELECT id FROM categories WHERE name = 'Programación Java')
    ),
    (
        'Phrasal Verbs Esenciales',
        'Los 60 phrasal verbs más usados en inglés cotidiano y profesional',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'alumno',
        (SELECT id FROM categories WHERE name = 'Inglés B2')
    ),
    (
        'La Guerra Civil Española',
        'Causas, desarrollo y consecuencias del conflicto civil español (1936-1939)',
        false,
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'alumno',
        (SELECT id FROM categories WHERE name = 'Historia de España')
    );


-- 6. CARDS (21 total — BASIC, CLOZE, MULTIPLE_CHOICE, TRUE_FALSE)

-- ---- Deck 1: La Segunda Guerra Mundial (6 cards) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'BASIC',
    '¿En qué fecha comenzó la Segunda Guerra Mundial?',
    '{"answer": "El 1 de septiembre de 1939, con la invasión alemana de Polonia"}',
    'Francia y el Reino Unido declararon la guerra a Alemania dos días después. Es el inicio oficial del conflicto en Europa.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'BASIC',
    '¿Qué fue el Día D?',
    '{"answer": "El desembarco aliado en las playas de Normandía el 6 de junio de 1944"}',
    'Operación Overlord: la mayor operación anfibia de la historia. Supuso la apertura del frente occidental contra la Alemania nazi.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'TRUE_FALSE',
    'La SGMU finalizó en 1945 con la rendición de Alemania y Japón.',
    '{"answer": true}',
    'Alemania se rindió el 8 de mayo de 1945 (VE Day). Japón se rindió el 2 de septiembre de 1945 (VJ Day) tras los bombardeos de Hiroshima y Nagasaki.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'TRUE_FALSE',
    'La Unión Soviética formó parte del Eje junto con Alemania e Italia.',
    '{"answer": false}',
    'La URSS firmó el Pacto Molotov–Ribbentrop con Alemania, pero se unió a los Aliados tras la Operación Barbarroja en junio de 1941.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'MULTIPLE_CHOICE',
    '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?',
    '{"options": ["Berlín", "Tokio", "Hiroshima", "Shanghai"], "correctIndex": 2}',
    'El 6 de agosto de 1945, EE.UU. lanzó la bomba "Little Boy" sobre Hiroshima. El 9 de agosto, "Fat Man" cayó sobre Nagasaki.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'MULTIPLE_CHOICE',
    '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?',
    '{"options": ["Neville Chamberlain", "Winston Churchill", "Clement Attlee", "Anthony Eden"], "correctIndex": 1}',
    'Churchill fue PM desde mayo de 1940 hasta julio de 1945, liderando al Reino Unido durante los momentos más críticos del conflicto.'
);


-- ---- Deck 2: Patrones de Diseño GoF (5 cards) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'BASIC',
    '¿Qué problema resuelve el patrón Singleton?',
    '{"answer": "Garantiza que una clase tenga una única instancia y proporciona un punto de acceso global a ella"}',
    'Se usa para recursos compartidos: pools de conexiones, loggers, configuración global. En Spring, todos los beans son Singleton por defecto.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'CLOZE',
    'El patrón ___ define una interfaz para crear objetos, pero deja que las subclases decidan qué clase instanciar.',
    '{"answer": "Factory Method"}',
    'Factory Method es un patrón creacional. Desacopla el código que usa el objeto del código que lo crea, respetando el principio Open/Closed.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'CLOZE',
    'El patrón ___ convierte la interfaz de una clase en otra que el cliente espera, actuando como intermediario entre incompatibles.',
    '{"answer": "Adapter"}',
    'También llamado Wrapper. Ejemplo clásico: adaptar una librería de terceros a la interfaz de tu aplicación sin modificar su código.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'MULTIPLE_CHOICE',
    '¿A qué categoría pertenece el patrón Observer?',
    '{"options": ["Creacional", "Estructural", "De comportamiento", "Concurrencia"], "correctIndex": 2}',
    'Observer es de comportamiento. Define una dependencia uno-a-muchos: cuando un objeto cambia, todos sus suscriptores son notificados.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'BASIC',
    '¿Qué es el patrón Strategy y cuándo se usa?',
    '{"answer": "Define una familia de algoritmos intercambiables. Se usa cuando queremos seleccionar el algoritmo a ejecutar en tiempo de ejecución sin cambiar el cliente"}',
    'Ejemplo: un sistema de ordenación que puede usar QuickSort o MergeSort según el contexto. En Spring Security, la autenticación usa este patrón.'
);


-- ---- Deck 3: Phrasal Verbs Esenciales (5 cards) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "give up" mean?',
    '{"answer": "To stop trying; to abandon an effort or habit"}',
    'Examples: "I gave up smoking last year." / "Don''t give up on your dreams!" Synonym: to quit, to abandon.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "look into" mean?',
    '{"answer": "To investigate or examine something carefully"}',
    'Example: "The police are looking into the matter." Synonyms: to investigate, to research, to examine.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "put off" mean?',
    '{"answer": "To postpone or delay something to a later time"}',
    'Example: "Don''t put off until tomorrow what you can do today." Synonyms: to delay, to defer, to postpone.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'TRUE_FALSE',
    '"Turn down" means to increase the volume of something.',
    '{"answer": false}',
    '"Turn down" means to DECREASE volume or to reject an offer. The opposite is "turn up". E.g., "Can you turn down the music, please?"'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'TRUE_FALSE',
    '"Carry on" means to continue doing something.',
    '{"answer": true}',
    'Example: "Carry on with your work." Synonyms: to continue, to keep going, to proceed.'
);


-- ---- Deck 4: La Guerra Civil Española (5 cards — no study_progress, all NEW) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'BASIC',
    '¿Cuándo comenzó la Guerra Civil Española y quiénes fueron los dos bandos principales?',
    '{"answer": "Comenzó el 17 de julio de 1936. Los dos bandos fueron el bando republicano (gobierno legítimo) y el bando nacional (sublevados liderados por Franco)"}',
    'El detonante fue el golpe de Estado de un sector del ejército contra la Segunda República. La guerra duró casi tres años, hasta abril de 1939.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'TRUE_FALSE',
    'La Guerra Civil Española duró más de cinco años.',
    '{"answer": false}',
    'El conflicto duró exactamente 2 años y 8 meses: desde el 17 de julio de 1936 hasta el 1 de abril de 1939, cuando Franco firmó el parte de victoria.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'MULTIPLE_CHOICE',
    '¿Qué país apoyó militarmente al bando republicano durante la Guerra Civil?',
    '{"options": ["Alemania", "Italia", "La Unión Soviética", "Portugal"], "correctIndex": 2}',
    'La URSS suministró armas y asesores militares. Alemania e Italia apoyaron al bando nacional con la Legión Cóndor y el CTV respectivamente.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'CLOZE',
    'El bombardeo de ___ el 26 de abril de 1937, llevado a cabo por la Legión Cóndor alemana, fue inmortalizado por Picasso en un famoso cuadro.',
    '{"answer": "Guernica"}',
    'Guernica era una ciudad vasca sin valor militar estratégico. El cuadro de Picasso es uno de los símbolos antibelicistas más reconocidos del siglo XX.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'BASIC',
    '¿Qué régimen político se instauró en España tras la victoria del bando nacional?',
    '{"answer": "La dictadura franquista, un régimen autoritario liderado por Francisco Franco que se mantuvo hasta su muerte en 1975"}',
    'El franquismo fue un régimen de partido único con represión política, censura y aislamiento internacional inicial. Duró 36 años.'
);


-- ---- Deck 1: La Segunda Guerra Mundial — additional cards (6 → 12) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'BASIC',
    '¿Qué fue la Operación Barbarroja y cuándo se lanzó?',
    '{"answer": "La invasión alemana de la Unión Soviética, lanzada el 22 de junio de 1941. Fue la mayor operación terrestre de la historia"}',
    'Hitler rompió el Pacto Molotov–Ribbentrop sorprendiendo a Stalin. La URSS sufrió pérdidas catastróficas inicialmente, pero logró resistir y contraatacar hasta la victoria en 1945.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'MULTIPLE_CHOICE',
    '¿Cuándo comenzó la batalla de Stalingrado?',
    '{"options": ["Agosto de 1942", "Junio de 1941", "Diciembre de 1942", "Enero de 1944"], "correctIndex": 0}',
    'La batalla duró de agosto de 1942 a febrero de 1943. La rendición del Sexto Ejército alemán marcó un punto de inflexión decisivo en el frente oriental.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'TRUE_FALSE',
    'Los Estados Unidos entraron en la Segunda Guerra Mundial tras el ataque japonés a Pearl Harbor en diciembre de 1941.',
    '{"answer": true}',
    'El 7 de diciembre de 1941, Japón atacó la base naval de Pearl Harbor (Hawái). Al día siguiente, EE.UU. declaró la guerra a Japón; Alemania e Italia le declararon la guerra a EE.UU. el 11 de diciembre.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'CLOZE',
    'El programa de ___ permitió a EE.UU. suministrar material bélico a los Aliados sin violar formalmente su neutralidad antes de entrar en la guerra.',
    '{"answer": "Préstamo y Arriendo"}',
    'Firmado en marzo de 1941, el Lend-Lease Act envió más de 50.000 millones de dólares en material a Reino Unido, URSS, China y Francia Libre. Fue fundamental para sostener la resistencia aliada.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'BASIC',
    '¿Qué fueron los Juicios de Núremberg y qué importancia tuvieron?',
    '{"answer": "Tribunales militares internacionales celebrados entre 1945 y 1946 para juzgar a los líderes nazis por crímenes de guerra y crímenes contra la humanidad"}',
    'Establecieron el precedente de la responsabilidad penal individual en el derecho internacional. 12 acusados fueron condenados a muerte. Sentaron las bases del derecho penal internacional moderno.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial'),
    'MULTIPLE_CHOICE',
    '¿Cuántas personas aproximadamente murieron durante el Holocausto?',
    '{"options": ["Menos de 1 millón", "Alrededor de 3 millones", "Aproximadamente 6 millones de judíos, más de 11 millones en total", "Más de 20 millones"], "correctIndex": 2}',
    'El Holocausto fue el genocidio sistemático de 6 millones de judíos europeos perpetrado por el régimen nazi. Además, millones de polacos, gitanos, discapacitados y prisioneros de guerra soviéticos fueron exterminados.'
);


-- ---- Deck 2: Patrones de Diseño GoF — additional cards (5 → 12) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'BASIC',
    '¿Qué hace el patrón Decorator y en qué se diferencia de la herencia?',
    '{"answer": "Añade responsabilidades a un objeto dinámicamente en tiempo de ejecución sin modificar su clase. La herencia extiende en compilación; el Decorator lo hace en tiempo de ejecución"}',
    'Ejemplo clásico: los flujos de Java (BufferedReader envuelve FileReader). Se pueden apilar múltiples Decorators para combinar comportamientos de forma flexible.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'CLOZE',
    'El patrón ___ proporciona una interfaz simplificada a un conjunto complejo de subsistemas, ocultando su complejidad al cliente.',
    '{"answer": "Facade"}',
    'Ejemplo: una clase HomeTheaterFacade que coordina proyector, amplificador y reproductor con un solo método watchMovie(). El cliente no necesita conocer los subsistemas internos.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'MULTIPLE_CHOICE',
    '¿A qué categoría pertenece el patrón Builder?',
    '{"options": ["Estructural", "Creacional", "De comportamiento", "Concurrencia"], "correctIndex": 1}',
    'Builder es creacional: separa la construcción de un objeto complejo de su representación final. Permite construir el mismo proceso con diferentes resultados (ej. StringBuilder en Java).'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'BASIC',
    '¿Qué es el patrón Command y qué problema resuelve?',
    '{"answer": "Encapsula una solicitud como un objeto, permitiendo parametrizar clientes, encolar operaciones y soportar deshacer/rehacer"}',
    'Ejemplo: un editor de texto donde cada acción es un Command. El historial de comandos permite implementar Ctrl+Z. Se usa también en sistemas de colas y transacciones.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'CLOZE',
    'El patrón ___ define el esqueleto de un algoritmo en una superclase, dejando que las subclases implementen los pasos específicos sin cambiar la estructura general.',
    '{"answer": "Template Method"}',
    'Ejemplo: una clase abstracta DataMiner con un método mine() que llama a extractData(), parseData() y analyzeData(). Las subclases concretan los pasos; el flujo general es invariable.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'TRUE_FALSE',
    'El patrón Proxy y el patrón Decorator tienen exactamente el mismo propósito y son intercambiables.',
    '{"answer": false}',
    'Aunque ambos envuelven un objeto, sus intenciones difieren: Proxy controla el acceso (lazy init, seguridad, caché); Decorator añade comportamiento. En Spring, @Transactional usa Proxy; los flujos de Java usan Decorator.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Patrones de Diseño GoF'),
    'BASIC',
    '¿Qué problema resuelve el patrón Iterator?',
    '{"answer": "Proporciona una forma de acceder secuencialmente a los elementos de una colección sin exponer su representación interna"}',
    'El cliente usa el iterador sin saber si la colección es una lista, un árbol o un grafo. En Java, la interfaz Iterable y el bucle for-each son implementaciones directas de este patrón.'
);


-- ---- Deck 3: Phrasal Verbs Esenciales — additional cards (5 → 12) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "break up" mean?',
    '{"answer": "To end a romantic relationship; or to separate something into smaller parts"}',
    'Examples: "They broke up after two years together." / "The company broke up into smaller divisions." Context determines which meaning applies.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'MULTIPLE_CHOICE',
    'Which sentence uses "run out of" correctly?',
    '{"options": ["We ran out of the building.", "We ran out of milk, so I need to go shopping.", "She ran out of the race early.", "He ran out of his friend."], "correctIndex": 1}',
    '"Run out of" means to have no more of something left. It requires an object (what you have run out of). It is NOT the same as "run out" (to exit a place running).'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'TRUE_FALSE',
    '"Get along" means to have a good relationship with someone.',
    '{"answer": true}',
    'Example: "I get along well with my colleagues." Also used as "get along with" followed by a person. Synonyms: to get on with, to be on good terms with.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'CLOZE',
    'After losing her job, she decided to ___ her own business from scratch.',
    '{"answer": "set up"}',
    '"Set up" means to start, establish, or arrange something. Other uses: "set up a meeting", "set up a system". Synonyms: to establish, to found, to start.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "bring about" mean?',
    '{"answer": "To cause something to happen; to make something occur"}',
    'Example: "The new law brought about significant changes in the industry." Synonyms: to cause, to produce, to lead to. Commonly used in formal writing.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'TRUE_FALSE',
    '"Call off" means to cancel something that was previously planned.',
    '{"answer": true}',
    'Example: "They called off the meeting due to bad weather." Synonyms: to cancel, to abandon. Opposite in meaning: to call on (to request/visit) or to call for (to demand).'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales'),
    'BASIC',
    'What does "come across" mean?',
    '{"answer": "To find or meet something/someone by chance; or to make a particular impression on others"}',
    'Examples: "I came across an interesting article online." / "She comes across as very confident in interviews." Two distinct meanings — context is key.'
);


-- ---- Deck 4: La Guerra Civil Española — additional cards (5 → 12) ----

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'BASIC',
    '¿Qué fueron las Brigadas Internacionales?',
    '{"answer": "Unidades militares formadas por voluntarios extranjeros de más de 50 países que lucharon del lado de la República Española entre 1936 y 1938"}',
    'Unos 35.000 voluntarios de todo el mundo, incluyendo intelectuales como Hemingway y Orwell. Fueron disueltas en 1938 como gesto diplomático de la República ante la Sociedad de Naciones.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'TRUE_FALSE',
    'Alemania y la URSS firmaron el Pacto de No Intervención y cumplieron sus compromisos de no apoyar a ningún bando.',
    '{"answer": false}',
    'Ambas potencias firmaron el Pacto de No Intervención (1936) pero lo ignoraron: Alemania e Italia apoyaron al bando nacional con la Legión Cóndor y el CTV; la URSS suministró armas y asesores al bando republicano.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'MULTIPLE_CHOICE',
    '¿Qué fue el Frente Popular que ganó las elecciones de febrero de 1936?',
    '{"options": ["Una alianza de partidos de izquierdas y republicanos", "Un partido político único", "La coalición de militares y monárquicos", "Una organización sindical anarquista"], "correctIndex": 0}',
    'El Frente Popular agrupó a republicanos de izquierda, socialistas y comunistas. Su victoria electoral fue el detonante del golpe de Estado del 17 de julio de 1936.'
);

-- [CLOZE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'CLOZE',
    'La ___ fue la unidad aérea alemana enviada por Hitler para apoyar al bando nacional, responsable del bombardeo de Guernica en abril de 1937.',
    '{"answer": "Legión Cóndor"}',
    'La Legión Cóndor permitió a Alemania probar sus tácticas de guerra aérea en combate real. Sus experiencias en España influyeron directamente en la estrategia de la Luftwaffe durante la SGMU.'
);

-- [BASIC]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'BASIC',
    '¿Qué fue el exilio republicano tras la Guerra Civil Española?',
    '{"answer": "La huida de cientos de miles de republicanos hacia Francia, México y otros países en 1939 para escapar de la represión franquista"}',
    'Se calcula que unos 500.000 refugiados cruzaron la frontera francesa (La Retirada). México fue el destino más destacado: el gobierno de Cárdenas acogió al gobierno republicano en el exilio.'
);

-- [TRUE_FALSE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'TRUE_FALSE',
    'España participó directamente en la Segunda Guerra Mundial como aliada formal de la Alemania nazi.',
    '{"answer": false}',
    'Franco se mantuvo en una posición de "no beligerancia" y luego "neutralidad". Aunque envió la División Azul de voluntarios al frente soviético, no entró formalmente en la guerra, en parte por su debilidad económica.'
);

-- [MULTIPLE_CHOICE]
INSERT INTO cards (deck_id, type, question, answer_json, explanation) VALUES (
    (SELECT id FROM decks WHERE title = 'La Guerra Civil Española'),
    'MULTIPLE_CHOICE',
    '¿Cuál fue la principal consecuencia internacional del aislamiento de la España franquista tras la SGMU?',
    '{"options": ["La recomendación de la ONU de retirar embajadores en 1946", "La invasión aliada de la Península Ibérica", "El embargo económico unilateral de EE.UU.", "La pérdida de las colonias africanas"], "correctIndex": 0}',
    'La Asamblea General de la ONU recomendó en 1946 la retirada de embajadores de España por su colaboración con el Eje. El aislamiento se rompió gradualmente en los años 50 con los acuerdos con EE.UU. durante la Guerra Fría.'
);


-- 7. CARD TAGS
INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id FROM cards c CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial')
  AND t.name = 'importante'
ON CONFLICT (card_id, tag_id) DO NOTHING;

INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id FROM cards c CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'La Segunda Guerra Mundial')
  AND c.type = 'MULTIPLE_CHOICE' AND t.name = 'difícil'
ON CONFLICT (card_id, tag_id) DO NOTHING;

INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id FROM cards c CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'Phrasal Verbs Esenciales')
  AND t.name = 'vocabulario'
ON CONFLICT (card_id, tag_id) DO NOTHING;

INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id FROM cards c CROSS JOIN tags t
WHERE c.deck_id = (SELECT id FROM decks WHERE title = 'La Guerra Civil Española')
  AND t.name = 'importante'
ON CONFLICT (card_id, tag_id) DO NOTHING;

INSERT INTO card_tags (card_id, tag_id)
SELECT c.id, t.id FROM cards c CROSS JOIN tags t
WHERE c.question LIKE '%Guernica%' AND t.name = 'difícil'
ON CONFLICT (card_id, tag_id) DO NOTHING;


-- 8. STUDY PROGRESS
-- 6 cards from Deck 1 (SGMU). States: Review(2), Learning(1), Relearning(3).
-- Cards with next_review in the past appear immediately in GET /study/due.

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'),
    21.5, 4.2, 7, 21, 3, 0, 2, NOW() - INTERVAL '7 days', NOW() + INTERVAL '14 days');  -- Review, not due

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Qué fue el Día D?'),
    10.3, 5.1, 10, 10, 2, 0, 2, NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 minute');  -- Review, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = 'La SGMU finalizó en 1945 con la rendición de Alemania y Japón.'),
    0.9, 5.0, 0, 0, 1, 0, 1, NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '5 minutes');  -- Learning, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = 'La Unión Soviética formó parte del Eje junto con Alemania e Italia.'),
    0.6, 6.2, 0, 0, 1, 0, 1, NOW() - INTERVAL '15 minutes', NOW() - INTERVAL '2 minutes');  -- Learning, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'),
    2.1, 7.4, 3, 3, 4, 1, 3, NOW() - INTERVAL '3 days', NOW() - INTERVAL '30 minutes');  -- Relearning, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'),
    1.8, 6.9, 5, 5, 3, 2, 3, NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 hour');  -- Relearning, DUE


-- Additional study_progress for Deck 2 (GoF) — 2 cards in different states
INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Qué problema resuelve el patrón Singleton?'),
    15.2, 3.8, 5, 15, 2, 0, 2, NOW() - INTERVAL '5 days', NOW() + INTERVAL '10 days');  -- Review, not due

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = '¿Qué es el patrón Strategy y cuándo se usa?'),
    0.8, 4.5, 0, 0, 1, 0, 1, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '10 minutes');  -- Learning, DUE

-- Additional study_progress for Deck 3 (Phrasal Verbs) — 2 cards in different states
INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = 'What does "give up" mean?'),
    8.7, 4.1, 8, 8, 2, 0, 2, NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 hours');  -- Review, DUE

INSERT INTO study_progress (user_id, card_id, stability, difficulty, elapsed_days, scheduled_days, reps, lapses, state, last_review, next_review)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    (SELECT id FROM cards WHERE question = 'What does "put off" mean?'),
    1.2, 5.3, 0, 0, 1, 0, 1, NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '15 minutes');  -- Learning, DUE


-- 9. REVIEW LOGS (ratings: 1=Again, 2=Hard, 3=Good, 4=Easy)

INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'), 3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿En qué fecha comenzó la Segunda Guerra Mundial?'), 4, 7, 7),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Qué fue el Día D?'), 2, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Qué fue el Día D?'), 3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = 'La SGMU finalizó en 1945 con la rendición de Alemania y Japón.'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = 'La Unión Soviética formó parte del Eje junto con Alemania e Italia.'), 2, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 3, 1, 1),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 4, 3, 3),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Cuál de estas ciudades fue destruida por una bomba atómica en agosto de 1945?'), 1, 3, 3),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'), 1, 5, 5),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Quién fue el Primer Ministro británico durante la mayor parte de la SGMU?'), 1, 5, 5);

-- Additional review logs for the new study_progress records
INSERT INTO review_logs (user_id, card_id, rating, elapsed_days, scheduled_days) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Qué problema resuelve el patrón Singleton?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Qué problema resuelve el patrón Singleton?'), 4, 5, 5),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = '¿Qué es el patrón Strategy y cuándo se usa?'), 2, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = 'What does "give up" mean?'), 3, 0, 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = 'What does "give up" mean?'), 4, 8, 8),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM cards WHERE question = 'What does "put off" mean?'), 3, 0, 0);


COMMIT;
