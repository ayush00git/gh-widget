# gh-widget

**gh-widget** is a custom Android home screen widget designed to bring your GitHub contribution graph directly to your mobile device but the cool way.
More tweaks comming soon!!

<div align="center">
  <img src="public/img/image.png" alt="GitHub Widget Preview" width="400">
</div>

## Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/ayush00git/gh-widget.git
   cd gh-widget
   ```

2. **Create `local.properties`** in the root directory and add your GitHub credentials:
   ```properties
   github_token=ghp_xxxxxxxxxxxx
   github_username=your_username
   ```

3. **Install via USB (Recommended)**
   - Connect your Android device via USB with USB debugging enabled
   ```bash
   ./gradlew :app:installDebug
   ```

4. **Or build and install manually**
   - Build: `./gradlew assembleDebug`
   - APK location: `app/build/outputs/apk/debug/app-debug.apk`
   - Transfer to device and install manually
