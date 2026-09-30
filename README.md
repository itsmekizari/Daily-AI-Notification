# Daily AI Notification

Daily AI-generated Burmese + English notifications for Android.

## Android build on GitHub

The repository is configured so GitHub Actions builds a debug APK without Android Studio.

### Workflow

1. Push the repository to GitHub.
2. Open **Actions**.
3. Select **Build Android APK**.
4. Click **Run workflow** (or push to `main`).
5. Open the completed run.
6. Download the artifact named `Daily-AI-Notification-debug-apk`.

The workflow uses Java 17, Gradle 9.6, and Android Gradle Plugin 9.4.0.

## Backend

The FastAPI backend lives in `backend/`.

Required Render environment variables:

- `OPENAI_API_KEY`
- `OPENAI_MODEL`

Keep the real API key only in the server environment. Never commit it to GitHub.

## Package

`com.kizari.dailynotify`
