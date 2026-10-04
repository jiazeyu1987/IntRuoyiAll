"""Validate the main-flow navigation source and completed offline verification."""
from pathlib import Path
import hashlib
import json
import re

MAIN = Path(__file__).resolve().parent
REPO = Path('C:/IntRuoyi/20261001-dcc-integration')
BACKEND = REPO / 'doc/tasks/20261002-dcc-public-backend-completion'
DETAIL = REPO / 'doc/tasks/20261002-dcc-detail-integration'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def verify(item):
    path = Path(item['path'])
    if not path.is_absolute():
        path = REPO / path
    assert sha(path) == item['sha256'], str(path)
    assert path.stat().st_size == item['bytes']
    return path


manifest_path = BACKEND / 'g35-navigation-delivery-fingerprints.json'
assert sha(manifest_path) == '286796c7ab37c715461df866a61147337ecee4ef5414c0cd134a84d5b932fd02'
manifest = read(manifest_path)
for item in manifest['sourceFiles'] + manifest['rawEvidence']:
    verify(item)
verify(manifest['unchangedRouteGuard'])
assert manifest['green']['cliExitCode'] == 0 and manifest['green']['tests'] == 7
assert manifest['effectiveRed']['failures'] == 1 and not manifest['green']['actualBrowserRun']

parent_path = MAIN / 'g20-delivery-fingerprints.json'
assert sha(parent_path) == manifest['priorSourceParent']['rawSha256']
parent = read(parent_path)
detail_path = 'IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue'
for item in parent['assets']:
    if item['path'] != detail_path:
        assert sha(REPO / item['path']) == item['sha256'], 'prior source drift: ' + item['path']
body = (REPO / detail_path).read_bytes()
new_import = b"import { buildSubmittedApplicationRoute, requireSubmittedApplicationId } from '../shared/submitted-application-navigation'\n"
assert body.count(new_import) == 1
old_handler = b"const handleApplicationSubmitted = async (id: string) => {\r\n  message.success('\xe6\x9c\xac\xe6\xac\xa1\xe7\x94\xb3\xe8\xaf\xb7\xe5\xb7\xb2\xe6\xad\xa3\xe5\xbc\x8f\xe6\x8f\x90\xe4\xba\xa4')\r\n  if (id === controlledFileId.value) await reloadAll()\r\n  else await router.push({ path: `/dcc/controlled-file/detail/${id}`, query: { from: 'application-submit' } })\r\n}"
new_handler = b"const handleApplicationSubmitted = async (id: string) => {\n  const submittedId = requireSubmittedApplicationId(id)\n  message.success('\xe6\x9c\xac\xe6\xac\xa1\xe7\x94\xb3\xe8\xaf\xb7\xe5\xb7\xb2\xe6\xad\xa3\xe5\xbc\x8f\xe6\x8f\x90\xe4\xba\xa4')\n  if (submittedId === controlledFileId.value) await reloadAll()\n  else await router.push(buildSubmittedApplicationRoute(submittedId, route.query))\n}"
assert body.count(new_handler) == 1
restored = body.replace(new_import, b'').replace(new_handler, old_handler.replace(b'\r\n', b'\n'))
assert hashlib.sha256(restored).hexdigest() == manifest['priorSourceParent']['detailPriorSha256'], 'unreviewed detail changes beyond authorized caller/import'

fixture = read(DETAIL / 'g35-browser-entry-fixture-receipt.json')
verify({'path': fixture['file'], 'bytes': fixture['bytes'], 'sha256': fixture['sha256']})
assert fixture['exitCode'] == 0 and fixture['tests'] == 2 and fixture['productionFilesEdited'] == 0
review_path = DETAIL / 'g35-navigation-independent-review.md'
assert '未识别确认主线范围内新的导航断链' in review_path.read_text(encoding='utf-8')
evidence = {}
for kind in ['types', 'lint', 'build']:
    path = MAIN / ('g35-frontend-' + kind + '-execution.json')
    execution = read(path)
    assert execution['exitCode'] == 0, kind
    evidence[kind] = {'exitCode': 0, 'logSha256': sha(execution['log'])}
combination = read(MAIN / 'g35-detail-combination-r2-execution.json')
assert combination['exitCode'] == 0 and len(combination['files']) == 11
log = Path(combination['log']).read_text(encoding='utf-8', errors='replace')
assert all(re.search(r'\b' + key + r'\s+' + str(count) + r'\b', log) for key, count in [('tests', 84), ('pass', 84), ('fail', 0), ('skipped', 0)])
evidence['combination'] = {'tests': 84, 'files': 11, 'exitCode': 0, 'logSha256': sha(combination['log'])}
assert (REPO / 'IntRuoyiFronted/dist/index.html').is_file()
assert (REPO / 'IntRuoyiFronted/dist/index.html').stat().st_size > 0
for item in read(MAIN / 'goal-preserved-nontask-assets.json')['preservedAssets']:
    assert sha(Path(item['workspace']) / item['path']) == item['sha256']
output = {
    'status': 'ROOT_REVIEWED_MAIN_FLOW_NAVIGATION_AND_FRONTEND_NOT_RUNTIME_ACCEPTANCE',
    'sourceManifestSha256': sha(manifest_path), 'independentReviewSha256': sha(review_path),
    'priorDetailDeltaExactlyTwoBlocks': True, 'otherParentAssetsUnchanged': 19,
    'navigationTests': 7, 'navigationFailures': 0, 'combinationTests': 84,
    'combinationFailureWasStaleFixture': True, 'verification': evidence,
    'distIndexSha256': sha(REPO / 'IntRuoyiFronted/dist/index.html'),
    'actualBrowserAcceptance': False, 'actualDatabaseWrites': False, 'actualLocalMerge': False,
    'newProjectInitialAccessDecision': 'USER_ANSWER_REQUIRED',
}
path = MAIN / 'g35-frontend-root-review.json'
path.write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': output['status'], 'combinationTests': 84, 'navigationTests': 7}, ensure_ascii=False))
