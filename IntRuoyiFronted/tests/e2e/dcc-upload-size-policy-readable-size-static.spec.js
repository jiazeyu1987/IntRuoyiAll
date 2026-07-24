const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const filePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8('package.json'))
const globalDialog = readUtf8(
  'src/views/dcc/controlled-file/categories/components/UploadSizePolicyDialog.vue'
)
const categoryDialog = readUtf8(
  'src/views/dcc/controlled-file/categories/components/CategoryUploadSizePolicyDialog.vue'
)
const managementStatic = readUtf8('tests/e2e/dcc-upload-size-policy-management-static.spec.js')
const realFlowScript = readUtf8('tests/e2e/dcc-upload-size-policy-real-flow.e2e.js')
const setupScript = readUtf8('tests/e2e/dcc-upload-size-policy-real-setup.e2e.js')

assert.equal(
  packageJson.scripts?.['e2e:dcc:upload-policy:readable-size:static'],
  'node tests/e2e/dcc-upload-size-policy-readable-size-static.spec.js',
  'package.json must expose the upload size policy readable-size static gate'
)

function assertReadableSizeDialog(source, name) {
  assert.ok(source.includes('label="最大大小"'), `${name} must use readable max-size label`)
  assert.ok(!source.includes('label="最大字节"'), `${name} must not expose raw max-byte label`)
  assert.ok(!source.includes('label="最大字节数"'), `${name} must not expose raw max-byte-count label`)
  assert.ok(source.includes('formatPolicySize'), `${name} must format maxBytes into human-readable size`)
  assert.ok(source.includes('formatExactBytes'), `${name} must preserve exact byte value for audit`)
  assert.ok(
    source.includes('data-testid="dcc-upload-size-policy-readable-preview"'),
    `${name} must show a readable preview near the maxBytes input`
  )
  assert.ok(
    source.includes('最大大小不能为空'),
    `${name} validation copy must match the readable max-size label`
  )
  assert.ok(
    source.includes('KB') && source.includes('MB') && source.includes('GB'),
    `${name} formatter must cover common readable size units`
  )
}

assertReadableSizeDialog(globalDialog, 'global upload size policy dialog')
assertReadableSizeDialog(categoryDialog, 'category upload size policy dialog')

assert.ok(
  !/<el-table-column[^>\n]*label="最大大小"[^>\n]*prop="maxBytes"/.test(globalDialog) &&
    !/<el-table-column[^>\n]*label="最大大小"[^>\n]*prop="maxBytes"/.test(categoryDialog),
  'max-size table columns must render formatted content instead of raw prop-only bytes'
)
assert.ok(
  managementStatic.includes('最大大小') && !managementStatic.includes('最大字节数'),
  'management static gate must assert the new readable max-size copy'
)
assert.ok(
  realFlowScript.includes("'最大大小'") && !realFlowScript.includes("'最大字节'"),
  'real upload policy flow must locate the renamed max-size form item'
)
assert.ok(
  setupScript.includes("'最大大小'") && !setupScript.includes("'最大字节数'"),
  'real upload policy setup must locate the renamed max-size form item'
)

console.log('PASS: DCC upload size policy readable-size static contract is present')
