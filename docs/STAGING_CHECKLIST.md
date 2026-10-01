# Staging acceptance checklist

Run against a private staging spreadsheet and three dedicated test accounts.
Record the deployment version, app build, device/API level and results before release.

## Identity and permissions
- Approved administrator signs in; an unknown or inactive account is rejected.
- Student and faculty accounts cannot access administrator actions, including when
  requests are made directly rather than through the app.
- A faculty member cannot access another faculty member's subject roster or marks.
- A student cannot select another student's records using a request parameter.
- A tampered/expired token is rejected; expiration and logout return the app to login.
- Closing/restarting the app requires sign-in (tokens currently stay in memory).

## Records and enrollment
- Create faculty, students and subjects; counts reflect active records.
- Duplicate IDs/emails and nonexistent faculty assignments are rejected.
- Enroll distinct students in a subject; verify the faculty roster matches exactly.
- Two administrators editing the same record cannot overwrite a newer revision.
- Deactivate a student; previous submitted records remain. Faculty assignment blocks
  deactivation until subjects are reassigned.
- Rotate the device on create/edit forms; field values survive and navigation remains correct.

## Attendance
- Submit present/absent/late/excused entries for a full roster and a valid date/session.
- Missing/duplicate students and future/invalid dates are rejected.
- Submit the same request twice and simulate a lost response; no second session is created.
- Verify present + late numerator, excused exclusion, zero denominator and newly enrolled students.
- Switch subjects quickly during report loading; export must match the displayed subject.
- Export names containing commas/quotes/newlines and formula-like prefixes; open the CSV
  in a spreadsheet app and check both formatting and formula neutralization.

## Marks
- Save a draft with zero, absent and pending; students see no draft marks.
- Reject values outside 0..maximum and attempts to publish pending marks.
- Publish complete marks; each student sees their own result only.
- Retry a save after losing the response; no duplicate assessment or revision is created.
- Concurrent draft edits detect stale revisions. Published results reject changes.

## Mobile and operations
- Check narrow/large screens, large font sizes, TalkBack and keyboard navigation.
- Check network loss, service errors, retries, back while saving and unsaved-change dialogs.
- Confirm live Apps Script certificate fetch, cache rotation and lock behavior.
- Measure response times and quotas at expected journal size/concurrency.
- Restore a backup into a separate sheet and confirm records/audit metadata are intact.
- Rehearse deployment rollback and verify monitoring/support procedures.

No checks in this file have been marked complete solely from local mocks or compilation.
