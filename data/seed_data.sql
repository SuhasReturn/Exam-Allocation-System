-- ====================================================================
-- Exam Seat Allocation System - Seed Data Script
-- Database: exam_allocation
-- ====================================================================

-- 1. Examination Halls
INSERT INTO hall (id, name, total_rows, total_columns) VALUES
(1, 'LH-101', 6, 4),
(2, 'LH-102', 6, 4),
(3, 'LH-201', 5, 4);

-- 2. Faculty Members
INSERT INTO faculty (id, name, department) VALUES
(1, 'Dr. Rajesh Rao', 'Computer Science and Engineering'),
(2, 'Prof. Anita Desai', 'Information Science and Engineering'),
(3, 'Dr. Vikram Singh', 'Electronics and Communication Engineering'),
(4, 'Prof. Priya Sharma', 'Mathematics');

-- 3. Default Accounts (passwords hashed with BCrypt)
-- admin: admin123  -> $2a$10$wK1sWpWvR4J... (seeded by Spring Boot DataSeeder)
-- faculty1: faculty123
-- student1: student123
