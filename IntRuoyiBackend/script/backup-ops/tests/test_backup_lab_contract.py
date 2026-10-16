"""Private contract rejection tests; these do not assert native rehearsal success."""
import copy
import hashlib
from pathlib import Path
import sys

import pytest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_backup_lab as lab
import formal_capture_producer as producer
import formal_source_migration_ledger as ledger
from formal_snapshot_linux import FormalSnapshotError
from formal_normal_owner import canonical

PIN=dict(path='/private/original.json',bytes=1,sha256='a'*64)


def scope():
    return dict(schemaVersion='backup-lab-source-scope/v1',environment='backup-lab',
        targetRawSha256='a'*64,targetFingerprint='b'*64,database='owned_probe',
        databaseUuid='11111111-1111-1111-1111-111111111111',labContextPin=PIN)


def test_lab_scope_requires_exact_known_schema_and_original_context():
    lab.validate_source_scope(scope())
    for key,value in [('environment','prod'),('schemaVersion','backup-lab-source-scope/v2'),
                      ('databaseUuid','unknown'),('labContextPin',{'sha256':'a'*64})]:
        changed=scope();changed[key]=value
        with pytest.raises(FormalSnapshotError):lab.validate_source_scope(changed)
    changed=scope();changed['allowFallback']=True
    with pytest.raises(FormalSnapshotError):lab.validate_source_scope(changed)


def test_lab_scope_alone_never_grants_execution_or_ledger_observation():
    with pytest.raises(FormalSnapshotError,match='CURRENT_AUTHORITY_REQUIRED'):
        lab.require_authority(scope())
    with pytest.raises(FormalSnapshotError,match='INPUT_INVALID'):
        ledger.observe_migration_ledger(None,'owned_probe',scope()['databaseUuid'],
            source_environment='backup-lab',full_data=None,guard=lambda:None,max_rows=1)
    with pytest.raises(FormalSnapshotError,match='CURRENT_AUTHORITY_REQUIRED'):
        ledger.observe_lab_migration_ledger(None,'owned_probe',scope()['databaseUuid'],
            source_scope=scope(),full_data=None,guard=lambda:None,max_rows=1)


def test_production_entry_never_accepts_lab_or_converts_failure():
    request=dict(schemaVersion='backup-lab-capture-request/v1',action='capture-shared-payload',
        claimBinding=dict(databaseUuid=scope()['databaseUuid']),sourceScope=scope())
    with pytest.raises(FormalSnapshotError,match='SOURCE_APPROVAL_REQUIRED'):
        producer.validate_source_request(request,scope())


def test_context_exact_fields_key_and_time_window():
    value={k:copy.deepcopy(PIN) for k in lab.CONTEXT_FIELDS}
    value.update(schemaVersion='BackupRehearsalAuthorityContext/v1',rehearsalId=lab.LAB_ID,
        environment='backup-lab',issuedAt='2026-10-12T00:00:00Z',expiresAt='2026-10-12T01:00:00Z')
    lab.validate_context(value,PIN)
    wrong=dict(PIN,sha256='c'*64)
    with pytest.raises(FormalSnapshotError):lab.validate_context(value,wrong)
    changed=copy.deepcopy(value);changed['expiresAt']=changed['issuedAt']
    with pytest.raises(FormalSnapshotError):lab.validate_context(changed,PIN)
    changed=copy.deepcopy(value);changed['autoProd']=True
    with pytest.raises(FormalSnapshotError):lab.validate_context(changed,PIN)


def test_explicit_target_profile_preserves_prod_rejection():
    target=dict(schemaVersion='release-formal-target/v1',environment='backup-lab',host='172.30.30.59',
        machineId='a'*32,project='brlab-20261012',root=lab.LAB_ROOT+'/source',dataMount=lab.LAB_ROOT+'/mount',
        dataDevice='/dev/loop99',dataRoot=lab.LAB_ROOT+'/mount/data',releaseRoot=lab.LAB_ROOT+'/source/releases',
        frontendPort=18881,backendPort=18880,database='owned_probe',databaseServerUuid=scope()['databaseUuid'],
        containers={k:'brlab-20261012-'+k for k in ('mysql','redis','minio','backend','frontend')})
    raw=canonical(target); digest=hashlib.sha256(raw).hexdigest()
    claim=canonical(dict(operationId='lab-full-1',sourceFingerprint=digest,
        databaseUuid=target['databaseServerUuid'],minioContainerId='b'*64))
    with pytest.raises(FormalSnapshotError):ledger.approved_source_scope(raw,digest,claim,'owned_probe')
    approved=ledger.approved_lab_source_scope(raw,digest,claim,'owned_probe',PIN)
    assert approved['labContextPin']==PIN and approved['targetFingerprint']==digest
    target['environment']='prod';raw=canonical(target);digest=hashlib.sha256(raw).hexdigest()
    with pytest.raises(FormalSnapshotError):ledger.approved_lab_source_scope(raw,digest,claim,'owned_probe',PIN)


def test_repository_identity_uses_private_records_without_public_request_field():
    context=dict(sourceIdentityPin=PIN,executionHostPin=PIN,labScopePin=PIN)
    source=dict(schemaVersion='RepositoryNativeBackupSource/v1',repositoryId='lab-repo',sourceIdentityPin=PIN,executionHostPin=PIN)
    policy=dict(schemaVersion='RepositoryPolicy/v1',repositoryId='lab-repo',sourceIdentityPin=PIN)
    peer=dict(schemaVersion='RepositoryNativePeerConfig/v2',repositoryId='lab-repo',sourceIdentityPin=PIN,executionHostPin=PIN,labScopePin=PIN)
    scope=dict(repositoryId='lab-repo',sourceIdentityPin=PIN)
    request=dict(sourceOrTargetRef=PIN,policyPin=PIN)
    grant=dict(repositoryId='lab-repo',sourceOrTargetRef=PIN,policyPin=PIN);lease=dict(repositoryId='lab-repo')
    assert 'repositoryId' not in request
    assert lab.repository_binding(source,policy,peer,scope,grant,lease,context,request)=='lab-repo'
    for original in (source,policy,peer,grant,lease):
        before=original['repositoryId'];original['repositoryId']='wrong'
        with pytest.raises(FormalSnapshotError,match='REPOSITORY_BINDING_CHANGED'):
            lab.repository_binding(source,policy,peer,scope,grant,lease,context,request)
        original['repositoryId']=before


def test_lab_capacity_checks_allocated_peak_not_only_logical_size():
    workspace=dict(budgetBytes=80*1024**3,reserveBytes=1024**3,maxAttemptBytes=10*1024**3)
    intent=dict(logicalPayloadUpperBoundBytes=1024**3,nasAllocatedUpperBoundBytes=2*1024**3)
    doc=dict(workspaceBudgetBytes=workspace['budgetBytes'],maxAttemptBytes=workspace['maxAttemptBytes'],
        reserveBytes=workspace['reserveBytes'],workspaceUsageBytes=0,availableBytes=3*1024**3,**intent)
    lab.nas_capacity(doc,workspace,intent)
    for mutate in (lambda d:d.update(availableBytes=3*1024**3-1),
                   lambda d:d.update(workspaceUsageBytes=79*1024**3),
                   lambda d:d.update(maxAttemptBytes=True)):
        invalid=dict(doc);mutate(invalid)
        with pytest.raises(FormalSnapshotError):lab.nas_capacity(invalid,workspace,intent)
    over=dict(workspace,budgetBytes=80*1024**3+1)
    with pytest.raises(FormalSnapshotError):lab.nas_capacity(doc,over,intent)


def test_scope_action_vocabulary_matches_frozen_twelve_tokens():
    assert lab.LAB_ACTIONS=={'BACKUP_FULL','BACKUP_INCREMENTAL','LIST_POINTS','POINT_DETAIL','READBACK_VERIFY',
        'RESTORE_PREFLIGHT','PREPARE_TARGET','RESTORE_EXECUTE','START_APPLICATION','VERIFY_RECOVERY','REHEARSAL','PLAN_REHEARSAL'}


def test_selected_scope_and_specific_grant_must_both_authorize_action():
    assert lab.valid_action_scope(['BACKUP_FULL','PREPARE_TARGET'],'BACKUP_FULL',['BACKUP_FULL'])
    for scope,action,grant in [(['BACKUP_FULL'],'BACKUP_INCREMENTAL',['BACKUP_INCREMENTAL']),
        (['BACKUP_FULL'],'BACKUP_FULL',['BACKUP_INCREMENTAL']),
        (['BACKUP_FULL','CUTOVER'],'BACKUP_FULL',['BACKUP_FULL']),
        (['BACKUP_FULL','BACKUP_FULL'],'BACKUP_FULL',['BACKUP_FULL']),
        (['BACKUP_FULL'],'BACKUP_FULL',['FULL']), (['BACKUP_FULL',{}],'BACKUP_FULL',['BACKUP_FULL'])]:
        with pytest.raises(FormalSnapshotError):lab.valid_action_scope(scope,action,grant)
