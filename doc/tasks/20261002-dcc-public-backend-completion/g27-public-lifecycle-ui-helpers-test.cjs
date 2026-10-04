const assert = require('node:assert/strict')
const helpers = require('./g27-public-lifecycle-ui-helpers.cjs')
const punctuation = ['+', '(', ')', '[', ']', '.', '\\', '^', '$', '*', '?', '{', '}', '|', '中文（账号）', '含 空格+%.pdf']
let cases = 0
for (const label of punctuation) {
  const calls = []
  const scope = { locator(selector) {
    calls.push(selector)
    return { filter(options) {
      assert.ok(options.hasText instanceof RegExp)
      assert.ok(options.hasText.test(label), `literal label must match ${label}`)
      assert.ok(options.hasText.test(`${label}：`), 'optional Chinese colon allowed')
      assert.equal(options.hasText.test(`prefix${label}`), false)
      assert.equal(options.hasText.test(`${label}suffix`), false)
      return { locator(selector) { calls.push(selector); return 'FORM_ITEM' } }
    } }
  } }
  assert.equal(helpers.item(scope, label), 'FORM_ITEM')
  assert.deepEqual(calls, ['.el-form-item__label', '..'])
  cases++
}
console.log(`PASS ${cases} pure offline exact-label/relative-parent cases; actual browser/API/DB actions=0`)
