const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:upload-name-version-autofill:static'],
  'node tests/e2e/dcc-upload-name-version-autofill-static.spec.js',
  'package.json must expose the DCC upload name/version autofill static contract'
)

assert.match(
  workflowApi,
  /getControlledFileUploadNameOptions = async \(\s*params:\s*\{\s*dccProjectCodeId:\s*number\s*fileTypeTaxonomyId:\s*number\s*\}/,
  'Upload name options API must be filtered by DCC project and file type taxonomy'
)
assert.match(
  workflowApi,
  /url:\s*'\/dcc\/controlled-files\/upload-name-options'[\s\S]*params/,
  'Upload name options API must call the backend upload-name-options endpoint with params'
)
for (const field of ['controlledFileId?: number | null', 'fileNumber?: string | null']) {
  assert.match(workflowApi, new RegExp(field.replace(/[?|]/g, '\\$&')), `Name option must include ${field}`)
}

assert.match(
  uploadPage,
  /<el-autocomplete[\s\S]*v-model="formData\.fileName"[\s\S]*@select="handleHistoryFileNameSelect"[\s\S]*@input="handleFileNameInput"/,
  'File name must remain a selectable and manually editable autocomplete input'
)
assert.match(
  uploadPage,
  /:trigger-on-focus="canLoadUploadNameOptions"/,
  'File name dropdown must only open after DCC project and file classification are both valid'
)
assert.match(
  uploadPage,
  /const DEFAULT_MANUAL_VERSION_NO = 'V1\.0'/,
  'Manual file name input must default version to V1.0'
)
assert.match(
  uploadPage,
  /const resolveNextMajorVersionNo = \(currentVersionNo: string \| null \| undefined\)/,
  'Existing file selection must compute the next major version'
)
assert.match(
  uploadPage,
  /majorVersion \+ 1[\s\S]*`V\$\{nextMajorVersion\}\.0`/,
  'Next version must use major-version increment, for example V1.0 to V2.0'
)
assert.match(
  uploadPage,
  /effectiveDate:\s*resolveTodayDate\(\)/,
  'Effective date must default to today when the upload form is created'
)
assert.match(
  uploadPage,
  /const ensureUploadNameOptionsLoaded = async \(\) => \{[\s\S]*await loadUploadNameOptions\(formData\.dccProjectCodeId, formData\.fileTypeTaxonomyId\)/,
  'Upload page must lazily load file name options by selected DCC project and file classification'
)
assert.match(
  uploadPage,
  /const queryUploadNameSuggestions = async \([\s\S]*await ensureUploadNameOptionsLoaded\(\)[\s\S]*callback\(suggestions\)/,
  'File name suggestions must load history on demand when the user opens or queries the autocomplete'
)
assert.match(
  uploadPage,
  /const handleProjectCodeChange = async \(\) => \{[\s\S]*resetUploadNameContext\(true\)[\s\S]*\}/,
  'Changing DCC project must clear stale file name options without calling the history API eagerly'
)
assert.doesNotMatch(
  uploadPage,
  /const handleProjectCodeChange = async \(\) => \{[\s\S]*refreshUploadNameOptionsForProjectTaxonomy\(\)/,
  'Changing DCC project must not eagerly call upload-name-options'
)
assert.doesNotMatch(
  uploadPage,
  /const handleFileTypeTaxonomyChange = async \(\) => \{[\s\S]*refreshUploadNameOptionsForProjectTaxonomy\(\)/,
  'Changing file classification must not eagerly call upload-name-options'
)
assert.match(
  uploadPage,
  /formData\.versionNo = resolveNextMajorVersionNo\(selectedHistoryVersion\.value\)/,
  'Selecting an existing file name must default version to the next major version'
)
assert.match(
  uploadPage,
  /formData\.versionNo = DEFAULT_MANUAL_VERSION_NO/,
  'Manual file name input path must reset version to V1.0'
)

console.log('PASS: DCC upload file name/version autofill static contract')
