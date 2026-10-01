# Student Tracker feature checklist

Updated: 1 October 2026. Backend: Google Sheets and Google Apps Script, as required.

This checklist separates implemented code, unfinished production work, and optional ideas. Checked boxes mean the implementation or documented setup exists; they do not mean every feature has passed testing on a real device. Unchecked boxes are outstanding. Optional features are candidates, not commitments.

## 1. Built so far

### Login, identity and access
- [x] Google sign-in through Android Credential Manager.
- [x] Server verification of signed Google ID tokens, including issuer, audience, expiry and verified email.
- [x] Google certificate caching and refresh when an unfamiliar signing key appears.
- [x] Active-user allowlist with administrator, faculty and student roles.
- [x] Optional Google account subject binding and hosted-domain restriction.
- [x] Rejection of unknown, inactive, duplicate or unsupported account mappings.
- [x] Active profile requirement for faculty and students.
- [x] Server permission checks for every supported action.
- [x] Faculty access limited to assigned subjects.
- [x] Student access limited to the authenticated student's records.
- [x] Role-specific navigation and dashboards.
- [x] Explicit logout and expired-session cleanup.
- [x] Tokens kept in memory; saved email/role metadata cannot authenticate a restarted app.
- [x] Credentials transported in JSON request bodies rather than URLs.

### Administrator and directories
- [x] Dashboard counts drawn from actual active records.
- [x] Student directory with search, details, creation and editing.
- [x] Faculty directory with search, details, creation and editing.
- [x] Subject directory with search, details, creation and editing.
- [x] Student academic, contact and guardian information fields.
- [x] Faculty employment and contact information fields.
- [x] Subject faculty assignment, semester and planned class count.
- [x] Required-field, email and supported numeric validation.
- [x] Duplicate identifier and same-kind email checks.
- [x] Immutable record identifiers.
- [x] Record deactivation that preserves historical data.
- [x] Prevention of faculty deactivation while active subjects remain assigned.
- [x] Stable record-ID navigation and fresh record loading on details/edit screens.
- [x] Revision checks that reject stale profile edits.

### Subjects and enrollment
- [x] Academic hub for subject workflows.
- [x] Faculty subject list filtered by assignment.
- [x] Administrator selection of enrolled students for a subject.
- [x] Validation of distinct, active student IDs.
- [x] Revision checks for concurrent enrollment changes.
- [x] Enrollment size and stored aggregate limits to bound requests.

### Attendance
- [x] Attendance entry by subject, date and session slot.
- [x] Enrolled roster shown for attendance entry.
- [x] Present, absent, late and excused statuses.
- [x] Mark-all-present action.
- [x] Completed-roster count and full-roster validation before submission.
- [x] Submission confirmation and save-in-progress controls.
- [x] Rejection of invalid dates and future attendance dates using the institution's configured script timezone.
- [x] Unique subject/date/slot sessions to prevent duplicate attendance.
- [x] Matching request retries deduplicated by request ID.
- [x] Historical roster/name snapshots saved with each session.
- [x] Attendance calculation: present and late count as attended; excused is excluded from the denominator.
- [x] N/A display when no counted attendance exists.
- [x] Students not penalized for historical sessions where they were outside the recorded roster.
- [x] Submitted sessions protected from direct overwrite.

### Assessments and marks
- [x] Assessment list and editor for authorized subjects.
- [x] Assessment title and maximum-mark validation.
- [x] Marks support scored values, zero, absent and pending entries.
- [x] Mark-range and roster validation.
- [x] Draft assessment saving.
- [x] Explicit publication confirmation.
- [x] Publication blocked while marks remain pending.
- [x] Published assessments locked against direct editing.
- [x] Revision checks and matching-retry handling for assessment changes.
- [x] Students see only their own published results.

### Student dashboard and reports
- [x] Student attendance summary and progress indicators.
- [x] Student published assessment results.
- [x] Dashboard refresh and logout actions.
- [x] Subject attendance reports with permission checks.
- [x] Attendance calculation rules explained in the report.
- [x] Shortage indication using the current fixed 75% threshold.
- [x] CSV report export and Android sharing.
- [x] CSV handling for Unicode, quoted fields, embedded quotes and spreadsheet formula prefixes.
- [x] Export filenames sanitized and report generated from a selected subject snapshot.
- [x] Temporary read permission through a narrowly scoped FileProvider.
- [x] Cleanup of export files older than 24 hours when export cleanup runs.

### App experience and transport
- [x] Shared screen components and consistent dark theme.
- [x] Loading, empty, failure and retry states for remote content.
- [x] Busy controls to reduce accidental duplicate submissions.
- [x] Form values retained through supported configuration changes.
- [x] Unsaved-change detection across form fields and navigation confirmation.
- [x] Typed API failure handling and bounded network timeouts.
- [x] Versioned API requests; legacy email-only authentication refused.
- [x] Cleartext network traffic disabled.
- [x] Device backup disabled with sensitive storage exclusions configured.

### Apps Script, storage and local verification
- [x] Spreadsheet initialization for TrackerUsers and TrackerRecords.
- [x] Append-only record journal with actor, action and timestamp metadata.
- [x] Current records reconstructed from journal revisions.
- [x] Script locking around supported storage operations.
- [x] Request/token/field-size limits and masked internal errors.
- [x] Bundled, pinned RSA verification library and retained third-party license.
- [x] External Android configuration for deployment URL and Google Web client ID.
- [x] Reproducible backend bundle and Windows-compatible Android build.
- [x] Backend test suite: 27 tests passed in the recorded verification run.
- [x] Android JVM test suite: 9 tests passed in the recorded verification run.
- [x] Debug build and lint passed; lint has 0 errors and 33 remaining warnings in the recorded run.

### Demo setup already completed
- [x] Existing matching Android OAuth client identified.
- [x] Web OAuth client ID configured in Android and Apps Script.
- [x] Demo spreadsheet and Apps Script backend configured.
- [x] Web app deployment completed and deployment URL saved.
- [x] Live spreadsheet initialization executed successfully.
- [x] Demo administrator account added.
- [x] Faculty account and linked F001 profile created.
- [x] Kirito student account and linked S001 profile created.
- [x] Debug APK rebuilt using the real backend configuration.

## 2. Next work: verify the working demo first

These are immediate acceptance tasks, not claims of missing implementation.

- [ ] Install the rebuilt APK on the presentation device.
- [ ] Verify actual Google sign-in against the deployed Apps Script runtime.
- [ ] Check OAuth audience/test-account access and resolve any sign-in restriction.
- [ ] Verify administrator login and dashboard with live data.
- [ ] Replace demo placeholder profile details with correct institution information.
- [ ] Create a demo subject and assign it to the faculty profile.
- [ ] Enroll Kirito in the demo subject.
- [ ] Verify faculty login and assigned-subject visibility.
- [ ] Submit a complete attendance session and check the saved spreadsheet record.
- [ ] Save an assessment draft, enter marks and publish it.
- [ ] Verify student login, attendance percentage and published marks.
- [ ] Export and open a CSV on the actual phone.
- [ ] Verify logout, account switching and access rejection for an unlisted account.
- [ ] Rehearse the full administrator → faculty → student presentation.

## 3. Next features: production roadmap

The order below is proposed. Work in every production release-gate section must be complete before claiming production readiness; optional features can wait.

### A. Account management and institution settings
- [ ] Administrator screen to provision, link and manage login accounts without manually editing Sheets.
- [ ] Invitation, activation, suspension and archive workflows with clear statuses.
- [ ] Audited role changes and safeguards against losing the last administrator.
- [ ] Profile/account integrity checks and actionable repair messages.
- [ ] Session reauthentication that preserves unfinished work safely.
- [ ] Explicit account switching and removal of the previous account's local data.
- [ ] Decide and implement the required persistent sign-in/renewal experience.
- [ ] Institution name, logo, timezone and support contact settings.
- [ ] Attendance and assessment policy settings with effective dates.

### B. Academic structure and lifecycle
- [ ] Department and program master records with validated selection controls.
- [ ] Academic years and terms with start/end dates and active-term selection.
- [ ] Batches and class sections.
- [ ] Subject offerings tied to a term, section and assigned faculty.
- [ ] Enrollment start/end dates and withdrawals.
- [ ] Transfers and corrections to enrollment without rewriting past attendance.
- [ ] Elective enrollment where required by the institution.
- [ ] Controlled substitute-faculty access with expiry.
- [ ] Academic calendar, holidays and non-teaching days.
- [ ] Promotion, term closure and historical archive access.
- [ ] Rules preventing edits to closed academic periods.

### C. Attendance completion and corrections
- [ ] Session history and detailed roster/status views.
- [ ] Scheduled-session tracking and missing-attendance detection.
- [ ] Roster search and useful sorting on large attendance lists.
- [ ] Bulk status actions with undo before submission.
- [ ] Durable attendance drafts that survive process death and phone restart.
- [ ] Clear draft, queued, submitting, submitted and failed states.
- [ ] Lost-response recovery that checks whether a submission already succeeded.
- [ ] Configurable thresholds and late/excused counting rules.
- [ ] Cancelled-session handling and exclusion from calculations.
- [ ] Correction requests with a mandatory reason.
- [ ] Authorized correction approval and versioned before/after records.
- [ ] Student attendance discrepancy requests and status tracking.
- [ ] Leave/exemption rules if the institution uses them.
- [ ] Attendance shortage alerts and missing-submission reminders.

### D. Assessment policies and corrections
- [ ] Assessment types, dates and term association.
- [ ] Weightage, final-result calculation and rounding policy.
- [ ] Configurable pass criteria and grade boundaries.
- [ ] Retest/attempt handling according to institution policy.
- [ ] Exempt/not-applicable statuses distinct from absent and zero.
- [ ] Durable assessment drafts and lost-response recovery.
- [ ] Validated bulk marks import with preview and row errors.
- [ ] Publication preview showing recipients, missing data and resulting totals.
- [ ] Approved corrections to published marks with reasons and audit history.
- [ ] Student feedback and explanation of result calculations.
- [ ] Assessment completion and missing-marks reports.

### E. Administration, import and data quality
- [ ] CSV templates for student, faculty, subject and enrollment imports.
- [ ] Import preview with duplicate, missing-field and invalid-reference detection.
- [ ] Per-row error reports and safe retry of failed import rows.
- [ ] Bulk faculty assignments and enrollment operations.
- [ ] Dependency warnings before destructive lifecycle changes.
- [ ] Controlled restoration of archived records.
- [ ] Data-quality dashboard for unmapped accounts and inconsistent records.
- [ ] Searchable audit viewer showing who changed what and when.
- [ ] Explicit institution rules for required fields and identifier formats.

### F. Student information and reports
- [ ] Attendance session history and date/term filters.
- [ ] Detailed assessment view with feedback and grade breakdown.
- [ ] Profile correction requests rather than unrestricted edits to official records.
- [ ] Reports filtered by term, department, section, subject and date range.
- [ ] Policy-aware shortage and missing-attendance reports.
- [ ] Printable student progress and attendance PDFs.
- [ ] Report branding, filter labels, generation time and readable page breaks.
- [ ] Consistent authorized report snapshots and calculation reconciliation.
- [ ] Bounded export jobs for reports too large for a single request.

### G. Reliable offline behavior
- [ ] Account-isolated local storage for drafts and allowed cached records.
- [ ] Cached-data age and offline indicators.
- [ ] Persistent operation IDs and a controlled submission queue.
- [ ] Bounded retry/backoff for temporary failures.
- [ ] No automatic retry of permission or validation failures.
- [ ] Conflict UI for changed records and enrollment rosters.
- [ ] Permission revalidation before queued writes synchronize.
- [ ] Manual retry, discard and draft recovery actions.
- [ ] Local cache/draft cleanup according to logout and retention policy.

### H. Small user-experience details
- [ ] Field-level errors, required indicators and clear examples.
- [ ] Whitespace normalization without losing leading zeros in identifiers.
- [ ] Date picker and consistent institution-timezone date display.
- [ ] Appropriate keyboards, Next/Done actions and visible focused fields.
- [ ] Specific success/error messages that survive enough time to be read.
- [ ] Search clearing, result counts and useful sort/filter controls.
- [ ] Refresh that keeps existing data visible while updating.
- [ ] Distinct no-results, no-permission and connection-failure states.
- [ ] Scroll/filter restoration after navigating back.
- [ ] Unsaved-work protection during account and term changes.
- [ ] TalkBack labels, meaningful reading order and adequate touch targets.
- [ ] Contrast and status labels that do not rely only on color.
- [ ] Large-font, small-screen, tablet and landscape verification.
- [ ] Keyboard/system-bar overlap fixes where device testing finds issues.
- [ ] User-facing strings moved into resources for maintainability and localization.
- [ ] About screen, app version, help and support instructions.

### I. Sheets and Apps Script capacity and reliability
- [ ] Measure expected students, subjects, sessions and concurrent users.
- [ ] Replace full-journal reads per request with a recoverable efficient current-record projection.
- [ ] Bounded pagination and stable cursors for growing lists/history.
- [ ] Short lock duration, batched reads/writes and contention handling.
- [ ] Checkpointed larger jobs with recoverable partial failures.
- [ ] Schema versions, validated headers and migration scripts.
- [ ] Journal archival/compaction with preserved audit history.
- [ ] Quota-aware scheduling and useful limit-exceeded messages.
- [ ] Automated backups and a successfully rehearsed restore.
- [ ] Separate development, staging and production configurations.
- [ ] Monitoring for failures, latency, lock contention and quota usage.
- [ ] Operator alerts and an actionable incident runbook.
- [ ] Version rollback tested together with data compatibility.

### J. Security, privacy and production release gates
- [ ] Direct API tests for every role/action, forged identifiers and cross-student access.
- [ ] Live expiry, revocation and signing-key rotation checks.
- [ ] Abuse/rate controls appropriate to the measured deployment.
- [ ] Restricted spreadsheet sharing and institution-owned service administration.
- [ ] Audit coverage for account approvals, role changes and report exports.
- [ ] Redacted diagnostics with safe support/error identifiers.
- [ ] Privacy notice and minimum collection of contact/guardian data.
- [ ] Institution-approved retention, archive and deletion rules.
- [ ] Emergency administrator recovery procedure without an authentication bypass.
- [ ] Dependency/license review and an update policy.
- [ ] End-to-end device tests for all three roles against staging.
- [ ] Network loss, process death, rotation, concurrent editing and lost-response tests.
- [ ] Attendance/grade edge-case tests against agreed policy.
- [ ] Representative capacity, response-time and resource-use tests.
- [ ] Legacy-data migration rehearsal if existing records will be imported.
- [ ] Release keystore stored securely and release fingerprint registered with Google.
- [ ] Signed release build, versioning and installation/update verification.
- [ ] CI checks for builds, meaningful tests, lint and backend bundling.
- [ ] Resolve or document remaining lint warnings and known limitations.
- [ ] Support owner, deployment procedure and rollback instructions.
- [ ] Store listing, privacy disclosures and distribution requirements where applicable.
- [ ] Institution acceptance and release sign-off after the required gates pass.

## 4. Optional features: choose after the core is reliable

These need scope decisions. Some may become required if the institution requests them.

### Communication and scheduling
- [ ] In-app announcements and notification inbox.
- [ ] Push notifications with preferences, recipient checks, deduplication and private lock-screen content.
- [ ] Email summaries/reminders with delivery logs and quota-aware retries.
- [ ] SMS alerts with approved provider, cost limits and recipient consent.
- [ ] Timetable views and timetable management.
- [ ] Google Calendar integration with approved scopes and timezone handling.
- [ ] Quiet hours and notification-category preferences.
- [ ] Authorized deep links from notifications to the relevant record.

### Extended teaching and student workflows
- [ ] Assignment creation, submission and deadlines.
- [ ] Drive-backed attachments with file-size/type limits, access controls and retention.
- [ ] Rubric-based grading.
- [ ] Formal revaluation workflow and appeals.
- [ ] Faculty feedback/comments on student progress.
- [ ] Guardian portal restricted to explicitly linked students.
- [ ] Scoped class messaging with moderation and retention rules.
- [ ] Multiple faculty teaching one subject.
- [ ] Department-head/delegated roles and multiple roles per account.
- [ ] Multi-campus support with explicit data separation.

### Attendance enhancements
- [ ] Rotating QR attendance with expiry, replay prevention and faculty review.
- [ ] Location-assisted attendance with permission handling and a manual fallback.
- [ ] Device binding if justified by institution policy and recoverable when a phone changes.
- [ ] Student attendance recovery estimate based on the configured threshold.
- [ ] Biometric attendance only after an explicit need, privacy review and an alternative method are agreed.

### Analytics and personalization
- [ ] Attendance and marks trend charts.
- [ ] Class-level assessment distribution and teaching analytics.
- [ ] Early-support indicators with transparent calculations and human review.
- [ ] AI-assisted summaries or predictions with validated limitations and approved data use.
- [ ] Student goals and progress reminders.
- [ ] Light/dark/system theme selection.
- [ ] Additional interface languages according to actual user needs.
- [ ] Home-screen widgets or shortcuts that protect student information.
- [ ] Institution-approved engagement features such as badges.

## Evidence and limitations

The recorded local checks are useful implementation evidence, not a live acceptance certificate. No new test run is implied by this checklist. Real Google login, device behavior and the complete deployed academic workflow remain unverified. The current app uses a fixed attendance threshold, has no durable offline queue, and does not yet support approved corrections to submitted attendance or published marks. Journal reads currently grow with record history.

Related documents:
- [Production implementation plan](../PRODUCTION_PLAN.md)
- [Public setup and current status](../README.md)
- [Staging acceptance checklist](STAGING_CHECKLIST.md)
- [Backend setup instructions](../backend/README.md)
- [Seven-member study plan](SEVEN_MEMBER_STUDY_PLAN.md)
- [Presentation script](PRESENTATION_SCRIPT.md)
