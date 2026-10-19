#!/usr/bin/env bash
#
# Build a signed Notto APK without Gradle, using the Android SDK build-tools.
#
# Requirements:
#   - JDK 17 on PATH (or JAVA_HOME set)
#   - Android SDK with build-tools;34.0.0 and platforms;android-34
#   - ANDROID_HOME (or ANDROID_SDK_ROOT) pointing at the SDK
#
# Usage:
#   export ANDROID_HOME=/path/to/android-sdk
#   export NOTTO_KS_PASS="a-keystore-password"   # creates notto-release.jks on first run
#   ./scripts/build_apk.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then echo "ERROR: set ANDROID_HOME"; exit 1; fi

BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"
if [ ! -x "$BT/aapt2" ]; then echo "ERROR: build-tools 34.0.0 not found at $BT"; exit 1; fi
if [ ! -f "$PLATFORM" ]; then echo "ERROR: android-34 platform not found"; exit 1; fi

export JAVA_HOME="${JAVA_HOME:-}"
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$PATH"

SRC="$ROOT/app/src/main"
BUILD="$ROOT/build"
KS="${NOTTO_KEYSTORE:-$ROOT/notto-release.jks}"
KS_PASS="${NOTTO_KS_PASS:-}"
KS_ALIAS="${NOTTO_KEY_ALIAS:-notto}"

VERSION_CODE=1
VERSION_NAME="1.0.0"
PKG="com.notto.app"

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"

echo "==> aapt2 compile resources"
"$BT/aapt2" compile --dir "$SRC/res" -o "$BUILD/res.zip"

echo "==> prepare manifest (aapt2 needs a package attribute; Gradle uses namespace)"
python3 - "$SRC/AndroidManifest.xml" "$BUILD/AndroidManifest.xml" "$PKG" <<'EOF'
import sys
src, dst, pkg = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(src).read()
s = s.replace('<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
              '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="%s">' % pkg, 1)
open(dst, "w").write(s)
EOF

echo "==> aapt2 link"
"$BT/aapt2" link \
  -o "$BUILD/base.apk" \
  -I "$PLATFORM" \
  --manifest "$BUILD/AndroidManifest.xml" \
  -A "$SRC/assets" \
  --java "$BUILD/gen" \
  --min-sdk-version 24 \
  --target-sdk-version 34 \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  "$BUILD/res.zip"

echo "==> javac"
find "$SRC/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
javac -source 8 -target 8 -nowarn -bootclasspath "$PLATFORM" -classpath "$PLATFORM" \
  -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "==> d8 (dex)"
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
"$BT/d8" --release --min-api 24 --lib "$PLATFORM" --output "$BUILD/dex" @"$BUILD/classes.txt"

echo "==> package dex"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
( cd "$BUILD/dex" && zip -q -X "$BUILD/unsigned.apk" classes.dex )

echo "==> zipalign"
"$BT/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

echo "==> sign"
if [ ! -f "$KS" ]; then
  if [ -z "$KS_PASS" ]; then echo "ERROR: keystore not found and NOTTO_KS_PASS unset"; exit 1; fi
  keytool -genkeypair -v -keystore "$KS" -alias "$KS_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=Notto, OU=Notto, O=Notto, C=IN" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-key-alias "$KS_ALIAS" \
  --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --out "$BUILD/notto-$VERSION_NAME.apk" "$BUILD/aligned.apk"

echo "==> verify"
"$BT/apksigner" verify --print-certs "$BUILD/notto-$VERSION_NAME.apk" | head -3
ls -la "$BUILD/notto-$VERSION_NAME.apk"
echo "OK"
