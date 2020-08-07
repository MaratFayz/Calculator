INSERT INTO users(id, username, password, user_changed_id,
    date_time_last_change, is_locked, is_enabled,
    is_credentials_expired, is_account_expired) VALUES
    (1, "a", "$2a$10$RWxBp/xQm9YOHFsTMSs.suKLFipCu4OszqN7JnK1Z1PQwKB6FacA.", null,
    "2020-05-27 21:05:12.937360",
    null, 'X', null, null);

INSERT INTO users_roles(user_id, role_id) VALUES (1, 1);

INSERT INTO users(id, username, password, user_changed_id,
    date_time_last_change, is_locked, is_enabled,
    is_credentials_expired, is_account_expired) VALUES
    (2, "USER1", "$2a$10$RWxBp/xQm9YOHFsTMSs.suKLFipCu4OszqN7JnK1Z1PQwKB6FacA.", null,
    "2020-05-27 21:05:12.937360",
    null, 'X', null, null);

INSERT INTO users_roles(user_id, role_id) VALUES (2, 2);