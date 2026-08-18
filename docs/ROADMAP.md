# Roadmap

This roadmap tracks intentionally deferred work. Items here are not treated as implemented until the target classes/slots are verified on the current Android 16 SystemUI build and CI/runtime validation exists.

## P1 — Status-bar ignored icons

**Status:** planned; not implemented.

The analogous current PixelXpert `StatusIconTuner` locates `IconManager` containers by parent resource IDs and then directly rewrites the private `mIgnoredSlots` list. That approach is reported unreliable on this target, so Pixel Dense UI will not copy it blindly.

### Required implementation approach

1. Capture the Android 16 status-icon slot model/controller path on the target SystemUI build.
2. Enumerate actual slot names and ownership before exposing selectable values.
3. Prefer filtering configured slots **before icon-view materialisation** rather than hiding arbitrary child views after layout.
4. Keep status-bar, Quick Settings and keyguard icon surfaces separately addressable; do not assume one container/list controls all three.
5. Preserve privacy, emergency, security and other critical indicators by default. Any ability to hide those must be explicit and separately validated.
6. Make filtering reversible at runtime and resilient to reinflation, rotation, density/configuration changes, user switch and SystemUI restart.
7. Fail soft when an OTA changes the controller/model/slot mapping: unrecognised targets remain visible rather than causing SystemUI failure.
8. Add target-build evidence and regression tests before promoting the feature from roadmap to supported.

### Acceptance criteria

- configured ordinary slots remain hidden across portrait/landscape and QS/keyguard transitions;
- clearing a slot restores it without reboot where the host pipeline supports live refresh;
- no duplicate/missing unrelated icons after SystemUI restart;
- no mutation of unrelated container state;
- no main-SystemUI crash if the expected slot pipeline is absent.

## P2 — Runtime target diagnostics

Add a read-only diagnostics page that reports which optional hook targets were found at runtime (Compose QS repositories, `DeviceEntryIconView`, scrim classes, screenshot QPR1/QPR2 controllers, launcher taskbar targets). This should make OTA drift visible without relying only on logcat.

## P2 — Preference migration/versioning

Before a stable public release, add an explicit preference-schema version so renamed controls such as fixed-dp status-bar height can be migrated or discarded deterministically instead of leaving stale keys.

## P3 — Per-orientation status-bar profiles

Consider independent portrait/landscape status-bar height and pixel-padding values only after the single-profile percentage model is physically validated. Do not add orientation complexity before the current top-edge/cutout policy is proven stable.
