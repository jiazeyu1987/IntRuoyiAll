"""Real retained files; explicit claim, SQL, Docker and native-fence doubles."""
import copy
from datetime import datetime, timezone
import gzip
import hashlib
import importlib
import json
from pathlib import Path
import sys
from unittest.mock import patch

import pytest
from test_formal_data_evidence import contract_observation

LINUX = Path(__file__).parents[1]/'linux'
sys.path.insert(0,str(LINUX))
ROLES = ('mysql','frontend','backend','showroom','onlyoffice','redis','minio')
DATA_LIMITS={'max_rows':1000,'max_nodes':10000,'max_bytes':1024*1024,'seconds':30}
REJECTION_PROBE={'table':'probe','column':'id'}


@pytest.mark.parametrize('fault', ['none', 'original-stopped', 'missing-authority',
    'authority-before', 'authority-after-intent', 'thaw', 'unknown-start', 'repeat'])
def test_controlled_original_restore_keeps_authority_and_full_sources(tmp_path, fault):
    """Actual shared journal logic and files, explicit native/authority protocol seams."""
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    calls=[]; original={'serverUuid':json.loads(claim._plan)['databaseUuid'],
        'readOnly':1,'superReadOnly':0}; flags=dict(original,superReadOnly=1)
    if fault=='original-stopped':rows['4'*64]['state']['Running']=False
    def query(sql):
        assert sql.startswith('SET GLOBAL');calls.append('flags');flags.update(original)
    mysql.query=query
    patchers.extend([patch.object(m.snapshot,'_claimed_database_original',
        return_value=(original['serverUuid'],original,'a'*64)),
        patch.object(m.snapshot,'_database_flags',side_effect=lambda *a:copy.deepcopy(flags))])
    for p in patchers[-2:]:p.start()
    try:
        m.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
        claim.record('db-fenced',{'originalSha256':'a'*64})
        for role in m.STOP_ORDER[:-1]:m.stop_shared_container(claim,role,guard=guard)
        claim.record('split-sql-captured',{})
        m.stop_shared_container(claim,'mysql',guard=guard)
        def resume():
            calls.append('authority');guard()
            if fault=='authority-before' or fault=='authority-after-intent' and any(
                e['step']=='shared-mysql-restore-intent' for e in claim.events):
                raise m.snapshot.FormalSnapshotError('FORMAL_PRODUCTION_A_RESUME_APPROVAL_INVALID')
        def thaw():
            calls.append('thaw');guard()
            if fault=='thaw':raise m.snapshot.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED')
        def action(kind,identity):
            role=next(r for r in ROLES if rows[identity]['name']=='/'+r)
            assert kind=='start' and calls[-1] in ('thaw','authority')
            assert any(e['step']=='shared-'+role+'-restore-intent' for e in claim.events)
            calls.append('start:'+role)
            if fault=='unknown-start':raise m.snapshot.FormalSnapshotError('CONTROLLED_CONTAINER_ACTION_UNCERTAIN')
            rows[identity]['state'].update(Running=True,StartedAt='owned-restart',FinishedAt='zero')
        adapter=ControlledActionProtocol(rows,action)
        before=len(guards)
        with patch.object(m.snapshot,'run_formal_container_action',
                side_effect=AssertionError('unprotected legacy restore')):
            invoke=lambda:m.restore_controlled_shared_originals(claim,mysql,guard=guard,
                thaw_guard=thaw,resume_guard=None if fault=='missing-authority' else resume,
                container_actions=adapter)
            if fault in ('missing-authority','authority-before','authority-after-intent','thaw','unknown-start'):
                with pytest.raises(m.snapshot.FormalSnapshotError):invoke()
                assert not any(c=='start:redis' or c=='flags' for c in calls)
                if fault!='unknown-start':assert not any(c.startswith('start:') for c in calls)
                if fault in ('authority-after-intent','unknown-start'):
                    assert any(e['step']=='shared-mysql-restore-intent' for e in claim.events)
                    assert not any(e['step']=='shared-mysql-restored' for e in claim.events)
            else:
                result=invoke()
                assert result['state']=='ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF'
                assert result['productionEvidence'] is False and not claim.closed
                assert flags==original and len(guards)>before
                starts=[c for c in calls if c.startswith('start:')]
                assert starts==['start:'+r for r in m.RESTORE_ORDER if fault!='original-stopped' or r!='showroom']
                assert rows['4'*64]['state']['Running']==(fault!='original-stopped')
                assert calls.index('start:mysql')<calls.index('flags')<calls.index('start:redis')
                if fault=='repeat':
                    effects=list(starts)
                    with pytest.raises(m.snapshot.FormalSnapshotError):invoke()
                    assert [c for c in calls if c.startswith('start:')]==effects
    finally:
        for p in reversed(patchers):p.stop()


class ControlledActionProtocol:
    """Explicit complete-source action seam; dedicated sources tests use real files."""
    def __init__(self,rows,perform):self.rows=rows;self.perform=perform;self.checks=[]
    def assert_role(self,role,original,current,*,guard,deadline):
        guard();self.checks.append(role);assert self.rows[current['id']]==current
    def action(self,role,kind,original,current,*,guard,deadline):
        self.assert_role(role,original,current,guard=guard,deadline=deadline)
        self.perform(kind,original['id'])


@pytest.mark.parametrize('stage',[1,2,3,4,5])
@pytest.mark.parametrize('drift',['fence','intent'])
def test_controlled_stop_binds_stage_after_every_adapter_guard(tmp_path,stage,drift):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        originals=m.bind_shared_container_scope(claim,{r:str(i)*64 for i,r in enumerate(ROLES,1)})
        scope=hashlib.sha256(m.snapshot._canonical_bytes(originals)).hexdigest();actions=[];checks=[]
        def perform(kind,identity):
            actions.append((kind,identity));rows[identity]['state'].update(Running=False,FinishedAt='owned-stop')
        protected=ControlledActionProtocol(rows,perform)
        original_check=protected.assert_role
        # Mutate inside the caller's real guard, not after the phase-bound guard
        # returns: this models a source/lease callback reaching a new DB stage.
        def selected_guard():
            guard()
            if len(checks)==stage:
                if drift=='fence' and not m._has(claim,'db-fence-intent'):claim.record('db-fence-intent',{})
                elif drift=='intent':
                    if m._has(claim,'shared-frontend-stop-intent'):
                        m._event(claim,'shared-frontend-stop-intent')['stopStrategy']='ALTERED'
                    else:claim.record('shared-frontend-stop-intent',{})
        def check_bound(role,original,current,*,guard,deadline):
            checks.append(1);original_check(role,original,current,guard=guard,deadline=deadline)
        protected.assert_role=check_bound
        with patch.object(m.snapshot,'run_formal_container_action') as legacy:
            with pytest.raises(m.snapshot.FormalSnapshotError):
                m.stop_controlled_shared_writer(claim,'frontend',expected_shared_scope_sha256=scope,
                    guard=selected_guard,container_actions=protected)
            legacy.assert_not_called()
        assert actions==([] if stage<=3 else [('stop','2'*64)])
        assert not m._has(claim,'shared-frontend-stopped') and not mysql.calls
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('case',['missing','changed-before-intent','changed-after-intent'])
def test_controlled_stop_requires_actual_complete_source_interface(tmp_path,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        originals=m.bind_shared_container_scope(claim,{r:str(i)*64 for i,r in enumerate(ROLES,1)})
        scope=hashlib.sha256(m.snapshot._canonical_bytes(originals)).hexdigest();checks=[]
        protected=ControlledActionProtocol(rows,lambda *args:pytest.fail('unprotected action'))
        def check(*args,**kwargs):
            checks.append(1);kwargs['guard']()
            if case=='changed-before-intent' or len(checks)>1:raise m.snapshot.FormalSnapshotError('OWNED_FULL_SOURCE_CHANGED')
        protected.assert_role=check
        with patch.object(m.snapshot,'run_formal_container_action') as legacy:
            with pytest.raises(m.snapshot.FormalSnapshotError):
                m.stop_controlled_shared_writer(claim,'frontend',expected_shared_scope_sha256=scope,
                    guard=guard,container_actions=None if case=='missing' else protected)
            legacy.assert_not_called()
        assert any(e['step']=='shared-frontend-stop-intent' for e in claim.events)==(case=='changed-after-intent')
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('case',['normal','original-stopped','wrong-role','wrong-order','wrong-scope',
    'fence-already-set','sql-already-started','replay','runtime-drift','lost-guard','unknown-stop'])
def test_controlled_writers_stop_before_database_fence(tmp_path,case):
    """Explicit A phase; actual retained files, named Docker/claim unit seams."""
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        if case=='original-stopped':rows['2'*64]['state']['Running']=False
        originals=m.bind_shared_container_scope(claim,{r:str(i)*64 for i,r in enumerate(ROLES,1)})
        scope=hashlib.sha256(m.snapshot._canonical_bytes(originals)).hexdigest()
        run=m.stop_controlled_shared_writer
        if case=='wrong-scope':scope='0'*64
        if case=='fence-already-set':claim.record('db-fenced',{})
        if case=='sql-already-started':claim.record('split-sql-intent',{})
        if case=='runtime-drift':rows['2'*64]['state']['StartedAt']='unapproved-restart'
        role='mysql' if case=='wrong-role' else 'backend' if case=='wrong-order' else 'frontend'
        calls=[]
        def action(kind,identity):
            calls.append((kind,identity));assert kind=='stop'
            if case=='unknown-stop':raise m.snapshot.FormalSnapshotError('OWNED_ACTION_UNCERTAIN')
            rows[identity]['state'].update(Running=False,FinishedAt='owned-controlled-stop')
        def selected_guard():
            guard()
            if case=='lost-guard':raise m.snapshot.FormalSnapshotError('OWNED_GUARD_LOST')
        protected=ControlledActionProtocol(rows,action)
        with patch.object(m.snapshot,'run_formal_container_action',side_effect=action):
            if case not in ('normal','original-stopped','replay','unknown-stop'):
                with pytest.raises(m.snapshot.FormalSnapshotError):
                    run(claim,role,expected_shared_scope_sha256=scope,guard=selected_guard,container_actions=protected)
                assert not calls and not any(e['step'].startswith('shared-frontend-stop') for e in claim.events)
                return
            if case=='unknown-stop':
                with pytest.raises(m.snapshot.FormalSnapshotError,match='OWNED_ACTION_UNCERTAIN'):
                    run(claim,role,expected_shared_scope_sha256=scope,guard=selected_guard,container_actions=protected)
                assert [e['step'] for e in claim.events]==['shared-container-scope','shared-frontend-stop-intent']
                with pytest.raises(m.snapshot.FormalSnapshotError):
                    run(claim,role,expected_shared_scope_sha256=scope,guard=selected_guard,container_actions=protected)
                with pytest.raises(m.snapshot.FormalSnapshotError):
                    m.reconcile_controlled_shared_writer(claim,role,expected_shared_scope_sha256=scope,guard=selected_guard,container_actions=protected)
                assert len(calls)==1
                # Actual later stop can be reconciled without another action.
                rows['2'*64]['state'].update(Running=False,FinishedAt='owned-late-stop')
                m.reconcile_controlled_shared_writer(claim,role,expected_shared_scope_sha256=scope,guard=selected_guard,container_actions=protected)
                assert len(calls)==1
                return
            for role in m.STOP_ORDER[:-1]:
                run(claim,role,expected_shared_scope_sha256=scope,guard=selected_guard,container_actions=protected)
            assert rows['1'*64]['state']['Running'] is True and not mysql.calls
            assert not any(e['step']=='db-fenced' for e in claim.events)
            for role in m.STOP_ORDER[:-1]:
                assert m._event(claim,'shared-'+role+'-stop-intent')['stopStrategy']=='CONTROLLED_NORMAL_STOP'
            assert len(calls)==(5 if case=='original-stopped' else 6)
            if case=='replay':
                with pytest.raises(m.snapshot.FormalSnapshotError):run(claim,'frontend',expected_shared_scope_sha256=scope,guard=selected_guard,container_actions=protected)
                assert len(calls)==6
            # SQL/fence is still required for MySQL through the original path.
            with pytest.raises(m.snapshot.FormalSnapshotError):m.stop_shared_container(claim,'mysql',guard=selected_guard)
            claim.record('db-fenced',{});claim.record('split-sql-captured',{})
            m.stop_shared_container(claim,'mysql',guard=selected_guard)
            assert rows['1'*64]['state']['Running'] is False
    finally:
        for patcher in reversed(patchers):patcher.stop()


def test_legacy_shared_writer_still_requires_database_fence(tmp_path):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        m.bind_shared_container_scope(claim,{r:str(i)*64 for i,r in enumerate(ROLES,1)})
        with patch.object(m.snapshot,'run_formal_container_action') as action:
            with pytest.raises(m.snapshot.FormalSnapshotError):m.stop_shared_container(claim,'frontend',guard=guard)
            action.assert_not_called()
        assert not any(e['step']=='shared-frontend-stop-intent' for e in claim.events)
    finally:
        for patcher in reversed(patchers):patcher.stop()

@pytest.mark.parametrize('case',['normal','original-stopped','missing','strategy','before','required',
    'completion','runtime','after-fence','guard-drift','missing-guard'])
def test_controlled_capture_requires_exact_completed_prefence_stops_without_new_actions(tmp_path,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        if case=='original-stopped':rows['2'*64]['state']['Running']=False
        originals=m.bind_shared_container_scope(claim,{r:str(i)*64 for i,r in enumerate(ROLES,1)})
        scope=hashlib.sha256(m.snapshot._canonical_bytes(originals)).hexdigest()
        def perform(kind,identity):rows[identity]['state'].update(Running=False,FinishedAt='owned-controlled-stop')
        protected=ControlledActionProtocol(rows,perform)
        for role in m.STOP_ORDER[:-1]:m.stop_controlled_shared_writer(claim,role,expected_shared_scope_sha256=scope,guard=guard,container_actions=protected)
        claim.record('db-fenced',{})
        intent=m._event(claim,'shared-frontend-stop-intent');complete=m._event(claim,'shared-frontend-stopped')
        if case=='missing':claim.events=[e for e in claim.events if e['step']!='shared-frontend-stopped']
        if case=='strategy':intent['stopStrategy']='LEGACY'
        if case=='before':intent['before']['StartedAt']='changed'
        if case=='required':intent['actionRequired']=False
        if case in ('strategy','before','required'):
            complete['intentSha256']=hashlib.sha256(m.snapshot._canonical_bytes(intent)).hexdigest()
        if case=='completion':complete['actionPerformed']=False
        if case=='runtime':rows['2'*64]['state']['Running']=True
        if case=='after-fence':claim.events.insert(1,claim.events.pop())
        def selected_guard():
            guard()
            if case=='guard-drift':rows['2'*64]['state']['Running']=True
        events=copy.deepcopy(claim.events)
        with patch.object(m.snapshot,'run_formal_container_action') as action:
            if case in ('normal','original-stopped'):
                result=m.assert_controlled_prefence_stops(claim,expected_shared_scope_sha256=scope,guard=selected_guard)
                assert result['state']=='CONTROLLED_PREFENCE_STOPS_OBSERVED'
                assert result['scopeSha256']==scope and result['productionEvidence'] is False
            else:
                with pytest.raises(m.snapshot.FormalSnapshotError):
                    m.assert_controlled_prefence_stops(claim,expected_shared_scope_sha256=scope,
                        guard=None if case=='missing-guard' else selected_guard)
            action.assert_not_called()
        assert claim.events==events and not mysql.calls
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('case',['pre-intent-budget','known-before-start','unknown-start','tampered-abort'])
def test_native_prestart_refusal_requires_explicit_retained_proof(tmp_path,monkeypatch,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        prepare_sql(m,claim,mysql,source,rows,guard);m.stop_shared_container(claim,'mysql',guard=guard)
        fence={'mount':str(source),'seconds':5,'binding':{'operationId':'owned-split','bootId':'owned-boot',
            'mount':{'source':'/dev/owned','device':'9:1','fstype':'ext4','target':str(source)}}}
        monkeypatch.setattr(m.snapshot.time,'monotonic',lambda:10.)
        def fail(*a,**k):
            if case=='unknown-start':raise m.snapshot.FormalSnapshotError('FORMAL_DATA_DEADLINE')
            raise m.snapshot.NativeCaptureNotStarted('FORMAL_DATA_DEADLINE')
        monkeypatch.setattr(m.snapshot,'_capture_native_physical',fail)
        token=m.snapshot.observation_deadline.set(20. if case=='pre-intent-budget' else 100.)
        try:
            with pytest.raises(m.snapshot.FormalSnapshotError):m.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
        finally:m.snapshot.observation_deadline.reset(token)
        if case=='pre-intent-budget':
            assert not any(e['step']=='split-raw-intent' for e in claim.events)
            assert m._thaw_receipt(claim)=={'nativeState':'NOT_REQUESTED'}
        elif case=='unknown-start':
            with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_SHARED_THAW_REQUIRED'):m._thaw_receipt(claim)
            assert not any(e['step']=='split-native-not-requested' for e in claim.events)
        else:
            monkeypatch.setattr(m,'_observe_native_environment',lambda _:None)
            monkeypatch.setattr(m.stat,'S_IMODE',lambda _:0o600)
            if case=='tampered-abort':(claim.root/'payload/092-native-not-requested.json').write_bytes(b'{}')
            if case=='tampered-abort':
                with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_SHARED_THAW_REQUIRED'):m._thaw_receipt(claim)
            else:
                receipt=m._thaw_receipt(claim)
                assert receipt['nativeState']=='NOT_REQUESTED' and len(receipt['nativeAbortSha256'])==64
                assert m.begin_shared_recovery(claim,guard=guard,thaw_guard=guard)==receipt
        assert not (claim.root/'payload/040-payload.json').exists()
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('state',['short','expired','timeout'])
def test_native_mount_observation_respects_outer_budget_and_retains_error(tmp_path,monkeypatch,state):
    import subprocess
    m=importlib.import_module('formal_split_capture');native=importlib.import_module('formal_filesystem_watchdog')
    mount=tmp_path/'mount';mount.mkdir()
    binding={'bootId':'owned-boot','mount':{'source':'/dev/owned','device':'9:1','fstype':'ext4','target':str(mount)}}
    monkeypatch.setattr(native,'require_native_runtime',lambda:None)
    monkeypatch.setattr(m.Path,'read_text',lambda self,*a,**k:'owned-boot')
    monkeypatch.setattr(native.os,'major',lambda _:9,raising=False)
    monkeypatch.setattr(native.os,'minor',lambda _:1,raising=False)
    monkeypatch.setattr(m.snapshot.time,'monotonic',lambda:13. if state=='expired' else 10.)
    seen=[]
    def execute(command,**kwargs):
        seen.append(kwargs['timeout']);assert 0<kwargs['timeout']<=1.
        if state=='timeout':raise subprocess.TimeoutExpired(command,kwargs['timeout'])
        return subprocess.CompletedProcess(command,0,json.dumps({'filesystems':[{'source':'/dev/owned','maj:min':'9:1','fstype':'ext4','target':str(mount)}]}).encode(),b'')
    monkeypatch.setattr(native.subprocess,'run',execute)
    token=m.snapshot.observation_deadline.set(11.)
    try:
        if state=='short':m._observe_native_environment(binding);assert seen==[1.]
        else:
            with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_DATA_DEADLINE' if state=='expired' else 'FORMAL_SHARED_THAW_REQUIRED'):m._observe_native_environment(binding)
            assert len(seen)==(0 if state=='expired' else 1)
    finally:m.snapshot.observation_deadline.reset(token)


def fixture(tmp_path):
    m=importlib.import_module('formal_split_capture')
    class Claim:
        def __init__(self):
            self.root=tmp_path/'claim';self.root.mkdir(mode=0o700)
            self.events=[];self.closed=False
            self._plan=json.dumps({'operationId':'owned-split','sourceFingerprint':'a'*64,
                'databaseUuid':'0fa6c22b-5a90-11f1-ade1-0242ac150002','minioContainerId':'7'*64}).encode()
        def assert_owned(self): assert not self.closed
        def record(self,step,details):
            self.assert_owned();assert step in m.snapshot.FormalMaintenanceClaim._steps
            assert not any(e['step']==step for e in self.events)
            self.events.append({'step':step,'details':copy.deepcopy(details)})
        def _write(self,path,raw):
            with Path(path).open('xb') as f:f.write(raw)
            Path(path).chmod(0o600)
        def _read(self,path):return Path(path).read_bytes()
    rows={}
    for index,role in enumerate(ROLES,1):
        rows[str(index)*64]={'id':str(index)*64,'name':'/'+role,'image':'sha256:'+'b'*64,
            'mounts':[],'restartPolicy':{'Name':'unless-stopped','MaximumRetryCount':0},
            'state':{'Running':True,'Paused':False,'Restarting':False,'Dead':False,
                'OOMKilled':False,'ExitCode':0,'StartedAt':'original-start','FinishedAt':'zero'}}
    source=tmp_path/'source';source.mkdir()
    (source/'xl.meta').write_bytes(b'OWNED_REAL_FILE_BYTES')
    class Mysql:
        container='1'*64
        _password='contract-private';timeout=30
        def __init__(self):self.calls=[];self.probe_calls=[]
        def probe_rejected_zero_row_update(self,database,table,column):
            self.probe_calls.append((database,table,column))
            sql='UPDATE `'+database+'`.`'+table+'` SET `'+column+'`=`'+column+'` WHERE 1=0;'
            return {'result':'REJECTED','mysqlErrorNumber':1290,
                'statementSha256':hashlib.sha256(sql.encode()).hexdigest(),
                'observedAt':datetime.now(timezone.utc).isoformat()}
        def dump(self,database,path,*,guard):
            assert callable(guard);guard()
            self.calls.append('dump');path.write_bytes(gzip.compress(b'ACTUAL_TEST_SQL',mtime=0));path.chmod(0o600)
            return {'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
        def query(self,sql):raise AssertionError('split raw stage must not query MySQL')
    claim=Claim();mysql=Mysql();guards=[]
    def guard():claim.assert_owned();guards.append('guard')
    def inspect(identity):return copy.deepcopy(rows[identity])
    def barrier(claim,mysql,*,dump_in_progress=False):claim.assert_owned()
    def action(kind,identity):
        assert kind=='stop'
        rows[identity]['state'].update(Running=False,FinishedAt='owned-stopped')
    def native(control,mount,binding,src,dst,seconds,*,guard):
        assert callable(guard);guard()
        assert not any(row['state']['Running'] for row in rows.values())
        before=len(mysql.calls)
        physical=m.snapshot.copy_physical_minio_tree(src,dst)
        assert len(mysql.calls)==before
        return {'state':'RAW_CAPTURED_AND_THAWED_PENDING_FORMAL_BARRIERS',
            'physical':physical,'productionEvidence':False}
    patchers=[patch.object(m.snapshot,'inspect_formal_container',side_effect=inspect),
        patch.object(m,'observe_data',side_effect=lambda client,database,uuid,**kw:
            contract_observation(client.container,uuid,database)),
        patch.object(m,'observe_split_sql_barrier',side_effect=barrier),
        patch.object(m.snapshot,'run_formal_container_action',side_effect=action),
        patch.object(m.snapshot,'_require_native_physical_runtime'),
        patch.object(m.snapshot,'_capture_native_physical',side_effect=native),
        patch.object(m.snapshot,'collect_docker_barrier_records',return_value=[]),
        patch.object(m.snapshot,'observe_stopped_object_containers',return_value={})]
    for p in patchers:p.start()
    return m,claim,mysql,source,rows,guard,guards,patchers


def stop(rows,roles=ROLES):
    for role in roles:
        row=next(row for row in rows.values() if row['name']=='/'+role)
        row['state'].update(Running=False,FinishedAt='owned-stopped')


def prepare_sql(m,claim,mysql,source,rows,guard):
    m.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
    claim.record('db-fenced',{})
    for role in m.STOP_ORDER[:-1]:m.stop_shared_container(claim,role,guard=guard)
    return m.capture_sql_phase(claim,mysql,'owned_probe',source,guard=guard,data_limits=DATA_LIMITS,
        rejection_probe=REJECTION_PROBE)


@pytest.mark.parametrize('phase',['sql','sql-tail','raw'])
def test_capture_refuses_recovery_intent_created_during_work(tmp_path,phase):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        if phase=='sql-tail':
            base_guard=guard
            def tail_guard():
                base_guard()
                if any(e['step']=='split-db-probe-after' for e in claim.events) and not any(e['step']=='ingress-recovery-intent' for e in claim.events):claim.record('ingress-recovery-intent',{})
            action=lambda:prepare_sql(m,claim,mysql,source,rows,tail_guard)
        elif phase=='sql':
            original=mysql.dump
            def dump(*args,guard):
                result=original(*args,guard=guard);claim.record('ingress-recovery-intent',{});return result
            mysql.dump=dump
            action=lambda:prepare_sql(m,claim,mysql,source,rows,guard)
        else:
            prepare_sql(m,claim,mysql,source,rows,guard);m.stop_shared_container(claim,'mysql',guard=guard)
            original=m.snapshot._capture_native_physical
            def native(*args,guard):
                result=original(*args,guard=guard);claim.record('ingress-recovery-intent',{});return result
            patcher=patch.object(m.snapshot,'_capture_native_physical',side_effect=native);patcher.start();patchers.append(patcher)
            action=lambda:m.capture_raw_phase(claim,source,filesystem_fence={'mount':str(tmp_path),'binding':{'operationId':'owned-split'},'seconds':5},guard=guard,native_guard=guard)
        with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_SPLIT_STAGE_INVALID'):action()
        assert not any(e['step']==('split-sql-captured' if phase.startswith('sql') else 'split-captured') for e in claim.events)
        assert not (claim.root/'payload'/('020-sql.json' if phase.startswith('sql') else '040-payload.json')).exists()
        assert (claim.root/'payload'/('090-sql-failed.json' if phase.startswith('sql') else '091-raw-failed.json')).exists()
    finally:
        for patcher in reversed(patchers):patcher.stop()


@pytest.mark.parametrize('case',['pass','accepted-before','accepted-after','wrong-error','wrong-statement',
    'stale-time','uncertain','write-interrupted','receipt-drift','receipt-missing','scope-changed',
    'receipt-drift-during-raw'])
def test_split_sql_requires_bound_rejection_without_legacy_events(tmp_path,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    original=mysql.probe_rejected_zero_row_update
    record=claim.record
    def probe(database,table,column):
        phase='before' if not mysql.probe_calls else 'after'
        assert any(e['step']=='split-db-probe-'+phase+'-intent' for e in claim.events)
        result=original(database,table,column)
        if case=='uncertain':raise m.snapshot.FormalSnapshotError('FORMAL_MYSQL_PROBE_TIMEOUT')
        if case=='accepted-'+phase:result['result']='ACCEPTED'
        if case=='wrong-error':result['mysqlErrorNumber']=1142
        if case=='wrong-statement':result['statementSha256']='c'*64
        if case=='stale-time':result['observedAt']='2000-01-01T00:00:00+00:00'
        return result
    def interrupted(step,details):
        if case=='write-interrupted' and step=='split-db-probe-before':
            raise m.snapshot.FormalSnapshotError('FORMAL_MAINTENANCE_JOURNAL_WRITE_FAILED')
        return record(step,details)
    mysql.probe_rejected_zero_row_update=probe;claim.record=interrupted
    valid=case in ('pass','receipt-drift','receipt-missing','scope-changed','receipt-drift-during-raw')
    try:
        if not valid:
            with pytest.raises(m.snapshot.FormalSnapshotError):
                prepare_sql(m,claim,mysql,source,rows,guard)
            assert mysql.calls==(['dump'] if case=='accepted-after' else [])
            assert len(mysql.probe_calls)==(2 if case=='accepted-after' else 1)
            assert not any(e['step']=='split-sql-captured' for e in claim.events)
            assert (claim.root/'payload/090-sql-failed.json').is_file()
            if case=='write-interrupted':
                assert (claim.root/'payload/006-database-rejection-before.json').exists()
            with pytest.raises(m.snapshot.FormalSnapshotError):
                m.capture_sql_phase(claim,mysql,'owned_probe',source,guard=guard,
                    data_limits=DATA_LIMITS,rejection_probe=REJECTION_PROBE)
            assert len(mysql.probe_calls)==(2 if case=='accepted-after' else 1)
            return
        result=prepare_sql(m,claim,mysql,source,rows,guard)
        assert mysql.probe_calls==[('owned_probe','probe','id')]*2
        assert not any(e['step'] in ('backend-stopped','minio-stopped','captured') for e in claim.events)
        references=result['databaseRejectionEvidence']
        for phase,number in (('before','006'),('after','018')):
            p=claim.root/('payload/'+number+'-database-rejection-'+phase+'.json')
            value=json.loads(p.read_bytes())
            assert value['operationId']=='owned-split' and value['phase']==phase
            assert value['claimBindingSha256']==hashlib.sha256(claim._plan).hexdigest()
            assert value['sqlSha256']==(None if phase=='before' else result['database']['sha256'])
            assert references[phase+'Sha256']==hashlib.sha256(p.read_bytes()).hexdigest()
        if case!='pass':
            p=claim.root/'payload/006-database-rejection-before.json'
            if case=='receipt-drift-during-raw':
                prior=m.snapshot._capture_native_physical.side_effect
                def tamper(*a,**kw):
                    result=prior(*a,**kw);p.write_bytes(b'{}');return result
                m.snapshot._capture_native_physical.side_effect=tamper
                m.stop_shared_container(claim,'mysql',guard=guard)
                fence={'mount':str(tmp_path),'binding':{'operationId':'owned-split'},'seconds':5}
                with pytest.raises(m.snapshot.FormalSnapshotError):
                    m.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
                assert not any(e['step']=='split-captured' for e in claim.events)
                return
            if case=='receipt-missing':p.unlink()
            elif case=='receipt-drift':p.write_bytes(b'{}')
            else:
                value=json.loads(p.read_bytes());value['database']='different'
                p.write_bytes(m.snapshot._canonical_bytes(value))
            m.stop_shared_container(claim,'mysql',guard=guard)
            with pytest.raises(m.snapshot.FormalSnapshotError):
                m.capture_raw_phase(claim,source,filesystem_fence={},guard=guard,native_guard=guard)
            assert not any(e['step']=='split-raw-intent' for e in claim.events)
    finally:
        for p in reversed(patchers):p.stop()


@pytest.mark.parametrize('case',['pass','changed-after-dump','proof-tamper-before-raw',
    'binding-operation-changed','binding-sql-changed','after-proof-missing','claim-changed','dotted-operation'])
def test_capture_requires_source_data_bound_to_claim_sql_and_operation(tmp_path,monkeypatch,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    if case=='dotted-operation':
        plan=json.loads(claim._plan);plan['operationId']='capture.20261002';claim._plan=m.snapshot._canonical_bytes(plan)
    calls=[]
    def observe(client,database,uuid,*,guard,scratch,**limits):
        guard();calls.append(mysql.calls.copy());guard()
        return contract_observation(mysql.container,uuid,value='changed' if
            case=='changed-after-dump' and len(calls)==2 else 'original')
    monkeypatch.setattr(m,'observe_data',observe,raising=False)
    try:
        if case=='changed-after-dump':
            with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_DATA_MISMATCH'):
                prepare_sql(m,claim,mysql,source,rows,guard)
            assert not any(e['step']=='split-sql-captured' for e in claim.events)
            assert (claim.root/'payload/090-sql-failed.json').is_file()
            return
        sql=prepare_sql(m,claim,mysql,source,rows,guard)
        assert calls==[[],['dump']]
        binding=json.loads((claim.root/'payload/015-source-data-binding.json').read_bytes())
        assert binding['operationId']==('capture.20261002' if case=='dotted-operation' else 'owned-split')
        assert binding['claimBindingSha256']==hashlib.sha256(claim._plan).hexdigest()
        assert binding['sqlSha256']==sql['database']['sha256']
        if case not in ('pass','changed-after-dump','dotted-operation'):
            if case=='proof-tamper-before-raw':(claim.root/'payload/011-source-data.json').write_bytes(b'{}')
            if case=='after-proof-missing':(claim.root/'payload/012-source-data-after.json').unlink()
            if case.startswith('binding-'):
                binding['operationId' if case=='binding-operation-changed' else 'sqlSha256']='f'*64
                (claim.root/'payload/015-source-data-binding.json').write_bytes(m.snapshot._canonical_bytes(binding))
            if case=='claim-changed':
                plan=json.loads(claim._plan);plan['sourceFingerprint']='b'*64
                claim._plan=m.snapshot._canonical_bytes(plan)
            m.stop_shared_container(claim,'mysql',guard=guard)
            with pytest.raises(m.snapshot.FormalSnapshotError,match='FORMAL_DATA_SOURCE_CHANGED'):
                m.capture_raw_phase(claim,source,filesystem_fence={},guard=guard,native_guard=guard)
            assert not any(e['step']=='split-raw-intent' for e in claim.events)
    finally:
        for p in reversed(patchers):p.stop()


@pytest.mark.parametrize('case',['pass','missing-role','duplicate-id','changed-mysql','before-sql',
    'mysql-running','other-running','paused','stop-error','image-drift','policy-drift',
    'sql-drift','receipt-drift','guard-drift','lost-native','repeat-sql','repeat-raw','restore-started'])
def test_split_capture_holds_order_and_retains_failures(tmp_path,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        if case in ('missing-role','duplicate-id'):
            scope={role:str(i)*64 for i,role in enumerate(ROLES,1)}
            if case=='missing-role':scope.pop('redis')
            else:scope['mysql']=scope['redis']
            with pytest.raises(m.snapshot.FormalSnapshotError):m.bind_shared_container_scope(claim,scope)
            assert not claim.events;return
        if case=='before-sql':
            with pytest.raises(m.snapshot.FormalSnapshotError):
                m.capture_raw_phase(claim,source,filesystem_fence={},guard=guard,native_guard=guard)
            assert not (claim.root/'payload').exists();return
        if case=='changed-mysql':mysql.container='a'*64
        if case=='changed-mysql':
            with pytest.raises(m.snapshot.FormalSnapshotError):prepare_sql(m,claim,mysql,source,rows,guard)
            assert not mysql.calls;return
        sql=prepare_sql(m,claim,mysql,source,rows,guard)
        assert sql['state']=='SQL_CAPTURED_PENDING_RAW_AND_FULL_BARRIERS'
        if case=='repeat-sql':
            with pytest.raises(m.snapshot.FormalSnapshotError):m.capture_sql_phase(claim,mysql,'owned_probe',source,guard=guard,data_limits=DATA_LIMITS,rejection_probe=REJECTION_PROBE)
            assert mysql.calls==['dump'];return
        m.stop_shared_container(claim,'mysql',guard=guard)
        if case=='mysql-running':rows['1'*64]['state']['Running']=True
        if case=='other-running':rows['2'*64]['state']['Running']=True
        if case=='paused':rows['1'*64]['state']['Paused']=True
        if case=='stop-error':rows['1'*64]['state']['ExitCode']=137
        if case=='image-drift':rows['1'*64]['image']='sha256:'+'c'*64
        if case=='policy-drift':rows['1'*64]['restartPolicy']['Name']='always'
        if case=='sql-drift':(claim.root/'payload/database.sql.gz').write_bytes(b'CHANGED_SQL')
        if case=='receipt-drift':(claim.root/'payload/020-sql.json').write_bytes(b'{}')
        if case=='restore-started':claim.record('minio-restore-intent',{})
        fence={'mount':str(tmp_path),'binding':{'operationId':'owned-split'},'seconds':5}
        if case=='guard-drift':
            def guard():raise m.snapshot.FormalSnapshotError('FORMAL_SPLIT_CALLER_CHANGED')
        if case=='lost-native':
            def lost(*args,**kwargs):raise m.snapshot.FormalSnapshotError('WATCHDOG_CAPTURE_INVALIDATED')
            patchers.append(patch.object(m.snapshot,'_capture_native_physical',side_effect=lost));patchers[-1].start()
        valid=case in ('pass','repeat-raw')
        if valid:
            result=m.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
            assert result['state']=='CAPTURED_PENDING_FULL_BARRIER_PROOF'
            assert result['hostWritersVerified'] is False
            assert result['database']==sql['database']
            assert (claim.root/'payload/minio-physical/xl.meta').read_bytes()==b'OWNED_REAL_FILE_BYTES'
            before=(claim.root/'payload/040-payload.json').read_bytes()
            assert mysql.calls==['dump']
            if case=='repeat-raw':
                with pytest.raises(m.snapshot.FormalSnapshotError):m.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
                assert (claim.root/'payload/040-payload.json').read_bytes()==before
        else:
            expected={
                'mysql-running':'FORMAL_SPLIT_SHARED_NOT_STOPPED',
                'other-running':'FORMAL_SPLIT_SHARED_NOT_STOPPED',
                'paused':'FORMAL_CONTAINER_RUNTIME_INVALID',
                'stop-error':'FORMAL_SPLIT_SHARED_NOT_STOPPED',
                'image-drift':'FORMAL_SPLIT_SHARED_SCOPE_CHANGED',
                'policy-drift':'FORMAL_SPLIT_SHARED_SCOPE_CHANGED',
                'sql-drift':'FORMAL_SPLIT_SQL_CHANGED',
                'receipt-drift':'FORMAL_SPLIT_SQL_RECEIPT_CHANGED',
                'guard-drift':'FORMAL_SPLIT_CALLER_CHANGED',
                'lost-native':'WATCHDOG_CAPTURE_INVALIDATED',
                'restore-started':'FORMAL_SPLIT_STAGE_INVALID'}[case]
            with pytest.raises(m.snapshot.FormalSnapshotError) as failure:
                m.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
            assert failure.value.code==expected
            if case=='lost-native':
                assert any(e['step']=='split-raw-intent' for e in claim.events)
                assert (claim.root/'payload/091-raw-failed.json').is_file()
            assert not (claim.root/'payload/040-payload.json').exists()
            assert not any(e['step']=='split-captured' for e in claim.events)
        assert mysql.calls==['dump']
    finally:
        for p in reversed(patchers):p.stop()


@pytest.mark.parametrize('case', ['pass','original-stopped','repeat-stop','stop-order',
    'mysql-before-sql','stop-interrupted','stop-not-applied','stop-failed-exit',
    'scope-drift','guard-after-intent','restore-before-recovery','thaw-missing',
    'thaw-changed','restore-order','start-interrupted','start-not-applied',
    'repeat-start','restart-flags-reset','original-readonly','db-interrupted',
    'db-not-applied','db-uuid-drift','restore-runtime-drift','partial-stop-recovery'])
def test_shared_actions_preserve_original_and_reconcile_without_replay(tmp_path,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    actions=[];sets=[]
    original={'serverUuid':json.loads(claim._plan)['databaseUuid'],'readOnly':int(case=='original-readonly'),'superReadOnly':0}
    flags={**original,'readOnly':1,'superReadOnly':1}
    def db_original(c):return original['serverUuid'],copy.deepcopy(original),'a'*64
    def query(sql):
        if sql.startswith('SET GLOBAL'):
            sets.append(sql);flags.update(original)
        else:raise AssertionError(sql)
    mysql.query=query
    def action(kind,identity):
        # The exact durable intent must precede the actual mutation.
        role=next(r for r in ROLES if rows[identity]['name']=='/'+r)
        step='shared-'+role+('-stop-intent' if kind=='stop' else '-restore-intent')
        assert any(e['step']==step for e in claim.events)
        actions.append((kind,identity))
        if kind=='stop':rows[identity]['state'].update(Running=False,FinishedAt='owned-stop')
        else:rows[identity]['state'].update(Running=True,StartedAt='owned-restart',FinishedAt='zero')
    patchers.extend([patch.object(m.snapshot,'_claimed_database_original',side_effect=db_original),
        patch.object(m.snapshot,'_database_flags',side_effect=lambda *a:copy.deepcopy(flags)),
        patch.object(m.snapshot,'run_formal_container_action',side_effect=action)])
    for p in patchers[-3:]:p.start()
    def expect(code,fn):
        with pytest.raises(m.snapshot.FormalSnapshotError) as e:fn()
        assert e.value.code==code
    def begin():return m.begin_shared_recovery(claim,guard=guard,thaw_guard=guard)
    def stop_role(role):return m.stop_shared_container(claim,role,guard=guard)
    def restore(role):return m.restore_shared_container(claim,role,guard=guard,thaw_guard=guard)
    try:
        if case=='original-stopped':rows['4'*64]['state']['Running']=False
        m.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
        claim.record('db-fenced',{'originalSha256':'a'*64})
        if case=='stop-order':
            expect('FORMAL_SHARED_ACTION_ORDER_INVALID',lambda:stop_role('minio'));assert not actions;return
        if case=='mysql-before-sql':
            for role in m.STOP_ORDER[:-1]:stop_role(role)
            expect('FORMAL_SPLIT_STAGE_REQUIRED',lambda:stop_role('mysql'));return
        if case in ('stop-interrupted','stop-not-applied','stop-failed-exit','guard-after-intent'):
            real_record=claim.record
            if case=='guard-after-intent':
                def guarded():
                    if any(e['step']=='shared-frontend-stop-intent' for e in claim.events):
                        raise m.snapshot.FormalSnapshotError('FORMAL_SPLIT_CALLER_CHANGED')
                guard=guarded
                expect('FORMAL_SPLIT_CALLER_CHANGED',lambda:stop_role('frontend'));assert not actions
                return
            def record(step,details):
                if step=='shared-frontend-stopped':raise RuntimeError('OWNED_INTERRUPTION')
                real_record(step,details)
            claim.record=record
            with pytest.raises(RuntimeError,match='OWNED_INTERRUPTION'):stop_role('frontend')
            claim.record=real_record
            before=list(actions)
            expect('FORMAL_SHARED_ACTION_REPLAY_REFUSED',lambda:stop_role('frontend'))
            if case=='stop-not-applied':rows['2'*64]['state'].update(Running=True,FinishedAt='zero')
            if case=='stop-failed-exit':rows['2'*64]['state']['ExitCode']=137
            if case!='stop-interrupted':
                expect('FORMAL_SHARED_RECONCILIATION_REQUIRED',lambda:m.reconcile_shared_container(claim,'frontend','stop',guard=guard))
            else:m.reconcile_shared_container(claim,'frontend','stop',guard=guard)
            assert actions==before;return
        if case=='scope-drift':
            rows['2'*64]['restartPolicy']['Name']='always'
            expect('FORMAL_SPLIT_SHARED_SCOPE_CHANGED',lambda:stop_role('frontend'));assert not actions;return
        if case=='restore-before-recovery':
            expect('FORMAL_SPLIT_STAGE_REQUIRED',lambda:restore('mysql'));assert not actions;return
        if case=='partial-stop-recovery':
            stop_role('frontend');begin()
        else:
            for role in m.STOP_ORDER[:-1]:stop_role(role)
            if case=='repeat-stop':
                count=len(actions);expect('FORMAL_SHARED_ACTION_REPLAY_REFUSED',lambda:stop_role('frontend'))
                assert len(actions)==count;return
            claim.record('split-sql-captured',{})
            stop_role('mysql')
            if case in ('thaw-missing','thaw-changed'):
                binding={'operationId':'owned-split'}
                claim.record('split-raw-intent',{'nativeBinding':binding})
                if case=='thaw-changed':
                    root=claim.root/'payload/native-fence';root.mkdir(parents=True)
                    (root/'020-thawed.json').write_bytes(m.snapshot._canonical_bytes({'status':'THAWED_CAPTURE_REMAINS_INVALID','bindingSha256':'b'*64,'observedBootSeconds':2}))
                expect('FORMAL_SHARED_THAW_REQUIRED',begin);assert not any(a[0]=='start' for a in actions);return
            begin()
        if case=='restore-order':
            expect('FORMAL_SHARED_ACTION_ORDER_INVALID',lambda:restore('minio'));return
        if case in ('start-interrupted','start-not-applied'):
            real_record=claim.record
            def record(step,details):
                if step=='shared-mysql-restored':raise RuntimeError('OWNED_INTERRUPTION')
                real_record(step,details)
            claim.record=record
            with pytest.raises(RuntimeError,match='OWNED_INTERRUPTION'):restore('mysql')
            claim.record=real_record;before=list(actions)
            expect('FORMAL_SHARED_ACTION_REPLAY_REFUSED',lambda:restore('mysql'))
            if case=='start-not-applied':rows['1'*64]['state'].update(Running=False,StartedAt='original-start',FinishedAt='owned-stop')
            if case=='start-not-applied':
                expect('FORMAL_SHARED_RECONCILIATION_REQUIRED',lambda:m.reconcile_shared_container(claim,'mysql','start',guard=guard,thaw_guard=guard))
            else:m.reconcile_shared_container(claim,'mysql','start',guard=guard,thaw_guard=guard)
            assert actions==before;return
        restore('mysql')
        if case=='repeat-start':
            before=list(actions);expect('FORMAL_SHARED_ACTION_REPLAY_REFUSED',lambda:restore('mysql'));assert actions==before;return
        if case=='restore-runtime-drift':
            rows['1'*64]['state']['StartedAt']='unowned-second-restart'
            expect('FORMAL_SHARED_DATABASE_RUNTIME_CHANGED',lambda:m.restore_shared_database_flags(claim,mysql,guard=guard,thaw_guard=guard));assert not sets;return
        if case in ('restart-flags-reset','original-readonly'):flags.update(readOnly=0,superReadOnly=0)
        if case=='db-uuid-drift':
            flags['serverUuid']='0'*36
            expect('FORMAL_SHARED_DATABASE_IDENTITY_CHANGED',lambda:m.restore_shared_database_flags(claim,mysql,guard=guard,thaw_guard=guard));assert not sets;return
        if case in ('db-interrupted','db-not-applied'):
            real_record=claim.record
            def record(step,details):
                if step=='shared-db-restored':raise RuntimeError('OWNED_INTERRUPTION')
                real_record(step,details)
            claim.record=record
            with pytest.raises(RuntimeError,match='OWNED_INTERRUPTION'):m.restore_shared_database_flags(claim,mysql,guard=guard,thaw_guard=guard)
            claim.record=real_record;before=list(sets)
            if case=='db-not-applied':flags.update(readOnly=1,superReadOnly=1)
            expect('FORMAL_SHARED_ACTION_REPLAY_REFUSED',lambda:m.restore_shared_database_flags(claim,mysql,guard=guard,thaw_guard=guard))
            if case=='db-not-applied':
                expect('FORMAL_SHARED_RECONCILIATION_REQUIRED',lambda:m.reconcile_shared_database_flags(claim,mysql,guard=guard,thaw_guard=guard))
            else:m.reconcile_shared_database_flags(claim,mysql,guard=guard,thaw_guard=guard)
            assert sets==before;return
        m.restore_shared_database_flags(claim,mysql,guard=guard,thaw_guard=guard)
        assert flags==original
        assert len(sets)==int(case!='restart-flags-reset')
        for role in m.RESTORE_ORDER[1:]:restore(role)
        assert all(row['state']['Running']==(case!='original-stopped' or row['name']!='/showroom') for row in rows.values())
        if case in ('original-stopped','partial-stop-recovery'):
            untouched='4'*64 if case=='original-stopped' else '1'*64
            assert not any(identity==untouched for kind,identity in actions)
        assert not any(e['step']=='db-restored' for e in claim.events)
        proof=m.verify_shared_restoration(claim,mysql,guard=guard,thaw_guard=guard)
        assert proof['state']=='ORIGINAL_SERVICES_RESTORED_PENDING_CALLER_PROOF'
        assert proof['productionEvidence'] is False
        rows['3'*64]['state']['StartedAt']='changed-after-restoration'
        expect('FORMAL_SHARED_RUNTIME_CHANGED',lambda:m.verify_shared_restoration(claim,mysql,guard=guard,thaw_guard=guard))
    finally:
        for p in reversed(patchers):p.stop()


@pytest.mark.parametrize('case',['accepted','invalidated','missing-ready','wrong-binding',
    'wrong-boot','wrong-mount','earlier-thaw','nonprivate','pending-stop','changed-thaw-before-start'])
def test_native_recovery_gate_precedes_any_docker_observation(tmp_path,case):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        m.bind_shared_container_scope(claim,{role:str(i)*64 for i,role in enumerate(ROLES,1)})
        if case=='pending-stop':
            claim.record('shared-frontend-stop-intent',{'scopeSha256':'a'*64,'before':rows['2'*64]['state'],'actionRequired':True})
            with pytest.raises(m.snapshot.FormalSnapshotError) as e:
                m.begin_shared_recovery(claim,guard=guard,thaw_guard=guard)
            assert e.value.code=='FORMAL_SHARED_PENDING_ACTION_REQUIRED';return
        root=claim.root/'payload/native-fence';root.mkdir(mode=0o700,parents=True)
        binding={'operationId':'owned-split','bootId':'owned-boot',
            'mount':{'source':'/dev/loop1','device':'7:1','fstype':'ext4','target':str(source)}}
        digest=hashlib.sha256(m.snapshot._canonical_bytes(binding)).hexdigest()
        claim.record('split-raw-intent',{'nativeBinding':binding,'nativeMount':str(source),'nativeControlRoot':str(root)})
        ready={'status':'READY_NOT_FENCE_PROOF','binding':binding,'bindingSha256':digest,
            'startedBootSeconds':10,'deadlineBootSeconds':20,'pid':123,'productionEvidence':False}
        row={'status':'THAWED_CAPTURE_REMAINS_INVALID' if case=='invalidated' else 'THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS',
            'bindingSha256':digest,'observedBootSeconds':11}
        if case=='wrong-binding':row['bindingSha256']='0'*64
        if case=='earlier-thaw':row['observedBootSeconds']=9
        if case!='missing-ready':
            (root/'000-ready.json').write_bytes(m.snapshot._canonical_bytes(ready));(root/'000-ready.json').chmod(0o600)
        thaw=root/'020-thawed.json';thaw.write_bytes(m.snapshot._canonical_bytes(row));thaw.chmod(0o600)
        if case=='nonprivate':root.chmod(0o755)
        def observe_native(value):
            assert value==binding
            if case in ('wrong-boot','wrong-mount'):raise m.snapshot.FormalSnapshotError('FORMAL_SHARED_THAW_REQUIRED')
        # Explicit POSIX mode observation double for this host-portable unit test;
        # the owned Linux fixture verifies actual directory/file modes.
        def mode(value):return (0o755 if case=='nonprivate' else 0o700) if m.stat.S_ISDIR(value) else 0o600
        with patch.object(m,'_observe_native_environment',side_effect=observe_native), patch.object(m.stat,'S_IMODE',side_effect=mode):
            valid=case in ('accepted','invalidated','changed-thaw-before-start')
            with patch.object(m.snapshot,'inspect_formal_container',side_effect=lambda _: (_ for _ in ()).throw(AssertionError('Docker queried before thaw'))):
                if not valid:
                    with pytest.raises(m.snapshot.FormalSnapshotError) as e:
                        m.begin_shared_recovery(claim,guard=guard,thaw_guard=guard)
                    assert e.value.code=='FORMAL_SHARED_THAW_REQUIRED';return
            result=m.begin_shared_recovery(claim,guard=guard,thaw_guard=guard)
            assert result['nativeState']==row['status']
            if case=='changed-thaw-before-start':
                thaw.write_bytes(b'{}')
                with patch.object(m.snapshot,'inspect_formal_container',side_effect=AssertionError('Docker queried before thaw')):
                    with pytest.raises(m.snapshot.FormalSnapshotError) as e:
                        m.restore_shared_container(claim,'mysql',guard=guard,thaw_guard=guard)
                    assert e.value.code=='FORMAL_SHARED_THAW_REQUIRED'
            assert not any(e['step']=='shared-mysql-restore-intent' for e in claim.events)
    finally:
        for p in reversed(patchers):p.stop()


def test_raw_requires_all_durable_stops_and_recovery_blocks_capture(tmp_path):
    m,claim,mysql,source,rows,guard,guards,patchers=fixture(tmp_path)
    try:
        prepare_sql(m,claim,mysql,source,rows,guard)
        stop(rows)
        fence={'mount':str(tmp_path),'binding':{'operationId':'owned-split'},'seconds':5}
        with pytest.raises(m.snapshot.FormalSnapshotError) as e:
            m.capture_raw_phase(claim,source,filesystem_fence=fence,guard=guard,native_guard=guard)
        assert e.value.code=='FORMAL_SHARED_RUNTIME_CHANGED'
        assert not any(e['step']=='split-raw-intent' for e in claim.events)
    finally:
        for p in reversed(patchers):p.stop()
