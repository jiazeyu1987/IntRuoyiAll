"""Run only inside a task-owned --network none container with NET_ADMIN/SYS_ADMIN.

Real Linux namespaces exercise INPUT, IPv6, established TCP, Docker-like DNAT
and direct forwarding, host OUTPUT, drift, retained uncertain actions and exact
removal. This is neither a live-server nor a Docker-daemon compatibility proof.
"""
import hashlib
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
import threading
import time

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_ingress_control as ingress
import formal_snapshot_linux as snapshot


def run(args,raw=None):
    result=subprocess.run(args,input=raw,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=10)
    if result.returncode:raise RuntimeError((args,result.returncode,result.stderr.decode()))
    return result.stdout


def ip(*args):return run(['ip',*args])
def nft(text):return run(['/usr/sbin/nft','-f','-'],text.encode())


SERVER='''import socket,threading,time
def serve(addr,port):
 s=socket.socket(socket.AF_INET6 if ':' in addr else socket.AF_INET);s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
 if ':' in addr:s.setsockopt(socket.IPPROTO_IPV6,socket.IPV6_V6ONLY,1)
 s.bind((addr,port));s.listen()
 def echo(c):
  try:
   while True:
    b=c.recv(64)
    if not b:return
    c.sendall(b)
  finally:c.close()
 while True:
  c,_=s.accept();threading.Thread(target=echo,args=(c,),daemon=True).start()
for a in ['0.0.0.0','::']:
 for p in PORTS:threading.Thread(target=serve,args=(a,p),daemon=True).start()
time.sleep(120)
'''
CLIENT='''import socket,sys
try:
 s=socket.create_connection((sys.argv[1],int(sys.argv[2])),timeout=.4)
 s.sendall(b'bound');assert s.recv(5)==b'bound';s.close()
except (OSError,AssertionError):sys.exit(3)
'''


def request(address,port,namespace=None):
    args=([ 'ip','netns','exec',namespace] if namespace else [])+[sys.executable,'-c',CLIENT,address,str(port)]
    return subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=3).returncode==0


def expect_error(call,code):
    try:call()
    except snapshot.FormalSnapshotError as error:assert error.code==code,(error.code,code)
    else:raise AssertionError('expected '+code)


def main():
    _marker=Path('/owned-fixture-only')
    assert _marker.exists(),'explicit task-owned fixture marker required'
    ip('link','set','lo','up')
    for ns,outer,inner,subnet in [('client','hostclient','clienteth',10),('target','hosttarget','targeteth',20)]:
        ip('netns','add',ns)
        ip('link','add',outer,'type','veth','peer','name',inner)
        ip('link','set',inner,'netns',ns)
        for commands in [ ['addr','add',f'192.0.{subnet}.1/24','dev',outer],
            ['-6','addr','add',f'2001:db8:{subnet}::1/64','dev',outer,'nodad'],['link','set',outer,'up'] ]:ip(*commands)
        for commands in [['link','set','lo','up'],['addr','add',f'192.0.{subnet}.2/24','dev',inner],
            ['-6','addr','add',f'2001:db8:{subnet}::2/64','dev',inner,'nodad'],['link','set',inner,'up'],
            ['route','add','default','via',f'192.0.{subnet}.1'],['-6','route','add','default','via',f'2001:db8:{subnet}::1']]:
            ip('-n',ns,*commands)
    assert Path('/proc/sys/net/ipv4/ip_forward').read_text().strip()=='1'
    assert Path('/proc/sys/net/ipv6/conf/all/forwarding').read_text().strip()=='1'
    processes=[subprocess.Popen([sys.executable,'-c',SERVER.replace('PORTS','[8081,9090,22,8082]')]),
        subprocess.Popen(['ip','netns','exec','target',sys.executable,'-c',SERVER.replace('PORTS','[80,3306,9000,8082]')])]
    # Separate unrelated native tables emulate Docker DNAT and a later ACCEPT.
    nft('create table ip fixture_nat\nadd chain ip fixture_nat pre { type nat hook prerouting priority -100; }\n'
        'add rule ip fixture_nat pre ip daddr 192.0.10.1 tcp dport 28081 dnat to 192.0.20.2:80\n'
        'create table inet fixture_other\nadd chain inet fixture_other other_forward { type filter hook forward priority 0; }\n'
        'add rule inet fixture_other other_forward accept\n')
    native=ingress.NativeNft()
    plan={**native.identity(),'hostAddresses':['127.0.0.1','::1','192.0.10.1','2001:db8:10::1'],
        'hostPorts':[8081,9090,28081], 'targets':[{'address':'192.0.20.2','ports':[80,3306,9000]},
            {'address':'2001:db8:20::2','ports':[80,3306,9000]}]}
    paths=[('192.0.10.1',8081,'client'),('2001:db8:10::1',8081,'client'),
        ('192.0.10.1',9090,'client'),('192.0.10.1',28081,'client'),
        ('192.0.20.2',80,'client'),('2001:db8:20::2',9000,'client'),
        ('192.0.20.2',3306,None),('2001:db8:20::2',9000,None),('127.0.0.1',8081,None),('::1',8081,None)]
    end=time.monotonic()+5
    while not all(request(*p) for p in paths):
        assert time.monotonic()<end,'fixture connectivity unavailable before restriction'
        time.sleep(.05)
    original_other=native.run(['-j','-a','list','table','inet','fixture_other'])
    original_nat=native.run(['-j','-a','list','table','ip','fixture_nat'])
    dependency_report={}
    binding={'operationId':'owned-ingress','sourceFingerprint':'a'*64,'databaseUuid':'00000000-0000-0000-0000-000000000001','minioContainerId':'b'*64}
    with tempfile.TemporaryDirectory() as root:
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'claim',binding)
        established=socket.create_connection(('127.0.0.1',8081),timeout=.4)
        established.sendall(b'old');assert established.recv(3)==b'old'
        result=ingress.install_ingress(claim,plan,guard=claim.assert_owned)
        assert result['fullWriterExclusion'] is False
        assert not any(request(*p) for p in paths),'actual protected path remained reachable'
        for family_host in ['192.0.10.1','2001:db8:10::1']:
            assert request(family_host,22,'client'),'management port blocked'
            assert request(family_host,8082,'client'),'unrelated host port blocked'
        assert request('192.0.20.2',8082,'client'),'unrelated forwarded port blocked'
        try:
            established.sendall(b'after');received=established.recv(5)
            assert not received,'established business session bypassed restriction'
        except OSError:pass
        finally:established.close()
        claim.close();claim=snapshot.FormalMaintenanceClaim(Path(root)/'claim',binding,resume=True)
        assert ingress.observe_ingress_reconciliation(claim)['state']=='HELD'
        def unhealthy():raise snapshot.FormalSnapshotError('OWNED_HEALTH_NOT_READY')
        expect_error(lambda:ingress.release_ingress(claim,unhealthy),'OWNED_HEALTH_NOT_READY')
        ingress.assert_ingress(claim)
        ingress.release_ingress(claim,lambda:None)
        assert all(request(*p) for p in paths),'paths not restored'
        assert original_other==native.run(['-j','-a','list','table','inet','fixture_other'])
        assert original_nat==native.run(['-j','-a','list','table','ip','fixture_nat'])
        assert ingress.observe_ingress_reconciliation(claim)['state']=='RELEASED'
        claim.close()
        binding['operationId']='owned-ingress-dependency'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'dependency',binding)
        ingress.install_ingress(claim,plan,guard=claim.assert_owned)
        approved=[{'source':'192.0.10.2','destination':'192.0.20.2','ports':[3306]},
                  {'source':'2001:db8:10::2','destination':'2001:db8:20::2','ports':[9000]}]
        def thaw_guard():
            # This owned network test never requests filesystem freeze.
            claim.assert_owned()
        ingress.permit_recovery_dependencies(claim,approved,
            guard=lambda:claim.assert_owned(),thaw_guard=thaw_guard)
        assert request('192.0.20.2',3306,'client'),'exact IPv4 dependency remains blocked'
        assert request('2001:db8:20::2',9000,'client'),'exact IPv6 dependency remains blocked'
        assert not request('192.0.20.2',80,'client'),'same actor gained unapproved business port'
        assert not request('192.0.20.2',3306,None),'host gained dependency bypass'
        assert not request('2001:db8:20::2',9000,None),'IPv6 host gained dependency bypass'
        assert not request('192.0.10.1',8081,'client'),'business INPUT reopened'
        assert request('192.0.20.2',8082,'client'),'unrelated forwarding changed'
        expect_error(lambda:ingress.permit_recovery_dependencies(claim,approved,
            guard=claim.assert_owned,thaw_guard=thaw_guard),'FORMAL_INGRESS_RECOVERY_ALREADY_STARTED')
        claim.close();claim=snapshot.FormalMaintenanceClaim(Path(root)/'dependency',binding,resume=True)
        dependency_report=ingress.assert_ingress(claim)
        assert dependency_report['phase']=='RECOVERY_DEPENDENCIES_PERMITTED'
        expect_error(lambda:ingress.release_ingress(claim,lambda:unhealthy()),'OWNED_HEALTH_NOT_READY')
        ingress.assert_ingress(claim)
        ingress.release_ingress(claim,claim.assert_owned)
        assert all(request(*p) for p in paths)
        assert original_other==native.run(['-j','-a','list','table','inet','fixture_other'])
        assert original_nat==native.run(['-j','-a','list','table','ip','fixture_nat'])
        claim.close()
        # Altered/recreated table refuses capture and automatic deletion.
        binding['operationId']='owned-ingress-drift'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'drift',binding)
        table=ingress.install_ingress(claim,plan,guard=claim.assert_owned)['table']
        nft(f'add rule inet {table} input tcp dport 8082 drop\n')
        expect_error(lambda:ingress.assert_ingress(claim),'FORMAL_INGRESS_RULES_CHANGED')
        expect_error(lambda:ingress.release_ingress(claim,lambda:None),'FORMAL_INGRESS_RULES_CHANGED')
        claim.close()
        # Simulate lost acknowledgement after real kernel apply; no completion or replay.
        class LostAcknowledgement(ingress.NativeNft):
            def run(self,args,raw=None):
                result=super().run(args,raw)
                if args==['-f','-']:raise snapshot.FormalSnapshotError('FORMAL_INGRESS_COMMAND_UNCERTAIN')
                return result
        binding['operationId']='owned-ingress-uncertain'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'uncertain',binding)
        expect_error(lambda:ingress.install_ingress(claim,plan,native=LostAcknowledgement(),guard=claim.assert_owned),'FORMAL_INGRESS_COMMAND_UNCERTAIN')
        claim.close();claim=snapshot.FormalMaintenanceClaim(Path(root)/'uncertain',binding,resume=True)
        observed=ingress.observe_ingress_reconciliation(claim)
        assert observed['present'] and observed['state']=='INSTALL_UNCERTAIN_RETAINED'
        expect_error(lambda:ingress.install_ingress(claim,plan,guard=claim.assert_owned),'FORMAL_INGRESS_ACTION_ALREADY_STARTED')
        expect_error(lambda:ingress.assert_ingress(claim),'FORMAL_INGRESS_NOT_HELD')
        claim.close()
        # Kernel changes between acknowledgement and first observation cannot
        # be adopted as the approved original restriction.
        binding['operationId']='owned-ingress-install-drift'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'install-drift',binding)
        table=ingress._table(hashlib.sha256(claim._plan).hexdigest())
        class DriftAfterApply(ingress.NativeNft):
            def run(self,args,raw=None):
                result=super().run(args,raw)
                if args==['-f','-']:nft(f'flush chain inet {table} input\n')
                return result
        expect_error(lambda:ingress.install_ingress(claim,plan,native=DriftAfterApply(),guard=claim.assert_owned),'FORMAL_INGRESS_APPLIED_POLICY_CHANGED')
        assert not any(e['step']=='ingress-installed' for e in claim.events)
        claim.close()
        binding['operationId']='owned-ingress-release-race'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'release-race',binding)
        table=ingress.install_ingress(claim,plan,guard=claim.assert_owned)['table']
        class RecreatedBeforeDelete(ingress.NativeNft):
            def run(self,args,raw=None):
                if args==['-f','-']:
                    nft(f'delete table inet {table}\ncreate table inet {table}\n')
                return super().run(args,raw)
        expect_error(lambda:ingress.release_ingress(claim,lambda:None,native=RecreatedBeforeDelete()),'FORMAL_INGRESS_COMMAND_FAILED')
        assert native.inspect(table) is not None,'replacement table removed by stale identity'
        claim.close()
        binding['operationId']='owned-ingress-recovery-race'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'recovery-race',binding)
        table=ingress.install_ingress(claim,plan,guard=claim.assert_owned)['table'];replacement=[]
        class RecreatedBeforeRecovery(ingress.NativeNft):
            def run(self,args,raw=None):
                if args==['-f','-']:
                    nft(f'delete table inet {table}\n'+ingress.render_policy(hashlib.sha256(claim._plan).hexdigest(),plan).decode())
                    replacement.append(self.inspect(table))
                return super().run(args,raw)
        try:
            ingress.permit_recovery_dependencies(claim,approved,guard=claim.assert_owned,
                thaw_guard=claim.assert_owned,native=RecreatedBeforeRecovery())
        except snapshot.FormalSnapshotError as error:code=error.code
        else:raise AssertionError('recovery applied to replacement table')
        assert replacement and native.inspect(table)==replacement[0],'recovery modified the replacement table before refusing'
        assert code=='FORMAL_INGRESS_COMMAND_FAILED'
        assert not any(e['step']=='ingress-recovery-permitted' for e in claim.events)
        claim.close()
        binding['operationId']='owned-ingress-release-uncertain'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'release-uncertain',binding)
        table=ingress.install_ingress(claim,plan,guard=claim.assert_owned)['table']
        expect_error(lambda:ingress.release_ingress(claim,lambda:None,native=LostAcknowledgement()),'FORMAL_INGRESS_COMMAND_UNCERTAIN')
        claim.close();claim=snapshot.FormalMaintenanceClaim(Path(root)/'release-uncertain',binding,resume=True)
        observed=ingress.observe_ingress_reconciliation(claim)
        assert observed['state']=='RELEASE_UNCERTAIN_RETAINED' and not observed['present']
        expect_error(lambda:ingress.release_ingress(claim,lambda:None),'FORMAL_INGRESS_NOT_HELD')
        claim.close()
        binding['operationId']='owned-ingress-dependency-uncertain'
        claim=snapshot.FormalMaintenanceClaim(Path(root)/'dependency-uncertain',binding)
        table=ingress.install_ingress(claim,plan,guard=claim.assert_owned)['table']
        expect_error(lambda:ingress.permit_recovery_dependencies(claim,approved,
            guard=claim.assert_owned,thaw_guard=claim.assert_owned,native=LostAcknowledgement()),
            'FORMAL_INGRESS_COMMAND_UNCERTAIN')
        claim.close();claim=snapshot.FormalMaintenanceClaim(Path(root)/'dependency-uncertain',binding,resume=True)
        assert native.inspect(table) is not None
        assert not any(e['step']=='ingress-recovery-permitted' for e in claim.events)
        expect_error(lambda:ingress.assert_ingress(claim),'FORMAL_INGRESS_RECOVERY_UNCERTAIN_RETAINED')
        expect_error(lambda:ingress.permit_recovery_dependencies(claim,approved,
            guard=claim.assert_owned,thaw_guard=claim.assert_owned),'FORMAL_INGRESS_RECOVERY_ALREADY_STARTED')
        claim.close()
    for process in processes:
        process.terminate();process.wait(timeout=3)
    print(json.dumps({'result':'PASS','protectedPaths':len(paths),'ipv4':True,'ipv6':True,
        'dnatForward':True,'directForward':True,'hostOutput':True,'establishedRejected':True,
        'sshAndUnrelatedPreserved':True,'healthFailureRetainsIsolation':True,'reopenReadback':True,
        'driftRefused':True,'uncertainInstallNoReplay':True,'unrelatedTablesUnchanged':True,
        'nftVersion':native.run(['--version']).decode().strip(),'productionEvidence':False,
        'hostWritersVerified':False,'nativeTarget09CompatibilityVerified':False,
        'recoveryDependencyTraffic':{'state':'PASS','ipv4':True,'ipv6':True,
            'sameActorOtherPortBlocked':True,'hostOutputBlocked':True,'businessInputBlocked':True,
            'unrelatedRulesPreserved':True,'reopen':True,'uncertainNoReplay':True,
            'replacementTableUnchangedOnAtomicFailure':True,
            'finalBoundRestrictionBeforeRelease':dependency_report}}))


if __name__=='__main__':main()
