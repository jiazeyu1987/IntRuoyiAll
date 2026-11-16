const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const pagePath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.doesNotMatch(
  pageSource,
  /data-qa-regulation-workspace-tabs/,
  'Independent menu pages must not retain the nested workspace switch.'
)
const routes = fs.readFileSync(path.join(workspaceRoot, 'IntRuoyiFronted/src/router/modules/remaining.ts'), 'utf8')
assert.ok(routes.includes("path: 'pro/process-pool/common-regulation'"))
assert.match(routes, /CommonRegulationPage.vue/)
assert.match(
  pageSource,
  /v-show="regulationWorkspaceTab === 'common'"[\s\S]*data-qa-regulation-common-workspace/,
  'The common regulation workspace must render independently of DCC project selection.'
)
assert.doesNotMatch(
  pageSource,
  /data-qa-regulation-common-empty/,
  'The independent common regulation workspace must not require a DCC project empty state.'
)
assert.match(
  pageSource,
  /data-qa-regulation-common-panel/,
  'QA regulation common tab must render a stable content panel.'
)
assert.match(
  pageSource,
  /data-qa-common-detail-tabs[\s\S]*label="总览"[\s\S]*label="检验项目"[\s\S]*label="版本记录"/,
  'The independent common regulation panel must use the QA-style detail tabs.'
)
for (const selector of [
  'data-qa-common-set-switch',
  'data-qa-common-overview-summary',
  'data-qa-common-overview-info',
  'data-qa-common-overview-note',
  'data-qa-common-set-version-detail',
  'data-qa-common-set-member-detail',
  'data-qa-regulation-common-word-import'
]) {
  assert.ok(pageSource.includes(selector), `Independent common regulation page missing ${selector}`)
}
assert.doesNotMatch(
  pageSource,
  /data-qa-common-set-standard-list|table-key="mes\.qa\.common-regulation-set\.main"/,
  'The independent common overview must not keep the old table-based set list.'
)

console.log('PASS independent common regulation tab static contract')
