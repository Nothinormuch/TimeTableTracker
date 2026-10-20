# UPES myUPES API Reference

**Base URL:** `https://myupes-beta.upes.ac.in`  
**Static app secret (portal):** `ku7GUMtyT8er51rTfTc7HC`  
**Timetable date format:** `DD-Mon-YYYY` (e.g. `17-Oct-2026`)

---

## Auth Flow (3 steps)
1. POST login → establishes session
2. GET studentloginbasicinfo → get `StudentId` GUID
3. POST timetable with `StudentId`

---

## 1. Login
`POST /sso/api/account/oauth2/token`

**Headers:**
```
Content-Type: application/json
x-applicationname: oneportal
x-requestfrom: mobile
```

**Body:**
```json
{
  "UserId": "Nikhil.12509@stu.upes.ac.in",
  "Password": "...",
  "Captcha": "-",
  "MobileAppPlatform": "android"
}
```
> `Captcha: "-"` bypasses ReCaptcha. Call this first to establish session cookies.

---

## 2. Get Student ID
`GET /apigateway/connect-portal/api/studentloginbasicinfo`

**Headers:**
```
x-applicationname: connectportal
x-appsecret: ku7GUMtyT8er51rTfTc7HC
x-requestfrom: web
```

**Key response fields** (`Items[0]`):
| Field | Example | Notes |
|---|---|---|
| `StudentId` | `a573eb8c-58d3-4f6a-8075-5cc602e7b412` | GUID — use as `x-studentUniqueId` |
| `GlobalId` | `590012509` | Numeric enrollment ID |
| `EmailId` | `Nikhil.12509@stu.upes.ac.in` | |
| `TermCode` | `SEM_5` | Current semester |
| `CourseFamilyCode` | `BTECH_CS_CSE` | Programme |
| `AcademicYearCode` | `UPES_AY26` | |
| `CenterCode` | `UPES_BID` | Campus |

---

## 3. Timetable Masters (venue categories)
`POST /apigateway/api/timetable/masters`

**Headers:** same as §2 + `x-studentUniqueId: {StudentId}`  
**Body:** `{"DropdownType":"venuecategory"}`

**Response:**
```json
[
  {"Id":1,"Code":"VT001","Name":"Class Room","ColorCode":"#006097"},
  {"Id":3,"Code":"HCR","Name":"Hybrid Class Room","ColorCode":"#F4C430"},
  {"Id":2,"Code":"OCR","Name":"Virtual Class Room","ColorCode":"#e46097"}
]
```

---

## 4. Fetch Timetable
`POST /apigateway/api/timetable`

**Headers:** same as §2 + `x-studentUniqueId: {StudentId}`

**Body:**
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
> Same date for start/end = single day. `StudentCode` = same GUID as `x-studentUniqueId`.

## Actual Response Structure (array of slot objects)
```json
[
  {
    "SlotDate": "2026-Oct-17",
    "SlotStartTime": "09:00 AM",
    "SlotEndTime": "09:55 AM",
    "ModuleList": [{"ModuleName":"Data Structures","ModuleCode":"CS101"}],
    "TeacherList": [{"Name":"Dr. Smith"}],
    "FloorPlanDetails": {"VenueName":"10206","VenueCategory":"Class Room","MeetingLink":"https://teams.microsoft.com/..."},
    "DayofWeekDetails": {"DayOfWeekName":"Saturday"},
    "IsCancelledSlot": false,
    "IsRescheduledSlot": false,
    "IsExtraSlot": false
  }
]
```
| Field | Notes |
|---|---|
| `SlotDate` | `2026-Oct-17` |
| `SlotStartTime` / `SlotEndTime` | `09:00 AM` / `09:55 AM` |
| `ModuleList[].ModuleName` | Subject name |
| `ModuleList[].ModuleCode` | Subject code |
| `TeacherList[].Name` | Faculty name |
| `FloorPlanDetails.VenueName` | Room e.g. `10206` |
| `FloorPlanDetails.VenueCategory` | Class Room / Hybrid / Virtual |
| `FloorPlanDetails.MeetingLink` | MS Teams URL for virtual |
| `DayofWeekDetails.DayOfWeekName` | `Saturday` |
| `IsCancelledSlot` | bool — skip if true |
| `IsRescheduledSlot` / `IsExtraSlot` | bool |

---

## Common Headers

| Header | connectportal API | SSO (login) |
|---|---|---|
| `x-applicationname` | `connectportal` | `oneportal` |
| `x-requestfrom` | `web` | `mobile` |
| `x-appsecret` | `ku7GUMtyT8er51rTfTc7HC` | — |
| `x-studentUniqueId` | `{StudentId GUID}` | — |

---

## Minimal Python

```python
import requests
from datetime import datetime

BASE = "https://myupes-beta.upes.ac.in"
SECRET = "ku7GUMtyT8er51rTfTc7HC"

# 1. Login → get Bearer token from Item.Identity.AccessToken
token = requests.post(f"{BASE}/sso/api/account/oauth2/token",
    headers={"Content-Type":"application/json","x-applicationname":"oneportal","x-requestfrom":"mobile"},
    json={"UserId":"EMAIL","Password":"PASS","Captcha":"-","MobileAppPlatform":"android"}
).json()["Item"]["Identity"]["AccessToken"]

# Both x-appsecret (app identity) AND Authorization (user identity) are required
HEADERS = {"Content-Type":"application/json","x-applicationname":"connectportal",
           "x-appsecret":SECRET,"x-requestfrom":"web","Authorization":f"Bearer {token}"}

# 2. Get StudentId
sid = requests.get(f"{BASE}/apigateway/connect-portal/api/studentloginbasicinfo", headers=HEADERS).json()["Items"][0]["StudentId"]

# 3. Fetch timetable
date = datetime.today().strftime("%d-%b-%Y")  # e.g. 17-Oct-2026
slots = requests.post(f"{BASE}/apigateway/api/timetable",
    headers={**HEADERS,"x-studentUniqueId":sid},
    json={"ActivityCode":"studentdashboard","TimeTableContextDetails":{"SlotStartDate":date,"SlotEndDate":date,"StudentCode":sid}}).json()
```

> **Auth note:** `x-appsecret` alone is not enough — it identifies the app but not the user. Without `Authorization: Bearer {token}` the server returns 500 ("Object reference not set"). Token path in login response: `Item.Identity.AccessToken`. Token TTL is 45 minutes (`ExpiresIn: 45`).
