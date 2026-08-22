# 🎓 Smart Study Planner

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-6DB33F?logo=springboot)
![Java](https://img.shields.io/badge/Java-17-007396?logo=java)
![MySQL](https://img.shields.io/badge/MySQL-Database-4479A1?logo=mysql)
![Mistral AI](https://img.shields.io/badge/AI-Mistral-FF7000)

> **A comprehensive Spring Boot application utilizing Thymeleaf and Mistral AI to help students intelligently organize, track, and optimize their study routines.**

## 🎯 Core Purpose & Problem Solved
Managing multiple subjects, assignments, and study materials can quickly become overwhelming for students. **Smart Study Planner** acts as a centralized dashboard to track academic progress, manage study boards and tasks, organize notes (documents), and leverage AI-powered study assistance. 

---

## 🛠️ Tech Stack & Tools

### **Backend**
- **Java 17** & **Spring Boot 3.2.4**
- **Spring Data JPA** (Hibernate)
- **Spring Security** (Authentication & Authorization)

### **Frontend**
- **Thymeleaf** (Server-side rendering)
- **HTML5, CSS3, JS** (Static resources decoupled from backend build)

### **Database & APIs**
- **MySQL** (Relational Data Storage)
- **Mistral AI API** (For intelligent AI study assistance)
- **Lombok** (Code reduction)

---

## 🚀 Current Status & Features Implemented

- [x] **User Authentication & Role Management:** Secure signup/login using Spring Security.
- [x] **Dashboard UI:** An interactive dashboard providing a bird's-eye view of academic progress.
- [x] **Subject & Task Management:** Create, read, update, and delete subjects and to-do lists.
- [x] **Study Board:** Kanban-style board to track ongoing study sessions and priorities.
- [x] **Document Management:** Secure file upload and retrieval system for study materials and notes.
- [x] **Analytics:** Visual insights and statistics tracking user study habits and completion rates.
- [x] **Mistral AI Integration:** AI-powered assistance for generating study plans, summarizing notes, or answering queries.

**Pending / Upcoming Work (Roadmap):**
- [ ] Add comprehensive Unit and Integration testing for controllers/services.
- [ ] Containerize application using Docker.
- [ ] Deploy to cloud platform (e.g., AWS, Render, Heroku).

---

## 📂 Project Directory Structure

```text
smart-study-planner/
├── backend/                  # Spring Boot Backend Source
│   ├── src/main/java/...     # Controllers, Models, Repositories, Services
│   ├── src/main/resources/   # application.properties
│   ├── pom.xml               # Maven configuration
├── frontend/                 # Decoupled Frontend
│   ├── static/               # CSS, JS, Images
│   └── templates/            # Thymeleaf HTML Templates
└── uploads/                  # Local storage for user-uploaded documents (generated)
```

---

## ⚙️ Getting Started / Local Setup Guide

### 1. Prerequisites
- **Java Development Kit (JDK) 17** or higher
- **Maven**
- **MySQL Server** (Running locally)
- **Git**

### 2. Clone the Repository
```bash
git clone https://github.com/yourusername/smart-study-planner.git
cd smart-study-planner
```

### 3. Database Setup
Create a local MySQL database for the application:
```sql
CREATE DATABASE smart_planner;
```

### 4. Environment Configuration
Navigate to the application properties file:
`backend/src/main/resources/application.properties`

Ensure the database credentials and your API keys are correctly set:
```properties
# Database Configuration (MySQL)
spring.datasource.url=jdbc:mysql://localhost:3306/smart_planner?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

# Mistral AI Configuration
mistral.api.key=YOUR_MISTRAL_API_KEY
mistral.api.url=https://api.mistral.ai/v1/chat/completions
mistral.model=mistral-small-latest
```

### 5. Install Dependencies and Run
Navigate to the `backend` directory, clean, install, and run the Spring Boot app:

```bash
cd backend
mvn clean install
mvn spring-boot:run
```
The application will start on `http://localhost:8080`.

---

## 🔌 API Reference / Key Controllers

While the app primarily uses Thymeleaf for rendering views, the backend is highly modular with distinct routing logic:

| Controller Name          | Primary Responsibility |
|--------------------------|------------------------|
| `AuthController`         | Handles user registration, login, and session management. |
| `SubjectController`      | CRUD operations for academic subjects. |
| `TaskController`         | Manages user to-do lists and study tasks. |
| `StudyBoardController`   | Manages the Kanban-style study tracking board. |
| `DocumentController`     | Manages file uploads (max 10MB) and secure file retrieval. |
| `AIController`           | Integrates with Mistral AI for intelligent study assistance. |
| `AnalyticsRestController`| Serves raw JSON data for frontend analytical charts. |

---

## 🤝 Contributing
1. Fork the repository
2. Create a new Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.
