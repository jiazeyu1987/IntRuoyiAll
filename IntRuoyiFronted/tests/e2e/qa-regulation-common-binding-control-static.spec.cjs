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
  /<el-tab-pane label="关联" name="association"/,
  'QA association tab must remain accessible from the 关联 tab.'
)
for (const selector of [
  'data-qa-regulation-common-binding-list',
  'data-qa-regulation-common-binding-add',
  'data-qa-regulation-common-binding-remove',
  'data-qa-regulation-common-binding-version-select'
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
const buildPayload = pageSource.slice(pageSource.indexOf('const buildQaRegulationSavePayload ='), pageSource.indexOf('const qaRegulationPublishChecks ='))
assert.match(buildPayload, /commonRegulationVersionIds: commonRegulationBindingRows\.value\.map/)
assert.match(publish, /payload = buildQaRegulationSavePayload\(\)/)
const saveDraft = pageSource.slice(pageSource.indexOf('const previewQaRegulationDraft ='), pageSource.indexOf('const runQaPublishPrecheck ='))
assert.match(saveDraft, /payload = buildQaRegulationSavePayload\(\)/)
assert.doesNotMatch(
  pageSource,
  /通用检验规程绑定接口尚未接入|接口待接入/,
  'QA association tab common binding control must not retain the obsolete pending-interface state.'
)

console.log('PASS QA association tab common binding control static contract')
