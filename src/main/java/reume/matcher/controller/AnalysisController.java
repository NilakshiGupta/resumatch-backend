package reume.matcher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reume.matcher.config.JwtUtil;
import reume.matcher.model.AnalysisResult;
import reume.matcher.service.AnalysisService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final JwtUtil jwtUtil;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyze(
            @RequestParam("resumeId") UUID resumeId,
            @RequestParam("jobDescription") String jobDescription,
            @RequestHeader("Authorization") String token) throws Exception {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        AnalysisResult result = analysisService.analyze(resumeId, jobDescription, email);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisResult>> getHistory(
            @RequestHeader("Authorization") String token) {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        List<AnalysisResult> history = analysisService.getUserHistory(email);
        return ResponseEntity.ok(history);
    }
}