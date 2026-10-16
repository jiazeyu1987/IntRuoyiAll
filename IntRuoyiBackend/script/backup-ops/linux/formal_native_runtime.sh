#!/bin/sh
# Private pinned preparation only. Failure retains staging; no system replacement.
set -eu
ROOT=/var/lib/intruoyi-native-runtime
VERSION=20250902
EXPECTED_SHA=49b3c31d7c51bfb469d6ff77db1b800cbdd7aaa11fafb9af0b517d9aa129b48b
EXPECTED_NOTICES_SHA=6bea71af33a7c00c9f61609d839b8927fb1a2204aef26347fd836114b7202772
fail() { echo "$1"; exit 1; }
[ "$#" -eq 6 ] || fail NATIVE_RUNTIME_ARGUMENTS_INVALID
action=$1; artifact=$2; expected_machine=$3; control_device=$4; source_device=$5; notices=$6
case "$action" in install|verify) ;; *) fail NATIVE_RUNTIME_ACTION_INVALID ;; esac
[ -f "$artifact" ] && [ ! -L "$artifact" ] || fail NATIVE_RUNTIME_ARTIFACT_MISSING
[ -f "$notices" ] && [ ! -L "$notices" ] || fail NATIVE_RUNTIME_NOTICES_MISSING
[ "$(sha256sum "$notices" | awk '{print $1}')" = "$EXPECTED_NOTICES_SHA" ] || fail NATIVE_RUNTIME_NOTICES_CHANGED
[ -d "$ROOT" ] && [ ! -L "$ROOT" ] || fail NATIVE_RUNTIME_CONTROL_MISSING
for directory in / /var /var/lib "$ROOT"; do
    [ -d "$directory" ] && [ ! -L "$directory" ] || fail NATIVE_RUNTIME_CONTROL_UNSAFE
    [ "$(stat -c %u "$directory")" = 0 ] || fail NATIVE_RUNTIME_CONTROL_UNSAFE
    mode=$(stat -c %a "$directory")
    [ "$((0$mode & 0022))" -eq 0 ] || fail NATIVE_RUNTIME_CONTROL_UNSAFE
done
[ "$(stat -c %a "$ROOT")" = 700 ] || fail NATIVE_RUNTIME_CONTROL_PRIVATE_REQUIRED
[ "$(cat /etc/machine-id)" = "$expected_machine" ] || fail NATIVE_RUNTIME_MACHINE_CHANGED
[ "$control_device" != "$source_device" ] || fail NATIVE_RUNTIME_CONTROL_ON_SOURCE_DEVICE
[ "$(stat -c %d "$ROOT")" = "$control_device" ] || fail NATIVE_RUNTIME_CONTROL_DEVICE_CHANGED
[ "$(sha256sum "$artifact" | awk '{print $1}')" = "$EXPECTED_SHA" ] || fail NATIVE_RUNTIME_ARTIFACT_CHANGED
target="$ROOT/$VERSION"
if [ "$action" = install ]; then
    [ ! -e "$target" ] && [ ! -L "$target" ] || fail NATIVE_RUNTIME_ALREADY_INSTALLED
    # Atomic directory claim; any interruption prevents implicit install retries.
    (umask 077; mkdir "$target") || fail NATIVE_RUNTIME_INSTALL_CLAIM_FAILED
    tar -xzf "$artifact" -C "$target" || fail NATIVE_RUNTIME_EXTRACT_FAILED_RETAINED
    tar -xzf "$notices" -C "$target" || fail NATIVE_RUNTIME_NOTICES_EXTRACT_FAILED_RETAINED
fi
[ -d "$target" ] && [ ! -L "$target" ] || fail NATIVE_RUNTIME_INSTALL_MISSING
# Check every packaged file before executing installed Python, including stdlib
# and native libraries. Do not accept an unchanged executable with changed deps.
tar --compare -zf "$artifact" -C "$target" >/dev/null 2>&1 || fail NATIVE_RUNTIME_INSTALLED_FILES_CHANGED
tar --compare -zf "$notices" -C "$target" >/dev/null 2>&1 || fail NATIVE_RUNTIME_INSTALLED_NOTICES_CHANGED
[ -x "$target/python/bin/python3" ] || fail NATIVE_RUNTIME_EXECUTABLE_MISSING
"$target/python/bin/python3" -I -B -S - "$action" "$target/runtime-receipt.json" "$expected_machine" "$EXPECTED_SHA" "$EXPECTED_NOTICES_SHA" <<'PY'
import hashlib,json,os,sys,time,platform,subprocess
action,path,machine,archive_sha,notices_sha=sys.argv[1:]
assert sys.version_info[:3]==(3,12,11)
assert platform.machine()=='x86_64' and os.name=='posix'
assert type(getattr(time,'CLOCK_BOOTTIME',None)) is int
assert time.clock_gettime(time.CLOCK_BOOTTIME)>0
assert type(getattr(os,'O_NOFOLLOW',None)) is int and type(getattr(os,'O_DIRECTORY',None)) is int
subprocess.run(['/bin/true'],check=True,timeout=5)
with open(sys.executable,'rb') as f: executable_sha=hashlib.file_digest(f,'sha256').hexdigest()
data={'schemaVersion':'intruoyi-native-runtime/v1','version':'20250902','python':platform.python_version(),
 'machineId':machine,'archiveSha256':archive_sha,'noticesArchiveSha256':notices_sha,'executableSha256':executable_sha,
 'boottime':True,'noFollow':True,'directoryFlag':True,'productionEvidence':False}
if action=='install':
    fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
    with os.fdopen(fd,'w',encoding='utf-8') as f:
        json.dump(data,f,separators=(',',':'));f.write('\n');f.flush();os.fsync(f.fileno())
    directory=os.open(os.path.dirname(path),os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW)
    try:os.fsync(directory)
    finally:os.close(directory)
else:
    fd=os.open(path,os.O_RDONLY|os.O_NOFOLLOW)
    with os.fdopen(fd,'r',encoding='utf-8') as f:assert json.load(f)==data
print(json.dumps(data,separators=(',',':')))
PY
