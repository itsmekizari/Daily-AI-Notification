Daily AI Notification v1.9 - changed files

Behavior:
1) A message is pre-generated before the scheduled notification.
2) At the scheduled time, the cached message is shown in the notification.
3) Immediately after that notification, the app quietly starts generating the NEXT day's AI message.
4) The worker does not post a second notification.
5) The cached message is consumed after notification, so a failed generation does not repeat yesterday's message.
6) Language selection remains Burmese + English / Burmese only / English only.

Replace only the four Java files in the existing Android project with these files.
Keep backend, Gradle files, manifest, ApiClient, NotificationHelper and other existing files unless a later build error says otherwise.
