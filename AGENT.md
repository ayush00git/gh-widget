# GitHub Contribution Widget — Android App

## Project Overview

A sideloadable Android APK that renders a GitHub contribution graph as a home screen widget. The widget displays a dot grid (53 weeks × 7 days) where each dot represents a day's contribution count, fetched live from the GitHub API. No Play Store required.

---

## Goals

- Display GitHub contribution data as a white dot grid on the Android home screen
- Fetch data from the GitHub GraphQL API in the background
- Sideloadable APK (no Play Store publishing needed)
- No Activity — widget only
- Periodic auto-refresh

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| Widget UI | Jetpack Glance (Compose-based) |
| HTTP / API | Ktor Client |
| Background fetch | WorkManager |
| Build system | Android Studio + Gradle (Kotlin DSL) |

**Why Jetpack Glance:** Standard `RemoteViews` widgets do not support custom drawing easily. Glance allows writing widget UI in Compose syntax, making a dot grid straightforward using `Box` composables with `CircleShape`.

---

## Project Structure

```
app/
├── src/main/
│   ├── kotlin/com/example/githubwidget/
│   │   ├── GithubWidget.kt        # Glance widget class + dot grid UI
│   │   └── WidgetWorker.kt        # WorkManager job — GitHub API fetch
│   ├── res/
│   │   └── xml/
│   │       └── widget_info.xml    # Widget size + update interval declaration
│   └── AndroidManifest.xml        # Widget registration (no Activity)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## File Responsibilities

### `GithubWidget.kt`
- Extends `GlanceAppWidget`
- Reads contribution data from `SharedPreferences` (written by the worker)
- Renders a 53-column × 7-row dot grid
- Each dot: 8dp circle, white if `count > 0`, semi-transparent white if `count == 0`
- Background: dark color (e.g. `#1a1a2e`)

### `WidgetWorker.kt`
- Extends `CoroutineWorker`
- Hits GitHub GraphQL API: `https://api.github.com/graphql`
- Query: `contributionsCollection` → `contributionCalendar` → `weeks` → `contributionDays`
- Requires a GitHub Personal Access Token (PAT) with `read:user` scope
- Parses response, flattens into a `List<Int>` (contribution counts, oldest-first)
- Saves to `SharedPreferences` as JSON
- Triggers widget update via `GlanceAppWidgetManager`
- Scheduled with `PeriodicWorkRequest` (interval: 1–6 hours, your choice)

### `widget_info.xml`
- Declares minimum widget size (e.g. 4×2 cells)
- Sets `updatePeriodMillis` (note: Android enforces a minimum of 30 minutes; WorkManager handles actual refresh)
- Points to a preview layout if desired

### `AndroidManifest.xml`
- Registers `GithubWidget` as a `BroadcastReceiver` with `APPWIDGET_UPDATE` intent
- Registers `WidgetWorker` is scheduled at app init via a one-time bootstrap (no Activity needed — use `androidx.startup` or a `BroadcastReceiver` for first launch)
- No `<activity>` tag required

---

## GitHub API Details

**Endpoint:** `POST https://api.github.com/graphql`

**Headers:**
```
Authorization: bearer YOUR_GITHUB_PAT
Content-Type: application/json
```

**GraphQL Query:**
```graphql
query {
  user(login: "YOUR_USERNAME") {
    contributionsCollection {
      contributionCalendar {
        weeks {
          contributionDays {
            contributionCount
            date
          }
        }
      }
    }
  }
}
```

**Auth:** Store the PAT in `local.properties` (excluded from version control) and read via `BuildConfig`.

---

## Dot Grid UI Logic

- Total dots: 371 (53 weeks × 7 days)
- Layout: horizontal weeks, vertical days (Mon–Sun top to bottom)
- Active dot color: `Color.White`
- Inactive dot color: `Color(0x33FFFFFF)` (20% white)
- Dot size: `8.dp`
- Dot shape: `cornerRadius(50)` (full circle)
- Gap between dots: `2.dp`

---

## Dependencies (build.gradle.kts)

```kotlin
implementation("androidx.glance:glance-appwidget:1.0.0")
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("io.ktor:ktor-client-android:2.3.0")
implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
```

---

## Build & Sideload Instructions

1. Clone the repo and open in Android Studio (Hedgehog or later)
2. Add your GitHub PAT and username to `local.properties`:
   ```
   github_token=ghp_xxxxxxxxxxxx
   github_username=your_username
   ```
3. Build: `./gradlew assembleDebug`
4. Output APK: `app/build/outputs/apk/debug/app-debug.apk`
5. Transfer APK to device and install (enable "Install from unknown sources" in Settings)
6. Long-press home screen → Widgets → find "GitHub Widget" → place it

---

## Notes & Constraints

- WorkManager periodic tasks have a minimum interval of 15 minutes (Android enforces this)
- Glance does not support all Compose modifiers — stick to `GlanceModifier` APIs only
- `LazyVerticalGrid` is not available in Glance; use nested `Row`/`Column` or a custom grid built with `Row` inside a `Column`
- Widget UI updates must go through `GlanceAppWidgetManager.updateIf<>()` — direct recomposition is not triggered automatically
- The PAT should never be hardcoded in source files; use `BuildConfig` fields injected from `local.properties`