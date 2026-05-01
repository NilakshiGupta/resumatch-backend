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

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public String analyzeResume(String resumeText, String jobDescription) throws Exception {
        String prompt = "Analyze this resume against the JD. Return ONLY JSON: {matchPercentage, atsScore, jobTitle, industry, missingKeywords, matchedKeywords, suggestions, improvementTips, experienceGap, skillsGap}. Resume: " + resumeText + " JD: " + jobDescription;
        return callGemini(prompt);
    }

    public String generateTailoredResume(String resumeText, String jobDescription) throws Exception {
        String prompt = "Tailor this resume for the JD. Return ONLY JSON: {name, email, phone, location, linkedin, summary, skills[], experience:[{title, company, duration, points[]}], education:[], certifications[], jobTitle}. Use JD keywords. Resume: " + resumeText + " JD: " + jobDescription;
        return callGemini(prompt);
    }

    public String generateCoverLetter(String resumeText, String jobDescription, String companyName, String jobTitle) throws Exception {
        String prompt = "Write a professional cover letter for " + jobTitle + " at " + companyName + ". Use this resume data: " + resumeText + " and JD: " + jobDescription + ". Return ONLY the text.";
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

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

        String text = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject()
                .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();

        return text.replace("```json", "").replace("```", "").trim();
    }
}