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
  /data-qa-regulation-common-binding-control/,
  'QA overview must expose the product-to-common-regulation binding control.'
)
assert.match(
  pageSource,
  /关联通用检验规程/,
  'QA overview must label the binding control as 关联通用检验规程.'
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
  assert.ok(pageSource.includes(selector), `QA overview common binding control missing ${selector}`)
}
assert.match(
  pageSource,
  /QcTemplateApi\.bindCommonRegulationVersion/,
  'QA overview common binding control must use the formal binding API.'
)
assert.doesNotMatch(
  pageSource,
  /通用检验规程绑定接口尚未接入|接口待接入/,
  'QA overview common binding control must not retain the obsolete pending-interface state.'
)

console.log('PASS QA overview common binding control static contract')
