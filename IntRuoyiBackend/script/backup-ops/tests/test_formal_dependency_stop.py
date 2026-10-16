"""Finite stop-point tests; explicit Docker/SQL doubles, never live recovery proof."""
import copy
import hashlib
import importlib
import json
from pathlib import Path
from unittest.mock import patch

import pytest

from test_formal_split_capture import fixture, ROLES
from test_formal_capture_producer_entry import shared_request, load


@pytest.mark.parametrize('case',['pass','original-stopped','original-backend-stopped','original-frontend-stopped','mysql-failure','minio-failure',
    'missing-original','app-running','app-drift','flags-drift','early-flags-drift','mysql-baseline-drift','health-drift','receipt-drift','replay'])
def test_dependency_recovery_stops_before_applications(tmp_path,monkeypatch,case):
    split,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    health=importlib.import_module('formal_service_health')
    identity=json.loads(claim._plan)['databaseUuid']
    original_flags=dict(serverUuid=identity,readOnly=0,superReadOnly=0)
    flags=copy.deepcopy(original_flags);calls=[]
    monkeypatch.setattr(split.snapshot,'_claimed_database_original',lambda _:(identity,copy.deepcopy(original_flags),'a'*64))
    monkeypatch.setattr(split.snapshot,'_database_flags',lambda *a:copy.deepcopy(flags))
    class Probe:
        recovery=False
        def observe(self,role,record,seconds):
            if self.recovery:
                calls.append('health:'+role)
                if case=='minio-failure' and role=='minio':raise split.snapshot.FormalSnapshotError('OWNED_UNHEALTHY')
                if case=='app-drift' and role=='showroom':rows['3'*64]['state']['FinishedAt']='changed'
                if case=='flags-drift' and role=='showroom':flags['readOnly']=1
                if case=='early-flags-drift' and role=='redis':flags['readOnly']=1
            if role=='redis':return dict(protocol='redis',authentication='none',response='PONG',networkNamespaceInode=1,pid=1)
            body=b'{"status":"UP"}' if role=='backend' else b'true' if role=='onlyoffice' else b''
            if role in ('frontend','showroom'):body=b'<html>changed</html>' if self.recovery and case=='health-drift' else b'<html>original</html>'
            return dict(health.validate_http(role,200,body),networkNamespaceInode=1,pid=1)
    probe=Probe()
    try:
        if case=='original-stopped':rows['4'*64]['state']['Running']=False
        if case=='original-backend-stopped':rows['3'*64]['state']['Running']=False
        if case=='original-frontend-stopped':rows['2'*64]['state']['Running']=False
        split.bind_shared_container_scope(claim,{r:str(i)*64 for i,r in enumerate(ROLES,1)})
        if case!='missing-original':health.bind_shared_health_scope(claim,mysql,guard=guard,redis_authentication='none',probe=probe)
        claim.record('db-fenced',{'originalSha256':'a'*64})
        for role in split.STOP_ORDER[:-1]:split.stop_shared_container(claim,role,guard=guard)
        claim.record('split-sql-captured',{})
        split.stop_shared_container(claim,'mysql',guard=guard)
        flags.update(readOnly=1,superReadOnly=1)
        held={role:copy.deepcopy(rows['3'*64 if role=='backend' else '2'*64]) for role in ('backend','frontend')}
        def action(kind,container):
            assert kind=='start';role=rows[container]['name'][1:];calls.append('start:'+role)
            assert role not in ('backend','frontend')
            rows[container]['state'].update(Running=True,StartedAt='restored:'+role)
        def query(sql):
            calls.append('flags');assert sql=='SET GLOBAL super_read_only=OFF; SET GLOBAL read_only=OFF;'
            flags.update(readOnly=0,superReadOnly=0)
        def ready():
            calls.append('health:mysql')
            if case=='mysql-failure':raise split.snapshot.FormalSnapshotError('OWNED_MYSQL_UNHEALTHY')
            return True
        mysql.query=query;mysql.ready=ready;probe.recovery=True
        if case=='mysql-baseline-drift':
            original_mysql=split._restored_mysql
            def drift_after_verified_mysql(*args,**kwargs):
                observed=original_mysql(*args,**kwargs)
                if any(e['step']=='shared-db-restored' for e in claim.events):
                    rows['1'*64]['state']['StartedAt']='unexpected-mysql-restart'
                return observed
            monkeypatch.setattr(split,'_restored_mysql',drift_after_verified_mysql)
        if case=='app-running':rows['3'*64]['state']['Running']=True
        if case=='receipt-drift':
            original_record=claim.record
            def record(step,details):
                original_record(step,details)
                if step=='shared-dependencies-ready':(claim.root/'shared-dependencies-health.json').write_bytes(b'{}')
            claim.record=record
        # Replace the fixture's stop-only Docker boundary only during this stage.
        with patch.object(split.snapshot,'run_formal_container_action',side_effect=action):
            if case not in ('pass','original-stopped','original-backend-stopped','original-frontend-stopped','replay'):
                with pytest.raises(split.snapshot.FormalSnapshotError):
                    health.restore_shared_dependencies_with_health(claim,mysql,guard=guard,thaw_guard=guard,probe=probe)
                assert any(e['step']=='shared-dependencies-ready' for e in claim.events)==(case=='receipt-drift')
                if case in ('missing-original','app-running'):assert not calls
                if case=='mysql-failure':assert calls==['start:mysql','health:mysql']
                if case=='minio-failure':assert 'start:onlyoffice' not in calls
                if case=='early-flags-drift':assert 'start:minio' not in calls
                if case=='mysql-baseline-drift':assert 'start:redis' not in calls
                return
            result=health.restore_shared_dependencies_with_health(claim,mysql,guard=guard,thaw_guard=guard,probe=probe)
            assert result['state']=='DEPENDENCIES_RESTORED_PENDING_OPEN_APPROVAL'
            assert result['productionEvidence'] is False and result['releaseReady'] is False
            assert set(result['health'])==set(split.RESTORE_ORDER[:5])
            assert result['applicationsHeld']==held and result['claimSha256']==hashlib.sha256(claim._plan).hexdigest()
            retained=claim._read(claim.root/'shared-dependencies-health.json')
            assert json.loads(retained)==result
            assert [e['details'] for e in claim.events if e['step']=='shared-dependencies-ready']==[{'sha256':hashlib.sha256(retained).hexdigest()}]
            assert not any(e['step'] in ('shared-backend-restore-intent','shared-frontend-restore-intent','released','ingress-released') for e in claim.events)
            assert [x for x in calls if x.startswith('start:')]==['start:'+r for r in split.RESTORE_ORDER[:5] if r!='showroom' or case!='original-stopped']
            if case=='replay':
                before=list(calls)
                with pytest.raises(split.snapshot.FormalSnapshotError,match='REPLAY_REFUSED'):
                    health.restore_shared_dependencies_with_health(claim,mysql,guard=guard,thaw_guard=guard,probe=probe)
                assert calls==before
    finally:
        for p in reversed(patchers):p.stop()


@pytest.mark.parametrize('case',['accepted','extra','wrong-action','wrong-version'])
def test_explicit_dependency_request_contract(tmp_path,case):
    module=load();request=shared_request(tmp_path,'restore-shared-dependencies')
    request['schemaVersion']='release-formal-capture-request/v7'
    if case=='extra':request['openApproved']=True
    if case=='wrong-action':request['action']='restore-shared-services'
    if case=='wrong-version':request['schemaVersion']='release-formal-capture-request/v3'
    if case=='accepted':module.validate_dependency_request(request)
    else:
        with pytest.raises(module.FormalSnapshotError,match='REQUEST_INVALID'):module.validate_dependency_request(request)


def test_old_whole_recovery_refused_before_claim_or_services(tmp_path,capsys):
    module=load();request=shared_request(tmp_path,'restore-shared-services')
    path=tmp_path/'request.json';path.write_bytes(json.dumps(request).encode());path.chmod(0o600)
    with patch.object(module,'require_native_runtime'),patch.object(module,'FormalMaintenanceClaim') as claimed,\
            patch.object(module,'run_shared_request') as dispatched:
        assert module.main(['--request',str(path),'--expected-request-sha256',hashlib.sha256(path.read_bytes()).hexdigest()])==1
        assert json.loads(capsys.readouterr().out)['code']=='FORMAL_CAPTURE_EXPLICIT_OPEN_STOP_REQUIRED'
        claimed.assert_not_called();dispatched.assert_not_called()


def test_direct_old_whole_recovery_cannot_bypass_stop_gate():
    module=load()
    with pytest.raises(module.FormalSnapshotError,match='FORMAL_CAPTURE_EXPLICIT_OPEN_STOP_REQUIRED'):
        module.run_shared_request(None,{'schemaVersion':'release-formal-capture-request/v3','action':'restore-shared-services'},None)


@pytest.mark.parametrize('invalid',['unknown-action','unknown-schema'])
def test_direct_unknown_recovery_rejected_before_guard(invalid):
    module=load()
    request={'schemaVersion':'release-formal-capture-request/v7','action':'restore-shared-dependencies'}
    request['action' if invalid=='unknown-action' else 'schemaVersion']='unknown'
    with pytest.raises(module.FormalSnapshotError,match='REQUEST_INVALID'):
        module.run_shared_request(None,request,None)


@pytest.mark.parametrize('size',[16385,32768,32769])
def test_fixed_dependency_receipt_has_explicit_bounded_reader(tmp_path,size):
    health=importlib.import_module('formal_service_health')
    claim=type('Claim',(),{'root':tmp_path,'assert_owned':lambda self:None})()
    path=tmp_path/'shared-dependencies-health.json';raw=b'x'*size
    path.write_bytes(raw);path.chmod(0o600)
    if size<=32768:assert health.read_shared_dependency_receipt(claim)==raw
    else:
        with pytest.raises(health.snapshot.FormalSnapshotError,match='RECEIPT_INVALID'):
            health.read_shared_dependency_receipt(claim)
