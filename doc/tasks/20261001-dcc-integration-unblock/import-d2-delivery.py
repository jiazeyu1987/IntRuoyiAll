"""Receive frozen B dependencies and D's latest two test files, preserving Root edits."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
TARGET = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
B = Path('C:/IntRuoyi/20260930-dcc-b')
D = Path('C:/IntRuoyi/20260930-dcc-d')
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
digest = lambda raw: hashlib.sha256(raw).hexdigest()
normalize = lambda raw: raw.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
known = {}
def register(rows):
    for row in rows:
        for key in ('sha256', 'before_sha256'):
            if row.get(key):
                known.setdefault(row['path'], set()).add(row[key])
for name in ('dependency-sync-manifest.json', 'b-delivery-import-manifest.json',
             'ad-delivery-import-manifest.json', 'b-review-dependency-sync.json'):
    register(json.loads((TASK / name).read_text(encoding='utf-8'))['files'])
sync = json.loads((TASK / 'ad-review-dependency-sync.json').read_text(encoding='utf-8'))
for worker in sync['workers']:
    register(worker['files'])

b_rows = json.loads((B / 'doc/tasks/20260930-dcc-b-project/changed-files.json').read_text(encoding='utf-8'))
sources = [(B / row['path'], row['path'], row['sha256'], 'B') for row in b_rows]
for name in ('DccSignedRelationAssignmentContractTest.java', 'DccRelationControlledEventIntegrationTest.java'):
    rel = 'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/' + name
    raw = (D / rel).read_bytes()
    sources.append((D / rel, rel, digest(raw), 'D'))
for repo in (B, D, TARGET):
    assert subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip() == BASE

payload = []
for source, rel, expected, owner in sources:
    raw = source.read_bytes()
    assert digest(raw) == expected, f'{owner} handoff changed: {rel}'
    destination = TARGET / rel
    assert destination.resolve().is_relative_to(TARGET.resolve())
    before = destination.read_bytes() if destination.is_file() else None
    if before is not None and normalize(before) == normalize(raw):
        continue
    before_hash = digest(before) if before is not None else None
    if before is not None and before_hash not in known.get(rel, set()):
        baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
        assert baseline.returncode == 0 and normalize(before) == normalize(baseline.stdout), f'Root changed after reviewed handoff: {rel}'
    payload.append((source, rel, raw, before_hash, owner))

assert all(digest(source.read_bytes()) == expected for source, rel, expected, owner in sources)
for source, rel, raw, before_hash, owner in payload:
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
report = {'base': BASE, 'target': str(TARGET), 'B_manifest_files': len(b_rows), 'files': [
    {'owner': owner, 'source': str(source), 'path': rel, 'sha256': digest(raw), 'before_sha256': before_hash}
    for source, rel, raw, before_hash, owner in payload], 'workers_written': False}
for source, rel, raw, before_hash, owner in payload:
    destination = TARGET / rel
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'd2-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'B_manifest_files': len(b_rows), 'received_delta': len(payload),
                  'per_owner': {o: sum(row[4] == o for row in payload) for o in ('B', 'D')},
                  'A_C_workers_written': False}))
