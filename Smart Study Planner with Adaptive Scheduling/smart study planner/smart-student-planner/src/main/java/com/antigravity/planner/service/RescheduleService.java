package com.antigravity.planner.service;

import java.sql.*;
import java.time.LocalDateTime;

public class RescheduleService {

    private final Connection connection;

    public RescheduleService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Entry point for adaptive rescheduling when a session is missed.
     */
    public void processMissedTask(long scheduleId, long taskId, long userId) throws SQLException {
        // Increment missed count
        updateMissedCount(taskId);

        // Fetch current priority to determine if replacement is worth it
        double missedTaskScore = getTaskPriority(taskId);

        // 1. Scan for the next optimal free slot within 48 hours
        LocalDateTime optimalSlot = findOptimalSlotIn48Hours(userId, taskId);

        if (optimalSlot != null) {
            updateScheduleSlot(scheduleId, optimalSlot, "Adaptive 48h Free Scan Success");
            logDecision(taskId, userId, null, optimalSlot, "Optimization success: found free gap in next 2 days.", "{}");
        } else {
            // 2. Overload Protection: No free slots exist, replace lowest priority task (if threshold met)
            replaceLowestPriorityForHighPriority(userId, taskId, scheduleId, missedTaskScore);
        }
    }

    private LocalDateTime findOptimalSlotIn48Hours(long userId, long taskId) throws SQLException {
        LocalDateTime scanPointer = LocalDateTime.now().plusHours(1).withMinute(0);
        LocalDateTime scanLimit = LocalDateTime.now().plusHours(48);

        while (scanPointer.isBefore(scanLimit)) {
            if (isSlotFree(userId, scanPointer, 45) && passesSubjectConstraint(userId, taskId, scanPointer)) {
                return scanPointer;
            }
            scanPointer = scanPointer.plusMinutes(45); // Assuming 30m session + 15m buffer
        }
        return null;
    }

    private void replaceLowestPriorityForHighPriority(long userId, long missedTaskId, long missedScheduleId, double missedTaskScore) throws SQLException {
        // Find task in next 48h with lowest priority score, and check if it's significantly lower
        String lowestSql = "SELECT s.id, s.start_time, t.priority_score, t.title FROM Schedule s " +
                           "JOIN Tasks t ON s.task_id = t.id WHERE s.user_id = ? AND s.start_time > NOW() " +
                           "AND t.priority_score < (? - 20) " + // Senior Rule: Only replace if gap is significant
                           "ORDER BY t.priority_score ASC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(lowestSql)) {
            pstmt.setLong(1, userId);
            pstmt.setDouble(2, missedTaskScore);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime targetTime = rs.getObject("start_time", LocalDateTime.class);
                    
                    // Replace the low priority session with the current high priority one
                    updateScheduleSlot(missedScheduleId, targetTime, "Overload Protection: Replaced low-priority task");
                    logDecision(missedTaskId, userId, null, targetTime, "Overload detected. Replaced lowest priority task: " + rs.getString("title"), "{}");
                }
            }
        }
    }

    private boolean isSlotFree(long userId, LocalDateTime start, int durationMins) throws SQLException {
        LocalDateTime end = start.plusMinutes(durationMins);
        // Strict overlap protection: Check if ANY existing session exists within the new [start, end] window
        String query = "SELECT COUNT(*) FROM Schedule WHERE user_id = ? " +
                       "AND NOT (end_time <= ? OR start_time >= ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);
            pstmt.setObject(2, start);
            pstmt.setObject(3, end);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }

    private double getTaskPriority(long taskId) throws SQLException {
        String sql = "SELECT priority_score FROM Tasks WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    private boolean passesSubjectConstraint(long userId, long taskId, LocalDateTime time) throws SQLException {
        // Constraint: No same subject back-to-back
        String query = "SELECT task_id FROM Schedule WHERE user_id = ? AND end_time = ? - INTERVAL 15 MINUTE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);
            pstmt.setObject(2, time);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("task_id") != taskId;
                }
            }
        }
        return true;
    }

    private void updateScheduleSlot(long scheduleId, LocalDateTime newTime, String reason) throws SQLException {
        String sql = "UPDATE Schedule SET start_time = ?, end_time = ?, is_completed = FALSE WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setObject(1, newTime);
            pstmt.setObject(2, newTime.plusMinutes(45));
            pstmt.setLong(3, scheduleId);
            pstmt.executeUpdate();
        }
    }

    private void updateMissedCount(long taskId) throws SQLException {
        String sql = "UPDATE Tasks SET missed_count = missed_count + 1 WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, taskId);
            pstmt.executeUpdate();
        }
    }

    private void logDecision(long taskId, long userId, LocalDateTime oldTime, LocalDateTime newTime, String reason, String factors) throws SQLException {
        String logSql = "INSERT INTO RescheduleLogs (task_id, user_id, old_time, new_time, reason, factors) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(logSql)) {
            pstmt.setLong(1, taskId);
            pstmt.setLong(2, userId);
            pstmt.setObject(3, oldTime);
            pstmt.setObject(4, newTime);
            pstmt.setString(5, reason);
            pstmt.setString(6, factors);
            pstmt.executeUpdate();
        }
    }
}
