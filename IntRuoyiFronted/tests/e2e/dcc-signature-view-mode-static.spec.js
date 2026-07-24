const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const signaturePage = readSource('src/views/dcc/controlled-file/signatures/index.vue')
const recordsTableStart = signaturePage.indexOf('<el-table v-loading="recordLoading"')
assert.notEqual(recordsTableStart, -1, 'signature records table must exist')
const recordsTableEnd = signaturePage.indexOf('</el-table>', recordsTableStart)
assert.notEqual(recordsTableEnd, -1, 'signature records table must have a closing tag')
const recordsTable = signaturePage.slice(recordsTableStart, recordsTableEnd + '</el-table>'.length)

assert.equal(
  packageJson.scripts['e2e:dcc:signature-view-mode:static'],
  'node tests/e2e/dcc-signature-view-mode-static.spec.js',
  'package.json must expose the DCC signature view mode static contract'
)

assert.doesNotMatch(
  signaturePage,
  /data-testid="dcc-signature-view-mode"/,
  'signature page must remove the common/advanced view switch outside the user red box'
)
assert.doesNotMatch(signaturePage, /常用视图|高级视图/, 'signature page must remove view-mode labels')
assert.doesNotMatch(
  signaturePage,
  /signatureViewModeOptions|type SignatureViewMode|const signatureViewMode/,
  'signature page must remove view-mode state that no longer has a visible control'
)
assert.match(
  signaturePage,
  /const isAdvancedSignatureView = computed\(\(\) => false\)/,
  'signature page must stay in the fixed common view after removing the switch'
)

const commonColumnLabels = [
  '文件名称',
  '文件编号',
  '版本',
  '文件状态',
  '签名人',
  '签名摘要',
  '证据摘要'
]
for (const label of commonColumnLabels) {
  assert.match(recordsTable, new RegExp(`label="${label}"`), `common view must keep ${label}`)
}

const removedCommonColumnLabels = ['签名动作', '签名含义', '副本状态', '证据状态', '签名时间']
for (const label of removedCommonColumnLabels) {
  assert.doesNotMatch(
    recordsTable,
    new RegExp(`label="${label}"`),
    `common view must summarize ${label} instead of keeping it as a standalone column`
  )
}

const advancedFilterLabels = ['修订ID', '证据 hash']
for (const label of advancedFilterLabels) {
  const filterPattern = new RegExp(`<el-form-item\\s+v-if="isAdvancedSignatureView"\\s+label="${label}"`, 'm')
  assert.doesNotMatch(signaturePage, filterPattern, `${label} filter must be removed with the advanced view controls`)
}

const advancedColumnLabels = ['修订ID', '源文件 hash', '副本 hash', '证据 hash']
for (const label of advancedColumnLabels) {
  const columnPattern = new RegExp(
    `<el-table-column\\s+v-if="isAdvancedSignatureView"\\s+label="${label}"`,
    'm'
  )
  assert.match(signaturePage, columnPattern, `${label} column must only render in advanced view`)
}

const detailRequiredLabels = [
  '任务ID',
  '源文件 hash',
  '副本 hash',
  '证据 hash',
  '载荷版本',
  '算法/密钥',
  '校验结果',
  '字段顺序',
  '规范载荷'
]
for (const label of detailRequiredLabels) {
  assert.match(signaturePage, new RegExp(label), `signature detail must keep ${label}`)
}

assert.doesNotMatch(
  signaturePage,
  /signature-view-toolbar|signature-view-mode/,
  'signature page must remove the view-mode toolbar styles and classes'
)
const fixedViewModeLogic = signaturePage.match(/const isAdvancedSignatureView = computed\(\(\) => false\)/)
assert.ok(fixedViewModeLogic, 'fixed common-view logic must exist')
assert.doesNotMatch(
  fixedViewModeLogic[0],
  /mock|placeholder|fallback|降级|吞异常/i,
  'signature view-mode removal must not introduce mock, fallback, downgrade, or swallowed-error behavior'
)

console.log('PASS: DCC signature fixed common-view static contract')
