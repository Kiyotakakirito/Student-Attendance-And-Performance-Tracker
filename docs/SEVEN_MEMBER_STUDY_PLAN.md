# Seven-member study and presentation allocation

Everyone: read original guide pages 1-5 first. Page references below are the original 207-page PDF page numbers, not source line numbers. Each file has exactly one primary owner; members should still know the overall request flow.

Aim for 60-90 seconds per speaker; use 2 minutes each if your presentation slot allows it. This allocates responsibility, not authorship credit.

## Member 1: App startup, login, and navigation

Explain app startup, Compose state, Google login, session expiry, logout, role-specific routes, and theme configuration. Trace LoginScreen to AuthSession to AppNavigation.

Practice questions: What happens when a token expires? Why is saved role metadata not authorization?

| Original guide pages | Assigned file |
| --- | --- |
| 18-19 | `app/src/main/java/com/example/studenttracker/MainActivity.kt` |
| 20 | `app/src/main/java/com/example/studenttracker/StudentTrackerApp.kt` |
| 21-22 | `app/src/main/java/com/example/studenttracker/data/local/UserPreferences.kt` |
| 24 | `app/src/main/java/com/example/studenttracker/data/network/AuthSession.kt` |
| 36 | `app/src/main/java/com/example/studenttracker/domain/model/UserRoleResponse.kt` |
| 54-58 | `app/src/main/java/com/example/studenttracker/presentation/auth/LoginScreen.kt` |
| 86-89 | `app/src/main/java/com/example/studenttracker/presentation/navigation/AppNavigation.kt` |
| 132 | `app/src/main/java/com/example/studenttracker/ui/theme/Color.kt` |
| 133-134 | `app/src/main/java/com/example/studenttracker/ui/theme/Theme.kt` |
| 135 | `app/src/main/java/com/example/studenttracker/ui/theme/Type.kt` |

## Member 2: Administrator and student profile management

Explain real dashboard counts, student search, add/edit/detail/deactivate screens, required fields, dirty-state guards, and revision numbers. Demonstrate creating or editing one student.

Practice questions: Why deactivate instead of deleting? How are unsaved changes protected?

| Original guide pages | Assigned file |
| --- | --- |
| 48-49 | `app/src/main/java/com/example/studenttracker/presentation/admin/AdminDashboardScreen.kt` |
| 90-98 | `app/src/main/java/com/example/studenttracker/presentation/student/AddStudentScreen.kt` |
| 99-104 | `app/src/main/java/com/example/studenttracker/presentation/student/EditStudentScreen.kt` |
| 107-112 | `app/src/main/java/com/example/studenttracker/presentation/student/StudentDetailsScreen.kt` |
| 113 | `app/src/main/java/com/example/studenttracker/presentation/student/StudentListScreen.kt` |

## Member 3: Faculty and subject profile management

Explain faculty and subject directories, add/edit/detail forms, faculty assignment, immutable identifiers, and planned class count. The repeated form patterns make this larger page allocation manageable.

Practice questions: How is a faculty member assigned to a subject? Who can manage these profiles?

| Original guide pages | Assigned file |
| --- | --- |
| 66-72 | `app/src/main/java/com/example/studenttracker/presentation/faculty/AddFacultyScreen.kt` |
| 73-78 | `app/src/main/java/com/example/studenttracker/presentation/faculty/EditFacultyScreen.kt` |
| 80-84 | `app/src/main/java/com/example/studenttracker/presentation/faculty/FacultyDetailsScreen.kt` |
| 85 | `app/src/main/java/com/example/studenttracker/presentation/faculty/FacultyListScreen.kt` |
| 114-119 | `app/src/main/java/com/example/studenttracker/presentation/subject/AddSubjectScreen.kt` |
| 120-125 | `app/src/main/java/com/example/studenttracker/presentation/subject/EditSubjectScreen.kt` |
| 126-130 | `app/src/main/java/com/example/studenttracker/presentation/subject/SubjectDetailsScreen.kt` |
| 131 | `app/src/main/java/com/example/studenttracker/presentation/subject/SubjectListScreen.kt` |

## Member 4: Enrollment, attendance, assessments, and student progress

Explain enrollment, the full roster, attendance statuses, date/slot, draft marks, publication, personal results, shared page components, and loading/retry. Trace one attendance form submission; coordinate server validation with Member 6.

Practice questions: What is the attendance formula? How do zero, pending, and absent differ? Why cannot published marks be changed?

| Original guide pages | Assigned file |
| --- | --- |
| 37-38 | `app/src/main/java/com/example/studenttracker/presentation/academic/AcademicHubScreen.kt` |
| 39-42 | `app/src/main/java/com/example/studenttracker/presentation/academic/AssessmentsScreen.kt` |
| 43-45 | `app/src/main/java/com/example/studenttracker/presentation/academic/AttendanceScreen.kt` |
| 46-47 | `app/src/main/java/com/example/studenttracker/presentation/academic/EnrollmentScreen.kt` |
| 59-60 | `app/src/main/java/com/example/studenttracker/presentation/components/DirectoryScreen.kt` |
| 61-62 | `app/src/main/java/com/example/studenttracker/presentation/components/SkeletonLoading.kt` |
| 63-65 | `app/src/main/java/com/example/studenttracker/presentation/components/WorkspaceComponents.kt` |
| 79 | `app/src/main/java/com/example/studenttracker/presentation/faculty/FacultyDashboardScreen.kt` |
| 105-106 | `app/src/main/java/com/example/studenttracker/presentation/student/StudentDashboardScreen.kt` |

## Member 5: API communication, models, and CSV reports

Explain request/response data classes, suspend Retrofit methods, Gson, authenticated JSON POST conversion, response errors, report export, FileProvider sharing, and the interceptor/CSV tests.

Practice questions: Why are declared GET requests sent as POST? What prevents spreadsheet formula execution?

| Original guide pages | Assigned file |
| --- | --- |
| 23 | `app/src/main/java/com/example/studenttracker/data/network/AcademicModels.kt` |
| 25-31 | `app/src/main/java/com/example/studenttracker/data/network/GoogleSheetsApi.kt` |
| 32-34 | `app/src/main/java/com/example/studenttracker/data/network/NetworkModule.kt` |
| 35 | `app/src/main/java/com/example/studenttracker/domain/model/CsvExporter.kt` |
| 50-53 | `app/src/main/java/com/example/studenttracker/presentation/admin/AttendanceReportScreen.kt` |
| 151 | `app/src/test/java/com/example/studenttracker/CsvExporterTest.kt` |
| 153-154 | `app/src/test/java/com/example/studenttracker/TrackerInterceptorTest.kt` |

## Member 6: Backend security and academic rules

Explain Google JWT signature and claim checks, approved account roles, CRUD rules, revisions, full-roster validation, duplicate-session rules, assessment publication, student privacy, and domain/authentication tests. Prioritize rules over memorizing cryptographic internals.

Practice questions: Why must the server check permissions? What are issuer, audience, and expiry? How are stale edits rejected?

| Original guide pages | Assigned file |
| --- | --- |
| 165-166 | `backend/src/auth.js` |
| 167-175 | `backend/src/core.js` |
| 179-180 | `backend/test/auth.test.cjs` |
| 181-187 | `backend/test/core.test.cjs` |

## Member 7: Google Sheets integration, setup, resources, and validation

Explain doPost, Script Properties, certificate caching, script locks, journal projection, setupTracker, backend packaging, Android build settings/resources, tests, and staging requirements. For wrapper scripts and icon geometry, explain their purpose rather than memorize generated details. Own the live-configuration limitation and conclusion.

Practice questions: How do the two Sheets work? Why a script lock? What has actually been tested and what still needs live verification?

| Original guide pages | Assigned file |
| --- | --- |
| 6 | `.gitignore` |
| 7-8 | `PRODUCTION_PLAN.md` |
| 9-10 | `README.md` |
| 11 | `app/.gitignore` |
| 12-14 | `app/build.gradle.kts` |
| 15 | `app/src/androidTest/java/com/example/studenttracker/ExampleInstrumentedTest.kt` |
| 16-17 | `app/src/main/AndroidManifest.xml` |
| 136 | `app/src/main/keepRules/rules.keep` |
| 137-141 | `app/src/main/res/drawable/ic_launcher_background.xml` |
| 142 | `app/src/main/res/drawable/ic_launcher_foreground.xml` |
| 143 | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` |
| 144 | `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` |
| 145 | `app/src/main/res/values/colors.xml` |
| 146 | `app/src/main/res/values/strings.xml` |
| 147 | `app/src/main/res/values/themes.xml` |
| 148 | `app/src/main/res/xml/backup_rules.xml` |
| 149 | `app/src/main/res/xml/data_extraction_rules.xml` |
| 150 | `app/src/main/res/xml/file_paths.xml` |
| 152 | `app/src/test/java/com/example/studenttracker/ExampleUnitTest.kt` |
| 155-157 | `backend/README.md` |
| 158 | `backend/package-lock.json` |
| 159 | `backend/package.json` |
| 160 | `backend/scripts/build.mjs` |
| 161-164 | `backend/src/apps-script.js` |
| 176-178 | `backend/test/apps-script.test.cjs` |
| 188 | `build.gradle.kts` |
| 189-190 | `docs/STAGING_CHECKLIST.md` |
| 191 | `gradle.properties` |
| 192 | `gradle.properties.example` |
| 193 | `gradle/gradle-daemon-jvm.properties` |
| 194 | `gradle/libs.versions.toml` |
| 195 | `gradle/wrapper/gradle-wrapper.properties` |
| 196-201 | `gradlew` |
| 202-204 | `gradlew.bat` |
| 205 | `settings.gradle.kts` |

Also study pages 206-207: all 11 binary assets and generated/private infrastructure categories.

## Speaking order

1. Member 1: problem, architecture, login, and role navigation.
2. Member 2: administrator and student record management.
3. Member 3: faculty and subject setup.
4. Member 4: enrollment, attendance, marks, and student view.
5. Member 5: API request flow and reports.
6. Member 6: server permission checks and academic rules.
7. Member 7: Sheets storage, testing, deployment status, and conclusion.

## Shared rehearsal

Every member should explain: the project purpose; the three roles; Android -> API -> Apps Script -> Sheets -> JSON -> UI; the attendance formula; and the fact that local verification does not prove live deployment.

For your assigned files, prepare four answers: what the file owns, its input, its main operations, and its output/error behavior. Open the guide at the exact source block if asked about a particular line.

Rehearse one attendance request together: Member 4 explains the screen; Member 5 the network; Member 6 the validation; Member 7 the Sheets write. Members 1-3 explain the identity, student profile, and subject prerequisites.

Before the live demo, verify Google authentication and Apps Script deployment. If unavailable, clearly label the demonstration as a source/test walkthrough.
