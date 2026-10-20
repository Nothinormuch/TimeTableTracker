# Timetable Change Notification System — Project Brief

Oct 19, 2026 · @Mika Chan

## Project overview

The college portal sends no notifications when the timetable changes — classes get added, removed, or rescheduled without any alert. Students miss classes or show up to the wrong room.

This project builds a **self-contained native Android app** (Kotlin) that monitors the portal on a schedule and sends notifications when anything changes. The app uses Android's `AlarmManager` to fire at configured times; it does **not** run persistently in the background. Between alarms the process is fully dead — no services, no wake locks, no battery drain.

## Goals

- **Primary:** Detect and notify about timetable changes automatically
- **Secondary:** Minimize battery consumption — the app is completely inactive between scheduled checks
- **Tertiary:** Reliable, timely notifications through multiple channels (system notification + WhatsApp)

## App architecture — no persistent background process

**The app does not run in the background.** This is a hard architectural constraint, not a goal to approximate.

### How AlarmManager works

Android's `AlarmManager` (specifically `setExactAndAllowWhileIdle`) lets an app register a future alarm with the OS. The OS holds the alarm — not the app. When the alarm fires, Android briefly wakes the app, runs a `BroadcastReceiver`, does the work, and the process dies again. Nothing keeps the app alive in between.

This is the same mechanism clock apps and calendar reminders use. It survives Doze mode and fires on time regardless of battery saver.

### What is and isn't running

| State | What is alive |
| --- | --- |
| Between checks | Nothing — the app process does not exist |
| Alarm fires | OS wakes a `BroadcastReceiver` for \~10 seconds |
| During fetch | A `WorkManager` one-off job runs (guaranteed to finish even if screen locks) |
| Job complete | Process exits; OS reschedules next alarm |

### Why not WorkManager for scheduling?

`WorkManager` is designed for deferrable work and does not guarantee exact timing — it can drift by 30–90 minutes under Doze and battery saver. `AlarmManager` with `setExactAndAllowWhileIdle` fires on time. The app uses `AlarmManager` for scheduling and `WorkManager` only as the execution container for the fetch job itself (to survive screen-off and process death mid-fetch).

### Boot persistence

A `BOOT_COMPLETED` `BroadcastReceiver` re-registers the next alarm after a reboot. The receiver runs for milliseconds and exits; nothing stays resident.

## System components

### 1. Trigger mechanism

- `AlarmManager.setExactAndAllowWhileIdle()` schedules exact-time alarms
- `BOOT_COMPLETED` receiver re-registers alarms after reboot
- User-configurable schedule (e.g. every 2 hours on weekdays)
- Conditional execution: skip check if no network available

### 2. Data acquisition module

- Authenticates against the myUPES API (3-step flow: token → StudentId → timetable)
- Static app secret: `ku7GUMtyT8er51rTfTc7HC`; `Captcha: "-"` header bypasses reCAPTCHA
- Fetches timetable JSON for a configurable date range
- Stores raw response and parsed snapshot to local file storage

### 3. Comparison engine

- Loads previous snapshot from storage
- Computes set differences: added slots, removed slots
- Field-by-field diff for modified slots (time, venue, instructor, cancellation status)
- Generates a human-readable change report

### 4. Notification & Visualization system

- **System notification:** `NotificationManager` — fires immediately on detection, no user interaction needed.
- **In-App Highlighted Timetable:** Displayed directly on the Home tab in a clean, interactive tabular format:
  - **Green border:** Added classes.
  - **Red border:** Deleted/removed classes (retained from old timetable for immediate visual contrast).
  - **Muted-orange border:** Rescheduled/modified classes.
- Silent exit if no changes detected.

### 5. Storage layer

- Only stores the timetable from the previous check at any instance.
- When changes are detected, notes the changes, saves the new timetable, and deletes/replaces the old one.
- Annotated display snapshot file for Home tab UI rendering.
- `SharedPreferences` for metadata (last check time, token expiry, schedule config).

### 6. Settings UI

- One-time login screen (credential form)
- Schedule configuration
- Manual "check now" trigger
- Notification preferences

## User flow

```
[AlarmManager fires — OS wakes BroadcastReceiver]
    ↓
[Enqueue one-off WorkManager job]
    ↓
[Check network; abort silently if offline]
    ↓
[Authenticate: POST /oauth2/token → GET studentinfo → token cached]
    ↓
[POST /api/timetable — fetch current slots]
    ↓
[Load previous snapshot]
    ↓
[Compare] ──→ [No changes] → [Save new snapshot / overwrite old] → [Update last-checked time] → [Exit]
    ↓
[Changes detected]
    ↓
[Annotate display slots: Green=Added, Red=Deleted, Muted-Orange=Rescheduled]
    ↓
[Overwrite previous snapshot with new timetable]
    ↓
[Send system notification]
    ↓
[Schedule next AlarmManager alarm] → [Exit — process dies]
```

## Key features

### Must-have (MVP)

- [ ] `AlarmManager` exact-alarm scheduling
- [ ] `BOOT_COMPLETED` receiver to re-register alarms after reboot
- [ ] myUPES API authentication with token caching
- [ ] Timetable fetch and local JSON snapshot storage
- [ ] Basic comparison: added and removed slots
- [ ] System notification on change
- [ ] WhatsApp self-message intent on change
- [ ] Settings screen: login, schedule, manual trigger

### Should-have (V1.1)

- [ ] Field-level diff: time, venue, instructor, cancellation flag changes
- [ ] Configurable check frequency per day
- [ ] Token expiry detection and re-auth prompt
- [ ] Retry logic with exponential backoff on network failure
- [ ] Change history log
- [ ] Quiet hours (no notifications between e.g. 11 PM–7 AM)

### Nice-to-have (V2.0)

- [ ] Home screen widget showing next class
- [ ] Smart scheduling (skip weekends and vacation periods)
- [ ] Multiple notification channels (email, Telegram)
- [ ] Shareable change report (send to classmates)
- [ ] iCal export

## Technical considerations

### Framework

Native Android (Kotlin). Required for `AlarmManager` exact alarms, `BroadcastReceiver` lifecycle, `WorkManager`, and fine-grained notification control. Flutter/React Native cannot reliably use `setExactAndAllowWhileIdle` or respond to `BOOT_COMPLETED`.

### Android API components

| Component | Role |
| --- | --- |
| `AlarmManager.setExactAndAllowWhileIdle` | Schedules the next check; fires even in Doze |
| `BroadcastReceiver` | Entry point when alarm fires; enqueues the WorkManager job |
| `WorkManager` (one-shot) | Runs the fetch/compare job; survives screen-off |
| `BOOT_COMPLETED` receiver | Re-registers alarm after reboot |
| `NotificationManager` | Delivers change notification |
| `EncryptedSharedPreferences` | Stores auth tokens securely |

### Required permissions

- `SCHEDULE_EXACT_ALARM` (Android 12+) — must request at runtime
- `USE_EXACT_ALARM` (Android 13+) — for precise alarm scheduling
- `RECEIVE_BOOT_COMPLETED`
- `INTERNET`
- `POST_NOTIFICATIONS` (Android 13+)

### Timetable data format

Response from `POST /apigateway/api/timetable` is an array of slot objects:

```json
[
  {
    "SlotDate": "2026-Oct-17",
    "SlotStartTime": "09:00 AM",
    "SlotEndTime": "09:55 AM",
    "ModuleList": [{"ModuleName": "...", "ModuleCode": "..."}],
    "TeacherList": [{"Name": "..."}],
    "FloorPlanDetails": {
      "VenueName": "10206",
      "VenueCategory": "Class Room",
      "MeetingLink": "MS Teams URL"
    },
    "DayofWeekDetails": {"DayOfWeekName": "Saturday"},
    "IsCancelledSlot": false,
    "IsRescheduledSlot": false,
    "IsExtraSlot": false
  }
]
```

### Comparison algorithm

- Key each slot by `SlotDate + SlotStartTime + ModuleCode`
- Set difference for added/removed
- Field-by-field comparison on matching keys for modifications
- Flag changes to: time, venue, instructor, `IsCancelledSlot`, `IsRescheduledSlot`

## Authentication strategy

**Base URL:** `https://myupes-beta.upes.ac.in`\
**Static app secret:** `ku7GUMtyT8er51rTfTc7HC`\
**Token TTL:** 45 minutes (`ExpiresIn: 45`); path in login response: `Item.Identity.AccessToken`\
**reCAPTCHA bypass:** send `"Captcha": "-"` in the login body — no solving needed

---

### Step 1 — Login

`POST /sso/api/account/oauth2/token`

```
Content-Type: application/json
x-applicationname: oneportal
x-requestfrom: mobile
```

```json
{
  "UserId": "Nikhil.12509@stu.upes.ac.in",
  "Password": "...",
  "Captcha": "-",
  "MobileAppPlatform": "android"
}
```

Extract the Bearer token from `Item.Identity.AccessToken` in the response. Cache it with its expiry timestamp.

---

### Step 2 — Get StudentId

`GET /apigateway/connect-portal/api/studentloginbasicinfo`

```
Content-Type: application/json
x-applicationname: connectportal
x-appsecret: ku7GUMtyT8er51rTfTc7HC
x-requestfrom: web
Authorization: Bearer {token}
```

Extract `Items[0].StudentId` — a GUID used as `x-studentUniqueId` on all subsequent calls.

Key response fields:

| Field | Example | Notes |
| --- | --- | --- |
| `StudentId` | `a573eb8c-58d3-4f6a-8075-5cc602e7b412` | GUID — use as `x-studentUniqueId` |
| `GlobalId` | `590012509` | Numeric enrollment ID |
| `TermCode` | `SEM_5` | Current semester |
| `CourseFamilyCode` | `BTECH_CS_CSE` | Programme |

---

### Step 3 — Fetch timetable

`POST /apigateway/api/timetable`

```
Content-Type: application/json
x-applicationname: connectportal
x-appsecret: ku7GUMtyT8er51rTfTc7HC
x-requestfrom: web
Authorization: Bearer {token}
x-studentUniqueId: {StudentId}
```

```json
{
  "ActivityCode": "studentdashboard",
  "TimeTableContextDetails": {
    "SlotStartDate": "17-Oct-2026",
    "SlotEndDate": "17-Oct-2026",
    "StudentCode": "a573eb8c-58d3-4f6a-8075-5cc602e7b412"
  }
}
```

Date format: `DD-Mon-YYYY` (e.g. `17-Oct-2026`). Use the same date for start and end to fetch a single day. `StudentCode` is the same GUID as `x-studentUniqueId`.

> **Auth note:** Both `x-appsecret` and `Authorization: Bearer {token}` are required on every connectportal call. `x-appsecret` identifies the app; `Authorization` identifies the user. Omitting `Authorization` causes the server to return HTTP 500.

---

### Header reference

| Header | Login (`/sso`) | connectportal calls |
| --- | --- | --- |
| `x-applicationname` | `oneportal` | `connectportal` |
| `x-requestfrom` | `mobile` | `web` |
| `x-appsecret` | — | `ku7GUMtyT8er51rTfTc7HC` |
| `Authorization` | — | `Bearer {token}` |
| `x-studentUniqueId` | — | `{StudentId GUID}` (timetable call only) |

---

### Token caching strategy

Store credentials in `EncryptedSharedPreferences`. On each scheduled check:

1. Read cached token and its expiry timestamp
2. If token has > 5 minutes remaining — reuse it (skip Steps 1–2)
3. If expired or < 5 min remaining — re-run Steps 1–2 silently (no user interaction; `Captcha: "-"` makes this fully automatic)
4. `StudentId` can be cached indefinitely — it doesn't change between sessions

**Fallback:** If the auth endpoint changes and the captcha bypass stops working, fire a notification prompting the user to open the app and complete a WebView login.

**Endpoint:** `https://myupes-beta.upes.ac.in`\
**Static app secret:** `ku7GUMtyT8er51rTfTc7HC`\
**reCAPTCHA bypass:** `Captcha: "-"` header on the login call

### Auth flow (3 steps)

1. `POST /sso/api/account/oauth2/token` — login with credentials + `Captcha: "-"` header; returns Bearer token (TTL: \~45 min) at `Item.Identity.AccessToken`
2. `GET /apigateway/connect-portal/api/studentloginbasicinfo` — retrieve `StudentId` GUID using the token
3. `POST /apigateway/api/timetable` — fetch slots; requires both `Authorization: Bearer {token}` and `x-studentUniqueId: {StudentId}`

**Key headers for all calls:**

- `x-applicationname`: `oneportal` (login) / `connectportal` (timetable)
- `x-appsecret: ku7GUMtyT8er51rTfTc7HC`
- `Authorization: Bearer {token}` — required; omitting it returns HTTP 500

### Recommended approach: token persistence + re-auth prompt

**Primary:** Store credentials securely in `EncryptedSharedPreferences`. Re-authenticate automatically when the token expires (detected by 401 or TTL check). Since `Captcha: "-"` bypasses reCAPTCHA, re-auth is fully silent — no user interaction needed for routine checks.

**Fallback:** If the auth API changes and the bypass stops working, fire a notification prompting the user to open the app and log in via a WebView.

This is fully automated from the user's perspective after the initial login setup.

## WhatsApp integration

Four options in order of preference:

1. **WhatsApp deep-link intent** — `Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=..."))` opens WhatsApp with a pre-filled message to self; user taps Send once. No API, no approval, works on any device.
2. **Tasker / AutoResponder bridge** — if the user has Tasker, the app can fire a named intent that Tasker delivers silently. Adds a dependency but removes the manual tap.
3. **WhatsApp Business API** — requires Meta approval and is paid. Not appropriate for personal use.
4. **Third-party services (e.g. CallMeBot)** — free WhatsApp messaging API via a registered number. Requires one-time setup; adds an external dependency.

**Recommended for MVP:** Option 1 (deep-link intent). Zero dependencies, works immediately, no ToS risk. The single manual tap per notification is acceptable. Option 4 can be added in V1.1 for fully silent delivery.

## Battery optimization

Battery impact is minimal by design because the app does not run between checks:

- `setExactAndAllowWhileIdle` wakes the CPU for only the duration of the fetch (\~3–5 seconds)
- No wake locks held outside the `WorkManager` job window
- Token caching avoids redundant auth calls (1 network call per check instead of 3 when token is valid)
- Abort early if network is unavailable — no wasted wake
- Exponential backoff on failures — avoids hammering the network after an error
- User-configurable quiet schedule (e.g. disable on weekends)
- **Estimated impact:** \~1–3 mAh per check; at 6 checks/day this is well under 1% of a typical 4500 mAh battery

## Risks and challenges

| Risk | Impact | Mitigation |
| --- | --- | --- |
| `SCHEDULE_EXACT_ALARM` permission denied (Android 12+) | Critical | Guide user to grant it on first launch; show clear rationale screen |
| `Captcha: "-"` bypass stops working | Critical | Detect 403/captcha response; notify user to re-authenticate via WebView fallback |
| Portal API changes (endpoints, response schema) | High | Modular API layer; version-check the response; notify user on parse failure |
| Token expiry mid-check | Medium | Check TTL before fetch; re-auth silently if token has < 5 min remaining |
| Nothing OS aggressive battery killing | Medium | Document the "Unrestricted battery" setting in onboarding; `AlarmManager` alarms survive this anyway |
| WhatsApp not installed | Low | Check for WhatsApp before firing intent; fall back to system notification only |
| Network unavailable at alarm time | Low | Abort and reschedule a retry 15 minutes later |

## Development phases

### Phase 1 — Project setup and scheduling skeleton (Week 1)

- Create Android (Kotlin) project with Gradle
- Implement `AlarmManager` exact-alarm registration
- Implement `BOOT_COMPLETED` receiver
- Implement `BroadcastReceiver` → `WorkManager` job handoff
- Verify alarms fire correctly with a stub job (log only)

### Phase 2 — API integration and storage (Week 2)

- Implement the 3-step myUPES auth flow
- Token caching in `EncryptedSharedPreferences` with TTL tracking
- Timetable fetch and JSON snapshot storage
- Unit tests for API client with mock responses

### Phase 3 — Comparison engine (Week 3)

- Slot keying and set-difference logic
- Field-level diff for time, venue, instructor, cancellation flags
- Human-readable change report generation
- Unit tests with static snapshot pairs

### Phase 4 — Notifications (Week 4)

- Android `NotificationManager` integration
- WhatsApp deep-link intent
- Silent exit on no-change runs
- End-to-end test with real API data

### Phase 5 — Settings UI and onboarding (Week 5)

- Login screen (credential form → auth → store token)
- Schedule configuration screen
- Manual "check now" button
- `SCHEDULE_EXACT_ALARM` permission rationale and request flow
- "Unrestricted battery" onboarding prompt

### Phase 6 — Polish and release (Week 6)

- Error handling and edge cases (network failure, parse errors, missing snapshot)
- Retry logic with exponential backoff
- Testing on Android 12, 13, 14 (Nothing OS)
- APK build and direct distribution (no Play Store required)

---

## Success metrics

- **Reliability:** 95%+ detection rate for timetable changes
- **Timeliness:** Notification within one check interval of portal update
- **Battery impact:** < 1% daily drain at 6 checks/day
- **Background footprint:** Zero processes, services, or wake locks between checks

---

## Open questions

1. **Token TTL:** Exact session duration of the myUPES Bearer token needs live testing
2. **WhatsApp silent delivery:** Is CallMeBot or a similar service worth adding for V1.1?
3. **Quiet hours:** Should checks be skipped entirely at night, or just notifications suppressed?
4. **Multi-user distribution:** If shared with classmates, does the `x-studentUniqueId` need to be fetched per user or is it static per account?
5. **Date range for timetable fetch:** How many days ahead should the fetch cover to catch future additions early?

---

## Next steps

1. Create Android project; scaffold `AlarmManager` + `BroadcastReceiver` + `WorkManager` skeleton
2. Test myUPES auth flow manually (curl/Postman) to confirm token TTL
3. Build and test the comparison engine against static JSON snapshots
4. Wire up notifications and WhatsApp intent
5. Build settings/onboarding UI
6. Sideload APK on Nothing Phone 1 for end-to-end testing

---

**Document version:** 2.0\
**Last updated:** 2026-10-19\
**Status:** Planning — native Android app approach confirmed
