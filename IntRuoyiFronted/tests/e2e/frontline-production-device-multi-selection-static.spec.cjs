const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/feedback/index.ts'), 'utf8')

assert.match(api, /selectionMode: 'SINGLE' \| 'MULTIPLE'/)
assert.match(api, /selectedDevices\?: ProFrontlineSelectedDeviceReqVO\[\]/)
assert.match(panel, /const selectedProductionDeviceKeys = ref<string\[\]>\(\[\]\)/)
assert.match(panel, /const toggleProductionDeviceSelection = \(device: ProductionDeviceCard\) =>/)
assert.match(panel, /device\.selectionMode === 'SINGLE'[\s\S]*groupDevice\.deviceGroupKey === device\.deviceGroupKey/)
assert.match(panel, /selectedProductionDeviceKeys\.value\.flatMap[\s\S]*buildProductionDeviceParameterReadingsFromDraft/)
assert.match(panel, /selectedDevices: buildProductionSelectedDevicesPayload\(\)/)
assert.doesNotMatch(panel, /selectedDevice: buildProductionSelectedDevicePayload\(\)/)

console.log('PASS: production device single, multiple and none selection contract')
