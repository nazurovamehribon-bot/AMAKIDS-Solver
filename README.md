# AMAKIDS Solver (Android prototype)

Android 8+ app: screen capture (MediaProjection), Google ML Kit OCR, floating running total.

## Build using iPhone (GitHub Actions)
1. Create a new GitHub repository.
2. Upload all project files, preserving folders and hidden `.github/workflows/android.yml`.
3. Open Actions > Build Android APK > Run workflow (or push to main).
4. Download the `AMAKIDS-Solver-debug-apk` artifact and extract `app-debug.apk`.
5. Install the APK on **Android only**, grant "Display over other apps" and screen-recording permissions, then start monitoring.

## Important limitations
- **Not an iOS app**. iPhones cannot install APK files.
- This is an **untested source prototype**. An APK has not been built or verified here.
- OCR is asynchronous; 100 ms capture sampling does **not** guarantee recognition of every 100 ms flash.
- A repeated identical number without a blank transition may be counted only once.
- The screenshot crop uses 12–88% of screen width and 20–55% of screen height. Change these fractions if your AMAKIDS screen layout differs.
- No automatic detection of new question boundaries; press RESET between exercises.
- It ignores an unsigned initial `1` because only signed numbers are accepted.
- The overlay itself may appear in captured frames, but OCR crops to the central exercise card.
