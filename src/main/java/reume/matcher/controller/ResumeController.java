package reume.matcher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reume.matcher.config.JwtUtil;
import reume.matcher.model.Resume;
import reume.matcher.model.User;
import reume.matcher.repository.UserRepository;
import reume.matcher.service.AiService;
import reume.matcher.service.ResumeService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final AiService aiService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<Resume> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) throws IOException {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        return ResponseEntity.ok(resumeService.uploadResume(file, email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        resumeService.deleteResume(id, email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Resume> toggle(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        return ResponseEntity.ok(resumeService.toggleResume(id, email));
    }

    @PostMapping("/tailor")
    public ResponseEntity<String> tailor(
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String token) throws Exception {

        UUID resumeId = UUID.fromString(body.get("resumeId"));
        String jobDescription = body.get("jobDescription");
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

        // Database se user lo — 100% accurate name & email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeService.getUserResumes(email).stream()
                .filter(r -> r.getId().equals(resumeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        String rawText = resume.getRawText();

        // Regex se personal details extract karo
        String detectedPhone    = extractPhone(rawText);
        String detectedLinkedin = extractLinkedin(rawText);
        String detectedGithub   = extractGithub(rawText);
        String detectedCollege  = extractCollege(rawText);

        // AI se tailored resume JSON
        String aiJson = aiService.generateTailoredResume(rawText, jobDescription);

        try {
            JsonObject obj = JsonParser.parseString(aiJson).getAsJsonObject();

            // Database se inject karo — AI pe depend mat karo
            obj.addProperty("name", user.getName());
            obj.addProperty("email", user.getEmail());

            // Regex se inject karo agar blank hai
            injectIfBlank(obj, "phone",    detectedPhone);
            injectIfBlank(obj, "linkedin", detectedLinkedin);
            injectIfBlank(obj, "github",   detectedGithub);

            // Array fields ensure karo
            ensureArray(obj, "skills");
            ensureArray(obj, "experience");
            ensureArray(obj, "education");
            ensureArray(obj, "projects");
            ensureArray(obj, "certifications");
            ensureArray(obj, "achievements");

            // College injection
            if (detectedCollege != null && obj.has("education") && obj.get("education").isJsonArray()) {
                JsonArray eduArray = obj.getAsJsonArray("education");
                eduArray.forEach(el -> {
                    if (el.isJsonObject()) {
                        JsonObject edu = el.getAsJsonObject();
                        String aiCollege = edu.has("college") ? edu.get("college").getAsString().trim() : "";
                        if (aiCollege.isBlank() || aiCollege.equalsIgnoreCase("not specified"))
                            edu.addProperty("college", detectedCollege);
                    }
                });
            }

            return ResponseEntity.ok(obj.toString());

        } catch (Exception e) {
            return ResponseEntity.ok(aiJson);
        }
    }

    @PostMapping("/save-tailored")
    public ResponseEntity<Resume> saveTailored(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        String email        = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        UUID resumeId       = UUID.fromString(request.get("resumeId").toString());
        String jobTitle     = request.get("jobTitle").toString();
        String tailoredData = new Gson().toJson(request.get("tailoredData"));
        return ResponseEntity.ok(resumeService.saveTailoredResume(resumeId, tailoredData, jobTitle, email));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        return ResponseEntity.ok(resumeService.getUserResumes(email));
    }

    // ── Injection helpers ──────────────────────────────────────────────────

    private void injectIfBlank(JsonObject obj, String key, String value) {
        if (value == null) return;
        String existing = obj.has(key) ? obj.get(key).getAsString().trim() : "";
        if (existing.isBlank() || existing.equalsIgnoreCase("not specified"))
            obj.addProperty(key, value);
    }

    private void ensureArray(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray())
            obj.add(key, new JsonArray());
    }

    // ── Regex Helpers ──────────────────────────────────────────────────────

    private String extractPhone(String text) {
        String compact = text.replaceAll("[\\s]", "");
        Matcher m1 = Pattern.compile("(\\+?91[\\-]?)?[6-9]\\d{9}").matcher(compact);
        if (m1.find()) return m1.group();
        Matcher m2 = Pattern.compile("[6-9]\\d{9}").matcher(compact);
        return m2.find() ? m2.group() : null;
    }

    private String extractLinkedin(String text) {
        Matcher m = Pattern.compile(
                "(?:https?://)?(?:www\\.)?linkedin\\.com/in/[\\w\\-]+/?",
                Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    private String extractGithub(String text) {
        Matcher m = Pattern.compile(
                "(?:https?://)?(?:www\\.)?github\\.com/[\\w\\-]+/?",
                Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    private String extractCollege(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String t = line.trim();
            if (t.length() < 5 || t.length() > 120) continue;
            String lower = t.toLowerCase();
            if (lower.contains("university") || lower.contains("college")
                    || lower.contains("institute") || lower.contains("iit")
                    || lower.contains("nit") || lower.contains("school of")
                    || lower.contains("academy") || lower.contains("vidyalaya")
                    || lower.contains("polytechnic")
                    || lower.matches(".*\\b(b\\.?tech|mca|bca|mba|b\\.?sc|m\\.?sc|b\\.?e\\.?|m\\.?tech)\\b.*")) {
                if (t.split("\\s+").length >= 2) return t;
            }
        }
        return null;
    }
}