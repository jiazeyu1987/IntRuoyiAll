"""Real Linux claim/mask files; explicit systemd protocol boundary double.

No daemon/socket or server operation, and no derived-work drain assertion.
"""
import copy
import hashlib
import importlib
import os
from pathlib import Path
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_snapshot_linux as snapshot


class HostAdmission(unittest.TestCase):
    def setUp(self):
        self.m=importlib.import_module('formal_host_admission')
        self.tmp=tempfile.TemporaryDirectory(prefix='owned-host-',dir='/control')
        self.root=Path(self.tmp.name);self.runtime=self.root/'runtime';self.runtime.mkdir()
        self.claim=snapshot.FormalMaintenanceClaim(self.root/'claim',dict(operationId='owned-host',
            sourceFingerprint='c'*64,databaseUuid='00000000-0000-0000-0000-000000000001',minioContainerId='d'*64))
        self.persistent=self.root/'persistent';self.persistent.mkdir()
        self.native=self.m.NativeHostAdmission(runtime_directory=self.runtime,persistent_directory=self.persistent)
        self.states={u:dict(loadState='loaded',active='active',sub='running' if u=='crond.service' else 'listening'
            if u=='cockpit.socket' else 'waiting',mainPid=41 if u=='crond.service' else 0,controlPid=0,
            unitFileState='static' if u=='systemd-tmpfiles-clean.timer' else 'enabled',
            configurationSha256='a'*64,configurationFiles=[],loadedConfigurationPaths=[],linksSha256='b'*64,stopContractSha256='e'*64,
            timerSemanticsSha256='f'*64 if u.endswith('.timer') else None,originalMasked=False)
            for u in self.m.CLOSE_ORDER}
        self.calls=[];self.fail_action=None;self.cap=dict(bootId='00000000-0000-0000-0000-000000000002',
            machineIdSha256='1'*64,systemctlSha256='2'*64)
        # Keep actual native filesystem operations. Only manager transport is doubled.
        self.native.capabilities=lambda deadline:copy.deepcopy(self.cap)
        self.native.observe=lambda unit,deadline,original=None,**kwargs:dict(copy.deepcopy(self.states[unit]),
            mask=self.native.mask_identity(unit),persistentMask=None)
        def command(action,unit,deadline):
            self.calls.append((action,unit))
            if action=='stop':self.states[unit].update(active='inactive',sub='dead',mainPid=0,controlPid=0)
            if action=='start':self.states[unit].update(active='active',sub='running' if unit=='crond.service'
                else 'listening' if unit=='cockpit.socket' else 'waiting',mainPid=41 if unit=='crond.service' else 0)
            if action=='reload':
                for name in self.m.CLOSE_ORDER:
                    saved=self.plan['units'][name]
                    if self.native.mask_identity(name) is not None:
                        self.states[name].update(loadState='masked',unitFileState='masked-runtime',
                            loadedConfigurationPaths=['/dev/null',*saved['loadedConfigurationPaths'][1:]])
                    else:
                        for key in ('loadState','unitFileState','loadedConfigurationPaths'):
                            self.states[name][key]=copy.deepcopy(saved[key])
            if action==self.fail_action:raise snapshot.FormalSnapshotError('MANAGER_EFFECT_UNCERTAIN')
        self.native.command=command
        create=self.native.create_owned_mask
        def create_owned(unit):
            self.calls.append(('mask',unit));result=create(unit)
            if self.fail_action=='mask':raise snapshot.FormalSnapshotError('MASK_EFFECT_UNCERTAIN')
            return result
        self.native.create_owned_mask=create_owned
        self.plan={'schemaVersion':'formal-host-admission-plan/v1','host':copy.deepcopy(self.cap),
            'units':{u:dict(self.native.observe(u,0),stopApproved=True,
                timerResumeApproved=u.endswith('.timer')) for u in self.m.CLOSE_ORDER}}
        self.digest=hashlib.sha256(snapshot._canonical_bytes(self.plan)).hexdigest()
        self.bound=False
    def tearDown(self):
        if hasattr(self,'claim'):self.claim.close()
        if hasattr(self,'tmp'):self.tmp.cleanup()
    def bind(self):
        self.m.bind_host_originals(self.claim,self.plan,self.digest,guard=lambda:None,native=self.native)
        if not getattr(self,'defer_derived_binding',False):
            # Legacy manager tests retain an explicit derived-source protocol
            # double. DerivedClaimWiring below exercises the actual reader.
            pid=self.plan['units']['crond.service']['mainPid']
            known=[] if pid==0 else [dict(pid=pid,startTicks=100)]
            binding=dict(schemaVersion='formal-host-derived-source/v1',hierarchy='systemd-v1',
                bootId=self.cap['bootId'],hierarchyId=7,
                mount=dict(mountId=50,device='0:1',root='/',target='/control/protocol-systemd',namespace='mnt:[7]'),
                units={u:dict(cgroupPath='/system.slice/'+u) for u in ('crond.service',*self.m.ASSOCIATED)},
                knownProcesses=known)
            def protocol(binding,digest,**kwargs):
                current=self.states['crond.service']['mainPid']
                return dict(processes=[] if current==0 else [dict(pid=current,startTicks=100)],
                    units={'crond.service':dict(MainPID=current,ControlPID=0)},
                    selectedScopeIdleObserved=current==0,fullHostFence=False,fullDrain=False,
                    releaseReady=False,productionEvidence=False)
            self.native.observe_related_work=protocol;self.native.observe_related_work_native=protocol
            self.m.bind_host_derived_sources(self.claim,binding,self.m._digest(binding),
                guard=lambda:None,native=self.native)
        self.bound=True
    def close_one(self,unit,guard=lambda:None):
        return self.m.close_host_admission(self.claim,unit,guard=guard,native=self.native)
    def close_all(self):
        for u in self.m.CLOSE_ORDER:self.close_one(u)
    def restore_one(self,u,guard=lambda:None):
        return self.m.restore_host_admission(self.claim,u,guard=guard,recovery_guard=lambda:None,native=self.native)
    def test_real_masks_closed_then_owned_restore_in_original_semantics(self):
        self.bind();self.close_all()
        self.assertEqual([u for a,u in self.calls if a=='stop'],list(self.m.CLOSE_ORDER))
        for u in self.m.RESTORE_ORDER:
            result=self.restore_one(u)
            self.assertFalse((self.runtime/u).is_symlink())
            self.assertFalse(result['fullHostFence']);self.assertFalse(result['fullDrain'])
        self.assertEqual([u for a,u in self.calls if a=='start'],list(self.m.RESTORE_ORDER))
        self.assertFalse(any(u.endswith('.service') and u!='crond.service' for a,u in self.calls if u))
    def test_bad_plan_digest_or_unknown_unit_does_not_write(self):
        before=set(self.claim.root.iterdir())
        with self.assertRaises(snapshot.FormalSnapshotError):
            self.m.bind_host_originals(self.claim,self.plan,'0'*64,guard=lambda:None,native=self.native)
        self.assertEqual(before,set(self.claim.root.iterdir()));self.assertEqual(self.calls,[])
        self.bind()
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one('docker.service')
        self.assertEqual(self.calls,[])
    def test_out_of_order_and_second_close_refuse_mutation(self):
        self.bind()
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(self.m.CLOSE_ORDER[1])
        self.assertEqual(self.calls,[]);self.close_one(self.m.CLOSE_ORDER[0]);before=list(self.calls)
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(self.m.CLOSE_ORDER[0])
        self.assertEqual(self.calls,before)
    def test_mask_effect_without_completion_refuses_replay_and_restore(self):
        self.bind();self.fail_action='mask';u=self.m.CLOSE_ORDER[0]
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(u)
        self.assertTrue((self.runtime/u).is_symlink());before=list(self.calls)
        self.fail_action=None
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(u)
        with self.assertRaises(snapshot.FormalSnapshotError):self.restore_one(u)
        self.assertEqual(before,self.calls);self.assertTrue((self.runtime/u).is_symlink())
    def test_stop_effect_without_completion_refuses_replay(self):
        self.bind();self.fail_action='stop';u=self.m.CLOSE_ORDER[0]
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(u)
        before=list(self.calls);self.fail_action=None
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(u)
        self.assertEqual(before,self.calls);self.assertEqual(self.states[u]['active'],'inactive')
    def test_foreign_equal_target_mask_is_not_removed(self):
        self.bind();self.close_all();u=self.m.RESTORE_ORDER[0]
        # Retain the old inode so replacement cannot reuse it.
        (self.runtime/u).rename(self.runtime/'retained-original-mask')
        os.symlink('/dev/null',self.runtime/u);before=list(self.calls)
        with self.assertRaises(snapshot.FormalSnapshotError):self.restore_one(u)
        self.assertEqual(before,self.calls);self.assertTrue((self.runtime/u).is_symlink())
    def test_original_inactive_and_masked_are_preserved(self):
        u=self.m.CLOSE_ORDER[1];self.states[u].update(active='inactive',sub='dead',originalMasked=True,
            loadState='masked',loadedConfigurationPaths=['/dev/null'],unitFileState='masked-runtime')
        os.symlink('/dev/null',self.runtime/u)
        self.plan['units'][u]=dict(self.native.observe(u,0),stopApproved=True,timerResumeApproved=True)
        self.digest=hashlib.sha256(snapshot._canonical_bytes(self.plan)).hexdigest();old=self.native.mask_identity(u)
        self.bind();self.close_all()
        for name in self.m.RESTORE_ORDER:self.restore_one(name)
        self.assertEqual(old,self.native.mask_identity(u));self.assertFalse(any(unit==u for a,unit in self.calls))
    def test_active_original_masked_refuses_before_binding(self):
        u=self.m.CLOSE_ORDER[0];self.plan['units'][u]['originalMasked']=True
        self.digest=hashlib.sha256(snapshot._canonical_bytes(self.plan)).hexdigest()
        with self.assertRaises(snapshot.FormalSnapshotError):self.bind()
        self.assertEqual(self.calls,[])
    def test_restore_before_recovery_guard_does_not_unlink(self):
        self.bind();self.close_all();u=self.m.RESTORE_ORDER[0];old=self.native.mask_identity(u)
        def refuse():raise snapshot.FormalSnapshotError('RECOVERY_NOT_READY')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'RECOVERY_NOT_READY'):
            self.m.restore_host_admission(self.claim,u,guard=lambda:None,recovery_guard=refuse,native=self.native)
        self.assertEqual(old,self.native.mask_identity(u))
    def test_timer_resume_unapproved_refuses_even_before_unlink(self):
        u=self.m.RESTORE_ORDER[0];self.plan['units'][u]['timerResumeApproved']=False
        self.digest=hashlib.sha256(snapshot._canonical_bytes(self.plan)).hexdigest();self.bind();self.close_all()
        old=self.native.mask_identity(u)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'TIMER_RESUME_SEMANTICS_UNAPPROVED'):self.restore_one(u)
        self.assertEqual(old,self.native.mask_identity(u))
    def test_final_guard_configuration_drift_refuses_completion(self):
        self.bind();u=self.m.CLOSE_ORDER[0]
        def drift():
            if ('stop',u) in self.calls:self.states[u]['configurationSha256']='0'*64
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(u,guard=drift)
        self.assertNotIn('host-crond-closed',{e['step'] for e in self.claim.events})
    def test_boot_change_refuses_before_mutation(self):
        self.bind();self.cap['bootId']='00000000-0000-0000-0000-000000000099'
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(self.m.CLOSE_ORDER[0])
        self.assertEqual(self.calls,[])
    def test_start_effect_without_completion_keeps_pending(self):
        self.bind();self.close_all();u=self.m.RESTORE_ORDER[0];self.fail_action='start'
        with self.assertRaises(snapshot.FormalSnapshotError):self.restore_one(u)
        self.fail_action=None;before=list(self.calls)
        with self.assertRaises(snapshot.FormalSnapshotError):self.restore_one(u)
        self.assertEqual(before,self.calls);self.assertNotIn('host-dnf-makecache-restored',{e['step'] for e in self.claim.events})
    def test_mask_inserted_by_other_owner_before_native_mask_is_never_adopted(self):
        self.bind();u=self.m.CLOSE_ORDER[0];original=self.native.create_owned_mask
        def raced(unit):
            os.symlink('/dev/null',self.runtime/unit)
            return original(unit)
        self.native.create_owned_mask=raced
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(u)
        self.assertTrue((self.runtime/u).is_symlink())
        self.assertNotIn('host-crond-masked',{e['step'] for e in self.claim.events})
    def test_last_guard_failure_does_not_leave_closed_completion(self):
        self.bind();u=self.m.CLOSE_ORDER[0]
        def late():
            if self.claim.events and self.claim.events[-1]['step']=='host-crond-closed':
                raise snapshot.FormalSnapshotError('LATE_GUARD_REFUSAL')
        try:self.close_one(u,guard=late)
        except snapshot.FormalSnapshotError:
            self.assertNotIn('host-crond-closed',{e['step'] for e in self.claim.events})
    def test_last_guard_failure_does_not_leave_restored_completion(self):
        self.bind();self.close_all();u=self.m.RESTORE_ORDER[0]
        def late():
            if self.claim.events and self.claim.events[-1]['step']=='host-dnf-makecache-restored':
                raise snapshot.FormalSnapshotError('LATE_GUARD_REFUSAL')
        try:self.restore_one(u,guard=late)
        except snapshot.FormalSnapshotError:
            self.assertNotIn('host-dnf-makecache-restored',{e['step'] for e in self.claim.events})
    def test_previously_closed_entry_reopened_blocks_next_entry(self):
        self.bind();first=self.m.CLOSE_ORDER[0];self.close_one(first)
        self.states[first].update(active='active',sub='running',mainPid=41)
        before=list(self.calls)
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(self.m.CLOSE_ORDER[1])
        self.assertEqual(before,self.calls)
    def test_equal_bytes_original_file_replacement_refuses(self):
        self.bind();p=self.claim.root/'host-originals.json';raw=p.read_bytes()
        p.rename(self.claim.root/'retained-original.json');p.write_bytes(raw);p.chmod(0o600)
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(self.m.CLOSE_ORDER[0])
        self.assertEqual(self.calls,[])
    def test_new_loaded_drop_in_refuses_even_when_original_files_unchanged(self):
        original=copy.deepcopy(self.plan['units']['crond.service'])
        original['loadedConfigurationPaths']=['/usr/lib/systemd/system/crond.service']
        current=copy.deepcopy(original)
        current['loadedConfigurationPaths'].append('/etc/systemd/system/crond.service.d/new.conf')
        with self.assertRaises(snapshot.FormalSnapshotError):self.m._source(current,original)
    def test_normal_pending_job_is_observed_without_replaying_stop(self):
        self.bind();u=self.m.CLOSE_ORDER[0];observe=self.native.observe;pending=[True]
        def during(unit,deadline,original=None):
            if unit==u and ('stop',u) in self.calls and pending[0]:
                pending[0]=False;raise snapshot.FormalSnapshotError('FORMAL_HOST_SYSTEMD_EFFECT_PENDING')
            return observe(unit,deadline,original)
        self.native.observe=during;self.close_one(u)
        self.assertEqual(self.calls.count(('stop',u)),1);self.assertFalse(pending[0])
    def test_final_native_read_exceeding_deadline_refuses_completion(self):
        self.bind();u=self.m.CLOSE_ORDER[0];observe=self.native.observe;clock=[0.0];stopped_reads=[0]
        def late(unit,deadline,original=None):
            result=observe(unit,deadline,original)
            if unit==u and ('stop',u) in self.calls:
                stopped_reads[0]+=1
                if stopped_reads[0]==2:clock[0]=100.0
            return result
        self.native.observe=late
        with patch.object(self.m.time,'monotonic',side_effect=lambda:clock[0]):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_HOST_DEADLINE'):self.close_one(u)
        self.assertNotIn('host-crond-closed',{e['step'] for e in self.claim.events})
    def test_stop_uses_original_loaded_contract_before_mask_reload(self):
        self.bind();u=self.m.CLOSE_ORDER[0];command=self.native.command
        def reload_drops_original(action,unit,deadline):
            command(action,unit,deadline)
            if action=='reload' and (self.runtime/u).is_symlink():
                self.states[u]['stopContractSha256']='0'*64
                self.states[u]['loadedConfigurationPaths']=['/dev/null']
                self.states[u]['loadState']='masked'
        self.native.command=reload_drops_original
        self.close_one(u)
        self.assertLess(self.calls.index(('stop',u)),self.calls.index(('reload',None)))
    def test_known_drop_in_survives_mask_reload_but_new_drop_in_is_refused(self):
        original=copy.deepcopy(self.plan['units']['crond.service'])
        original['loadedConfigurationPaths']=['/usr/lib/systemd/system/crond.service','/etc/systemd/system/crond.service.d/safe.conf']
        current=copy.deepcopy(original);current['mask']={'target':'/dev/null','signature':[1]};current['loadState']='masked'
        current['loadedConfigurationPaths']=['/dev/null',original['loadedConfigurationPaths'][1]]
        self.m._source(current,original)
        current['loadedConfigurationPaths'].append('/etc/systemd/system/crond.service.d/new.conf')
        with self.assertRaises(snapshot.FormalSnapshotError):self.m._source(current,original)
    def test_disk_mask_with_unreloaded_original_manager_refuses_before_bind(self):
        u=self.m.CLOSE_ORDER[1];os.symlink('/dev/null',self.runtime/u)
        self.states[u].update(active='inactive',sub='dead',originalMasked=True,unitFileState='masked-runtime')
        self.plan['units'][u]=dict(self.native.observe(u,0),stopApproved=True,timerResumeApproved=True)
        self.digest=hashlib.sha256(snapshot._canonical_bytes(self.plan)).hexdigest()
        with self.assertRaises(snapshot.FormalSnapshotError):self.bind()
        self.assertEqual(self.calls,[])
    def test_daemon_pid_change_after_mask_refuses_stop(self):
        self.bind();u=self.m.CLOSE_ORDER[0]
        def drift():
            if ('mask',u) in self.calls:self.states[u]['mainPid']=99
        with self.assertRaises(snapshot.FormalSnapshotError):self.close_one(u,guard=drift)
        self.assertNotIn(('stop',u),self.calls)
    def test_runtime_mask_with_persistent_cached_state_refuses_binding(self):
        u=self.m.CLOSE_ORDER[1];os.symlink('/dev/null',self.runtime/u)
        self.states[u].update(active='inactive',sub='dead',originalMasked=True,loadState='masked',
            loadedConfigurationPaths=['/dev/null'],unitFileState='masked')
        self.plan['units'][u]=dict(self.native.observe(u,0),stopApproved=True,timerResumeApproved=True)
        self.digest=hashlib.sha256(snapshot._canonical_bytes(self.plan)).hexdigest()
        with self.assertRaises(snapshot.FormalSnapshotError):self.bind()
        self.assertEqual(self.calls,[])
    def closed_checks(self):
        return self.m.ClosedHostAdmissionChecks(self.claim,native=self.native)
    def test_closed_checks_require_all_completed_before_observation(self):
        self.bind();self.close_one(self.m.CLOSE_ORDER[0]);before=list(self.calls)
        with self.assertRaises(snapshot.FormalSnapshotError):self.closed_checks()
        self.assertEqual(before,self.calls)
    def test_closed_checks_are_repeatable_readonly_and_do_not_claim_full_fence(self):
        self.bind();self.close_all();before=list(self.calls);events=copy.deepcopy(self.claim.events)
        checked=self.closed_checks()
        for _ in range(2):
            result=checked(guard=lambda:None)
            self.assertFalse(result['fullHostFence']);self.assertFalse(result['fullDrain'])
        self.assertEqual(before,self.calls);self.assertEqual(events,self.claim.events)
    def test_closed_checks_late_owner_reopens_previous_entry_refuses(self):
        self.bind();self.close_all();checked=self.closed_checks();u=self.m.CLOSE_ORDER[0]
        def reopen():self.states[u].update(active='active',sub='running',mainPid=41)
        with self.assertRaises(snapshot.FormalSnapshotError):checked(guard=reopen)
    def test_closed_checks_equal_bytes_completed_event_replacement_refuses(self):
        self.bind();self.close_all();checked=self.closed_checks()
        event=next(e for e in self.claim.events if e['step']=='host-crond-closed')
        p=self.claim.root/('event-%06d.json'%event['sequence']);raw=p.read_bytes()
        p.rename(p.with_suffix('.retained'));p.write_bytes(raw);p.chmod(0o600)
        with self.assertRaises(snapshot.FormalSnapshotError):checked(guard=lambda:None)
    def test_closed_checks_after_host_restore_started_refuses(self):
        self.bind();self.close_all();checked=self.closed_checks();self.restore_one(self.m.RESTORE_ORDER[0])
        with self.assertRaises(snapshot.FormalSnapshotError):checked(guard=lambda:None)
    def test_closed_checks_requires_manager_mask_loaded_not_only_disk_link(self):
        self.bind();self.close_all();checked=self.closed_checks()
        self.states[self.m.CLOSE_ORDER[0]]['loadState']='loaded'
        with self.assertRaises(snapshot.FormalSnapshotError):checked(guard=lambda:None)
    def test_closed_checks_foreign_equal_target_mask_refuses(self):
        self.bind();self.close_all();checked=self.closed_checks();u=self.m.CLOSE_ORDER[0]
        (self.runtime/u).rename(self.runtime/'retained-mask');os.symlink('/dev/null',self.runtime/u)
        with self.assertRaises(snapshot.FormalSnapshotError):checked(guard=lambda:None)
    def test_closed_checks_final_signatures_include_approved_configuration(self):
        self.bind();self.close_all();checked=self.closed_checks();armed=[]
        def changed(files):
            if armed:raise snapshot.FormalSnapshotError('OWNED_CONFIGURATION_CHANGED')
            return files
        self.native._configuration_signatures=changed
        checked.prepare(guard=lambda:None,deadline=time.monotonic()+10)
        checked.assert_native(deadline=time.monotonic()+10);armed.append(True)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_CONFIGURATION_CHANGED'):checked._signatures()
    def test_closed_checks_final_signatures_include_owned_mask_identity(self):
        self.bind();self.close_all();checked=self.closed_checks();u=self.m.CLOSE_ORDER[0]
        checked.prepare(guard=lambda:None,deadline=time.monotonic()+10)
        checked.assert_native(deadline=time.monotonic()+10)
        (self.runtime/u).rename(self.runtime/'retained-final-mask');os.symlink('/dev/null',self.runtime/u)
        with self.assertRaises(snapshot.FormalSnapshotError):checked._signatures()


class NativeProtocol(unittest.TestCase):
    def setUp(self):
        self.m=importlib.import_module('formal_host_admission')
        self.tmp=tempfile.TemporaryDirectory(prefix='owned-host-protocol-',dir='/control');self.root=Path(self.tmp.name)
        self.runtime=self.root/'runtime';self.runtime.mkdir();self.persistent=self.root/'persistent';self.persistent.mkdir()
        self.native=self.m.NativeHostAdmission(runtime_directory=self.runtime)
        self.native.persistent=self.persistent
        self.native._configuration=lambda paths:[{'path':p,'sha256':'a'*64,'signature':[1,2,0,0,33188,1,99,1,1]} for p in paths]
        self.native._links=lambda unit:'b'*64
        self.commands=[];self.jobs=b'';self.unit_file_raw=b's "enabled"\n';self.unit_file_calls=[]
        self.native._bus_run=lambda args,deadline,limit=65536:self.unit_file_calls.append(args) or self.unit_file_raw
        self.bus_calls=[];self.bus={
            'ExecStop':b'a(sasbttttuii) 0\n','ExecStopPost':b'a(sasbttttuii) 0\n',
            'TimersCalendar':b'a(sst) 0\n','TimersMonotonic':b'a(stt) 1 "OnBootUSec" 900000000 1900000000\n'}
        self.native._bus_property=lambda unit,interface,name,deadline:self.bus_calls.append((unit,interface,name)) or self.bus[name]
    def tearDown(self):self.tmp.cleanup()
    def props(self,unit='crond.service'):
        return dict(Id=unit,Names=unit,LoadState='loaded',ActiveState='active',SubState='running',UnitFileState='enabled',
            FragmentPath='/usr/lib/systemd/system/'+unit,DropInPaths='',MainPID='11',ControlPID='0',Job='0',
            KillMode='process',KillSignal='15',SendSIGKILL='no',TimeoutStopUSec='infinity',ExecStop='',ExecStopPost='',
            PartOf='',BindsTo='',Conflicts='shutdown.target',Before='',After='',PropagatesStopTo='')
    def read_properties(self,properties):
        raw=('\n'.join(k+'='+v for k,v in properties.items() if k in self.m.PROPERTIES)+'\n').encode()
        def manager(args,deadline,limit=1048576):
            self.commands.append(args)
            return raw if args[0]=='show' else self.jobs if args[0]=='list-jobs' else b''
        self.native._run=manager
        return self.native.observe(properties['Id'],time.monotonic()+5)
    def test_native_loaded_sources_are_retained_after_original_mode(self):
        p=self.props();original=self.read_properties(p);p['DropInPaths']='/etc/systemd/system/crond.service.d/exec.conf'
        raw=('\n'.join(k+'='+v for k,v in p.items() if k in self.m.PROPERTIES)+'\n').encode()
        self.native._run=lambda args,deadline,limit=1048576:raw if args[0]=='show' else b''
        current=self.native.observe('crond.service',time.monotonic()+5,original)
        self.assertEqual(current['configurationFiles'],original['configurationFiles'])
        self.assertIn(p['DropInPaths'],current['loadedConfigurationPaths'])
        with self.assertRaises(snapshot.FormalSnapshotError):self.m._source(current,original)
    def test_original_inactive_masked_cron_has_no_stop_contract_requirement(self):
        self.native.create_owned_mask('crond.service');p=self.props()
        self.unit_file_raw=b's "masked-runtime"\n'
        p.update(LoadState='masked',ActiveState='inactive',SubState='dead',MainPID='0',UnitFileState='masked-runtime',FragmentPath='/dev/null')
        for k in ('KillMode','KillSignal','SendSIGKILL','TimeoutStopUSec','ExecStop','ExecStopPost'):p.pop(k)
        result=self.read_properties(p);self.assertTrue(result['originalMasked']);self.assertEqual(result['active'],'inactive')
    def test_unknown_job_output_is_refused(self):
        self.jobs=b'opaque non-native output\n'
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'JOB_INVALID'):self.read_properties(self.props())
    def test_active_cron_strong_kill_or_bounded_daemon_contract_is_refused(self):
        for changes in ({'KillSignal':'9'}, {'SendSIGKILL':'yes'}, {'SendSIGKILL':''},
                        {'TimeoutStopUSec':'1min 30s'}, {'TimeoutStopUSec':''}):
            with self.subTest(changes=changes):
                p=self.props();p.update(KillSignal='15',SendSIGKILL='no',TimeoutStopUSec='infinity');p.update(changes)
                with self.assertRaisesRegex(snapshot.FormalSnapshotError,'CRON_CONTROLLED_STOP_UNSAFE'):
                    self.read_properties(p)
    def test_current_mask_cannot_hide_unsafe_original_active_cron_contract(self):
        p=self.props();p.update(SendSIGKILL='no',TimeoutStopUSec='infinity')
        original=self.read_properties(p);self.native.create_owned_mask('crond.service')
        self.unit_file_raw=b's "masked-runtime"\n'
        p.update(UnitFileState='masked-runtime',LoadState='masked',FragmentPath='/dev/null',SendSIGKILL='yes')
        raw=('\n'.join(k+'='+v for k,v in p.items() if k in self.m.PROPERTIES)+'\n').encode()
        self.native._run=lambda args,deadline,limit=1048576:raw if args[0]=='show' else b''
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'CRON_CONTROLLED_STOP_UNSAFE'):
            self.native.observe('crond.service',time.monotonic()+5,original)
    def masked_after_stop(self,changes=None):
        original=self.read_properties(self.props());self.native.create_owned_mask('crond.service')
        self.unit_file_raw=b's "masked-runtime"\n'
        p=self.props();p.update(LoadState='masked',ActiveState='inactive',SubState='dead',
            MainPID='0',ControlPID='0',UnitFileState='masked-runtime',FragmentPath='/dev/null')
        for key in ('KillMode','KillSignal','SendSIGKILL','TimeoutStopUSec','ExecStop','ExecStopPost'):
            p.pop(key)
        if changes:p.update(changes)
        raw=('\n'.join(k+'='+v for k,v in p.items() if k in self.m.PROPERTIES)+'\n').encode()
        self.native._run=lambda args,deadline,limit=1048576:raw if args[0]=='show' else b''
        self.bus.update(ExecStop=b'',ExecStopPost=b'');self.bus_calls.clear()
        return original,p
    def test_stopped_masked_manager_does_not_require_or_forge_original_stop_properties(self):
        original,_=self.masked_after_stop()
        current=self.native.observe('crond.service',time.monotonic()+5,original)
        self.m._source(current,original)
        self.assertEqual((current['active'],current['sub'],current['mainPid'],current['controlPid']),
            ('inactive','dead',0,0))
        self.assertNotEqual(current['stopContractSha256'],original['stopContractSha256'])
        self.assertEqual(self.bus_calls,[])
    def test_masked_missing_pid_fields_are_not_zero_defaults(self):
        for missing in ('MainPID','ControlPID'):
            with self.subTest(missing=missing):
                original,p=self.masked_after_stop();p.pop(missing)
                raw=('\n'.join(k+'='+v for k,v in p.items() if k in self.m.PROPERTIES)+'\n').encode()
                self.native._run=lambda args,deadline,limit=1048576:raw if args[0]=='show' else b''
                with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROPERTY_UNSUPPORTED'):
                    self.native.observe('crond.service',time.monotonic()+5,original)
                (self.runtime/'crond.service').unlink();self.unit_file_raw=b's "enabled"\n'
                self.bus.update(ExecStop=b'a(sasbttttuii) 0\n',ExecStopPost=b'a(sasbttttuii) 0\n')
    def test_mask_and_inactive_label_do_not_hide_live_pid_or_stop_transition(self):
        for changes in ({'MainPID':'11'},{'ControlPID':'12'},{'SubState':'stop-sigterm'},
                        {'ActiveState':'deactivating'},{'Job':'4'}):
            with self.subTest(changes=changes):
                original,_=self.masked_after_stop(changes)
                with self.assertRaises(snapshot.FormalSnapshotError):
                    self.native.observe('crond.service',time.monotonic()+5,original)
                (self.runtime/'crond.service').unlink();self.unit_file_raw=b's "enabled"\n'
                self.bus.update(ExecStop=b'a(sasbttttuii) 0\n',ExecStopPost=b'a(sasbttttuii) 0\n')
    def test_related_job_is_pending(self):
        self.jobs=b'7 cockpit.service start running\n'
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'SYSTEMD_EFFECT_PENDING'):self.read_properties(self.props())
    def test_stop_argv_is_nonblocking_job_fail_and_fixed_unit(self):
        self.native._run=lambda args,deadline,limit=1048576:self.commands.append(args) or b''
        self.native.command('stop','crond.service',time.monotonic()+2)
        self.assertEqual(self.commands,[['--quiet','--no-block','--job-mode=fail','stop','--','crond.service']])
        for action,unit in (('kill','crond.service'),('stop','cockpit.service'),('mask','crond.service')):
            with self.assertRaises(snapshot.FormalSnapshotError):self.native.command(action,unit,time.monotonic()+2)
    def test_missing_native_capability_fails_before_identity_read(self):
        self.native._run=lambda args,deadline,limit=1048576:b'systemd 239\n' if args==['--version'] else b'--no-block'
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'CAPABILITY_UNSUPPORTED'):
            self.native.capabilities(time.monotonic()+2)
    def test_real_atomic_mask_collision_keeps_foreign_inode(self):
        old=self.native.create_owned_mask('crond.service')
        with self.assertRaises(snapshot.FormalSnapshotError):self.native.create_owned_mask('crond.service')
        self.assertEqual(old,self.native.mask_identity('crond.service'))
    def test_timer_next_elapse_is_dynamic_while_source_schedule_stays_bound(self):
        p=self.props('dnf-makecache.timer')
        p.update(SubState='waiting',MainPID='0',Persistent='no',Triggers='dnf-makecache.service',
            TimersCalendar='',TimersMonotonic='{ OnBootUSec=15min ; next_elapse=15min }',
            AccuracyUSec='1min',RandomizedDelayUSec='0')
        before=self.read_properties(p)
        p['TimersMonotonic']='{ OnBootUSec=15min ; next_elapse=2h 15min }'
        after=self.read_properties(p)
        self.assertEqual(before['configurationSha256'],after['configurationSha256'])
        self.assertEqual(before['timerSemanticsSha256'],after['timerSemanticsSha256'])
    def test_omitted_show_empty_arrays_need_positive_typed_source(self):
        p=self.props();p.pop('ExecStop');p.pop('ExecStopPost')
        observed=self.read_properties(p)
        self.assertEqual(observed['active'],'active')
        self.assertEqual([v[2] for v in self.bus_calls],['ExecStop','ExecStopPost'])
    def test_nonempty_or_missing_typed_stop_array_refuses(self):
        p=self.props();p.pop('ExecStop');p.pop('ExecStopPost')
        for raw in (b'',b'a(sasbttttuii) 1 "/usr/bin/kill"\n',b'as 0\n'):
            self.bus['ExecStop']=raw
            with self.assertRaises(snapshot.FormalSnapshotError):self.read_properties(p)
    def test_multi_trigger_timer_uses_typed_arrays_without_duplicate_show_keys(self):
        p=self.props('dnf-makecache.timer');p.update(SubState='waiting',MainPID='0',Persistent='no',
            Triggers='dnf-makecache.service',AccuracyUSec='1min',RandomizedDelayUSec='0')
        self.bus['TimersMonotonic']=b'a(stt) 2 "OnBootUSec" 900000000 1900000000 "OnUnitActiveUSec" 3600000000 9000000000\n'
        def manager(args,deadline,limit=1048576):
            if args[0]!='show':return b''
            requested=args[2].removeprefix('--property=').split(',')
            lines=[k+'='+v for k,v in p.items() if k in requested]
            if 'TimersMonotonic' in requested:
                lines+=['TimersMonotonic={ OnBootUSec=15min ; next_elapse=15min }',
                        'TimersMonotonic={ OnUnitActiveUSec=1h ; next_elapse=2h }']
            return ('\n'.join(lines)+'\n').encode()
        self.native._run=manager
        observed=self.native.observe('dnf-makecache.timer',time.monotonic()+5)
        self.assertEqual(observed['active'],'active')
        self.assertEqual([x[2] for x in self.bus_calls],['TimersCalendar','TimersMonotonic'])
    def test_disk_unit_state_primary_uses_fixed_typed_manager_call(self):
        result=self.read_properties(self.props());self.assertEqual(result['unitFileState'],'enabled')
        self.assertEqual(self.unit_file_calls,[['--system','--no-pager','call','org.freedesktop.systemd1',
            '/org/freedesktop/systemd1','org.freedesktop.systemd1.Manager','GetUnitFileState','s','crond.service']])
    def test_initial_cached_unit_state_differs_from_disk_refuses(self):
        self.unit_file_raw=b's "disabled"\n'
        with self.assertRaises(snapshot.FormalSnapshotError):self.read_properties(self.props())
    def test_missing_or_unknown_typed_disk_unit_state_refuses(self):
        for raw in (b'',b'as 1 "enabled"\n',b's "enabled" "extra"\n',b's "unknown"\n',b's\n"enabled"\n',b's enabled\n'):
            self.unit_file_raw=raw
            with self.assertRaises(snapshot.FormalSnapshotError):self.read_properties(self.props())
    def test_owned_mask_before_reload_retains_original_loaded_object(self):
        original=self.read_properties(self.props());self.native.create_owned_mask('crond.service')
        self.unit_file_raw=b's "masked-runtime"\n'
        current=self.native.observe('crond.service',time.monotonic()+5,original)
        self.assertEqual(current['loadState'],'loaded');self.assertEqual(current['unitFileState'],'masked-runtime')
        self.m._source(current,original)


class RelatedWorkSources(unittest.TestCase):
    """Real bounded/no-follow files; explicit systemd/proc/cgroup protocol fixture."""
    def setUp(self):
        self.m=importlib.import_module('formal_host_admission')
        self.tmp=tempfile.TemporaryDirectory(prefix='owned-related-work-',dir='/control')
        self.root=Path(self.tmp.name);self.proc=self.root/'proc';self.proc.mkdir()
        self.cg=self.root/'cgroup';self.cg.mkdir();self.native=self.m.NativeHostAdmission()
        self.units=('crond.service',*self.m.ASSOCIATED)
        self.boot='00000000-0000-0000-0000-000000000002'
        boot=self.proc/'sys/kernel/random';boot.mkdir(parents=True)
        (boot/'boot_id').write_text(self.boot+'\n')
        self.observer=self.proc/str(os.getpid());self.observer.mkdir()
        (self.observer/'ns').mkdir();os.symlink('mnt:[7]',self.observer/'ns/mnt')
        dev=self.cg.stat().st_dev;device=str(os.major(dev))+':'+str(os.minor(dev))
        self.mount=dict(mountId=50,device=device,root='/',target=str(self.cg),namespace='mnt:[7]')
        self.mount_line='50 1 '+device+' / '+str(self.cg)+' rw - cgroup cgroup rw,name=systemd\n'
        (self.observer/'mountinfo').write_text(self.mount_line)
        self.properties={};self.groups={};self.calls=[]
        for unit in self.units:
            path='/system.slice/'+unit;group=self.cg/path.lstrip('/');group.mkdir(parents=True)
            (group/'cgroup.procs').write_bytes(b'');self.groups[unit]=group
            self.properties[unit]=dict(Id=unit,LoadState='loaded',ActiveState='inactive',SubState='dead',
                Job='(uo) 0 "/"\n',InvocationID='ay 16 '+' '.join(['0']*16)+'\n',
                MainPID=0,ControlPID=0,ControlGroup='')
        def bus(args,deadline,limit=65536):
            self.calls.append(args)
            unit=args[-3].rsplit('/',1)[1].replace('_2e','.').replace('_2d','-')
            key=args[-1];value=self.properties[unit][key]
            if key in ('Job','InvocationID'):return value.encode()
            if key in ('MainPID','ControlPID'):return ('u '+str(value)+'\n').encode()
            return ('s "'+str(value)+'"\n').encode()
        self.native._bus_run=bus
        self.binding=dict(schemaVersion='formal-host-derived-source/v1',hierarchy='systemd-v1',
            bootId=self.boot,hierarchyId=7,mount=copy.deepcopy(self.mount),
            units={u:dict(cgroupPath='/system.slice/'+u) for u in self.units},knownProcesses=[])
    def tearDown(self):self.tmp.cleanup()
    def process(self,pid=41,ticks=100,path='/system.slice/crond.service',cgroup=None):
        base=self.proc/str(pid);base.mkdir(exist_ok=True)
        fields=['S','1']+['0']*17+[str(ticks)]+['0']*8
        (base/'stat').write_text(str(pid)+' (owned ) source) '+' '.join(fields)+'\n')
        (base/'status').write_text('Name:\towned\nTgid:\t'+str(pid)+'\nPid:\t'+str(pid)+'\n')
        (base/'cgroup').write_text(cgroup if cgroup is not None else '7:name=systemd:'+path+'\n')
        (base/'mountinfo').write_text('1 0 0:1 / / rw - tmpfs tmpfs rw\n')
        (base/'ns').mkdir(exist_ok=True)
        for key,value in (('pid','pid:[8]'),('mnt','mnt:[9]')):
            if not (base/'ns'/key).is_symlink():os.symlink(value,base/'ns'/key)
        return base
    def member(self,pid=41):
        self.process(pid);(self.groups['crond.service']/'cgroup.procs').write_text(str(pid)+'\n')
    def observe(self,guard=lambda:None,idle=False,allow_absence=False):
        digest=hashlib.sha256(snapshot._canonical_bytes(self.binding)).hexdigest()
        extra={'allow_bound_process_absence':True} if allow_absence else {}
        return self.native.observe_related_work(self.binding,digest,guard=guard,
            deadline=time.monotonic()+10,proc_directory=self.proc,require_idle=idle,**extra)
    def test_selected_member_identity_preserves_hierarchy_and_no_private_process_content(self):
        self.member();result=self.observe();row=result['processes'][0]
        self.assertEqual((row['pid'],row['parentPid'],row['startTicks']),(41,1,100))
        self.assertEqual(row['cgroups'],[dict(hierarchyId=7,controllers=['name=systemd'],path='/system.slice/crond.service')])
        self.assertEqual(row['mountNamespace'],'mnt:[9]');self.assertEqual(row['pidNamespace'],'pid:[8]')
        self.assertEqual(row['mountinfoSha256'],hashlib.sha256((self.proc/'41/mountinfo').read_bytes()).hexdigest())
        self.assertFalse(result['selectedScopeIdleObserved']);self.assertFalse(result['fullDrain'])
        self.assertNotIn('cmdline',str(result));self.assertNotIn('environ',str(result))
    def test_stopped_cron_with_live_child_is_pending_without_killing_or_mutation(self):
        self.member();before=(self.proc/'41/stat').read_bytes()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_WORK_PENDING'):self.observe(idle=True)
        self.assertEqual((self.proc/'41/stat').read_bytes(),before)
        self.assertTrue(all(args[:3]==['--system','--no-pager','get-property'] for args in self.calls))
    def test_idle_selected_scope_never_claims_full_fence_or_business_drain(self):
        result=self.observe(idle=True)
        self.assertTrue(result['selectedScopeIdleObserved'])
        self.assertTrue(all(result[key] is False for key in ('fullHostFence','fullDrain','releaseReady','productionEvidence','serviceActionsPerformed')))
    def test_known_live_process_escaping_original_cgroup_is_unresolved(self):
        self.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        self.process(path='/other.scope')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_SCOPE_CHANGED'):self.observe()
    def test_same_pid_with_new_start_ticks_does_not_rebase_known_writer(self):
        self.member();self.binding['knownProcesses']=[dict(pid=41,startTicks=99)]
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_IDENTITY_CHANGED'):self.observe()
    def test_known_process_disappearance_does_not_claim_safe_terminal(self):
        self.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_UNAVAILABLE'):self.observe(idle=True)
    def test_controlled_idle_observation_retains_binding_and_records_only_absence(self):
        self.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        before=copy.deepcopy(self.binding)
        result=self.observe(idle=True,allow_absence=True)
        self.assertEqual(result['boundProcessesAbsent'],before['knownProcesses'])
        self.assertEqual(self.binding,before);self.assertEqual(result['processes'],[])
        self.assertTrue(result['selectedScopeIdleObserved'])
        self.assertTrue(all(result[k] is False for k in ('fullHostFence','fullDrain','releaseReady','productionEvidence','serviceActionsPerformed')))
    def test_controlled_absence_is_not_allowed_during_initial_running_scope(self):
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_ABSENCE_REQUIRES_IDLE'):
            self.observe(idle=False,allow_absence=True)
        self.assertEqual(self.calls,[])
    def test_controlled_absence_does_not_accept_reused_or_migrated_bound_pid(self):
        self.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        self.process(ticks=101)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_IDENTITY_CHANGED'):
            self.observe(idle=True,allow_absence=True)
        self.process(ticks=100,path='/other.scope')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_SCOPE_CHANGED'):
            self.observe(idle=True,allow_absence=True)
    def test_controlled_absence_requires_inactive_units_and_empty_groups(self):
        self.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        self.properties['crond.service'].update(ActiveState='active',SubState='running',MainPID=41)
        with self.assertRaises(snapshot.FormalSnapshotError):self.observe(idle=True,allow_absence=True)
        self.properties['crond.service'].update(ActiveState='inactive',SubState='dead',MainPID=0)
        self.member(42)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_WORK_PENDING'):
            self.observe(idle=True,allow_absence=True)
    def test_controlled_absence_permission_error_cannot_become_missing_pid(self):
        self.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        original=os.stat
        def denied(path,*args,**kwargs):
            if path=='41' and kwargs.get('dir_fd') is not None:raise PermissionError('fixture')
            return original(path,*args,**kwargs)
        with patch('formal_host_derived_work.os.stat',side_effect=denied):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_SOURCE_UNAVAILABLE'):
                self.observe(idle=True,allow_absence=True)
    def test_controlled_second_sample_new_bound_pid_is_refused(self):
        self.binding['knownProcesses']=[dict(pid=41,startTicks=100)];calls=[]
        def changed():
            calls.append(True)
            if len(calls)==2:self.member()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_SOURCE_CHANGED'):
            self.observe(changed,idle=True,allow_absence=True)
    def test_last_guard_new_cgroup_member_refuses_consistent_snapshot(self):
        self.member();calls=[]
        def change():
            calls.append(True)
            if len(calls)==2:
                self.process(42);(self.groups['crond.service']/'cgroup.procs').write_text('41\n42\n')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'SOURCE_CHANGED'):self.observe(change)
    def test_equal_content_group_directory_replacement_refuses(self):
        calls=[]
        def change():
            calls.append(True)
            if len(calls)==2:
                group=self.groups['crond.service'];group.rename(group.with_name('retained-crond'))
                group.mkdir();(group/'cgroup.procs').write_bytes(b'')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'SOURCE_CHANGED'):self.observe(change)
    def test_current_observer_namespace_change_refuses(self):
        calls=[]
        def change():
            calls.append(True)
            if len(calls)==2:
                (self.observer/'ns/mnt').unlink();os.symlink('mnt:[70]',self.observer/'ns/mnt')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'MOUNT_BINDING_CHANGED'):self.observe(change)
    def test_explicit_v2_binding_refuses_before_any_bus_or_file_observation(self):
        self.binding['hierarchy']='v2-domain'
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'HIERARCHY_UNSUPPORTED'):self.observe()
        self.assertEqual(self.calls,[])
    def test_equal_path_wrong_controller_or_hierarchy_is_not_selected_source(self):
        self.member()
        for raw in ('0::/system.slice/crond.service\n','8:name=systemd:/system.slice/crond.service\n','7:cpu:/system.slice/crond.service\n'):
            (self.proc/'41/cgroup').write_text(raw)
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_SCOPE_CHANGED'):self.observe()
    def test_symlink_or_fifo_procs_is_refused_without_blocking(self):
        path=self.groups['crond.service']/'cgroup.procs'
        path.unlink();os.symlink('/dev/null',path)
        with self.assertRaises(snapshot.FormalSnapshotError):self.observe()
        path.unlink();os.mkfifo(path)
        with self.assertRaises(snapshot.FormalSnapshotError):self.observe()
    def test_mount_device_change_or_unapproved_child_mount_refuses(self):
        (self.observer/'mountinfo').write_text(self.mount_line.replace(self.mount['device'],'0:99999',1))
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'MOUNT_BINDING_CHANGED'):self.observe()
        (self.observer/'mountinfo').write_text(self.mount_line+'51 50 '+self.mount['device']+' / '+str(self.groups['crond.service'])+' rw - cgroup cgroup rw,name=systemd\n')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'MOUNT_BINDING_CHANGED'):self.observe()
    def test_typed_service_pid_and_exact_unit_identity_are_required(self):
        self.properties['cockpit.service']['Id']='other.service'
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'UNIT_SOURCE_INVALID'):self.observe()
        self.properties['cockpit.service']['Id']='cockpit.service';bus=self.native._bus_run
        self.native._bus_run=lambda args,deadline,limit=65536:b's "0"\n' if args[-1]=='MainPID' else bus(args,deadline,limit)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'UNIT_SOURCE_INVALID'):self.observe()
    def test_expired_deadline_refuses_before_bus_observation(self):
        digest=hashlib.sha256(snapshot._canonical_bytes(self.binding)).hexdigest()
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_DEADLINE'):
            self.native.observe_related_work(self.binding,digest,guard=lambda:None,
                deadline=time.monotonic()-1,proc_directory=self.proc)
        self.assertEqual(self.calls,[])

    def test_main_pid_cannot_be_attributed_to_another_selected_service(self):
        self.member()
        self.properties['cockpit.service'].update(MainPID=41,ControlGroup='/system.slice/cockpit.service')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_SCOPE_CHANGED'):self.observe()
    def test_directory_limit_applies_to_the_whole_fixed_source_scope(self):
        for group in self.groups.values():
            for index in range(26):
                child=group/('child-'+str(index));child.mkdir();(child/'cgroup.procs').write_bytes(b'')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_SOURCE_LIMIT'):self.observe(idle=True)
    def test_pseudo_file_byte_limit_refuses_before_process_source_reads(self):
        (self.groups['crond.service']/'cgroup.procs').write_bytes(b'41\n'*22000)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_SOURCE_LIMIT'):self.observe()
    def test_zero_job_id_cannot_mask_a_nonempty_systemd_job_path(self):
        self.properties['cockpit.service']['Job']='(uo) 0 "/org/freedesktop/systemd1/job/7"\n'
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'UNIT_SOURCE_INVALID'):self.observe(idle=True)


class DerivedClaimWiring(unittest.TestCase):
    """Real same-claim files and real derived reader over explicit Linux sources."""
    def setUp(self):
        self.host=HostAdmission();self.host.setUp()
        self.host.defer_derived_binding=True
        self.related=RelatedWorkSources();self.related.setUp()
        self.m=self.host.m;self.claim=self.host.claim;self.native=self.host.native
        # An originally empty selected scope is a legitimate finite case.
        for unit in self.m.CLOSE_ORDER:
            self.host.states[unit].update(active='inactive',sub='dead',mainPid=0)
            self.host.plan['units'][unit]=dict(self.native.observe(unit,0),stopApproved=True,
                timerResumeApproved=unit.endswith('.timer'))
        self.host.digest=hashlib.sha256(snapshot._canonical_bytes(self.host.plan)).hexdigest()
        self.host.bind()
        self.native.observe_related_work=lambda binding,digest,**kwargs:self.related.native.observe_related_work(
            binding,digest,proc_directory=self.related.proc,**kwargs)
        self.native.observe_related_work_native=lambda binding,digest,**kwargs:self.related.native.observe_related_work_native(
            binding,digest,proc_directory=self.related.proc,**kwargs)
    def tearDown(self):
        self.host.tearDown();self.related.tearDown()
    def bind(self,binding=None,digest=None):
        binder=getattr(self.m,'bind_host_derived_sources',None)
        self.assertTrue(callable(binder),'normal host module lacks same-claim derived source binder')
        binding=self.related.binding if binding is None else binding
        digest=hashlib.sha256(snapshot._canonical_bytes(binding)).hexdigest() if digest is None else digest
        return binder(self.claim,binding,digest,guard=lambda:None,native=self.native)
    def closed(self):
        self.bind();self.host.close_all()
        return self.m.ClosedHostAdmissionChecks(self.claim,native=self.native)
    def test_missing_derived_binding_refuses_before_host_mask(self):
        before=set(self.claim.root.iterdir())
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_ORIGINALS_REQUIRED'):
            self.host.close_one(self.m.CLOSE_ORDER[0])
        self.assertEqual(before,set(self.claim.root.iterdir()));self.assertEqual(self.host.calls,[])
    def test_closed_checker_cannot_accept_only_five_entry_projection(self):
        # Retain legacy events as an explicit old-operation fixture.
        for unit in self.m.CLOSE_ORDER:
            original=self.host.plan['units'][unit]
            mask=self.native.create_owned_mask(unit)
            self.host.states[unit].update(loadState='masked',unitFileState='masked-runtime',loadedConfigurationPaths=['/dev/null'])
            self.claim.record(self.m._step(unit,'-closed'),{'originalSha256':self.m._digest(original),'ownedMask':mask})
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_ORIGINALS_REQUIRED'):
            self.m.ClosedHostAdmissionChecks(self.claim,native=self.native)
    def test_approved_empty_sources_are_sealed_and_all_flags_remain_false(self):
        result=self.bind()
        self.assertEqual(result['state'],'HOST_DERIVED_ORIGINALS_BOUND_ONLY')
        self.assertTrue(all(result[k] is False for k in ('fullHostFence','fullDrain','releaseReady','productionEvidence')))
        row=next(e for e in self.claim.events if e['step']=='host-derived-originals-bound')
        path=self.claim.root/'host-derived-originals.json'
        self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),row['details']['sha256'])
    def test_wrong_sha_refuses_without_source_write(self):
        before=set(self.claim.root.iterdir())
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_BINDING_CHANGED'):self.bind(digest='0'*64)
        self.assertEqual(before,set(self.claim.root.iterdir()))
    def test_live_process_cannot_be_omitted_from_known_original_set(self):
        self.related.member()
        before=set(self.claim.root.iterdir())
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_KNOWN_SET_CHANGED'):self.bind()
        self.assertEqual(before,set(self.claim.root.iterdir()))
    def test_boot_identity_must_match_original_host_claim(self):
        self.related.binding['bootId']='00000000-0000-0000-0000-000000000099'
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_BOOT_CHANGED'):self.bind()
    def test_second_binding_cannot_replace_original(self):
        self.bind();before={p.name:p.read_bytes() for p in self.claim.root.iterdir() if p.is_file()}
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_ACTION_ALREADY_STARTED'):self.bind()
        self.assertEqual(before,{p.name:p.read_bytes() for p in self.claim.root.iterdir() if p.is_file()})
    def test_binding_after_host_intent_is_rejected(self):
        self.claim.record('host-crond-mask-intent',{'originalSha256':'a'*64})
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_BINDING_TOO_LATE'):self.bind()
    def test_last_guard_host_intent_cannot_seal_late_sources(self):
        guards=[]
        def late():
            guards.append(True)
            if len(guards)==3:self.claim.record('host-crond-mask-intent',{'originalSha256':'a'*64})
        binder=getattr(self.m,'bind_host_derived_sources',None)
        self.assertTrue(callable(binder))
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_BINDING_TOO_LATE'):
            binder(self.claim,self.related.binding,self.m._digest(self.related.binding),guard=late,native=self.native)
        self.assertFalse((self.claim.root/'host-derived-originals.json').exists())
        self.assertNotIn('host-derived-originals-bound',{e['step'] for e in self.claim.events})
    def test_consumer_rejects_derived_event_after_host_action(self):
        self.claim.record('host-crond-mask-intent',{'originalSha256':'a'*64})
        original=dict(schemaVersion='formal-host-derived-originals/v1',hostOriginalsSha256=
            self.m._event(self.claim,'host-originals-bound')['sha256'],binding=self.related.binding,
            bindingSha256=self.m._digest(self.related.binding),observationSha256='a'*64)
        raw=snapshot._canonical_bytes(original);path=self.claim.root/'host-derived-originals.json'
        self.claim._write(path,raw)
        import formal_boot_state as boot
        pair=boot._read_secure(path,0o600,65536,no_atime=True)
        self.claim.record('host-derived-originals-bound',{'sha256':self.m._sha(raw),'signature':list(pair[1])})
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_BINDING_TOO_LATE'):
            self.m._derived_bound(self.claim)
    def test_idle_checker_reads_derived_scope_in_prepare_and_native_tail(self):
        checked=self.closed();calls=[];normal=self.native.observe_related_work;native=self.native.observe_related_work_native
        def observe(*args,**kwargs):calls.append('prepare');return normal(*args,**kwargs)
        def final(*args,**kwargs):
            self.assertNotIn('guard',kwargs);calls.append('native');return native(*args,**kwargs)
        self.native.observe_related_work=observe;self.native.observe_related_work_native=final
        result=checked(guard=lambda:calls.append('owner'))
        self.assertIn('prepare',calls);self.assertEqual(calls[-1],'native')
        self.assertTrue(all(result[k] is False for k in ('fullHostFence','fullDrain','releaseReady','productionEvidence')))
    def test_stopped_entries_with_new_live_child_refuse_without_stopping_it(self):
        checked=self.closed();self.related.member();before=list(self.host.calls)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_WORK_PENDING'):checked(guard=lambda:None)
        self.assertEqual(before,self.host.calls);self.assertTrue((self.related.proc/'41/stat').is_file())
    def test_known_process_disappearance_is_unresolved(self):
        self.related.member();self.related.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        self.bind();self.host.close_all();checked=self.m.ClosedHostAdmissionChecks(self.claim,native=self.native)
        (self.related.proc/'41').rename(self.related.proc/'retained-41')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_UNAVAILABLE'):checked(guard=lambda:None)
    def test_controlled_checker_accepts_bound_absence_only_after_entries_and_groups_close(self):
        self.related.member();self.related.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        self.bind();self.host.close_all()
        (self.related.proc/'41').rename(self.related.proc/'retained-41')
        (self.related.groups['crond.service']/'cgroup.procs').write_bytes(b'')
        checked=self.m.ClosedHostAdmissionChecks(self.claim,native=self.native,controlled_stop=True)
        result=checked(guard=lambda:None)
        self.assertTrue(all(result[k] is False for k in ('fullHostFence','fullDrain','releaseReady','productionEvidence')))
        self.assertEqual(self.m._derived_bound(self.claim)[0]['binding']['knownProcesses'],[dict(pid=41,startTicks=100)])
    def test_known_pid_start_ticks_change_is_unresolved(self):
        self.related.member();self.related.binding['knownProcesses']=[dict(pid=41,startTicks=100)]
        self.bind();self.host.close_all();checked=self.m.ClosedHostAdmissionChecks(self.claim,native=self.native)
        self.related.process(ticks=101)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'PROCESS_IDENTITY_CHANGED'):checked(guard=lambda:None)
    def test_equal_bytes_source_file_replacement_cannot_rebase_checker(self):
        checked=self.closed();path=self.claim.root/'host-derived-originals.json';raw=path.read_bytes()
        path.rename(path.with_suffix('.retained'));path.write_bytes(raw);path.chmod(0o600)
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'DERIVED_ORIGINAL_BINDING_CHANGED'):checked(guard=lambda:None)
    def test_native_tail_rejects_namespace_change_without_owner_callback(self):
        checked=self.closed();checked.prepare(guard=lambda:None,deadline=time.monotonic()+10)
        (self.related.observer/'ns/mnt').unlink();os.symlink('mnt:[70]',self.related.observer/'ns/mnt')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'MOUNT_BINDING_CHANGED'):
            checked.assert_native(deadline=time.monotonic()+10)


class RestoredPrefixProcessProtocol(unittest.TestCase):
    """Deterministic recovery observations; no claim, clock or systemd action."""

    def setUp(self):
        self.m=importlib.import_module('formal_host_admission')
        self.first='crond.service';self.next='cockpit.socket'
        original=dict(loadState='loaded',mask=None,persistentMask=None,active='active',sub='running',
            mainPid=41,controlPid=0,unitFileState='enabled',configurationSha256='a'*64,
            configurationFiles=[],loadedConfigurationPaths=['/usr/lib/systemd/system/crond.service'],
            linksSha256='b'*64,stopContractSha256='c'*64,timerSemanticsSha256=None)
        self.plan={'units':{self.first:original,self.next:copy.deepcopy(original)}}
        self.current=copy.deepcopy(self.plan['units']);self.observed=[]
        def observe(unit,deadline,original):
            self.observed.append(unit)
            return copy.deepcopy(self.current[unit])
        self.native=type('RecoveryObservations',(),{})();self.native.observe=observe

    def inactive(self):
        for value in (self.plan['units'][self.first],self.current[self.first]):
            value.update(active='inactive',sub='dead',mainPid=0)

    def select_first(self,unit):
        original=self.plan['units'].pop(self.first);current=self.current.pop(self.first)
        following=self.plan['units'].pop(self.next);following_current=self.current.pop(self.next)
        self.first=unit;self.next='crond.service' if unit=='cockpit.socket' else 'cockpit.socket'
        for value in (original,current):
            value['loadedConfigurationPaths']=['/usr/lib/systemd/system/'+unit]
        self.plan['units']={self.first:original,self.next:following}
        self.current={self.first:current,self.next:following_current}

    def prefix(self):
        self.m._prefix(None,self.plan,self.native,[self.first,self.next],0,restored=True)

    def refuses_before_next_entry(self):
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'^FORMAL_HOST_PREVIOUS_ENTRY_CHANGED$'):
            self.prefix()
        self.assertEqual(self.observed,[self.first])

    def test_inactive_service_with_new_main_pid_refuses_next_entry(self):
        self.inactive();self.current[self.first]['mainPid']=903
        self.refuses_before_next_entry()

    def test_active_service_with_control_pid_refuses_next_entry(self):
        self.current[self.first]['controlPid']=904
        self.refuses_before_next_entry()

    def test_inactive_timer_with_control_pid_refuses_next_entry(self):
        self.select_first('dnf-makecache.timer')
        self.inactive()
        for value in (self.plan['units'][self.first],self.current[self.first]):
            value['timerSemanticsSha256']='d'*64
        self.current[self.first]['controlPid']=905
        self.refuses_before_next_entry()

    def test_active_socket_with_control_pid_refuses_next_entry(self):
        self.select_first('cockpit.socket')
        for value in (self.plan['units'][self.first],self.current[self.first]):
            value.update(sub='listening',mainPid=0)
        self.current[self.first]['controlPid']=906
        self.refuses_before_next_entry()

    def test_active_service_accepts_new_restored_main_pid(self):
        self.current[self.first]['mainPid']=987
        self.prefix();self.assertEqual(self.observed,[self.first,self.next])

    def test_inactive_service_accepts_zero_main_and_control_pids(self):
        self.inactive()
        self.prefix();self.assertEqual(self.observed,[self.first,self.next])

    def test_active_timer_accepts_zero_main_and_control_pids(self):
        self.select_first('dnf-makecache.timer')
        for value in (self.plan['units'][self.first],self.current[self.first]):
            value.update(sub='waiting',mainPid=0,timerSemanticsSha256='d'*64)
        self.prefix();self.assertEqual(self.observed,[self.first,self.next])

    def test_configuration_drift_still_refuses_before_next_entry(self):
        self.current[self.first]['configurationSha256']='e'*64
        with self.assertRaisesRegex(snapshot.FormalSnapshotError,'^FORMAL_HOST_ORIGINAL_BINDING_CHANGED$'):
            self.prefix()
        self.assertEqual(self.observed,[self.first])


if __name__=='__main__':unittest.main()
