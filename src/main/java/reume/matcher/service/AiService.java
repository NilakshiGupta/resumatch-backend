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

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String analyzeResume(String resumeText, String jobDescription) throws Exception {
        String resume = cap(resumeText, 4000);

        String prompt =
                "You are an expert ATS analyst. Analyze the resume against the job description.\n" +
                        "Return ONLY valid JSON, no markdown:\n" +
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
                        "industry must be ONE of: SOFTWARE_ENGINEERING | DATA_SCIENCE | DEVOPS | FRONTEND | PRODUCT_MANAGEMENT\n\n" +
                        "RESUME:\n" + resume + "\n\n" +
                        "JOB DESCRIPTION:\n" + jobDescription;

        return callGroq(prompt, 0.3, 1024);
    }

    public String generateCoverLetter(String resumeText, String jobDescription,
                                      String companyName, String jobTitle) throws Exception {
        String resume = cap(resumeText, 4000);

        String prompt =
                "Write a professional cover letter.\n" +
                        "Company: " + companyName + "\n" +
                        "Job Title: " + jobTitle + "\n" +
                        "Job Description:\n" + jobDescription + "\n\n" +
                        "Candidate Resume:\n" + resume + "\n\n" +
                        "Write 3-4 paragraphs. Return ONLY the letter text.";

        return callGroq(prompt, 0.7, 1024);
    }

    /**
     * Tailored resume generation — now includes projects, certifications,
     * achievements, and links sections.
     */
    public String generateTailoredResume(String resumeText, String jobDescription) throws Exception {
        String fullResume = cap(resumeText, 10000);

        String prompt =
                "You are a world-class ATS resume optimizer. Your ONLY goal is to tailor " +
                        "the candidate's resume to achieve MAXIMUM ATS score for the given job description.\n\n" +

                        "=== PERSONAL DETAILS EXTRACTION — MANDATORY ===\n\n" +

                        "NAME: Take the VERY FIRST non-empty line of resume as name.\n\n" +

                        "EMAIL: Find any token containing '@' — that is the email.\n\n" +

                        "PHONE: Find 10-digit number or +91 prefixed number.\n\n" +

                        "LINKEDIN: Find text containing 'linkedin.com' — extract as-is including path.\n\n" +

                        "GITHUB: Find text containing 'github.com' — extract as-is including path.\n\n" +

                        "=== ATS OPTIMIZATION — MANDATORY RULES ===\n\n" +

                        "1. SKILLS: List JD's exact keywords FIRST, then candidate's other skills.\n" +
                        "2. SUMMARY: Must contain at least 5 exact phrases from the JD. Make it 3 sentences.\n" +
                        "3. EXPERIENCE: Rewrite each bullet point to include JD keywords naturally.\n" +
                        "   - Keep all original numbers/metrics (%, counts, scale).\n" +
                        "   - Add JD technologies where candidate has used similar ones.\n" +
                        "4. PROJECTS: Rewrite descriptions to highlight JD-relevant technologies.\n" +
                        "5. JOB TITLE: Use EXACT job title from JD — one title only, no slashes.\n\n" +

                        "=== EDUCATION EXTRACTION ===\n\n" +

                        "- Find lines with: university, college, institute, iit, nit, b.tech, mca, bca, mba, b.sc, m.tech\n" +
                        "- Extract degree, full college name, and graduation year.\n" +
                        "- NEVER write 'Not specified' — use empty string if not found.\n\n" +

                        "=== CERTIFICATIONS ===\n" +
                        "- Extract all certifications from resume.\n" +
                        "- Add JD-relevant certifications the candidate likely has based on their skills.\n\n" +

                        "=== OUTPUT — STRICT JSON ONLY ===\n" +
                        "No markdown, no explanation, no ```json. Raw JSON only:\n" +
                        "{\n" +
                        "  \"name\": \"Full Name\",\n" +
                        "  \"email\": \"email@example.com\",\n" +
                        "  \"phone\": \"+91-XXXXXXXXXX\",\n" +
                        "  \"linkedin\": \"linkedin.com/in/username\",\n" +
                        "  \"github\": \"github.com/username\",\n" +
                        "  \"jobTitle\": \"Exact Job Title From JD\",\n" +
                        "  \"summary\": \"3 sentences with JD keywords packed in\",\n" +
                        "  \"skills\": [\"JD Skill 1\", \"JD Skill 2\", \"Candidate Skill 3\"],\n" +
                        "  \"experience\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Job Title\",\n" +
                        "      \"company\": \"Company Name\",\n" +
                        "      \"duration\": \"Mon YYYY - Mon YYYY\",\n" +
                        "      \"points\": [\n" +
                        "        \"Rewritten bullet with JD keywords and original metrics\"\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"projects\": [\n" +
                        "    {\n" +
                        "      \"name\": \"Project Name\",\n" +
                        "      \"description\": \"JD-optimized description with impact metrics\",\n" +
                        "      \"techStack\": [\"Tech1\", \"Tech2\"]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"certifications\": [\n" +
                        "    {\"name\": \"Cert Name\", \"issuer\": \"Issuer\"}\n" +
                        "  ],\n" +
                        "  \"achievements\": [\n" +
                        "    \"Achievement 1\"\n" +
                        "  ],\n" +
                        "  \"education\": [\n" +
                        "    {\n" +
                        "      \"degree\": \"Degree\",\n" +
                        "      \"college\": \"Full College Name\",\n" +
                        "      \"year\": \"YYYY\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +
                        "=== RESUME ===\n" + fullResume + "\n\n" +
                        "=== JOB DESCRIPTION ===\n" + jobDescription;

        return callGroq(prompt, 0.1, 4096);
    }

    private String cap(String text, int limit) {
        return text != null && text.length() > limit ? text.substring(0, limit) : text;
    }

    private String callGroq(String prompt, double temperature, int maxTokens) throws Exception {
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
                "You are a precise JSON-only assistant. " +
                        "Output ONLY a raw JSON object — no markdown, no ```json, no text before or after. " +
                        "Follow all field-level instructions exactly. Never output 'Not specified'.");

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(system);
        messages.add(user);

        JsonObject body = new JsonObject();
        body.addProperty("model", "llama-3.3-70b-versatile");
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temperature);
        body.add("messages", messages);

        HttpClient client   = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("GROQ RESPONSE: " + response.body());

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (json.has("error")) {
            throw new RuntimeException("Groq API Error: " +
                    json.getAsJsonObject("error").get("message").getAsString());
        }

        return json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}