"""Real private files and explicit SQL/Docker/native protocol boundaries."""
import copy
import hashlib
import importlib
import json
from pathlib import Path

import pytest

from test_formal_source_migration_ledger import fixture, observe, refresh, UUID
from test_formal_split_capture import fixture as split_fixture, ROLES, DATA_LIMITS, REJECTION_PROBE
from formal_snapshot_linux import FormalSnapshotError, _canonical_bytes


def module():
    return importlib.import_module('formal_source_migration_ledger')


def source_scope():
    return dict(schemaVersion='release-formal-approved-source/v1', environment='prod',
                targetRawSha256='d'*64, targetFingerprint='c'*64, database='owned_probe', databaseUuid=UUID)


def saved_source(root):
    client, proof, _, _, _ = fixture()
    ledger = observe(client, proof)
    claim = _canonical_bytes(dict(operationId='owned-ledger', sourceFingerprint='c'*64,
                                 databaseUuid=UUID, minioContainerId='e'*64))
    scope = source_scope(); sql = 'b'*64
    binding = module().bind_migration_ledger(claim, ledger, ledger, sql, scope)
    names = ('013-source-migration-ledger.json', '014-source-migration-ledger-after.json', '016-source-migration-binding.json')
    values = (ledger, ledger, binding)
    for name, value in zip(names, values):
        p = root/name; p.write_bytes(_canonical_bytes(value)); p.chmod(0o600)
    ref = dict(bindingSha256=hashlib.sha256((root/names[-1]).read_bytes()).hexdigest(),
               ledgerObservationSha256=ledger['observationSha256'])
    return claim, sql, scope, proof, ledger, binding, ref


@pytest.mark.parametrize('fault', ['none', 'missing', 'changed', 'extra-reference', 'claim', 'sql',
    'scope', 'full-data', 'table', 'identity', 'rehashed-row', 'rehashed-binding', 'noncanonical', 'symlink'])
def test_reader_is_bound_to_original_claim_dump_full_data_and_receipt(tmp_path, fault):
    claim, sql, scope, proof, ledger, binding, ref = saved_source(tmp_path)
    original = (tmp_path/'013-source-migration-ledger.json').read_bytes()
    if fault == 'missing': (tmp_path/'014-source-migration-ledger-after.json').unlink()
    if fault == 'changed': (tmp_path/'014-source-migration-ledger-after.json').write_bytes(b'{}')
    if fault == 'extra-reference': ref['passed'] = True
    if fault == 'claim': claim = claim.replace(b'owned-ledger', b'other-ledger')
    if fault == 'sql': sql = 'a'*64
    if fault == 'scope': scope['targetRawSha256'] = 'a'*64
    if fault == 'full-data': proof['observationSha256'] = 'a'*64
    if fault == 'table': proof['tables']['infra_release_migration']['rows'] += 1
    if fault == 'identity': proof['observationIdentity']['containerId'] = 'b'*64
    if fault == 'rehashed-row':
        ledger['rows'][0]['status'] = 'FAILED'; refresh(ledger, 'observationSha256')
        binding = module().bind_migration_ledger(claim, ledger, ledger, sql, scope)
        for name, value in zip(('013-source-migration-ledger.json', '014-source-migration-ledger-after.json', '016-source-migration-binding.json'), (ledger, ledger, binding)):
            (tmp_path/name).write_bytes(_canonical_bytes(value))
    if fault == 'rehashed-binding':
        binding['sourceScope']['targetRawSha256'] = 'a'*64
        (tmp_path/'016-source-migration-binding.json').write_bytes(_canonical_bytes(binding))
    if fault == 'noncanonical': (tmp_path/'013-source-migration-ledger.json').write_bytes(json.dumps(ledger, indent=2).encode())
    if fault == 'symlink':
        # Cross-platform real path guard; no symlink privilege is required.
        from unittest.mock import patch
        with patch.object(Path, 'is_symlink', return_value=True):
            with pytest.raises(FormalSnapshotError, match='FORMAL_SOURCE_LEDGER_'):
                module().read_source_ledger(tmp_path, claim, sql, ref, proof, scope)
        return
    if fault == 'none':
        value, originals = module().read_source_ledger(tmp_path, claim, sql, ref, proof, scope)
        assert value == ledger and originals['013-source-migration-ledger.json'] == original
        assert len(originals) == 3 and value['productionEvidence'] is False
    else:
        with pytest.raises(FormalSnapshotError, match='FORMAL_SOURCE_LEDGER_'):
            module().read_source_ledger(tmp_path, claim, sql, ref, proof, scope)


@pytest.mark.parametrize('fault', ['none', 'rows', 'multiset', 'database', 'same-identity', 'source-environment'])
def test_restore_comparison_preserves_history_and_binds_independent_target(fault):
    client, proof, _, _, _ = fixture(); source = observe(client, proof); target = copy.deepcopy(source)
    target['observationIdentity'] = dict(containerId='b'*64, databaseUuid='22222222-2222-2222-2222-222222222222')
    target['fullDataObservationSha256'] = 'e'*64
    if fault == 'rows': target['rows'][1]['status'] = 'APPLIED'
    if fault == 'multiset': target['tableMultiset']['multisetSha256'] = 'e'*64
    if fault == 'database': target['database'] = 'other_probe'
    if fault == 'same-identity': target['observationIdentity'] = copy.deepcopy(source['observationIdentity'])
    if fault == 'source-environment': target['sourceEnvironment'] = 'backup'
    refresh(target, 'observationSha256')
    if fault == 'none':
        result = module().compare_restored_ledger(source, target)
        assert result['sourceIdentity'] == source['observationIdentity']
        assert result['targetIdentity'] == target['observationIdentity'] and result['rowCount'] == 3
        assert result['sourceLedgerSha256'] == source['observationSha256'] and result['targetLedgerSha256'] == target['observationSha256']
        assert result['applicationVerified'] is False and result['productionEvidence'] is False
        assert target['rows'][1]['status'] == 'FAILED' and target['rows'][2]['deleted'] is True
    else:
        with pytest.raises(FormalSnapshotError, match='FORMAL_SOURCE_LEDGER_'):
            module().compare_restored_ledger(source, target)


def approved_target():
    return dict(schemaVersion='release-formal-target/v1', environment='prod', host='owned-host',
        machineId='1'*32, project='owned-project', root='/owned/app', dataMount='/owned/data',
        dataDevice='/dev/owned', dataRoot='/owned/data/runtime', releaseRoot='/owned/releases',
        frontendPort=8081, backendPort=48081, database='owned_probe', databaseServerUuid=UUID,
        containers={role:'owned-'+role for role in ('mysql','redis','minio','backend','frontend')})


def source_request_author_fixture(tmp_path,monkeypatch):
    """Actual private files; native device/nft/MySQL observations are named bridges."""
    import formal_capture_producer as producer
    import formal_ingress_control as ingress
    import formal_split_capture as split
    from test_formal_capture_producer_entry import shared_request
    request=shared_request(tmp_path,'capture-shared-payload')
    target=dict(approved_target(),databaseServerUuid=request['claimBinding']['databaseUuid'])
    request['sourceRoot']=target['dataRoot']+'/minio'
    request['filesystemFence']['mount']=target['dataMount']
    request['filesystemFence']['binding']['mount']['target']=target['dataMount']
    request['filesystemFence']['binding']['mount']['source']=target['dataDevice']
    target_raw=json.dumps(target,indent=2).encode('utf-8')
    target_path=tmp_path/'approved-target.json';target_path.write_bytes(target_raw)
    approved_sha=hashlib.sha256(target_raw).hexdigest()
    binding=dict(request['claimBinding'],sourceFingerprint=hashlib.sha256(json.dumps(target,
        sort_keys=True,separators=(',',':'),ensure_ascii=False).encode('utf-8')).hexdigest())
    root=tmp_path/'native-claim';root.mkdir()
    (root/'binding.json').write_bytes(_canonical_bytes(binding))
    for name,value in (
        ('shared-container-original.json',{role:dict(id='c'*64 if role=='mysql' else
            binding['minioContainerId'] if role=='minio' else str(index)*64,
            name='/'+target['containers'][role],mounts=[dict(Source=request['sourceRoot'],Destination='/data',RW=True)]
            if role=='minio' else []) for index,role in enumerate(target['containers'],1)}),
        ('shared-health-original.json',{'fixtureOnly':'exact-original-health'})):
        (root/name).write_bytes(_canonical_bytes(value));(root/name).chmod(0o600)
    secret=Path(request['mysqlCredentialFile']);secret.write_bytes(b'owned-private-only\n');secret.chmod(0o600)
    options={key:copy.deepcopy(request[key]) for key in
        ('database','sourceRoot','mysqlCredentialFile','totalSeconds','dataLimits','rejectionProbe','filesystemFence')}
    calls=[]
    class Claim:
        _plan=_canonical_bytes(binding)
        def __init__(self):self.root=root;self.live=True
        def assert_owned(self):
            calls.append('owned')
            if not self.live:raise FormalSnapshotError('FORMAL_MAINTENANCE_OWNER_INVALID')
            assert (self.root/'binding.json').read_bytes()==self._plan
        def _read(self,path):return path.read_bytes()
        def _write(self,path,raw):
            calls.append('write')
            with path.open('xb') as stream:stream.write(raw)
            path.chmod(0o600)
    claim=Claim()
    held={'rulesSha256':'f'*64,'phase':'CAPTURE_RESTRICTED'}
    def originals(selected,body):
        calls.append('bound-originals')
        assert body['claimRoot']==str(root) and body['claimBinding']==binding
        assert body['sharedScopeSha256']==hashlib.sha256((root/'shared-container-original.json').read_bytes()).hexdigest()
        assert body['originalHealthSha256']==hashlib.sha256((root/'shared-health-original.json').read_bytes()).hexdigest()
        assert body['mysqlContainerId']=='c'*64
        return {'shared':(root/'shared-container-original.json').read_bytes(),
                'health':(root/'shared-health-original.json').read_bytes()}
    monkeypatch.setattr(producer,'verify_shared_request_binding',originals)
    monkeypatch.setattr(ingress,'assert_ingress',lambda selected:dict(held))
    monkeypatch.setattr(split,'_thaw_receipt',lambda selected:{'nativeState':'NOT_REQUESTED'})
    monkeypatch.setattr(producer,'verify_shared_database_before_stop',lambda *a:calls.append('real-db-fence-observer-bridge'))
    monkeypatch.setattr(producer,'_observe_approved_source_storage',
        lambda *a:(calls.append('native-approved-mount-and-alias-observer-bridge') or
            {'native-protocol-fixture-only':(9,1)}),raising=False)
    return producer,claim,options,target_path,approved_sha,calls,held


@pytest.mark.parametrize('fault',['none','none-alias','alternate-source','other-fence','other-device','wrong-bind-root',
    'minio-source','duplicate-minio','role-name','main-mount-drift','alias-stat-drift',
    'late-mount-drift','late-alias-drift'])
def test_source_request_author_storage_must_match_approved_target_and_original(tmp_path,monkeypatch,fault):
    producer,claim,options,target_path,approved_sha,calls,held=source_request_author_fixture(tmp_path,monkeypatch)
    target=json.loads(target_path.read_bytes())
    if fault=='none-alias':
        options['sourceRoot']=target['root']+'/data/minio'
        options['filesystemFence']['mount']=target['root']+'/data'
        options['filesystemFence']['binding']['mount'].update(target=target['root']+'/data',
            source=target['dataDevice']+'[/runtime]')
    if fault=='alternate-source':options['sourceRoot']='/another-valid-disk/minio'
    if fault=='other-fence':
        options['filesystemFence']['mount']='/another-valid-disk'
        options['filesystemFence']['binding']['mount']['target']='/another-valid-disk'
    if fault=='other-device':options['filesystemFence']['binding']['mount']['source']='/dev/another'
    if fault=='wrong-bind-root':
        options['sourceRoot']=target['root']+'/data/minio'
        options['filesystemFence']['mount']=target['root']+'/data'
        options['filesystemFence']['binding']['mount'].update(target=target['root']+'/data',
            source=target['dataDevice']+'[/another-subtree]')
    originals_path=claim.root/'shared-container-original.json'
    originals=json.loads(originals_path.read_bytes())
    if fault=='minio-source':originals['minio']['mounts'][0]['Source']='/same-disk/wrong-minio'
    if fault=='duplicate-minio':originals['minio']['mounts'].append(copy.deepcopy(originals['minio']['mounts'][0]))
    if fault=='role-name':originals['mysql']['name']='/another-mysql'
    if fault in ('wrong-bind-root','none-alias'):originals['minio']['mounts'][0]['Source']=options['sourceRoot']
    originals_path.write_bytes(_canonical_bytes(originals))
    def observer(*a):
        calls.append('native-approved-mount-and-alias-observer-bridge')
        if fault in ('main-mount-drift','alias-stat-drift') or (
            fault in ('late-mount-drift','late-alias-drift') and 'write' in calls):
            raise FormalSnapshotError('FORMAL_CAPTURE_APPROVED_STORAGE_CHANGED')
        return {'native-protocol-fixture-only':(9,1)}
    monkeypatch.setattr(producer,'_observe_approved_source_storage',observer,raising=False)
    if fault in ('none','none-alias'):
        result=producer.author_source_capture_request(claim,options,target_path,approved_sha,guard=lambda:None)
        assert result['fullRunnerCompleted'] is False
        assert calls.count('native-approved-mount-and-alias-observer-bridge')>=2
    else:
        with pytest.raises(FormalSnapshotError,match='FORMAL_CAPTURE_APPROVED_STORAGE_CHANGED'):
            producer.author_source_capture_request(claim,options,target_path,approved_sha,guard=lambda:None)
    assert calls.count('write')==(1 if fault in ('none','none-alias','late-mount-drift','late-alias-drift') else 0)
    assert 'service-action' not in calls


@pytest.mark.parametrize('fault',['none','before-write','after-write'])
def test_source_request_author_pins_actual_inode_across_all_guards(tmp_path,monkeypatch,fault):
    producer,claim,options,target_path,approved_sha,calls,held=source_request_author_fixture(tmp_path,monkeypatch)
    leaf=tmp_path/'owned-observed-minio';leaf.mkdir()
    def observe(*a):
        # Linux mount/name translation is a named bridge; this inode is an actual directory.
        info=leaf.stat();return {str(leaf):(info.st_dev,info.st_ino)}
    monkeypatch.setattr(producer,'_observe_approved_source_storage',observe)
    guards=[]
    def replace():leaf.rename(tmp_path/'retained-original-minio');leaf.mkdir()
    def guard():
        guards.append(True)
        if fault=='before-write' and len(guards)==2:replace()
    original_write=claim._write
    def write(path,raw):
        original_write(path,raw)
        if fault=='after-write':replace()
    monkeypatch.setattr(claim,'_write',write)
    if fault=='none':
        assert producer.author_source_capture_request(claim,options,target_path,approved_sha,guard=guard)['fullRunnerCompleted'] is False
    else:
        with pytest.raises(FormalSnapshotError,match='FORMAL_CAPTURE_APPROVED_STORAGE_CHANGED'):
            producer.author_source_capture_request(claim,options,target_path,approved_sha,guard=guard)
        assert (tmp_path/'retained-original-minio').is_dir()
    assert calls.count('write')==(0 if fault=='before-write' else 1)


@pytest.mark.parametrize('fault',['none','same-device-other-inode','unproven-alias','missing-source','late-replacement'])
def test_source_request_author_actual_directory_identity_rejects_same_disk_alias_guess(tmp_path,monkeypatch,fault):
    import formal_capture_producer as producer
    import formal_split_capture as split
    data=tmp_path/'data';data.mkdir();leaf=data/'minio';leaf.mkdir()
    app=tmp_path/'app';app.mkdir()
    target=dict(dataMount=str(tmp_path),dataRoot=str(data),root=str(app),dataDevice='/dev/owned')
    request=dict(sourceRoot=str(leaf),filesystemFence=dict(mount=str(tmp_path),
        binding=dict(operationId='owned-alias',bootId=UUID,
            mount=dict(source='/dev/owned',device='9:1',fstype='ext4',target=str(tmp_path)))))
    if fault=='same-device-other-inode':
        other=tmp_path/'another-minio';other.mkdir();request['sourceRoot']=str(other)
        assert other.stat().st_dev==leaf.stat().st_dev and other.stat().st_ino!=leaf.stat().st_ino
    if fault=='unproven-alias':
        alias=app/'data';alias.mkdir();(alias/'minio').mkdir();request['sourceRoot']=str(alias/'minio')
        request['filesystemFence']['mount']=str(alias)
        assert alias.stat().st_dev==data.stat().st_dev and alias.stat().st_ino!=data.stat().st_ino
    if fault=='missing-source':request['sourceRoot']=str(tmp_path/'absent')
    observations=[]
    def native_observer(binding):
        # Named native mount bridge; directory identities below are actual files.
        observations.append(copy.deepcopy(binding))
        if fault=='late-replacement' and len(observations)==1:
            leaf.rename(data/'old-minio-retained');leaf.mkdir()
    monkeypatch.setattr(split,'_observe_native_environment',native_observer)
    if fault=='none':
        producer._observe_approved_source_storage(request,target)
        assert len(observations)==2
        assert observations[0]['mount']['source']==target['dataDevice']
        assert observations[0]['mount']['target']==target['dataMount']
    else:
        with pytest.raises(FormalSnapshotError,match='FORMAL_CAPTURE_APPROVED_STORAGE_CHANGED'):
            producer._observe_approved_source_storage(request,target)


@pytest.mark.parametrize('fault',['none','missing-target','approval-sha','missing-shared','missing-health',
    'claim-not-live','ingress-phase','input-extra','deadline','target-drift','shared-drift',
    'credential-drift','source-options-drift','existing-request','write-error','saved-request-drift'])
def test_source_request_author_uses_owned_originals_and_independent_approval(tmp_path,monkeypatch,fault):
    producer,claim,options,target_path,approved_sha,calls,held=source_request_author_fixture(tmp_path,monkeypatch)
    request_path=claim.root/'source-capture-request.json'
    original_plan=claim._plan
    if fault=='missing-target':target_path.unlink()
    if fault=='approval-sha':approved_sha='a'*64
    if fault=='missing-shared':(claim.root/'shared-container-original.json').unlink()
    if fault=='missing-health':(claim.root/'shared-health-original.json').unlink()
    if fault=='claim-not-live':claim.live=False
    if fault=='ingress-phase':held['phase']='RECOVERY_DEPENDENCIES_PERMITTED'
    if fault=='input-extra':options['sourceScope']={'environment':'prod'}
    if fault=='deadline':options['totalSeconds']=True
    if fault=='existing-request':request_path.write_bytes(b'old-request-not-overwritten')
    guards=[]
    def guard():
        guards.append(True)
        if len(guards)==2:
            if fault=='target-drift':target_path.write_bytes(b'{}')
            if fault=='shared-drift':(claim.root/'shared-container-original.json').write_bytes(b'{}')
            if fault=='credential-drift':Path(options['mysqlCredentialFile']).write_bytes(b'changed-private-only\n')
            if fault=='source-options-drift':options['filesystemFence']['seconds']=21
    original_write=claim._write
    def write(path,raw):
        if fault=='write-error':raise OSError('owned fixture persistence failure')
        original_write(path,raw)
        if fault=='saved-request-drift':path.write_bytes(b'{}')
    monkeypatch.setattr(claim,'_write',write)
    if fault=='none':
        result=producer.author_source_capture_request(claim,options,target_path,approved_sha,guard=guard)
        saved=request_path.read_bytes();body=json.loads(saved)
        assert body['schemaVersion']=='release-formal-capture-request/v12'
        assert body['claimBinding']==json.loads(original_plan)
        assert body['sourceScope']==module().approved_source_scope(target_path.read_bytes(),approved_sha,original_plan,options['database'])
        assert result['state']=='SOURCE_CAPTURE_REQUEST_PREPARED_INGRESS_HELD'
        assert result['requestFile']==str(request_path) and result['requestSha256']==hashlib.sha256(saved).hexdigest()
        assert result['fullRunnerCompleted'] is result['claimReleased'] is result['productionEvidence'] is False
        assert result['serviceActionsPerformed'] is False and calls.count('write')==1
        assert guards and 'bound-originals' in calls and 'real-db-fence-observer-bridge' in calls
    else:
        with pytest.raises(FormalSnapshotError):
            producer.author_source_capture_request(claim,options,target_path,approved_sha,guard=guard)
        assert calls.count('write')==(1 if fault=='saved-request-drift' else 0)
        if fault=='existing-request':assert request_path.read_bytes()==b'old-request-not-overwritten'
    assert claim._plan==original_plan


@pytest.mark.parametrize('fault',['none','missing-approval','approval-sha','input-sha','input-drift','target-drift','input-extra','claim-binding'])
def test_source_request_author_standard_cli_is_explicit_and_not_full_success(tmp_path,monkeypatch,capsys,fault):
    producer,claim,options,target_path,approved_sha,calls,held=source_request_author_fixture(tmp_path,monkeypatch)
    body=dict(schemaVersion='release-formal-capture-author-input/v1',action='prepare-source-capture',
        claimRoot=str(claim.root),claimBinding=json.loads(claim._plan),**options)
    if fault=='input-extra':body['approved']=True
    if fault=='claim-binding':body['claimBinding']['sourceFingerprint']='a'*64
    input_path=tmp_path/'author-input.json';input_raw=_canonical_bytes(body)
    input_path.write_bytes(input_raw);input_path.chmod(0o600)
    input_sha=hashlib.sha256(input_raw).hexdigest()
    monkeypatch.setattr(producer,'require_native_runtime',lambda:None)
    acquired=[]
    def acquire(root,binding,*,resume):
        assert root==str(claim.root) and binding==json.loads(claim._plan) and resume is True
        acquired.append(True);return claim
    def close():calls.append('close')
    monkeypatch.setattr(claim,'close',close,raising=False)
    monkeypatch.setattr(producer,'FormalMaintenanceClaim',acquire)
    original=producer.author_source_capture_request
    def author(selected,selected_options,target,approved,*,guard):
        if fault=='input-drift':input_path.write_bytes(b'{}')
        if fault=='target-drift':target_path.write_bytes(b'{}')
        return original(selected,selected_options,target,approved,guard=guard)
    monkeypatch.setattr(producer,'author_source_capture_request',author)
    monkeypatch.setattr(producer,'run_shared_request',lambda *a,**k:pytest.fail('author must not dispatch capture'))
    args=['--request',str(input_path),'--expected-request-sha256','a'*64 if fault=='input-sha' else input_sha]
    if fault!='missing-approval':args+=['--approved-source-target',str(target_path),
        '--expected-approved-source-target-sha256','a'*64 if fault=='approval-sha' else approved_sha]
    exit_code=producer.main(args)
    result=json.loads(capsys.readouterr().out)
    if fault=='none':
        assert exit_code==0 and result['state']=='SOURCE_CAPTURE_REQUEST_PREPARED_INGRESS_HELD'
        assert result['fullRunnerCompleted'] is result['claimReleased'] is result['productionEvidence'] is False
        assert acquired==[True] and calls[-1]=='close' and calls.count('write')==1
    else:
        assert exit_code==1 and result['state']=='FAILED_RETAINED' and 'write' not in calls
        if fault in ('missing-approval','approval-sha','input-sha','input-extra','claim-binding'):assert not acquired
        else:assert acquired==[True] and calls[-1]=='close'


@pytest.mark.parametrize('fault', ['none', 'digest', 'environment', 'database', 'uuid', 'extra', 'duplicate', 'boolean-port','claim-source'])
def test_approved_source_is_derived_from_independent_original_target_bytes(fault):
    value = approved_target(); claim = dict(operationId='owned-ledger', sourceFingerprint='f'*64, databaseUuid=UUID, minioContainerId='e'*64)
    if fault == 'environment': value['environment'] = 'backup'
    if fault == 'database': value['database'] = 'other_probe'
    if fault == 'uuid': value['databaseServerUuid'] = '22222222-2222-2222-2222-222222222222'
    if fault == 'extra': value['approved'] = True
    if fault == 'boolean-port': value['backendPort'] = True
    claim['sourceFingerprint']=hashlib.sha256(json.dumps(approved_target(),sort_keys=True,separators=(',',':'),ensure_ascii=False).encode('utf-8')).hexdigest()
    if fault == 'claim-source':claim['sourceFingerprint']='f'*64
    raw = json.dumps(value, indent=2).encode()
    if fault == 'duplicate': raw = raw.replace(b'"environment": "prod",', b'"environment": "prod", "environment": "prod",')
    digest = 'a'*64 if fault == 'digest' else hashlib.sha256(raw).hexdigest()
    if fault == 'none':
        result = module().approved_source_scope(raw, digest, _canonical_bytes(claim), 'owned_probe')
        assert result['environment'] == 'prod' and result['databaseUuid'] == UUID
        assert result['targetRawSha256'] == digest
        # Registered target identity is canonical JSON without a final LF,
        # matching the standard Java/Python target consumers.
        assert result['targetFingerprint'] == hashlib.sha256(json.dumps(value,sort_keys=True,
            separators=(',',':'),ensure_ascii=False).encode('utf-8')).hexdigest()
    else:
        with pytest.raises(FormalSnapshotError, match='FORMAL_S3_JSON_DUPLICATE_KEY' if fault == 'duplicate' else 'FORMAL_SOURCE_LEDGER_'):
            module().approved_source_scope(raw, digest, _canonical_bytes(claim), 'owned_probe')


@pytest.mark.parametrize('fault', ['none', 'before-missing-table', 'after-row-drift', 'after-file-drift', 'raw-file-drift', 'scope','source-fingerprint'])
def test_actual_split_capture_retains_ledger_before_dump_after_dump_and_raw(tmp_path, monkeypatch, fault):
    split, claim, mysql, source, rows, guard, guards, patchers = split_fixture(tmp_path)
    client, proof, physical, _, _ = fixture()
    client.container = mysql.container
    binding = json.loads(claim._plan); binding['databaseUuid'] = UUID; binding['sourceFingerprint']='c'*64;claim._plan = _canonical_bytes(binding)
    for value in (proof['schema'], proof):
        value['observationIdentity'] = dict(containerId=mysql.container, databaseUuid=UUID)
        refresh(value, 'observationSha256')
    scope = source_scope(); calls = []
    monkeypatch.setattr(split, 'DataMySql', lambda *a, **k: client)
    def data(*a, **k):
        calls.append('data'); k['guard']()
        value = copy.deepcopy(proof)
        if fault == 'before-missing-table':
            del value['tables']['infra_release_migration']; value['schema']['catalog']['TABLES'].pop()
        return value
    monkeypatch.setattr(split, 'observe_data', data)
    original_dump = mysql.dump
    def dump(database, path, *, guard):
        assert (path.parent/'013-source-migration-ledger.json').is_file()
        calls.append('dump'); value = original_dump(database, path, guard=guard)
        if fault == 'after-row-drift': physical[0][5] = 'FAILED'
        return value
    monkeypatch.setattr(mysql, 'dump', dump)
    if fault == 'scope': scope['databaseUuid'] = '22222222-2222-2222-2222-222222222222'
    if fault == 'source-fingerprint':scope['targetFingerprint']='f'*64
    try:
        split.bind_shared_container_scope(claim, {role:str(i)*64 for i,role in enumerate(ROLES,1)})
        claim.record('db-fenced', {})
        for role in split.STOP_ORDER[:-1]: split.stop_shared_container(claim, role, guard=guard)
        if fault in ('before-missing-table', 'after-row-drift', 'scope','source-fingerprint'):
            with pytest.raises(FormalSnapshotError, match='FORMAL_SOURCE_LEDGER_'):
                split.capture_sql_phase(claim,mysql,'owned_probe',source,guard=guard,data_limits=DATA_LIMITS,
                    rejection_probe=REJECTION_PROBE,source_scope=scope)
            assert not (claim.root/'payload/020-sql.json').exists()
            if fault == 'before-missing-table': assert 'dump' not in calls
            return
        sql = split.capture_sql_phase(claim,mysql,'owned_probe',source,guard=guard,data_limits=DATA_LIMITS,
            rejection_probe=REJECTION_PROBE,source_scope=scope)
        assert calls == ['data','dump','data']
        assert sql['sourceScope'] == scope and 'sourceMigrationEvidence' in sql
        payload = claim.root/'payload'
        assert (payload/'013-source-migration-ledger.json').read_bytes() == (payload/'014-source-migration-ledger-after.json').read_bytes()
        split.stop_shared_container(claim,'mysql',guard=guard)
        fence = dict(mount=str(source),seconds=5,binding=dict(operationId=binding['operationId'],bootId='owned-boot',
            mount=dict(source='/dev/owned',device='9:1',fstype='ext4',target=str(source))))
        if fault == 'after-file-drift': (payload/'013-source-migration-ledger.json').write_bytes(b'{}')
        if fault == 'raw-file-drift':
            original_native = split.snapshot._capture_native_physical
            def native(*a, **k):
                value = original_native(*a, **k); (payload/'014-source-migration-ledger-after.json').write_bytes(b'{}'); return value
            monkeypatch.setattr(split.snapshot,'_capture_native_physical',native)
        if fault in ('after-file-drift','raw-file-drift'):
            with pytest.raises(FormalSnapshotError,match='FORMAL_SOURCE_LEDGER_'):
                split.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
            assert not (payload/'040-payload.json').exists()
        else:
            result = split.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
            assert result['sourceMigrationEvidence'] == sql['sourceMigrationEvidence'] and result['sourceScope'] == scope
            assert result['hostWritersVerified'] is False
    finally:
        for patcher in reversed(patchers): patcher.stop()


@pytest.mark.parametrize('fault', ['none','missing-approval','scope','version','restore-action','extra','claim-source'])
def test_v12_route_requires_independent_source_approval_before_shared_calls(tmp_path, monkeypatch, fault):
    import formal_capture_producer as producer
    from test_formal_capture_producer_entry import shared_request
    request = shared_request(tmp_path,'capture-shared-payload')
    request['schemaVersion'] = 'release-formal-capture-request/v12'
    scope = dict(source_scope(), databaseUuid=request['claimBinding']['databaseUuid'])
    request['sourceScope'] = scope
    request['claimBinding']['sourceFingerprint']=scope['targetFingerprint']
    selected = copy.deepcopy(scope)
    if fault == 'scope': selected['targetRawSha256'] = 'a'*64
    if fault == 'version': request['schemaVersion'] = 'release-formal-capture-request/v3'
    if fault == 'restore-action': request['action'] = 'restore-shared-services'
    if fault == 'extra': request['approved'] = True
    if fault == 'claim-source': request['claimBinding']['sourceFingerprint']='f'*64
    if fault == 'none': producer.validate_source_request(request,selected)
    else:
        import formal_split_capture as split
        calls=[]
        monkeypatch.setattr(split,'_thaw_receipt',lambda *a:calls.append('shared-call'))
        with pytest.raises(FormalSnapshotError, match='FORMAL_CAPTURE_'):
            producer.run_shared_request(object(),request,lambda:None,
                approved_source=None if fault == 'missing-approval' else selected)
        assert calls == []


@pytest.mark.parametrize('fault', ['none','missing-approval','approval-sha','target-drift','preclaim-target-drift','scope','legacy-approval','claim-source',
    'wrong-source','other-mount','minio-original','role-name','late-storage','between-guard-storage-drift'])
def test_v12_standard_cli_pins_independent_target_original_bytes(tmp_path, monkeypatch, capsys, fault):
    import formal_service_health as health
    producer,owned,options,target_path,sha,native_calls,held=source_request_author_fixture(tmp_path,monkeypatch)
    from test_formal_capture_producer_entry import shared_request
    request = shared_request(tmp_path,'capture-shared-payload')
    request.update(options,claimRoot=str(owned.root),claimBinding=json.loads(owned._plan))
    raw=target_path.read_bytes()
    request['schemaVersion']='release-formal-capture-request/v12'
    request['sourceScope']=module().approved_source_scope(raw,sha,_canonical_bytes(request['claimBinding']),request['database'])
    if fault == 'claim-source':request['claimBinding']['sourceFingerprint']='f'*64
    if fault == 'scope': request['sourceScope']['environment']='test'
    if fault == 'legacy-approval': request.pop('sourceScope');request['schemaVersion']='release-formal-capture-request/v3'
    if fault=='wrong-source':request['sourceRoot']='/another-valid-disk/minio'
    if fault=='other-mount':
        request['filesystemFence']['mount']='/another-valid-disk'
        request['filesystemFence']['binding']['mount']['target']='/another-valid-disk'
    original_path=owned.root/'shared-container-original.json'
    original=json.loads(original_path.read_bytes())
    if fault=='minio-original':original['minio']['mounts'][0]['Source']='/same-disk/wrong-minio'
    if fault=='role-name':original['mysql']['name']='/other-mysql'
    original_path.write_bytes(_canonical_bytes(original))
    request['sharedScopeSha256']=hashlib.sha256(original_path.read_bytes()).hexdigest()
    request['originalHealthSha256']=hashlib.sha256((owned.root/'shared-health-original.json').read_bytes()).hexdigest()
    path=tmp_path/'request.json';path.write_bytes(_canonical_bytes(request));path.chmod(0o600)
    calls=[]
    leaf=tmp_path/'owned-consumer-minio';leaf.mkdir()
    def observe(*a):
        info=leaf.stat();return {str(leaf):(info.st_dev,info.st_ino)}
    monkeypatch.setattr(producer,'_observe_approved_source_storage',observe)
    def Claim(*a,**k):calls.append('claim');return owned
    monkeypatch.setattr(owned,'close',lambda:calls.append('close'),raising=False)
    def run(claim,selected,guard,*,approved_source,native_guard):
        calls.append('run');assert approved_source==selected['sourceScope'];native_guard();guard()
        if fault == 'target-drift': target_path.write_bytes(b'{}')
        if fault=='late-storage':
            original['minio']['mounts'][0]['Source']='/same-disk/wrong-minio'
            original_path.write_bytes(_canonical_bytes(original))
        if fault=='between-guard-storage-drift':
            leaf.rename(tmp_path/'retained-original-consumer-minio');leaf.mkdir()
        guard()
        return dict(state='PROTOCOL_FIXTURE_ONLY',productionEvidence=False)
    monkeypatch.setattr(producer,'require_native_runtime',lambda:None)
    monkeypatch.setattr(health,'NativeNetworkProbe',lambda:target_path.write_bytes(b'{}') if fault=='preclaim-target-drift' else None)
    monkeypatch.setattr(producer,'FormalMaintenanceClaim',Claim)
    monkeypatch.setattr(producer,'run_shared_request',run)
    args=['--request',str(path),'--expected-request-sha256',hashlib.sha256(path.read_bytes()).hexdigest()]
    if fault!='missing-approval': args += ['--approved-source-target',str(target_path),'--expected-approved-source-target-sha256','a'*64 if fault=='approval-sha' else sha]
    assert producer.main(args)==(0 if fault=='none' else 1)
    result=json.loads(capsys.readouterr().out)
    if fault=='none':assert result['state']=='PROTOCOL_FIXTURE_ONLY'
    elif fault in ('target-drift','late-storage','between-guard-storage-drift'):assert calls==['claim','run','close'] and result['state']=='FAILED_RETAINED'
    elif fault in ('wrong-source','other-mount','minio-original','role-name'):
        assert calls==['claim','close'] and result['state']=='FAILED_RETAINED'
    else:assert 'claim' not in calls and result['state']=='FAILED_RETAINED'


@pytest.mark.parametrize('fault',['none','missing-ledger','source-drift','scope','legacy-downgrade',
    'target-row','after-import-source','target-ledger-file','target-comparison-file'])
def test_v2_restore_route_rechecks_actual_retained_ledger_and_target_files(tmp_path, monkeypatch, capsys, fault):
    from test_formal_sql_restore import fixture as restore_fixture, load, Mysql
    restore=load();request,path,row=restore_fixture(tmp_path)
    capture=tmp_path/'capture'
    claim,sql,scope,proof,ledger,binding,ref=saved_source(capture)
    # All original references are built before the request is pinned; no test
    # drift is incorporated into the request/capture approval afterwards.
    sql=request['sourceSqlSha256'];claim_path=tmp_path/'binding.json';claim_path.write_bytes(claim)
    binding=module().bind_migration_ledger(claim,ledger,ledger,sql,scope)
    (capture/'016-source-migration-binding.json').write_bytes(_canonical_bytes(binding))
    ref['bindingSha256']=hashlib.sha256(_canonical_bytes(binding)).hexdigest()
    from formal_data_evidence import source_binding
    data_binding=source_binding(claim,proof,proof,sql)
    for name,value in (('011-source-data.json',proof),('012-source-data-after.json',proof),('015-source-data-binding.json',data_binding)):
        (capture/name).write_bytes(_canonical_bytes(value));(capture/name).chmod(0o600)
    payload=json.loads((capture/'040-payload.json').read_bytes())
    payload.update(sourceScope=scope,sourceMigrationEvidence=ref,
        sourceDataEvidence=dict(bindingSha256=hashlib.sha256(_canonical_bytes(data_binding)).hexdigest(),sourceObservationSha256=proof['observationSha256']))
    (capture/'040-payload.json').write_bytes(_canonical_bytes(payload))
    request.update(schemaVersion='release-formal-sql-restore-request/v2',sourceScope=scope,
        sourceDatabaseUuid=UUID,sourceClaimBindingSha256=hashlib.sha256(claim).hexdigest(),
        sourceCaptureReceiptSha256=hashlib.sha256(_canonical_bytes(payload)).hexdigest())
    request['target']['containerId']='b'*64;row['Id']='b'*64
    if fault=='scope':request['sourceScope']=dict(scope,environment='backup')
    if fault=='legacy-downgrade':request.pop('sourceScope');request['schemaVersion']='release-formal-sql-restore-request/v1'
    path.write_bytes(_canonical_bytes(request));digest=hashlib.sha256(path.read_bytes()).hexdigest()
    if fault=='missing-ledger':(capture/'013-source-migration-ledger.json').unlink()
    if fault=='source-drift':(capture/'014-source-migration-ledger-after.json').write_bytes(b'{}')
    mysql=Mysql(request['target']);calls=[]
    def importer(client,*a,**k):
        calls.append('import');mysql.imported=True
        if fault=='after-import-source':(capture/'013-source-migration-ledger.json').write_bytes(b'{}')
    def data(client,database,uuid,*,guard,**kwargs):
        guard();assert mysql.imported
        result=copy.deepcopy(proof)
        for value in (result['schema'],result):
            value['observationIdentity']=dict(containerId=request['target']['containerId'],databaseUuid=uuid)
            refresh(value,'observationSha256')
        return result
    def target_ledger(client,database,uuid,*,source_environment,full_data,guard,max_rows):
        calls.append('target-ledger');guard();assert source_environment=='prod' and full_data['observationIdentity']['databaseUuid']==uuid
        result=copy.deepcopy(ledger);result['observationIdentity']=copy.deepcopy(full_data['observationIdentity'])
        result['fullDataObservationSha256']=full_data['observationSha256']
        if fault=='target-row':result['rows'][1]['status']='APPLIED'
        refresh(result,'observationSha256');return result
    record=restore.private_record
    def write(root,name,value):
        record(root,name,value)
        if name=={'target-ledger-file':'024-target-migration-ledger.json','target-comparison-file':'035-migration-comparison.json'}.get(fault):
            (root/name).write_bytes(b'{}')
    monkeypatch.setattr(restore,'require_native_runtime',lambda:None)
    def inspect_protocol(*args, guard):
        guard();return row
    monkeypatch.setattr(restore,'_private_docker_json',inspect_protocol)
    monkeypatch.setattr(restore,'IsolatedTargetMySql',lambda *a,**k:mysql)
    monkeypatch.setattr(restore,'stream_import',importer)
    monkeypatch.setattr(restore,'observe_data',data)
    monkeypatch.setattr(restore,'observe_migration_ledger',target_ledger,raising=False)
    monkeypatch.setattr(restore,'private_record',write)
    assert restore.main(['--request',str(path),'--expected-request-sha256',digest])==(0 if fault=='none' else 1)
    result=json.loads(capsys.readouterr().out)
    if fault=='none':
        assert result['schemaVersion']=='release-formal-sql-restore/v2'
        assert result['sourceScope']==scope and result['sourceMigrationEvidence']==ref
        assert result['applicationVerified'] is False and result['productionEvidence'] is False
        assert calls==['import','target-ledger']
        for name,key in (('024-target-migration-ledger.json','targetMigrationFileSha256'),('035-migration-comparison.json','migrationComparisonFileSha256')):
            assert hashlib.sha256((tmp_path/'evidence'/name).read_bytes()).hexdigest()==result[key]
    else:
        assert result['state']=='FAILED_RETAINED' and not (tmp_path/'evidence/restore-receipt.json').exists()
        if fault in ('missing-ledger','source-drift','scope','legacy-downgrade'):assert calls==[]
        else:assert result['stage']=='READBACK' and result['targetReconciliationRequired'] is True


@pytest.mark.parametrize('fault',['none','sql-scope','raw-reference','missing-ledger'])
def test_v12_actual_phase_router_preserves_source_and_refuses_drift(tmp_path,monkeypatch,fault):
    import formal_capture_producer as producer
    import formal_split_capture as split
    import formal_ingress_control as ingress
    from test_formal_capture_producer_entry import shared_request
    request=shared_request(tmp_path,'capture-shared-payload')
    request['schemaVersion']='release-formal-capture-request/v12'
    scope=dict(source_scope(),databaseUuid=request['claimBinding']['databaseUuid'])
    request['sourceScope']=scope;request['claimBinding']['sourceFingerprint']=scope['targetFingerprint']
    Path(request['mysqlCredentialFile']).write_bytes(b'unit-private\n');Path(request['mysqlCredentialFile']).chmod(0o600)
    class Claim:
        events=[]
        _plan=_canonical_bytes(request['claimBinding'])
        root=tmp_path
        def assert_owned(self):pass
        def _read(self,path):return b'original'
    claim=Claim();calls=[]
    monkeypatch.setattr(split,'_thaw_receipt',lambda *a:dict(nativeState='NOT_REQUESTED'))
    monkeypatch.setattr(producer,'verify_shared_request_binding',lambda *a:dict(shared=b'original',health=b'original'))
    monkeypatch.setattr(producer,'verify_shared_database_before_stop',lambda *a:calls.append('barrier'))
    monkeypatch.setattr(producer,'PrivateDockerMySql',lambda *a,**k:object())
    monkeypatch.setattr(ingress,'assert_ingress',lambda *a:dict(rulesSha256=request['ingressRulesSha256'],phase='CAPTURE_RESTRICTED'))
    monkeypatch.setattr(split,'stop_shared_container',lambda c,role,**k:calls.append('stop:'+role))
    reference=dict(bindingSha256='a'*64,ledgerObservationSha256='b'*64)
    def sql(*a,**k):
        assert k['source_scope']==scope;k['guard']();calls.append('sql')
        result=dict(sourceScope=copy.deepcopy(scope),sourceMigrationEvidence=reference)
        if fault=='sql-scope':result['sourceScope']['targetFingerprint']='f'*64
        if fault=='missing-ledger':result.pop('sourceMigrationEvidence')
        return result
    def raw(*a,**k):
        k['guard']();calls.append('raw')
        return dict(sourceScope=scope,sourceMigrationEvidence=dict(reference,bindingSha256='c'*64) if fault=='raw-reference' else reference)
    monkeypatch.setattr(split,'capture_sql_phase',sql);monkeypatch.setattr(split,'capture_raw_phase',raw)
    if fault=='none':
        result=producer.run_shared_request(claim,request,lambda:None,approved_source=scope,native_guard=lambda:None)
        assert result['state']=='SHARED_PAYLOAD_CAPTURED_INGRESS_HELD' and result['productionEvidence'] is False
        assert result['fullRunnerCompleted'] is False and result['claimReleased'] is False
        assert calls==['barrier']+['stop:'+role for role in split.STOP_ORDER[:-1]]+['sql','stop:mysql','raw']
    else:
        with pytest.raises(FormalSnapshotError,match='FORMAL_CAPTURE_SOURCE_LEDGER_CHANGED'):
            producer.run_shared_request(claim,request,lambda:None,approved_source=scope,native_guard=lambda:None)
