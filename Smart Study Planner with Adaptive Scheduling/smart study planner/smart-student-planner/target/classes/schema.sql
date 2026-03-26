CREATE DATABASE IF NOT EXISTS smart_student_planner;
USE smart_student_planner;

CREATE TABLE IF NOT EXISTS Users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    deadline DATETIME NOT NULL,
    priority_score DOUBLE DEFAULT 0.0,
    missed_count INT DEFAULT 0,
    time_drift DOUBLE DEFAULT 0.0, -- In minutes, tracks cumulative deviation
    last_studied_time DATETIME,
    avg_session_duration INT DEFAULT 30, -- In minutes, adjusts based on 'Drift Learning'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES Tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS SessionLogs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    schedule_id BIGINT,
    focus_score INT NOT NULL,
    confidence_score INT NOT NULL CHECK (confidence_score BETWEEN 1 AND 5),
    status VARCHAR(50) DEFAULT 'COMPLETED', -- COMPLETED, PARTIAL, MISSED
    planned_duration INT DEFAULT 30,
    actual_duration INT DEFAULT 30,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES Tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES Schedule(id) ON DELETE SET NULL
);

-- Explainable Rescheduling Decisions Table
CREATE TABLE IF NOT EXISTS RescheduleLogs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    old_time DATETIME,
    new_time DATETIME,
    reason TEXT NOT NULL, -- e.g. "Scan failed; replaced lowest priority task"
    factors JSON, -- Store deadline weight, confidence, consistency, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES Tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);

-- Force initialize Demo User mapping to frontend userId 1
INSERT IGNORE INTO Users (id, username, email) VALUES (1, 'demo_student', 'demo@antigravity.edu');
