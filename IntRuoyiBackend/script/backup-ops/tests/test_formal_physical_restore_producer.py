"""Native stage orchestration with real export files and explicit effect seams."""
import copy
import hashlib
import importlib
import json
from pathlib import Path
import sys

import pytest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
from formal_snapshot_linux import FormalSnapshotError, _canonical_bytes
from test_formal_physical_restore_readback import Source, fixture_owner
from formal_s3_export import export_s3_versions


def setup(tmp_path,monkeypatch, fault):
    module=importlib.import_module('formal_physical_restore_producer')
    capture=tmp_path/'capture';capture.mkdir(mode=0o700)
    (capture/'raw').write_bytes(b'raw-preserved')
    source=tmp_path/'original-export';export_s3_versions(Source(),['yudao','edhr'],source,guard=fixture_owner)
    physical={'physicalTreeDigest':'a'*64};observed={'bucketObservations':{'buckets':['yudao','edhr']},'metadataScope':{'buckets':['yudao','edhr']}}
    data=tmp_path/'approved-recovery-minio';evidence=tmp_path/'evidence';calls=[]
    def fail(stage):
        if fault==stage:raise FormalSnapshotError('FORMAL_TEST_'+stage.upper())
    def copy_raw(actual_capture,target,actual_physical,actual_observed,*,guard):
        guard();calls.append('copy');assert actual_capture==capture and target==data
        assert actual_physical==physical and actual_observed==observed
        target.mkdir(mode=0o700);(target/'raw').write_bytes((capture/'raw').read_bytes());fail('copy')
        return {'observationSha256':'b'*64}
    def start(operation,target,actual_physical,actual_observed,proof,credentials,root,*,image_id,guard):
        guard();calls.append('start');assert target==data and (target/'raw').read_bytes()==b'raw-preserved'
        fail('start');return dict(containerId='c'*64,imageId=image_id)
    def live(identity,*,guard):guard();calls.append('live');fail('live')
    def client(identity,access,secret,*,timeout,guard):
        guard();calls.append('client');assert identity=='c'*64
        return Source('restored')
    def dependencies(client,target,buckets,*,guard):
        guard();calls.append('dependencies');fail('dependencies');return {'explicitProtocol':True}
    def check(observed,dependencies):
        calls.append('protection');fail('protection')
    native_export=export_s3_versions
    def export(client,buckets,target,*,guard,timeout):
        calls.append('export');fail('export');return native_export(client,buckets,target,guard=guard,timeout=timeout)
    def raw_readback(target,proof,*,lease_guard):
        lease_guard();calls.append('raw-recheck');assert target==capture
        assert (capture/'raw').read_bytes()==b'raw-preserved';fail('raw-recheck')
    for name,value in dict(prepare_checked_minio_replica=copy_raw,create_and_start_checked_replica=start,
        _wait_live=live,ContainerReadOnlyS3=client,observe_bound_replica_dependencies=dependencies,
        verify_started_replica_dependencies=check,export_s3_versions=export,
        verify_physical_minio_tree=raw_readback).items():monkeypatch.setattr(module,name,value)
    kwargs=dict(operation='owned-restore',capture=capture,physical=physical,observed=observed,
        credential_file=tmp_path/'unused.secret',access='fixture-access',secret='fixture-secret',
        evidence=evidence,recovery_data=data,source_export=source,
        source_inventory_sha256=hashlib.sha256((source/'versions.jsonl').read_bytes()).hexdigest(),
        source_export_receipt_sha256=hashlib.sha256((source/'export-receipt.json').read_bytes()).hexdigest(),
        image_id='sha256:'+'d'*64,guard=fixture_owner,timeout=30)
    if fault=='source-pin':(source/'export-receipt.json').write_bytes(b'{}')
    return module,kwargs,calls


def test_actual_stage_calls_copy_start_get_and_exact_compare(tmp_path,monkeypatch):
    module,kwargs,calls=setup(tmp_path,monkeypatch,'none')
    result=module.produce_physical_restore_readback(**kwargs)
    assert calls==['copy','start','live','client','dependencies','protection','export','raw-recheck']
    assert result['state']=='PHYSICAL_COPY_RESTORED_READBACK_PENDING_OWNER_AND_FULL_BARRIERS'
    assert result['readback']['exactVersionIdentityVerified'] is True
    assert result['productionEvidence'] is False and result['normalOwnerIntegrationVerified'] is False
    receipt=kwargs['evidence']/'physical-restore-stage.json'
    assert json.loads(receipt.read_bytes())==result
    assert (kwargs['recovery_data']/'raw').read_bytes()==b'raw-preserved'


@pytest.mark.parametrize('fault',['source-pin','copy','start','live','dependencies','protection','export','raw-recheck'])
def test_actual_stage_retains_failure_without_completion(tmp_path,monkeypatch,fault):
    module,kwargs,calls=setup(tmp_path,monkeypatch,fault)
    with pytest.raises(FormalSnapshotError):module.produce_physical_restore_readback(**kwargs)
    assert not (kwargs['evidence']/'physical-restore-stage.json').exists()
    if fault=='source-pin':assert calls==[] and not kwargs['evidence'].exists() and not kwargs['recovery_data'].exists()
    else:
        failure=json.loads((kwargs['evidence']/'physical-restore-failed.json').read_bytes())
        assert failure['state']=='FAILED_RETAINED' and failure['targetReconciliationRequired'] is True
        assert failure['remoteStopConfirmed'] is False and failure['productionEvidence'] is False


@pytest.mark.parametrize('fault',['no-guard','capture-overlap','source-overlap','preexisting-data'])
def test_actual_stage_rejects_missing_guard_or_unsafe_paths_before_effects(tmp_path,monkeypatch,fault):
    module,kwargs,calls=setup(tmp_path,monkeypatch,'none')
    if fault=='no-guard':kwargs['guard']=None
    elif fault=='capture-overlap':kwargs['recovery_data']=kwargs['capture']/'nested'
    elif fault=='source-overlap':kwargs['recovery_data']=kwargs['source_export']/'nested'
    else:kwargs['recovery_data'].mkdir()
    with pytest.raises(FormalSnapshotError):module.produce_physical_restore_readback(**kwargs)
    assert calls==[] and not kwargs['evidence'].exists()


@pytest.mark.parametrize('slow_stage', ['copy', 'export'])
def test_one_total_budget_refuses_next_effect_and_restores_context(tmp_path,monkeypatch,slow_stage):
    import time
    from formal_snapshot_linux import observation_deadline
    module,kwargs,calls=setup(tmp_path,monkeypatch,'none')
    name='prepare_checked_minio_replica' if slow_stage=='copy' else 'export_s3_versions'
    original=getattr(module,name)
    def slow(*args,**parameters):
        value=original(*args,**parameters)
        time.sleep(1.08)
        return value
    monkeypatch.setattr(module,name,slow)
    kwargs['timeout']=1
    outer=time.monotonic()+60
    token=observation_deadline.set(outer)
    try:
        with pytest.raises(FormalSnapshotError):module.produce_physical_restore_readback(**kwargs)
        assert observation_deadline.get()==outer
    finally:observation_deadline.reset(token)
    assert not (kwargs['evidence']/'physical-restore-stage.json').exists()
    if slow_stage=='copy':assert calls==['copy']
    else:assert 'raw-recheck' not in calls
    failure=json.loads((kwargs['evidence']/'physical-restore-failed.json').read_bytes())
    assert failure['targetReconciliationRequired'] is True and failure['remoteStopConfirmed'] is False


def test_total_deadline_is_installed_during_copy_and_preserves_shorter_outer(tmp_path,monkeypatch):
    import time
    from formal_snapshot_linux import observation_deadline
    for offset in (0.5,60):
        case=tmp_path/str(offset);case.mkdir()
        module,kwargs,calls=setup(case,monkeypatch,'none')
        original=module.prepare_checked_minio_replica
        outer=time.monotonic()+offset;before=time.monotonic();observed=[]
        def copy(*args,**parameters):
            observed.append(observation_deadline.get())
            return original(*args,**parameters)
        monkeypatch.setattr(module,'prepare_checked_minio_replica',copy)
        kwargs['timeout']=3
        token=observation_deadline.set(outer)
        try:
            module.produce_physical_restore_readback(**kwargs)
            assert observation_deadline.get()==outer
        finally:observation_deadline.reset(token)
        assert len(observed)==1
        if offset<3:assert observed[0]==outer
        else:assert before+3<=observed[0]<=before+3.1
