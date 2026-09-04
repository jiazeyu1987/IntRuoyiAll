const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

function extractBlock(source, startNeedle, endNeedle) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `${startNeedle} must exist.`)
  const end = source.indexOf(endNeedle, start)
  assert.ok(end > start, `${startNeedle} block must close before ${endNeedle}.`)
  return source.slice(start, end)
}

const typeBlock = extractBlock(
  panel,
  'type ProductionMaterialDraftState = {',
  'type FrontlineEmployeeSwitchResult = {'
)
assert.match(
  typeBlock,
  /type ProductionMaterialDeviceDraftState = Pick<ProductionMaterialDraftState, 'selectedDeviceKey' \| 'selectedDeviceKeys' \| 'deviceParameters' \| 'deviceMeteringValidity'>/,
  'material device memory must only store device selection, parameters, and metering validity.'
)

assert.match(
  panel,
  /const productionMaterialLastDeviceDrafts = reactive<Record<string, ProductionMaterialDeviceDraftState>>\(\{\}\)/,
  'material device memory must be keyed by material.'
)

const createDraftBlock = extractBlock(
  panel,
  'const createProductionMaterialDraftState = (materialKey: string): ProductionMaterialDraftState => {',
  '\n}\n\nconst persistActiveProductionMaterialDraft ='
)
assert.match(
  createDraftBlock,
  /const lastDeviceDraft = productionMaterialLastDeviceDrafts\[materialKey\]/,
  'new material drafts must read the same material last device draft.'
)
assert.match(
  createDraftBlock,
  /outputQuantity:\s*undefined/,
  'new material drafts must not reuse the last output quantity.'
)
assert.match(
  createDraftBlock,
  /defectQuantities:[\s\S]*configuredDefectReasons\.value\.map\(\(defect\) => \[defect\.key, 0\]\)/,
  'new material drafts must not reuse the last loss quantities.'
)
assert.match(
  createDraftBlock,
  /selectedDeviceKey:\s*lastDeviceDraft\?\.selectedDeviceKey/,
  'new material drafts must restore the last active device key.'
)
assert.match(
  createDraftBlock,
  /selectedDeviceKeys:\s*\[\.\.\.\(lastDeviceDraft\?\.selectedDeviceKeys \|\| \[\]\)\]/,
  'new material drafts must restore the last selected device keys.'
)
assert.match(
  createDraftBlock,
  /deviceParameters:\s*lastDeviceDraft\s*\?\s*cloneProductionDeviceParameters\(lastDeviceDraft\.deviceParameters\)/,
  'new material drafts must restore the last device parameter values.'
)
assert.match(
  createDraftBlock,
  /deviceMeteringValidity:\s*lastDeviceDraft\s*\?\s*\{ \.\.\.lastDeviceDraft\.deviceMeteringValidity \}/,
  'new material drafts must restore the last per-device metering validity.'
)

const rememberBlock = extractBlock(
  panel,
  'const rememberProductionMaterialDeviceDrafts = () => {',
  '\n}\n\nconst resetProductionSubmissionDraft ='
)
assert.match(
  rememberBlock,
  /persistActiveProductionMaterialDraft\(\)/,
  'submit success memory must first persist the active material draft.'
)
assert.match(
  rememberBlock,
  /for \(const \[materialKey, materialDraft\] of Object\.entries\(productionMaterialDrafts\)\)/,
  'submit success memory must store every current material draft by material key.'
)
assert.match(
  rememberBlock,
  /selectedDeviceKeys:\s*\[\.\.\.materialDraft\.selectedDeviceKeys\]/,
  'submit success memory must store selected device keys.'
)
assert.match(
  rememberBlock,
  /deviceParameters:\s*cloneProductionDeviceParameters\(materialDraft\.deviceParameters\)/,
  'submit success memory must store device parameters.'
)
assert.doesNotMatch(
  rememberBlock,
  /outputQuantity|defectQuantities/,
  'submit success memory must not store output or loss quantities.'
)

const submitHandler = extractBlock(
  panel,
  'const handleProductionFormalSubmit = async () => {',
  '\n}\n\nconst assertPqcSignatureAndQuantityReady ='
)
const submitIndex = submitHandler.indexOf('await ProFeedbackApi.frontlineSubmit(formalPayload)')
const rememberIndex = submitHandler.indexOf('rememberProductionMaterialDeviceDrafts()')
const resetIndex = submitHandler.indexOf('resetProductionSubmissionDraft()')
assert.ok(submitIndex >= 0, 'production submit endpoint must be awaited.')
assert.ok(rememberIndex > submitIndex, 'last device memory may update only after submit succeeds.')
assert.ok(resetIndex > rememberIndex, 'draft reset must happen after last device memory is saved.')

assert.match(
  panel,
  /productionMaterialDrafts\[material\.key\] = createProductionMaterialDraftState\(material\.key\)/,
  'material draft sync must pass material key into the default-state factory.'
)

console.log('PASS: frontline production remembers each material last device draft')
