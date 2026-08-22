const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const frontlineApiSource = read('src/api/mes/pro/feedback/index.ts')
const qaPageSource = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const qaApiSource = read('src/api/mes/qc/template/index.ts')
const backendRoot = '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes'
const qaSaveVoSource = read(
  `${backendRoot}/controller/admin/qa/regulation/vo/MesQaInspectionRegulationSaveReqVO.java`
)
const qaPublishedVoSource = read(
  `${backendRoot}/controller/admin/qa/regulation/vo/MesQaInspectionRegulationPublishedVersionRespVO.java`
)
const qaItemDoSource = read(
  `${backendRoot}/dal/dataobject/qa/regulation/MesQaInspectionRegulationItemDO.java`
)
const qaServiceSource = read(
  `${backendRoot}/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java`
)
const frontlineItemSource = read(
  `${backendRoot}/service/pro/frontline/MesFrontlinePqcInspectionItem.java`
)
const frontlineContextSource = read(
  `${backendRoot}/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java`
)
const frontlineVoSource = read(
  `${backendRoot}/controller/admin/pro/feedback/vo/frontline/MesFrontlineRouteProcessRespVO.java`
)
const frontlineControllerSource = read(
  `${backendRoot}/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java`
)

const qaSaveItem = blockBetween(
  qaApiSource,
  'export interface QaInspectionRegulationItemVO {',
  'export interface QaInspectionRegulationSaveReqVO {'
)
for (const field of ['inspectionTool: string', 'samplingPlanText: string']) {
  assert.ok(qaSaveItem.includes(field), `QA save item must require ${field}.`)
}

const qaSaveMapping = blockBetween(
  qaPageSource,
  'const buildQaRegulationSaveItem = (',
  'const buildQaRegulationProcesses = ('
)
assert.match(
  qaSaveMapping,
  /inspectionTool:\s*resolveRequiredText\(item\.inspectionTool,/,
  'QA save payload must persist the inspection-tool text from the current inspection item row.'
)
assert.match(
  qaSaveMapping,
  /samplingPlanText:\s*resolveRequiredText\(item\.samplingPlanText,/,
  'QA save payload must persist the sampling-plan text from the current inspection item row.'
)

for (const source of [qaSaveVoSource, qaItemDoSource, qaPublishedVoSource]) {
  assert.match(source, /String inspectionTool;/, 'Backend QA contract must carry inspectionTool.')
  assert.match(source, /String samplingPlanText;/, 'Backend QA contract must carry samplingPlanText.')
}
assert.match(
  qaServiceSource,
  /\.inspectionTool\(StrUtil\.trim\(item\.getInspectionTool\(\)\)\)[\s\S]*\.samplingPlanText\(StrUtil\.trim\(item\.getSamplingPlanText\(\)\)\)/,
  'QA service must persist both display fields on each published inspection item.'
)
assert.match(
  qaServiceSource,
  /\.inspectionTool\(source\.getInspectionTool\(\)\)[\s\S]*\.samplingPlanText\(source\.getSamplingPlanText\(\)\)[\s\S]*\.firstInspectionQuantity/,
  'Published-version evidence must return both exact QA inspection-item display fields.'
)
assert.match(
  qaServiceSource,
  /validateItem\([\s\S]*StrUtil\.isBlank\(item\.getInspectionTool\(\)\)[\s\S]*StrUtil\.isBlank\(item\.getSamplingPlanText\(\)\)[\s\S]*QA_INSPECTION_REGULATION_ITEM_INVALID/,
  'The service boundary must reject missing exact QA display fields before persistence.'
)

assert.match(
  frontlineItemSource,
  /String inspectionTool,[\s\S]*String samplingPlanText,/,
  'The frontline PQC item snapshot must carry exact inspection tool and sampling plan text.'
)
assert.match(
  frontlineContextSource,
  /StrUtil\.isBlank\(item\.getInspectionTool\(\)\)[\s\S]*inspectionItem\.inspectionTool[\s\S]*StrUtil\.isBlank\(item\.getSamplingPlanText\(\)\)[\s\S]*inspectionItem\.samplingPlanText/,
  'Runtime loading must identify the exact missing published QA display field.'
)
assert.match(
  frontlineContextSource,
  /item\.getInspectionTool\(\),\s*item\.getSamplingPlanText\(\),/,
  'Runtime item mapping must read both fields directly from the published QA item row.'
)
assert.match(frontlineVoSource, /private String inspectionTool;/)
assert.match(frontlineVoSource, /private String samplingPlanText;/)
assert.match(frontlineApiSource, /inspectionTool: string/)
assert.match(frontlineApiSource, /samplingPlanText: string/)
assert.match(
  frontlineControllerSource,
  /setInspectionTool\(item\.inspectionTool\(\)\)[\s\S]*setSamplingPlanText\(item\.samplingPlanText\(\)\)/,
  'Frontline response mapping must preserve both exact published QA fields.'
)

const itemInterface = blockBetween(
  panelSource,
  'interface PqcInspectionItem {',
  'interface PqcItemSelection {'
)
for (const field of ['inspectionTool: string', 'samplingPlanText: string']) {
  assert.ok(itemInterface.includes(field), `PQC view model must include ${field}.`)
}
const itemMapping = blockBetween(
  panelSource,
  'const mapPqcInspectionItem = (item: FrontlinePqcInspectionItemVO)',
  'const normalizePqcTaskOptionItemKey = ('
)
assert.match(itemMapping, /inspectionTool:\s*item\.inspectionTool,/)
assert.match(itemMapping, /samplingPlanText:\s*item\.samplingPlanText,/)
assert.doesNotMatch(
  itemMapping,
  /inspectionTool:\s*item\.inspectionTool\s*(?:\|\||\?\?)|samplingPlanText:\s*item\.samplingPlanText\s*(?:\|\||\?\?)/,
  'Exact QA display fields must not be downgraded to frontend defaults.'
)

const methodDialog = blockBetween(
  panelSource,
  'data-pqc-method-dialog',
  '<main class="frontline-operator-main is-pqc"'
)
assert.match(
  methodDialog,
  /id="pqc-method-dialog-title">\{\{\s*activePqcMethodItem\.samplingPlanText\s*\}\}<\/h3>/,
  'The red-box title area must display the exact QA sampling plan.'
)
assert.match(
  methodDialog,
  /data-pqc-method-detail-text[\s\S]*formatPqcMethodSummary\(activePqcMethodItem\)/,
  'The method detail must continue to display the exact QA inspection method.'
)
assert.match(
  methodDialog,
  /data-pqc-method-equipment-text[\s\S]*<span>检验器具及设备<\/span>[\s\S]*activePqcMethodItem\.inspectionTool/,
  'The yellow-box area must display the exact QA inspection-tool text.'
)
assert.doesNotMatch(
  methodDialog,
  /data-pqc-method-meta-grid|<dt>检验项目<\/dt>|<dt>结果类型<\/dt>|<dt>单位<\/dt>|<dt>来源<\/dt>/,
  'The old four-card metadata grid must be removed from the method dialog.'
)

console.log('PASS: frontline PQC method dialog reads sampling plan and equipment text from QA items')
