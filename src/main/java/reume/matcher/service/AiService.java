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
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

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
        System.out.println("FULL RESPONSE: " + response.body());

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