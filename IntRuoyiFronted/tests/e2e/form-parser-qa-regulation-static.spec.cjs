const fs = require('fs')
const path = require('path')

const root = process.cwd()

const readFile = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  if (!fs.existsSync(absolutePath)) {
    throw new Error(`Missing expected file: ${relativePath}`)
  }
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message || `Expected content to include: ${expected}`)
  }
}

const assertNotIncludes = (content, unexpected, message) => {
  if (content.includes(unexpected)) {
    throw new Error(message || `Expected content not to include: ${unexpected}`)
  }
}

const parserPage = readFile('src/views/form-center/parser/index.vue')
assertIncludes(parserPage, 'activeParserResultTab')
assertIncludes(parserPage, 'form-parser-result-tabs')
assertIncludes(parserPage, 'label="生产批记录"')
assertIncludes(parserPage, 'name="production-batch-record"')
assertIncludes(parserPage, 'label="QA检验规程"')
assertIncludes(parserPage, 'name="qa-inspection-regulation"')
assertIncludes(parserPage, 'QaInspectionRegulationParserPanel')
assertIncludes(parserPage, 'ref="qaRegulationParserRef"')
assertIncludes(parserPage, 'handleQaInspectionRegulation')
assertIncludes(parserPage, 'handleQaRegulationFileChange')
assertIncludes(parserPage, "activeParserResultTab.value = 'production-batch-record'")
assertIncludes(parserPage, "activeParserResultTab.value = 'qa-inspection-regulation'")
const parserButtons = parserPage.match(/<el-button[\s\S]*?<\/el-button>/g) || []
const qaButton = parserButtons.find((button) => button.includes('QA检验规程'))
if (!qaButton) {
  throw new Error('QA检验规程按钮必须存在')
}
assertNotIncludes(
  qaButton,
  'v-hasPermi',
  'QA检验规程按钮必须随表单解析页面显示，不能依赖额外按钮权限'
)

const qaPanel = readFile(
  'src/views/form-center/parser/components/QaInspectionRegulationParserPanel.vue'
)
assertIncludes(qaPanel, "name: 'QaInspectionRegulationParserPanel'")
assertIncludes(qaPanel, 'parseWordFile')
assertIncludes(qaPanel, 'defineExpose')
assertIncludes(qaPanel, 'parseQaInspectionRegulationJson')
assertIncludes(qaPanel, 'qaEditableJson')
assertIncludes(qaPanel, 'qaPreviewJson')
assertIncludes(qaPanel, 'qaJsonSearchKeyword')
assertIncludes(qaPanel, 'locateNextQaJsonKeyword')
assertIncludes(qaPanel, 'setSelectionRange')
assertIncludes(qaPanel, 'applyQaEditedJson')
assertIncludes(qaPanel, 'downloadQaCurrentJson')
assertIncludes(qaPanel, 'validateQaRecognitionJson')
assertIncludes(qaPanel, 'qaCurrentProcessIndex')
assertIncludes(qaPanel, 'qaProcessSelectorVisible')
assertIncludes(qaPanel, 'qaCurrentItemIndex')
assertIncludes(qaPanel, 'QA检验规程 JSON编辑')
assertIncludes(qaPanel, '一线PQC预览')
assertIncludes(qaPanel, '检验设备')
assertIncludes(qaPanel, '接受标准')
assertIncludes(qaPanel, '检验方法')
assertIncludes(qaPanel, '抽样方案')
assertIncludes(qaPanel, '首检数量')
assertIncludes(qaPanel, '巡检比例')
assertIncludes(qaPanel, '全部合格')
assertIncludes(qaPanel, '全部不良')
assertIncludes(qaPanel, '逐件选择')
assertIncludes(qaPanel, 'regulationCode')
assertIncludes(qaPanel, 'regulationName')
assertIncludes(qaPanel, 'versionNo')
assertIncludes(qaPanel, 'effectiveDate')
assertIncludes(qaPanel, 'processCode')
assertIncludes(qaPanel, 'itemCode')
assertIncludes(qaPanel, 'itemName')
assertIncludes(qaPanel, 'inspectionTool')
assertIncludes(qaPanel, 'standardText')
assertIncludes(qaPanel, 'inspectionMethod')
assertIncludes(qaPanel, 'samplingPlanText')
assertIncludes(qaPanel, 'firstInspectionQuantity')
assertIncludes(qaPanel, 'patrolInspectionRatio')
assertNotIncludes(qaPanel, 'BatchRecordReportApi')
assertNotIncludes(qaPanel, 'parseProductionBatchRecordTotalRecognitionJson')
assertNotIncludes(qaPanel, 'importQaRegulationWordDraft')

const qaApi = readFile('src/api/mes/qc/template/index.ts')
assertIncludes(qaApi, 'QaInspectionRegulationParseVO')
assertIncludes(qaApi, 'parseQaInspectionRegulationJson')
assertIncludes(qaApi, '/mes/qa/inspection-regulation/form-parser-json')
assertIncludes(qaApi, 'request.upload<{ data: QaInspectionRegulationParseVO }>')

console.log('form parser QA regulation tab isolation and PQC preview static contract passed')
