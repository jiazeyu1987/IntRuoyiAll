"""Deliver reviewed A/D and B dependencies with complete preflight hash checks."""
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
previous = {row['path']: row['sha256'] for row in json.loads(
    (TASK / 'dependency-sync-manifest.json').read_text(encoding='utf-8'))['files']}
ad = {row['path']: row for row in json.loads((TASK / 'ad-delivery-import-manifest.json').read_text(encoding='utf-8'))['files']}
b = {row['path']: row for row in json.loads((TASK / 'b-delivery-import-manifest.json').read_text(encoding='utf-8'))['files']}
paths = set(ad) | set(b) | {
    'IntRuoyiBackend/config/gxp-audit-policy.yaml',
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileLifecycleEvent.java',
}
payload = {rel: (SOURCE / rel).read_bytes() for rel in paths}
plans = []
report = {'source': str(SOURCE), 'base': BASE, 'workers': [], 'git_or_runtime_changes': False}
for module in ('a', 'd'):
    worker = Path('C:/IntRuoyi/20260930-dcc-' + module)
    assert subprocess.check_output(['git', '-C', str(worker), 'rev-parse', 'HEAD'], text=True).strip() == BASE
    rows = []
    for rel, raw in payload.items():
        destination = worker / rel
        assert destination.resolve().is_relative_to(worker.resolve())
        before = destination.read_bytes() if destination.is_file() else None
        if before is not None and normalize(before) == normalize(raw):
            continue
        before_hash = digest(before) if before is not None else None
        expected = previous.get(rel)
        if rel in ad and ad[rel]['owner'] == module:
            expected = ad[rel]['sha256']
        if expected is not None:
            assert before_hash == expected, f'{module} edit since frozen handoff: {rel}'
        elif before is not None:
            baseline = subprocess.run(['git', '-C', str(worker), 'show', BASE + ':' + rel], capture_output=True)
            assert baseline.returncode == 0 and normalize(before) == normalize(baseline.stdout), f'{module} unrelated destination: {rel}'
        plans.append((destination, raw, before_hash))
        rows.append({'path': rel, 'before_sha256': before_hash, 'sha256': digest(raw)})
    report['workers'].append({'module': module, 'path': str(worker), 'files': rows})

# No worker is touched until all source and destination files pass preflight.
assert all(digest((SOURCE / rel).read_bytes()) == digest(raw) for rel, raw in payload.items())
for destination, raw, before_hash in plans:
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
for destination, raw, before_hash in plans:
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'ad-review-dependency-sync.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'files_per_worker': {w['module']: len(w['files']) for w in report['workers']},
    'B_C_source_written': False, 'owner_changes_preserved': True}))
