package reume.matcher.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "analysis_results")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "match_percentage")
    private Integer matchPercentage;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "missing_keywords", columnDefinition = "TEXT")
    private String missingKeywords;

    @Column(name = "matched_keywords", columnDefinition = "TEXT")
    private String matchedKeywords;

    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "improvement_tips", columnDefinition = "TEXT")
    private String improvementTips;

    @Column(name = "experience_gap", columnDefinition = "TEXT")
    private String experienceGap;

    @Column(name = "skills_gap", columnDefinition = "TEXT")
    private String skillsGap;

    @Column(name = "industry")
    private String industry;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}