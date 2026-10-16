"""Owned Linux files/processes; capacity enforcement, not server/NAS proof."""
import gzip
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
import unittest

sys.path.insert(0,str(Path(__file__).parents[1]/'linux'))
import formal_snapshot_linux as snapshot


class CaptureBudgetTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(prefix='entry-bound-',dir='/root')
        self.addCleanup(self.tmp.cleanup)
        self.parent=Path(self.tmp.name)
        plan=dict(operationId='owned-budget',sourceFingerprint='1'*64,
            databaseUuid='11111111-1111-1111-1111-111111111111',minioContainerId='2'*64)
        self.claim=snapshot.FormalMaintenanceClaim(self.parent/'claim',plan)
        self.addCleanup(self.claim.close)
        self.owner=threading.get_ident();self.revoked=False;self.on_guard=None

    def guard(self):
        self.assertEqual(threading.get_ident(),self.owner)
        if self.revoked:raise snapshot.FormalSnapshotError('OWNED_REVOKED')
        if self.on_guard is not None:self.on_guard()

    def make(self,component=1024,total=8192,files=20):
        import formal_capture_budget as module
        policy=dict(schemaVersion='release-formal-capture-output-policy/v1',
            componentBytes={k:component for k in module.COMPONENTS},totalBytes=total,
            maxFiles=files,maxDirectories=20,maxPathBytes=512)
        raw=snapshot._canonical_bytes(policy)
        return module.CaptureBudget(self.claim,raw,hashlib.sha256(raw).hexdigest(),
            attempt_id='owned-attempt',source_context_sha256='3'*64,
            guard=self.guard,deadline=time.monotonic()+10)

    def test_limit_checked_before_excess_reaches_actual_file(self):
        budget=self.make(component=8)
        path=self.claim.root/'bound.bin'
        with budget.open(path,'proof') as out:
            out.write(b'12345678')
            with self.assertRaises(snapshot.FormalSnapshotError):out.write(b'9')
        self.assertEqual(path.read_bytes(),b'12345678')
        self.assertEqual(budget.receipt()['writtenBytes'],8)

    def test_aggregate_across_files_and_failed_artifacts_not_refunded(self):
        budget=self.make(component=20,total=10)
        with budget.open(self.claim.root/'one','proof') as out:out.write(b'123456')
        with budget.open(self.claim.root/'two','config') as out:
            with self.assertRaises(snapshot.FormalSnapshotError):out.write(b'12345')
        self.assertTrue((self.claim.root/'two').exists())
        self.assertEqual(budget.receipt()['writtenBytes'],6)

    def test_gzip_header_and_footer_are_real_bounded_bytes(self):
        budget=self.make(component=16)
        path=self.claim.root/'compressed'
        with self.assertRaises(snapshot.FormalSnapshotError):
            with budget.open(path,'sql-gzip') as out:
                with gzip.GzipFile(fileobj=out,mode='wb',mtime=0,filename='') as encoded:
                    encoded.write(b'x'*300)
        self.assertLessEqual(path.stat().st_size,16)

    def test_count_path_and_unknown_category_fail_before_creation(self):
        budget=self.make(files=1)
        with budget.open(self.claim.root/'one','proof') as out:out.write(b'a')
        with self.assertRaises(snapshot.FormalSnapshotError):budget.open(self.claim.root/'two','proof')
        self.assertFalse((self.claim.root/'two').exists())
        with self.assertRaises(snapshot.FormalSnapshotError):budget.open(self.parent/'escape','proof')
        with self.assertRaises(snapshot.FormalSnapshotError):budget.open(self.claim.root/'bad','unknown')

    def test_retained_claim_fd_owner_and_deadline_required(self):
        budget=self.make()
        with budget.open(self.claim.root/'revoked','proof') as out:
            self.revoked=True
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'OWNED_REVOKED'):out.write(b'a')
        self.revoked=False;self.claim.close()
        with self.assertRaises(snapshot.FormalSnapshotError):budget.receipt()

    def test_callback_mutation_is_rechecked_before_actual_write(self):
        budget=self.make(component=8)
        path=self.claim.root/'callback'
        with budget.open(path,'proof') as out:
            def mutate():budget.policy['componentBytes']['proof']=100
            self.on_guard=mutate
            with self.assertRaises(snapshot.FormalSnapshotError):out.write(b'x'*9)
        self.assertEqual(path.stat().st_size,0)
        self.assertEqual(budget.total,0)

    def test_replaced_output_path_cannot_write_to_detached_original_fd(self):
        budget=self.make();path=self.claim.root/'output'
        with budget.open(path,'proof') as out:
            path.rename(self.claim.root/'detached')
            path.write_bytes(b'replacement');path.chmod(0o600)
            with self.assertRaises(snapshot.FormalSnapshotError):out.write(b'unsafe')
        self.assertEqual((self.claim.root/'detached').stat().st_size,0)

    def test_link_cannot_bypass_reserved_control_file_limit(self):
        budget=self.make(component=300000,total=1000000,files=17)
        budget.reserve_watchdog(self.claim.root/'watchdog')
        path=self.claim.root/'one'
        with budget.open(path,'proof') as out:out.write(b'a')
        with self.assertRaises(snapshot.FormalSnapshotError):budget.link(path,self.claim.root/'alias')
        self.assertFalse((self.claim.root/'alias').exists())

    def test_receipt_refuses_unbounded_reserved_child_records(self):
        budget=self.make(component=300000,total=1000000,files=30)
        root=self.claim.root/'watchdog';budget.reserve_watchdog(root);budget.mkdir(root)
        bad=root/'bad.json';bad.write_bytes(b'x'*16385);bad.chmod(0o600)
        with self.assertRaises(snapshot.FormalSnapshotError):budget.receipt()

    def test_receipt_is_binding_and_observation_not_allocation_guarantee(self):
        budget=self.make();row=budget.receipt()
        self.assertEqual(row['attemptId'],'owned-attempt')
        self.assertEqual(row['sourceContextSha256'],'3'*64)
        self.assertFalse(row['allocatedSpaceGuaranteed'])
        self.assertFalse(row['productionEvidence'])
        self.assertGreater(row['observedFreeBytes'],0)

    def test_process_stream_excess_reaps_only_owned_child(self):
        import formal_capture_budget as module
        budget=self.make(component=5000)
        child=subprocess.Popen([sys.executable,'-c',
            'import sys,time; sys.stdout.buffer.write(b"x"*100000); sys.stdout.buffer.flush(); time.sleep(10)'],
            stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        path=self.claim.root/'stream'
        with self.assertRaises(snapshot.FormalSnapshotError):
            with budget.open(path,'sql-raw') as out:
                module.drain_owned_process(child,b'',out,guard=budget.check,deadline=budget.deadline)
        self.assertIsNotNone(child.poll());self.assertLessEqual(path.stat().st_size,5000)

    def test_process_real_bytes_no_blocked_stdin_or_stderr_deadlock(self):
        import formal_capture_budget as module
        budget=self.make(component=300000,total=1000000)
        code='import sys; x=sys.stdin.buffer.read(); sys.stderr.buffer.write(b"e"*100000); sys.stdout.buffer.write(x)'
        child=subprocess.Popen([sys.executable,'-c',code],stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        with budget.open(self.claim.root/'out','sql-raw') as out, \
                budget.open(self.claim.root/'err','image-stderr') as err:
            result=module.drain_owned_process(child,b'a'*100000,out,guard=budget.check,
                deadline=budget.deadline,stderr_writer=err,stderr_limit=200000)
        self.assertEqual(result,dict(stdoutBytes=100000,stderrBytes=100000,returncode=0))
        self.assertEqual((self.claim.root/'out').read_bytes(),b'a'*100000)

    def test_raw_copy_enforces_actual_component_limit(self):
        budget=self.make(component=8)
        source=self.parent/'source';source.mkdir(mode=0o700)
        (source/'one').write_bytes(b'12345');(source/'two').write_bytes(b'67890')
        with budget.activate():
            with self.assertRaises(snapshot.FormalSnapshotError):
                snapshot.copy_physical_minio_tree(source,self.claim.root/'raw',lease_guard=self.guard)
        retained=sum(p.stat().st_size for p in (self.claim.root/'raw').rglob('*') if p.is_file())
        self.assertLessEqual(retained,8)

    def mysql(self, data):
        payload=self.parent/'client-payload'
        payload.write_bytes(data)
        class LocalClientProtocol(snapshot.PrivateDockerMySql):
            def _command(self,program,args):
                # Actual subprocess and original dump code, no MySQL/fence claim.
                return [sys.executable,'-c',
                    'import pathlib,sys; sys.stdin.buffer.readline(); sys.stdout.buffer.write(pathlib.Path(sys.argv[1]).read_bytes())',
                    str(payload)]
        return LocalClientProtocol('owned-client','owned-stdin',timeout=5)

    def test_actual_dump_raw_writer_refuses_unbounded_client_before_publish(self):
        budget=self.make(component=8)
        with budget.activate():
            with self.assertRaises(snapshot.FormalSnapshotError):
                self.mysql(b'a'*100000).dump('owned_db',self.claim.root/'sql.gz',guard=self.guard)
        self.assertFalse((self.claim.root/'sql.gz').exists())
        self.assertLessEqual((self.claim.root/'sql.gz.raw').stat().st_size,8)

    def test_dump_exact_gzip_bytes_and_retained_peak_without_double_counting_link(self):
        budget=self.make(component=100000,total=200000)
        data=b'originalSQL\r\n'+os.urandom(20000)
        with budget.activate():
            result=self.mysql(data).dump('owned_db',self.claim.root/'sql.gz',guard=self.guard)
        actual=(self.claim.root/'sql.gz').read_bytes()
        self.assertEqual(gzip.decompress(actual),data)
        self.assertEqual(result,dict(bytes=len(actual),sha256=hashlib.sha256(actual).hexdigest()))
        row=budget.receipt()
        self.assertEqual(row['writtenComponentBytes']['sql-raw'],len(data))
        self.assertEqual(row['writtenComponentBytes']['sql-gzip'],len(actual))
        self.assertEqual(row['writtenBytes'],len(data)+len(actual))

    def test_claim_and_split_proof_writes_enforce_actual_bytes(self):
        import formal_split_capture as split
        budget=self.make(component=8)
        with budget.activate():
            with self.assertRaises(snapshot.FormalSnapshotError):self.claim._write(self.claim.root/'proof',b'x'*9)
            with self.assertRaises(snapshot.FormalSnapshotError):split._record(self.claim.root,'proof2',dict(long='x'*50))
        self.assertEqual((self.claim.root/'proof').stat().st_size,0)
        self.assertEqual((self.claim.root/'proof2').stat().st_size,0)

    def test_config_copy_consumes_actual_config_component(self):
        import formal_selected_config_capture as config
        source=self.parent/'configs';source.mkdir(mode=0o700);(source/'config').mkdir(mode=0o700)
        for relative in config.FILES.values():
            path=source/relative;path.write_bytes(b'12345');path.chmod(0o600)
        observed,check=config.observe_selected_config_originals(source,guard=self.guard)
        budget=self.make(component=8)
        with budget.activate():
            with self.assertRaises(snapshot.FormalSnapshotError):
                config.capture_selected_config_originals(source,observed,self.claim.root/'copies',guard=check)
        actual=sum(p.stat().st_size for p in (self.claim.root/'copies').glob('*.bin'))
        self.assertLessEqual(actual,8)

    def test_data_query_actual_process_capped_on_owner_thread(self):
        import formal_data_evidence as data
        class OwnedDataProtocol(data.DataMySql):
            def _command(self,program,args):
                return [sys.executable,'-c','import sys; sys.stdin.buffer.readline(); sys.stdout.buffer.write(b"x"*100000)']
        budget=self.make(component=8)
        with budget.activate():
            with self.assertRaises(snapshot.FormalSnapshotError):
                OwnedDataProtocol('owned','private',timeout=5).data_query_to_file('SELECT 1;',
                    database='owned_db',expected_uuid='11111111-1111-1111-1111-111111111111',
                    path=self.claim.root/'tokens',max_bytes=200000)
        self.assertLessEqual((self.claim.root/'tokens').stat().st_size,8)

    def test_old_image_export_actual_tar_and_stderr_writers_bounded(self):
        import formal_original_launch_capture as images
        from unittest.mock import patch
        def launch(argv,**kwargs):
            return subprocess.Popen([sys.executable,'-c',
                'import sys; sys.stdout.buffer.write(b"x"*100000)'],**kwargs)
        budget=self.make(component=8)
        with budget.activate(),patch.object(images,'_image_export_process',launch):
            with self.assertRaises(snapshot.FormalSnapshotError):
                images.export_bounded_original_image_bytes(['sha256:'+'a'*64],self.claim.root/'images.tar',
                    guard=self.guard,deadline=budget.deadline)
        self.assertLessEqual((self.claim.root/'images.tar').stat().st_size,8)

    def test_original_image_full_capture_uses_bounded_export_seam(self):
        import formal_original_launch_capture as images
        from unittest.mock import patch
        originals={'backend':{'image':'sha256:'+'a'*64},'frontend':{'image':'sha256:'+'b'*64}}
        self.claim._write(self.claim.root/'shared-container-original.json',b'{}\n')
        budget=self.make(component=8);calls=[]
        def capped(ids,path,**kwargs):
            calls.append((ids,path));raise snapshot.FormalSnapshotError('ACTUAL_BOUND_SEAM')
        with budget.activate(),patch.object(images.split,'_scope',return_value=(originals,{},'c'*64)), \
                patch.object(images,'export_bounded_original_image_bytes',capped):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'ACTUAL_BOUND_SEAM'):
                images.capture_original_application_images(self.claim,self.claim.root/'originals.tar',guard=self.guard)
        self.assertEqual(len(calls),1)

    def test_successful_image_export_retires_empty_stderr_before_archive_epoch(self):
        import formal_original_launch_capture as images
        from unittest.mock import patch
        def launch(argv,**kwargs):
            return subprocess.Popen([sys.executable,'-c','import sys; sys.stdout.buffer.write(b"tar-protocol")'],**kwargs)
        budget=self.make();target=self.claim.root/'images.tar'
        with budget.activate(),patch.object(images,'_image_export_process',launch):
            row=images.export_bounded_original_image_bytes(['sha256:'+'a'*64],target,
                guard=self.guard,deadline=budget.deadline)
        self.assertEqual(row['stderrBytes'],0)
        self.assertFalse(target.with_name(target.name+'.stderr').exists())
        self.assertEqual(budget.receipt()['writtenComponentBytes']['images'],12)

    def test_sql_payload_directory_is_counted_before_downstream_capture(self):
        import formal_split_capture as split
        from types import SimpleNamespace
        from unittest.mock import patch
        source=self.parent/'minio';source.mkdir(mode=0o700)
        budget=self.make();budget.mkdir(self.claim.root/'first')
        # Keep policy bytes identical while exhausting the existing directory cap.
        for index in range(19):budget.mkdir(self.claim.root/('dir'+str(index)))
        def downstream(*args,**kwargs):raise snapshot.FormalSnapshotError('DOWNSTREAM_PROTOCOL_REACHED')
        # Directory-routing protocol only: no fake database/fence journal proof.
        with budget.activate(),patch.object(split,'_active'),patch.object(self.claim,'record'), \
                patch.object(split,'_scope',return_value=({'mysql':{'id':'2'*64}},{},'c'*64)), \
                patch.object(split,'observe_split_sql_barrier'),patch.object(split,'_split_rejection',downstream):
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_CAPTURE_OUTPUT_DIRECTORY_LIMIT'):
                split.capture_sql_phase(self.claim,SimpleNamespace(container='2'*64),'owned_db',source,
                    guard=self.guard,data_limits=dict(max_rows=10,max_nodes=100,max_bytes=10000,seconds=5),
                    rejection_probe=dict(table='owned_table',column='owned_column'))
        self.assertFalse((self.claim.root/'payload').exists())

    def test_full_shared_capture_refuses_missing_admission_before_config_or_stop(self):
        import formal_capture_producer as producer
        budget=self.make();events=list(self.claim.events)
        with budget.activate():
            with self.assertRaisesRegex(snapshot.FormalSnapshotError,'FORMAL_CAPTURE_COMPLETE_SPACE_ADMISSION_REQUIRED'):
                producer.run_production_a_shared_lifecycle(self.claim,None,approved_source=None,
                    resume_checks=None,container_actions=None,source_guard=self.guard,
                    maintenance_end=time.monotonic()+10,recovery_flows={})
        self.assertEqual(self.claim.events,events)
        self.assertFalse((self.claim.root/'original-configs').exists())

    def test_native_watchdog_control_records_have_actual_fixed_ceiling(self):
        import formal_filesystem_watchdog as watchdog
        control=self.parent/'control';control.mkdir(mode=0o700)
        with self.assertRaises(watchdog.WatchdogError):
            watchdog.write_exclusive(control,'too-large.json',dict(data='x'*16384))
        self.assertFalse((control/'too-large.json').exists())

    def test_control_subtree_is_reserved_before_child_and_has_no_false_full_admission(self):
        budget=self.make(component=300000,total=1000000,files=30)
        root=self.claim.root/'native-control'
        row=budget.reserve_watchdog(root)
        self.assertEqual(row['bytes'],16*16384)
        self.assertEqual(row['files'],16)
        self.assertEqual(budget.receipt()['writtenComponentBytes']['control'],0)
        self.assertEqual(budget.receipt()['reservedControlUpperBoundBytes'],16*16384)
        with self.assertRaises(snapshot.FormalSnapshotError):budget.require_full_capture_admission()


if __name__=='__main__':unittest.main(verbosity=2)
