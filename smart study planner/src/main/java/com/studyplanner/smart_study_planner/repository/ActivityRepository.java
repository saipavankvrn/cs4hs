package com.studyplanner.smart_study_planner.repository;

import com.studyplanner.smart_study_planner.model.Activity;
import com.studyplanner.smart_study_planner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByUserOrderByTimestampDesc(User user);
}
