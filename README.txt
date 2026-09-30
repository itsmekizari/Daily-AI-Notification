Daily AI Notification - FINAL PATCH

Behavior:
1) The app has one fixed AI style: Love Teasing.
2) When daily scheduling is enabled, the app automatically pre-generates the next message.
3) At the exact scheduled time, the cached AI message is shown in one notification.
4) Immediately after the notification is posted, the app starts generating the next message automatically.
5) The next generation happens silently. It never creates a second notification.
6) The Android client limits each AI HTTP generation to about 30 seconds.
7) The backend uses GPT-5.6 Luna through the OpenAI Responses API with strict JSON output.
8) Language choices remain: Burmese + English, Burmese only, English only.
9) Only the Java/Android files and backend files included here should replace the matching files in the existing project.

Replace these paths:
android/app/src/main/java/com/kizari/dailynotify/MainActivity.java
android/app/src/main/java/com/kizari/dailynotify/ApiClient.java
android/app/src/main/java/com/kizari/dailynotify/DailyMessageReceiver.java
android/app/src/main/java/com/kizari/dailynotify/DailyMessageWorker.java
android/app/src/main/java/com/kizari/dailynotify/BootReceiver.java
android/app/src/main/java/com/kizari/dailynotify/NotificationHelper.java
android/app/src/main/java/com/kizari/dailynotify/FallbackMessages.java
android/app/src/main/AndroidManifest.xml
android/app/build.gradle
backend/main.py
backend/requirements.txt
