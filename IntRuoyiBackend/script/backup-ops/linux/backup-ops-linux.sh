#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
selected_mode=''
previous=''
for argument in "$@"; do
    if [[ "$previous" == '--mode' ]]; then selected_mode="$argument"; fi
    case "$argument" in --mode=*) selected_mode="${argument#--mode=}" ;; esac
    previous="$argument"
done
case "$selected_mode" in
    formal-capture|formal-replica-export|formal-object-restore|formal-sql-restore)
        for name in INTRUOYI_NATIVE_RUNTIME_ARTIFACT INTRUOYI_NATIVE_RUNTIME_NOTICES INTRUOYI_NATIVE_MACHINE_ID INTRUOYI_NATIVE_CONTROL_DEVICE INTRUOYI_NATIVE_SOURCE_DEVICE; do
            if [[ -z "${!name:-}" ]]; then echo NATIVE_RUNTIME_CONFIGURATION_REQUIRED >&2; exit 1; fi
        done
        if [[ "${PYTHON_BIN:-}" != '/var/lib/intruoyi-native-runtime/20250902/python/bin/python3' ]]; then
            echo NATIVE_RUNTIME_EXECUTABLE_BINDING_REQUIRED >&2; exit 1
        fi
        # The selected private interpreter and all packaged files are checked
        # before any formal request or credentials are read.
        if runtime_result=$(sh "$SCRIPT_DIR/formal_native_runtime.sh" verify "$INTRUOYI_NATIVE_RUNTIME_ARTIFACT" \
            "$INTRUOYI_NATIVE_MACHINE_ID" "$INTRUOYI_NATIVE_CONTROL_DEVICE" "$INTRUOYI_NATIVE_SOURCE_DEVICE" "$INTRUOYI_NATIVE_RUNTIME_NOTICES"); then
            unset runtime_result
        else
            printf '%s\n' "$runtime_result" >&2
            exit 1
        fi
        exec "$PYTHON_BIN" -B -E -s "$SCRIPT_DIR/backup_ops_linux.py" "$@"
        ;;
esac
PYTHON_BIN="${PYTHON_BIN:-python3}"

exec "$PYTHON_BIN" "$SCRIPT_DIR/backup_ops_linux.py" "$@"
