const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const scriptPath = path.join(__dirname, 'dcc-onlyoffice-release-preview-real.e2e.js')

assert.equal(
  fs.existsSync(scriptPath),
  true,
  'OnlyOffice release preview real E2E script must be packaged with the release gate'
)

const source = fs.readFileSync(scriptPath, 'utf8')
const requiredEnvironmentNames = [
  'DCC_ONLYOFFICE_RELEASE_E2E_BASE_URL',
  'DCC_ONLYOFFICE_RELEASE_E2E_TENANT',
  'DCC_ONLYOFFICE_RELEASE_E2E_USERNAME',
  'DCC_ONLYOFFICE_RELEASE_E2E_PASSWORD',
  'DCC_ONLYOFFICE_RELEASE_E2E_DOCX_FILE_ID',
  'DCC_ONLYOFFICE_RELEASE_E2E_XLSX_FILE_ID',
  'DCC_ONLYOFFICE_RELEASE_E2E_PPTX_FILE_ID'
]

for (const name of requiredEnvironmentNames) {
  const requiredCall = name.endsWith('_FILE_ID') ? 'requireFileId' : 'requireEnvironment'
  assert.match(source, new RegExp(`${requiredCall}\\('${name}'\\)`), `${name} must fail fast when missing`)
}

assert.match(source, /\/dcc\/controlled-file\/detail\/\$\{sample\.fileId\}\?viewer=1&from=release-gate/)
assert.match(source, /\/dcc\/controlled-files\/\$\{sample\.fileId\}\/preview-metadata/)
assert.match(source, /\.onlyoffice-viewer-frame iframe/)
assert.match(source, /state: 'attached'/)
assert.match(source, /\/cache\/files\//)
assert.match(source, /禁止截图\/外传/)
assert.match(source, /只读预览态/)
assert.match(source, /DCC write request detected/)
assert.doesNotMatch(source, /DCC_ONLYOFFICE_RELEASE_E2E_PASSWORD\s*\|\|/)
assert.doesNotMatch(source, /DCC_ONLYOFFICE_RELEASE_E2E_(DOCX|XLSX|PPTX)_FILE_ID\s*\|\|/)

console.log('GREEN: dcc-onlyoffice-release-preview-static -> PASS')
