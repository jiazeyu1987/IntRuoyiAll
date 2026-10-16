"""Isolated rehearsal app: MySQL authority, immutable S3, required Redis cache."""
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
import base64
import hashlib
import hmac
import json
import os
import re
import secrets
import threading
import time
from urllib.parse import urlsplit

VERSION = 'smallapp-20261014-r2-redis'
MAX_FILE = 65536
TABLES = ('smallapp_tenants', 'smallapp_users', 'smallapp_sessions',
          'smallapp_records', 'smallapp_objects', 'smallapp_attachments')
COLUMNS = {
    'smallapp_tenants': ('tenant_id', 'name'),
    'smallapp_users': ('id', 'tenant_id', 'username', 'password_hash', 'active', 'failed_logins', 'locked_until'),
    'smallapp_sessions': ('session_hash', 'tenant_id', 'user_id', 'csrf', 'expires_at'),
    'smallapp_records': ('id', 'tenant_id', 'owner_id', 'record_key', 'title', 'content', 'version', 'created_at', 'updated_at'),
    'smallapp_objects': ('id', 'tenant_id', 'bucket', 'object_key', 'size_bytes', 'sha256', 'state'),
    'smallapp_attachments': ('tenant_id', 'record_id', 'object_id', 'filename'),
}


class AppError(Exception):
    def __init__(self, code, status=400):
        super().__init__(code)
        self.code, self.status = code, status


def need(condition, code, status=400):
    if not condition:
        raise AppError(code, status)


def strict_json(raw):
    def pairs(items):
        value = {}
        for key, item in items:
            need(key not in value, 'DUPLICATE_JSON_FIELD')
            value[key] = item
        return value
    def constant(value):
        raise AppError('NONFINITE_JSON_VALUE')
    try:
        return json.loads(raw.decode('utf-8'), object_pairs_hook=pairs, parse_constant=constant)
    except (UnicodeError, ValueError) as exc:
        raise AppError('INVALID_JSON') from exc


def exact(value, fields):
    need(type(value) is dict and set(value) == set(fields), 'EXACT_FIELDS_REQUIRED')
    return value


def identifier(value):
    need(type(value) is int and 0 < value < 2**63, 'POSITIVE_ID_REQUIRED')
    return value


def text(value, maximum, *, empty=False):
    need(type(value) is str and len(value) <= maximum and '\x00' not in value,
         'INVALID_TEXT')
    need(empty or bool(value.strip()), 'NONEMPTY_TEXT_REQUIRED')
    return value.strip() if not empty else value


def filename(value):
    need(type(value) is str and 0 < len(value) <= 200 and value not in ('.', '..')
         and not any(c in value for c in '/\\\r\n\0')
         and not any(ord(c) < 32 for c in value), 'INVALID_FILENAME')
    return value


def record_key(value):
    need(type(value) is str and re.fullmatch('[A-Za-z0-9_-]{1,80}', value), 'RECORD_KEY_INVALID')
    return value


def password_hash(value, salt=None):
    need(type(value) is str and 12 <= len(value) <= 256, 'PASSWORD_LENGTH_REQUIRED')
    salt = secrets.token_bytes(16) if salt is None else salt
    derived = hashlib.scrypt(value.encode(), salt=salt, n=16384, r=8, p=1, dklen=32)
    return 'scrypt$16384$8$1$' + salt.hex() + '$' + derived.hex()


def password_matches(value, encoded):
    need(type(value) is str and 0 < len(value) <= 256, 'LOGIN_DENIED', 401)
    need(type(encoded) is str and re.fullmatch(r'scrypt\$16384\$8\$1\$[0-9a-f]{32}\$[0-9a-f]{64}', encoded),
         'PASSWORD_RECORD_INVALID', 503)
    salt, expected = encoded.split('$')[-2:]
    actual = hashlib.scrypt(value.encode(), salt=bytes.fromhex(salt), n=16384, r=8, p=1, dklen=32).hex()
    return hmac.compare_digest(actual, expected)


def load_fd():
    value = os.environ.get('SMALLAPP_CONFIG_FD')
    need(value is not None and re.fullmatch(r'(?:[3-9]|[1-9][0-9]+)', value), 'PRIVATE_CONFIG_FD_REQUIRED', 503)
    with os.fdopen(int(value), 'rb', closefd=True) as stream:
        raw = stream.read(65537)
    need(0 < len(raw) <= 65536, 'PRIVATE_CONFIG_SIZE_INVALID', 503)
    return strict_json(raw)


def config(value):
    exact(value, ('schemaVersion', 'version', 'logicalConfigRef', 'runtimeRole', 'tenantId',
        'username', 'database', 's3', 'redis', 'http', 'controlSocket', 'startState', 'secretRefs'))
    need(value['schemaVersion'] == 'SmallAppRuntimeConfig/v2' and value['version'] == VERSION,
         'RUNTIME_VERSION_MISMATCH', 503)
    need(value['logicalConfigRef'] in ('smallapp-20261014-source-r1', 'smallapp-20261014-target-r1'),
         'CONFIG_REFERENCE_NOT_ALLOWED', 503)
    need(value['runtimeRole'] in ('SOURCE', 'TARGET') and value['tenantId'] == 122
         and value['username'] == 'aoteman', 'RUNTIME_SCOPE_MISMATCH', 503)
    need((value['runtimeRole'] == 'SOURCE') == (value['logicalConfigRef'] == 'smallapp-20261014-source-r1'),
         'CONFIG_ROLE_MISMATCH', 503)
    database = exact(value['database'], ('host', 'port', 'name', 'tlsCaFile'))
    need(type(database['host']) is str and database['host'] and '\0' not in database['host']
         and type(database['port']) is int and 0 < database['port'] <= 65535
         and type(database['name']) is str
         and re.fullmatch(r'brlab_20261012_smallapp_[a-z0-9_]+', database['name']),
         'TASK_DATABASE_REQUIRED', 503)
    need(database['tlsCaFile'] is None or (type(database['tlsCaFile']) is str
         and database['tlsCaFile'].startswith('/')), 'TLS_CONFIGURATION_INVALID', 503)
    storage = exact(value['s3'], ('endpoint', 'region', 'bucket', 'keyPrefix', 'tlsCaFile'))
    endpoint = urlsplit(storage['endpoint'])
    need(endpoint.scheme in ('http', 'https') and endpoint.hostname and not endpoint.username
         and not endpoint.password and not endpoint.query and not endpoint.fragment
         and endpoint.path in ('', '/'), 'S3_ENDPOINT_INVALID', 503)
    need(type(storage['region']) is str and bool(storage['region'])
         and re.fullmatch(r'brlab-20261012-smallapp-[a-z0-9-]+', storage['bucket'])
         and storage['keyPrefix'] == 'smallapp/122/', 'TASK_OBJECT_SCOPE_REQUIRED', 503)
    need(storage['tlsCaFile'] is None or (type(storage['tlsCaFile']) is str
         and storage['tlsCaFile'].startswith('/')), 'TLS_CONFIGURATION_INVALID', 503)
    http = exact(value['http'], ('listenHost', 'listenPort', 'origin', 'cookieSecure'))
    origin = urlsplit(http['origin'])
    need(origin.scheme in ('http', 'https') and origin.hostname and not origin.username
         and not origin.password and origin.path == '' and not origin.query and not origin.fragment,
         'HTTP_ORIGIN_INVALID', 503)
    need(type(http['cookieSecure']) is bool and http['cookieSecure'] == (origin.scheme == 'https'),
         'COOKIE_SECURITY_MISMATCH', 503)
    need(http['listenHost'] in ('127.0.0.1', '0.0.0.0') and type(http['listenPort']) is int
         and 1024 <= http['listenPort'] <= 65535, 'HTTP_LISTENER_INVALID', 503)
    need(type(value['controlSocket']) is str and value['controlSocket'].startswith('/run/smallapp/')
         and re.fullmatch(r'/run/smallapp/[a-z0-9-]+\.sock', value['controlSocket']),
         'CONTROL_SOCKET_INVALID', 503)
    need(value['startState'] in ('OPEN', 'PROTECTED')
         and (value['runtimeRole'] != 'TARGET' or value['startState'] == 'PROTECTED'),
         'TARGET_PROTECTED_START_REQUIRED', 503)
    from redis_cache import redis_configuration
    redis_configuration(value['redis'], value['runtimeRole'])
    exact(value['secretRefs'], ('mysql', 's3', 'redis', 'control'))
    need(value['secretRefs'] == {'mysql': 'smallapp.mysql', 's3': 'smallapp.s3',
         'redis': 'smallapp.redis', 'control': 'smallapp.control'},
         'SECRET_REFERENCE_SCOPE_INVALID', 503)
    return value


class Gate:
    """One process serializes all DB/object/session mutations and control transitions."""
    def __init__(self, state):
        need(state in ('OPEN', 'QUIESCED', 'PROTECTED'), 'GATE_STATE_INVALID')
        self.lock = threading.RLock()
        self.state = state

    @contextmanager
    def mutation(self):
        with self.lock:
            need(self.state == 'OPEN', 'WRITES_PAUSED', 503)
            try:
                yield
            except BaseException as exc:
                if not isinstance(exc, AppError) or exc.status >= 500:
                    self.state = 'QUIESCED'
                raise


class Store:
    def __init__(self, settings, private):
        import pymysql
        import boto3
        from botocore.config import Config
        self.settings = config(settings)
        exact(private, ('mysql', 's3', 'redis', 'control'))
        mysql = exact(private['mysql'], ('username', 'password'))
        s3 = exact(private['s3'], ('accessKey', 'secretKey'))
        control = exact(private['control'], ('authKeyBase64', 'controlToken'))
        for value in (*mysql.values(), *s3.values(), control['controlToken']):
            need(type(value) is str and value and not any(c in value for c in '\r\n\0'),
                 'PRIVATE_CREDENTIAL_INVALID', 503)
        try:
            self.auth_key = base64.b64decode(control['authKeyBase64'], validate=True)
        except (ValueError, TypeError) as exc:
            raise AppError('AUTH_KEY_INVALID', 503) from exc
        need(32 <= len(self.auth_key) <= 64 and re.fullmatch('[A-Za-z0-9_-]{32,128}', control['controlToken']),
             'PRIVATE_KEY_LENGTH_REQUIRED', 503)
        self.control_token = control['controlToken']
        db = settings['database']
        self.db_args = dict(host=db['host'], port=db['port'], database=db['name'],
            user=mysql['username'], password=mysql['password'], charset='utf8mb4',
            autocommit=False, connect_timeout=5, read_timeout=15, write_timeout=15,
            cursorclass=pymysql.cursors.DictCursor)
        if db['tlsCaFile'] is not None:
            self.db_args.update(ssl_ca=db['tlsCaFile'], ssl_verify_cert=True, ssl_verify_identity=True)
        self._connect = pymysql.connect
        object_config = settings['s3']
        self.s3 = boto3.client('s3', endpoint_url=object_config['endpoint'],
            aws_access_key_id=s3['accessKey'], aws_secret_access_key=s3['secretKey'],
            region_name=object_config['region'],
            verify=True if object_config['tlsCaFile'] is None else object_config['tlsCaFile'],
            config=Config(signature_version='s3v4', s3={'addressing_style': 'path'},
                          retries={'total_max_attempts': 1}, connect_timeout=5, read_timeout=15))
        self.bucket = object_config['bucket']
        self.prefix = object_config['keyPrefix']
        self.tenant = settings['tenantId']
        self.gate = Gate(settings['startState'])
        from redis_cache import SessionCache
        self.cache = SessionCache(settings['redis'], settings['runtimeRole'], private['redis'])

    @contextmanager
    def transaction(self):
        connection = self._connect(**self.db_args)
        try:
            with connection.cursor() as cursor:
                cursor.execute("SET time_zone = '+00:00'")
                yield cursor
            connection.commit()
        except BaseException:
            connection.rollback()
            raise
        finally:
            connection.close()

    def environment(self, *, empty=False):
        with self.transaction() as cur:
            cur.execute('SELECT @@log_bin AS enabled, @@binlog_format AS format, @@binlog_row_image AS image')
            flags = cur.fetchone()
            need(flags == {'enabled': 1, 'format': 'ROW', 'image': 'FULL'}, 'ROW_FULL_BINLOG_REQUIRED', 503)
            cur.execute('SHOW TABLES')
            tables = {next(iter(row.values())) for row in cur.fetchall()}
            need(tables == (set() if empty else set(TABLES)), 'TASK_SCHEMA_TABLE_SET_MISMATCH', 503)
            if not empty:
                for table in TABLES:
                    cur.execute('DESCRIBE `' + table + '`')
                    need(tuple(row['Field'] for row in cur.fetchall()) == COLUMNS[table],
                         'TASK_SCHEMA_COLUMN_SET_MISMATCH', 503)
                cur.execute('SELECT tenant_id, username, active FROM smallapp_users')
                need(list(cur.fetchall()) == [{'tenant_id': 122, 'username': 'aoteman', 'active': 1}],
                     'INITIAL_IDENTITY_SCOPE_MISMATCH', 503)
                cur.execute('SELECT tenant_id,name FROM smallapp_tenants')
                need(list(cur.fetchall()) == [{'tenant_id': 122, 'name': '测试租户'}], 'TENANT_SCOPE_MISMATCH', 503)
                for table in TABLES:
                    cur.execute('SELECT COUNT(*) AS n FROM `' + table + '` WHERE tenant_id<>%s', (self.tenant,))
                    need(cur.fetchone()['n'] == 0, 'CROSS_TENANT_ROWS_REFUSED', 503)
        self.s3.head_bucket(Bucket=self.bucket)
        self.cache.health()
        if empty:
            self.cache.require_empty()

    def require_clean(self):
        with self.transaction() as cur:
            cur.execute("SELECT COUNT(*) AS n FROM smallapp_objects WHERE state <> 'ACTIVE'")
            need(cur.fetchone()['n'] == 0, 'OBJECT_OPERATION_PENDING', 503)
            cur.execute('SELECT COUNT(*) AS n FROM smallapp_objects o LEFT JOIN smallapp_attachments a '
                        'ON a.object_id=o.id AND a.tenant_id=o.tenant_id WHERE a.object_id IS NULL')
            need(cur.fetchone()['n'] == 0, 'OBJECT_REFERENCE_INCONSISTENT', 503)

    def login(self, username, password):
        need(username == self.settings['username'], 'LOGIN_DENIED', 401)
        with self.gate.mutation():
            self.require_clean()
            with self.transaction() as cur:
                cur.execute('SELECT id,password_hash,failed_logins,locked_until FROM smallapp_users '
                            'WHERE tenant_id=%s AND username=%s AND active=1 FOR UPDATE',
                            (self.tenant, username))
                user = cur.fetchone()
                need(user is not None, 'LOGIN_DENIED', 401)
                now = datetime.now(timezone.utc).replace(tzinfo=None)
                need(user['locked_until'] is None or user['locked_until'] <= now, 'LOGIN_RATE_LIMITED', 429)
                accepted = password_matches(password, user['password_hash'])
                if not accepted:
                    failures = (0 if user['locked_until'] is not None else user['failed_logins']) + 1
                    cur.execute('UPDATE smallapp_users SET failed_logins=%s,locked_until=%s '
                                'WHERE id=%s AND tenant_id=%s',
                                (failures, now + timedelta(minutes=5) if failures >= 5 else None, user['id'], self.tenant))
                else:
                    cur.execute('UPDATE smallapp_users SET failed_logins=0,locked_until=NULL WHERE id=%s AND tenant_id=%s',
                                (user['id'], self.tenant))
                    cur.execute('DELETE FROM smallapp_sessions WHERE tenant_id=%s AND expires_at<=%s', (self.tenant, now))
                    sid, csrf = secrets.token_hex(32), secrets.token_hex(32)
                    expires = now + timedelta(hours=2)
                    session_hash = hashlib.sha256(sid.encode()).digest()
                    cur.execute('INSERT INTO smallapp_sessions(session_hash,tenant_id,user_id,csrf,expires_at) '
                                'VALUES(%s,%s,%s,%s,%s)',
                                (session_hash, self.tenant, user['id'], csrf, expires))
            # Failed-attempt accounting must commit before denying login.
            need(accepted, 'LOGIN_DENIED', 401)
            self.cache.put(dict(session_hash=session_hash, tenant_id=self.tenant,
                                user_id=user['id'], username=username, csrf=csrf, expires_at=expires))
            return sid, csrf

    def session(self, sid):
        need(type(sid) is str and re.fullmatch('[0-9a-f]{64}', sid), 'LOGIN_REQUIRED', 401)
        with self.gate.lock:
            with self.transaction() as cur:
                cur.execute('SELECT s.session_hash,s.tenant_id,s.user_id,s.csrf,s.expires_at,u.username '
                            'FROM smallapp_sessions s JOIN smallapp_users u '
                            'ON u.id=s.user_id AND u.tenant_id=s.tenant_id WHERE s.session_hash=%s '
                            'AND s.tenant_id=%s AND u.active=1 AND s.expires_at>UTC_TIMESTAMP(6)',
                            (hashlib.sha256(sid.encode()).digest(), self.tenant))
                result = cur.fetchone()
            need(result is not None and result['username'] == self.settings['username'], 'LOGIN_REQUIRED', 401)
            cached = self.cache.read(result)
            if cached is None:
                # Declared REBUILD from SQL authority, never a Redis outage fallback.
                with self.gate.mutation():
                    cached = self.cache.put(result)
            return dict(user_id=cached['userId'], username=cached['username'], csrf=cached['csrf'])

    def logout(self, sid):
        with self.gate.mutation(), self.transaction() as cur:
            cur.execute('DELETE FROM smallapp_sessions WHERE session_hash=%s AND tenant_id=%s',
                        (hashlib.sha256(sid.encode()).digest(), self.tenant))
            self.cache.remove(hashlib.sha256(sid.encode()).digest())

    def rebuild_redis_from_database(self, guard):
        """Business cache rebuild for the existing target-bound derived executor.

        The owner supplies the existing native bound/lease/lock guard. This is
        not an archive/restore entry point or a RecoveryDerivedExecution proof.
        """
        need(callable(guard), 'NATIVE_REBUILD_GUARD_REQUIRED', 503)
        with self.gate.lock:
            need(self.settings['runtimeRole'] == 'TARGET' and self.gate.state == 'PROTECTED',
                 'TARGET_PROTECTED_REBUILD_REQUIRED', 503)
            guard()
            self.environment()
            self.require_clean()
            self.cache.require_empty()
            with self.transaction() as cur:
                cur.execute('SELECT s.session_hash,s.tenant_id,s.user_id,s.csrf,s.expires_at,u.username '
                            'FROM smallapp_sessions s JOIN smallapp_users u '
                            'ON u.id=s.user_id AND u.tenant_id=s.tenant_id '
                            'WHERE s.tenant_id=%s AND u.active=1 AND s.expires_at>UTC_TIMESTAMP(6) '
                            'ORDER BY s.session_hash', (self.tenant,))
                rows = cur.fetchall()
            need(len(rows) <= 10000, 'SESSION_REBUILD_LIMIT_EXCEEDED', 503)
            rebuilt = 0
            for row in rows:
                guard()
                # Sessions expiring during rebuild stay expired, with no SET.
                if self.cache.live(row):
                    self.cache.put(row)
                    rebuilt += 1
            guard()
            verified = 0
            for row in rows:
                if self.cache.live(row):
                    need(self.cache.read(row) is not None, 'SESSION_REBUILD_READBACK_FAILED', 503)
                    verified += 1
            self.cache.health()
            guard()
            return {'schemaVersion': 'SmallAppRedisRebuildObservation/v1', 'version': VERSION,
                    'classification': 'REBUILD', 'rebuildFrom': 'DATABASE', 'rebuiltCount': rebuilt,
                    'liveReadbackCount': verified, 'state': 'READ_BACK',
                    'runtimeRole': 'TARGET', 'gateState': self.gate.state}

    def records(self):
        with self.transaction() as cur:
            cur.execute('SELECT r.id,r.tenant_id,r.record_key,r.title,r.content,r.version,a.filename,o.sha256,o.size_bytes,o.bucket,o.object_key '
                        'FROM smallapp_records r LEFT JOIN smallapp_attachments a '
                        'ON a.record_id=r.id AND a.tenant_id=r.tenant_id LEFT JOIN smallapp_objects o '
                        'ON o.id=a.object_id AND o.tenant_id=a.tenant_id WHERE r.tenant_id=%s ORDER BY r.id', (self.tenant,))
            result = cur.fetchall()
        need(len(result) <= 1000, 'RECORD_LIMIT_EXCEEDED', 503)
        return result

    def record(self, cur, record_id, expected=None):
        cur.execute('SELECT id,version FROM smallapp_records WHERE id=%s AND tenant_id=%s FOR UPDATE',
                    (identifier(record_id), self.tenant))
        row = cur.fetchone()
        need(row is not None, 'RECORD_NOT_FOUND', 404)
        if expected is not None:
            need(row['version'] == identifier(expected), 'RECORD_VERSION_CONFLICT', 409)
        return row

    def create_record(self, user, payload):
        exact(payload, ('key', 'title', 'content'))
        key = record_key(payload['key'])
        title, content = text(payload['title'], 120), text(payload['content'], 5000, empty=True)
        with self.gate.mutation():
            self.require_clean()
            with self.transaction() as cur:
                cur.execute('SELECT COUNT(*) AS n FROM smallapp_records WHERE tenant_id=%s', (self.tenant,))
                need(cur.fetchone()['n'] < 1000, 'RECORD_LIMIT_EXCEEDED', 409)
                cur.execute('SELECT COUNT(*) AS n FROM smallapp_records WHERE tenant_id=%s AND record_key=%s', (self.tenant, key))
                need(cur.fetchone()['n'] == 0, 'RECORD_KEY_CONFLICT', 409)
                cur.execute('INSERT INTO smallapp_records(tenant_id,owner_id,record_key,title,content) VALUES(%s,%s,%s,%s,%s)',
                            (self.tenant, user['user_id'], key, title, content))
                record_id = cur.lastrowid
            return record_id

    def edit_record(self, record_id, payload):
        exact(payload, ('key', 'title', 'content', 'expectedVersion'))
        key = record_key(payload['key'])
        title, content = text(payload['title'], 120), text(payload['content'], 5000, empty=True)
        with self.gate.mutation():
            self.require_clean()
            with self.transaction() as cur:
                self.record(cur, record_id, payload['expectedVersion'])
                cur.execute('SELECT COUNT(*) AS n FROM smallapp_records WHERE tenant_id=%s AND record_key=%s AND id<>%s',
                            (self.tenant, key, record_id))
                need(cur.fetchone()['n'] == 0, 'RECORD_KEY_CONFLICT', 409)
                cur.execute('UPDATE smallapp_records SET record_key=%s,title=%s,content=%s,version=version+1 WHERE id=%s AND tenant_id=%s',
                            (key, title, content, record_id, self.tenant))
                need(cur.rowcount == 1, 'RECORD_UPDATE_FAILED', 503)

    def key(self, object_id):
        need(type(object_id) is str and re.fullmatch('[0-9a-f]{32}', object_id), 'OBJECT_ID_INVALID', 503)
        return self.prefix + object_id

    def read_object(self, row):
        need(row['bucket'] == self.bucket and row['object_key'] == self.key(row['id'])
             and row['tenant_id'] == self.tenant and 0 < row['size_bytes'] <= MAX_FILE,
             'OBJECT_SCOPE_MISMATCH', 503)
        result = self.s3.get_object(Bucket=self.bucket, Key=row['object_key'])
        with result['Body'] as stream:
            raw = stream.read(MAX_FILE + 1)
        need(len(raw) == row['size_bytes'] and hashlib.sha256(raw).hexdigest() == row['sha256'],
             'OBJECT_CONTENT_MISMATCH', 503)
        return raw

    def attachment(self, record_id):
        with self.gate.lock:
            with self.transaction() as cur:
                cur.execute('SELECT o.*,a.filename FROM smallapp_attachments a JOIN smallapp_objects o '
                            'ON o.id=a.object_id AND o.tenant_id=a.tenant_id '
                            "WHERE a.record_id=%s AND a.tenant_id=%s AND o.state='ACTIVE'", (identifier(record_id), self.tenant))
                row = cur.fetchone()
            need(row is not None, 'ATTACHMENT_NOT_FOUND', 404)
            return filename(row['filename']), self.read_object(row)

    def queue_old_attachment(self, cur, record_id, *, required=False):
        cur.execute('SELECT object_id FROM smallapp_attachments WHERE record_id=%s AND tenant_id=%s FOR UPDATE',
                    (record_id, self.tenant))
        old = cur.fetchone()
        need(not required or old is not None, 'ATTACHMENT_NOT_FOUND', 404)
        if old is not None:
            cur.execute('DELETE FROM smallapp_attachments WHERE record_id=%s AND tenant_id=%s', (record_id, self.tenant))
            cur.execute("UPDATE smallapp_objects SET state='DELETE_PENDING' WHERE id=%s AND tenant_id=%s AND state='ACTIVE'",
                        (old['object_id'], self.tenant))
            need(cur.rowcount == 1, 'ATTACHMENT_STATE_CONFLICT', 503)

    def upload(self, record_id, expected, name, raw):
        name = filename(name)
        need(type(raw) is bytes and 0 < len(raw) <= MAX_FILE, 'FILE_SIZE_LIMIT')
        object_id = secrets.token_hex(16)
        row = dict(id=object_id, tenant_id=self.tenant, bucket=self.bucket, object_key=self.key(object_id),
                   size_bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest())
        with self.gate.mutation():
            self.require_clean()
            with self.transaction() as cur:
                self.record(cur, record_id, expected)
                cur.execute("INSERT INTO smallapp_objects(id,tenant_id,bucket,object_key,size_bytes,sha256,state) "
                            "VALUES(%s,%s,%s,%s,%s,%s,'STAGED')", tuple(row.values()))
            # A durable intent precedes S3. An ambiguous PUT/commit leaves STAGED/ACTIVE,
            # never an invisible object or a blind destructive compensation.
            try:
                self.s3.put_object(Bucket=self.bucket, Key=row['object_key'], Body=raw,
                                   ContentType='application/octet-stream', Metadata={'sha256': row['sha256']})
                need(self.read_object(row) == raw, 'OBJECT_UPLOAD_READBACK_FAILED', 503)
                with self.transaction() as cur:
                    self.record(cur, record_id, expected)
                    self.queue_old_attachment(cur, record_id)
                    cur.execute('INSERT INTO smallapp_attachments(tenant_id,record_id,object_id,filename) VALUES(%s,%s,%s,%s)',
                                (self.tenant, record_id, object_id, name))
                    cur.execute("UPDATE smallapp_objects SET state='ACTIVE' WHERE id=%s AND tenant_id=%s AND state='STAGED'",
                                (object_id, self.tenant))
                    need(cur.rowcount == 1, 'OBJECT_ACTIVATION_FAILED', 503)
                    cur.execute('UPDATE smallapp_records SET version=version+1 WHERE id=%s AND tenant_id=%s', (record_id, self.tenant))
                self.reconcile_objects()
            except BaseException:
                self.gate.state = 'QUIESCED'
                raise

    def delete(self, record_id, expected, *, attachment_only=False):
        with self.gate.mutation():
            self.require_clean()
            with self.transaction() as cur:
                self.record(cur, record_id, expected)
                self.queue_old_attachment(cur, record_id, required=attachment_only)
                if attachment_only:
                    cur.execute('UPDATE smallapp_records SET version=version+1 WHERE id=%s AND tenant_id=%s', (record_id, self.tenant))
                else:
                    cur.execute('DELETE FROM smallapp_records WHERE id=%s AND tenant_id=%s', (record_id, self.tenant))
                need(cur.rowcount == 1, 'RECORD_DELETE_FAILED', 503)
            try:
                self.reconcile_objects()
            except BaseException:
                self.gate.state = 'QUIESCED'
                raise

    def object_absent(self, key):
        from botocore.exceptions import ClientError
        try:
            self.s3.head_object(Bucket=self.bucket, Key=key)
        except ClientError as exc:
            if exc.response['ResponseMetadata']['HTTPStatusCode'] == 404:
                return True  # Explicit S3 absence, not a connectivity downgrade.
            raise
        return False

    def reconcile_objects(self):
        """Only removes durable unreferenced staged/retired objects, never business rows."""
        with self.gate.lock:
            with self.transaction() as cur:
                cur.execute("SELECT * FROM smallapp_objects WHERE tenant_id=%s AND state IN ('STAGED','DELETE_PENDING')", (self.tenant,))
                pending = cur.fetchall()
            for row in pending:
                need(row['bucket'] == self.bucket and row['object_key'] == self.key(row['id']), 'OBJECT_SCOPE_MISMATCH', 503)
                with self.transaction() as cur:
                    cur.execute('SELECT COUNT(*) AS n FROM smallapp_attachments WHERE object_id=%s AND tenant_id=%s', (row['id'], self.tenant))
                    need(cur.fetchone()['n'] == 0, 'PENDING_OBJECT_STILL_REFERENCED', 503)
                if not self.object_absent(row['object_key']):
                    self.read_object(row)
                    self.s3.delete_object(Bucket=self.bucket, Key=row['object_key'])
                    need(self.object_absent(row['object_key']), 'OBJECT_DELETE_READBACK_FAILED', 503)
                with self.transaction() as cur:
                    cur.execute("DELETE FROM smallapp_objects WHERE id=%s AND tenant_id=%s AND state IN ('STAGED','DELETE_PENDING')",
                                (row['id'], self.tenant))
                    need(cur.rowcount == 1, 'OBJECT_INTENT_DELETE_FAILED', 503)

    def login_token(self):
        payload = secrets.token_hex(16) + '.' + str(int(time.time()))
        return payload + '.' + hmac.new(self.auth_key, payload.encode(), hashlib.sha256).hexdigest()

    def require_login_token(self, supplied, cookie):
        need(type(supplied) is str and type(cookie) is str
             and re.fullmatch(r'[0-9a-f]{32}\.[0-9]{10}\.[0-9a-f]{64}', supplied)
             and re.fullmatch(r'[0-9a-f]{32}\.[0-9]{10}\.[0-9a-f]{64}', cookie)
             and hmac.compare_digest(supplied, cookie), 'LOGIN_CSRF_INVALID', 403)
        parts = supplied.split('.')
        need(len(parts) == 3 and re.fullmatch('[0-9a-f]{32}', parts[0])
             and re.fullmatch('[0-9]{10}', parts[1]) and re.fullmatch('[0-9a-f]{64}', parts[2]), 'LOGIN_CSRF_INVALID', 403)
        payload = '.'.join(parts[:2])
        need(hmac.compare_digest(parts[2], hmac.new(self.auth_key, payload.encode(), hashlib.sha256).hexdigest())
             and 0 <= time.time() - int(parts[1]) <= 600, 'LOGIN_CSRF_EXPIRED', 403)

    def control(self, request):
        exact(request, ('schemaVersion', 'operation', 'requestId', 'token'))
        need(request['schemaVersion'] == 'SmallAppControlRequest/v1'
             and type(request['requestId']) is str and re.fullmatch('[A-Za-z0-9_-]{1,80}', request['requestId'])
             and type(request['token']) is str and re.fullmatch('[A-Za-z0-9_-]{32,128}', request['token'])
             and hmac.compare_digest(request['token'], self.control_token),
             'CONTROL_DENIED', 403)
        operation = request['operation']
        need(operation in ('STATUS', 'QUIESCE', 'OPEN', 'RECONCILE_OBJECTS', 'STOP'), 'CONTROL_OPERATION_INVALID')
        with self.gate.lock:
            if operation in ('QUIESCE', 'STOP'):
                self.gate.state = 'QUIESCED' if operation == 'QUIESCE' else 'PROTECTED'
            if operation == 'RECONCILE_OBJECTS':
                need(self.gate.state in ('QUIESCED', 'PROTECTED'), 'RECONCILE_REQUIRES_PAUSE', 409)
                self.reconcile_objects()
            self.require_clean()
            redis_health = self.cache.health()
            if operation == 'OPEN':
                self.gate.state = 'OPEN'
            return dict(schemaVersion='SmallAppControlResult/v2', requestId=request['requestId'],
                        operation=operation, state=self.gate.state, pendingObjects=0,
                        activeMutations=0, version=VERSION, tenantId=self.tenant,
                        logicalConfigRef=self.settings['logicalConfigRef'], stopRequested=operation == 'STOP',
                        redis=redis_health)
