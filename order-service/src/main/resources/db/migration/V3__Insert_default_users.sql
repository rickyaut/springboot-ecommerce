-- Insert default users for production
-- Passwords are BCrypt encoded: 'password' and 'admin123'
INSERT INTO users (id, username, password, roles, enabled) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8imdQMEww2RFQK4nWpflrCK0OYjm6', 'USER', true),
('550e8400-e29b-41d4-a716-446655440002', 'admin', '$2a$10$5OpTL7/rP5.HD9k7KqIKPeOI8HQg3pR6WO7oF8KJz.8QfJy8XjXoS', 'ADMIN,USER', true);