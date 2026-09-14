const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const page = readUtf8('src/views/mes/pro/processpool/QaRegulationPage.vue')

const functionMatch = page.match(
  /const buildQaRegulationItemEquipmentOptions = \(\n[\s\S]*?\n\}/
)

assert.ok(functionMatch, 'QA regulation equipment option payload builder must exist.')

const functionBody = functionMatch[0]

assert.match(
  functionBody,
  /settings:\s*\{\s*publishing\?:\s*boolean\s*\}\s*=\s*\{\}/,
  'Equipment option payload builder must name publishing settings separately from option rows.'
)
assert.match(
  functionBody,
  /const optionRows = getQaRegulationItemEquipmentOptions\(item\)/,
  'Equipment option rows must use an optionRows variable, not shadow publishing settings.'
)
assert.match(
  functionBody,
  /const publishing = Boolean\(settings\.publishing\)/,
  'Publishing mode must read from the settings object.'
)
assert.match(
  functionBody,
  /optionRows\.filter\(/,
  'Publishing filter must be applied to the equipment option row array.'
)
assert.doesNotMatch(
  functionBody,
  /const options = getQaRegulationItemEquipmentOptions\(item\)/,
  'Equipment option row array must not redeclare the settings parameter name.'
)

console.log('qa-regulation-equipment-options-static PASS')
