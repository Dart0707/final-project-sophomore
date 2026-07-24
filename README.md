# 🚀 FinalAcademicProject - Active Learning Seminar Tracker Portal

![Project Status: Completed](https://img.shields.io/badge/Project%20Status-Completed-brightgreen?style=flat&logo=github&logoColor=white)

An enterprise-ready Java EE web application for managing academic seminars, built to support active learning. It provides tailored, secure portals for students, instructors, and administrators, featuring multi-database synchronization, automated log tracking, and real-time PDF report exports.

---

## ✨ Features

* **🔐 Role-Based Access Control (RBAC):** Customized dashboards and control panels for Students (enrollment), Instructors (seminar management), and Admins (user account administration).
* **🛡️ Hardened Security:** Protects authentication workflows with Google reCAPTCHA v2 bot prevention, AES-128 symmetric encryption for passwords, and prepared SQL statements to prevent injection.
* **📂 Hybrid Multi-Database Integration:** Integrates Apache Derby for secure user accounts, MySQL for transactional courses/enrollments, and PostgreSQL for centralized system event auditing.
* **📄 Dynamically Generated PDF Reports:** Streamlines report workflows by utilizing iText to generate and download participant directories (for instructors) and user logs/lists (for admins).
* **📊 Centralized Event Logger:** Actively intercepts system events (logins, deletions, edits) and persists logs to PostgreSQL, rendering them in a secure log viewer portal.

---

## 🛠️ Tech Stack

* **Languages & Specifications:** ![Java](https://img.shields.io/badge/Java-8-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Java EE](https://img.shields.io/badge/Java_EE-5%2F6-0073B7?style=flat-square&logo=oracle&logoColor=white)
* **Application Server:** ![GlassFish](https://img.shields.io/badge/GlassFish-3%2B-2C2255?style=flat-square&logo=eclipse&logoColor=white)
* **Databases:** ![Apache Derby](https://img.shields.io/badge/Apache_Derby-Auth-D22128?style=flat-square&logo=apache&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-Courses-4479A1?style=flat-square&logo=mysql&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Logs-4169E1?style=flat-square&logo=postgresql&logoColor=white)
* **Build System & IDE:** ![Apache Ant](https://img.shields.io/badge/Apache_Ant-Build-A01F25?style=flat-square&logo=apache-ant&logoColor=white) ![NetBeans](https://img.shields.io/badge/NetBeans-IDE-1B6AC6?style=flat-square&logo=apache-netbeans&logoColor=white)
* **Security & Utils:** ![reCAPTCHA v2](https://img.shields.io/badge/reCAPTCHA-v2-4EA94B?style=flat-square&logo=google&logoColor=white) ![AES Encryption](https://img.shields.io/badge/AES-128_bit-lightgrey?style=flat-square&logo=letsencrypt&logoColor=white) ![JDBC](https://img.shields.io/badge/JDBC-Drivers-orange?style=flat-square)
* **Libraries:** ![iText PDF](https://img.shields.io/badge/iText-PDF-red?style=flat-square&logo=adobe&logoColor=white)


---

## ⚡ Getting Started

Follow these instructions to get a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Make sure you have the following installed on your machine:

* **Java Development Kit (JDK 8)** (Highly recommended for GlassFish 3/Java EE 5 compatibility)
* **Apache Ant** (v1.10.0 or higher) or an IDE with built-in Ant support (e.g., **Apache NetBeans IDE**)
* **GlassFish Server 3** (or any modern Java EE application server)
* **Databases:**
  * **MySQL** (v8.0 or higher)
  * **PostgreSQL** (v12 or higher with SCRAM-SHA-256 support)
  * **Apache Derby** (Network server mode)

---

### Database Setup

Before launching the application, you must initialize the databases and create the required tables.

#### 1. MySQL Database Setup (`active_learning`)
Create a database named `active_learning` and run the following script:
```sql
CREATE DATABASE active_learning;
USE active_learning;

-- Create courses table
CREATE TABLE courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    course_date DATE NOT NULL,
    instructor_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Create enrollments table
CREATE TABLE enrollments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    student_id INT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);
```

#### 2. PostgreSQL Database Setup (`active_learning_logs`)
Create a database named `active_learning_logs` and configure the logging schema:
```sql
CREATE DATABASE active_learning_logs;
\c active_learning_logs;

-- Create action_logs table
CREATE TABLE action_logs (
    log_id SERIAL PRIMARY KEY,
    log_timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    log_level VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    user_id INT,
    source VARCHAR(255) NOT NULL,
    message TEXT NOT NULL
);
```

#### 3. Apache Derby Database Setup (`LoginDB`)
Start the Derby Network Server and connect using your database URL (e.g., `[DERBY_DATABASE_URL];create=true`) as user `[DERBY_USER]` with password `[DERBY_PASSWORD]`. Execute:
```sql
CREATE TABLE USERS (
    ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    EMAIL VARCHAR(255) NOT NULL UNIQUE,
    PASSWORD VARCHAR(255) NOT NULL,
    USERROLE VARCHAR(50) NOT NULL,
    PRIMARY KEY (ID)
);
```

---

### Installation & Configuration

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/your-repo-name.git
   cd your-repo-name
   ```

2. **Configure Environment Parameters:**
   Open the deployment descriptor [web.xml](file:///web/WEB-INF/web.xml) and configure credentials, connection strings, reCAPTCHA keys, and symmetric encryption keys to match your environment:
   ```xml
   <!-- Symmetric Encryption Keys -->
   <context-param>
       <param-name>cipherAlgorithm</param-name>
       <param-value>[CIPHER_ALGORITHM]</param-value>
   </context-param>
   <context-param>
       <param-name>secretKey</param-name>
       <param-value>[AES_SECRET_KEY]</param-value>
   </context-param>

   <!-- Apache Derby Credentials -->
   <context-param>
       <param-name>URL</param-name>
       <param-value>[DERBY_DATABASE_URL]</param-value>
   </context-param>

   <!-- MySQL Credentials -->
   <context-param>
       <param-name>mySQLURL</param-name>
       <param-value>[MYSQL_DATABASE_URL]</param-value>
   </context-param>

   <!-- PostgreSQL Credentials -->
   <context-param>
       <param-name>postgreURL</param-name>
       <param-value>[POSTGRESQL_DATABASE_URL]</param-value>
   </context-param>
   ```
---

## 📂 Directory Structure

Here is an overview of the key components in the repository:

* **[src/java/](file:///src/java)** — Backend Java source files
  * **[myservlets/](file:///src/java/myservlets)** — Controllers managing authentication, file downloads, participant filters, and logs.
  * **[myhelper/](file:///src/java/myhelper)** — Entities (`Course`, `User`, `LogEntry`) and Data Access Objects (`UserDAO`, `CourseDAO`, `LogDAO`).
  * **[utils/](file:///src/java/utils)** — Encryption utilities, system log handlers, reCAPTCHA validators, and DB connections.
* **[web/](file:///web)** — Client-facing JSP views, stylesheets, and scripts
  * **[index.jsp](file:///web/index.jsp)** — The entry point featuring the reCAPTCHA-enabled login portal.
  * **[success.jsp](file:///web/success.jsp)** — Main dashboard serving personalized views for Student, Instructor, and Admin roles.
  * **[participants.jsp](file:///web/participants.jsp)** — Renders interactive course participant directories.
  * **[logs.jsp](file:///web/logs.jsp)** — System auditing interface accessible by administrators.
* **[nbproject/](file:///nbproject)** — NetBeans build, property, and dependency mapping configurations.
