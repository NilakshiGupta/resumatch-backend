package reume.matcher.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IndustryKeywordService {

    private static final Map<String, List<String>> INDUSTRY_KEYWORDS = new HashMap<>();

    static {
        INDUSTRY_KEYWORDS.put("Software Engineering", Arrays.asList(
                "Java", "Spring Boot", "REST API", "Microservices", "Docker", "Kubernetes",
                "AWS", "Git", "CI/CD", "SQL", "NoSQL", "React", "Node.js", "Python", "Agile"
        ));
        INDUSTRY_KEYWORDS.put("Data Science", Arrays.asList(
                "Python", "Machine Learning", "TensorFlow", "PyTorch", "Pandas", "NumPy",
                "SQL", "Tableau", "Power BI", "Statistics", "Deep Learning", "NLP", "Jupyter"
        ));
        INDUSTRY_KEYWORDS.put("DevOps", Arrays.asList(
                "Docker", "Kubernetes", "Jenkins", "AWS", "Azure", "GCP", "Terraform",
                "Ansible", "CI/CD", "Linux", "Bash", "Monitoring", "Prometheus", "Grafana"
        ));
        INDUSTRY_KEYWORDS.put("Frontend", Arrays.asList(
                "React", "Angular", "Vue.js", "JavaScript", "TypeScript", "HTML", "CSS",
                "Redux", "Webpack", "Jest", "Figma", "Responsive Design", "REST API"
        ));
        INDUSTRY_KEYWORDS.put("Backend", Arrays.asList(
                "Java", "Python", "Node.js", "Spring Boot", "Django", "PostgreSQL", "MySQL",
                "MongoDB", "Redis", "REST API", "GraphQL", "Microservices", "AWS"
        ));
    }

    public Map<String, Object> analyzeKeywords(String resumeText, String industry) {
        List<String> industryKeys = INDUSTRY_KEYWORDS.getOrDefault(industry,
                INDUSTRY_KEYWORDS.get("Software Engineering"));

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        String resumeLower = resumeText.toLowerCase();
        for (String keyword : industryKeys) {
            if (resumeLower.contains(keyword.toLowerCase())) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("matchedKeywords", String.join(", ", matched));
        result.put("missingKeywords", String.join(", ", missing));
        result.put("industryScore", matched.size() * 100 / industryKeys.size());

        return result;
    }

    public Set<String> getAvailableIndustries() {
        return INDUSTRY_KEYWORDS.keySet();
    }
}