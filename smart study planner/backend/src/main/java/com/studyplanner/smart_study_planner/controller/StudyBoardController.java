package com.studyplanner.smart_study_planner.controller;

import com.studyplanner.smart_study_planner.model.Activity;
import com.studyplanner.smart_study_planner.model.Subject;
import com.studyplanner.smart_study_planner.model.User;
import com.studyplanner.smart_study_planner.model.StudyCard;
import com.studyplanner.smart_study_planner.repository.ActivityRepository;
import com.studyplanner.smart_study_planner.repository.DocumentRepository;
import com.studyplanner.smart_study_planner.repository.StudyCardRepository;
import com.studyplanner.smart_study_planner.repository.SubjectRepository;
import com.studyplanner.smart_study_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class StudyBoardController {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudyCardRepository studyCardRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Value("${smartplanner.upload-dir}")
    private String uploadDir;

    @GetMapping("/subjects/{id}/board")
    public String showStudyBoard(@PathVariable("id") Long id, Model model) {
        User user = getLoggedInUser();
        Subject subject = subjectRepository.findById(id).orElse(null);
        
        if (subject == null || !subject.getUser().getId().equals(user.getId())) {
            return "redirect:/subjects";
        }

        model.addAttribute("subject", subject);
        model.addAttribute("documents", subject.getDocuments());
        model.addAttribute("studyCards", studyCardRepository.findBySubjectAndUser(subject, user));
        model.addAttribute("newCard", new StudyCard());
        return "study-board";
    }

    @PostMapping("/subjects/{id}/cards/add")
    public String addStudyCard(@PathVariable("id") Long id,
                               @RequestParam("title") String title,
                               @RequestParam("content") String content,
                               @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        User user = getLoggedInUser();
        Subject subject = subjectRepository.findById(id).orElse(null);

        if (subject == null || !subject.getUser().getId().equals(user.getId())) {
            return "redirect:/subjects";
        }

        StudyCard card = new StudyCard();
        card.setTitle(title);
        card.setContent(content);
        card.setSubject(subject);
        card.setUser(user);

        // Optional image upload
        if (image != null && !image.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
            Files.copy(image.getInputStream(), uploadPath.resolve(fileName));
            card.setImagePath(fileName);
        }

        studyCardRepository.save(card);

        // Record Activity
        Activity activity = new Activity();
        activity.setDescription("Added revision card: " + title + " to " + subject.getName());
        activity.setActivityType("STUDY_CARD_CREATED");
        activity.setUser(user);
        activityRepository.save(activity);

        return "redirect:/subjects/" + id + "/board";
    }

    @PostMapping("/subjects/{subjectId}/cards/edit/{cardId}")
    public String editStudyCard(@PathVariable("subjectId") Long subjectId,
                                @PathVariable("cardId") Long cardId,
                                @RequestParam("title") String title,
                                @RequestParam("content") String content,
                                @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        User user = getLoggedInUser();
        Subject subject = subjectRepository.findById(subjectId).orElse(null);
        StudyCard card = studyCardRepository.findById(cardId).orElse(null);

        if (subject != null && card != null && subject.getUser().getId().equals(user.getId()) && card.getSubject().getId().equals(subjectId)) {
            card.setTitle(title);
            card.setContent(content);

            if (image != null && !image.isEmpty()) {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                Files.copy(image.getInputStream(), uploadPath.resolve(fileName));
                card.setImagePath(fileName);
            }

            studyCardRepository.save(card);

            Activity activity = new Activity();
            activity.setDescription("Updated revision card: " + title);
            activity.setActivityType("STUDY_CARD_UPDATED");
            activity.setUser(user);
            activityRepository.save(activity);
        }
        return "redirect:/subjects/" + subjectId + "/board";
    }

    @PostMapping("/subjects/{subjectId}/cards/delete/{cardId}")
    public String deleteStudyCard(@PathVariable("subjectId") Long subjectId,
                                  @PathVariable("cardId") Long cardId) {
        User user = getLoggedInUser();
        Subject subject = subjectRepository.findById(subjectId).orElse(null);
        StudyCard card = studyCardRepository.findById(cardId).orElse(null);

        if (subject != null && card != null && subject.getUser().getId().equals(user.getId()) && card.getSubject().getId().equals(subjectId)) {
            studyCardRepository.delete(card);

            Activity activity = new Activity();
            activity.setDescription("Deleted revision card: " + card.getTitle());
            activity.setActivityType("STUDY_CARD_DELETED");
            activity.setUser(user);
            activityRepository.save(activity);
        }
        return "redirect:/subjects/" + subjectId + "/board";
    }

    private User getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
