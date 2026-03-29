package com.studyplanner.smart_study_planner.controller;

import com.studyplanner.smart_study_planner.model.Activity;
import com.studyplanner.smart_study_planner.model.Subject;
import com.studyplanner.smart_study_planner.model.User;
import com.studyplanner.smart_study_planner.repository.ActivityRepository;
import com.studyplanner.smart_study_planner.repository.SubjectRepository;
import com.studyplanner.smart_study_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @GetMapping("/subjects")
    public String listSubjects(Model model) {
        User user = getLoggedInUser();
        model.addAttribute("subjects", subjectRepository.findByUser(user));
        model.addAttribute("newSubject", new Subject());
        return "subjects";
    }

    @PostMapping("/subjects/create")
    public String createSubject(@ModelAttribute Subject subject) {
        User user = getLoggedInUser();
        subject.setUser(user);
        subjectRepository.save(subject);

        // Record Activity
        Activity activity = new Activity();
        activity.setDescription("Added new subject: " + subject.getName());
        activity.setActivityType("SUBJECT_ADDED");
        activity.setUser(user);
        activityRepository.save(activity);

        return "redirect:/subjects";
    }

    private User getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
