# Job Notification Data Flow Guide

## 📱 How Your Payload Data Flows Through the App

### 1. **OneSignal API Payload Structure**

```json
{
  "included_segments": ["All"],
  "headings": { "en": "Ламбда ХХК аас" },
  "contents": { "en": "Мобайл аппликейшн хөгжүүлэгчийн ажил зарлалаа" },
  "content_available": true,
  "mutable_content": true,
  "data": {
    "type": "job",
    "company": "Ламбда ХХК",
    "jobTitle": "Мобайл аппликейшн хөгжүүлэгч",
    "description": "iOS болон Android...",
    "companyImageUrl": "https://content.ikon.mn/news/2014/3/7/a899ce_fdfffff_x974.jpg",
    "action": "update",
    "ride_id": 123,
    "eta": "5 min",
    "driver": "Ariun"
  }
}
```

### 2. **Data Flow Path**

#### **Foreground (App is Open)**

```
OneSignal Notification
    ↓
main.dart: OneSignal.Notifications.addForegroundWillDisplayListener
    ↓
Extract: data['company'], data['jobTitle'], data['description'], data['companyImageUrl']
    ↓
_ensureJobActivity(company, jobTitle, description, companyImageUrl)
    ↓
LiveActivities.createActivity() / updateActivity()
    ↓
Android: CustomLiveActivityManager.buildNotification()
    ↓
Extract: company, jobTitle, description, companyImageUrl from data map
    ↓
updateRemoteViewsForJob()
    ↓
Display in live_activity.xml layout:
  - team1_image_placeholder ← companyImageUrl (56dp logo)
  - team1_name ← company (Ламбда ХХК)
  - team2_name ← jobTitle (Мобайл аппликейшн хөгжүүлэгч)
  - score ← description (iOS болон Android...)
  - match_time ← chronometer (posted time)
```

#### **Background (App is Closed/Minimized)**

```
OneSignal Notification
    ↓
Firebase Cloud Messaging (FCM)
    ↓
MyFirebaseMessagingService.onMessageReceived()
    ↓
Parse OneSignal "custom" JSON (additionalData)
    ↓
Extract: type, company, jobTitle, description, companyImageUrl
    ↓
if type == "job":
    JobNotificationHelper.show()
    ↓
    Display in notification_small.xml / notification_big.xml:
      - job_icon / job_big_icon ← companyImageUrl (async loaded)
      - job_company ← company
      - job_title ← jobTitle
      - job_description ← description
      - Buttons: ҮЗЭХ (View) | CV Илгээх (Apply)
```

### 3. **Key Files and Their Roles**

#### **Flutter Side**

- **lib/main.dart**

  - `OneSignal.Notifications.addForegroundWillDisplayListener`: Intercepts foreground notifications
  - `_ensureJobActivity()`: Creates/updates Live Activity with job data
  - Extracts: `company`, `jobTitle`, `description`, `companyImageUrl`

- **lib/screens/send_notification_screen.dart**
  - Sends test notifications to OneSignal API
  - Constructs payload with `companyImageUrl` in data field
  - Company, job title, description, and image URL inputs

#### **Android Native Side**

##### **MyFirebaseMessagingService.kt**

- Receives FCM messages in background
- Parses OneSignal `custom.a` JSON structure
- Extracts job fields with fallback logic:
  ```kotlin
  val company = additionalData.optString("company", company)
  val jobTitle = additionalData.optString("jobTitle", jobTitle)
  val description = additionalData.optString("description", description)
  val companyImageUrl = additionalData.optString("companyImageUrl",
      additionalData.optString("imageUrl", companyImageUrl))
  ```
- **Logging**: Look for `🔔 Showing JOB notification:` with all extracted values

##### **CustomLiveActivityManager.kt**

- Builds Live Activity notification for Android
- Receives data map from `live_activities` plugin
- Extracts fields: `company`, `jobTitle`, `description`, `companyImageUrl`
- Loads company logo from URL (async)
- Updates RemoteViews:
  ```kotlin
  remoteViews.setTextViewText(R.id.team1_name, company)
  remoteViews.setTextViewText(R.id.team2_name, jobTitle)
  remoteViews.setTextViewText(R.id.score, description)
  remoteViews.setImageViewBitmap(R.id.team1_image_placeholder, logo)
  ```
- **Logging**: Look for `=== buildNotification called ===` with full data map

##### **JobNotificationHelper.kt**

- Shows standard Android notification (background)
- Async loads company logo from `companyImageUrl`
- Uses notification_small.xml and notification_big.xml layouts
- Two action buttons: View and Apply

### 4. **Layout Mappings**

#### **live_activity.xml** (Live Activity - Foreground)

| View ID                 | Data Field             | Example Value                |
| ----------------------- | ---------------------- | ---------------------------- |
| team1_image_placeholder | companyImageUrl        | https://content.ikon.mn/...  |
| team1_name              | company                | Ламбда ХХК                   |
| team2_name              | jobTitle               | Мобайл аппликейшн хөгжүүлэгч |
| score                   | description            | iOS болон Android...         |
| match_time              | postedAt (chronometer) | "5 минутын өмнө"             |

#### **notification_big.xml** (Standard Notification - Background)

| View ID         | Data Field      | Example Value                |
| --------------- | --------------- | ---------------------------- |
| job_big_icon    | companyImageUrl | (async loaded bitmap)        |
| job_big_company | company         | Ламбда ХХК                   |
| job_big_title   | jobTitle        | Мобайл аппликейшн хөгжүүлэгч |
| job_description | description     | iOS болон Android...         |
| action_view     | jobUrl          | "ҮЗЭХ" button                |
| action_apply    | jobUrl/apply    | "CV Илгээх" button           |

### 5. **Debugging Steps**

#### Check if data is reaching Android:

```bash
# Filter logs for job notification data
adb logcat | grep -E "CustomLiveActivity|MyFirebaseMsgService|JobNotificationHelper"
```

#### Key log messages to look for:

**MyFirebaseMessagingService:**

```
D/MyFirebaseMsgService: === FCM Message Received ===
D/MyFirebaseMsgService: Notification type: job
D/MyFirebaseMsgService: 📦 Extracted from OneSignal additionalData:
D/MyFirebaseMsgService:   company: Ламбда ХХК
D/MyFirebaseMsgService:   jobTitle: Мобайл аппликейшн хөгжүүлэгч
D/MyFirebaseMsgService:   description: iOS болон Android...
D/MyFirebaseMsgService:   companyImageUrl: https://content.ikon.mn/...
D/MyFirebaseMsgService: 🔔 Showing JOB notification: company=..., companyImageUrl=...
```

**CustomLiveActivityManager:**

```
D/CustomLiveActivity: === buildNotification called ===
D/CustomLiveActivity: Data keys: [company, jobTitle, description, companyImageUrl, ...]
D/CustomLiveActivity: 📋 Extracted values:
D/CustomLiveActivity:   company: Ламбда ХХК
D/CustomLiveActivity:   jobTitle: Мобайл аппликейшн хөгжүүлэгч
D/CustomLiveActivity:   description: iOS болон Android...
D/CustomLiveActivity:   companyImageUrl: https://content.ikon.mn/...
D/CustomLiveActivity: 🎨 updateRemoteViewsForJob called
D/CustomLiveActivity: ✅ Logo loaded successfully: 180x180
```

### 6. **Testing Checklist**

- [ ] **Send Test Notification**

  1. Tap "Send Test Notification" button in app
  2. Check all fields are filled (Company, Job Title, Description, Image URL)
  3. Tap "Send Notification"
  4. Check response shows "Recipients: 1"

- [ ] **Foreground Test** (App Open)

  1. Send notification
  2. Check Live Activity appears at top of screen
  3. Verify: company name, job title, description, logo all visible
  4. Check logcat for `CustomLiveActivity` logs

- [ ] **Background Test** (App Closed)

  1. Close/minimize app
  2. Send notification
  3. Check notification tray
  4. Verify: company logo, name, job title, description
  5. Check buttons: ҮЗЭХ, CV Илгээх
  6. Check logcat for `MyFirebaseMsgService` logs

- [ ] **Click Test**
  1. Tap notification
  2. Check Live Activity appears (foreground mode)
  3. Verify all data still displays correctly

### 7. **Common Issues**

| Issue                                | Cause                               | Solution                                               |
| ------------------------------------ | ----------------------------------- | ------------------------------------------------------ |
| No data shown in Live Activity       | Data not passed to plugin           | Check `_ensureJobActivity()` parameters in main.dart   |
| Logo not loading                     | Invalid URL or network error        | Check logcat for image loading errors                  |
| Background notification missing data | OneSignal custom JSON parsing issue | Check `MyFirebaseMessagingService` logs for extraction |
| Live Activity shows default values   | Data map keys don't match           | Verify `companyImageUrl` not `imageUrl`                |

### 8. **Data Field Backward Compatibility**

Both `companyImageUrl` and `imageUrl` are supported:

```kotlin
// CustomLiveActivityManager.kt
val companyImageUrl = data["companyImageUrl"] as? String ?: data["imageUrl"] as? String

// MyFirebaseMessagingService.kt
companyImageUrl = additionalData.optString("companyImageUrl",
    additionalData.optString("imageUrl", companyImageUrl))
```

This means your old payloads with `imageUrl` will still work! 🎉

---

## Quick Test Command

Run this from another terminal while app is running:

```bash
# Watch live logs
adb logcat -s CustomLiveActivity:D MyFirebaseMsgService:D JobNotificationHelper:D flutter:I
```

Then send a notification and watch the data flow in real-time!
