package com.studyplanner.smart_study_planner.repository;

import com.studyplanner.smart_study_planner.model.Document;
import com.studyplanner.smart_study_planner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUser(User user);
}
