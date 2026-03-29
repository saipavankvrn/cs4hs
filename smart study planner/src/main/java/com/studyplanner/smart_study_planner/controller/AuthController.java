package com.studyplanner.smart_study_planner.controller;

import com.studyplanner.smart_study_planner.model.User;
import com.studyplanner.smart_study_planner.repository.ActivityRepository;
import com.studyplanner.smart_study_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String register(@ModelAttribute User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return "redirect:/signup?error=username_taken";
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);
        
        return "redirect:/login?signup_success";
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        User user = getLoggedInUser();
        // Placeholder values for the dashboard statistics
        model.addAttribute("totalSubjects", user != null ? user.getSubjects().size() : 0);
        model.addAttribute("totalDocuments", user != null ? user.getDocuments().size() : 0);
        model.addAttribute("recentActivities", user != null ? activityRepository.findByUserOrderByTimestampDesc(user) : null);
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = getLoggedInUser();
        model.addAttribute("user", user);
        return "profile";
    }

    private User getLoggedInUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
