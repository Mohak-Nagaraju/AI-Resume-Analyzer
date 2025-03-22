package com.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    public GeminiAIService() {
        this.restTemplate = new RestTemplate();
    }

    public String analyzeResume(String resumeText, String jobDescription) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return "Error: Resume text is required for analysis.";
        }

        try {
            // Prepare the prompt
            String basePrompt = "You are an experienced Technical Human Resource Manager,your task is to review the provided resume against the job description. \n" +
                    "  Please share your professional evaluation on whether the candidate's profile aligns with the role. \n" +
                    " Highlight the strengths and weaknesses of the applicant in relation to the specified job requirements. Also mention Skills they already have and suggest some skills to improve their resume, also suggest some courses they might take to improve the skills.\n\n"
                    + "Resume:\n" + resumeText;

            if (jobDescription != null && !jobDescription.trim().isEmpty()) {
                basePrompt += "\n\nJob Description:\n" + jobDescription;
            }

            // Construct the request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();

            parts.put("text", basePrompt);
            contents.put("parts", new Object[]{parts});
            requestBody.put("contents", new Object[]{contents});

            // Set request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            // Create and send the request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String fullUrl = geminiApiUrl + "?key=" + apiKey;

            ResponseEntity<Map> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Process the response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map responseBody = response.getBody();

                // Extract the generated text from the response
                try {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                        List<Map<String, Object>> parts2 = (List<Map<String, Object>>) content.get("parts");
                        if (parts2 != null && !parts2.isEmpty()) {
                            return (String) parts2.get(0).get("text");
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error parsing Gemini response: " + e.getMessage());
                }
            }

            return "Unable to analyze the resume. Please try again later.";

        } catch (Exception e) {
            return "Error analyzing resume: " + e.getMessage();
        }
    }
    public String matchResume(String resumeText, String jobDescription) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return "Error: Resume text is required for analysis.";
        }

        try {
            // Match prompt
            String matchPrompt = """
            You are a skilled ATS (Applicant Tracking System) scanner with a deep understanding of computer science and ATS functionality.
            Your task is to evaluate the resume against the provided job description.
            Give me the percentage of match if the resume matches the job description.
            First, the output should come as a percentage, then keywords missing (like technical or soft skills), and lastly, final thoughts.
            """ + "\n\nResume:\n" + resumeText + "\n\nJob Description:\n" + jobDescription;

            return sendRequestToGemini(matchPrompt);

        } catch (Exception e) {
            return "Error matching resume: " + e.getMessage();
        }
    }
    private String sendRequestToGemini(String prompt) {
        try {
            // Construct the request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();

            parts.put("text", prompt);
            contents.put("parts", new Object[]{parts});
            requestBody.put("contents", new Object[]{contents});

            // Set request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            // Create and send the request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String fullUrl = geminiApiUrl + "?key=" + apiKey;

            ResponseEntity<Map> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Process the response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map responseBody = response.getBody();

                // Extract the generated text from the response
                try {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                        List<Map<String, Object>> parts2 = (List<Map<String, Object>>) content.get("parts");
                        if (parts2 != null && !parts2.isEmpty()) {
                            return (String) parts2.get(0).get("text");
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error parsing Gemini response: " + e.getMessage());
                }
            }

            return "Unable to process request. Please try again later.";

        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

}