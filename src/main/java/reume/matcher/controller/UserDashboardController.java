package reume.matcher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reume.matcher.config.JwtUtil;
import reume.matcher.model.AnalysisResult;
import reume.matcher.model.Resume;
import reume.matcher.model.User;
import reume.matcher.repository.AnalysisResultRepository;
import reume.matcher.repository.ResumeRepository;
import reume.matcher.repository.UserRepository;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final AnalysisResultRepository analysisResultRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestHeader("Authorization") String token) {

        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Resume> resumes = resumeRepository.findByUserId(user.getId());
        List<AnalysisResult> analyses = analysisResultRepository.findByUserId(user.getId());

        OptionalDouble avgAts = analyses.stream()
                .mapToInt(AnalysisResult::getAtsScore)
                .average();

        OptionalDouble avgMatch = analyses.stream()
                .mapToInt(AnalysisResult::getMatchPercentage)
                .average();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", user.getName());
        summary.put("email", user.getEmail());
        summary.put("totalResumes", resumes.size());
        summary.put("totalAnalyses", analyses.size());
        summary.put("averageAtsScore", avgAts.isPresent() ? (int) avgAts.getAsDouble() : 0);
        summary.put("averageMatchPercentage", avgMatch.isPresent() ? (int) avgMatch.getAsDouble() : 0);
        summary.put("recentAnalyses", analyses.stream()
                .sorted(Comparator.comparing(AnalysisResult::getCreatedAt).reversed())
                .limit(5)
                .toList());

        return ResponseEntity.ok(summary);
    }
}