import requests
from datetime import datetime

BASE = "https://myupes-beta.upes.ac.in"
SECRET = "ku7GUMtyT8er51rTfTc7HC"

s = requests.Session()

# 1. Login
login_resp = s.post(
    f"{BASE}/sso/api/account/oauth2/token",
    headers={"Content-Type":"application/json","x-applicationname":"oneportal","x-requestfrom":"mobile"},
    json={"UserId":"Nikhil.12509@stu.upes.ac.in","Password":"Q!346492626600uj","Captcha":"-","MobileAppPlatform":"android"},
)
token = login_resp.json()["Item"]["Identity"]["AccessToken"]  # ← fixed path

HEADERS = {
    "Content-Type": "application/json",
    "x-applicationname": "connectportal",
    "x-appsecret": SECRET,
    "x-requestfrom": "web",
    "Authorization": f"Bearer {token}",  # ← this was missing
}

# 2. Get StudentId
sid = s.get(f"{BASE}/apigateway/connect-portal/api/studentloginbasicinfo", headers=HEADERS).json()["Items"][0]["StudentId"]
print("StudentId:", sid)

# 3. Fetch timetable
date = datetime.today().strftime("%d-%b-%Y")
slots = s.post(f"{BASE}/apigateway/api/timetable",
    headers={**HEADERS, "x-studentUniqueId": sid},
    json={"ActivityCode":"studentdashboard","TimeTableContextDetails":{"SlotStartDate":date,"SlotEndDate":date,"StudentCode":sid}},
).json()

for slot in slots:
    if slot.get("IsCancelledSlot"): continue
    subj = slot["ModuleList"][0]["ModuleName"] if slot["ModuleList"] else "—"
    room = slot["FloorPlanDetails"]["VenueName"]
    print(f"  {slot['SlotStartTime']} – {slot['SlotEndTime']}  {subj}  [{room}]")
