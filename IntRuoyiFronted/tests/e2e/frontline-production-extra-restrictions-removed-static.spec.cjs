const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const readContextStart = source.indexOf('const readFrontlineFormalSubmitContext = ()')
assert.ok(readContextStart >= 0, 'formal submit context builder must exist.')
const readContextEnd = source.indexOf('const assertFrontlineFormalSubmitContext', readContextStart)
assert.ok(readContextEnd > readContextStart, 'formal submit context builder must end before validator.')
const readContextBlock = source.slice(readContextStart, readContextEnd)

assert.match(
  readContextBlock,
  /deviceId:\s*activeProductionDevice\.value\?\.key\s*\?\s*Number\(activeProductionDevice\.value\.key\)\s*:\s*undefined/,
  'No-device production submit must not fall back to selectedProcess.deviceId.'
)
assert.doesNotMatch(
  readContextBlock,
  /:\s*selectedProcess\?\.deviceId/,
  'Workstation candidate device id must not force a selectedDevice when runtime config has no device.'
)

assert.doesNotMatch(
  source,
  /请输入当前登录账号的电子签名密码/,
  'Production submit copy must not ask for the current login account signature password.'
)
assert.match(
  source,
  /请输入所选员工的电子签名密码/,
  'Production submit copy must ask for the selected employee signature password.'
)

console.log('PASS: frontline production extra restrictions removed static contract')
