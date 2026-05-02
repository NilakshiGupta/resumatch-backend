package reume.matcher.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reume.matcher.model.AnalysisResult;
import reume.matcher.model.Resume;
import reume.matcher.model.User;
import reume.matcher.repository.AnalysisResultRepository;
import reume.matcher.repository.ResumeRepository;
import reume.matcher.repository.UserRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisResultRepository analysisResultRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AiService aiService;

    public AnalysisResult analyze(UUID resumeId, String jobDescription, String userEmail) throws Exception {
        // Single DB call — user aur resume dono fetch karo
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Ownership check — koi aur ka resume analyze nahi kar sakte
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: This resume does not belong to you");
        }

        // Validate resume has text
        if (resume.getRawText() == null || resume.getRawText().isBlank()) {
            throw new RuntimeException("Resume has no extractable text. Please re-upload.");
        }

        String aiResponse = aiService.analyzeResume(resume.getRawText(), jobDescription);

        JsonObject json;
        try {
            json = JsonParser.parseString(aiResponse).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("AI returned invalid response. Please try again.");
        }

        AnalysisResult result = new AnalysisResult();
        result.setUser(user);
        result.setResume(resume);
        result.setMatchPercentage(getInt(json, "matchPercentage"));
        result.setAtsScore(getInt(json, "atsScore"));
        result.setJobTitle(getString(json, "jobTitle"));
        result.setIndustry(getString(json, "industry"));
        result.setMissingKeywords(getString(json, "missingKeywords"));
        result.setMatchedKeywords(getString(json, "matchedKeywords"));
        result.setSuggestions(getString(json, "suggestions"));
        result.setImprovementTips(getString(json, "improvementTips"));
        result.setExperienceGap(getString(json, "experienceGap"));
        result.setSkillsGap(getString(json, "skillsGap"));

        return analysisResultRepository.save(result);
    }

    public List<AnalysisResult> getUserHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return analysisResultRepository.findByUserId(user.getId());
    }

    private int getInt(JsonObject json, String key) {
        try {
            return json.has(key) && !json.get(key).isJsonNull()
                    ? json.get(key).getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getString(JsonObject json, String key) {
        try {
            return json.has(key) && !json.get(key).isJsonNull()
                    ? json.get(key).getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }
}