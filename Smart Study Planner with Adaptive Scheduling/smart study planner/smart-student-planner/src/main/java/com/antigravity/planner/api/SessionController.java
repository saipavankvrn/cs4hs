package com.antigravity.planner.api;

import com.antigravity.planner.service.FocusSessionService;
import com.antigravity.planner.service.SchedulerService;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
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
        try (Connection conn = dataSource.getConnection()) {
            // Adaptive Engineering Pattern: Services instantiated per session request to maintain transactional state
            SchedulerService scheduler = new SchedulerService(conn);
            FocusSessionService service = new FocusSessionService(conn, scheduler);

            service.finalizeSession(
                ((Number) payload.get("userId")).longValue(),
                ((Number) payload.get("taskId")).longValue(),
                ((Number) payload.get("scheduleId")).longValue(),
                ((Number) payload.get("focusScore")).intValue(),
                ((Number) payload.get("confidenceScore")).intValue(),
                (String) payload.getOrDefault("status", "COMPLETED"),
                ((Number) payload.getOrDefault("plannedDuration", 30)).intValue(),
                ((Number) payload.getOrDefault("actualDuration", 30)).intValue(),
                (String) payload.getOrDefault("notes", "")
            );

            return Map.of("status", "success", "message", "Adaptive Session Sync Complete via FocusSessionService.");
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
