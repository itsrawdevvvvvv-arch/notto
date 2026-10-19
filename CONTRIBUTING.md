# Contributing to Notto

Thanks for wanting to help! Notto is a small project and every contribution — code, docs, design, bug reports — is welcome.

## Ways to contribute

- **Report a bug** — open an issue using the bug report template.
- **Request a feature** — open an issue using the feature request template.
- **Send a pull request** — fix a bug, improve the UI, or add a feature.
- **Improve docs** — README, `docs/`, screenshots.

## Getting started

1. Fork the repository and clone your fork.
2. Create a branch: `git checkout -b feature/my-change`.
3. Make your change and build it (see below).
4. Test on a device or emulator.
5. Open a pull request against `main` with a clear description.

## Building

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK
```

Full details, including a Gradle-free build path, are in [`docs/BUILDING.md`](docs/BUILDING.md).

## Guidelines

- **Keep it small.** Notto has no third-party runtime dependencies. Prefer the Android framework and vanilla JavaScript.
- **Stay offline.** Do not add network calls or analytics.
- **Match the style.** The UI lives in `app/src/main/assets/index.html`; keep the existing design language (dark, tactile, rounded).
- **Java 17**, Android `minSdk 24`, `targetSdk 34`.
- One logical change per pull request. Describe *what* and *why*.

## Commit messages

Use short, imperative subjects, e.g. `fix: keep sketch strokes after rotate`. Conventional-commit prefixes (`feat:`, `fix:`, `docs:`, `chore:`) are appreciated but not required.

## Code of Conduct

By participating you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).
