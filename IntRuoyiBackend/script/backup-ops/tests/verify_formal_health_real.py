"""Owned Docker protocol verification; no live/business acceptance claim."""
import json
import hashlib
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import tempfile
import time
import uuid

ROOT=Path(__file__).parents[1]
NATIVE_IMAGE='sha256:070da5e6fd4a490e009ce13c042339c673724b577520f624a1bc0c4b85a7b10d'


def docker(args,timeout=40):
    r=subprocess.run(['docker',*args],capture_output=True,timeout=timeout)
    if r.returncode:raise RuntimeError('OWNED_HEALTH_DOCKER_FAILED '+str(r.returncode)+' '+r.stderr.decode()[-4096:])
    return r.stdout


def inside():
    sys.path.insert(0,'/code/linux')
    import formal_snapshot_linux as snapshot
    import formal_split_capture as split
    import formal_service_health as health
    request=json.loads(Path('/fixtures/request.json').read_text())
    identities=request['identities']
    for role in ('backend','frontend','showroom','onlyoffice'):
        row=snapshot.inspect_formal_container(identities[role])
        if not row['state']['Running']:
            logs=subprocess.run(['docker','logs',identities[role]],capture_output=True,timeout=3)
            raise RuntimeError('OWNED_HTTP_FIXTURE_STOPPED '+role+' '+logs.stderr.decode()[-2048:])
    secret=Path('/fixtures/mysql-password').read_text().strip()
    mysql=snapshot.PrivateDockerMySql(identities['mysql'],secret,timeout=5)
    # Only the final mysqld, not image initialization's temporary server, counts.
    end=time.monotonic()+90
    while True:
        r=subprocess.run(['docker','exec',identities['mysql'],'cat','/proc/1/comm'],capture_output=True,timeout=3)
        if r.returncode==0 and r.stdout==b'mysqld\n' and mysql.ready():
            db_uuid=mysql.query('SELECT @@server_uuid;').strip();break
        assert time.monotonic()<end,'owned MySQL final process not ready'
        time.sleep(.2)
    binding={'operationId':request['prefix'],'sourceFingerprint':'a'*64,'databaseUuid':db_uuid,
             'minioContainerId':identities['minio']}
    claim=snapshot.FormalMaintenanceClaim('/tmp/health-claim',binding)
    split.bind_shared_container_scope(claim,identities)
    probe=health.NativeNetworkProbe()
    report=health.bind_shared_health_scope(claim,mysql,guard=claim.assert_owned,
        redis_authentication='none',probe=probe,timeout=90)
    assert set(report['health'])==split.ROLES
    assert all(row['state']=='HEALTHY' for row in report['health'].values())
    assert report['health']['redis']['proof']['response']=='PONG'
    assert report['health']['mysql']['proof']['flags']['readOnly']==0
    assert report['health']['mysql']['proof']['flags']['superReadOnly']==0
    # Ingress remains closed: no host ports were published and namespace probe
    # targets loopback directly. Confirm exact original runtime before/after.
    for role,identity in identities.items():
        original,current,_=split._shared_role(claim,role);assert original==current
    claim.close();claim=snapshot.FormalMaintenanceClaim('/tmp/health-claim',binding,resume=True)
    assert snapshot._canonical_bytes(report)==claim._read(claim.root/'shared-health-original.json')
    claim.close()
    # The HTTP fixtures expose deliberate invalid/slow endpoints. Use the same
    # real worker transport; this does not represent a real backend application.
    record=snapshot.inspect_formal_container(identities['backend'])
    saved=health.HTTP_ROLES['backend']
    checks=[]
    try:
        for path,code in [('/redirect','FORMAL_HEALTH_RESPONSE_INVALID'),
            ('/duplicate','FORMAL_HEALTH_RESPONSE_INVALID'),('/large','FORMAL_HEALTH_PROBE_FAILED'),
            ('/truncated','FORMAL_HEALTH_PROBE_FAILED')]:
            health.HTTP_ROLES['backend']=(48081,path)
            try:probe.observe('backend',record,3)
            except snapshot.FormalSnapshotError as error:assert error.code==code,(path,error.code,code)
            else:raise AssertionError('invalid endpoint accepted')
            checks.append(path)
        health.HTTP_ROLES['backend']=(48081,'/pending')
        assert probe.observe('backend',record,3) is None
        checks.append('/pending')
        health.HTTP_ROLES['backend']=(48081,'/slow')
        started=time.monotonic()
        try:probe.observe('backend',record,.5)
        except snapshot.FormalSnapshotError:pass
        else:raise AssertionError('slow response escaped deadline')
        assert time.monotonic()-started<2
        checks.append('/slow')
    finally:health.HTTP_ROLES['backend']=saved
    recovery=recovery_and_release(snapshot,split,health,identities,binding,mysql,probe)
    print(json.dumps({'state':'PASS','mysqlActual':True,'redisActual':True,'minioActual':True,
        'httpProtocolFixtures':['backend','frontend','showroom','onlyoffice'],
        'namespacePinned':True,'originalRuntimeUnchanged':True,'privateClaimReopen':True,
        'negativeRealPaths':checks,'healthScope':report,'fullRestorationVerified':False,
        'recoveryAndRelease':recovery,'realApplicationAcceptance':False,'productionEvidence':False}))


def recovery_and_release(snapshot,split,health,identities,binding,mysql,probe):
    import formal_ingress_control as ingress
    import threading
    # This case recovers an interrupted pre-capture stop; it does not freeze or
    # restart the database. One original stopped role must remain stopped.
    snapshot.run_formal_container_action('stop',identities['showroom'])
    binding={**binding,'operationId':binding['operationId']+'-recovery'}
    claim=snapshot.FormalMaintenanceClaim('/tmp/health-recovery-claim',binding)
    split.bind_shared_container_scope(claim,identities)
    original=health.bind_shared_health_scope(claim,mysql,guard=claim.assert_owned,
        redis_authentication='none',probe=probe,timeout=90)
    assert original['health']['showroom']['state']=='ORIGINALLY_STOPPED'
    listener=socket.socket();listener.bind(('127.0.0.1',18081));listener.listen()
    def accept():
        while True:
            connection,_=listener.accept();connection.close()
    threading.Thread(target=accept,daemon=True).start()
    def reachable():
        try:connection=socket.create_connection(('127.0.0.1',18081),timeout=.2)
        except (TimeoutError,ConnectionRefusedError):return False
        connection.close();return True
    assert reachable()
    native=ingress.NativeNft()
    plan={**native.identity(),'hostAddresses':['127.0.0.1','::1'],'hostPorts':[18081],
          'targets':[{'address':'127.0.0.1','ports':[18081]}]}
    ingress.install_ingress(claim,plan,guard=claim.assert_owned);assert not reachable()
    def guard():return ingress.assert_ingress(claim)
    def thaw_guard():
        # This owned fixture explicitly has no filesystem freeze operation.
        assert split._thaw_receipt(claim)=={'nativeState':'NOT_REQUESTED'}
    snapshot.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
    split.stop_shared_container(claim,'frontend',guard=guard)
    result=health.restore_shared_services_with_health(claim,mysql,guard=guard,
        thaw_guard=thaw_guard,probe=probe,timeout=120)
    assert result['health']['showroom']['state']=='ORIGINALLY_STOPPED'
    assert not reachable()
    # Observe through the actual standard CLI in a fresh process, using the
    # persisted claim. No orchestration mock or service action is substituted.
    credential=Path('/tmp/standard-health-password')
    credential.write_text(Path('/fixtures/mysql-password').read_text());credential.chmod(0o600)
    selected={'schemaVersion':'release-formal-capture-request/v2','action':'observe-shared-health',
        'claimRoot':str(claim.root),'claimBinding':binding,'mysqlContainerId':mysql.container,
        'mysqlCredentialFile':str(credential),'healthSeconds':90,
        'sharedScopeSha256':hashlib.sha256(claim._read(claim.root/'shared-container-original.json')).hexdigest(),
        'originalHealthSha256':hashlib.sha256(claim._read(claim.root/'shared-health-original.json')).hexdigest(),
        'ingressRulesSha256':guard()['rulesSha256']}
    request_path=Path('/tmp/standard-health-request.json')
    request_path.write_bytes(snapshot._canonical_bytes(selected));request_path.chmod(0o600)
    approved=hashlib.sha256(request_path.read_bytes()).hexdigest()
    journal_before=snapshot._canonical_bytes(claim.events);claim.close()
    bootstrap='import runpy,sys;sys.path.insert(0,"/code/linux");sys.argv=sys.argv[1:];runpy.run_path(sys.argv[0],run_name="__main__")'
    command=[sys.executable,'-I','-c',bootstrap,'/code/linux/backup_ops_linux.py','--mode','formal-capture',
             '--request',str(request_path),'--expected-request-sha256',approved]
    child=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=110,cwd='/tmp')
    if child.returncode or child.stderr:
        raise RuntimeError('OWNED_STANDARD_HEALTH_ENTRY_FAILED '+child.stdout.decode()+' '+child.stderr.decode()[-2048:])
    standard=json.loads(child.stdout)
    assert standard['state']=='ORIGINAL_SERVICES_HEALTHY_INGRESS_HELD'
    assert standard['serviceActionsPerformed'] is False and standard['claimReleased'] is False
    assert standard['hostWritersVerified'] is False and standard['productionEvidence'] is False
    claim=snapshot.FormalMaintenanceClaim('/tmp/health-recovery-claim',binding,resume=True)
    assert snapshot._canonical_bytes(claim.events)==journal_before
    assert not reachable() and standard['health']['health']['showroom']['state']=='ORIGINALLY_STOPPED'
    claim._write(claim.root/'owned-standard-health.json',snapshot._canonical_bytes(standard))
    def healthy():
        return health.observe_shared_recovery_health(claim,mysql,guard=guard,
            thaw_guard=thaw_guard,probe=probe,timeout=90)
    docker(['exec',identities['backend'],'python3','-I','-c',
        "from pathlib import Path;Path('/tmp/owned-unhealthy').write_text('explicit test fault')"])
    try:ingress.release_ingress(claim,healthy)
    except snapshot.FormalSnapshotError as error:assert error.code=='FORMAL_HEALTH_RESPONSE_INVALID',error.code
    else:raise AssertionError('unhealthy service allowed ingress release')
    guard();assert not reachable()
    assert not any(e['step']=='ingress-release-intent' for e in claim.events)
    docker(['exec',identities['backend'],'python3','-I','-c',
        "from pathlib import Path;Path('/tmp/owned-unhealthy').unlink()"])
    assert ingress.release_ingress(claim,healthy)['state']=='INGRESS_RELEASED'
    assert reachable()
    assert snapshot.inspect_formal_container(identities['showroom'])['state']['Running'] is False
    raw=snapshot._canonical_bytes(result)
    claim._write(claim.root/'owned-recovery-health.json',raw)
    claim.close();listener.close()
    return {'state':'PASS','partialStopRestored':True,'originalStoppedPreserved':True,
        'realMysqlFlagsRestored':True,'unhealthyHeldIngress':True,'healthyReleasedIngress':True,
        'fullRawCaptureAndRecovery':False,'health':result,'standardCliObservation':{
            'state':'PASS','receipt':standard,'journalUnchanged':True,'freshProcess':True}}


HTTP_SERVER=r'''
import http.server,json,os,signal,sys,threading,time
from pathlib import Path
signal.signal(signal.SIGTERM,lambda *args:sys.exit(0))
role=os.environ['OWNED_HTTP_ROLE']
class Handler(http.server.BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_GET(self):
  if self.path=='/redirect':
   self.send_response(302);self.send_header('Location','/actuator/health');self.end_headers();return
  if self.path=='/pending':self.send_response(503);self.end_headers();return
  if self.path=='/slow':time.sleep(2)
  if self.path=='/duplicate':body=b'{"status":"DOWN","status":"UP"}'
  elif self.path=='/large':body=b'x'*65537
  elif role=='backend':body=b'{"status":"DOWN"}' if Path('/tmp/owned-unhealthy').exists() else b'{"status":"UP"}'
  elif role=='onlyoffice':body=b'true'
  else:body=('<!DOCTYPE html><html><body>'+role+'</body></html>').encode()
  self.send_response(200);self.send_header('Content-Length',str(len(body)+10 if self.path=='/truncated' else len(body)))
  self.end_headers();self.wfile.write(body)
http.server.ThreadingHTTPServer(('127.0.0.1',48081 if role=='backend' else 80),Handler).serve_forever()
'''


def outside():
    prefix='owned-health-'+uuid.uuid4().hex[:10];containers=[]
    with tempfile.TemporaryDirectory(prefix=prefix) as temp:
        root=Path(temp);password=secrets.token_hex(32)
        (root/'mysql-password').write_text(password+'\n')
        (root/'mysql.env').write_text('MYSQL_ROOT_PASSWORD='+password+'\n')
        (root/'minio.env').write_text('MINIO_ROOT_USER=owned-health\nMINIO_ROOT_PASSWORD='+secrets.token_hex(32)+'\n')
        (root/'http.py').write_text(HTTP_SERVER)
        try:
            identities={}
            for role in ['mysql','redis','minio','backend','frontend','showroom','onlyoffice']:
                name=prefix+'-'+role
                args=['run','-d','--name',name,'--network','none','--label','task-owner='+prefix]
                if role=='mysql':args+=['--env-file',str(root/'mysql.env'),'--tmpfs','/var/lib/mysql','mysql:8.0.39']
                elif role=='redis':args+=['redis:6-alpine','redis-server','--save','','--appendonly','no']
                elif role=='minio':args+=['--env-file',str(root/'minio.env'),'--tmpfs','/data',
                    'quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z','server','/data']
                else:args+=['-e','OWNED_HTTP_ROLE='+role,'--mount','type=bind,src='+str(root/'http.py')+',dst=/owned_server.py,readonly',
                    '--entrypoint','python3',NATIVE_IMAGE,'-I','/owned_server.py']
                identities[role]=docker(args).decode().strip();containers.append(name)
            # Read-only Docker client obtained from an existing pinned image.
            name=prefix+'-cli'
            docker(['create','--name',name,'--label','task-owner='+prefix,'dd43b430341a']);containers.append(name)
            docker(['cp',name+':/usr/local/bin/docker',str(root/'docker')])
            (root/'request.json').write_text(json.dumps({'prefix':prefix,'identities':identities}))
            name=prefix+'-driver';containers.append(name)
            args=['run','--name',name,'--network','none','--pid','host','--cap-add','SYS_ADMIN','--cap-add','SYS_PTRACE','--cap-add','NET_ADMIN',
                '--security-opt','apparmor=unconfined','--label','task-owner='+prefix,
                '--mount','type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock,readonly',
                '--mount','type=bind,src='+str(ROOT)+',dst=/code,readonly',
                '--mount','type=bind,src='+str(root)+',dst=/fixtures,readonly',
                '--mount','type=bind,src='+str(root/'docker')+',dst=/usr/local/bin/docker,readonly',
                '--entrypoint','python3',NATIVE_IMAGE,'/code/tests/verify_formal_health_real.py','--inside']
            result=docker(args,timeout=300)
            value=json.loads(result);assert value['state']=='PASS'
            retained=ROOT.parents[2]/'doc/tasks/20260927-three-environment-release'/(prefix+'-retained')
            retained.mkdir()
            docker(['cp',name+':/tmp/health-recovery-claim/.',str(retained)])
            value['retainedEvidence']=str(retained)
            print(json.dumps(value))
        finally:
            for name in reversed(containers):
                assert docker(['inspect','--format','{{index .Config.Labels "task-owner"}}',name]).decode().strip()==prefix
                if docker(['inspect','--format','{{.State.Running}}',name]).decode().strip()=='true':
                    docker(['stop','--time','5',name])
                docker(['rm',name])
            print(json.dumps({'ownedFixtureCleanup':'PASS','prefix':prefix}))


if __name__=='__main__':
    inside() if '--inside' in sys.argv else outside()
