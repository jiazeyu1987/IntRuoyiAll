const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(path.join(process.cwd(), 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8').replace(/\r\n/g, '\n')
for (const selector of [
  '.frontline-production-stage .frontline-production-fullscreen-toggle',
  '.frontline-production-stage .frontline-production-reset-button,\n.frontline-production-stage .frontline-production-submit-button'
]) {
  const start = source.indexOf(`${selector} {`)
  assert.ok(start >= 0, `${selector}: scaled action styles missing`)
  const block = source.slice(start, source.indexOf('}', start) + 1)
  assert.match(block, /font-size:\s*var\(--frontline-production-/, 'Keep the existing visible action font size')
  assert.match(block, /white-space:\s*nowrap;/, 'Action text must not wrap, enlarge the header, and cover material selection')
}
console.log('PASS: scaled production action labels preserve font size without wrapping')
