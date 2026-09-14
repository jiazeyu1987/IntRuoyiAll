const fs = require('fs')
const path = require('path')

const root = process.cwd()

const assertFile = (relativePath) => {
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

const extractFunction = (content, functionName) => {
  const start = content.indexOf(`const ${functionName} =`)
  if (start === -1) {
    throw new Error(`Missing function: ${functionName}`)
  }
  const rest = content.slice(start)
  const next = rest.search(/\nconst [a-zA-Z0-9_]+ =/)
  return next === -1 ? rest : rest.slice(0, next)
}

const batchRecordApi = assertFile('src/api/mes/pro/batchrecordreport/index.ts')
assertIncludes(batchRecordApi, 'parseProductionBatchRecordTotalRecognitionJson')
assertIncludes(
  batchRecordApi,
  '/mes/pro/batch-record-report/production-batch-record/total-recognition-json'
)
assertIncludes(batchRecordApi, 'request.upload<{ data: string }>')
assertIncludes(batchRecordApi, 'WORD_IMPORT_REQUEST_TIMEOUT')

const parserPage = assertFile('src/views/form-center/parser/index.vue')
assertIncludes(parserPage, "name: 'FormCenterParser'")
assertIncludes(parserPage, '表单解析')
assertIncludes(parserPage, '生产批记录')
assertIncludes(parserPage, 'QA检验规程')
assertIncludes(parserPage, '过程检验记录')
assertIncludes(parserPage, 'handleProductionBatchRecord')
assertIncludes(parserPage, 'handleUnsupportedParseType')
assertIncludes(parserPage, 'accept=".doc,.docx"')
assertIncludes(parserPage, 'BatchRecordReportApi')
assertIncludes(parserPage, 'parseProductionBatchRecordTotalRecognitionJson')
assertIncludes(parserPage, 'download.json')
assertIncludes(parserPage, "application/json;charset=utf-8")
assertIncludes(parserPage, "'.json'")
assertIncludes(parserPage, 'JSON.stringify')
assertIncludes(parserPage, 'JSON.parse(totalRecognitionJson)')
assertIncludes(parserPage, 'processes')
assertIncludes(parserPage, 'product')

assertIncludes(parserPage, 'form-parser-workbench')
assertIncludes(parserPage, 'form-parser-json-editor')
assertIncludes(parserPage, 'form-parser-json-search')
assertIncludes(parserPage, 'form-parser-frontline-preview')
assertIncludes(parserPage, 'form-parser-frontline-shell')
assertIncludes(parserPage, 'form-parser-frontline-header')
assertIncludes(parserPage, 'form-parser-process-card')
assertIncludes(parserPage, 'form-parser-device-tabs')
assertIncludes(parserPage, 'form-parser-device-tab-card')
assertIncludes(parserPage, 'form-parser-device-tab')
assertIncludes(parserPage, 'form-parser-device-current')
assertIncludes(parserPage, 'form-parser-parameter-field')
assertIncludes(parserPage, 'form-parser-device-tabs device-tabs')
assertIncludes(parserPage, 'form-parser-device-tab-card device-tab-card')
assertIncludes(parserPage, 'form-parser-device-tab device-tab')
assertIncludes(parserPage, 'form-parser-device-tab-selection device-tab-selection')
assertIncludes(parserPage, 'form-parser-device-tab-code device-tab-code')
assertIncludes(parserPage, 'editableRecognitionJson')
assertIncludes(parserPage, 'jsonSearchKeyword')
assertIncludes(parserPage, 'jsonSearchMatchIndex')
assertIncludes(parserPage, 'jsonSearchMatchTotal')
assertIncludes(parserPage, 'jsonSearchMatchLabel')
assertIncludes(parserPage, 'jsonEditorInputRef')
assertIncludes(parserPage, 'locateNextJsonKeyword')
assertIncludes(parserPage, 'findJsonKeywordMatches')
assertIncludes(parserPage, 'resetJsonSearchPosition')
assertIncludes(parserPage, 'previewRecognitionJson')
assertIncludes(parserPage, 'handleApplyEditedJson')
assertIncludes(parserPage, 'downloadCurrentRecognitionJson')
assertIncludes(parserPage, 'currentProcessIndex')
assertIncludes(parserPage, 'currentProcess')
assertIncludes(parserPage, 'processSelectorVisible')
assertIncludes(parserPage, 'visiblePreviewDeviceCards')
assertIncludes(parserPage, 'selectedPreviewDeviceKeys')
assertIncludes(parserPage, 'selectedPreviewDeviceKey')
assertIncludes(parserPage, 'activePreviewDevice')
assertIncludes(parserPage, 'togglePreviewDeviceSelection')
assertIncludes(parserPage, 'goPreviousProcess')
assertIncludes(parserPage, 'goNextProcess')
assertIncludes(parserPage, 'openProcessSelector')
assertIncludes(parserPage, 'handleSelectProcess')
assertIncludes(parserPage, 'validateTotalRecognitionJson')
assertIncludes(parserPage, 'initializePreviewState')
assertIncludes(parserPage, 'parameterPreviewValues')
assertIncludes(parserPage, 'buildParameterPreviewValueKey')
assertIncludes(parserPage, 'handleParameterPreviewValueChange')
assertIncludes(parserPage, 'formatNameCode')
assertIncludes(parserPage, 'formatParameterTargetRange')
assertIncludes(parserPage, 'getParameterDisplayName')
assertIncludes(parserPage, 'isNumberParameterControl')
assertIncludes(parserPage, 'isSelectParameterControl')
assertIncludes(parserPage, 'isTextParameterControl')
assertIncludes(parserPage, 'type="textarea"')
assertIncludes(parserPage, 'v-model="editableRecognitionJson"')
assertIncludes(parserPage, '<el-input-number')
assertIncludes(parserPage, '<el-select')
assertIncludes(parserPage, '<el-dialog')
assertIncludes(parserPage, 'JSON编辑')
assertIncludes(parserPage, '查找关键词')
assertIncludes(parserPage, '查找下一个')
assertIncludes(parserPage, '应用')
assertIncludes(parserPage, '下载当前JSON')
assertIncludes(parserPage, '一线生产预览')
assertIncludes(parserPage, '输入物料')
assertIncludes(parserPage, '输出物料')
assertIncludes(parserPage, '设备')
assertIncludes(parserPage, '设备参数')
assertIncludes(parserPage, '目标范围')
assertIncludes(parserPage, '默认值')
assertIncludes(parserPage, '共 {{ previewRecognitionJson.processes.length }} 个工序')
assertIncludes(parserPage, ':disabled="!canGoPreviousProcess"')
assertIncludes(parserPage, ':disabled="!canGoNextProcess"')
assertIncludes(parserPage, ':style="{ \'--frontline-device-tab-count\': visiblePreviewDeviceCards.length }"')
assertIncludes(parserPage, 'role="checkbox"')
assertIncludes(parserPage, 'activePreviewDevice.parameters')
assertIncludes(parserPage, 'ref="jsonEditorInputRef"')
assertIncludes(parserPage, 'v-model="jsonSearchKeyword"')
assertIncludes(parserPage, '@keyup.enter="locateNextJsonKeyword"')
assertIncludes(parserPage, '@click="locateNextJsonKeyword"')
assertIncludes(parserPage, 'setSelectionRange')
assertIncludes(parserPage, 'scrollTop')
assertIncludes(parserPage, '请输入查找关键词')
assertIncludes(parserPage, '未找到关键词')
assertIncludes(parserPage, '.form-parser-device-tabs')
assertIncludes(parserPage, '.form-parser-device-tab-card')
assertIncludes(parserPage, '.form-parser-device-tab')
assertIncludes(parserPage, '.form-parser-device-metering-validity')
assertIncludes(parserPage, '.form-parser-device-current')
assertIncludes(parserPage, 'grid-template-columns: repeat(var(--frontline-device-tab-count, 1), minmax(0, 1fr))')
assertIncludes(parserPage, 'grid-template-rows: minmax(0, 1fr) 36px')
assertIncludes(parserPage, 'height: 110px')
assertIncludes(parserPage, 'border: 3px solid #cad6d0')
assertIncludes(parserPage, 'background: #20352d')

assertNotIncludes(
  parserPage,
  '<el-tree',
  '表单解析结果不得继续使用树状图，结果区必须是 JSON 编辑 + 一线生产预览'
)
assertNotIncludes(parserPage, 'batchRecordTreeData')
assertNotIncludes(parserPage, 'buildBatchRecordTreeData')
assertNotIncludes(parserPage, 'buildProcessTreeNode')
assertNotIncludes(parserPage, 'buildInputMaterialTreeNode')
assertNotIncludes(parserPage, 'buildOutputMaterialTreeNode')
assertNotIncludes(parserPage, 'buildEquipmentDeviceTreeNodes')
assertNotIncludes(parserPage, 'buildParameterTreeNode')
assertNotIncludes(parserPage, 'buildExtraFieldNodes')
assertNotIncludes(parserPage, 'form-parser-tree')
assertNotIncludes(parserPage, '完整 JSON')
assertNotIncludes(parserPage, 'formatProcessParameterSummary')
assertNotIncludes(parserPage, 'buildOutputEquipmentParameterRows')
assertNotIncludes(
  parserPage,
  'v-for="(equipment, equipmentIndex) in resolveEquipmentOptions(group)"',
  '多设备不应继续把每台设备完整参数卡纵向堆叠'
)
assertNotIncludes(parserPage, '.form-parser-device-group')
assertNotIncludes(parserPage, '.form-parser-device-card')
assertNotIncludes(
  parserPage,
  'form-parser-product-card',
  '一线生产预览顶部不再显示产品信息卡'
)
assertNotIncludes(
  parserPage,
  'currentProductText',
  '隐藏产品信息卡后不得保留未使用的产品卡展示计算'
)
assertNotIncludes(
  parserPage,
  'form-parser-employee-card',
  '一线生产预览顶部不再显示员工信息卡'
)
assertNotIncludes(
  parserPage,
  'form-parser-home-button',
  '一线生产预览顶部不再显示主页按钮'
)
assertNotIncludes(
  parserPage,
  'importTemplateDoc',
  '表单解析页只能做 parse-only JSON 下载，不得复用会创建模板版本的导入接口'
)
assertNotIncludes(parserPage, '/form-center/templates/import-doc')
assertNotIncludes(
  parserPage,
  'parseProductionBatchRecordJson',
  '生产批记录按钮必须下载批记录总识别 JSON，不能继续下载 Jimu 表单 JSON'
)
assertNotIncludes(parserPage, 'FormTemplateParseJsonRespVO')
assertNotIncludes(parserPage, 'recognizedFields')
assertNotIncludes(parserPage, 'jimuSchemaJson')
assertNotIncludes(parserPage, 'recognizedSchemaJson')

const parseHandler = extractFunction(parserPage, 'parseAndDownloadProductionBatchRecord')
assertIncludes(parseHandler, 'parseProductionBatchRecordTotalRecognitionJson')
assertIncludes(parseHandler, 'parseTotalRecognitionJson(totalRecognitionJson)')
assertIncludes(parseHandler, 'editableRecognitionJson.value')
assertIncludes(parseHandler, 'previewRecognitionJson.value')
assertIncludes(parseHandler, 'download.json')

const applyHandler = extractFunction(parserPage, 'handleApplyEditedJson')
assertIncludes(applyHandler, 'parseTotalRecognitionJson(editableRecognitionJson.value)')
assertIncludes(applyHandler, 'previewRecognitionJson.value = mapping')
assertIncludes(applyHandler, 'initializePreviewState(mapping)')
assertIncludes(applyHandler, 'resetJsonSearchPosition()')
assertIncludes(applyHandler, 'message.success')
assertIncludes(applyHandler, 'message.error')
assertNotIncludes(
  applyHandler,
  'parseProductionBatchRecordTotalRecognitionJson',
  '应用左侧 JSON 只能使用本地编辑内容，不得重新调用上传解析 API'
)

const searchMatches = extractFunction(parserPage, 'findJsonKeywordMatches')
assertIncludes(searchMatches, 'editableRecognitionJson.value.indexOf')
assertIncludes(searchMatches, 'jsonSearchKeyword.value.trim()')
assertIncludes(searchMatches, 'matches.push')

const locateKeyword = extractFunction(parserPage, 'locateNextJsonKeyword')
assertIncludes(locateKeyword, 'findJsonKeywordMatches()')
assertIncludes(locateKeyword, 'jsonSearchMatchTotal.value = matches.length')
assertIncludes(locateKeyword, '(jsonSearchMatchIndex.value + 1) % matches.length')
assertIncludes(locateKeyword, 'textarea.setSelectionRange(match.start, match.end)')
assertIncludes(locateKeyword, 'textarea.focus()')
assertIncludes(locateKeyword, 'textarea.scrollTop')
assertIncludes(locateKeyword, 'message.warning')

const activeDevice = extractFunction(parserPage, 'activePreviewDevice')
assertIncludes(activeDevice, "selectedPreviewDeviceKeys.value.includes(selectedPreviewDeviceKey.value || '')")
assertNotIncludes(
  activeDevice,
  '|| visiblePreviewDeviceCards.value.find',
  '当前设备参数区必须只跟随一线生产同款当前设备，不得自动回退展示其它设备'
)

const toggleDevice = extractFunction(parserPage, 'togglePreviewDeviceSelection')
assertIncludes(toggleDevice, 'if (selected.has(device.key))')
assertIncludes(toggleDevice, 'clearPreviewDeviceParameterValues(device)')
assertIncludes(toggleDevice, 'selectedPreviewDeviceKeys.value = [...selected]')
assertIncludes(toggleDevice, "selectedPreviewDeviceKey.value = selectedPreviewDeviceKeys.value[0] || ''")
assertIncludes(toggleDevice, "device.selectionMode === 'SINGLE'")
assertIncludes(toggleDevice, 'visibleDevice.groupIndex === device.groupIndex')
assertIncludes(toggleDevice, 'selectedPreviewDeviceKey.value = device.key')

const parserButtons = parserPage.match(/<el-button[\s\S]*?<\/el-button>/g) || []
const productionButton = parserButtons.find((button) => button.includes('生产批记录'))
const qaButton = parserButtons.find((button) => button.includes('QA检验规程'))
const processButton = parserButtons.find((button) => button.includes('过程检验记录'))
if (!productionButton || !productionButton.includes('handleProductionBatchRecord')) {
  throw new Error('生产批记录按钮必须打开 Word 上传解析流程')
}
if (!qaButton || !qaButton.includes('handleQaInspectionRegulation')) {
  throw new Error('QA检验规程按钮必须打开独立 QA Word 上传解析流程')
}
if (qaButton.includes('handleProductionBatchRecord')) {
  throw new Error('QA检验规程按钮不得调用生产批记录解析流程')
}
if (!processButton || !processButton.includes('handleUnsupportedParseType')) {
  throw new Error('过程检验记录按钮当前只能提示未实现')
}

const sql = assertFile('../IntRuoyiBackend/sql/mysql/20260908_bpm_form_parser_menu.sql')
assertIncludes(sql, "'表单解析'")
assertIncludes(sql, "'form-center/parser'")
assertIncludes(sql, "'form-center/parser/index'")
assertIncludes(sql, "'FormCenterParser'")
assertIncludes(sql, "'form:parser:query'")
assertIncludes(sql, "'form:parser:production-batch-record'")

console.log('form parser json editor frontline preview static contract passed')
