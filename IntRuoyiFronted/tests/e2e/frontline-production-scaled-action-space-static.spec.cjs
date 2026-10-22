const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(path.join(process.cwd(), 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8').replace(/\r\n/g, '\n')
function block(selector) {
  const start = source.indexOf(`${selector} {`)
  assert.ok(start >= 0, `${selector}: production action sizing missing`)
  return source.slice(start, source.indexOf('}', start) + 1)
}
assert.match(block('.frontline-production-stage .frontline-operator-top.is-production'), /grid-template-columns:[\s\S]*max\(240px, calc\(var\(--frontline-production-top-action-font-size, 42px\) \* 3 \+ 24px\)\)/)
assert.match(block('.frontline-production-stage .frontline-production-main'), /grid-template-rows:[\s\S]*max\(126px, calc\(var\(--frontline-production-footer-action-font-size, 54px\) \* 1\.35 \+ 24px\)\)/)
assert.match(block('.frontline-production-stage .frontline-production-submit-bar'), /grid-template-columns:[\s\S]*max\(300px, calc\(var\(--frontline-production-footer-action-font-size, 54px\) \* 2 \+ 24px\)\)/)
console.log('PASS: scaled production actions reserve width and height for their visible text')
