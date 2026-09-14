const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

function extractBalancedBlock(source, startNeedle) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, startNeedle + ' block must exist.')
  const open = source.indexOf('{', start)
  assert.ok(open > start, startNeedle + ' block must open.')
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
  throw new Error(startNeedle + ' block must close.')
}

function extractSection(source, classNeedle) {
  const start = source.indexOf(classNeedle)
  assert.ok(start >= 0, classNeedle + ' section must exist.')
  const end = source.indexOf('class="frontline-production-submit-bar bottom"', start)
  assert.ok(end > start, classNeedle + ' section must close.')
  return source.slice(start, end)
}

const quantitySection = extractSection(
  panel,
  'class="frontline-work-panel panel quantity-panel frontline-production-quantity-panel"'
)
const deviceSection = extractSection(
  panel,
  'class="frontline-work-panel panel device-panel frontline-production-device-panel"'
)

assert.doesNotMatch(
  quantitySection,
  /<div class="panel-title">填数量<\/div>/,
  'red-box quantity panel title must be removed from the production layout.'
)
assert.doesNotMatch(
  deviceSection,
  /<div class="panel-title">填设备<\/div>/,
  'red-box device panel title must be removed from the production layout.'
)

assert.match(
  deviceSection,
  /class="frontline-production-clearance-confirmations"[\s\S]*v-for="confirmation in FRONTLINE_PRODUCTION_CLEARANCE_CONFIRMATIONS"/,
  'clearance confirmations must still render from the formal confirmation list.'
)

const devicePanelBlock = extractBalancedBlock(panel, '.frontline-production-device-panel')
assert.ok(
  devicePanelBlock.includes('grid-template-rows: 118px minmax(0, 1fr) auto;'),
  'device panel grid rows must remove the old title row while preserving tabs, parameters, and clearance rows.'
)
assert.ok(
  !devicePanelBlock.includes('grid-template-rows: auto 84px minmax(0, 1fr) auto;'),
  'device panel grid must not reserve the removed title row.'
)

const clearanceBlock = extractBalancedBlock(panel, '.frontline-production-clearance-confirmations')
assert.ok(
  clearanceBlock.includes('grid-template-columns: repeat(2, minmax(0, 1fr));'),
  'blue-box clearance confirmations must render as two columns.'
)
assert.ok(
  clearanceBlock.includes('grid-template-rows: repeat(2, minmax(0, auto));'),
  'blue-box clearance confirmations must reserve two rows for four items.'
)
assert.ok(
  !clearanceBlock.includes('grid-template-columns: repeat(4, minmax(0, 1fr));'),
  'blue-box clearance confirmations must not remain a single row of four items.'
)

console.log('PASS: frontline production header removal and clearance layout static contract')
