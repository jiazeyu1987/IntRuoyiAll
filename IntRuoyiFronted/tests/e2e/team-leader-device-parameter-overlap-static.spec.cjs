const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const extractColumn = (prop) => {
  const propIndex = page.indexOf(`prop="${prop}"`)
  const start = page.lastIndexOf('<el-table-column', propIndex)
  const end = page.indexOf('</el-table-column>', propIndex)
  assert.ok(propIndex >= 0 && start >= 0 && end > propIndex, `${prop} column must be locatable.`)
  return page.slice(start, end)
}

const extractStyleRule = (selector) => {
  const start = page.indexOf(selector)
  const end = page.indexOf('\n}', start)
  assert.ok(start >= 0 && end > start, `${selector} style rule must be locatable.`)
  return page.slice(start, end + 2)
}

for (const prop of ['parameterSnapshot', 'deviceParameterReadings']) {
  const column = extractColumn(prop)
  assert.match(
    column,
    /:show-overflow-tooltip="false"/,
    `${prop} must opt out of the table-level single-line tooltip behavior.`
  )
}

const deviceParameterColumn = extractColumn('deviceParameterReadings')
for (const stableBehavior of [
  'data-team-leader-device-parameter-readings',
  'resolveSubmissionParameterItems(row)',
  'data-parameter-status',
  ':aria-label='
]) {
  assert.ok(
    deviceParameterColumn.includes(stableBehavior),
    `device parameter column must preserve ${stableBehavior}.`
  )
}

const listStyle = extractStyleRule('.team-leader-workbench__parameter-list {')
for (const expected of [
  /min-width:\s*0;/,
  /white-space:\s*normal;/,
  /overflow-wrap:\s*anywhere;/
]) {
  assert.match(listStyle, expected, 'parameter list must wrap within the table cell.')
}

const itemStyle = extractStyleRule('.team-leader-workbench__parameter-item {')
assert.match(
  itemStyle,
  /grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto\s+minmax\(72px,\s*auto\);/,
  'parameter rows must reserve independent grid tracks without letting the name cross into the value.'
)
assert.match(itemStyle, /align-items:\s*start;/, 'wrapped parameter names must align with the first text line.')

const childStyle = extractStyleRule('.team-leader-workbench__parameter-item > * {')
assert.match(childStyle, /min-width:\s*0;/, 'every parameter grid item must be allowed to shrink inside its track.')

const labelStyle = extractStyleRule('.team-leader-workbench__parameter-label {')
assert.match(labelStyle, /overflow-wrap:\s*anywhere;/, 'long parameter names must wrap at the cell boundary.')
assert.match(labelStyle, /word-break:\s*break-word;/, 'long parameter names must not overlap adjacent values.')

console.log('PASS: team leader device parameter rows wrap without overlapping values')
