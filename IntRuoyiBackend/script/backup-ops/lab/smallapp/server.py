"""Single backend process; real DB/object operations and private Unix control."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import json
import os
import re
import signal
import socketserver
import stat
import threading
from urllib.parse import quote, unquote_to_bytes
from core import AppError, Store, VERSION, MAX_FILE, exact, identifier, load_fd, need, strict_json
from http_contract import MAX_JSON, allowed_method, cookie, cookies, csrf, framing, header, require_origin, route


def event(code, exc=None):
    value = {'schemaVersion': 'SmallAppRuntimeEvent/v1', 'version': VERSION, 'code': code}
    if exc is not None:
        value['exceptionClass'] = type(exc).__name__
    print(json.dumps(value), flush=True)


class Backend(ThreadingHTTPServer):
    daemon_threads = False
    block_on_close = True
    request_queue_size = 16

    def __init__(self, store):
        self.store = store
        self.slots = threading.BoundedSemaphore(12)
        self.stop_lock = threading.Lock()
        self.stopping = False
        http = store.settings['http']
        super().__init__((http['listenHost'], http['listenPort']), Handler)

    def process_request(self, request, address):
        self.slots.acquire()
        try:
            super().process_request(request, address)
        except BaseException:
            self.slots.release()
            raise

    def process_request_thread(self, request, address):
        try:
            super().process_request_thread(request, address)
        finally:
            self.slots.release()

    def handle_error(self, request, address):
        event('HTTP_HANDLER_FAILED')  # Base implementation prints request/traceback; never do that.

    def request_stop(self):
        with self.stop_lock:
            if self.stopping:
                return
            self.stopping = True
        def stop():
            with self.store.gate.lock:
                self.store.gate.state = 'PROTECTED'
            self.shutdown()
        threading.Thread(target=stop, name='smallapp-stop', daemon=False).start()


class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.0'
    server_version = VERSION
    sys_version = ''

    def setup(self):
        super().setup()
        self.connection.settimeout(15)

    def log_message(self, format, *args):
        return  # Never log raw URLs, headers, bodies, cookies or request lines.

    def handle_expect_100(self):
        self.send_error(417)
        return False

    def send_error(self, code, message=None, explain=None):
        self.json_response(code, {'errorCode': 'HTTP_REQUEST_REJECTED'})

    def response(self, status, raw, content_type, extra=()):
        self.send_response(status)
        for key, value in (('Content-Type', content_type), ('Content-Length', str(len(raw))),
            ('Cache-Control', 'no-store'), ('Connection', 'close'), ('X-Content-Type-Options', 'nosniff'),
            ('Referrer-Policy', 'no-referrer'), ('X-Frame-Options', 'DENY'),
            ('Content-Security-Policy', "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'"), *extra):
            self.send_header(key, value)
        self.end_headers()
        self.close_connection = True
        self.wfile.write(raw)

    def json_response(self, status, value, extra=()):
        self.response(status, json.dumps(value, ensure_ascii=False, separators=(',', ':')).encode('utf-8'),
                      'application/json; charset=utf-8', extra)

    def read_body(self, maximum):
        size = framing(self.headers, self.command, maximum)
        raw = self.rfile.read(size)
        need(len(raw) == size, 'REQUEST_BODY_INCOMPLETE')
        return raw

    def json_body(self):
        need(header(self.headers, 'Content-Type', required=True) == 'application/json', 'JSON_CONTENT_TYPE_REQUIRED', 415)
        return strict_json(self.read_body(MAX_JSON))

    def run_request(self):
        store = self.server.store
        kind, record_id = route(self.path)
        allowed_method(kind, self.command)
        mutation = self.command != 'GET'
        require_origin(self.headers, store.settings['http']['origin'], mutation=mutation)
        if not mutation:
            framing(self.headers, self.command, 0)
        secure = store.settings['http']['cookieSecure']
        session_cookie = 'smallapp_session_' + store.settings['runtimeRole'].lower()
        login_cookie = 'smallapp_login_' + store.settings['runtimeRole'].lower()
        if kind in ('/', '/app.js', '/style.css'):
            name, mime = {'/': ('index.html', 'text/html'), '/app.js': ('app.js', 'text/javascript'),
                          '/style.css': ('style.css', 'text/css')}[kind]
            self.response(200, (Path(__file__).parent / 'static' / name).read_bytes(), mime + '; charset=utf-8')
            return
        if kind == '/health':
            with store.gate.lock:
                store.require_clean()
                store.s3.head_bucket(Bucket=store.bucket)
                redis_health = store.cache.health()
                self.json_response(200, {'version': VERSION, 'state': store.gate.state,
                    'components': {'mysql': 'UP', 'minio': 'UP', 'redis': redis_health}})
            return
        jar = cookies(self.headers)
        if kind == '/api/login-token':
            token = store.login_token()
            self.json_response(200, {'csrf': token}, [('Set-Cookie', cookie(login_cookie, token, secure, age=600))])
            return
        if kind == '/api/login':
            payload = exact(self.json_body(), ('tenantId', 'username', 'password', 'csrf'))
            need(type(payload['tenantId']) is int and payload['tenantId'] == store.tenant, 'LOGIN_DENIED', 401)
            store.require_login_token(payload['csrf'], jar.get(login_cookie))
            sid, token = store.login(payload['username'], payload['password'])
            self.json_response(200, {'username': 'aoteman', 'tenantId': 122, 'csrf': token},
                [('Set-Cookie', cookie(session_cookie, sid, secure, age=7200)),
                 ('Set-Cookie', cookie(login_cookie, '', secure, age=0))])
            return
        sid = jar.get(session_cookie)
        user = store.session(sid)
        if mutation:
            csrf(header(self.headers, 'X-CSRF-Token', required=True), user['csrf'])
        if kind == '/api/session':
            self.json_response(200, {'username': user['username'], 'tenantId': 122,
                                     'csrf': user['csrf'], 'state': store.gate.state, 'version': VERSION,
                                     'sessionCache': 'REDIS_READ_BACK_WITH_SQL_AUTHORITY'})
        elif kind == '/api/logout':
            exact(self.json_body(), ())
            store.logout(sid)
            self.json_response(200, {'state': 'LOGGED_OUT'}, [('Set-Cookie', cookie(session_cookie, '', secure, age=0))])
        elif kind == '/api/records' and self.command == 'GET':
            with store.gate.lock:
                store.require_clean()
                records = store.records()
                attachments = [{'recordKey': row['record_key'], 'filename': row['filename'],
                    'reference': row['bucket'] + '/' + row['object_key'], 'bytes': row['size_bytes'], 'sha256': row['sha256']}
                    for row in records if row['filename'] is not None]
                self.json_response(200, {'records': records, 'attachments': attachments, 'state': store.gate.state})
        elif kind == '/api/records':
            record_id = store.create_record(user, self.json_body())
            self.json_response(201, {'id': record_id})
        elif kind == 'record' and self.command == 'PUT':
            store.edit_record(record_id, self.json_body())
            self.json_response(200, {'id': record_id})
        elif kind in ('record', 'attachment') and self.command == 'DELETE':
            payload = exact(self.json_body(), ('expectedVersion',))
            store.delete(record_id, identifier(payload['expectedVersion']), attachment_only=kind == 'attachment')
            self.json_response(200, {'id': record_id})
        elif kind == 'attachment' and self.command == 'GET':
            name, raw = store.attachment(record_id)
            self.response(200, raw, 'application/octet-stream',
                          [('Content-Disposition', "attachment; filename*=UTF-8''" + quote(name, safe=''))])
        elif kind == 'attachment' and self.command == 'PUT':
            need(header(self.headers, 'Content-Type', required=True) == 'application/octet-stream', 'BINARY_CONTENT_TYPE_REQUIRED', 415)
            name = header(self.headers, 'X-File-Name', required=True)
            need(len(name) <= 2400 and re.fullmatch(r'(?:[A-Za-z0-9_.~-]|%[0-9A-Fa-f]{2})+', name), 'ENCODED_FILENAME_REQUIRED')
            try:
                name = unquote_to_bytes(name).decode('utf-8')
            except UnicodeError as exc:
                raise AppError('INVALID_FILENAME') from exc
            version = header(self.headers, 'X-Expected-Version', required=True)
            need(re.fullmatch('[1-9][0-9]{0,18}', version) is not None, 'POSITIVE_ID_REQUIRED')
            store.upload(record_id, identifier(int(version)), name, self.read_body(MAX_FILE))
            self.json_response(200, {'id': record_id})

    def dispatch(self):
        try:
            self.run_request()
        except AppError as exc:
            if exc.status >= 500 and self.server.store.gate.state == 'OPEN':
                with self.server.store.gate.lock:
                    self.server.store.gate.state = 'QUIESCED'
            event(exc.code)
            self.json_response(exc.status, {'errorCode': exc.code})
        except (BrokenPipeError, ConnectionResetError, TimeoutError) as exc:
            event('HTTP_CONNECTION_ENDED', exc)
        except Exception as exc:
            with self.server.store.gate.lock:
                if self.server.store.gate.state == 'OPEN':
                    self.server.store.gate.state = 'QUIESCED'
            event('DEPENDENCY_FAILED', exc)
            self.json_response(503, {'errorCode': 'DEPENDENCY_FAILED'})

    do_GET = do_POST = do_PUT = do_DELETE = dispatch


class Control(socketserver.ThreadingUnixStreamServer):
    daemon_threads = False
    block_on_close = True

    def __init__(self, backend):
        import fcntl
        self.backend = backend
        path = backend.store.settings['controlSocket']
        parent = Path(path).parent
        info = parent.lstat()
        need(stat.S_ISDIR(info.st_mode) and not stat.S_ISLNK(info.st_mode)
             and info.st_uid == os.getuid() and stat.S_IMODE(info.st_mode) == 0o700,
             'PRIVATE_CONTROL_DIRECTORY_REQUIRED', 503)
        self.lock_fd = os.open(path + '.lock', os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        lock_info = os.fstat(self.lock_fd)
        need(stat.S_ISREG(lock_info.st_mode) and lock_info.st_uid == os.getuid()
             and stat.S_IMODE(lock_info.st_mode) == 0o600, 'PRIVATE_CONTROL_LOCK_REQUIRED', 503)
        fcntl.flock(self.lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        need(not os.path.lexists(path), 'CONTROL_SOCKET_ALREADY_EXISTS', 503)
        super().__init__(path, ControlHandler)
        os.chmod(path, 0o600)
        self.bound_path = path

    def handle_error(self, request, address):
        event('CONTROL_HANDLER_FAILED')

    def server_close(self):
        super().server_close()
        # Only the socket bound by this process is removed, after all handlers drain.
        os.unlink(self.bound_path)
        os.close(self.lock_fd)


class ControlHandler(socketserver.StreamRequestHandler):
    def handle(self):
        self.request.settimeout(15)
        try:
            raw = self.rfile.readline(8193)
            need(0 < len(raw) <= 8192 and raw.endswith(b'\n'), 'CONTROL_FRAME_INVALID')
            result = self.server.backend.store.control(strict_json(raw))
            self.wfile.write(json.dumps(result).encode() + b'\n')
            self.wfile.flush()
            if result['stopRequested']:
                self.server.backend.request_stop()
        except Exception as exc:
            code = exc.code if isinstance(exc, AppError) else 'CONTROL_DEPENDENCY_FAILED'
            event(code, exc)
            self.wfile.write(json.dumps({'schemaVersion': 'SmallAppControlError/v1', 'state': 'FAILED', 'errorCode': code}).encode() + b'\n')


def main():
    need(os.name == 'posix', 'LINUX_RUNTIME_REQUIRED', 503)
    envelope = exact(load_fd(), ('config', 'secrets'))
    store = Store(envelope['config'], envelope['secrets'])
    store.environment()
    store.require_clean()
    backend = Backend(store)
    control = Control(backend)
    worker = threading.Thread(target=control.serve_forever, name='smallapp-control', daemon=False)
    worker.start()
    for number in (signal.SIGTERM, signal.SIGINT):
        signal.signal(number, lambda *_: backend.request_stop())
    event('STARTED')
    try:
        backend.serve_forever()
    finally:
        backend.server_close()
        control.shutdown()
        worker.join()
        control.server_close()
        event('STOPPED')


if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        event(exc.code if isinstance(exc, AppError) else 'STARTUP_DEPENDENCY_FAILED', exc)
        raise SystemExit(1) from None
