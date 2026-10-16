"""Required derived session cache. Redis errors propagate; SQL is the authority."""
from datetime import datetime, timezone
import json
import re
import time
from core import AppError, exact, need, strict_json

EXECUTOR_KEY = 'smallapp.redis-session-cache.mysql.v1'


def redis_configuration(value, role):
    exact(value, ('host', 'port', 'database', 'instanceId', 'keyPrefix', 'serverVersion', 'tlsCaFile'))
    need(role in ('SOURCE', 'TARGET') and type(value['host']) is str
         and re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.:-]{0,252}', value['host'])
         and type(value['port']) is int and 0 < value['port'] <= 65535,
         'REDIS_ENDPOINT_REQUIRED', 503)
    need(type(value['database']) is int and value['database'] == 0
         and type(value['instanceId']) is str
         and re.fullmatch('brlab-20261012-smallapp-' + role.lower() + r'-[a-z0-9-]+', value['instanceId'])
         and value['keyPrefix'] == 'brlab-20261012:smallapp:122:' + role.lower() + ':',
         'DEDICATED_REDIS_SCOPE_REQUIRED', 503)
    need(type(value['serverVersion']) is str and re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', value['serverVersion'])
         and tuple(map(int, value['serverVersion'].split('.'))) >= (6, 2, 0),
         'OBSERVED_REDIS_VERSION_WITH_PXAT_REQUIRED', 503)
    need(value['tlsCaFile'] is None or (type(value['tlsCaFile']) is str
         and value['tlsCaFile'].startswith('/')), 'TLS_CONFIGURATION_INVALID', 503)
    return value


def redis_client(settings, private):
    """Real redis-py RESP2 client with its Python parser and zero command retries."""
    from redis import Redis, ConnectionPool
    from redis.connection import Connection, SSLConnection
    from redis._parsers import _RESP2Parser
    from redis.backoff import NoBackoff
    from redis.retry import Retry
    exact(private, ('username', 'password'))
    need(all(type(value) is str and value and not any(c in value for c in '\r\n\0')
             for value in private.values()), 'PRIVATE_REDIS_CREDENTIAL_INVALID', 503)
    arguments = dict(host=settings['host'], port=settings['port'], db=settings['database'],
                     username=private['username'], password=private['password'],
                     protocol=2, parser_class=_RESP2Parser, decode_responses=False,
                     socket_timeout=3, socket_connect_timeout=3, retry=Retry(NoBackoff(), 0),
                     retry_on_error=[], health_check_interval=0, max_connections=4,
                     lib_name=None, lib_version=None)
    if settings['tlsCaFile'] is not None:
        arguments.update(ssl_ca_certs=settings['tlsCaFile'], ssl_cert_reqs='required', ssl_check_hostname=True)
    pool = ConnectionPool(connection_class=Connection if settings['tlsCaFile'] is None else SSLConnection,
                          **arguments)
    return Redis(connection_pool=pool)


def expiry_milliseconds(value):
    need(type(value) is datetime and value.tzinfo is None, 'UTC_SQL_EXPIRY_REQUIRED', 503)
    delta = value.replace(tzinfo=timezone.utc) - datetime(1970, 1, 1, tzinfo=timezone.utc)
    # Floor to milliseconds: Redis must never extend the SQL expiration.
    return (delta.days * 86400 + delta.seconds) * 1000 + delta.microseconds // 1000


def session_value(row):
    exact(row, ('session_hash', 'tenant_id', 'user_id', 'username', 'csrf', 'expires_at'))
    need(type(row['session_hash']) is bytes and len(row['session_hash']) == 32
         and type(row['tenant_id']) is int and row['tenant_id'] == 122
         and type(row['user_id']) is int and 0 < row['user_id'] < 2**63
         and row['username'] == 'aoteman' and type(row['csrf']) is str
         and re.fullmatch('[0-9a-f]{64}', row['csrf']), 'SESSION_AUTHORITY_INVALID', 503)
    return {'schemaVersion': 'SmallAppRedisSession/v1', 'tenantId': 122, 'userId': row['user_id'],
            'username': row['username'], 'csrf': row['csrf'], 'expiresAtMilliseconds': expiry_milliseconds(row['expires_at'])}


def live_value(value, now_ms):
    return value['expiresAtMilliseconds'] > now_ms


def cache_key(prefix, session_hash):
    need(type(session_hash) is bytes and len(session_hash) == 32, 'SESSION_HASH_INVALID', 503)
    return prefix + session_hash.hex()


def decode_value(raw, expected, now_ms):
    need(type(raw) is bytes and 0 < len(raw) <= 1024, 'SESSION_CACHE_VALUE_INVALID', 503)
    try:
        value = strict_json(raw)
        exact(value, expected)
    except AppError as exc:
        raise AppError('SESSION_CACHE_VALUE_INVALID', 503) from exc
    need(all(type(value[key]) is type(expected[key]) and value[key] == expected[key] for key in expected),
         'SESSION_CACHE_AUTHORITY_MISMATCH', 503)
    need(live_value(value, now_ms), 'LOGIN_REQUIRED', 401)
    return value


class SessionCache:
    def __init__(self, settings, role, private):
        self.settings = redis_configuration(settings, role)
        self.client = redis_client(settings, private)
        self.prefix = settings['keyPrefix']

    def health(self):
        need(self.client.ping() is True, 'REDIS_PING_FAILED', 503)
        server = self.client.info('server')
        replication = self.client.info('replication')
        need(server['redis_version'] == self.settings['serverVersion'] and server['redis_mode'] == 'standalone'
             and replication['role'] == 'master', 'REDIS_OBSERVED_RUNTIME_MISMATCH', 503)
        seconds, micros = self.client.time()
        need(abs(seconds * 1000 + micros // 1000 - int(time.time() * 1000)) <= 5000,
             'REDIS_EXPIRY_CLOCK_MISMATCH', 503)
        return {'state': 'UP', 'serverVersion': server['redis_version'], 'mode': server['redis_mode'],
                'role': replication['role'], 'instanceId': self.settings['instanceId'],
                'database': self.settings['database'], 'classification': 'REBUILD', 'rebuildFrom': 'DATABASE'}

    def require_empty(self):
        # The registry must bind a dedicated task Redis instance, never a shared DB.
        need(self.client.dbsize() == 0, 'FRESH_DEDICATED_REDIS_REQUIRED', 503)

    @staticmethod
    def live(row):
        return live_value(session_value(row), int(time.time() * 1000))

    def read(self, row):
        expected = session_value(row)
        raw = self.client.get(cache_key(self.prefix, row['session_hash']))
        if raw is None:
            return None  # Only confirmed key absence admits the declared rebuild.
        value = decode_value(raw, expected, int(time.time() * 1000))
        ttl = self.client.pttl(cache_key(self.prefix, row['session_hash']))
        need(type(ttl) is int and 0 < ttl <= expected['expiresAtMilliseconds'] - int(time.time() * 1000) + 5000,
             'SESSION_CACHE_TTL_INVALID', 503)
        return value

    def put(self, row):
        value = session_value(row)
        need(live_value(value, int(time.time() * 1000)), 'LOGIN_REQUIRED', 401)
        raw = json.dumps(value, separators=(',', ':'), sort_keys=True).encode('utf-8')
        need(self.client.set(cache_key(self.prefix, row['session_hash']), raw,
                             pxat=value['expiresAtMilliseconds']) is True, 'SESSION_CACHE_SET_FAILED', 503)
        readback = self.read(row)
        need(readback is not None, 'SESSION_CACHE_WRITE_READBACK_FAILED', 503)
        return readback

    def remove(self, session_hash):
        key = cache_key(self.prefix, session_hash)
        self.client.delete(key)
        need(self.client.get(key) is None, 'SESSION_CACHE_DELETE_READBACK_FAILED', 503)
