# Student Attendance & Performance Tracker

Student Tracker is an Android application for managing class attendance and assessment marks. We built it around three roles: administrators prepare student, faculty and subject records; faculty record attendance and publish marks for their assigned subjects; students view their own progress.

The app uses Kotlin, Jetpack Compose and Material 3. Google Sheets stores the records, and Google Apps Script handles authentication, permissions and academic operations. Google Sheets and Apps Script are required parts of this project.

## What the app does

### Administrator

- View active student, faculty and subject counts.
- Search, add, edit and deactivate student, faculty and subject records.
- Assign faculty to subjects and manage enrolled students.
- Access attendance, assessments and subject reports.

Deactivation keeps historical records. Edits include a revision check so an older screen cannot silently overwrite a newer change.

### Faculty

- Open subjects assigned to their faculty profile.
- Record attendance by date and session using present, absent, late or excused statuses.
- Save assessment drafts, enter marks and publish completed results.
- Review attendance reports and share them as CSV files.

Submitted attendance and published assessments are locked in the current version. Matching request retries are recognized by the backend to prevent duplicate submissions.

### Student

- View personal attendance totals and percentages.
- View their own published assessment results.
- Refresh the dashboard and sign out.

Students cannot view another student's records or unpublished marks. These restrictions are checked by the backend, not just by hiding screens.

## Attendance and marks rules

Present and late count as attended. Present, late and absent count toward the attendance denominator; excused entries are excluded. With no counted sessions, attendance is shown as N/A. Reports currently use a fixed 75% shortage threshold.

Assessment entries distinguish pending, absent and scored marks, including a valid score of zero. Drafts may contain pending entries. Publication requires every entry to be scored or absent.

## Project structure

```text
app/                 Android application and tests
backend/src/         Authentication, domain rules and Apps Script adapter
backend/test/        Backend tests
backend/scripts/     Apps Script bundle generation
docs/                Requirements, design, checklists and study material
output/pdf/          Project reports and presentation guides
```

The client uses Compose state and direct suspend API calls. It does not currently include a complete MVVM/repository layer, Room database or WorkManager synchronization.

The backend creates two spreadsheet tabs: `TrackerUsers` for approved accounts and `TrackerRecords` for an append-only journal of JSON record snapshots and audit metadata. It reconstructs current records from that journal.

## Build and setup

You need Android Studio, JDK 21, the Android SDK packages requested by Gradle, and Node.js 22 or newer. The app supports Android API 24 and later.

### 1. Build the backend

From the `backend` directory:

```powershell
npm ci --ignore-scripts
npm run build
npm test
```

This generates `backend/dist/Code.gs` and `backend/dist/appsscript.json`. Follow [the backend setup guide](backend/README.md) to create a private spreadsheet, set Script Properties, initialize the tabs and deploy the web app.

### 2. Configure Google sign-in

Register an Android OAuth client for `com.example.studenttracker` with the SHA-1 of the signing certificate you actually use. Create a Web OAuth client and use its client ID as the token audience in both Android and Apps Script.

Add approved users to `TrackerUsers`. Faculty accounts must link to an employee ID; student accounts must link to a roll number. Creating a profile does not automatically approve its Google account.

### 3. Configure the Android app

Copy the keys from [gradle.properties.example](gradle.properties.example) into your user-level Gradle properties file, usually `%USERPROFILE%\.gradle\gradle.properties`, and supply your own values. Environment variables are also supported.

```properties
TRACKER_BASE_URL=https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/
GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

The base URL must end with `/` and omit `exec`. Do not commit live configuration or signing credentials. The client requires the version 2 API included in this repository.

### 4. Build and check the app

Open the project in Android Studio, select its bundled JDK or JDK 21, and sync Gradle. From PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### 5. Try the full workflow

Sign in as an approved administrator, create student and faculty profiles, create a subject and enroll students. Sign in as the assigned faculty member, submit attendance and publish an assessment. Finally, sign in as a student and check the personal dashboard. Use [the staging checklist](docs/STAGING_CHECKLIST.md) to record the results.

## Authentication and data handling

Apps Script verifies signed Google ID tokens and checks the active account mapping on every request. Faculty access is limited to assigned subjects; student progress is derived from the verified student profile.

Tokens stay in memory and travel in HTTPS JSON request bodies. A fresh app process requires sign-in again. CSV exports quote fields, preserve Unicode and neutralize formula-like values before sharing through Android's FileProvider.

Direct spreadsheet access must be restricted to trusted operators. The journal preserves changes made through the app; it cannot prevent a spreadsheet owner from editing cells directly.

## Current status

The core workflows are implemented, and demo backend configuration has been prepared. Real-device Google sign-in and the complete deployed workflow still need acceptance testing. A successful build is not a production release approval.

Durable offline drafts, approved record corrections, academic terms, configurable policies, bulk imports, advanced reports and operational recovery are still planned. Full-journal reads and global locking also need capacity testing before wider use.

## Documentation

- [Feature checklist](docs/FEATURE_CHECKLIST.md)
- [Production plan](PRODUCTION_PLAN.md)
- [Staging checklist](docs/STAGING_CHECKLIST.md)
- [Software Requirements Specification](output/pdf/Student_Tracker_SRS.pdf)
- [Software Design Document](output/pdf/Student_Tracker_SDD.pdf)
- [Presentation script](docs/PRESENTATION_SCRIPT.md)
- [Seven-member study plan](docs/SEVEN_MEMBER_STUDY_PLAN.md)

## Team

| Name | Roll number |
| --- | --- |
| R. Nirmal | 231801370002 |
| L. Nitish Kumar | 231801370009 |
| T. Veera Siva Krishna | 231801370025 |
| N. Laasya | 231801370046 |
| S. Jayanth Kumar | 231801370051 |
| S. Sai Vignesh | 231801370052 |
| T. Sai Devanand | 231801370070 |
