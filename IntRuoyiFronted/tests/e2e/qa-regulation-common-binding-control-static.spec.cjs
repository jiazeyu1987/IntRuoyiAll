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
  'QA association tab must expose the product-to-common-regulation binding control.'
)
assert.match(
  pageSource,
  /关联通用检验规程/,
  'QA association tab must label the binding control as 关联通用检验规程.'
)
for (const selector of [
  'data-qa-regulation-common-binding-list',
  'data-qa-regulation-common-binding-add',
  'data-qa-regulation-common-binding-remove',
  'data-qa-regulation-common-binding-status',
  'data-qa-regulation-common-binding-current',
  'data-qa-regulation-common-binding-version-select',
  'data-qa-regulation-common-binding-publish-notice'
]) {
  assert.ok(pageSource.includes(selector), `QA association tab common binding control missing ${selector}`)
}
assert.doesNotMatch(
  pageSource,
  /QcTemplateApi\.(saveCommonRegulationBindingList|bindCommonRegulationVersion|unbindCommonRegulation)/,
  'Association edits must never call an immediate-persistence API.'
)
assert.doesNotMatch(pageSource, /data-qa-regulation-common-binding-(switch|view|change|disable)/)
const publish = pageSource.slice(pageSource.indexOf('const runQaPublishPrecheck ='))
assert.match(publish, /payload\.commonRegulationVersionIds = commonRegulationBindingRows\.value\.map/)
const saveDraft = pageSource.slice(pageSource.indexOf('const previewQaRegulationDraft ='), pageSource.indexOf('const runQaPublishPrecheck ='))
assert.doesNotMatch(saveDraft, /payload\.commonRegulationVersionIds/)
assert.doesNotMatch(
  pageSource,
  /通用检验规程绑定接口尚未接入|接口待接入/,
  'QA association tab common binding control must not retain the obsolete pending-interface state.'
)

console.log('PASS QA association tab common binding control static contract')
