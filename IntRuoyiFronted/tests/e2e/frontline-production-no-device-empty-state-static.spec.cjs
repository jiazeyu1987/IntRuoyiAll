const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8')

const sectionStart = panel.indexOf('class="frontline-work-panel panel device-panel frontline-production-device-panel"')
assert.ok(sectionStart >= 0, 'production device panel must exist.')
const sectionEnd = panel.indexOf('</section>', sectionStart)
assert.ok(sectionEnd > sectionStart, 'production device panel section must close.')
const devicePanel = panel.slice(sectionStart, sectionEnd)

assert.match(devicePanel, /<div class="panel-title">填设备<\/div>/, 'device panel title must remain 填设备.')
assert.match(
  devicePanel,
  /<div\s+v-if="visibleDeviceCards\.length > 0"\s+class="frontline-production-device-tabs device-tabs"/,
  'device tabs must render only when formal visibleDeviceCards is non-empty.'
)
assert.match(
  devicePanel,
  /<div\s+v-else\s+class="frontline-production-device-empty device-empty"\s+data-frontline-production-no-device-empty[\s\S]*?>[\s\S]*无设备[\s\S]*<\/div>/,
  'device panel must show 无设备 empty state when there is no formal device.'
)
assert.match(
  devicePanel,
  /<div\s+v-if="activeProductionDevice && visibleDeviceCards\.length > 0"\s+class="frontline-production-device-current device-current"\s*>/,
  'device parameter inputs must stay behind the active device and non-empty list guard.'
)
assert.doesNotMatch(
  devicePanel,
  /设备\s*[1一]|默认设备|mock/i,
  'empty state must not use default/mock device labels to hide missing device configuration.'
)

const stylesStart = panel.indexOf('.frontline-production-device-current {')
assert.ok(stylesStart >= 0, 'device current styles must exist.')
const stylesEnd = panel.indexOf('.frontline-production-device-param {', stylesStart)
assert.ok(stylesEnd > stylesStart, 'device current styles must be followed by device parameter styles.')
const deviceStyles = panel.slice(stylesStart, stylesEnd)
assert.match(
  deviceStyles,
  /\.frontline-production-device-empty\s*\{/,
  'no-device empty state must have dedicated styling in the device panel style block.'
)
assert.match(
  deviceStyles,
  /font-size:\s*42px/,
  'no-device empty state must be readable at frontline production fullscreen scale.'
)

console.log('PASS: frontline production no-device empty state static contract')
