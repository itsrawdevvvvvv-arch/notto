# Building Notto

Notto is a standard Android project. You can build it with Gradle (recommended)
or, if you prefer not to pull Gradle and the Android Gradle Plugin, with a small
script that drives the Android SDK build-tools directly.

## Prerequisites

- **JDK 17** (Temurin, or any JDK 17+).
- **Android SDK** with:
  - `platforms;android-34`
  - `build-tools;34.0.0`
  - `platform-tools`
- Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to your SDK location.

You can install the SDK packages with `sdkmanager`:

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

## Gradle build (recommended)

```bash
git clone https://github.com/itsrawdevvvvvv-arch/notto.git
cd notto

./gradlew assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # -> app/build/outputs/apk/release/app-release-unsigned.apk
```

### Signing a release build

Create a keystore once:

```bash
keytool -genkeypair -v -keystore notto-release.jks -alias notto \
  -keyalg RSA -keysize 2048 -validity 10950
```

Then expose it to Gradle through environment variables:

```bash
export NOTTO_KEYSTORE="$PWD/notto-release.jks"
export NOTTO_STORE_PASSWORD="your-store-password"
export NOTTO_KEY_ALIAS="notto"
export NOTTO_KEY_PASSWORD="your-key-password"
./gradlew assembleRelease
```

`app/build.gradle` reads those variables and signs the release build when
`NOTTO_KEYSTORE` is set.

## Gradle-free build

`scripts/build_apk.sh` builds, aligns and signs an APK using `aapt2`, `javac`,
`d8`, `zipalign` and `apksigner` from the SDK build-tools — no Gradle, no
downloads.

```bash
export ANDROID_HOME=/path/to/android-sdk
export NOTTO_KS_PASS="a-keystore-password"   # first run creates notto-release.jks
./scripts/build_apk.sh
```

The signed APK is written to `build/notto-<version>.apk`.

## Automated releases

`.github/workflows/release.yml` builds a signed APK whenever a `v*` tag is
pushed, and attaches it (plus a SHA-256 checksum) to the GitHub Release. To
enable automatic signing, add these repository secrets:

| Secret | Value |
| --- | --- |
| `NOTTO_KEYSTORE_BASE64` | `base64 -w0 notto-release.jks` |
| `NOTTO_STORE_PASSWORD` | keystore password |
| `NOTTO_KEY_ALIAS` | key alias (e.g. `notto`) |
| `NOTTO_KEY_PASSWORD` | key password |

Without these secrets the workflow still builds the APK and uploads it as a
workflow artifact, but leaves the Release untouched.

## Project notes

- `minSdk 24`, `targetSdk 34`, `compileSdk 34`.
- Java 17, no third-party dependencies, no AndroidX.
- The entire UI is `app/src/main/assets/index.html`; fonts are bundled under
  `app/src/main/assets/fonts/`.
