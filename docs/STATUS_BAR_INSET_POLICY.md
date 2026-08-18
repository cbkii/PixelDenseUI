# Status-bar / camera-hole inset policy

Goal: keep status content at the physical **top edge** while preserving the real centre-camera cutout model.

## Height model

Pixel Dense UI no longer substitutes a fixed dp height. It scales the stock Android/SystemUI status-bar height by the configured **percentage** (50–150%).

- `SystemBarUtils.getStatusBarHeight*()` results are scaled from their platform values.
- SystemUI's `status_bar_height` resource/view receives the same percentage.
- The top `DisplayCutout` safe inset and top-bound bottom edge are clamped to the same scaled height.

This mirrors PixelXpert's percentage-size model while retaining Pixel Dense UI's stricter cutout policy.

## Pixel padding model

Status-bar layout exposes raw-pixel controls:

- start padding: `-1` = preserve current stock value, otherwise 0–240 px;
- end padding: `-1` = preserve current stock value, otherwise 0–240 px;
- top content padding: 0–48 px.

Start/end padding is applied to `status_bar_contents` after `PhoneStatusBarView` layout/height updates. Top padding is also applied to the targeted status content/icon containers.

Raw px is intentional here: these controls are for exact physical placement around the target display/camera geometry rather than density-independent app layout.

## What the module does not do

- It never returns `DisplayCutout.NO_CUTOUT`.
- It does not remove or fake the centre cutout object.
- It does not intentionally force icons through the camera region.
- It does not spoof display resolution or global density.

## Defaults

- height: **100% of stock**;
- start padding: stock (`-1`);
- end padding: stock (`-1`);
- top padding: **0 px**;
- Y offset: **0 dp**.

The height default is deliberately conservative because no preferred percentage has been specified. Change one value at a time during physical validation; validate height/cutout alignment before changing start/end/top padding or Y offset.
