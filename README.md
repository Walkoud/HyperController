# HyperController

**Take back control of background app management on Xiaomi (MIUI / HyperOS).**

HyperController is a root app that lets you decide — per app — how MIUI's power
manager treats your apps in the background. Stop notifications being missed, music
being killed, and alarms not firing because MIUI decided to shut an app down.

---

## Screenshots

<!-- Screenshots to be added -->
| App list | Filters | App detail | Templates |
|----------|---------|------------|-----------|
| ![App list](docs/screenshots/list.png) | ![Filters](docs/screenshots/filter.png) | ![Detail](docs/screenshots/detail.png) | ![Templates](docs/screenshots/templates.png) |

---

## Features

- **Per-app restriction control** — set any app to one of four states: Full freedom,
  Default (MIUI), Soft saver, or Strict kill.
- **Batch actions** — apply a restriction to many apps at once.
- **Search & filter** — by name, package, restriction state, category, or app type.
- **Templates** — one-tap presets. Ships with *Unchained*, *Anti-Kill Global*, and
  *Thermal Unlock*, plus a full editor to build your own.
- **Auto-config service** — automatically apply a template to newly installed apps.
- **Advanced cloud config view** — inspect the raw PowerKeeper values MIUI stores.
- **Safety guards** — critical system apps are protected from modification; backups
  are taken before batch changes.

---

## Requirements

- Xiaomi / Redmi / POCO device running **MIUI or HyperOS**
- **Root** (Magisk, KernelSU, or APatch)

The app bundles its own `sqlite3` binary — nothing else to install.

---

## Install

Download the latest `HyperController-vX.Y.Z.apk` from the
[**Releases**](https://github.com/Walkoud/HyperController/releases) page and install
it. Grant root when prompted.

---

## Build from source

Requires JDK 17 and the Android SDK.

```bash
git clone https://github.com/Walkoud/HyperController.git
cd HyperController
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Release APKs are built automatically by GitHub Actions when a `v*` tag is pushed.

---

## Documentation

See the [**User Guide**](docs/USER_GUIDE.md) for a full walkthrough of the
restriction states, templates, and the auto-config service.

---

## Disclaimer

HyperController modifies system databases as root. It protects critical system apps
and takes backups, but you use it at your own risk. Freeing many apps from all
restrictions increases battery usage — that's the trade-off for keeping them alive.
Settings may reset after a system/OTA update.
