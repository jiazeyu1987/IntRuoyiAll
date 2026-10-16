"""Private real files with explicit SSH/Docker doubles; never live server proof."""
import copy
import hashlib
import importlib
import json
from pathlib import Path
import sys

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_snapshot_linux as snapshot

ROLES = ('test-backend', 'test-rehearsal', 'backup-backend', 'backup-rehearsal')


def fixture(tmp_path):
    m = importlib.import_module('formal_external_callers')
    class Claim:
        def __init__(self):
            self.root = tmp_path / 'claim'; self.root.mkdir(mode=0o700)
            self.events = []
            self._plan = snapshot._canonical_bytes({'operationId':'owned-external-health'})
        def assert_owned(self): pass
        def _write(self, path, raw):
            with path.open('xb') as out: out.write(raw)
            path.chmod(0o600)
        def record(self, step, details):
            assert step in snapshot.FormalMaintenanceClaim._steps
            assert not any(e['step'] == step for e in self.events)
            self.events.append({'step': step, 'details': copy.deepcopy(details)})
    class Transport:
        def __init__(self): self.rows = {}; self.actions = []; self.inspections = 0
        def inspect(self, connection, identity):
            self.inspections += 1
            return copy.deepcopy(self.rows[identity])
        def action(self, connection, before, action):
            identity = before['id']; assert self.rows[identity] == before
            self.actions.append((connection['host'], identity, action))
            state = self.rows[identity]['state']
            if action == 'stop': state.update(Running=False, FinishedAt='stopped')
            else: state.update(Running=True, StartedAt='restarted')
    plan = {}; transport = Transport(); claim = Claim()
    for i, role in enumerate(ROLES, 1):
        identity = str(i) * 64
        connection = {'host': '172.30.30.' + ('58' if i <= 2 else '59'), 'port': 22,
            'user': 'root', 'bootId': '0fa6c22b-5a90-11f1-ade1-0242ac150002',
            'knownHostsFile': '/owned/known_hosts', 'knownHostsSha256': 'a' * 64,
            'dockerEndpoint': 'unix:///var/run/docker.sock'}
        plan[role] = {'connection': connection, 'containerId': identity}
        transport.rows[identity] = {'id': identity,
            'name': '/intruoyi-' + ('backend' if i % 2 else 'rehearsal-backend'),
            'image': 'sha256:' + 'b' * 64, 'mounts': [],
            'restartPolicy': {'Name': 'unless-stopped', 'MaximumRetryCount': 0},
            'state': {'Running': i != 2, 'Paused': False, 'Restarting': False,
                'Dead': False, 'OOMKilled': False, 'ExitCode': 0,
                'StartedAt': 'original-start', 'FinishedAt': 'zero'}}
    return m, claim, plan, transport


@pytest.mark.parametrize('stage',['binding-journal','final-original-health'])
def test_external_health_success_refuses_final_work_past_total_deadline(tmp_path,monkeypatch,stage):
    m,claim,plan,transport=fixture(tmp_path);clock=[0.]
    monkeypatch.setattr(m.time,'monotonic',lambda:clock[0])
    m.bind_external_callers(claim,plan,transport)
    transport.observe_http=lambda *a:{'httpStatus':200,'bytes':15,'sha256':'a'*64,
        'port':48081,'networkNamespaceInode':12,'pid':31}
    if stage=='binding-journal':
        record=claim.record
        def slow(step,details):
            record(step,details)
            if step=='external-health-scope':clock[0]=31.
        claim.record=slow
        with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):
            m.bind_external_health_scope(claim,transport,guard=claim.assert_owned,seconds=30)
        assert any(e['step']=='external-health-scope' for e in claim.events)
    else:
        m.bind_external_health_scope(claim,transport,guard=claim.assert_owned,seconds=30)
        for role in ROLES:m.stop_external_caller(claim,role,transport,guard=claim.assert_owned)
        m.restore_external_services_with_health(claim,transport,dependency_guard=claim.assert_owned,seconds=30)
        original=m._original_health;count=[0]
        def slow(current):
            value=original(current);count[0]+=1
            if count[0]==2:clock[0]=31.
            return value
        monkeypatch.setattr(m,'_original_health',slow)
        before=list(claim.events);actions=list(transport.actions)
        with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_DATA_DEADLINE'):
            m.observe_external_restoration_health(claim,transport,dependency_guard=claim.assert_owned,seconds=30)
        assert claim.events==before and transport.actions==actions


@pytest.mark.parametrize('case',['healthy','unhealthy-first','tampered-original','bind-after-stop','changed-runtime','no-proof'])
def test_external_original_http_health_is_sequential_and_retained(tmp_path,case):
    m,claim,plan,transport=fixture(tmp_path)
    m.bind_external_callers(claim,plan,transport)
    seen=[]
    proof={'httpStatus':200,'bytes':15,'sha256':hashlib.sha256(b'{"status":"UP"}').hexdigest(),
        'port':48081,'networkNamespaceInode':71,'pid':172}
    def observe(connection,record,role,seconds):
        assert 0<seconds<=30 and record==transport.rows[record['id']]
        seen.append((role,len(transport.actions)))
        return dict(proof)
    transport.observe_http=observe
    guard=lambda:None
    if case=='no-proof':
        with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_CALLER_HEALTH_REQUIRED'):
            m.restore_external_services_with_health(claim,transport,dependency_guard=guard,seconds=30)
        assert not transport.actions;return
    if case=='bind-after-stop':
        m.stop_external_caller(claim,ROLES[0],transport,guard=claim.assert_owned)
        with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_CALLER_HEALTH_STAGE_INVALID'):
            m.bind_external_health_scope(claim,transport,guard=guard,seconds=30)
        return
    original=m.bind_external_health_scope(claim,transport,guard=guard,seconds=30)
    assert original['health'][ROLES[1]]=={'state':'ORIGINALLY_STOPPED','healthObserved':False}
    for role in ROLES:m.stop_external_caller(claim,role,transport,guard=claim.assert_owned)
    seen.clear();before=len(transport.actions)
    if case=='tampered-original':
        (claim.root/'external-callers-health-original.json').write_bytes(b'{}')
        with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_CALLER_HEALTH_SCOPE_CHANGED'):
            m.restore_external_services_with_health(claim,transport,dependency_guard=guard,seconds=30)
        assert len(transport.actions)==before;return
    if case=='changed-runtime':
        old=observe
        def drift(connection,record,role,seconds):
            answer=old(connection,record,role,seconds)
            transport.rows[record['id']]['state']['StartedAt']='unowned-change'
            return answer
        transport.observe_http=drift
    elif case=='unhealthy-first':
        transport.observe_http=lambda *a,**k: (_ for _ in ()).throw(snapshot.FormalSnapshotError('FORMAL_HEALTH_RESPONSE_INVALID'))
    if case in ('unhealthy-first','changed-runtime'):
        with pytest.raises(snapshot.FormalSnapshotError):
            m.restore_external_services_with_health(claim,transport,dependency_guard=guard,seconds=30)
        assert len(transport.actions)==before+1
        assert not any(e['step']==m._names(ROLES[2],'start')[0] for e in claim.events)
    else:
        result=m.restore_external_services_with_health(claim,transport,dependency_guard=guard,seconds=30)
        assert result['state']=='EXTERNAL_CALLERS_HEALTHY_INGRESS_HELD' and result['productionEvidence'] is False
        assert [x[0] for x in seen[:3]]==[ROLES[0],ROLES[2],ROLES[3]]
        assert seen[0][1]==before+1 and seen[1][1]==before+2
        retained=snapshot._canonical_bytes(claim.events)
        observed=m.observe_external_restoration_health(claim,transport,dependency_guard=guard,seconds=30)
        assert observed['health']==result['health'] and snapshot._canonical_bytes(claim.events)==retained
    assert not any(e['step'] in ('released','ingress-release-intent') for e in claim.events)


@pytest.mark.parametrize('case',['length','chunked','not-ready','duplicate-length','truncated','bad-chunk','trailing','encoded','down'])
def test_external_http_wire_framing_is_strict(tmp_path,case):
    m,_,_,_=fixture(tmp_path)
    body=b'{"status":"UP"}'
    wire=b'HTTP/1.1 200 OK\r\nContent-Length: 15\r\n\r\n'+body
    if case=='chunked':wire=b'HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\nf\r\n'+body+b'\r\n0\r\n\r\n'
    if case=='not-ready':wire=b'HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\n\r\n'
    if case=='duplicate-length':wire=wire.replace(b'Content-Length: 15',b'Content-Length: 15\r\nContent-Length: 15')
    if case=='truncated':wire=wire[:-1]
    if case=='bad-chunk':wire=b'HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\nz\r\n'+body
    if case=='trailing':wire+=b'EXTRA'
    if case=='encoded':wire=wire.replace(b'Content-Length: 15',b'Content-Length: 15\r\nContent-Encoding: gzip')
    if case=='down':wire=wire.replace(body,b'{"status":"NO"}')
    if case in ('length','chunked'):
        assert m._parse_http_wire(wire)=={'httpStatus':200,'bytes':15,'sha256':hashlib.sha256(body).hexdigest()}
    elif case=='not-ready':assert m._parse_http_wire(wire) is None
    else:
        with pytest.raises(snapshot.FormalSnapshotError):m._parse_http_wire(wire)


@pytest.mark.parametrize('case',['ready','not-ready','bad-frame','bad-meta','runtime-change'])
def test_ssh_health_uses_bound_remote_namespace_only(tmp_path,monkeypatch,case):
    from types import SimpleNamespace
    m,_,plan,rows=fixture(tmp_path);record=rows.rows['1'*64];connection=copy.deepcopy(plan[ROLES[0]]['connection'])
    public=b'owned-public-host-key';connection['knownHostsSha256']=hashlib.sha256(public).hexdigest()
    monkeypatch.setattr(m,'_known_hosts_bytes',lambda _:public)
    calls=[]
    def execute(args,**kwargs):
        body=kwargs['input'].decode();calls.append(body)
        assert kwargs['timeout']<=8 and 'ClearAllForwardings=yes' in args
        if '/usr/bin/nsenter' in body:
            assert '/proc/$$/fd/' in body and '127.0.0.1:48081/actuator/health' in body
            assert '/usr/bin/curl --disable' in body and '--noproxy' in body and '--raw' in body
            wire=b'HTTP/1.1 200 OK\r\nContent-Length: 15\r\n\r\n{"status":"UP"}'
            if case=='bad-frame':wire=wire[:-1]
            prefix=b'777 888\n' if case!='bad-meta' else b'0 0\n'
            raw=prefix+(wire if case!='not-ready' else b'')+b'\nFORMAL_CURL_EXIT='+ (b'7' if case=='not-ready' else b'0')+b'\n'
            return SimpleNamespace(returncode=0,stdout=raw,stderr=b'')
        observed=copy.deepcopy(record)
        if case=='runtime-change':observed['state']['StartedAt']='changed'
        return SimpleNamespace(returncode=0,stdout=json.dumps(observed).encode()+b'\n',stderr=b'')
    monkeypatch.setattr(m.subprocess,'run',execute)
    token=snapshot.observation_deadline.set(m.time.monotonic()+8)
    try:
        transport=m.SshDockerTransport()
        if case in ('bad-frame','bad-meta','runtime-change'):
            with pytest.raises(snapshot.FormalSnapshotError):transport.observe_http(connection,record,ROLES[0],8)
        else:
            result=transport.observe_http(connection,record,ROLES[0],8)
            if case=='not-ready':assert result is None
            else:assert result['httpStatus']==200 and result['pid']==777 and result['networkNamespaceInode']==888
        if case=='runtime-change':assert not any('/usr/bin/nsenter' in body for body in calls)
    finally:snapshot.observation_deadline.reset(token)


def test_ssh_runtime_digest_sorts_mounts_on_the_bound_peer(tmp_path,monkeypatch):
    from types import SimpleNamespace
    m,_,plan,rows=fixture(tmp_path);connection=copy.deepcopy(plan[ROLES[0]]['connection'])
    public=b'owned-public';connection['knownHostsSha256']=hashlib.sha256(public).hexdigest()
    monkeypatch.setattr(m,'_known_hosts_bytes',lambda _:public)
    record=rows.rows['1'*64]
    def run(args,**kwargs):
        body=kwargs['input'].decode()
        assert 'normalized_inspect()' in body and '/usr/bin/sort' in body and '/usr/bin/paste' in body
        assert 'LC_ALL=C' in body and '{{range .Mounts}}' in body
        return SimpleNamespace(returncode=0,stderr=b'',stdout=json.dumps(record).encode()+b'\n')
    monkeypatch.setattr(m.subprocess,'run',run)
    assert m.SshDockerTransport().inspect(connection,record['id'])==record


def test_four_callers_stop_and_restore_original_state(tmp_path):
    m, claim, plan, transport = fixture(tmp_path)
    originals = copy.deepcopy(transport.rows)
    m.bind_external_callers(claim, plan, transport)
    for role in ROLES: m.stop_external_caller(claim, role, transport,guard=claim.assert_owned)
    proof = m.guard_external_callers(claim, transport)
    assert proof['state'] == 'KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION'
    assert proof['productionEvidence'] is False
    health = []
    guard = lambda: health.append('actual-health-contract-double')
    m.begin_external_recovery(claim, transport, dependency_guard=guard)
    for role in ROLES: m.restore_external_caller(claim, role, transport, dependency_guard=guard)
    result = m.verify_external_restoration(claim, transport, dependency_guard=guard)
    assert result['state'] == 'EXTERNAL_CALLERS_RESTORED_PENDING_INGRESS_PROOF'
    assert health
    assert [a[2] for a in transport.actions] == ['stop'] * 3 + ['start'] * 3
    for identity, before in originals.items():
        assert transport.rows[identity]['state']['Running'] == before['state']['Running']
        assert snapshot._container_stable(transport.rows[identity]) == snapshot._container_stable(before)


def test_partial_stop_recovery_leaves_untouched_callers_running(tmp_path):
    m, claim, plan, transport = fixture(tmp_path)
    originals = copy.deepcopy(transport.rows)
    m.bind_external_callers(claim, plan, transport)
    m.stop_external_caller(claim, ROLES[0], transport,guard=claim.assert_owned)
    m.begin_external_recovery(claim, transport, dependency_guard=lambda: None)
    for role in ROLES: m.restore_external_caller(claim, role, transport, dependency_guard=lambda: None)
    assert len(transport.actions) == 2
    for identity, before in originals.items():
        assert transport.rows[identity]['state']['Running'] == before['state']['Running']


def test_disconnect_before_action_stays_unresolved_and_never_replays(tmp_path):
    m, claim, plan, transport = fixture(tmp_path)
    m.bind_external_callers(claim, plan, transport)
    calls = []
    def interrupted(connection, before, action):
        calls.append(action)
        raise snapshot.FormalSnapshotError('FORMAL_CALLER_SSH_UNCERTAIN')
    transport.action = interrupted
    with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_CALLER_SSH_UNCERTAIN'):
        m.stop_external_caller(claim, ROLES[0], transport,guard=claim.assert_owned)
    with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_CALLER_ACTION_REPLAY_REFUSED'):
        m.stop_external_caller(claim, ROLES[0], transport,guard=claim.assert_owned)
    with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_CALLER_RECONCILIATION_REQUIRED'):
        m.reconcile_external_caller(claim, ROLES[0], 'stop', transport,guard=claim.assert_owned)
    with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_CALLER_PENDING_ACTION_REQUIRED'):
        m.begin_external_recovery(claim, transport, dependency_guard=lambda: None)
    assert calls == ['stop']


@pytest.mark.parametrize('action', ['stop', 'start'])
def test_uncertain_action_reconciles_without_replay_or_next_mutation(tmp_path, action):
    m, claim, plan, transport = fixture(tmp_path)
    m.bind_external_callers(claim, plan, transport)
    health = lambda: None
    if action == 'start':
        for role in ROLES: m.stop_external_caller(claim, role, transport,guard=claim.assert_owned)
        m.begin_external_recovery(claim, transport, dependency_guard=health)
    original = claim.record
    complete = 'external-test-backend-' + ('stopped' if action == 'stop' else 'restored')
    def interrupted(step, details):
        if step == complete: raise RuntimeError('owned-connection-lost')
        return original(step, details)
    claim.record = interrupted
    run = m.stop_external_caller if action == 'stop' else m.restore_external_caller
    kwargs = {'guard':claim.assert_owned} if action == 'stop' else {'dependency_guard': health}
    with pytest.raises(RuntimeError, match='owned-connection-lost'):
        run(claim, ROLES[0], transport, **kwargs)
    saved = list(transport.actions); claim.record = original
    # Reopen only retained journal data; no in-memory pending action shortcut.
    claim.events = json.loads(json.dumps(claim.events))
    with pytest.raises(snapshot.FormalSnapshotError): run(claim, ROLES[0], transport, **kwargs)
    with pytest.raises(snapshot.FormalSnapshotError): run(claim, ROLES[1], transport, **kwargs)
    assert transport.actions == saved
    m.reconcile_external_caller(claim, ROLES[0], action, transport, **kwargs)
    assert transport.actions == saved
    run(claim, ROLES[1], transport, **kwargs)


@pytest.mark.parametrize('case', ['image', 'mount', 'restart', 'runtime', 'scope', 'order', 'health'])
def test_drift_order_and_health_fail_before_next_mutation(tmp_path, case):
    m, claim, plan, transport = fixture(tmp_path)
    m.bind_external_callers(claim, plan, transport)
    if case == 'order':
        with pytest.raises(snapshot.FormalSnapshotError): m.stop_external_caller(claim, ROLES[1], transport,guard=claim.assert_owned)
        assert not transport.actions; return
    for role in ROLES: m.stop_external_caller(claim, role, transport,guard=claim.assert_owned)
    saved = list(transport.actions)
    row = transport.rows['1' * 64]
    if case == 'image': row['image'] = 'sha256:' + 'c' * 64
    if case == 'mount': row['mounts'] = [{'Source': '/changed'}]
    if case == 'restart': row['restartPolicy']['Name'] = 'always'
    if case == 'runtime': row['state'].update(Running=True, StartedAt='unexpected')
    if case == 'scope':
        path = claim.root / 'external-callers-original.json'
        path.write_bytes(path.read_bytes().replace(b'172.30.30.58', b'172.30.30.59'))
    if case == 'health':
        def broken(): raise snapshot.FormalSnapshotError('FORMAL_DEPENDENCY_UNHEALTHY')
        with pytest.raises(snapshot.FormalSnapshotError, match='FORMAL_DEPENDENCY_UNHEALTHY'):
            m.begin_external_recovery(claim, transport, dependency_guard=broken)
        assert not any(e['step'] == 'external-recovery-intent' for e in claim.events)
    else:
        with pytest.raises(snapshot.FormalSnapshotError): m.guard_external_callers(claim, transport)
    assert transport.actions == saved


@pytest.mark.parametrize('field,value', [('host','172.30.30.57'), ('port',2222), ('user','other'),
    ('dockerEndpoint','tcp://127.0.0.1:2375'), ('knownHostsSha256','invalid'), ('bootId','invalid')])
def test_bad_connection_scope_refused_before_observation(tmp_path, field, value):
    m, claim, plan, transport = fixture(tmp_path)
    plan[ROLES[0]]['connection'][field] = value
    with pytest.raises(snapshot.FormalSnapshotError): m.bind_external_callers(claim, plan, transport)
    assert not claim.events and not transport.inspections and not transport.actions


@pytest.mark.parametrize('case', ['pass', 'host-key-drift', 'timeout', 'action-timeout', 'boot-drift', 'uncached-action'])
def test_bound_ssh_transport_never_uses_ambient_docker_or_retries(tmp_path, monkeypatch, case):
    import subprocess
    from types import SimpleNamespace
    m, _, plan, fixture_transport = fixture(tmp_path)
    connection = plan[ROLES[0]]['connection']; record = fixture_transport.rows['1' * 64]
    public = b'owned public host key fixture\n'
    connection['knownHostsSha256'] = hashlib.sha256(public).hexdigest()
    monkeypatch.setattr(m, '_known_hosts_bytes', lambda selected: public)
    calls = []
    def remote(args, **kwargs):
        calls.append((args, kwargs)); body = kwargs['input'].decode()
        assert '172.30.30.58' in body and connection['bootId'] in body
        assert 'unix:///var/run/docker.sock' in body
        assert '.Config.Env' not in body and 'private-password' not in body
        assert 'StrictHostKeyChecking=yes' in args and 'GlobalKnownHostsFile=/dev/null' in args
        assert 'UserKnownHostsFile=/owned/known_hosts' in args
        if case == 'timeout': raise subprocess.TimeoutExpired(args, 1)
        if case == 'boot-drift': return SimpleNamespace(returncode=61, stdout=b'', stderr=b'')
        if 'stop --timeout' in body:
            assert 'sha256sum' in body and 'FORMAL_CALLER' not in body
            assert 'stop --timeout -1 --signal SIGTERM '+record['id'] in body
            assert 'stop --time 60' not in body
            assert 0 < kwargs['timeout'] <= 110
            if case == 'action-timeout': raise subprocess.TimeoutExpired(args, 1)
            return SimpleNamespace(returncode=0, stdout=(record['id'] + '\n').encode(), stderr=b'')
        return SimpleNamespace(returncode=0, stdout=json.dumps(record).encode() + b'\n', stderr=b'')
    monkeypatch.setattr(m.subprocess, 'run', remote)
    transport = m.SshDockerTransport()
    if case == 'uncached-action':
        with pytest.raises(snapshot.FormalSnapshotError): transport.action(connection, record, 'stop')
        assert not calls; return
    if case in ('timeout', 'boot-drift'):
        with pytest.raises(snapshot.FormalSnapshotError): transport.inspect(connection, record['id'])
        assert len(calls) == 1; return
    assert transport.inspect(connection, record['id']) == record
    if case == 'host-key-drift':
        public += b'changed'
        with pytest.raises(snapshot.FormalSnapshotError): transport.action(connection, record, 'stop')
        assert len(calls) == 1
    elif case == 'action-timeout':
        with pytest.raises(snapshot.FormalSnapshotError,match='FORMAL_CALLER_SSH_UNCERTAIN'):
            transport.action(connection, record, 'stop')
        assert len(calls) == 2
    else:
        transport.action(connection, record, 'stop')
        assert len(calls) == 2
