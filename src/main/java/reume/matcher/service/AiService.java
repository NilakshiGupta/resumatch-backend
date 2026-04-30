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

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    public String analyzeResume(String resumeText, String jobDescription) throws Exception {

        String shortResume = resumeText.length() > 2000 ? resumeText.substring(0, 2000) : resumeText;

        String prompt = "You are an ATS expert. Analyze the resume against the job description.\n" +
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

        // Build request body (OpenAI-compatible format)
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are an ATS expert. Always respond with valid JSON only.");

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "llama-3.3-70b-versatile");
        requestBody.addProperty("max_tokens", 1024);
        requestBody.addProperty("temperature", 0.3);
        requestBody.add("messages", messages);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("FULL RESPONSE: " + response.body());

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

        if (jsonResponse.has("error")) {
            throw new RuntimeException("Groq API Error: " +
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