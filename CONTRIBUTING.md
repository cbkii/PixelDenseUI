# 🤝 Contributing to Pixel Dense UI

Thanks for helping improve Pixel Dense UI.

The project intentionally stays **small, understandable and Pixel-focused**. Contributions are welcome when they improve correctness, reliability, compatibility or the existing dense-UI feature set without turning the module into another all-in-one customisation suite.

## Before opening a PR

1. Check the [README](README.md), [roadmap](docs/ROADMAP.md) and existing issues/PRs.
2. For Android/SystemUI changes, identify the exact target build/class/resource rather than assuming an older PixelXpert/AOSP target still exists.
3. Prefer inspection and evidence before changing hooks.
4. Keep one logical change per PR where practical.

## Good contribution areas

- Fixing a hook broken by a Pixel Android 16 OTA/QPR.
- Improving fallback ordering or fail-soft behaviour.
- Tests for hook discovery, preference bounds, formatting or release invariants.
- Safer status-bar/QS/notification density behaviour.
- Better runtime diagnostics.
- Documentation and reproducible validation evidence.
- Roadmap items once the current target implementation has been verified.

## Out of scope by default

Pixel Dense UI is not intended to grow broad theming, battery-style collections, colour engines, root-hiding features, unrelated privacy bypasses, large launcher customisation suites or duplicated features that are better maintained elsewhere.

A new feature should have a clear connection to the project's core goal: **denser, cleaner, practical Pixel SystemUI/Launcher behaviour with a small hook surface**.

## Engineering expectations

Please preserve these invariants:

- register hooks against **declared methods only**;
- keep optional/private targets fail-soft;
- isolate feature-family failures;
- retain the physical display cutout;
- avoid global resource/inset spoofing when a narrow runtime hook works;
- avoid broad hooks in main SystemUI when a child-process-specific hook is sufficient;
- do not silently broaden a target when an expected class/method disappears;
- preserve deterministic preference defaults;
- keep release/signing secrets out of source and logs.

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the full maintainer rules.

## Tests and CI

At minimum, changes should pass:

```bash
bash scripts/verify.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

GitHub Actions is the authoritative clean-environment gate and additionally checks release assembly/signing-path integrity.

For private SystemUI hooks, compile/lint success is **not** sufficient: include the physical/runtime validation performed, or clearly mark what remains unverified.

## Reporting OTA regressions

A useful regression report includes:

- Pixel model/codename;
- Android build number and security patch;
- Pixel Dense UI version;
- Xposed/libxposed runtime and API level;
- exact feature/settings involved;
- whether overlapping SystemUI modules were disabled;
- relevant logs;
- reproducible steps;
- whether disabling Pixel Dense UI restores stock behaviour.

## Upstream code and attribution

Pixel Dense UI intentionally learns from and adapts open-source projects such as PixelXpert, Pixel Taskbar Enabler, Iconify, libxposed and AOSP.

If a contribution copies or materially adapts an implementation pattern:

1. verify licence compatibility;
2. preserve required copyright/licence notices;
3. document the source and adapted feature in [docs/UPSTREAM.md](docs/UPSTREAM.md);
4. update [NOTICE.md](NOTICE.md) when introducing a newly referenced project;
5. never present an upstream technique as an original Pixel Dense UI invention.

## Pull request checklist

- [ ] Scope is focused and unrelated refactoring is avoided.
- [ ] Target classes/resources are verified against the relevant Android build or reliable upstream source.
- [ ] Missing targets fail soft.
- [ ] Tests/verification cover the changed behaviour where practical.
- [ ] `scripts/verify.sh` passes.
- [ ] Android unit/lint/build CI passes.
- [ ] Runtime validation is documented, or explicitly marked pending.
- [ ] New upstream/reference material is attributed.
- [ ] No APKs, keystores, credentials, logs or generated build residue are committed.

## Licence

By contributing, you agree that your contribution can be distributed under this repository's **GPL-3.0-only** licence, subject to any compatible upstream notices that also apply.
