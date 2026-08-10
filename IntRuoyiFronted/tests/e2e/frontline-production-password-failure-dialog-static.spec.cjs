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

const productionStageIndex = source.indexOf('data-frontline-production-stage')
const failureDialogIndex = source.indexOf('data-production-submit-password-failure-dialog')
const pqcPickerIndex = source.indexOf('data-pqc-process-picker')

assert.ok(productionStageIndex >= 0, 'production fullscreen stage must exist.')
assert.ok(failureDialogIndex >= 0, 'password failure dialog must exist.')
assert.ok(
  failureDialogIndex > productionStageIndex && failureDialogIndex < pqcPickerIndex,
  'password failure dialog must render inside the fullscreen root so it remains visible after requestFullscreen().'
)
assert.match(
  source,
  /class="frontline-production-submit-success-modal"[\s\S]*data-production-submit-password-failure-dialog[\s\S]*role="dialog"[\s\S]*aria-modal="true"/,
  'password failure must use the same in-component modal shell as the submit-success dialog.'
)
assert.match(
  source,
  /data-production-submit-password-failure-dialog[\s\S]*<section class="frontline-production-submit-success-dialog"/,
  'password failure must reuse the submit-success dialog panel size and spacing.'
)
assert.match(
  source,
  /data-production-submit-password-failure-message[\s\S]*\{\{\s*productionSubmitFailureText\s*\}\}/,
  'password failure dialog must show the formal backend error text.'
)
assert.match(
  source,
  /data-production-submit-password-failure-close[\s\S]*@click="closeProductionSubmitFailureDialog"/,
  'password failure dialog must expose an explicit close action.'
)

const submitHandler = extractFunctionBlock('handleProductionFormalSubmit')
assert.match(
  submitHandler,
  /catch \(error\) \{[\s\S]*isProductionPasswordValidationFailure\(error\)[\s\S]*openProductionSubmitFailureDialog\(resolveErrorMessage\(error\)\)[\s\S]*return[\s\S]*throw error[\s\S]*\} finally/,
  'formal submit must convert only password validation failure into the fullscreen-root dialog and rethrow other failures.'
)
assert.doesNotMatch(
  submitHandler,
  /catch \(error\) \{[\s\S]*message\.error/,
  'formal submit must not downgrade password validation failure to a body-level toast.'
)

const isPasswordFailure = extractFunctionBlock('isProductionPasswordValidationFailure')
assert.match(
  isPasswordFailure,
  /当前密码校验失败|密码校验失败|电子签名密码/,
  'password-failure classifier must be explicit and limited to password/signature validation errors.'
)

assert.match(
  source,
  /const isSubmitBlocked = computed\(\(\) =>[\s\S]*productionSubmitFailureOpen\.value/,
  'submit controls must remain blocked while the password-failure dialog covers the page.'
)

console.log('PASS: frontline production password failure uses fullscreen-root result dialog')
