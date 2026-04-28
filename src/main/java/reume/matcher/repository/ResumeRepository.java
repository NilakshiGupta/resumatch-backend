package reume.matcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reume.matcher.model.Resume;

import java.util.List;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByUserId(UUID userId);
    List<Resume> findByParentResumeId(UUID parentResumeId);
}