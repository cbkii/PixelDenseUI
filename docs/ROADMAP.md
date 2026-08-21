# Roadmap

This roadmap tracks intentionally deferred work. Items are not treated as implemented until current Android 16 targets and physical validation support them.

## P1 — Status-bar ignored icons

**Status:** planned; not implemented.

The analogous PixelXpert path mutates private ignored-slot lists. Pixel Dense UI will not copy that blindly. The target Android 16 slot/controller pipeline must first be enumerated, with status-bar, QS and keyguard surfaces handled separately and critical security/privacy/emergency indicators preserved by default.

Acceptance requires reversible slot filtering, no unrelated icon loss/duplication after restart/configuration changes, and fail-soft behaviour after OTA drift.

## P1 — Physical validation of corrected system-server/status-bar geometry

**Status:** implementation landed on the validation branch; device validation required.

The modern libxposed scope now explicitly includes `system`, which is required for `onSystemServerStarting()` under Vector. The SystemUI path also replaces recursive descendant gravity rewriting with targeted status/notification-icon container alignment and moves network traffic into a non-layout cellular overlay.

Validate framework/SystemUI height agreement first with cutout clamp disabled, then test the clamp independently.

## P1 — Low-overhead notification renderer

**Status:** implementation landed on the validation branch; controlled performance validation required.

The old process-wide notification resource scaling, notification height hooks, recurring row-layout callbacks and recursive icon traversal are removed. The replacement supports `Off / Silent only / All` and applies only to conservative contracted-content cases at stable update/content lifecycle points.

Promotion requires no material shade-jank regression versus mode Off and correct normal/silent/group/special-layout behaviour.

## P2 — Runtime target diagnostics

**Status:** first stage implemented.

The settings screen reports source/build identity, connected framework/API and approved scope. Host-process reachability is intentionally verified separately from Vector module-load/runtime logs and the maintained validation collector rather than by injected hosts writing synthetic preference markers. A later stage may expose authoritative running-target/private-hook state directly if the supported module-app service API can do so without widening the Android SDK boundary solely for diagnostics.

## P2 — Optional Magisk/RRO benchmark

A patched/re-signed `SystemUIGoogle.apk` is rejected as a production design. An RRO may be prototyped only for small static/global resource values after exact `<overlayable>` eligibility is mapped and a controlled benchmark demonstrates a material advantage. RRO cannot implement per-row silent-only behaviour. See `RRO_EVALUATION.md`.

## P2 — Preference migration/versioning

Before a stable public release, add a preference-schema version for renamed/removed settings. Scope migration is handled through the libxposed service's scope request API; preference migration remains separate.

## P3 — Per-orientation status-bar profiles

Consider independent portrait/landscape status-bar height and pixel-padding values only after the corrected single-profile system-server/top-edge policy is physically proven.
