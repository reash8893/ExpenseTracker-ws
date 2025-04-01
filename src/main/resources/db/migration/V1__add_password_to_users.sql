-- Add password column to users table
ALTER TABLE users ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '$2a$10$defaultpasswordhashedvalue'; 