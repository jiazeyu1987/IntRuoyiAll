"""Real native Ed25519 bridge tests; /control is an explicit isolated fixture.

Current source/owner callbacks are protocol fixtures, never live admission.
No grant ledger, STARTED, service operation, or production receipt is created.
"""
import base64
import copy
import hashlib
import importlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_open_authorization as authorization
import test_selected_service_authorization_native as fixture_source

FIELDS = ('schemaVersion', 'role', 'purpose', 'dispatchBinding', 'sourceBinding', 'policy')
CODE = 'RELEASE_SELECTED_NATIVE_ORIGINALS_INVALID'

def packed(raw):
    return dict(sha256=hashlib.sha256(raw).hexdigest(), sizeBytes=len(raw),
                base64=base64.b64encode(raw).decode('ascii'))

class SelectedReleaseEntryNativeTest(unittest.TestCase):
    def test_native_current_guard_executes_and_refuses_source_or_owner_drift(self):
        module=importlib.import_module('formal_selected_release_entry')
        fn=getattr(module,'selected_entry_current_guard',None)
        self.assertTrue(callable(fn),'trusted native guard must execute fresh source and readonly ownership queries')
        # Real child processes, explicitly protocol fixtures: not a live SQL or
        # Docker observation. Production generator supplies the real probe/SQL carrier.
        request=dict(sourceProbe="print('{}')",sourceObservation={'changed':True},
            mysqlCommand="printf LOCK_OWNED",owner=dict(environment='test',host='172.30.30.58',
                machineId='1'*32,database='db',serverUuid='11111111-1111-1111-1111-111111111111',
                lockOperationId='test-tag',releaseTag='tag',leaseTokenDigest='a'*64,
                executorHost='host',executorPid='321',executorIdentity='id'))
        with self.assertRaisesRegex(authorization.FormalSnapshotError,'^RELEASE_SELECTED_CURRENT_SOURCE_CHANGED$'):
            fn(request,lambda:None)
        request['sourceObservation']={};request['mysqlCommand']='printf LOCK_LOST'
        with self.assertRaisesRegex(authorization.FormalSnapshotError,'^RELEASE_CAPTURE_LOCK_LOST$'):
            fn(request,lambda:None)

    def setUp(self):
        self.assertEqual(os.geteuid(), 0)
        self.directory = tempfile.TemporaryDirectory(prefix='normal-entry-', dir='/control')
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.private = self.root / 'private.pem'
        self.openssl('genpkey', '-algorithm', 'ED25519', '-out', str(self.private))
        self.private.chmod(0o600)
        self.key = self.root / 'public.der'
        self.key.write_bytes(self.openssl('pkey', '-in', str(self.private), '-pubout', '-outform', 'DER'))
        self.key.chmod(0o644)
        # Reuse the existing standard TEST binding fixture, without invoking its tests.
        fixture = fixture_source.SelectedServiceNativeAuthorizationTest()
        fixture.setUp()
        try:
            source = copy.deepcopy(fixture.expected)
        finally:
            fixture.doCleanups()
        now = int(time.time())
        source.update(issuedAt=now-5, expiresAt=now+300, actorId=777, tenantId=321)
        restore = copy.deepcopy(source)
        restore.update(role='restoreAuthority', purpose='RESTORE_SELECTED_SAVED_SOURCE_STATE',
            grantId='7'*32, policy=dict(originalStateOnly=True, preserveAbsent=True,
                retainOnUnknownNewWrites=True, requireIndependentOriginalOpen=True))
        self.payloads = dict(stopAuthority=source, restoreAuthority=restore)
        self.paths = {role: str(self.root/(role+'.json')) for role in self.payloads}
        self.expected = {role: {key: copy.deepcopy(payload[key]) for key in FIELDS}
                         for role, payload in self.payloads.items()}
        self.capsule = dict(schemaVersion='release-selected-test-native-authorization-input/v1',
            state='NATIVE_INPUT_PINNED_RESOURCE_ADMISSION_REQUIRED', attemptId='owned-fixture-attempt',
            scopeDigest='a'*64, expectedBindings=copy.deepcopy(self.expected), approvals={},
            publicKey=packed(self.key.read_bytes()), signatureVerified=False,
            grantConsumed=False, sourceEffectsPerformed=False)
        for role in self.payloads:
            self.publish(role)

    def openssl(self, *args):
        return subprocess.check_output(['/usr/bin/openssl', *args], stderr=subprocess.DEVNULL)

    def publish(self, role):
        message = self.root / 'message'
        message.write_bytes(json.dumps(self.payloads[role], separators=(',', ':')).encode())
        signature = self.openssl('pkeyutl', '-sign', '-inkey', str(self.private), '-rawin', '-in', str(message))
        envelope = dict(schemaVersion='release-selected-test-service-approval/v1',
            keySha256=self.capsule['publicKey']['sha256'], payloadBase64=base64.b64encode(message.read_bytes()).decode(),
            signatureBase64=base64.b64encode(signature).decode())
        path = Path(self.paths[role]); path.write_bytes(json.dumps(envelope).encode()); path.chmod(0o600)
        self.capsule['approvals'][role] = packed(path.read_bytes())

    def verify(self, guard=lambda: None):
        spec = importlib.util.find_spec('formal_selected_release_entry')
        self.assertIsNotNone(spec, 'native identical-original verifier seam is required')
        module = importlib.import_module('formal_selected_release_entry')
        return module.verify_selected_service_originals(self.capsule, self.expected,
            'owned-fixture-attempt', 'a'*64, self.paths, str(self.key), guard=guard)

    def test_real_both_signatures_accept_issuer_metadata_without_consumption(self):
        before = {path.name: path.read_bytes() for path in self.root.iterdir()}
        calls = []
        result = self.verify(guard=lambda: calls.append(True))
        self.assertEqual(result['state'], 'SELECTED_SERVICE_ORIGINALS_VERIFIED_ADMISSION_REQUIRED')
        self.assertEqual(result['authorities'], self.payloads)
        self.assertEqual(result['attemptId'], 'owned-fixture-attempt')
        self.assertTrue(result['signatureVerified'])
        for name in ('grantConsumed', 'startedPersisted', 'sourceEffectsPerformed', 'releaseReady'):
            self.assertIs(result[name], False)
        self.assertGreaterEqual(len(calls), 6)
        self.assertEqual(before, {path.name: path.read_bytes() for path in self.root.iterdir()})

    def test_forged_signature_refused_even_with_matching_transport_pins(self):
        path = Path(self.paths['stopAuthority']); envelope = json.loads(path.read_bytes())
        envelope['signatureBase64'] = base64.b64encode(b'x'*64).decode()
        path.write_bytes(json.dumps(envelope).encode())
        self.capsule['approvals']['stopAuthority'] = packed(path.read_bytes())
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^RELEASE_SELECTED_SERVICE_APPROVAL_INVALID$'):
            self.verify()

    def test_same_byte_custody_and_exact_attempt_are_mandatory(self):
        for fault in ('original', 'key', 'attempt', 'scope', 'transport-byte', 'claimed-approval'):
            with self.subTest(fault=fault):
                saved = copy.deepcopy(self.capsule)
                path = Path(self.paths['stopAuthority']); raw = path.read_bytes(); key_raw = self.key.read_bytes()
                if fault == 'original': path.write_bytes(raw+b' ')
                elif fault == 'key': self.key.write_bytes(key_raw+b'x')
                elif fault == 'attempt': self.capsule['attemptId'] = 'other'
                elif fault == 'scope': self.capsule['scopeDigest'] = 'b'*64
                elif fault == 'transport-byte': self.capsule['approvals']['stopAuthority']['base64'] += '='
                else: self.capsule['grantConsumed'] = True
                with self.assertRaisesRegex(authorization.FormalSnapshotError, '^'+CODE+'$'):
                    self.verify()
                self.capsule = saved; path.write_bytes(raw); self.key.write_bytes(key_raw)

    def test_current_binding_and_authenticated_distinct_grants_are_mandatory(self):
        self.expected['stopAuthority']['dispatchBinding']['operationId'] = 'changed'
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^'+CODE+'$'):
            self.verify()
        self.expected = copy.deepcopy(self.capsule['expectedBindings'])
        self.payloads['restoreAuthority']['grantId'] = self.payloads['stopAuthority']['grantId']
        self.publish('restoreAuthority')
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^'+CODE+'$'):
            self.verify()

    def test_lost_owner_keeps_exact_error_and_originals(self):
        before = {path.name: path.read_bytes() for path in self.root.iterdir()}
        def guard():
            raise authorization.FormalSnapshotError('OWNED_CURRENT_GUARD_REFUSED')
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^OWNED_CURRENT_GUARD_REFUSED$'):
            self.verify(guard)
        self.assertEqual(before, {path.name: path.read_bytes() for path in self.root.iterdir()})

    def test_final_guard_cannot_change_first_original_after_second_verification(self):
        calls = []
        def guard():
            calls.append(True)
            if len(calls) == 6:
                path = Path(self.paths['stopAuthority']); path.write_bytes(path.read_bytes()+b' ')
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^'+CODE+'$'):
            self.verify(guard)

    def test_normal_resource_entry_uses_real_custody_and_refuses_tmpfs_roles(self):
        module = importlib.import_module('formal_selected_release_entry')
        fn = getattr(module, 'admit_selected_entry_resources', None)
        self.assertTrue(callable(fn), 'normal native entry must call the single admission API and native observer')
        # Reuse the released resource body fixtures without mocking admission or
        # native mount checks. Authority files are genuinely signed here.
        import test_selected_resource_admission as resource_fixture
        generator = resource_fixture.case.__wrapped__()
        case = next(generator)
        try:
            self.expected = {role: dict(self.expected[role],dispatchBinding=copy.deepcopy(case['dispatch']),
                sourceBinding=copy.deepcopy(case['source'])) for role in self.payloads}
            self.capsule['expectedBindings'] = copy.deepcopy(self.expected)
            for role in self.payloads:
                for key in FIELDS:self.payloads[role][key]=copy.deepcopy(self.expected[role][key])
                self.publish(role)
            collection_path=case['root']/'test.capture-sources.json'
            collection=json.loads(collection_path.read_bytes())
            for role in self.paths:
                destination=case['root']/(role+'.json')
                destination.write_bytes(Path(self.paths[role]).read_bytes());destination.chmod(0o600)
                collection['originals'][role]['sha256']=hashlib.sha256(destination.read_bytes()).hexdigest()
            collection_path.write_bytes(json.dumps(collection).encode())
            pin=hashlib.sha256(collection_path.read_bytes()).hexdigest()
            case['root'].joinpath('test.service-authority-public.der').write_bytes(self.key.read_bytes())
            case['root'].joinpath('test.service-authority-public.der').chmod(0o644)
            with self.assertRaisesRegex(authorization.FormalSnapshotError,
                '^RELEASE_SELECTED_NATIVE_RESOURCE_OBSERVATION_INVALID$'):
                fn(str(case['root']),pin,self.capsule,self.expected,'owned-fixture-attempt','a'*64,
                    expected_host='172.30.30.58',expected_machine_id='5'*32,guard=lambda:None)
            # Same signed bytes through the actual normal bootstrap after shell
            # newline normalization. Install only this test's private copies.
            sys.path.insert(0,'/maintenance')
            import release_selected_native_entry as bootstrap
            code_root=self.root/'installed';code_root.mkdir(mode=0o700)
            pins={}
            for name in bootstrap.MODULES:
                raw=(Path(__file__).parents[1]/'linux'/(name+'.py')).read_bytes()
                destination=code_root/(name+'.py');destination.write_bytes(raw);destination.chmod(0o644)
                pins[name]=hashlib.sha256(raw).hexdigest()
            request=dict(configuration=dict(codeRoot=str(code_root),moduleSha256=pins),
                privateRoot=str(case['root']),collectionSha256=pin,capsule=self.capsule,currentBindings=self.expected,
                consumptionStorage=dict(root=str(self.root/'consumption'),fileSystemId=1,rootInode=1),
                attemptId='owned-fixture-attempt',scopeDigest='a'*64,expectedHost='172.30.30.58',expectedMachineId='5'*32,
                sourceProbe="print('{}')",sourceObservation={},mysqlCommand='printf LOCK_OWNED',
                owner=dict(environment='test',database='db',serverUuid='11111111-1111-1111-1111-111111111111',
                    lockOperationId='test-tag',releaseTag='tag',leaseTokenDigest='a'*64,
                    executorHost='host',executorPid='321',executorIdentity='id'))
            # These fresh subprocess source/owner responses are protocol fixtures;
            # installation, A037, original custody and resource refusal are real.
            for fault,expected in ((None,'RELEASE_SELECTED_NATIVE_RESOURCE_OBSERVATION_INVALID'),
                ('code-pin','RELEASE_SELECTED_NATIVE_INSTALLATION_INVALID'),
                ('source','RELEASE_SELECTED_CURRENT_SOURCE_CHANGED'),('owner','RELEASE_CAPTURE_LOCK_LOST')):
                with self.subTest(bootstrap=fault):
                    value=copy.deepcopy(request)
                    if fault=='code-pin':value['configuration']['moduleSha256']['formal_boot_state']='0'*64
                    if fault=='source':value['sourceObservation']={'changed':True}
                    if fault=='owner':value['mysqlCommand']='printf LOCK_LOST'
                    program=bootstrap.program(value).replace('\n','\r\n').replace('\r\n','\n')
                    completed=subprocess.run(['sh','-c',program],capture_output=True,timeout=15)
                    self.assertEqual(completed.returncode,2,completed.stdout+completed.stderr)
                    self.assertEqual(completed.stdout,b'')
                    self.assertEqual(completed.stderr.strip(),expected.encode())
        finally:
            generator.close()

if __name__ == '__main__':
    unittest.main()
