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
  /acceptanceStandard:\s*string/,
  'PQC inspection item view model must keep the QA process 接收标准 column as acceptanceStandard.'
)
assert.match(
  itemInterface,
  /processInspectionMethod:\s*string/,
  'PQC inspection item view model must keep the QA process 检验方法 column as processInspectionMethod.'
)

const itemMapping = blockBetween(
  panelSource,
  'const pqcInspectionItems = computed<PqcInspectionItem[]>',
  'const pqcInspectionItemMap = computed'
)
assert.match(
  itemMapping,
  /acceptanceStandard:\s*item\.acceptanceStandard \|\| ''/,
  'PQC view model must map acceptanceStandard directly from the backend QA process column alias.'
)
assert.match(
  itemMapping,
  /processInspectionMethod:\s*item\.processInspectionMethod \|\| ''/,
  'PQC view model must map processInspectionMethod directly from the backend QA process column alias.'
)

const standardDialog = blockBetween(
  panelSource,
  'data-pqc-standard-dialog',
  '<div\n        v-if="activePqcMethodItem"'
)
assert.match(
  standardDialog,
  /data-pqc-standard-detail-text[\s\S]*activePqcStandardItem\.acceptanceStandard/,
  'The 接收标准 dialog body must display the QA process acceptanceStandard column.'
)
assert.doesNotMatch(
  standardDialog,
  /data-pqc-standard-detail-text[\s\S]*activePqcStandardItem\.standardText/,
  'The 接收标准 dialog body must not read the legacy standardText field directly.'
)

const standardSummary = blockBetween(
  panelSource,
  'const formatPqcStandardSummary = (item: PqcInspectionItem) => {',
  'const normalizePqcInspectionMethodLabel = (inspectionMethod: string) => {'
)
assert.match(
  standardSummary,
  /item\.acceptanceStandard/,
  'The 接收标准 card summary must use the same QA process acceptanceStandard source as the dialog.'
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
  /item\.processInspectionMethod/,
  'The 检验方法 card and dialog must use the QA process processInspectionMethod column.'
)
assert.doesNotMatch(
  methodSummary,
  /item\.inspectionMethod/,
  'The 检验方法 card and dialog must not read the legacy inspectionMethod field directly.'
)

const itemDetailsPayload = blockBetween(
  panelSource,
  'const buildPqcItemDetailsPayload = () =>',
  'const getPqcCurrentChoiceValues = (itemKey: PqcInspectionItemKey) =>'
)
assert.match(
  itemDetailsPayload,
  /standardText:\s*item\.acceptanceStandard/,
  'PQC raw item detail payload must persist the same QA process 接收标准 text shown in the dialog.'
)
assert.match(
  itemDetailsPayload,
  /inspectionMethod:\s*item\.processInspectionMethod/,
  'PQC raw item detail payload must persist the same QA process 检验方法 text shown in the dialog.'
)

assert.match(
  apiSource,
  /acceptanceStandard\?:\s*string/,
  'Frontend PQC API type must expose the QA process 接收标准 column alias.'
)
assert.match(
  apiSource,
  /processInspectionMethod\?:\s*string/,
  'Frontend PQC API type must expose the QA process 检验方法 column alias.'
)
assert.match(
  backendVoSource,
  /private String acceptanceStandard;/,
  'Backend frontline PQC response VO must expose acceptanceStandard for the QA process 接收标准 column.'
)
assert.match(
  backendVoSource,
  /private String processInspectionMethod;/,
  'Backend frontline PQC response VO must expose processInspectionMethod for the QA process 检验方法 column.'
)
assert.match(
  backendControllerSource,
  /respVO\.setAcceptanceStandard\(item\.standardText\(\)\);/,
  'Backend response mapping must populate acceptanceStandard from the published QA regulation standard column.'
)
assert.match(
  backendControllerSource,
  /respVO\.setProcessInspectionMethod\(item\.inspectionMethod\(\)\);/,
  'Backend response mapping must populate processInspectionMethod from the published QA regulation method column.'
)

console.log('PASS: frontline PQC dialogs read QA process standard and method columns')
