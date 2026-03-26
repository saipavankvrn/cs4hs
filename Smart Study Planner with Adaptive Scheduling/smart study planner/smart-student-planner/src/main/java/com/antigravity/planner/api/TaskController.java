package com.antigravity.planner.api;

import com.antigravity.planner.service.PriorityEngine;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final DataSource dataSource;
    private final PriorityEngine priorityEngine = new PriorityEngine();

    public TaskController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping
    public Map<String, Object> createTask(@RequestBody Map<String, Object> payload) {
        long userId = ((Number) payload.get("userId")).longValue();
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        LocalDateTime deadline = LocalDateTime.parse((String) payload.get("deadline"));
        int confidence = payload.containsKey("confidenceScore") ? ((Number) payload.get("confidenceScore")).intValue() : 3;

        // Adaptive: Initial priority with null lastStudied and 0 drift
        double priorityScore = priorityEngine.calculatePriority(deadline, 0, confidence, 0.0, null);

        String sql = "INSERT INTO Tasks (user_id, title, description, deadline, priority_score, missed_count, time_drift) VALUES (?, ?, ?, ?, ?, 0, 0.0)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, userId);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.setObject(4, deadline);
            pstmt.setDouble(5, priorityScore);
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return Map.of("status", "success", "taskId", rs.getLong(1), "priorityScore", priorityScore);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
        return Map.of("status", "failed");
    }

    @GetMapping
    public List<Map<String, Object>> getTasks(@RequestParam("userId") long userId) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String sql = "SELECT * FROM Tasks WHERE user_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> task = new HashMap<>();
                    task.put("id", rs.getLong("id"));
                    task.put("title", rs.getString("title"));
                    task.put("description", rs.getString("description"));
                    task.put("deadline", rs.getObject("deadline"));
                    task.put("priorityScore", rs.getDouble("priority_score"));
                    task.put("missedCount", rs.getInt("missed_count"));
                    task.put("timeDrift", rs.getDouble("time_drift"));
                    task.put("lastStudied", rs.getObject("last_studied_time"));
                    task.put("avgDuration", rs.getInt("avg_session_duration"));
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    @PostMapping("/recalculate")
    public Map<String, String> recalculatePriorities(@RequestParam("userId") long userId) {
        String fetchAll = "SELECT * FROM Tasks WHERE user_id = ?";
        String updateTask = "UPDATE Tasks SET priority_score = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement fetchPstmt = conn.prepareStatement(fetchAll);
             PreparedStatement updatePstmt = conn.prepareStatement(updateTask)) {
            
            fetchPstmt.setLong(1, userId);
            try (ResultSet rs = fetchPstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    LocalDateTime deadline = rs.getObject("deadline", LocalDateTime.class);
                    int missed = rs.getInt("missed_count");
                    double drift = rs.getDouble("time_drift");
                    LocalDateTime lastStudied = rs.getObject("last_studied_time", LocalDateTime.class);
                    
                    // Confidence is 3 by default for batch recalc
                    double newScore = priorityEngine.calculatePriority(deadline, missed, 3, drift, lastStudied);
                    
                    updatePstmt.setDouble(1, newScore);
                    updatePstmt.setLong(2, id);
                    updatePstmt.executeUpdate();
                }
            }
            return Map.of("status", "success", "message", "Adaptive priorities refreshed.");
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteTask(@PathVariable long id) {
        String sql = "DELETE FROM Tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            return Map.of("status", "success");
        } catch (SQLException e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
