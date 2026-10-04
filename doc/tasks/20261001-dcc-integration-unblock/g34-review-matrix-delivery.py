"""Verify the reviewed main-flow route isolation candidate without running services."""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET

MAIN = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
CHILD = REPO / 'doc/tasks/20261002-dcc-detail-integration'
BACKEND = REPO / 'doc/tasks/20261002-dcc-public-backend-completion'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def verify(item):
    path = REPO / item['path']
    assert path.stat().st_size == item['bytes']
    assert sha(path) == item['sha256'], str(path)
    return path


manifest_path = CHILD / 'g34-matrix-delivery-fingerprints.json'
assert sha(manifest_path) == '06cb3caa852542337fa132b308be410d64994d4c1eaf173184163616be67b98e'
manifest = read(manifest_path)
assert (manifest['productionFiles'], manifest['testFiles']) == (2, 1)
for item in manifest['assets']:
    verify(item)
receipt_path = CHILD / 'g34-matrix-verification-receipt.json'
receipt = read(receipt_path)
assert (receipt['tests'], receipt['classes'], receipt['MavenExitCode'], receipt['failures'], receipt['errors'], receipt['skipped']) == (68, 3, 0, 0, 0, 0)
final_log = verify(receipt['currentFinalLog']).read_text(encoding='utf-8', errors='replace')
assert 'Tests run: 68, Failures: 0, Errors: 0, Skipped: 0' in final_log
assert 'BUILD SUCCESS' in final_log
verify(receipt['effectiveRed']['log'])
assert receipt['effectiveRed']['failures'] == 3 and receipt['effectiveRed']['errors'] == 0
for item in receipt['previous65And68GreenLogs'].values():
    verify(item)
totals = [0, 0, 0, 0]
xml_receipts = []
for name in ['category.DccCategoryApprovalMatrixAdminServiceImplTest', 'file.DccControlledFileApprovalRouteAssigneeResolverTest', 'route.DccApprovalRouteAdminServiceImplTest']:
    path = REPO / 'IntRuoyiBackend/yudao-module-dcc/target/surefire-reports' / ('TEST-cn.iocoder.yudao.module.dcc.service.' + name + '.xml')
    suite = ET.parse(path).getroot()
    counts = [int(suite.attrib[key]) for key in ['tests', 'failures', 'errors', 'skipped']]
    totals = [a + b for a, b in zip(totals, counts)]
    xml_receipts.append({'class': name, 'sha256': sha(path), 'counts': counts})
assert totals == [68, 0, 0, 0]
review_path = BACKEND / 'g34-matrix-main-flow-review.md'
assert sha(review_path) == '829db6d8d13b28bfdc38a174d904a6415e9989331b736a2add7828a84e7c0616'
review = review_path.read_text(encoding='utf-8')
for item in manifest['assets']:
    assert item['sha256'] in review
for item in read(CHILD / 'g27-legacy-final-delivery-fingerprints.json')['files']:
    verify(item)
for item in read(BACKEND / 'g33-maintenance-entry-delivery-fingerprints.json')['sourceFiles']:
    verify(item)
for item in read(MAIN / 'goal-preserved-nontask-assets.json')['preservedAssets']:
    assert sha(Path(item['workspace']) / item['path']) == item['sha256']
output = {
    'status': 'ROOT_REVIEWED_MAIN_FLOW_ROUTE_ISOLATION_NOT_RUNTIME_ACCEPTANCE',
    'manifestSha256': sha(manifest_path), 'verificationSha256': sha(receipt_path),
    'independentReviewSha256': sha(review_path), 'assets': manifest['assets'],
    'tests': 68, 'testClasses': 3, 'failures': 0, 'errors': 0, 'skipped': 0,
    'xmlReceipts': xml_receipts, 'old41Unchanged': True, 'G33Source10Unchanged': True,
    'protectedNontask6Unchanged': True, 'actualDatabaseWrite': False,
    'actualBrowserAcceptance': False, 'actualLocalMerge': False,
    'sourceNewerThanPackagedJar': True,
    'detailScripts': 'DEFERRED_FOR_DETAIL_PHASE_NOT_VALIDATED',
    'priorFailureRawLogsLost': receipt['lostEarlierRawEvidence'],
}
path = MAIN / 'g34-matrix-root-review.json'
path.write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': output['status'], 'tests': 68, 'output': str(path)}, ensure_ascii=False))
