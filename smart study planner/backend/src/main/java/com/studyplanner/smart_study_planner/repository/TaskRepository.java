package com.studyplanner.smart_study_planner.repository;

import com.studyplanner.smart_study_planner.model.Task;
import com.studyplanner.smart_study_planner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserAndIsCompleted(User user, boolean isCompleted);
    List<Task> findByUserOrderByDueDateAsc(User user);
    long countByUserAndIsCompleted(User user, boolean isCompleted);
}
