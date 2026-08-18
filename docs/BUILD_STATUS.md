# Build / verification status

Snapshot date: 2026-08-19.

Completed in the generation environment:

- repository structure verification;
- Bash syntax and reliability-policy review for `scripts/verify.sh`;
- XML well-formedness checks;
- Java parser-level syntax scan (dependency resolution intentionally unavailable locally);
- exact class/resource target cross-check against the supplied SystemUI APK string tables;
- GPL/SPDX header and provenance checks;
- archive integrity check.

Not performed locally:

- Android Gradle compilation, because this execution environment does not provide an Android SDK or Gradle installation.

`.github/workflows/build.yml` performs the authoritative Android SDK/AGP compile on GitHub using JDK 17 and Gradle 9.3.1. A successful CI build is required before treating v0.1 as installable.
