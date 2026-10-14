#!/data/data/com.termux/files/usr/bin/bash
# lemuroid-saves.sh — backup/restore Lemuroid (free build) saves via wifi adb.
#
#   lemuroid-saves.sh pull      backup device -> ~/backups/lemuroid
#   lemuroid-saves.sh push      restore backup -> device
#   lemuroid-saves.sh status    show what's on device and in the local backup
#
# Covered: saves/ (.srm in-game saves), states/ + state-previews/ (save
# states), system/ (BIOS files, pulled via run-as from the internal dir).
# Cores are re-downloadable and ROMs are your own files — not backed up.
#
# Restore-to-new-device: install the same APK (debug build), run `push`.
# App must be debuggable (debug build) for the BIOS part; saves/states
# work for release builds too.

set -euo pipefail

DEVICE=100.71.118.48:46883
PKG=com.swordfish.lemuroid.debug
BACKUP=${LEMUROID_BACKUP_DIR:-$HOME/storage/downloads/LemuroidSaves}
EXT=/storage/emulated/0/Android/data/$PKG

say() { printf '\033[1;32m==>\033[0m %s\n' "$*"; }

connect() {
    adb connect "$DEVICE" >/dev/null 2>&1 || true
    adb -s "$DEVICE" get-state >/dev/null 2>&1 || {
        echo "device $DEVICE not reachable; run: adb connect $DEVICE" >&2
        exit 1
    }
}

# ---- device -> local ------------------------------------------------------
pull_ext() {  # pull_ext <subdir>
    if ! adb -s "$DEVICE" shell "test -d $EXT/files/$1"; then
        say "files/$1 not on device yet, skipping"
        return 0
    fi
    say "pulling files/$1"
    mkdir -p "$BACKUP/files"
    adb -s "$DEVICE" exec-out tar cf - -C "$EXT" "files/$1" \
        | tar xf - -C "$BACKUP"
}

pull_internal_system() {
    say "pulling files/system (BIOS, via run-as)"
    mkdir -p "$BACKUP/internal"
    adb -s "$DEVICE" exec-out run-as "$PKG" tar cf - -C files system \
        | tar xf - -C "$BACKUP/internal"
}

# ---- local -> device ------------------------------------------------------
push_ext() {  # push_ext <subdir>
    [ -d "$BACKUP/files/$1" ] || { say "no local backup of files/$1, skipping"; return; }
    say "pushing files/$1"
    adb -s "$DEVICE" push "$BACKUP/files/$1" "$EXT/files/" >/dev/null
}

push_internal_system() {
    [ -d "$BACKUP/internal/system" ] || { say "no local BIOS backup, skipping"; return; }
    say "pushing files/system (BIOS, via run-as)"
    tar cf "$BACKUP/system.tar" -C "$BACKUP/internal" system
    adb -s "$DEVICE" push "$BACKUP/system.tar" /data/local/tmp/lemuroid-system.tar >/dev/null
    adb -s "$DEVICE" shell "run-as $PKG sh -c 'mkdir -p files && tar xf /data/local/tmp/lemuroid-system.tar -C files'"
    adb -s "$DEVICE" shell rm -f /data/local/tmp/lemuroid-system.tar
    rm -f "$BACKUP/system.tar"
}

# ---- status ---------------------------------------------------------------
fmt() { printf '%-16s %8s files  %s\n' "$1" "$2" "$3"; }

status() {
    connect
    say "device ($DEVICE)"
    for d in saves states state-previews; do
        n=$(adb -s "$DEVICE" shell "find $EXT/files/$d -type f 2>/dev/null | wc -l" | tr -d '[:space:]')
        s=$(adb -s "$DEVICE" shell "du -sh $EXT/files/$d 2>/dev/null | cut -f1" | tr -d '[:space:]')
        fmt "files/$d" "${n:-0}" "${s:-empty}"
    done
    n=$(adb -s "$DEVICE" shell "run-as $PKG sh -c 'find files/system -type f 2>/dev/null | wc -l'" | tr -d '[:space:]')
    s=$(adb -s "$DEVICE" shell "run-as $PKG sh -c 'du -sh files/system 2>/dev/null | cut -f1'" | tr -d '[:space:]')
    fmt "files/system" "${n:-0}" "${s:-empty}"
    echo
    say "local backup ($BACKUP)"
    [ -d "$BACKUP" ] || { echo "  (no backup yet — run: $0 pull)"; exit 0; }
    for d in files/saves files/states files/state-previews internal/system; do
        if [ -d "$BACKUP/$d" ]; then
            n=$(find "$BACKUP/$d" -type f | wc -l | tr -d '[:space:]')
            s=$(du -sh "$BACKUP/$d" | cut -f1)
            fmt "$d" "$n" "$s"
        fi
    done
}

case "${1:-}" in
    pull)
        connect
        pull_ext saves
        pull_ext states
        pull_ext state-previews
        pull_internal_system
        say "backup complete -> $BACKUP"
        ;;
    push)
        connect
        push_ext saves
        push_ext states
        push_ext state-previews
        push_internal_system
        say "restore complete"
        ;;
    status) status ;;
    *) sed -n '2,8p' "$0"; exit 1 ;;
esac
