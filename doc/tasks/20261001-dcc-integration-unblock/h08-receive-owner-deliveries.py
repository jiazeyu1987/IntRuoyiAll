"""Receive only frozen B/D increments; refuse changed source or target assets."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
TARGET = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
sha = lambda raw: hashlib.sha256(raw).hexdigest()
lf = lambda raw: raw.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
known = {}

def register(value):
    if isinstance(value, dict):
        if 'path' in value:
            for key in ('sha256', 'before_sha256', 'source_sha256', 'previousAcceptedSha256'):
                if value.get(key):
                    known.setdefault(value['path'], set()).add(value[key])
        for child in value.values():
            register(child)
    elif isinstance(value, list):
        for child in value:
            register(child)

for path in TASK.glob('*manifest.json'):
    register(json.loads(path.read_text(encoding='utf-8-sig')))
for path in TASK.glob('*sync.json'):
    register(json.loads(path.read_text(encoding='utf-8-sig')))

payload = []
receipts = []
for owner, task_suffix, freeze_name in (
    ('b', 'project', 'h06-review-manifest.json'),
    ('d', 'relations', 'h04-delivery-fingerprints.json'),
):
    source = Path('C:/IntRuoyi/20260930-dcc-' + owner)
    assert subprocess.check_output(['git', '-C', str(source), 'branch', '--show-current'], text=True).strip() == 'codex/20260930-dcc-' + owner
    assert subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip() == BASE
    freeze_path = source / f'doc/tasks/20260930-dcc-{owner}-{task_suffix}' / freeze_name
    freeze_raw = freeze_path.read_bytes()
    freeze = json.loads(freeze_raw.decode('utf-8-sig'))
    rows = freeze['fullSources'] if owner == 'b' else freeze['files']
    for row in rows:
        assert sha((source / row['path']).read_bytes()) == row['sha256'], f'Owner source changed: {owner}/{row["path"]}'
    changes = freeze['delta'] if owner == 'b' else [r for r in rows if r.get('role') != 'synchronized_read_only_dependency']
    register(changes)
    for row in changes:
        rel = row['path']
        raw = (source / rel).read_bytes()
        destination = TARGET / rel
        assert destination.resolve().is_relative_to(TARGET.resolve())
        before = destination.read_bytes() if destination.exists() else None
        if before is not None and lf(before) == lf(raw):
            continue
        before_hash = sha(before) if before is not None else None
        if before is not None and before_hash not in known.get(rel, set()):
            baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
            assert baseline.returncode == 0 and lf(before) == lf(baseline.stdout), f'Unreviewed Root change: {rel}'
        payload.append((source / rel, destination, raw, before_hash, owner, rel))
    for rel in freeze.get('deletedPaths', []):
        assert not (TARGET / rel).exists(), f'Obsolete mapping requires separately reviewed removal: {rel}'
    receipts.append((freeze_path, freeze_raw, f'h08-{owner}-freeze-receipt.json'))

assert subprocess.check_output(['git', '-C', str(TARGET), 'branch', '--show-current'], text=True).strip() == 'codex/20261001-dcc-integration'
assert subprocess.check_output(['git', '-C', str(TARGET), 'rev-parse', 'HEAD'], text=True).strip() == BASE
for source_path, destination, raw, before_hash, owner, rel in payload:
    assert sha(source_path.read_bytes()) == sha(raw)
    assert (sha(destination.read_bytes()) if destination.exists() else None) == before_hash
for freeze_path, freeze_raw, receipt_name in receipts:
    assert freeze_path.read_bytes() == freeze_raw
for source_path, destination, raw, before_hash, owner, rel in payload:
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
for freeze_path, freeze_raw, receipt_name in receipts:
    (TASK / receipt_name).write_bytes(freeze_raw)
report = {'base': BASE, 'target': str(TARGET), 'owner_sources_written': False,
    'files': [{'owner': owner, 'path': rel, 'before_sha256': before_hash, 'sha256': sha(raw)}
        for source_path, destination, raw, before_hash, owner, rel in payload]}
(TASK / 'h08-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'received': len(payload), 'B': sum(p[4] == 'b' for p in payload),
    'D': sum(p[4] == 'd' for p in payload), 'worker_sources_written': False}))
