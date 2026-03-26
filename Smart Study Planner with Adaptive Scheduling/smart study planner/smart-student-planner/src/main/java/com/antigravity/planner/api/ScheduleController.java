package com.antigravity.planner.api;

import com.antigravity.planner.service.RescheduleService;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final DataSource dataSource;

    public ScheduleController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/adapt")
    public Map<String, String> adaptSchedule(@RequestBody Map<String, Object> payload) {
        long scheduleId = ((Number) payload.get("scheduleId")).longValue();
        long taskId = ((Number) payload.get("taskId")).longValue();
        long userId = ((Number) payload.get("userId")).longValue();

        try (Connection conn = dataSource.getConnection()) {
            // Trigger Antigravity rescheduler using pure JDBC driver layer
            RescheduleService rescheduleService = new RescheduleService(conn);
            rescheduleService.processMissedTask(scheduleId, taskId, userId);
            
            return Map.of("status", "success", "message", "Adaptive rescheduling module executed successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
