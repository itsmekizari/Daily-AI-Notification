Daily AI Notification v2.3 - compile fix

Changed file only:
android/app/src/main/java/com/kizari/dailynotify/BootReceiver.java

Fix:
Replaced the missing method call `DailyMessageReceiver.ensurePrefetched(...)`
with the method that actually exists in DailyMessageReceiver:
`DailyMessageReceiver.ensureNextMessage(...)`

Put this file at the exact path above and replace the existing BootReceiver.java.
No other project files need to be changed for this specific compile error.
