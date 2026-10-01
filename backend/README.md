# Google Sheets / Apps Script backend

This backend stays entirely on Google Sheets and Google Apps Script. It implements
the Android client's version 2 API. A demo deployment has been configured, but
real-device sign-in and the full live workflow still require acceptance testing.
Create your own private spreadsheet and deployment using the steps below.

## Build and verify locally

Requires Node.js 22 or later. From this directory:

```powershell
npm ci --ignore-scripts
npm run build
npm test
npm audit --omit=dev
```

The build produces `dist/Code.gs` and `dist/appsscript.json`. The generated script
includes the pinned RSA verification library, application rules and Apps Script
adapter. Local tests execute the generated bundle with Apps Script service doubles
and independently signed RSA tokens; they do not access Google or live student data.

## Set up a staging deployment

1. Create a private Google spreadsheet. Share it only with authorized operators.
2. Open **Extensions → Apps Script**. Replace `Code.gs` with the generated
   `dist/Code.gs`. Enable showing the manifest in Project Settings and copy
   `dist/appsscript.json` into `appsscript.json`.
3. Add these **Script Properties** in Project Settings:
   - `SPREADSHEET_ID`: the spreadsheet's ID, not its URL.
   - `GOOGLE_WEB_CLIENT_ID`: your Web OAuth client ID, matching the Android build.
   - Optional `ALLOWED_GOOGLE_DOMAIN`: the exact Workspace domain. This checks the
     signed `hd` claim; an email suffix alone does not establish domain membership.
4. Run `setupTracker` once in the editor and authorize spreadsheet/external-request
   access. It adds two tabs without modifying other tabs:
   - `TrackerUsers`: `email, role, profileId, name, active, googleSubject`.
   - `TrackerRecords`: an append-only record journal with audit metadata.
5. Add your administrator to `TrackerUsers`: Google email, role `admin`, blank
   `profileId`, display name, `active` = TRUE. `googleSubject` can initially be blank;
   filling it with the account's Google subject ID additionally pins the identity.
6. Deploy as a **Web app**, executing as the owner, accessible to **Anyone** if your
   Workspace policy allows it. The public HTTP endpoint still requires a verified
   Google ID token and an active user allowlist entry for all data requests.
7. Configure the Android base URL as the deployment URL **without `exec`**, ending
   with `/`: `https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/`.
8. Configure Android OAuth registration for `com.example.studenttracker` and your
   actual debug/release signing certificate, plus the matching Web client ID.

## First complete workflow

1. Sign in as the allowlisted administrator.
2. Create faculty and student profiles in the app, then create subjects assigned to
   active faculty employee IDs.
3. In `TrackerUsers`, allowlist faculty using role `faculty` and `profileId` equal
   to their employee ID. Allowlist students with role `student` and `profileId`
   equal to their roll number. Adding a profile does not automatically grant access.
4. Open **Academic workspace → Manage enrolled students** for each subject.
5. Sign in as faculty, select an assigned subject, and submit a date/session roster.
6. Create an assessment, save marks as a draft, then publish after every entry is
   scored or absent. Published assessments are locked in this version.
7. Sign in as a student and verify that only that student's attendance and published
   results are visible. Sign in as faculty/admin to review and export attendance.

## API and authorization

- All actions use `POST /exec` with JSON containing `action` and `idToken`. GET
  requests never return student records. Tokens are not placed in URLs or logs.
- Responses contain `apiVersion: 2` and `status: success | error`. Errors include
  a machine-readable `code` and a user-facing `message`; Apps Script's HTTP status
  is not used to represent domain errors.
- RS256 signatures use Google's HTTPS public certificates, with issuer, audience,
  expiry, issue-time and verified-email checks. Certificate caching respects
  `Cache-Control`; an unknown signing key triggers one refresh.
- The user allowlist is read on every request. Faculty also need an active profile
  and subject assignment. Students need an active profile and only receive their
  own progress. Client-provided student/email parameters cannot select a different
  identity.
- Administrator reads: `getDashboard`, `getStudents`, `getStudent`, `getFaculty`,
  `getFacultyMember`, `getSubject`. Profile actions: `add/update/delete` +
  `Student/Faculty/Subject`.
- Faculty/admin actions: `getSubjects`, `getRoster`, `saveAttendance`,
  `getAttendanceReport`, `getAssessments`, `saveAssessment`.
- Enrollment writes are administrator-only: `saveEnrollment`.
- Student progress: `getMyProgress`; student `getSubjects` is limited to enrollment.

## Integrity and policies

- `TrackerRecords` holds complete entity revisions, including actor/action/time.
  A script lock serializes all access. Each mutation appends one aggregate row;
  attendance/assessment/enrollment retries are deduplicated and stale revisions are
  rejected. Identifiers and JSON are stored as text to preserve leading zeroes and
  prevent formula execution. Do not edit or sort this journal manually.
- Updating or deactivating profiles requires the last observed `revision`.
  Deactivation retains historical attendance/marks. Faculty with assigned subjects
  must be reassigned before deactivation. Identifiers cannot be reused.
- Attendance is unique by subject + date + case-insensitive session label. Present
  and late count as attendance; excused entries are excluded from the denominator.
  Only sessions with that student in the roster count for that student. No counted
  sessions means an unavailable percentage, not zero attendance.
- The institution-local date follows the manifest timezone (`Asia/Kolkata` by
  default). Change it for institutions in another timezone.
- Marks distinguish zero, absent and pending. Publishing requires all marks to be
  complete. The current version supports individual assessment scores, not weighted
  final grades or approved corrections to published results.
- Enrollment is limited to 250 students per subject; any aggregate above 40,000 JSON
  characters is rejected before writing. This is a guard, not a measured capacity
  guarantee. Every request currently rebuilds projections from the journal; pagination,
  compaction and load testing are required before a large deployment.

## Existing data and release gates

Legacy Students/Faculty/Subjects/Attendance tabs are not automatically imported.
Back up the legacy sheet, map its columns explicitly, and rehearse import into a
staging spreadsheet before switching the app. No legacy data has been changed by
this implementation.

Before public release, verify the real Apps Script runtime and Google sign-in flow,
perform a backup/restore drill, load-test the expected institution size, set up
monitoring and establish a rollback procedure. Google accounts and cloud deployment
configuration are required for those checks.

The RSA verifier uses pinned `node-forge` 1.4.0, including its corrected RSA signature
validation. Review dependencies and run the audit before each deployment.
The generated third-party license file must accompany distribution of the backend bundle.

References:
- [Google ID-token verification](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token)
- [Apps Script locks](https://developers.google.com/apps-script/reference/lock/lock-service)
- [Forge cryptography library](https://github.com/digitalbazaar/forge)
- [Forge RSA signature verification fix](https://github.com/digitalbazaar/forge/security/advisories/GHSA-ppp5-5v6c-4jwp)
