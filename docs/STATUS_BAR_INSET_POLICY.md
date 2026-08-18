# Status-bar / camera-hole inset policy

Goal: put status content at the physical **top edge**, as far above/alongside the Pixel 9a centre punch-hole as practical.

## What v0.1 changes

- Framework status-bar height (`SystemBarUtils`).
- The top `DisplayCutout` safe inset and top-bound bottom edge are clamped to the selected compact status-bar height.
- SystemUI's `PhoneStatusBarView` receives the same compact height.
- SystemUI content top padding defaults to 0 dp.

## What v0.1 does not do

- It never returns `DisplayCutout.NO_CUTOUT`.
- It does not remove the centre cutout object.
- It does not horizontally force icons through the camera region.
- It does not spoof display resolution or density.

This is a deliberate refinement of PixelXpert's `StatusbarSize`: use its proven height/cutout clamp, but exclude its more aggressive no-cutout mode.

## Default

- height: 20 dp
- top padding: 0 dp
- Y offset: 0 dp

Only use a negative Y offset after confirming the 20 dp / 0 dp baseline; a negative translation can clip glyphs at the physical display boundary.
