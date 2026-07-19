# HyperController — Build & Debug Notes (CLAUDE.md)

## Build

Java 11+ required (AGP 8.5.0). Default system Java is 1.8 — use Corretto 17 via scoop:

```bash
export JAVA_HOME="/c/Users/volka/scoop/apps/corretto17-jdk/current"
export PATH="$JAVA_HOME/bin:$PATH"
cd "C:/Users/volka/Desktop/temp/HyperController"
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Debugging on device (Xiaomi / HyperOS / Thanox ROM)

Device is API 36. A custom ROM (Thanox) blocks foreground services by default.
If the auto-config persistent notification does not appear, grant these app-ops
(they default to deny / 'ignore' and may reset after reboot):

```bash
PKG=com.walkoud.hypercontroller
adb shell cmd appops set $PKG START_FOREGROUND allow
adb shell cmd appops set $PKG MIUIOP 10022 allow
adb shell cmd appops set $PKG RUN_IN_BACKGROUND allow
adb shell cmd appops set $PKG RUN_ANY_IN_BACKGROUND allow
adb shell dumpsys deviceidle whitelist +$PKG
```

Capture logs for the service:

```bash
adb logcat -c && adb shell am force-stop com.walkoud.hypercontroller
adb shell monkey -p com.walkoud.hypercontroller -c android.intent.category.LAUNCHER 1
adb logcat -d | grep -i AutoConfigService
adb shell dumpsys notification --noredact | grep com.walkoud
```

## Key architecture

- Foreground service: service/AutoConfigService.kt (auto-started from MainActivity.onResume when auto_config=true)
- DB access: core/db (PowerKeeperDb, SecurityCenterDb, DbExecutor) via sqlite3 shell as root
- Batch SQL: DbExecutor.execSQLBatch validates each statement individually then joins with ';'
- Templates stored as JSON in SharedPreferences ('templates' pref)
