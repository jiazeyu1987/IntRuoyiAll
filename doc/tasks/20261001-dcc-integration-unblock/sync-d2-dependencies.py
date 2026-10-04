"""Deliver only verified B dependencies/policy to D, preserving its own latest tests."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
SOURCE = Path('C:/IntRuoyi/20261001-dcc-integration')
TARGET = Path('C:/IntRuoyi/20260930-dcc-d')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
digest = lambda raw: hashlib.sha256(raw).hexdigest()
normalize = lambda raw: raw.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
known = {}
def register(rows):
    for row in rows:
        for key in ('sha256', 'before_sha256'):
            if row.get(key):
                known.setdefault(row['path'], set()).add(row[key])
for name in ('dependency-sync-manifest.json', 'b-delivery-import-manifest.json', 'ad-delivery-import-manifest.json',
             'b-review-dependency-sync.json', 'd2-import-manifest.json'):
    register(json.loads((TASK / name).read_text(encoding='utf-8'))['files'])
sync = json.loads((TASK / 'ad-review-dependency-sync.json').read_text(encoding='utf-8'))
register(next(worker for worker in sync['workers'] if worker['module'] == 'd')['files'])
imported = json.loads((TASK / 'd2-import-manifest.json').read_text(encoding='utf-8'))
paths = [row['path'] for row in imported['files'] if row['owner'] == 'B'] + ['IntRuoyiBackend/config/gxp-audit-policy.yaml']
assert subprocess.check_output(['git', '-C', str(TARGET), 'rev-parse', 'HEAD'], text=True).strip() == BASE
payload = []
for rel in paths:
    raw = (SOURCE / rel).read_bytes()
    destination = TARGET / rel
    assert destination.resolve().is_relative_to(TARGET.resolve())
    before = destination.read_bytes() if destination.is_file() else None
    if before is not None and normalize(before) == normalize(raw):
        continue
    before_hash = digest(before) if before is not None else None
    if before is not None and before_hash not in known.get(rel, set()):
        baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
        assert baseline.returncode == 0 and normalize(before) == normalize(baseline.stdout), f'D changed dependency after frozen synchronization: {rel}'
    payload.append((rel, raw, before_hash))
for rel, raw, before_hash in payload:
    assert digest((SOURCE / rel).read_bytes()) == digest(raw)
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
report = {'source': str(SOURCE), 'target': str(TARGET), 'files': [
    {'path': rel, 'sha256': digest(raw), 'before_sha256': before_hash} for rel, raw, before_hash in payload],
    'D_owner_source_written': False, 'A_B_C_workers_written': False}
for rel, raw, before_hash in payload:
    destination = TARGET / rel
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'd2-dependency-sync.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'worker': 'D', 'files': len(payload),
                  'D_own_tests_preserved': True, 'other_workers_written': False}))
