# HyperController — User Guide

HyperController is a root app for Xiaomi phones (MIUI / HyperOS) that gives you
direct control over how the system manages your apps in the background. It stops
MIUI from aggressively killing, freezing, or throttling the apps you care about.

---

## Why this app exists

MIUI/HyperOS ships with a very aggressive power manager (`com.miui.powerkeeper`).
It decides, often without asking, which apps get killed in the background. This is
why messaging apps miss notifications, music players stop, alarms don't fire, and
fitness/tracking apps lose data.

HyperController edits the PowerKeeper database directly (as root) so you decide the
policy per app, instead of leaving it to MIUI.

---

## Requirements

- A Xiaomi / Redmi / POCO device running **MIUI or HyperOS**.
- **Root access** (Magisk, KernelSU, or APatch). The app runs shell commands as
  root to read and write the system databases.
- The app bundles its own `sqlite3` binary, so you don't need to install one.

Without root, the app can't read or modify anything and will show
"Root not available" in Settings.

---

## The four restriction states

Every app is in one of these states. You can see and change it on the app's detail
screen, or set several at once with batch actions.

| State | What it does |
|-------|--------------|
| **Full freedom** | No restriction — the app can run freely in the background. |
| **Default (MIUI)** | Managed automatically by MIUI (the system default). |
| **Soft saver** | Cuts background data/location but keeps the app in memory. |
| **Strict kill** | Kills or freezes the app in the background immediately. |

For most "won't stay alive" problems, set the app to **Full freedom**.

### Kill delay

Some states support a **delay before kill** (in minutes):
- `-1` = immediate
- `0`–`1440` = wait that many minutes before the system acts

---

## The app list

The main screen lists your installed apps with their current restriction state
shown as a colored badge.

- **Search** — tap the search icon and type; it matches app name or package name.
- **Filter** — tap the filter icon to filter by restriction state, category, or to
  show/hide system apps, and to change the sort order.
- **Batch actions** — tick the checkbox on several apps, then tap the
  "N selected" button to apply one restriction to all of them at once.
  Protected critical system apps are automatically skipped for safety.

---

## App detail screen

Tap an app to open its detail screen. Here you can:

- See its package, type (system/user), category, current state, and delay.
- **Change the restriction** — pick one of the four states, optionally set a delay,
  and tap Apply.
- View the **cloud config (advanced)** — the raw PowerKeeper values MIUI stores for
  that app (`bgData`, `k_delay`, `k_policy`, etc.), for troubleshooting.

Critical system apps are locked (modification blocked) to keep your phone stable.
Sensitive system apps show a warning but can still be changed.

---

## Templates

Templates let you apply a whole set of actions in one tap. Open the **Templates**
tab.

### Built-in templates

- **Unchained** — Frees selected apps from all MIUI restrictions (sets them to Full
  freedom and adds them to the MIUI whitelists).
- **Anti-Kill Global** — Disables the aggressiveness of the MIUI task killer
  globally (turns off several kill/idle/standby features and restarts PowerKeeper).
- **Thermal Unlock** — Pushes back thermal limits for more sustained performance.

### Custom templates

Tap **+** to create your own. A template has a name, a target, and a list of
actions. Available action types:

- **Restriction** — set a restriction state (+ delay).
- **Global setting** — set a raw PowerKeeper key/value.
- **Add to whitelist** — add the target apps to a named MIUI whitelist.
- **Kill process** — restart a process (default `com.miui.powerkeeper`) so changes
  take effect.

To apply a template, tap it and confirm. For templates targeting selected packages,
pick the apps first in the app list.

---

## Auto-config service

In **Settings**, enable **Auto-config for new apps** and choose a template. From
then on, whenever you install a new app, HyperController automatically applies that
template to it. A persistent notification shows the service is active, and you get a
notification each time a template is auto-applied.

> On some custom ROMs, foreground services are blocked by default. If the
> persistent notification doesn't appear, you may need to allow background activity
> for HyperController in system settings.

---

## Settings

- **Root** — shows whether root is available, your MIUI/HyperOS version, and whether
  the databases are accessible. "Test again" re-checks.
- **Auto-config for new apps** — auto-apply a template to newly installed apps.
- **Restart after change** — force a PowerKeeper restart after each change so it
  takes effect immediately (recommended).
- **Dark theme** — toggle dark mode.
- **Safety log** — a record of important/blocked operations.
- **About** — version and a link to the GitHub repository.

---

## Safety notes

- Critical system apps are protected and cannot be modified.
- Changes are written to system databases; a backup is taken before batch changes.
- Freeing many apps from all restrictions can increase battery drain — that's the
  trade-off for keeping them alive.
- Settings may reset after a system/OTA update; just re-apply your templates.
