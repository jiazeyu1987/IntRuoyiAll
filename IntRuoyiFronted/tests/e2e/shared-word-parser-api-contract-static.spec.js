const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const templateApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/form-center/template.ts'),
  'utf8'
)
const batchRecordApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/batchrecordreport/index.ts'),
  'utf8'
)

const templateImport = extract(
  templateApi,
  'export const importTemplateDoc',
  'export const saveTemplateJimuSchema'
)
assert.match(templateImport, /request\.upload<FormTemplateImportRespVO>/)
assert.match(templateImport, /url:\s*['"]\/form-center\/templates\/import-doc['"]/)
assert.doesNotMatch(templateImport, /\/mes\/pro\/batch-record-report\//)

const uploadedRoute = extract(batchRecordApi, 'recognizeUploadedRoute:', 'preflightUploadedRoute:')
assert.match(uploadedRoute, /request\.upload/)
assert.match(uploadedRoute, /url:\s*['"]\/mes\/pro\/batch-record-report\/recognize-uploaded['"]/)
assert.doesNotMatch(uploadedRoute, /\/form-center\/templates\/import-doc|upload-extra-slot/)

const extraSlot = extract(batchRecordApi, 'uploadExtraFormSlot:', 'existsBatchRecordName:')
assert.match(extraSlot, /request\.upload/)
assert.match(extraSlot, /url:\s*['"]\/mes\/pro\/batch-record-report\/upload-extra-slot['"]/)
assert.doesNotMatch(extraSlot, /\/form-center\/templates\/import-doc|recognize-uploaded/)

for (const source of [templateImport, uploadedRoute, extraSlot]) {
  assert.doesNotMatch(source, /fallback|retry|catch\s*\(/i)
}

function extract(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker)
  const end = source.indexOf(endMarker, start + startMarker.length)
  assert.notEqual(start, -1, `Missing API method marker: ${startMarker}`)
  assert.notEqual(end, -1, `Missing API method boundary: ${endMarker}`)
  return source.slice(start, end)
}

console.log('PASS: shared Word parser keeps all three business API contracts independent')
