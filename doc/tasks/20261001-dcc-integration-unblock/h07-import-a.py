"""Receive A's implemented-path increment while preserving its explicit full-gate failure."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
SOURCE = Path('C:/IntRuoyi/20260930-dcc-a')
TARGET = Path('C:/IntRuoyi/20261001-dcc-integration')
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
for name in ('dependency-sync-manifest.json', 'ad-delivery-import-manifest.json', 'h01-import-manifest.json', 'h02-import-manifest.json'):
    register(json.loads((TASK / name).read_text(encoding='utf-8'))['files'])
for name in ('ad-review-dependency-sync.json', 'h01-dependency-sync.json', 'h02-dependency-sync.json'):
    for worker in json.loads((TASK / name).read_text(encoding='utf-8'))['workers']:
        register(worker['files'])
freeze_path = SOURCE / 'doc/tasks/20260930-dcc-a-workflow/h02-review-freeze-manifest.json'
freeze_raw = freeze_path.read_bytes()
freeze = json.loads(freeze_raw.decode('utf-8'))
assert freeze['fullDirectedRegression']['failures'] == 1
assert freeze['fullDirectedRegression']['failingCase'].endswith('h02UnsubmittedSourceMustKeepItsSavedSourceWhenAnotherRealFileIsAllocated')
for repo in (SOURCE, TARGET):
    assert subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip() == BASE
payload = []
for row in freeze['files']:
    raw = (SOURCE / row['path']).read_bytes()
    assert digest(raw) == row['rawSha256'], f'A source changed after freeze: {row["path"]}'
    if not row['path'].startswith('IntRuoyiBackend/'):
        continue
    rel = row['path']
    destination = TARGET / rel
    assert destination.resolve().is_relative_to(TARGET.resolve())
    before = destination.read_bytes() if destination.exists() else None
    before_hash = digest(before) if before is not None else None
    if before is not None and before_hash not in known.get(rel, set()):
        baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
        assert baseline.returncode == 0 and normalize(before) == normalize(baseline.stdout), f'Unreviewed Root edit: {rel}'
    payload.append((rel, raw, before_hash))
assert freeze_path.read_bytes() == freeze_raw
for rel, raw, before_hash in payload:
    assert digest((SOURCE / rel).read_bytes()) == digest(raw)
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
report = {'source': str(SOURCE), 'target': str(TARGET), 'base': BASE,
    'full_gate_open_failure': freeze['fullDirectedRegression']['failingCase'], 'overall_review': 'changes_requested',
    'files': [{'path': rel, 'before_sha256': before_hash, 'sha256': digest(raw)} for rel, raw, before_hash in payload],
    'worker_source_written': False}
for rel, raw, before_hash in payload:
    destination = TARGET / rel
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'h07-a-freeze-receipt.json').write_bytes(freeze_raw)
(TASK / 'h07-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'received_source_files': len(payload), 'full_gate_failure_retained': True, 'workers_written': False}))
