"""Health response boundaries, using actual local HTTP/TCP in separate tests."""
import importlib
import copy
import json
from pathlib import Path
import sys
from types import SimpleNamespace

import pytest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))


@pytest.mark.parametrize('role,status,body',[
    ('backend',200,b'{"status":"DOWN"}'),('backend',302,b'{"status":"UP"}'),
    ('backend',200,b'{"status":"DOWN","status":"UP"}'),('backend',200,b'[]'),
    ('onlyoffice',200,b'false'),('onlyoffice',503,b'true'),('minio',204,b''),
    ('frontend',200,b''),('showroom',301,b'<html>moved</html>')])
def test_http_health_rejects_nonhealthy_status_or_body(role,status,body):
    m=importlib.import_module('formal_service_health')
    with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_HEALTH_RESPONSE_INVALID'):
        m.validate_http(role,status,body)


@pytest.mark.parametrize('role,body',[
    ('backend',b'{"status":"UP"}'),('onlyoffice',b'true'),('minio',b''),
    ('frontend',b'<!DOCTYPE html><html><body>app</body></html>'),
    ('showroom',b'<!DOCTYPE html><html><body>showroom</body></html>')])
def test_http_health_retains_response_identity(role,body):
    m=importlib.import_module('formal_service_health')
    proof=m.validate_http(role,200,body)
    assert proof['bytes']==len(body) and len(proof['sha256'])==64
    assert proof['httpStatus']==200


@pytest.mark.parametrize('value',[0,-1,True,301,'60',float('nan'),float('inf')])
def test_invalid_total_deadline_is_rejected_before_probing(value):
    m=importlib.import_module('formal_service_health')
    with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_HEALTH_DEADLINE_INVALID'):
        m.health_deadline(value)


def test_namespace_access_failure_reports_stage_and_errno_without_path(monkeypatch):
    m=importlib.import_module('formal_service_health')
    probe=object.__new__(m.NativeNetworkProbe)
    record={'id':'a'*64,'state':{'Running':True}}
    monkeypatch.setattr(m.snapshot,'inspect_formal_container',lambda _:record)
    monkeypatch.setattr(probe,'_pid',lambda _:123)
    monkeypatch.setattr(m.os,'O_CLOEXEC',0,raising=False)
    def denied(*args):raise PermissionError(13,'private system detail')
    monkeypatch.setattr(m.os,'open',denied)
    with pytest.raises(m.snapshot.FormalSnapshotError,
        match='^FORMAL_HEALTH_NAMESPACE_OPEN_OS_ERROR_13$'):
        probe.observe('redis',record,3)


def test_inner_health_deadline_never_extends_outer_budget(monkeypatch):
    m=importlib.import_module('formal_service_health')
    monkeypatch.setattr(m.time,'monotonic',lambda:10.)
    token=m.snapshot.observation_deadline.set(12.)
    try:assert m.health_deadline(60)==12.
    finally:m.snapshot.observation_deadline.reset(token)


def test_native_probe_entire_observation_has_deadline_and_restores_caller(monkeypatch):
    m=importlib.import_module('formal_service_health')
    probe=object.__new__(m.NativeNetworkProbe)
    record={'id':'a'*64,'state':{'Running':True}}
    calls=[]
    def inspect(_):
        calls.append(m.snapshot.observation_deadline.get())
        raise m.snapshot.FormalSnapshotError('OWNED_STOP_AFTER_FIRST_INSPECT')
    monkeypatch.setattr(m.time,'monotonic',lambda:10.)
    monkeypatch.setattr(m.snapshot,'inspect_formal_container',inspect)
    token=m.snapshot.observation_deadline.set(12.)
    try:
        with pytest.raises(m.snapshot.FormalSnapshotError,match='OWNED_STOP_AFTER_FIRST_INSPECT'):
            probe.observe('redis',record,1)
        assert calls==[11.]
        assert m.snapshot.observation_deadline.get()==12.
    finally:m.snapshot.observation_deadline.reset(token)


@pytest.mark.parametrize('case',['pass','original-stopped','page-changed','scope-changed',
    'runtime-changed','guard-lost','mysql-flags-changed'])
def test_fresh_recovery_health_binds_originals_and_refuses_drift(tmp_path,monkeypatch,case):
    # Explicit Docker/SQL doubles test the observer boundary, not live recovery.
    from test_formal_split_capture import fixture,ROLES
    split,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    m=importlib.import_module('formal_service_health')
    flags={'serverUuid':json.loads(claim._plan)['databaseUuid'],'readOnly':0,'superReadOnly':0}
    monkeypatch.setattr(m.snapshot,'_database_flags',lambda *args:copy.deepcopy(flags))
    class Probe:
        changed=False
        def observe(self,role,record,seconds):
            if role=='redis':return {'response':'PONG'}
            body=b'{"status":"UP"}' if role=='backend' else b'true' if role=='onlyoffice' else b''
            if role in ('frontend','showroom'):
                body=b'<html>changed</html>' if self.changed else b'<html>original</html>'
            return m.validate_http(role,200,body)
    probe=Probe()
    try:
        if case=='original-stopped':rows['4'*64]['state']['Running']=False
        split.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
        original=m.bind_shared_health_scope(claim,mysql,guard=guard,redis_authentication='none',probe=probe)
        restored={'sharedScopeSha256':original['sharedScopeSha256'],'originalFlags':copy.deepcopy(flags),
                  'services':{row['name'][1:]:copy.deepcopy(row) for row in rows.values()}}
        calls=[]
        def verify(*args,**kwargs):
            kwargs['guard']();kwargs['thaw_guard']();calls.append('verify')
            value=copy.deepcopy(restored)
            if case=='runtime-changed' and len(calls)>1:value['services']['backend']['state']['StartedAt']='drift'
            return value
        monkeypatch.setattr(split,'verify_shared_restoration',verify)
        if case=='page-changed':probe.changed=True
        if case=='scope-changed':(claim.root/'shared-health-original.json').write_bytes(b'{}')
        if case=='mysql-flags-changed':flags['readOnly']=1
        if case=='guard-lost':
            def guard():raise m.snapshot.FormalSnapshotError('OWNED_GUARD_LOST')
        if case not in ('pass','original-stopped'):
            with pytest.raises(m.snapshot.FormalSnapshotError):
                m.observe_shared_recovery_health(claim,mysql,guard=guard,thaw_guard=guard,probe=probe)
            return
        result=m.observe_shared_recovery_health(claim,mysql,guard=guard,thaw_guard=guard,probe=probe)
        assert result['state']=='ORIGINAL_SERVICES_HEALTHY' and result['healthOnlyNotReleaseAcceptance']
        assert result['productionEvidence'] is False and calls==['verify','verify']
        if case=='original-stopped':assert result['health']['showroom']=={'state':'ORIGINALLY_STOPPED','healthObserved':False}
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('stdout,stderr,code,expected',[
    (b'1\n',b'',0,True),
    (b'',b"ERROR 2002 (HY000): Can't connect to local MySQL server through socket '/var/run/mysqld/mysqld.sock' (2)\n",1,False),
    (b'',b"ERROR 2002 (HY000): Can't connect to local MySQL server through socket '/var/run/mysqld/mysqld.sock' (111)\n",1,False),
    (b'',b'ERROR 1045 (28000): Access denied\n',1,'FAIL'),
    (b'',b'unknown docker error',1,'FAIL'),(b'0\n',b'',0,'FAIL'),
    (b'1\n',b'warning',0,'FAIL')])
def test_mysql_readiness_only_retries_explicit_socket_startup(stdout,stderr,code,expected,monkeypatch):
    m=importlib.import_module('formal_service_health')
    mysql=m.snapshot.PrivateDockerMySql('a'*64,'owned-test-password',timeout=3)
    seen=[]
    def run(command,**kwargs):
        seen.append(command)
        assert 'owned-test-password' not in ' '.join(command)
        assert kwargs['input']==b'owned-test-password\nSELECT 1;'
        assert kwargs['timeout']<=3
        return SimpleNamespace(returncode=code,stdout=stdout,stderr=stderr)
    monkeypatch.setattr(m.snapshot.subprocess,'run',run)
    if expected=='FAIL':
        with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_MYSQL_READINESS_FAILED'):
            mysql.ready()
    else:assert mysql.ready() is expected
    assert len(seen)==1


@pytest.mark.parametrize('failure',[None,'mysql','minio','missing-original'])
def test_health_checked_recovery_never_starts_next_dependency_after_failure(tmp_path,monkeypatch,failure):
    from test_formal_split_capture import fixture,ROLES
    split,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    m=importlib.import_module('formal_service_health')
    flags={'serverUuid':json.loads(claim._plan)['databaseUuid'],'readOnly':0,'superReadOnly':0}
    monkeypatch.setattr(m.snapshot,'_database_flags',lambda *args:copy.deepcopy(flags))
    calls=[]
    class Probe:
        recovery=False
        def observe(self,role,record,seconds):
            if self.recovery:
                calls.append('health-'+role)
                if failure==role:raise m.snapshot.FormalSnapshotError('OWNED_UNHEALTHY')
            return {'response':'PONG'} if role=='redis' else {'sha256':'a'*64}
    probe=Probe()
    try:
        split.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
        if failure!='missing-original':
            m.bind_shared_health_scope(claim,mysql,guard=guard,redis_authentication='none',probe=probe)
        probe.recovery=True
        monkeypatch.setattr(split,'begin_shared_recovery',lambda *a,**k:calls.append('begin'))
        monkeypatch.setattr(split,'restore_shared_container',lambda c,r,**k:calls.append('start-'+r))
        monkeypatch.setattr(split,'restore_shared_database_flags',lambda *a,**k:calls.append('flags'))
        monkeypatch.setattr(m,'observe_shared_recovery_health',lambda *a,**k:{'state':'OWNED_FINAL_CHECK'})
        def ready():
            calls.append('health-mysql')
            if failure=='mysql':raise m.snapshot.FormalSnapshotError('OWNED_UNHEALTHY')
            return True
        mysql.ready=ready
        if failure:
            with pytest.raises(m.snapshot.FormalSnapshotError):
                m.restore_shared_services_with_health(claim,mysql,guard=guard,thaw_guard=guard,probe=probe)
            if failure=='missing-original':assert calls==[]
            elif failure=='mysql':assert calls==['begin','start-mysql','health-mysql']
            else:assert calls==['begin','start-mysql','health-mysql','flags','start-redis','health-redis','start-minio','health-minio']
        else:
            result=m.restore_shared_services_with_health(claim,mysql,guard=guard,thaw_guard=guard,probe=probe)
            assert result=={'state':'OWNED_FINAL_CHECK'}
            assert calls==['begin','start-mysql','health-mysql','flags']+[
                action+'-'+role for role in split.RESTORE_ORDER[1:] for action in ('start','health')]
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('entry',['restore_shared_services_with_health','observe_shared_recovery_health'])
def test_recovery_checks_positive_thaw_before_any_docker_observation(tmp_path,monkeypatch,entry):
    from test_formal_split_capture import fixture,ROLES
    split,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    m=importlib.import_module('formal_service_health')
    try:
        claim.events.append({'step':'shared-health-scope','details':{'sha256':'a'*64}})
        # Make the original health helper expose the very first Docker read.
        def original(_):
            m.snapshot.inspect_formal_container('1'*64)
        monkeypatch.setattr(m,'_original_health',original)
        def forbidden(_):raise AssertionError('Docker observed before positive thaw')
        def guard():raise AssertionError('Exclusion guard ran before positive thaw')
        def thaw():raise m.snapshot.FormalSnapshotError('OWNED_THAW_UNPROVEN')
        # End the inner patch before the fixture restores its outer patch.
        # pytest teardown would otherwise resurrect the fixture's old mock.
        with patch.object(m.snapshot,'inspect_formal_container',side_effect=forbidden):
            with pytest.raises(m.snapshot.FormalSnapshotError,match='OWNED_THAW_UNPROVEN'):
                getattr(m,entry)(claim,mysql,guard=guard,thaw_guard=thaw,probe=object())
    finally:
        for patcher in reversed(patchers):patcher.stop()
