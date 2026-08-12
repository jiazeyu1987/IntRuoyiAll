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

const cardBlock = extractCssBlock(panel, '.frontline-production-device-card')
assert.doesNotMatch(
  cardBlock,
  /&\.active\s+\.device-tab/,
  'the code header colors must not depend on the active device state.'
)

const codeHeaderBlock = extractCssBlock(panel, '.frontline-production-device-card .device-tab')
assert.match(
  codeHeaderBlock,
  /background:\s*var\(--frontline-dark\);/,
  'every device code header must use the dark background.'
)
assert.match(
  codeHeaderBlock,
  /color:\s*#ffffff;/,
  'every device code header must use white text.'
)

const meteringValidityBlock = extractCssBlock(panel, '.frontline-production-device-metering-validity')
assert.match(
  meteringValidityBlock,
  /grid-template-columns:\s*18px\s+auto;/,
  'the visible checkbox and label must use two centered grid columns.'
)
assert.doesNotMatch(
  meteringValidityBlock,
  /grid-template-columns:\s*18px\s+18px\s+auto;/,
  'the hidden native input must not reserve a third grid column.'
)
assert.match(
  meteringValidityBlock,
  /background:\s*#ffffff;/,
  'every device metering-validity row must use a white background.'
)
assert.match(
  meteringValidityBlock,
  /color:\s*var\(--frontline-ink\);/,
  'every device metering-validity row must keep dark readable text.'
)

console.log('PASS: all frontline production device cards use dark headers and white footers')
