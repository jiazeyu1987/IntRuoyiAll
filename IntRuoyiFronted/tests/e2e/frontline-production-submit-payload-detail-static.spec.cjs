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
assert.match(feedbackApi, /selectedDevices\??:\s*ProFrontlineSelectedDeviceReqVO\[\]/, 'feedback payload must include selectedDevices snapshots')
assert.match(feedbackApi, /deviceParameterReadings\??:\s*ProFrontlineDeviceParameterReadingReqVO\[\]/, 'feedback payload must include deviceParameterReadings')

assert.match(panel, /buildProductionLossDetailsPayload/, 'panel must build structured lossDetails from all configured defect reasons')
assert.match(panel, /lossDetails:\s*buildProductionLossDetailsPayload\(\)/, 'formal submit feedbackPayload must send lossDetails')
assert.match(panel, /selectedDevices:\s*buildProductionSelectedDevicesPayload\(\)/, 'formal submit feedbackPayload must send selectedDevices')
assert.match(panel, /materialDetails,[\s\S]*lossDetails:[\s\S]*selectedDevices:[\s\S]*deviceParameterReadings:/, 'material detail payload must carry loss, selected devices, and parameter readings together')
assert.match(panel, /rawPayload:[\s\S]*buildProductionStructuredRawPayload\([\s\S]*materialDetails/, 'formal submit rawPayload must be built from the structured production payload')
assert.match(panel, /selectedDevices:\s*buildProductionSelectedDevicesPayload\(\)/, 'structured raw payload must send selectedDevices')
assert.match(panel, /deviceParameterReadings:\s*buildProductionDeviceParameterReadingsPayload\(\)/, 'formal submit feedbackPayload must send deviceParameterReadings')
assert.match(panel, /selectedProductionDeviceKeys\.value/, 'payload must be based on the selected production device keys')
assert.doesNotMatch(
  panel,
  /selectedDevice:\s*buildProductionSelectedDevicePayload\(\)/,
  'formal submit must not reintroduce the legacy single selectedDevice payload'
)
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
