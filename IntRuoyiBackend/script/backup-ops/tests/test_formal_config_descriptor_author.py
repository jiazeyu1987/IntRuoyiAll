"""Actual private native files and held claim; no server/fence/approval fixture."""
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import time
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]/'linux'))
import formal_selected_config_capture as config
import formal_snapshot_linux as snapshot


class ConfigDescriptorAuthorTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='entry-config-', dir='/root')
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.source = self.root/'source'
        self.source.mkdir(mode=0o700)
        (self.source/'config').mkdir(mode=0o700)
        for role, relative in config.FILES.items():
            path = self.source/relative
            path.write_bytes(('self-owned-'+role+'\r\n中文\n').encode())
            path.chmod(0o600)
        self.owner = threading.get_ident()
        self.calls = 0

    def guard(self):
        self.assertEqual(threading.get_ident(), self.owner)
        self.calls += 1

    def observe(self, guard=None):
        return config.observe_selected_config_originals(self.source, guard=guard or self.guard)

    def test_independent_exact_bytes_and_stat_then_real_copy(self):
        observed, retained = self.observe()
        for role, relative in config.FILES.items():
            path = self.source/relative
            raw = path.read_bytes(); info = path.stat()
            self.assertEqual(observed[role], dict(path=str(path), bytes=len(raw),
                sha256=hashlib.sha256(raw).hexdigest(), fileSystemId=info.st_dev,
                inode=info.st_ino, mtimeNs=info.st_mtime_ns, ctimeNs=info.st_ctime_ns))
        retained()
        result = config.capture_selected_config_originals(self.source, observed,
            self.root/'copies', guard=retained)
        receipt, check = config.verify_selected_config_originals(self.source, observed,
            self.root/'copies', result, guard=retained)
        check(); self.assertFalse(receipt['productionEvidence'])
        self.assertGreater(self.calls, 10)

    def test_retained_refuses_changed_content(self):
        _, retained = self.observe()
        (self.source/'.env').write_bytes(b'changed')
        with self.assertRaises(snapshot.FormalSnapshotError): retained()

    def test_retained_refuses_replacement_and_descriptor_mutation(self):
        observed, retained = self.observe()
        observed['application-env']['bytes'] += 1
        with self.assertRaises(snapshot.FormalSnapshotError): retained()

    def test_retained_refuses_same_bytes_new_inode(self):
        _, retained = self.observe(); path = self.source/'.env'
        old = self.source/'retained-old-env'; path.rename(old)
        path.write_bytes(old.read_bytes()); path.chmod(0o600)
        with self.assertRaises(snapshot.FormalSnapshotError): retained()

    def test_private_mode_missing_and_link_refused(self):
        path = self.source/'.env'
        path.chmod(0o644)
        with self.assertRaises(snapshot.FormalSnapshotError): self.observe()
        path.unlink()
        with self.assertRaises(snapshot.FormalSnapshotError): self.observe()
        path.symlink_to(self.source/'docker-compose.yml')
        with self.assertRaises(snapshot.FormalSnapshotError): self.observe()

    def test_all_originals_reread_after_later_guard_change(self):
        def guard():
            self.guard()
            if self.calls == 8: (self.source/'config/application-instance.yaml').write_bytes(b'drift')
        with self.assertRaises(snapshot.FormalSnapshotError): self.observe(guard)

    def test_deadline_and_owner_revocation_preserved(self):
        limit = time.monotonic()-1
        token = snapshot.observation_deadline.set(limit)
        try:
            with self.assertRaises(snapshot.FormalSnapshotError): self.observe()
            self.assertEqual(snapshot.observation_deadline.get(), limit)
        finally: snapshot.observation_deadline.reset(token)
        def revoked(): raise snapshot.FormalSnapshotError('OWNED_REVOKED')
        with self.assertRaisesRegex(snapshot.FormalSnapshotError, 'OWNED_REVOKED'): self.observe(revoked)

    def test_cross_thread_retained_check_refused(self):
        _, retained = self.observe(); errors=[]
        def other():
            try: retained()
            except snapshot.FormalSnapshotError as error: errors.append(error)
        thread=threading.Thread(target=other); thread.start(); thread.join()
        self.assertEqual(len(errors), 1)

    def claim_case(self):
        target=dict(schemaVersion='release-formal-target/v1',environment='prod',host='owned',
            machineId='1'*32,project='owned',root=str(self.source),dataMount='/owned-data',
            dataDevice='/dev/owned',dataRoot='/owned-data/minio',releaseRoot='/owned-release',
            frontendPort=81,backendPort=82,database='owned_db',
            databaseServerUuid='11111111-1111-1111-1111-111111111111',
            containers={r:'owned-'+r for r in ('mysql','redis','minio','backend','frontend')})
        plan=dict(operationId='owned-config',sourceFingerprint=hashlib.sha256(json.dumps(
            target,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()).hexdigest(),
            databaseUuid=target['databaseServerUuid'],minioContainerId='2'*64)
        claim=snapshot.FormalMaintenanceClaim(self.root/'claim',plan)
        self.addCleanup(claim.close)
        raw=snapshot._canonical_bytes(target)
        claim._write(claim.root/'approved-source-target.json',raw)
        return claim,raw,hashlib.sha256(raw).hexdigest()

    def test_real_claim_seals_before_authority_and_keeps_original_fd(self):
        import formal_capture_producer as producer
        claim,raw,digest=self.claim_case(); fd=claim._fd
        reference,retained=producer.seal_production_a_config_originals(claim,raw,digest,
            guard=self.guard,deadline=time.monotonic()+10)
        path=claim.root/'original-configs.json'; data=path.read_bytes()
        self.assertEqual(reference,dict(path=str(path),sha256=hashlib.sha256(data).hexdigest()))
        self.assertEqual(set(json.loads(data)),set(config.FILES))
        self.assertEqual(claim._fd,fd); claim.assert_owned(); retained()
        self.assertEqual(claim.events,[])
        with self.assertRaises(snapshot.FormalSnapshotError):
            producer.seal_production_a_config_originals(claim,raw,digest,
                guard=self.guard,deadline=time.monotonic()+10)

    def test_config_author_approval_pin_and_retained_drift_refused(self):
        import formal_capture_producer as producer
        claim,raw,digest=self.claim_case()
        with self.assertRaises(snapshot.FormalSnapshotError):
            producer.seal_production_a_config_originals(claim,raw,'0'*64,
                guard=self.guard,deadline=time.monotonic()+10)
        self.assertFalse((claim.root/'original-configs.json').exists())
        _,retained=producer.seal_production_a_config_originals(claim,raw,digest,
            guard=self.guard,deadline=time.monotonic()+10)
        (self.source/'.env').write_bytes(b'drift')
        with self.assertRaises(snapshot.FormalSnapshotError): retained()

    def prepare_case(self):
        import formal_capture_producer as producer
        claim,raw,digest=self.claim_case()
        generated={'database-prefence-original.json',
            *('boot-'+r+'-original.json' for r in
                ('backend','test-backend','test-rehearsal','backup-backend','backup-rehearsal')),
            *('controlled-'+r+'-inspect.json' for r in
                ('mysql','frontend','backend','showroom','onlyoffice','redis','minio'))}
        refs={}
        for name in producer.PRODUCTION_A_SOURCE_FILES-generated-{'original-configs.json'}:
            path=claim.root/name
            if not path.exists():claim._write(path,b'{}\n')
            refs[name]=dict(path=str(path),sha256=hashlib.sha256(path.read_bytes()).hexdigest())
        release=snapshot._canonical_bytes(dict(schemaVersion='release-formal-original-package-binding/v2',
            claimBindingSha256=hashlib.sha256(claim._plan).hexdigest(),
            approvedSourceTargetSha256=digest,productionEvidence=False))
        return claim,refs,release

    def test_normal_preparation_authors_configs_and_hands_same_owner_to_existing_prepare(self):
        import formal_capture_producer as producer
        from unittest.mock import patch
        claim,refs,release=self.prepare_case();fd=claim._fd;calls=[]
        def prepare(actual,**kwargs):
            self.assertIs(actual,claim);self.assertEqual(actual._fd,fd)
            self.assertEqual(set(kwargs['source_refs']),set(refs)|{'original-configs.json'})
            kwargs['guard']();calls.append(kwargs)
            # Protocol delegation seam; not boot/MySQL/container acceptance.
            return dict(state='PREPARATION_PROTOCOL_ONLY',productionEvidence=False),kwargs['guard']
        with patch.object(producer,'prepare_production_a_normal_sources',prepare):
            result,check=producer.prepare_production_a_observed_normal_sources(claim,
                release_binding_raw=release,source_refs=refs,boot_sources={},controlled_sources={},
                mysql=object(),guard=self.guard,deadline=time.monotonic()+10)
        self.assertEqual(result['state'],'PREPARATION_PROTOCOL_ONLY');check()
        self.assertEqual(len(calls),1);self.assertEqual(claim.events,[])
        self.assertNotIn('original-configs.json',refs)
        (self.source/'.env').write_bytes(b'later-drift')
        with self.assertRaises(snapshot.FormalSnapshotError):check()

    def test_normal_observation_refuses_supplied_config_or_wrong_approval_before_author(self):
        import formal_capture_producer as producer
        claim,refs,release=self.prepare_case()
        kwargs=dict(release_binding_raw=release,source_refs=refs,boot_sources={},controlled_sources={},
            mysql=object(),guard=self.guard,deadline=time.monotonic()+10)
        refs['original-configs.json']=dict(path=str(claim.root/'original-configs.json'),sha256='0'*64)
        with self.assertRaises(snapshot.FormalSnapshotError):
            producer.prepare_production_a_observed_normal_sources(claim,**kwargs)
        del refs['original-configs.json']
        kwargs['release_binding_raw']=snapshot._canonical_bytes(dict(approvedSourceTargetSha256='0'*64))
        with self.assertRaises(snapshot.FormalSnapshotError):
            producer.prepare_production_a_observed_normal_sources(claim,**kwargs)
        self.assertFalse((claim.root/'original-configs.json').exists())

    def test_normal_observation_retains_earliest_deadline_and_refs_pin(self):
        import formal_capture_producer as producer
        from unittest.mock import patch
        claim,refs,release=self.prepare_case();deadline=time.monotonic()+10
        def prepare(actual,**kwargs):
            self.assertEqual(kwargs['deadline'],deadline)
            return dict(productionEvidence=False),kwargs['guard']
        token=snapshot.observation_deadline.set(deadline)
        try:
            with patch.object(producer,'prepare_production_a_normal_sources',prepare):
                _,check=producer.prepare_production_a_observed_normal_sources(claim,
                    release_binding_raw=release,source_refs=refs,boot_sources={},controlled_sources={},
                    mysql=object(),guard=self.guard,deadline=deadline+1000)
            self.assertEqual(snapshot.observation_deadline.get(),deadline)
            refs['binding.json']['sha256']='f'*64
            with self.assertRaises(snapshot.FormalSnapshotError):check()
        finally:snapshot.observation_deadline.reset(token)


if __name__ == '__main__': unittest.main(verbosity=2)
