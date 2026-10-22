const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')

assertContains(api, '/infra/runtime-control/actions/preview', 'action preview API')
assertContains(api, '/infra/runtime-control/actions', 'action execute API')
assertContains(api, '/infra/runtime-control/release-status', 'release status API')
assertContains(api, '/infra/runtime-control/rollback-candidates', 'rollback candidates API')
assertContains(api, '/infra/runtime-control/restore-candidates', 'restore candidates API')

assertContains(page, "action: 'build-release'", 'build-release button action')
assertContains(page, "action: 'publish-test'", 'publish-test button action')
assertContains(page, "action: 'mark-release-tested'", 'mark-tested button action')
assertContains(page, "action: 'promote-prod'", 'promote-prod button action')
assertContains(page, '上线已验证发布包', 'production promotion label')
assertContains(page, 'PROD', 'production confirmation literal')
assertContains(page, 'prodConfirmText', 'production confirmation payload')
assertContains(page, 'releaseTag', 'releaseTag payload')
assertContains(page, 'publishScope', 'publish scope payload')
assertContains(page, 'selectedRecoverySetCandidateId', 'tested recovery set payload')
assertContains(page, 'testConclusion', 'test conclusion payload')

assertNotContains(page, 'build-release publish-test', 'combined rebuild on promote')
assertNotContains(page, '重新构建正式服', 'production promote rebuild copy')

console.log('PASS: runtime-control one-button release static contract is wired')
