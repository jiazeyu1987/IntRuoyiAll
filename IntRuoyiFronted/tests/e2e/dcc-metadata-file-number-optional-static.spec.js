const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const metadataDialogSource = readSource(
  'src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue'
)
const workflowApiSource = readSource('src/api/dcc/controlledFile/workflow.ts')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.equal(
  packageJson.scripts['e2e:dcc:metadata-file-number-optional:static'],
  'node tests/e2e/dcc-metadata-file-number-optional-static.spec.js',
  'package.json must expose the DCC metadata optional file number static contract'
)

const fileNumberFormItem = extractBetween(
  metadataDialogSource,
  '<el-form-item label="文件编号"',
  '</el-form-item>'
)
assert.match(fileNumberFormItem, /clearable/, 'File number remains editable and clearable')
assert.match(fileNumberFormItem, /placeholder="请输入文件编号"/, 'File number keeps its input hint')

const validateBlock = extractBetween(
  metadataDialogSource,
  'const validateMetadataDialog = () => {',
  'const buildMetadataPayload'
)
assert.ok(validateBlock.includes("errors.fileName = '请输入文件名称'"), 'File name remains required')
assert.ok(
  !validateBlock.includes('errors.fileNumber'),
  'Metadata dialog must not block saving when fileNumber is blank'
)
assert.ok(
  !validateBlock.includes('请输入文件编号'),
  'Metadata dialog must not show a required error for blank fileNumber'
)
assert.match(
  metadataDialogSource,
  /fileNumber:\s*metadataForm\.fileNumber\.trim\(\)/,
  'Blank file number should still be submitted as the trimmed user value'
)
assert.match(
  extractBetween(
    workflowApiSource,
    'export interface ControlledFileSubmitReqVO {',
    'export interface ControlledFileMetadataUpdateReqVO {'
  ),
  /fileNumber:\s*string/,
  'Submit API type remains unchanged because this task only changes metadata update'
)
assert.match(
  extractBetween(
    workflowApiSource,
    'export interface ControlledFileMetadataUpdateReqVO {',
    'export interface ControlledFileProjectCodeRecognitionRespVO {'
  ),
  /fileNumber\?:\s*string\s*\|\s*null/,
  'Metadata update API type must mark fileNumber as optional because some files have no number'
)

console.log('PASS: DCC metadata file number optional static contract')
