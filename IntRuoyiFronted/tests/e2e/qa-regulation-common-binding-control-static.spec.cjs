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
  /data-qa-regulation-common-binding-control/,
  'QA common tab must expose the common regulation binding control.'
)
assert.match(
  pageSource,
  /通用检验规程引用/,
  'QA common tab must label the binding control as 通用检验规程引用.'
)
for (const selector of [
  'data-qa-regulation-common-binding-current',
  'data-qa-regulation-common-binding-status',
  'data-qa-regulation-common-binding-scope',
  'data-qa-regulation-common-binding-version',
  'data-qa-regulation-common-binding-view',
  'data-qa-regulation-common-binding-change',
  'data-qa-regulation-common-binding-disable'
]) {
  assert.ok(pageSource.includes(selector), `QA common binding control missing ${selector}`)
}
assert.match(
  pageSource,
  /通用检验规程绑定接口尚未接入/,
  'QA common binding control must fail fast with a pending backend API message.'
)
assert.doesNotMatch(
  pageSource,
  /message\.success\(['"`]绑定成功/,
  'QA common binding control must not fake a successful binding save.'
)
assert.match(
  realE2eSource,
  /data-qa-regulation-common-binding-control/,
  'Real QA regulation E2E must assert the common binding control.'
)

console.log('PASS QA regulation common binding control static contract')
