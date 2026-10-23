"""Supply verified B inheritance dependencies without changing A/C owned workflow code."""
from pathlib import Path
import hashlib
import json
import re
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
SOURCE = Path('C:/IntRuoyi/20261001-dcc-integration')
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
        for child in value.values(): register(child)
    elif isinstance(value, list):
        for child in value: register(child)
for path in list(TASK.glob('*manifest.json')) + list(TASK.glob('*sync.json')):
    register(json.loads(path.read_text(encoding='utf-8-sig')))
receipt = json.loads((TASK / 'h08-b-freeze-receipt.json').read_text(encoding='utf-8-sig'))
freeze_rows = receipt['fullSources'] + receipt['formalDependencies'] + receipt['existingMigrationDependencies']
expected = {r['path']: r['sha256'] for r in freeze_rows}
register(freeze_rows)
be = 'IntRuoyiBackend/yudao-module-dcc/'
attrs = be + 'src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/attributes/'
common = [attrs + 'DccProjectApplicationSnapshotService.java', attrs + 'DccProjectAttributesService.java']
c_extra = [be + 'src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/projectcode/DccProjectApplicationAttributesDO.java',
    be + 'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationRoundService.java',
    'IntRuoyiBackend/sql/mysql/20261001_dcc_application_round_link.sql',
    'IntRuoyiBackend/sql/mysql/20261001_dcc_b_reserved_draft_round.sql']
plans = []
workers = []
for module in ('a', 'c'):
    worker = Path('C:/IntRuoyi/20260930-dcc-' + module)
    assert subprocess.check_output(['git', '-C', str(worker), 'branch', '--show-current'], text=True).strip() == 'codex/20260930-dcc-' + module
    assert subprocess.check_output(['git', '-C', str(worker), 'rev-parse', 'HEAD'], text=True).strip() == BASE
    rows = []
    for rel in common + (c_extra if module == 'c' else [be + 'src/test/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectUnsubmittedInheritanceTest.java']):
        raw = (SOURCE / rel).read_bytes()
        assert sha(raw) == expected[rel], f'Root received B asset changed: {rel}'
        target = worker / rel
        assert target.resolve().is_relative_to(worker.resolve())
        before = target.read_bytes() if target.exists() else None
        if before is not None and lf(before) == lf(raw): continue
        before_hash = sha(before) if before is not None else None
        if before is not None and before_hash not in known.get(rel, set()):
            baseline = subprocess.run(['git', '-C', str(worker), 'show', BASE + ':' + rel], capture_output=True)
            assert baseline.returncode == 0 and lf(before) == lf(baseline.stdout), f'Unreviewed worker edit: {module}/{rel}'
        plans.append((target, raw, before_hash, SOURCE / rel))
        rows.append({'path': rel, 'before_sha256': before_hash, 'sha256': sha(raw)})
    if module == 'c':
        rel = be + 'src/test/resources/sql/create_tables.sql'
        target = worker / rel
        before = target.read_bytes()
        text = before.decode('utf-8-sig')
        pattern = r'CREATE TABLE IF NOT EXISTS dcc_project_application_attributes \([\s\S]*?\n\);'
        existing = re.search(pattern, text)
        formal = re.search(pattern, (SOURCE / rel).read_text(encoding='utf-8-sig'))
        assert existing and formal
        addition = ' source_application_id BIGINT NULL, source_application_round INT NULL,\n'
        expected_previous = formal.group(0).replace(addition, '')
        assert lf(existing.group(0).encode()) == lf(expected_previous.encode()), 'C attributes fixture contains unknown changes'
        newline = '\r\n' if '\r\n' in text else '\n'
        replacement = formal.group(0).replace('\n', newline)
        raw = (text[:existing.start()] + replacement + text[existing.end():]).encode('utf-8')
        assert lf(raw).replace(addition, '') == lf(before), 'Unexpected C fixture changes outside provenance columns'
        plans.append((target, raw, sha(before), None))
        rows.append({'path': rel, 'before_sha256': sha(before), 'sha256': sha(raw), 'merged_columns': ['source_application_id', 'source_application_round']})
    workers.append({'module': module, 'path': str(worker), 'files': rows})
for target, raw, before_hash, origin in plans:
    assert (sha(target.read_bytes()) if target.exists() else None) == before_hash
    if origin is not None: assert sha(origin.read_bytes()) == sha(raw)
for target, raw, before_hash, origin in plans:
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(raw)
report = {'base': BASE, 'source': str(SOURCE), 'workers': workers, 'A_C_owner_production_written': False,
    'worker_heartbeat_created': False, 'instant_messages_sent': False}
(TASK / 'h08-dependency-sync.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'workers': {r['module']: len(r['files']) for r in workers}, 'A_C_owner_production_written': False}))
