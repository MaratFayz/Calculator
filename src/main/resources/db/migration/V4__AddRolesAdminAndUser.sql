INSERT INTO roles(id, name, user_id, date_time_last_change) VALUES
(1, 'ROLE_SUPERADMIN', 1, '2020-05-27 21:05:12.937360');

INSERT INTO role_authorities(role_id, authority_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18),
(1, 19), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27),
(1, 28), (1, 29), (1, 30), (1, 31), (1, 32), (1, 33), (1, 34), (1, 35), (1, 36),
(1, 37), (1, 38), (1, 39), (1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 45),
(1, 46), (1, 47), (1, 48), (1, 49), (1, 50), (1, 51), (1, 52), (1, 53), (1, 54),
(1, 55), (1, 56), (1, 57), (1, 58), (1, 59), (1, 60), (1, 61), (1, 62), (1, 63),
(1, 64), (1, 65), (1, 66), (1, 67), (1, 68);

INSERT INTO roles(id, name, user_id, date_time_last_change) VALUES
(2, 'ROLE_STANDARD_USER', 1, '2020-05-27 21:05:12.937360');

INSERT INTO role_authorities(role_id, authority_id) VALUES
(2, 68), (2, 15), (2, 19), (2, 20), (2, 21), (2, 23), (2, 27), (2, 28), (2, 29), (2, 30), (2, 31),
(2, 35), (2, 39), (2, 47), (2, 48), (2, 49), (2, 51), (2, 55), (2, 59), (2, 63), (2, 67);

INSERT INTO users_roles(user_id, role_id) VALUES (1, 1);
INSERT INTO users_roles(user_id, role_id) VALUES (2, 2);
INSERT INTO users_roles(user_id, role_id) VALUES (3, 2);