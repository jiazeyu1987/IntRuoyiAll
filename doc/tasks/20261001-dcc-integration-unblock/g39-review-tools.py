"""Root review of the frozen exact migration correction and actual stopped FIRST."""
from pathlib import Path
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = REPO / 'doc/tasks/20261002-dcc-detail-integration'
PROTECTED = Path('C:/IntRuoyiBackups/20261003-dcc-integration')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def desc(path):
    path = Path(path)
    return {'path': str(path.resolve()), 'sha256': sha(path), 'bytes': path.stat().st_size}


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


manifest = TASK / 'g39-tools-delivery-fingerprints.json'
assert sha(manifest) == 'ed950b3a79da5e51827d29ea48ae682454d655fabd2851c04d1844a8da4309eb'
sealed = json.loads(manifest.read_text())
for item in sealed['assets'] + sealed['rawEvidence'] + sealed['originalSealedTools'] + [sealed['originalSql']]:
    path = REPO / item['path']
    assert sha(path) == item['sha256'] and path.stat().st_size == item['bytes'], str(path)
schema = module('g39_root_review_schema', TASK / 'g39-schema.py')
driver = module('g39_root_review_driver', TASK / 'g39-driver.py')
facts = PROTECTED / 'g39-clone-migration-run/first-postflight-captured-after-validator-stop.json'
assert sha(facts) == sealed['protectedActualFactSha256']
assert schema.validate_facts(schema.prepare_contract(), json.loads(facts.read_text()), 'post', database=driver.CLONE) == 'POSTFLIGHT_PASS'
stop = PROTECTED / 'g39-clone-migration-run/driver-receipt.json'
journal = driver.validate_stopped_first(desc(stop), driver.CLONE, 'dcc-g28-local-new-sidecars')
assert len(journal['steps']) == 1 and journal['steps'][0]['execution']['exitCode'] == 0
result = {
    'status': 'ROOT_REVIEWED_ACTUAL_CHECK_AND_STOPPED_FIRST_REPEAT_ONLY_RESUME_CANDIDATE',
    'toolManifestSha256': sha(manifest), 'actualFacts': 104, 'enforcedChecks': 7,
    'firstSqlActualExit': 0, 'firstLedgerAlreadyCommitted': True, 'firstReplayForbidden': True,
    'stopJournalSha256': sha(stop), 'newDriverSha256': sha(TASK / 'g39-driver.py'),
    'sourceExecuted': False, 'actualResumeExecuted': False,
}
(ROOT / 'g39-migration-tool-root-review.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps(result))
