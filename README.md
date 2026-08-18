<div align="center">

# ✨ Pixel Dense UI

**A focused Android 16 SystemUI tuning module for stock Google Pixel devices.**

[![Android 16](https://img.shields.io/badge/Android-16-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![libxposed API 101](https://img.shields.io/badge/libxposed-API%20101-6f42c1)](https://github.com/libxposed/api)
[![CI](https://github.com/cbkii/PixelDenseUI/actions/workflows/build.yml/badge.svg)](https://github.com/cbkii/PixelDenseUI/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/cbkii/PixelDenseUI?include_prereleases)](https://github.com/cbkii/PixelDenseUI/releases)
[![License: GPL-3.0](https://img.shields.io/github/license/cbkii/PixelDenseUI)](LICENSE)

Make more of the Pixel UI fit on screen: **denser Quick Settings, a compact top-edge status bar, optional compact notifications, lockscreen cleanup, screenshot mute, and the Pixel Launcher taskbar** — without becoming an all-in-one theming suite.

[📦 Releases](https://github.com/cbkii/PixelDenseUI/releases) · [⚙️ Advanced guide](docs/ADVANCED.md) · [🗺️ Roadmap](docs/ROADMAP.md) · [🤝 Contributing](CONTRIBUTING.md)

</div>

> [!IMPORTANT]
> Pixel Dense UI currently targets **stock Google Pixel Android 16** and is physically validated primarily against the **Pixel 9a (`tegu`)** SystemUI/Pixel Launcher stack. Private targets can change in an OTA.

## 🌟 What it does

### ⚡ Quick Settings

- Scale QS sizing/spacing from **25–100%**.
- Independently shrink Compose tile height from **50–100% of stock**.
- Configure portrait quick rows, full rows and columns.
- Configure landscape columns independently while optionally leaving row counts at SystemUI defaults.
- Default portrait profile: **50% sizing, 100% tile height, 3 quick rows, 4 full rows, 8 columns**; landscape uses **12 columns**.

### 📶 Status bar

- Move status-bar content toward the physical top edge while retaining the real camera cutout.
- Scale framework and SystemUI status-bar height from **50–150% of stock**.
- Set one common content distance from the physical top edge, plus start/end padding and a small vertical fine offset.
- Align the Android 16 notification/system icon containers at the semantic container level instead of recursively rewriting arbitrary descendants.
- Put the clock in stock, left, right or centre position and optionally show seconds.
- Show a tiny upload/download monitor as a **non-layout overlay over the cellular/reception cluster**. It auto-hides, uses an approximately 50% black background, a green download arrow and a red upload arrow.

> [!NOTE]
> Centre clock means literal screen centre and can overlap a centre punch-hole depending on the chosen bar geometry.

### 🔔 Notifications

Choose one notification mode:

- **Off** — install no PixelDenseUI notification hook pack; useful as the performance/behaviour baseline.
- **Silent only** — compact supported silent contracted rows only.
- **All** — compact supported normal and silent contracted rows independently.

The redesigned path no longer performs recursive notification tree walking from layout callbacks, no longer hooks notification height getters, and no longer applies PixelDenseUI notification dimensions through the process-wide resource interceptor. Group children, heads-up, media, calls, conversations/messaging, progress and unknown/custom layouts remain stock until independently validated.

> [!WARNING]
> `Off` is the safe default while the new renderer completes physical performance validation. Recreate SystemUI or reboot after changing notification hook mode.

### 🔒 Lock screen / screenshot

- Hide the fingerprint background circle and/or fingerprint icon visually without deliberately disabling UDFPS authentication.
- Adjust keyguard wallpaper dimming from 0–100%.
- Disable screenshot sound in the isolated SystemUI screenshot process.

### 🧭 Pixel Launcher

- Enable the native tablet-style Pixel Launcher taskbar on phones.

## 📲 Install and required scopes

1. Install the APK from **GitHub Releases**.
2. Enable Pixel Dense UI in an API-101-capable libxposed/Vector-compatible manager.
3. Keep these static scopes approved:
   - `system` — **system_server** on modern libxposed/Vector;
   - `android` — Android framework-package compatibility scope;
   - `com.android.systemui`;
   - `com.google.android.apps.nexuslauncher`.
4. Open Pixel Dense UI. If a required scope is missing, use **Request required scope**, approve it in the framework manager, then reboot.
5. During first validation, disable overlapping PixelXpert/Iconify hooks for the same UI areas.

> [!IMPORTANT]
> v0.1.2 and earlier omitted the modern `system` scope, so `FrameworkStatusBarHooks` could not run under Vector. An existing installation may retain the old manager scope after updating; the settings app now detects and requests the missing scope.

## 🧪 Runtime diagnostics

The settings screen reports:

- app version/code and build source revision;
- connected framework name/version/API;
- actual approved scope;
- whether the current PixelDenseUI build has reached `system_server`, main SystemUI, the screenshot child process and Pixel Launcher.

One-time PixelDenseUI hook-installation messages are also mirrored to Android logcat as well as the framework log.

## 🚧 Current limitations / validation boundary

- The corrected modern `system` scope activates framework status-bar height/cutout code that v0.1.2 did not exercise under Vector. **Cutout-safe-inset clamp therefore defaults off** until separately physically validated; first test framework/SystemUI height agreement, then enable the clamp as one variable.
- Notification mode `Off` is the safe default until controlled Off/Silent/All traces demonstrate no material shade-jank regression.
- Ignored status-bar icon selection remains roadmap-only.
- A patched/re-signed `SystemUIGoogle.apk` via Magisk is intentionally not used. A future RRO is only a benchmark candidate for static/global resources and cannot implement per-row silent-only behaviour; see [RRO_EVALUATION.md](docs/RRO_EVALUATION.md).

## 🛟 Recovery

If SystemUI, lockscreen, screenshot or Launcher behaves incorrectly after an OTA or setting change, disable Pixel Dense UI in the framework manager and reboot. Re-enable and validate one feature family at a time. Do not mask a reproducible problem with multiple simultaneous setting/module changes.

See [VALIDATION.md](docs/VALIDATION.md) for the maintained physical test sequence.

## 📚 Documentation

| Topic | Document |
|---|---|
| Advanced runtime / troubleshooting | [docs/ADVANCED.md](docs/ADVANCED.md) |
| Physical validation | [docs/VALIDATION.md](docs/VALIDATION.md) |
| Target classes/resources | [docs/DEVICE_HOOK_MAP.md](docs/DEVICE_HOOK_MAP.md) |
| Initial APK evidence | [docs/APK_EVIDENCE.md](docs/APK_EVIDENCE.md) |
| Magisk/RRO decision | [docs/RRO_EVALUATION.md](docs/RRO_EVALUATION.md) |
| Roadmap | [docs/ROADMAP.md](docs/ROADMAP.md) |
| Development / CI | [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) |
| Provenance | [docs/UPSTREAM.md](docs/UPSTREAM.md) |

## 🙏 Credits

Pixel Dense UI builds on published Android/Xposed work from **PixelXpert / siavash79, Pixel Taskbar Enabler, Iconify, libxposed, JingMatrix/Vector and libxposed examples, LSPosed, and AOSP**. Feature-level provenance and intentional deviations remain documented in [docs/UPSTREAM.md](docs/UPSTREAM.md) and [NOTICE.md](NOTICE.md).

## 📄 License

GPL-3.0-only. See [LICENSE](LICENSE).
