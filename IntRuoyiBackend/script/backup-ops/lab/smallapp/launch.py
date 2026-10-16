"""Explicit container launcher: private mount -> inherited FD, no secret argv/env."""
from pathlib import Path
import os
import stat
import sys
import json
from core import AppError, need
from verify_runtime import verify


def main():
    need(os.name == 'posix', 'LINUX_RUNTIME_REQUIRED', 503)
    need(len(sys.argv) == 2 and sys.argv[1] in ('backend', 'frontend', 'install'), 'ROLE_ARGUMENT_REQUIRED', 503)
    role = sys.argv[1]
    verify()
    parent = Path('/run/smallapp')
    info = parent.lstat()
    need(stat.S_ISDIR(info.st_mode) and info.st_uid == os.getuid()
         and stat.S_IMODE(info.st_mode) == 0o700, 'PRIVATE_RUNTIME_DIRECTORY_REQUIRED', 503)
    descriptor = os.open(str(parent / (role + '.private.json')), os.O_RDONLY | os.O_NOFOLLOW)
    info = os.fstat(descriptor)
    need(stat.S_ISREG(info.st_mode) and info.st_uid == os.getuid() and info.st_nlink == 1
         and stat.S_IMODE(info.st_mode) == 0o600 and 0 < info.st_size <= 65536,
         'PRIVATE_RUNTIME_MATERIAL_REQUIRED', 503)
    need(descriptor >= 3, 'PRIVATE_CONFIG_FD_REQUIRED', 503)
    os.set_inheritable(descriptor, True)
    os.environ['SMALLAPP_CONFIG_FD'] = str(descriptor)
    script = {'backend': 'server.py', 'frontend': 'frontend.py', 'install': 'install.py'}[role]
    os.execv(sys.executable, [sys.executable, '-B', str(Path(__file__).parent / script)])


if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        print(json.dumps({'state': 'FAILED', 'errorCode': exc.code if isinstance(exc, AppError) else 'LAUNCH_DEPENDENCY_FAILED',
                          'exceptionClass': type(exc).__name__}))
        raise SystemExit(1) from None
