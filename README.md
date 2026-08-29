<p align="center">
  <img src="docs/images/readme/logo.png" width="132" alt="VoidLauncher app icon">
</p>

<h1 align="center">VoidLauncher</h1>

<p align="center">
  An Android launcher for choosing what deserves space on Home.
</p>

<p align="center">
  <img alt="Android 10+" src="https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&amp;logoColor=white">
  <img alt="Kotlin 2.4.10" src="https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&amp;logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&amp;logoColor=white">
</p>

## See it in use

<table>
  <tr>
    <td width="25%"><img src="docs/images/readme/home.png" alt="VoidLauncher Home"></td>
    <td width="25%"><img src="docs/images/readme/drawer.png" alt="VoidLauncher app drawer"></td>
    <td width="25%"><img src="docs/images/readme/schedule.png" alt="VoidLauncher schedule editor"></td>
    <td width="25%"><img src="docs/images/readme/customize.png" alt="VoidLauncher customization"></td>
  </tr>
  <tr>
    <td align="center"><sub>Home shows the apps chosen for now.</sub></td>
    <td align="center"><sub>Every installed app stays one swipe away.</sub></td>
    <td align="center"><sub>Schedules change what appears and when.</sub></td>
    <td align="center"><sub>Choose the background, shortcuts, and tutorial.</sub></td>
  </tr>
</table>

## The idea

Most launchers treat every installed app as equally urgent. VoidLauncher does not.

Home is a short list that you control. The full drawer stays one swipe away, and schedules can change the Home list by weekday and time. They decide what is prominent, never what remains accessible.

The launcher asks for no internet permission, account, or cloud service.

## What it does

- Keeps a small, reorderable Home list
- Searches installed apps or hands a query to the browser, Play Store, or Maps
- Filters the full drawer and jumps by letter
- Renames, adds, removes, or uninstalls apps from launcher menus
- Assigns two bottom shortcuts to Contacts, Camera, or another app
- Uses a chosen Home image with optional image-derived colors
- Schedules different Home lists, including overnight and all-day ranges
- Explains its gestures and shortcuts in a replayable tutorial

## How it is built

Each feature has a root, immutable state, a ViewModel, and Compose content. Compose sends actions back to the ViewModel; feature roots own navigation and Android effects. Koin supplies repositories and adapters. Room stores one local launcher snapshot behind narrow repository contracts.

| Path | Purpose |
| --- | --- |
| `app/src/main/java/.../domain` | Launcher actions, search rules, and schedules |
| `app/src/main/java/.../data` | Installed apps, Room storage, and repositories |
| `app/src/main/java/.../ui` | Compose screens, gestures, state, and navigation |
| `app/src/test` | JVM behavior and state tests |
| `app/src/androidTest` | Device-level Compose tests |

## Build it

You need JDK 17 and an Android SDK with API 37. VoidLauncher supports Android 10 and newer.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew check
```

Android will offer VoidLauncher the next time you press Home.
