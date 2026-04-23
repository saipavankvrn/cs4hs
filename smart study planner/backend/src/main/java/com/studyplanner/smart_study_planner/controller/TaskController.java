package com.studyplanner.smart_study_planner.controller;

import com.studyplanner.smart_study_planner.model.Activity;
import com.studyplanner.smart_study_planner.model.Task;
import com.studyplanner.smart_study_planner.model.User;
import com.studyplanner.smart_study_planner.repository.ActivityRepository;
import com.studyplanner.smart_study_planner.repository.TaskRepository;
import com.studyplanner.smart_study_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @GetMapping
    public String showTaskList(Model model) {
        User user = getLoggedInUser();
        List<Task> tasks = taskRepository.findByUserOrderByDueDateAsc(user);
        model.addAttribute("tasks", tasks);
        return "todo-list";
    }

    @PostMapping("/add")
    public String addTask(@RequestParam("title") String title,
                          @RequestParam("description") String description,
                          @RequestParam("dueDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate) {
        User user = getLoggedInUser();
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.setUser(user);
        task.setCompleted(false);
        taskRepository.save(task);

        // Record Activity
        Activity activity = new Activity();
        activity.setDescription("Added new task: " + title);
        activity.setActivityType("TASK_CREATED");
        activity.setUser(user);
        activityRepository.save(activity);

        return "redirect:/tasks";
    }

    @PostMapping("/complete/{id}")
    @ResponseBody
    public ResponseEntity<?> completeTask(@PathVariable("id") Long id) {
        User user = getLoggedInUser();
        Optional<Task> taskOpt = taskRepository.findById(id);

        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (task.getUser().getId().equals(user.getId())) {
                task.setCompleted(true);
                taskRepository.save(task);

                // Record Activity
                Activity activity = new Activity();
                activity.setDescription("Completed task: " + task.getTitle());
                activity.setActivityType("TASK_COMPLETED");
                activity.setUser(user);
                activityRepository.save(activity);

                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/delete/{id}")
    public String deleteTask(@PathVariable("id") Long id) {
        User user = getLoggedInUser();
        Optional<Task> taskOpt = taskRepository.findById(id);

        if (taskOpt.isPresent() && taskOpt.get().getUser().getId().equals(user.getId())) {
            taskRepository.deleteById(id);
        }
        return "redirect:/tasks";
    }

    private User getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
