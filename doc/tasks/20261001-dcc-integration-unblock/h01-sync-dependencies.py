"""Deliver reviewed B dependencies to A and reviewed A state audit to D, with hash protection."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
SOURCE = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
digest = lambda raw: hashlib.sha256(raw).hexdigest()
normalize = lambda raw: raw.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
known = {}
def register(rows):
    for row in rows:
        for key in ('sha256', 'before_sha256', 'source_sha256'):
            if row.get(key):
                known.setdefault(row['path'], set()).add(row[key])
for name in ('dependency-sync-manifest.json', 'ad-delivery-import-manifest.json',
             'd2-import-manifest.json', 'd2-dependency-sync.json', 'h01-import-manifest.json'):
    register(json.loads((TASK / name).read_text(encoding='utf-8'))['files'])
for worker in json.loads((TASK / 'ad-review-dependency-sync.json').read_text(encoding='utf-8'))['workers']:
    register(worker['files'])

d2 = json.loads((TASK / 'd2-import-manifest.json').read_text(encoding='utf-8'))
audit = json.loads((TASK / 'h01-import-manifest.json').read_text(encoding='utf-8'))
policy = 'IntRuoyiBackend/config/gxp-audit-policy.yaml'
paths = {
    'a': {row['path'] for row in d2['files'] if row['owner'] == 'B'} | {policy},
    'd': {row['path'] for row in audit['files']} | {policy},
}
plans = []
report = {'source': str(SOURCE), 'base': BASE, 'workers': [], 'B_C_sources_written': False}
for module, selected in paths.items():
    worker = Path('C:/IntRuoyi/20260930-dcc-' + module)
    assert subprocess.check_output(['git', '-C', str(worker), 'rev-parse', 'HEAD'], text=True).strip() == BASE
    rows = []
    for rel in sorted(selected):
        destination = worker / rel
        assert destination.resolve().is_relative_to(worker.resolve())
        raw = (SOURCE / rel).read_bytes()
        before = destination.read_bytes() if destination.exists() else None
        if before is not None and normalize(before) == normalize(raw):
            continue
        before_hash = digest(before) if before is not None else None
        if before is not None and before_hash not in known.get(rel, set()):
            baseline = subprocess.run(['git', '-C', str(worker), 'show', BASE + ':' + rel], capture_output=True)
            assert baseline.returncode == 0 and normalize(before) == normalize(baseline.stdout), f'Owner changed: {module}/{rel}'
        rows.append({'path': rel, 'before_sha256': before_hash, 'sha256': digest(raw)})
        plans.append((destination, raw, before_hash, rel))
    report['workers'].append({'module': module, 'path': str(worker), 'files': rows})

for destination, raw, before_hash, rel in plans:
    assert digest((SOURCE / rel).read_bytes()) == digest(raw)
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
for destination, raw, before_hash, rel in plans:
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'h01-dependency-sync.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'files': {w['module']: len(w['files']) for w in report['workers']},
                  'B_C_sources_written': False, 'A_frozen_own_code_unchanged': True}))
