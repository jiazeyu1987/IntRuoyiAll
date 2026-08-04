const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const detailSource = readSource('src/views/bpm/processInstance/detail/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:bpm-approval-preview-pane:static'],
  'node tests/e2e/bpm-dcc-approval-preview-pane-static.spec.js',
  'package.json must expose the BPM DCC approval preview-pane contract'
)

assert.match(
  detailSource,
  /import ProtectedPdfViewer from '@\/views\/dcc\/controlled-file\/view\/index\.vue'/,
  'BPM DCC approval detail must reuse the formal controlled-file preview component'
)
assert.match(
  detailSource,
  /const showProcessInstanceTechnicalHeader\s*=\s*computed\(\(\) => !isDccControlledFileCustomForm\.value\)/,
  'DCC approval detail must hide the generic process number/print header'
)
assert.match(
  detailSource,
  /<div\s+v-if="showProcessInstanceTechnicalHeader"\s+class="flex">[\s\S]*编号：\{\{\s*id\s*\}\}[\s\S]*handlePrint/,
  'generic process number and print icon must be gated away from DCC approval detail'
)
assert.match(
  detailSource,
  /<el-divider\s+v-if="showProcessInstanceTechnicalHeader"/,
  'the technical divider under the generic process number must also be hidden for DCC approval detail'
)

const dccSummary = extractBetween(
  detailSource,
  'data-testid="bpm-dcc-approval-compact-summary"',
  '<BusinessFormComponent v-else :id="processInstance.businessKey" />',
  'BPM DCC approval compact summary'
)

assert.match(
  dccSummary,
  /data-testid="bpm-dcc-approval-file-preview"[\s\S]*<ProtectedPdfViewer[\s\S]*:controlled-file-id="dccControlledFileBusinessId"/,
  'red-box content area must render the controlled-file preview using the process business key'
)
assert.match(
  dccSummary,
  /class="bpm-dcc-approval-preview"/,
  'DCC approval preview must have a dedicated layout class for the red-box content area'
)
assert.doesNotMatch(
  dccSummary,
  /进入文控审批处理页|需要预览文件、电子签名、通过或拒绝时/,
  'yellow-box DCC handling-entry prompt must not render in the approval detail summary'
)
assert.doesNotMatch(
  dccSummary,
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'preview-pane fix must not introduce mock data, fallback, downgrade, swallowed errors, or default success'
)

assert.match(
  detailSource,
  /<BusinessFormComponent v-else :id="processInstance\.businessKey" \/>/,
  'non-DCC custom forms must still mount their configured business form component'
)

console.log('PASS: BPM DCC approval preview-pane static contract')
