const fs = require('fs')
const path = require('path')

const findWorkspaceRoot = () => {
  const candidates = [
    process.cwd(),
    path.resolve(process.cwd(), '..')
  ]
  const root = candidates.find((candidate) =>
    fs.existsSync(path.join(candidate, 'IntRuoyiFronted')) &&
    fs.existsSync(path.join(candidate, 'IntRuoyiBackend'))
  )
  if (!root) {
    throw new Error('Cannot locate IntRuoyi workspace root')
  }
  return root
}

const workspaceRoot = findWorkspaceRoot()

const readWorkspaceFile = (relativePath) => {
  const absolutePath = path.join(workspaceRoot, relativePath)
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

const assertMatch = (content, expected, message) => {
  if (!expected.test(content)) {
    throw new Error(message || `Expected content to match: ${expected}`)
  }
}

const extractMethodBody = (source, signature) => {
  const signatureIndex = source.indexOf(signature)
  if (signatureIndex < 0) {
    throw new Error(`${signature} must exist`)
  }
  const bodyStart = source.indexOf('{', signatureIndex)
  if (bodyStart < 0) {
    throw new Error(`${signature} body must start`)
  }
  let depth = 0
  for (let index = bodyStart; index < source.length; index++) {
    const current = source[index]
    if (current === '{') {
      depth++
    } else if (current === '}') {
      depth--
      if (depth === 0) {
        return source.slice(bodyStart, index + 1)
      }
    }
  }
  throw new Error(`${signature} body must end`)
}

const parserPage = readWorkspaceFile('IntRuoyiFronted/src/views/form-center/parser/index.vue')
const frontendApi = readWorkspaceFile('IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts')
const controller = readWorkspaceFile(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/MesProBatchRecordReportController.java'
)
const service = readWorkspaceFile(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java'
)
const extractor = readWorkspaceFile(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordTotalRecognitionExtractor.java'
)
const fixture = JSON.parse(readWorkspaceFile('resource/按压式球囊扩充压力泵IDI-001/批记录总对应.json'))

assertIncludes(parserPage, 'form-parser-workbench', 'result area must use the JSON editor + preview workbench')
assertIncludes(parserPage, 'form-parser-json-editor', 'left side must keep the editable JSON module')
assertIncludes(parserPage, 'form-parser-frontline-preview', 'right side must keep the frontline preview module')
assertIncludes(parserPage, 'form-parser-frontline-shell', 'preview must use the frontline production shell')
assertIncludes(parserPage, 'form-parser-process-switcher', 'preview must keep process switching')
assertIncludes(parserPage, 'form-parser-device-tabs device-tabs', 'multi-device area must use frontline device tabs')
assertIncludes(parserPage, 'form-parser-device-tab-card device-tab-card', 'device cards must match frontline card structure')
assertIncludes(parserPage, 'form-parser-device-tab device-tab', 'device card button must match frontline tab structure')
assertIncludes(parserPage, 'form-parser-device-current', 'parameter area must follow the current device')
assertIncludes(parserPage, 'selectedPreviewDeviceKeys', 'preview must track selected device keys like frontline')
assertIncludes(parserPage, 'selectedPreviewDeviceKey', 'preview must track the current device like frontline')
assertIncludes(parserPage, 'togglePreviewDeviceSelection', 'preview must switch device cards on click')
assertIncludes(parserPage, 'activePreviewDevice.parameters', 'parameter controls must render only for the current device')
assertIncludes(parserPage, '<el-input-number', 'number parameters must render as number controls')
assertIncludes(parserPage, '<el-select', 'select parameters must render as select controls')
assertIncludes(parserPage, '目标范围', 'parameter UI must show the target range')
assertIncludes(parserPage, '默认值', 'parameter UI must show the parsed default value')
assertIncludes(parserPage, 'JSON编辑', 'operator must be able to correct recognized JSON')
assertIncludes(parserPage, '应用', 'edited JSON must be appliable to the preview')
assertIncludes(parserPage, '下载当前JSON', 'current JSON must still be downloadable')
assertIncludes(parserPage, '输入物料', 'preview must show input materials')
assertIncludes(parserPage, '输出物料', 'preview must show output materials')
assertIncludes(parserPage, 'grid-template-columns: repeat(var(--frontline-device-tab-count, 1), minmax(0, 1fr))')
assertIncludes(parserPage, 'grid-template-rows: minmax(0, 1fr) 36px')
assertIncludes(parserPage, 'border: 3px solid #cad6d0')
assertIncludes(parserPage, 'background: #20352d')
assertNotIncludes(parserPage, '<el-tree', 'result area must not return to the old tree')
assertNotIncludes(parserPage, '<el-table', 'result area must not fall back to the old table')
assertNotIncludes(parserPage, '<el-collapse', 'result area must not hide JSON in a separate panel')
assertNotIncludes(parserPage, 'form-parser-product-card', 'preview top product card must stay hidden')
assertNotIncludes(parserPage, 'form-parser-employee-card', 'preview top employee card must stay hidden')
assertNotIncludes(parserPage, 'form-parser-home-button', 'preview top home button must stay hidden')
assertNotIncludes(
  parserPage,
  'v-for="(equipment, equipmentIndex) in resolveEquipmentOptions(group)"',
  'multi-device preview must not stack full parameter cards for every device'
)

assertIncludes(frontendApi, 'parseProductionBatchRecordTotalRecognitionJson')
assertIncludes(
  frontendApi,
  "/mes/pro/batch-record-report/production-batch-record/total-recognition-json",
  'frontend must call the production batch record total recognition parse-only endpoint'
)
assertIncludes(frontendApi, 'request.upload<{ data: string }>')
assertNotIncludes(frontendApi, '/form-center/templates/import-doc')

const parseControllerBody = extractMethodBody(controller, 'parseProductionBatchRecordTotalRecognitionJson')
assertIncludes(controller, '@PreAuthorize("@ss.hasPermission(\'form:parser:production-batch-record\')")')
assertIncludes(parseControllerBody, 'batchRecordReportService.parseProductionBatchRecordTotalRecognitionJson(file)')

const parseServiceBody = extractMethodBody(service, 'public String parseProductionBatchRecordTotalRecognitionJson')
const buildTotalBody = extractMethodBody(service, 'private String buildTotalRecognitionJson')
assertIncludes(parseServiceBody, 'validateUploadedRouteDoc(file)')
assertIncludes(parseServiceBody, 'parseWordByFileName(bytes, sourceFileName)')
assertIncludes(parseServiceBody, 'attachDocumentFrame(parsedTables, extractDocumentFrameByFileName(bytes, sourceFileName))')
assertIncludes(parseServiceBody, 'buildTotalRecognitionJson(sourceFileName, parsedTables)')
assertIncludes(buildTotalBody, 'OBJECT_MAPPER.writeValueAsString')
assertIncludes(buildTotalBody, 'new MesProBatchRecordTotalRecognitionExtractor().extract(sourceFileName, parsedTables)')
for (const writeToken of [
  'saveGeneratedReports(',
  'updateProjectCodeTotalRecognitionJson(',
  'recognitionDeviceSyncService',
  'jimuReportGateway',
  'definitionMapper',
  'versionMapper'
]) {
  assertNotIncludes(parseServiceBody, writeToken, `parse-only service body must not call ${writeToken}`)
  assertNotIncludes(buildTotalBody, writeToken, `total recognition serializer must not call ${writeToken}`)
}

assertIncludes(extractor, 'record ProcessRecognition')
assertIncludes(extractor, 'List<EquipmentGroup> equipmentGroups')
assertIncludes(extractor, 'record EquipmentGroup(List<EquipmentOption> equipmentOptions, List<Parameter> parameters')
assertIncludes(extractor, 'record Parameter(String name, String referenceValue, String actualValue, Ui ui)')
assertIncludes(extractor, 'record Ui(String control, Object defaultValue, BigDecimal step, BigDecimal min, BigDecimal max')

if (!fixture.product || fixture.schemaVersion !== 2 || !Array.isArray(fixture.processes)) {
  throw new Error('sample total recognition JSON must expose product/schemaVersion/processes')
}
const processWithParameterUi = fixture.processes.find((process) =>
  Array.isArray(process.equipmentGroups) &&
  process.equipmentGroups.some((group) =>
    Array.isArray(group.parameters) &&
    group.parameters.some((parameter) => parameter.ui && parameter.ui.control && parameter.ui.defaultValue !== undefined)
  )
)
if (!processWithParameterUi) {
  throw new Error('sample total recognition JSON must include equipment parameter UI data')
}

console.log('form parser production batch record flow static contract passed')
