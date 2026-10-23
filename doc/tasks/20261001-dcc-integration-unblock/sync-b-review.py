"""Deliver only the two Root-owned fixes to B; preserve all worker development."""
from pathlib import Path
import hashlib
import json

ROOT = Path('C:/IntRuoyiAll-int_main')
SOURCE = Path('C:/IntRuoyi/20261001-dcc-integration')
TARGET = Path('C:/IntRuoyi/20260930-dcc-b')
TASK = ROOT / 'doc/tasks/20261001-dcc-integration-unblock'
previous = {row['path']: row['sha256'] for row in json.loads(
    (TASK / 'dependency-sync-manifest.json').read_text(encoding='utf-8'))['files']}
files = [
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccLatestControlledFileResolverImpl.java',
    'IntRuoyiBackend/config/gxp-audit-policy.yaml',
]
digest = lambda raw: hashlib.sha256(raw).hexdigest()
payload = []
for rel in files:
    destination = TARGET / rel
    assert destination.resolve().is_relative_to(TARGET.resolve())
    assert digest(destination.read_bytes()) == previous[rel], f'B changed Root-owned dependency: {rel}'
    raw = (SOURCE / rel).read_bytes()
    payload.append((rel, raw))
for rel, raw in payload:
    assert digest((TARGET / rel).read_bytes()) == previous[rel]
    (TARGET / rel).write_bytes(raw)
report = {'source': str(SOURCE), 'target': str(TARGET), 'files': [
    {'path': rel, 'before_sha256': previous[rel], 'sha256': digest(raw)} for rel, raw in payload],
    'other_worker_or_owner_source_written': False}
(TASK / 'b-review-dependency-sync.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({'result': 'PASS', 'files': len(payload), 'worker': 'B', 'owner_source_preserved': True}))
