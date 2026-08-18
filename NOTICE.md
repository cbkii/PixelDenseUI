# Notices, acknowledgements and provenance

Pixel Dense UI was initially assembled in August 2026 as a focused Android 16 / Google Pixel libxposed module.

## 🙏 Thank you

This project exists because open-source Android and Xposed projects made the platform internals, hook patterns and modern module APIs available to study and reuse. **Thank you to the maintainers and contributors of every project listed below.**

### GPL-licensed implementation references

- **PixelXpert** — <https://github.com/siavash79/PixelXpert>
  - Primary battle-tested reference for Pixel status-bar height/cutout handling, status-bar clock/icon/network traffic behaviour, Android 16 Compose Quick Settings, UDFPS/keyguard handling, screenshot-sound fallbacks, reflection safety, restart protection, and independent hook-pack loading.
- **Pixel Taskbar Enabler** — <https://github.com/beymans-code/pixel-taskbar-enabler>
  - Reference/basis for enabling the native Pixel Launcher tablet-style taskbar on phones.
- **Iconify** — <https://github.com/Mahmud0808/Iconify>
  - Independent Android 16 reference corroborating the Compose Quick Settings repository/resource-wrapper strategy.

Pixel Dense UI is distributed under **GPL-3.0-only**. It intentionally does not present adapted upstream hook techniques as original inventions.

### Modern Xposed/libxposed ecosystem

- **libxposed API/service** — <https://github.com/libxposed>
  - Modern Xposed API/service foundation used by the module.
- **libxposed example module** — <https://github.com/libxposed/example>
  - Reference implementation for modern module structure and API usage.
- **JingMatrix/libxposed-example** — <https://github.com/JingMatrix/libxposed-example>
  - Current API-100+ example consulted while aligning Pixel Dense UI with modern libxposed conventions.
- **LSPosed** — <https://github.com/LSPosed/LSPosed>
  - Framework/tooling lineage and wider Xposed ecosystem that enables system-process module development and use.

### Android platform

- **Android Open Source Project (AOSP)** — <https://android.googlesource.com/platform/frameworks/base/>
  - Android framework and SystemUI source used as the reference architecture for understanding status-bar, notification, Quick Settings, keyguard and display-cutout behaviour.

Google Pixel SystemUI/Pixel Launcher binaries from the target device were used for compatibility analysis only and are **not redistributed** by this repository.

## Detailed feature-level provenance

See [`docs/UPSTREAM.md`](docs/UPSTREAM.md) for the feature/file-level mapping, fallback rationale and intentional differences from upstream implementations.

See [`LICENSE`](LICENSE) for the repository licence text.
