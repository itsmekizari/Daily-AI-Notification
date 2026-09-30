# Setup

## 1. GitHub
Create a repository and upload this folder.

Do not upload:
- real API keys
- `.env` containing a real key
- database files containing private data

## 2. Backend
Upload `backend/` to PHP hosting with:
- PHP
- PDO SQLite
- cURL
- mbstring

Set the server environment variable:

`OPENAI_API_KEY`

Optional:

`OPENAI_MODEL=gpt-5.6-luna`

The backend uses the OpenAI Responses API.

## 3. Android
Set `ApiClient.API_URL` to your deployed `api.php` endpoint.

For Sketchware Pro, the Android source files are reference implementations. Add them through the project's source-code/custom-class facilities rather than placing the API key in the APK.

## 4. Test
1. Install the app.
2. Allow notifications.
3. Enable the daily switch.
4. Pick a category and time.
5. Press Test Notification.
6. Save & Schedule.
7. Check the backend request.
