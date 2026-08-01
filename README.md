# 🗳️ Secure Online Voting System with Face & OTP Verification

A highly secure, state-of-the-art web-based voting management application designed with a premium, responsive **Light Green & Mint** theme. The system enforces multi-factor voter authentication via biometrics (Python DeepFace face comparison) and one-time passwords (OTP).

---

## 🎨 Interface & Screenshots

### Voter & Admin Logins
| Voter Login Screen | Admin Login Screen |
|:---:|:---:|
| ![Voter Login](Screen%20Shots/voter_login_light_green.png) | ![Admin Login](Screen%20Shots/admin_login.png) |

### Admin Dashboard & Management
| System Dashboard | Candidates List | Voters List | Positions Configuration |
|:---:|:---:|:---:|:---:|
| ![Dashboard](Screen%20Shots/dashboard_light_green.png) | ![Candidates List](Screen%20Shots/candidate_list_light_green.png) | ![Voters List](Screen%20Shots/voters_list_light_green.png) | ![Positions](Screen%20Shots/positions.png) |

### Ballot Submission & Vote Tracking
| Voter Ballot | Ballot Verification Preview | Vote Records |
|:---:|:---:|:---:|
| ![Voter Ballot](Screen%20Shots/voter%20ballot.png) | ![Preview Vote](Screen%20Shots/preview%20vote.png) | ![Votes Log](Screen%20Shots/votes.png) |

---

## 🛡️ Database Diagrams & Structure

### ER Diagram & Relational Model
| OVS ER Diagram | Database Relational Model |
|:---:|:---:|
| ![OVS ER Diagram](Screen%20Shots/OVS%20ER%20Diagram.jpg) | ![Relational Model](Screen%20Shots/Relational%20Model.jpg) |

---

## 🚀 Key Features

*   **Multi-Stage Voter Verification Pipeline**:
    1.  **Voter Credentials Check**: Login with a secure alphanumeric Voter ID and password.
    2.  **Face Capture Verification**: Live webcam feed captures the voter's face and matches it against their registered voter photo using a Python-based DeepFace verification script.
    3.  **One-Time Password (OTP)**: Generates and verifies a secure numeric code.
*   **Security Protections (OWASP Top 10 Mitigation)**:
    *   **SQL Injection Prevention**: All queries rewritten to use parameterized, prepared statements (`prepare()` + `bind_param()`).
    *   **CSRF Token Protection**: Secure session-based token injection in every form, validated upon POST submission.
    *   **Open Redirect Mitigation**: Form redirection target validation using relative path enforcement.
    *   **Vote Reset Safety**: Converted unsafe GET-triggers into secure POST actions protected with CSRF checks.
*   **Auditability**:
    *   **RSA Vote Cryptographic Auditing**: Ensures votes can be verified for tamper-proofing and voter integrity.

---

## 🛠️ Technology Stack

*   **Backend**: PHP (v7.4+ or v8.x), Python 3 (for Face Verification models)
*   **Database**: MySQL / MariaDB
*   **Frontend**: HTML5, CSS3 (Custom Light Green Glassmorphism theme), JavaScript, jQuery, Bootstrap 3, SweetAlert2 Popups
*   **Biometrics Engine**: Python `deepface`, `opencv-python`, `numpy`

---

## 💻 Setup & Installation Instructions

### 1. Prerequisite Environments
1.  Download and install **XAMPP** (or any LAMP/WAMP server supporting PHP 7.4+ and MySQL).
2.  Install **Python 3.8+** on your system and ensure it is added to your environment `PATH`.

### 2. Clone and Place the Project
Clone the repository and place the project folder inside your web server's public directory (e.g., `C:\xampp\htdocs\Online-Voting-System\`).

### 3. Database Import
1.  Start Apache and MySQL services in the XAMPP Control Panel.
2.  Open your browser and navigate to `http://localhost/phpmyadmin/`.
3.  Create a new database named `votesystem`.
4.  Select the `votesystem` database, go to the **Import** tab, choose the file [votesystem.sql](file:///c:/xampp/htdocs/Online-Voting-System/Online-Voting-System/voting-management-system/votesystem/votesystem/db/votesystem.sql) located under the `db/` folder, and click **Go**.

### 4. Setup Database Credentials
If your database configuration differs, update the connection parameters inside:
*   [includes/conn.php](file:///c:/xampp/htdocs/Online-Voting-System/Online-Voting-System/voting-management-system/votesystem/votesystem/includes/conn.php)
*   [admin/includes/conn.php](file:///c:/xampp/htdocs/Online-Voting-System/Online-Voting-System/voting-management-system/votesystem/votesystem/admin/includes/conn.php)

*Note: In local Windows/XAMPP environments, using `127.0.0.1` as the host (with an empty password) is recommended to prevent IPv6 DNS loopback connection failures.*

### 5. Install Python Dependencies
Open your command terminal and install the required dependencies for face comparison:
```bash
pip install deepface opencv-python numpy tf-keras
```

---

## 🔑 Login Credentials

*   **Voter Login**:
    *   Voter ID: `HrEn9AbXxtpo6jf`
    *   Password: `password`
*   **Admin Login**:
    *   Username: `Venkatesh`
    *   Password: `password`
