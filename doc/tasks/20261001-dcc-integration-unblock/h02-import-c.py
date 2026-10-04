"""Freeze and receive C's completed CC-2/preceding increments; preserve the newer B fixture."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT = Path('C:/IntRuoyiAll-int_main')
SOURCE = Path('C:/IntRuoyi/20260930-dcc-c')
TARGET = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
BASE = 'a801dc8b91579241e221d129ab34343997673f40'
digest = lambda raw: hashlib.sha256(raw).hexdigest()
normalize = lambda raw: raw.decode('utf-8-sig').replace('\r\n', '\n').replace('\r', '\n')
previous = {row['path']: row['sha256'] for row in json.loads((TASK / 'dependency-sync-manifest.json').read_text(encoding='utf-8'))['files']}
known = {rel: {sha} for rel, sha in previous.items()}
def register(rows):
    for row in rows:
        for key in ('sha256', 'before_sha256'):
            if row.get(key):
                known.setdefault(row['path'], set()).add(row[key])
for name in ('ad-delivery-import-manifest.json', 'd2-import-manifest.json', 'h01-import-manifest.json'):
    register(json.loads((TASK / name).read_text(encoding='utf-8'))['files'])
for name in ('ad-review-dependency-sync.json', 'h01-dependency-sync.json'):
    for worker in json.loads((TASK / name).read_text(encoding='utf-8'))['workers']:
        register(worker['files'])
contract_path = SOURCE / 'doc/tasks/20260930-dcc-c-version/cc2-delivery-fingerprints.json'
contract_raw = contract_path.read_bytes()
contract = json.loads(contract_raw.decode('utf-8'))
for rel, expected in contract['files'].items():
    assert digest(normalize((SOURCE / rel).read_bytes()).encode('utf-8')) == expected, f'C frozen contract changed: {rel}'
for repo in (SOURCE, TARGET):
    assert subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip() == BASE

entries = subprocess.check_output(['git', '-C', str(SOURCE), 'status', '--porcelain=v1', '-z', '--untracked-files=all',
    '--', 'IntRuoyiBackend', 'IntRuoyiFronted']).decode().split('\0')
payload = []
for entry in entries:
    if not entry:
        continue
    assert entry[:2].strip() not in ('D', 'R'), f'Review deletion separately: {entry}'
    rel = entry[3:]
    source, destination = SOURCE / rel, TARGET / rel
    assert source.resolve().is_relative_to(SOURCE.resolve()) and destination.resolve().is_relative_to(TARGET.resolve())
    raw = source.read_bytes()
    source_hash = digest(raw)
    if previous.get(rel) == source_hash:
        continue
    before = destination.read_bytes() if destination.exists() else None
    if before is not None and normalize(before) == normalize(raw):
        continue
    before_hash = digest(before) if before is not None else None
    if before is not None and before_hash not in known.get(rel, set()):
        baseline = subprocess.run(['git', '-C', str(TARGET), 'show', BASE + ':' + rel], capture_output=True)
        assert baseline.returncode == 0 and normalize(before) == normalize(baseline.stdout), f'Unreviewed Root edit: {rel}'
    assert (rel in contract['files'] or rel.endswith(('/DccMajorRevisionBoundaryTest.java', '/DccControlledFileCheckoutContractTest.java',
        '/DccControlledFileRevisionOptions.java', '/DccControlledFileSelectorRow.java'))), f'Unexpected C scope: {rel}'
    mode = 'frozen-C-increment'
    if rel.endswith('/src/test/resources/sql/create_tables.sql'):
        text = normalize(before)
        old = '`c_version_key` VARBINARY(256) AS (CASE WHEN source_original_file_name IS NOT NULL THEN CAST(version_no AS VARBINARY) ELSE NULL END),'
        new = '`c_version_key` VARBINARY(288) AS (CASE WHEN source_original_file_name IS NOT NULL THEN CAST(CASE WHEN revision_change_type=\'INITIAL\' AND selected_iteration_controlled_file_id IS NOT NULL THEN CONCAT(version_no,\'#INITIAL\') ELSE version_no END AS VARBINARY) ELSE NULL END),'
        assert text.count(old) == 1 and new in normalize(raw)
        raw = text.replace(old, new, 1).encode('utf-8')
        assert 'source_application_id' in raw.decode() and 'creation_reason' in raw.decode()
        mode = 'Root-B-fixture-preserved-C-role-key-only'
    payload.append((rel, source, raw, before_hash, source_hash, mode))

assert contract_path.read_bytes() == contract_raw
for rel, source, raw, before_hash, source_hash, mode in payload:
    assert digest(source.read_bytes()) == source_hash
    destination = TARGET / rel
    assert (digest(destination.read_bytes()) if destination.exists() else None) == before_hash
report = {'source': str(SOURCE), 'target': str(TARGET), 'base': BASE, 'cc2_contract_files': len(contract['files']),
    'files': [{'path': rel, 'sha256': digest(raw), 'before_sha256': before_hash, 'source_sha256': source_hash, 'mode': mode}
        for rel, source, raw, before_hash, source_hash, mode in payload], 'workers_written': False}
for rel, source, raw, before_hash, source_hash, mode in payload:
    destination = TARGET / rel
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(raw)
(TASK / 'h02-c-contract-receipt.json').write_bytes(contract_raw)
(TASK / 'h02-import-manifest.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'PASS': True, 'C_CC2_frozen_files': len(contract['files']), 'received_increment': len(payload),
    'newer_B_fixture_preserved': True, 'worker_sources_written': False}))
