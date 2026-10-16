"""Actual task-owned ext4/Linux entry verification, with no shared host mounts."""
from pathlib import Path
import subprocess


def test_native_watchdog_deadline_owner_loss_and_cancel_are_not_capture_success():
    module = Path(__file__).parents[1] / 'linux' / 'formal_filesystem_watchdog.py'
    fixture = Path(__file__).parent / 'fixtures' / 'owned_ext4_fence.py'
    source = module.read_text(encoding='utf-8') if module.is_file() else ''
    snapshot = (module.parent / 'formal_snapshot_linux.py').read_text(encoding='utf-8')
    script = 'NATIVE_WATCHDOG_SOURCE = ' + repr(source) + '\nNATIVE_SNAPSHOT_SOURCE = ' + repr(snapshot) + '\n' + fixture.read_text(encoding='utf-8')
    result = subprocess.run(['docker', 'run', '--rm', '-i', '--network', 'none', '--privileged',
                             '--label', 'intruoyi.task=20260927-three-environment-release',
                             'sha256:aa88ecce96d1abff3797127aa1b22171134b4f217b99e81f0c01b2c1b1bd2992',
                             'python3', '-', 'native'], input=script, capture_output=True, text=True, timeout=40)
    assert result.returncode == 0, result.stdout + result.stderr
    for required in ['NATIVE_EXPIRY_INVALIDATION_BEFORE_THAW_PASS',
                     'NATIVE_SAME_DEVICE_CHANGED_DEVICE_BOOT_REFUSAL_PASS',
                     'NATIVE_OWNER_EOF_INVALIDATION_THAW_PASS',
                     'NATIVE_CANCEL_IS_NOT_CAPTURE_PROOF_PASS',
                     'NATIVE_LEASE_CONSUMED_BY_ACTUAL_RAW_COPY_PASS',
                     'NATIVE_EXPIRED_RAW_COPY_REFUSES_RECEIPT_RETAINS_FILES_PASS',
                     'NATIVE_OWNS_FREEZE_ACQUISITION_AND_EOF_THAW_PASS',
                     'NATIVE_MANAGED_CAPTURE_ACK_AND_THAW_PASS',
                     'NATIVE_BAD_FINISH_INVALIDATES_AND_THAWS_PASS',
                     'NATIVE_EXPIRED_GUARD_WITHOUT_INVALIDATION_REFUSES_PASS', 'OWNED_MOUNT_AND_LOOP_RELEASED']:
        assert required in result.stdout
