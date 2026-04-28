package reume.matcher.repository;

import reume.matcher.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {
    List<AnalysisResult> findByUserId(UUID userId);
}