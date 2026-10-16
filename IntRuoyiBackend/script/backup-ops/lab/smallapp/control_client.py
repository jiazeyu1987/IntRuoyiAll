"""W05/W01 private control client. Envelope arrives only through inherited FD."""
import json
from pathlib import PurePosixPath
import re
import socket
from core import AppError, exact, load_fd, need, strict_json
from core import VERSION

TASK_ROOT = '/mnt/intruoyi-data/backup-restore-lab/20261012-w01-w07/'


def control_socket_path(path):
    need(type(path) is str and '\\' not in path and str(PurePosixPath(path)) == path
         and '..' not in PurePosixPath(path).parts
         and re.fullmatch(r'[a-z0-9-]+\.sock', PurePosixPath(path).name), 'CONTROL_SOCKET_INVALID')
    # Two explicit views of the same task-owned mounted socket. No probing or
    # automatic namespace switch: the owner supplies the actual observed path.
    need(path.startswith('/run/smallapp/') and len(PurePosixPath(path).parts) == 4
         or path.startswith(TASK_ROOT) and '/private/' in path, 'CONTROL_SOCKET_INVALID')
    return path


def main():
    envelope = exact(load_fd(), ('socket', 'request'))
    path = control_socket_path(envelope['socket'])
    request = exact(envelope['request'], ('schemaVersion', 'operation', 'requestId', 'token'))
    raw = json.dumps(request, separators=(',', ':')).encode() + b'\n'
    need(len(raw) <= 8192, 'CONTROL_FRAME_INVALID')
    with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as client:
        client.settimeout(30)
        client.connect(path)
        client.sendall(raw)
        with client.makefile('rb') as stream:
            response = stream.readline(8193)
    need(0 < len(response) <= 8192 and response.endswith(b'\n'), 'CONTROL_RESPONSE_INVALID', 503)
    result = strict_json(response)
    if result.get('schemaVersion') == 'SmallAppControlError/v1':
        exact(result, ('schemaVersion', 'state', 'errorCode'))
        need(False, result['errorCode'], 503)
    exact(result, ('schemaVersion', 'requestId', 'operation', 'state', 'pendingObjects', 'activeMutations',
                  'version', 'tenantId', 'logicalConfigRef', 'stopRequested', 'redis'))
    need(result['schemaVersion'] == 'SmallAppControlResult/v2' and result['version'] == VERSION
         and result['requestId'] == request['requestId']
         and result['operation'] == request['operation'], 'CONTROL_RESPONSE_BINDING_INVALID', 503)
    print(json.dumps(result))


if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        print(json.dumps({'state': 'FAILED', 'errorCode': exc.code if isinstance(exc, AppError) else 'CONTROL_TRANSPORT_FAILED',
                          'exceptionClass': type(exc).__name__}))
        raise SystemExit(1) from None
