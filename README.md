# Daily AI Notification

Android app that schedules a daily AI-generated notification from a FastAPI backend.

## v1.2 fixes
- Reliable AlarmManager -> WorkManager flow
- Better handling of exact-alarm permission and clock/timezone changes
- Burmese + English / Burmese only / English only selector
- Generate AI Now button for direct API testing
- Structured OpenAI Responses API output
- 90-second Android API read timeout for Render cold starts
- Fallback notification when AI is temporarily unavailable
- Last AI status shown in the app UI
