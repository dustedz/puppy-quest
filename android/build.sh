#!/usr/bin/env bash
# Builds Puppy Quest for Android from ../../dog-game.
#   ./build.sh          -> out/puppy-quest-debug.apk (install on your phone) + out/puppy-quest-release-unsigned.aab
#   RELEASE_KEYSTORE=path RELEASE_ALIAS=upload ./build.sh  -> also out/puppy-quest-release.aab, signed for Google Play
# Optional: VERSION_NAME=1.0.3 VERSION_CODE=4 override the manifest's version (the GitHub release build sets these).
#   SIDELOAD_KEYSTORE / SIDELOAD_PASS / SIDELOAD_ALIAS pick the key the phone APK is signed with
#   (default keys/debug.keystore; every APK must use the same key so updates install over the old one).
# Needs (Ubuntu/Debian): apt install openjdk-21-jdk-headless aapt apksigner zipalign dalvik-exchange android-sdk-platform-23 zip unzip
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
if [ -z "${GAME:-}" ]; then
  if [ -d "$HERE/../game" ]; then GAME="$HERE/../game"; else GAME="$HERE/../../dog-game"; fi
fi
SDK=/usr/lib/android-sdk
JAR="$SDK/platforms/android-23/android.jar"
BT="$SDK/build-tools/debian"
BUNDLETOOL_VER=1.17.2
CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/puppy-quest-android"
B="$(mktemp -d)"; trap 'rm -rf "$B"' EXIT
OUT="$HERE/out"; mkdir -p "$OUT" "$CACHE"

echo "1/6 Packing the game for offline play"
W="$B/assets/www"; mkdir -p "$W/fonts"
cp -r "$GAME/nature" "$W/nature"
cp "$HERE/vendor/three.min.js" "$HERE/vendor/android-bridge.js" "$W/"
cp "$HERE"/vendor/fonts/*.woff2 "$HERE/vendor/fonts/fonts.css" "$W/fonts/"
python3 - "$GAME/index.html" "$W/index.html" <<'PY'
import re, sys
src = open(sys.argv[1], encoding="utf-8").read()
n0 = len(src)
src, a = re.subn(r'<script src="https://cdnjs\.cloudflare\.com/ajax/libs/three\.js/r128/three\.min\.js"></script>', '<script src="three.min.js"></script>', src)
src = re.sub(r'<link rel="preconnect"[^>]*>\s*', '', src)
src, b = re.subn(r'<link href="https://fonts\.googleapis\.com/css2[^"]*" rel="stylesheet">', '<link href="fonts/fonts.css" rel="stylesheet">', src)
# The game file is published as a page fragment (the artifact host adds the page shell),
# so the app adds its own shell: charset, phone viewport, and the Android bridge first.
shell = ('<meta charset="utf-8">\n<meta name="viewport" content="width=device-width,initial-scale=1,'
         'maximum-scale=1,user-scalable=no,viewport-fit=cover">\n<script src="android-bridge.js"></script>\n')
if re.search(r'<head[^>]*>', src, re.I):
    src, c = re.subn(r'(<head[^>]*>)', lambda m: m.group(1) + '\n' + shell, src, count=1, flags=re.I)
else:
    src, c = '<!doctype html>\n' + shell + src, 1
if (a, b, c) != (1, 1, 1):
    sys.exit(f"index.html changed shape (three={a} fonts={b} head={c}); update build.sh")
left = re.findall(r'(?:src|href)="(https?://[^"]+)"', src)
if left: sys.exit(f"index.html still loads from the internet: {left}")
open(sys.argv[2], "w", encoding="utf-8").write(src)
PY

MANIFEST="$B/AndroidManifest.xml"
cp "$HERE/src/AndroidManifest.xml" "$MANIFEST"
[ -n "${VERSION_NAME:-}" ] && sed -i "s/android:versionName=\"[^\"]*\"/android:versionName=\"$VERSION_NAME\"/" "$MANIFEST"
[ -n "${VERSION_CODE:-}" ] && sed -i "s/android:versionCode=\"[^\"]*\"/android:versionCode=\"$VERSION_CODE\"/" "$MANIFEST"
grep -oE 'android:version(Code|Name)="[^"]*"' "$MANIFEST" | tr '\n' ' '; echo

echo "2/6 Compiling Java"
mkdir -p "$B/classes"
javac -nowarn -Xlint:-options --release 8 -cp "$JAR" -d "$B/classes" $(find "$HERE/src/java" -name '*.java')
"$BT/dx" --dex --min-sdk-version=24 --output="$B/classes.dex" "$B/classes"

echo "3/6 Linking resources"
"$BT/aapt2" compile --dir "$HERE/src/res" -o "$B/res.zip"
"$BT/aapt2" link -I "$JAR" --manifest "$MANIFEST" -A "$B/assets" "$B/res.zip" -o "$B/app.apk" --auto-add-overlay
"$BT/aapt2" link -I "$JAR" --manifest "$MANIFEST" -A "$B/assets" "$B/res.zip" -o "$B/proto.apk" --auto-add-overlay --proto-format

echo "4/6 Building and signing the debug APK"
(cd "$B" && zip -q -j app.apk classes.dex)
"$BT/zipalign" -f -p 4 "$B/app.apk" "$B/aligned.apk"
KS="${SIDELOAD_KEYSTORE:-$HERE/keys/debug.keystore}"
KS_PASS="${SIDELOAD_PASS:-android}"
KS_ALIAS="${SIDELOAD_ALIAS:-androiddebugkey}"
if [ ! -f "$KS" ]; then
  [ -n "${SIDELOAD_KEYSTORE:-}" ] && { echo "Signing key $KS not found"; exit 1; }
  mkdir -p "$HERE/keys"
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
    -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
fi
apksigner sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" --ks-key-alias "$KS_ALIAS" \
  --min-sdk-version 24 --v4-signing-enabled false --out "$OUT/puppy-quest-debug.apk" "$B/aligned.apk"
apksigner verify "$OUT/puppy-quest-debug.apk"

echo "5/6 Building the Play Store bundle (.aab)"
BTJ="$CACHE/bundletool-all-$BUNDLETOOL_VER.jar"
[ -f "$BTJ" ] || curl -fsSL -o "$BTJ" "https://github.com/google/bundletool/releases/download/$BUNDLETOOL_VER/bundletool-all-$BUNDLETOOL_VER.jar"
M="$B/module"; mkdir -p "$M/manifest" "$M/dex"
(cd "$M" && unzip -q "$B/proto.apk")
mv "$M/AndroidManifest.xml" "$M/manifest/"
cp "$B/classes.dex" "$M/dex/"
(cd "$M" && zip -q -r ../base.zip .)
rm -f "$OUT/puppy-quest-release-unsigned.aab"
java -jar "$BTJ" build-bundle --modules="$B/base.zip" --output="$OUT/puppy-quest-release-unsigned.aab"

echo "6/6 Signing for Google Play"
if [ -n "${RELEASE_KEYSTORE:-}" ]; then
  cp "$OUT/puppy-quest-release-unsigned.aab" "$OUT/puppy-quest-release.aab"
  jarsigner -keystore "$RELEASE_KEYSTORE" -sigalg SHA256withRSA -digestalg SHA-256 \
    "$OUT/puppy-quest-release.aab" "${RELEASE_ALIAS:-upload}"
  echo "Signed: $OUT/puppy-quest-release.aab"
else
  echo "Skipped (no RELEASE_KEYSTORE). Upload key steps: PUBLISHING.md"
fi
ls -la "$OUT"
