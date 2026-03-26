package com.antigravity.planner.api;

import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final DataSource dataSource;

    public SessionController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/complete")
    public Map<String, String> completeSession(@RequestBody Map<String, Object> payload) {
        long userId = ((Number) payload.get("userId")).longValue();
        long taskId = ((Number) payload.get("taskId")).longValue();
        long scheduleId = ((Number) payload.get("scheduleId")).longValue();
        
        int focusScore = ((Number) payload.get("focusScore")).intValue();
        int confidenceScore = ((Number) payload.get("confidenceScore")).intValue();
        
        // Storing post-study notes explicitly via JDBC schema update
        String notes = (String) payload.getOrDefault("notes", "");

        String insertLogsSql = "INSERT INTO SessionLogs (user_id, task_id, schedule_id, focus_score, confidence_score, notes) VALUES (?, ?, ?, ?, ?, ?)";
        String completeScheduleSql = "UPDATE Schedule SET is_completed = TRUE WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            
            // Insert execution records bridging notes and parameters dynamically into Log table
            try (PreparedStatement pstmt = conn.prepareStatement(insertLogsSql)) {
                pstmt.setLong(1, userId);
                pstmt.setLong(2, taskId);
                pstmt.setLong(3, scheduleId);
                pstmt.setInt(4, focusScore);
                pstmt.setInt(5, confidenceScore);
                pstmt.setString(6, notes);
                pstmt.executeUpdate();
            }

            // Acknowledge schedule block as globally finalized
            try (PreparedStatement updateStmt = conn.prepareStatement(completeScheduleSql)) {
                updateStmt.setLong(1, scheduleId);
                updateStmt.executeUpdate();
            }

            return Map.of("status", "success", "message", "Session logged. Schedule explicitly marked COMPLETE.");
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
