const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

function extractFunction(sourceText, declaration) {
  const start = sourceText.indexOf(declaration)
  assert.ok(start >= 0, `missing function: ${declaration}`)
  const open = sourceText.indexOf('{', start)
  assert.ok(open > start, `missing function body: ${declaration}`)
  let depth = 0
  for (let index = open; index < sourceText.length; index += 1) {
    if (sourceText[index] === '{') depth += 1
    if (sourceText[index] === '}') {
      depth -= 1
      if (depth === 0) {
        return sourceText.slice(start, index + 1)
      }
    }
  }
  assert.fail(`unterminated function: ${declaration}`)
}

const refreshSubmittedQuantity = extractFunction(
  source,
  'const refreshProductionSelectedProcessSubmittedQuantity ='
)
assert.match(
  refreshSubmittedQuantity,
  /ProFeedbackApi\.getFrontlineProductionActiveOrderProcesses\(\s*selectedActiveOrder\.activeOrderId\s*\)/,
  'after a formal submit, the selected process submitted quantity must come from a fresh server process response.'
)
assert.match(
  refreshSubmittedQuantity,
  /const refreshedProcess\s*=\s*processes\.find\(\(process\) =>\s*isSameProcess\(process,\s*selectedProcess\)/,
  'the refreshed process must be matched by formal process identity, not by array position.'
)
assert.match(
  refreshSubmittedQuantity,
  /deviceState\.selectedProcess\s*=\s*refreshedProcess/,
  'the selected process must be replaced so the red submitted-quantity card re-renders.'
)
assert.match(
  refreshSubmittedQuantity,
  /deviceState\.processOptions\s*=\s*processes/,
  'the process picker and navigation options must be updated with the same fresh response.'
)

const submitHandler = extractFunction(source, 'const handleProductionFormalSubmit =')
const requestIndex = submitHandler.indexOf('await ProFeedbackApi.frontlineSubmit(formalPayload)')
const refreshIndex = submitHandler.indexOf('await refreshProductionSelectedProcessSubmittedQuantity()')
const successDialogIndex = submitHandler.indexOf('openProductionSubmitSuccessDialog()')
assert.ok(requestIndex >= 0, 'formal submit request must still be awaited.')
assert.ok(
  refreshIndex > requestIndex,
  'submitted quantity refresh must happen only after the formal submit succeeds.'
)
assert.ok(
  successDialogIndex > refreshIndex,
  'the success dialog must open after the red submitted-quantity card has been refreshed.'
)

console.log('PASS: frontline production submitted quantity refreshes after formal submit')
