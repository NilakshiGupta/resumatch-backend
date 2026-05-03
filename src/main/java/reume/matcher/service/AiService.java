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

    // ── ANALYZE ──────────────────────────────────────────────────────────────
    public String analyzeResume(String resumeText, String jobDescription) throws Exception {
        String resume = cap(resumeText, 4000);

        String prompt =
                "You are a senior ATS engineer at a top-tier MNC. Analyze this resume against the JD with zero sugarcoating.\n" +
                        "Score strictly — a fresher with 1 internship should NOT get above 55 ATS score at an MNC.\n\n" +
                        "Return ONLY valid raw JSON, no markdown:\n" +
                        "{\n" +
                        "  \"matchPercentage\": <0-100 integer — how well candidate matches JD requirements>,\n" +
                        "  \"atsScore\": <0-100 integer — pure ATS keyword + format score, be strict>,\n" +
                        "  \"jobTitle\": \"<exact title from JD>\",\n" +
                        "  \"industry\": \"<ONE of: SOFTWARE_ENGINEERING | DATA_SCIENCE | DEVOPS | FRONTEND | BACKEND | PRODUCT_MANAGEMENT>\",\n" +
                        "  \"missingKeywords\": \"<comma-separated list of JD keywords completely absent from resume>\",\n" +
                        "  \"matchedKeywords\": \"<comma-separated list of JD keywords found in resume>\",\n" +
                        "  \"suggestions\": \"<2-3 specific, actionable changes — not generic advice>\",\n" +
                        "  \"improvementTips\": \"<numbered list: 1. specific tip  2. specific tip  3. specific tip>\",\n" +
                        "  \"experienceGap\": \"<exact gap between JD requirement and candidate's experience — be specific>\",\n" +
                        "  \"skillsGap\": \"<exact technical skills in JD that candidate does not have>\"\n" +
                        "}\n\n" +
                        "ATS SCORING RULES (follow strictly):\n" +
                        "- Keyword match < 40% → atsScore max 45\n" +
                        "- Only 1 internship (< 6 months) → deduct 15 points\n" +
                        "- Missing required skills from JD → deduct 5 points each (max -25)\n" +
                        "- No quantified achievements → deduct 10 points\n" +
                        "- Generic summary not tailored to JD → deduct 8 points\n" +
                        "- Each exact JD keyword present → add 3 points\n\n" +
                        "RESUME:\n" + resume + "\n\n" +
                        "JOB DESCRIPTION:\n" + jobDescription;

        return callGroq(prompt, 0.1, 1024);
    }

    // ── COVER LETTER ──────────────────────────────────────────────────────────
    public String generateCoverLetter(String resumeText, String jobDescription,
                                      String companyName, String jobTitle) throws Exception {
        String resume = cap(resumeText, 4000);

        String prompt =
                "Write a professional, ATS-optimized cover letter.\n" +
                        "Company: " + companyName + "\n" +
                        "Job Title: " + jobTitle + "\n" +
                        "Rules:\n" +
                        "- Use at least 6 exact keywords from the JD naturally in the letter\n" +
                        "- 3 paragraphs: (1) hook with role + top 2 strengths, (2) specific project/achievement mapped to JD need, (3) call to action\n" +
                        "- Do NOT use filler phrases like 'I am excited to apply' or 'I believe I am a good fit'\n" +
                        "- Keep it under 280 words\n" +
                        "- Return ONLY the letter text, no subject line, no placeholders\n\n" +
                        "Job Description:\n" + jobDescription + "\n\n" +
                        "Candidate Resume:\n" + resume;

        return callGroq(prompt, 0.5, 1024);
    }

    // ── TAILORED RESUME ───────────────────────────────────────────────────────
    public String generateTailoredResume(String resumeText, String jobDescription,
                                         String companyType) throws Exception {
        String fullResume = cap(resumeText, 10000);

        // Build company-type specific instructions
        String companyInstructions = buildCompanyInstructions(companyType);

        String prompt =
                "You are a world-class ATS resume optimizer at a Big 4 recruiting firm. " +
                        "Your ONLY goal: tailor this resume to achieve MAXIMUM ATS score (90+) for the given JD.\n\n" +

                        "=== COMPANY TYPE OPTIMIZATION ===\n" +
                        companyInstructions + "\n\n" +

                        "=== PERSONAL DETAILS — MANDATORY EXTRACTION ===\n" +
                        "NAME: Take the VERY FIRST non-empty line of resume as name.\n" +
                        "EMAIL: Find any token containing '@' — that is the email.\n" +
                        "PHONE: Find 10-digit number or +91 prefixed number.\n" +
                        "LINKEDIN: Find text containing 'linkedin.com' — extract as-is.\n" +
                        "GITHUB: Find text containing 'github.com' — extract as-is.\n\n" +

                        "=== ATS OPTIMIZATION RULES (MANDATORY — FOLLOW ALL) ===\n" +
                        "1. SKILLS LIST:\n" +
                        "   - First 8 skills MUST be exact keywords copied from JD (word-for-word match)\n" +
                        "   - Then add candidate's remaining relevant skills\n" +
                        "   - Remove skills not mentioned in JD and not relevant to the role\n" +
                        "   - Total skills: 12-18 items\n\n" +
                        "2. PROFESSIONAL SUMMARY (3 sentences, MANDATORY):\n" +
                        "   - Sentence 1: Candidate's background + exact JD job title + years of experience\n" +
                        "   - Sentence 2: Top 3 technical skills from JD + one specific achievement with number\n" +
                        "   - Sentence 3: What value they bring to this specific role\n" +
                        "   - Must contain at least 7 exact phrases from JD\n" +
                        "   - NEVER use: 'highly motivated', 'passionate', 'team player', 'quick learner'\n\n" +
                        "3. EXPERIENCE BULLETS (rewrite every bullet):\n" +
                        "   - Start every bullet with a strong action verb (Engineered, Architected, Reduced, Increased, Deployed, Optimized)\n" +
                        "   - Every bullet must have: action verb + JD keyword + quantified result (%, time, scale)\n" +
                        "   - If original bullet has a number, KEEP it — never remove metrics\n" +
                        "   - If candidate used similar tech to JD tech, substitute JD's exact term\n" +
                        "   - Minimum 3 bullets per experience entry\n\n" +
                        "4. PROJECTS (rewrite descriptions):\n" +
                        "   - Rewrite to highlight JD-relevant technologies and impact\n" +
                        "   - Add business impact if missing (e.g., 'reducing latency by X%', 'supporting N users')\n" +
                        "   - techStack must list JD keywords first\n\n" +
                        "5. JOB TITLE: Use EXACT job title from JD — no creative variations\n\n" +
                        "6. ACHIEVEMENTS:\n" +
                        "   - Reframe achievements to connect to JD requirements\n" +
                        "   - IEEE papers, publications → lead with impact on the domain in JD\n\n" +

                        "=== EDUCATION EXTRACTION ===\n" +
                        "Extract: degree name, full college/university name, graduation year.\n" +
                        "NEVER write 'Not specified' — use empty string if not found.\n\n" +

                        "=== CERTIFICATIONS ===\n" +
                        "Extract all certifications. If candidate has skills matching well-known certs in the JD domain, suggest those certs as 'Recommended' in issuer field.\n\n" +

                        "=== OUTPUT — STRICT RAW JSON ONLY ===\n" +
                        "No markdown, no explanation, no ```json fences. Raw JSON only:\n" +
                        "{\n" +
                        "  \"name\": \"Full Name\",\n" +
                        "  \"email\": \"email@example.com\",\n" +
                        "  \"phone\": \"+91-XXXXXXXXXX\",\n" +
                        "  \"linkedin\": \"linkedin.com/in/username\",\n" +
                        "  \"github\": \"github.com/username\",\n" +
                        "  \"jobTitle\": \"Exact Job Title From JD\",\n" +
                        "  \"summary\": \"3 precise sentences packed with JD keywords and one metric\",\n" +
                        "  \"skills\": [\"JD Exact Skill 1\", \"JD Exact Skill 2\", \"JD Exact Skill 3\", \"JD Exact Skill 4\", \"JD Exact Skill 5\", \"JD Exact Skill 6\", \"JD Exact Skill 7\", \"JD Exact Skill 8\", \"Other Skill 9\"],\n" +
                        "  \"experience\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Job Title\",\n" +
                        "      \"company\": \"Company Name\",\n" +
                        "      \"duration\": \"Mon YYYY - Mon YYYY\",\n" +
                        "      \"points\": [\n" +
                        "        \"ActionVerb + JD keyword + quantified result\",\n" +
                        "        \"ActionVerb + JD keyword + quantified result\",\n" +
                        "        \"ActionVerb + JD keyword + quantified result\"\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"projects\": [\n" +
                        "    {\n" +
                        "      \"name\": \"Project Name\",\n" +
                        "      \"description\": \"JD-optimized description with impact metrics and JD keywords\",\n" +
                        "      \"techStack\": [\"JD Tech First\", \"Other Tech\"]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"certifications\": [\n" +
                        "    {\"name\": \"Cert Name\", \"issuer\": \"Issuer or Recommended\"}\n" +
                        "  ],\n" +
                        "  \"achievements\": [\n" +
                        "    \"Achievement reframed to connect with JD domain\"\n" +
                        "  ],\n" +
                        "  \"education\": [\n" +
                        "    {\n" +
                        "      \"degree\": \"Degree Name\",\n" +
                        "      \"college\": \"Full College Name\",\n" +
                        "      \"year\": \"YYYY\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +
                        "=== RESUME ===\n" + fullResume + "\n\n" +
                        "=== JOB DESCRIPTION ===\n" + jobDescription;

        return callGroq(prompt, 0.1, 4096);
    }

    // ── Overload for backward compatibility (no companyType) ──────────────────
    public String generateTailoredResume(String resumeText, String jobDescription) throws Exception {
        return generateTailoredResume(resumeText, jobDescription, "both");
    }

    // ── Company-specific optimization instructions ────────────────────────────
    private String buildCompanyInstructions(String companyType) {
        if (companyType == null) companyType = "both";
        return switch (companyType.toLowerCase()) {
            case "product" -> """
                TARGET: Product-based companies (FAANG, unicorn startups, mid-size product firms)
                RULES:
                - Emphasize: system design thinking, scalability, ownership, impact at scale
                - Use metrics that show scale: "served 1M+ requests/day", "reduced p99 latency by 40%"
                - Highlight: open-source contributions, side projects, publications, competitive programming
                - Summary must mention: problem-solving approach, system design, data structures
                - Skills order: DSA/Algorithms first, then core tech, then frameworks
                - Avoid: vague descriptions, team-size mentions without impact, process-heavy language
                - Projects: must show technical depth — mention architecture decisions, tradeoffs
                """;
            case "service" -> """
                TARGET: Service-based companies (TCS, Infosys, Wipro, Cognizant, Capgemini, Accenture)
                RULES:
                - Emphasize: client delivery, team collaboration, process adherence, certifications
                - Use metrics that show delivery: "delivered project 2 weeks ahead of schedule", "managed 5-member team"
                - Highlight: communication skills, adaptability, multiple technology exposure
                - Summary must mention: client-facing experience, Agile/Scrum, cross-functional collaboration
                - Skills order: JD-specific tech first, then tools, then methodologies (Agile, SDLC)
                - Certifications section is CRITICAL — list all, suggest relevant ones
                - Projects: mention client impact, team size, delivery timeline
                """;
            default -> """
                TARGET: Both product-based and service-based companies
                RULES:
                - Balance technical depth (for product) with delivery focus (for service)
                - Include metrics that show both scale AND delivery quality
                - Summary: mention both problem-solving AND collaboration
                - Skills: JD keywords first, then core tech, then methodologies
                - Projects: show technical depth + business/delivery impact
                - Certifications: list all existing ones prominently
                """;
        };
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private String cap(String text, int limit) {
        return text != null && text.length() > limit ? text.substring(0, limit) : text;
    }

    private String callGroq(String prompt, double temperature, int maxTokens) throws Exception {
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
                "You are a precise JSON-only assistant. " +
                        "Output ONLY a raw JSON object — no markdown, no ```json fences, no text before or after. " +
                        "Every field must be filled. Never output 'Not specified' or leave required fields empty. " +
                        "Follow all field-level instructions exactly.");

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