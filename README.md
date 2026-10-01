# FF Headshot Panel

Free Fire headshot overlay mod — gesture injection via Accessibility Service + Shizuku.

## How to Build (GitHub Actions)

1. Fork/push this project to a GitHub repo
2. Go to **Actions** tab → Run **Build FF Headshot APK**
3. Download the APK from **Artifacts** when done

## How to Install

1. Enable "Install Unknown Apps" for your file manager
2. Install the APK
3. Grant every permission on the first screen

## Permissions Needed

| Permission | Why |
|---|---|
| Draw Over Apps | Floating panel overlay |
| Install Unknown Apps (PIP) | Sideload updates |
| All Files Access | Read/write game files |
| Usage Stats | Detect when game is open |
| Accessibility Service | Inject headshot gestures |
| Shizuku | Privileged shell tap injection |
| Phone + Storage | Device detection |

## How To Use

1. Install **Shizuku** from Play Store and activate it
2. Open FF Headshot Panel → grant ALL permissions → tap PROCEED
3. Tap **START PANEL** — Free Fire launches automatically
4. Floating **FF** button appears on screen — drag anywhere
5. Tap it → mod menu opens
6. Toggle **HEADSHOT ONLY** → ON
7. Press **✕** to close the menu (toggle stays ON)
8. Play — headshot injection is active

## How Headshot Injection Works

- `HeadshotService` (Accessibility) uses `dispatchGesture()` to inject touch events
- When you fire in Free Fire, it dispatches an additional tap **145px above** your aim point
- This maps to enemy head level in-game (1080p calibrated)
- `ShizukuHelper` provides privileged `input tap` shell commands as backup injection
- Works on Free Fire (com.dts.freefireth) and Free Fire MAX (com.dts.freefiremax)

## Calibration

Edit `HeadshotService.kt` line:
```kotlin
private const val HEAD_OFFSET_PX = -145
```
- `-145` = 1080p screens
- `-175` = 1440p screens  
- `-120` = 720p screens

## Project Structure

```
app/src/main/java/com/ff/headshot/
  PermissionsActivity.kt   — First screen, all permission grants
  MainActivity.kt          — START/STOP panel, status display
  FloatingMenuService.kt   — Overlay window + mod menu
  HeadshotService.kt       — Accessibility gesture injection
  ShizukuHelper.kt         — Privileged shell via Shizuku
  GameLauncher.kt          — Auto-launch Free Fire
```
