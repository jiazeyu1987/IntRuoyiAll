"""Freeze and receive A/D increments without overwriting B or concurrent edits."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
TARGET = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
digest = lambda raw: hashlib.sha256(raw).hexdigest()
normalize = lambda raw: raw.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
previous = {row['path']: row['sha256'] for row in json.loads(
    (TASK / 'dependency-sync-manifest.json').read_text(encoding='utf-8'))['files']}
root_fixes = {row['path']: row['sha256'] for row in json.loads(
    (TASK / 'b-review-dependency-sync.json').read_text(encoding='utf-8'))['files']}
payload = {}
for module in ('a', 'd'):
    worker = Path('C:/IntRuoyi/20260930-dcc-' + module)
    assert subprocess.check_output(['git', '-C', str(worker), 'rev-parse', 'HEAD'], text=True).strip() == BASE
    entries = subprocess.check_output(['git', '-C', str(worker), 'status', '--porcelain=v1', '-z',
        '--untracked-files=all', '--', 'IntRuoyiBackend', 'IntRuoyiFronted']).decode().split('\0')
    for entry in entries:
        if not entry:
            continue
        assert entry[:2].strip() not in ('D', 'R'), f'Review deletion/rename separately: {entry}'
        rel = entry[3:]
        source, destination = worker / rel, TARGET / rel
        assert source.resolve().is_relative_to(worker.resolve())
        assert destination.resolve().is_relative_to(TARGET.resolve())
        raw = source.read_bytes()
        source_hash = digest(raw)
        if previous.get(rel) == source_hash:
            continue
        before = destination.read_bytes() if destination.is_file() else None
        if before is not None and normalize(before) == normalize(raw):
            continue
        before_hash = digest(before) if before is not None else None
        if rel in previous:
            assert before_hash in {previous[rel], root_fixes.get(rel)}, f'Root/B edit conflict: {rel}'
        elif before is not None:
            baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
            assert baseline.returncode == 0 and normalize(baseline.stdout) == normalize(before), f'Existing target conflict: {rel}'
        assert rel not in payload, f'A/D overlapping increment requires manual merge: {rel}'
        payload[rel] = (module, source, raw, before_hash)

assert subprocess.check_output(['git', '-C', str(TARGET), 'rev-parse', 'HEAD'], text=True).strip() == BASE
for rel, (module, source, raw, before_hash) in payload.items():
    assert digest(source.read_bytes()) == digest(raw), f'Owner changed during freeze: {rel}'
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash

report = {'base': BASE, 'target': str(TARGET), 'files': [
    {'path': rel, 'owner': module, 'source': str(source), 'sha256': digest(raw), 'before_sha256': before_hash}
    for rel, (module, source, raw, before_hash) in payload.items()], 'worker_files_written': False}
for rel, (module, source, raw, before_hash) in payload.items():
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'ad-delivery-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'received_delta': len(payload), 'workers_written': False,
    'per_owner': {m: sum(v[0] == m for v in payload.values()) for m in ('a', 'd')}}))
