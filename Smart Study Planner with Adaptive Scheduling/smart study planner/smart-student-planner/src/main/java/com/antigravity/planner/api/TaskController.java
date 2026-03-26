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
        // Defaulting to 3 if not provided
        int confidence = payload.containsKey("confidenceScore") ? ((Number) payload.get("confidenceScore")).intValue() : 3;

        // Dynamically determining priority based on incoming deadline and confidence logic
        double priorityScore = priorityEngine.calculatePriority(deadline, 0, confidence);

        String sql = "INSERT INTO Tasks (user_id, title, description, deadline, priority_score, missed_count) VALUES (?, ?, ?, ?, ?, 0)";
        
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
                    task.put("priorityScore", rs.getInt("priority_score"));
                    task.put("missedCount", rs.getInt("missed_count"));
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    @PutMapping("/{id}")
    public Map<String, String> updateTask(@PathVariable("id") long id, @RequestBody Map<String, Object> payload) {
        String sql = "UPDATE Tasks SET title = ?, description = ?, deadline = ?, priority_score = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, (String) payload.get("title"));
            pstmt.setString(2, (String) payload.get("description"));
            
            LocalDateTime deadline = LocalDateTime.parse((String) payload.get("deadline"));
            pstmt.setObject(3, deadline);
            
            // Recalculate priority automatically on task mutation
            int missedCount = payload.containsKey("missedCount") ? ((Number) payload.get("missedCount")).intValue() : 0;
            int confidence = payload.containsKey("confidenceScore") ? ((Number) payload.get("confidenceScore")).intValue() : 3;
            double priorityScore = priorityEngine.calculatePriority(deadline, missedCount, confidence);
            
            pstmt.setDouble(4, priorityScore);
            pstmt.setLong(5, id);
            pstmt.executeUpdate();
            
            return Map.of("status", "success", "newPriorityScore", String.valueOf(priorityScore));
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteTask(@PathVariable("id") long id) {
        String sql = "DELETE FROM Tasks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            return Map.of("status", "success");
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
