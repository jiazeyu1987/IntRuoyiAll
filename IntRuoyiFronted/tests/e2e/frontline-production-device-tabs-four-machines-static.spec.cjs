const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const panelPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const deviceTabsStart = panel.indexOf('class="frontline-production-device-tabs device-tabs"')
assert.ok(deviceTabsStart >= 0, 'production device tabs must exist.')
const deviceTabsEnd = panel.indexOf('</div>', deviceTabsStart)
assert.ok(deviceTabsEnd > deviceTabsStart, 'production device tabs block must close.')
const deviceTabsBlock = panel.slice(deviceTabsStart, deviceTabsEnd)

assert.match(
  deviceTabsBlock,
  /v-for="device in visibleDeviceCards"/,
  'device tabs must iterate the full visible device collection.'
)
assert.match(
  deviceTabsBlock,
  /'--frontline-device-tab-count': visibleDeviceCards\.length/,
  'device tabs must expose the full visible device count to CSS.'
)

assert.match(
  panel,
  /const visibleDeviceCards = computed\(\(\) => configuredDeviceCards\.value\)/,
  'visible devices must be the full configured runtime device collection.'
)
assert.doesNotMatch(
  panel,
  /configuredDeviceCards\.value\.slice\(0,\s*3\)/,
  'visible devices must not be truncated to the first three runtime devices.'
)

const deviceTabsStyleStart = panel.indexOf('.frontline-production-device-tabs {')
assert.ok(deviceTabsStyleStart >= 0, 'production device tabs styles must exist.')
const deviceTabsStyleEnd = panel.indexOf('\n}', deviceTabsStyleStart)
assert.ok(deviceTabsStyleEnd > deviceTabsStyleStart, 'production device tabs style block must close.')
const deviceTabsStyleBlock = panel.slice(deviceTabsStyleStart, deviceTabsStyleEnd + 2)

assert.doesNotMatch(
  deviceTabsStyleBlock,
  /grid-template-columns:\s*repeat\(3,/,
  'device tabs CSS must not hard-code three columns, otherwise A03274 is clipped on 单包装.'
)
assert.match(
  deviceTabsStyleBlock,
  /grid-template-columns:\s*repeat\(var\(--frontline-device-tab-count,\s*1\),\s*minmax\(0,\s*1fr\)\)/,
  'device tabs CSS must allocate one column per visible device so four 单包装 machines fit in the first row.'
)

console.log('PASS: frontline production device tabs render four formal machines')
