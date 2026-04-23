package com.studyplanner.smart_study_planner.controller;

import com.studyplanner.smart_study_planner.model.User;
import com.studyplanner.smart_study_planner.repository.ActivityRepository;
import com.studyplanner.smart_study_planner.repository.TaskRepository;
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
    private TaskRepository taskRepository;

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
        if (user != null) {
            model.addAttribute("totalSubjects", user.getSubjects().size());
            model.addAttribute("totalDocuments", user.getDocuments().size());
            model.addAttribute("totalPendingTasks", taskRepository.countByUserAndIsCompleted(user, false));
            model.addAttribute("recentActivities", activityRepository.findTop7ByUserOrderByTimestampDesc(user));
            
            // Upcoming tasks (due in the next 24 hours, not completed)
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime next24Hours = now.plusHours(24);
            model.addAttribute("upcomingTasks", taskRepository.findByUserAndIsCompleted(user, false).stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isAfter(now) && t.getDueDate().isBefore(next24Hours))
                .collect(java.util.stream.Collectors.toList()));
            
            model.addAttribute("user", user);
        }
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = getLoggedInUser();
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User updatedUser) {
        User user = getLoggedInUser();
        if (user != null) {
            try {
                user.setUsername(updatedUser.getUsername());
                user.setEmail(updatedUser.getEmail());
                userRepository.save(user);
                return "redirect:/profile?success";
            } catch (Exception e) {
                return "redirect:/profile?error=username_taken";
            }
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String updatePassword(@org.springframework.web.bind.annotation.RequestParam String oldPassword, 
                                 @org.springframework.web.bind.annotation.RequestParam String newPassword, 
                                 @org.springframework.web.bind.annotation.RequestParam String confirmPassword) {
        User user = getLoggedInUser();
        if (user != null) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return "redirect:/profile?error=wrong_password";
            }
            if (!newPassword.equals(confirmPassword)) {
                return "redirect:/profile?error=mismatch";
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
        return "redirect:/profile?pw_success";
    }

    private User getLoggedInUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
