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
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }

        PDDocument document = Loader.loadPDF(file.getBytes());
        String rawText = new PDFTextStripper().getText(document);
        document.close();

        if (rawText == null || rawText.isBlank()) {
            throw new RuntimeException("Could not extract text from PDF. Please upload a text-based (not scanned) PDF.");
        }

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(file.getOriginalFilename());
        resume.setRawText(rawText);
        resume.setVersionNumber(1);
        resume.setIsActive(true);
        return resumeRepository.save(resume);
    }

    public Resume saveTailoredResume(UUID parentResumeId, String tailoredJson, String jobTitle, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume parent = resumeRepository.findById(parentResumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Ownership check — sirf apna resume tailor kar sakte ho
        if (!parent.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: This resume does not belong to you");
        }

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return resumeRepository.findByUserId(user.getId());
    }

    /**
     * Delete resume — ownership verify karo pehle
     */
    public void deleteResume(UUID id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Security fix: sirf apna resume delete kar sakte ho
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: This resume does not belong to you");
        }

        resumeRepository.deleteById(id);
    }

    /**
     * Toggle isActive — ownership verify karo pehle
     */
    public Resume toggleResume(UUID id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Security fix: sirf apna resume toggle kar sakte ho
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: This resume does not belong to you");
        }

        resume.setIsActive(!resume.getIsActive());
        return resumeRepository.save(resume);
    }
}