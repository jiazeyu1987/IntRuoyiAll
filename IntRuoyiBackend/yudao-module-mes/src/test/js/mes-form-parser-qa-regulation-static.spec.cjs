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

const controller = readFile(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/MesQaInspectionRegulationController.java'
)
assertIncludes(controller, 'MesQaInspectionRegulationParseService')
assertIncludes(controller, '@PostMapping("/form-parser-json")')
assertIncludes(controller, 'parseQaInspectionRegulationJson')
assertIncludes(controller, "@PreAuthorize(\"@ss.hasPermission('form:parser:query')\")")
assertNotIncludes(controller, 'form:parser:qa-inspection-regulation')

const service = readFile(
  'src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationParseService.java'
)
assertIncludes(service, 'MesQaInspectionRegulationWordParser')
assertIncludes(service, 'MesQaInspectionRegulationParseRespVO parseWord')
assertIncludes(service, 'parser.parse(file.getBytes(), fileName)')
assertIncludes(service, '.effectiveDate(parsed.effectiveDate().toString())')
assertIncludes(service, 'groupItemsByProcess')
assertIncludes(service, 'generatedCode')
assertIncludes(service, 'firstInspectionQuantity')
assertIncludes(service, 'patrolInspectionRatio')
assertNotIncludes(service, 'Mapper')
assertNotIncludes(service, 'saveDraft')
assertNotIncludes(service, '@Transactional')

const extraPermissionMigration = path.join(
  root,
  '..',
  'sql',
  'mysql',
  '20260910_bpm_form_parser_qa_regulation_permission.sql'
)
if (fs.existsSync(extraPermissionMigration)) {
  throw new Error('QA parse-only must reuse the form parser page permission without an extra migration')
}

const response = readFile(
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationParseRespVO.java'
)
assertIncludes(response, 'private String effectiveDate;')
assertNotIncludes(response, 'private LocalDate effectiveDate;')
for (const field of [
  'schemaVersion',
  'sourceFileName',
  'regulationCode',
  'regulationName',
  'versionNo',
  'effectiveDate',
  'processes',
  'processCode',
  'processName',
  'itemCode',
  'itemName',
  'inspectionMethod',
  'inspectionTool',
  'samplingPlanText',
  'standardText',
  'applicableInspectionTypes',
  'firstInspectionQuantity',
  'patrolInspectionRatio'
]) {
  assertIncludes(response, field)
}

console.log('form parser QA regulation parse-only backend static contract passed')
