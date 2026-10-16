"""Actual owned standard SQL/stop/raw/thaw/restart health; no host proof.

The nested Docker engine, ext4 loop source and ingress namespace belong solely
to this attempt. HTTP roles are protocol fixtures, not business applications.
No remote access, external callers, NAS or production completion is claimed.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import sys
import tempfile
import tarfile
import threading
import time
import uuid

ROOT=Path(__file__).parents[1]
from verify_formal_health_real import HTTP_SERVER,NATIVE_IMAGE


def docker(args,timeout=60):
    result=subprocess.run(['docker',*args],capture_output=True,timeout=timeout)
    if result.returncode:
        detail=result.stderr.lower()
        print(json.dumps({'ownedDockerFailure':{'exit':result.returncode,
            'imageMissing':b'unable to find image' in detail or b'no such image' in detail,
            'mountMissing':b'bind source path does not exist' in detail,
            'runtimeFailure':b'oci runtime' in detail,'networkFailure':b'network' in detail,
            'spaceFailure':b'no space left' in detail,'execMissing':b'executable file not found' in detail}}),flush=True)
        raise RuntimeError('OWNED_SHARED_DOCKER_FAILED')
    return result.stdout


def inside():
    sys.path.insert(0,'/code/linux')
    import formal_snapshot_linux as snapshot
    import formal_split_capture as split
    import formal_service_health as health
    import formal_ingress_control as ingress
    config=json.loads(Path('/fixtures/config.json').read_text())
    assert docker(['info','--format','{{.Name}}']).decode().strip()==config['engine']
    native_tag='intruoyi-owned-nft-fixture:20261003'
    observed_image=docker(['image','inspect','--format','{{.Id}}',native_tag]).decode().strip()
    print(json.dumps({'nativeImageObserved':observed_image,'nativeArchiveConfigExpected':config['nativeConfigId'],
        'nativeSourceManifestId':NATIVE_IMAGE}),flush=True)
    assert observed_image==config['nativeConfigId']
    loop=os.environ['OWNED_LOOP_DEVICE'];assert re.fullmatch('/dev/loop[0-9]+',loop)
    subprocess.run(['mount','-t','ext4','-o','nosuid,nodev,noexec',loop,'/owned/source'],
        check=True,capture_output=True,timeout=10)
    source=Path('/owned/source/minio');source.mkdir();Path('/owned/source/mysql').mkdir()
    def launch(name,image,arguments,options=()):
        return docker(['run','-d','--name',name,'--label','task-owner='+config['prefix'],*options,image,*arguments]).decode().strip()
    roles={
        'mysql':launch('intruoyi-mysql','mysql:8.0.39',['--event-scheduler=OFF'],
            ['--network','none','--env-file','/fixtures/mysql.env','--mount','type=bind,src=/owned/source/mysql,dst=/var/lib/mysql']),
        'minio':launch('ragflow_compose-minio-1','quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z',['server','/data'],
            ['--env-file','/fixtures/minio.env','--mount','type=bind,src=/owned/source/minio,dst=/data']),
        'redis':launch('owned-redis','redis:6-alpine',['redis-server','--save','','--appendonly','no'],['--network','none'])}
    for role in ['backend','frontend','showroom','onlyoffice']:
        roles[role]=launch('owned-'+role,native_tag,['-I','/owned-server.py'],
            ['--network','none','-e','OWNED_HTTP_ROLE='+role,'--entrypoint','python3',
             '--mount','type=bind,src=/fixtures/http.py,dst=/owned-server.py,readonly'])
    password=Path('/fixtures/mysql-password').read_text().strip()
    mysql=snapshot.PrivateDockerMySql(roles['mysql'],password,timeout=5)
    end=time.monotonic()+90
    while True:
        daemon=docker(['exec',roles['mysql'],'cat','/proc/1/comm']).strip()
        if daemon==b'mysqld' and mysql.ready():break
        assert time.monotonic()<end,'owned final database not ready';time.sleep(.2)
    identity=mysql.query('SELECT @@server_uuid;').strip()
    mysql.query('CREATE DATABASE owned_probe; CREATE TABLE owned_probe.probe(id INT PRIMARY KEY,value VARCHAR(32)); '
        'INSERT INTO owned_probe.probe VALUES(7,"owned-original"); SET GLOBAL read_only=ON;')
    original_flags=snapshot._database_flags(mysql,identity)
    original_rows=mysql.query('SELECT id,value FROM owned_probe.probe;')
    snapshot.run_formal_container_action('stop',roles['showroom'])
    binding={'operationId':config['prefix'],'sourceFingerprint':'a'*64,'databaseUuid':identity,'minioContainerId':roles['minio']}
    claim=snapshot.FormalMaintenanceClaim('/tmp/shared-phase-claim',binding)
    original_scope=split.bind_shared_container_scope(claim,roles)
    report=health.bind_shared_health_scope(claim,mysql,guard=claim.assert_owned,redis_authentication='none',timeout=90)
    assert report['health']['showroom']['state']=='ORIGINALLY_STOPPED'
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
    address=docker(['inspect','--format','{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}',roles['minio']]).decode().strip()
    gateway=docker(['inspect','--format','{{range .NetworkSettings.Networks}}{{.Gateway}}{{end}}',roles['minio']]).decode().strip()
    native=ingress.NativeNft()
    plan={**native.identity(),'hostAddresses':['127.0.0.1','::1'],'hostPorts':[18081],
        'targets':[{'address':address,'ports':[9000]}]}
    ingress.install_ingress(claim,plan,guard=claim.assert_owned);assert not reachable()
    snapshot.acquire_claimed_database_fence(claim,mysql,guard=claim.assert_owned)
    mounted=json.loads(subprocess.check_output(['findmnt','-J','--mountpoint','/owned/source','-o','SOURCE,MAJ:MIN,FSTYPE,TARGET']))['filesystems'][0]
    fence={'mount':'/owned/source','seconds':30,'binding':{'operationId':binding['operationId'],
        'bootId':Path('/proc/sys/kernel/random/boot_id').read_text().strip(),
        'mount':{'source':mounted['source'],'device':mounted['maj:min'],'fstype':mounted['fstype'],'target':mounted['target']}}}
    credential=Path('/tmp/shared-phase-password');credential.write_text(password+'\n');credential.chmod(0o600)
    common={'schemaVersion':'release-formal-capture-request/v3','claimRoot':str(claim.root),'claimBinding':binding,
        'mysqlContainerId':roles['mysql'],'mysqlCredentialFile':str(credential),'totalSeconds':180,
        'sharedScopeSha256':hashlib.sha256(claim._read(claim.root/'shared-container-original.json')).hexdigest(),
        'originalHealthSha256':hashlib.sha256(claim._read(claim.root/'shared-health-original.json')).hexdigest(),
        'ingressRulesSha256':ingress.assert_ingress(claim)['rulesSha256']}
    captured_request={**common,'action':'capture-shared-payload','database':'owned_probe','sourceRoot':str(source),
        'dataLimits':{'max_rows':10000,'max_nodes':100000,'max_bytes':16*1024*1024,'seconds':120},
        'rejectionProbe':{'table':'probe','column':'id'},'filesystemFence':fence}
    bootstrap='import runpy,sys;sys.path.insert(0,"/code/linux");sys.argv=sys.argv[1:];runpy.run_path(sys.argv[0],run_name="__main__")'
    def dispatch(selected,name,expected=0):
        nonlocal claim
        path=Path('/tmp')/(name+'.json');path.write_bytes(snapshot._canonical_bytes(selected));path.chmod(0o600)
        digest=hashlib.sha256(path.read_bytes()).hexdigest();claim.close()
        command=[sys.executable,'-I','-c',bootstrap,'/code/linux/backup_ops_linux.py','--mode','formal-capture',
            '--request',str(path),'--expected-request-sha256',digest]
        started=time.monotonic();result=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=210,cwd='/tmp')
        if result.returncode!=expected or result.stderr:
            # Fixed JSON code or frame locations only; never private stderr.
            try:output=json.loads(result.stdout)
            except ValueError:output={'code':'OWNED_SHARED_CLI_NONJSON'}
            frames=[{'file':m.group(1),'line':int(m.group(2)),'function':m.group(3)} for line in result.stderr.decode().splitlines()
                if (m:=re.fullmatch(r'  File "([^"]+)", line ([0-9]+), in ([A-Za-z0-9_<>]+)',line))]
            print(json.dumps({'childFailed':name,'exit':result.returncode,'code':output.get('code'),'frames':frames}),flush=True)
            raise RuntimeError('OWNED_SHARED_STANDARD_ENTRY_FAILED')
        output=json.loads(result.stdout)
        assert output['productionEvidence'] is False
        claim=snapshot.FormalMaintenanceClaim('/tmp/shared-phase-claim',binding,resume=True)
        assert not reachable();ingress.assert_ingress(claim)
        claim._write(claim.root/(name+'-receipt.json'),snapshot._canonical_bytes(output))
        print(json.dumps({'phase':name,'state':output['state'],'seconds':round(time.monotonic()-started,3)}),flush=True)
        return output
    captured=dispatch(captured_request,'standard-capture')
    assert captured['state']=='SHARED_PAYLOAD_CAPTURED_INGRESS_HELD'
    assert captured['native']['nativeState']=='THAWED_COPY_ACCEPTED_PENDING_FORMAL_BARRIERS'
    assert all(snapshot.inspect_formal_container(i)['state']['Running'] is False for i in roles.values())
    receipt=claim._read(claim.root/'payload/040-payload.json');journal=snapshot._canonical_bytes(claim.events)
    rejected=dispatch(captured_request,'capture-replay',1)
    assert rejected['code']=='FORMAL_SPLIT_STAGE_INVALID' and claim._read(claim.root/'payload/040-payload.json')==receipt
    assert snapshot._canonical_bytes(claim.events)==journal
    # Separate explicit dependency transition, still no INPUT/host OUTPUT.
    current_plan=ingress.assert_ingress(claim)
    flow=[{'source':gateway,
           'destination':address,'ports':[9000]}]
    transition=ingress.permit_recovery_dependencies(claim,flow,guard=lambda:ingress.assert_ingress(claim),
        thaw_guard=lambda:split._thaw_receipt(claim))
    assert transition['phase']=='RECOVERY_DEPENDENCIES_PERMITTED' and not reachable()
    common['ingressRulesSha256']=transition['rulesSha256']
    restored=dispatch({**common,'action':'restore-shared-services'},'standard-restore')
    assert restored['state']=='ORIGINAL_SERVICES_HEALTHY_INGRESS_HELD'
    assert restored['health']['health']['showroom']['state']=='ORIGINALLY_STOPPED'
    assert restored['serviceActionsPerformed'] and not restored['fullRunnerCompleted'] and not restored['claimReleased']
    assert snapshot._database_flags(mysql,identity)==original_flags
    assert mysql.query('SELECT id,value FROM owned_probe.probe;')==original_rows
    for role,row in original_scope.items():
        now=snapshot.inspect_formal_container(row['id'])
        assert snapshot._container_stable(now)==snapshot._container_stable(row)
        assert now['state']['Running']==row['state']['Running']
    before=snapshot._canonical_bytes(claim.events)
    rerequest={k:v for k,v in common.items() if k!='totalSeconds'}
    rerequest.update(schemaVersion='release-formal-capture-request/v2',action='observe-shared-health',healthSeconds=90)
    observed=dispatch(rerequest,'standard-observe')
    assert observed['serviceActionsPerformed'] is False and snapshot._canonical_bytes(claim.events)==before
    def guard():return ingress.assert_ingress(claim)
    def thaw_guard():return split._thaw_receipt(claim)
    def healthy():return health.observe_shared_recovery_health(claim,mysql,guard=guard,thaw_guard=thaw_guard,timeout=90)
    docker(['exec',roles['backend'],'python3','-I','-c',"from pathlib import Path;Path('/tmp/owned-unhealthy').write_text('owned fault')"])
    try:ingress.release_ingress(claim,healthy)
    except snapshot.FormalSnapshotError as error:assert error.code=='FORMAL_HEALTH_RESPONSE_INVALID'
    else:raise AssertionError('unhealthy ingress released')
    assert not reachable() and not any(e['step']=='ingress-release-intent' for e in claim.events)
    docker(['exec',roles['backend'],'python3','-I','-c',"from pathlib import Path;Path('/tmp/owned-unhealthy').unlink()"])
    ingress.release_ingress(claim,healthy);assert reachable()
    assert not snapshot.inspect_formal_container(roles['showroom'])['state']['Running']
    claim.close();listener.close()
    print(json.dumps({'state':'PASS','standardCaptureAndRestoreFreshProcesses':True,'mysqlActualStopRestartAndOriginalFlags':True,
        'mysqlAndMinioShareActualExt4Source':True,'nativeRawThawAndV2Branch':True,'allSevenStoppedBeforeRaw':True,
        'originalStoppedShowroomPreserved':True,'replayRefusedJournalUnchanged':True,'unhealthyFinalReleaseRefused':True,
        'explicitHealthyFinalFixtureRelease':True,'standardFinalReleaseIntegrated':False,'actualSshCallers':False,
        'hostWritersVerified':False,'productionEvidence':False,'realApplicationAcceptance':False,'remoteWrites':False,
        'httpProtocolFixtures':['backend','frontend','showroom','onlyoffice']}),flush=True)


def outside():
    prefix='owned-shared-phase-'+uuid.uuid4().hex[:10]
    engine=prefix+'-engine';driver=prefix+'-driver';data=prefix+'-data';store=prefix+'-engine-data'
    containers=[];volumes=[];loop=None;engine_mount=False
    with tempfile.TemporaryDirectory(prefix=prefix) as temp:
        root=Path(temp);password=secrets.token_hex(32)
        (root/'mysql-password').write_text(password+'\n')
        (root/'mysql.env').write_text('MYSQL_ROOT_PASSWORD='+password+'\n')
        (root/'minio.env').write_text('MINIO_ROOT_USER=owned-shared\nMINIO_ROOT_PASSWORD='+secrets.token_hex(32)+'\n')
        (root/'http.py').write_text(HTTP_SERVER)
        try:
            docker(['save','-o',str(root/'images.tar'),'mysql:8.0.39','redis:6-alpine',
                'quay.io/minio/minio:RELEASE.2025-06-13T11-33-47Z','intruoyi-owned-nft-fixture:20261003'],timeout=180)
            assert docker(['image','inspect','--format','{{.Id}}','intruoyi-owned-nft-fixture:20261003']).decode().strip()==NATIVE_IMAGE
            with tarfile.open(root/'images.tar') as archive:
                manifests=json.load(archive.extractfile('manifest.json'))
                selected=[row for row in manifests if 'intruoyi-owned-nft-fixture:20261003' in row['RepoTags']]
                assert len(selected)==1
                configraw=archive.extractfile(selected[0]['Config']).read(1024*1024+1)
                assert len(configraw)<=1024*1024
                native_config='sha256:'+hashlib.sha256(configraw).hexdigest()
                assert native_config[7:] in selected[0]['Config']
            (root/'config.json').write_text(json.dumps({'prefix':prefix,'engine':engine,'nativeConfigId':native_config}))
            for name in (data,store):docker(['volume','create','--label','task-owner='+prefix,name]);volumes.append(name)
            setup="from pathlib import Path;import subprocess,json;p=Path('/owned/native.img');f=p.open('xb');f.truncate(1024**3);f.close();Path('/owned/source').mkdir();loop=subprocess.check_output(['losetup','--find','--show',str(p)],timeout=10).decode().strip();subprocess.run(['mkfs.ext4','-q',loop],check=True,timeout=15);print(json.dumps({'loop':loop}))"
            loop=json.loads(docker(['run','--rm','--privileged','--network','none','--label','task-owner='+prefix,
                '--mount','type=volume,src='+data+',dst=/owned',NATIVE_IMAGE,'python3','-I','-c',setup]))['loop']
            assert re.fullmatch('/dev/loop[0-9]+',loop)
            (root/'driver.env').write_text('OWNED_LOOP_DEVICE='+loop+'\nDOCKER_HOST=tcp://127.0.0.1:2375\n')
            docker(['run','-d','--name',engine,'--hostname',engine,'--label','task-owner='+prefix,'--privileged','--network','none',
                '-e','DOCKER_TLS_CERTDIR=','--mount','type=volume,src='+store+',dst=/var/lib/docker',
                '--mount','type=volume,src='+data+',dst=/owned','--mount','type=bind,src='+str(root)+',dst=/fixtures,readonly',
                'dd43b430341a']);containers.append(engine)
            end=time.monotonic()+45
            while True:
                result=subprocess.run(['docker','exec',engine,'docker','-H','unix:///var/run/docker.sock','info','--format','{{.Name}}'],
                    capture_output=True,timeout=5)
                if result.returncode==0 and result.stdout.decode().strip()==engine:break
                assert time.monotonic()<end,'owned engine not ready';time.sleep(.2)
            docker(['exec',engine,'mount','-t','ext4','-o','nosuid,nodev,noexec',loop,'/owned/source']);engine_mount=True
            docker(['exec',engine,'docker','-H','unix:///var/run/docker.sock','load','-i','/fixtures/images.tar'],timeout=180)
            docker(['cp',engine+':/usr/local/bin/docker',str(root/'docker')])
            docker(['create','--privileged','--name',driver,'--label','task-owner='+prefix,'--network','container:'+engine,
                '--pid','container:'+engine,'--env-file',str(root/'driver.env'),
                '--mount','type=volume,src='+data+',dst=/owned','--mount','type=bind,src='+str(ROOT)+',dst=/code,readonly',
                '--mount','type=bind,src='+str(root)+',dst=/fixtures,readonly',
                '--mount','type=bind,src='+str(root/'docker')+',dst=/usr/local/bin/docker,readonly',
                '--entrypoint','python3',NATIVE_IMAGE,'/code/tests/verify_formal_shared_phase_real.py','--inside']);containers.append(driver)
            process=subprocess.run(['docker','start','-a',driver],capture_output=True,timeout=600)
            rows=[json.loads(line) for line in process.stdout.decode().splitlines() if line.startswith('{')]
            for value in rows:print(json.dumps(value),flush=True)
            if process.returncode or docker(['inspect','--format','{{.State.ExitCode}}',driver]).decode().strip()!='0':
                # Capture the attempt's private journal before fixture cleanup.
                # Credentials live outside this claim and are never retained.
                retained=ROOT.parents[2]/'doc/tasks/20260927-three-environment-release'/(prefix+'-failed-retained')
                retained.mkdir()
                probe=subprocess.run(['docker','cp',driver+':/tmp/shared-phase-claim/.',str(retained)],
                    capture_output=True,timeout=30)
                print(json.dumps({'failedClaimRetention':{'root':str(retained),'copied':probe.returncode==0,
                    'fileCount':sum(p.is_file() for p in retained.rglob('*'))}}),flush=True)
                lines=process.stderr.decode().splitlines()
                codes=[m.group(1) for line in lines if (m:=re.fullmatch(r'[A-Za-z_.]*(?:FormalSnapshotError|WatchdogError): ((?:FORMAL|WATCHDOG)_[A-Z0-9_]+)',line))]
                frames=[{'file':m.group(1),'line':int(m.group(2)),'function':m.group(3)} for line in lines
                    if (m:=re.fullmatch(r'  File "([^"]+)", line ([0-9]+), in ([A-Za-z0-9_<>]+)',line))]
                print(json.dumps({'fixedFailureCodes':codes,'frames':frames,'privateStderrSuppressed':True}),flush=True)
                raise RuntimeError('OWNED_SHARED_DRIVER_FAILED')
            assert rows[-1]['state']=='PASS' and rows[-1]['productionEvidence'] is False
            retained=ROOT.parents[2]/'doc/tasks/20260927-three-environment-release'/(prefix+'-retained');retained.mkdir()
            docker(['cp',driver+':/tmp/shared-phase-claim/.',str(retained)])
            files={p.relative_to(retained).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in retained.rglob('*') if p.is_file()}
            print(json.dumps({'retainedEvidence':str(retained),'sha256':files}),flush=True)
        finally:
            if driver in containers and docker(['inspect','--format','{{.State.Running}}',driver]).decode().strip()=='true':
                docker(['stop','--time','20',driver])
            if engine_mount:
                ids=docker(['exec',engine,'docker','-H','unix:///var/run/docker.sock','ps','-q']).decode().split()
                if ids:docker(['exec',engine,'docker','-H','unix:///var/run/docker.sock','stop','--time','15',*ids],timeout=90)
                docker(['exec',engine,'umount','/owned/source']);engine_mount=False
            if loop is not None:
                detach="import subprocess,sys;loop=sys.argv[1];rows=subprocess.check_output(['losetup','-j','/owned/native.img']).decode().splitlines();assert len(rows)==1 and rows[0].split(':',1)[0]==loop;subprocess.run(['losetup','--detach',loop],check=True,timeout=10)"
                docker(['run','--rm','--privileged','--network','none','--label','task-owner='+prefix,
                    '--mount','type=volume,src='+data+',dst=/owned',NATIVE_IMAGE,'python3','-I','-c',detach,loop])
            for name in reversed(containers):
                assert docker(['inspect','--format','{{index .Config.Labels "task-owner"}}',name]).decode().strip()==prefix
                if docker(['inspect','--format','{{.State.Running}}',name]).decode().strip()=='true':docker(['stop','--time','20',name])
                docker(['rm',name])
            for name in reversed(volumes):
                assert docker(['volume','inspect','--format','{{index .Labels "task-owner"}}',name]).decode().strip()==prefix
                docker(['volume','rm',name])
            print(json.dumps({'ownedFixtureCleanup':'PASS','prefix':prefix}),flush=True)


if __name__=='__main__':inside() if '--inside' in sys.argv else outside()
