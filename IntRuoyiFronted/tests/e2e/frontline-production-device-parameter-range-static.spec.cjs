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

const deviceSectionStart = panel.indexOf(
  'class="frontline-work-panel panel device-panel frontline-production-device-panel"'
)
assert.ok(deviceSectionStart >= 0, 'production device panel must exist.')
const deviceSectionEnd = panel.indexOf('</section>', deviceSectionStart)
assert.ok(deviceSectionEnd > deviceSectionStart, 'production device panel section must close.')
const deviceSection = panel.slice(deviceSectionStart, deviceSectionEnd)

assert.match(
  deviceSection,
  /class="device-param-label"[\s\S]*class="device-param-name"[\s\S]*parameter\.parameterName \|\| parameter\.parameterCode/,
  'device parameter label must keep a dedicated name line driven by the formal parameter name/code.'
)
assert.match(
  deviceSection,
  /data-frontline-device-parameter-range[\s\S]*formatProductionParameterTargetRange\(parameter\)/,
  'numeric device parameters with formal limits must render their target range under the name.'
)
assert.match(
  deviceSection,
  /v-if="!isTextStandardParameter\(parameter\) && formatProductionParameterTargetRange\(parameter\)"/,
  'target range must only appear for non-text parameters with formal lower or upper limits.'
)

const formatterStart = panel.indexOf('const formatProductionParameterTargetRange =')
const formatterEnd = panel.indexOf('const resolveProductionParameterStatus', formatterStart)
assert.ok(formatterStart >= 0 && formatterEnd > formatterStart, 'production parameter range formatter must exist.')
const formatterBlock = panel.slice(formatterStart, formatterEnd)
assert.match(
  formatterBlock,
  /toFiniteProductionParameterNumber\(parameter\.lowerLimit\)[\s\S]*toFiniteProductionParameterNumber\(parameter\.upperLimit\)/,
  'target range formatter must derive display values from formal lowerLimit and upperLimit.'
)
assert.match(
  formatterBlock,
  /lowerLimit === undefined && upperLimit === undefined[\s\S]*return undefined/,
  'target range formatter must not render a placeholder when both formal limits are missing.'
)
for (const token of ['目标范围：', '≥', '≤', 'parameter.unit']) {
  assert.ok(formatterBlock.includes(token), `target range formatter must include ${token}`)
}

const parameterBlock = extractCssBlock(panel, '.frontline-production-device-param')
for (const token of [
  'grid-template-columns: 224px 70px minmax(0, 1fr) 70px 58px;',
  '.device-param-label {',
  '.device-param-name {',
  'white-space: nowrap;',
  '.device-param-range {'
]) {
  assert.ok(parameterBlock.includes(token), `device parameter style must include: ${token}`)
}
assert.ok(
  !parameterBlock.includes('grid-template-columns: 126px 70px minmax(0, 1fr) 70px 58px;'),
  'device parameter name area must not keep the old narrow 126px column.'
)

console.log('PASS: frontline production device parameter target range display contract')
