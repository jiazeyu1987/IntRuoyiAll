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

assert.doesNotMatch(deviceSection, /<div class="panel-title">填设备<\/div>/, 'device panel title must stay removed from the compact production layout.')
assert.match(deviceSection, /class="frontline-production-device-param device-param"/, 'device parameter rows must remain rendered.')
assert.match(deviceSection, /class="device-param-label"/, 'device parameter labels must remain rendered.')
assert.match(deviceSection, /class="device-num"/, 'device parameter step buttons must remain rendered.')
assert.match(deviceSection, /class="device-value"/, 'device parameter input values must remain rendered.')
assert.match(deviceSection, /class="device-unit"/, 'device parameter units must remain rendered.')
assert.match(
  deviceSection,
  /class="frontline-production-device-standard-text"/,
  'text standard parameters must keep the dedicated read-only value row.'
)

const currentBlock = extractCssBlock(panel, '.frontline-production-device-current')
for (const token of ['gap: 10px;', 'padding: 14px;', 'overflow: auto;']) {
  assert.ok(currentBlock.includes(token), `device parameter panel must use compact container token: ${token}`)
}
for (const oldToken of ['gap: 24px;', 'padding: 26px;', 'gap: 14px;', 'padding: 18px;']) {
  assert.ok(!currentBlock.includes(oldToken), `device parameter panel must not keep old loose container token: ${oldToken}`)
}

const parameterBlock = extractCssBlock(panel, '.frontline-production-device-param')
for (const token of [
  'grid-template-columns: 224px 70px minmax(0, 1fr) 70px 58px;',
  'gap: 10px;',
  'font-size: 28px;',
  'line-height: 1.1;',
  'white-space: nowrap;',
  'font-size: 18px;',
  'height: 72px;',
  'font-size: 38px;',
  'font-size: 40px;',
  'font-size: 26px;',
  'padding: 14px 18px;',
  'font-size: 28px;',
  'line-height: 1.25;'
]) {
  assert.ok(parameterBlock.includes(token), `device parameter rows must use compact token: ${token}`)
}
for (const oldToken of [
  'grid-template-columns: 126px 70px minmax(0, 1fr) 70px 58px;',
  'grid-template-columns: 150px 82px minmax(0, 1fr) 82px 78px;',
  'gap: 14px;',
  'height: 96px;',
  'font-size: 52px;',
  'padding: 22px 26px;'
]) {
  assert.ok(!parameterBlock.includes(oldToken), `device parameter rows must not keep old loose token: ${oldToken}`)
}

console.log('PASS: frontline production device row density static contract')
