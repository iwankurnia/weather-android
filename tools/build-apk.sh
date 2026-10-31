#!/bin/zsh
# Builds a fresh, installable APK. Every run bumps the build number, so no two APKs share a
# version and the phone always accepts the new one as an update.
#
#   tools/build-apk.sh
#
# In order: refuse a dirty tree, run the unit tests (a number is never burned on a broken
# build), bump buildNumber, assemble as a SECOND Gradle invocation (versionCode is read at
# configuration time), check the packaged versionCode with aapt2, copy the APK to dist/, and
# commit the bump. It does not install or push anything.

set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

say() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }
die() { printf '\033[31merror:\033[0m %s\n' "$1" >&2; exit 1; }

say "Preflight"
if [[ -n "$(git status --porcelain)" ]]; then
    git status --short
    die "working tree is not clean — commit or stash first, so the APK matches a commit"
fi
echo "branch: $(git branch --show-current) @ $(git log -1 --format='%h %s')"

say "Tests"
./gradlew :app:testDebugUnitTest --console=plain -q

say "Bump build number"
./gradlew :app:bumpBuildNumber --console=plain -q
CODE=$(sed -n 's/^buildNumber=//p' version.properties)
NAME="$(sed -n 's/^marketingVersion=//p' version.properties).$CODE"

say "Build $NAME ($CODE)"
./gradlew :app:assembleDebug --console=plain -q
APK=app/build/outputs/apk/debug/app-debug.apk

AAPT2=$(ls -d "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/aapt2 2>/dev/null | sort | tail -1)
[[ -x "$AAPT2" ]] || die "aapt2 not found under the Android SDK build-tools"
BADGING=$("$AAPT2" dump badging "$APK" | head -1)
echo "$BADGING"
[[ "$BADGING" == *"versionCode='$CODE'"* && "$BADGING" == *"versionName='$NAME'"* ]] \
    || die "APK version does not match version.properties ($NAME / $CODE)"

mkdir -p dist
OUT="dist/weather-$NAME.apk"
cp "$APK" "$OUT"

git add version.properties
git commit -q -m "chore: build $NAME ($CODE)"

say "Done"
echo "APK: $REPO/$OUT"
echo "Install: adb install -r \"$REPO/$OUT\""
