from pathlib import Path
import html, math, json
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, Flowable
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/pdf/Student_Tracker_SRS.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)
SOURCE = ROOT / 'docs/Student_Tracker_SRS.md'
for name, file in [('Body', 'segoeui.ttf'), ('Bold', 'segoeuib.ttf')]:
    pdfmetrics.registerFont(TTFont(name, 'C:/Windows/Fonts/' + file))
INK = colors.HexColor('#18334F')
BLUE = colors.HexColor('#245F8A')
PALE = colors.HexColor('#EEF4F9')
GREEN = colors.HexColor('#EAF5EE')
ORANGE = colors.HexColor('#FFF2DA')
GRAY = colors.HexColor('#627285')
W = A4[0] - 92
styles = getSampleStyleSheet()
for name, size, leading, extra in [
    ('BodyX', 10, 15, {'spaceAfter': 8}),
    ('SmallX', 8.7, 12, {'spaceAfter': 5}),
    ('H1X', 19, 24, {'fontName': 'Bold', 'textColor': INK, 'spaceAfter': 14}),
    ('H2X', 12, 17, {'fontName': 'Bold', 'textColor': INK, 'spaceBefore': 9, 'spaceAfter': 7}),
    ('TableX', 8.5, 12, {}),
    ('TitleX', 30, 36, {'fontName': 'Bold', 'textColor': INK, 'spaceAfter': 15}),
]:
    styles.add(ParagraphStyle(name=name, fontSize=size, leading=leading, **({'fontName':'Body'} | extra)))

def p(s, style='BodyX'):
    return Paragraph(html.escape(str(s)), styles[style])

def table(rows, widths=None):
    items = [[p(v, 'TableX') for v in row] for row in rows]
    t = Table(items, colWidths=widths, repeatRows=1, hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), INK),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, PALE]),
        ('GRID', (0, 0), (-1, -1), .35, colors.HexColor('#C8D3DF')),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
    ]))
    for cell in items[0]:
        cell.style = ParagraphStyle('TH', parent=styles['TableX'], fontName='Bold', textColor=colors.white)
    return t

pages = []
def page(title, elements):
    pages.append((title, elements))
def para(s): return ('p', s)
def sub(s): return ('h', s)
def tab(rows, widths=None): return ('t', (rows, widths))
def bullets(items): return [('b', x) for x in items]

requirements = [
('01','Application launch','Built','All users','Tap the installed app icon.','Initialize Compose and configuration; use only a valid in-memory session for routing. A fresh process requires Google sign-in.','Login screen or the verified role dashboard.','Saved email/role alone must never bypass login.'),
('02','Google sign-in','Built','All users','Select a Google account through Credential Manager.','Obtain an ID token for the configured Web client; POST it to Apps Script; verify signature, issuer, audience, expiry and verified email.','Authenticated identity or a readable authentication failure.','An altered, expired or wrong-audience token is rejected.'),
('03','Role and profile authorization','Built','All users','Verified Google claims and an allowlist entry.','Require one active TrackerUsers mapping; apply role checks server-side. Faculty/student require active matching profiles. Scope faculty by assignment and students by their verified profile.','Only authorized records/actions are returned.','An unknown account or forged student/subject identifier cannot expose restricted data.'),
('04','Session expiry and logout','Built','All users','Token expiry, unauthenticated response or Logout action.','Clear the memory token and identity; clear saved session metadata on logout; rebuild navigation for the new authentication state.','Return to login without previous-role access.','No token is persisted and a restarted app cannot authenticate from metadata.'),
('05','Administrator dashboard','Built','Administrator','Open the administrator home screen.','Retrieve active student, faculty and subject counts; derive department count from active student department values; show management navigation.','Current counts or explicit loading/error/empty feedback.','Counts match active records rather than hard-coded values.'),
('06','Student management','Built','Administrator','Student ID, name, email, department and optional academic/contact/guardian fields.','Search, view, create and edit student records; validate required values and duplicate identifiers/emails; preserve stable IDs and check revisions.','Saved profile, directory entry or actionable validation/conflict error.','Duplicate/archived IDs and stale edits are rejected; optional fields remain optional.'),
('07','Faculty management','Built','Administrator','Employee ID, name, email, department and optional employment/contact fields.','Search, view, add and revise profiles; validate identity fields and check current revisions.','Current faculty directory and saved profile.','A duplicate email/ID or stale revision cannot overwrite another record.'),
('08','Subject management','Built','Administrator','Subject code/name, active faculty ID, semester and planned class count.','Validate faculty reference; require whole-number planned classes from 1 to 1000; save subject with immutable code and revision.','Subject directory, details and assigned faculty link.','Invalid faculty references and out-of-range/non-integer class counts are rejected.'),
('09','Safe deactivation','Built','Administrator','Record ID and expected revision.','Set active=false without removing historical journal entries; block faculty deactivation until active subjects are reassigned.','Deactivated record or dependency/conflict message.','Past attendance/results remain stored after supported deactivation.'),
('10','Subject enrollment','Built','Administrator','Subject code, selected student IDs and current enrollment revision.','Require distinct active students; allow at most 250; compare revision and persist the roster. Matching retries do not create a second change.','Updated subject roster and revision.','Stale, duplicate or inactive roster entries are rejected.'),
('11','Faculty workspace','Built','Faculty; administrator','Open the academic hub.','Load active subjects within the caller scope; offer attendance, assessment and report actions. Administrator also receives enrollment management.','Authorized subject cards and actions.','Faculty cannot read/write a subject assigned to another faculty profile.'),
('12','Attendance capture','Built','Faculty; administrator','Authorized subject, date, slot and one status per enrolled student.','Load minimal roster; mark present/absent/late/excused; support mark-all-present; validate real nonfuture date and complete current roster; confirm submission.','Complete validated attendance payload or visible field/roster error.','Missing, duplicate, extra or invalid-status entries cannot be submitted.'),
('13','Attendance persistence','Built','Faculty; administrator','Attendance payload and stable request ID.','Use subject/date/lowercase-slot as the unique key; append a session snapshot including student names. Accept an identical retry; reject a competing overwrite.','Submission success, already-submitted success or conflict.','Retrying the same logical submission does not create duplicate attendance.'),
('14','Attendance calculation','Built','Authorized report viewer; student','Stored attendance session snapshots.','Count present+late as attended and present+late+absent as counted; exclude excused. Use only sessions containing that student. Display N/A for zero denominator.','Percentage rounded to one decimal with present and counted totals.','One present, one late, one absent and one excused produces 66.7%, not 50%.'),
('15','Assessment drafts','Built','Faculty; administrator','Assessment ID, subject, title, maximum marks, revision, request ID and complete roster marks.','Validate maximum marks from 1 to 10000; accept scored 0..maximum, absent or pending; save unpublished draft; reject stale revision and mismatched request reuse.','Saved draft with updated revision.','Zero is a scored value; pending and absent are distinct; out-of-range scores fail.'),
('16','Assessment publication','Built','Faculty; administrator','Completed draft and explicit publish confirmation.','Require every entry scored or absent; save published=true; lock the published record against direct edits; protect student visibility.','Published results accessible only to the relevant student and authorized staff.','Pending entries block publication; students cannot retrieve draft marks.'),
('17','Student progress','Built','Student','Open or refresh own dashboard.','Derive student ID from the verified profile; retrieve own attendance summaries and own published mark entries.','Personal attendance and results, including no-data states.','Supplying another student ID does not change the authenticated scope.'),
('18','Reports and CSV sharing','Built','Administrator; assigned faculty','Select an authorized subject and export its report.','Calculate a consistent report snapshot; show current fixed 75% shortage rule; quote CSV fields and neutralize formula prefixes; share a temporary cache file with narrow read permission.','Readable UTF-8 CSV and Android share chooser.','Unicode/commas/quotes remain valid; unauthorized report access fails.'),
('19','Loading, retry and form protection','Built','All users','Screen load, failed request, field edit or Back action.','Display loading/error/empty states; retry on demand; disable busy writes; preserve supported configuration-change form state; warn before discarding dirty forms.','Clear feedback and guarded navigation.','A failed request is not shown as an empty successful dataset; dirty Back asks before discarding.'),
('20','Audit journal and conflicts','Built','Backend; authorized operators','Authorized mutation and expected revision/request identity.','Lock supported operations; append kind, ID, data, time, actor and action; reconstruct latest record; reject stale changes.','Traceable journal update or conflict/busy response.','Concurrent stale edits cannot silently replace a newer version.'),
('21','In-app account administration','Planned','Administrator','Approved account email, role, profile mapping and lifecycle action.','Provide account provisioning/suspension screens, integrity checks and audited role changes; preserve at least one recoverable administrator.','Managed account status without routine manual spreadsheet editing.','Suspended users lose server access and incompatible mappings are refused.'),
('22','Academic lifecycle and policies','Planned','Administrator','Terms, programs, sections, enrollment dates and policy settings.','Link subject offerings to academic periods; handle transfers, term closure and policy effective dates; calculate threshold/grade results using agreed rules.','Term-aware records and configurable institutional policies.','A policy change does not silently rewrite closed historical results.'),
('23','Approved record corrections','Planned','Student/requester; faculty; administrator','Record reference, correction reason and proposed values.','Track requests, verify approver permissions, preserve before/after versions and recalculate affected results after approval.','Pending/approved/rejected request with traceable outcome.','Submitted attendance and published marks change only through the authorized correction workflow.'),
('24','Durable offline drafts and sync','Planned','Faculty; administrator','Draft or queued operation while offline; reconnection.','Persist account-isolated drafts and stable operation IDs; revalidate permissions/rosters; retry temporary failures with bounded backoff; show conflicts for human resolution.','Recovered drafts and explicit pending/synced/failed status.','Process death preserves drafts; sync does not duplicate writes or use last-write-wins for conflicts.'),
('25','Bulk import and advanced reporting','Planned','Administrator; scoped report viewers','Import file/template or report filters.','Preview imports, validate rows and references, report errors; support term/department/section/date filters and printable branded progress PDFs.','Approved imported rows, error report or filtered output.','Invalid rows cannot silently corrupt records; report scope matches caller permissions.'),
('26','Operations and release readiness','Planned','Institution operator','Backup schedule, staged release and support incident.','Use separate environments; test restore, migrations and rollback; monitor failures/capacity; register release signing; document ownership/privacy/support.','Recoverable, monitored deployment with accepted release evidence.','Restore and all-role device workflow pass before production sign-off.'),
('27','Optional extensions','Optional','Institution-approved roles','An explicitly approved extension request.','Consider QR/location-assisted attendance, notifications, timetable, assignments, guardian access or analytics with scoped permissions and documented dependencies.','Separately approved enhancement.','No optional feature is represented as implemented or required without an institution decision.'),
]

page('Software Requirements Specification', [
    ('cover','Student Attendance &\nPerformance Tracker'),
    para('An Android application for institution administration, faculty attendance and assessment management, and student progress monitoring.'),
    tab([['Document control','Value'],['Identification','CUTM-SRS-SAPT-VER-0.1'],['Abbreviation','SAPT / Student Tracker'],['Document version','0.1 - academic review draft'],['Baseline date','1 October 2026'],['Application build version','1.0 (versionCode 1); production release pending'],['Backend constraint','Google Sheets + Google Apps Script'],['Prepared for','Seven-member project team and academic reviewers']], [155,W-155]),
    sub('Document purpose'),
    para('Specify the system scope, functional behavior, interfaces, data structures, quality requirements and verification criteria. The section order follows the supplied Library Management System SRS sample; the content describes this project.'),
    sub('Implementation status legend'),
    para('Built = present in the inspected source or documented setup. Planned = outstanding production requirement. Optional = candidate extension requiring a scope decision. Built does not imply live-device acceptance.'),
    para('This specification includes ten labeled diagrams and flowcharts. They depict the baseline architecture unless explicitly labeled as a proposed extension.'),
])
page('Contents', [])
page('1. Scope', [
    para('The Student Attendance & Performance Tracker replaces fragmented manual attendance sheets and mark records with role-controlled Android workflows. Administrators maintain profiles, subjects and enrollment; faculty record attendance and publish assessments; students view their own progress.'),
    sub('1.1 Identification'),
    tab([['Item','Specification'],['System title','Student Attendance & Performance Tracker'],['Document ID','CUTM-SRS-SAPT-VER-0.1'],['Package identifier','com.example.studenttracker'],['Document version / release','0.1 / review draft; not a production release approval'],['Actors','Administrator, faculty, student and backend operator']], [155,W-155]),
    sub('1.2 System overview'),
    para('The frontend is written in Kotlin using Jetpack Compose and Material 3. Retrofit/OkHttp transports JSON POST requests to an Apps Script web app. The backend verifies Google identity, enforces permissions and stores academic records in a private Google spreadsheet.'),
    para('The existing implementation uses Compose screen state, Kotlin coroutines and StateFlow, with a direct network layer. It does not currently implement a complete ViewModel/repository architecture, Room database, Hilt or WorkManager.'),
    sub('1.3 Document overview'),
    para('Sections 1-4 follow the sample: scope, references, requirements and database backend. Sections 5-7 add compulsory system diagrams, acceptance/traceability and the production roadmap.'),
    sub('Scope boundary'),
    para('Fees, library operations, biometric identity, admissions, payroll and general campus ERP functions are outside the baseline. Optional attendance/communication integrations are listed separately rather than assumed implemented.'),
])
page('1. Scope - environment and users', [
    sub('1.4 Software and hardware requirements'),
    tab([['Environment','Requirement / recommendation'],['Development','Windows workstation, Android Studio, JDK 21 and Gradle wrapper; Node.js 22+ for backend bundling/tests.'],['Development hardware','Project recommendation: modern quad-core processor, 16 GB RAM and SSD with space for Android SDK/emulators. Not a measured minimum.'],['Client device','Android 7.0/API 24 or newer; Google account and compatible Google credential services. No camera requirement for baseline attendance.'],['Network','Internet required for authentication and current academic reads/writes. Offline queue is planned.'],['Cloud','Institution-controlled Google account, OAuth clients, Apps Script project and private spreadsheet.'],['Testing','Physical phone or emulator; real phone verification required for sign-in, sharing and presentation.']], [125,W-125]),
    sub('1.5 Brief software functional description'),
    para('After Google sign-in and server role verification, the app opens an administrator, faculty or student workspace. Administrators prepare academic records and enroll students. Assigned faculty submit complete attendance rosters and assessment drafts, then publish complete results. Students receive only their own summaries and published marks.'),
    sub('1.6 Functional requirement groups'),
    para('Data management: profiles, subjects, enrollment, attendance, assessments and audit records. Administration: dashboards, directories and safe deactivation. User work: assigned faculty workflows, student progress, reports, refresh and logout.'),
    sub('1.7 Configuration and operating boundary'),
    para('Android receives the deployment base URL and Web OAuth client ID from external build configuration. Script Properties contain SPREADSHEET_ID and GOOGLE_WEB_CLIENT_ID, with optional ALLOWED_GOOGLE_DOMAIN. Institution-local dates use the Apps Script timezone. The current journal model is capacity-limited and must be load-tested before production use.'),
])
page('1.8 Development details / 2. References', [
    sub('1.8 Development details'),
    tab([['Role','Responsibility'],['Seven-member development team','Implement, review, document and present the project; individual member names are not supplied.'],['Institution administrator','Approve accounts, maintain academic records and review reports.'],['Faculty users','Maintain attendance and assessments for assigned subjects.'],['Student users','View personal attendance and published performance.'],['Backend operator','Maintain cloud ownership, configuration, backups and releases.']], [155,W-155]),
    sub('2. Referenced documents'),
    para('R1. Supplied SRS_Library_Management_System.pdf, 15 pages: structural and formatting example only. Library-specific requirements are not inherited.'),
    para('R2. docs/FEATURE_CHECKLIST.md: implemented features, production work and optional backlog.'),
    para('R3. PRODUCTION_PLAN.md and docs/STAGING_CHECKLIST.md: milestones and acceptance gates.'),
    para('R4. docs/DEMO_SETUP_STATUS.md: deployment and demo-account evidence, with live verification limitations.'),
    para('R5. Android source under app/src/main, app/build.gradle.kts and gradle/libs.versions.toml: client behavior and build configuration.'),
    para('R6. backend/src/core.js, auth.js and apps-script.js: authorization, domain rules and physical storage.'),
    para('R7. Backend and Android local test suites: recorded automated verification evidence.'),
    sub('3.1 Required mode of operation'),
    para('Baseline operation is online. Google sign-in requires an active approved account; all academic operations pass through the authenticated backend. Tokens remain in memory and a fresh process requires sign-in. Configuration-change form retention is present; durable offline records and automatic synchronization are not.'),
])
page('3.2 System capability requirements', [
    sub('3.2.1 System software requirements'),
    tab([['Category','Baseline technology / status'],['IDE and language','Android Studio; Kotlin'],['User interface','Jetpack Compose + Material 3; fixed dark theme'],['Asynchronous state','Kotlin coroutines; StateFlow for authentication; Compose state for screens'],['Navigation','Navigation Compose; stable encoded record IDs'],['Networking / serialization','Retrofit 2.11.0, OkHttp 4.12.0 and Gson'],['Authentication','Credential Manager + Google ID tokens; backend RSA signature verification'],['Local preferences','Preferences DataStore for metadata only; not an authenticated session store'],['Cloud storage','Google Sheets; TrackerUsers allowlist and TrackerRecords journal'],['Backend','Google Apps Script; JavaScript domain rules; bundled node-forge 1.4.0'],['Data exchange','Authenticated HTTPS JSON POST; API envelope version 2'],['Reports','CSV generation; narrow FileProvider sharing'],['Build settings','minSdk 24; compileSdk/targetSdk 37; JDK 21; Java compatibility 11'],['Testing / version control','JUnit, backend Node tests, Android lint; Git'],['Future reliability layer','Durable local database/queue and background synchronization: planned, technology not finalized']], [133,W-133]),
    sub('3.2.2 Application software requirements'),
    para('The baseline shall support administrator profile/subject/enrollment management, assigned faculty attendance/assessment operations, and student-only progress visibility. It shall reject unauthorized or inconsistent writes and communicate validation, conflict and network failures clearly.'),
    para('Document IDs below use CUTM-SRS-SAPT-XX. Status records implementation state; it is separate from priority and acceptance evidence.'),
])
page('3.2.2.1 Requirements table', [
    tab([['ID suffix','Software requirement','Status']] + [[r[0],r[1],r[2]] for r in requirements], [62, W-135,73]),
])
page('3.2.2.2 Brief description of requirements', [
    tab([['Group / IDs','Required behavior'],['01-04: access','Launch; Google sign-in; server role/profile verification; expiry and logout.'],['05-09: administration','Real counts; student/faculty/subject directories; validated CRUD; history-preserving deactivation.'],['10-11: subject workflows','Administrator roster selection; faculty assignment scope; academic navigation.'],['12-14: attendance','Complete statuses; unique session submission; auditable snapshots; correct counted-session percentage.'],['15-16: assessments','Validated drafts; complete marks before publication; published record locking.'],['17-18: progress / reports','Own student summaries; authorized subject reports; safe CSV export.'],['19-20: interaction / integrity','Loading and retry feedback; unsaved-form guards; journal audits and conflict checks.'],['21-26: production work','Account administration, academic lifecycle, approved corrections, durable offline behavior, bulk import and operational release gates.'],['27: extensions','Separately approved optional enhancements; not baseline commitments.']], [140,W-140]),
    sub('Permissions matrix - baseline'),
    tab([['Action','Admin','Faculty','Student'],['Manage profiles / subjects','Yes','No','No'],['Change enrollment','Yes','No','No'],['Attendance / assessments','Any active subject','Assigned only','No'],['Staff attendance report / CSV','Any active subject','Assigned only','No'],['Student progress endpoint','No','No','Own only'],['Spreadsheet operations','Authorized operator','No direct access','No direct access']], [175,70,118,W-363]),
    para('Interface visibility is not a security boundary. Every API action must repeat the required role and record-scope check on the server.'),
])
for start in range(0,len(requirements),3):
    elems=[]
    for num,title,status,actors,inp,proc,out,accept in requirements[start:start+3]:
        elems += [sub(f'CUTM-SRS-SAPT-{num} | {title}'), tab([
            ['Field','Specification'],['Status / actors',f'{status} / {actors}'],['Input',inp],['Process',proc],['Output',out],['Acceptance',accept]
        ], [85,W-85]), ('space',7)]
    page('3.2.3 Detailed functional requirements',elems)

page('3.3-3.6 Configuration items and interfaces', [
    sub('3.3 Computer Software Configuration Items (CSCI)'),
    tab([['Item','Responsibility'],['Android application','Compose screens, navigation, credential flow, API models, feedback and CSV sharing.'],['Backend domain/auth bundle','Token verification, role/scope rules, validation, calculations and retry/conflict semantics.'],['Apps Script adapter','HTTP dispatch, certificate cache, Script Properties, locking and spreadsheet access.'],['Spreadsheet configuration','Allowlist and append-only record journal; restricted direct operator access.'],['Build and verification assets','Gradle configuration, backend bundling, tests and deployment documentation.']], [150,W-150]),
    sub('3.4 External interface requirements'),
    para('EI-01: Credential Manager obtains a Google ID token for the configured OAuth audience. EI-02: the app sends HTTPS JSON POST to the Apps Script deployment /exec endpoint. EI-03: Apps Script retrieves trusted Google signing certificates. EI-04: Apps Script accesses only its configured spreadsheet. EI-05: Android share targets receive temporary read access only to the selected exported file.'),
    sub('3.5 Internal interface requirements'),
    para('Compose screens call suspend Retrofit APIs and interpret typed responses. AuthSession holds token and verified identity; DataStore holds metadata. TrackerInterceptor moves declared query fields into a JSON POST and adds the token. Apps Script authenticates the request, then createService applies domain permissions/rules using a sheetStore adapter.'),
    sub('3.6 Internal data requirements'),
    para('Exchange fields shall use stable identifiers and documented JSON types. Missing optional profile fields may be blank; pending/absent marks use null score. Attendance snapshots retain student names. UTC journal timestamps and institution-local session dates serve different purposes and shall not be interchanged.'),
])
page('3.7-3.9 Adaptation, safety and privacy', [
    sub('3.7 Adaptation requirements'),
    para('Baseline deployment parameters are spreadsheet ID, OAuth audience, deployment URL and script timezone, with an optional signed Google hosted-domain restriction. Academic threshold/grade policy, first-class terms and institution branding are planned adaptations. They are not currently configurable in-app.'),
    sub('3.8 Safety and academic data integrity'),
    para('Incorrect attendance or marks can affect academic decisions. The system shall validate complete rosters, distinguish pending/absent/zero marks, reject stale updates and preserve historical evidence. Submitted attendance and published results shall not be silently overwritten. Production corrections require explicit approval and before/after audit records.'),
    para('A network timeout must not be presented as proof that a write failed. The baseline has server retry deduplication; durable client recovery/status checking is still planned. Backup and restore verification are required before institution-wide reliance.'),
    sub('3.9 Security and privacy requirements'),
    *bullets([
        'SEC-01: Verify token signature and claims before reading academic data; reject unknown/inactive allowlist mappings.',
        'SEC-02: Enforce role, subject assignment and own-student scope on every backend action.',
        'SEC-03: Use HTTPS; keep tokens out of request URLs, exported files and device persistence.',
        'SEC-04: Validate request sizes and field bounds; return safe internal-error messages.',
        'SEC-05: Restrict direct spreadsheet sharing and cloud administrator privileges.',
        'SEC-06: Preserve actor/action/time audit data and define secure administrator recovery.',
        'SEC-07: Publish institution privacy/retention rules and minimize guardian/contact collection.',
        'SEC-08: Revalidate access for offline synchronization and clear prior-account cached data when that planned layer is added.',
    ]),
])
page('3.10 Non-functional requirements', [
    para('The following are proposed measurable production acceptance targets. They are not measured guarantees of the current demo. Capacity and policy targets require institution agreement.'),
    tab([['ID / quality','Target / verification'],['NFR-01 Security','All role/action and cross-user denial tests pass; no academic data returned to an unapproved principal.'],['NFR-02 Correctness','Agreed attendance/grade edge-case fixtures pass, including zero denominator, excused, zero marks and roster changes.'],['NFR-03 Integrity','Identical supported retries yield one logical write; stale concurrent updates return conflict rather than overwrite.'],['NFR-04 Response time','Proposed pilot target: p95 read <=5 seconds and write <=10 seconds on an agreed dataset/network; measure cold starts separately.'],['NFR-05 Capacity','Baseline guard: <=250 enrolled students per subject and <=40,000 characters per saved aggregate. Measure smaller effective limits for real payloads; do not promise unlimited growth.'],['NFR-06 Recovery','Proposed pilot RPO <=24 hours and RTO <=4 hours; prove through a restore drill and institution-approved backup policy.'],['NFR-07 Usability','All three roles complete primary tasks without developer help; loading/failure/empty states remain distinguishable.'],['NFR-08 Accessibility','Verify TalkBack, 200% font scaling, non-color status meaning and appropriate touch targets on supported devices.'],['NFR-09 Compatibility','Test API 24 and a current supported Android device, plus representative phone/tablet and orientation configurations.'],['NFR-10 Maintainability','Reproducible client/backend builds; meaningful tests and lint in CI; documented API/schema changes and dependency ownership.'],['NFR-11 Availability','Monitor service failures and Google service limits; agree the supported operating hours and incident response rather than claiming an untested uptime SLA.'],['NFR-12 Privacy','No token/marks/guardian contact leakage in public logs; only authorized exports and institution-defined retention.']], [115,W-115]),
])
page('4. Database backend - physical storage', [
    para('Google Sheets is the compulsory cloud data store. The current implementation creates exactly two application tabs. Logical academic entities are JSON records in a journal, not separate relational tables or one sheet per entity.'),
    sub('4.1 TrackerUsers - account allowlist'),
    tab([['Column','Type / rule'],['email','Normalized Google account email; exactly one matching entry required.'],['role','admin, faculty or student.'],['profileId','Faculty employeeId or student rollNumber; blank permitted for admin.'],['name','Display name; Google claim may be used as fallback.'],['active','TRUE means authorized; other values are denied.'],['googleSubject','Optional pinned Google sub claim.']], [130,W-130]),
    sub('4.2 TrackerRecords - append-only journal'),
    tab([['Column','Type / rule'],['kind','student, faculty, subject, enrollment, attendance or assessment.'],['id','JSON-encoded record key; attendance key is a JSON tuple.'],['data','JSON object containing the entity snapshot and relevant revision/state.'],['at','ISO timestamp of the journal write.'],['actor','JSON-encoded verified actor email for normal API mutations.'],['action','Domain action, such as saveAttendance or saveAssessment.']], [130,W-130]),
    para('Latest state is reconstructed by scanning the journal in row order and retaining the last record for each (kind, id). Append-only here describes application behavior; a spreadsheet owner can still edit cells directly. Production controls must restrict and monitor that access.'),
])
page('4.3 Logical data dictionary and rules', [
    tab([['Entity / identifier','Main stored fields and relationships'],['Student / rollNumber','name, email, phone, department, batch, year, semester, section, parentName, parentPhone, parentEmail; active, revision, updatedAt.'],['Faculty / employeeId','name, email, phone, department, designation, joiningDate; active, revision, updatedAt.'],['Subject / subjectCode','subjectName, facultyId -> Faculty, semester, totalClasses; active, revision, updatedAt.'],['Enrollment / subjectCode','studentIds[] -> Student; revision, updatedAt. One aggregate per subject, with zero or more current students.'],['Attendance / composite key','subjectCode -> Subject, date, slot, requestId, records[] {studentId, name, status}; updatedAt.'],['Assessment / assessmentId','subjectCode -> Subject, title, maxMarks, marks[] {studentId, name, status, score}, published, revision, requestId, updatedAt.']], [145,W-145]),
    sub('4.4 Keys and relationships'),
    para('Subject.facultyId references an active Faculty at creation/update. Enrollment resolves active Student records. Attendance and assessment contain embedded roster/mark snapshots; their historical entries do not rely on current enrollment to retain evidence. Account.profileId is role-dependent and must match an active profile for faculty/student authorization.'),
    sub('4.5 Calculation and status rules'),
    para('Attendance % = 100 x (present + late) / (present + late + absent). Excused entries are excluded; no denominator means N/A. Example: P=1, L=1, A=1, E=1 gives 66.7%. Planned subject.totalClasses is not the recorded attendance denominator.'),
    para('Marks status = pending, absent or scored. Scored zero remains zero; pending/absent score is null. An assessment may be published only when no marks are pending. Weighted grades and retest policies are planned.'),
    sub('4.6 Planned schema evolution'),
    para('Terms, subject offerings, dated enrollment, correction requests and durable sync metadata need versioned schema changes. Add migrations and projection recovery before changing production data; do not assume these entities already exist.'),
])

class Diagram(Flowable):
    def __init__(self, kind):
        Flowable.__init__(self); self.width=W; self.height=510; self.kind=kind
    def draw(self):
        c=self.canv
        c.setLineWidth(1)
        def label(x,y,text,size=9,bold=False,align='center'):
            c.setFillColor(INK); c.setFont('Bold' if bold else 'Body',size)
            lines=text.split('\n'); y += (len(lines)-1)*size*.6 if align=='center' else 0
            for line in lines:
                getattr(c, {'center':'drawCentredString','left':'drawString','right':'drawRightString'}[align])(x,y,line)
                y-=size*1.25
        def box(x,y,w,h,text,fill=PALE,shape='rect',size=9):
            c.setStrokeColor(BLUE); c.setFillColor(fill)
            if shape=='diamond':
                path=c.beginPath();path.moveTo(x+w/2,y+h);path.lineTo(x+w,y+h/2);path.lineTo(x+w/2,y);path.lineTo(x,y+h/2);path.close();c.drawPath(path,fill=1,stroke=1)
            elif shape=='ellipse': c.ellipse(x,y,x+w,y+h,fill=1,stroke=1)
            elif shape=='store':
                c.rect(x,y,w,h,fill=1,stroke=1); c.line(x+7,y,x+7,y+h)
            else: c.roundRect(x,y,w,h,7,fill=1,stroke=1)
            label(x+w/2,y+h/2-3,text,size)
        def arrow(points,text='',tx=None,ty=None,dashed=False):
            c.setStrokeColor(GRAY);c.setFillColor(GRAY);c.setDash(4,3) if dashed else c.setDash()
            path=c.beginPath();path.moveTo(*points[0])
            for pt in points[1:]:path.lineTo(*pt)
            c.drawPath(path);c.setDash()
            (x0,y0),(x1,y1)=points[-2:];a=math.atan2(y1-y0,x1-x0);length=6
            path=c.beginPath();path.moveTo(x1,y1);path.lineTo(x1-length*math.cos(a-.45),y1-length*math.sin(a-.45));path.lineTo(x1-length*math.cos(a+.45),y1-length*math.sin(a+.45));path.close();c.drawPath(path,fill=1,stroke=0)
            if text: label(tx if tx is not None else (points[0][0]+x1)/2,ty if ty is not None else (points[0][1]+y1)/2+6,text,8)
        def actor(x,y,name):
            c.setStrokeColor(BLUE);c.circle(x,y+26,8);c.line(x,y+18,x,y-5);c.line(x-13,y+7,x+13,y+7);c.line(x,y-5,x-12,y-21);c.line(x,y-5,x+12,y-21);label(x,y-36,name,9,True)
        if self.kind=='architecture':
            box(0,426,320,58,'Android application\nCompose screens | Navigation | AuthSession',size=11)
            box(0,328,143,54,'Preferences DataStore\nEmail / role metadata only')
            box(177,328,160,54,'Retrofit + OkHttp\nAuthenticated JSON POST')
            box(366,426,132,58,'Google identity\nID token',GREEN,size=9)
            arrow([(366,455),(320,455)],'ID token',342,470)
            arrow([(170,426),(170,398),(70,398),(70,382)],'metadata',104,404)
            arrow([(257,426),(257,382)])
            box(100,215,314,65,'Google Apps Script web app\nVerify identity -> authorize -> validate\nLock -> domain action -> audit write',size=10)
            arrow([(257,328),(257,280)],'HTTPS + token',307,302)
            box(0,114,148,56,'Trusted Google\nsigning certificates',GREEN)
            arrow([(120,215),(120,185),(73,185),(73,170)],'key lookup',57,193)
            box(185,88,296,82,'Private Google spreadsheet\nTrackerUsers: account allowlist\nTrackerRecords: JSON journal',size=11)
            arrow([(300,215),(300,170)],'read / append',348,190)
            label(249,35,'Trust boundary: device data is untrusted; backend checks every action.',9)
        elif self.kind=='usecase':
            c.setStrokeColor(GRAY);c.roundRect(130,52,265,420,8,stroke=1,fill=0);label(262,485,'Student Tracker system boundary',10,True)
            actor(49,399,'Admin');actor(49,192,'Faculty');actor(454,294,'Student')
            ys=[419,354,289,224,159,94]
            names=['Sign in / logout','Manage profiles,\nsubjects and enrollment','Record attendance','Draft / publish marks','Staff reports / CSV','View own attendance\nand published results']
            for y,n in zip(ys,names):box(159,y-23,207,46,n,shape='ellipse')
            for i in [0,1,2,3,4]:arrow([(72,399),(144,ys[i]),(159,ys[i])])
            for i in [0,2,3,4]:arrow([(72,192),(137,ys[i]),(159,ys[i])])
            for i in [0,5]:arrow([(430,294),(382,ys[i]),(366,ys[i])])
            label(249,12,'Staff actions are subject-scoped; student progress is own-record only.',9)
        elif self.kind=='context':
            box(155,201,190,98,'0. Student Tracker\nAttendance and\nPerformance System',shape='ellipse',size=11)
            box(0,385,140,56,'Administrator');box(360,385,140,56,'Faculty');box(180,28,140,56,'Student')
            box(180,407,140,56,'Google identity',GREEN)
            arrow([(140,410),(156,410),(156,270)],'profiles / roster',65,328)
            arrow([(185,290),(185,360),(69,360),(69,385)],'counts / reports',72,366)
            arrow([(360,410),(344,410),(344,270)],'attendance / marks',427,328)
            arrow([(315,290),(315,360),(429,360),(429,385)],'subjects / reports',428,367)
            arrow([(233,84),(233,201)],'own progress request',150,138)
            arrow([(267,201),(267,84)],'own summaries',338,138)
            arrow([(248,407),(248,299)],'signed identity / keys',321,350)
            label(249,8,'Context diagram: internal storage is intentionally inside process 0.',8.5)
        elif self.kind=='dfd':
            box(0,428,113,50,'Admin / faculty\n/ student')
            box(173,428,150,50,'1. Authenticate\nand authorize',shape='ellipse')
            box(370,428,128,50,'D1 TrackerUsers',shape='store')
            arrow([(113,454),(173,454)],'token',142,465)
            arrow([(323,466),(370,466)],'lookup',345,478)
            arrow([(370,440),(323,440)],'role',346,425)
            box(173,327,150,50,'2. Manage profiles\nand enrollment',shape='ellipse')
            box(173,220,150,50,'3. Record attendance\nand assessments',shape='ellipse')
            box(173,110,150,50,'4. Calculate progress\nand reports',shape='ellipse')
            box(370,150,128,155,'D2 TrackerRecords\nJournal snapshots\n+ audit metadata',shape='store')
            arrow([(247,428),(247,399),(82,399),(82,352),(173,352)],'verified principal',71,385)
            arrow([(82,352),(82,245),(173,245)],'authorized payload',77,285)
            arrow([(82,245),(82,135),(173,135)],'scoped query',70,187)
            arrow([(323,351),(434,351),(434,305)],'profiles / roster',410,360)
            arrow([(323,258),(370,258)],'append',346,274)
            arrow([(370,232),(323,232)],'roster',346,217)
            arrow([(400,150),(400,135),(323,135)],'sessions / marks',413,114)
            arrow([(173,121),(26,121),(26,428)],'results',52,73)
            label(249,27,'All mutation paths carry the verified actor and use the script lock.',9)
        elif self.kind=='er':
            def entity(x,y,w,h,title,fields):
                c.setStrokeColor(BLUE);c.setFillColor(colors.white);c.roundRect(x,y,w,h,5,fill=1,stroke=1);c.setFillColor(PALE);c.rect(x,y+h-27,w,27,fill=1,stroke=0);label(x+w/2,y+h-18,title,9.5,True)
                label(x+8,y+h-44,'\n'.join(fields),8,align='left')
            entity(0,397,145,100,'Account allowlist',['email (key)','role, profileId','active, googleSubject'])
            entity(190,397,145,100,'Faculty',['employeeId (key)','name, email','active, revision'])
            entity(380,397,118,100,'Student',['rollNumber (key)','name, email','active, revision'])
            entity(190,231,145,104,'Subject',['subjectCode (key)','facultyId -> Faculty','semester, totalClasses'])
            entity(380,231,118,104,'Enrollment',['subjectCode (key)','studentIds[]','revision'])
            entity(0,53,145,120,'Attendance',['subject/date/slot (key)','subjectCode, requestId','records[]: studentId,','name, status'])
            entity(190,53,145,120,'Assessment',['assessmentId (key)','subjectCode, maxMarks','published, revision','marks[]: id/status/score'])
            arrow([(145,469),(190,469)],'0..1 profile',166,482,dashed=True)
            arrow([(145,410),(165,410),(165,373),(439,373),(439,397)],'Student role link',121,356,dashed=True)
            arrow([(262,397),(262,335)],'1 to 0..*',306,365)
            arrow([(335,287),(380,287)],'1 to 0..1',357,302)
            arrow([(439,335),(439,397)],'0..* members',439,355)
            arrow([(190,254),(72,254),(72,173)],'1 to 0..* sessions',85,219)
            arrow([(262,231),(262,173)],'1 to 0..*',306,203)
            label(415,124,'Snapshots embed\nstudent IDs and names;\nno separate entry tables.\n\nLogical relationships,\nnot physical SQL tables.',8.5)
            label(249,15,'All academic entity versions live in TrackerRecords JSON journal rows.',9)
        elif self.kind in ['login','attendance','assessment','enrollment']:
            box(160,461,180,32,'Start',GREEN,shape='ellipse')
            if self.kind=='login':
                box(145,389,210,46,'Choose Google account\nObtain ID token')
                box(145,311,210,46,'POST token to Apps Script\nVerify signature and claims')
                box(169,205,162,72,'Valid token, active\nallowlist and profile?',ORANGE,'diamond',9)
                box(150,115,200,50,'Open verified role dashboard\nAdmin / Faculty / Student',GREEN)
                box(150,39,200,34,'Use scoped actions',GREEN,shape='ellipse')
                box(0,215,135,54,'Reject access\nShow error / retry',ORANGE)
                arrow([(250,461),(250,435)]);arrow([(250,389),(250,357)]);arrow([(250,311),(250,277)])
                arrow([(250,205),(250,165)],'Yes',270,185);arrow([(250,115),(250,73)])
                arrow([(169,241),(135,241)],'No',150,255)
                arrow([(65,269),(65,412),(145,412)],'Try again',67,340)
            elif self.kind=='attendance':
                box(145,395,210,43,'Choose assigned subject\nLoad current roster')
                box(145,326,210,43,'Enter date / slot\nMark every student')
                box(175,233,150,62,'Complete and\nvalid roster?',ORANGE,'diamond')
                box(145,160,210,43,'Confirm submission\nPOST with request ID')
                box(145,89,210,43,'Server scope + roster checks\nUnique key; append snapshot')
                box(145,16,210,43,'Success / identical retry\nUpdate report totals',GREEN)
                box(0,234,130,59,'Fix missing status\nor reload changed roster',ORANGE,size=8.5)
                arrow([(250,461),(250,438)]);arrow([(250,395),(250,369)]);arrow([(250,326),(250,295)]);arrow([(250,233),(250,203)],'Yes',270,215);arrow([(250,160),(250,132)]);arrow([(250,89),(250,59)])
                arrow([(175,264),(130,264)],'No',150,275);arrow([(65,293),(65,348),(145,348)])
                box(372,85,126,58,'Different existing session\n-> conflict; no overwrite',ORANGE,size=8)
                arrow([(355,110),(372,110)])
            elif self.kind=='assessment':
                box(145,395,210,43,'Choose subject\nCreate / load assessment')
                box(145,326,210,43,'Enter title, maximum marks\nEnter roster marks')
                box(170,232,160,62,'Publish now?',ORANGE,'diamond')
                box(0,236,135,52,'Validate and save draft\nPending allowed',PALE,size=8.5)
                box(145,151,210,48,'Require no pending entries\nConfirm publication')
                box(145,80,210,43,'Check scope, revision and ID\nSave published version')
                box(145,11,210,43,'Lock result\nStudent sees own marks',GREEN)
                arrow([(250,461),(250,438)]);arrow([(250,395),(250,369)]);arrow([(250,326),(250,294)])
                arrow([(170,263),(135,263)],'No',152,276);arrow([(250,232),(250,199)],'Yes',272,216)
                arrow([(250,151),(250,123)]);arrow([(250,80),(250,54)])
                arrow([(65,288),(65,347),(145,347)],'Continue later',66,303)
                box(372,149,126,50,'Pending / stale / invalid\nShow error; do not publish',ORANGE,size=8)
                arrow([(355,173),(372,173)])
            else:
                box(145,395,210,43,'Admin selects subject\nLoad students and revision')
                box(145,326,210,43,'Select active students\nReview roster')
                box(145,256,210,43,'POST IDs + expected revision')
                box(168,157,164,68,'Distinct active IDs,\ncapacity and revision OK?',ORANGE,'diamond',8.5)
                box(145,85,210,43,'Append enrollment version\nReturn new revision')
                box(145,16,210,43,'Faculty sees current roster',GREEN)
                box(0,162,132,56,'Reject / show conflict\nReload and review',ORANGE,size=8.5)
                arrow([(250,461),(250,438)]);arrow([(250,395),(250,369)]);arrow([(250,326),(250,299)]);arrow([(250,256),(250,225)]);arrow([(250,157),(250,128)],'Yes',271,142);arrow([(250,85),(250,59)])
                arrow([(168,191),(132,191)],'No',151,204);arrow([(65,218),(65,417),(145,417)],'Retry after review',70,286)
        elif self.kind=='sequence':
            xs=[45,178,320,455]
            for x,title in zip(xs,['Faculty','Android app','Apps Script','Google Sheets']):
                box(x-43,459,86,38,title,size=8.5)
                c.setStrokeColor(GRAY);c.setDash(3,3);c.line(x,454,x,32);c.setDash()
            steps=[(0,1,420,'Submit complete roster'),(1,2,377,'HTTPS: token + action + request ID'),(2,3,334,'Lock; read allowlist\nand current journal'),(3,2,292,'Account, subject\nand roster records'),(2,3,249,'Validate; append attendance\nwith actor and timestamp'),(3,2,207,'Flush completed'),(2,1,164,'Success envelope (apiVersion 2)'),(1,0,120,'Show submitted state')]
            for a,b,y,text in steps:
                arrow([(xs[a],y),(xs[b],y)],text,(xs[a]+xs[b])/2,y+(17 if '\n' in text else 10))
            label(250,53,'Token signature verification precedes Sheets reads; trusted key cache is omitted here.',8)

figures = [
('architecture','System architecture','Android, Google identity, Apps Script and private Sheets are separated by explicit interfaces. Stored preferences are metadata, not an authenticated offline session.'),
('usecase','Use-case diagram','Administrator and faculty staff actions overlap, but faculty actions are restricted to assigned subjects. Students view their own summaries and published results only.'),
('context','Data-flow diagram - context / level 0','Process 0 represents the complete Student Tracker system. Actors exchange management data, attendance/marks and own-progress requests; Google identity supplies trusted credentials.'),
('dfd','Data-flow diagram - level 1','Authentication yields a verified principal. Profile/enrollment, attendance/assessment and report processes use the allowlist and journal data stores with server authorization.'),
('er','Logical entity relationship diagram','1 means one; 0..1 means optional single; 0..* means zero or more. Account profile links are conditional on role. Enrollment arrays and roster/mark entries are embedded JSON, not SQL tables. The physical backend still has only two tabs.'),
('login','Login and authorization flowchart','No self-registration or role selection grants access. A valid Google identity must also match one active authorized mapping and, where required, an active profile.'),
('enrollment','Enrollment management flowchart','Administrator edits carry the current revision. Stale edits require reloading and review; the server does not silently apply last-write-wins.'),
('attendance','Attendance submission flowchart','A complete roster is confirmed before submission. The server deduplicates an identical retry and rejects another payload for an already submitted session.'),
('assessment','Assessment draft/publication flowchart','Pending marks are valid in drafts, but publication requires every mark scored or absent. Published assessments are locked; an approved correction workflow is planned.'),
('sequence','Attendance request sequence diagram','The simplified sequence shows the normal success path. Invalid identity/permissions, changed rosters, duplicate sessions and lock contention return explicit errors without unauthorized writes.'),
]
for i,(kind,title,caption) in enumerate(figures,1):
    page(f'5.{i} {title}', [('diagram',kind),sub(f'Figure {i}. {title}'),para(caption)])

page('6. Verification and acceptance', [
    sub('6.1 Recorded evidence and remaining limits'),
    para('Recorded local checks: 27 backend tests and 9 Android JVM tests passed; debug assembly and lint passed with zero errors and 33 remaining warnings. These results are prior verification evidence, not newly executed tests for this document. The example instrumented device test has not been run as a complete acceptance suite.'),
    para('The demo Apps Script deployment, spreadsheet initialization, OAuth configuration and three role accounts are documented. Real-device Google login and the complete deployed academic workflow remain unverified. A deployed URL and a successful APK build do not establish production readiness.'),
    sub('6.2 Acceptance and traceability matrix'),
    tab([['Test / requirement IDs','Acceptance scenario'],['AT-01 / 01-04','Launch, approved sign-in, expired/forged token rejection, inactive account rejection and logout.'],['AT-02 / 05-10','Create profiles/subject, reject duplicates/stale edits, prevent unsafe faculty deactivation, enroll distinct active students.'],['AT-03 / 03, 11-13','Assigned faculty submits full roster; another faculty/student cannot submit it; identical retry yields one session.'],['AT-04 / 14, 17','Verify 66.7% example, N/A and historical roster eligibility; student sees only own results.'],['AT-05 / 15-16','Draft accepts pending; publish rejects pending; zero/absent behave correctly; published direct edit rejected.'],['AT-06 / 18-19','Export Unicode/quoted/formula-like values; open share target; verify loading/error/unsaved-form behavior on phone.'],['AT-07 / 20, 23-24','Concurrency and lost-response scenarios; approved correction audit and durable draft recovery after implementation.'],['AT-08 / 21-26, NFRs','Account lifecycle, dated terms/policies, import errors, representative load, backup restore and signed release rehearsal.']], [130,W-130]),
])
page('7. Roadmap, open decisions and glossary', [
    sub('7.1 Delivery order'),
    tab([['Stage','Outcome'],['Immediate demo acceptance','Install APK; verify all three logins; create/assign subject; enroll student; submit attendance; publish marks; confirm student results and export.'],['Production domain work','Account administration, academic terms/offerings, dated enrollment, policy settings and approved corrections.'],['Reliability and reporting','Durable drafts/sync, bulk imports, filtered/printable reports and accessibility improvements.'],['Mandatory release gates','Capacity validation, separate environments, backup restore, monitoring, release signing, privacy/support ownership and institution acceptance.'],['Optional extensions','Notification channels, timetable, QR/location aids, assignments, guardian portal, analytics, themes and additional languages.']], [130,W-130]),
    sub('7.2 Decisions required before production acceptance'),
    para('Agree attendance counting/shortage and correction policy; grade weightage/retests; term structure; expected institution size; data retention; account approval owner; supported device matrix; backup recovery targets; deployment ownership; and whether any optional feature becomes a requirement.'),
    sub('7.3 Glossary'),
    tab([['Term','Meaning'],['SRS / CSCI','Software Requirements Specification / Computer Software Configuration Item.'],['ID token / principal','Google-signed identity token / verified actor and permissions used by the backend.'],['Revision / idempotency','Version used to detect stale edits / repeating the same operation does not create another logical change.'],['Journal / projection','Append-only record history / latest state reconstructed from that history.'],['RPO / RTO / p95','Maximum acceptable recovery data loss / recovery time / response time at the 95th percentile.']], [130,W-130]),
    para('Document review status: draft for team/institution review. No reviewer approval or production sign-off is implied.'),
])

# Build a compact contents page using the final explicit page plan.
contents = [
    ['Section','Page'],
    ['1. Scope and identification','3'],
    ['1.4 Environment; 1.8 development details','4-5'],
    ['2. Referenced documents; 3.1 mode of operation','5'],
    ['3.2 Technology stack and requirement inventory','6-8'],
    ['3.2.3 Detailed functional requirements (27 IDs)','9-17'],
    ['3.3-3.6 Configuration items and interfaces','18'],
    ['3.7-3.9 Adaptation, safety and privacy','19'],
    ['3.10 Non-functional requirements','20'],
    ['4. Physical storage and logical data dictionary','21-22'],
    ['5. Diagrams and flowcharts (10 figures)','23-32'],
    ['6. Verification and acceptance','33'],
    ['7. Roadmap, decisions and glossary','34'],
]
pages[1] = ('Contents', [tab(contents,[W-65,65]),sub('List of figures'), *[para(f'{i}. {title} - page {22+i}') for i,(_,title,_) in enumerate(figures,1)]])

story=[]; md=['# Software Requirements Specification\n\nStudent Attendance & Performance Tracker\n\nVersion 0.1 | 1 October 2026\n']
for index,(title,elems) in enumerate(pages):
    if index:story.append(PageBreak())
    if index:story.append(p(title,'H1X'))
    md.append(f'\n## {title}\n')
    for kind,value in elems:
        if kind=='cover': story += [Spacer(1,20),p('Software Requirements Specification','H2X'),p(value.replace('\n',' '),'TitleX'),Spacer(1,8)];md.append(value.replace('\n',' '))
        elif kind in ['p','h','b']:
            story.append(p(('• ' if kind=='b' else '')+value,'H2X' if kind=='h' else 'BodyX'))
            md.append(('### ' if kind=='h' else '- ' if kind=='b' else '')+value+'\n')
        elif kind=='t':
            rows,widths=value;story.append(table(rows,widths));story.append(Spacer(1,9))
            md += ['| '+' | '.join(rows[0])+' |','| '+' | '.join(['---']*len(rows[0]))+' |']
            md += ['| '+' | '.join(str(x).replace('|','/') for x in r)+' |' for r in rows[1:]];md.append('')
        elif kind=='space':story.append(Spacer(1,value))
        elif kind=='diagram':story.append(Diagram(value));md.append(f'<!-- Diagram: {value}; vector drawing in docs/build_srs.py and generated PDF. -->\n')

def footer(c,doc):
    c.setStrokeColor(INK);c.setLineWidth(.7);c.line(46,43,A4[0]-46,43)
    c.setFont('Body',8);c.setFillColor(GRAY)
    c.drawString(46,29,'CUTM-SRS-SAPT | VERSION 0.1 | 01 OCT 2026')
    c.drawRightString(A4[0]-46,29,f'{doc.page} / {len(pages)}')
    if doc.page>1:
        c.setFont('Body',8);c.drawString(46,A4[1]-30,'Student Attendance & Performance Tracker')

SimpleDocTemplate(str(OUT),pagesize=A4,leftMargin=46,rightMargin=46,topMargin=53,bottomMargin=58,title='Student Attendance & Performance Tracker - Software Requirements Specification',author='Student Tracker Project Team').build(story,onFirstPage=footer,onLaterPages=footer)
SOURCE.write_text('\n'.join(md),encoding='utf-8')
reader=PdfReader(OUT)
if len(reader.pages)!=len(pages):
    raise RuntimeError(f'Page overflow: expected {len(pages)}, produced {len(reader.pages)}; fix layout and contents before delivery.')
for i,pg in enumerate(reader.pages):
    if len((pg.extract_text() or '').strip())<100:raise RuntimeError(f'Unexpected near-empty page {i+1}')
print(json.dumps({'pdf':str(OUT),'pages':len(reader.pages),'requirements':len(requirements),'figures':len(figures),'source':str(SOURCE)},indent=2))
