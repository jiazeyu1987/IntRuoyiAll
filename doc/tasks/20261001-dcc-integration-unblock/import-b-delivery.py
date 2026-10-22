"""Receive the frozen B handoff; refuse concurrent source or target edits."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
WORKER = Path('C:/IntRuoyi/20260930-dcc-b')
TARGET = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
digest = lambda raw: hashlib.sha256(raw).hexdigest()
previous = {row['path']: row['sha256'] for row in json.loads(
    (TASK / 'dependency-sync-manifest.json').read_text(encoding='utf-8'))['files']}
rows = json.loads((WORKER / 'doc/tasks/20260930-dcc-b-project/changed-files.json').read_text(encoding='utf-8'))
for repo in (WORKER, TARGET):
    assert subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip() == BASE

payload = []
for row in rows:
    rel = row['path']
    assert rel.startswith(('IntRuoyiBackend/', 'IntRuoyiFronted/'))
    source, destination = WORKER / rel, TARGET / rel
    assert source.resolve().is_relative_to(WORKER.resolve())
    assert destination.resolve().is_relative_to(TARGET.resolve())
    raw = source.read_bytes()
    assert digest(raw) == row['sha256'], f'B owner edit after handoff: {rel}'
    if row['sha256'] == previous.get(rel):
        continue
    before = digest(destination.read_bytes()) if destination.is_file() else None
    if rel in previous:
        assert before == previous[rel], f'Root edit after last synchronization: {rel}'
    elif before is not None:
        baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
        assert baseline.returncode == 0, f'Existing unrelated destination: {rel}'
        normalize = lambda raw: raw.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
        assert normalize(destination.read_bytes()) == normalize(baseline.stdout), f'Root edit outside synchronization manifest: {rel}'
    payload.append((rel, raw, before))

# Recheck the complete handoff immediately before any writes.
assert all(digest((WORKER / row['path']).read_bytes()) == row['sha256'] for row in rows)
report = {'base': BASE, 'source': str(WORKER), 'target': str(TARGET),
          'owner_manifest_files': len(rows), 'git_merge_or_commit': False,
          'files': [{'path': rel, 'before_sha256': before, 'sha256': digest(raw)} for rel, raw, before in payload]}
for rel, raw, before in payload:
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'b-delivery-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'result': 'PASS', 'source_files': len(rows), 'received_delta': len(payload),
                  'other_workers_written': False, 'git_merge_or_commit': False}))
