package reume.matcher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reume.matcher.config.JwtUtil;
import reume.matcher.model.Resume;
import reume.matcher.service.AiService;
import reume.matcher.service.ResumeService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final JwtUtil jwtUtil;
    private final AiService aiService;

    @PostMapping("/upload")
    public ResponseEntity<Resume> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) throws IOException {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        Resume resume = resumeService.uploadResume(file, email);
        return ResponseEntity.ok(resume);
    }

    @PostMapping("/upload/version")
    public ResponseEntity<Resume> uploadVersion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentResumeId") UUID parentResumeId,
            @RequestParam(value = "versionLabel", required = false) String versionLabel,
            @RequestHeader("Authorization") String token) throws IOException {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        Resume resume = resumeService.uploadResume(file, email, parentResumeId, versionLabel);
        return ResponseEntity.ok(resume);
    }

    @GetMapping("/list")
    public ResponseEntity<List<Resume>> getResumes(
            @RequestHeader("Authorization") String token) {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        List<Resume> resumes = resumeService.getUserResumes(email);
        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/versions/{parentResumeId}")
    public ResponseEntity<List<Resume>> getVersions(
            @PathVariable UUID parentResumeId) {

        return ResponseEntity.ok(resumeService.getResumeVersions(parentResumeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        resumeService.deleteResume(id, email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Resume> toggleResume(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        Resume resume = resumeService.toggleResume(id, email);
        return ResponseEntity.ok(resume);
    }

    @PostMapping("/tailor")
    public ResponseEntity<String> tailorResume(
            @RequestParam("resumeId") UUID resumeId,
            @RequestParam("jobDescription") String jobDescription,
            @RequestHeader("Authorization") String token) throws Exception {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        Resume resume = resumeService.getUserResumes(email)
                .stream()
                .filter(r -> r.getId().equals(resumeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        String tailored = aiService.generateTailoredResume(resume.getRawText(), jobDescription);
        return ResponseEntity.ok(tailored);
    }
    @PostMapping("/save-tailored")
    public ResponseEntity<Resume> saveTailored(
            @RequestBody java.util.Map<String, Object> request,
            @RequestHeader("Authorization") String token) {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        UUID resumeId = UUID.fromString(request.get("resumeId").toString());
        String jobTitle = request.get("jobTitle").toString();

        // JSON data ko String mein convert karke save karenge
        String tailoredData = new com.google.gson.Gson().toJson(request.get("tailoredData"));

        Resume saved = resumeService.saveTailoredResume(resumeId, tailoredData, jobTitle, email);
        return ResponseEntity.ok(saved);
    }
}