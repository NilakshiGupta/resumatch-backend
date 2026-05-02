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
                "You are an expert ATS resume writer. Your job is to tailor the given resume " +
                        "to exactly match the job description for maximum ATS score.\n\n" +

                        "=== STRICT EXTRACTION RULES — READ CAREFULLY ===\n\n" +

                        "NAME:\n" +
                        "- Take the VERY FIRST non-empty line of the resume text as the candidate name.\n\n" +

                        "EMAIL:\n" +
                        "- Search for any token containing '@' — that is the email.\n\n" +

                        "PHONE:\n" +
                        "- Search for 10-digit number sequences, or numbers prefixed with +91 or 91.\n\n" +

                        "LINKEDIN:\n" +
                        "- Search for any URL or text containing 'linkedin.com' or 'linkedin' — extract it as-is.\n" +
                        "- If not found, output empty string \"\".\n\n" +

                        "GITHUB:\n" +
                        "- Search for any URL or text containing 'github.com' or 'github' — extract it as-is.\n" +
                        "- If not found, output empty string \"\".\n\n" +

                        "COLLEGE / UNIVERSITY (MOST IMPORTANT — DO NOT SKIP):\n" +
                        "- Scan EVERY line of the resume text.\n" +
                        "- If ANY line contains one of these words (case-insensitive), that line IS the college name:\n" +
                        "  university, college, institute, iit, nit, polytechnic, school of, vidyalaya, academy\n" +
                        "- Also look for lines near degree keywords (B.Tech, MCA, BCA, MBA, B.Sc, M.Sc, B.E, M.Tech).\n" +
                        "- The college name is usually on the SAME line or the line IMMEDIATELY AFTER the degree.\n" +
                        "- NEVER output 'Not specified' for college. If you cannot find it, output an empty string \"\".\n\n" +

                        "DEGREE:\n" +
                        "- Extract: B.Tech / MCA / BCA / MBA / B.Sc / M.Sc / B.E / M.Tech etc.\n\n" +

                        "YEAR:\n" +
                        "- Find a 4-digit year (e.g. 2024, 2025, 2026) near the degree/college.\n\n" +

                        "JOB TITLE:\n" +
                        "- Output ONE single concise job title taken directly from the job description.\n" +
                        "- NEVER use slashes, NEVER combine multiple titles.\n" +
                        "- Example CORRECT: \"Full Stack Developer\"\n" +
                        "- Example WRONG:   \"Full Stack Developer / Software Engineer / Azure Developer\"\n\n" +

                        "PROJECTS (VERY IMPORTANT — extract ALL projects from resume):\n" +
                        "- Look for sections titled: Projects, Personal Projects, Academic Projects, Key Projects.\n" +
                        "- For each project extract: name, description (1-2 sentences with tech stack), techStack (array of technologies used).\n" +
                        "- Rewrite descriptions to include JD keywords where relevant.\n" +
                        "- If no projects found, output empty array [].\n\n" +

                        "CERTIFICATIONS:\n" +
                        "- Look for sections titled: Certifications, Certificates, Courses, Training.\n" +
                        "- For each: extract name and issuer (if mentioned).\n" +
                        "- If none found, output empty array [].\n\n" +

                        "ACHIEVEMENTS:\n" +
                        "- Look for sections titled: Achievements, Honors, Awards, Publications, Extra-Curricular.\n" +
                        "- Also look for IEEE papers, hackathon wins, contest rankings, etc.\n" +
                        "- Extract each as a single string.\n" +
                        "- If none found, output empty array [].\n\n" +

                        "=== ATS OPTIMIZATION RULES ===\n" +
                        "- Mirror EXACT keywords from the job description in skills and experience.\n" +
                        "- Rewrite summary using the JD's exact terminology and role requirements.\n" +
                        "- Add quantified achievements wherever possible (%, numbers, scale).\n" +
                        "- Include ALL technologies from the JD that the candidate has used.\n\n" +

                        "=== OUTPUT FORMAT ===\n" +
                        "Return ONLY raw JSON — no markdown fences, no explanation:\n" +
                        "{\n" +
                        "  \"name\": \"Candidate Full Name\",\n" +
                        "  \"email\": \"email@example.com\",\n" +
                        "  \"phone\": \"+91-XXXXXXXXXX\",\n" +
                        "  \"linkedin\": \"linkedin.com/in/username\",\n" +
                        "  \"github\": \"github.com/username\",\n" +
                        "  \"jobTitle\": \"Single Job Title From JD\",\n" +
                        "  \"summary\": \"2-3 sentences packed with JD keywords\",\n" +
                        "  \"skills\": [\"Skill1\", \"Skill2\"],\n" +
                        "  \"experience\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Job Title\",\n" +
                        "      \"company\": \"Company Name\",\n" +
                        "      \"duration\": \"Mon YYYY - Mon YYYY\",\n" +
                        "      \"points\": [\"Achievement 1\", \"Achievement 2\"]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"projects\": [\n" +
                        "    {\n" +
                        "      \"name\": \"Project Name\",\n" +
                        "      \"description\": \"What it does, technologies used, impact.\",\n" +
                        "      \"techStack\": [\"React\", \"Node.js\", \"MongoDB\"]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"certifications\": [\n" +
                        "    { \"name\": \"Certification Name\", \"issuer\": \"Issuing Organization\" }\n" +
                        "  ],\n" +
                        "  \"achievements\": [\n" +
                        "    \"Published IEEE paper on XYZ\",\n" +
                        "    \"Won Hackathon 2024\"\n" +
                        "  ],\n" +
                        "  \"education\": [\n" +
                        "    {\n" +
                        "      \"degree\": \"Degree Name\",\n" +
                        "      \"college\": \"Full University or College Name\",\n" +
                        "      \"year\": \"YYYY\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +

                        "=== RESUME TEXT ===\n" +
                        fullResume + "\n\n" +

                        "=== JOB DESCRIPTION ===\n" +
                        jobDescription;

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