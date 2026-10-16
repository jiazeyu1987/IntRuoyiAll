"""Owned two-engine Docker actions and Linux journal; SSH is not exercised."""
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time

sys.path.insert(0, '/work/IntRuoyiBackend/script/backup-ops/linux')
import formal_snapshot_linux as snapshot
import formal_external_callers as callers

prefix = os.environ['OWNED_CALLER_PREFIX']
SOCKETS = {'172.30.30.58': '/engines/test/docker.sock', '172.30.30.59': '/engines/backup/docker.sock'}
OBSERVERS={}


def docker(host, *args):
    result = subprocess.run(['/usr/local/bin/docker', '--host', 'unix://' + SOCKETS[host], *args],
        capture_output=True, timeout=90)
    if result.returncode: raise RuntimeError('OWNED_CALLER_DOCKER_FAILED')
    return result.stdout


class OwnedDockerTransport:
    """Only transport routing is replaced; every container action is real."""
    def __init__(self): self.actions = []
    def inspect(self, connection, identity):
        host = connection['host']
        expected_name = prefix + ('-test' if host.endswith('.58') else '-backup')
        assert docker(host, 'info', '--format', '{{.Name}}').decode().strip() == expected_name
        return json.loads(docker(host, 'inspect', '--format', snapshot.CONTAINER_INSPECT_TEMPLATE, identity))
    def action(self, connection, before, action):
        assert self.inspect(connection, before['id']) == before
        self.actions.append([connection['host'], before['id'], action])
        args = ['stop', '--time', '10', before['id']] if action == 'stop' else ['start', before['id']]
        assert docker(connection['host'], *args).decode().strip() == before['id']
    def observe_http(self,connection,record,role,seconds):
        script=('import json,sys;sys.path.insert(0,"/work/IntRuoyiBackend/script/backup-ops/linux");'
            'import formal_service_health as h;record=json.loads(sys.argv[1]);'
            'proof=h.NativeNetworkProbe().observe("backend",record,float(sys.argv[2]));'
            'print(json.dumps(None if proof is None else {**proof,"port":48081}))')
        result=subprocess.run(['/usr/local/bin/docker','--host','unix://'+SOCKETS[connection['host']],'exec',OBSERVERS[connection['host']],
            'python3','-I','-c',script,json.dumps(record),str(seconds)],capture_output=True,timeout=seconds+3)
        if result.returncode or result.stderr:raise snapshot.FormalSnapshotError('FORMAL_CALLER_OWNED_HEALTH_FAILED')
        assert len(result.stdout)<=16384
        return json.loads(result.stdout)


plan = {}
for host in SOCKETS:
    expected_name = prefix + ('-test' if host.endswith('.58') else '-backup')
    deadline = time.monotonic() + 45
    while True:
        result = subprocess.run(['/usr/local/bin/docker', '--host', 'unix://' + SOCKETS[host],
            'info', '--format', '{{.Name}}'], capture_output=True, timeout=5)
        if result.returncode == 0:
            assert result.stdout.decode().strip() == expected_name; break
        if time.monotonic() > deadline: raise RuntimeError('OWNED_CALLER_ENGINE_NOT_READY')
        time.sleep(1)
    docker(host, 'load', '-i', '/fixtures/images.tar')
    observer='owned-health-observer'
    OBSERVERS[host]=docker(host,'run','-d','--name',observer,'--label','task-owner='+prefix,
        '--privileged','--pid','host','--network','none',
        '--mount','type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock',
        '--mount','type=bind,src=/work,dst=/work,readonly',
        '--mount','type=bind,src=/fixtures/docker,dst=/usr/local/bin/docker,readonly',
        '--entrypoint','python3','intruoyi-owned-nft-fixture:20261003','-I','-c','import time;time.sleep(600)').decode().strip()
    for name in ('intruoyi-backend', 'intruoyi-rehearsal-backend'):
        identity = docker(host, 'run', '-d', '--network', 'none', '--name', name,
            '--restart', 'unless-stopped', '--label', 'task-owner=' + prefix,
            '--mount','type=bind,src=/fixtures/health-nginx.conf,dst=/etc/nginx/conf.d/default.conf,readonly',
            'nginx:1.27-alpine').decode().strip()
        role = ('test' if host.endswith('.58') else 'backup') + ('-backend' if name == 'intruoyi-backend' else '-rehearsal')
        plan[role] = {'containerId': identity, 'connection': {'host': host, 'port': 22, 'user': 'root',
            'bootId': Path('/proc/sys/kernel/random/boot_id').read_text().strip(),
            'knownHostsFile': '/owned/known_hosts', 'knownHostsSha256': 'a' * 64,
            'dockerEndpoint': 'unix:///var/run/docker.sock'}}
docker('172.30.30.58', 'stop', '--time', '10', plan['test-rehearsal']['containerId'])
transport = OwnedDockerTransport()
claim_binding = {'operationId': 'owned-four-callers', 'sourceFingerprint': 'a' * 64,
    'databaseUuid': '0fa6c22b-5a90-11f1-ade1-0242ac150002', 'minioContainerId': 'b' * 64}
claim = snapshot.FormalMaintenanceClaim('/owned/claim', claim_binding)
originals = callers.bind_external_callers(claim, plan, transport)


def reopen():
    global claim
    claim.close()
    claim = snapshot.FormalMaintenanceClaim('/owned/claim', claim_binding, resume=True)


def interrupt_completion(step):
    real = claim.record
    def record(selected, details):
        if selected == step: raise RuntimeError('OWNED_CALLER_INTERRUPTED_AFTER_ACTION')
        return real(selected, details)
    claim.record = record


interrupt_completion('external-test-backend-stopped')
try: callers.stop_external_caller(claim, 'test-backend', transport,guard=claim.assert_owned); raise AssertionError('interruption absent')
except RuntimeError as error: assert str(error) == 'OWNED_CALLER_INTERRUPTED_AFTER_ACTION'
reopen(); saved = list(transport.actions)
try: callers.stop_external_caller(claim, 'test-backend', transport,guard=claim.assert_owned); raise AssertionError('replayed stop')
except snapshot.FormalSnapshotError as error: assert error.code == 'FORMAL_CALLER_ACTION_REPLAY_REFUSED'
callers.reconcile_external_caller(claim, 'test-backend', 'stop', transport,guard=claim.assert_owned)
assert transport.actions == saved
for role in callers.ROLES[1:]: callers.stop_external_caller(claim, role, transport,guard=claim.assert_owned)
stopped = callers.guard_external_callers(claim, transport)
assert stopped['productionEvidence'] is False

# This fixture has no application dependency-health claim. The explicitly
# supplied dependency contract double cannot count as real app acceptance.
def dependency_contract_double(): claim.assert_owned()
callers.begin_external_recovery(claim, transport, dependency_guard=dependency_contract_double)
interrupt_completion('external-test-backend-restored')
try:
    callers.restore_external_caller(claim, 'test-backend', transport, dependency_guard=dependency_contract_double)
    raise AssertionError('interruption absent')
except RuntimeError as error: assert str(error) == 'OWNED_CALLER_INTERRUPTED_AFTER_ACTION'
reopen(); saved = list(transport.actions)
try:
    callers.restore_external_caller(claim, 'test-backend', transport, dependency_guard=dependency_contract_double)
    raise AssertionError('replayed start')
except snapshot.FormalSnapshotError as error: assert error.code == 'FORMAL_CALLER_ACTION_REPLAY_REFUSED'
callers.reconcile_external_caller(claim, 'test-backend', 'start', transport, dependency_guard=dependency_contract_double,guard=claim.assert_owned)
assert transport.actions == saved
for role in callers.ROLES[1:]:
    callers.restore_external_caller(claim, role, transport, dependency_guard=dependency_contract_double)
restored = callers.verify_external_restoration(claim, transport, dependency_guard=dependency_contract_double)
assert [a[2] for a in transport.actions] == ['stop'] * 3 + ['start'] * 3
health_claim=snapshot.FormalMaintenanceClaim('/owned/external-health-claim',json.loads(claim._plan))
health_originals=callers.bind_external_callers(health_claim,plan,transport)
def health_dependency_contract_double():health_claim.assert_owned()
callers.bind_external_health_scope(health_claim,transport,guard=health_dependency_contract_double,seconds=120)
for role in callers.ROLES:callers.stop_external_caller(health_claim,role,transport,guard=health_claim.assert_owned)
health_restored=callers.restore_external_services_with_health(health_claim,transport,
    dependency_guard=health_dependency_contract_double,seconds=120)
assert health_restored['state']=='EXTERNAL_CALLERS_HEALTHY_INGRESS_HELD'
assert health_restored['health']['test-rehearsal']['state']=='ORIGINALLY_STOPPED'
assert not any(e['step'] in ('released','ingress-release-intent') for e in health_claim.events)
health_files={p.name:base64.b64encode(p.read_bytes()).decode('ascii') for p in health_claim.root.iterdir()
    if p.name in ('binding.json','external-callers-original.json','external-callers-health-original.json') or p.name.startswith('event-')}
health_claim.close()
for role, before in originals.items():
    after = transport.inspect(plan[role]['connection'], before['id'])
    assert snapshot._container_stable(after) == snapshot._container_stable(before)
    assert after['state']['Running'] == before['state']['Running']
for host in SOCKETS:
    assert set(docker(host, 'ps', '-aq', '--no-trunc').decode().split()) == {
        selected['containerId'] for selected in plan.values() if selected['connection']['host'] == host} | {OBSERVERS[host]}
files = {p.name: base64.b64encode(p.read_bytes()).decode('ascii') for p in claim.root.iterdir()
         if p.name in ('binding.json', 'external-callers-original.json') or p.name.startswith('event-')}
claim.close()
files.update({'health-'+name:raw for name,raw in health_files.items()})
print(json.dumps({'state': 'OWNED_FOUR_CALLER_JOURNAL_ACTIONS_PASS', 'productionEvidence': False,
    'actualDockerEngines': 2, 'actualDockerCallers': 4, 'originalStoppedPreserved': True,
    'interruptedStopStartReconciledWithoutReplay': True, 'actions': transport.actions,
    'sshTransportActuallyVerified': False, 'dependencyHealthActuallyVerified': False,
    'actualProtocolHealthAfterRestart':True,'sequentialHealthHelperActuallyVerified':True,
    'hostPidNamespacesIndependentlyBound':True,'realApplicationAcceptance':False,
    'fullWriterExclusionVerified': False, 'retained': files}))
