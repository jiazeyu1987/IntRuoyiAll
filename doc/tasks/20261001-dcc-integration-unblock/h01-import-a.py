"""Receive A's frozen state-audit delivery, preserving D's newer shared tests."""
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
freeze_path = SOURCE / 'doc/tasks/20260930-dcc-a-workflow/review-freeze-manifest.json'
freeze_raw = freeze_path.read_bytes()
freeze = json.loads(freeze_raw.decode('utf-8'))
known = {}
def register(rows):
    for row in rows:
        for key in ('sha256', 'before_sha256'):
            if row.get(key):
                known.setdefault(row['path'], set()).add(row[key])
for name in ('dependency-sync-manifest.json', 'ad-delivery-import-manifest.json', 'd2-import-manifest.json'):
    register(json.loads((TASK / name).read_text(encoding='utf-8'))['files'])
for worker in json.loads((TASK / 'ad-review-dependency-sync.json').read_text(encoding='utf-8'))['workers']:
    register(worker['files'])
for repo in (SOURCE, TARGET):
    assert subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip() == BASE
assert all(digest((SOURCE / row['path']).read_bytes()) == row['rawSha256'] for row in freeze['files'])

payload = []
shared_name = 'DccRelationControlledEventIntegrationTest.java'
for row in freeze['files']:
    if row['role'] not in ('production', 'test'):
        continue
    rel = row['path']
    source, destination = SOURCE / rel, TARGET / rel
    assert destination.resolve().is_relative_to(TARGET.resolve())
    before = destination.read_bytes() if destination.exists() else None
    before_hash = digest(before) if before is not None else None
    assert before is None or before_hash in known.get(rel, set()), f'Unknown Root edit: {rel}'
    if rel.endswith('/' + shared_name):
        # D's 15-case file contains newer real Flowable signed-control evidence; add only A's required audit wiring.
        text = before.decode('utf-8')
        anchor = '@Import({DccControlledFileLifecycleService.class,DccWorkflowDatePolicy.class,'
        assert text.count(anchor) == 1
        text = text.replace(anchor, anchor + 'DccWorkflowFileStateAudit.class,', 1)
        anchor = 'policy("dcc.relation.arrange");policy("dcc.relation.controlled");'
        assert text.count(anchor) == 1
        text = text.replace(anchor, anchor + '\n        policy("dcc.controlled-file.control");policy("dcc.controlled-file.activate");policy("dcc.controlled-file.auto-obsolete");', 1)
        raw = text.encode('utf-8')
        assert 'signedAssignmentAndControlShareOneActualRoundBeforePlatformNotification' in text or 'actualSigned' in text or 'continuousSigned' in text or 'sameReal' in text or 'SELECTED' in text
        mode = 'retain-D-shared-tests-add-A-audit-wiring'
    else:
        raw = source.read_bytes()
        mode = 'frozen-A-delivery'
    payload.append((rel, raw, before_hash, row['rawSha256'], mode))

assert freeze_path.read_bytes() == freeze_raw
assert all(digest((SOURCE / row['path']).read_bytes()) == row['rawSha256'] for row in freeze['files'])
for rel, raw, before_hash, _, _ in payload:
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
report = {'source': str(SOURCE), 'target': str(TARGET), 'base': BASE, 'files': [
    {'path': rel, 'sha256': digest(raw), 'before_sha256': before_hash, 'source_sha256': source_hash, 'mode': mode}
    for rel, raw, before_hash, source_hash, mode in payload], 'workers_written': False}
for rel, raw, before_hash, _, _ in payload:
    destination = TARGET / rel
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'h01-a-freeze-receipt.json').write_bytes(freeze_raw)
(TASK / 'h01-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'A_freeze_files': len(freeze['files']), 'integrated_files': len(payload),
                  'D_signed_flowable_scenarios_preserved': True, 'worker_writes': False}))
