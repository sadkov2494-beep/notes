#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
APK_SRC="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_NAME="notes-vault-debug.apk"
ARTIFACTS_DIR="/opt/cursor/artifacts"

mkdir -p "$SDK_DIR"
if [ ! -f "$ROOT_DIR/local.properties" ]; then
  echo "sdk.dir=$SDK_DIR" > "$ROOT_DIR/local.properties"
fi

if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Installing Android command line tools..."
  TMP_ZIP="/tmp/android-cmdline-tools.zip"
  curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$TMP_ZIP"
  mkdir -p "$SDK_DIR/cmdline-tools/latest"
  unzip -qo "$TMP_ZIP" -d "$SDK_DIR/cmdline-tools/latest"
  mv "$SDK_DIR/cmdline-tools/latest/cmdline-tools"/* "$SDK_DIR/cmdline-tools/latest/" || true
  rm -rf "$SDK_DIR/cmdline-tools/latest/cmdline-tools" "$TMP_ZIP"
fi

export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

yes | sdkmanager --licenses >/dev/null
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

cd "$ROOT_DIR"
chmod +x gradlew
./gradlew assembleDebug --no-daemon

mkdir -p "$ARTIFACTS_DIR"
cp "$APK_SRC" "$ARTIFACTS_DIR/$APK_NAME"
cp "$APK_SRC" "$ROOT_DIR/$APK_NAME"

echo "APK ready:"
echo "  $ARTIFACTS_DIR/$APK_NAME"
echo "  $ROOT_DIR/$APK_NAME"
