package reume.matcher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reume.matcher.model.AnalysisResult;
import reume.matcher.model.Resume;
import reume.matcher.model.User;
import reume.matcher.repository.AnalysisResultRepository;
import reume.matcher.repository.ResumeRepository;
import reume.matcher.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock private AnalysisResultRepository analysisResultRepo;
    @Mock private ResumeRepository         resumeRepo;
    @Mock private UserRepository           userRepo;
    @Mock private AiService                aiService;

    @InjectMocks
    private AnalysisService analysisService;

    private User   testUser;
    private Resume testResume;
    private UUID   resumeId;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        resumeId = UUID.randomUUID();
        testResume = new Resume();
        testResume.setId(resumeId);
        testResume.setUser(testUser);
        testResume.setFileName("test_resume.pdf");
        testResume.setRawText("Java developer with 3 years experience in Spring Boot and REST APIs.");
        testResume.setIsActive(true);
    }

    // ── Test 1: Successful analysis ───────────────────────────────────────────
    @Test
    void analyze_success_savesAndReturnsResult() throws Exception {
        String jobDesc = "We are looking for a Java developer with Spring Boot experience.";

        String mockAiJson = """
                {
                  "matchPercentage": 78,
                  "atsScore": 72,
                  "jobTitle": "Java Developer",
                  "industry": "SOFTWARE_ENGINEERING",
                  "matchedKeywords": "Java, Spring Boot, REST API",
                  "missingKeywords": "Docker, Kubernetes",
                  "suggestions": "Add cloud experience.",
                  "improvementTips": "Quantify your achievements.",
                  "experienceGap": "Job needs 5 years, resume shows 3 years.",
                  "skillsGap": "Missing: Docker, Kubernetes"
                }
                """;

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(resumeRepo.findById(resumeId)).thenReturn(Optional.of(testResume));
        when(aiService.analyzeResume(any(), any())).thenReturn(mockAiJson);
        when(analysisResultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnalysisResult result = analysisService.analyze(resumeId, jobDesc, "test@example.com");

        assertThat(result.getMatchPercentage()).isEqualTo(78);
        assertThat(result.getAtsScore()).isEqualTo(72);
        assertThat(result.getJobTitle()).isEqualTo("Java Developer");
        assertThat(result.getMatchedKeywords()).contains("Spring Boot");
        verify(analysisResultRepo, times(1)).save(any());
    }

    // ── Test 2: Unauthorized — user doesn't own the resume ───────────────────
    @Test
    void analyze_throwsException_whenResumeDoesNotBelongToUser() {
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID()); // different ID
        testResume.setUser(anotherUser);

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(resumeRepo.findById(resumeId)).thenReturn(Optional.of(testResume));

        assertThatThrownBy(() ->
                analysisService.analyze(resumeId, "some jd", "test@example.com")
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");

        verify(analysisResultRepo, never()).save(any());
    }

    // ── Test 3: Empty resume text ─────────────────────────────────────────────
    @Test
    void analyze_throwsException_whenResumeHasNoText() throws Exception  {
        testResume.setRawText("   ");

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(resumeRepo.findById(resumeId)).thenReturn(Optional.of(testResume));
        assertThatThrownBy(() ->
                analysisService.analyze(resumeId, "some jd", "test@example.com")
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no extractable text");

        verify(aiService, never()).analyzeResume(any(), any());
    }

    // ── Test 4: User not found ────────────────────────────────────────────────
    @Test
    void analyze_throwsException_whenUserNotFound() {
        when(userRepo.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                analysisService.analyze(resumeId, "some jd", "ghost@example.com")
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── Test 5: getUserHistory returns correct list ───────────────────────────
    @Test
    void getUserHistory_returnsListForCorrectUser() {
        AnalysisResult r1 = new AnalysisResult();
        r1.setUser(testUser); r1.setMatchPercentage(80); r1.setAtsScore(75);

        AnalysisResult r2 = new AnalysisResult();
        r2.setUser(testUser); r2.setMatchPercentage(65); r2.setAtsScore(60);

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(analysisResultRepo.findByUserId(testUser.getId())).thenReturn(List.of(r1, r2));

        List<AnalysisResult> history = analysisService.getUserHistory("test@example.com");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getMatchPercentage()).isEqualTo(80);
    }
}