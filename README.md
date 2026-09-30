# DailyAI Notification

A Sketchware-friendly Android notification app with a separate PHP backend.

## Features
- Daily notification time
- English category names
- Burmese + English notification text
- Local fallback messages
- Duplicate prevention
- Android 13 notification permission handling
- Exact alarm scheduling
- Re-schedule after reboot
- Backend AI generation
- API key stays on the server

## Repository layout

- `android/` - Android/Sketchware source reference files
- `backend/` - PHP + SQLite backend
- `docs/` - setup notes

> Do not commit API keys. Configure `OPENAI_API_KEY` as a server environment variable.
