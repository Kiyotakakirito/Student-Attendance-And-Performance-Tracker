# Production implementation plan

## Objective
Complete the administrator → faculty → student workflow with trustworthy records,
then prove operational readiness before a public release. Existing IDE changes are
outside this work. Google Sheets and Apps Script remain the required backend.

## Implementation order and acceptance criteria

1. **Build and authentication foundation**
   - Reproducible Windows build, external environment configuration, typed API failures.
   - Google identity verified on the server; deny unknown/inactive users and unsupported roles.
   - All reads/writes authorized on the server; no authentication based on submitted email.
   - Explicit expiration and logout; no credentials in logs, URLs, or device backups.
2. **Connected academic workflows**
   - Real administrator counts and role-specific dashboards with loading/error/retry states.
   - Explicit subject enrollment; faculty see assigned subjects and minimal roster details.
   - Attendance by subject/date/session, complete roster validation, duplicate prevention.
   - Assessments with validated marks, drafts and explicit publication; students see only their own published results.
   - Attendance reports derived from submitted sessions, safe CSV export, empty-data handling.
3. **Academic lifecycle and record integrity**
   - First-class terms, departments, programs, sections and subject offerings.
   - Enrollment dates, transfers, electives, substitutions, promotion and archival.
   - Configurable attendance/grade policy, weighted assessments, retests and exemptions.
   - Versioned corrections with approval/locking; audit trail and safe deactivation.
   - Validated bulk imports with preview and actionable row errors.
4. **Reliability and user experience**
   - Durable offline drafts, retries with idempotency, conflict recovery and sync indicators.
   - Stable record-ID navigation, process recreation and accessibility/device testing.
   - Notifications, correction requests, date-filtered reports and printable progress reports.
   - Pagination, measured response times and tested maximum institution size.
5. **Release gates**
   - End-to-end tests against a separate staging backend and real Google credentials.
   - Permission, concurrency, calculation, export and failure-path tests pass.
   - Backup/restore drill, monitoring/alerts, retention policy and support runbook.
   - Release signing, environment separation, migration/rollback rehearsal and device QA.

## Backend decision
Google Sheets and Apps Script are compulsory, as confirmed by the owner.
Google recommends a supported server library for production ID-token verification;
the tokeninfo debugging endpoint is not a production authentication strategy.
Use a bundled RSA verification library inside Apps Script with Google's rotating public
certificates, issuer/audience/expiry checks and an explicit user allowlist.
The new backend must be configured and staged before
the updated Android client can connect; never silently fall back to the old email-only API.

## Scope and evidence
Each implementation pass must update this file with delivered work, exact checks and
remaining release blockers. A successful debug build is not proof of production readiness.

## Current status
- Initial connected implementation complete locally; the demo backend is now deployed
  and administrator, faculty and student accounts are provisioned. Live device login
  and the full academic workflow still require verification.
- Detailed implemented, next-work and optional checklists: `docs/FEATURE_CHECKLIST.md`.
  Live configuration evidence: `docs/DEMO_SETUP_STATUS.md`.
- Windows wrapper now supports the ampersand in the checkout path. Use JDK 21 or
  Android Studio's bundled JDK; the workstation's default Java 8 cannot build this app.
- Authentication uses signed Google tokens in POST bodies, server role checks,
  explicit expiration and logout. Credentials remain in memory. Stored role metadata
  cannot authenticate a restarted app. The old email-only API is refused.
- Administrator counts, searchable directories, stable profile-ID navigation,
  enrollment, faculty attendance/assessments, student progress and CSV reports are connected.
- Profile/enrollment/assessment revision conflicts, attendance uniqueness, retry
  deduplication, complete roster validation and append-only audit metadata are implemented.
- Deactivation preserves records; form values survive rotation and all fields participate
  in unsaved-change checks. Loading, errors and retries are explicit.
- The generated backend stays within Google Sheets and Apps Script. RSA verification
  uses pinned node-forge 1.4.0 with the library's corrected signature validation.
- Deployment instructions: `backend/README.md`. Real acceptance checklist:
  `docs/STAGING_CHECKLIST.md`. Environment example: `gradle.properties.example`.

## Verification evidence
- Android: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed.
- Android unit tests: 9 passed (CSV handling, network credential transport/failures,
  plus the existing example test). Lint: zero errors; 33 non-blocking warnings remain,
  primarily dependency updates, version-catalog suggestions and unused resources.
- Backend: generated bundle built; 27 tests passed for permissions, RSA signatures,
  X509 certificates/Unicode claims, dispatcher/storage, attendance/marks, conflicts
  and retries.
- npm audit: zero reported vulnerabilities in the pinned backend dependency.
- Git whitespace check passed. Existing `.idea` modifications were not edited.

## Remaining release blockers and next milestones
- Install the configured debug APK and verify all three roles against the deployed
  demo backend; create a subject, assign faculty and enroll the demo student.
- Establish an independent staging environment before production release.
- Validate Google sign-in, the actual Apps Script runtime, key rotation, concurrency,
  narrow/large screens, accessibility and interrupted submissions on devices.
- Rehearse a mapped legacy-data import, backup/restore, monitoring and rollback.
- Decide the secure session renewal experience; complete durable offline drafts/sync, first-class academic
  terms/offerings, configurable policy/grades, approved published-record corrections,
  bulk imports, notifications and report filters.
- Measure expected institution capacity; current journal projection reads are not paginated
  and enrollment is guarded at 250 students per subject / 40,000-character aggregates.
- Public release remains pending these checks and milestones.
