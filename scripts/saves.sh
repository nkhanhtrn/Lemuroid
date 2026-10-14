#!/data/data/com.termux/files/usr/bin/bash
# saves.sh — backup the internal (non-syncable) Lemuroid data via wifi adb.
#
#   saves.sh pull      backup device BIOS -> Download/EmuSaves/internal-system
#   saves.sh push      restore BIOS backup -> device
#   saves.sh status    inventory of the synced EmuSaves tree + device BIOS
#
# saves/ states/ state-previews/ are written by the app DIRECTLY into
# Download/EmuSaves (see DirectoriesManager.kt), so they sync live with
# Syncthing/adb/MTP — this script only handles files/system (BIOS), which
# must stay in the app's private dir. App must be a debug build for run-as.

set -euo pipefail

DEVICE=${LEMUROID_DEVICE:-100.71.118.48:46883}
PKG=com.swordfish.lemuroid.debug
BACKUP=${LEMUROID_BACKUP_DIR:-$HOME/storage/downloads/EmuSaves}

say() { printf '\033[1;32m==>\033[0m %s\n' "$*"; }

connect() {
    adb connect "$DEVICE" >/dev/null 2>&1 || true
    adb -s "$DEVICE" get-state >/dev/null 2>&1 || {
        echo "device $DEVICE not reachable; run: adb connect $DEVICE" >&2
        exit 1
    }
}

pull_bios() {
    say "pulling files/system (BIOS, via run-as)"
    mkdir -p "$BACKUP/internal-system"
    adb -s "$DEVICE" exec-out run-as "$PKG" tar cf - -C files system \
        | tar xf - -C "$BACKUP/internal-system"
}

push_bios() {
    [ -d "$BACKUP/internal-system/system" ] || { say "no BIOS backup, skipping"; return; }
    say "pushing files/system (BIOS, via run-as)"
    tar cf "$BACKUP/system.tar" -C "$BACKUP/internal-system" system
    adb -s "$DEVICE" push "$BACKUP/system.tar" /data/local/tmp/emu-system.tar >/dev/null
    adb -s "$DEVICE" shell "run-as $PKG sh -c 'mkdir -p files && tar xf /data/local/tmp/emu-system.tar -C files'"
    adb -s "$DEVICE" shell rm -f /data/local/tmp/emu-system.tar
    rm -f "$BACKUP/system.tar"
}

fmt() { printf '  %-18s %6s files  %s\n' "$1" "$2" "$3"; }

status() {
    connect
    say "synced tree ($BACKUP)"
    [ -d "$BACKUP" ] || { echo "  (nothing yet — the app creates it on first save)"; }
    for d in saves states state-previews internal-system; do
        [ -d "$BACKUP/$d" ] || continue
        n=$(find "$BACKUP/$d" -type f ! -name '.write_probe' 2>/dev/null | wc -l | tr -d ' ')
        s=$(du -sh "$BACKUP/$d" 2>/dev/null | cut -f1)
        fmt "$d" "$n" "$s"
    done
    echo
    say "device internal ($PKG)"
    n=$(adb -s "$DEVICE" shell "run-as $PKG sh -c 'find files/system -type f 2>/dev/null | wc -l'" | tr -d '[:space:]')
    s=$(adb -s "$DEVICE" shell "run-as $PKG sh -c 'du -sh files/system 2>/dev/null | cut -f1'" | tr -d '[:space:]')
    fmt "files/system" "${n:-0}" "${s:-empty}"
}

case "${1:-}" in
    pull)
        connect
        pull_bios
        say "backup complete -> $BACKUP/internal-system"
        ;;
    push)
        connect
        push_bios
        say "restore complete"
        ;;
    status) status ;;
    *) sed -n '2,12p' "$0"; exit 1 ;;
esac
