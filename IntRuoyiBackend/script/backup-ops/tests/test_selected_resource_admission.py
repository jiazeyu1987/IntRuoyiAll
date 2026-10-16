"""Real Linux private-file custody; explicit protocol observations, not server proof."""
import copy
import hashlib
import importlib
import json
import os
from pathlib import Path
import sys
import tempfile
import time

import pytest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
from formal_snapshot_linux import FormalSnapshotError

CODE = 'RELEASE_SELECTED_RESOURCE_ADMISSION_INVALID'
sha = lambda raw: hashlib.sha256(raw).hexdigest()
raw = lambda value: json.dumps(value, separators=(',', ':')).encode()


@pytest.fixture
def case():
    with tempfile.TemporaryDirectory(prefix='selected-resource-', dir='/control') as directory:
        root = Path(directory)
        dispatch = dict(workflowId='fixture-workflow', operationId='fixture-operation', releaseTag='fixture-package',
            packageDigest='1'*64, manifestDigest='2'*64, targetDigest='3'*64, deploymentBindingDigest='4'*64)
        scope = dict(environment='test', host='172.30.30.58', machineId='5'*32, database='fixture_db',
            serverUuid='11111111-1111-1111-1111-111111111111')
        storage = dict(mode='local-bind', mount='/mnt/fixture-source', device='/dev/fixture-source',
            majorMinor='8:17', filesystemUuid='22222222-2222-2222-2222-222222222222',
            fileSystemId=17, sourceDirectory='/mnt/fixture-source/minio')
        source = dict(scope=scope, instanceIdentity=dict(target=dict(schemaVersion='release-instance-target/v3',
            host=scope['host'], minioStorage=storage, containers=dict(mysql='fixture-source-mysql', minio='fixture-source-minio'))),
            configSha256='6'*64, envSha256='7'*64,
            originalsSha256={role:'8'*64 for role in ('application-config','application-env','data-env','application-compose','data-compose')},
            containers={role:dict(id='9'*64, imageId='sha256:'+'a'*64,
                volume=dict(fileSystemId=17 if role=='minio' else 18, inode=101 if role=='minio' else 102))
                for role in ('mysql','minio')}, applicationsSha256='b'*64)
        source['containers']['minio']['volume']['bindProof'] = {
            key:value for key,value in storage.items() if key!='mode'}
        source['containers']['minio']['volume']['bindProof']['inode'] = 101
        def role(name, device, inode):
            source_role = name=='source'
            mount='/mnt/fixture-'+('source' if source_role else 'control' if device==18 else 'recovery')
            path=mount+'/'+('minio' if source_role else name+'/next')
            return dict(host=scope['host'], machineId=scope['machineId'], mount=mount,
                device='/dev/fixture-'+('source' if source_role else 'control' if device==18 else 'recovery'),
                majorMinor='8:'+str(device), filesystemUuid=('2' if source_role else '3' if device==18 else '4')*8+'-'+
                ('2222-2222-2222-222222222222' if source_role else '3333-3333-3333-333333333333' if device==18 else '4444-4444-4444-444444444444'),
                fileSystemId=device, parent=path if source_role else str(Path(path).parent), parentInode=inode, path=path)
        now=int(time.time())
        resources=dict(schemaVersion='release-selected-test-resources/v1', dispatchBinding=copy.deepcopy(dispatch),
            sourceBinding=copy.deepcopy(source), issuedAt=now-5, expiresAt=now+900,
            roles={name:role(name,dev,ino) for name,dev,ino in
                (('source',17,101),('control',18,103),('capture',18,104),('recoveryMysql',19,105),('recoveryMinio',19,106))},
            budget=dict(sourceBytes=100, sqlBytes=20, metadataBytes=10, growthBytes=10, retainedBytes=50,
                reserveBytes=30, requiredInodes=20, copyBytesPerSecond=100, freezeSeconds=30, totalSeconds=100))
        recovery=dict(schemaVersion='release-selected-test-recovery-scope/v1', dispatchBinding=copy.deepcopy(dispatch),
            sourceBinding=copy.deepcopy(source), issuedAt=now-5, expiresAt=now+900,
            runtime=dict(host=scope['host'], machineId=scope['machineId'], database=scope['database'],
                mysqlContainer='fixture-recovery-mysql', minioContainer='fixture-recovery-minio', networkId='d'*64,
                mysqlImageId=source['containers']['mysql']['imageId'], minioImageId=source['containers']['minio']['imageId']),
            policy=dict(preservePhysicalVersions=True, preserveProtectionMetadata=True, startApplication=False, retainFailedArtifacts=True))
        result=dict(root=root, dispatch=dispatch, source=source, resources=resources, recovery=recovery)
        def publish():
            collection=dict(schemaVersion='release-selected-test-capture-sources/v1', environment='test',
                host=scope['host'], targetDigest=dispatch['targetDigest'], deploymentBindingDigest=dispatch['deploymentBindingDigest'], originals={})
            for name,value in (('resources',resources),('recoveryScope',recovery),('stopAuthority',{'fixture':'other separately verified original'}),
                               ('restoreAuthority',{'fixture':'other separately verified original'})):
                path=root/(name+'.json'); path.write_bytes(raw(value)); path.chmod(0o600)
                collection['originals'][name]=dict(file=path.name,sha256=sha(path.read_bytes()))
            path=root/'test.capture-sources.json'; path.write_bytes(raw(collection)); path.chmod(0o600)
            result['pin']=sha(path.read_bytes())
        result['publish']=publish; publish()
        def observations(plan):
            return dict(roles={name:dict(identity=copy.deepcopy(value), availableBytes=10000, availableInodes=1000,
                readOnly=False,targetAbsent=name!='source',filesystemType='ext4') for name,value in plan['resources']['roles'].items()},
                sourceBytes=90,sourceFiles=5,
                recoveryRuntime=dict(identity=copy.deepcopy(plan['recoveryScope']['runtime']), containersAbsent=True,
                    networkInternal=True, networkMemberCount=0, publishedPortCount=0))
        result['observe']=observations
        yield result


def admit(case, *, observe=None, guard=lambda:None):
    spec=importlib.util.find_spec('formal_selected_resource_admission')
    assert spec is not None, 'one implemented resource admission contract is required'
    module=importlib.import_module('formal_selected_resource_admission')
    return module.admit_selected_test_resources(str(case['root']),case['pin'],case['dispatch'],case['source'],
        observe=case['observe'] if observe is None else observe,guard=guard)


def test_finite_contract_preserves_private_originals_and_never_claims_effects(case):
    before={p.name:(p.read_bytes(),p.stat().st_ino,p.stat().st_mtime_ns) for p in case['root'].iterdir()}
    calls=[]
    result=admit(case,guard=lambda:calls.append(True))
    assert result['state']=='SELECTED_RESOURCE_POINT_IN_TIME_ADMITTED'
    for flag in ('grantConsumed','startedPersisted','sourceEffectsPerformed','continuousProtectionProven','recoveryCompleted','releaseReady'):
        assert result[flag] is False
    assert result['collectionSha256']==case['pin']
    assert result['originalsSha256']['resources']==sha((case['root']/'resources.json').read_bytes())
    assert len(calls)>=3
    assert before=={p.name:(p.read_bytes(),p.stat().st_ino,p.stat().st_mtime_ns) for p in case['root'].iterdir()}


@pytest.mark.parametrize('fault', ['pin','file-bytes','mode','link','duplicate','missing','extra','source-binding','dispatch-binding',
    'expired','too-long','future','deadline','path-dot','path-double','path-root','path-symlink-name','bool-budget','float-budget',
    'source-budget','inode-budget','freeze-budget','total-budget','overlap','control-device','recovery-source-device',
    'recovery-capture-device','source-inode','wrong-target','image','source-container','application-policy'])
def test_invalid_originals_reject_without_effects(case,fault):
    r=case['resources']; recovery=case['recovery']
    if fault=='missing': del r['budget']['sqlBytes']
    elif fault=='extra': r['approved']=True
    elif fault=='source-binding': r['sourceBinding']['configSha256']='f'*64
    elif fault=='dispatch-binding': r['dispatchBinding']['operationId']='other'
    elif fault=='expired': r['expiresAt']=int(time.time())-1
    elif fault=='too-long': r['expiresAt']=r['issuedAt']+3601
    elif fault=='future': r['issuedAt']=int(time.time())+100
    elif fault=='deadline': r['expiresAt']=int(time.time())+50
    elif fault.startswith('path-'):
        r['roles']['capture']['path']={'path-dot':'/mnt/fixture-control/capture/../next','path-double':'/mnt//fixture-control/next',
            'path-root':'/','path-symlink-name':'relative'}[fault]
    elif fault=='bool-budget': r['budget']['sourceBytes']=True
    elif fault=='float-budget': r['budget']['sourceBytes']=100.0
    elif fault=='source-budget': r['budget']['sourceBytes']=89
    elif fault=='inode-budget': r['budget']['requiredInodes']=9
    elif fault=='freeze-budget': r['budget']['freezeSeconds']=12
    elif fault=='total-budget': r['budget']['totalSeconds']=12
    elif fault=='overlap': r['roles']['capture']=copy.deepcopy(r['roles']['control'])
    elif fault=='control-device': r['roles']['capture']['fileSystemId']=999
    elif fault=='recovery-source-device': r['roles']['recoveryMysql']['fileSystemId']=17
    elif fault=='recovery-capture-device': r['roles']['recoveryMinio']['fileSystemId']=18
    elif fault=='source-inode': r['roles']['source']['parentInode']=999
    elif fault=='wrong-target': r['roles']['source']['path']='/mnt/fixture-source/other'
    elif fault=='image': recovery['runtime']['mysqlImageId']='sha256:'+'f'*64
    elif fault=='source-container': recovery['runtime']['minioContainer']='fixture-source-minio'
    elif fault=='application-policy': recovery['policy']['startApplication']=True
    case['publish']()
    if fault=='pin': case['pin']='f'*64
    elif fault=='file-bytes': (case['root']/'resources.json').write_bytes(raw(r)+b' ')
    elif fault=='mode': (case['root']/'resources.json').chmod(0o644)
    elif fault=='link': os.link(case['root']/'resources.json',case['root']/'another')
    elif fault=='duplicate':
        path=case['root']/'test.capture-sources.json'; value=path.read_bytes().replace(b'{',b'{"environment":"test",',1)
        path.write_bytes(value); case['pin']=sha(value)
    with pytest.raises(FormalSnapshotError,match='^'+CODE+'$'): admit(case)


@pytest.mark.parametrize('fault',['identity','capacity','aggregate-capacity','inodes','readonly','exists','filesystem','source-bytes',
    'source-files','runtime-id','runtime-member','runtime-port','runtime-public','runtime-container','bool-capacity','missing-role'])
def test_actual_observation_mismatch_rejects(case,fault):
    def observe(plan):
        value=case['observe'](plan); role=value['roles']['capture']
        if fault=='identity': role['identity']['parentInode']=999
        elif fault=='capacity': role['availableBytes']=1
        elif fault=='aggregate-capacity':
            # Capture alone (190 + reserve 30) fits 225; combined control requires 230.
            role['availableBytes']=225; value['roles']['control']['availableBytes']=225
        elif fault=='inodes': role['availableInodes']=1
        elif fault=='readonly': role['readOnly']=True
        elif fault=='exists': role['targetAbsent']=False
        elif fault=='filesystem': value['roles']['source']['filesystemType']='xfs'
        elif fault=='source-bytes': value['sourceBytes']=101
        elif fault=='source-files': value['sourceFiles']=21
        elif fault=='runtime-id': value['recoveryRuntime']['identity']['networkId']='f'*64
        elif fault=='runtime-member': value['recoveryRuntime']['networkMemberCount']=1
        elif fault=='runtime-port': value['recoveryRuntime']['publishedPortCount']=1
        elif fault=='runtime-public': value['recoveryRuntime']['networkInternal']=False
        elif fault=='runtime-container': value['recoveryRuntime']['containersAbsent']=False
        elif fault=='bool-capacity': role['availableBytes']=True
        elif fault=='missing-role': del value['roles']['source']
        return value
    with pytest.raises(FormalSnapshotError,match='^'+CODE+'$'): admit(case,observe=observe)


def test_cross_host_recovery_uses_machine_and_device_tuple(case):
    for name in ('recoveryMysql','recoveryMinio'):
        case['resources']['roles'][name].update(host='172.30.30.59',machineId='e'*32,fileSystemId=17)
    case['recovery']['runtime'].update(host='172.30.30.59',machineId='e'*32)
    case['publish']()
    assert admit(case)['state']=='SELECTED_RESOURCE_POINT_IN_TIME_ADMITTED'


def test_callback_cannot_mutate_bound_plan_or_return_stale_second_observation(case):
    calls=[]
    def observe(plan):
        calls.append(True); value=case['observe'](plan)
        plan['resources']['roles']['source']['parentInode']=999
        if len(calls)==2: value['roles']['capture']['identity']['parentInode']=999
        return value
    with pytest.raises(FormalSnapshotError,match='^'+CODE+'$'): admit(case,observe=observe)


@pytest.mark.parametrize('fault',['owner','original-bytes','original-inode','expected','collection'])
def test_final_guard_or_original_drift_rejected(case,fault):
    calls=[]
    def guard():
        calls.append(True)
        if len(calls)!=3:return
        if fault=='owner': raise FormalSnapshotError('OWNED_SOURCE_GUARD_LOST')
        if fault=='expected': case['source']['configSha256']='f'*64
        elif fault=='collection': (case['root']/'test.capture-sources.json').write_bytes(b'{}')
        else:
            path=case['root']/'resources.json'; content=path.read_bytes()
            if fault=='original-inode':
                new=case['root']/'replacement';new.write_bytes(content);new.chmod(0o600);new.replace(path)
            else: path.write_bytes(content+b' ')
    code='OWNED_SOURCE_GUARD_LOST' if fault=='owner' else CODE
    with pytest.raises(FormalSnapshotError,match='^'+code+'$'): admit(case,guard=guard)


def test_readonly_callback_failure_is_not_default_success(case):
    def unavailable(_):raise FormalSnapshotError('NATIVE_RESOURCE_OBSERVATION_UNAVAILABLE')
    with pytest.raises(FormalSnapshotError,match='^NATIVE_RESOURCE_OBSERVATION_UNAVAILABLE$'):
        admit(case,observe=unavailable)


@pytest.mark.parametrize('fault',['symlink','fifo','root-mode','missing-original','duplicate-role-file','collection-as-original'])
def test_real_native_custody_rejects_unsafe_files_and_ambiguous_collection(case,fault):
    path=case['root']/'resources.json'
    if fault=='symlink':
        target=case['root']/'kept-resources';path.rename(target);path.symlink_to(target)
    elif fault=='fifo': path.unlink();os.mkfifo(path,0o600)
    elif fault=='root-mode': case['root'].chmod(0o755)
    elif fault=='missing-original': path.unlink()
    else:
        collection=case['root']/'test.capture-sources.json'; value=json.loads(collection.read_bytes())
        value['originals']['resources']['file']='stopAuthority.json' if fault=='duplicate-role-file' else collection.name
        collection.write_bytes(raw(value));case['pin']=sha(collection.read_bytes())
    with pytest.raises(FormalSnapshotError,match='^'+CODE+'$'): admit(case)


def test_detached_callback_plan_cannot_change_success_result(case):
    def observe(plan):
        value=case['observe'](plan)
        plan['resources']['budget']['reserveBytes']=0
        plan['recoveryScope']['policy']['startApplication']=True
        return value
    result=admit(case,observe=observe)
    assert result['plan']['resources']['budget']['reserveBytes']==30
    assert result['plan']['recoveryScope']['policy']['startApplication'] is False


@pytest.mark.parametrize('fault',['clone-device','clone-major-minor','clone-uuid'])
def test_same_native_device_cannot_claim_independent_filesystem_id(case,fault):
    source=case['resources']['roles']['source']
    for name in ('recoveryMysql','recoveryMinio'):
        role=case['resources']['roles'][name]
        key={'clone-device':'device','clone-major-minor':'majorMinor','clone-uuid':'filesystemUuid'}[fault]
        role[key]=source[key]
    case['publish']()
    with pytest.raises(FormalSnapshotError,match='^'+CODE+'$'):admit(case)
