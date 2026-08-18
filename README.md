<div align="center">

# ✨ Pixel Dense UI

**A focused Android 16 SystemUI tuning module for stock Google Pixel devices.**

[![Android 16](https://img.shields.io/badge/Android-16-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![libxposed API 101](https://img.shields.io/badge/libxposed-API%20101-6f42c1)](https://github.com/libxposed/api)
[![CI](https://github.com/cbkii/PixelDenseUI/actions/workflows/build.yml/badge.svg)](https://github.com/cbkii/PixelDenseUI/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/cbkii/PixelDenseUI?include_prereleases)](https://github.com/cbkii/PixelDenseUI/releases)
[![License: GPL-3.0](https://img.shields.io/github/license/cbkii/PixelDenseUI)](LICENSE)

Make more of the Pixel UI fit on screen: **denser Quick Settings, a compact top-edge status bar, compact notifications, lockscreen cleanup, screenshot mute, and the Pixel Launcher taskbar** — without becoming an all-in-one theming suite.

[📦 Releases](https://github.com/cbkii/PixelDenseUI/releases) · [⚙️ Advanced guide](docs/ADVANCED.md) · [🗺️ Roadmap](docs/ROADMAP.md) · [🤝 Contributing](CONTRIBUTING.md)

</div>

> [!IMPORTANT]
> Pixel Dense UI currently targets **stock Google Pixel Android 16** and was developed against the **Pixel 9a (`tegu`)** SystemUI/Pixel Launcher stack. Other Pixel models or Android builds are not yet validated.

## 🌟 What it does

### ⚡ Quick Settings

- Scale QS sizing/spacing from **25–100%**.
- Independently shrink the Compose tile **height from 50–100% of stock**.
- Configure portrait **quick rows, full rows, and columns**.
- Configure landscape columns independently while optionally leaving row counts at the SystemUI default.
- Default layout: **50% sizing, 100% tile height, 3 quick rows, 4 full rows, 8 columns**; landscape uses **12 columns**.

### 📶 Status bar

- Move status-bar content to the **top edge**, as far above/alongside the centre camera hole as practical while retaining the real display cutout.
- Scale status-bar height from **50–150% of stock**.
- Set one common **content distance from the physical top edge in pixels**, plus start/end padding and a small vertical fine offset.
- Adjust system-icon spacing and the visible notification-icon limit.
- Put the clock in the **stock, left, right, or centre** position and optionally show **seconds**.
- Show compact **upload/download speed** with an auto-hide threshold.

> [!NOTE]
> Centre clock means literal screen centre. On a Pixel with a centre punch-hole it can overlap the camera cutout; use it only when your chosen status-bar geometry leaves suitable room.

### 🔒 Lock screen

- Hide the fingerprint **background circle**.
- Hide the fingerprint **icon**.
- Adjust keyguard wallpaper dimming from **0–100%**.
- Fingerprint hiding is visual only; it is not intended to disable UDFPS authentication or touch handling.

### 🔔 Notifications & screenshots

- Compress normal collapsed notification rows.
- Apply stronger density and smaller icons to silent notifications.
- Independently scale normal and silent notification icons.
- Disable the screenshot sound.

> [!WARNING]
> Notification rendering/performance is under active redesign after testing exposed oversized row icons, grouped-icon corruption and shade jank. The next implementation will replace the current hot-path scaling approach rather than stack more tuning on top of it.

### 🧭 Pixel Launcher

- Enable the tablet-style **Pixel taskbar on phones** using the native Pixel Launcher.

## 🎛️ Default profile

Pixel Dense UI starts with a deliberately dense profile that can be changed from the app:

| Setting | Default |
|---|---:|
| QS sizing / spacing | 50% |
| QS tile height | 100% of stock |
| Portrait quick rows | 3 |
| Portrait full rows | 4 |
| Portrait columns | 8 |
| Landscape quick/full rows | System default |
| Landscape columns | 12 |
| Status-bar height | 100% of stock |
| Status-bar start/end padding | Stock |
| Status-bar content distance from top | 0 px |
| Clock | Left + seconds |
| Network traffic | Enabled |
| Fingerprint circle + icon | Hidden |
| Keyguard wallpaper dim | 66% |
| Screenshot sound | Disabled |
| Normal notification density | 72% |
| Silent notification density | 55% |

## 📲 Install

### Requirements

- A **stock Google Pixel** running **Android 16**.
- A working **API-101-capable libxposed/Xposed runtime** that can hook Android system processes.
- Native **Pixel Launcher** for the taskbar feature.

### Steps

1. Download the APK from **[GitHub Releases](https://github.com/cbkii/PixelDenseUI/releases)** and install it.
2. Enable **Pixel Dense UI** in your libxposed/LSPosed-compatible manager.
3. Keep these scopes enabled:
   - `android`
   - `com.android.systemui`
   - `com.google.android.apps.nexuslauncher`
4. Disable overlapping PixelXpert/Iconify hooks for the same UI areas while testing Pixel Dense UI.
5. Open **Pixel Dense UI** and choose your settings.
6. **Reboot** for the first deterministic validation after enabling the module or making major layout changes.

> [!TIP]
> Start with the defaults, confirm SystemUI/lockscreen/Launcher stability, then change one density or geometry control at a time.

## 🧩 Scope by design

Pixel Dense UI is intentionally small. It focuses on density, layout and a few practical visibility/behaviour controls. It does **not** try to replace full customisation suites with battery themes, colour engines, volume-panel mods, root hiding, unrestricted screenshot features, or hundreds of unrelated toggles.

That narrow scope is deliberate: fewer hooks are easier to understand, validate and repair when Google changes SystemUI in an OTA.

## 🚧 Current limitations

- **Ignored status-bar icon selection is planned, not implemented.** The current roadmap calls for a more reliable Android 16 slot-level implementation instead of copying the unreliable container-mutation path used by PixelXpert.
- The notification density/icon implementation is scheduled for a low-overhead redesign before further visual tuning.
- Private SystemUI classes can change between Pixel OTAs. Pixel Dense UI uses fail-soft hook boundaries, but a new build may still require updated targets.
- Pixel 9a / Android 16 is the current validation target; support on other Pixels is not yet claimed.
- Some changes apply most deterministically after the affected process is recreated; a reboot is the clean baseline.

See **[ROADMAP.md](docs/ROADMAP.md)** for planned work.

## 🛟 If something goes wrong

If SystemUI, the lockscreen, screenshot process or Pixel Launcher behaves incorrectly after an OTA or setting change:

1. disable Pixel Dense UI in your Xposed/libxposed manager;
2. reboot to return the affected processes to stock behaviour;
3. re-enable the module and test one feature family at a time;
4. capture the Android build, module version and relevant logs before reporting a reproducible issue.

More detail: **[Advanced use & troubleshooting](docs/ADVANCED.md)** and **[validation checklist](docs/VALIDATION.md)**.

## 📚 Documentation

| Audience | Document |
|---|---|
| End users | **This README** |
| Advanced users / troubleshooting | [docs/ADVANCED.md](docs/ADVANCED.md) |
| Developers / maintainers | [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) |
| Contributors | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Planned work | [docs/ROADMAP.md](docs/ROADMAP.md) |
| Runtime validation | [docs/VALIDATION.md](docs/VALIDATION.md) |
| Build evidence | [docs/BUILD_STATUS.md](docs/BUILD_STATUS.md) |
| Detailed upstream provenance | [docs/UPSTREAM.md](docs/UPSTREAM.md) |
| Licensing notices | [NOTICE.md](NOTICE.md) |

## 🙏 Credits & thanks

Pixel Dense UI exists because several open-source Android/Xposed projects made the hard parts understandable and reusable. **Thank you to their maintainers and contributors.**

- **[PixelXpert](https://github.com/siavash79/PixelXpert)** by **siavash79** — the primary battle-tested reference for Pixel status-bar geometry, clock/traffic behaviour, Android 16 Compose QS hooks, lockscreen/UDFPS handling, screenshot fallbacks, reflection safety and restart protection.
- **[Pixel Taskbar Enabler](https://github.com/beymans-code/pixel-taskbar-enabler)** by **beymans-code** — the basis/reference for enabling the native Pixel Launcher tablet taskbar on phones.
- **[Iconify](https://github.com/Mahmud0808/Iconify)** by **Mahmud0808** and contributors — an independent Android 16 reference for the Compose Quick Settings repository/resource-wrapper strategy.
- **[libxposed](https://github.com/libxposed)** and its **[example module](https://github.com/libxposed/example)** — the modern Xposed API/service foundation used by this project.
- **[JingMatrix/libxposed-example](https://github.com/JingMatrix/libxposed-example)** — a current API-100+ example used while aligning the module with modern libxposed conventions.
- **[LSPosed](https://github.com/LSPosed/LSPosed)** and the wider Xposed ecosystem — for the framework, tooling and community knowledge that make this kind of system modification possible.
- **[Android Open Source Project](https://android.googlesource.com/platform/frameworks/base/)** — for Android framework/SystemUI source and the reference architecture used to understand the platform behaviour being modified.

Pixel Dense UI does **not** claim these hook techniques as original inventions. Feature-level provenance and intentional deviations are documented in **[docs/UPSTREAM.md](docs/UPSTREAM.md)** and **[NOTICE.md](NOTICE.md)**.

## 📄 License

Pixel Dense UI is licensed under **GPL-3.0-only**. See [LICENSE](LICENSE).
