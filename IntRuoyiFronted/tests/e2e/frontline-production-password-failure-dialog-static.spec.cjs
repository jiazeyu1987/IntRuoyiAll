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

const productionPanelStart = source.indexOf(
  'class="frontline-work-panel panel quantity-panel frontline-production-quantity-panel"'
)
const devicePanelStart = source.indexOf(
  'class="frontline-work-panel panel device-panel frontline-production-device-panel"'
)
assert.ok(
  productionPanelStart >= 0 && devicePanelStart > productionPanelStart,
  'production left quantity panel must exist.'
)
const productionPanel = source.slice(productionPanelStart, devicePanelStart)

assert.match(
  productionPanel,
  /data-frontline-error-slot[\s\S]*data-frontline-error-message[\s\S]*frontlineErrorMessage/,
  'password failure must render in the production left-panel inline error zone.'
)
assert.doesNotMatch(
  source,
  /data-production-submit-password-failure-dialog|productionSubmitFailureOpen|productionSubmitFailureText/,
  'password failure must not open a separate result dialog.'
)

const submitHandler = extractFunctionBlock('handleProductionFormalSubmit')
assert.match(
  submitHandler,
  /try \{[\s\S]*ProFeedbackApi\.frontlineSubmit\(formalPayload\)[\s\S]*\} finally \{[\s\S]*payloadLoading\.value = false/,
  'formal submit must release loading and propagate failures to the shared command boundary.'
)
assert.doesNotMatch(
  submitHandler,
  /isProductionPasswordValidationFailure|openProductionSubmitFailureDialog|catch \(error\)/,
  'formal submit must not divert password failures into a dedicated dialog branch.'
)

const validateHandler = extractFunctionBlock('handleValidate')
assert.match(
  validateHandler,
  /await handleProductionFormalSubmit\(\)[\s\S]*catch \(error\) \{[\s\S]*showFrontlineError\(error\)/,
  'the production command boundary must show backend password text in the inline error zone.'
)

const passwordConfirmHandler = extractFunctionBlock(
  'confirmProductionFormalSubmitConfirmation'
)
assert.match(
  passwordConfirmHandler,
  /!productionSignaturePassword\.value\.trim\(\)[\s\S]*showFrontlineError\('请输入所选员工的电子签名密码。'\)[\s\S]*return/,
  'missing production signature password must use the same inline error zone.'
)

console.log('PASS: frontline production password failure uses the fixed inline error zone')
