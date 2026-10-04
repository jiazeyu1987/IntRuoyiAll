"""Revalidate the exact authorized sidecar execution and actual object proof."""
from pathlib import Path
import hashlib
import importlib.util
import json

ROOT = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
TASK = REPO / 'doc/tasks/20261002-dcc-detail-integration'
PROTECTED = Path('C:/IntRuoyiBackups/20261003-dcc-integration')


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


spec = importlib.util.spec_from_file_location('g39_runtime_review_driver', TASK / 'g39-driver.py')
driver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(driver)
assert sha(TASK / 'g39-driver.py') == '667a81edf468e1212afef8a01ae5cb996bd62737cfba7db6138e30259e0db2d4'
executions = []
for name, database in [('g39-clone-reviewed-resume-run', driver.CLONE), ('g39-source-migration-run', driver.SOURCE)]:
    path = PROTECTED / name / 'driver-receipt.json'
    journal = json.loads(path.read_text())
    driver.validate_completed_journal(journal, database)
    before = json.loads(Path(journal['originalBefore']['path']).read_text())
    after = json.loads(Path(journal['steps'][-1]['originalAfter']['path']).read_text())
    driver.compare_snapshots(before, after, {'infra_release_migration': 1})
    post = json.loads(Path(journal['steps'][-1]['postflight']['path']).read_text())
    assert len([r for r in post if r['kind'] == 'table']) == 3
    assert len([r for r in post if r['kind'] == 'column']) == 70
    assert len([r for r in post if r['kind'] == 'check']) == 7
    assert len([r for r in post if r['kind'] == 'row_count' and r['count'] == 0]) == 3
    executions.append({'database': database, 'status': journal['status'], 'receiptSha256': sha(path),
                       'originalRowCounts': {t: r['count'] for t, r in before.items()},
                       'newLedgerRows': 1, 'repeatLedgerWrites': 0,
                       'firstReplayExecuted': journal.get('firstReplayExecuted', False),
                       'oldRowsUnchanged': True, 'threeNewTablesEmpty': True,
                       'columns': 70, 'checks': 7})
stop = PROTECTED / 'g39-clone-migration-run/driver-receipt.json'
assert json.loads(stop.read_text())['status'] == 'STOPPED_DDL_PARTIAL_COMMIT_POSSIBLE_REVIEW_REQUIRED'
originals = json.loads((ROOT / 'g39-original-recovery-root-review.json').read_text())
assert originals['acceptedUniqueKeys'] == 3 and originals['matches'] == 39
result = {'status': 'ACTUAL_CLONE_AND_SOURCE_SIDECAR_FIRST_REPEAT_PASS_ORIGINALS39_MATCH_NOT_APPLICATION_ACCEPTANCE',
          'executions': executions, 'originalValidatorStopReceiptPreserved': True,
          'stopReceiptSha256': sha(stop), 'originalRecoveryReceiptSha256': sha(ROOT / 'g39-original-recovery-root-review.json'),
          'actualQualityApproved': False, 'auditOperationsEnabled': 0,
          'legacyRegistrationExecuted': False, 'actualBrowserAcceptance': False, 'localIntQmsMergeComplete': False}
(ROOT / 'g39-runtime-root-review.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': result['status'], 'databases': 2, 'originalObjectsMatched': 39}))
