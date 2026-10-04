"""Deliver Root-owned read API/panel wiring without overwriting resumed worker edits."""
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
        for key in ('sha256', 'before_sha256'):
            if row.get(key):
                known.setdefault(row['path'], set()).add(row[key])
register(json.loads((TASK / 'dependency-sync-manifest.json').read_text(encoding='utf-8'))['files'])
for name in ('ad-review-dependency-sync.json', 'h01-dependency-sync.json', 'h02-dependency-sync.json'):
    for worker in json.loads((TASK / name).read_text(encoding='utf-8'))['workers']:
        register(worker['files'])
last_path = TASK / 'h03-dependency-sync.json'
if last_path.exists():
    for worker in json.loads(last_path.read_text(encoding='utf-8'))['workers']:
        register(worker['files'])
paths = [
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java',
    'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationReadHttpContractTest.java',
    'IntRuoyiFronted/src/api/dcc/controlledFile/applicationRead.ts',
    'IntRuoyiFronted/src/views/dcc/controlled-file/project-attributes/ApplicationEvidencePanel.vue',
    'IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue',
    'IntRuoyiFronted/tests/unit/dcc-application-read-loader.test.cjs',
    'IntRuoyiFronted/tests/unit/dcc-application-evidence-panel.test.cjs',
    'IntRuoyiFronted/tests/unit/dcc-detail-training-context.test.cjs',
]
plans = []
report = {'source': str(SOURCE), 'base': BASE, 'workers': [], 'B_C_sources_written': False}
for module in ('a', 'd'):
    worker = Path('C:/IntRuoyi/20260930-dcc-' + module)
    assert subprocess.check_output(['git', '-C', str(worker), 'rev-parse', 'HEAD'], text=True).strip() == BASE
    rows = []
    verified = []
    for rel in paths:
        raw = (SOURCE / rel).read_bytes()
        destination = worker / rel
        assert destination.resolve().is_relative_to(worker.resolve())
        before = destination.read_bytes() if destination.exists() else None
        verified.append({'path': rel, 'sha256': digest(raw)})
        if before is not None and normalize(before) == normalize(raw):
            continue
        before_hash = digest(before) if before is not None else None
        if before is not None and before_hash not in known.get(rel, set()):
            baseline = subprocess.run(['git', '-C', str(worker), 'show', BASE + ':' + rel], capture_output=True)
            assert baseline.returncode == 0 and normalize(before) == normalize(baseline.stdout), f'Resumed worker edited Root asset: {module}/{rel}'
        plans.append((destination, raw, before_hash, rel))
        rows.append({'path': rel, 'before_sha256': before_hash, 'sha256': digest(raw)})
    report['workers'].append({'module': module, 'path': str(worker), 'files': rows, 'verified_files': verified})
for destination, raw, before_hash, rel in plans:
    assert digest((SOURCE / rel).read_bytes()) == digest(raw)
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
for destination, raw, before_hash, rel in plans:
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'h03-dependency-sync.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'files': {w['module']: len(w['files']) for w in report['workers']},
                  'B_C_sources_written': False, 'resumed_owner_changes_preserved': True}))
