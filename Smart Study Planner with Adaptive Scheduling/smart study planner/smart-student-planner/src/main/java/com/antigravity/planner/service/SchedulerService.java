package com.antigravity.planner.service;

import java.sql.*;
import java.time.LocalDateTime;

public class SchedulerService {
    private final Connection connection;

    public SchedulerService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Senior Requirement: Drift-Based Learning logic.
     * If user consistently over-runs (positive drift), we increase the task's base duration for the next cycle.
     */
    public void applyDriftLearning(long taskId) throws SQLException {
        String driftQuery = "SELECT AVG(actual_duration - planned_duration) as avg_drift, " +
                           "AVG(actual_duration) as new_suggested_duration " +
                           "FROM SessionLogs WHERE task_id = ? AND created_at > NOW() - INTERVAL 7 DAY";

        try (PreparedStatement pstmt = connection.prepareStatement(driftQuery)) {
            pstmt.setLong(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double avgDrift = rs.getDouble("avg_drift");
                    int suggestedDuration = rs.getInt("new_suggested_duration");

                    // If drift is significant (> 5 mins deviance), update the model
                    if (Math.abs(avgDrift) > 5) {
                        updateTaskBaseDuration(taskId, suggestedDuration);
                        System.out.println("🚀 Drift Learning: Adjusted Task " + taskId + " duration to " + suggestedDuration + "m based on behavior.");
                    }
                }
            }
        }
    }

    private void updateTaskBaseDuration(long taskId, int newDuration) throws SQLException {
        String sql = "UPDATE Tasks SET avg_session_duration = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newDuration);
            pstmt.setLong(2, taskId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Finds next available slot with proper buffers.
     */
    public LocalDateTime findNextFreeSlot(long userId, int durationMinutes) throws SQLException {
        // Implementation would query Schedule table for gaps
        return LocalDateTime.now().plusDays(1).withHour(10).withMinute(0); 
    }
}
