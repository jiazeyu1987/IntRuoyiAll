const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panel = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

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

const productionMainStart = panel.indexOf(
  '<main class="frontline-operator-main frontline-production-main main">'
)
assert.ok(productionMainStart >= 0, 'production main must exist.')
const productionMainEnd = panel.indexOf('</main>', productionMainStart)
assert.ok(productionMainEnd > productionMainStart, 'production main must close.')
const productionMainTemplate = panel.slice(productionMainStart, productionMainEnd)
assert.match(
  productionMainTemplate,
  /class="frontline-production-submit-bar bottom"/,
  'the submit bar must belong to the production main grid.'
)

const screenBlock = extractCssBlock(panel, '.frontline-operator-screen')
const productionScreenRules = screenBlock.slice(0, screenBlock.indexOf('&.is-pqc'))
assert.match(
  productionScreenRules,
  /grid-template-rows:\s*130px\s+minmax\(0,\s*1fr\);/,
  'the production screen must allocate the footer inside the main row.'
)

const mainBlock = extractCssBlock(panel, '.frontline-production-main')
for (const token of [
  'grid-template-rows: minmax(0, 1fr) 126px;',
  'column-gap: 28px;',
  'row-gap: 20px;'
]) {
  assert.ok(mainBlock.includes(token), 'production main must include ' + token)
}

const quantityBlock = extractCssBlock(panel, '.frontline-production-quantity-panel')
assert.match(quantityBlock, /grid-column:\s*1;/, 'the quantity panel must occupy the left column.')
assert.match(quantityBlock, /grid-row:\s*1;/, 'the quantity panel must occupy the upper row.')

const devicePanelBlock = extractCssBlock(panel, '.frontline-production-device-panel')
assert.match(devicePanelBlock, /grid-column:\s*2;/, 'the device panel must occupy the right column.')
assert.match(devicePanelBlock, /grid-row:\s*1\s*\/\s*3;/, 'the device panel must span both main rows.')
assert.match(
  devicePanelBlock,
  /grid-template-rows:\s*118px\s+minmax\(0,\s*1fr\)\s+auto;/,
  'only the parameter area may absorb the additional device panel height.'
)

const submitBarBlock = extractCssBlock(panel, '.frontline-production-submit-bar')
assert.match(submitBarBlock, /grid-column:\s*1;/, 'the submit bar must remain in the left column.')
assert.match(submitBarBlock, /grid-row:\s*2;/, 'the submit bar must occupy the lower row.')

console.log('PASS: production submit actions stay left while the device panel spans both rows')
