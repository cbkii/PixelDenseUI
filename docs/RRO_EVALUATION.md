# Magisk / RRO evaluation for notification density

This document records the implementation decision made after physical Pixel 9a Android 16 validation of v0.1.2.

## Decision

Pixel Dense UI will **not** replace or patch `SystemUIGoogle.apk` through Magisk. Replacing a privileged Google SystemUI APK would couple the module to the exact OTA binary, change the APK signing boundary, and greatly increase boot/OTA failure risk.

A Magisk-delivered Runtime Resource Overlay (RRO) remains a possible **optional future backend** for static, global resource values only. It is not part of the current implementation because the immediate performance defect can be removed without adding another system modification layer.

## Why an RRO cannot replace the dynamic notification path

An RRO resolves resources for the target package. It has no per-notification ranking or section context. It can therefore express a global value such as a common notification padding or minimum height, but it cannot express:

- compact this silent row while leaving the normal row above it stock;
- skip a grouped child or heads-up notification while compacting an ordinary contracted row;
- choose a value from the notification's current silent bucket at runtime.

The user-facing `Off / Silent only / All` notification modes therefore remain a runtime SystemUI concern.

## Current architecture

The current low-overhead implementation:

1. removes PixelDenseUI notification dimensions from the process-wide `Resources#getDimensionPixelSize()` interceptor;
2. does not hook notification measurement/height accessors or recurring layout callbacks;
3. applies contracted-row geometry only at stable notification content/update/state transitions;
4. leaves grouped children, HUN, media, calls, conversations, progress and unknown/custom layouts stock until independently mapped;
5. installs no notification hook pack at all when notification mode is `Off`.

## Gate for a future RRO prototype

Do not ship an RRO merely because it is technically possible. Prototype it only after all of the following are available:

1. the exact current `SystemUIGoogle.apk` is extracted and its `<overlayable>`/idmap eligibility is mapped;
2. the candidate resource set is small and global by nature;
3. controlled traces compare stock/notification-Off, the current runtime implementation, and the RRO prototype with the same notification set and device state;
4. the RRO shows a material performance or correctness benefit;
5. OTA mismatch fails safe rather than leaving an incompatible overlay enabled.

Until then, the smaller Vector/libxposed implementation is the lower-risk production path.
