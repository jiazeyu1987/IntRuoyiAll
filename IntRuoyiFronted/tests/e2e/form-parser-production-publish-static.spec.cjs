const fs = require('fs')
const path = require('path')

const workspaceRoot = path.resolve(process.cwd(), '..')

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

const extractFunction = (content, functionName) => {
  const start = content.indexOf(`const ${functionName} =`)
  if (start === -1) {
    throw new Error(`Missing function: ${functionName}`)
  }
  const rest = content.slice(start)
  const next = rest.search(/\nconst [a-zA-Z0-9_]+ =/)
  return next === -1 ? rest : rest.slice(0, next)
}

const parserPage = readWorkspaceFile('IntRuoyiFronted/src/views/form-center/parser/index.vue')
const batchRecordApi = readWorkspaceFile('IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts')
const projectCodeApi = readWorkspaceFile('IntRuoyiFronted/src/api/dcc/controlledFile/projectCodes.ts')
const qaPanel = readWorkspaceFile(
  'IntRuoyiFronted/src/views/form-center/parser/components/QaInspectionRegulationParserPanel.vue'
)

assertIncludes(projectCodeApi, 'getProjectCodePage')
assertIncludes(projectCodeApi, 'DCC_PROJECT_CODE_STATUS_ENABLE')

assertIncludes(batchRecordApi, 'BatchRecordTotalRecognitionPublishReqVO')
assertIncludes(batchRecordApi, 'BatchRecordTotalRecognitionPublishResultVO')
assertIncludes(batchRecordApi, 'publishTotalRecognitionJson')
assertIncludes(
  batchRecordApi,
  "/mes/pro/batch-record-report/total-recognition-json/publish",
  'frontend publish API must call the JSON publish endpoint'
)
assertIncludes(batchRecordApi, 'request.post<BatchRecordTotalRecognitionPublishResultVO>')

assertIncludes(parserPage, 'getProjectCodePage')
assertIncludes(parserPage, 'DCC_PROJECT_CODE_STATUS_ENABLE')
assertIncludes(parserPage, 'publishDialogVisible')
assertIncludes(parserPage, 'publishProjectCodeOptions')
assertIncludes(parserPage, 'publishSelectedDccProjectCodeId')
assertIncludes(parserPage, 'publishSubmitting')
assertIncludes(parserPage, 'publishResult')
assertIncludes(parserPage, 'publishErrorMessage')
assertIncludes(parserPage, 'handleOpenProductionPublishDialog')
assertIncludes(parserPage, 'loadPublishProjectCodes')
assertIncludes(parserPage, 'handleConfirmProductionPublish')
assertIncludes(parserPage, 'BatchRecordReportApi.publishTotalRecognitionJson')
assertIncludes(parserPage, '选择 DCC 项目代码')
assertIncludes(parserPage, '项目代码')
assertIncludes(parserPage, '确认发布')
assertIncludes(parserPage, '发布结果')
assertIncludes(parserPage, '发布')
assertIncludes(parserPage, '@click="handleOpenProductionPublishDialog"')
assertIncludes(parserPage, 'parseTotalRecognitionJson(editableRecognitionJson.value)')
assertIncludes(parserPage, 'recognitionJson: parsedJson')
assertIncludes(parserPage, 'dccProjectCodeId: publishSelectedDccProjectCodeId.value')
assertIncludes(parserPage, 'message.success')
assertIncludes(parserPage, 'message.error')

const publishHandler = extractFunction(parserPage, 'handleConfirmProductionPublish')
assertIncludes(publishHandler, 'if (!publishSelectedDccProjectCodeId.value)')
assertIncludes(publishHandler, 'parseTotalRecognitionJson(editableRecognitionJson.value)')
assertIncludes(publishHandler, 'BatchRecordReportApi.publishTotalRecognitionJson')
assertIncludes(publishHandler, 'publishResult.value = result')
assertIncludes(publishHandler, 'publishErrorMessage.value')
assertIncludes(publishHandler, 'finally')
assertNotIncludes(
  publishHandler,
  'downloadCurrentRecognitionJson',
  'publishing must not trigger JSON download'
)
assertNotIncludes(publishHandler, 'download.json', 'publishing must not synthesize a JSON download')

const applyHandler = extractFunction(parserPage, 'handleApplyEditedJson')
assertNotIncludes(applyHandler, 'publishTotalRecognitionJson', '应用按钮不得发布路线')

assertIncludes(qaPanel, 'handleQaPublishPlaceholder')
assertIncludes(qaPanel, '发布功能待实现')
assertNotIncludes(qaPanel, 'publishTotalRecognitionJson')
assertNotIncludes(qaPanel, 'getProjectCodePage')

console.log('form parser production publish static contract passed')
