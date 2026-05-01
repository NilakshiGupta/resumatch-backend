package reume.matcher.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public String analyzeResume(String resumeText, String jobDescription) throws Exception {

        String shortResume = resumeText.length() > 2000 ? resumeText.substring(0, 2000) : resumeText;

        String prompt = "You are an ATS (Applicant Tracking System) expert. Analyze the resume against the job description.\n" +
                "Return ONLY a valid JSON object, no markdown, no extra text:\n" +
                "{\n" +
                "  \"matchPercentage\": 75,\n" +
                "  \"atsScore\": 68,\n" +
                "  \"jobTitle\": \"Software Engineer\",\n" +
                "  \"industry\": \"SOFTWARE_ENGINEERING\",\n" +
                "  \"missingKeywords\": \"Docker, Kubernetes, AWS\",\n" +
                "  \"matchedKeywords\": \"Java, Spring Boot, REST API\",\n" +
                "  \"suggestions\": \"Add cloud platform experience.\",\n" +
                "  \"improvementTips\": \"1. Quantify achievements. 2. Add certifications.\",\n" +
                "  \"experienceGap\": \"Job needs 5 years, resume shows 2 years.\",\n" +
                "  \"skillsGap\": \"Missing: Docker, AWS, Kubernetes\"\n" +
                "}\n" +
                "industry must be one of: SOFTWARE_ENGINEERING, DATA_SCIENCE, DEVOPS, FRONTEND, PRODUCT_MANAGEMENT\n" +
                "Resume: " + shortResume + "\n" +
                "Job Description: " + jobDescription;

        return callGemini(prompt);
    }

    public String generateTailoredResume(String resumeText, String jobDescription) throws Exception {

        String shortResume = resumeText.length() > 2000 ? resumeText.substring(0, 2000) : resumeText;

        String prompt = "You are an expert resume writer. Create a 100% ATS-optimized tailored resume.\n" +
                "Return ONLY a valid JSON object, no markdown, no extra text:\n" +
                "{\n" +
                "  \"name\": \"Candidate Name\",\n" +
                "  \"email\": \"email@example.com\",\n" +
                "  \"phone\": \"+91 XXXXXXXXXX\",\n" +
                "  \"location\": \"City, Country\",\n" +
                "  \"linkedin\": \"linkedin.com/in/profile\",\n" +
                "  \"summary\": \"2-3 line professional summary tailored to job\",\n" +
                "  \"skills\": [\"Skill1\", \"Skill2\", \"Skill3\"],\n" +
                "  \"experience\": [\n" +
                "    {\"title\": \"Job Title\", \"company\": \"Company\", \"duration\": \"Jan 2022 - Present\", \"points\": [\"Achievement 1 with metrics\", \"Achievement 2\"]}\n" +
                "  ],\n" +
                "  \"education\": [\n" +
                "    {\"degree\": \"B.Tech CSE\", \"institution\": \"University\", \"year\": \"2023\", \"gpa\": \"8.5\"}\n" +
                "  ],\n" +
                "  \"certifications\": [\"Cert 1\", \"Cert 2\"],\n" +
                "  \"jobTitle\": \"Target Job Title\"\n" +
                "}\n" +
                "IMPORTANT: Use keywords from job description. Quantify achievements. Make it ATS-friendly.\n" +
                "Original Resume: " + shortResume + "\n" +
                "Job Description: " + jobDescription;

        return callGemini(prompt);
    }

    private String callGemini(String prompt) throws Exception {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        String url = GEMINI_URL + "?key=" + apiKey;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

        if (jsonResponse.has("error")) {
            throw new RuntimeException("AI API Error: " +
                    jsonResponse.getAsJsonObject("error").get("message").getAsString());
        }

        String text = jsonResponse
                .getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

        return text.replace("```json", "").replace("```", "").trim();
    }
}