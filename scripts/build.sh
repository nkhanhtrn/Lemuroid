#!/data/data/com.termux/files/usr/bin/bash
# build.sh — build the Lemuroid freeDynamic APK on-device and install it
# over wifi adb, then relaunch it.
#
#   build.sh              debug build + install + launch
#   build.sh --no-launch  skip the relaunch
#   build.sh --no-install just build
#
# Requirements (one-time, already done in ~/android-dev):
#   sdk/ hybrid SDK at ~/android-dev/sdk, Termux aapt2 override and tuned
#   memory settings live in gradle.properties; JDK 17.

set -euo pipefail

cd "$(dirname "$0")/.."

DEVICE=${LEMUROID_DEVICE:-100.71.118.48:46883}
PKG=com.swordfish.lemuroid.debug
VARIANT=${VARIANT:-FreeDynamicDebug}
APK_GLOB="lemuroid-app/build/outputs/apk/freeDynamic/debug/*.apk"
export JAVA_HOME=${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk}

LAUNCH=1
for arg in "$@"; do
    case "$arg" in
        --no-launch) LAUNCH=0 ;;
        --no-install) LAUNCH=0; NO_INSTALL=1 ;;
    esac
done

say() { printf '\033[1;32m==>\033[0m %s\n' "$*"; }

say "building :lemuroid-app:assemble$VARIANT (JDK $(basename "$JAVA_HOME"))"
t0=$SECONDS
./gradlew ":lemuroid-app:assemble$VARIANT" --console=plain \
    | grep -Ev '^\s*$|Unzip|Downloading|Download|^w: ' | tail -25
say "build finished in $((SECONDS - t0))s"

APK=$(ls $APK_GLOB)
say "apk: $APK ($(du -h "$APK" | cut -f1))"

[ -n "${NO_INSTALL:-}" ] && exit 0

SHARE=${LEMUROID_APK_DIR:-$HOME/storage/downloads}
if [ -d "$SHARE" ]; then
    cp -f "$APK" "$SHARE/lemuroid-free-dynamic-debug.apk"
    say "copied apk to $SHARE/lemuroid-free-dynamic-debug.apk (for syncthing)"
fi

say "installing on $DEVICE"
adb connect "$DEVICE" >/dev/null
adb -s "$DEVICE" install -r "$APK"

if [ "$LAUNCH" = 1 ]; then
    say "launching"
    adb -s "$DEVICE" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
fi

say "done"
