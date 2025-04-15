DO $$
DECLARE
    admin_exists BOOLEAN;
    moderator_exists BOOLEAN;
    admin_id BIGINT;
    moderator_id BIGINT;
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

        -- Add ROLE_CLIENT and ROLE_ADMIN to the admin user
        INSERT INTO user_roles (user_id, role) VALUES (admin_id, 'ROLE_CLIENT');
        INSERT INTO user_roles (user_id, role) VALUES (admin_id, 'ROLE_ADMIN');

        RAISE NOTICE 'Admin user created with ID %', admin_id;
    ELSE
        -- Get admin user ID
        SELECT id FROM users WHERE email = 'dominiklach@poczta.fm' INTO admin_id;

        -- Check if admin already has ROLE_ADMIN
        IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = admin_id AND role = 'ROLE_ADMIN') THEN
            -- Add ROLE_ADMIN to the admin user
            INSERT INTO user_roles (user_id, role) VALUES (admin_id, 'ROLE_ADMIN');
            RAISE NOTICE 'Added ROLE_ADMIN to existing user with ID %', admin_id;
        END IF;
    END IF;

    -- Check if moderator user exists
    SELECT EXISTS (
        SELECT 1 FROM users WHERE email = 'moderator@example.com'
    ) INTO moderator_exists;

    -- Create moderator user if not exists
    IF NOT moderator_exists THEN
        INSERT INTO users (
            email,
            first_name,
            last_name,
            password,
            verified,
            created_at
        ) VALUES (
            'moderator@example.com',
            'Moderator',
            'User',
            '$2a$10$JQOfPxoQlKw2YGjYkN6we.p2y2Nt3s5QFUKlL6Yi9P2livRuAyLqK', -- hashed 'misiek'
            true,
            CURRENT_TIMESTAMP
        ) RETURNING id INTO moderator_id;

        -- Add ROLE_CLIENT and ROLE_MODERATOR to the moderator user
        INSERT INTO user_roles (user_id, role) VALUES (moderator_id, 'ROLE_CLIENT');
        INSERT INTO user_roles (user_id, role) VALUES (moderator_id, 'ROLE_MODERATOR');

        RAISE NOTICE 'Moderator user created with ID %', moderator_id;
    ELSE
        -- Get moderator user ID
        SELECT id FROM users WHERE email = 'moderator@example.com' INTO moderator_id;

        -- Check if moderator already has ROLE_MODERATOR
        IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = moderator_id AND role = 'ROLE_MODERATOR') THEN
            -- Add ROLE_MODERATOR to the moderator user
            INSERT INTO user_roles (user_id, role) VALUES (moderator_id, 'ROLE_MODERATOR');
            RAISE NOTICE 'Added ROLE_MODERATOR to existing user with ID %', moderator_id;
        END IF;
    END IF;
END $$;