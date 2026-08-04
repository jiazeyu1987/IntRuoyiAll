const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const packageJson = read('package.json')
const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const uploadPage = read('src/views/dcc/controlled-file/upload/index.vue')
const protectedViewer = read('src/views/dcc/controlled-file/view/index.vue')
const uploadForbiddenFieldsBlock = workflowApi.match(
  /const DCC_UPLOAD_RESPONSE_FORBIDDEN_FIELDS = \[[\s\S]*?\] as const/
)?.[0] || ''

assert.match(
  packageJson,
  /"e2e:dcc:upload-onlyoffice-document-url:static"\s*:\s*"node tests\/e2e\/dcc-upload-onlyoffice-document-url-static\.spec\.js"/,
  'package.json must expose the DCC upload OnlyOffice document URL static contract'
)

assert.match(
  workflowApi,
  /export interface ControlledFileUploadRespVO[\s\S]*onlyofficeDocumentUrl\?: string/,
  'DCC upload response type must expose signed onlyofficeDocumentUrl'
)

assert.match(
  workflowApi,
  /parseControlledFileUploadResp[\s\S]*onlyofficeDocumentUrl:\s*readOptionalString\(payload,\s*'onlyofficeDocumentUrl'\)/,
  'DCC upload parser must read signed onlyofficeDocumentUrl from backend response'
)

assert.doesNotMatch(
  uploadForbiddenFieldsBlock,
  /'onlyofficeDocumentUrl'/,
  'DCC upload parser must not reject signed OnlyOffice document URLs as forbidden raw file capability fields'
)

assert.match(
  uploadPage,
  /:onlyoffice-document-url="previewUpload\.onlyofficeDocumentUrl"/,
  'DCC upload page must pass upload-preview onlyofficeDocumentUrl into ProtectedPdfViewer'
)

assert.match(
  protectedViewer,
  /onlyofficeDocumentUrl\?: string/,
  'Protected viewer props must accept an explicit OnlyOffice document URL for upload-preview mode'
)

assert.match(
  protectedViewer,
  /resolvedOnlyOfficeDocumentUrl\.value = props\.onlyofficeDocumentUrl \|\| ''/,
  'Protected viewer must initialize OnlyOffice document URL from props before metadata loading'
)

assert.match(
  protectedViewer,
  /props\.onlyofficeDocumentUrl/,
  'Protected viewer watcher must reload when OnlyOffice document URL prop changes'
)

console.log('PASS: DCC upload OnlyOffice document URL static contract')
