"""Focused offline contracts/security/concurrency checks. No fake DB/S3 success."""
from email.message import Message
from pathlib import Path
import ast
import copy
import hashlib
import json
import secrets
import sys
import threading
import unittest
from core import AppError, COLUMNS, Gate, Store, VERSION, config, filename, identifier, password_hash, password_matches, record_key, strict_json
from http_contract import allowed_method, cookies, csrf, framing, require_origin, route
from capture_binding import REFERENCE_COLUMNS, reference_recipe, require_quiescence


def headers(**values):
    result = Message()
    for name, value in values.items():
        result[name.replace('_', '-')] = value
    return result


def settings():
    # Validation fixture only; never connected, installed, or delivered as config.
    return {'schemaVersion': 'SmallAppRuntimeConfig/v2', 'version': VERSION,
        'logicalConfigRef': 'smallapp-20261014-source-r1', 'runtimeRole': 'SOURCE', 'tenantId': 122,
        'username': 'aoteman', 'database': {'host': '127.0.0.1', 'port': 3306,
            'name': 'brlab_20261012_smallapp_contract', 'tlsCaFile': None},
        's3': {'endpoint': 'http://127.0.0.1:9000', 'region': 'us-east-1',
            'bucket': 'brlab-20261012-smallapp-contract', 'keyPrefix': 'smallapp/122/', 'tlsCaFile': None},
        'redis': {'host': '127.0.0.1', 'port': 6379, 'database': 0,
            'instanceId': 'brlab-20261012-smallapp-source-contract',
            'keyPrefix': 'brlab-20261012:smallapp:122:source:', 'serverVersion': '6.2.0', 'tlsCaFile': None},
        'http': {'listenHost': '127.0.0.1', 'listenPort': 8082, 'origin': 'http://127.0.0.1:8082', 'cookieSecure': False},
        'controlSocket': '/run/smallapp/contract.sock', 'startState': 'OPEN',
        'secretRefs': {'mysql': 'smallapp.mysql', 's3': 'smallapp.s3', 'redis': 'smallapp.redis', 'control': 'smallapp.control'}}


class CriticalChecks(unittest.TestCase):
    def rejected(self, fn, code):
        with self.assertRaises(AppError) as caught:
            fn()
        self.assertEqual(caught.exception.code, code)

    def test_strict_json(self):
        self.rejected(lambda: strict_json(b'{"x":1,"x":2}'), 'DUPLICATE_JSON_FIELD')
        self.rejected(lambda: strict_json(b'{"x":NaN}'), 'NONFINITE_JSON_VALUE')
        self.rejected(lambda: strict_json(b'\xff'), 'INVALID_JSON')

    def test_foreign_tenant_and_unrelated_database_rejected(self):
        value = settings(); value['tenantId'] = 1
        self.rejected(lambda: config(value), 'RUNTIME_SCOPE_MISMATCH')
        value = settings(); value['database']['name'] = 'yudao'
        self.rejected(lambda: config(value), 'TASK_DATABASE_REQUIRED')
        value = settings(); value['s3']['bucket'] = 'yudao'
        self.rejected(lambda: config(value), 'TASK_OBJECT_SCOPE_REQUIRED')

    def test_target_requires_protected_start(self):
        value = settings(); value.update(runtimeRole='TARGET', logicalConfigRef='smallapp-20261014-target-r1')
        value['redis'].update(instanceId='brlab-20261012-smallapp-target-contract', keyPrefix='brlab-20261012:smallapp:122:target:')
        self.rejected(lambda: config(value), 'TARGET_PROTECTED_START_REQUIRED')
        value['startState'] = 'PROTECTED'
        self.assertEqual(config(value)['startState'], 'PROTECTED')

    def test_target_bootstrap_denied_before_any_db_or_object_client(self):
        from install import install
        value = settings(); value.update(runtimeRole='TARGET', logicalConfigRef='smallapp-20261014-target-r1', startState='PROTECTED')
        value['redis'].update(instanceId='brlab-20261012-smallapp-target-contract', keyPrefix='brlab-20261012:smallapp:122:target:')
        self.rejected(lambda: install({'config': value, 'secrets': None, 'bootstrap': None}), 'TARGET_BOOTSTRAP_REFUSED')

    def test_public_bundle_checks_both_roles_and_one_origin(self):
        from materialize import public_configuration
        value = {'schemaVersion': 'SmallAppPublicConfiguration/v2', 'version': VERSION, 'backend': settings(),
            'frontend': {'schemaVersion': 'SmallAppFrontendConfig/v1', 'version': VERSION,
                'backendOrigin': 'http://brlab-20261012-contract-backend:8082', 'backendTlsCaFile': None,
                'http': {'listenHost': '127.0.0.1', 'listenPort': 8083, 'origin': 'http://127.0.0.1:8082'}}}
        public_configuration(value)
        value['frontend']['http']['origin'] = 'http://127.0.0.1:8083'
        self.rejected(lambda: public_configuration(value), 'PUBLIC_ORIGIN_ROLE_MISMATCH')

    def test_credentials_and_paths_not_allowed_in_public_endpoint(self):
        value = settings(); value['s3']['endpoint'] = 'http://private:private@127.0.0.1:9000'
        self.rejected(lambda: config(value), 'S3_ENDPOINT_INVALID')
        value = settings(); value['s3']['endpoint'] += '/other'
        self.rejected(lambda: config(value), 'S3_ENDPOINT_INVALID')
        value = settings(); value['unexpected'] = 'not allowed'
        self.rejected(lambda: config(value), 'EXACT_FIELDS_REQUIRED')

    def test_scope_reference_and_cookie_security_cannot_drift(self):
        value = settings(); value['logicalConfigRef'] = 'smallapp-20261014-target-r1'
        self.rejected(lambda: config(value), 'CONFIG_ROLE_MISMATCH')
        value = settings(); value['http']['origin'] = 'https://127.0.0.1:8082'
        self.rejected(lambda: config(value), 'COOKIE_SECURITY_MISMATCH')

    def test_actual_password_kdf(self):
        password = secrets.token_urlsafe(24)
        encoded = password_hash(password)
        self.assertTrue(password_matches(password, encoded), 'Generated password should match')
        self.assertFalse(password_matches(password + 'x', encoded), 'Other password must fail')
        self.rejected(lambda: password_matches(password, 'invalid'), 'PASSWORD_RECORD_INVALID')

    def test_login_csrf_hmac_binds_cookie(self):
        owner = Store.__new__(Store)  # Only pure HMAC methods; no persistence/client constructed.
        owner.auth_key = secrets.token_bytes(32)
        token = owner.login_token()
        owner.require_login_token(token, token)
        self.rejected(lambda: owner.require_login_token(token, owner.login_token()), 'LOGIN_CSRF_INVALID')
        self.rejected(lambda: owner.require_login_token('汉字', '汉字'), 'LOGIN_CSRF_INVALID')

    def test_csrf_and_origin(self):
        token = secrets.token_hex(32)
        csrf(token, token)
        self.rejected(lambda: csrf('汉字', token), 'CSRF_DENIED')
        require_origin(headers(Host='127.0.0.1:8082', Origin='http://127.0.0.1:8082'), 'http://127.0.0.1:8082', mutation=True)
        self.rejected(lambda: require_origin(headers(Host='127.0.0.1:8082', Origin='https://other.invalid'),
            'http://127.0.0.1:8082', mutation=True), 'ORIGIN_DENIED')
        self.rejected(lambda: require_origin(headers(Host='other.invalid'), 'http://127.0.0.1:8082', mutation=False), 'HOST_DENIED')

    def test_body_smuggling_and_size_rejected(self):
        value = headers(Content_Length='10'); value['Content-Length'] = '12'
        self.rejected(lambda: framing(value, 'POST', 65536), 'HEADER_CARDINALITY_INVALID')
        self.rejected(lambda: framing(headers(Transfer_Encoding='chunked', Content_Length='10'), 'POST', 65536), 'HTTP_FRAMING_NOT_SUPPORTED')
        self.rejected(lambda: framing(headers(Content_Length='65537'), 'PUT', 65536), 'REQUEST_TOO_LARGE')
        self.rejected(lambda: framing(headers(Content_Length='1'), 'GET', 0), 'GET_BODY_NOT_ALLOWED')
        self.assertEqual(framing(headers(Content_Length='65536'), 'PUT', 65536), 65536)

    def test_duplicate_cookie_rejected(self):
        self.rejected(lambda: cookies(headers(Cookie='smallapp_session=x; smallapp_session=y')), 'DUPLICATE_COOKIE')

    def test_path_and_method_not_shell_or_filesystem(self):
        self.rejected(lambda: route('/api/records/1?tenant=1'), 'PATH_INVALID')
        self.rejected(lambda: route('/../schema.sql'), 'ROUTE_NOT_FOUND')
        self.rejected(lambda: route('/api/records/%31'), 'PATH_INVALID')
        self.rejected(lambda: allowed_method('/api/session', 'POST'), 'METHOD_NOT_ALLOWED')
        self.assertEqual(route('/api/records/1/attachment'), ('attachment', 1))

    def test_ids_filenames_and_business_keys(self):
        self.rejected(lambda: identifier(True), 'POSITIVE_ID_REQUIRED')
        self.rejected(lambda: identifier(2**63), 'POSITIVE_ID_REQUIRED')
        for value in ('../x', 'x\\y', 'x\r\nInjected: value', '..'):
            self.rejected(lambda: filename(value), 'INVALID_FILENAME')
        self.assertEqual(filename('附件一.txt'), '附件一.txt')
        self.assertEqual(record_key('SA-W06-01'), 'SA-W06-01')
        self.rejected(lambda: record_key('a/b'), 'RECORD_KEY_INVALID')

    def test_quiescence_waits_for_inflight_mutation(self):
        gate = Gate('OPEN')
        entered, release, paused = threading.Event(), threading.Event(), threading.Event()
        errors = []
        def writer():
            try:
                with gate.mutation():
                    entered.set()
                    if not release.wait(2): errors.append('writer-timeout')
            except BaseException: errors.append('writer-error')
        def barrier():
            with gate.lock:
                gate.state = 'QUIESCED'; paused.set()
        writer_thread = threading.Thread(target=writer)
        writer_thread.start()
        self.assertTrue(entered.wait(1))
        barrier_thread = threading.Thread(target=barrier); barrier_thread.start()
        try:
            self.assertFalse(paused.wait(.05), 'Barrier must wait for writer')
        finally:
            release.set(); writer_thread.join(2); barrier_thread.join(2)
        self.assertFalse(errors)
        self.assertTrue(paused.is_set())
        self.rejected(lambda: self.enter_gate(gate), 'WRITES_PAUSED')

    @staticmethod
    def enter_gate(gate):
        with gate.mutation():
            raise AssertionError('Paused gate admitted a mutation')

    def test_uncertain_mutation_closes_gate_and_rethrows(self):
        gate = Gate('OPEN')
        with self.assertRaises(OSError):
            with gate.mutation(): raise OSError('unit contract failure')
        self.assertEqual(gate.state, 'QUIESCED')

    def test_validation_conflict_preserves_gate(self):
        gate = Gate('OPEN')
        with self.assertRaises(AppError):
            with gate.mutation(): raise AppError('RECORD_VERSION_CONFLICT', 409)
        self.assertEqual(gate.state, 'OPEN')

    def test_capture_recipe_uses_existing_validator_and_no_binary_secrets(self):
        sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'linux'))
        from formal_capture_semantics import validate_reference_recipe
        path = Path(__file__).parent / 'config-contract.json'; raw = path.read_bytes()
        reference = {'path': str(path.resolve()), 'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}
        recipe = reference_recipe('unit-interface-only', reference)
        validate_reference_recipe(recipe, 'unit-interface-only', reference)
        self.assertNotIn('session_hash', REFERENCE_COLUMNS['smallapp_sessions'])
        self.assertNotIn('csrf', REFERENCE_COLUMNS['smallapp_sessions'])
        self.assertNotIn('password_hash', REFERENCE_COLUMNS['smallapp_users'])
        self.assertIn('record_key', REFERENCE_COLUMNS['smallapp_records'])
        self.assertEqual(set(REFERENCE_COLUMNS), set(COLUMNS))

    def test_quiescence_receipt_rejects_wrong_scope_or_pending(self):
        receipt = dict(schemaVersion='SmallAppControlResult/v2', requestId='unit-only', operation='QUIESCE',
            state='QUIESCED', pendingObjects=0, activeMutations=0, version=VERSION, tenantId=122,
            logicalConfigRef='smallapp-20261014-source-r1', stopRequested=False,
            redis={'state': 'UP', 'serverVersion': '6.2.0', 'mode': 'standalone', 'role': 'master',
                'instanceId': 'brlab-20261012-smallapp-source-contract', 'database': 0,
                'classification': 'REBUILD', 'rebuildFrom': 'DATABASE'})
        require_quiescence(receipt, receipt['logicalConfigRef'])
        receipt['pendingObjects'] = 1
        self.rejected(lambda: require_quiescence(receipt, receipt['logicalConfigRef']), 'APP_QUIESCENCE_REQUIRED')
        receipt['pendingObjects'] = 0; receipt['tenantId'] = 1
        self.rejected(lambda: require_quiescence(receipt, receipt['logicalConfigRef']), 'APP_QUIESCENCE_REQUIRED')

    def test_control_client_only_explicit_task_socket_views(self):
        from control_client import control_socket_path, TASK_ROOT
        self.assertEqual(control_socket_path('/run/smallapp/source.sock'), '/run/smallapp/source.sock')
        path = TASK_ROOT + 'source/private/source.sock'
        self.assertEqual(control_socket_path(path), path)
        for invalid in ('/run/other/source.sock', TASK_ROOT + '../other/private/source.sock', '/tmp/source.sock'):
            self.rejected(lambda: control_socket_path(invalid), 'CONTROL_SOCKET_INVALID')


if __name__ == '__main__':
    unittest.main()
