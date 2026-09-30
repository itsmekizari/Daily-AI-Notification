FINAL local 1,000-message version

Exact GitHub paths:
android/app/src/main/java/com/kizari/dailynotify/MainActivity.java
android/app/src/main/java/com/kizari/dailynotify/DailyMessageReceiver.java
android/app/src/main/java/com/kizari/dailynotify/DailyMessageWorker.java
android/app/src/main/java/com/kizari/dailynotify/ApiClient.java
android/app/src/main/java/com/kizari/dailynotify/LocalMessageBank.java
backend/main.py
android/app/src/main/AndroidManifest.xml
android/app/src/main/res/drawable/ic_launcher.xml
android/app/src/main/res/drawable/ic_notification.xml

Behavior:
- Main screen only: Language, Daily time, Enable daily notification, Save & Schedule.
- Removed AI Style, top title/subtitle, status and extra bottom controls.
- Bundled exactly 1,000 pre-generated playful love-topic teasing messages.
- Messages are selected from a shuffled no-repeat bag, one per scheduled day.
- Immediately after notification, the next message is selected and saved for the next day.
- OpenAI generator code remains but is disabled and never called by the app.
