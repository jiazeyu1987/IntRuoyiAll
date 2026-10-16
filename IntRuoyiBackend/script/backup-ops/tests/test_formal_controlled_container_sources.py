"""A source fidelity and private file boundaries; no lifecycle authority."""
import copy
import hashlib
import importlib
import json
import os
from pathlib import Path
import sys
import tempfile
import time
import subprocess

import pytest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
CID='a'*64
IMAGE='sha256:'+'b'*64

def module():return importlib.import_module('formal_controlled_container_sources')

BOOT_ID='0fa6c22b-5a90-11f1-ade1-0242ac150002'

def fixture():
    return dict(Id=CID,Name='/approved-backend',Image=IMAGE,Path='/private/start',Args=['--secret=private-args'],
        Config=dict(Env=['PRIVATE=never-expose','ORDER=one','ORDER=two'],FutureField=dict(values=[None,1,False])),
        HostConfig=dict(RestartPolicy=dict(Name='unless-stopped'),FutureHost=dict(source='private-host')),
        Mounts=[dict(Type='bind',Source='/private',Destination='/data',RW=True)],
        State=dict(Pid=41,Status='running',Running=True,Paused=False,Restarting=False,Dead=False,OOMKilled=False,
            ExitCode=0,StartedAt='2026-10-01T00:00:00Z',FinishedAt='0001-01-01T00:00:00Z',
            Health=dict(Log=[dict(Output='private-health')]),Error=''))

def project(value=None):
    raw=json.dumps([fixture() if value is None else value]).encode()
    return module().controlled_inspect_projection(raw,CID,IMAGE,'/approved-backend')

def test_full_source_preserves_existing_static_algorithm_and_hides_private_values():
    value=fixture();actual=project(value)
    previous=importlib.import_module('formal_original_launch').inspect_projection(json.dumps([value]).encode(),CID,IMAGE)
    assert actual['digests']==previous['digests']
    assert actual['containerId']==CID and actual['name']==value['Name'] and actual['runtimeState']['Pid']==41
    for secret in ('never-expose','private-args','private-host','private-health','/private/start'):
        assert secret not in json.dumps(actual)
    assert 'Health' not in actual['runtimeState'] and 'Error' not in actual['runtimeState']

@pytest.mark.parametrize('field',['Config','HostConfig','Image','Path','Args','Mounts'])
def test_complete_launch_field_drift_changes_source_or_refuses_identity(field):
    value=fixture();original=project(value)
    if field=='Image':
        value[field]='sha256:'+'c'*64
        with pytest.raises(module().ControlledSourceError):project(value)
        return
    if field in ('Config','HostConfig'):value[field]['AddedUnrecognizedField']=['secret',None]
    elif field=='Mounts':value[field][0]['RW']=False
    elif field=='Args':value[field].append('secret')
    else:value[field]='/different'
    assert project(value)['digests']['launchSha256']!=original['digests']['launchSha256']

@pytest.mark.parametrize('change',['reverse','duplicate','null'])
def test_env_order_duplicate_and_null_are_distinct(change):
    value=fixture()
    if change=='reverse':value['Config']['Env'].reverse()
    elif change=='duplicate':value['Config']['Env'].append(value['Config']['Env'][0])
    else:value['Config']['Env']=None
    assert project(value)['digests']['configSha256']!=project()['digests']['configSha256']

def test_mount_row_order_has_existing_canonical_meaning():
    value=fixture();value['Mounts'].append(dict(Type='volume',Name='approved',Destination='/other',RW=False))
    changed=copy.deepcopy(value);changed['Mounts'].reverse()
    assert project(value)['digests']==project(changed)['digests']

@pytest.mark.parametrize('status,exit_code,oom',[('created',0,False),('exited',7,False),('exited',137,True)])
def test_original_stopped_or_never_started_state_is_preserved_without_success_claim(status,exit_code,oom):
    value=fixture();value['State'].update(Pid=0,Status=status,Running=False,ExitCode=exit_code,OOMKilled=oom)
    if status=='created':value['State'].update(StartedAt='0001-01-01T00:00:00Z',FinishedAt='0001-01-01T00:00:00Z')
    else:value['State']['FinishedAt']='2026-10-01T00:10:00Z'
    result=project(value)
    assert result['runtimeState']=={k:value['State'][k] for k in module().STATE_FIELDS}
    assert result['digests']==project()['digests']
    assert result['productionEvidence'] is False and result['serviceActionsPerformed'] is False

@pytest.mark.parametrize('change',['name','id','bool-pid','running-zero','paused','restarting','dead','missing-state','missing-static'])
def test_unstable_or_wrong_identity_is_fixed_redacted_failure(change):
    value=fixture()
    if change=='name':value['Name']='/other'
    elif change=='id':value['Id']='c'*64
    elif change=='bool-pid':value['State']['Pid']=True
    elif change=='running-zero':value['State']['Pid']=0
    elif change in ('paused','restarting','dead'):value['State'][change.title()]=True
    elif change=='missing-state':value['State'].pop('ExitCode')
    else:value.pop('HostConfig')
    with pytest.raises(module().ControlledSourceError) as error:project(value)
    assert str(error.value).startswith('CONTROLLED_CONTAINER_') and 'private' not in str(error.value)

@pytest.mark.parametrize('raw',[b'[]',b'{}',b'[NaN]',b'[{"Config":1,"Config":2}]',b'[',b'x'*1048577])
def test_invalid_or_unbounded_json_is_fixed_failure(raw):
    with pytest.raises(module().ControlledSourceError) as error:
        module().controlled_inspect_projection(raw,CID,IMAGE,'/approved-backend')
    assert 'private' not in str(error.value)

def test_private_saved_original_has_exclusive_durable_copy_and_terminal_identity_check():
    m=module();raw=json.dumps([fixture()]).encode()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-original-') as folder:
        path=Path(folder)/'backend-inspect.json'
        summary=m.seal_controlled_inspect_original(path,raw,CID,IMAGE,'/approved-backend',guard=lambda:None,deadline=time.monotonic()+10)
        assert path.read_bytes()==raw and path.stat().st_mode&0o777==0o600
        assert summary['rawSha256']==hashlib.sha256(raw).hexdigest()
        public=json.dumps(summary);assert 'never-expose' not in public
        result,terminal=m.verify_controlled_inspect_original(path,summary,guard=lambda:None,deadline=time.monotonic()+10)
        assert result==summary;terminal()
        with pytest.raises(m.ControlledSourceError):
            m.seal_controlled_inspect_original(path,raw,CID,IMAGE,'/approved-backend',guard=lambda:None,deadline=time.monotonic()+10)
        path.chmod(0o644)
        with pytest.raises(m.ControlledSourceError):terminal()

def test_verifier_rejects_late_guard_replacement_and_does_not_reseal():
    m=module();raw=json.dumps([fixture()]).encode()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-original-') as folder:
        path=Path(folder)/'backend-inspect.json'
        summary=m.seal_controlled_inspect_original(path,raw,CID,IMAGE,'/approved-backend',guard=lambda:None,deadline=time.monotonic()+10)
        original_inode=path.stat().st_ino
        calls=[]
        def guard():
            calls.append(1)
            if len(calls)==2:
                replacement=path.with_name('replacement');replacement.write_bytes(raw);replacement.chmod(0o600)
                os.replace(replacement,path)
        with pytest.raises(m.ControlledSourceError):m.verify_controlled_inspect_original(path,summary,guard=guard,deadline=time.monotonic()+10)
        assert path.stat().st_ino!=original_inode

def test_insecure_parent_and_symlink_source_refuse_before_original_read():
    m=module();raw=json.dumps([fixture()]).encode()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-original-') as folder:
        root=Path(folder);path=root/'backend-inspect.json';root.chmod(0o755)
        with pytest.raises(m.ControlledSourceError):
            m.seal_controlled_inspect_original(path,raw,CID,IMAGE,'/approved-backend',guard=lambda:None,deadline=time.monotonic()+10)
        assert not path.exists()

@pytest.mark.parametrize('timestamp',['2026-99-01T00:00:00Z','0000-01-01T00:00:00Z','2026-02-30T00:00:00Z'])
def test_invalid_calendar_timestamp_cannot_be_original_state(timestamp):
    value=fixture();value['State']['StartedAt']=timestamp
    with pytest.raises(module().ControlledSourceError):project(value)

class Sources:
    """Explicit successful inventory/inspect protocol seam, no action methods."""
    def __init__(self):
        self.rows=(CID+'\tapproved-backend\n').encode();self.value=fixture();self.calls=[]
    def inventory(self,deadline):self.calls.append('inventory');return self.rows
    def inspect(self,identity,deadline):
        assert identity==CID;self.calls.append('inspect');return json.dumps([self.value]).encode()

def expected(present=True):
    return dict(present=present,containerId=CID if present else None,imageId=IMAGE if present else None,
        name='/approved-backend')

def observe(sources,selection=None,guard=lambda:None):
    return module().observe_controlled_container_source(expected() if selection is None else selection,
        guard=guard,deadline=time.monotonic()+10,transport=sources)

def test_present_source_requires_successful_inventory_and_two_equal_complete_observations():
    sources=Sources();actual=observe(sources)
    assert actual==project() and sources.calls==['inventory','inspect','inventory','inspect']

def test_absence_is_explicit_successful_inventory_and_never_calls_inspect_or_action():
    sources=Sources();sources.rows=b''
    result=observe(sources,expected(False))
    assert result['present'] is False and result['name']=='/approved-backend'
    assert result['containerId'] is None and result['runtimeState'] is None
    assert result['productionEvidence'] is False
    assert sources.calls==['inventory','inventory']

@pytest.mark.parametrize('failure',[OSError('private-connection'),TimeoutError('private-timeout'),PermissionError('private-permission')])
def test_inventory_failure_cannot_be_converted_to_absence(failure):
    sources=Sources()
    def broken(deadline):raise failure
    sources.inventory=broken
    with pytest.raises(module().ControlledSourceError) as error:observe(sources,expected(False))
    assert 'private' not in str(error.value)

@pytest.mark.parametrize('rows',[b'opaque\n',b'abc\tapproved-backend\n',
    (CID+'\tapproved-backend\n'+CID+'\tother\n').encode(),
    (CID+'\tapproved-backend\n'+'c'*64+'\tapproved-backend\n').encode()])
def test_invalid_or_duplicate_inventory_is_not_absence(rows):
    sources=Sources();sources.rows=rows
    with pytest.raises(module().ControlledSourceError):observe(sources,expected(False))

@pytest.mark.parametrize('presence',[True,False])
def test_named_replacement_or_existing_container_refuses_expected_presence(presence):
    sources=Sources()
    if presence:sources.rows=('c'*64+'\tapproved-backend\n').encode()
    with pytest.raises(module().ControlledSourceError):observe(sources,expected(presence))

def test_late_full_launch_drift_is_rejected_before_source_return():
    sources=Sources();calls=[]
    def guard():
        calls.append(1)
        if len(calls)==2:sources.value['HostConfig']['FutureHost']['source']='changed'
    with pytest.raises(module().ControlledSourceError):observe(sources,guard=guard)

def test_unrelated_inventory_change_does_not_change_selected_absence():
    sources=Sources();sources.rows=b'';calls=[]
    def guard():
        calls.append(1)
        if len(calls)==2:sources.rows=('c'*64+'\tunrelated\n').encode()
    assert observe(sources,expected(False),guard)['present'] is False

def test_native_read_argv_pins_docker_endpoint_and_complete_private_inspect(monkeypatch):
    m=module();calls=[]
    def command(argv,*,deadline,limit):calls.append(argv);return b''
    monkeypatch.setattr(m.launch,'_bounded_command',command)
    native=m.NativeControlledDockerSources();native.inventory(time.monotonic()+5);native.inspect(CID,time.monotonic()+5)
    assert calls[0]==['/usr/bin/docker','--host','unix:///var/run/docker.sock','ps','--all','--no-trunc',
        '--format','{{.ID}}\t{{.Names}}']
    assert calls[1]==['/usr/bin/docker','--host','unix:///var/run/docker.sock','inspect',CID]

def test_native_capture_seals_actual_observed_bytes_and_preserves_terminal_identity():
    m=module();sources=Sources()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-native-') as folder:
        path=Path(folder)/'backend-inspect.json'
        result=m.capture_controlled_container_source(path,expected(),guard=lambda:None,
            deadline=time.monotonic()+10,transport=sources)
        assert result['source']==project() and result['rawSha256']==hashlib.sha256(path.read_bytes()).hexdigest()
        assert sources.calls.count('inventory')==4 and sources.calls.count('inspect')==4

def test_expired_seal_guard_does_not_create_an_original(monkeypatch):
    m=module();now=[time.monotonic()];deadline=now[0]+1
    monkeypatch.setattr(m.time,'monotonic',lambda:now[0])
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-expiry-') as folder:
        path=Path(folder)/'backend-inspect.json'
        def guard():now[0]=deadline+1
        with pytest.raises(m.ControlledSourceError):
            m.seal_controlled_inspect_original(path,json.dumps([fixture()]).encode(),CID,IMAGE,
                '/approved-backend',guard=guard,deadline=deadline)
        assert not path.exists()

def test_capture_does_not_reset_outer_budget_before_exclusive_write(monkeypatch):
    m=module();sources=Sources();now=[time.monotonic()];deadline=now[0]+1;calls=[]
    monkeypatch.setattr(m.time,'monotonic',lambda:now[0])
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-expiry-') as folder:
        path=Path(folder)/'backend-inspect.json'
        def guard():
            calls.append(1)
            if len(calls)==3:now[0]=deadline+1
        with pytest.raises(m.ControlledSourceError):
            m.capture_controlled_container_source(path,expected(),guard=guard,deadline=deadline,transport=sources)
        assert not path.exists()


def action_wire(value):
    # Explicit fixed Docker-format wire: Health/Error are not transported.
    row={k:copy.deepcopy(value[k]) for k in (*module().STATIC_FIELDS,'Id','Name')}
    row['State']={k:value['State'][k] for k in module().STATE_FIELDS}
    return (json.dumps([row],separators=(',',':'))+'\n').encode()


def stopped(value):
    value=copy.deepcopy(value)
    value['State'].update(Pid=0,Status='exited',Running=False,ExitCode=0,OOMKilled=False,
        FinishedAt='2026-10-09T00:00:00Z')
    return value


class ActionSources(Sources):
    """Explicit conditional action seam, not actual daemon or approval."""
    def __init__(self):super().__init__();self.actions=[];self.fail=None
    def action(self,before,action,deadline):
        assert project(self.value)==before
        self.actions.append(action)
        if self.fail:raise self.fail
        if action=='stop':self.value=stopped(self.value)
        else:self.value['State'].update(Pid=84,Status='running',Running=True,ExitCode=0,
            StartedAt='2026-10-09T00:05:00Z',FinishedAt='0001-01-01T00:00:00Z')


def original_for_action(folder,value=None):
    m=module();path=Path(folder)/'original.json';value=fixture() if value is None else value
    receipt=m.seal_controlled_inspect_original(path,json.dumps([value]).encode(),CID,IMAGE,
        '/approved-backend',guard=lambda:None,deadline=time.monotonic()+10)
    return receipt


@pytest.mark.parametrize('action',['stop','start'])
def test_protected_action_checks_private_original_and_complete_source_before_and_after(action):
    m=module();sources=ActionSources()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-action-') as folder:
        original=original_for_action(folder)
        if action=='start':sources.value=stopped(sources.value)
        before=project(sources.value);guards=[]
        result=m.run_controlled_container_action(original,before,action,guard=lambda:guards.append(1),
            deadline=time.monotonic()+10,transport=sources)
        assert sources.actions==[action] and result['source']==project(sources.value)
        assert result['state']=='CONTROLLED_CONTAINER_ACTION_OBSERVED' and result['serviceActionsPerformed']
        assert not result['productionEvidence'] and not result['releaseReady']
        assert len(guards)>=4
        assert 'never-expose' not in json.dumps(result)


@pytest.mark.parametrize('field',['Config','HostConfig','Args','Path','Mounts'])
def test_protected_action_refuses_full_launch_drift_before_any_effect(field):
    m=module();sources=ActionSources()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-action-') as folder:
        original=original_for_action(folder)
        if field in ('Config','HostConfig'):sources.value[field]['UnknownAdded']=True
        elif field=='Path':sources.value[field]='/changed'
        elif field=='Args':sources.value[field].append('--changed')
        else:sources.value[field][0]['RW']=False
        with pytest.raises(m.ControlledSourceError):
            m.run_controlled_container_action(original,project(sources.value),'stop',guard=lambda:None,
                deadline=time.monotonic()+10,transport=sources)
        assert not sources.actions


@pytest.mark.parametrize('case',['original-stopped','original-created','stop-nonzero','stop-oom','before-changed','missing-original'])
def test_start_preserves_original_stopped_and_refuses_unknown_stop(case):
    m=module();sources=ActionSources()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-action-') as folder:
        value=fixture()
        if case.startswith('original-'):
            value=stopped(value)
            if case=='original-created':value['State'].update(Status='created',StartedAt='0001-01-01T00:00:00Z',FinishedAt='0001-01-01T00:00:00Z')
        original=original_for_action(folder,value);sources.value=stopped(sources.value)
        if case=='stop-nonzero':sources.value['State']['ExitCode']=7
        if case=='stop-oom':sources.value['State']['OOMKilled']=True
        if case=='missing-original':Path(original['path']).unlink()
        before=project(sources.value)
        if case=='before-changed':before['runtimeState']['FinishedAt']='2026-10-08T00:00:00Z'
        with pytest.raises(m.ControlledSourceError):
            m.run_controlled_container_action(original,before,'start',guard=lambda:None,
                deadline=time.monotonic()+10,transport=sources)
        assert not sources.actions


@pytest.mark.parametrize('case',['original','live-source','deadline','expected'])
def test_guard_cannot_invalidate_original_or_current_source_and_still_act(case,monkeypatch):
    m=module();sources=ActionSources();now=[time.monotonic()];deadline=now[0]+10
    monkeypatch.setattr(m.time,'monotonic',lambda:now[0])
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-action-') as folder:
        original=original_for_action(folder);before=project(sources.value)
        def guard():
            if case=='original':Path(original['path']).chmod(0o644)
            elif case=='live-source':sources.value['Config']['Env'].reverse()
            elif case=='deadline':now[0]=deadline+1
            else:before['runtimeState']['Pid']=99
        with pytest.raises(m.ControlledSourceError):
            m.run_controlled_container_action(original,before,'stop',guard=guard,
                deadline=deadline,transport=sources)
        assert not sources.actions


@pytest.mark.parametrize('case',['no-stop','nonzero','static-drift','guard-original'])
def test_failed_post_action_proof_never_returns_a_completion(case):
    m=module();sources=ActionSources()
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-action-') as folder:
        original=original_for_action(folder);action=sources.action
        def perform(before,kind,deadline):
            action(before,kind,deadline)
            if case=='no-stop':sources.value=fixture()
            elif case=='nonzero':sources.value['State']['ExitCode']=137
            elif case=='static-drift':sources.value['HostConfig']['NewField']=True
        sources.action=perform
        def guard():
            if sources.actions and case=='guard-original':Path(original['path']).chmod(0o644)
        with pytest.raises(m.ControlledSourceError):
            m.run_controlled_container_action(original,project(),'stop',guard=guard,
                deadline=time.monotonic()+10,transport=sources)
        assert sources.actions==['stop']


@pytest.mark.parametrize('remote',[False,True])
def test_actual_transport_has_fixed_endpoint_peer_boot_and_consumable_full_wire(remote,monkeypatch):
    m=module();calls=[];raw=action_wire(fixture())
    def execute(argv,body,*,deadline,action):
        calls.append((argv,body,action))
        if action:return (CID+'\n').encode()
        if b'ps --all' in body and b'normalized_inspect' not in body:return (CID+'\tapproved-backend\n').encode()
        return raw
    monkeypatch.setattr(m,'_execute_controlled_script',execute)
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-peer-') as folder:
        keys=Path(folder)/'known_hosts';keys.write_bytes(b'approved-key\n');keys.chmod(0o600)
        connection=dict(host='172.30.30.58',port=22,user='root',bootId=BOOT_ID,
            knownHostsFile=str(keys),knownHostsSha256=hashlib.sha256(keys.read_bytes()).hexdigest(),
            dockerEndpoint='unix:///var/run/docker.sock')
        native=m.SshControlledDockerActions(connection) if remote else m.NativeControlledDockerActions(BOOT_ID)
        assert native.inventory(time.monotonic()+10)==(CID+'\tapproved-backend\n').encode()
        actual=m.controlled_inspect_projection(native.inspect(CID,time.monotonic()+10),CID,IMAGE,'/approved-backend')
        native.action(actual,'stop',time.monotonic()+10)
        with pytest.raises(m.ControlledSourceError):native.action(actual,'stop',time.monotonic()+10)
        argv,body,is_action=calls[-1];text=body.decode()
        assert is_action and 'stop --signal SIGTERM --timeout -1 '+CID in text
        assert 'unix:///var/run/docker.sock' in text and BOOT_ID in text
        assert hashlib.sha256(raw).hexdigest() in text
        assert '{{json .Config}}' in text and '{{json .HostConfig}}' in text
        assert text.index('sha256sum')<text.index('stop --signal')
        assert 'never-expose' not in text and 'private-health' not in text
        if remote:
            assert argv[0]=='/usr/bin/ssh' and 'StrictHostKeyChecking=yes' in argv
            assert 'root@172.30.30.58' in argv and 'SSH_CONNECTION' in text
            keys.write_bytes(b'changed-key\n')
            count=len(calls)
            with pytest.raises(m.ControlledSourceError):native.inventory(time.monotonic()+10)
            assert len(calls)==count
        else:assert argv==['/bin/bash','--noprofile','--norc','-s']


@pytest.mark.parametrize('failure',['timeout','stderr','bad-result'])
def test_actual_transport_uncertainty_consumes_observation_without_retry(failure,monkeypatch):
    m=module();calls=[]
    def execute(argv,body,*,deadline,action):
        calls.append(action)
        if action:
            if failure!='bad-result':raise m.ControlledSourceError('CONTROLLED_CONTAINER_COMMAND_UNCERTAIN')
            return b'wrong-id\n'
        return action_wire(fixture())
    monkeypatch.setattr(m,'_execute_controlled_script',execute)
    native=m.NativeControlledDockerActions(BOOT_ID)
    actual=project(json.loads(native.inspect(CID,time.monotonic()+10))[0])
    with pytest.raises(m.ControlledSourceError):native.action(actual,'stop',time.monotonic()+10)
    with pytest.raises(m.ControlledSourceError):native.action(actual,'stop',time.monotonic()+10)
    assert calls==[False,True]


@pytest.mark.parametrize('case',['healthy','config-drift','runtime-drift','boot-drift','same-name-replaced'])
def test_real_bash_conditional_protocol_checks_before_single_action(case,monkeypatch):
    """Real Bash/private files, explicitly substituted Docker protocol executable."""
    m=module();execute=m._execute_controlled_script
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-shell-') as folder:
        root=Path(folder);data=root/'source.json';data.write_text(json.dumps(fixture()))
        log=root/'actions';peer=root/'boot';peer.write_text(BOOT_ID+'\n')
        cli=root/'docker-protocol'
        cli.write_text('#!'+sys.executable+'\n'+
            'import json,sys\nfrom pathlib import Path\n'+
            'p=Path('+repr(str(data))+');v=json.loads(p.read_bytes());a=sys.argv[3:]\n'+
            'if a[0]=="ps":print(v["Id"]+"\\t"+v["Name"][1:])\n'+
            'elif a[0]=="inspect":\n'+
            ' mounts=v["Mounts"];v["Mounts"]=[]\n'+
            ' print(json.dumps([v],separators=(",",":")))\n'+
            ' for row in mounts:print(json.dumps(row,separators=(",",":")))\n'+
            'else:\n'+
            ' Path('+repr(str(log))+').write_text(a[0]);print(v["Id"])\n')
        cli.chmod(0o700)
        def local_protocol(argv,body,*,deadline,action):
            # Only this test replaces fixed daemon/boot reads with explicit fixtures.
            # /control is noexec: use the fixed real interpreter on this data file.
            return execute(argv,body.replace(b'/usr/bin/docker',(sys.executable+' '+str(cli)).encode()).replace(
                b'/proc/sys/kernel/random/boot_id',str(peer).encode()),deadline=deadline,action=action)
        monkeypatch.setattr(m,'_execute_controlled_script',local_protocol)
        native=m.NativeControlledDockerActions(BOOT_ID)
        before=m.controlled_inspect_projection(native.inspect(CID,time.monotonic()+10),CID,IMAGE,'/approved-backend')
        value=fixture()
        if case=='config-drift':value['Config']['FutureField']['values'].append(9)
        elif case=='runtime-drift':value['State']['Pid']=90
        elif case=='boot-drift':peer.write_text('00000000-0000-0000-0000-000000000000\n')
        elif case=='same-name-replaced':value['Id']='c'*64
        data.write_text(json.dumps(value))
        if case=='healthy':
            native.action(before,'stop',time.monotonic()+10)
            assert log.read_text()=='stop'
        else:
            with pytest.raises(m.ControlledSourceError):native.action(before,'stop',time.monotonic()+10)
            assert not log.exists()


def test_bounded_execution_rejects_private_stderr_and_never_exposes_it():
    m=module()
    with pytest.raises(m.ControlledSourceError) as error:
        m._execute_controlled_script(['/bin/bash','--noprofile','--norc','-s'],
            b'printf "private-diagnostic" >&2\n',deadline=time.monotonic()+2,action=False)
    assert 'private-diagnostic' not in str(error.value)


@pytest.mark.parametrize('case',['healthy','missing-role','duplicate-id','missing-original','original-mode',
    'full-config-drift','scope-config','scope-original-state','scope-current-state','guard-drift','original-stopped'])
def test_shared_action_adapter_binds_real_private_originals_and_actual_reduced_journal(case):
    m=module();roles=('mysql','frontend','backend','showroom','onlyoffice','redis','minio')
    with tempfile.TemporaryDirectory(dir='/control',prefix='controlled-scope-') as folder:
        receipts={};values={}
        for index,role in enumerate(roles,1):
            value=fixture();value.update(Id=str(index)*64,Name='/'+role)
            if case=='original-stopped' and role=='frontend':value=stopped(value)
            raw=json.dumps([value]).encode();values[value['Id']]=value
            receipts[role]=m.seal_controlled_inspect_original(Path(folder)/(role+'.json'),raw,
                value['Id'],IMAGE,value['Name'],guard=lambda:None,deadline=time.monotonic()+10)
        class NativeScopeProtocol:
            def inventory(self,deadline):
                return ''.join(v['Id']+'\t'+v['Name'][1:]+'\n' for v in values.values()).encode()
            def inspect(self,identity,deadline):return action_wire(values[identity])
            def action(self,*args):pytest.fail('observation must not act')
        def reduced(v):return dict(id=v['Id'],name=v['Name'],image=v['Image'],mounts=copy.deepcopy(v['Mounts']),
            restartPolicy=copy.deepcopy(v['HostConfig']['RestartPolicy']),state={k:v['State'][k] for k in
                ('Running','Paused','Restarting','Dead','OOMKilled','ExitCode','StartedAt','FinishedAt')})
        original=reduced(values['2'*64]);current=copy.deepcopy(original)
        if case=='missing-role':receipts.pop('onlyoffice')
        if case=='duplicate-id':receipts['backend']=copy.deepcopy(receipts['frontend'])
        if case in ('missing-role','duplicate-id'):
            with pytest.raises(m.ControlledSourceError):m.ControlledSharedContainerActions(receipts,NativeScopeProtocol())
            return
        bound=m.ControlledSharedContainerActions(receipts,NativeScopeProtocol())
        if case=='missing-original':Path(receipts['frontend']['path']).unlink()
        if case=='original-mode':Path(receipts['frontend']['path']).chmod(0o644)
        if case=='full-config-drift':values['2'*64]['Config']['FutureField']['values'].append(2)
        if case=='scope-config':original['restartPolicy']['Name']='always'
        if case=='scope-original-state':original['state']['StartedAt']='2026-10-08T00:00:00Z'
        if case=='scope-current-state':current['state']['StartedAt']='2026-10-08T00:00:00Z'
        def guard():
            if case=='guard-drift':values['2'*64]['Config']['Env'].reverse()
        if case in ('healthy','original-stopped'):
            proof=bound.assert_role('frontend',original,current,guard=guard,deadline=time.monotonic()+10)
            assert proof==m.controlled_inspect_projection(action_wire(values['2'*64]),'2'*64,IMAGE,'/frontend')
        else:
            with pytest.raises(m.ControlledSourceError):
                bound.assert_role('frontend',original,current,guard=guard,deadline=time.monotonic()+10)
