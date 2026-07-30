# 🎓 Student Attendance & Performance Tracker

Welcome to the **Student Attendance & Performance Tracker**! 🚀 This is a modern, lightweight, and incredibly fast native Android application designed to help administrators, faculty, and students effortlessly track attendance, performance, and academic progress without the hassle of maintaining a heavy database.

By leveraging **Google Apps Script** and **Google Sheets** as a serverless, zero-cost backend, this app provides real-time syncing and easy data management accessible from both mobile and desktop.

## ✨ Key Features

- **📊 Admin Dashboard**: A comprehensive hub to view statistics, manage users, and export reports instantly.
- **👨‍🎓 Student Management**: Add, edit, delete, and browse student profiles seamlessly.
- **👩‍🏫 Faculty Management**: Assign faculty to subjects, manage their details, and maintain an organized faculty directory.
- **📚 Subject Management**: Keep track of the curriculum, total classes, semesters, and subject codes.
- **📈 Attendance Reports & CSV Export**: With just a tap, admins can generate and download entire class attendance records as a `.csv` file to share with parents, faculty, or management.
- **🛡️ Bulletproof UX**: Built-in safeguards like "Unsaved Changes" dialogs, exit confirmations, and intuitive error states ensure you never accidentally lose your work.
- **🎨 Modern UI**: Built entirely with **Jetpack Compose** and Material Design 3, providing a beautiful, dark-themed, reactive user experience.
- **⚡ Serverless Backend**: Powered by Google Sheets API, meaning zero hosting costs, massive scalability for standard academic use cases, and immediate spreadsheet-level data accessibility for non-technical staff.

## 🛠️ Tech Stack

### Frontend (Android)
- **Kotlin**: 100% Kotlin codebase.
- **Jetpack Compose**: Modern declarative UI framework.
- **Retrofit**: Type-safe HTTP client for REST API requests.
- **Gson**: JSON serialization and deserialization.
- **Coroutines**: For asynchronous background network processing and safe state management.

### Backend (Cloud)
- **Google Apps Script**: Middleware acting as a RESTful API (`doGet` and `doPost`).
- **Google Sheets**: Cloud database for tracking Students, Faculty, Subjects, and dynamic Attendance tables.

## 🚀 Getting Started

### 1. Backend Setup
1. Create a new Google Sheet.
2. Go to `Extensions > Apps Script`.
3. Copy the backend code (check the `google_apps_script_backend` file) and paste it into `Code.gs`.
4. Deploy the script as a **Web App** (Execute as: "Me", Access: "Anyone").
5. Copy the generated Web App URL.

### 2. App Setup
1. Clone this repository.
2. Open the project in **Android Studio**.
3. Open `NetworkModule.kt` (inside `app/src/main/java/.../data/network/`) and replace the `BASE_URL` with your Google Apps Script Web App URL.
4. Sync the Gradle files and hit **Run** on your emulator or physical Android device!

## 🤝 Contribution
This project is open-source and welcomes community contributions! Feel free to fork the repository, make improvements, and submit a pull request. Let's build the ultimate education management tool together!

## 📜 License
Distributed under the MIT License. See `LICENSE` for more information.

---
*Built with ❤️ for educators and students.*
