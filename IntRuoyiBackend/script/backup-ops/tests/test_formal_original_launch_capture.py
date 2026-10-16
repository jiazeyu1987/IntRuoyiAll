"""Actual Linux private files/claim; explicit Docker/proc boundary doubles.

This is not a live server or cold-start completion attestation.
"""
import contextlib
import base64
import copy
import hashlib
import importlib
import io
import json
import os
from pathlib import Path
import sys
import subprocess
import tempfile
import time
import unittest
import socket
import struct
import signal
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_snapshot_linux as snapshot
import formal_original_launch as launch


class ReadonlyJvmIdentity(unittest.TestCase):
    """Real Linux root/socket/proc; explicit response boundary, not a JVM claim."""
    def setUp(self):
        self.m = importlib.import_module('formal_jvm_identity')
        self.tmp = tempfile.TemporaryDirectory(prefix='owned-identity-', dir='/control')
        self.root = Path(self.tmp.name); self.root.chmod(0o755)
        self.directory = self.root/'socket'; self.directory.mkdir(mode=0o700)
        os.chown(self.directory,1000,1000)
        self.path = self.directory/'control.sock'; self.child = None
        self.boot = Path('/proc/sys/kernel/random/boot_id').read_text().strip()

    def tearDown(self):
        if self.child:
            os.kill(self.child,signal.SIGTERM); os.waitpid(self.child,0)
        self.tmp.cleanup()

    def start(self, mode='good'):
        read, write = os.pipe(); self.child = os.fork()
        if self.child == 0:
            os.close(read); os.setgroups([]); os.setgid(1000); os.setuid(1000)
            listener=socket.socket(socket.AF_UNIX,socket.SOCK_STREAM)
            listener.bind(str(self.path)); self.path.chmod(0o600); listener.listen(4)
            os.write(write,b'1'); os.close(write)
            while True:
                with listener.accept()[0] as conn:
                    def take(n):
                        raw=b''
                        while len(raw)<n:
                            chunk=conn.recv(n-len(raw))
                            if not chunk:raise EOFError()
                            raw+=chunk
                        return raw
                    request=json.loads(take(struct.unpack('!I',take(4))[0]))
                    peer=struct.unpack('3i',conn.getsockopt(socket.SOL_SOCKET,socket.SO_PEERCRED,12))
                    assert peer[1:]==(0,0), 'REAL_ROOT_PEER_REQUIRED'
                    assert set(request)=={'schemaVersion','action','nonce'} and request['action']=='READ_IDENTITY'
                    raw_stat=Path('/proc/self/stat').read_text()
                    identity=dict(jvmInstanceId='00000000-0000-0000-0000-000000000002',instanceId='owned-identity',
                        artifactSha256='a'*64,pid=os.getpid(),startTicks=int(raw_stat[raw_stat.rindex(')')+2:].split()[19]),
                        hostBootId=self.boot,pidNamespace=os.readlink('/proc/self/ns/pid'),
                        mountNamespace=os.readlink('/proc/self/ns/mnt'),fullIdentity=False)
                    value=dict(schemaVersion='formal-maintenance-identity-response/v1',nonce=request['nonce'],identity=identity,
                        codeSourceArtifactBound=True,readOnlyObservation=True,admissionChanged=False,
                        loadedAtStartupBytesProven=False,fullIdentity=False,fullDrain=False,releaseReady=False)
                    if mode=='nonce':value['nonce']='0'*32
                    if mode=='ticks':identity['startTicks']+=1
                    if mode=='pid':identity['pid']+=1
                    if mode=='namespace':identity['mountNamespace']='mnt:[1]'
                    if mode=='uuid':identity['jvmInstanceId']='not-uuid'
                    if mode=='full':value['fullDrain']=True
                    if mode=='bool':identity['pid']=True
                    if mode=='extra':value['token']='SECRET_DO_NOT_ECHO'
                    if mode=='timeout':time.sleep(5);continue
                    raw=json.dumps(value).encode()
                    if mode=='duplicate':raw=raw[:-1]+b',"nonce":"bad"}'
                    conn.sendall(struct.pack('!I',len(raw))+raw)
        os.close(write)
        try:self.assertEqual(os.read(read,1),b'1')
        finally:os.close(read)
        info=self.path.stat()
        self.binding=dict(path=str(self.path),identity=dict(device=info.st_dev,inode=info.st_ino),pid=self.child,uid=1000,gid=1000)
        self.process=launch.process_snapshot(self.child,deadline=time.monotonic()+3)

    def observe(self, seconds=3):
        return self.m.observe_jvm_identity(self.binding,self.process,'owned-identity','a'*64,self.boot,
                                           deadline=time.monotonic()+seconds)

    def test_actual_root_peer_and_process_nonce_without_token(self):
        self.start();result=self.observe()
        self.assertEqual(result['identity']['startTicks'],self.process['startTicks'])
        self.assertEqual(result['peerPid'],self.child)
        self.assertTrue(result['readOnlyObservation']); self.assertFalse(result['releaseReady'])

    def test_forged_response_or_scope_refuses(self):
        for mode in ('nonce','ticks','pid','namespace','uuid','full','bool','extra','duplicate'):
            with self.subTest(mode=mode):
                self.start(mode)
                try:
                    with self.assertRaises(self.m.JvmIdentityError):self.observe()
                finally:
                    os.kill(self.child,signal.SIGTERM);os.waitpid(self.child,0);self.child=None;self.path.unlink()

    def test_wrong_socket_inode_peer_or_process_rejects_before_request(self):
        self.start()
        for change in ('inode','pid','ticks'):
            with self.subTest(change=change):
                binding=copy.deepcopy(self.binding); process=copy.deepcopy(self.process)
                if change=='inode':binding['identity']['inode']+=1
                if change=='pid':binding['pid']+=1
                if change=='ticks':process['startTicks']+=1
                with self.assertRaises(self.m.JvmIdentityError):
                    self.m.observe_jvm_identity(binding,process,'owned-identity','a'*64,self.boot,deadline=time.monotonic()+2)

    def test_deadline_is_bounded_without_replay(self):
        self.start('timeout');begin=time.monotonic()
        with self.assertRaises(self.m.JvmIdentityError):self.observe(.1)
        self.assertLess(time.monotonic()-begin,1)

    def test_user_namespace_remap_is_explicitly_unsupported(self):
        self.start();original=os.read
        def mapped(fd,count):
            if os.readlink('/proc/self/fd/'+str(fd)).endswith('/uid_map'):
                return b'0 100000 65536\n'
            return original(fd,count)
        with patch.object(self.m.os,'read',side_effect=mapped):
            with self.assertRaises(self.m.JvmIdentityError):self.observe()

    def test_response_completed_then_socket_inode_or_mode_changes_refuses(self):
        self.start();original=self.m._response
        def changed(*args):
            value=original(*args);self.path.chmod(0o666);return value
        with patch.object(self.m,'_response',side_effect=changed):
            with self.assertRaises(self.m.JvmIdentityError):self.observe()

    def test_json_failure_never_emits_private_response(self):
        self.start('extra')
        with self.assertRaises(self.m.JvmIdentityError) as caught:self.observe()
        self.assertNotIn('SECRET_DO_NOT_ECHO',str(caught.exception))

    def test_uuid_change_without_new_actual_process_is_not_cold_start(self):
        self.start();previous=self.observe();current=copy.deepcopy(previous)
        current['identity']['jvmInstanceId']='00000000-0000-0000-0000-000000000003'
        with self.assertRaises(self.m.JvmIdentityError):self.m.require_new_jvm(previous,current)

    def test_container_pid_one_or_host_pid_reuse_with_later_ticks_can_match(self):
        self.start();previous=self.observe();previous['identity']['pid']=1;current=copy.deepcopy(previous)
        current['identity']['jvmInstanceId']='00000000-0000-0000-0000-000000000003'
        current['identity']['startTicks']+=1
        result=self.m.require_new_jvm(previous,current)
        self.assertTrue(result['newProcessIdentityMatched']);self.assertFalse(result['coldStartConfirmed'])
        self.assertFalse(result['releaseReady'])

    def test_new_process_match_requires_exact_artifact_boot_instance_and_types(self):
        self.start();previous=self.observe();current=copy.deepcopy(previous)
        current['identity']['jvmInstanceId']='00000000-0000-0000-0000-000000000003';current['identity']['startTicks']+=1
        for key,value in (('artifactSha256','b'*64),('hostBootId','00000000-0000-0000-0000-000000000001'),
                          ('instanceId','other-instance'),('pid',True),('startTicks',True)):
            with self.subTest(key=key):
                changed=copy.deepcopy(current);changed['identity'][key]=value
                with self.assertRaises(self.m.JvmIdentityError):self.m.require_new_jvm(previous,changed)

class DependencyHealth(unittest.TestCase):
    """Real private Linux files; explicit verifier/protocol boundaries."""
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(prefix='owned-dependency-',dir='/control');self.root=Path(self.tmp.name)
        self.binding={'operationId':'owned-dependency','sourceFingerprint':'c'*64,
            'databaseUuid':'00000000-0000-0000-0000-000000000001','minioContainerId':'d'*64}
        self.claim=snapshot.FormalMaintenanceClaim(self.root/'claim',self.binding)
        self.m=importlib.import_module('formal_service_health')
        self.flags={'serverUuid':self.binding['databaseUuid'],'readOnly':1,'superReadOnly':0}
        self.services={};self.held={};self.probes=[]
        for i,role in enumerate(('mysql','redis','minio','onlyoffice','showroom','backend','frontend'),10):
            row=scope();row.update(id='%064x'%i,name='/owned-'+role)
            if role in ('backend','frontend'):row['state']['Running']=False;self.held[role]=row
            else:self.services[role]=row
        self.restored=dict(sharedScopeSha256='e'*64,originalFlagsSha256='f'*64,originalFlags=self.flags,
            services=self.services,applicationsHeld=self.held,native={'nativeState':'NOT_REQUESTED'},productionEvidence=False)
        health={role:self.health(role) for role in self.services}
        self.original={
            'schemaVersion':'formal-shared-health-original/v1','claimSha256':sha(self.claim._plan),
            'sharedScopeSha256':'e'*64,'health':{role:self.health(role) for role in self.services.keys()|self.held.keys()},
            'redisAuthentication':'none','observedAt':'2026-10-06T00:00:00+00:00'}
        self.original_path=self.claim.root/'shared-health-original.json'
        self.claim._write(self.original_path,snapshot._canonical_bytes(self.original))
        self.claim.record('shared-health-scope',{'sha256':sha(self.original_path.read_bytes())})
        self.claim.record('shared-container-scope',{'scopeSha256':'e'*64})
        self.report=dict(schemaVersion='formal-shared-dependencies-health/v1',state='DEPENDENCIES_RESTORED_PENDING_OPEN_APPROVAL',
            claimSha256=sha(self.claim._plan),sharedScopeSha256='e'*64,originalHealthSha256=sha(self.original_path.read_bytes()),
            originalFlags=self.flags,runtimeSha256=sha(snapshot._canonical_bytes(self.services)),dependencies=self.services,
            applicationsHeld=self.held,health=health,native=self.restored['native'],observedAt='2026-10-06T00:01:00+00:00',
            productionEvidence=False,releaseReady=False)
        self.receipt=self.claim.root/'shared-dependencies-health.json'
        self.claim._write(self.receipt,snapshot._canonical_bytes(self.report));self.digest=sha(self.receipt.read_bytes())
        self.claim.record('shared-dependencies-ready',{'sha256':self.digest})
        self.mysql=type('Mysql',(),{'container':self.services['mysql']['id']})()
    def health(self,role):
        if role=='mysql':return {'state':'HEALTHY','proof':{'protocol':'mysql','flags':copy.deepcopy(self.flags)}}
        if role=='redis':proof={'protocol':'redis','response':'PONG','authentication':'none'}
        else:proof=self.m.validate_http(role,200,b'true' if role=='onlyoffice' else b'<html>original</html>'
            if role in ('showroom','frontend') else b'{"status":"UP"}' if role=='backend' else b'')
        proof.update(networkNamespaceInode=500,pid=100)
        return {'state':'HEALTHY','proof':proof}
    def tearDown(self):self.claim.close();self.tmp.cleanup()
    def run_health(self,probe_callback=None,verify_callback=None,guard_callback=None,runner=None,flags_callback=None):
        outer=self
        class Probe:
            def observe(self,role,record,seconds):
                outer.probes.append(role)
                if probe_callback:return probe_callback(role,record,seconds)
                return outer.health(role)['proof']
        def verifier(*args,**kwargs):
            kwargs['guard']();kwargs['thaw_guard']()
            return verify_callback() if verify_callback else copy.deepcopy(self.restored)
        def guard():
            if guard_callback:guard_callback()
        with patch.object(self.m.split,'verify_shared_dependency_restoration',side_effect=verifier), \
             patch.object(self.m.snapshot,'_database_flags',side_effect=flags_callback or (lambda *a:copy.deepcopy(self.flags))), \
             patch.object(self.m.snapshot,'inspect_formal_container',side_effect=lambda identity:copy.deepcopy(
                 next(row for row in list(self.services.values())+list(self.held.values()) if row['id']==identity))), \
             patch.object(self.m,'NativeNetworkProbe',return_value=Probe()):
            if runner:return runner(guard)
            with patch.object(self.claim,'record',side_effect=AssertionError('readonly observer cannot record')), \
                 patch.object(self.claim,'_write',side_effect=AssertionError('readonly observer cannot write')):
                return self.m.observe_shared_dependency_health(self.claim,self.mysql,expected_receipt_sha256=self.digest,
                    guard=guard,thaw_guard=guard,timeout=5,probe=Probe())
    def rewrite_report(self,change):
        report=copy.deepcopy(self.report);change(report);payload=snapshot._canonical_bytes(report)
        self.receipt.write_bytes(payload);self.digest=sha(payload)
        self.claim.events[-1]['details']={'sha256':self.digest}
        self.rewrite_events()
    def rewrite_events(self):
        previous=sha(self.claim._plan)
        for event in self.claim.events:
            event['previousDigest']=previous;payload=snapshot._canonical_bytes(event)
            (self.claim.root/('event-%06d.json'%event['sequence'])).write_bytes(payload);previous=sha(payload)
        self.claim.assert_owned()
    def test_fresh_dependencies_are_readonly_and_held(self):
        before={p.name:p.read_bytes() for p in self.claim.root.iterdir()};result=self.run_health()
        self.assertEqual(before,{p.name:p.read_bytes() for p in self.claim.root.iterdir()})
        self.assertEqual(result['state'],'DEPENDENCIES_HEALTH_OBSERVED_ONLY')
        self.assertEqual(self.probes,['redis','minio','onlyoffice','showroom'])
        for name in ('releaseReady','productionEvidence','serviceActionsPerformed','fullRunnerCompleted','claimReleased'):
            self.assertIs(result[name],False)
    def test_receipt_precise_fields_refuse(self):
        self.rewrite_report(lambda value:value.update(extra=True))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
        self.assertEqual(self.probes,[])
    def test_receipt_bad_native_or_bool_flags_refuse(self):
        self.rewrite_report(lambda value:value['originalFlags'].update(readOnly=True))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_historical_mysql_bool_flags_refuse(self):
        self.rewrite_report(lambda value:value['health']['mysql']['proof']['flags'].update(readOnly=True))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_historical_mysql_wrong_original_flags_refuse(self):
        self.rewrite_report(lambda value:value['health']['mysql']['proof']['flags'].update(readOnly=0))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_historical_mysql_unknown_protocol_refuses(self):
        self.rewrite_report(lambda value:value['health']['mysql']['proof'].update(protocol='redis'))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_historical_redis_unknown_authentication_refuses(self):
        self.rewrite_report(lambda value:value['health']['redis']['proof'].update(authentication='unknown'))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_receipt_noncanonical_refuses(self):
        self.receipt.write_bytes(json.dumps(self.report,indent=2).encode());self.digest=sha(self.receipt.read_bytes())
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_unapproved_receipt_sha_refuses(self):
        self.digest='0'*64
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_ready_hash_refuses(self):
        self.claim.events[-1]['details']={'sha256':'0'*64}
        self.rewrite_events()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_dependency_role_inventory_refuses(self):
        self.rewrite_report(lambda value:value['dependencies'].pop('redis'))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_application_must_remain_stopped(self):
        self.restored['applicationsHeld']['backend']['state']['Running']=True
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_live_health_page_drift_refuses(self):
        def probe(role,*a):
            proof=self.health(role)['proof']
            if role=='showroom':proof['sha256']='0'*64
            return proof
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health(probe_callback=probe)
    def test_health_callback_changes_receipt_identity_refuses(self):
        def probe(role,*a):
            if role=='showroom':
                value=self.receipt.read_bytes();self.receipt.rename(self.claim.root/'retained-receipt')
                self.receipt.write_bytes(value);self.receipt.chmod(0o600)
            return self.health(role)['proof']
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health(probe_callback=probe)
    def test_final_guard_changes_earlier_original_identity_refuses(self):
        m=self.m;actual=snapshot._canonical_bytes;armed=[];done=[]
        def canonical(value):
            raw=actual(value)
            if type(value) is dict and value.get('state')=='DEPENDENCIES_HEALTH_OBSERVED_ONLY':armed.append(1)
            return raw
        def guard():
            if armed and not done:
                value=self.original_path.read_bytes();self.original_path.rename(self.claim.root/'retained-health')
                self.original_path.write_bytes(value);self.original_path.chmod(0o600);done.append(1)
        with patch.object(snapshot,'_canonical_bytes',side_effect=canonical):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_health(guard_callback=guard)
        self.assertEqual(done,[1])
    def test_last_claim_read_changes_lock_mode_refuses(self):
        actual=snapshot.FormalMaintenanceClaim._read;canonical=snapshot._canonical_bytes;armed=[];reads=[];done=[]
        def ready(value):
            result=canonical(value)
            if type(value) is dict and value.get('state')=='DEPENDENCIES_HEALTH_OBSERVED_ONLY':armed.append(1)
            return result
        def read(claim,path):
            result=actual(claim,path)
            if armed and Path(path).name=='event-000003.json' and not done:
                reads.append(1)
                if len(reads)==2:(self.claim.root/'ownership.lock').chmod(0o604);done.append(1)
            return result
        with patch.object(snapshot,'_canonical_bytes',side_effect=ready),patch.object(snapshot.FormalMaintenanceClaim,'_read',read):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
        self.assertEqual(done,[1])
    def test_last_claim_read_appends_valid_open_intent_refuses(self):
        actual=snapshot.FormalMaintenanceClaim._read;canonical=snapshot._canonical_bytes;armed=[];reads=[];done=[]
        def ready(value):
            result=canonical(value)
            if type(value) is dict and value.get('state')=='DEPENDENCIES_HEALTH_OBSERVED_ONLY':armed.append(1)
            return result
        def read(claim,path):
            result=actual(claim,path)
            if armed and Path(path).name=='event-000003.json' and not done:
                reads.append(1)
                if len(reads)==2:
                    event=dict(sequence=4,step='boot-backend-open-intent',details={},previousDigest=sha(result))
                    path=self.claim.root/'event-000004.json';path.write_bytes(canonical(event));path.chmod(0o600);done.append(1)
            return result
        with patch.object(snapshot,'_canonical_bytes',side_effect=ready),patch.object(snapshot.FormalMaintenanceClaim,'_read',read):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
        self.assertEqual(done,[1])
    def test_originally_stopped_showroom_is_not_promoted_healthy(self):
        stopped={'state':'ORIGINALLY_STOPPED','healthObserved':False}
        self.services['showroom']['state']['Running']=False
        self.original['health']['showroom']=stopped
        self.original_path.write_bytes(snapshot._canonical_bytes(self.original));digest=sha(self.original_path.read_bytes())
        self.claim.events[0]['details']={'sha256':digest}
        def change(value):
            value['dependencies']=self.services;value['health']['showroom']=stopped;value['originalHealthSha256']=digest
            value['runtimeSha256']=sha(snapshot._canonical_bytes(self.services))
        self.rewrite_report(change);result=self.run_health()
        self.assertEqual(result['health']['showroom'],stopped);self.assertNotIn('showroom',self.probes)
    def test_probe_failure_is_fatal(self):
        def failed(*a):raise snapshot.FormalSnapshotError('OWNED_UNHEALTHY')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_UNHEALTHY'):self.run_health(probe_callback=failed)
    def test_outer_deadline_is_not_extended(self):
        token=snapshot.observation_deadline.set(time.monotonic()-1)
        try:
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
            self.assertEqual(self.probes,[])
        finally:snapshot.observation_deadline.reset(token)
    def test_last_owner_guard_changes_flags_refuses(self):
        canonical=snapshot._canonical_bytes;armed=[];calls=[]
        def ready(value):
            result=canonical(value)
            if type(value) is dict and value.get('state')=='DEPENDENCIES_HEALTH_OBSERVED_ONLY':armed.append(1)
            return result
        def count():
            if armed:calls.append(1)
        with patch.object(snapshot,'_canonical_bytes',side_effect=ready):self.run_health(guard_callback=count)
        final_count=len(calls);self.assertGreater(final_count,0);armed.clear();calls.clear();done=[]
        def changed():
            if armed:
                calls.append(1)
                if len(calls)==final_count:self.flags['superReadOnly']=1;done.append(1)
        with patch.object(snapshot,'_canonical_bytes',side_effect=ready):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_health(guard_callback=changed)
        self.assertEqual(done,[1])

    def prepare_open(self,artifact_sha='a'*64):
        """Real close, signed approval and actual private files; source doubles remain explicit."""
        import formal_boot_state as boot
        self.bootdir=self.root/'boot';self.bootdir.mkdir(mode=0o755);self.target=self.bootdir/'state.json'
        value=dict(schemaVersion='formal-maintenance-boot/v1',instanceId='owned-backend',artifactSha256=artifact_sha,
            state='OPEN',epoch='',phase='NORMAL')
        self.saved=(json.dumps(value,indent=2)+'\n').encode();self.target.write_bytes(self.saved);self.target.chmod(0o644)
        originals={}
        for name in boot.ORIGINALS:
            path=self.claim.root/name
            if not path.exists():self.claim._write(path,b'{"sourceBoundaryDouble":true}')
            originals[name]=sha(path.read_bytes())
        self.claim.record('maintenance-originals-bound',dict(originalFileSha256=originals))
        closed=snapshot._canonical_bytes(dict(value,state='CLOSED',epoch=self.binding['operationId']))
        host=Path('/proc/sys/kernel/random/boot_id').read_text().strip();info=self.target.stat()
        request=dict(schemaVersion='release-formal-capture-request/v6',action='close-durable-boot-state',
            claimRoot=str(self.claim.root),claimBinding=self.binding,totalSeconds=30,hostBootId=host,
            bootBinding=dict(role='backend',path=str(self.target),instanceId=value['instanceId'],artifactSha256=artifact_sha,
                originalOpenSha256=sha(self.saved),closedSha256=sha(closed),epoch=self.binding['operationId'],
                expectedIdentity=dict(device=info.st_dev,inode=info.st_ino)))
        path=self.root/'close.json';path.write_bytes(snapshot._canonical_bytes(request));path.chmod(0o600)
        self.claim.close();boot.close_approved_boot_state(str(path),sha(path.read_bytes()))
        self.claim=snapshot.FormalMaintenanceClaim(self.root/'claim',self.binding,resume=True)
        def openssl(*args):return subprocess.check_output(['/usr/bin/openssl',*args],stderr=subprocess.DEVNULL)
        private=self.root/'private.pem';openssl('genpkey','-algorithm','ED25519','-out',str(private));private.chmod(0o600)
        self.private=private
        self.key=self.root/'public.der';self.key.write_bytes(openssl('pkey','-in',str(private),'-pubout','-outform','DER'))
        self.key.chmod(0o644);self.keysha=sha(self.key.read_bytes());self.closed=closed
        self.expected=dict(operationId=self.binding['operationId'],role='backend',hostBootId=host,
            containerId=self.held['backend']['id'],launchSha256='e'*64,artifactSha256=artifact_sha,
            originalOpenSha256=sha(self.saved),closedSha256=sha(closed))
        now=int(time.time());self.payload=dict(self.expected,purpose='RESTORE_SAVED_ORIGINAL_OPEN',grantId='f'*32,
            actorId=17,tenantId=122,issuedAt=now-5,expiresAt=now+300)
        message=self.root/'message';message.write_bytes(snapshot._canonical_bytes(self.payload))
        signature=openssl('pkeyutl','-sign','-inkey',str(private),'-rawin','-in',str(message))
        self.approval=self.root/'approval.json';self.approval.write_bytes(snapshot._canonical_bytes(dict(
            schemaVersion='formal-original-open-approval/v1',keySha256=self.keysha,
            payloadBase64=base64.b64encode(message.read_bytes()).decode(),signatureBase64=base64.b64encode(signature).decode())))
        self.approval.chmod(0o600)
    def run_open_health(self,guard_callback=None,probe_callback=None):
        import formal_original_open_writer as writer
        def runner(guard):
            with patch.object(self.m.split,'_thaw_receipt',return_value=copy.deepcopy(self.restored['native'])):
                return writer.restore_saved_original_open_with_dependency_health(self.claim,self.mysql,str(self.approval),
                    str(self.key),self.keysha,self.expected,expected_receipt_sha256=self.digest,source_guard=guard,timeout=30)
        return self.run_health(guard_callback=guard_callback,probe_callback=probe_callback,runner=runner)
    def test_signed_open_health_integration_keeps_dependencies_and_applications_held(self):
        self.prepare_open();result=self.run_open_health()
        self.assertEqual(self.target.read_bytes(),self.saved)
        self.assertEqual([e['step'] for e in self.claim.events[-3:]],
            ['boot-backend-open-intent','boot-backend-open-approval-consumed','boot-backend-open-restored'])
        self.assertGreater(self.probes.count('redis'),3)
        self.assertEqual(result['state'],'SAVED_ORIGINAL_OPEN_RESTORED_PENDING_COLD_START')
        for name in ('releaseReady','productionEvidence','serviceActionsPerformed','claimReleased','fullRunnerCompleted'):
            self.assertIs(result[name],False)
        self.assertTrue(all(row['state']['Running'] is False for row in self.held.values()))
    def test_open_health_failure_after_intent_retains_no_replay(self):
        self.prepare_open()
        def failed(role,*a):
            if any(e['step']=='boot-backend-open-intent' for e in self.claim.events):
                raise snapshot.FormalSnapshotError('OWNED_HEALTH_FAILED')
            return self.health(role)['proof']
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_open_health(probe_callback=failed)
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertEqual(self.claim.events[-1]['step'],'boot-backend-open-intent')
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_open_health()
        self.assertEqual(self.claim.events[-1]['step'],'boot-backend-open-intent')
    def test_open_health_foreign_journal_event_refuses(self):
        self.prepare_open();done=[]
        def drift():
            if any(e['step']=='boot-backend-open-intent' for e in self.claim.events) and not done:
                self.claim.record('boot-test-backend-open-intent',{});done.append(1)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_open_health(guard_callback=drift)
        self.assertEqual(done,[1]);self.assertEqual(self.target.read_bytes(),self.closed)
    def test_open_health_consumption_drift_refuses(self):
        self.prepare_open();done=[]
        def drift():
            if any(e['step']=='boot-backend-open-approval-consumed' for e in self.claim.events) and not done:
                (self.claim.root/'boot-backend-open-approval-consumed.json').write_bytes(b'{}');done.append(1)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_open_health(guard_callback=drift)
        self.assertEqual(done,[1]);self.assertEqual(self.target.read_bytes(),self.closed)
    def test_default_observer_still_refuses_completed_open(self):
        self.prepare_open();self.run_open_health()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_health()
    def test_open_health_same_bytes_receipt_replacement_refuses(self):
        self.prepare_open();done=[]
        def drift():
            if any(e['step']=='boot-backend-open-intent' for e in self.claim.events) and not done:
                value=self.receipt.read_bytes();self.receipt.rename(self.claim.root/'old-health-receipt')
                self.receipt.write_bytes(value);self.receipt.chmod(0o600);done.append(1)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_open_health(guard_callback=drift)
        self.assertEqual(done,[1]);self.assertEqual(self.target.read_bytes(),self.closed)
    def test_open_health_expected_container_must_match_held_backend(self):
        self.prepare_open();self.expected['containerId']='0'*64
        self.payload.update(self.expected);message=self.root/'message';message.write_bytes(snapshot._canonical_bytes(self.payload))
        signature=subprocess.check_output(['/usr/bin/openssl','pkeyutl','-sign','-inkey',str(self.private),'-rawin',
            '-in',str(message)],stderr=subprocess.DEVNULL)
        envelope=json.loads(self.approval.read_bytes());envelope.update(
            payloadBase64=base64.b64encode(message.read_bytes()).decode(),signatureBase64=base64.b64encode(signature).decode())
        self.approval.write_bytes(snapshot._canonical_bytes(envelope))
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_open_health()
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertFalse(any('-open-' in e['step'] for e in self.claim.events))
    def test_open_health_no_content_callback_after_final_target_check(self):
        import formal_original_open_writer as writer
        self.prepare_open();actual=writer.restore_saved_original_open;secure=writer.boot._read_secure;armed=[];done=[]
        def author(*args,**kwargs):
            result=actual(*args,**kwargs);armed.append(1);return result
        def read(*args,**kwargs):
            result=secure(*args,**kwargs)
            if armed and not done:self.target.write_bytes(self.closed);done.append(1)
            return result
        with patch.object(writer,'restore_saved_original_open',side_effect=author),patch.object(writer.boot,'_read_secure',side_effect=read):
            try:result=self.run_open_health()
            except snapshot.FormalSnapshotError:
                self.assertEqual(done,[1]);return
        self.assertIs(result['durableOriginalOpenConfirmed'],True)
        self.assertEqual(self.target.read_bytes(),self.saved)
    def test_open_health_transition_content_finishes_before_final_native_flags(self):
        import formal_original_open_writer as writer
        self.prepare_open();verified=dict(self.payload,approvalSha256=sha(self.approval.read_bytes()),keySha256=self.keysha)
        transition=writer.OriginalOpenHealthTransition(self.claim,self.expected,verified,str(self.approval),str(self.key))
        canonical=snapshot._canonical_bytes;validate=transition.assert_valid;armed=[];final_db=[];late=[]
        def ready(value):
            result=canonical(value)
            if type(value) is dict and value.get('state')=='DEPENDENCIES_HEALTH_OBSERVED_ONLY':armed.append(1)
            return result
        def flags(*args):
            if armed:final_db.append(1)
            return copy.deepcopy(self.flags)
        def content():
            if final_db:late.append(1)
            return validate()
        def runner(guard):
            return self.m.observe_shared_dependency_health(self.claim,self.mysql,expected_receipt_sha256=self.digest,
                guard=guard,thaw_guard=guard,timeout=5,open_transition=transition)
        with patch.object(snapshot,'_canonical_bytes',side_effect=ready),patch.object(transition,'assert_valid',side_effect=content):
            result=self.run_health(runner=runner,flags_callback=flags)
        self.assertEqual(final_db,[1]);self.assertEqual(late,[])
        self.assertIs(result['releaseReady'],False)
    def test_open_health_missing_receipt_backend_gives_fixed_refusal(self):
        self.rewrite_report(lambda report:report['applicationsHeld'].pop('backend'))
        self.prepare_open()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_open_health()
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertFalse(any('-open-' in event['step'] for event in self.claim.events))

    def prepare_source_checks(self,artifact_sha='a'*64):
        import formal_original_open_writer as writer
        import formal_ingress_control as ingress
        import formal_external_callers as callers
        import formal_original_launch_capture as capture
        self.source_writer=writer;self.source_ingress=ingress;self.source_callers=callers;self.source_capture=capture
        self.services['minio']['id']=self.binding['minioContainerId']
        self.held['backend']['state']['FinishedAt']='2026-10-06T00:01:00Z'
        self.restored.update(services=copy.deepcopy(self.services),applicationsHeld=copy.deepcopy(self.held))
        self.rewrite_report(lambda report:report.update(applicationsHeld=copy.deepcopy(self.held),
            dependencies=copy.deepcopy(self.services),runtimeSha256=sha(snapshot._canonical_bytes(self.services))))
        self.report=json.loads(self.receipt.read_bytes())
        original=copy.deepcopy({**self.services,**self.held})
        original['backend']['state'].update(Running=True,FinishedAt='0001-01-01T00:00:00Z')
        shared_raw=snapshot._canonical_bytes(original);self.shared_sha=sha(shared_raw)
        (self.claim.root/'shared-container-original.json').write_bytes(shared_raw)
        (self.claim.root/'shared-container-original.json').chmod(0o600)
        next(e for e in self.claim.events if e['step']=='shared-container-scope')['details']={'scopeSha256':self.shared_sha}
        self.restored['sharedScopeSha256']=self.shared_sha
        self.original['sharedScopeSha256']=self.shared_sha
        self.original_path.write_bytes(snapshot._canonical_bytes(self.original))
        original_health_sha=sha(self.original_path.read_bytes())
        next(e for e in self.claim.events if e['step']=='shared-health-scope')['details']={'sha256':original_health_sha}
        self.rewrite_report(lambda report:report.update(sharedScopeSha256=self.shared_sha,
            originalHealthSha256=original_health_sha))
        self.report=json.loads(self.receipt.read_bytes())
        self.rewrite_events()
        intent=dict(scopeSha256=self.shared_sha,before=original['backend']['state'],actionRequired=True)
        self.claim.record('shared-backend-stop-intent',intent)
        self.claim.record('shared-backend-stopped',dict(scopeSha256=self.shared_sha,runtime=self.held['backend']['state'],
            actionPerformed=True,intentSha256=sha(snapshot._canonical_bytes(intent))))
        self.external_sha=sha(b'{"scopeBoundaryDouble":true}')
        self.claim._write(self.claim.root/'external-callers-original.json',b'{"scopeBoundaryDouble":true}')
        self.claim.record('external-caller-scope',dict(scopeSha256=self.external_sha))
        for name in ('ingress-plan.json','ingress-installed.json','ingress-recovery-plan.json','ingress-recovery-installed.json'):
            self.claim._write(self.claim.root/name,b'{"nftBoundaryDouble":true}')
        self.rules_sha=sha((self.claim.root/'ingress-recovery-installed.json').read_bytes())
        self.prepare_open(artifact_sha)
        self.stopped_full=full();self.stopped_full.update(Id=self.held['backend']['id'],Image=self.held['backend']['image'])
        self.stopped_full['State'].update(self.held['backend']['state'],Pid=0)
        self.expected['launchSha256']=launch.stopped_inspect_projection(raw(self.stopped_full),
            self.held['backend']['id'],self.held['backend']['image'])['digests']['launchSha256']
        self.payload.update(self.expected);message=self.root/'message';message.write_bytes(snapshot._canonical_bytes(self.payload))
        signature=subprocess.check_output(['/usr/bin/openssl','pkeyutl','-sign','-inkey',str(self.private),'-rawin',
            '-in',str(message)],stderr=subprocess.DEVNULL)
        envelope=json.loads(self.approval.read_bytes());envelope.update(payloadBase64=base64.b64encode(message.read_bytes()).decode(),
            signatureBase64=base64.b64encode(signature).decode());self.approval.write_bytes(snapshot._canonical_bytes(envelope))
        self.source_calls=[]
        # Real host claim/mask fixture, explicit systemd protocol boundary.
        from test_formal_host_admission import HostAdmission
        import formal_host_admission as host
        self.source_host_module=host
        fixture=HostAdmission();fixture.setUp();fixture.claim.close();fixture.claim=self.claim
        fixture.cap['bootId']=self.expected['hostBootId'];fixture.plan['host']=copy.deepcopy(fixture.cap)
        fixture.digest=sha(snapshot._canonical_bytes(fixture.plan));fixture.bind();fixture.close_all()
        self.source_host_fixture=fixture;self.addCleanup(fixture.tmp.cleanup)
    def run_source_checks(self,ingress_change=None,caller_failure=False,command_change=None,runtime_change=None,source_only=False,caller_change=None):
        def nft(*args,**kwargs):
            self.source_calls.append('ingress')
            result=dict(phase='RECOVERY_DEPENDENCIES_PERMITTED',rulesSha256=self.rules_sha)
            if ingress_change:ingress_change(result)
            return result
        def external(*args,**kwargs):
            self.source_calls.append('external')
            if caller_failure:raise snapshot.FormalSnapshotError('OWNED_EXTERNAL_NOT_STOPPED')
            result=dict(state='KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION',scopeSha256=self.external_sha)
            if caller_change:caller_change(result)
            return result
        def command(*args,**kwargs):
            self.source_calls.append('launch');value=copy.deepcopy(self.stopped_full)
            if command_change:command_change(value)
            return raw(value)
        def runtime(*args,**kwargs):
            self.source_calls.append('runtime');value=copy.deepcopy(self.held['backend'])
            if runtime_change:runtime_change(value)
            return value
        def runner(_):
            if source_only:
                checks=self.source_writer.OriginalOpenSourceChecks(self.claim,self.expected,self.rules_sha,self.external_sha)
                return checks()
            with patch.object(self.m.split,'_thaw_receipt',return_value=copy.deepcopy(self.restored['native'])):
                return self.source_writer.restore_saved_original_open_with_source_checks(self.claim,self.mysql,
                    str(self.approval),str(self.key),self.keysha,self.expected,expected_receipt_sha256=self.digest,
                    expected_rules_sha256=self.rules_sha,expected_external_scope_sha256=self.external_sha,timeout=180)
        with patch.object(self.source_ingress,'assert_ingress',side_effect=nft), \
             patch.object(self.source_callers,'guard_external_callers',side_effect=external), \
             patch.object(launch,'_bounded_command',side_effect=command), \
             patch.object(self.source_host_module,'NativeHostAdmission',return_value=self.source_host_fixture.native), \
             patch.object(self.source_capture,'inspect_local_scope',side_effect=runtime):
            return self.run_health(runner=runner)
    def test_source_checks_call_all_real_boundaries_without_claiming_full_guard(self):
        self.prepare_source_checks();result=self.run_source_checks()
        self.assertEqual(self.target.read_bytes(),self.saved)
        self.assertTrue(all(self.source_calls.count(name)>1 for name in ('ingress','external','launch','runtime')))
        self.assertIs(result['sourceChecksPerformed'],True)
        for name in ('fullPreOpenGuard','hostWritersVerified','coldStartConfirmed','releaseReady'):
            self.assertIs(result[name],False)

    def prepare_cold_open(self,completed=True):
        self.prepare_source_checks()
        if completed:self.complete_cold_open_fixture()
        return self.source_writer.CompletedOriginalOpenChecks(self.claim,str(self.approval),str(self.key),
            self.keysha,self.expected,deadline=time.monotonic()+30)

    def complete_cold_open_fixture(self):
        # Real signature/close/one-attempt OPEN author. Full source/health
        # integration is covered separately; this finite handoff test does not
        # substitute this test guard for a production exclusion proof.
        return self.source_writer.restore_saved_original_open(self.claim,str(self.approval),str(self.key),
            self.keysha,self.expected,live_guard=self.claim.assert_owned)

    def cold_start_intent(self):
        return dict(scopeSha256=self.shared_sha,before=copy.deepcopy(self.held['backend']['state']),actionRequired=True)

    def cold_start_completion(self,intent):
        runtime=dict(self.held['backend']['state'],Running=True,StartedAt='2026-10-06T00:02:00Z',FinishedAt='0001-01-01T00:00:00Z')
        return dict(scopeSha256=self.shared_sha,runtime=runtime,actionPerformed=True,
            intentSha256=sha(snapshot._canonical_bytes(intent)))

    def prepare_backend_phase_health(self):
        """Actual claim/crypto/files and split verifier; Docker/SQL/network are explicit boundaries."""
        self.prepare_source_checks()
        self.record_dependency_recovery_fixture()
        self.complete_cold_open_fixture()
        self.phase_open_checks=self.source_writer.CompletedOriginalOpenChecks(self.claim,str(self.approval),str(self.key),
            self.keysha,self.expected,deadline=time.monotonic()+60)
        self.phase_stop=copy.deepcopy(self.held['backend'])
        self.phase_probes=[]

    def record_dependency_recovery_fixture(self):
        import formal_split_capture as split
        database_raw=snapshot._canonical_bytes(self.flags);database_sha=sha(database_raw)
        (self.claim.root/'database-original.json').write_bytes(database_raw)
        (self.claim.root/'database-original.json').chmod(0o600)
        self.claim.record('db-fence-intent',dict(originalFlags=self.flags,originalSha256=database_sha))
        self.claim.record('db-fenced',dict(originalSha256=database_sha))
        self.claim.record('shared-recovery-intent',dict(scopeSha256=self.shared_sha,nativeState='NOT_REQUESTED'))
        for role in split.RESTORE_ORDER[:5]:
            state=copy.deepcopy(self.services[role]['state'])
            intent=dict(scopeSha256=self.shared_sha,before=state,actionRequired=False)
            self.claim.record('shared-'+role+'-restore-intent',intent)
            self.claim.record('shared-'+role+'-restored',dict(scopeSha256=self.shared_sha,runtime=state,
                actionPerformed=False,intentSha256=sha(snapshot._canonical_bytes(intent))))
        self.claim.record('shared-db-restored',dict(originalSha256=database_sha,originalFlags=self.flags,
            mysqlRuntime=self.services['mysql']['state'],actionPerformed=False))

    def phase_backend_start(self,completed=False):
        intent=self.cold_start_intent()
        self.claim.record('shared-backend-restore-intent',intent)
        complete=self.cold_start_completion(intent)
        self.held['backend']['state']=copy.deepcopy(complete['runtime'])
        if completed:self.claim.record('shared-backend-restored',complete)

    def run_backend_phase_health(self,guard_callback=None,probe_callback=None):
        import formal_split_capture as split
        outer=self
        class Probe:
            def observe(self,role,record,seconds):
                outer.phase_probes.append(role)
                return probe_callback(role,record,seconds) if probe_callback else outer.health(role)['proof']
        def current(identity):
            return copy.deepcopy(next(row for row in list(self.services.values())+list(self.held.values()) if row['id']==identity))
        with patch.object(snapshot,'inspect_formal_container',side_effect=current), \
             patch.object(snapshot,'_database_flags',side_effect=lambda *args:copy.deepcopy(self.flags)), \
             patch.object(split,'_thaw_receipt',return_value={'nativeState':'NOT_REQUESTED'}), \
             patch.object(self.m,'_mysql_proof',side_effect=lambda *args:self.health('mysql')), \
             patch.object(self.claim,'record',side_effect=AssertionError('readonly phase observer cannot record')), \
             patch.object(self.claim,'_write',side_effect=AssertionError('readonly phase observer cannot write')), \
             patch.object(snapshot,'run_formal_container_action',side_effect=AssertionError('readonly phase observer cannot dispatch')):
            return self.m.observe_shared_dependency_health(self.claim,self.mysql,expected_receipt_sha256=self.digest,
                guard=guard_callback or self.claim.assert_owned,thaw_guard=self.claim.assert_owned,timeout=30,
                probe=Probe(),backend_transition=self.phase_open_checks)

    def test_backend_dependency_phase_before_intent_keeps_original_backend_stopped(self):
        self.prepare_backend_phase_health();result=self.run_backend_phase_health()
        self.assertEqual(result['backendStartStage'],'NOT_ATTEMPTED')
        self.assertEqual(result['backendHealth']['state'],'HELD_STOPPED_PENDING_START')
        self.assertNotIn('backend',self.phase_probes)
        for name in ('fullRunnerCompleted','productionEvidence','releaseReady','claimReleased','serviceActionsPerformed'):
            self.assertIs(result[name],False)

    def test_backend_dependency_phase_exact_pending_intent_allows_only_original_stopped_state(self):
        self.prepare_backend_phase_health();self.claim.record('shared-backend-restore-intent',self.cold_start_intent())
        result=self.run_backend_phase_health();self.assertEqual(result['backendStartStage'],'INTENT_RETAINED')
        self.assertEqual(self.claim.events[-1]['step'],'shared-backend-restore-intent')

    def test_backend_dependency_phase_pending_running_requires_fresh_up_without_recording_completion(self):
        self.prepare_backend_phase_health();self.phase_backend_start()
        result=self.run_backend_phase_health();self.assertEqual(result['backendStartStage'],'INTENT_RETAINED')
        self.assertEqual(result['backendHealth']['state'],'HEALTHY');self.assertIn('backend',self.phase_probes)
        self.assertEqual(self.claim.events[-1]['step'],'shared-backend-restore-intent')

    def test_backend_dependency_phase_completed_running_keeps_exact_recorded_runtime(self):
        self.prepare_backend_phase_health();self.phase_backend_start(completed=True)
        result=self.run_backend_phase_health()
        self.assertEqual(result['backendStartStage'],'RUNTIME_RECORDED_PENDING_FULL_COLD_CONFIRMATION')
        self.assertEqual(result['backendHealth']['state'],'HEALTHY')
        self.held['backend']['state']['StartedAt']='2026-10-06T00:03:00Z'
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_backend_phase_health()

    def test_backend_dependency_phase_without_intent_running_is_rejected(self):
        self.prepare_backend_phase_health()
        self.held['backend']['state']=self.cold_start_completion(self.cold_start_intent())['runtime']
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_backend_phase_health()

    def test_backend_dependency_phase_frontend_start_and_mysql_flags_drift_refuse(self):
        self.prepare_backend_phase_health();self.held['frontend']['state']['Running']=True
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_backend_phase_health()
        self.held['frontend']['state']['Running']=False;self.flags['superReadOnly']=1
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_backend_phase_health()

    def test_backend_dependency_phase_healthy_response_cannot_hide_runtime_or_receipt_drift(self):
        self.prepare_backend_phase_health();self.phase_backend_start()
        changed=[]
        def probe(role,record,seconds):
            if role=='backend':
                self.services['redis']['state']['StartedAt']='2026-10-06T00:03:00Z';changed.append(role)
            return self.health(role)['proof']
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_backend_phase_health(probe_callback=probe)
        self.assertEqual(changed,['backend'])

    def test_backend_dependency_phase_unhealthy_original_backend_leaves_pending_intent(self):
        self.prepare_backend_phase_health();self.phase_backend_start()
        def probe(role,record,seconds):
            if role=='backend':raise snapshot.FormalSnapshotError('OWNED_BACKEND_NOT_UP')
            return self.health(role)['proof']
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_BACKEND_NOT_UP'):
            self.run_backend_phase_health(probe_callback=probe)
        self.assertEqual(self.claim.events[-1]['step'],'shared-backend-restore-intent')

    def test_backend_dependency_phase_equal_bytes_receipt_replacement_refuses(self):
        self.prepare_backend_phase_health();changed=[]
        def guard():
            if not changed:
                replacement=self.root/'replacement';replacement.write_bytes(self.receipt.read_bytes());replacement.chmod(0o600)
                replacement.replace(self.receipt);changed.append(1)
        # Replacement after this invocation's private pins are captured.
        calls=[]
        def probe(role,record,seconds):
            if not calls:guard();calls.append(1)
            return self.health(role)['proof']
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_backend_phase_health(probe_callback=probe)
        self.assertEqual(changed,[1])

    def test_backend_dependency_phase_last_private_content_precedes_final_database_flags(self):
        import formal_boot_state as boot
        self.prepare_backend_phase_health();order=[];read=boot._read_secure
        def content(*args,**kwargs):
            order.append('content');return read(*args,**kwargs)
        def flags(*args):order.append('flags');return copy.deepcopy(self.flags)
        # The actual split verifier stays wired. Trace content reads, not an estimated callback count.
        with patch.object(boot,'_read_secure',side_effect=content), \
             patch.object(snapshot,'_database_flags',side_effect=flags):
            # run_backend_phase_health has an inner flags boundary; trace that native boundary explicitly.
            inner=self.flags
            class TracedFlags(dict):
                def __deepcopy__(value,memo):order.append('flags');return dict(value)
            self.flags=TracedFlags(inner)
            try:self.run_backend_phase_health()
            finally:self.flags=inner
        last=max(index for index,name in enumerate(order) if name=='flags')
        self.assertNotIn('content',order[last+1:])

    def test_cold_open_handoff_validates_actual_completion_and_exact_start_suffix(self):
        checks=self.prepare_cold_open();initial=copy.deepcopy(self.claim.events)
        self.assertEqual(checks.assert_valid()['startStage'],'NOT_ATTEMPTED')
        intent=self.cold_start_intent();self.claim.record('shared-backend-restore-intent',intent)
        self.assertEqual(checks.assert_valid()['startStage'],'INTENT_RETAINED')
        self.claim.record('shared-backend-restored',self.cold_start_completion(intent))
        result=checks.assert_valid();self.assertEqual(result['startStage'],'RUNTIME_RECORDED_PENDING_FULL_COLD_CONFIRMATION')
        for key in ('coldStartConfirmed','fullPreOpenGuard','releaseReady','serviceActionsPerformed'):self.assertIs(result[key],False)
        self.assertEqual(self.claim.events[:len(initial)],initial);self.assertEqual(self.target.read_bytes(),self.saved)

    def test_cold_open_handoff_missing_completion_or_consumption_refuses(self):
        with self.assertRaises(snapshot.FormalSnapshotError):self.prepare_cold_open(completed=False)
        self.complete_cold_open_fixture();(self.claim.root/'boot-backend-open-approval-consumed.json').unlink()
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.source_writer.CompletedOriginalOpenChecks(self.claim,str(self.approval),str(self.key),self.keysha,
                self.expected,deadline=time.monotonic()+30)

    def test_cold_open_handoff_target_equal_bytes_inode_replacement_refuses(self):
        checks=self.prepare_cold_open();value=self.target.read_bytes()
        self.target.rename(self.target.with_suffix('.old'));self.target.write_bytes(value);self.target.chmod(0o644)
        with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()

    def test_cold_open_handoff_changed_approval_or_trusted_key_refuses(self):
        checks=self.prepare_cold_open();value=self.approval.read_bytes()
        self.approval.rename(self.approval.with_suffix('.old'));self.approval.write_bytes(value);self.approval.chmod(0o600)
        with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.source_writer.CompletedOriginalOpenChecks(self.claim,str(self.approval),str(self.key),'0'*64,
                self.expected,deadline=time.monotonic()+30)

    def test_cold_open_handoff_wrong_start_before_or_foreign_event_refuses(self):
        checks=self.prepare_cold_open();intent=self.cold_start_intent();intent['before']['StartedAt']='2026-10-06T00:00:01Z'
        self.claim.record('shared-backend-restore-intent',intent)
        with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()

    def test_cold_open_handoff_wrong_completion_digest_and_second_start_refuse(self):
        checks=self.prepare_cold_open();intent=self.cold_start_intent();self.claim.record('shared-backend-restore-intent',intent)
        completed=self.cold_start_completion(intent);completed['intentSha256']='0'*64
        self.claim.record('shared-backend-restored',completed)
        with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()

    def test_cold_open_handoff_last_private_read_changes_target_refuses(self):
        checks=self.prepare_cold_open();secure=self.source_writer.boot._read_secure;done=[]
        def changed(path,*args,**kwargs):
            value=secure(path,*args,**kwargs)
            if Path(path)==self.approval and not done:
                raw=self.target.read_bytes();self.target.rename(self.target.with_suffix('.late'))
                self.target.write_bytes(raw);self.target.chmod(0o644);done.append(1)
            return value
        with patch.object(self.source_writer.boot,'_read_secure',side_effect=changed):
            with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()
        self.assertEqual(done,[1])

    def test_cold_open_handoff_malformed_close_publication_identity_refuses(self):
        self.prepare_source_checks();self.complete_cold_open_fixture()
        next(e for e in self.claim.events if e['step']=='boot-backend-closed')['details']['publishedIdentity']={}
        self.rewrite_events()
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.source_writer.CompletedOriginalOpenChecks(self.claim,str(self.approval),str(self.key),self.keysha,
                self.expected,deadline=time.monotonic()+30)

    def test_cold_open_handoff_foreign_suffix_event_refuses(self):
        checks=self.prepare_cold_open();self.claim.record('host-crond-restore-intent',{})
        with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()

    def test_cold_open_handoff_completed_start_record_cannot_be_replaced(self):
        checks=self.prepare_cold_open();intent=self.cold_start_intent()
        self.claim.record('shared-backend-restore-intent',intent)
        self.claim.record('shared-backend-restored',self.cold_start_completion(intent));checks.assert_valid()
        self.claim.events[-1]['details']['runtime']['StartedAt']='2026-10-06T00:03:00Z';self.rewrite_events()
        with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()

    def test_cold_open_handoff_expired_current_approval_refuses(self):
        checks=self.prepare_cold_open()
        with patch.object(self.source_writer.time,'time',return_value=self.payload['expiresAt']):
            with self.assertRaises(snapshot.FormalSnapshotError):checks.assert_valid()
    def test_source_checks_require_recovery_ingress_phase(self):
        self.prepare_source_checks()
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.run_source_checks(ingress_change=lambda value:value.update(phase='CAPTURE_RESTRICTED'))
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertFalse(any('-open-' in e['step'] for e in self.claim.events))
    def test_source_checks_missing_closed_host_originals_refuses_before_open(self):
        self.prepare_source_checks();(self.claim.root/'host-originals.json').unlink()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks()
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertNotIn('boot-backend-open-intent',{e['step'] for e in self.claim.events})
    def test_source_checks_reopened_host_entry_refuses_before_consumption(self):
        self.prepare_source_checks();fixture=self.source_host_fixture;unit=fixture.m.CLOSE_ORDER[0]
        def reopen(value):fixture.states[unit].update(active='active',sub='running',mainPid=99)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks(ingress_change=reopen)
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertNotIn('boot-backend-open-approval-consumed',{e['step'] for e in self.claim.events})
    def test_source_checks_require_exact_approved_rules(self):
        self.prepare_source_checks()
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.run_source_checks(ingress_change=lambda value:value.update(rulesSha256='1'*64))
        self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_external_failure_cannot_be_optional(self):
        self.prepare_source_checks()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_EXTERNAL_NOT_STOPPED'):
            self.run_source_checks(caller_failure=True)
        self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_full_launch_change_refuses(self):
        self.prepare_source_checks()
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.run_source_checks(command_change=lambda value:value['HostConfig'].update(FutureField=['changed']))
        self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_final_runtime_after_launch_refuses(self):
        self.prepare_source_checks()
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.run_source_checks(runtime_change=lambda value:value['state'].update(Running=True))
        self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_equal_byte_ingress_replacement_refuses(self):
        self.prepare_source_checks();done=[]
        def replace(_):
            if done:return
            path=self.claim.root/'ingress-recovery-installed.json';value=path.read_bytes()
            path.rename(path.with_suffix('.old'));path.write_bytes(value);path.chmod(0o600);done.append(1)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks(ingress_change=replace)
        self.assertEqual(done,[1]);self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_missing_external_scope_refuses(self):
        self.prepare_source_checks();(self.claim.root/'external-callers-original.json').unlink()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks()
        self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_late_full_launch_change_after_private_read_refuses(self):
        self.prepare_source_checks();secure=self.source_writer.boot._read_secure;done=[]
        def change(*args,**kwargs):
            result=secure(*args,**kwargs)
            if self.source_calls.count('launch')>=2 and not done:
                self.stopped_full['HostConfig']['FutureField']=['late-change'];done.append(1)
            return result
        with patch.object(self.source_writer.boot,'_read_secure',side_effect=change):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks(source_only=True)
        self.assertEqual(done,[1]);self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_pending_legacy_backend_start_refuses_before_consumption(self):
        self.prepare_source_checks();self.claim.record('backend-restore-intent',{})
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks()
        self.assertEqual(self.target.read_bytes(),self.closed)
        self.assertFalse(any('-open-' in e['step'] for e in self.claim.events))
    def test_source_checks_late_ingress_after_private_read_refuses(self):
        self.prepare_source_checks();secure=self.source_writer.boot._read_secure;done=[]
        def change(*args,**kwargs):
            result=secure(*args,**kwargs)
            if self.source_calls.count('launch')>=2 and not done:done.append(1)
            return result
        def nft(value):
            if done:value['rulesSha256']='9'*64
        with patch.object(self.source_writer.boot,'_read_secure',side_effect=change):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks(ingress_change=nft,source_only=True)
        self.assertEqual(done,[1]);self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_late_external_after_private_read_refuses(self):
        self.prepare_source_checks();secure=self.source_writer.boot._read_secure;done=[]
        def change(*args,**kwargs):
            result=secure(*args,**kwargs)
            if self.source_calls.count('launch')>=2 and not done:done.append(1)
            return result
        def external(value):
            if done:value['state']='CHANGED_CALLER_NOT_STOPPED'
        with patch.object(self.source_writer.boot,'_read_secure',side_effect=change):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_source_checks(caller_change=external,source_only=True)
        self.assertEqual(done,[1]);self.assertEqual(self.target.read_bytes(),self.closed)
    def test_source_checks_host_tail_content_cannot_change_already_observed_launch(self):
        self.prepare_source_checks();secure=self.source_writer.boot._read_secure;done=[]
        def change(path,*args,**kwargs):
            result=secure(path,*args,**kwargs)
            if Path(path).name=='host-originals.json' and self.source_calls.count('runtime')>=1 and not done:
                self.stopped_full['HostConfig']['FutureField']=['changed-in-host-tail'];done.append(1)
            return result
        with patch.object(self.source_writer.boot,'_read_secure',side_effect=change):
            try:self.run_source_checks(source_only=True)
            except snapshot.FormalSnapshotError:self.assertEqual(done,[1])
            else:self.assertEqual(done,[],'no host content reads may follow the final launch/runtime observations')

CID='a'*64; IMAGE='sha256:'+'b'*64
def sha(raw):return hashlib.sha256(raw).hexdigest()
def full():
    return {'Id':CID,'Image':IMAGE,'Path':'/bin/sh','Args':['-c','exec java'],
        'Config':{'Env':['PRIVATE=never-emit-me','ORDER=one','ORDER=two'],'Cmd':['java','-jar','app.jar'],'Entrypoint':None},
        'HostConfig':{'ReadonlyRootfs':False,'FutureField':[None,'x'],'RestartPolicy':{'Name':'no'}},
        'Mounts':[], 'State':{'Pid':55,'StartedAt':'2026-10-06T00:00:00Z',
        'FinishedAt':'0001-01-01T00:00:00Z','Running':True,'Paused':False,'Restarting':False,
        'Dead':False,'OOMKilled':False,'ExitCode':0}}
def scope():
    value=full()
    return {'id':CID,'name':'/owned-backend','image':IMAGE,'mounts':[],
        'restartPolicy':{'Name':'no'},'state':{k:v for k,v in value['State'].items() if k!='Pid'}}
def raw(value):return json.dumps([value]).encode()

class CurrentArtifactSource(unittest.TestCase):
    """Real file/FD tests; the proc root link is an explicit source boundary."""
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(prefix='owned-jar-',dir='/control')
        self.root=Path(self.tmp.name);self.proc=self.root/'proc';self.proc.mkdir()
        self.fs=self.root/'fs';self.fs.mkdir();self.folder=self.fs/'yudao-server';self.folder.mkdir()
        self.jar=self.folder/'app.jar';self.bytes=b'actual-current-artifact\0private-not-emitted'*4096
        self.jar.write_bytes(self.bytes);self.jar.chmod(0o644)
        (self.proc/'root').symlink_to(self.fs,target_is_directory=True)
        self.fd=os.open(self.proc,os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW)
    def tearDown(self):os.close(self.fd);self.tmp.cleanup()
    def source(self):return launch.NativeJarSource(self.fd,deadline=time.monotonic()+3)
    def readonly_source(self):
        (self.proc/'root').unlink();(self.proc/'root').symlink_to('/original',target_is_directory=True)
        return self.source()
    def test_retained_readonly_source_survives_proc_entry_loss(self):
        actual=Path('/original/yudao-server/app.jar');before=actual.stat().st_atime_ns
        with self.readonly_source() as source:
            original=source.observe();self.assertIs(original['readOnlyFilesystem'],True)
            retained=source.retain();(self.proc/'root').unlink()
        try:
            result=retained.observe(deadline=time.monotonic()+3)
            self.assertEqual(result['sha256'],original['sha256']);self.assertEqual(result['identity'],original['identity'])
            self.assertIs(result['retainedSource'],True);self.assertIs(result['coldStartConfirmed'],False)
            self.assertEqual(before,actual.stat().st_atime_ns)
        finally:retained.close()
    def test_retained_requires_observation_and_real_readonly_filesystem(self):
        with self.source() as source:
            with self.assertRaises(launch.LaunchObservationError):source.retain()
            source.observe()
            with self.assertRaises(launch.LaunchObservationError):source.retain()
    def test_retained_is_once_only_bounded_and_closed_refuses(self):
        with self.readonly_source() as source:
            source.observe();retained=source.retain()
            try:
                with self.assertRaises(launch.LaunchObservationError):source.retain()
                with self.assertRaises(launch.LaunchObservationError):retained.observe(deadline=time.monotonic()-1)
            finally:retained.close()
            with self.assertRaises(launch.LaunchObservationError):retained.assert_unchanged(deadline=time.monotonic()+3)
    def test_retained_tail_is_metadata_only_and_rejects_file_signature_drift(self):
        with self.readonly_source() as source:
            source.observe();retained=source.retain()
        try:
            with patch.object(launch.os,'read',side_effect=AssertionError('tail content read forbidden')), \
                 patch.object(launch.os,'pread',side_effect=AssertionError('tail content read forbidden')):
                retained.assert_unchanged(deadline=time.monotonic()+3)
            original_stat=os.stat
            def changed(path,*args,**kwargs):
                info=original_stat(path,*args,**kwargs)
                if path=='app.jar':
                    import types
                    names=('st_dev','st_ino','st_mode','st_uid','st_gid','st_nlink','st_size','st_mtime_ns','st_ctime_ns')
                    info=types.SimpleNamespace(**{name:getattr(info,name) for name in names});info.st_ino+=1
                return info
            with patch.object(launch.os,'stat',side_effect=changed):
                with self.assertRaises(launch.LaunchObservationError):retained.assert_unchanged(deadline=time.monotonic()+3)
        finally:retained.close()
    def test_retained_survives_actual_process_exit(self):
        """Actual proc magic link and process exit; child is not a Java server."""
        import select
        ready_read,ready_write=os.pipe();stop_read,stop_write=os.pipe();child=os.fork()
        if child==0:
            try:
                os.close(ready_read);os.close(stop_write)
                os.chroot('/original');os.chdir('/')
                os.write(ready_write,b'R');os.close(ready_write)
                os.read(stop_read,1)
                os._exit(0)
            except BaseException:os._exit(71)
        os.close(ready_write);os.close(stop_read);proc=None;retained=None;waited=False
        try:
            self.assertTrue(select.select([ready_read],[],[],2)[0])
            self.assertEqual(os.read(ready_read,1),b'R')
            proc=os.open('/proc/'+str(child),os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW)
            with launch.NativeJarSource(proc,deadline=time.monotonic()+3) as source:
                original=source.observe();retained=source.retain()
                os.close(stop_write);stop_write=None
                self.assertEqual(os.waitpid(child,0),(child,0));waited=True
                self.assertFalse(Path('/proc/'+str(child)).exists())
            result=retained.observe(deadline=time.monotonic()+3)
            self.assertEqual(result['identity'],original['identity'])
            self.assertEqual(result['sha256'],original['sha256'])
            self.assertIs(result['coldStartConfirmed'],False)
        finally:
            if retained is not None:retained.close()
            if proc is not None:os.close(proc)
            if stop_write is not None:os.close(stop_write)
            os.close(ready_read)
            if not waited:os.waitpid(child,0)
    def test_retained_partial_duplicate_failure_closes_owned_descriptors(self):
        with self.readonly_source() as source:
            source.observe();native_dup=os.dup;made=[]
            def duplicate(fd):
                if made:raise OSError('private failure must not escape')
                made.append(native_dup(fd));return made[0]
            with patch.object(launch.os,'dup',side_effect=duplicate):
                with self.assertRaisesRegex(launch.LaunchObservationError,'^ORIGINAL_LAUNCH_ARTIFACT_RETAIN_UNAVAILABLE$'):
                    source.retain()
            self.assertEqual(len(made),1)
            with self.assertRaises(OSError):os.fstat(made[0])
            source.assert_unchanged()
    def test_actual_bytes_identity_and_no_startup_claim(self):
        before=self.jar.stat().st_atime_ns
        with self.source() as source:
            value=source.observe();source.assert_unchanged()
        self.assertEqual(value['sha256'],sha(self.bytes));self.assertEqual(value['bytes'],len(self.bytes))
        self.assertEqual(value['identity']['inode'],self.jar.stat().st_ino)
        self.assertIs(value['readOnlyFilesystem'],False)
        self.assertIs(value['loadedAtStartupBytesProven'],False)
        self.assertEqual(before,self.jar.stat().st_atime_ns)
        self.assertNotIn('private-not-emitted',json.dumps(value))
    def test_same_bytes_new_inode_refuses(self):
        with self.source() as source:
            source.observe();self.jar.rename(self.folder/'old-app.jar')
            self.jar.write_bytes(self.bytes);self.jar.chmod(0o644)
            with self.assertRaises(launch.LaunchObservationError):source.assert_unchanged()
    def test_root_link_retarget_refuses(self):
        with self.source() as source:
            source.observe();(self.proc/'root').unlink();other=self.root/'other';other.mkdir()
            (self.proc/'root').symlink_to(other,target_is_directory=True)
            with self.assertRaises(launch.LaunchObservationError):source.assert_unchanged()
    def test_jar_symlink_hardlink_permissions_or_empty_refuse(self):
        for case in ('symlink','hardlink','writable','empty'):
            with self.subTest(case=case):
                self.jar.unlink();self.jar.write_bytes(self.bytes);self.jar.chmod(0o644)
                if case=='symlink':
                    self.jar.rename(self.folder/'source.jar');self.jar.symlink_to(self.folder/'source.jar')
                elif case=='hardlink':os.link(self.jar,self.folder/'linked.jar')
                elif case=='writable':self.jar.chmod(0o666)
                else:self.jar.write_bytes(b'')
                with self.assertRaises(launch.LaunchObservationError):
                    with self.source() as source:source.observe()
    def test_fifo_refuses_without_waiting(self):
        self.jar.unlink();os.mkfifo(self.jar,0o644);before=time.monotonic()
        with self.assertRaises(launch.LaunchObservationError):
            with self.source() as source:source.observe()
        self.assertLess(time.monotonic()-before,1)
    def test_symlink_directory_refuses(self):
        self.folder.rename(self.fs/'elsewhere');self.folder.symlink_to(self.fs/'elsewhere',target_is_directory=True)
        with self.assertRaises(launch.LaunchObservationError):
            with self.source() as source:source.observe()
    def test_content_tail_is_not_reopened(self):
        with self.source() as source:
            source.observe()
            with patch.object(launch.os,'read',side_effect=AssertionError('tail must be stat-only')):
                source.assert_unchanged()
    def test_read_drift_or_expired_budget_refuses(self):
        read=os.read;changed=[]
        def drift(fd,count):
            raw=read(fd,count)
            if raw and not changed:
                self.jar.write_bytes(self.bytes[:-1]+b'x')
                # tmpfs timestamp precision can place both writes in one tick;
                # this case specifically injects a visible metadata change.
                stamp=self.jar.stat();os.utime(self.jar,ns=(stamp.st_atime_ns,stamp.st_mtime_ns+1000000000))
                changed.append(1)
            return raw
        with self.source() as source,patch.object(launch.os,'read',side_effect=drift):
            with self.assertRaises(launch.LaunchObservationError):source.observe()
        with self.assertRaises(launch.LaunchObservationError):
            with launch.NativeJarSource(self.fd,deadline=time.monotonic()-1) as source:source.observe()
    def test_actual_proc_namespaces_are_part_of_identity(self):
        observed=launch.process_snapshot(os.getpid(),deadline=time.monotonic()+3)
        for name in ('ns/pid','ns/mnt'):
            self.assertEqual(observed[name.replace('/','_')+'Sha256'],sha(os.fsencode(os.readlink('/proc/self/'+name))))
    def run_observer(self,tail=None,*,held_body=None):
        value=full();process={'pid':55,'parentPid':1,'startTicks':10}
        process.update(ns_pidSha256=sha(b'pid:[77]'),ns_mntSha256=sha(b'mnt:[88]'))
        self.observer_value=value;self.observer_process=process
        groups=('0::/docker/'+CID+'\n').encode();argv=b'java\0-jar\0app.jar\0'
        for name,data in (('cgroup',groups),('cmdline',argv)):
            (self.proc/name).write_bytes(data);process[name+'Sha256']=sha(data)
        for name,target in (('exe','/usr/bin/java'),('cwd','/yudao-server')):
            (self.proc/name).symlink_to(target);process[name+'Sha256']=sha(os.fsencode(target))
        actual_open=os.open;calls=[];host=Path('/proc/sys/kernel/random/boot_id').read_text().strip()
        def opened(path,*args,**kwargs):
            if path=='/proc/55':return os.dup(self.fd)
            return actual_open(path,*args,**kwargs)
        def inspect(*args,**kwargs):
            calls.append(1)
            if len(calls)==2 and tail:tail()
            return raw(value)
        # os is shared by the real secure-claim reader. Keep the explicit proc
        # mapping, without a Mock call-history for every real journal open.
        with patch.object(launch.os,'open',new=opened), \
             patch.object(launch,'process_snapshot',return_value=process), \
             patch.object(launch,'_bounded_command',side_effect=inspect):
            if held_body is not None:
                with launch.hold_original_launch('backend',CID,IMAGE,host,deadline=time.monotonic()+5) as held:
                    return held_body(held)
            return launch.observe_original_launch('backend',CID,IMAGE,host,deadline=time.monotonic()+5)
    def test_running_observer_connects_actual_artifact(self):
        result=self.run_observer()
        self.assertEqual(result['currentJar']['sha256'],sha(self.bytes))
        self.assertIs(result['loadedAtStartupBytesProven'],False)
    def test_last_running_inspect_replacing_artifact_refuses(self):
        def replace():
            self.jar.rename(self.folder/'old-last.jar');self.jar.write_bytes(self.bytes);self.jar.chmod(0o644)
        with self.assertRaises(launch.LaunchObservationError):self.run_observer(tail=replace)
    def test_held_launch_binds_readonly_source_and_closes_at_scope_exit(self):
        self.readonly_source();kept=[]
        def body(held):
            original,artifact=held;kept.append(artifact)
            self.assertIs(original['currentJar']['readOnlyFilesystem'],True)
            (self.proc/'root').unlink()
            observed=artifact.observe(deadline=time.monotonic()+3)
            self.assertEqual(observed['identity'],original['currentJar']['identity'])
            self.assertEqual(observed['sha256'],original['currentJar']['sha256'])
            self.assertIs(observed['coldStartConfirmed'],False)
        self.run_observer(held_body=body)
        with self.assertRaises(launch.LaunchObservationError):kept[0].assert_unchanged(deadline=time.monotonic()+3)
    def test_held_launch_rejects_writable_source_before_returning(self):
        with self.assertRaises(launch.LaunchObservationError):
            self.run_observer(held_body=lambda _:self.fail('writable artifact returned'))
    def test_held_launch_rejects_final_full_observation_drift(self):
        self.readonly_source();observe=launch.observe_original_launch;calls=[]
        def drift(*args,**kwargs):
            value=observe(*args,**kwargs);calls.append(1)
            if len(calls)==2:value['inspect']['digests']['launchSha256']='f'*64
            return value
        with patch.object(launch,'observe_original_launch',side_effect=drift):
            with self.assertRaises(launch.LaunchObservationError):
                self.run_observer(held_body=lambda _:self.fail('drift artifact returned'))
        self.assertEqual(len(calls),2)
    def test_held_launch_body_exception_closes_only_owned_descriptors(self):
        self.readonly_source();kept=[]
        def body(held):kept.append(held[1]);raise ValueError('explicit test body failure')
        with self.assertRaisesRegex(ValueError,'explicit test body failure'):self.run_observer(held_body=body)
        with self.assertRaises(launch.LaunchObservationError):kept[0].assert_unchanged(deadline=time.monotonic()+3)
        os.fstat(self.fd)

    def run_recovery_phase(self,body,instance='owned-recovery'):
        """Actual RO FDs/socket metadata; Docker/proc/JVM response boundary."""
        self.readonly_source();self.recovery_path=self.root/'identity.sock'
        self.recovery_socket=socket.socket(socket.AF_UNIX,socket.SOCK_STREAM)
        self.recovery_socket.bind(str(self.recovery_path));self.recovery_path.chmod(0o600)
        self.recovery_uuid='00000000-0000-0000-0000-000000000001';self.recovery_calls=[]
        self.recovery_wire_change=None
        m=importlib.import_module('formal_jvm_identity')
        def wire(binding,process,instance,artifact,host,*,deadline):
            self.recovery_calls.append('jvm')
            native=dict(pid=1,startTicks=process['startTicks'],hostBootId=host,
                pidNamespace='pid:[77]',mountNamespace='mnt:[88]')
            result=dict(schemaVersion='formal-maintenance-identity-response/v1',nonce='a'*32,
                identity=dict(native,jvmInstanceId=self.recovery_uuid,instanceId=instance,
                    artifactSha256=artifact,fullIdentity=False),codeSourceArtifactBound=True,
                readOnlyObservation=True,admissionChanged=False,loadedAtStartupBytesProven=False,
                fullIdentity=False,fullDrain=False,releaseReady=False,peerPid=binding['pid'])
            if self.recovery_wire_change:self.recovery_wire_change(result)
            return result
        def held_body(held):
            original,artifact=held;info=self.recovery_path.stat()
            binding=dict(path=str(self.recovery_path),identity=dict(device=info.st_dev,inode=info.st_ino),pid=55,uid=0,gid=0)
            checks=launch.OriginalLaunchRecoveryChecks(original,artifact,binding,instance,deadline=time.monotonic()+5)
            return body(checks,original,artifact,binding)
        try:
            with patch.object(m,'observe_jvm_identity',side_effect=wire):
                return self.run_observer(held_body=held_body)
        finally:self.recovery_socket.close()

    def stop_recovery_phase(self,checks):
        self.observer_value['State'].update(Pid=0,Running=False,FinishedAt='2026-10-06T00:01:00Z')
        runtime={key:self.observer_value['State'][key] for key in
            ('Running','Paused','Restarting','Dead','OOMKilled','ExitCode','StartedAt','FinishedAt')}
        result=checks.stopped(runtime,deadline=time.monotonic()+3)
        return runtime,result

    def start_recovery_phase(self,binding):
        self.observer_value['State'].update(Pid=55,Running=True,StartedAt='2026-10-06T00:02:00Z',
            FinishedAt='0001-01-01T00:00:00Z')
        self.observer_process['startTicks']=11
        self.recovery_uuid='00000000-0000-0000-0000-000000000002'
        self.recovery_socket.close();self.recovery_path.unlink()
        self.recovery_socket=socket.socket(socket.AF_UNIX,socket.SOCK_STREAM)
        self.recovery_socket.bind(str(self.recovery_path));self.recovery_path.chmod(0o600)
        info=self.recovery_path.stat()
        return dict(binding,identity=dict(device=info.st_dev,inode=info.st_ino))

    def run_cold_once(self,body,*,action_failure=False,unchanged_ticks=False,guard_after_intent=False,
                      health_failure=None,pin_swap_after_intent=None,source_failure=None):
        """Real claim/crypto/RO holder; explicit Docker/proc/JVM/thaw boundaries."""
        import formal_original_launch_capture as capture
        import formal_original_open_writer as writer
        import formal_split_capture as split
        dependency=DependencyHealth();dependency.setUp()
        self.addCleanup(dependency.doCleanups);self.addCleanup(dependency.tearDown)
        def held(checks,original,artifact,binding):
            dependency.held['backend']['id']=CID
            dependency.prepare_source_checks(original['currentJar']['sha256'])
            # Dependency restoration order and thaw remain explicit finite
            # boundary fixtures; no real SQL, host or service restoration.
            dependency.record_dependency_recovery_fixture()
            dependency.complete_cold_open_fixture()
            opened=writer.CompletedOriginalOpenChecks(dependency.claim,str(dependency.approval),str(dependency.key),
                dependency.keysha,dependency.expected,deadline=time.monotonic()+60)
            runtime,_=self.stop_recovery_phase(checks)
            self.assertEqual(runtime,dependency.held['backend']['state'])
            actions=[];guards=[];swaps=[]
            dependency.owned_source_calls=[]
            dependency.owned_health_failure=health_failure
            class Probe:
                def observe(self,role,record,seconds):
                    failure=dependency.owned_health_failure
                    if role=='redis' and (failure=='before-intent' or (failure=='after-intent' and
                            any(e['step']=='shared-backend-restore-intent' for e in dependency.claim.events))):
                        raise snapshot.FormalSnapshotError('OWNED_DEPENDENCY_NOT_HEALTHY')
                    if role=='backend' and failure=='backend-up':
                        raise snapshot.FormalSnapshotError('OWNED_ORIGINAL_BACKEND_NOT_UP')
                    return dependency.health(role)['proof']
            def guard():
                guards.append(1)
                pending=any(e['step']=='shared-backend-restore-intent' for e in dependency.claim.events)
                running=dependency.held['backend']['state']['Running']
                if source_failure=='host-before-intent' or (source_failure=='host-after-intent' and pending) or \
                        (source_failure=='host-after-start' and running):
                    unit=dependency.source_host_module.CLOSE_ORDER[0]
                    dependency.source_host_fixture.states[unit].update(active='active',sub='running',mainPid=99)
                if guard_after_intent and any(e['step']=='shared-backend-restore-intent' for e in dependency.claim.events):
                    raise snapshot.FormalSnapshotError('OWNED_GUARD_AFTER_INTENT')
                if pin_swap_after_intent and not swaps and any(e['step']=='shared-backend-restore-intent' for e in dependency.claim.events):
                    selected=dependency.claim.root/pin_swap_after_intent
                    saved=selected.read_bytes();selected.rename(dependency.claim.root/('owned-old-'+pin_swap_after_intent))
                    selected.write_bytes(saved);selected.chmod(0o600);swaps.append(1)
            def current(identity):
                return copy.deepcopy(next(row for row in list(dependency.services.values())+list(dependency.held.values()) if row['id']==identity))
            def nft(*args,**kwargs):
                dependency.owned_source_calls.append('ingress')
                pending=any(e['step']=='shared-backend-restore-intent' for e in dependency.claim.events)
                result=dict(phase='RECOVERY_DEPENDENCIES_PERMITTED',rulesSha256=dependency.rules_sha)
                if source_failure=='ingress-before-intent':result['phase']='CAPTURE_RESTRICTED'
                if source_failure=='ingress-after-intent' and pending:result['rulesSha256']='1'*64
                return result
            def callers(*args,**kwargs):
                dependency.owned_source_calls.append('external')
                if source_failure=='caller-before-intent' or (source_failure=='caller-after-start' and
                        dependency.held['backend']['state']['Running']):
                    raise snapshot.FormalSnapshotError('FORMAL_CALLER_NOT_STOPPED')
                return dict(state='KNOWN_EXTERNAL_CALLERS_STOPPED_PENDING_FULL_EXCLUSION',scopeSha256=dependency.external_sha)
            def start_runtime():
                self.start_recovery_phase(binding)
                if unchanged_ticks:self.observer_process['startTicks']=10
                dependency.held['backend']['state']={k:v for k,v in self.observer_value['State'].items() if k!='Pid'}
            def action(kind,identity):
                self.assertEqual((kind,identity),('start',CID))
                self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
                actions.append((kind,identity));start_runtime()
                if action_failure:raise snapshot.FormalSnapshotError('OWNED_START_INTERRUPTION')
            def invoke(reconcile_only=False):
                return capture.restore_original_backend_once(dependency.claim,opened,checks,guard=guard,
                    thaw_guard=lambda:None,timeout=60,reconcile_only=reconcile_only,
                    mysql=dependency.mysql,expected_receipt_sha256=dependency.digest,
                    expected_rules_sha256=dependency.rules_sha,expected_external_scope_sha256=dependency.external_sha)
            with patch.object(snapshot,'inspect_formal_container',side_effect=current), \
                 patch.object(snapshot,'run_formal_container_action',side_effect=action), \
                 patch.object(split,'_thaw_receipt',return_value={'nativeState':'NOT_REQUESTED'}), \
                 patch.object(snapshot,'_database_flags',side_effect=lambda *args:copy.deepcopy(dependency.flags)), \
                 patch.object(dependency.m,'NativeNetworkProbe',return_value=Probe()), \
                 patch.object(dependency.m,'_mysql_proof',side_effect=lambda *args:dependency.health('mysql')), \
                 patch.object(dependency.source_host_module,'NativeHostAdmission',return_value=dependency.source_host_fixture.native), \
                 patch.object(dependency.source_ingress,'assert_ingress',side_effect=nft), \
                 patch.object(dependency.source_callers,'guard_external_callers',side_effect=callers), \
                 patch.object(capture,'inspect_local_scope',side_effect=lambda identity,**kw:current(identity)):
                return body(invoke,dependency,opened,checks,actions,start_runtime,guards)
        return self.run_recovery_phase(held,instance='owned-backend')

    def assert_cold_source_refusal(self,failure,stage):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[] if stage!='after-start' else [('start',CID)])
            self.assertEqual(any(e['step']=='shared-backend-restore-intent' for e in dependency.claim.events),stage!='before-intent')
            self.assertFalse(any(e['step']=='shared-backend-restored' for e in dependency.claim.events))
        self.run_cold_once(body,source_failure=failure)

    def test_cold_once_actual_sources_host_before_intent_refuses(self):
        self.assert_cold_source_refusal('host-before-intent','before-intent')
    def test_cold_once_actual_sources_host_after_intent_refuses(self):
        self.assert_cold_source_refusal('host-after-intent','after-intent')
    def test_cold_once_actual_sources_host_after_start_retains(self):
        self.assert_cold_source_refusal('host-after-start','after-start')
    def test_cold_once_actual_sources_ingress_before_intent_refuses(self):
        self.assert_cold_source_refusal('ingress-before-intent','before-intent')
    def test_cold_once_actual_sources_ingress_after_intent_refuses(self):
        self.assert_cold_source_refusal('ingress-after-intent','after-intent')
    def test_cold_once_actual_sources_caller_before_intent_refuses(self):
        self.assert_cold_source_refusal('caller-before-intent','before-intent')
    def test_cold_once_actual_sources_caller_after_start_retains(self):
        self.assert_cold_source_refusal('caller-after-start','after-start')
    def test_cold_once_actual_sources_ingress_plan_inode_after_intent_refuses(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[])
            self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
        self.run_cold_once(body,pin_swap_after_intent='ingress-recovery-plan.json')
    def test_cold_once_actual_sources_ingress_rules_inode_after_intent_refuses(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[])
            self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
        self.run_cold_once(body,pin_swap_after_intent='ingress-recovery-installed.json')

    def test_cold_once_exact_original_starts_once_with_real_journal_and_source_checks(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            result=invoke();self.assertEqual(actions,[('start',CID)]);self.assertGreater(len(guards),3)
            self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restored')
            self.assertEqual(checks.phase,'STARTED');self.assertTrue(result['newProcessIdentityMatched'])
            self.assertTrue(result['dependencyHealthChecked'])
            self.assertTrue(result['registeredHostIngressCallerSourcesChecked'])
            self.assertGreater(dependency.owned_source_calls.count('ingress'),3)
            self.assertGreater(dependency.owned_source_calls.count('external'),3)
            for key in ('coldStartConfirmed','fullPreOpenGuard','releaseReady','fullDrain','fullHostFence'):self.assertIs(result[key],False)
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[('start',CID)])
        self.run_cold_once(body)

    def test_cold_once_dependency_failure_before_intent_prevents_action(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_DEPENDENCY_NOT_HEALTHY'):invoke()
            self.assertEqual(actions,[]);self.assertEqual(dependency.claim.events[-1]['step'],'boot-backend-open-restored')
        self.run_cold_once(body,health_failure='before-intent')

    def test_cold_once_dependency_failure_after_intent_retains_without_dispatch(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_DEPENDENCY_NOT_HEALTHY'):invoke()
            self.assertEqual(actions,[]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[])
        self.run_cold_once(body,health_failure='after-intent')

    def test_cold_once_backend_health_failure_allows_only_healthy_readonly_reconcile(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_ORIGINAL_BACKEND_NOT_UP'):invoke()
            self.assertEqual(actions,[('start',CID)]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
            with self.assertRaises(snapshot.FormalSnapshotError):invoke(reconcile_only=True)
            dependency.owned_health_failure=None
            result=invoke(reconcile_only=True);self.assertTrue(result['dependencyHealthChecked'])
            self.assertFalse(result['startDispatched']);self.assertEqual(actions,[('start',CID)])
        self.run_cold_once(body,health_failure='backend-up')

    def test_cold_once_receipt_same_bytes_new_inode_after_intent_prevents_dispatch(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_COLD_START_DEPENDENCY_INPUT_CHANGED'):invoke()
            self.assertEqual(actions,[]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
        self.run_cold_once(body,pin_swap_after_intent='shared-dependencies-health.json')

    def test_cold_once_database_original_same_bytes_new_inode_after_intent_prevents_dispatch(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_COLD_START_DEPENDENCY_INPUT_CHANGED'):invoke()
            self.assertEqual(actions,[]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
        self.run_cold_once(body,pin_swap_after_intent='database-original.json')

    def test_cold_once_flags_change_during_final_original_source_retains_intent(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            actual=checks.started
            def changed(*args,**kwargs):
                result=actual(*args,**kwargs);dependency.flags['readOnly']=0;return result
            with patch.object(checks,'started',new=changed):
                with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_COLD_START_DATABASE_FLAGS_CHANGED'):invoke()
            self.assertEqual(actions,[('start',CID)]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
            with self.assertRaises(snapshot.FormalSnapshotError):invoke(reconcile_only=True)
            self.assertEqual(actions,[('start',CID)])
        self.run_cold_once(body)

    def test_cold_once_changed_open_refuses_before_any_intent_or_action(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            dependency.target.write_bytes(dependency.closed)
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[]);self.assertEqual(dependency.claim.events[-1]['step'],'boot-backend-open-restored')
        self.run_cold_once(body)

    def test_cold_once_guard_after_intent_retains_without_dispatch(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_GUARD_AFTER_INTENT'):invoke()
            self.assertEqual(actions,[]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[])
        self.run_cold_once(body,guard_after_intent=True)

    def test_cold_once_interrupted_start_only_explicit_readonly_reconcile_can_complete(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_START_INTERRUPTION'):invoke()
            self.assertEqual(actions,[('start',CID)]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            result=invoke(reconcile_only=True);self.assertEqual(actions,[('start',CID)])
            self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restored');self.assertFalse(result['startDispatched'])
        self.run_cold_once(body,action_failure=True)

    def test_cold_once_explicit_reconcile_never_dispatches_start(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            dependency.claim.record('shared-backend-restore-intent',dependency.cold_start_intent());start_runtime()
            result=invoke(reconcile_only=True);self.assertEqual(actions,[]);self.assertFalse(result['startDispatched'])
            with self.assertRaises(snapshot.FormalSnapshotError):invoke(reconcile_only=True)
            self.assertEqual(actions,[])
        self.run_cold_once(body)

    def test_cold_once_uuid_change_without_new_ticks_retains_unresolved_intent(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[('start',CID)]);self.assertEqual(dependency.claim.events[-1]['step'],'shared-backend-restore-intent')
            with self.assertRaises(snapshot.FormalSnapshotError):invoke(reconcile_only=True)
            self.assertEqual(actions,[('start',CID)])
        self.run_cold_once(body,unchanged_ticks=True)

    def test_cold_once_closed_holder_refuses_before_dispatch(self):
        def body(invoke,dependency,opened,checks,actions,start_runtime,guards):
            checks.retained.close()
            with self.assertRaises(snapshot.FormalSnapshotError):invoke()
            self.assertEqual(actions,[]);self.assertEqual(dependency.claim.events[-1]['step'],'boot-backend-open-restored')
        self.run_cold_once(body)

    def test_recovery_phase_stopped_matches_original_and_live_retained_source(self):
        def body(checks,original,artifact,binding):
            runtime,result=self.stop_recovery_phase(checks)
            self.assertTrue(result['stoppedLaunchMatched']);self.assertFalse(result['coldStartConfirmed'])
            self.assertEqual(checks.stopped(runtime,deadline=time.monotonic()+3)['originalArtifactSha256'],original['currentJar']['sha256'])
            self.assertEqual(self.recovery_calls,['jvm'])
        self.run_recovery_phase(body)

    def test_recovery_phase_started_requires_observed_stop(self):
        def body(checks,original,artifact,binding):
            with self.assertRaises(launch.LaunchObservationError):checks.started(binding,deadline=time.monotonic()+3)
            self.assertEqual(self.recovery_calls,['jvm'])
        self.run_recovery_phase(body)

    def test_recovery_phase_started_matches_new_jvm_and_pins_continuation(self):
        def body(checks,original,artifact,binding):
            self.stop_recovery_phase(checks);current=self.start_recovery_phase(binding)
            result=checks.started(current,deadline=time.monotonic()+3)
            self.assertTrue(result['newProcessIdentityMatched']);self.assertTrue(result['originalLaunchMatched'])
            for key in ('coldStartConfirmed','fullDrain','fullHostFence','releaseReady'):self.assertIs(result[key],False)
            self.assertTrue(checks.started(current,deadline=time.monotonic()+3)['newProcessIdentityMatched'])
            with self.assertRaises(launch.LaunchObservationError):self.stop_recovery_phase(checks)
        self.run_recovery_phase(body)

    def test_recovery_phase_uuid_without_new_process_or_same_uuid_refuses(self):
        def body(checks,original,artifact,binding):
            self.stop_recovery_phase(checks);current=self.start_recovery_phase(binding)
            self.observer_process['startTicks']=10
            with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
            self.observer_process['startTicks']=11;self.recovery_uuid='00000000-0000-0000-0000-000000000001'
            with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
        self.run_recovery_phase(body)

    def test_recovery_phase_full_launch_or_socket_scope_drift_refuses(self):
        def body(checks,original,artifact,binding):
            self.stop_recovery_phase(checks);current=self.start_recovery_phase(binding)
            for key,value in (('path','/unapproved/control.sock'),('pid',66),('uid',1000),('gid',1000)):
                with self.subTest(key=key):
                    changed=dict(current,**{key:value})
                    with self.assertRaises(launch.LaunchObservationError):checks.started(changed,deadline=time.monotonic()+3)
            self.observer_value['HostConfig']['FutureField']=['changed']
            with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
            self.assertEqual(self.recovery_calls,['jvm'])
        self.run_recovery_phase(body)

    def test_recovery_phase_final_full_launch_drift_after_jvm_refuses(self):
        def body(checks,original,artifact,binding):
            self.stop_recovery_phase(checks);current=self.start_recovery_phase(binding)
            self.recovery_wire_change=lambda _:self.observer_value['HostConfig'].update(FutureField=['late'])
            with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
        self.run_recovery_phase(body)

    def test_recovery_phase_final_socket_signature_or_current_jar_drift_refuses(self):
        def body(checks,original,artifact,binding):
            self.stop_recovery_phase(checks);current=self.start_recovery_phase(binding);observe=launch.observe_original_launch
            def changed(*args,**kwargs):
                result=observe(*args,**kwargs);result['currentJar']['identity']['inode']+=1;return result
            with patch.object(launch,'observe_original_launch',side_effect=changed):
                with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
            self.recovery_wire_change=lambda _:self.recovery_path.chmod(0o666)
            with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
        self.run_recovery_phase(body)

    def test_recovery_phase_closed_holder_or_changed_stopped_runtime_refuses(self):
        def body(checks,original,artifact,binding):
            runtime,_=self.stop_recovery_phase(checks)
            self.observer_value['State']['FinishedAt']='2026-10-06T00:01:01Z'
            changed=dict(runtime,FinishedAt='2026-10-06T00:01:01Z')
            with self.assertRaises(launch.LaunchObservationError):checks.stopped(changed,deadline=time.monotonic()+3)
            artifact.close()
            with self.assertRaises(launch.LaunchObservationError):checks.stopped(runtime,deadline=time.monotonic()+3)
        self.run_recovery_phase(body)

    def test_recovery_phase_continuation_second_restart_or_proc_argv_drift_refuses(self):
        def body(checks,original,artifact,binding):
            self.stop_recovery_phase(checks);current=self.start_recovery_phase(binding)
            checks.started(current,deadline=time.monotonic()+3)
            self.observer_process['startTicks']=12;self.recovery_uuid='00000000-0000-0000-0000-000000000003'
            with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
            self.observer_process['startTicks']=11;self.recovery_uuid='00000000-0000-0000-0000-000000000002'
            self.observer_process['cmdlineSha256']='f'*64
            with self.assertRaises(launch.LaunchObservationError):checks.started(current,deadline=time.monotonic()+3)
        self.run_recovery_phase(body)

class StoppedProjection(unittest.TestCase):
    def stopped(self):
        value=full();value['State'].update(Pid=0,Running=False,FinishedAt='2026-10-06T00:01:00Z');return value
    def test_stopped_preserves_same_full_launch_sha_without_process(self):
        m=importlib.import_module('formal_original_launch')
        result=m.stopped_inspect_projection(raw(self.stopped()),CID,IMAGE)
        self.assertEqual(result['digests'],launch.inspect_projection(raw(full()),CID,IMAGE)['digests'])
        self.assertIs(result['state']['Running'],False);self.assertEqual(result['pid'],0)
        self.assertNotIn('never-emit-me',json.dumps(result))
    def test_stopped_private_semantics_remain_complete(self):
        m=importlib.import_module('formal_original_launch');base=self.stopped()
        first=m.stopped_inspect_projection(raw(base),CID,IMAGE)
        for field in ('Config','HostConfig','Path','Args','Mounts'):
            value=copy.deepcopy(base)
            if field in ('Config','HostConfig'):value[field]['Unknown']=['x',None,'x']
            elif field=='Path':value[field]='/other'
            elif field=='Args':value[field].reverse()
            else:value[field].append({'Type':'bind','Source':'/private','Destination':'/config','RW':False})
            self.assertNotEqual(first['digests']['launchSha256'],m.stopped_inspect_projection(raw(value),CID,IMAGE)['digests']['launchSha256'])
    def test_stopped_unstable_state_refuses(self):
        m=importlib.import_module('formal_original_launch')
        for key,value in (('Pid',True),('Pid',1),('Running',True),('Paused',True),('Dead',True),
                          ('Restarting',True),('OOMKilled',True),('ExitCode',1),('ExitCode',False),
                          ('StartedAt','x'*256),('FinishedAt','0001-01-01T00:00:00Z')):
            with self.subTest(key=key,value=value):
                item=self.stopped();item['State'][key]=value
                with self.assertRaises(launch.LaunchObservationError):m.stopped_inspect_projection(raw(item),CID,IMAGE)
    def test_stopped_invalid_json_refuses(self):
        m=importlib.import_module('formal_original_launch')
        for value in (b'[]',b'[{},{}]',b'[NaN]',b'[{"Id":"x","Id":"y"}]',b'\xff'):
            with self.assertRaises(launch.LaunchObservationError):m.stopped_inspect_projection(value,CID,IMAGE)
    def test_stopped_actual_transport_fixed_no_proc(self):
        m=importlib.import_module('formal_original_launch');calls=[]
        def command(argv,**kwargs):calls.append(argv);return raw(self.stopped())
        host=Path('/proc/sys/kernel/random/boot_id').read_text().strip()
        with patch.object(m,'_bounded_command',side_effect=command),patch.object(m,'process_snapshot',side_effect=AssertionError('no proc')):
            result=m.observe_stopped_original_launch('backend',CID,IMAGE,host,deadline=time.monotonic()+5)
        self.assertEqual(calls,[['/usr/bin/docker','--host','unix:///var/run/docker.sock','inspect','--type','container',CID]]*2)
        self.assertIs(result['processObserved'],False);self.assertIs(result['loadedAtStartupBytesProven'],False)
    def test_stopped_last_inspect_drift_refuses(self):
        m=importlib.import_module('formal_original_launch');calls=[]
        def command(*a,**k):
            calls.append(1);value=self.stopped()
            if len(calls)==2:value['Config']['Env'].reverse()
            return raw(value)
        host=Path('/proc/sys/kernel/random/boot_id').read_text().strip()
        with patch.object(m,'_bounded_command',side_effect=command):
            with self.assertRaisesRegex(launch.LaunchObservationError,'SOURCE_CHANGED'):
                m.observe_stopped_original_launch('backend',CID,IMAGE,host,deadline=time.monotonic()+5)

class ApprovedLaunch(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(prefix='owned-launch-',dir='/control');self.root=Path(self.tmp.name)
        self.binding={'operationId':'owned-launch','sourceFingerprint':'c'*64,
            'databaseUuid':'00000000-0000-0000-0000-000000000001','minioContainerId':'d'*64}
        self.claimroot=self.root/'claim';claim=snapshot.FormalMaintenanceClaim(self.claimroot,self.binding)
        self.original=scope();self.current=copy.deepcopy(self.original)
        original={'backend':self.original}
        for i,role in enumerate(('mysql','minio','redis','frontend','showroom','onlyoffice')):
            value=copy.deepcopy(self.original);value.update(id=('%064x'%(i+10)),name='/owned-'+role);original[role]=value
        original['minio']['id']=self.binding['minioContainerId']
        blobs={'shared-container-original.json':snapshot._canonical_bytes(original),
            'external-callers-original.json':b'{"privateFixture":"not real external scope"}',
            'shared-health-original.json':b'{"privateFixture":"not real health"}',
            'external-callers-health-original.json':b'{"privateFixture":"not real caller health"}'}
        hashes={}
        for name,value in blobs.items():claim._write(self.claimroot/name,value);hashes[name]=sha(value)
        for step,name,key in (('shared-container-scope','shared-container-original.json','scopeSha256'),
             ('external-caller-scope','external-callers-original.json','scopeSha256'),
             ('shared-health-scope','shared-health-original.json','sha256'),
             ('external-health-scope','external-callers-health-original.json','sha256')):
            claim.record(step,{key:hashes[name]})
        claim.record('maintenance-originals-bound',{'originalFileSha256':hashes});claim.close()
        self.request={'schemaVersion':'release-formal-capture-request/v10','action':'observe-running-original-launch',
            'claimRoot':str(self.claimroot),'claimBinding':self.binding,'totalSeconds':5,
            'hostBootId':Path('/proc/sys/kernel/random/boot_id').read_text().strip(),'role':'backend',
            'expectedLaunchSha256':launch.inspect_projection(raw(full()),CID,IMAGE)['digests']['launchSha256']}
        self.approval=self.root/'request.json';self.save()
    def tearDown(self):self.tmp.cleanup()
    def save(self):self.approval.write_bytes(snapshot._canonical_bytes(self.request));self.approval.chmod(0o600);self.digest=sha(self.approval.read_bytes())
    def retained(self):return {p.name:p.read_bytes() for p in self.claimroot.iterdir() if p.is_file()}
    def event(self,step,details):
        c=snapshot.FormalMaintenanceClaim(self.claimroot,self.binding,resume=True);c.record(step,details);c.close()
    def stopped(self):
        self.event('db-fence-intent',{});self.event('db-fenced',{})
        digest=sha((self.claimroot/'shared-container-original.json').read_bytes())
        intent={'scopeSha256':digest,'before':self.original['state'],'actionRequired':True}
        self.event('shared-backend-stop-intent',intent)
        self.current['state'].update(Running=False,FinishedAt='2026-10-06T00:01:00Z')
        self.event('shared-backend-stopped',{'scopeSha256':digest,'runtime':self.current['state'],
            'actionPerformed':True,'intentSha256':sha(snapshot._canonical_bytes(intent))})
        self.request['action']='observe-stopped-original-launch';self.save()
    def rewrite_stop_chain(self, *, required=True, before_change=None, runtime_change=None):
        previous=sha((self.claimroot/'binding.json').read_bytes());intent_digest=None
        for path in sorted(self.claimroot.glob('event-*')):
            value=json.loads(path.read_bytes());value['previousDigest']=previous
            if value['step']=='shared-backend-stop-intent':
                value['details']['actionRequired']=required
                if before_change:value['details']['before'].update(before_change)
                intent_digest=sha(snapshot._canonical_bytes(value['details']))
            if value['step']=='shared-backend-stopped':
                value['details']['intentSha256']=intent_digest
                if runtime_change:value['details']['runtime'].update(runtime_change)
            payload=snapshot._canonical_bytes(value);path.write_bytes(payload);previous=sha(payload)
        # These are valid hash chains; the refusal must be field grammar.
        claim=snapshot.FormalMaintenanceClaim(self.claimroot,self.binding,resume=True);claim.close()
    def observe(self,*a,**kwargs):
        value=full()
        if self.request['action']=='observe-stopped-original-launch':
            value['State'].update(self.current['state'],Pid=0)
            return {'schemaVersion':'formal-original-stopped-launch-observation/v1','role':'backend',
                'bootId':self.request['hostBootId'],'inspect':launch.stopped_inspect_projection(raw(value),CID,IMAGE),
                'processObserved':False,'releaseReady':False}
        return {'schemaVersion':'formal-original-launch-observation/v1','role':'backend',
            'bootId':self.request['hostBootId'],'inspect':launch.inspect_projection(raw(value),CID,IMAGE),
            'process':{'pid':55,'startTicks':7},'releaseReady':False}
    def run_client(self,observer=None,inspect=None,cli=False):
        m=importlib.import_module('formal_original_launch_capture')
        with patch.object(m,'inspect_local_scope',side_effect=inspect or (lambda *a,**k:copy.deepcopy(self.current))), \
             patch.object(launch,'observe_original_launch',side_effect=observer or self.observe), \
             patch.object(launch,'observe_stopped_original_launch',side_effect=observer or self.observe):
            if not cli:return m.run_approved_original_launch(str(self.approval),self.digest)
            capture=importlib.import_module('formal_capture_producer');out=io.StringIO()
            with contextlib.redirect_stdout(out):status=capture.main(['--request',str(self.approval),'--expected-request-sha256',self.digest,'--observe-original-launch'])
            self.assertNotIn('never-emit-me',out.getvalue());return status,json.loads(out.getvalue())
    def test_actual_running_claim_cli_retains_all_originals(self):
        before=self.retained();status,result=self.run_client(cli=True)
        self.assertEqual(status,0);self.assertEqual(result['state'],'ORIGINAL_LAUNCH_OBSERVED_ONLY')
        self.assertEqual(before,self.retained())
        for key in ('fullDrain','releaseReady','productionEvidence','serviceActionsPerformed','claimReleased','hostWritersVerified',
                    'coldStartConfirmed','originalLaunchVerified','fullRunnerCompleted'):
            self.assertIs(result[key],False)
    def test_actual_stopped_claim_retains_originals(self):
        self.stopped();before=self.retained();result=self.run_client()
        self.assertEqual(before,self.retained());self.assertIs(result['observation']['processObserved'],False)
    def jvm_request(self):
        self.request.update(schemaVersion='release-formal-capture-request/v11',action='observe-running-original-jvm',
            jvmBinding=dict(instanceId='owned-backend',artifactSha256='a'*64,
                socket=dict(path='/control/socket/control.sock',identity=dict(device=1,inode=2),pid=55,uid=1000,gid=1000)))
        self.save()
    def jvm_observer(self,*args,**kwargs):
        result=self.observe(*args,**kwargs)
        result['currentJar']=dict(sha256='a'*64,readOnlyFilesystem=True,loadedAtStartupBytesProven=False)
        return result
    def jvm_receipt(self):
        return dict(identity=dict(jvmInstanceId='00000000-0000-0000-0000-000000000002',
            instanceId='owned-backend',artifactSha256='a'*64,pid=55,startTicks=7,
            hostBootId=self.request['hostBootId'],pidNamespace='pid:[1]',mountNamespace='mnt:[2]',fullIdentity=False),
            peerPid=55,codeSourceArtifactBound=True,readOnlyObservation=True,admissionChanged=False,
            loadedAtStartupBytesProven=False,fullIdentity=False,fullDrain=False,releaseReady=False)
    def test_jvm_explicit_cli_binds_actual_sources_without_writes(self):
        self.jvm_request();before=self.retained();jvm=importlib.import_module('formal_jvm_identity')
        with patch.object(jvm,'_socket_signature',return_value=('owned-socket',)), \
             patch.object(jvm,'observe_jvm_identity',return_value=self.jvm_receipt()) as identity:
            code,result=self.run_client(observer=self.jvm_observer,cli=True)
        self.assertEqual(code,0);self.assertEqual(before,self.retained())
        self.assertEqual(result['jvmObservation'],self.jvm_receipt());identity.assert_called_once()
        call=identity.call_args
        self.assertEqual(call.args,(self.request['jvmBinding']['socket'],{'pid':55,'startTicks':7},
            'owned-backend','a'*64,self.request['hostBootId']))
        for key in ('fullDrain','releaseReady','coldStartConfirmed','serviceActionsPerformed','fullRunnerCompleted'):
            self.assertIs(result[key],False)
    def test_jvm_artifact_or_socket_scope_refuses_before_identity_read(self):
        self.jvm_request();jvm=importlib.import_module('formal_jvm_identity')
        for case in ('writable','artifact','peer'):
            with self.subTest(case=case):
                def observer(*args,**kwargs):
                    result=self.jvm_observer(*args,**kwargs)
                    if case=='writable':result['currentJar']['readOnlyFilesystem']=False
                    if case=='artifact':result['currentJar']['sha256']='b'*64
                    return result
                self.request['jvmBinding']['socket']['pid']=56 if case=='peer' else 55;self.save()
                with patch.object(jvm,'observe_jvm_identity') as identity:
                    with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=observer)
                identity.assert_not_called()
    def test_jvm_read_failure_uses_fixed_error_and_retains_claim(self):
        self.jvm_request();before=self.retained();jvm=importlib.import_module('formal_jvm_identity')
        with patch.object(jvm,'_socket_signature',return_value=('owned-socket',)), \
             patch.object(jvm,'observe_jvm_identity',side_effect=jvm.JvmIdentityError('PRIVATE_DO_NOT_EMIT')) as identity:
            code,result=self.run_client(observer=self.jvm_observer,cli=True)
        identity.assert_called_once();self.assertEqual(code,1)
        self.assertEqual(result['code'],'FORMAL_LAUNCH_JVM_SOURCE_UNAVAILABLE')
        self.assertNotIn('PRIVATE_DO_NOT_EMIT',json.dumps(result));self.assertEqual(before,self.retained())
    def test_jvm_final_full_launch_drift_after_identity_refuses(self):
        self.jvm_request();jvm=importlib.import_module('formal_jvm_identity');calls=[]
        def observer(*args,**kwargs):
            calls.append(1);result=self.jvm_observer(*args,**kwargs)
            if len(calls)==2:result['inspect']['digests']['launchSha256']='b'*64
            return result
        with patch.object(jvm,'_socket_signature',return_value=('owned-socket',)), \
             patch.object(jvm,'observe_jvm_identity',return_value=self.jvm_receipt()) as identity:
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=observer)
        identity.assert_called_once();self.assertEqual(len(calls),2)
    def test_jvm_socket_tail_drift_refuses(self):
        self.jvm_request();jvm=importlib.import_module('formal_jvm_identity')
        with patch.object(jvm,'_socket_signature',side_effect=[('owned-socket',),('changed-socket',)]), \
             patch.object(jvm,'observe_jvm_identity',return_value=self.jvm_receipt()) as identity:
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=self.jvm_observer)
        identity.assert_called_once()
    def test_jvm_request_requires_explicit_observer_flag(self):
        self.jvm_request();capture=importlib.import_module('formal_capture_producer');out=io.StringIO()
        with contextlib.redirect_stdout(out):
            code=capture.main(['--request',str(self.approval),'--expected-request-sha256',self.digest])
        self.assertEqual(code,1)
        self.assertEqual(json.loads(out.getvalue())['code'],'FORMAL_CAPTURE_EXPLICIT_LAUNCH_OBSERVER_REQUIRED')
    def test_private_mode_refuses(self):
        self.approval.chmod(0o644)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_private_symlink_refuses(self):
        dest=self.root/'retained';self.approval.rename(dest);self.approval.symlink_to(dest)
        with self.assertRaises((snapshot.FormalSnapshotError,OSError)):self.run_client()
    def test_unapproved_hash_refuses(self):
        self.digest='0'*64
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_extra_and_unknown_role_refuse(self):
        original=copy.deepcopy(self.request)
        for change in ({'role':'test-backend'},{'totalSeconds':True},{'action':'start'},{'containerId':CID}):
            self.request={**original,**change};self.save()
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_foreign_claim_refuses(self):
        self.request['claimBinding']=dict(self.binding,operationId='foreign');self.save()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_running_fence_refuses_before_transport(self):
        self.event('db-fence-intent',{});calls=[]
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_LAUNCH_STAGE_INVALID'):
            self.run_client(observer=lambda *a,**k:calls.append(1))
        self.assertEqual(calls,[])
    def test_running_close_completed_allowed(self):
        # Does not attest the fixture v6/v8 semantics; only allowed event names.
        self.event('boot-backend-bound',{});self.event('boot-backend-close-intent',{});self.event('boot-backend-closed',{})
        self.run_client()
    def test_stopped_missing_completion_refuses(self):
        self.request['action']='observe-stopped-original-launch';self.save()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stopped_restoration_or_open_refuses(self):
        self.stopped();self.event('shared-recovery-intent',{})
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_LAUNCH_STAGE_INVALID'):self.run_client()
    def test_stopped_other_pending_action_refuses(self):
        self.stopped();self.event('shared-redis-stop-intent',{})
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stopped_pending_sql_capture_refuses_before_transport(self):
        self.stopped();self.event('split-sql-intent',{});calls=[]
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=lambda *a,**k:calls.append(1))
        self.assertEqual(calls,[])
    def test_running_pending_boot_close_refuses(self):
        self.event('boot-backend-bound',{});self.event('boot-backend-close-intent',{})
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stopped_bad_runtime_refuses(self):
        self.stopped();self.current['state']['ExitCode']=1
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stop_numeric_action_required_refuses(self):
        self.stopped();self.rewrite_stop_chain(required=1)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stop_numeric_before_running_refuses(self):
        self.stopped();self.rewrite_stop_chain(before_change={'Running':1})
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stop_boolean_before_exit_code_refuses(self):
        self.stopped();self.rewrite_stop_chain(before_change={'ExitCode':False})
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stop_numeric_completed_running_refuses(self):
        self.stopped();self.rewrite_stop_chain(runtime_change={'Running':0})
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_stop_boolean_completed_exit_code_refuses(self):
        self.stopped();self.rewrite_stop_chain(runtime_change={'ExitCode':False})
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
    def test_launch_sha_drift_refuses(self):
        self.request['expectedLaunchSha256']='1'*64;self.save()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_LAUNCH_SOURCE_CHANGED'):self.run_client()
    def test_source_failure_fixed_cli(self):
        def failed(*a,**k):raise launch.LaunchObservationError('private-do-not-emit')
        status,result=self.run_client(observer=failed,cli=True)
        self.assertEqual(status,1);self.assertEqual(result['code'],'FORMAL_LAUNCH_SOURCE_UNAVAILABLE')
    def test_actual_cli_never_enters_ordinary_reader(self):
        capture=importlib.import_module('formal_capture_producer')
        with patch.object(capture,'read_private',side_effect=AssertionError('ordinary read forbidden')):
            status,result=self.run_client(cli=True)
        self.assertEqual(status,0)
    def test_actual_cli_fifo_refuses_bounded(self):
        self.approval.unlink();os.mkfifo(self.approval,0o600)
        begin=time.monotonic();status,result=self.run_client(cli=True)
        self.assertEqual(status,1);self.assertLess(time.monotonic()-begin,1)
    def test_scope_fixed_local_transport(self):
        m=importlib.import_module('formal_original_launch_capture');commands=[]
        def transport(argv,**kwargs):commands.append(argv);return snapshot._canonical_bytes(self.current)
        with patch.dict(os.environ,{'DOCKER_HOST':'ssh://never-contact','DOCKER_CONTEXT':'foreign','PATH':'/foreign'}), \
             patch.object(launch,'_bounded_command',side_effect=transport):
            self.assertEqual(m.inspect_local_scope(CID,deadline=time.monotonic()+2),self.current)
        self.assertEqual(commands,[['/usr/bin/docker','--host','unix:///var/run/docker.sock','inspect',
            '--type','container','--format',snapshot.CONTAINER_INSPECT_TEMPLATE,CID]])
    def test_final_observer_replaces_other_original_equal_bytes(self):
        def changed(*a,**k):
            result=self.observe(*a,**k);p=self.claimroot/'external-callers-original.json'
            value=p.read_bytes();p.rename(self.claimroot/'retained-original');p.write_bytes(value);p.chmod(0o600);return result
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=changed)
    def test_final_inspect_changes_journal(self):
        calls=[]
        def changed(*a,**k):
            calls.append(1)
            if len(calls)==2:(self.claimroot/'event-000001.json').write_bytes(b'{}')
            return copy.deepcopy(self.current)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(inspect=changed)
    def test_final_observer_changes_request_identity(self):
        def changed(*a,**k):
            result=self.observe(*a,**k);value=self.approval.read_bytes()
            self.approval.rename(self.root/'retained-request');self.approval.write_bytes(value);self.approval.chmod(0o600);return result
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=changed)
    def test_final_observer_replaces_equal_journal_bytes(self):
        def changed(*a,**k):
            result=self.observe(*a,**k);path=self.claimroot/'event-000001.json';value=path.read_bytes()
            path.rename(self.claimroot/'retained-event');path.write_bytes(value);path.chmod(0o600);return result
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=changed)
    def test_final_claim_read_replaces_request_after_secure_sweep(self):
        m=importlib.import_module('formal_original_launch_capture')
        actual=snapshot.FormalMaintenanceClaim._read;canonical=snapshot._canonical_bytes
        at_final=[];done=[];reads=[]
        def result_ready(value):
            result=canonical(value)
            if type(value) is dict and value.get('state')=='ORIGINAL_LAUNCH_OBSERVED_ONLY':at_final.append(1)
            return result
        def changed(claim,path):
            result=actual(claim,path)
            if at_final and Path(path).name=='event-000005.json' and not done:
                reads.append(1)
                if len(reads)==2:
                    value=self.approval.read_bytes();self.approval.rename(self.root/'retained-last-request')
                    self.approval.write_bytes(value);self.approval.chmod(0o600);done.append(1)
            return result
        with patch.object(snapshot,'_canonical_bytes',side_effect=result_ready),patch.object(snapshot.FormalMaintenanceClaim,'_read',changed):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertEqual(done,[1])
    def test_last_claim_read_appends_valid_forbidden_event(self):
        actual=snapshot.FormalMaintenanceClaim._read;canonical=snapshot._canonical_bytes
        at_final=[];done=[];reads=[]
        def result_ready(value):
            result=canonical(value)
            if type(value) is dict and value.get('state')=='ORIGINAL_LAUNCH_OBSERVED_ONLY':at_final.append(1)
            return result
        def changed(claim,path):
            result=actual(claim,path)
            if at_final and Path(path).name=='event-000005.json' and not done:
                reads.append(1)
                if len(reads)==2:
                    value={'sequence':6,'step':'boot-backend-open-intent','details':{},'previousDigest':sha(result)}
                    p=self.claimroot/'event-000006.json';p.write_bytes(canonical(value));p.chmod(0o600);done.append(1)
            return result
        with patch.object(snapshot,'_canonical_bytes',side_effect=result_ready),patch.object(snapshot.FormalMaintenanceClaim,'_read',changed):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertEqual(done,[1])
    def test_final_observer_exhausts_budget(self):
        def expired(*a,**k):
            result=self.observe(*a,**k);snapshot.observation_deadline.set(time.monotonic()-1);return result
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=expired)
    def test_final_private_read_changes_claim(self):
        boot=importlib.import_module('formal_boot_state');read=boot._read_secure;done=[]
        finished=[]
        def observed(*a,**k):finished.append(1);return self.observe(*a,**k)
        def changed(path,*a,**k):
            result=read(path,*a,**k)
            if finished and Path(path)==self.claimroot/'external-callers-health-original.json' and not done:
                (self.claimroot/'event-000001.json').write_bytes(b'{}');done.append(1)
            return result
        with patch.object(boot,'_read_secure',side_effect=changed):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=observed)
        self.assertEqual(done,[1])
    def test_last_private_read_changes_previously_read_request(self):
        boot=importlib.import_module('formal_boot_state');read=boot._read_secure;done=[];after=[]
        def observed(*a,**k):after.append(1);return self.observe(*a,**k)
        def changed(path,*a,**k):
            result=read(path,*a,**k)
            if after and Path(path)==self.claimroot/'external-callers-health-original.json' and not done:
                saved=self.approval.read_bytes();self.approval.rename(self.root/'old-request')
                self.approval.write_bytes(saved);self.approval.chmod(0o600);done.append(1)
            return result
        # The private callback fires during the first final guard. Every guard
        # must detect the prior request pin itself, not rely on a later guard.
        m=importlib.import_module('formal_original_launch_capture')
        inspect_calls=[]
        def inspected(*a,**k):
            inspect_calls.append(1)
            if done:raise AssertionError('drift guard let the next Docker read execute')
            return copy.deepcopy(self.current)
        with patch.object(boot,'_read_secure',side_effect=changed):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client(observer=observed,inspect=inspected)
        self.assertEqual(done,[1]);self.assertEqual(inspect_calls,[1])
    def test_actual_unprivileged_refuses_before_read(self):
        module=importlib.import_module('formal_original_launch_capture');child=os.fork()
        if child==0:
            try:
                os.setgroups([]);os.setgid(65534);os.setuid(65534)
                module.run_approved_original_launch(str(self.approval),self.digest)
            except snapshot.FormalSnapshotError as e:os._exit(0 if e.code=='FORMAL_LAUNCH_ROOT_REQUIRED' else 80)
            except BaseException:os._exit(81)
            os._exit(82)
        _,status=os.waitpid(child,0);self.assertEqual(os.waitstatus_to_exitcode(status),0)

if __name__=='__main__':unittest.main()
