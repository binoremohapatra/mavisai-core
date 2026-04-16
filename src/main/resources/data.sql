-- Create User 1 safely
INSERT INTO users (id, name, email, password) 
VALUES (1, 'Alex', 'alex@example.com', 'password') 
ON CONFLICT (id) DO NOTHING;

-- Initialize config safely
INSERT INTO attendance_config (id, user_id, total_classes, attended_classes, required_attendance) 
VALUES (1, 1, 60, 45, 75) 
ON CONFLICT (id) DO NOTHING;

