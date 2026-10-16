"""Actual Linux uid/process/socket/files; controlled server, not business proof."""
import contextlib
import hashlib
import importlib
import io
import json
import os
from pathlib import Path
import select
import shutil
import signal
import socket
import struct
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

LINUX = Path(__file__).parents[1] / 'linux'
sys.path.insert(0, str(LINUX))
import formal_snapshot_linux as snapshot
import formal_boot_state as boot
import formal_capture_producer as producer


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def process_identity(pid):
    stat = Path('/proc/%d/stat' % pid).read_text()
    return {'jvmInstanceId':'00000000-0000-0000-0000-000000000002',
            'instanceId':'owned-live-fixture', 'artifactSha256':'a'*64,
            'pid':int(Path('/proc/%d/status' % pid).read_text().split('NSpid:')[1].splitlines()[0].split()[-1]),
            'startTicks':int(stat[stat.rindex(')')+2:].split()[19]),
            'hostBootId':Path('/proc/sys/kernel/random/boot_id').read_text().strip(),
            'pidNamespace':os.readlink('/proc/%d/ns/pid' % pid),
            'mountNamespace':os.readlink('/proc/%d/ns/mnt' % pid), 'fullIdentity':False}


def read_frame(connection):
    def take(n):
        result=b''
        while len(result)<n:
            chunk=connection.recv(n-len(result))
            if not chunk:raise EOFError()
            result+=chunk
        return result
    return json.loads(take(struct.unpack('!I',take(4))[0]))


class NativeLiveControlTest(unittest.TestCase):
    def setUp(self):
        self.m = importlib.import_module('formal_live_control')
        self.assertEqual(os.geteuid(),0,'NATIVE_OWNED_ROOT_FIXTURE_REQUIRED')
        self.root=Path(tempfile.mkdtemp(prefix='live-',dir='/control'))
        self.root.chmod(0o755)
        self.sockdir=self.root/'socket';self.sockdir.mkdir(mode=0o700)
        os.chown(self.sockdir,1000,1000)
        self.sockpath=self.sockdir/'control.sock'
        self.bootdir=self.root/'boot';self.bootdir.mkdir(mode=0o755)
        self.target=self.bootdir/'state.json'
        self.binding={'operationId':'owned-live','sourceFingerprint':'b'*64,
                      'databaseUuid':'00000000-0000-0000-0000-000000000001','minioContainerId':'c'*64}
        self.claimpath=self.root/'claim'
        c=snapshot.FormalMaintenanceClaim(self.claimpath,self.binding)
        originals={}
        for name in boot.ORIGINALS:
            raw=b'{"fixture":"original boundary only"}\n'
            c._write(self.claimpath/name,raw);originals[name]=sha(raw)
        c.record('maintenance-originals-bound',{'originalFileSha256':originals});c.close()
        original=snapshot._canonical_bytes({'schemaVersion':'formal-maintenance-boot/v1',
                    'instanceId':'owned-live-fixture','artifactSha256':'a'*64,'state':'OPEN','epoch':'','phase':'NORMAL'})
        self.target.write_bytes(original);self.target.chmod(0o644)
        closed=snapshot._canonical_bytes(dict(json.loads(original),state='CLOSED',epoch='owned-live'))
        info=self.target.stat()
        v6={'schemaVersion':'release-formal-capture-request/v6','action':'close-durable-boot-state',
            'claimRoot':str(self.claimpath),'claimBinding':self.binding,'totalSeconds':30,
            'hostBootId':Path('/proc/sys/kernel/random/boot_id').read_text().strip(),
            'bootBinding':{'role':'backend','path':str(self.target),'instanceId':'owned-live-fixture',
                'artifactSha256':'a'*64,'originalOpenSha256':sha(original),'closedSha256':sha(closed),
                'epoch':'owned-live','expectedIdentity':{'device':info.st_dev,'inode':info.st_ino}}}
        v6path=self.root/'v6.json';v6path.write_bytes(snapshot._canonical_bytes(v6));v6path.chmod(0o600)
        boot.close_approved_boot_state(str(v6path),sha(v6path.read_bytes()))
        self.grantpath=self.root/'grant.json'
        self.grant={'token':'e'*64,'jvmInstanceId':'00000000-0000-0000-0000-000000000002',
                    'epoch':'owned-live','closedSha256':sha(closed),
                    'issuedAt':'2026-10-06T00:00:00Z','closeExpiresAt':'2026-10-06T00:05:00Z',
                    'observeExpiresAt':'2026-10-06T01:00:00Z'}
        self.grantpath.write_bytes(snapshot._canonical_bytes(self.grant));self.grantpath.chmod(0o600)
        self.request=None;self.server=None;self.server_mode='good'

    def tearDown(self):
        if self.server:
            os.kill(self.server,signal.SIGTERM);os.waitpid(self.server,0)
        shutil.rmtree(self.root)

    def start_server(self,mode='good'):
        ready_r,ready_w=os.pipe()
        self.server=os.fork()
        if self.server==0:
            try:
                os.close(ready_r);os.setgroups([]);os.setgid(1000);os.setuid(1000)
                listener=socket.socket(socket.AF_UNIX,socket.SOCK_STREAM)
                listener.bind(str(self.sockpath));os.chmod(self.sockpath,0o600);listener.listen(4)
                os.write(ready_w,b'1');os.close(ready_w)
                while True:
                    with listener.accept()[0] as conn:
                        incoming=read_frame(conn)
                        pid,uid,gid=struct.unpack('3i',conn.getsockopt(socket.SOL_SOCKET,socket.SO_PEERCRED,12))
                        value=self.response(incoming,pid,uid,gid)
                        if mode=='timeout':time.sleep(15);continue
                        if mode=='wrong-nonce':value['nonce']='0'*32
                        if mode=='wrong-identity':value['identity']['startTicks']+=1
                        if mode=='wrong-actor':value['operatorId']=18
                        if mode=='true-full':value['fullDrain']=True
                        if mode=='invalid-count':value['outstandingWork']=True
                        if mode=='echo-token':value['acceptedRootsSha256']=self.grant['token']
                        if mode=='identity-bool':value['identity']['fullIdentity']=0
                        if mode=='refuse':value={'schemaVersion':'formal-maintenance-control-refusal/v1','code':'TOKEN '+self.grant['token']}
                        if mode=='unresolved':value['unresolvedCodeCount']=1;value['unresolvedCodesSha256']='c'*64
                        if mode=='uncovered':value['uncoveredProducers']=['persistent-queues']
                        raw=snapshot._canonical_bytes(value)
                        if mode=='duplicate':raw=raw[:-2]+b',"releaseReady":false}\n'
                        if mode=='oversize':conn.sendall(struct.pack('!I',8193));continue
                        if mode=='truncated':conn.sendall(struct.pack('!I',len(raw))+raw[:8]);continue
                        conn.sendall(struct.pack('!I',len(raw))+raw)
            except BaseException:os._exit(80)
        os.close(ready_w)
        self.assertTrue(select.select([ready_r],[],[],5)[0]);self.assertEqual(os.read(ready_r,1),b'1');os.close(ready_r)
        info=self.sockpath.stat()
        self.request={'schemaVersion':'release-formal-capture-request/v8','action':'close-live-admission',
            'claimRoot':str(self.claimpath),'claimBinding':self.binding,'totalSeconds':5,
            'hostBootId':self.grant_host_boot(),'role':'backend',
            'control':{'socketPath':str(self.sockpath),'socketIdentity':{'device':info.st_dev,'inode':info.st_ino},
                'peerPid':self.server,'uid':1000,'gid':1000,'identity':process_identity(self.server),
                'operatorId':17,'tenantId':122,'grantFile':str(self.grantpath),
                'grantSha256':sha(self.grantpath.read_bytes())}}
        self.save_request()

    def grant_host_boot(self):
        return Path('/proc/sys/kernel/random/boot_id').read_text().strip()

    def response(self,incoming,pid,uid,gid):
        if (uid,gid)!=(1000,1000) or incoming['token']!=self.grant['token']:
            return {'code':'FIXTURE_PEER_OR_TOKEN_REJECTED'}
        return {'schemaVersion':'formal-maintenance-control-response/v1','nonce':incoming['nonce'],
            'identity':process_identity(os.getpid()),'operatorId':17,'tenantId':122,
            'epoch':'owned-live','closedSha256':self.grant['closedSha256'],'admissionOpen':False,
            'outstandingWork':1,'acceptedRootCount':1,'acceptedRootsSha256':'d'*64,'completedAcceptedRoots':0,
            'uncoveredProducers':[],'unresolvedCodeCount':0,'unresolvedCodesSha256':sha(b''),
            'connectionFailures':0,'lastConnectionFailure':None,'codeSourceArtifactBound':True,
            'durableClosedConfirmed':False,'fullIdentity':False,'fullDrain':False,'releaseReady':False}

    def save_request(self):
        self.approval=self.root/'v8.json'
        self.approval.write_bytes(snapshot._canonical_bytes(self.request));self.approval.chmod(0o600)
        self.approved=sha(self.approval.read_bytes())

    def run_client(self):
        return self.m.run_approved_live_control(str(self.approval),self.approved)

    def events(self):
        c=snapshot.FormalMaintenanceClaim(self.claimpath,self.binding,resume=True)
        try:return [e['step'] for e in c.events]
        finally:c.close()

    def assert_redacted(self):
        for p in self.claimpath.glob('*.json'):
            self.assertNotIn(self.grant['token'].encode(),p.read_bytes())

    def test_actual_uid_peer_and_retained_once_close(self):
        self.start_server();result=self.run_client()
        self.assertEqual(result['state'],'LIVE_ADMISSION_CLOSED_PENDING_FULL_DRAIN')
        self.assertEqual(result['observation']['outstandingWork'],1)
        self.assertEqual(self.events()[-2:],['live-backend-close-intent','live-backend-closed'])
        self.assertEqual(result['responseSha256'],sha((self.claimpath/'live-backend-close-response.json').read_bytes()))
        self.assertTrue(result['liveAdmissionClosed']);self.assertTrue(result['durableClosedConfirmed'])
        for key in ('fullDrain','fullIdentity','releaseReady','productionEvidence','claimReleased'):
            self.assertIs(result[key],False)
        self.assert_redacted()
        before={p.name:p.read_bytes() for p in self.claimpath.glob('*') if p.is_file()}
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertEqual(before,{p.name:p.read_bytes() for p in self.claimpath.glob('*') if p.is_file()})

    def test_explicit_observe_keeps_all_close_original_bytes(self):
        self.start_server();self.run_client()
        before={p.name:p.read_bytes() for p in self.claimpath.glob('*') if p.is_file()}
        self.request['action']='observe-live-admission';self.save_request()
        result=self.run_client()
        self.assertEqual(result['state'],'LIVE_ADMISSION_OBSERVED_ONLY')
        self.assertTrue(result['closeCompletionConfirmed'])
        self.assertEqual(before,{p.name:p.read_bytes() for p in self.claimpath.glob('*') if p.is_file()})

    def test_observe_without_attempt_refuses_before_connection(self):
        self.start_server();self.request['action']='observe-live-admission';self.save_request()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertNotIn('live-backend-close-intent',self.events())

    def invalid_response(self,mode):
        self.start_server(mode)
        with self.assertRaises(snapshot.FormalSnapshotError) as error:self.run_client()
        self.assertNotIn(self.grant['token'],str(error.exception))
        self.assertEqual(self.events()[-1],'live-backend-close-intent')
        self.assertFalse((self.claimpath/'live-backend-close-response.json').exists());self.assert_redacted()

    def test_wrong_nonce(self):self.invalid_response('wrong-nonce')
    def test_wrong_jvm_identity(self):self.invalid_response('wrong-identity')
    def test_wrong_operator(self):self.invalid_response('wrong-actor')
    def test_full_drain_claim_refused(self):self.invalid_response('true-full')
    def test_boolean_count_refused(self):self.invalid_response('invalid-count')
    def test_duplicate_response_fields_refused(self):self.invalid_response('duplicate')
    def test_oversized_frame_refused(self):self.invalid_response('oversize')
    def test_truncated_frame_refused(self):self.invalid_response('truncated')
    def test_secret_bearing_server_refusal_redacted(self):self.invalid_response('refuse')
    def test_secret_echo_in_valid_digest_field_refused(self):self.invalid_response('echo-token')
    def test_identity_boolean_type_is_strict(self):self.invalid_response('identity-bool')

    def test_timeout_reaps_only_owned_child_and_never_retries(self):
        self.start_server('timeout');self.request['totalSeconds']=1;self.save_request()
        start=time.monotonic()
        real_fork=os.fork;children=[]
        def record_child():
            pid=real_fork()
            if pid:children.append(pid)
            return pid
        with patch.object(self.m.os,'fork',side_effect=record_child):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertLess(time.monotonic()-start,4)
        self.assertEqual(self.events()[-1],'live-backend-close-intent')
        self.assertEqual(len(children),1)
        with self.assertRaises(ChildProcessError):os.waitpid(children[0],os.WNOHANG)
        os.kill(self.server,0)

    def test_changed_grant_refused_before_intent(self):
        self.start_server();self.grantpath.write_bytes(b'{}')
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertNotIn('live-backend-close-intent',self.events())

    def test_changed_peer_pin_refused_before_intent(self):
        self.start_server();self.request['control']['identity']['startTicks']+=1;self.save_request()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertNotIn('live-backend-close-intent',self.events())

    def test_symlink_approval_refused_before_intent(self):
        self.start_server();saved=self.root/'saved.json';self.approval.rename(saved);self.approval.symlink_to(saved)
        with self.assertRaises((snapshot.FormalSnapshotError,OSError)):self.run_client()
        self.assertNotIn('live-backend-close-intent',self.events())

    def test_durable_closed_changed_refuses_before_intent(self):
        self.start_server();self.target.write_bytes(b'{}')
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertNotIn('live-backend-close-intent',self.events())

    def test_database_fence_started_refuses_late_close(self):
        self.start_server()
        c=snapshot.FormalMaintenanceClaim(self.claimpath,self.binding,resume=True)
        c.record('db-fence-intent',{});c.close()
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertNotIn('live-backend-close-intent',self.events())

    def test_real_unprivileged_client_refuses_before_private_read(self):
        self.start_server();pid=os.fork()
        if pid==0:
            try:
                os.setgroups([]);os.setgid(65534);os.setuid(65534)
                self.run_client()
            except snapshot.FormalSnapshotError as error:
                os._exit(0 if error.code=='FORMAL_LIVE_ROOT_REQUIRED' else 80)
            except BaseException:os._exit(81)
            os._exit(82)
        _,status=os.waitpid(pid,0);self.assertEqual(os.waitstatus_to_exitcode(status),0)
        self.assertNotIn('live-backend-close-intent',self.events())

    def test_socket_replaced_refuses_before_intent(self):
        self.start_server();self.sockpath.rename(self.sockdir/'displaced.sock')
        self.sockpath.write_bytes(b'not a socket');self.sockpath.chmod(0o600);os.chown(self.sockpath,1000,1000)
        with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertNotIn('live-backend-close-intent',self.events())

    def test_last_transport_original_drift_retains_only_intent(self):
        self.start_server();real=self.m._exchange_as_owner
        def drift(*args,**kwargs):
            raw=real(*args,**kwargs);(self.claimpath/'shared-container-original.json').write_bytes(b'{}');return raw
        with patch.object(self.m,'_exchange_as_owner',side_effect=drift):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertEqual(self.events()[-1],'live-backend-close-intent')
        self.assertFalse((self.claimpath/'live-backend-close-response.json').exists())

    def test_last_transport_callback_grant_drift_retains_only_intent(self):
        self.start_server();real=self.m._exchange_as_owner
        def drift(*args,**kwargs):
            raw=real(*args,**kwargs);self.grantpath.write_bytes(b'{}');return raw
        with patch.object(self.m,'_exchange_as_owner',side_effect=drift):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.assertEqual(self.events()[-1],'live-backend-close-intent')
        self.assertFalse((self.claimpath/'live-backend-close-response.json').exists())

    def test_pending_close_is_observable_without_replaying_close(self):
        self.start_server();real=self.m._exchange_as_owner
        def lost(*args,**kwargs):
            real(*args,**kwargs);raise snapshot.FormalSnapshotError('FORMAL_LIVE_DELIVERY_UNCONFIRMED')
        with patch.object(self.m,'_exchange_as_owner',side_effect=lost):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()
        self.request['action']='observe-live-admission';self.save_request()
        result=self.run_client()
        self.assertFalse(result['closeCompletionConfirmed'])
        self.assertEqual(self.events()[-1],'live-backend-close-intent')

    def test_last_observe_callback_changed_completion_refuses(self):
        self.start_server();self.run_client()
        self.request['action']='observe-live-admission';self.save_request()
        real=self.m._exchange_as_owner
        def drift(*args,**kwargs):
            raw=real(*args,**kwargs)
            (self.claimpath/'live-backend-close-response.json').write_bytes(b'{}')
            return raw
        with patch.object(self.m,'_exchange_as_owner',side_effect=drift):
            with self.assertRaises(snapshot.FormalSnapshotError):self.run_client()

    def test_standard_capture_cli_v8_routes_real_client(self):
        self.start_server();out=io.StringIO()
        with contextlib.redirect_stdout(out):
            status=producer.main(['--request',str(self.approval),'--expected-request-sha256',self.approved])
        self.assertEqual(status,0,out.getvalue());self.assertEqual(json.loads(out.getvalue())['state'],'LIVE_ADMISSION_CLOSED_PENDING_FULL_DRAIN')
        self.assertNotIn(self.grant['token'],out.getvalue())


if __name__=='__main__':unittest.main()
