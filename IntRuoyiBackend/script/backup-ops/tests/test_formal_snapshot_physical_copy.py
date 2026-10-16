"""Real local file-copy boundaries; not live formal snapshot acceptance."""
import importlib.util
from pathlib import Path
from unittest.mock import patch
import pytest

SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
def load():
    spec=importlib.util.spec_from_file_location('physical_copy',SOURCE)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
def tree(tmp_path):
    root=tmp_path/'source';root.mkdir();(root/'.minio.sys').mkdir();(root/'empty').mkdir()
    (root/'.minio.sys'/'format.json').write_bytes(b'PHYSICAL_FORMAT_FIXTURE')
    (root/'yudao').mkdir();(root/'yudao'/'xl.meta').write_bytes(b'PHYSICAL_METADATA_FIXTURE')
    return root

def test_copy_hashes_all_files_and_preserves_empty_dirs(tmp_path):
    m=load();source=tree(tmp_path);target=tmp_path/'copy'
    result=m.copy_physical_minio_tree(source,target)
    assert result['fileCount']==2 and result['hostWritersVerified'] is False
    assert len(result['physicalTreeDigest'])==64 and (target/'empty').is_dir()
    for relative in ('.minio.sys/format.json','yudao/xl.meta'):
        assert (source/relative).read_bytes()==(target/relative).read_bytes()

@pytest.mark.parametrize('case',['existing','nested','empty'])
def test_copy_refuses_invalid_source_or_destination(tmp_path,case):
    m=load();source=tree(tmp_path);target=tmp_path/'copy'
    if case=='existing':target.mkdir();(target/'keep').write_bytes(b'KEEP')
    if case=='nested':target=source/'copy'
    if case=='empty':source=tmp_path/'empty-source';source.mkdir()
    with pytest.raises(m.FormalSnapshotError):m.copy_physical_minio_tree(source,target)
    if case=='existing':assert (target/'keep').read_bytes()==b'KEEP'

def test_source_content_change_retains_failed_copy(tmp_path):
    m=load();source=tree(tmp_path);target=tmp_path/'copy';original=m._hash_object
    def drift(path,*,guard=None):
        if Path(path)==source/'.minio.sys/format.json':Path(path).write_bytes(b'CHANGED')
        return original(path,guard=guard)
    with patch.object(m,'_hash_object',side_effect=drift):
        with pytest.raises(m.FormalSnapshotError):m.copy_physical_minio_tree(source,target)
    assert target.exists() and (target/'.minio.sys/format.json').read_bytes()==b'PHYSICAL_FORMAT_FIXTURE'

@pytest.mark.parametrize('case',['pass','changed-capture','changed-replica'])
def test_replica_uses_separate_verified_copy(tmp_path,case):
    m=load();source=tree(tmp_path);capture=tmp_path/'capture'
    receipt=m.copy_physical_minio_tree(source,capture);replica=tmp_path/'replica'
    if case=='changed-capture':
        (capture/'yudao/xl.meta').write_bytes(b'CHANGED')
        with pytest.raises(m.FormalSnapshotError):m.prepare_minio_replica_copy(capture,replica,receipt)
        assert not replica.exists();return
    actual=m.prepare_minio_replica_copy(capture,replica,receipt)
    assert actual['physicalTreeDigest']==receipt['physicalTreeDigest']
    (replica/'yudao/xl.meta').write_bytes(b'OWNED_REPLICA_CAN_CHANGE')
    assert m.verify_physical_minio_tree(capture,receipt)==receipt
    if case=='changed-replica':
        with pytest.raises(m.FormalSnapshotError):m.verify_physical_minio_tree(replica,receipt)

@pytest.mark.parametrize('case',['same-size','deleted','late-file','late-directory'])
def test_full_copy_rechecks_earlier_destination_after_later_source_readback(tmp_path,case):
    m=load();source=tree(tmp_path);target=tmp_path/'copy';original=m._hash_object
    first=target/'.minio.sys/format.json';tampered=False
    def drift(path,*,guard=None):
        nonlocal tampered
        if Path(path)==source/'yudao/xl.meta' and not tampered:
            tampered=True
            if case=='same-size':first.write_bytes(b'X'*first.stat().st_size)
            elif case=='deleted':first.unlink()
            elif case=='late-file':(target/'unexpected.sql.gz').write_bytes(b'UNPLANNED')
            else:(target/'unexpected-directory').mkdir()
        return original(path,guard=guard)
    with patch.object(m,'_hash_object',side_effect=drift):
        with pytest.raises(m.FormalSnapshotError):m.copy_physical_minio_tree(source,target)
    assert tampered and target.exists()
    assert (source/'.minio.sys/format.json').read_bytes()==b'PHYSICAL_FORMAT_FIXTURE'
    assert (target/'yudao/xl.meta').read_bytes()==b'PHYSICAL_METADATA_FIXTURE'

def test_complete_real_payload_readback_covers_all_file_types_and_preserves_false_authority(tmp_path):
    m=load();source=tree(tmp_path);target=tmp_path/'copy'
    for name,content in {'database.sql.gz':b'RETAINED_SQL_BYTES',
                         'old-images.tar.gz':b'RETAINED_IMAGE_BYTES',
                         'private-config.bin':b'RETAINED_CONFIG_BYTES'}.items():
        (source/name).write_bytes(content)
    original=m._hash_object;visited=[]
    def read(path,*,guard=None):
        visited.append(Path(path));return original(path,guard=guard)
    with patch.object(m,'_hash_object',side_effect=read):
        result=m.copy_physical_minio_tree(source,target)
    for path in source.rglob('*'):
        if path.is_file():
            destination=target/path.relative_to(source)
            assert destination.read_bytes()==path.read_bytes()
            assert visited.count(destination)>=2
    assert result['fileCount']==5 and result['hostWritersVerified'] is False
    assert m.verify_physical_minio_tree(target,result)==result
