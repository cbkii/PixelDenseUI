# 🛠️ Development & maintenance

This document contains maintainer/developer material intentionally kept out of the end-user README.

## Toolchain

Current project baseline:

- JDK 17
- Android SDK / compileSdk 36
- targetSdk 36
- minSdk 36
- Gradle 9.3.1
- libxposed API 101

Build a debug APK with:

```bash
gradle :app:assembleDebug
```

Run the same core source gate used by CI with:

```bash
bash scripts/verify.sh
```

## Project structure

The module is deliberately small:

- `app/src/main/java/dev/pixeldenseui/ModuleMain.java` — package/process routing and hook-pack isolation.
- `app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java` — deterministic preference defaults/bounds.
- `app/src/main/java/dev/pixeldenseui/hooks/` — framework, SystemUI, screenshot and Launcher hook families.
- `app/src/main/java/dev/pixeldenseui/safety/` — rapid-restart protection.
- `app/src/main/resources/META-INF/xposed/` — modern libxposed entry point, metadata and static scopes.
- `scripts/verify.sh` — repository invariants and release-contract checks.
- `docs/` — target evidence, validation, roadmap and upstream provenance.

## Hooking rules

These are correctness requirements, not style preferences:

1. **Hook only the intended class's declared methods.** Do not walk into superclasses while registering hooks; a removed SystemUI override must not widen into a framework-wide `View`/`ViewGroup` hook.
2. **Optional private classes must fail soft.** A missing target disables only that feature path.
3. **Keep hook packs independent.** Framework, main SystemUI, lockscreen, notifications, screenshot child process and Launcher must not share a single fatal installation boundary.
4. **Preserve physical display geometry.** Do not implement `DisplayCutout.NO_CUTOUT` or globally falsify display insets for the compact status bar.
5. **Prefer host-process runtime hooks over broad resource replacement.** Modern libxposed does not provide the legacy resource-hook architecture PixelXpert historically used.
6. **Scope broad fallbacks to narrow processes.** Example: the screenshot `MediaPlayer` fallback belongs only in the screenshot child process.
7. **Do not guess OTA targets.** Unknown classes/fields/methods should remain stock and log diagnostics.
8. **One variable at a time.** Avoid unrelated refactors in hook-repair PRs.

## Testing

Before a PR is considered merge-ready, run or obtain CI evidence for:

- `bash -n scripts/verify.sh`;
- `bash scripts/verify.sh`;
- `:app:testDebugUnitTest`;
- `:app:lintDebug` (or the relevant release lint task when changing release code);
- `:app:assembleDebug`;
- release-variant assembly;
- repository cleanliness after build/signing material cleanup.

The unit regression around `HookUtil.methodsNamed()` is especially important: null targets, unknown names, inherited-only methods and declared overloads must keep the documented contract.

## GitHub Actions CI

`.github/workflows/build.yml` runs source verification followed by Android unit/lint/build gates. It also creates an **ephemeral CI-only keystore**, builds a signed release-smoke APK and verifies it with `apksigner`.

CI signing is only a path test. It does not use or expose the real release key.

See [BUILD_STATUS.md](BUILD_STATUS.md) for current evidence.

## Manual release

`.github/workflows/release.yml` is `workflow_dispatch` only and uses the `release` GitHub Environment.

Configured secret names:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Version behaviour:

- blank input => next patch from current `versionName`;
- explicit `0.1.2` or `v0.1.2` => requested version is authoritative;
- a new version increments `versionCode` once;
- an explicit unreleased version already present in source can resume publication without incrementing twice;
- existing tags/releases are immutable.

Transaction order:

1. resolve requested/next version;
2. rewrite release metadata in the runner worktree;
3. run repository verification;
4. validate signing secrets and JKS alias;
5. run unit tests, release lint and signed release build;
6. verify the APK with `apksigner`;
7. only then commit `chore(release): prepare vX.Y.Z` and guarded-push the metadata update to `main`;
8. recheck that the tag/release did not appear concurrently;
9. publish the GitHub Release from that exact commit;
10. remove signing material in the unconditional cleanup step.

This ordering intentionally prevents failed signing/build validation from advancing the source version on `main`.

## Updating for a Pixel OTA

Use the target APK/build as the source of truth.

Recommended sequence:

1. obtain the current SystemUI/Launcher/framework evidence;
2. compare the exact failing target with AOSP and current upstream implementations;
3. keep existing working fallback order where possible;
4. add a new fallback only when the class/method relationship is understood;
5. keep old targets optional if they remain useful for adjacent QPRs;
6. add/update regression coverage and `docs/DEVICE_HOOK_MAP.md` / `docs/APK_EVIDENCE.md`;
7. physically validate one feature family at a time.

## Upstream/reference policy

Pixel Dense UI deliberately reuses or adapts battle-tested patterns where licensing permits. Do not erase provenance when moving/refactoring code.

When importing/adapting a new implementation pattern:

- confirm the upstream licence;
- record the project, file/feature and nature of the adaptation in [UPSTREAM.md](UPSTREAM.md);
- update [NOTICE.md](../NOTICE.md) if the project is newly referenced;
- prefer a narrow adaptation over copying an all-in-one subsystem.

## Related maintainer docs

- [ADVANCED.md](ADVANCED.md) — runtime architecture and troubleshooting.
- [VALIDATION.md](VALIDATION.md) — physical validation sequence.
- [ROADMAP.md](ROADMAP.md) — intentionally deferred work.
- [DEVICE_HOOK_MAP.md](DEVICE_HOOK_MAP.md) — current target map.
- [UPSTREAM.md](UPSTREAM.md) — detailed feature-level provenance.
