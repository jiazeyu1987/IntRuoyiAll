const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.match(
  panelSource,
  /const pqcTaskDrafts = reactive<Record<PqcTaskDraftKey, PqcTaskDraftState>>/,
  'PQC must keep quantity, scrap, and defect drafts per inspection-method task.'
)
assert.match(
  panelSource,
  /const getPqcPieceStateKeyForTask = \(\s*itemKey: PqcInspectionItemKey,\s*taskOption: PqcTaskOptionSnapshot \| undefined/,
  'PQC piece values must be addressable by an explicit task option, not only the active tab.'
)

const applyTaskDraftBlock = blockBetween(
  panelSource,
  'const applyPqcTaskOptionToDraft = (option: PqcTaskOptionSnapshot) => {',
  'const clearPqcTaskOptionDraft = () => {'
)
assert.doesNotMatch(
  applyTaskDraftBlock,
  /clearPqcPieceValues\(\)/,
  'Switching inspection methods must not clear already filled data for other methods.'
)
assert.match(
  applyTaskDraftBlock,
  /const storedDraft = getPqcTaskDraft\(option\)[\s\S]*pqcDraft\.scrapQuantity = storedDraft\.scrapQuantity/,
  'Switching back to an inspection method must restore its own scrap and defect draft.'
)

const submitOptionsBlock = blockBetween(
  panelSource,
  'const getPqcCurrentSubmitTaskOptions = () => {',
  'const buildPqcItemResultsPayload = ('
)
assert.match(
  submitOptionsBlock,
  /activeOption\.inspectionType[\s\S]*activeOption\.businessDate[\s\S]*activeOption\.shiftCode[\s\S]*activeOption\.roundNo/,
  'Submit scope must be the current process and current inspection type/date/shift/round.'
)
assert.match(
  submitOptionsBlock,
  /for \(const item of pqcInspectionItems\.value\)[\s\S]*pqcTaskOptionIncludesItem\(option, item\.key\)/,
  'Submit scope must require a matching task for every inspection method in the current process.'
)
assert.match(
  submitOptionsBlock,
  /throw new Error\(`\$\{item\.label\}缺少\$\{formatPqcTaskOptionLabel\(activeOption\)\}PQC任务。`\)/,
  'Missing method task must fail fast with the exact inspection method.'
)

const submitPayloadsBlock = blockBetween(
  panelSource,
  'function buildPqcInspectionSubmitPayloads(): FrontlinePqcInspectionSubmitReqVO[] {',
  'const buildPqcInspectionSubmitPayload = (): FrontlinePqcInspectionSubmitReqVO =>'
)
assert.match(
  submitPayloadsBlock,
  /persistCurrentPqcTaskDraft\(\)[\s\S]*const taskOptions = getPqcCurrentSubmitTaskOptions\(\)[\s\S]*taskOptions\.map\(\(taskOption\) =>\s*buildPqcInspectionSubmitPayloadForTask\(taskOption\)/,
  'Confirm submit must build one formal payload for every current-process inspection method task.'
)

const confirmBlock = blockBetween(
  panelSource,
  'const handleConfirmPqcSubmit = async () => {',
  'const assertFormalPayloadContext = () => {'
)
assert.match(
  confirmBlock,
  /let submitPayloads: FrontlinePqcInspectionSubmitReqVO\[\][\s\S]*submitPayloads = buildPqcInspectionSubmitPayloads\(\)/,
  'PQC confirm handler must build all method payloads before issuing write requests.'
)
assert.match(
  confirmBlock,
  /for \(const submitPayload of submitPayloads\)[\s\S]*ProFeedbackApi\.submitFrontlinePqcInspection\(\s*submitPayload\s*\)/,
  'PQC confirm handler must submit each current-process inspection method payload.'
)
assert.match(
  confirmBlock,
  /resetPqcSubmissionDrafts\(submitPayloads\.map\(\(payload\) => payload\.pqcTaskId\)\)/,
  'PQC submit success must reset every submitted method task, not only the active tab.'
)

console.log('frontline-pqc-submit-all-methods-static: PASS')
