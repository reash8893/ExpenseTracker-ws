-- Example of inserting test users with BCrypt hashed passwords
-- IMPORTANT: Replace these hashes with new ones generated from PasswordHashGenerator
-- These are example hashes for the passwords: 'ET@2025', 'test123', 'admin123'

INSERT INTO users (user_name, email, password) 
VALUES (
    'testuser1', 
    'test1@example.com',
    '$2a$10$YourGeneratedHashHere'  -- Replace with hash for 'ET@2025'
);

INSERT INTO users (user_name, email, password) 
VALUES (
    'testuser2', 
    'test2@example.com',
    '$2a$10$AnotherGeneratedHashHere'  -- Replace with hash for 'test123'
);

-- How to verify if a password is hashed:
SELECT 
    user_name,
    CASE 
        WHEN password LIKE '$2a$%' AND length(password) >= 60 THEN 'Properly Hashed'
        ELSE 'NOT Properly Hashed - Security Risk!'
    END as password_status
FROM users; 