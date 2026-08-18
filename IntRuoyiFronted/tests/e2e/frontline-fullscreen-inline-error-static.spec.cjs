const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panelSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const feedbackApiSource = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/feedback/index.ts'),
  'utf8'
).replace(/\r\n/g, '\n')
const templateApiSource = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/feedbackFrontlineTemplate.ts'),
  'utf8'
).replace(/\r\n/g, '\n')

const extractBraceBlock = (source, anchor) => {
  const start = source.indexOf(anchor)
  assert.ok(start >= 0, `missing anchor: ${anchor}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing block body: ${anchor}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(openIndex + 1, index)
    }
  }
  assert.fail(`unterminated block: ${anchor}`)
}

const extractArrowBlock = (source, anchor) => {
  const start = source.indexOf(anchor)
  assert.ok(start >= 0, `missing arrow function: ${anchor}`)
  const arrowIndex = source.indexOf('=> {', start)
  assert.ok(arrowIndex > start, `missing arrow function body: ${anchor}`)
  const openIndex = arrowIndex + 3
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(openIndex + 1, index)
    }
  }
  assert.fail(`unterminated arrow function: ${anchor}`)
}

const pqcContentStart = panelSource.indexOf('class="frontline-work-panel frontline-pqc-content-panel"')
const pqcFillStart = panelSource.indexOf('class="frontline-work-panel frontline-pqc-fill-panel"')
assert.ok(pqcContentStart >= 0 && pqcFillStart > pqcContentStart, 'PQC left content panel must exist.')
const pqcContentTemplate = panelSource.slice(pqcContentStart, pqcFillStart)

const productionContentStart = panelSource.indexOf(
  'class="frontline-work-panel panel quantity-panel frontline-production-quantity-panel"'
)
const productionDeviceStart = panelSource.indexOf(
  'class="frontline-work-panel panel device-panel frontline-production-device-panel"'
)
assert.ok(
  productionContentStart >= 0 && productionDeviceStart > productionContentStart,
  'production left quantity panel must exist.'
)
const productionContentTemplate = panelSource.slice(productionContentStart, productionDeviceStart)

for (const [name, template] of [
  ['PQC', pqcContentTemplate],
  ['production', productionContentTemplate]
]) {
  assert.match(
    template,
    /class="frontline-inline-error-slot"[\s\S]*data-frontline-error-slot[\s\S]*role="alert"[\s\S]*aria-live="assertive"/,
    `${name} left panel must reserve the fullscreen-visible inline error slot.`
  )
  assert.match(
    template,
    /data-frontline-error-message[\s\S]*\{\{\s*frontlineErrorMessage\s*\}\}/,
    `${name} error slot must render the shared latest error message.`
  )
  assert.match(
    template,
    /data-frontline-error-dismiss[\s\S]*@click="clearFrontlineError"/,
    `${name} error slot must provide an explicit dismiss action.`
  )
}

assert.equal(
  (panelSource.match(/data-frontline-error-slot/g) || []).length,
  2,
  'production and PQC must each render exactly one inline error slot.'
)
assert.match(
  panelSource,
  /const frontlineErrorMessage = ref\(''\)/,
  'both modes must share one component-owned error state.'
)

const showErrorBlock = extractArrowBlock(panelSource, 'const showFrontlineError =')
assert.match(
  showErrorBlock,
  /frontlineErrorMessage\.value\s*=\s*resolveErrorMessage\(error\)/,
  'the visible error boundary must preserve the real resolved error text.'
)
assert.doesNotMatch(
  panelSource,
  /message\.error\(/,
  'frontline errors must not be owned by body-level Element Plus toasts.'
)
assert.doesNotMatch(
  panelSource,
  /showFrontlineError\([^\n]+\)\n\s*throw error/,
  'a visible frontline error must terminate the command instead of escaping into a native event handler.'
)

const productionPasswordConfirm = extractArrowBlock(
  panelSource,
  'const confirmProductionFormalSubmitConfirmation ='
)
assert.match(
  productionPasswordConfirm,
  /showFrontlineError\('请输入所选员工的电子签名密码。'\)/,
  'missing production signature password must use the inline error boundary.'
)
const pqcSubmitConfirm = extractArrowBlock(panelSource, 'const handleConfirmPqcSubmit =')
assert.match(
  pqcSubmitConfirm,
  /showFrontlineError\('请输入所选员工的电子签名密码。'\)/,
  'missing PQC signature password must use the inline error boundary.'
)
assert.doesNotMatch(
  panelSource,
  /productionSubmitFailureOpen|data-production-submit-password-failure-dialog|isProductionPasswordValidationFailure/,
  'production password errors must no longer diverge into a separate fullscreen dialog.'
)

const errorSlotStyle = extractBraceBlock(panelSource, '.frontline-inline-error-slot {')
assert.match(
  errorSlotStyle,
  /min-height:\s*54px;[\s\S]*min-width:\s*0;/,
  'the reserved red-box position must remain stable without shifting the main layout.'
)
const visibleErrorStyle = extractBraceBlock(panelSource, '.frontline-inline-error-slot.is-visible {')
assert.match(
  visibleErrorStyle,
  /border:\s*3px solid #e85d5d;[\s\S]*background:\s*#fff2f2;/,
  'active errors must be visually distinct in the requested red-box position.'
)

for (const methodName of [
  'frontlineSubmit',
  'getFrontlineDeviceAccountProcesses',
  'getFrontlineProductionActiveOrders',
  'getFrontlinePqcActiveOrders',
  'getPqcProcesses',
  'getFrontlineEmployeeCandidates',
  'getFrontlineRuntimeConfig',
  'getFrontlinePqcEmployeeCandidates',
  'switchFrontlineActualEmployee',
  'switchFrontlinePqcActualEmployee',
  'submitFrontlinePqcInspection',
  'getFrontlinePqcSubmitReceipt'
]) {
  const methodBlock = extractArrowBlock(feedbackApiSource, `${methodName}: async`)
  assert.match(
    methodBlock,
    /ignoreErrorMessage:\s*true/,
    `${methodName} must delegate visible error ownership to the fullscreen component.`
  )
}

const catalogMethod = extractArrowBlock(templateApiSource, 'getCatalog: async')
assert.match(
  catalogMethod,
  /ignoreErrorMessage:\s*true/,
  'frontline template catalog failures must use the same in-component error boundary.'
)

console.log('PASS: production and PQC errors use the fullscreen-visible inline error zone')
