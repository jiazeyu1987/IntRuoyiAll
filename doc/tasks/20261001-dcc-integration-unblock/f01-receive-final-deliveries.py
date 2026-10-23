"""Freeze final Owner deliveries and receive only recognized source/target versions."""
from pathlib import Path
import argparse
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
        rel = value.get('path')
        if rel:
            for key, val in value.items():
                if isinstance(val, str) and ('sha' in key.lower() or 'hash' in key.lower()) and len(val) == 64:
                    known.setdefault(rel, set()).add(val)
        for child in value.values(): register(child)
    elif isinstance(value, list):
        for child in value: register(child)
for path in TASK.glob('*.json'):
    register(json.loads(path.read_text(encoding='utf-8-sig')))

# These exact H08 Root fixture edits were inspected against D's final increment:
# both load the real B state; D retains that correction and adds upload/reference tests.
# No production conflict is authorized by this narrow reviewed fixture record.
for rel, digest in {
    'IntRuoyiFronted/scripts/dcc-relations-vue-runtime.test.mjs': 'd9f8fbd1ab6c53c8c0941a4a6384fd726ecd46457e3b0f5b9a2f615f12b223c3',
    'IntRuoyiFronted/scripts/dcc-selector-browser-contract.test.mjs': '06278caea31399092d3d5dae45e2a03f10c30af565be2e05e68a9bca00687cec',
}.items():
    known.setdefault(rel, set()).add(digest)

receipts = []
selected = {}
verified = []
def read(module, suffix, filename):
    source = Path('C:/IntRuoyi/20260930-dcc-' + module)
    assert subprocess.check_output(['git', '-C', str(source), 'branch', '--show-current'], text=True).strip() == 'codex/20260930-dcc-' + module
    assert subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip() == BASE
    path = source / f'doc/tasks/20260930-dcc-{module}-{suffix}' / filename
    raw = path.read_bytes()
    obj = json.loads(raw.decode('utf-8-sig'))
    register(obj)
    receipts.append((module, filename, path, raw))
    return source, obj

def verify(source, row, key):
    raw = (source / row['path']).read_bytes()
    assert sha(raw) == row[key], f'Frozen source drift: {source.name}/{row["path"]}'
    verified.append({'source': str(source), 'path': row['path'], 'sha256': sha(raw)})
    return raw

def choose(module, source, row, key):
    raw = verify(source, row, key)
    rel = row['path']
    assert rel.startswith(('IntRuoyiBackend/', 'IntRuoyiFronted/', 'doc/tasks/20260930-dcc-d-relations/'))
    selected[rel] = (module, source / rel, raw)

a_source, a_h08 = read('a', 'workflow', 'h08-review-freeze-manifest.json')
for row in a_h08['files']:
    if row['path'].startswith('IntRuoyiBackend/') and not row['path'].endswith('/DccControlledFileWorkflowServiceImpl.java'):
        choose('A', a_source, row, 'rawSha256')
a_source, a_selected = read('a', 'workflow', 'selected-iteration-review-manifest.json')
for row in a_selected['files']:
    if row['path'].startswith('IntRuoyiBackend/'):
        choose('A', a_source, row, 'rawSha256')
    else:
        verify(a_source, row, 'rawSha256')

b_source, b_final = read('b', 'project', 'project-discovery-review-manifest.json')
assert b_final['fullSourceCount'] == 117
for row in b_final['fullSources']:
    verify(b_source, row, 'sha256')
    if row['path'].endswith('/src/test/resources/sql/create_tables.sql'):
        continue  # Shared Root fixture already combines A/B/C/D facts and C INITIAL uniqueness.
    if row['path'].endswith('/DccApplicationRoundService.java'):
        continue  # Already reviewed Root/B shared service, never replace by a worker copy blindly.
    choose('B', b_source, row, 'sha256')

c_source, c_checkin = read('c', 'version', 'h08-checkin-delivery-fingerprints.json')
c_source, c_audit = read('c', 'version', 'lifecycle-audit-delivery-fingerprints.json')
newer_c = {row['path'] for row in c_audit['files']}
for row in c_checkin['files']:
    rel = row['path']
    if rel in newer_c or rel in selected:
        continue
    choose('C dependency', c_source, row, 'rawSha256')
for row in c_audit['files']:
    choose('C', c_source, row, 'afterRawSha256')

d_source, d_upload = read('d', 'relations', 'upload-relations-delivery-fingerprints.json')
for row in d_upload['files']:
    if row['path'].startswith('IntRuoyiFronted/scripts/'):
        # The reference delivery contains the newer runtime test; verify it there.
        if not row['path'].endswith('/dcc-relations-vue-runtime.test.mjs'):
            choose('D', d_source, row, 'sha256')
d_source, d_reference = read('d', 'relations', 'reference-delivery-fingerprints.json')
for row in d_reference['files']:
    raw = verify(d_source, row, 'sha256')
    role = row.get('role', '')
    if role.startswith(('new_d_', 'd_component_', 'd_test_', 'unchanged_d_')):
        choose('D', d_source, row, 'sha256')
    elif row['path'].endswith('/reference-view-real-responses.json'):
        choose('D evidence', d_source, row, 'sha256')
artifact = 'doc/tasks/20260930-dcc-d-relations/h02-real-selector-responses.json'
if not (TARGET / artifact).exists():
    raw = (d_source / artifact).read_bytes()
    selected[artifact] = ('D evidence', d_source / artifact, raw)

assert subprocess.check_output(['git', '-C', str(TARGET), 'branch', '--show-current'], text=True).strip() == 'codex/20261001-dcc-integration'
assert subprocess.check_output(['git', '-C', str(TARGET), 'rev-parse', 'HEAD'], text=True).strip() == BASE
plans = []
conflicts = []
unchanged = []
for rel, (module, origin, raw) in sorted(selected.items()):
    target = TARGET / rel
    assert target.resolve().is_relative_to(TARGET.resolve())
    before = target.read_bytes() if target.exists() else None
    if before is not None and lf(before) == lf(raw):
        unchanged.append({'owner': module, 'path': rel, 'sha256': sha(before)})
        continue
    before_hash = sha(before) if before is not None else None
    recognized = before is None or before_hash in known.get(rel, set()) or sha(lf(before).encode()) in known.get(rel, set())
    if not recognized:
        baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
        recognized = baseline.returncode == 0 and lf(before) == lf(baseline.stdout)
    if not recognized:
        conflicts.append({'owner': module, 'path': rel, 'target_sha256': before_hash, 'source_sha256': sha(raw)})
    plans.append((module, rel, target, origin, raw, before_hash))

report = {'base': BASE, 'target': str(TARGET), 'applied': False, 'source_files_verified': len(verified),
    'unchanged': unchanged, 'conflicts': conflicts, 'worker_sources_written': False,
    'files': [{'owner': module, 'path': rel, 'before_sha256': before_hash, 'sha256': sha(raw)}
        for module, rel, target, origin, raw, before_hash in plans]}
parser = argparse.ArgumentParser()
parser.add_argument('--apply', action='store_true')
args = parser.parse_args()
assert not conflicts, json.dumps(conflicts, ensure_ascii=False)
for module, filename, path, raw in receipts:
    assert path.read_bytes() == raw, f'Owner manifest changed: {path}'
for module, rel, target, origin, raw, before_hash in plans:
    assert sha(origin.read_bytes()) == sha(raw)
    assert (sha(target.read_bytes()) if target.exists() else None) == before_hash
if args.apply:
    for module, rel, target, origin, raw, before_hash in plans:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(raw)
    for module, filename, path, raw in receipts:
        (TASK / f'f01-{module}-{filename}').write_bytes(raw)
    report['applied'] = True
(TASK / 'f01-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'applied': args.apply, 'verified': len(verified), 'changes': len(plans),
    'unchanged': len(unchanged), 'owners': {m: sum(p[0] == m for p in plans) for m in ('A', 'B', 'C', 'C dependency', 'D', 'D evidence')},
    'worker_sources_written': False}))
