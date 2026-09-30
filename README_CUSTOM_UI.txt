Daily AI Notification v2.5 changed files

1) Replace:
android/app/src/main/java/com/kizari/dailynotify/MainActivity.java

2) Replace:
android/app/src/main/java/com/kizari/dailynotify/DailyMessageReceiver.java

3) Replace:
android/app/src/main/java/com/kizari/dailynotify/BootReceiver.java

4) Add:
android/app/src/main/java/com/kizari/dailynotify/CustomMessageStore.java

Hidden custom-message editor:
- Open the app normally.
- Press and hold the "Daily time" label.
- The custom-message editor opens.
- One line = one message.
- Optional bilingual format:
  Burmese text || English text
- The editor itself is not shown on the normal main screen.

Test notification:
- The visible TEST NOTIFICATION button is restored.
- It sends a random test message immediately.
- It does not consume the prepared message for the daily schedule.

OpenAI:
- AI generation remains disabled. The app uses the local 1,000-message bank plus
  the optional messages saved through the hidden editor.
