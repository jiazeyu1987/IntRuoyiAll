const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const extractFunctionBlock = (name) => {
  const start = source.indexOf(`const ${name} =`)
  assert.ok(start >= 0, `missing function: ${name}`)
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
const successDialogIndex = source.indexOf('data-production-submit-success-dialog')
const pqcPickerIndex = source.indexOf('data-pqc-process-picker')

assert.ok(productionStageIndex >= 0, 'production fullscreen stage must exist.')
assert.ok(successDialogIndex >= 0, 'production submit success dialog must exist.')
assert.ok(
  successDialogIndex > productionStageIndex && successDialogIndex < pqcPickerIndex,
  'success dialog must render inside the fullscreen root instead of under body.'
)
assert.match(
  source,
  /data-production-submit-success-dialog[\s\S]*role="dialog"[\s\S]*aria-modal="true"[\s\S]*提交成功/,
  'success result must be an accessible in-component modal.'
)
assert.match(
  source,
  /data-production-submit-success-message[\s\S]*\{\{\s*productionSubmitSuccessText\s*\}\}/,
  'success dialog must identify the employee captured for the completed submission.'
)
assert.match(
  source,
  /icon="ep:circle-check-filled"[\s\S]*:size="96"[\s\S]*frontline-production-submit-success-icon/,
  'success state icon must remain legible on the 1920x1080 operator canvas.'
)
assert.match(
  source,
  /data-production-submit-success-continue[\s\S]*@click="closeProductionSubmitSuccessDialog"[\s\S]*继续报工/,
  'success dialog must expose an explicit continue action.'
)

const submitHandler = extractFunctionBlock('handleProductionFormalSubmit')
const requestIndex = submitHandler.indexOf('await ProFeedbackApi.frontlineSubmit(formalPayload)')
const resetIndex = submitHandler.indexOf('resetProductionSubmissionDraft()')
const openIndex = submitHandler.indexOf('openProductionSubmitSuccessDialog(')
assert.ok(requestIndex >= 0, 'formal submit request must still be awaited.')
assert.ok(resetIndex > requestIndex, 'draft reset must happen only after explicit API success.')
assert.ok(openIndex > resetIndex, 'success dialog must open after the next submission draft is ready.')
assert.doesNotMatch(
  submitHandler,
  /message\.success/,
  'production success must use the fullscreen-root dialog instead of a body-level toast.'
)

const closeHandler = extractFunctionBlock('closeProductionSubmitSuccessDialog')
assert.match(
  closeHandler,
  /productionSubmitSuccessOpen\.value\s*=\s*false/,
  'continue action must close the success modal without changing formal facts.'
)
assert.match(
  source,
  /const isSubmitBlocked = computed\(\(\) =>[\s\S]*productionSubmitSuccessOpen\.value/,
  'submit controls must remain blocked while the modal covers the page.'
)

const modalStyle = extractStyleBlock('.frontline-production-submit-success-modal')
assert.match(
  modalStyle,
  /position:\s*absolute;[\s\S]*inset:\s*0;[\s\S]*z-index:\s*130;/,
  'success modal must cover the production fullscreen root above existing in-page overlays.'
)
assert.match(
  extractStyleBlock('.frontline-production-submit-success-dialog'),
  /width:\s*min\(100%,\s*720px\);[\s\S]*background:\s*#ffffff;/,
  'success dialog must use a stable readable panel size and background.'
)

console.log('PASS: frontline production success dialog stays inside fullscreen root')
