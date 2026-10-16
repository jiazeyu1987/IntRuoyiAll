"""Real local Linux fixture. Never mounts or freezes an existing host filesystem."""
import json
import importlib.util
import re
import select
import signal
import subprocess
import sys
import tempfile
import time
from pathlib import Path


def run(*args):
    return subprocess.run(args, check=True, capture_output=True, text=True, timeout=15).stdout.strip()


def bound(device, image):
    if not re.fullmatch(r"/dev/loop[0-9]+", device):
        raise RuntimeError("UNEXPECTED_DEVICE")
    actual = run("losetup", "--noheadings", "--output", "BACK-FILE", device)
    if actual != str(image):
        raise RuntimeError("BACKING_FILE_MISMATCH")


def thaw(device, image, mount):
    bound(device, image)
    run("fsfreeze", "--unfreeze", str(mount))


WRITER = """import os,sys
print('ATTEMPT', flush=True)
with open(sys.argv[1], 'xb') as f:
 f.write(b'owned-fence-writer'); f.flush(); os.fsync(f.fileno())
print('COMPLETED', flush=True)
"""

PREOPENED_WRITER = """import os,sys
fd=os.open(sys.argv[1],os.O_WRONLY|os.O_APPEND)
print('READY',flush=True)
if sys.stdin.readline().strip() != 'GO': raise RuntimeError('NO_GO')
print('ATTEMPT',flush=True)
os.write(fd,b'owned-fence-writer'); os.fsync(fd); os.close(fd)
print('COMPLETED',flush=True)
"""


def prepare_open_writer(path):
    path.write_bytes(b"baseline-")
    p = subprocess.Popen([sys.executable, "-c", PREOPENED_WRITER, str(path)], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if not select.select([p.stdout], [], [], 3)[0] or p.stdout.readline().strip() != "READY":
        p.kill()
        raise RuntimeError("PREOPENED_WRITER_NOT_READY")
    return p


def release_open_writer(p):
    p.stdin.write("GO\n")
    p.stdin.flush()
    if not select.select([p.stdout], [], [], 3)[0] or p.stdout.readline().strip() != "ATTEMPT":
        raise RuntimeError("PREOPENED_WRITER_NOT_STARTED")


def start_writer(path):
    p = subprocess.Popen([sys.executable, "-c", WRITER, str(path)], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if not select.select([p.stdout], [], [], 3)[0] or p.stdout.readline().strip() != "ATTEMPT":
        p.kill()
        raise RuntimeError("WRITER_NOT_STARTED")
    return p


def require_blocked(p):
    time.sleep(0.6)
    if p.poll() is not None:
        raise RuntimeError("WRITER_NOT_BLOCKED")


def require_completed(p, path, expected=b"owned-fence-writer"):
    out, err = p.communicate(timeout=8)
    if p.returncode != 0 or out.strip() != "COMPLETED" or err or path.read_bytes() != expected:
        raise RuntimeError("WRITER_RECOVERY_FAILED")


def main(mode):
    if mode not in {"unfenced", "maintained", "native"}:
        raise RuntimeError("MODE_INVALID")
    with tempfile.TemporaryDirectory(prefix="intruoyi-owned-ext4-") as tmp:
        root = Path(tmp)
        image = root / "owned.ext4"
        with image.open("xb") as f:
            f.truncate(64 * 1024 * 1024)
        mount = root / "mount"
        mount.mkdir()
        device = run("losetup", "--find", "--show", str(image))
        mounted = frozen = False
        writer = watchdog = None
        try:
            bound(device, image)
            run("mkfs.ext4", "-q", device)
            run("mount", "-t", "ext4", "-o", "nosuid,nodev,noexec", device, str(mount))
            mounted = True
            info = json.loads(run("findmnt", "--json", "--mountpoint", str(mount), "--output", "SOURCE,TARGET,FSTYPE"))["filesystems"]
            if len(info) != 1 or info[0] != {"source": device, "target": str(mount), "fstype": "ext4"}:
                raise RuntimeError("MOUNT_BINDING_INVALID")
            print("OWNED_LOOP_EXT4_BINDING_VERIFIED", flush=True)
            if mode == 'native':
                native_source = globals().get('NATIVE_WATCHDOG_SOURCE')
                if not native_source:
                    raise RuntimeError('NATIVE_WATCHDOG_ENTRY_REQUIRED')
                native = root / 'native-watchdog.py'
                native.write_text(native_source)
                spec = importlib.util.spec_from_file_location('native_watchdog', native)
                module = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(module)
                snapshot_source = globals().get('NATIVE_SNAPSHOT_SOURCE')
                if not snapshot_source:
                    raise RuntimeError('NATIVE_SNAPSHOT_ENTRY_REQUIRED')
                snapshot_path = root / 'formal_snapshot_linux.py'
                snapshot_path.write_text(snapshot_source)
                snapshot_spec = importlib.util.spec_from_file_location('formal_snapshot_linux', snapshot_path)
                snapshot = importlib.util.module_from_spec(snapshot_spec)
                sys.modules[snapshot_spec.name] = snapshot
                snapshot_spec.loader.exec_module(snapshot)
                dev = mount.stat().st_dev
                import os
                binding = {'operationId': 'owned-native-watchdog',
                           'bootId': Path('/proc/sys/kernel/random/boot_id').read_text().strip(),
                           'mount': {'source': device, 'device': f'{os.major(dev)}:{os.minor(dev)}',
                                     'fstype': 'ext4', 'target': str(mount)}}
                def require_preflight_refusal(directory, selected, code):
                    directory.mkdir(mode=0o700)
                    r = subprocess.run([sys.executable, str(native), str(directory), str(mount), '3'],
                                       input=json.dumps(selected)+'\n', capture_output=True, text=True, timeout=8)
                    if r.returncode != 1 or r.stdout or r.stderr.strip() != code or list(directory.iterdir()):
                        raise RuntimeError('NATIVE_UNSAFE_PREFLIGHT_ACCEPTED')
                require_preflight_refusal(mount / 'unsafe-control', binding, 'WATCHDOG_CONTROL_ON_FROZEN_DEVICE')
                changed = dict(binding, mount=dict(binding['mount'], device='999:999'))
                require_preflight_refusal(root / 'wrong-device', changed, 'WATCHDOG_MOUNT_CHANGED')
                changed = dict(binding, bootId='00000000-0000-0000-0000-000000000000')
                require_preflight_refusal(root / 'wrong-boot', changed, 'WATCHDOG_BOOT_CHANGED')
                print('NATIVE_SAME_DEVICE_CHANGED_DEVICE_BOOT_REFUSAL_PASS', flush=True)
                def launch(directory, seconds, selected=binding, acquire=False):
                    directory.mkdir(mode=0o700)
                    args = [sys.executable, str(native), str(directory), str(mount), str(seconds)]
                    if acquire: args.append('--acquire-freeze')
                    p = subprocess.Popen(args,
                                         stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                         text=True, start_new_session=True)
                    p.stdin.write(json.dumps(selected) + '\n'); p.stdin.flush()
                    if not select.select([p.stdout], [], [], 5)[0] or p.stdout.readline().strip() != 'WATCHDOG_READY_NOT_FENCE_PROOF':
                        p.kill(); p.communicate(timeout=5)
                        raise RuntimeError('NATIVE_WATCHDOG_NOT_READY')
                    return p
                control = root / 'control-expiry'
                watchdog = launch(control, 2)
                module.require_uncancelled_lease(control, binding)
                bound(device, image)
                run('fsfreeze', '--freeze', str(mount)); frozen = True
                writer = start_writer(mount / 'native-expiry-writer')
                require_blocked(writer)
                watchdog.send_signal(signal.SIGHUP)
                # communicate() closes stdin; preserve the owner channel until
                # the deadline to distinguish expiry from the separate EOF case.
                watchdog.wait(timeout=8)
                out, err = watchdog.communicate(timeout=8)
                if watchdog.returncode != 0 or out.strip() != 'WATCHDOG_THAW_CAPTURE_INVALIDATED' or err:
                    raise RuntimeError('NATIVE_EXPIRY_THAW_FAILED')
                watchdog = None; frozen = False
                require_completed(writer, mount / 'native-expiry-writer'); writer = None
                invalid = json.loads((control / '010-invalidated.json').read_bytes())
                thawed = json.loads((control / '020-thawed.json').read_bytes())
                if invalid['status'] != 'CAPTURE_INVALIDATED' or invalid['reason'] != 'deadline' or invalid['observedBootSeconds'] > thawed['observedBootSeconds']:
                    raise RuntimeError('NATIVE_INVALIDATION_ORDER_FAILED')
                try:
                    module.require_uncancelled_lease(control, binding)
                    raise RuntimeError('EXPIRED_CAPTURE_ACCEPTED')
                except module.WatchdogError as exc:
                    if str(exc) != 'WATCHDOG_CAPTURE_INVALIDATED': raise
                print('NATIVE_EXPIRY_INVALIDATION_BEFORE_THAW_PASS', flush=True)
                control = root / 'control-eof'
                watchdog = launch(control, 3)
                run('fsfreeze', '--freeze', str(mount)); frozen = True
                watchdog.stdin.close(); watchdog.stdin = None
                out, err = watchdog.communicate(timeout=8)
                if watchdog.returncode != 0 or out.strip() != 'WATCHDOG_THAW_CAPTURE_INVALIDATED' or err:
                    raise RuntimeError('NATIVE_EOF_THAW_FAILED')
                watchdog = None; frozen = False
                if json.loads((control / '010-invalidated.json').read_bytes())['reason'] != 'owner-control-eof':
                    raise RuntimeError('NATIVE_EOF_INVALIDATION_FAILED')
                print('NATIVE_OWNER_EOF_INVALIDATION_THAW_PASS', flush=True)
                control = root / 'control-cancel'
                watchdog = launch(control, 3)
                run('fsfreeze', '--freeze', str(mount)); frozen = True
                thaw(device, image, mount); frozen = False
                watchdog.stdin.write('THAW_VERIFIED_CANCEL\n'); watchdog.stdin.flush()
                out, err = watchdog.communicate(timeout=8)
                if watchdog.returncode != 0 or out or err or not (control / '010-cancelled.json').exists() or (control / '010-invalidated.json').exists():
                    raise RuntimeError('NATIVE_CANCEL_FAILED')
                watchdog = None
                try:
                    module.require_uncancelled_lease(control, binding)
                    raise RuntimeError('CANCELLED_CAPTURE_ACCEPTED')
                except module.WatchdogError as exc:
                    if str(exc) != 'WATCHDOG_LEASE_CANCELLED': raise
                print('NATIVE_CANCEL_IS_NOT_CAPTURE_PROOF_PASS', flush=True)
                # A retained, otherwise valid ready receipt whose deadline has
                # passed must refuse even if no invalidation file is present.
                ready = json.loads((control / '000-ready.json').read_bytes())
                expired_control = root / 'expired-guard'
                expired_control.mkdir(mode=0o700)
                ready['deadlineBootSeconds'] = 0
                module.write_exclusive(expired_control, '000-ready.json', ready)
                try:
                    module.require_uncancelled_lease(expired_control, binding)
                    raise RuntimeError('EXPIRED_UNJOURNALED_LEASE_ACCEPTED')
                except module.WatchdogError as exc:
                    if str(exc) != 'WATCHDOG_LEASE_EXPIRED': raise
                print('NATIVE_EXPIRED_GUARD_WITHOUT_INVALIDATION_REFUSES_PASS', flush=True)
                source = mount / 'raw-minio'
                source.mkdir(mode=0o700)
                (source / 'xl.meta').write_bytes(b'owned-metadata')
                (source / 'payload').write_bytes(b'owned-payload' * 1024)
                control = root / 'control-raw-copy'
                watchdog = launch(control, 3)
                run('fsfreeze', '--freeze', str(mount)); frozen = True
                copied = snapshot.copy_physical_minio_tree(source, root / 'raw-copy',
                    lease_guard=lambda: module.require_uncancelled_lease(control, binding))
                if copied['hostWritersVerified'] is not False or copied['fileCount'] != 2:
                    raise RuntimeError('FINITE_COPY_FALSE_COMPLETION')
                print('NATIVE_LEASE_CONSUMED_BY_ACTUAL_RAW_COPY_PASS', flush=True)
                # Actual stalled copy exceeds the independent lease. Its files
                # remain for diagnosis, but the production copier cannot return
                # a receipt. This delay represents a real paused owner process.
                guard_calls = []
                def stalled_guard():
                    guard_calls.append(1)
                    if len(guard_calls) == 5:
                        time.sleep(3.2)
                    module.require_uncancelled_lease(control, binding)
                try:
                    snapshot.copy_physical_minio_tree(source, root / 'raw-copy-expired', lease_guard=stalled_guard)
                    raise RuntimeError('EXPIRED_RAW_COPY_RECEIPT_RETURNED')
                except module.WatchdogError as exc:
                    if str(exc) not in {'WATCHDOG_CAPTURE_INVALIDATED', 'WATCHDOG_LEASE_EXPIRED'}: raise
                watchdog.wait(timeout=8)
                out, err = watchdog.communicate(timeout=8)
                if watchdog.returncode != 0 or out.strip() != 'WATCHDOG_THAW_CAPTURE_INVALIDATED' or err:
                    raise RuntimeError('NATIVE_RAW_COPY_EXPIRY_RECOVERY_FAILED')
                watchdog = None; frozen = False
                if not (root / 'raw-copy-expired').is_dir() or not list((root / 'raw-copy-expired').iterdir()):
                    raise RuntimeError('FAILED_RAW_COPY_NOT_RETAINED')
                print('NATIVE_EXPIRED_RAW_COPY_REFUSES_RECEIPT_RETAINS_FILES_PASS', flush=True)
                # The independent native process, rather than its owner, owns
                # freeze acquisition. Owner loss cannot race a later owner-side
                # freeze after watchdog expiry.
                control = root / 'control-native-acquire'
                writer_path = mount / 'native-acquire-writer'
                writer = prepare_open_writer(writer_path)
                watchdog = launch(control, 3, acquire=True)
                frozen = True
                acquired = json.loads((control / '005-freeze-acquired.json').read_bytes())
                if acquired['status'] != 'ACTUAL_FREEZE_ACQUIRED_STAGE_ONLY':
                    raise RuntimeError('NATIVE_FREEZE_RECEIPT_MISSING')
                release_open_writer(writer)
                require_blocked(writer)
                watchdog.stdin.close(); watchdog.stdin = None
                out, err = watchdog.communicate(timeout=8)
                if watchdog.returncode != 0 or out.strip() != 'WATCHDOG_THAW_CAPTURE_INVALIDATED' or err:
                    raise RuntimeError('NATIVE_ACQUIRED_EOF_RECOVERY_FAILED')
                watchdog = None; frozen = False
                require_completed(writer, writer_path, b'baseline-owned-fence-writer'); writer = None
                print('NATIVE_OWNS_FREEZE_ACQUISITION_AND_EOF_THAW_PASS', flush=True)
                source = mount / 'raw-minio'
                result = module.capture_physical_with_native_fence(root / 'native-managed-copy', mount,
                    binding, source, root / 'native-managed-raw', 3, guard=lambda: module.private_directory(root))
                if result['state'] != 'RAW_CAPTURED_AND_THAWED_PENDING_FORMAL_BARRIERS' or result['productionEvidence'] is not False:
                    raise RuntimeError('NATIVE_MANAGED_COPY_FALSE_FINALIZATION')
                event_root = root / 'native-managed-copy'
                if not (event_root / '010-copy-accepted.json').exists() or not (event_root / '020-thawed.json').exists():
                    raise RuntimeError('NATIVE_MANAGED_COPY_EVENTS_MISSING')
                print('NATIVE_MANAGED_CAPTURE_ACK_AND_THAW_PASS', flush=True)
                control = root / 'native-bad-finish'
                watchdog = launch(control, 3, acquire=True); frozen = True
                watchdog.stdin.write('CAPTURE_FINISH ' + '0'*64 + '\n'); watchdog.stdin.flush()
                watchdog.wait(timeout=8)
                out, err = watchdog.communicate(timeout=2)
                if watchdog.returncode != 0 or out.strip() != 'WATCHDOG_THAW_CAPTURE_INVALIDATED' or err:
                    raise RuntimeError('NATIVE_BAD_FINISH_LEFT_FROZEN')
                frozen = False; watchdog = None
                if json.loads((control / '010-invalidated.json').read_bytes())['reason'] != 'capture-receipt-rejected':
                    raise RuntimeError('NATIVE_BAD_FINISH_NOT_INVALIDATED')
                print('NATIVE_BAD_FINISH_INVALIDATES_AND_THAWS_PASS', flush=True)
                return
            path = mount / "writer-1"
            if mode == "unfenced":
                writer = prepare_open_writer(path)
                release_open_writer(writer)
                require_blocked(writer)
                raise RuntimeError("NEGATIVE_CONTROL_UNEXPECTED_PASS")
            writer = prepare_open_writer(path)
            # Watchdog is a separate process on the unfrozen container root.
            # It validates the owned loop binding again before thawing.
            watchdog_code = """import signal,subprocess,sys,time
signal.signal(signal.SIGHUP, signal.SIG_IGN)
print('WATCHDOG_READY',flush=True)
time.sleep(4)
actual=subprocess.run(['losetup','--noheadings','--output','BACK-FILE',sys.argv[1]],check=True,capture_output=True,text=True,timeout=5).stdout.strip()
if actual != sys.argv[2]: raise RuntimeError('WATCHDOG_BINDING_CHANGED')
subprocess.run(['fsfreeze','--unfreeze',sys.argv[3]],check=True,capture_output=True,timeout=5)
print('WATCHDOG_THAW',flush=True)
"""
            def start_watchdog():
                p = subprocess.Popen([sys.executable, "-c", watchdog_code, device, str(image), str(mount)], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, start_new_session=True)
                if not select.select([p.stdout], [], [], 3)[0] or p.stdout.readline().strip() != 'WATCHDOG_READY':
                    raise RuntimeError('WATCHDOG_NOT_READY')
                return p
            watchdog = start_watchdog()
            bound(device, image)
            run("fsfreeze", "--freeze", str(mount))
            frozen = True
            release_open_writer(writer)
            require_blocked(writer)
            print("PREEXISTING_OPEN_FD_WRITE_BLOCKED_DURING_FENCE", flush=True)
            copied = root / "sealed-copy"
            with copied.open("xb") as f:
                f.write(path.read_bytes()); f.flush()
                import os
                os.fsync(f.fileno())
            if copied.read_bytes() != b"baseline-" or path.read_bytes() != b"baseline-":
                raise RuntimeError("FROZEN_READ_COPY_CHANGED")
            print("FROZEN_READ_COPY_TO_OTHER_FILESYSTEM_PASS", flush=True)
            thaw(device, image, mount)
            frozen = False
            require_completed(writer, path, b"baseline-owned-fence-writer")
            if copied.read_bytes() != b"baseline-":
                raise RuntimeError("SEALED_COPY_CHANGED_AFTER_THAW")
            writer = None
            watchdog.terminate()
            watchdog.communicate(timeout=5)
            watchdog = None
            print("WRITE_COMPLETED_AFTER_EXPLICIT_THAW", flush=True)
            # A separate fence owner exits without thaw. Independent watchdog recovers.
            watchdog = start_watchdog()
            owner_code = "import subprocess,sys,time; subprocess.run(['fsfreeze','--freeze',sys.argv[1]],check=True,timeout=5); print('OWNER_FROZEN',flush=True); time.sleep(60)"
            bound(device, image)
            owner = subprocess.Popen([sys.executable, "-c", owner_code, str(mount)], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            if not select.select([owner.stdout], [], [], 5)[0] or owner.stdout.readline().strip() != 'OWNER_FROZEN':
                owner.kill()
                raise RuntimeError('OWNER_FREEZE_NOT_OBSERVED')
            frozen = True
            owner.kill()
            owner.communicate(timeout=5)
            if owner.returncode != -signal.SIGKILL:
                raise RuntimeError("OWNER_FAILURE_NOT_OBSERVED")
            watchdog.send_signal(signal.SIGHUP)
            path = mount / "writer-2"
            writer = start_writer(path)
            require_blocked(writer)
            print("WRITE_BLOCKED_AFTER_OWNER_SIGKILL", flush=True)
            out, err = watchdog.communicate(timeout=8)
            if watchdog.returncode != 0 or out.strip() != "WATCHDOG_THAW" or err:
                raise RuntimeError("WATCHDOG_FAILED")
            frozen = False
            watchdog = None
            require_completed(writer, path)
            writer = None
            print("INDEPENDENT_WATCHDOG_RECOVERY_PASS", flush=True)
            print("WATCHDOG_SURVIVED_DISCONNECT_SIGNAL_PASS", flush=True)
        finally:
            if frozen:
                thaw(device, image, mount)
            if writer is not None and writer.poll() is None:
                writer.kill()
                writer.communicate(timeout=5)
            if watchdog is not None and watchdog.poll() is None:
                watchdog.terminate()
                watchdog.communicate(timeout=5)
            if mounted:
                bound(device, image)
                run("umount", str(mount))
            bound(device, image)
            run("losetup", "--detach", device)
            print("OWNED_MOUNT_AND_LOOP_RELEASED", flush=True)


if __name__ == "__main__":
    main(sys.argv[1])
