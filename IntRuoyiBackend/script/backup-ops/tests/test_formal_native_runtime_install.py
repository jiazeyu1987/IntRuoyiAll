"""Actual shell/runtime installation in isolated task-owned Linux storage."""
import json,subprocess
import pytest
from pathlib import Path

LINUX=Path(__file__).parents[1]/'linux'
ARTIFACT=Path('D:/ProjectPackage/Int/IntRuoyiMaintance/doc/tasks/20260927-three-environment-release/native-runtime-20250902')
IMAGE='centos@sha256:be65f488b7764ad3638f236b7b515b3678369a5124c47b8d32916d6487418ea4'

def test_private_native_install_binding_and_real_capabilities():
    script=LINUX/'formal_native_runtime.sh'
    assert script.is_file(), 'native installer required before controller dispatch'
    # Deliberately selected fixture machine ID; no actual server is accessed.
    command=r"""set -eu
mkdir -m 700 /var/lib/intruoyi-native-runtime
machine=11111111111111111111111111111111
printf '%s\n' "$machine" > /etc/machine-id
control=$(stat -c %d /var/lib/intruoyi-native-runtime)
source=$(stat -c %d /fixtures)
artifact=/fixtures/cpython-3.12.11+20250902-x86_64-unknown-linux-gnu-install_only_stripped.tar.gz
notices=/fixtures/native-runtime-notices-20250902-v1.tar.gz
if sh /source/formal_native_runtime.sh install "$artifact" 22222222222222222222222222222222 "$control" "$source" "$notices"; then exit 21; fi
if sh /source/formal_native_runtime.sh install "$artifact" "$machine" "$control" "$control" "$notices"; then exit 22; fi
[ ! -e /var/lib/intruoyi-native-runtime/20250902 ]
sh /source/formal_native_runtime.sh install "$artifact" "$machine" "$control" "$source" "$notices"
sh /source/formal_native_runtime.sh verify "$artifact" "$machine" "$control" "$source" "$notices"
if sh /source/formal_native_runtime.sh install "$artifact" "$machine" "$control" "$source" "$notices"; then exit 23; fi
export PYTHON_BIN=/var/lib/intruoyi-native-runtime/20250902/python/bin/python3
export INTRUOYI_NATIVE_RUNTIME_ARTIFACT="$artifact"
export INTRUOYI_NATIVE_RUNTIME_NOTICES="$notices"
export INTRUOYI_NATIVE_MACHINE_ID="$machine"
export INTRUOYI_NATIVE_CONTROL_DEVICE="$control"
export INTRUOYI_NATIVE_SOURCE_DEVICE="$source"
if bash /source/backup-ops-linux.sh --mode formal-capture --request /absent --expected-request-sha256 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa >/tmp/entry-result; then exit 26; fi
grep -q FORMAL_ /tmp/entry-result
unset INTRUOYI_NATIVE_MACHINE_ID
if bash /source/backup-ops-linux.sh --mode formal-capture --request /absent --expected-request-sha256 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa >/tmp/missing-result 2>&1; then exit 27; fi
grep -q NATIVE_RUNTIME_CONFIGURATION_REQUIRED /tmp/missing-result
printf '# changed library\n' >> /var/lib/intruoyi-native-runtime/20250902/python/lib/python3.12/platform.py
if sh /source/formal_native_runtime.sh verify "$artifact" "$machine" "$control" "$source" "$notices"; then exit 24; fi
export INTRUOYI_NATIVE_MACHINE_ID="$machine"
if bash /source/backup-ops-linux.sh --mode formal-capture --request /absent --expected-request-sha256 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa >/tmp/drift-result 2>&1; then exit 28; fi
grep -q NATIVE_RUNTIME_INSTALLED_FILES_CHANGED /tmp/drift-result
cat /var/lib/intruoyi-native-runtime/20250902/runtime-receipt.json
"""
    result=subprocess.run(['docker','run','--rm','--network','none','--label','task-owner=native-runtime-install-test',
        '--tmpfs','/var/lib:rw,exec,nosuid,nodev,size=536870912',
        '--mount','type=bind,src='+str(ARTIFACT)+',dst=/fixtures,readonly',
        '--mount','type=bind,src='+str(LINUX.resolve())+',dst=/source,readonly',IMAGE,'sh','-c',command],
        capture_output=True,text=True,timeout=45)
    assert result.returncode==0,result.stdout+result.stderr
    lines=result.stdout.splitlines();assert 'NATIVE_RUNTIME_MACHINE_CHANGED' in lines
    assert 'NATIVE_RUNTIME_CONTROL_ON_SOURCE_DEVICE' in lines
    receipt=json.loads(lines[-1]);assert receipt['python']=='3.12.11'
    assert receipt['boottime'] is True and receipt['machineId']=='1'*32
    assert receipt['archiveSha256']=='49b3c31d7c51bfb469d6ff77db1b800cbdd7aaa11fafb9af0b517d9aa129b48b'
    assert receipt['productionEvidence'] is False
    assert receipt['noticesArchiveSha256']=='6bea71af33a7c00c9f61609d839b8927fb1a2204aef26347fd836114b7202772'

@pytest.mark.parametrize('case',['ancestor-link','root-owner','root-writable'])
def test_unsafe_native_install_location_refused_without_extraction(case):
    setup={
        'ancestor-link':'mkdir -p /actual/lib; mv /var/lib /var/lib-original; ln -s /actual/lib /var/lib; mkdir -m 700 /var/lib/intruoyi-native-runtime',
        'root-owner':'mkdir -m 700 /var/lib/intruoyi-native-runtime; chown 12345 /var/lib/intruoyi-native-runtime',
        'root-writable':'mkdir -m 700 /var/lib/intruoyi-native-runtime; chmod 777 /var'
    }[case]
    command=setup+"""
machine=11111111111111111111111111111111
printf '%s\n' "$machine" > /etc/machine-id
control=$(stat -c %d /var/lib/intruoyi-native-runtime)
source=$(stat -c %d /fixtures)
notices=/fixtures/native-runtime-notices-20250902-v1.tar.gz
if sh /source/formal_native_runtime.sh install /fixtures/cpython-3.12.11+20250902-x86_64-unknown-linux-gnu-install_only_stripped.tar.gz "$machine" "$control" "$source" "$notices"; then exit 25; fi
[ ! -e /var/lib/intruoyi-native-runtime/20250902 ]
"""
    result=subprocess.run(['docker','run','--rm','--network','none','--label','task-owner=native-runtime-location-test',
        '--mount','type=bind,src='+str(ARTIFACT)+',dst=/fixtures,readonly',
        '--mount','type=bind,src='+str(LINUX.resolve())+',dst=/source,readonly',IMAGE,'sh','-ec',command],
        capture_output=True,text=True,timeout=45)
    assert result.returncode==0,result.stdout+result.stderr
    assert 'NATIVE_RUNTIME_CONTROL_UNSAFE' in result.stdout


def test_notice_integrity_missing_dispatch_configuration_and_installed_drift():
    command=r'''set -eu
mkdir -m 700 /var/lib/intruoyi-native-runtime
machine=11111111111111111111111111111111
printf '%s\n' "$machine" > /etc/machine-id
control=$(stat -c %d /var/lib/intruoyi-native-runtime)
source=$(stat -c %d /fixtures)
artifact=/fixtures/cpython-3.12.11+20250902-x86_64-unknown-linux-gnu-install_only_stripped.tar.gz
notices=/fixtures/native-runtime-notices-20250902-v1.tar.gz
if sh /source/formal_native_runtime.sh install "$artifact" "$machine" "$control" "$source" /absent >/tmp/missing; then exit 31; fi
grep -q NATIVE_RUNTIME_NOTICES_MISSING /tmp/missing
cp "$notices" /tmp/altered-notices; printf X >> /tmp/altered-notices
if sh /source/formal_native_runtime.sh install "$artifact" "$machine" "$control" "$source" /tmp/altered-notices >/tmp/changed; then exit 32; fi
grep -q NATIVE_RUNTIME_NOTICES_CHANGED /tmp/changed
[ ! -e /var/lib/intruoyi-native-runtime/20250902 ]
sh /source/formal_native_runtime.sh install "$artifact" "$machine" "$control" "$source" "$notices"
export PYTHON_BIN=/var/lib/intruoyi-native-runtime/20250902/python/bin/python3
export INTRUOYI_NATIVE_RUNTIME_ARTIFACT="$artifact"
export INTRUOYI_NATIVE_MACHINE_ID="$machine"
export INTRUOYI_NATIVE_CONTROL_DEVICE="$control"
export INTRUOYI_NATIVE_SOURCE_DEVICE="$source"
if bash /source/backup-ops-linux.sh --mode formal-capture --request /absent --expected-request-sha256 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa >/tmp/no-config 2>&1; then exit 33; fi
grep -q NATIVE_RUNTIME_CONFIGURATION_REQUIRED /tmp/no-config
export INTRUOYI_NATIVE_RUNTIME_NOTICES="$notices"
printf '# altered notice\n' >> /var/lib/intruoyi-native-runtime/20250902/notices/licenses/LICENSE.zlib-ng.txt
if sh /source/formal_native_runtime.sh verify "$artifact" "$machine" "$control" "$source" "$notices" >/tmp/drift; then exit 34; fi
grep -q NATIVE_RUNTIME_INSTALLED_NOTICES_CHANGED /tmp/drift
if bash /source/backup-ops-linux.sh --mode formal-capture --request /absent --expected-request-sha256 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa >/tmp/dispatch-drift 2>&1; then exit 35; fi
grep -q NATIVE_RUNTIME_INSTALLED_NOTICES_CHANGED /tmp/dispatch-drift
echo NOTICE_REFUSALS_ACTUAL_PASS
'''
    result=subprocess.run(['docker','run','--rm','--network','none',
        '--label','task-owner=native-runtime-notice-test',
        '--tmpfs','/var/lib:rw,exec,nosuid,nodev,size=536870912',
        '--mount','type=bind,src='+str(ARTIFACT)+',dst=/fixtures,readonly',
        '--mount','type=bind,src='+str(LINUX.resolve())+',dst=/source,readonly',
        IMAGE,'sh','-c',command],capture_output=True,text=True,timeout=45)
    assert result.returncode==0,result.stdout+result.stderr
    assert 'NOTICE_REFUSALS_ACTUAL_PASS' in result.stdout
