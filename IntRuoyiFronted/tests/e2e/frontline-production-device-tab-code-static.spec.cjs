const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

function extractComputedBlock(source, marker) {
  const start = source.indexOf(marker)
  assert.ok(start >= 0, `${marker} must exist.`)
  const end = source.indexOf('\n)\n', start)
  assert.ok(end > start, `${marker} computed block must close.`)
  return source.slice(start, end + 3)
}

function extractFunctionBlock(source, marker) {
  const start = source.indexOf(marker)
  assert.ok(start >= 0, `${marker} must exist.`)
  const end = source.indexOf('\n}', start)
  assert.ok(end > start, `${marker} function block must close.`)
  return source.slice(start, end + 2)
}

const deviceSectionStart = panel.indexOf(
  'class="frontline-work-panel panel device-panel frontline-production-device-panel"'
)
assert.ok(deviceSectionStart >= 0, 'production device panel must exist.')
const deviceSectionEnd = panel.indexOf('</section>', deviceSectionStart)
assert.ok(deviceSectionEnd > deviceSectionStart, 'production device panel section must close.')
const deviceSection = panel.slice(deviceSectionStart, deviceSectionEnd)

assert.match(
  deviceSection,
  /{{\s*device\.label\s*}}/,
  'device tab should render the formal display label.'
)

const configuredBlock = extractComputedBlock(panel, 'const configuredDeviceCards = computed<ProductionDeviceCard[]>')
const labelResolverBlock = extractFunctionBlock(panel, 'const resolveProductionDeviceTabLabel =')
assert.match(
  labelResolverBlock,
  /deviceCode\s*=\s*device\.deviceCode\?\.trim\(\)/,
  'device tab label resolver must read the formal deviceCode field.'
)
assert.match(
  labelResolverBlock,
  /throw new Error\('当前设备缺少正式设备编号，不能渲染填设备卡片'\)/,
  'device tab label resolver must fail fast when the formal device code is missing.'
)
assert.doesNotMatch(
  labelResolverBlock,
  /deviceName/,
  'device tab label resolver must not read deviceName.'
)
assert.match(
  configuredBlock,
  /deviceCode:\s*device\.deviceCode/,
  'device card must preserve the formal deviceCode field.'
)
assert.match(
  configuredBlock,
  /deviceName:\s*device\.deviceName/,
  'device card must preserve the formal deviceName field.'
)
assert.match(
  configuredBlock,
  /label:\s*resolveProductionDeviceTabLabel\(device\)/,
  'device tab label must read the formal deviceCode field so B09393 renders in the red-box card.'
)
assert.doesNotMatch(
  configuredBlock,
  /label:\s*device\.deviceName\s*\|\|\s*device\.deviceCode/,
  'device tab label must not prefer deviceName over deviceCode.'
)

const selectedDevicePayloadBlock = extractFunctionBlock(
  panel,
  'const buildProductionSelectedDevicePayload = ()'
)
assert.match(
  selectedDevicePayloadBlock,
  /deviceId:\s*device\.deviceId/,
  'production submission payload must retain selected deviceId.'
)
assert.match(
  selectedDevicePayloadBlock,
  /deviceCode:\s*device\.deviceCode/,
  'production submission payload must retain selected deviceCode.'
)
assert.match(
  selectedDevicePayloadBlock,
  /deviceName:\s*device\.deviceName/,
  'production submission payload must retain selected deviceName.'
)

console.log('PASS: frontline production device tab displays device code')
