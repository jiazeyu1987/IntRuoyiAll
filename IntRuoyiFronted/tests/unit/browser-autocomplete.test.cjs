const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { chromium } = require('playwright')

const source = fs.readFileSync(
  path.join(__dirname, '../../src/plugins/browserAutocomplete.ts'), 'utf8'
)
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

test('global autocomplete policy covers existing and dynamic fields without changing form data', async (t) => {
  const browser = await chromium.launch({ headless: true })
  t.after(() => browser.close())
  const page = await browser.newPage()
  await page.setContent(`
    <form autocomplete="on">
      <input name="username" autocomplete="username" value="operator">
      <input name="quantity" type="number" value="12" min="0" step="1" required>
      <input name="credential" type="password" autocomplete="current-password">
      <textarea name="notes">batch notes</textarea>
      <select name="medium"><option value="water">Water</option></select>
    </form>
  `)
  await page.evaluate((code) => {
    const exports = {}
    new Function('exports', code)(exports)
    window.stopAutocomplete = exports.setupBrowserAutocomplete()
  }, compiled)

  const settings = await page.locator('form, input, textarea, select').evaluateAll(
    fields => fields.map(field => field.getAttribute('autocomplete'))
  )
  assert.deepEqual(settings, ['off', 'off', 'off', 'new-password', 'off', 'off'])
  await page.locator('[name="quantity"]').fill('23')
  await page.locator('[name="notes"]').fill('updated notes')
  assert.deepEqual(await page.locator('form').evaluate(form => ({
    data: Object.fromEntries(new FormData(form)), valid: form.checkValidity()
  })), {
    data: { username: 'operator', quantity: '23', credential: '', notes: 'updated notes', medium: 'water' },
    valid: true
  })

  // Teleported dialogs and third-party components insert native fields into body.
  await page.evaluate(() => {
    const dialog = document.createElement('section')
    dialog.innerHTML = '<input id="dynamic" autocomplete="on"><textarea id="dynamic-notes"></textarea>'
    document.body.append(dialog)
    dialog.querySelector('input').focus()
    if (document.activeElement.autocomplete !== 'off') throw new Error('focus must disable immediately')
  })
  assert.equal(await page.locator('#dynamic').getAttribute('autocomplete'), 'off')
  assert.equal(await page.locator('#dynamic-notes').getAttribute('autocomplete'), 'off')

  await page.evaluate(() => {
    document.querySelector('#dynamic').setAttribute('autocomplete', 'username')
    document.querySelector('[name="credential"]').type = 'text'
  })
  assert.equal(await page.locator('#dynamic').getAttribute('autocomplete'), 'off')
  assert.equal(await page.locator('[name="credential"]').getAttribute('autocomplete'), 'new-password')
  await page.evaluate(() => { document.querySelector('#dynamic').type = 'password' })
  assert.equal(await page.locator('#dynamic').getAttribute('autocomplete'), 'new-password')

  // HMR teardown releases the observer and focus listener.
  await page.evaluate(() => {
    window.stopAutocomplete()
    document.querySelector('#dynamic').autocomplete = 'on'
    document.querySelector('#dynamic').focus()
  })
  assert.equal(await page.locator('#dynamic').getAttribute('autocomplete'), 'on')
})
