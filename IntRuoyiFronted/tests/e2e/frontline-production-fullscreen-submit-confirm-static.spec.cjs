const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const extractFunctionBlock = (name) => {
  const start = source.indexOf(`const ${name} = async`)
  assert.ok(start >= 0, `missing async function: ${name}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing function body: ${name}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(openIndex + 1, index)
    }
  }
  assert.fail(`unterminated function body: ${name}`)
}

const extractStyleBlock = (selector) => {
  const start = source.indexOf(selector)
  assert.ok(start >= 0, `missing style selector: ${selector}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing style body: ${selector}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(openIndex + 1, index)
    }
  }
  assert.fail(`unterminated style body: ${selector}`)
}

const productionStageIndex = source.indexOf('data-frontline-production-stage')
const confirmDialogIndex = source.indexOf('data-production-submit-confirmation-dialog')
const pickerIndex = source.indexOf('data-pqc-process-picker')

assert.ok(productionStageIndex >= 0, 'production stage must exist.')
assert.ok(confirmDialogIndex >= 0, 'production submit confirmation dialog must exist.')
assert.ok(
  confirmDialogIndex > productionStageIndex && confirmDialogIndex < pickerIndex,
  'production submit confirmation must render inside the fullscreen root, adjacent to the production stage, not under body.'
)
assert.match(
  source,
  /data-production-submit-confirmation-dialog[\s\S]*role="dialog"[\s\S]*aria-modal="true"[\s\S]*确认正式提交/,
  'production submit confirmation must be an accessible in-component dialog.'
)
assert.match(
  source,
  /data-production-submit-confirmation-message[\s\S]*\{\{\s*productionFormalSubmitConfirmationText\s*\}\}/,
  'confirmation dialog must render the same formal submit summary that used to be passed to MessageBox.'
)
assert.match(
  source,
  /data-production-submit-confirm-cancel[\s\S]*@click="cancelProductionFormalSubmitConfirmation"/,
  'confirmation dialog must expose an explicit cancel action.'
)
assert.match(
  source,
  /data-production-submit-confirm-accept[\s\S]*@click="confirmProductionFormalSubmitConfirmation"/,
  'confirmation dialog must expose an explicit confirm action.'
)

const productionSubmitHandler = extractFunctionBlock('handleProductionFormalSubmit')
assert.doesNotMatch(
  productionSubmitHandler,
  /message\.confirm|ElMessageBox/,
  'production fullscreen submit must not use global MessageBox because body-mounted overlays are hidden behind the fullscreen top layer.'
)
assert.match(
  productionSubmitHandler,
  /const confirmed = await requestProductionFormalSubmitConfirmation\(buildProductionFormalSubmitConfirmation\(\)\)[\s\S]*if \(!confirmed\) \{\s*return\s*\}/,
  'production submit must wait for the in-component confirmation and stop cleanly on cancel.'
)
assert.strictEqual(
  (productionSubmitHandler.match(/ProFeedbackApi\.frontlineSubmit\(/g) || []).length,
  1,
  'confirmed production submit must still call the formal submit endpoint exactly once.'
)

assert.match(
  source,
  /const requestProductionFormalSubmitConfirmation = \(confirmationText: string\): Promise<boolean> =>/,
  'component must provide a Promise-based in-component confirmation request.'
)
assert.match(
  source,
  /const cancelProductionFormalSubmitConfirmation = \(\) =>[\s\S]*resolveProductionFormalSubmitConfirmation\(false\)/,
  'cancel action must resolve false instead of throwing or sending a write request.'
)
assert.match(
  source,
  /const confirmProductionFormalSubmitConfirmation = \(\) =>[\s\S]*resolveProductionFormalSubmitConfirmation\(true\)/,
  'confirm action must resolve true before the formal submit request continues.'
)

const modalStyle = extractStyleBlock('.frontline-production-submit-confirmation-modal')
assert.match(
  modalStyle,
  /position:\s*absolute;[\s\S]*inset:\s*0;[\s\S]*z-index:\s*120;/,
  'in-component confirmation overlay must cover the fullscreen production panel.'
)
assert.match(
  extractStyleBlock('.frontline-production-submit-confirmation-dialog'),
  /max-width:\s*860px;[\s\S]*font-size:\s*28px;/,
  'confirmation dialog must be touch-readable on the production fullscreen canvas.'
)

console.log('PASS: frontline production fullscreen submit confirmation static contract')
