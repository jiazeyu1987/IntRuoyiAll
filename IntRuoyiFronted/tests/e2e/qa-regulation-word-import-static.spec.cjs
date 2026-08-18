const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const compact = (source) => source.replace(/\s+/g, '')

const qaPageSource = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const apiSource = read('src/api/mes/qc/template/index.ts')
const compactPage = compact(qaPageSource)
const compactApi = compact(apiSource)

const headerStart = qaPageSource.indexOf('<div class="qa-regulation-page__header">')
const headerEnd = qaPageSource.indexOf('v-if="dccProjectCodeLoadError"', headerStart)
assert.ok(headerStart >= 0 && headerEnd > headerStart, 'QA header must exist.')
const headerSource = qaPageSource.slice(headerStart, headerEnd)

assert.match(
  headerSource,
  /data-qa-regulation-word-import[\s\S]*解析/,
  'The QA header must expose the Word parse command next to formal actions.'
)
assert.match(
  qaPageSource,
  /data-qa-regulation-word-import-dialog/,
  'The parse command must open an owned dialog.'
)
assert.match(qaPageSource, /accept="\.docx"/, 'The upload control must accept DOCX files only.')
assert.match(
  qaPageSource,
  /data-qa-regulation-word-import-project/,
  'The dialog must contain a formal DCC project selector.'
)
assert.ok(
  compactApi.includes('exportinterfaceQaInspectionRegulationImportRespVO{'),
  'The frontend API must declare the import response contract.'
)
assert.ok(
  compactApi.includes("url:`/mes/qa/inspection-regulation/import-word-draft`"),
  'The frontend API must call the formal Word import endpoint.'
)
assert.ok(
  compactApi.includes("headersType:'multipart/form-data'"),
  'The Word import request must use multipart form data.'
)
assert.ok(
  compactPage.includes("formData.append('file',qaWordImportFile.value)"),
  'The dialog must submit the selected file.'
)
assert.ok(
  compactPage.includes("formData.append('dccProjectCodeId',String(dccProjectCodeId))"),
  'The dialog must submit the selected DCC project identity.'
)
assert.ok(
  compactPage.includes("qaActiveTab.value='items'"),
  'A successful import must switch to the inspection-items tab.'
)

const importHandlerStart = qaPageSource.indexOf('const submitQaWordImport')
const importHandlerEnd = qaPageSource.indexOf('\n}', importHandlerStart)
assert.ok(importHandlerStart >= 0 && importHandlerEnd > importHandlerStart, 'Import handler must exist.')
const importHandler = qaPageSource.slice(importHandlerStart, importHandlerEnd)
assert.doesNotMatch(
  importHandler,
  /publishQaRegulation|runQaPublishPrecheck/,
  'Word import must save a draft without publishing.'
)

console.log('PASS qa-regulation-word-import-static')
