"""W02 consumer protocol checks only; no signed/native runtime success fixtures."""
import copy
import hashlib
from pathlib import Path
import sys
from types import SimpleNamespace

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_backup_lab as lab
from formal_snapshot_linux import FormalSnapshotError


def pin(path, content=b'original'):
    return dict(path=path, bytes=len(content), sha256=hashlib.sha256(content).hexdigest())


def authority(version='v2'):
    value = object.__new__(lab.LabAuthority)
    value.native_runtime = None; value.native_trust = None; value.native_proof = None
    value.native_originals = {}
    value.key_pin = pin(lab.LAB_ROOT + '/keys/authority.pem')
    value.context = dict(installedClosurePin=pin(lab.LAB_ROOT + '/installation.json'))
    value.installation = dict(schemaVersion='RepositoryNativeInstallation/' + version, root=lab.LAB_ROOT,
        sourceToolkitPin=pin(lab.LAB_ROOT + '/toolkit.tar'),
        filePins=[pin(lab.LAB_ROOT + '/toolkit/formal_backup_lab.py')],
        dependencyPins=[pin('/usr/lib/systemd/systemd')], pythonPin=pin(lab.LAB_ROOT + '/python/bin/python'))
    value.host = dict(pythonPin=value.installation['pythonPin'], hostId='machine', bootId='boot',
                      pidNamespace='pid:[1]', mountNamespace='mnt:[1]')
    if version == 'v2':
        value.installation.update(nativeRuntimeTrustPin=pin(lab.LAB_ROOT + '/native-trust.json'),
                                  nativeRuntimeProofPin=pin(lab.LAB_ROOT + '/native-proof.json'))
    return value


def test_v1_outside_dependency_and_default_private_outside_reads_stay_rejected():
    value = authority('v1')
    with pytest.raises(FormalSnapshotError, match='PATH_OUTSIDE_SCOPE'):
        value.verify_installation()
    for flags in ({}, {'executable': True}, {'installed': True}):
        with pytest.raises(FormalSnapshotError, match='PATH_OUTSIDE_SCOPE'):
            value.read(value.installation['dependencyPins'][0], **flags)


@pytest.mark.parametrize('mutation', [
    lambda m:m.update(schemaVersion='RepositoryNativeInstallation/v3'),
    lambda m:m.update(allowOutside=True),
    lambda m:m.pop('nativeRuntimeProofPin'),
    lambda m:m.update(root='/other'),
    lambda m:m['filePins'].append(pin('/usr/private/program.py')),
    lambda m:m.update(pythonPin=pin('/usr/bin/python')),
    lambda m:m['dependencyPins'].append(copy.deepcopy(m['dependencyPins'][0])),
])
def test_bad_v2_manifest_rejected_before_shared_helper(mutation):
    value = authority(); mutation(value.installation)
    value._load_native_runtime = lambda:pytest.fail('unadmitted shape reached helper')
    with pytest.raises(FormalSnapshotError):
        value.verify_installation()


def test_w03_api_receives_exact_selected_manifest_and_authority_and_role(monkeypatch):
    value = authority(); calls = []
    native = value.installation['dependencyPins'][0]
    trust = dict(runtimeRole='EXECUTOR', nativeFilePins=[dict(pin=native, roles=['native-manager'])])
    proof = {k:value.host[k] for k in ('hostId', 'bootId', 'pidNamespace', 'mountNamespace')}
    # Declared protocol boundary only. W03's real signed/PID/ns implementation is
    # deliberately not substituted into a live authority or claimed as passing.
    def verify(reference, keys):
        calls.append(('verify', reference, keys)); return copy.deepcopy(value.installation)
    def read(reference, selected_trust, selected_proof, *, role, limit):
        calls.append(('read', reference, selected_trust, selected_proof, role, limit)); return b'original'
    helper = SimpleNamespace(verify_native_installation=verify, read_native_member=read)
    value._load_native_runtime = lambda:setattr(value, 'native_runtime', helper)
    value.signed = lambda ref:trust if ref == value.installation['nativeRuntimeTrustPin'] else proof
    original_read = value.read
    value.read = lambda reference, *args, **flags:(original_read(reference, *args, **flags)
        if not reference['path'].startswith(lab.LAB_ROOT + '/') else b'original')
    value.verify_installation()
    assert calls[0] == ('verify', value.context['installedClosurePin'], [value.key_pin])
    assert calls[1][1:] == (native, value.installation['nativeRuntimeTrustPin'],
                          value.installation['nativeRuntimeProofPin'], 'native-manager', 128*1024*1024)
    assert value.read_native(native, role='native-manager') == b'original'
    assert len(calls) == 3  # Every native read invokes the shared revalidation API.
    with pytest.raises(FormalSnapshotError, match='NOT_DECLARED'):
        value.read_native(pin('/usr/bin/not-selected'), role='native-manager')
    helper.read_native_member = lambda *args, **kwargs:b'changed'
    with pytest.raises(FormalSnapshotError, match='ORIGINAL_CHANGED'):
        value.read_native(native, role='native-manager')


def test_shared_native_failure_is_not_converted_to_success():
    value = authority()
    value.native_trust = {}; value.native_proof = {}
    class NativeFailure(Exception):pass
    def fail(*args, **kwargs):raise NativeFailure('EXPIRED_OR_WRONG_NAMESPACE')
    value.native_runtime = SimpleNamespace(read_native_member=fail)
    with pytest.raises(NativeFailure, match='EXPIRED_OR_WRONG_NAMESPACE'):
        value.read_native(value.installation['dependencyPins'][0], role='native-manager')


def test_actual_w03_reader_requires_previously_verified_original_pins(monkeypatch):
    import importlib.util
    shared = Path(r'C:\Users\BJB110\.codex\worktrees\instance-snapshot-integration\IntRuoyiMaintance\ops\recovery')
    modules = []
    for name in ('repository_records', 'repository_native_runtime'):
        path = shared / (name + '.py')
        spec = importlib.util.spec_from_file_location(name, path)
        module = importlib.util.module_from_spec(spec)
        monkeypatch.setitem(sys.modules, name, module)
        exec(compile(path.read_bytes(), str(path), 'exec'), module.__dict__)
        modules.append(module)
    value = authority(); value.native_runtime = modules[1]
    value.native_trust = {}; value.native_proof = {}
    # Actual shared code, no registered native state, no fake signed records.
    # A decoded-record API mismatch would yield PIN_INVALID instead; current
    # consumer passes the original Pins then fails at real admission as required.
    with pytest.raises(modules[0].RepositoryError, match='NATIVE_VERIFICATION_REQUIRED'):
        value.read_native(value.installation['dependencyPins'][0], role='native-manager')


def test_helper_source_is_required_in_exact_approved_adjacent_install_root():
    value = authority()
    with pytest.raises(FormalSnapshotError, match='HELPER_NOT_INSTALLED'):
        value._load_native_runtime()


@pytest.mark.parametrize('changes', [dict(pid=2), dict(uid=1), dict(gid=1),
                                     dict(bootId='old-boot'), dict(executablePin=pin('/usr/bin/other'))])
def test_unselected_manager_is_rejected_before_any_bus_connection(changes):
    value = authority()
    manager = dict(pid=1, uid=0, gid=0, bootId=value.host['bootId'],
                   executablePin=value.installation['dependencyPins'][0])
    manager.update(changes)
    with pytest.raises(FormalSnapshotError, match='NATIVE_MANAGER_CHANGED'):
        lab.verify_native_manager(manager, {}, value)
