"""Mandatory standard unittest route; real native crypto, explicitly owned fixtures."""
import base64
import copy
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).parents[1] / 'linux'))
import formal_open_authorization as authorization


class SelectedServiceNativeAuthorizationTest(unittest.TestCase):
    def setUp(self):
        self.assertEqual(os.geteuid(), 0, 'NATIVE_OWNED_ROOT_FIXTURE_REQUIRED')
        self.directory = tempfile.TemporaryDirectory(prefix='selected-standard-', dir='/control')
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.private = self.root / 'private.pem'
        self.openssl('genpkey', '-algorithm', 'ED25519', '-out', str(self.private))
        self.private.chmod(0o600)
        self.key = self.root / 'public.der'
        self.key.write_bytes(self.openssl('pkey', '-in', str(self.private), '-pubout', '-outform', 'DER'))
        self.key.chmod(0o644)
        self.pin = hashlib.sha256(self.key.read_bytes()).hexdigest()
        self.path = self.root / 'approval.json'
        self.expected = dict(schemaVersion='release-selected-test-service-authority/v1', role='stopAuthority',
            purpose='CLOSE_SELECTED_SOURCE_FOR_CAPTURE', grantId='a'*32, actorId=17, tenantId=122, issuedAt=1000, expiresAt=1600,
            dispatchBinding=dict(workflowId='standard-fixture-workflow', operationId='standard-fixture-operation',
                releaseTag='standard-fixture-tag', packageDigest='b'*64, manifestDigest='c'*64,
                targetDigest='d'*64, deploymentBindingDigest='e'*64),
            sourceBinding=dict(scope=dict(environment='test', host='172.30.30.58', machineId='f'*32,
                database='owned_fixture', serverUuid='11111111-1111-1111-1111-111111111111'),
                instanceIdentity=dict(taskFixture='explicit observer contract; no actual resource admission'),
                configSha256='1'*64, envSha256='2'*64, applicationsSha256='3'*64,
                originalsSha256={name: '4'*64 for name in ('application-config', 'application-env', 'data-env', 'application-compose', 'data-compose')},
                containers={name: dict(id='5'*64, imageId='sha256:'+'6'*64,
                    volume=dict(taskFixture='explicit volume; no server')) for name in ('mysql', 'minio')}),
            policy=dict(actions=['quiesce-selected-writers', 'set-selected-mysql-read-only',
                'stop-selected-mysql', 'stop-selected-minio', 'freeze-approved-selected-source']))

    def openssl(self, *args):
        return subprocess.check_output(['/usr/bin/openssl', *args], stderr=subprocess.DEVNULL)

    def publish(self, value):
        message = self.root / 'message'; message.write_bytes(json.dumps(value, separators=(',', ':')).encode())
        signature = self.openssl('pkeyutl', '-sign', '-inkey', str(self.private), '-rawin', '-in', str(message))
        envelope = dict(schemaVersion='release-selected-test-service-approval/v1', keySha256=self.pin,
            payloadBase64=base64.b64encode(message.read_bytes()).decode(), signatureBase64=base64.b64encode(signature).decode())
        self.path.write_bytes(json.dumps(envelope).encode()); self.path.chmod(0o600)
        return envelope

    def verify(self):
        return authorization.verify_selected_service_approval(str(self.path), str(self.key), self.pin,
            {key: self.expected[key] for key in ('schemaVersion', 'role', 'purpose',
                'dispatchBinding', 'sourceBinding', 'policy')}, now_seconds=1200, guard=lambda: None)

    def test_two_explicit_purposes_are_readonly_pending_consumption(self):
        for role in ('stopAuthority', 'restoreAuthority'):
            with self.subTest(role=role):
                value = copy.deepcopy(self.expected)
                if role == 'restoreAuthority':
                    value.update(role=role, purpose='RESTORE_SELECTED_SAVED_SOURCE_STATE', grantId='7'*32,
                        policy=dict(originalStateOnly=True, preserveAbsent=True, retainOnUnknownNewWrites=True, requireIndependentOriginalOpen=True))
                self.expected = value; self.publish(value)
                before = self.path.read_bytes(); result = self.verify()
                self.assertEqual(result['authority'], value)
                self.assertEqual(result['state'], 'SELECTED_SERVICE_APPROVAL_VERIFIED_PENDING_CONSUMPTION')
                self.assertFalse(result['approvalConsumed']); self.assertFalse(result['serviceActionsPerformed'])
                self.assertFalse(result['releaseReady']); self.assertEqual(self.path.read_bytes(), before)

    def test_actual_signature_is_mandatory(self):
        envelope = self.publish(self.expected)
        envelope['signatureBase64'] = base64.b64encode(b'x'*64).decode()
        self.path.write_bytes(json.dumps(envelope).encode())
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^RELEASE_SELECTED_SERVICE_APPROVAL_INVALID$'):
            self.verify()

    def test_complete_current_source_and_type_match_are_mandatory(self):
        self.publish(self.expected)
        self.expected['sourceBinding']['containers']['minio']['volume']['taskFixture'] = True
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^RELEASE_SELECTED_SERVICE_APPROVAL_INVALID$'):
            self.verify()

    def test_matching_expected_cannot_broaden_scope(self):
        self.expected['sourceBinding']['scope']['environment'] = 'prod'
        self.publish(self.expected)
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^RELEASE_SELECTED_SERVICE_APPROVAL_INVALID$'):
            self.verify()

    def test_full_caller_metadata_is_refused(self):
        self.publish(self.expected)
        with self.assertRaisesRegex(authorization.FormalSnapshotError, '^RELEASE_SELECTED_SERVICE_APPROVAL_INVALID$'):
            authorization.verify_selected_service_approval(str(self.path), str(self.key), self.pin,
                self.expected, now_seconds=1200, guard=lambda: None)

    def test_issuer_metadata_is_authenticated_separately_from_current_source(self):
        self.expected.update(actorId=777, tenantId=321, grantId='8'*32)
        self.publish(self.expected)
        result = self.verify()
        self.assertEqual(result['authority']['actorId'], 777)
        self.assertEqual(result['authority']['tenantId'], 321)
        self.assertFalse(result['approvalConsumed'])
