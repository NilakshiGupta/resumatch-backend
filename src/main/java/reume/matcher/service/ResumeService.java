package reume.matcher.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reume.matcher.model.Resume;
import reume.matcher.model.User;
import reume.matcher.repository.ResumeRepository;
import reume.matcher.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    public Resume uploadResume(MultipartFile file, String userEmail) throws IOException {
        return uploadResume(file, userEmail, null, null);
    }

    public Resume uploadResume(MultipartFile file, String userEmail,
                               UUID parentResumeId, String versionLabel) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PDDocument document = Loader.loadPDF(file.getBytes());
        PDFTextStripper stripper = new PDFTextStripper();
        String rawText = stripper.getText(document);
        document.close();

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileUrl("local");
        resume.setRawText(rawText);
        resume.setIsActive(true);

        if (parentResumeId != null) {
            Resume parent = resumeRepository.findById(parentResumeId)
                    .orElseThrow(() -> new RuntimeException("Parent resume not found"));
            resume.setParentResume(parent);
            int nextVersion = resumeRepository.findByUserId(user.getId()).size() + 1;
            resume.setVersionNumber(nextVersion);
            resume.setVersionLabel(versionLabel != null ? versionLabel : "v" + nextVersion);
        } else {
            resume.setVersionNumber(1);
            resume.setVersionLabel(versionLabel != null ? versionLabel : "v1");
        }

        return resumeRepository.save(resume);
    }

    public List<Resume> getUserResumes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return resumeRepository.findByUserId(user.getId());
    }

    public List<Resume> getResumeVersions(UUID parentResumeId) {
        return resumeRepository.findByParentResumeId(parentResumeId);
    }
}