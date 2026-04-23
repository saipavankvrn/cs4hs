package com.studyplanner.smart_study_planner.controller;

import com.studyplanner.smart_study_planner.model.Activity;
import com.studyplanner.smart_study_planner.model.Document;
import com.studyplanner.smart_study_planner.model.Subject;
import com.studyplanner.smart_study_planner.model.User;
import com.studyplanner.smart_study_planner.repository.ActivityRepository;
import com.studyplanner.smart_study_planner.repository.DocumentRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Value("${smartplanner.upload-dir}")
    private String uploadDir;

    @GetMapping("/documents")
    public String listDocuments(Model model) {
        User user = getLoggedInUser();
        model.addAttribute("documents", documentRepository.findByUser(user));
        model.addAttribute("subjects", subjectRepository.findByUser(user));
        return "documents";
    }

    @PostMapping("/documents/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                 @RequestParam("subjectId") Long subjectId) throws IOException {
        if (file.isEmpty()) {
            return "redirect:/documents?error=empty_file";
        }

        User user = getLoggedInUser();
        Subject subject = subjectRepository.findById(subjectId).orElse(null);

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file physically
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // Save metadata to database
        Document doc = new Document();
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(fileName); // Store the generated filename
        doc.setFileType(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1));
        doc.setSubject(subject);
        doc.setUser(user);
        documentRepository.save(doc);

        // Log activity
        Activity activity = new Activity();
        activity.setDescription("Uploaded " + doc.getFileName() + (subject != null ? " to " + subject.getName() : ""));
        activity.setActivityType("UPLOAD");
        activity.setUser(user);
        activityRepository.save(activity);

        return "redirect:/documents?success=uploaded";
    }

    @PostMapping("/documents/delete/{id}")
    public String deleteDocument(@PathVariable("id") Long id) {
        User user = getLoggedInUser();
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc != null && doc.getUser().getId().equals(user.getId())) {
            // Delete from disk
            try {
                Path filePath = Paths.get(uploadDir).resolve(doc.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Log activity
            Activity activity = new Activity();
            activity.setDescription("Deleted document: " + doc.getFileName());
            activity.setActivityType("DELETE");
            activity.setUser(user);
            activityRepository.save(activity);

            // Delete from database
            documentRepository.delete(doc);
        }
        return "redirect:/documents?success=deleted";
    }

    private User getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
