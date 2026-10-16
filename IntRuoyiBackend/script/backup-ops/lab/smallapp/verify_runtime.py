"""Offline dependency probe; imports real libraries, never connects services."""
from importlib.metadata import version
from pathlib import Path
import json
import platform
import sys
from core import AppError, VERSION, need


def verify():
    need(sys.implementation.name == 'cpython' and (3, 11) <= sys.version_info[:2] < (3, 13),
         'TRUSTED_CPYTHON_311_OR_312_REQUIRED', 503)
    requirements = (Path(__file__).parent / 'requirements.txt').read_text(encoding='ascii').splitlines()
    dependencies = {}
    for requirement in requirements:
        name, expected = requirement.split('==')
        actual = version(name)
        need(actual == expected, 'PINNED_DEPENDENCY_VERSION_REQUIRED', 503)
        dependencies[name] = actual
    import pymysql
    import boto3
    from redis_cache import redis_client
    from pymysql import _auth
    from botocore.config import Config
    from botocore.session import get_session
    need(callable(pymysql.connect) and callable(boto3.client), 'SDK_INTERFACE_REQUIRED', 503)
    need(_auth._have_cryptography and callable(_auth.sha2_rsa_encrypt), 'MYSQL_SHA2_AUTH_DEPENDENCY_REQUIRED', 503)
    model = get_session().get_service_model('s3')
    for operation in ('HeadBucket', 'HeadObject', 'GetObject', 'PutObject', 'DeleteObject'):
        need(model.operation_model(operation) is not None, 'S3_OPERATION_REQUIRED', 503)
    sdk_config = Config(signature_version='s3v4', s3={'addressing_style': 'path'},
                        retries={'total_max_attempts': 1}, connect_timeout=5, read_timeout=15)
    need(sdk_config.retries == {'total_max_attempts': 1}, 'SDK_RETRY_POLICY_CHANGED', 503)
    # Interface fixture only. Client construction never acquires a connection.
    client = redis_client({'host': '127.0.0.1', 'port': 6379, 'database': 0, 'tlsCaFile': None},
                          {'username': 'offline-interface', 'password': 'offline-interface'})
    need(all(callable(getattr(client, name)) for name in ('ping', 'info', 'time', 'get', 'set', 'pttl', 'delete', 'dbsize')),
         'REDIS_SDK_INTERFACE_REQUIRED', 503)
    need(client.connection_pool.connection_kwargs['retry']._retries == 0,
         'REDIS_RETRY_POLICY_CHANGED', 503)
    client.close()
    return {'schemaVersion': 'SmallAppRuntimeProbe/v1', 'version': VERSION,
            'implementation': sys.implementation.name, 'pythonVersion': platform.python_version(),
            'platform': platform.system(), 'machine': platform.machine(), 'dependencies': dependencies,
            'databaseConnected': False, 'objectRequestSubmitted': False, 'redisConnected': False}


if __name__ == '__main__':
    try:
        print(json.dumps(verify()))
    except Exception as exc:
        print(json.dumps({'state': 'FAILED', 'errorCode': exc.code if isinstance(exc, AppError) else 'RUNTIME_DEPENDENCY_MISSING',
                          'exceptionClass': type(exc).__name__}))
        raise SystemExit(1) from None
