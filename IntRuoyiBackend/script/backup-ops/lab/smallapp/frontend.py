"""Actual frontend role: bounded, no-retry HTTP reverse proxy to the backend."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import http.client
import json
import re
import signal
import ssl
import threading
from urllib.parse import urlsplit
from core import AppError, MAX_FILE, VERSION, exact, load_fd, need
from http_contract import MAX_JSON, allowed_method, framing, header, require_origin, route

MAX_RESPONSE = 24 * 1024 * 1024
FORWARDED = ('Host', 'Origin', 'Cookie', 'Content-Type', 'X-CSRF-Token',
             'X-File-Name', 'X-Expected-Version', 'Sec-Fetch-Site')
RETURNED = {'content-type', 'content-disposition', 'set-cookie', 'content-security-policy',
            'x-frame-options', 'x-content-type-options', 'referrer-policy'}


def frontend_config(value):
    exact(value, ('schemaVersion', 'version', 'backendOrigin', 'backendTlsCaFile', 'http'))
    need(value['schemaVersion'] == 'SmallAppFrontendConfig/v1' and value['version'] == VERSION,
         'FRONTEND_VERSION_MISMATCH', 503)
    upstream = urlsplit(value['backendOrigin'])
    need(upstream.scheme in ('http', 'https') and upstream.hostname and not upstream.username
         and not upstream.password and upstream.path == '' and not upstream.query and not upstream.fragment,
         'BACKEND_ORIGIN_INVALID', 503)
    need(upstream.hostname == '127.0.0.1' or re.fullmatch(r'brlab-20261012-[a-z0-9-]+', upstream.hostname),
         'TASK_BACKEND_REQUIRED', 503)
    need(value['backendTlsCaFile'] is None or (type(value['backendTlsCaFile']) is str
         and value['backendTlsCaFile'].startswith('/')), 'TLS_CONFIGURATION_INVALID', 503)
    listener = exact(value['http'], ('listenHost', 'listenPort', 'origin'))
    need(listener['listenHost'] in ('127.0.0.1', '0.0.0.0') and type(listener['listenPort']) is int
         and 1024 <= listener['listenPort'] <= 65535, 'HTTP_LISTENER_INVALID', 503)
    origin = urlsplit(listener['origin'])
    need(origin.scheme in ('http', 'https') and origin.hostname and not origin.username and not origin.password
         and origin.path == '' and not origin.query and not origin.fragment, 'HTTP_ORIGIN_INVALID', 503)
    return value


class Frontend(ThreadingHTTPServer):
    daemon_threads = False
    block_on_close = True
    request_queue_size = 16

    def __init__(self, settings):
        self.settings = frontend_config(settings)
        self.slots = threading.BoundedSemaphore(12)
        http = settings['http']
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
        print('{"code":"FRONTEND_HANDLER_FAILED"}', flush=True)


class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.0'
    server_version = VERSION
    sys_version = ''

    def setup(self):
        super().setup()
        self.connection.settimeout(20)

    def log_message(self, format, *args):
        return

    def send_error(self, code, message=None, explain=None):
        self.error(code, 'HTTP_REQUEST_REJECTED')

    def error(self, status, code):
        raw = json.dumps({'errorCode': code}).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(raw)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('Connection', 'close')
        self.end_headers()
        self.close_connection = True
        self.wfile.write(raw)

    def proxy(self):
        kind, _ = route(self.path)
        allowed_method(kind, self.command)
        settings = self.server.settings
        require_origin(self.headers, settings['http']['origin'], mutation=self.command != 'GET')
        maximum = MAX_FILE if kind == 'attachment' and self.command == 'PUT' else MAX_JSON
        size = framing(self.headers, self.command, maximum)
        raw = self.rfile.read(size)
        need(len(raw) == size, 'REQUEST_BODY_INCOMPLETE')
        headers = {name: header(self.headers, name) for name in FORWARDED if header(self.headers, name) is not None}
        headers['Content-Length'] = str(size)
        headers['Connection'] = 'close'
        upstream = urlsplit(settings['backendOrigin'])
        if upstream.scheme == 'https':
            context = ssl.create_default_context(cafile=settings['backendTlsCaFile'])
            connection = http.client.HTTPSConnection(upstream.hostname, upstream.port, timeout=20, context=context)
        else:
            connection = http.client.HTTPConnection(upstream.hostname, upstream.port, timeout=20)
        try:
            connection.request(self.command, self.path, body=raw, headers=headers)
            result = connection.getresponse()
            declared = result.getheaders()
            lengths = [value for name, value in declared if name.lower() == 'content-length']
            need(len(lengths) == 1 and re.fullmatch('[0-9]{1,8}', lengths[0])
                 and int(lengths[0]) <= MAX_RESPONSE, 'BACKEND_RESPONSE_LENGTH_INVALID', 502)
            body = result.read(MAX_RESPONSE + 1)
            need(len(body) == int(lengths[0]), 'BACKEND_RESPONSE_INCOMPLETE', 502)
            self.send_response(result.status)
            for name, value in declared:
                if name.lower() in RETURNED:
                    self.send_header(name, value)
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Connection', 'close')
            self.end_headers()
            self.close_connection = True
            self.wfile.write(body)
        finally:
            connection.close()

    def dispatch(self):
        try:
            self.proxy()
        except AppError as exc:
            self.error(exc.status, exc.code)
        except (BrokenPipeError, ConnectionResetError, TimeoutError) as exc:
            print(json.dumps({'code': 'FRONTEND_CONNECTION_ENDED', 'exceptionClass': type(exc).__name__}), flush=True)
        except Exception as exc:
            print(json.dumps({'code': 'BACKEND_UNAVAILABLE', 'exceptionClass': type(exc).__name__}), flush=True)
            self.error(502, 'BACKEND_UNAVAILABLE')

    do_GET = do_POST = do_PUT = do_DELETE = dispatch


def main():
    envelope = exact(load_fd(), ('config',))
    server = Frontend(envelope['config'])
    for number in (signal.SIGTERM, signal.SIGINT):
        signal.signal(number, lambda *_: threading.Thread(target=server.shutdown, daemon=False).start())
    print(json.dumps({'code': 'FRONTEND_STARTED', 'version': VERSION}), flush=True)
    try:
        server.serve_forever()
    finally:
        server.server_close()
        print('{"code":"FRONTEND_STOPPED"}', flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        print(json.dumps({'state': 'FAILED', 'errorCode': exc.code if isinstance(exc, AppError) else 'FRONTEND_STARTUP_FAILED',
                          'exceptionClass': type(exc).__name__}), flush=True)
        raise SystemExit(1) from None
