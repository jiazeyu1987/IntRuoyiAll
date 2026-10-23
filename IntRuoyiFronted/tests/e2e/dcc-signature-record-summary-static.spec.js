const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end + endNeedle.length)
}

const packageJson = JSON.parse(readSource('package.json'))
const signaturePage = readSource('src/views/dcc/controlled-file/signatures/index.vue')
const recordsTable = extractBetween(
  signaturePage,
  '<el-table v-loading="recordLoading"',
  '</el-table>',
  'signature records table'
)
const signaturePdfPreviewDialog = extractBetween(
  signaturePage,
  '<el-dialog',
  '</el-dialog>',
  'signature evidence pdf preview dialog'
)
assert.match(
  signaturePdfPreviewDialog,
  /v-model="signaturePdfPreviewDialog\.visible"/,
  '签名证据预览必须使用正式 PDF 预览弹窗状态'
)
const signaturePreviewHandler = extractBetween(
  signaturePage,
  'const openSignaturePdfPreview = async',
  'const closeAuthorizationActionDialog',
  'signature evidence pdf preview handler'
)

assert.equal(
  packageJson.scripts['e2e:dcc:signature-record-summary:static'],
  'node tests/e2e/dcc-signature-record-summary-static.spec.js',
  'package.json 必须提供签名记录摘要静态契约脚本'
)

const retainedColumns = ['文件名称', '文件编号', '版本', '文件状态', '签名人', '部门/岗位', '角色', '签名摘要', '证据摘要', '操作']
for (const label of retainedColumns) {
  assert.match(recordsTable, new RegExp(`label="${label}"`), `签名记录默认表格必须展示：${label}`)
}

const removedCommonColumns = ['签名动作', '签名含义', '副本状态', '证据状态', '签名时间']
for (const label of removedCommonColumns) {
  assert.doesNotMatch(
    recordsTable,
    new RegExp(`label="${label}"`),
    `签名记录默认表格不应继续横向拆散：${label}`
  )
}

assert.match(
  recordsTable,
  /data-testid="dcc-signature-action-summary"/,
  '签名摘要列必须提供稳定测试标识'
)
assert.match(
  recordsTable,
  /getDccSignatureTaskActionLabel\(row\.taskActionResult\)/,
  '签名摘要必须继续展示签名动作中文标签'
)
assert.match(
  recordsTable,
  /getDccSignatureMeaningLabel\(row\.meaningCode\)/,
  '签名摘要必须继续展示签名含义中文标签'
)
assert.match(recordsTable, /row\.signedAt/, '签名摘要必须继续展示签名时间')
assert.match(
  recordsTable,
  /row\.actorUsernameSnapshot\s*\|\|\s*'旧版证据未记录'/,
  '签名人列必须展示签名当时账号快照，旧证据只能标识未记录'
)
assert.match(
  recordsTable,
  /formatDccSignatureSnapshotValue\(row\.actorDeptNameSnapshot\)/,
  '签名记录必须展示签名当时部门快照'
)
assert.match(
  recordsTable,
  /formatDccSignatureSnapshotValue\(row\.actorPostNamesSnapshot\)/,
  '签名记录必须展示签名当时岗位快照'
)
assert.match(
  recordsTable,
  /formatDccSignatureSnapshotValue\(row\.actorRoleNamesSnapshot\)/,
  '签名记录必须展示签名当时角色快照'
)
assert.match(
  recordsTable,
  /formatDccSignatureSnapshotValue\(row\.signaturePurpose\s*\|\|\s*row\.meaningCode\)/,
  '签名摘要必须展示签名目的快照'
)

assert.match(
  recordsTable,
  /data-testid="dcc-signature-evidence-summary"/,
  '证据摘要列必须提供稳定测试标识'
)
assert.match(
  recordsTable,
  /getDccControlledCopyHashStatusLabel\(row\.controlledCopyHashStatus\)/,
  '证据摘要必须继续展示副本摘要状态'
)
assert.match(
  recordsTable,
  /getDccSignatureEvidenceStatusLabel\(row\.evidenceStatus\)/,
  '证据摘要必须继续展示证据状态'
)

const advancedColumnLabels = ['修订ID', '源文件 hash', '副本 hash', '证据 hash']
for (const label of advancedColumnLabels) {
  const columnPattern = new RegExp(
    `<el-table-column\\s+v-if="isAdvancedSignatureView"\\s+label="${label}"`,
    'm'
  )
  assert.match(recordsTable, columnPattern, `${label} 技术列必须只在高级视图显示`)
}

for (const detailLabel of ['查看证据', '源文件 hash', '副本 hash', '证据 hash']) {
  assert.match(signaturePage, new RegExp(detailLabel), `签名证据功能必须保留：${detailLabel}`)
}

assert.match(
  signaturePdfPreviewDialog,
  /title="签名证据 PDF 预览"/,
  '签名证据弹窗必须直接预览正式 PDF 证据'
)
assert.match(
  signaturePdfPreviewDialog,
  /:src="signaturePdfPreviewDialog\.objectUrl"/,
  '签名证据弹窗必须使用正式证据 PDF objectUrl'
)
assert.match(
  signaturePage,
  /fetchDccSignatureEvidencePdfArtifact\(controlledFileId\)/,
  '查看证据必须调用正式签名证据 PDF API'
)
assert.match(
  signaturePage,
  /URL\.createObjectURL\(artifact\.blob\)/,
  '签名证据 PDF 必须由正式 blob 创建预览 URL'
)
assert.match(
  signaturePage,
  /URL\.revokeObjectURL\(signaturePdfPreviewDialog\.objectUrl\)/,
  '签名证据 PDF 弹窗关闭时必须回收 objectUrl'
)
assert.match(
  signaturePage,
  /openControlledFileViewer\(router,\s*route,\s*controlledFileId,\s*'signature'\)/,
  '签名记录文件入口必须使用共享受控预览导航'
)
assert.doesNotMatch(
  signaturePage,
  /const openControlledFileDetail = \(controlledFileId: number\) => \{[\s\S]*name:\s*'DccControlledFileDetail'/,
  '签名记录文件入口不得继续跳普通文件详情页'
)
assert.match(
  signaturePreviewHandler,
  /signaturePdfPreviewDialog\.target = signature/,
  '签名证据 PDF 预览必须保留当前签名记录上下文'
)

for (const snapshotField of [
  'actorUsernameSnapshot',
  'actorDeptNameSnapshot',
  'actorPostNamesSnapshot',
  'actorRoleNamesSnapshot',
  'signaturePurpose'
]) {
  assert.match(recordsTable, new RegExp(snapshotField), `签名记录摘要必须展示签名快照字段：${snapshotField}`)
}

assert.doesNotMatch(
  recordsTable,
  /mock|placeholder|fallback|降级|吞异常/i,
  '签名记录摘要优化不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC signature record summary static contract')
