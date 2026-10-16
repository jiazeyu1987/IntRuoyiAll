"""Finite formal inventory boundary; local files are not live source evidence."""
import hashlib
import importlib.util
import json
from pathlib import Path
import pytest
from unittest.mock import patch

SOURCE=Path(__file__).resolve().parents[1]/'linux/formal_snapshot_linux.py'
def load():
    spec=importlib.util.spec_from_file_location('formal_objects',SOURCE)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def fixture(tmp_path):
    root=tmp_path/'store';root.mkdir();data=b'ACTUAL_LOCAL_OBJECT_BYTES'
    digest=hashlib.sha256(data).hexdigest();relative=f'sha256/{digest[:2]}/{digest}'
    file=root/relative;file.parent.mkdir(parents=True);file.write_bytes(data)
    inventory={'mode':'incremental-manifest','bucket':'yudao','objectStoreRoot':str(root),
        'stats':{'reusedCount':1},'objects':[{'path':'showroom/document.docx',
        'sha256':'sha256:'+digest,'repositoryKey':'sha256:'+digest,
        'repositoryPath':relative,'size':len(data),'status':'active','changeType':'reused'}]}
    path=tmp_path/'inventory.json';path.write_text(json.dumps(inventory),encoding='utf-8')
    return root,file,inventory,path

def test_inventory_hashes_actual_reused_content(tmp_path):
    m=load();root,file,inventory,path=fixture(tmp_path)
    result=m.verify_object_inventory(path,root,'yudao')
    assert result['inventorySha256']==hashlib.sha256(path.read_bytes()).hexdigest()
    assert result['objectCount']==1 and result['totalBytes']==file.stat().st_size
    assert result['objects'][0]['sha256']==inventory['objects'][0]['sha256']

@pytest.mark.parametrize('case',['corrupt','size','duplicate','traversal','root','bucket','empty','bad-status'])
def test_inventory_refuses_invalid_recovery_content(tmp_path,case):
    m=load();root,file,inventory,path=fixture(tmp_path)
    if case=='corrupt':file.write_bytes(b'CORRUPTED')
    if case=='size':inventory['objects'][0]['size']+=1
    if case=='duplicate':inventory['objects'].append(dict(inventory['objects'][0]))
    if case=='traversal':inventory['objects'][0]['repositoryPath']='../outside'
    if case=='root':inventory['objectStoreRoot']=str(tmp_path)
    if case=='bucket':inventory['bucket']='other'
    if case=='empty':inventory['objects']=[]
    if case=='bad-status':inventory['objects'][0]['status']='unknown'
    path.write_text(json.dumps(inventory),encoding='utf-8')
    with pytest.raises(m.FormalSnapshotError):m.verify_object_inventory(path,root,'yudao')

def test_inventory_refuses_duplicate_json_keys(tmp_path):
    m=load();root,file,inventory,path=fixture(tmp_path)
    path.write_text('{"objects":[],"objects":[]}',encoding='utf-8')
    with pytest.raises(m.FormalSnapshotError):m.verify_object_inventory(path,root,'yudao')

def test_inventory_changed_during_content_hash_refuses(tmp_path):
    m=load();root,file,inventory,path=fixture(tmp_path);original=m._hash_object
    def changed(target):
        result=original(target);path.write_text('{}',encoding='utf-8');return result
    with patch.object(m,'_hash_object',side_effect=changed):
        with pytest.raises(m.FormalSnapshotError) as failure:m.verify_object_inventory(path,root,'yudao')
    assert failure.value.code=='FORMAL_INVENTORY_CHANGED'
