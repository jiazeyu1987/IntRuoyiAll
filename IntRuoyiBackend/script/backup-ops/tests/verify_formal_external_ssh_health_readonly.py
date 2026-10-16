"""Actual pinned Linux SSH/namespace health only; never service actions."""
from datetime import datetime,timezone
import hashlib
import json
import re
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import uuid

ROOT=Path(__file__).parents[1]
NATIVE='sha256:070da5e6fd4a490e009ce13c042339c673724b577520f624a1bc0c4b85a7b10d'
C=Path('D:/ProjectPackage/Int/IntRuoyiMaintance/doc/tasks/20260927-three-environment-release')

def inside():
    sys.path.insert(0,'/code/linux')
    import formal_external_callers as callers
    import formal_snapshot_linux as snapshot
    root=Path('/root/.ssh');root.mkdir(mode=0o700,parents=True,exist_ok=True);root.chmod(0o700)
    for name in ('id_rsa','known_hosts'):
        shutil.copyfile('/fixtures/'+name,root/name);(root/name).chmod(0o600)
    config=json.loads(Path('/fixtures/config.json').read_text())
    digest=hashlib.sha256((root/'known_hosts').read_bytes()).hexdigest()
    assert digest==config['knownHostsSha256']
    transport=callers.SshDockerTransport();observations=[]
    token=snapshot.observation_deadline.set(callers.time.monotonic()+60)
    try:
        for role in callers.ROLES:
            host=callers.HOSTS[role]
            connection={'host':host,'port':22,'user':'root','bootId':config['bootIds'][host],
                'knownHostsFile':str(root/'known_hosts'),'knownHostsSha256':digest,'dockerEndpoint':'unix:///var/run/docker.sock'}
            identity=config['containerIds'][role]
            record=transport.inspect(connection,identity)
            assert record['name']==callers.NAMES[role] and record['state']['Running']
            again=transport.inspect(connection,identity)
            if again!=record:
                print(json.dumps({'readonlyRuntimeDrift':{'role':role,
                    'changedFields':[k for k in record if record[k]!=again[k]],
                    'mountOrderOnly':sorted((snapshot._canonical_bytes(x) for x in record['mounts']))==sorted((snapshot._canonical_bytes(x) for x in again['mounts'])),
                    'beforeSha256':hashlib.sha256(snapshot._canonical_bytes(record)).hexdigest(),
                    'afterSha256':hashlib.sha256(snapshot._canonical_bytes(again)).hexdigest()}}),flush=True)
                raise snapshot.FormalSnapshotError('FORMAL_CALLER_RUNTIME_CHANGED')
            proof=transport.observe_http(connection,record,role,8)
            assert proof is not None
            callers._health_proof(role,proof)
            assert transport.inspect(connection,identity)==record
            observations.append({'role':role,'host':host,'containerId':identity,'bootId':connection['bootId'],
                'runtimeSha256':hashlib.sha256(snapshot._canonical_bytes(record)).hexdigest(),'proof':proof})
    finally:snapshot.observation_deadline.reset(token)
    print(json.dumps({'state':'PASS','observedAt':datetime.now(timezone.utc).isoformat(),'observations':observations,
        'actualProductionSshTransportReadonly':True,'remoteWrites':False,'serviceActionsPerformed':False,
        'postRestoreHealthVerified':False,'fullRunnerVerified':False,'productionEvidence':False}),flush=True)

def outside():
    task=ROOT.parents[2]/'doc/tasks/20260927-three-environment-release'
    source=json.loads((C/'external-health-prerequisites-20261002T222507Z.json').read_text())
    assert source['remoteWrites'] is False
    prefix='owned-external-ssh-health-'+uuid.uuid4().hex[:10]
    with tempfile.TemporaryDirectory(prefix=prefix) as directory:
        root=Path(directory)
        for name in ('id_rsa','known_hosts'):shutil.copyfile(Path('C:/Users/BJB110/.ssh')/name,root/name)
        config={'bootIds':{x['host']:x['bootId'] for x in source['observations']},'knownHostsSha256':source['knownHostsSha256'],
            'containerIds':{'test-backend':'8b7c48be91f9679282fb932f705123b1c191d98ada1266ac771788e05e499949',
            'test-rehearsal':'4c9f478235232e669c3a03a53499b8c14dd39d8d70059ae85eb09b408b0c18bf',
            'backup-backend':'0cf0146bb3742dd238bdb6e1b96c72c414fd3be83e718a9208a125de2caef5f4',
            'backup-rehearsal':'6a67f2f6da18153dc15b1c13c30108a92d1e16e8f48282dba9071ffb813830a3'}}
        (root/'config.json').write_text(json.dumps(config))
        result=subprocess.run(['docker','run','--rm','--name',prefix,'--label','task-owner='+prefix,'--network','bridge',
            '--mount','type=bind,src='+str(root)+',dst=/fixtures,readonly',
            '--mount','type=bind,src='+str(ROOT)+',dst=/code,readonly',
            '--entrypoint','python3',NATIVE,'-I','/code/tests/verify_formal_external_ssh_health_readonly.py','--inside'],
            stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=90)
        if result.returncode or result.stderr:
            # Fixed code only. Neither SSH diagnostics nor credential bytes leave memory.
            codes=[line.split(': ',1)[1] for line in result.stderr.decode().splitlines()
                if line.startswith('formal_snapshot_linux.FormalSnapshotError: FORMAL_')]
            for line in result.stdout.decode().splitlines():
                if line.startswith('{'):print(line,flush=True)
            frames=[{'file':m.group(1),'line':int(m.group(2)),'function':m.group(3)}
                for line in result.stderr.decode().splitlines()
                if (m:=re.fullmatch(r'  File "([^"]+)", line ([0-9]+), in ([A-Za-z0-9_<>]+)',line))]
            print(json.dumps({'state':'FAILED','codes':codes,'frames':frames,'privateStderrSuppressed':True,'remoteWrites':False}),flush=True)
            raise RuntimeError('EXTERNAL_SSH_READONLY_FAILED')
        receipt=json.loads(result.stdout);assert receipt['state']=='PASS' and receipt['remoteWrites'] is False
        path=task/(prefix+'-receipt.json')
        with path.open('x',encoding='utf-8') as out:json.dump(receipt,out,indent=2)
        print(json.dumps({'output':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),**receipt}),flush=True)

if __name__=='__main__':inside() if '--inside' in sys.argv else outside()
