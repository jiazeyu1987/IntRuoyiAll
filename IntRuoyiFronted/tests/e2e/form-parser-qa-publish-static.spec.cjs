const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const qaPanelPath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/views/form-center/parser/components/QaInspectionRegulationParserPanel.vue'
)
const qcTemplateApiPath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/api/mes/qc/template/index.ts'
)
const qaControllerPath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/MesQaInspectionRegulationController.java'
)
const qaServicePath = path.join(
  repoRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationJsonPublishService.java'
)
const batchRecordApiPath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts'
)

const qaPanel = fs.readFileSync(qaPanelPath, 'utf8')
const qcTemplateApi = fs.readFileSync(qcTemplateApiPath, 'utf8')
const qaController = fs.readFileSync(qaControllerPath, 'utf8')
const batchRecordApi = fs.readFileSync(batchRecordApiPath, 'utf8')

assert(
  qaPanel.includes('openQaPublishDialog'),
  'QA parser publish button must open the real publish dialog, not a placeholder'
)
assert(
  !qaPanel.includes('handleQaPublishPlaceholder'),
  'QA parser must remove the placeholder publish handler'
)
assert(
  qaPanel.includes('getProjectCodePage') && qaPanel.includes('DCC_PROJECT_CODE_STATUS_ENABLE'),
  'QA publish dialog must load enabled DCC project codes from the DCC project code API'
)
assert(
  qaPanel.includes('qaPublishProjectCodeId'),
  'QA publish dialog must require a selected DCC project code'
)
assert(
  qaPanel.includes('publishQaInspectionRegulationJson'),
  'QA publish confirm must call the QA JSON publish API'
)

const confirmStart = qaPanel.indexOf('const confirmQaPublish')
assert(confirmStart >= 0, 'QA parser must have a confirmQaPublish handler')
const confirmEndCandidates = [
  qaPanel.indexOf('\nconst ', confirmStart + 1),
  qaPanel.indexOf('\nfunction ', confirmStart + 1),
  qaPanel.indexOf('\ndefineExpose', confirmStart + 1)
].filter((index) => index > confirmStart)
const confirmEnd = Math.min(...confirmEndCandidates)
const confirmBlock = qaPanel.slice(confirmStart, confirmEnd)
assert(
  !confirmBlock.includes('downloadQaCurrentJson') &&
    !confirmBlock.includes('download.json') &&
    !confirmBlock.includes('createObjectURL'),
  'QA publish confirm must not download or create a JSON file'
)
assert(
  !confirmBlock.includes('publishBatchRecordTotalRecognition'),
  'QA publish confirm must not call the production batch record publish API'
)

assert(
  qcTemplateApi.includes('export interface QaInspectionRegulationJsonPublishReqVO') &&
    qcTemplateApi.includes('export interface QaInspectionRegulationJsonPublishRespVO'),
  'QC template API must expose typed QA JSON publish request and response contracts'
)
assert(
  qcTemplateApi.includes('/mes/qa/inspection-regulation/form-parser-json/publish'),
  'QC template API must post to the formal QA JSON publish endpoint'
)
assert(
  qaController.includes('@PostMapping("/form-parser-json/publish")') &&
    qaController.includes('publishFormParserJson'),
  'QA controller must expose a form-parser-json publish endpoint'
)
assert(
  fs.existsSync(qaServicePath),
  'Backend must implement a dedicated QA JSON publish service'
)
const qaService = fs.readFileSync(qaServicePath, 'utf8')
assert(
  qaService.includes('publishFormParserJson') &&
    /\bdccProjectCodeId\b/.test(qaService) &&
    /recognitionJson/i.test(qaService),
  'QA JSON publish service must publish by project code from the current recognition JSON'
)
assert(
  qaService.includes('regulationService.publish'),
  'QA JSON publish service must reuse the formal QA regulation publish chain'
)
assert(
  qaService.includes('item.setCritical(Boolean.FALSE)'),
  'QA JSON publish must default omitted critical flags instead of deriving binding from JSON codes'
)
assert(
  !qaService.includes('MesProBatchRecord') &&
    !qaService.includes('Route') &&
    !qaService.includes('formBindings'),
  'QA JSON publish service must stay isolated from batch record, route, and form slot chains'
)
assert(
  batchRecordApi.includes('publishTotalRecognitionJson'),
  'Production batch record API remains separate and should not be reused by QA publish'
)

console.log('form-parser QA publish static contract passed')
