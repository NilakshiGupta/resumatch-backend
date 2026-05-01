package reume.matcher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reume.matcher.config.JwtUtil;
import reume.matcher.model.Resume;
import reume.matcher.service.AiService;
import reume.matcher.service.ResumeService;
import com.google.gson.Gson;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final AiService aiService;
    private final JwtUtil jwtUtil;

    @PostMapping("/tailor")
    public ResponseEntity<String> tailor(@RequestParam UUID resumeId, @RequestParam String jobDescription, @RequestHeader("Authorization") String token) throws Exception {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        Resume resume = resumeService.getUserResumes(email).stream().filter(r -> r.getId().equals(resumeId)).findFirst().get();
        return ResponseEntity.ok(aiService.generateTailoredResume(resume.getRawText(), jobDescription));
    }

    @PostMapping("/save-tailored")
    public ResponseEntity<Resume> saveTailored(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        UUID resumeId = UUID.fromString(request.get("resumeId").toString());
        String jobTitle = request.get("jobTitle").toString();
        String tailoredData = new Gson().toJson(request.get("tailoredData"));

        return ResponseEntity.ok(resumeService.saveTailoredResume(resumeId, tailoredData, jobTitle, email));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        return ResponseEntity.ok(resumeService.getUserResumes(email));
    }
}