package reume.matcher.service;

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

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

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

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

        if (jsonResponse.has("error")) {
            throw new RuntimeException("AI API Error: " +
                    jsonResponse.getAsJsonObject("error").get("message").getAsString());
        }

        String text = jsonResponse
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        return text.replace("```json", "").replace("```", "").trim();
    }
}