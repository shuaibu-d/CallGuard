# CallGuard

Privacy-first Android call filtering app built with Kotlin and Jetpack Compose.

## MVP

- Prefix blocking, e.g. `020`
- Exact-number blocking
- Contains matching
- Enable/disable rules
- Local Room database
- Local blocked-call history
- Android `CallScreeningService`
- No backend required
- No account required

## Build with GitHub Actions

1. Upload this project to a GitHub repository.
2. Open **Actions**.
3. Select **Build APK**.
4. Run the workflow.
5. Download the **CallGuard-APK** artifact.

## Important Android behavior

Android requires the user to explicitly make CallGuard the call-screening app. Open the app, go to **Settings**, and tap **Enable call screening**.

For a first test, create a rule such as:

`020`

with match type:

`Starts with`

The app stores the rule locally.

## Notes

This is an MVP foundation. Device/OEM behavior around call screening can differ, so test on the actual target phone before release.
