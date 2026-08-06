const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const panel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApi = readUtf8('src/api/mes/pro/feedback/index.ts')

assert.match(feedbackApi, /interface\s+ProFrontlineLossDetailReqVO/, 'frontline API must type each loss reason quantity')
assert.match(feedbackApi, /interface\s+ProFrontlineSelectedDeviceReqVO/, 'frontline API must type the selected device snapshot')
assert.match(feedbackApi, /interface\s+ProFrontlineDeviceParameterReadingReqVO/, 'frontline API must type selected device parameter readings')
assert.match(feedbackApi, /lossDetails\??:\s*ProFrontlineLossDetailReqVO\[\]/, 'feedback payload must include structured lossDetails')
assert.match(feedbackApi, /selectedDevice\??:\s*ProFrontlineSelectedDeviceReqVO/, 'feedback payload must include selectedDevice snapshot')
assert.match(feedbackApi, /deviceParameterReadings\??:\s*ProFrontlineDeviceParameterReadingReqVO\[\]/, 'feedback payload must include deviceParameterReadings')

assert.match(panel, /buildProductionLossDetailsPayload/, 'panel must build structured lossDetails from all configured defect reasons')
assert.match(panel, /lossDetails:\s*buildProductionLossDetailsPayload\(\)/, 'formal submit feedbackPayload must send lossDetails')
assert.match(panel, /selectedDevice:\s*buildProductionSelectedDevicePayload\(\)/, 'formal submit feedbackPayload must send selectedDevice')
assert.match(panel, /deviceParameterReadings:\s*buildProductionDeviceParameterReadingsPayload\(\)/, 'formal submit feedbackPayload must send deviceParameterReadings')
assert.match(panel, /activeProductionDevice/, 'payload must be based on the selected active production device')
assert.doesNotMatch(
  panel,
  /const\s+equipmentParameters\s*=\s*Object\.fromEntries\(\s*visibleDeviceCards\.value\.map/,
  'formal submit must not submit every visible device as the selected equipment payload'
)
assert.doesNotMatch(
  panel,
  /lossReasonId:\s*selectedLossReasonId\.value/,
  'formal submit must not collapse multiple loss reasons into a single lossReasonId'
)

console.log('PASS: frontline production submit payload carries detailed loss/device parameters')
