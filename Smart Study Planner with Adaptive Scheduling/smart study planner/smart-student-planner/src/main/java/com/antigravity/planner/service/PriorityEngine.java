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
     * Advanced adaptive priority formula for Senior Engineer requirements.
     * Factors: Deadline Weight, Missed Count (x10), Confidence Penalty (x8), Time Decay, and Time Drift.
     */
    public double calculatePriority(LocalDateTime deadline, int missedCount, int confidence, double drift, LocalDateTime lastStudied) {
        int boundedConfidence = Math.max(1, Math.min(5, confidence));
        long hoursRemaining = ChronoUnit.HOURS.between(LocalDateTime.now(), deadline);
        double deadlineWeight = getDeadlineWeight(hoursRemaining);

        // Time Decay: Boost priority if the task hasn't been touched in a while
        double timeDecay = 0;
        if (lastStudied != null) {
            long daysSinceStudy = ChronoUnit.DAYS.between(lastStudied, LocalDateTime.now());
            timeDecay = Math.min(30.0, daysSinceStudy * 2.5); // Max 30 points for 'Starvation'
        }

        // Drift Sensitivity: High positive drift means user takes LONGER than planned
        // We increase priority to ensure these 'Heavy' tasks get earlier slots
        double driftFactor = Math.abs(drift) * 0.4;

        // Core Rule-Based Score
        return (deadlineWeight * 1.5) + (missedCount * 10.0) + ((5 - boundedConfidence) * 8.0) + timeDecay + driftFactor;
    }

    /**
     * Senior Deadline Curve: Exponential weight as deadline nears.
     */
    private double getDeadlineWeight(long hoursRemaining) {
        if (hoursRemaining <= 0) return 150.0; // Overdue
        if (hoursRemaining <= 6) return 110.0;
        if (hoursRemaining <= 24) return 80.0;
        if (hoursRemaining <= 72) return 40.0;
        return 10.0;
    }
}
