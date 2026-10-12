# Auto X Downloader — Native Android App

A lightweight, fast, privacy-focused native Android application for downloading media from X (formerly Twitter) using Kotlin, Jetpack Compose, Material 3, Room, DataStore, and Android's native platform APIs.

---

## 1. Product Overview & Key Features

- **Android Sharesheet Receiver (`ACTION_SEND`)**:
  - Share any X post directly from the official X app or web browsers to **Auto X Downloader**.
  - Parses status URLs (`x.com`, `twitter.com`, `mobile.twitter.com`), strips tracking parameters, and immediately initiates download when auto-download is enabled.
- **Foreground Clipboard Detection**:
  - Automatically detects eligible X status URLs on the clipboard when the app enters the foreground or is resumed.
  - Transparently complies with Android 10+ privacy policies (never monitors the clipboard in the background; Share-to-Downloader serves as the seamless zero-app-switch alternative).
- **Smart Quality Engine**:
  - **Images**: Automatically downloads original uncompressed photos (`name=orig`) or the highest available resolution.
  - **Videos & 140-Second Policy**: Videos longer than 140 seconds (2m 20s) default to 720p to preserve device storage and mobile bandwidth.
  - **User Resolution Overrides**: Choose between Maximum Available, 1080p (Full HD), 720p (HD), or 480p (SD), with honest fallback reporting.
- **Background-Capable Streaming & MediaStore Integration**:
  - Streams media in 32KB chunks via OkHttp into temporary files (`.tmp`), validates byte counts and MIME types, and commits them atomically to Android's `MediaStore` (`Movies/AutoXDownloader` and `Pictures/AutoXDownloader`).
  - Downloaded media appears immediately in Google Photos and device Gallery apps without requiring broad storage permissions.
  - Automatically handles Wi-Fi constraints, duplicate checks, cancellation, and partial-file cleanup.
- **Verified Completion Notifications**:
  - Displays `"X Media Downloaded ✓"` only after the file is completely written and verified on disk, with a direct tap action to open the file in the device media player.
- **Local Persistence & Settings**:
  - **Room Database**: Stores download history with thumbnail, status, resolution, byte size, author, and actions (Open, Share, Retry, Delete).
  - **Jetpack DataStore**: Stores local user preferences for auto-download, video quality, long-video policy, Wi-Fi-only, duplicate prevention, and optional custom syndication resolver endpoints.

---

## 2. Privacy & External Resolver Disclosure

To resolve public tweet media without requiring personal login tokens, web scraping, or invasive services:
1. **Network Media Resolution**: When a user shares or submits an X post link, the post identifier is queried against a syndication resolver gateway:
   - **Default Gateway**: `https://api.vxtwitter.com/Twitter/status/{statusId}`
   - **Custom Gateway**: User-configurable in **Settings > Media Resolver Service** for self-hosted syndication proxies.
2. **Data Scope**: **Only the public post URL / ID is transmitted to the resolver.** No personal credentials, account tokens, search histories, or device identifiers are ever collected or sent.
3. **No Background Clipboard Scraping**: The app reads clipboard content only when foregrounded and eligible under Android OS rules.
4. **Access Control & Privacy Respect**: The app does not bypass access controls, private-account restrictions, or login requirements. Private posts honestly report `"This post is private or requires authentication to view"` without faking success.

---

## 3. How to Build & Generate Debug APK

### Prerequisites
- JDK 17 or higher
- Android SDK with platform API 36 / 34 installed

### Generate Debug APK
To build the debug APK using the included Gradle Wrapper:

On Linux / macOS:
```bash
./gradlew assembleDebug
```

On Windows:
```cmd
gradlew.bat assembleDebug
```

Or using system Gradle:
```bash
gradle assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit and Robolectric Tests
To run the automated test suite:
```bash
./gradlew :app:testDebugUnitTest
```
or
```bash
gradle :app:testDebugUnitTest
```
