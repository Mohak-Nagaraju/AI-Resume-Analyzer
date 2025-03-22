package com.example.controller;

import com.example.service.PDFTextExtractor;
import com.example.service.GeminiAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class ResumeController {

    @Autowired
    private PDFTextExtractor pdfTextExtractor;

    @Autowired
    private GeminiAIService geminiAIService;

    private final Path uploadDir = Paths.get("uploads");

    public ResumeController() {
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    @GetMapping("/")
    public String showHomePage() {
        return "index";
    }

    @PostMapping("/analyze")
    public String analyzeResume(@RequestParam("resume") MultipartFile resumeFile,
                                @RequestParam("jobDescription") String jobDescription,
                                @RequestParam("action") String action,
                                Model model) {
        if (resumeFile.isEmpty()) {
            model.addAttribute("error", "Please upload a resume PDF file");
            return "index";
        }
        // In the analyzeResume method, add this line:
        model.addAttribute("actionType", action);

        try {
            // Save the uploaded file
            String filename = UUID.randomUUID().toString() + "_" + resumeFile.getOriginalFilename();
            Path filePath = uploadDir.resolve(filename);
            Files.copy(resumeFile.getInputStream(), filePath);

            // Extract text from PDF
            String resumeText = pdfTextExtractor.extractText(filePath.toFile());

            // Process based on action
            String result;
            if ("match".equals(action)) {
                result = geminiAIService.matchResume(resumeText, jobDescription);
            } else {
                result = geminiAIService.analyzeResume(resumeText, jobDescription);
            }

            // Convert Markdown to HTML
            String htmlResult = convertMarkdownToHtml(result);

            // Clean up - delete the temporary file
            Files.deleteIfExists(filePath);

            // Add results to model
            model.addAttribute("analysis", htmlResult);
            model.addAttribute("jobDescription", jobDescription);
            return "result";

        } catch (Exception e) {
            model.addAttribute("error", "Error processing resume: " + e.getMessage());
            return "index";
        }
    }
    // Helper method to convert Markdown to HTML
    private String convertMarkdownToHtml(String markdown) {
        //markdown = markdown.replaceAll("\n{2,}", "\n"); // Reduce extra newlines
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }
}