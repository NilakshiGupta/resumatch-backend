package reume.matcher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reume.matcher.config.JwtUtil;
import reume.matcher.model.Resume;
import reume.matcher.repository.ResumeRepository;
import reume.matcher.service.AiService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cover-letter")
@RequiredArgsConstructor
public class CoverLetterController {

    private final AiService aiService;
    private final JwtUtil jwtUtil;
    private final ResumeRepository resumeRepository;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String token) throws Exception {

        UUID resumeId = UUID.fromString(request.get("resumeId"));
        String jobDescription = request.get("jobDescription");
        String companyName = request.get("companyName");
        String jobTitle = request.get("jobTitle");

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        String coverLetter = aiService.generateCoverLetter(
                resume.getRawText(), jobDescription, companyName, jobTitle);

        return ResponseEntity.ok(Map.of("coverLetter", coverLetter));
    }
}