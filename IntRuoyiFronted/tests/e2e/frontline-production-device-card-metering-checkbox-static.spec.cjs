const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

function extractBlock(source, startNeedle, endNeedle) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, startNeedle + ' must exist.')
  const end = source.indexOf(endNeedle, start)
  assert.ok(end > start, startNeedle + ' block must close before ' + endNeedle + '.')
  return source.slice(start, end)
}

function extractCssBlock(source, selector) {
  const start = source.indexOf(selector + ' {')
  assert.ok(start >= 0, selector + ' style block must exist.')
  const open = source.indexOf('{', start)
  let depth = 0
  for (let index = open; index < source.length; index += 1) {
    if (source[index] === '{') {
      depth += 1
    } else if (source[index] === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, index + 1)
      }
    }
  }
  throw new Error(selector + ' style block must close.')
}

const deviceTabsBlock = extractBlock(
  panel,
  'class="frontline-production-device-tabs device-tabs"',
  'class="frontline-production-device-empty device-empty"'
)

assert.match(
  deviceTabsBlock,
  /v-for="device in visibleDeviceCards"/,
  'device card tabs must iterate all visible runtime devices.'
)
assert.match(
  deviceTabsBlock,
  /'--frontline-device-tab-count': visibleDeviceCards\.length/,
  'device card tabs must size columns from the visible device count.'
)
assert.match(
  deviceTabsBlock,
  /class="[^"]*\bdevice-tab-card\b[^"]*"/,
  'each device card must use a two-row card container.'
)
assert.match(
  deviceTabsBlock,
  /class="device-tab-code"[^>]*[\s\S]*\{\{ device\.label \}\}/,
  'the top row must display the formal device code.'
)
assert.match(
  deviceTabsBlock,
  /type="checkbox"[\s\S]*data-frontline-device-metering-validity[\s\S]*在计量效期内/,
  'the bottom row must render the metering-validity checkbox text.'
)
assert.match(
  deviceTabsBlock,
  /:checked="isProductionDeviceMeteringValid\(device\.key\)"/,
  'metering-validity checkboxes must default through the per-device state helper.'
)
assert.match(
  deviceTabsBlock,
  /@change="updateProductionDeviceMeteringValidity\(device\.key, \$event\)"/,
  'metering-validity checkboxes must update only their own device key.'
)

assert.match(
  panel,
  /const visibleDeviceCards = computed\(\(\) => configuredDeviceCards\.value\)/,
  'visible devices must remain the full configured runtime device collection.'
)
assert.doesNotMatch(
  panel,
  /configuredDeviceCards\.value\.slice\(0,\s*3\)/,
  'visible devices must not be truncated to the first three runtime devices.'
)
assert.match(
  panel,
  /const deviceMeteringValidityDraft = reactive<ProductionDeviceMeteringValidityDraft>\(\{\}\)/,
  'per-device metering validity state must be tracked independently from the active tab.'
)
assert.match(
  panel,
  /deviceMeteringValidityDraft\[device\.key\] === undefined[\s\S]*deviceMeteringValidityDraft\[device\.key\] = true/,
  'new device cards must default metering validity to checked.'
)
assert.match(
  panel,
  /const isProductionDeviceMeteringValid = \(deviceKey: string\) =>[\s\S]*deviceMeteringValidityDraft\[deviceKey\] !== false/,
  'unchecked must be the only false state so missing device keys default to checked.'
)
assert.match(
  panel,
  /deviceMeteringValidity: buildProductionDeviceMeteringValidityPayload\(\)/,
  'structured raw payload must preserve per-device metering validity without adding a backend top-level field.'
)
if (panel.includes('FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS')) {
  assert.match(
    panel,
    /type ProductionClearanceConfirmationKey = 'workplace' \| 'validity' \| 'material' \| 'cleaning'[\s\S]*key: 'validity'[\s\S]*label: '效期'/,
    'the bottom global validity confirmation must remain intact when the clearance block exists.'
  )
}

const tabsStyleBlock = extractCssBlock(panel, '.frontline-production-device-tabs')
assert.match(
  tabsStyleBlock,
  /grid-template-columns:\s*repeat\(var\(--frontline-device-tab-count,\s*1\),\s*minmax\(0,\s*1fr\)\)/,
  'device tabs CSS must allocate one column per visible device.'
)
assert.doesNotMatch(
  tabsStyleBlock,
  /grid-template-columns:\s*repeat\(3,/,
  'device tabs CSS must not hard-code three columns.'
)

const cardStyleBlock = extractCssBlock(panel, '.frontline-production-device-card')
for (const token of [
  'grid-template-rows: minmax(0, 1fr) 36px;',
  'height: 110px;',
  'overflow: hidden;'
]) {
  assert.ok(cardStyleBlock.includes(token), 'device card style must include ' + token)
}

console.log('PASS: frontline production device cards expose metering validity checkboxes')
