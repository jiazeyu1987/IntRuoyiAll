"""Affected offline checks. No Redis/SQL service doubles or runtime success claims."""
from datetime import datetime
from pathlib import Path
import hashlib
import json
import unittest
from core import AppError, Gate, Store, VERSION, config
from capture_binding import derived_recipe, require_quiescence
from critical_checks import settings
from http_contract import cookie
from redis_cache import cache_key, decode_value, expiry_milliseconds, live_value, redis_client, session_value


def authority():
    # Pure SQL-row-shaped input, not a fake SQL query/result or installed data.
    return dict(session_hash=bytes(range(32)), tenant_id=122, user_id=1, username='aoteman',
                csrf='a' * 64, expires_at=datetime(2030, 1, 1, 0, 0, 0, 999999))


class RedisChecks(unittest.TestCase):
    def rejected(self, fn, code):
        with self.assertRaises(AppError) as caught:
            fn()
        self.assertEqual(caught.exception.code, code)

    def test_required_redis_and_old_runtime_are_rejected(self):
        value = settings(); del value['redis']
        self.rejected(lambda: config(value), 'EXACT_FIELDS_REQUIRED')
        value = settings(); value['schemaVersion'] = 'SmallAppRuntimeConfig/v1'
        self.rejected(lambda: config(value), 'RUNTIME_VERSION_MISMATCH')

    def test_source_target_redis_scope_cannot_mix(self):
        value = settings(); value.update(runtimeRole='TARGET', logicalConfigRef='smallapp-20261014-target-r1', startState='PROTECTED')
        self.rejected(lambda: config(value), 'DEDICATED_REDIS_SCOPE_REQUIRED')
        value['redis'].update(instanceId='brlab-20261012-smallapp-target-contract', keyPrefix='brlab-20261012:smallapp:122:target:')
        self.assertEqual(config(value)['runtimeRole'], 'TARGET')
        value['redis']['database'] = 1
        self.rejected(lambda: config(value), 'DEDICATED_REDIS_SCOPE_REQUIRED')

    def test_actual_version_required_and_feature_minimum(self):
        for version in (None, 'latest', '6.0.20', '6.2', '7.2.4-claimed'):
            value = settings(); value['redis']['serverVersion'] = version
            self.rejected(lambda: config(value), 'OBSERVED_REDIS_VERSION_WITH_PXAT_REQUIRED')
        value = settings(); value['redis']['serverVersion'] = '6.2.0'
        config(value)

    def test_private_redis_reference_and_endpoint_are_strict(self):
        value = settings(); value['redis']['host'] = 'redis://user:password@127.0.0.1'
        self.rejected(lambda: config(value), 'REDIS_ENDPOINT_REQUIRED')
        value = settings(); value['secretRefs']['redis'] = 'other.redis'
        self.rejected(lambda: config(value), 'SECRET_REFERENCE_SCOPE_INVALID')
        self.rejected(lambda: redis_client(settings()['redis'], {'username': 'unit', 'password': ''}),
                      'PRIVATE_REDIS_CREDENTIAL_INVALID')

    def test_absolute_expiry_floors_microseconds_and_never_slides(self):
        self.assertEqual(expiry_milliseconds(datetime(1970, 1, 1)), 0)
        self.assertEqual(expiry_milliseconds(datetime(1970, 1, 1, 0, 0, 1, 999999)), 1999)
        value = session_value(authority())
        expires = value['expiresAtMilliseconds']
        self.assertTrue(live_value(value, expires - 1))
        self.assertFalse(live_value(value, expires))
        self.assertFalse(live_value(value, expires + 1))
        self.assertEqual(value['expiresAtMilliseconds'], expires)

    def test_cache_payload_cannot_supply_foreign_or_extended_authority(self):
        expected = session_value(authority())
        for field, replacement in (('tenantId', 1), ('userId', True), ('csrf', 'b' * 64),
                                   ('expiresAtMilliseconds', expected['expiresAtMilliseconds'] + 1)):
            value = dict(expected); value[field] = replacement
            self.rejected(lambda: decode_value(json.dumps(value).encode(), expected, 0), 'SESSION_CACHE_AUTHORITY_MISMATCH')
        self.assertEqual(decode_value(json.dumps(expected).encode(), expected, 0), expected)

    def test_corrupt_cache_fails_with_server_error(self):
        expected = session_value(authority())
        for raw in (b'{', b'{}', b'{"a":1,"a":2}', b'\xff'):
            with self.assertRaises(AppError) as caught:
                decode_value(raw, expected, 0)
            self.assertEqual(caught.exception.status, 503)
        self.rejected(lambda: decode_value(json.dumps(expected).encode(), expected,
                      expected['expiresAtMilliseconds']), 'LOGIN_REQUIRED')

    def test_cache_key_uses_hash_and_role_namespace(self):
        row = authority()
        source = cache_key(settings()['redis']['keyPrefix'], row['session_hash'])
        self.assertTrue(source.endswith(row['session_hash'].hex()))
        target = cache_key('brlab-20261012:smallapp:122:target:', row['session_hash'])
        self.assertNotEqual(source, target)
        self.rejected(lambda: cache_key('scope:', b'short'), 'SESSION_HASH_INVALID')

    def test_real_sdk_uses_resp2_python_parser_zero_retries(self):
        from redis._parsers import _RESP2Parser
        from redis.connection import Connection
        client = redis_client(settings()['redis'], {'username': 'offline-interface', 'password': 'offline-interface'})
        pool = client.connection_pool
        self.assertIs(pool.connection_class, Connection)
        self.assertEqual(pool.connection_kwargs['protocol'], 2)
        self.assertIs(pool.connection_kwargs['parser_class'], _RESP2Parser)
        self.assertEqual(pool.connection_kwargs['retry']._retries, 0)
        connection = pool.make_connection()  # No socket/network acquisition.
        wire = b''.join(connection.pack_command('SET', 'offline-key', b'private-value', 'PXAT', 1893456000999))
        self.assertIn(b'\r\nPXAT\r\n', wire)
        self.assertIn(b'\r\n1893456000999\r\n', wire)
        client.close()

    def test_tls_is_explicit_and_checked(self):
        from redis.connection import SSLConnection
        value = settings()['redis']; value['tlsCaFile'] = '/run/smallapp/redis-ca.pem'
        client = redis_client(value, {'username': 'offline-interface', 'password': 'offline-interface'})
        self.assertIs(client.connection_pool.connection_class, SSLConnection)
        self.assertTrue(client.connection_pool.connection_kwargs['ssl_check_hostname'])
        self.assertEqual(client.connection_pool.connection_kwargs['ssl_cert_reqs'], 'required')
        client.close()

    def test_capture_redis_classification_has_real_policy_pin(self):
        path = Path(__file__).parent / 'redis-policy.json'; raw = path.read_bytes()
        policy = {'path': str(path.resolve()), 'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}
        recipe = derived_recipe('offline-interface-only', redis_policy_config_pin=policy, queue_policy_config_pin=policy)
        redis = recipe['classes']['redis']
        self.assertEqual(redis['classification'], 'REBUILD')
        self.assertTrue(redis['requiredForBusiness'])
        self.assertEqual(redis['rebuildFrom'], 'DATABASE')
        self.assertEqual(redis['configurationReferences'], [policy])
        self.assertEqual(recipe['classes']['queue']['classification'], 'NOT_PRESENT')

    def test_rebuild_refuses_no_guard_and_source_before_clients(self):
        owner = Store.__new__(Store); owner.settings = settings(); owner.gate = Gate('PROTECTED')
        self.rejected(lambda: owner.rebuild_redis_from_database(None), 'NATIVE_REBUILD_GUARD_REQUIRED')
        self.rejected(lambda: owner.rebuild_redis_from_database(lambda: None), 'TARGET_PROTECTED_REBUILD_REQUIRED')

    def test_role_cookie_names_prevent_source_cookie_reuse(self):
        self.assertTrue(cookie('smallapp_session_source', 'a' * 64, False, age=7200).startswith('smallapp_session_source='))
        self.assertTrue(cookie('smallapp_session_target', 'a' * 64, False, age=7200).startswith('smallapp_session_target='))
        self.rejected(lambda: cookie('smallapp_session', 'a' * 64, False, age=7200), 'COOKIE_VALUE_INVALID')


if __name__ == '__main__':
    unittest.main()
