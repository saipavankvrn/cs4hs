package com.antigravity.planner.service;

import java.sql.*;
import java.time.LocalDateTime;

public class FocusSessionService {
    private final Connection connection;
    private final SchedulerService schedulerService;

    public FocusSessionService(Connection connection, SchedulerService schedulerService) {
        this.connection = connection;
        this.schedulerService = schedulerService;
    }

    /**
     * Complete a session and update the Adaptive Metrics (Drift, Last Studied).
     */
    public void finalizeSession(long userId, long taskId, long scheduleId, int focus, int confidence, String status, int planned, int actual, String notes) throws SQLException {
        // 1. Calculate Drift
        double drift = actual - planned;

        // 2. Insert Session Log with Status Support
        String logSql = "INSERT INTO SessionLogs (user_id, task_id, schedule_id, focus_score, confidence_score, status, planned_duration, actual_duration, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(logSql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, taskId);
            pstmt.setLong(3, scheduleId);
            pstmt.setInt(4, focus);
            pstmt.setInt(5, confidence);
            pstmt.setString(6, status);
            pstmt.setInt(7, planned);
            pstmt.setInt(8, actual);
            pstmt.setString(9, notes);
            pstmt.executeUpdate();
        }

        // 3. Update Task-Specific Adaptive Fields
        String taskSql = "UPDATE Tasks SET time_drift = time_drift + ?, last_studied_time = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(taskSql)) {
            pstmt.setDouble(1, drift);
            pstmt.setObject(2, LocalDateTime.now());
            pstmt.setLong(3, taskId);
            pstmt.executeUpdate();
        }

        // 4. Mark Schedule Status
        String scheduleSql = "UPDATE Schedule SET is_completed = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(scheduleSql)) {
            pstmt.setBoolean(1, "COMPLETED".equals(status));
            pstmt.setLong(2, scheduleId);
            pstmt.executeUpdate();
        }

        // 5. Trigger Drift-Based Learning (Update future task models)
        schedulerService.applyDriftLearning(taskId);
    }
}
