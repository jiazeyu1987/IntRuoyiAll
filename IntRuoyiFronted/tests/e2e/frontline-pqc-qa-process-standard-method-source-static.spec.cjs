const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const apiSource = read('src/api/mes/pro/feedback/index.ts')
const backendVoSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlineRouteProcessRespVO.java'
)
const backendControllerSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java'
)

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const itemInterface = blockBetween(
  panelSource,
  'interface PqcInspectionItem {',
  'interface PqcItemSelection {'
)
assert.match(
  itemInterface,
  /standardText:\s*string/,
  'PQC inspection item view model must keep the QA process 接收标准 column as standardText.'
)
assert.match(
  itemInterface,
  /inspectionMethod:\s*string/,
  'PQC inspection item view model must keep the QA process 检验方法 column as inspectionMethod.'
)

const itemMapping = blockBetween(
  panelSource,
  'const mapPqcInspectionItem = (item: FrontlinePqcInspectionItemVO)',
  'const normalizePqcTaskOptionItemKey'
)
assert.match(
  itemMapping,
  /standardText:\s*item\.standardText \|\| ''/,
  'PQC view model must map standardText directly from the backend QA process column.'
)
assert.match(
  itemMapping,
  /inspectionMethod:\s*item\.inspectionMethod \|\| ''/,
  'PQC view model must map inspectionMethod directly from the backend QA process column.'
)

const standardDialog = blockBetween(
  panelSource,
  'data-pqc-standard-dialog',
  '<div\n        v-if="activePqcMethodItem"'
)
assert.match(
  standardDialog,
  /data-pqc-standard-detail-text[\s\S]*activePqcStandardItem\.standardText/,
  'The 接收标准 dialog body must display the QA process standardText column.'
)
assert.doesNotMatch(standardDialog, /standardLowerLimit|standardUpperLimit/, 'Standard dialog must not synthesize QA text from numeric bounds.')

const standardSummary = blockBetween(
  panelSource,
  'const formatPqcStandardSummary = (item: PqcInspectionItem) => {',
  'const normalizePqcInspectionMethodLabel = (inspectionMethod: string) => {'
)
assert.match(
  standardSummary,
  /item\.standardText/,
  'The 接收标准 card summary must use the same QA process standardText source as the dialog.'
)
assert.doesNotMatch(
  standardSummary,
  /standardLowerLimit|standardUpperLimit|formatPqcStandardBound/,
  'The 接收标准 card summary must not synthesize a replacement from numeric bounds.'
)

const methodSummary = blockBetween(
  panelSource,
  'const formatPqcMethodSummary = (item: PqcInspectionItem) =>',
  'const formatPqcInspectionTitle = (item: PqcInspectionItem) =>'
)
assert.match(
  methodSummary,
  /item\.inspectionMethod/,
  'The 检验方法 card and dialog must use the QA process inspectionMethod column.'
)
assert.doesNotMatch(methodSummary, /item\.label|item\.itemName|item\.standardText/, 'Method must not be replaced by labels or standard text.')

const itemDetailsPayload = blockBetween(
  panelSource,
  'const buildPqcItemDetailsPayload = (',
  'const getPqcCurrentChoiceValues = (itemKey: PqcInspectionItemKey) =>'
)
assert.match(
  itemDetailsPayload,
  /standardText:\s*item\.standardText/,
  'PQC raw item detail payload must persist the same QA process 接收标准 text shown in the dialog.'
)
assert.match(
  itemDetailsPayload,
  /inspectionMethod:\s*item\.inspectionMethod/,
  'PQC raw item detail payload must persist the same QA process 检验方法 text shown in the dialog.'
)

assert.match(
  apiSource,
  /standardText:\s*string/,
  'Frontend PQC API type must expose the QA process 接收标准 column.'
)
assert.match(
  apiSource,
  /inspectionMethod:\s*string/,
  'Frontend PQC API type must expose the QA process 检验方法 column.'
)
assert.match(
  backendVoSource,
  /private String standardText;/,
  'Backend frontline PQC response VO must expose standardText for the QA process 接收标准 column.'
)
assert.match(
  backendVoSource,
  /private String inspectionMethod;/,
  'Backend frontline PQC response VO must expose inspectionMethod for the QA process 检验方法 column.'
)
assert.match(
  backendControllerSource,
  /respVO\.setStandardText\(item\.standardText\(\)\);/,
  'Backend response mapping must populate standardText from the published QA regulation standard column.'
)
assert.match(
  backendControllerSource,
  /respVO\.setInspectionMethod\(item\.inspectionMethod\(\)\);/,
  'Backend response mapping must populate inspectionMethod from the published QA regulation method column.'
)

console.log('PASS: frontline PQC dialogs read QA process standard and method columns')
