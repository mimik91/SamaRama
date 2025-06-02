INSERT INTO incomplete_users (id, email, phone_number, created_at)
VALUES (1, 'cyclopick@gmail.com', '123456789', NOW());

INSERT INTO users (id, first_name, last_name, password, verified)
VALUES (1, 'Admin', 'Admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iASznx5cCJpx3KeR8RfX.K5LBQ4K', true);

INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_ADMIN');

SELECT setval('incomplete_users_id_seq', 1);