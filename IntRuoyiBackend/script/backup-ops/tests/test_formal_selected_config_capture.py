"""Native private source/copy files; no server credentials or authority."""
import hashlib
import importlib
import json
import os
from pathlib import Path
import sys

import pytest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
from formal_snapshot_linux import FormalSnapshotError

FILES={'application-config':'config/application-instance.yaml','application-env':'.env',
       'data-env':'config/data.env','application-compose':'docker-compose.yml','data-compose':'data-compose.json'}


def fixture(tmp_path):
    source=tmp_path/'selected';source.mkdir(mode=0o700);(source/'config').mkdir(mode=0o700)
    expected={};raw={}
    for role,relative in FILES.items():
        path=source/relative;content=('private-fixture-'+role+'\r\n中文\n').encode()
        path.write_bytes(content);path.chmod(0o600);info=path.stat();raw[role]=content
        expected[role]=dict(path=str(path),bytes=len(content),sha256=hashlib.sha256(content).hexdigest(),
            fileSystemId=info.st_dev,inode=info.st_ino,mtimeNs=info.st_mtime_ns,ctimeNs=info.st_ctime_ns)
    return source,expected,tmp_path/'capture',raw


def fixture_owner():
    """Explicit owner protocol fixture, never real approval."""


@pytest.mark.parametrize('fault',[None,'source','copy','receipt','expected','result','mode','extra','guard'])
def test_final_original_config_verifier_requires_real_unchanged_bytes(tmp_path,fault):
    module=importlib.import_module('formal_selected_config_capture');source,expected,target,_=fixture(tmp_path)
    result=module.capture_selected_config_originals(source,expected,target,guard=fixture_owner)
    guard=fixture_owner
    if fault=='source':(source/'.env').write_bytes(b'CHANGED_SOURCE')
    elif fault=='copy':(target/'application-env.bin').write_bytes(b'CHANGED_COPY')
    elif fault=='receipt':(target/'config-capture-stage.json').write_bytes(b'CHANGED_RECEIPT')
    elif fault=='expected':expected['application-env']['sha256']='0'*64
    elif fault=='result':result['productionEvidence']=True
    elif fault=='mode':(target/'application-env.bin').chmod(0o644)
    elif fault=='extra':(target/'extra.bin').write_bytes(b'UNKNOWN')
    elif fault=='guard':guard=None
    if fault:
        with pytest.raises(FormalSnapshotError):module.verify_selected_config_originals(source,expected,target,result,guard=guard)
    else:
        checked,identity_check=module.verify_selected_config_originals(source,expected,target,result,guard=guard)
        assert checked==dict(configReceiptSha256=hashlib.sha256((target/'config-capture-stage.json').read_bytes()).hexdigest(),productionEvidence=False)
        identity_check()
    assert target.exists() and (target/'config-capture-stage.json').exists()


@pytest.mark.parametrize('fault',['source-bytes','source-replace','copy-bytes','copy-replace',
    'copy-mode','receipt-bytes','config-mode','config-replace'])
def test_final_original_config_readback_rejects_changes_after_last_content_read(tmp_path,monkeypatch,fault):
    module=importlib.import_module('formal_selected_config_capture');source,expected,target,_=fixture(tmp_path)
    result=module.capture_selected_config_originals(source,expected,target,guard=fixture_owner)
    native_read=module._read_secure;receipt_reads=0;changed=False
    def read(path,*args,**kwargs):
        nonlocal receipt_reads
        value=native_read(path,*args,**kwargs)
        if path==target/'config-capture-stage.json':receipt_reads+=1
        return value
    def guard():
        nonlocal changed
        if receipt_reads!=2 or changed:return
        changed=True
        if fault=='config-mode':(source/'config').chmod(0o755)
        elif fault=='config-replace':
            (source/'config').rename(source/'old-config');(source/'config').mkdir(mode=0o700)
            for name in ('application-instance.yaml','data.env'):
                (source/'old-config'/name).rename(source/'config'/name)
        else:
            selected=(source/'.env' if fault.startswith('source') else target/'application-env.bin')
            if fault=='receipt-bytes':selected=target/'config-capture-stage.json'
            if fault.endswith('replace'):
                raw=selected.read_bytes();selected.rename(selected.with_name(selected.name+'.retained'))
                selected.write_bytes(raw);selected.chmod(0o600)
            elif fault=='copy-mode':selected.chmod(0o644)
            else:selected.write_bytes(b'CHANGED_AT_FINAL_GUARD')
    monkeypatch.setattr(module,'_read_secure',read)
    with pytest.raises(FormalSnapshotError):
        module.verify_selected_config_originals(source,expected,target,result,guard=guard)
    assert changed and target.exists() and (target/'config-capture-stage.json').exists()


@pytest.mark.parametrize('fault',[None,'source-bytes','source-replace','copy-bytes','copy-replace',
    'receipt-bytes','config-mode','config-replace','copy-extra'])
def test_final_original_config_deferred_identity_check_requires_unchanged_readback(tmp_path,monkeypatch,fault):
    module=importlib.import_module('formal_selected_config_capture');source,expected,target,_=fixture(tmp_path)
    result=module.capture_selected_config_originals(source,expected,target,guard=fixture_owner)
    receipt,identity_check=module.verify_selected_config_originals(source,expected,target,result,guard=fixture_owner)
    assert receipt['productionEvidence'] is False and callable(identity_check)
    if fault=='config-mode':(source/'config').chmod(0o755)
    elif fault=='config-replace':
        (source/'config').rename(source/'retained-config');(source/'config').mkdir(mode=0o700)
        for name in ('application-instance.yaml','data.env'):
            (source/'retained-config'/name).rename(source/'config'/name)
    elif fault=='copy-extra':(target/'extra.bin').write_bytes(b'FINAL_EXTRA')
    elif fault:
        selected=source/'.env' if fault.startswith('source') else target/'application-env.bin'
        if fault=='receipt-bytes':selected=target/'config-capture-stage.json'
        if fault.endswith('replace'):
            raw=selected.read_bytes();selected.rename(selected.with_suffix('.final-old'))
            selected.write_bytes(raw);selected.chmod(0o600)
        else:selected.write_bytes(b'FINAL_CONFIG_CHANGED')
    def forbidden(*args,**kwargs):raise AssertionError('IDENTITY_ONLY_CHECK_READ_CONTENT_OR_OWNER')
    monkeypatch.setattr(module,'_read_secure',forbidden)
    monkeypatch.setattr(module,'current_owner_guard',forbidden)
    if fault:
        with pytest.raises(FormalSnapshotError):identity_check()
    else:identity_check()


def test_actual_five_raw_files_roundtrip_private_no_secrets(tmp_path):
    module=importlib.import_module('formal_selected_config_capture');source,expected,target,raw=fixture(tmp_path)
    result=module.capture_selected_config_originals(source,expected,target,guard=fixture_owner)
    assert result['state']=='SELECTED_CONFIG_ORIGINALS_CAPTURED_PENDING_FULL_PROTECTION'
    assert result['productionEvidence'] is False and result['fullCaptureVerified'] is False
    assert set(result['originals'])==set(FILES)
    for role,content in raw.items():
        path=target/(role+'.bin')
        assert path.read_bytes()==content and path.stat().st_mode&0o777==0o600
        assert result['originals'][role]['source']==expected[role]
        assert result['originals'][role]['copySha256']==hashlib.sha256(content).hexdigest()
        assert content.decode() not in json.dumps(result,ensure_ascii=False)
    assert json.loads((target/'config-capture-stage.json').read_bytes())==result
    assert target.stat().st_mode&0o777==0o700


@pytest.mark.parametrize('fault',['sha','bytes','inode','mtimeNs','ctimeNs','fileSystemId','missing-role','extra-role',
    'wrong-path','mode','hardlink','symlink','same-bytes-replace','existing-destination','nested-destination','no-guard'])
def test_refuses_drift_or_unsafe_inputs(tmp_path,fault):
    module=importlib.import_module('formal_selected_config_capture');source,expected,target,_=fixture(tmp_path)
    role='application-env';path=source/'.env';guard=fixture_owner
    if fault=='sha':expected[role]['sha256']='0'*64
    elif fault=='bytes':expected[role]['bytes']+=1
    elif fault in ('inode','mtimeNs','ctimeNs','fileSystemId'):expected[role][fault]+=1
    elif fault=='missing-role':expected.pop(role)
    elif fault=='extra-role':expected['extra']=expected[role]
    elif fault=='wrong-path':expected[role]['path']=str(source/'data-compose.json')
    elif fault=='mode':path.chmod(0o644)
    elif fault=='hardlink':os.link(path,source/'alias')
    elif fault=='symlink':path.rename(source/'retained');path.symlink_to(source/'retained')
    elif fault=='same-bytes-replace':
        content=path.read_bytes();path.rename(source/'old');path.write_bytes(content);path.chmod(0o600)
    elif fault=='existing-destination':target.mkdir()
    elif fault=='nested-destination':target=source/'nested'
    elif fault=='no-guard':guard=None
    with pytest.raises(FormalSnapshotError):module.capture_selected_config_originals(source,expected,target,guard=guard)
    assert not (target/'config-capture-stage.json').exists()


@pytest.mark.parametrize('fault',['source','copy','guard'])
def test_final_actual_readback_rejects_late_changes_and_retains(tmp_path,fault):
    module=importlib.import_module('formal_selected_config_capture');source,expected,target,_=fixture(tmp_path)
    changed=False
    def current():
        nonlocal changed
        copy=target/'data-compose.bin'
        if copy.exists() and not changed:
            changed=True
            if fault=='source':(source/'.env').write_bytes(b'changed-source')
            elif fault=='copy':(target/'application-env.bin').write_bytes(b'changed-copy')
            else:raise FormalSnapshotError('FORMAL_TEST_OWNER_REVOKED')
    with pytest.raises(FormalSnapshotError) as failure:module.capture_selected_config_originals(source,expected,target,guard=current)
    assert not (target/'config-capture-stage.json').exists()
    assert (target/'config-capture-failed.json').exists()
    assert (target/'data-compose.bin').exists()
    if fault=='guard':assert failure.value.code=='FORMAL_TEST_OWNER_REVOKED'
