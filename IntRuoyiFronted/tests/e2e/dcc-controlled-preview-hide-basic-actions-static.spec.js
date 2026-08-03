const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const basicInfoPanel = readSource('src/views/dcc/controlled-file/shared/ControlledFileBasicInfoPanel.vue')

const viewerTemplate = extractBetween(
  detailPage,
  '<ContentWrap v-if="viewerMode">',
  '<template v-else>',
  'DCC controlled-file viewer template'
)
const viewerBasicInfoBlock = extractBetween(
  viewerTemplate,
  '<ControlledFileBasicInfoPanel',
  '/>',
  'viewer basic info panel'
)

for (const forbidden of [
  'show-info-actions',
  ':show-product-recognition=',
  ':show-edit=',
  'edit-button-text="修改"',
  'edit-test-id="dcc-controlled-preview-detail-edit"',
  '@recognize-project-code=',
  '@edit='
]) {
  assert.doesNotMatch(
    viewerBasicInfoBlock,
    new RegExp(forbidden.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `viewer basic info panel must not render operation button entry: ${forbidden}`
  )
}

for (const forbiddenText of [
  'data-testid="dcc-controlled-preview-approval-button"',
  'data-testid="dcc-controlled-preview-distribution-button"',
  'data-testid="dcc-controlled-preview-version-button"',
  'dcc-controlled-preview-detail-edit',
  '识别基础信息'
]) {
  assert.doesNotMatch(
    viewerBasicInfoBlock,
    new RegExp(forbiddenText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `viewer basic info panel must not directly expose hidden button text/test-id: ${forbiddenText}`
  )
}

const normalTemplateStart = detailPage.indexOf('<template v-else>')
assert.notEqual(normalTemplateStart, -1, 'normal detail template missing v-else boundary')
const normalTemplate = detailPage.slice(normalTemplateStart)
const normalBasicInfoBlock = extractBetween(
  normalTemplate,
  '<ControlledFileBasicInfoPanel',
  '/>',
  'normal basic info panel'
)

assert.match(
  normalBasicInfoBlock,
  /:show-product-recognition="canEditMetadata && !!fileDetail"/,
  'normal detail page must keep project-code recognition action when metadata editing is allowed'
)
assert.match(
  normalBasicInfoBlock,
  /@recognize-project-code="handleRecognizeProjectCode"/,
  'normal detail page must keep project-code recognition handler'
)

for (const retained of [
  'showInfoActions',
  'showEdit',
  'showProductRecognition',
  "emit('openApprovalInfo')",
  "emit('openDistributionInfo')",
  "emit('openVersionInfo')",
  "emit('recognizeProjectCode')",
  '识别基础信息'
]) {
  assert.match(
    basicInfoPanel,
    new RegExp(retained.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `shared basic info panel must retain reusable action contract: ${retained}`
  )
}

assert.doesNotMatch(
  viewerBasicInfoBlock,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'viewer action hiding must not introduce mock data, fallback, downgrade, or swallowed errors'
)

console.log('PASS: DCC controlled preview basic action buttons are hidden in viewer mode')
