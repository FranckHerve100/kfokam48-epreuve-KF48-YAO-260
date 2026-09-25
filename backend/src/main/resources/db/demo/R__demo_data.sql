-- Données de démonstration (PostgreSQL uniquement, jamais chargées par le profil test).
-- Migration répétable : rejouée seulement si ce fichier change ; ON CONFLICT la rend idempotente.
-- Les dates sont relatives à now() : la session KF48YD est ouverte au premier démarrage (valable 15 min).

INSERT INTO promotion (id, nom) VALUES
    (1, 'KF48 Yaoundé'),
    (2, 'KF48 Douala')
ON CONFLICT (id) DO NOTHING;

INSERT INTO etudiant (id, nom, promotion_id) VALUES
    (1, 'Abena Mvondo',     1),
    (2, 'Boris Ngono',      1),
    (3, 'Carine Etoundi',   1),
    (4, 'Daniel Fouda',     1),
    (5, 'Estelle Nkoulou',  1),
    (6, 'Fabrice Onana',    1),
    (7, 'Grace Ekwalla',    2),
    (8, 'Hervé Din',        2)
ON CONFLICT (id) DO NOTHING;

-- 1 : code expiré (ouverte hier) · 2 : clôturée · 3 : ouverte au démarrage
INSERT INTO session (id, titre, promotion_id, code, ouverture_at, expiration_at, cloture_at) VALUES
    (1, 'Spring Boot : les bases',  1, 'EXPR22', now() - INTERVAL '1 day',  now() - INTERVAL '1 day' + INTERVAL '15 minutes', NULL),
    (2, 'JPA et Flyway',            1, 'CLTR23', now() - INTERVAL '2 days', now() - INTERVAL '2 days' + INTERVAL '15 minutes', now() - INTERVAL '2 days' + INTERVAL '2 hours'),
    (3, 'Tests et intégration',     1, 'KF48YD', now(),                     now() + INTERVAL '15 minutes',                     NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO presence (id, session_id, etudiant_id, source, marquee_at) VALUES
    (1, 1, 1, 'ETUDIANT',  now() - INTERVAL '1 day' + INTERVAL '2 minutes'),
    (2, 1, 2, 'ETUDIANT',  now() - INTERVAL '1 day' + INTERVAL '3 minutes'),
    (3, 1, 3, 'ETUDIANT',  now() - INTERVAL '1 day' + INTERVAL '4 minutes'),
    (4, 1, 4, 'ETUDIANT',  now() - INTERVAL '1 day' + INTERVAL '5 minutes'),
    (5, 2, 1, 'ETUDIANT',  now() - INTERVAL '2 days' + INTERVAL '2 minutes'),
    (6, 2, 2, 'ETUDIANT',  now() - INTERVAL '2 days' + INTERVAL '3 minutes'),
    (7, 2, 5, 'FORMATEUR', now() - INTERVAL '2 days' + INTERVAL '40 minutes'),
    (8, 3, 1, 'ETUDIANT',  now()),
    (9, 3, 2, 'ETUDIANT',  now())
ON CONFLICT (id) DO NOTHING;

-- Exercice 1 relu (note 15), exercice 2 en attente de relecture
INSERT INTO exercice (id, session_id, etudiant_id, lien, statut, depose_at) VALUES
    (1, 1, 1, 'https://github.com/abena-mvondo/kf48-spring-bases',  'RELU',                 now() - INTERVAL '1 day' + INTERVAL '1 hour'),
    (2, 1, 3, 'https://github.com/carine-etoundi/kf48-spring-bases', 'EN_ATTENTE_RELECTURE', now() - INTERVAL '1 day' + INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO relecture (id, exercice_id, relecteur_id, note, commentaire, assignee_at, rendue_at) VALUES
    (1, 1, 2, 15,   'Couches bien séparées. Ajouter des tests sur les cas d''erreur.', now() - INTERVAL '1 day' + INTERVAL '1 hour', now() - INTERVAL '20 hours'),
    (2, 2, 4, NULL, NULL,                                                               now() - INTERVAL '1 day' + INTERVAL '2 hours', NULL)
ON CONFLICT (id) DO NOTHING;

-- Les identifiants générés reprennent à 1000 (au-delà des données de démonstration)
SELECT setval(pg_get_serial_sequence('promotion', 'id'), GREATEST(1000, (SELECT COALESCE(MAX(id), 0) + 1 FROM promotion)), false);
SELECT setval(pg_get_serial_sequence('etudiant',  'id'), GREATEST(1000, (SELECT COALESCE(MAX(id), 0) + 1 FROM etudiant)),  false);
SELECT setval(pg_get_serial_sequence('session',   'id'), GREATEST(1000, (SELECT COALESCE(MAX(id), 0) + 1 FROM session)),   false);
SELECT setval(pg_get_serial_sequence('presence',  'id'), GREATEST(1000, (SELECT COALESCE(MAX(id), 0) + 1 FROM presence)),  false);
SELECT setval(pg_get_serial_sequence('exercice',  'id'), GREATEST(1000, (SELECT COALESCE(MAX(id), 0) + 1 FROM exercice)),  false);
SELECT setval(pg_get_serial_sequence('relecture', 'id'), GREATEST(1000, (SELECT COALESCE(MAX(id), 0) + 1 FROM relecture)), false);
