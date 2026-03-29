package com.studyplanner.smart_study_planner.repository;

import com.studyplanner.smart_study_planner.model.Subject;
import com.studyplanner.smart_study_planner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByUser(User user);
}
