const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

function extractCssBlock(source, selector) {
  const start = source.indexOf(`${selector} {`)
  assert.ok(start >= 0, `${selector} style block must exist.`)
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
  throw new Error(`${selector} style block must close.`)
}

const devicePanelStart = panel.indexOf(
  'class="frontline-work-panel panel device-panel frontline-production-device-panel"'
)
assert.ok(devicePanelStart >= 0, 'production device panel must exist.')
const devicePanelEnd = panel.indexOf(
  'class="frontline-production-submit-bar bottom"',
  devicePanelStart
)
assert.ok(devicePanelEnd > devicePanelStart, 'production device panel section must close.')
const devicePanel = panel.slice(devicePanelStart, devicePanelEnd)

assert.match(
  panel,
  /type ProductionClearanceConfirmationKey = 'workplace' \| 'validity' \| 'material' \| 'cleaning'/,
  'clearance confirmation keys must be explicit and stable.'
)
assert.match(
  panel,
  /const FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS[\s\S]*key: 'workplace'[\s\S]*label: '清场'[\s\S]*key: 'validity'[\s\S]*label: '效期'[\s\S]*key: 'material'[\s\S]*label: '物料'[\s\S]*key: 'cleaning'[\s\S]*label: '清洁'/,
  'four short labels must be 清场、效期、物料、清洁 in stable order.'
)
for (const label of ['清场', '效期', '物料', '清洁']) {
  assert.ok(label.length <= 4, `visible label must be no longer than four characters: ${label}`)
}
for (const description of [
  '工作场所无上批遗留的产品、文件或与本批产品生产无关的物料、工具',
  '是否在计量效期内',
  '所有的物料转移到指定的区域存放并标识',
  '按《INT/GL/7.5.8-03清场管理制度》、《INT/PD/6.4工作环境控制程序》规程执行清洁设备、工器具及环境'
]) {
  assert.ok(panel.includes(description), `full detail description must be present: ${description}`)
}
assert.match(
  panel,
  /createDefaultProductionClearanceConfirmations[\s\S]*confirmation\.key, true/,
  'clearance confirmation defaults must initialize every switch to true.'
)
assert.match(
  panel,
  /resetProductionClearanceConfirmations[\s\S]*productionClearanceConfirmationDraft\[confirmation\.key\] = true/,
  'reset must restore every clearance confirmation switch to true.'
)
assert.match(
  devicePanel,
  /data-production-clearance-confirmations[\s\S]*v-for="confirmation in FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS"[\s\S]*<input[\s\S]*v-model="productionClearanceConfirmationDraft\[confirmation\.key\]"[\s\S]*type="checkbox"[\s\S]*data-production-clearance-checkbox/,
  'device panel must render four editable checkbox controls bound to the clearance confirmation draft.'
)
assert.doesNotMatch(
  devicePanel,
  /<el-switch[\s\S]*data-production-clearance-confirmations|data-production-clearance-confirmations[\s\S]*<el-switch/,
  'clearance confirmation controls must use checkbox controls, not switch controls.'
)
assert.doesNotMatch(
  devicePanel,
  /v-if="activeProductionDevice && visibleDeviceCards\.length > 0"[\s\S]{0,240}data-production-clearance-confirmations/,
  'clearance confirmation controls must render for every process, including no-device processes.'
)
assert.match(
  devicePanel,
  /data-production-clearance-detail-trigger[\s\S]*@click="openProductionClearanceConfirmationDetail\(confirmation\.key\)"/,
  'each short label/details control must open the full description dialog.'
)
assert.match(
  panel,
  /v-if="activeProductionClearanceConfirmation[^"]*"[\s\S]*data-production-clearance-confirmation-dialog[\s\S]*role="dialog"[\s\S]*activeProductionClearanceConfirmation\.description/,
  'full description dialog must be rendered inside the component fullscreen root.'
)
assert.doesNotMatch(
  panel,
  /data-production-clearance-confirmation-dialog[\s\S]*append-to-body/,
  'clearance detail dialog must not teleport to body in fullscreen mode.'
)
assert.match(
  panel,
  /clearanceConfirmations: buildProductionClearanceConfirmationPayload\(\)/,
  'production raw payload must include clearance confirmation audit data.'
)
assert.match(
  panel,
  /entryContent:[\s\S]*clearanceConfirmations: buildProductionClearanceConfirmationPayload\(\)/,
  'recordbook entry content must include clearance confirmation audit data.'
)

const panelBlock = extractCssBlock(panel, '.frontline-production-device-panel')
for (const token of ['grid-template-rows: 118px minmax(0, 1fr) auto;', 'overflow: hidden;']) {
  assert.ok(panelBlock.includes(token), `device panel must reserve compact clearance switch row: ${token}`)
}

const confirmationBlock = extractCssBlock(panel, '.frontline-production-clearance-confirmations')
for (const token of [
  'display: grid;',
  'grid-template-columns: repeat(2, minmax(0, 1fr));',
  'grid-template-rows: repeat(2, minmax(0, auto));',
  'gap: 10px;',
  'min-height: 0;'
]) {
  assert.ok(confirmationBlock.includes(token), `clearance confirmation row must use compact grid token: ${token}`)
}

const checkboxBlock = extractCssBlock(panel, '.frontline-production-clearance-checkbox')
for (const token of [
  'display: grid;',
  'grid-template-columns: auto auto;',
  'min-height: 54px;',
  'cursor: pointer;'
]) {
  assert.ok(checkboxBlock.includes(token), `clearance checkbox must stay touch-friendly and compact: ${token}`)
}

const dialogBlock = extractCssBlock(panel, '.frontline-production-clearance-confirmation-dialog')
for (const token of [
  'max-height: min(76vh, 680px);',
  'overflow: auto;',
  'width: min(100%, 920px);'
]) {
  assert.ok(dialogBlock.includes(token), `detail dialog must support fullscreen/compact reading token: ${token}`)
}

console.log('PASS: frontline production clearance switches static contract')
