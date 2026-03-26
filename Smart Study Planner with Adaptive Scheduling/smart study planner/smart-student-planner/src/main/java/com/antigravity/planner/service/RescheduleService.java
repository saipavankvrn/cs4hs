package com.antigravity.planner.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class RescheduleService {

    private final Connection connection;

    public RescheduleService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Resolves the state when a task is marked as "MISSED".
     *
     * @param scheduleId The ID of the missed schedule slot.
     * @param taskId     The ID of the task itself.
     * @param userId     The ID of the user owning the task.
     */
    public void processMissedTask(long scheduleId, long taskId, long userId) throws SQLException {
        // Increment the missed_count for the task
        int missedCount = incrementMissedCount(taskId);

        if (missedCount >= 2) {
            System.out.println("⚠️ Task missed >= 2 times! Engaging fail-safe: Splitting task.");
            splitAndRescheduleTask(taskId, userId);
        } else {
            System.out.println("⚠️ Task missed. Performing standard push-back.");
            // standard schedule bump handling
        }
    }

    private int incrementMissedCount(long taskId) throws SQLException {
        String updateTask = "UPDATE Tasks SET missed_count = missed_count + 1 WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateTask)) {
            pstmt.setLong(1, taskId);
            pstmt.executeUpdate();
        }

        String fetchCount = "SELECT missed_count FROM Tasks WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(fetchCount)) {
            pstmt.setLong(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("missed_count");
                }
            }
        }
        return 0;
    }

    private void splitAndRescheduleTask(long taskId, long userId) throws SQLException {
        // Splitting into TWO smaller sessions
        for (int i = 0; i < 2; i++) {
            LocalDateTime newSessionStart = findNextLowPrioritySlot(userId);
            
            if (newSessionStart != null) {
                // Assuming divided sub-session length is 30 mins
                LocalDateTime newSessionEnd = newSessionStart.plusMinutes(30);

                // Re-insert 2 smaller sessions
                String insertSQL = "INSERT INTO Schedule (user_id, task_id, start_time, end_time, is_completed) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                    insertStmt.setLong(1, userId);
                    insertStmt.setLong(2, taskId);
                    insertStmt.setObject(3, newSessionStart);
                    insertStmt.setObject(4, newSessionEnd);
                    insertStmt.setBoolean(5, false);
                    insertStmt.executeUpdate();
                }
                
                System.out.println("✅ Re-scheduled smaller session at: " + newSessionStart);
            }
        }
    }

    /**
     * Searches the MySQL Schedule table to find the next available 'Low Priority' slot.
     * This logic grabs an empty gap after the user's latest scheduled item, taking into 
     * account a 15-minute buffer requirement.
     * 
     * Alternatively, it can be configured to find existing slots where priority is weak.
     */
    private LocalDateTime findNextLowPrioritySlot(long userId) throws SQLException {
        // We find the latest scheduled slot's end time to append our fail-over blocks
        String findSlotSQL = "SELECT MAX(end_time) AS latest_end FROM Schedule WHERE user_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(findSlotSQL)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getObject("latest_end") != null) {
                    LocalDateTime latestEnd = rs.getObject("latest_end", LocalDateTime.class);
                    // Add 15-minute buffer before placing the next slot
                    return latestEnd.plusMinutes(15);
                }
            }
        }
        
        // Fallback: If no slots exist yet, start an hour from right now with a perfect buffer spacing
        return LocalDateTime.now().plusHours(1).withMinute(15).withSecond(0).withNano(0);
    }
}
