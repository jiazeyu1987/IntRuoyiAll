const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const pagePath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.match(
  pageSource,
  /data-qa-regulation-workspace-tabs[\s\S]*label="QA检验规程"[\s\S]*label="通用检验规程"/,
  'The page must expose QA and common inspection regulations as separate first-level tabs.'
)
assert.match(
  pageSource,
  /data-qa-regulation-common-workspace-tab/,
  'The independent common regulation tab must have a stable selector.'
)
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
  'data-qa-common-set-standard-list',
  'data-qa-common-set-version-detail',
  'data-qa-common-set-member-detail',
  'data-qa-regulation-common-word-import'
]) {
  assert.ok(pageSource.includes(selector), `Independent common regulation page missing ${selector}`)
}

console.log('PASS independent common regulation tab static contract')
