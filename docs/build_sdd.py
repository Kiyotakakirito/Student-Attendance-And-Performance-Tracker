from pathlib import Path
import ast, html, json, math
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, Flowable, Preformatted
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/pdf/Student_Tracker_SDD.pdf'
SOURCE = ROOT / 'docs/Student_Tracker_SDD.md'
OUT.parent.mkdir(parents=True, exist_ok=True)
for name, file in [('Body','segoeui.ttf'),('Bold','segoeuib.ttf'),('Mono','consola.ttf')]:
    pdfmetrics.registerFont(TTFont(name,'C:/Windows/Fonts/'+file))
INK=colors.HexColor('#18334F'); BLUE=colors.HexColor('#245F8A')
PALE=colors.HexColor('#EEF4F9'); GREEN=colors.HexColor('#EAF5EE')
ORANGE=colors.HexColor('#FFF2DA'); GRAY=colors.HexColor('#627285')
W=A4[0]-92
styles=getSampleStyleSheet()
for name,size,leading,extra in [
    ('BodyX',10,15,{'spaceAfter':8}),('SmallX',8.7,12,{'spaceAfter':5}),
    ('H1X',19,24,{'fontName':'Bold','textColor':INK,'spaceAfter':14}),
    ('H2X',12,17,{'fontName':'Bold','textColor':INK,'spaceBefore':9,'spaceAfter':7}),
    ('TableX',8.5,12,{}),('TitleX',29,35,{'fontName':'Bold','textColor':INK,'spaceAfter':18}),
    ('CodeX',8,11,{'fontName':'Mono','spaceAfter':9}),
]:styles.add(ParagraphStyle(name=name,fontSize=size,leading=leading,**({'fontName':'Body'}|extra)))

def p(s,style='BodyX'):return Paragraph(html.escape(str(s)),styles[style])
def table(rows,widths=None):
    header_style=ParagraphStyle('TH',parent=styles['TableX'],fontName='Bold',textColor=colors.white)
    cells=[[Paragraph(html.escape(str(x)),header_style) for x in rows[0]]]+[[p(x,'TableX') for x in row] for row in rows[1:]]
    t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND',(0,0),(-1,0),INK),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,PALE]),
        ('GRID',(0,0),(-1,-1),.35,colors.HexColor('#C8D3DF')),('VALIGN',(0,0),(-1,-1),'TOP'),
        ('LEFTPADDING',(0,0),(-1,-1),8),('RIGHTPADDING',(0,0),(-1,-1),8),
        ('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5),
    ]));return t

# Reuse the SRS vector drawings without importing its PDF-generation side effects.
diagram_tree=ast.parse((ROOT/'docs/build_srs.py').read_text(encoding='utf-8'))
diagram_node=next(n for n in diagram_tree.body if isinstance(n,ast.ClassDef) and n.name=='Diagram')
exec(compile(ast.Module(body=[diagram_node],type_ignores=[]),'srs_vector_diagrams','exec'),globals())

class DesignDiagram(Flowable):
    def __init__(self,kind):Flowable.__init__(self);self.width=W;self.height=510;self.kind=kind
    def draw(self):
        c=self.canv
        def text(x,y,s,size=9,bold=False,background=False):
            lines=s.split('\n');y+=(len(lines)-1)*size*.6
            if background:
                width=max(pdfmetrics.stringWidth(line,'Bold' if bold else 'Body',size) for line in lines)
                c.setFillColor(colors.white);c.rect(x-width/2-3,y-(len(lines)-1)*size*1.25-3,width+6,len(lines)*size*1.25+2,fill=1,stroke=0)
            c.setFillColor(INK);c.setFont('Bold' if bold else 'Body',size)
            for line in lines:c.drawCentredString(x,y,line);y-=size*1.25
        def box(x,y,w,h,s,fill=PALE,size=9):
            c.setLineWidth(1);c.setStrokeColor(BLUE);c.setFillColor(fill);c.roundRect(x,y,w,h,6,stroke=1,fill=1);text(x+w/2,y+h/2-3,s,size)
        def arrow(points,s='',tx=None,ty=None,dashed=False):
            c.setStrokeColor(GRAY);c.setFillColor(GRAY);c.setLineWidth(1);c.setDash(3,3) if dashed else c.setDash()
            path=c.beginPath();path.moveTo(*points[0])
            for point in points[1:]:path.lineTo(*point)
            c.drawPath(path);c.setDash()
            (x0,y0),(x1,y1)=points[-2:];a=math.atan2(y1-y0,x1-x0);path=c.beginPath();path.moveTo(x1,y1);path.lineTo(x1-6*math.cos(a-.45),y1-6*math.sin(a-.45));path.lineTo(x1-6*math.cos(a+.45),y1-6*math.sin(a+.45));path.close();c.drawPath(path,fill=1,stroke=0)
            if s:text(tx if tx is not None else (points[0][0]+x1)/2,ty if ty is not None else (points[0][1]+y1)/2+9,s,8,background=True)
        if self.kind=='components':
            box(145,440,210,46,'MainActivity\nObserve session; rebuild navigation',size=10)
            box(0,340,145,56,'LoginScreen\nCredential Manager\nAuthSession.begin / complete')
            box(185,340,145,56,'AppNavigation\nRole-scoped routes\nEncoded record identifiers')
            box(365,340,133,56,'UserPreferences\nMetadata only\nDataStore')
            arrow([(190,440),(190,418),(72,418),(72,396)])
            arrow([(260,440),(260,396)])
            arrow([(330,440),(330,418),(430,418),(430,396)])
            box(185,232,145,60,'Feature screens\nRemoteContent / forms\nGoogleSheetsApi calls')
            arrow([(257,340),(257,292)],'navigate',302,314)
            box(185,131,145,60,'NetworkModule\nTrackerInterceptor\nRetrofit / OkHttp')
            arrow([(257,232),(257,191)],'suspend API',310,213)
            box(185,24,145,60,'Apps Script\ndoPost -> createService\nsheetStore',GREEN)
            arrow([(257,131),(257,84)],'JSON POST + token',321,108)
            arrow([(72,340),(72,160),(185,160)],'role lookup',110,248)
            text(418,215,'Baseline uses\ndirect screen-to-API calls.\nRepository/ViewModel\nrefactor is future work.',8.5)
        elif self.kind=='navigation':
            box(160,453,180,38,'Login / session verification',GREEN)
            box(0,361,145,56,'Administrator\nadmin_dashboard')
            box(181,361,145,56,'Faculty\nfaculty_dashboard')
            box(362,361,136,56,'Student\nstudent_dashboard')
            for x in [72,253,430]:arrow([(250,453),(250,433),(x,433),(x,417)])
            box(0,234,145,70,'Directories\nStudent / faculty / subject\nList -> details -> edit\nAdd -> save -> return',size=8.5)
            arrow([(72,361),(72,304)])
            box(181,234,145,70,'AcademicHubScreen\nAuthorized subject cards')
            arrow([(253,361),(253,304)])
            arrow([(145,389),(164,389),(164,269),(181,269)],'academic',142,325)
            box(362,234,136,70,'Own attendance\nOwn published results\nRefresh / logout')
            arrow([(430,361),(430,304)])
            box(0,103,145,65,'Enrollment\nenrollment/{subjectCode}\nAdmin only',ORANGE,size=8.5)
            box(181,103,145,65,'Attendance\nattendance/{subjectCode}\nScoped staff',size=8.5)
            box(362,103,136,65,'Assessments / reports\nSubject-based routes\nScoped staff',size=8.5)
            for x in [72,253,430]:arrow([(253,234),(253,207),(x,207),(x,168)])
            text(249,37,'Logout or expiry clears the session and rebuilds the graph at login.',9)
        elif self.kind=='loginsequence':
            xs=[40,170,313,454]
            for x,name in zip(xs,['User','Android app','Google identity','Apps Script']):
                box(x-40,463,80,36,name,size=8.5);c.setStrokeColor(GRAY);c.setDash(3,3);c.line(x,460,x,34);c.setDash()
            steps=[(0,1,423,'Tap Google sign-in'),(1,2,377,'Credential request'),(2,1,331,'Google ID token'),(1,3,285,'getUserRole + token (JSON POST)'),(3,3,239,'Verify signature / claims;\nresolve active allowlist + profile'),(3,1,187,'Role, name, email, expiresAt'),(1,0,139,'Open role dashboard')]
            for a,b,y,s in steps:
                if a==b:arrow([(xs[a],y),(xs[a]+31,y),(xs[a]+31,y-25),(xs[a],y-25)],s,365,y+12)
                else:arrow([(xs[a],y),(xs[b],y)],s,(xs[a]+xs[b])/2,y+11)
            text(249,61,'Only the server-approved role is used; failure clears the temporary token.',8.5)
        elif self.kind=='states':
            text(249,488,'Assessment lifecycle - implemented baseline',11,True)
            box(0,401,132,48,'New / unsaved')
            box(185,401,132,48,'Draft\npending marks allowed')
            box(370,401,128,48,'Published / locked',GREEN)
            arrow([(132,425),(185,425)],'save',158,437)
            arrow([(317,425),(370,425)],'complete + confirm',342,465)
            arrow([(242,401),(242,370),(296,370),(296,401)],'revise draft',313,352)
            text(249,325,'No direct Published -> Draft transition exists in the baseline.',9)
            text(249,274,'Attendance session lifecycle - implemented baseline',11,True)
            box(0,185,132,48,'New / editing\npartial statuses')
            box(185,185,132,48,'Ready\ncomplete roster')
            box(370,185,128,48,'Submitted / locked',GREEN)
            arrow([(132,209),(185,209)],'finish roster',158,248)
            arrow([(317,209),(370,209)],'confirm + validate',342,248)
            arrow([(426,185),(426,153),(474,153),(474,185)],'identical retry',425,133)
            text(249,95,'Different content for an existing subject/date/slot is a conflict.',9)
            box(40,20,418,43,'Proposed extension: correction request -> approval -> new audited version',ORANGE,size=8.5)
        elif self.kind=='deployment':
            box(0,381,208,105,'Developer workstation\nAndroid Studio / JDK 21 / Gradle\nNode.js backend bundle + tests\nGenerated Code.gs + manifest',size=10)
            box(290,381,208,105,'Android device\nSigned APK\nGoogle credential services\nApp configuration',size=10)
            arrow([(208,434),(290,434)],'install APK',249,446)
            box(290,245,208,80,'Google Cloud OAuth project\nAndroid client: package + SHA-1\nWeb client: token audience',GREEN,size=10)
            arrow([(394,381),(394,325)],'Google sign-in',440,352)
            box(0,124,208,110,'Apps Script web app\nV8 / Asia-Kolkata timezone\nExecute as owner\nHTTPS /exec; verify each token\nScript Properties',size=10)
            arrow([(105,381),(105,234)],'upload bundle / deploy',100,304)
            arrow([(290,402),(249,402),(249,190),(208,190)],'JSON POST',249,277)
            box(290,124,208,110,'Private Google spreadsheet\nTrackerUsers\nTrackerRecords\nRestricted operator sharing',size=10)
            arrow([(208,177),(290,177)],'read / append',249,159)
            text(249,58,'Proposed production topology: separate dev, staging and production copies.',9)
            text(249,32,'Public web-app reachability does not grant permission to academic data.',8.5)

members=[
('R. Nirmal','231801370002'),('L. Nitish Kumar','231801370009'),
('T. Veera Siva Krishna','231801370025'),('N. Laasya','231801370046'),
('S. Jayanth Kumar','231801370051'),('S. Sai Vignesh','231801370052'),
('T. Sai Devanand','231801370070'),
]
pages=[]
def page(title,elements,section=None):pages.append((title,elements,section))
def para(s):return ('p',s)
def sub(s):return ('h',s)
def tab(rows,widths=None):return ('t',(rows,widths))
def bullets(items):return [('b',x) for x in items]
def fig(title,kind,caption,number,custom=False,section=None):
    page(title,[('design' if custom else 'diagram',kind),sub(f'Figure {number}. {title.split(" ",1)[-1]}'),para(caption)],section)

page('Software Design Document',[
    ('space',8),('smallcenter','Centurion University of Technology and Management'),
    ('smallcenter','Student Tracker Project Team'),('space',25),
    ('cover','SOFTWARE DESIGN DOCUMENT'),('cover','Student Attendance & Performance Tracker'),
    para('Android application for administrator, faculty and student academic workflows'),('space',18),
    tab([['Team member','Roll number']]+[list(x) for x in members],[W*.61,W*.39]),
    ('space',20),tab([['Document control','Value'],['Document ID','CUTM-SDD-SAPT-VER-0.1'],['Version / date','0.1 - review draft / 1 October 2026'],['Requirements baseline','CUTM-SRS-SAPT-VER-0.1'],['Backend','Google Sheets + Google Apps Script']], [150,W-150]),
    para('The supplied TeamPulse SDD is a structural reference. Project design and implementation status are based on Student Tracker source and documentation.'),
])
page('Table of contents',[])
page('1. Introduction',[
    sub('1.1 Purpose'),para('This Software Design Document explains how Student Tracker implements the behavior described in its SRS. It records the application architecture, component responsibilities, data model, request contracts, runtime flows, security boundaries, deployment and verification strategy.'),
    sub('1.2 Scope'),para('The baseline covers Google-authenticated administrator management of students, faculty, subjects and enrollment; assigned staff attendance and assessment workflows; personal student progress; scoped attendance reports; and audit-backed storage.'),
    para('The Android client uses Kotlin and Jetpack Compose. The backend is JavaScript deployed as Google Apps Script with Google Sheets storage. Room, SQLCipher, Hilt, WorkManager, Firebase and a full MVVM/repository layer are not part of the current baseline.'),
    sub('1.3 Intended audience'),para('Project evaluators, faculty reviewers, the seven-member team, maintainers, testers and institution administrators. This document supports design review and presentation; it does not certify a production release.'),
    sub('1.4 Definitions and abbreviations'),
    tab([['Term','Meaning'],['SDD / SRS','Software Design Document / Software Requirements Specification.'],['API / UI','Application Programming Interface / User Interface.'],['Principal','Server-approved account identity, role, profile and expiry.'],['Revision','Record version used to detect stale edits.'],['Idempotent retry','Repeating an identical supported request yields one logical result.'],['Journal / projection','Append-only version history / reconstructed latest record state.'],['CSCI','Computer Software Configuration Item.']], [130,W-130]),
],1)
page('2. System overview',[
    sub('2.1 Product description'),para('Student Tracker maintains a shared academic record through three user experiences. Staff submit attendance for enrolled students and publish complete assessment marks. Students view their own attendance totals and published results. Administrators prepare profiles, subjects and rosters.'),
    sub('2.2 User groups'),tab([['User','Design responsibility'],['Administrator','Dashboard counts; profile/subject CRUD; enrollment; academic workflows and reports.'],['Faculty','Assigned-subject hub; current roster; attendance, assessment drafts/publication and scoped reports.'],['Student','Own attendance summaries and published marks; refresh and logout.'],['Operator / maintainer','Cloud configuration, deployment, allowlist provisioning, direct storage controls and eventual backups/monitoring.']], [133,W-133]),
    sub('2.3 Operating environment'),tab([['Item','Observed configuration'],['Application ID / namespace','com.example.studenttracker'],['Language / UI','Kotlin / Jetpack Compose + Material 3'],['Android SDK','Minimum 24; target 37; compile 37'],['Build JVM / compatibility','JDK 21 for build; Java source/target compatibility 11'],['Networking','Retrofit 2.11.0; OkHttp 4.12.0; Gson'],['Local persistence','Preferences DataStore metadata; temporary report files'],['Backend / storage','Apps Script V8 / two private Google Sheets tabs']], [150,W-150]),
    para('Current academic operations require connectivity. A fresh app process requires sign-in because tokens are not persisted. Durable offline drafts and automatic synchronization remain planned.'),
],2)
page('3. Architecture design',[
    sub('3.1 Architectural overview'),para('The design separates presentation packages, network contracts/session state, server domain logic and the spreadsheet adapter. The current client is screen-oriented: Compose screens invoke suspend API methods directly, usually through RemoteContent or a screen coroutine. Server domain rules are isolated in createService and tested with a store abstraction.'),
    sub('3.2 Architectural style'),tab([['Layer / boundary','Responsibility / source'],['Android entry and navigation','MainActivity, StudentTrackerApp and AppNavigation choose the verified role graph.'],['Presentation','Feature screens and shared WorkspacePage, RemoteContent, InfoCard and guardedBack.'],['Network / identity','GoogleSheetsApi models, NetworkModule, TrackerInterceptor and AuthSession.'],['Local metadata / export','UserPreferences and CsvExporter; FileProvider cache export sharing.'],['Backend authentication','auth.js verifies RSA signatures; core.js validates claims and allowlist identity.'],['Backend domain','createService applies CRUD, enrollment, attendance, assessments and report rules.'],['Cloud adapter','apps-script.js dispatches HTTP, obtains certificates, locks and reads/appends Sheets.']], [150,W-150]),
    sub('3.3 Asynchronous work and dependencies'),para('Compose LaunchedEffect and rememberCoroutineScope coordinate loads and user actions. CancellationException is rethrown. AuthSession exposes a read-only StateFlow. NetworkModule manually constructs one lazy Retrofit service; the baseline uses no dependency-injection framework or background worker.'),
    sub('3.4 Design rationale'),para('Sheets and Apps Script meet the compulsory backend requirement. Central domain authorization prevents UI-only permission enforcement. Journal versions preserve evidence, but reconstructing the entire journal per request creates a scaling limit. A future repository/ViewModel layer and recoverable current-state projection should improve maintainability and capacity without changing the mandated backend.'),
],3)
fig('3.5 System architecture','architecture','The device is outside the backend trust boundary. Google verifies identity issuance; Apps Script verifies the token and applies the account/subject scope before storage access. Device preferences are metadata only.',1)
fig('3.6 Component dependency view','components','Solid arrows show the main baseline dependencies. Shared screen helpers wrap direct suspend API calls. A Repository, Room cache and WorkManager queue are proposed future additions rather than existing components.',2,True)
page('4. Component design - identity and management',[
    sub('4.1 Entry and session component'),para('MainActivity creates the Compose root, applies the fixed dark theme, observes AuthSession.user and schedules expiry cleanup. key(email, role) rebuilds navigation for an identity change. StudentTrackerApp is a minimal Application extension point, not a dependency container.'),
    sub('4.2 Authentication component'),para('LoginScreen checks configuration, asks Credential Manager for a Google ID token and calls AuthSession.begin. getUserRole verifies identity on the server; the client checks success, supported role and future expiry, stores email/role metadata, then calls AuthSession.complete. Cancellation/failure clears the temporary token and provides user feedback.'),
    sub('4.3 Session storage component'),para('SignedInUser contains email, name, role and expiresAt. AuthSession holds the volatile token and MutableStateFlow privately. UserPreferences stores only email/role in Preferences DataStore; clearSession removes metadata and clears AuthSession. There is no stored refresh token or automatic authenticated restore.'),
    sub('4.4 Administrator and directory components'),para('AdminDashboardScreen loads real counts. DirectoryScreen adapts profile records into stable IDs, titles and subtitles, with case-insensitive search and keyed list rows. Student/Faculty/Subject list screens provide entity-specific loaders and navigation callbacks.'),
    sub('4.5 Profile and subject editors'),para('Add/Edit screens collect required and optional fields, perform frontend checks and submit typed requests. Record detail/edit destinations fetch the current record by encoded ID. Revisions protect updates and deactivation. Server checks remain authoritative for duplicates, active references, capacity and permissions.'),
    sub('4.6 Shared interaction components'),para('WorkspacePage supplies the Scaffold/top bar; RemoteContent models loading, failure, retry and successful content. guardedBack blocks leaving during supported writes and confirms discarding dirty forms. rememberSaveable provides supported configuration-change retention; it is not a durable offline database.'),
],4)
page('4. Component design - academic and backend',[
    sub('4.7 Academic hub and enrollment'),para('AcademicHubScreen lists active authorized subjects; FacultyDashboardScreen delegates to it. EnrollmentScreen loads students and the roster revision, edits a saveable selected-ID set and submits EnrollmentRequest. The server resolves active students and enforces a distinct roster of at most 250 IDs.'),
    sub('4.8 Attendance component'),para('AttendanceScreen loads minimal roster entries, maintains date/slot/status JSON, supports mark-all-present and confirms a complete roster. It derives a UUID from a payload fingerprint for matching retries. createService validates the roster/date/statuses and appends an immutable session snapshot keyed by subject/date/lowercase-slot.'),
    sub('4.9 Assessment component'),para('AssessmentsScreen lists assessments and opens an editor. New assessments use stable UUID identifiers; mark entries distinguish pending, absent and scored values. Draft saves include expected revision and request ID. Publication requires no pending marks and locks the stored published record.'),
    sub('4.10 Progress and reports'),para('StudentDashboardScreen calls getMyProgress without accepting an arbitrary student identity. AttendanceReportScreen shows a scoped subject snapshot and current fixed 75% shortage indicator. CsvExporter quotes cells, escapes quotes, preserves Unicode through UTF-8 BOM/CRLF and neutralizes spreadsheet formula prefixes.'),
    sub('4.11 Backend components'),tab([['Source component','Key design responsibilities'],['auth.js','JWT parsing, RS256 verification, trusted key constraints and error masking.'],['core.js','validateIdentity, principalFor and createService; domain authorization, rules, calculations and retry/conflict checks.'],['apps-script.js','doPost, doGet refusal, certificate caching, Script Properties, lock handling, setupTracker and sheetStore.'],['scripts/build.mjs','Bundle pinned RSA library + application source and generate manifest/license artifacts.']], [135,W-135]),
],4)
page('5. Data design - storage and concurrency',[
    sub('5.1 Storage design'),para('The live cloud data model has two tabs: TrackerUsers and TrackerRecords. Academic entities are serialized JSON snapshots in an append-only journal, not SQL tables or separate entity sheets. No Room database or encrypted local academic database exists in the baseline.'),
    tab([['Tab / columns','Design meaning'],['TrackerUsers','email, role, profileId, name, active, googleSubject. Exactly one active matching account is required. Faculty/student profileId links to an active domain record.'],['TrackerRecords','kind, id, data, at, actor, action. id and actor are JSON-encoded; data contains the entity snapshot. Cells are written with text format.']], [135,W-135]),
    sub('5.2 Projection and mutation algorithm'),
    *bullets(['Read journal rows in order and reconstruct latest values by (kind, decoded id).','Authenticate before academic reads; resolve the allowlist and role under the lock.','Check current record revision or attendance uniqueness; validate active references and complete rosters.','Append the next JSON snapshot with verified actor, action and timestamp; update the request-local Map.','Flush spreadsheet changes and release the lock in finally. Return a success or typed error envelope.']),
    sub('5.3 Concurrency and idempotency'),para('ScriptLock.tryLock uses a 10-second acquisition window and serializes supported operations, including reads. Revision checks protect profiles, enrollment and assessments. Identical supported retries are recognized; reusing a request ID with different assessment content returns conflict. The lock does not create a rollback-capable multi-operation database transaction.'),
    sub('5.4 Bounds and growth'),para('HTTP bodies are limited to 100,000 characters, tokens to 16,000 characters and saved aggregates to 40,000 characters. Enrollment allows at most 250 students per subject. Actual payload size can impose a lower practical capacity. Full-journal projection scans require a measured production optimization plan.'),
],5)
page('5. Data design - entity schema and calculations',[
    sub('5.5 Logical entity inventory'),tab([['Entity / key','Fields stored in JSON snapshot'],['Student / rollNumber','name, email, phone, department, batch, year, semester, section, parentName, parentPhone, parentEmail, active, revision, updatedAt.'],['Faculty / employeeId','name, email, phone, department, designation, joiningDate, active, revision, updatedAt.'],['Subject / subjectCode','subjectName, facultyId, semester, totalClasses, active, revision, updatedAt.'],['Enrollment / subjectCode','studentIds[], revision, updatedAt.'],['Attendance / subject-date-slot tuple','subjectCode, date, slot, requestId, records[] {studentId, name, status}, updatedAt.'],['Assessment / assessmentId','subjectCode, title, maxMarks, marks[] {studentId, name, status, score}, published, revision, requestId, updatedAt.']], [155,W-155]),
    sub('5.6 Calculation design'),para('Attendance denominator includes present, late and absent entries from snapshots containing the student. Numerator includes present and late; excused is excluded. percentage = 100 x numerator / denominator, displayed to one decimal. Denominator zero returns an empty percentage interpreted as N/A. Planned totalClasses is not substituted as the denominator.'),
    para('Maximum marks is 1..10000; scored marks are 0..maximum. Zero is a real score. Absent/pending have null score. Publication excludes pending entries. Final weighted grades, exemptions and retest policies require additional design and institution decisions.'),
    sub('5.7 Archive and schema evolution'),para('Supported deactivation sets active=false and preserves history. Archived IDs cannot be reused. A production migration mechanism is not yet implemented; proposed terms, corrections and indexed projections must have schema versions, tested migrations and rollback compatibility.'),
],5)
fig('5.8 Logical entity relationship diagram','er','The figure describes logical relationships only. Embedded roster and marks arrays preserve historical student IDs/names; direct spreadsheet operators still need restricted access. Physical persistence remains the two-tab journal design.',3)
page('6. Interface design - transport and reads',[
    sub('6.1 Authentication and network contract'),para('Credential Manager obtains a Google ID token for GOOGLE_WEB_CLIENT_ID. All actual academic requests are HTTPS JSON POST to the deployment /exec path. Some Retrofit read methods are declared @GET with @Query, but TrackerInterceptor copies query fields into the JSON body, removes the query string and adds idToken before transmission.'),
    para('The Web client ID is an audience identifier, not a service-account secret. The app does not call the Google Sheets REST API directly and does not use a Cloudflare role service. Apps Script uses its configured owner permissions to access the spreadsheet.'),
    sub('6.2 Read action inventory'),tab([['Action','Caller scope / response fields'],['getUserRole','Approved account -> email, name, role, expiresAt.'],['getDashboard','Admin -> dashboard {students, faculty, subjects, departments}.'],['getStudents / getStudent','Admin -> students[] / student; detail key rollNumber.'],['getFaculty / getFacultyMember','Admin -> faculty[] / facultyMember; detail key employeeId.'],['getSubject','Admin -> subject; key subjectCode.'],['getSubjects','Admin: all active; faculty: assigned; student: enrolled. Response subjects[].'],['getRoster','Admin or assigned faculty -> students[] {studentId,name}, revision.'],['getAttendanceReport','Admin or assigned faculty -> report[] with present/count/percentage.'],['getAssessments','Admin or assigned faculty -> assessments[] for subject.'],['getMyProgress','Student only -> own attendance[] and results[].']], [145,W-145]),
    sub('6.3 Envelope rules'),para('Success responses contain status="success", apiVersion=2 and action-specific fields. Failures contain status="error", apiVersion=2, code and message. Apps Script domain errors are expressed in JSON; the client must inspect the envelope even when HTTP transport succeeds. doGet rejects data requests.'),
],6)
page('6. Interface design - writes and examples',[
    sub('6.4 Mutation inventory'),tab([['Action','Required contract / scope'],['add/update/delete Student, Faculty, Subject','Administrator. Typed profile fields; update/delete expected revision. Delete deactivates rather than removes.'],['saveEnrollment','Administrator. subjectCode, studentIds[], revision.'],['saveAttendance','Admin or assigned faculty. subjectCode, date, slot, requestId, records[] {studentId,status}.'],['saveAssessment','Admin or assigned faculty. assessmentId, subjectCode, title, maxMarks, marks[], published, revision, requestId.']], [165,W-165]),
    sub('6.5 Example attendance request - illustrative data'),('code',json.dumps({'action':'saveAttendance','idToken':'<runtime Google ID token>','subjectCode':'DEMO101','date':'2026-10-01','slot':'Period 1','requestId':'<stable request UUID>','records':[{'studentId':'DEMO-S001','status':'present'}]},indent=2)),
    para('The example is valid only if the authorized subject roster contains exactly that student. It is not a live credential or claim that this demo session has been submitted.'),
    sub('6.6 Example conflict response'),('code',json.dumps({'status':'error','apiVersion':2,'code':'CONFLICT','message':'The class roster changed. Reload it before submitting.'},indent=2)),
    para('Internal interface: suspend Retrofit methods deserialize typed data classes; TrackerApiException carries a code and readable message. The backend store contract exposes get(kind,id), list(kind) and put(kind,id,data,audit), allowing domain tests to replace Sheets with an in-memory store.'),
],6)
page('6. Interface design - screens and exports',[
    sub('6.7 Screen responsibilities'),tab([['Screen group','Interaction design'],['Login','Google sign-in button, configuration failure and cancellation feedback.'],['Administrator dashboard','Active counts and management/academic/report navigation.'],['Directories / profile forms','Searchable stable-ID records; required fields; read-current edit; revision-aware save/deactivation.'],['Academic hub / enrollment','Authorized subject cards; administrator selection of enrolled students.'],['Attendance','Date/session fields, status chips, completion count, mark-all-present, confirmation and submitted state.'],['Assessment editor','Title/maximum, pending/absent/scored entries, draft save and explicit publish.'],['Student dashboard','Personal attendance indicators and published result cards; refresh/logout.'],['Attendance report','Subject snapshot, fixed threshold indication and CSV share action.']], [155,W-155]),
    sub('6.8 Navigation and form state'),para('AppNavigation constructs only the current role graph. Routes encode immutable record identifiers with Uri.encode and reload details rather than using a global selected-object cache. MainActivity keys the graph by email/role. Saveable form values support configuration changes; busy writes and dirty forms use guardedBack.'),
    sub('6.9 Export interface'),para('CSV output uses UTF-8 BOM, CRLF, quoted cells and escaped embedded quotes. Formula-like prefixes are neutralized. AttendanceReportScreen writes sanitized filenames under the cache export directory and shares through FileProvider with a temporary read grant. Cleanup removes export files older than 24 hours when cleanup runs.'),
    sub('6.10 Accessibility and UI limits'),para('The current theme is fixed dark and many labels are hard-coded. Large fonts, TalkBack, smaller screens, landscape and keyboard/system-bar interactions still require acceptance testing. String resources, consistent field-level feedback, filter/scroll restoration and optional theme selection are future improvements.'),
],6)
fig('6.11 Role-based navigation diagram','navigation','Shared staff routes are shown under the academic hub. Enrollment is administrator-only. The faculty hub receives backend-filtered subjects; student navigation exposes only the personal dashboard. The server independently checks every action.',4,True)
fig('7.1 Sign-in and role-resolution sequence','loginsequence','Credential issuance and application authorization are separate. The app begins a temporary token session, calls getUserRole and completes AuthSession only after a supported, unexpired server response. A fresh process repeats sign-in.',5,True,7)
fig('7.2 Attendance runtime flowchart','attendance','Attendance uses the current minimal roster. The payload fingerprint generates a stable retry identity; the server snapshots names/statuses and applies the unique session key. Roster changes and competing submissions return conflicts.',6)
fig('7.3 Assessment runtime flowchart','assessment','Drafts may contain pending marks. A publish request is validated against the complete current roster and expected revision. Published marks are read-only in the baseline and visible to the relevant student.',7)
fig('7.4 Attendance and assessment state transitions','states','States are conceptual descriptions of implemented fields/UI behavior. The baseline has no direct correction or unpublish path. The proposed correction extension must retain original and approved replacement evidence.',8,True)
page('8. Security design',[
    sub('8.1 Authentication trust boundary'),para('verifyGoogleToken accepts RS256 only, selects trusted Google signing material by kid and requires an RSA key of at least 2048 bits with exponent 65537. It verifies the signature and then issuer, audience, expiry, issued-at, optional not-before, subject and verified email. Cached Google certificates respect a bounded cache lifetime; an unknown key triggers one refresh.'),
    sub('8.2 Authorization design'),para('principalFor requires exactly one approved active TrackerUsers record and a supported role; an optional googleSubject pins the Google identity. An optional ALLOWED_GOOGLE_DOMAIN compares the signed hosted-domain claim. Non-admin actions require an active profile; subjectFor limits faculty to their assigned subjects; getMyProgress derives student scope from the verified principal.'),
    sub('8.3 Device and transport protection'),para('Tokens exist only in memory. DataStore metadata cannot establish authorization. Manifest cleartext traffic is disabled and backup is disabled with storage exclusions. NetworkModule adds tokens only to JSON request bodies; exports contain report data rather than credentials. These controls are not a claim of certificate pinning or encrypted local academic storage.'),
    sub('8.4 Backend and spreadsheet protection'),para('Validate body/token/field lengths before domain processing. Sheet journal cells use text format and JSON encoding to reduce unintended spreadsheet interpretation. Internal exception details are replaced by a generic client message. The public web-app endpoint still requires token verification and allowlist approval for all academic data.'),
    sub('8.5 Production security work'),para('Restrict operator spreadsheet sharing; establish cloud ownership and administrator recovery; add audited account/role management, redacted diagnostics, abuse controls and institution privacy/retention rules. Audit journal behavior is application-enforced and does not make the spreadsheet immutable to its owner.'),
],8)
page('9. Error handling and reliability',[
    sub('9.1 Failure categories and user response'),tab([['Failure code / condition','Handling design'],['UNAUTHENTICATED','Clear AuthSession; require sign-in; do not trust cached metadata.'],['FORBIDDEN','Show access denial; do not retry as another role or relax permissions.'],['VALIDATION / NOT_FOUND','Show actionable input/record message; user corrects or reloads.'],['CONFLICT','Reload current version/roster and review; never silently overwrite.'],['BUSY / UNAVAILABLE','Show temporary failure and allow controlled retry.'],['CAPACITY','Explain supported size limit; do not truncate academic records.'],['CONFIGURATION','Require matching deployment/audience/schema setup; refuse legacy API.'],['HTTP / INVALID_RESPONSE','Show service/deployment failure; close failed response.'],['INTERNAL','Expose a generic failure message; no internal stack trace in API response.'],['Coroutine cancellation','Rethrow cancellation rather than turning it into successful content.']], [145,W-145]),
    sub('9.2 Timeouts and retries'),para('OkHttp uses a 20-second connect timeout, 60-second read timeout and 75-second call timeout. Automatic connection retry is disabled. Server idempotency protects matching supported resubmissions, but there is no persistent client submission queue or background retry worker.'),
    sub('9.3 Data consistency and uncertainty'),para('An HTTP timeout can occur after a server write. Current workflows may allow an identical supported retry; durable submission status lookup/recovery remains planned. Revision checks and locking avoid silent competing updates, but multi-record operations need a checkpoint/partial-failure design before they are added.'),
    sub('9.4 Recovery limitations'),para('Forms may survive configuration changes; persistent offline draft recovery and term-level correction workflows are not implemented. Backup/restore, quota-aware operation, monitoring and release rollback must be established before institution-wide reliance.'),
],9)
page('10. Deployment and configuration',[
    sub('10.1 Build environment'),para('Use Android Studio or the Gradle wrapper with JDK 21 and required Android SDK packages. app/build.gradle.kts configures minSdk 24, target/compile 37, Java compatibility 11 and app version 1.0. Node.js 22+ builds/tests the Apps Script bundle with the pinned node-forge dependency.'),
    sub('10.2 External configuration'),tab([['Configuration','Location / purpose'],['TRACKER_BASE_URL','External Gradle property/environment; deployment URL without exec, ending in /.'],['GOOGLE_WEB_CLIENT_ID','Android build value and matching Script Property; token audience.'],['SPREADSHEET_ID','Script Property selecting the private institution spreadsheet.'],['ALLOWED_GOOGLE_DOMAIN','Optional Script Property; validate signed Google hosted-domain claim.'],['Script timezone / runtime','Generated appsscript.json: Asia/Kolkata; V8.'],['Script OAuth scopes','Spreadsheet access and external requests for certificate retrieval.'],['Android OAuth client','Register application package and actual debug/release signing SHA-1.']], [160,W-160]),
    sub('10.3 Deployment sequence'),para('Build the backend; upload dist/Code.gs and appsscript.json; set properties; run setupTracker and approve required scopes; provision approved account mappings; deploy the web app executing as owner. Where institution policy permits Anyone access, backend authentication remains mandatory. Configure and rebuild the Android client, then run all-role acceptance on a real device.'),
    sub('10.4 Current setup and release boundary'),para('Demo OAuth values, deployment URL and spreadsheet are configured, initialization succeeded and administrator/faculty/student mappings exist. The debug APK was rebuilt with real configuration. Live sign-in/full workflow and production signing are not yet verified. Production needs distinct environments, institutional ownership, secure release key handling and rollback evidence.'),
],10)
fig('10.5 Deployment topology','deployment','Developer artifacts are deployed to a Google Apps Script web app and installed as an Android APK. OAuth registration links the package/signing certificate to identity issuance; Apps Script owns authorized access to the private spreadsheet. No production credentials are printed here.',9,True)
page('11. Testing and verification',[
    sub('11.1 Recorded local verification'),para('Previous verification records show 27 backend tests passed and 9 Android JVM tests passed, plus successful debug assembly and lint with zero errors and 33 warnings. The count includes existing template tests. This SDD creation does not constitute a new app test run.'),
    para('Backend tests exercise independently signed RSA tokens, claims, allowlists, domain permissions, calculations, revisions, retry handling and mocked Apps Script/Sheets operations. Android tests cover CSV handling and authenticated network transport/failures. Mock tests do not prove live Google identity or quota behavior.'),
    sub('11.2 Acceptance test plan - outstanding live evidence'),tab([['Test ID','Scenario / expected result'],['SDD-T01','Google approved sign-in and cancellation; invalid/inactive accounts denied; correct role routing.'],['SDD-T02','Admin creates/edits profiles/subject and enrollment; duplicates and stale revisions rejected.'],['SDD-T03','Assigned faculty submits full roster; identical retry gives one session; unassigned request denied.'],['SDD-T04','Calculate present/late/absent/excused and N/A correctly; preserve historical roster eligibility.'],['SDD-T05','Draft pending allowed; publish pending denied; zero/absent correct; published edit denied.'],['SDD-T06','Student sees own attendance/marks and no other student/draft data.'],['SDD-T07','CSV opens on phone with Unicode, quotes and safe formula handling.'],['SDD-T08','Rotation, process restart, logout/account change and network loss produce accurate state.'],['SDD-T09','Concurrent edits, lock contention, lost response and realistic journal growth tested.'],['SDD-T10','Release signing, accessibility, backups/restore, migrations and rollback verified before production.']], [95,W-95]),
    para('Record actual device/environment, date, expected/actual result and logs/screenshots when executing this plan. The template instrumented test is not an end-to-end acceptance suite.'),
],11)

built_trace=[
('01','Launch','MainActivity / AppNavigation'),('02','Google sign-in','LoginScreen / auth.js'),('03','Role and profile authorization','principalFor / subjectFor / getMyProgress'),('04','Expiry and logout','AuthSession / UserPreferences / MainActivity'),
('05','Admin dashboard','AdminDashboardScreen / getDashboard'),('06','Student management','Student screens / core.js CRUD'),('07','Faculty management','Faculty screens / core.js CRUD'),('08','Subject management','Subject screens / active faculty validation'),('09','Safe deactivation','CRUD delete branch / active=false'),('10','Enrollment','EnrollmentScreen / saveEnrollment'),('11','Faculty workspace','AcademicHubScreen / getSubjects'),('12','Attendance capture','AttendanceScreen / validateRoster'),('13','Attendance persistence','saveAttendance / unique key / retry check'),('14','Attendance calculation','attendanceReport / snapshot aggregation'),('15','Assessment drafts','AssessmentEditor / saveAssessment'),('16','Assessment publication','published guard / no-pending validation'),('17','Student progress','StudentDashboardScreen / getMyProgress'),('18','Reports / CSV','AttendanceReportScreen / CsvExporter'),('19','Loading / retry / form protection','RemoteContent / guardedBack / saveable state'),('20','Audit / conflicts','sheetStore / ScriptLock / revision checks'),
]
page('12. Requirements traceability - baseline',[
    para('Each suffix below expands to CUTM-SRS-SAPT-XX in the project SRS. Built means implementation evidence exists; it does not mean every listed action passed live device acceptance.'),
    tab([['SRS ID','Requirement','Design / source evidence']]+[list(x) for x in built_trace],[52,140,W-192]),
    sub('12.1 Consistency with the SRS'),para('Both documents describe Compose, minSdk 24, direct screen-to-API calls, Google token verification, an online baseline and two Sheets tabs. Neither claims Room/Hilt/WorkManager, configurable grading or approved published-record corrections as implemented.'),
],12)
page('12. Requirements traceability - planned scope',[
    tab([['SRS ID','Status','Required design extension'],['21','Planned','Account-management screens; provisioning/suspension, audited role changes and last-admin recovery safeguards.'],['22','Planned','Terms/programs/sections/subject offerings; dated enrollment; configurable effective-dated attendance and grade policies.'],['23','Planned','Correction request lifecycle, approval permissions and before/after audited versions for attendance/marks.'],['24','Planned','Persistent account-isolated drafts/queue, stable operation IDs, permission revalidation and conflict recovery.'],['25','Planned','Import templates/preview/errors; term/section/date report filters; printable progress reports.'],['26','Planned','Separate environments, backups/restore, capacity/monitoring, privacy ownership and signed release acceptance.'],['27','Optional','Separately scoped QR/location attendance, communication, timetable, assignments, guardian access or analytics.']], [54,67,W-121]),
    sub('12.2 Non-functional design mapping'),tab([['Quality','Mechanism / remaining evidence'],['Security and privacy','Signed identity, server scoping, token memory, narrow exports; live denial/retention/abuse tests still required.'],['Correctness and integrity','Roster validation, status semantics, revisions, audit journal and matching retries; device/concurrency acceptance pending.'],['Performance and capacity','Request bounds and serialized access exist; p95/load targets need measurement and projection/pagination work.'],['Recovery and availability','Backup restore, monitoring, operator response and durable draft recovery are production work.'],['Accessibility and maintainability','Shared UI and isolated domain store help; device accessibility and CI/refactor work remain.']], [135,W-135]),
],12)
page('13. Limitations and future enhancements',[
    sub('13.1 Known baseline limitations'),
    *bullets(['Live Google sign-in, deployed academic operations and real-device acceptance are not yet established.','No durable offline academic database, background sync, refresh-token persistence or automatic authenticated process restart.','No first-class academic term/section/subject offering model or dated enrollment lifecycle.','Attendance shortage is currently fixed at 75%; weighted grades, retests and institution policies are not implemented.','Submitted attendance and published marks are locked without an approved correction workflow.','Allowlist account administration remains an operator task rather than an in-app workflow.','Full-journal scans, global locking and bounded aggregates limit capacity; no backend pagination/compaction is implemented.','Production signing, automated backup restore, monitoring, migration/rollback and complete accessibility evidence remain outstanding.']),
    sub('13.2 Proposed improvements'),para('Prioritize live demo acceptance, account/academic lifecycle and corrections, then durable drafts, bulk import, filtered reports and capacity improvements. Refactor presentation/data boundaries where needed for testable state and synchronization. All release gates must pass before production sign-off.'),
    sub('13.3 Optional scope'),para('Consider notifications, timetable, QR/location assistance, assignments/Drive attachments, guardian access, delegated roles, analytics and theme/language choices only after explicit institution scope decisions. Each extension needs its own authorization, privacy, dependency and acceptance design.'),
],13)
page('14. Conclusion and references',[
    sub('14.1 Design conclusion'),para('Student Tracker provides a connected Android design for administrator preparation, scoped staff attendance/assessment entry and personal student progress. Google Apps Script authenticates and authorizes requests before applying domain rules and appending JSON snapshots to Google Sheets. Revision and retry semantics protect supported writes from silent duplication or stale replacement.'),
    para('The baseline is suitable for further demonstration and acceptance work, with recorded local verification and configured demo resources. Production readiness remains dependent on live all-role testing, academic lifecycle/policy/correction features, recovery and operational controls. This SDD records those limits so the design, SRS and implementation remain aligned.'),
    sub('14.2 Referenced material'),
    *bullets(['TeamPulse_SDD 1.pdf, 19 pages: supplied structure/layout reference; sample names, guide and technology choices are not inherited.','Student_Tracker_SRS.pdf / docs/Student_Tracker_SRS.md: CUTM-SRS-SAPT-VER-0.1 requirements baseline.','docs/FEATURE_CHECKLIST.md and PRODUCTION_PLAN.md: implementation and production backlog.','docs/DEMO_SETUP_STATUS.md and docs/STAGING_CHECKLIST.md: deployment evidence and remaining live acceptance.','Android source under app/src/main; app/build.gradle.kts; gradle/libs.versions.toml; backend/src and backend/test.','backend/README.md: deployment procedure. Its initial undeployed description is superseded by DEMO_SETUP_STATUS.md.']),
    sub('14.3 Document control and review'),tab([['Version / date','Change / review state'],['0.1 / 1 October 2026','Initial project SDD; seven team members included; architecture, runtime, state and deployment diagrams; SRS traceability.'],['Review status','Team/institution review draft. Guide identity, member task allocation and production sign-off are not asserted.']], [145,W-145]),
],14)
page('Appendix A. Source component index',[
    para('Paths below are relative to the repository root. Android package root is app/src/main/java/com/example/studenttracker/. This index supports code explanation and links design responsibilities to implementation.'),
    tab([['Location','Responsibility'],['MainActivity.kt / StudentTrackerApp.kt','Activity/session-driven composition and application entry point.'],['presentation/navigation/AppNavigation.kt','Role graphs; URI-encoded ID routes; remote detail loads.'],['presentation/auth/LoginScreen.kt','Google credential acquisition and role handshake.'],['data/network/AuthSession.kt / NetworkModule.kt','Memory session, token POST interceptor, typed failures, lazy Retrofit.'],['data/network/GoogleSheetsApi.kt / AcademicModels.kt','Profile API contracts and academic request/response models.'],['data/local/UserPreferences.kt','Email/role Preferences DataStore metadata and logout cleanup.'],['presentation/admin / faculty / student / subject','Dashboards, directories, profile/subject forms and details.'],['presentation/academic','Academic hub, enrollment, attendance and assessments.'],['presentation/components','Shared remote loading/layout and unsaved-change guards.'],['domain/model/CsvExporter.kt; res/xml/file_paths.xml','CSV safety and narrow export file sharing.'],['backend/src/auth.js / core.js / apps-script.js','Token verifier, business rules, HTTP/Sheets adapter.'],['backend/test; app/src/test','Backend domain/auth/adapter and Android transport/CSV tests.'],['backend/scripts/build.mjs; backend/dist','Bundle generation; generated Apps Script code/manifest/license.']], [235,W-235]),
    para('No production credentials, keystore passwords or private signing material are included in this document. The supplied team list records authorship, not unverified module ownership.'),
])

contents=[['Section','Page']]
for sec in range(1,15):
    start=next(i+1 for i,(_,_,s) in enumerate(pages) if s==sec)
    nextstart=next((i+1 for i,(_,_,s) in enumerate(pages) if s is not None and s>sec),len(pages))
    end=nextstart-1
    title=pages[start-1][0].split(' - ',1)[0]
    contents.append([title,str(start) if start==end else f'{start}-{end}'])
contents.append(['Appendix A. Source component index',str(len(pages))])
figure_rows=[['Figure','Page']]
for i,(title,elems,_) in enumerate(pages,1):
    if any(k in ['design','diagram'] for k,_ in elems):figure_rows.append([title,str(i)])
pages[1]=('Table of contents',[tab(contents,[W-65,65]),sub('List of figures'),tab(figure_rows,[W-65,65])],None)

story=[];md=['# Software Design Document\n\nStudent Attendance & Performance Tracker\n\nVersion 0.1 | 1 October 2026\n']
for i,(title,elems,_) in enumerate(pages):
    if i:story.append(PageBreak());story.append(p(title,'H1X'))
    md.append('\n## '+title+'\n')
    for kind,value in elems:
        if kind=='cover':story.append(p(value,'TitleX'));md.append(value+'\n')
        elif kind=='smallcenter':
            style=ParagraphStyle('CenterX',parent=styles['BodyX'],alignment=1,textColor=INK,fontName='Bold');story.append(Paragraph(html.escape(value),style));md.append(value+'\n')
        elif kind in ['p','h','b']:
            story.append(p(('• ' if kind=='b' else '')+value,'H2X' if kind=='h' else 'BodyX'));md.append(('### ' if kind=='h' else '- ' if kind=='b' else '')+value+'\n')
        elif kind=='t':
            rows,widths=value;story+=[table(rows,widths),Spacer(1,9)]
            md+=['| '+' | '.join(rows[0])+' |','| '+' | '.join(['---']*len(rows[0]))+' |']
            md+=['| '+' | '.join(str(v).replace('|','/') for v in row)+' |' for row in rows[1:]];md.append('')
        elif kind=='space':story.append(Spacer(1,value))
        elif kind=='code':story.append(Preformatted(value,styles['CodeX']));md.append('```json\n'+value+'\n```\n')
        elif kind in ['diagram','design']:
            story.append((Diagram if kind=='diagram' else DesignDiagram)(value));md.append(f'<!-- Vector figure: {value}. See generated PDF and docs/build_sdd.py. -->\n')

def footer(c,doc):
    c.setLineWidth(.7);c.setStrokeColor(INK);c.line(46,43,A4[0]-46,43)
    c.setFillColor(GRAY);c.setFont('Body',8);c.drawString(46,29,'CUTM-SDD-SAPT | VERSION 0.1 | 01 OCT 2026');c.drawRightString(A4[0]-46,29,f'{doc.page} / {len(pages)}')
    if doc.page>1:c.drawString(46,A4[1]-30,'Student Tracker | Software Design Document')
SimpleDocTemplate(str(OUT),pagesize=A4,leftMargin=46,rightMargin=46,topMargin=53,bottomMargin=58,title='Student Attendance & Performance Tracker - Software Design Document',author='; '.join(n for n,_ in members)).build(story,onFirstPage=footer,onLaterPages=footer)
SOURCE.write_text('\n'.join(md),encoding='utf-8')
reader=PdfReader(OUT)
if len(reader.pages)!=len(pages):raise RuntimeError(f'Page overflow: expected {len(pages)}, produced {len(reader.pages)}. Fix contents/layout before delivery.')
text='\n'.join(pg.extract_text() or '' for pg in reader.pages)
for name,roll in members:
    if name not in text or roll not in text:raise RuntimeError('Missing team member: '+name)
print(json.dumps({'pdf':str(OUT),'pages':len(reader.pages),'figures':len(figure_rows)-1,'members':len(members),'source':str(SOURCE)},indent=2))
