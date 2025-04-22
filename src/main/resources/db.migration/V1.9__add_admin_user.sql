DO $$
DECLARE
    admin_exists BOOLEAN;
    admin_id BIGINT;
BEGIN
    -- Check if admin user exists
    SELECT EXISTS (
        SELECT 1 FROM users WHERE email = 'dominiklach@poczta.fm'
    ) INTO admin_exists;

    -- Create admin user if not exists
    IF NOT admin_exists THEN
        INSERT INTO users (
            email,
            first_name,
            last_name,
            password,
            verified,
            created_at
        ) VALUES (
            'dominiklach@poczta.fm',
            'Admin',
            'User',
            '$2a$10$JQOfPxoQlKw2YGjYkN6we.p2y2Nt3s5QFUKlL6Yi9P2livRuAyLqK', -- hashed 'misiek'
            true,
            CURRENT_TIMESTAMP
        ) RETURNING id INTO admin_id;

        -- Add ROLE_ADMIN to the admin user
        INSERT INTO user_roles (user_id, role) VALUES (admin_id, 'ROLE_ADMIN');

        RAISE NOTICE 'Admin user created with ID %', admin_id;
    ELSE
        -- Get admin user ID
        SELECT id FROM users WHERE email = 'dominiklach@poczta.fm' INTO admin_id;

        -- Remove all existing roles for admin
        DELETE FROM user_roles WHERE user_id = admin_id;

        -- Add ONLY ROLE_ADMIN
        INSERT INTO user_roles (user_id, role) VALUES (admin_id, 'ROLE_ADMIN');
        RAISE NOTICE 'Updated roles for existing admin user with ID %', admin_id;
    END IF;
END $$;