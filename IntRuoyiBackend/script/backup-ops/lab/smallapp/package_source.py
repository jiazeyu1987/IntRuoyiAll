"""Deterministic public source archive. Explicit allowlist excludes private material."""
from pathlib import Path
import gzip
import hashlib
import io
import json
import sys
import tarfile
from core import AppError, VERSION, need

FILES = ('core.py', 'schema.sql', 'install.py', 'http_contract.py', 'server.py', 'frontend.py',
    'materialize.py', 'launch.py', 'control_client.py', 'capture_binding.py', 'verify_runtime.py',
    'critical_checks.py', 'package_source.py', 'requirements.txt', 'Dockerfile', '.dockerignore',
    'config-contract.json', 'ui-contract.json', 'README.md', 'static/index.html', 'static/app.js', 'static/style.css',
    'redis_cache.py', 'redis_checks.py', 'redis-policy.json', 'recovery-semantics.json')


def package(destination):
    root = Path(__file__).parent.resolve()
    path = Path(destination)
    need(path.is_absolute() and path.suffix == '.gz' and not path.exists(), 'NEW_ABSOLUTE_ARCHIVE_REQUIRED')
    source_pins = []
    payload = io.BytesIO()
    with tarfile.open(fileobj=payload, mode='w', format=tarfile.PAX_FORMAT) as archive:
        for name in sorted(FILES):
            source = root / name
            need(source.is_file() and not source.is_symlink(), 'SOURCE_FILE_REQUIRED')
            raw = source.read_bytes()
            source_pins.append({'path': str(source).replace('\\', '/'), 'relativePath': name,
                                'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()})
            info = tarfile.TarInfo('source/' + name)
            info.size = len(raw); info.mode = 0o644; info.mtime = 0; info.uid = info.gid = 0
            archive.addfile(info, io.BytesIO(raw))
    encoded = gzip.compress(payload.getvalue(), mtime=0)
    with path.open('xb') as output:
        output.write(encoded)
    raw = path.read_bytes()
    need(raw == encoded, 'SOURCE_ARCHIVE_READBACK_FAILED')
    with tarfile.open(fileobj=io.BytesIO(raw), mode='r:gz') as archive:
        members = archive.getmembers()
        need(len(members) == len(FILES) and all(item.isfile() for item in members), 'SOURCE_ARCHIVE_CONTENT_INVALID')
        for member, item in zip(members, source_pins):
            need(member.name == 'source/' + item['relativePath'], 'SOURCE_ARCHIVE_MEMBER_INVALID')
            with archive.extractfile(member) as stream:
                need(hashlib.sha256(stream.read()).hexdigest() == item['sha256'], 'SOURCE_ARCHIVE_MEMBER_HASH_INVALID')
    return {'schemaVersion': 'SmallAppSourceArchive/v1', 'version': VERSION,
            'artifactPin': {'path': str(path).replace('\\', '/'), 'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()},
            'sourcePins': source_pins, 'memberCount': len(FILES), 'privateMaterialIncluded': False}


if __name__ == '__main__':
    try:
        need(len(sys.argv) == 2, 'ARCHIVE_OUTPUT_ARGUMENT_REQUIRED')
        print(json.dumps(package(sys.argv[1]), ensure_ascii=False))
    except Exception as exc:
        print(json.dumps({'state': 'FAILED', 'errorCode': exc.code if isinstance(exc, AppError) else 'PACKAGE_FAILED',
                          'exceptionClass': type(exc).__name__}))
        raise SystemExit(1) from None
