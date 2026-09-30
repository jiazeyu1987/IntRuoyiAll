const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const upload = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/upload/index.vue'), 'utf8')
const browser = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/browser/index.vue'), 'utf8')
const detail = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')

assert.match(upload, /uploadSessionBinding/, 'upload retry must retain the raw/scoped session correspondence')
assert.doesNotMatch(upload, /previewUpload\.value\?\.sessionId === uploadSessionId/, 'upload retry must not compare scoped session to raw client session')
assert.match(upload, /uploadPreviewLoading\.value = false/, 'source reset must release preview loading')
assert.match(upload, /uploadDrawingPdfLoading\.value = false/, 'drawing PDF reset must release loading')
assert.match(upload, /uploadDirectoryRequestSeq/, 'directory loading must have a request generation')
assert.match(upload, /requestSequence !== uploadDirectoryRequestSeq/, 'stale directory responses must be ignored')
assert.ok(
  upload.includes('const WINDCHILL_VERSION_PATTERN = /^[A-Z]+(?:\\/[1-9]\\d*)+$/i'),
  'controlled upload must accept configured multi-segment versions'
)

assert.match(browser, /resolveWorkingIterationRouteAction/, 'browser submit must derive route action from the selected working version')
assert.match(browser, /revisionBaseActiveControlledFileId/, 'browser route selection must use the formal active baseline projection')
assert.match(browser, /buildCurrentVersionOption[\s\S]*revisionBaseActiveControlledFileId: row\.revisionBaseActiveControlledFileId/, 'current browser version must preserve the formal active baseline projection')
assert.doesNotMatch(browser, /assertRevisionRouteReadiness\(row\)/, 'browser submit must not always preflight REVISION')
assert.match(browser, /parseWindchillVersion[\s\S]*split\('\/'\)/, 'browser version parser must preserve all version segments')

assert.match(detail, /applicantTrainingRecordUploadSequence/, 'training upload must have a request generation')
assert.match(detail, /isCurrentApplicantTrainingRecordUpload/, 'stale training upload responses must be rejected')
assert.match(detail, /cleanupControlledFileUploadTicket\(sessionId, uploaded\.uploadTicket, uploaded\.requestId\)/, 'stale training upload must be cleaned by its own ticket')
assert.match(detail, /canReadControlledFileAuxiliaries = checkPermi\(\['dcc:controlled-file:query'\]\)/, 'detail auxiliaries must not block file-level authorized readers')
assert.match(detail, /loadActiveObsoleteAction\(detail, sequence, requestedId, requestedRoute\)/, 'detail action loads must carry the current load generation')
assert.match(detail, /isCurrentDetailLoad\(sequence, requestedId, requestedRoute\)/, 'detail auxiliary writes must check the current load generation')
assert.match(detail, /培训记录已上传，流程已进入人工分发/, 'training success feedback must name the actual next stage')

console.log('PASS: DCC upload/revision/browse general fixes static contract')
