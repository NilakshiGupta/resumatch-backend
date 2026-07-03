# ResuMatch Backend

An AI-powered Spring Boot backend that matches resumes against job descriptions — parsing resumes, scoring them like an ATS, and generating tailored improvement suggestions.

Part of the **ResuMatch** platform (see [resumatch-frontend](https://github.com/NilakshiGupta/resumatch-frontend) for the client app).

## What it does

1. Resume upload — User uploads a PDF resume. The backend extracts structured data (skills, experience, education, projects) using **Apache PDFBox**.
2. Job description parsing — User pastes a JD. The backend identifies key requirements and keywords.
3. AI-powered matching — Resume data and JD are sent to the **Grok API (xAI)**, which:
   - Calculates an ATS-style match score
   - Identifies missing keywords/skills
   - Suggests specific improvements to resume bullet points
   - Generates tailored content (e.g. cover letters)
4. Results delivery — Match percentage, gaps, and suggestions are returned to the frontend.
5. Authentication— JWT-based auth keeps each user's resumes and match history private.
6. Persistence — User accounts, uploaded resumes, and match history are stored in PostgreSQL.

In short: an AI resume doctor — it compares a resume against a JD the way a recruiter or ATS would, and tells you exactly how to close the gap.

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.0 |
| Database | PostgreSQL (via Spring Data JPA) |
| Security | Spring Security + JWT (jjwt) |
| PDF Processing | Apache PDFBox |
| AI Integration | Grok API (xAI) |
| Build Tool | Maven |
| Containerization | Docker |
| CI/CD | GitHub Actions |

## Key Features

- 🔐 Secure JWT-based authentication and authorization
- 📄 Resume parsing engine — extracts structured data from PDF resumes
- 🤖 AI-powered resume-to-JD matching and ATS scoring via Grok API
- 🔌 RESTful APIs for resume upload, JD matching, and AI-generated content
- 🗄️ PostgreSQL-backed persistence layer via Spring Data JPA
- 🐳 Dockerized for consistent deployment across environments
- ⚙️ Automated build/test pipeline via GitHub Actions

## Architecture

```
Resume (PDF) ──▶ PDFBox Parser ──▶ Structured Data ─┐
                                                       ├──▶ Grok API ──▶ Match Score + Suggestions
Job Description ──▶ Keyword Parser ──▶ JD Data ──────┘
                                                       ▼
                                              PostgreSQL (persisted)
```

## Getting Started

### Prerequisites
- Java 21
- Maven
- PostgreSQL
- A Grok API (xAI) key

### Installation

```bash
# Clone the repository
git clone https://github.com/NilakshiGupta/resumatch-backend.git
cd resumatch-backend

# Configure environment variables (database URL, JWT secret, Grok API key)
# in src/main/resources/application.properties

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### Running with Docker

```bash
docker build -t resumatch-backend .
docker run -p 8080:8080 resumatch-backend
```

## API Overview

| Endpoint | Method | Description |
|---|---|---|
| `/api/auth/register` | POST | Register a new user |
| `/api/auth/login` | POST | Authenticate and receive a JWT |
| `/api/resume/upload` | POST | Upload and parse a resume PDF |
| `/api/match` | POST | Match a resume against a job description |
| `/api/match/history` | GET | Retrieve a user's past match results |

> Update this table with your actual controller mappings for accuracy.

## Related Repositories

- [resumatch-frontend](https://github.com/NilakshiGupta/resumatch-frontend) — React frontend for ResuMatch

## Author

Nilakshi Gupta — [GitHub](https://github.com/NilakshiGupta)
