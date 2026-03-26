package com.antigravity.planner.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class PriorityEngine {

    /**
     * Calculates the dynamic priority of a task based on the provided formula.
     * Formula: (DeadlineWeight * 1.5) + (MissedCount * 10) + ((5 - ConfidenceScore) * 8)
     * 
     * @param deadline The exact deadline of the task.
     * @param missedCount Number of times the task was missed.
     * @param confidenceScore The student's estimated confidence on the task material (1 to 5).
     * @return The calculated priority score double.
     */
    /**
     * Calculates the dynamic priority of a task based on the provided formula.
     * Formula: (DeadlineWeight * 1.5) + (MissedCount * 10) + ((5 - ConfidenceScore) * 8)
     * 
     * @param deadline The exact deadline of the task.
     * @param missedCount Number of times the task was missed.
     * @param confidenceScore The student's estimated confidence on the task material (1 to 5).
     * @return The calculated priority score double.
     */
    public double calculatePriority(LocalDateTime deadline, int missedCount, int confidenceScore) {
        // Enforce boundary constraints for ConfidenceScore (1 to 5)
        int boundedConfidence = Math.max(1, Math.min(5, confidenceScore));
        
        // Define DeadlineWeight (e.g., inversely proportional to remaining hours)
        long hoursRemaining = ChronoUnit.HOURS.between(LocalDateTime.now(), deadline);
        double deadlineWeight = getDeadlineWeight(hoursRemaining);
        
        // Core Algorithm
        return (deadlineWeight * 1.5) + (missedCount * 10.0) + ((5 - boundedConfidence) * 8.0);
    }

    /**
     * Helper to determine Deadline Weight.
     * Approaching deadlines generate exponentially higher weights.
     */
    private double getDeadlineWeight(long hoursRemaining) {
        if (hoursRemaining <= 0) {
            return 100.0; // Overdue items get maximum weight
        } else if (hoursRemaining <= 24) {
            return 80.0;  // High weight for tasks due tomorrow
        } else if (hoursRemaining <= 72) {
            return 50.0;  // Medium weight for 3-day tasks
        }
        return 10.0;      // Low baseline weight
    }
}
