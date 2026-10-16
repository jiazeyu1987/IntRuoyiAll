"""Strict HTTP framing shared by the actual backend and frontend roles."""
from http.cookies import SimpleCookie, CookieError
import re
from urllib.parse import urlsplit
from core import need

MAX_JSON = 32768


def header(headers, name, *, required=False):
    values = headers.get_all(name, [])
    need(len(values) <= 1 and (not required or len(values) == 1), 'HEADER_CARDINALITY_INVALID')
    return values[0] if values else None


def framing(headers, method, maximum):
    need(header(headers, 'Transfer-Encoding') is None and header(headers, 'Expect') is None,
         'HTTP_FRAMING_NOT_SUPPORTED')
    value = header(headers, 'Content-Length')
    if method == 'GET':
        need(value is None or value == '0', 'GET_BODY_NOT_ALLOWED')
        return 0
    need(value is not None and re.fullmatch(r'0|[1-9][0-9]{0,6}', value), 'CONTENT_LENGTH_REQUIRED', 411)
    size = int(value)
    need(0 < size <= maximum, 'REQUEST_TOO_LARGE', 413)
    return size


def require_origin(headers, origin, *, mutation):
    need(header(headers, 'Host', required=True).lower() == urlsplit(origin).netloc.lower(), 'HOST_DENIED', 403)
    if mutation:
        need(header(headers, 'Origin', required=True) == origin, 'ORIGIN_DENIED', 403)
    site = header(headers, 'Sec-Fetch-Site')
    need(site is None or site in ('same-origin', 'none'), 'CROSS_SITE_DENIED', 403)


def cookies(headers):
    raw = header(headers, 'Cookie')
    if raw is None:
        return {}
    need(len(raw) <= 2048, 'COOKIE_SIZE_INVALID')
    names = [item.split('=', 1)[0].strip() for item in raw.split(';')]
    need(len(names) == len(set(names)), 'DUPLICATE_COOKIE')
    parsed = SimpleCookie()
    try:
        parsed.load(raw)
    except CookieError:
        need(False, 'COOKIE_INVALID')
    return {name: item.value for name, item in parsed.items()}


def cookie(name, value, secure, *, age):
    need(name in ('smallapp_session_source', 'smallapp_login_source', 'smallapp_session_target', 'smallapp_login_target')
         and re.fullmatch(r'[A-Za-z0-9.]*', value), 'COOKIE_VALUE_INVALID')
    return f'{name}={value}; Path=/; HttpOnly; SameSite=Strict; Max-Age={age}' + ('; Secure' if secure else '')


def csrf(supplied, expected):
    import hmac
    need(type(supplied) is str and re.fullmatch('[0-9a-f]{64}', supplied)
         and hmac.compare_digest(supplied, expected), 'CSRF_DENIED', 403)


def route(path):
    need(type(path) is str and '?' not in path and '#' not in path and '%' not in path, 'PATH_INVALID')
    if path in ('/', '/app.js', '/style.css', '/health', '/api/login-token', '/api/login',
                '/api/session', '/api/logout', '/api/records'):
        return path, None
    match = re.fullmatch(r'/api/records/([1-9][0-9]{0,18})(/attachment)?', path)
    need(match is not None, 'ROUTE_NOT_FOUND', 404)
    record_id = int(match[1])
    need(record_id < 2**63, 'POSITIVE_ID_REQUIRED')
    return 'attachment' if match[2] else 'record', record_id


def allowed_method(kind, method):
    methods = {'/': ('GET',), '/app.js': ('GET',), '/style.css': ('GET',), '/health': ('GET',),
               '/api/login-token': ('GET',), '/api/login': ('POST',), '/api/session': ('GET',),
               '/api/logout': ('POST',), '/api/records': ('GET', 'POST'),
               'record': ('PUT', 'DELETE'), 'attachment': ('GET', 'PUT', 'DELETE')}
    need(method in methods[kind], 'METHOD_NOT_ALLOWED', 405)
