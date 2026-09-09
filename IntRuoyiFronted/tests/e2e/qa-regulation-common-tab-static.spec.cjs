const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const pagePath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const realE2ePath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/tests/e2e/qa-regulation-dcc-status-real.e2e.cjs'
)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const realE2eSource = fs.readFileSync(realE2ePath, 'utf8')

assert.match(
  pageSource,
  /<el-tab-pane\s+label="通用检验规程"\s+name="common"/,
  'QA regulation page must expose a 通用检验规程 tab.'
)
assert.match(
  pageSource,
  /data-qa-regulation-common-tab/,
  'QA regulation common tab must have a stable tab selector.'
)
assert.match(
  pageSource,
  /v-if="!selectedDccProjectCode"[\s\S]*<el-tab-pane\s+label="通用检验规程"\s+name="common"/,
  'QA regulation page must expose the 通用检验规程 tab before a DCC project is selected.'
)
assert.match(
  pageSource,
  /data-qa-regulation-common-empty/,
  'QA regulation common tab must explain that a DCC project is required before loading common regulation data.'
)
assert.match(
  pageSource,
  /data-qa-regulation-common-panel/,
  'QA regulation common tab must render a stable content panel.'
)
assert.match(
  pageSource,
  /当前通用规程主档/,
  'QA regulation common panel must explain the common regulation master record.'
)
for (const selector of [
  'data-qa-regulation-common-code',
  'data-qa-regulation-common-name',
  'data-qa-regulation-common-dcc-project',
  'data-qa-regulation-common-version',
  'data-qa-regulation-common-status'
]) {
  assert.ok(pageSource.includes(selector), `QA regulation common panel missing ${selector}`)
}
assert.match(
  realE2eSource,
  /getByRole\('tab', \{ name: '通用检验规程' \}\)/,
  'Real QA regulation E2E must click the 通用检验规程 tab.'
)
assert.match(
  realE2eSource,
  /data-qa-regulation-common-panel/,
  'Real QA regulation E2E must assert the common tab panel.'
)

console.log('PASS QA regulation common tab static contract')
