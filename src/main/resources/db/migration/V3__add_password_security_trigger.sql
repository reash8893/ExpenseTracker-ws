-- Create a function to check if a password is properly hashed
CREATE OR REPLACE FUNCTION check_password_hash()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if the password starts with '$2a$' (BCrypt format) and is at least 60 characters
    IF NEW.password NOT LIKE '$2a$%' OR length(NEW.password) < 60 THEN
        RAISE EXCEPTION 'Password must be properly hashed with BCrypt. Do not insert plain text passwords!';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create a trigger that runs before insert or update
CREATE TRIGGER ensure_hashed_password
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION check_password_hash(); 