-- First, let's update the existing passwords with BCrypt hashed versions
-- The hash below is for 'ET@2025' generated using BCrypt
UPDATE users 
SET password = '$2a$10$YourGeneratedBCryptHashHere'
WHERE password = 'ET@2025';

-- Make sure no plain text passwords can be stored in future
ALTER TABLE users 
ADD CONSTRAINT check_password_length 
CHECK (length(password) >= 60); 