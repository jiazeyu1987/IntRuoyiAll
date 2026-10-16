"""W05 explicit private-file materializer from pinned actual public config + FD secrets."""
from pathlib import Path
import hashlib
import json
import os
import stat
from core import AppError, VERSION, config, exact, load_fd, need, strict_json
from frontend import frontend_config


def public_configuration(value):
    exact(value, ('schemaVersion', 'version', 'backend', 'frontend'))
    need(value['schemaVersion'] == 'SmallAppPublicConfiguration/v2' and value['version'] == VERSION,
         'PUBLIC_CONFIGURATION_VERSION_MISMATCH')
    config(value['backend'])
    frontend_config(value['frontend'])
    need(value['backend']['http']['origin'] == value['frontend']['http']['origin'], 'PUBLIC_ORIGIN_ROLE_MISMATCH')
    return value


def materialize(request):
    exact(request, ('schemaVersion', 'role', 'publicConfigPath', 'expectedPublicConfigSha256',
                    'outputDirectory', 'secrets', 'bootstrap'))
    need(request['schemaVersion'] == 'SmallAppMaterializeRequest/v1'
         and request['role'] in ('backend', 'frontend', 'install'), 'MATERIALIZE_REQUEST_INVALID')
    public_path = Path(request['publicConfigPath'])
    need(public_path.is_absolute() and not public_path.is_symlink(), 'PUBLIC_CONFIG_PATH_INVALID')
    raw = public_path.read_bytes()
    need(len(raw) <= 65536 and hashlib.sha256(raw).hexdigest() == request['expectedPublicConfigSha256'],
         'PUBLIC_CONFIGURATION_CHANGED', 503)
    bundle = public_configuration(strict_json(raw))
    role = request['role']
    settings = bundle['frontend' if role == 'frontend' else 'backend']
    if role == 'frontend':
        frontend_config(settings)
        need(request['secrets'] is None and request['bootstrap'] is None, 'FRONTEND_PRIVATE_FIELDS_REFUSED')
        envelope = {'config': settings}
    else:
        config(settings)
        exact(request['secrets'], ('mysql', 's3', 'redis', 'control'))
        envelope = {'config': settings, 'secrets': request['secrets']}
        if role == 'install':
            need(settings['runtimeRole'] == 'SOURCE', 'TARGET_BOOTSTRAP_REFUSED')
            envelope['bootstrap'] = exact(request['bootstrap'], ('username', 'tenantId', 'password'))
        else:
            need(request['bootstrap'] is None, 'BOOTSTRAP_PASSWORD_NOT_ALLOWED_AT_RUNTIME')
    directory = Path(request['outputDirectory'])
    info = directory.lstat()
    need(directory.is_absolute() and stat.S_ISDIR(info.st_mode) and not directory.is_symlink()
         and info.st_uid == os.getuid() and stat.S_IMODE(info.st_mode) == 0o700,
         'PRIVATE_RUNTIME_DIRECTORY_REQUIRED', 503)
    output = directory / (role + '.private.json')
    encoded = json.dumps(envelope, ensure_ascii=False, separators=(',', ':')).encode()
    need(len(encoded) <= 65536, 'PRIVATE_CONFIG_SIZE_INVALID')
    descriptor = os.open(str(output), os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    with os.fdopen(descriptor, 'wb') as stream:
        stream.write(encoded)
        stream.flush()
        os.fsync(stream.fileno())
    return {'schemaVersion': 'SmallAppMaterializeResult/v1', 'role': role,
            'publicConfigSha256': request['expectedPublicConfigSha256'], 'state': 'MATERIALIZED'}


if __name__ == '__main__':
    try:
        print(json.dumps(materialize(load_fd())))
    except Exception as exc:
        print(json.dumps({'state': 'FAILED', 'errorCode': exc.code if isinstance(exc, AppError) else 'MATERIALIZE_FAILED',
                          'exceptionClass': type(exc).__name__}))
        raise SystemExit(1) from None
