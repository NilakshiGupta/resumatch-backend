package reume.matcher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reume.matcher.config.JwtUtil;
import reume.matcher.model.Resume;
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
}