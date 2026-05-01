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
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        PDDocument document = Loader.loadPDF(file.getBytes());
        String rawText = new PDFTextStripper().getText(document);
        document.close();

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(file.getOriginalFilename());
        resume.setRawText(rawText);
        resume.setVersionNumber(1);
        resume.setIsActive(true);
        return resumeRepository.save(resume);
    }

    public Resume saveTailoredResume(UUID parentResumeId, String tailoredJson, String jobTitle, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new RuntimeException("User not found"));
        Resume parent = resumeRepository.findById(parentResumeId).orElseThrow(() -> new RuntimeException("Resume not found"));

        Resume tailored = new Resume();
        tailored.setUser(user);
        tailored.setParentResume(parent);
        tailored.setFileName("Tailored_" + jobTitle.replace(" ", "_") + ".json");
        tailored.setRawText(tailoredJson);
        tailored.setIsActive(true);

        int version = resumeRepository.findByParentResumeId(parentResumeId).size() + 2;
        tailored.setVersionNumber(version);
        tailored.setVersionLabel("Tailored for " + jobTitle);

        return resumeRepository.save(tailored);
    }

    public List<Resume> getUserResumes(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return resumeRepository.findByUserId(user.getId());
    }

    public void deleteResume(UUID id, String email) {
        resumeRepository.deleteById(id);
    }
}