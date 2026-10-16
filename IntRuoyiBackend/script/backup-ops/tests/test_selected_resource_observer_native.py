"""Real Linux traversal and native refusal only; no actual ext4/server admission."""
import importlib
import os
from pathlib import Path
import sys
import tempfile

import pytest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
from formal_snapshot_linux import FormalSnapshotError

CODE='RELEASE_SELECTED_NATIVE_RESOURCE_OBSERVATION_INVALID'

def module():
    spec=importlib.util.find_spec('formal_selected_resource_observer')
    assert spec is not None,'trusted native resource observer must be implemented'
    return importlib.import_module('formal_selected_resource_observer')

@pytest.fixture
def root():
    with tempfile.TemporaryDirectory(prefix='native-resource-',dir='/control') as directory:
        yield Path(directory)

def test_real_native_inventory_counts_full_tree_without_writes(root):
    (root/'nested').mkdir();(root/'one').write_bytes(b'abc');(root/'nested'/'two').write_bytes(b'defgh')
    before={str(p):(p.stat().st_ino,p.stat().st_size,p.stat().st_mtime_ns) for p in root.rglob('*')}
    result=module()._scan_source(str(root),byte_limit=20,entry_limit=10,guard=lambda:None)
    assert result==dict(sourceBytes=8,sourceFiles=2,sourceEntries=3)
    assert before=={str(p):(p.stat().st_ino,p.stat().st_size,p.stat().st_mtime_ns) for p in root.rglob('*')}

@pytest.mark.parametrize('fault',['symlink','fifo','bytes','entries','root-link'])
def test_native_special_entries_and_limits_are_refused(root,fault):
    selected=root
    if fault=='symlink':(root/'bad').symlink_to('/etc/passwd')
    elif fault=='fifo':os.mkfifo(root/'bad',0o600)
    elif fault=='root-link':
        selected=root/'alias';selected.symlink_to(root,target_is_directory=True)
    else:(root/'data').write_bytes(b'0123456789')
    with pytest.raises(FormalSnapshotError,match='^'+CODE+'$'):
        module()._scan_source(str(selected),byte_limit=5 if fault=='bytes' else 100,
            entry_limit=0 if fault=='entries' else 10,guard=lambda:None)

def test_native_inventory_guard_error_preserved(root):
    (root/'data').write_bytes(b'abc')
    def lost():raise FormalSnapshotError('NATIVE_OBSERVER_OWNER_LOST')
    with pytest.raises(FormalSnapshotError,match='^NATIVE_OBSERVER_OWNER_LOST$'):
        module()._scan_source(str(root),byte_limit=100,entry_limit=10,guard=lost)

def test_actual_tmpfs_cannot_impersonate_approved_block_mount(root):
    info=root.stat()
    identity=dict(host='172.30.30.58',machineId='1'*32,mount='/control',device='/dev/fiction',
        majorMinor=f'{os.major(info.st_dev)}:{os.minor(info.st_dev)}',
        filesystemUuid='11111111-1111-1111-1111-111111111111',fileSystemId=info.st_dev,
        parent=str(root),parentInode=info.st_ino,path=str(root))
    with pytest.raises(FormalSnapshotError,match='^'+CODE+'$'):
        module()._observe_role('source',identity,guard=lambda:None)
