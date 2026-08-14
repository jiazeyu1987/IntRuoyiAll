const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')

const readSource = (relativePath, root = repoRoot) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${absolutePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const approvalCenter = readSource('src/views/approval-center/index.vue')
const approvalCenterApi = readSource('src/api/approval-center/index.ts')
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const approvalActions = readSource('src/views/dcc/controlled-file/detail/approval-actions.ts')
const dccRespVO = readSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java',
  workspaceRoot
)
const queryService = readSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java',
  workspaceRoot
)
const approvalSummary = readSource(
  'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/ApprovalTaskSummary.java',
  workspaceRoot
)
const dccApprovalAdapter = readSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/approval/DccApprovalTaskAdapter.java',
  workspaceRoot
)

assert.equal(
  packageJson.scripts['e2e:dcc:upload-governance-ux:static'],
  'node tests/e2e/dcc-upload-governance-ux-static.spec.js',
  'package.json must expose the DCC upload governance UX static contract'
)

const uploadPreflightPanel = extractBetween(
  uploadPage,
  'data-testid="dcc-upload-preflight-panel"',
  '</section>',
  'upload preflight readiness panel'
)
for (const label of ['文件编号/版本', '文件类别', '审批人链路', '受控浏览目录']) {
  assert.match(uploadPreflightPanel, new RegExp(label), `upload preflight must show ${label}`)
}
for (const sourceToken of [
  'uploadPreflightChecks',
  'isRequestedVersionDuplicate',
  'currentVersionInfo',
  'selectedCategory.value',
  'selectedUploadDirectoryPath',
  'routeReadiness.value?.ready',
  'routeReadiness.value.blockers',
  'checkControlledFileRouteReadiness',
  'selectedSignoffUserIds'
]) {
  assert.match(uploadPage, new RegExp(sourceToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `upload preflight must use formal source: ${sourceToken}`)
}

for (const field of ['publishedArtifactAvailable', 'stampedArtifactAvailable', 'previewUnavailableReason']) {
  assert.match(workflowApi, new RegExp(`${field}\\?:`), `frontend detail VO must expose business artifact state: ${field}`)
  assert.match(dccRespVO, new RegExp(`private (?:Boolean|String) ${field};`), `backend detail VO must expose business artifact state: ${field}`)
  assert.match(queryService, new RegExp(`respVO\\.set${field[0].toUpperCase()}${field.slice(1)}\\(`), `query service must project business artifact state: ${field}`)
}

const controlledBrowserSection = extractBetween(
  detailPage,
  'data-testid="dcc-detail-controlled-browser-linkage"',
  '</ContentWrap>',
  'detail controlled browser linkage section'
)
for (const label of ['受控浏览入口', '最终目录路径', '发布文件', '盖章文件', 'master 当前生效版本']) {
  assert.match(controlledBrowserSection, new RegExp(label), `detail controlled browser linkage must show ${label}`)
}
for (const token of [
  'openControlledBrowserLocation',
  'directoryPathMap',
  'fileDetail.value?.publishedArtifactAvailable',
  'fileDetail.value?.stampedArtifactAvailable',
  'fileDetail?.currentActiveVersionNo',
  'buildControlledFileViewerPath'
]) {
  assert.match(detailPage, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `detail controlled browser linkage must use ${token}`)
}

const signatureTraceSection = extractBetween(
  detailPage,
  'data-testid="dcc-detail-signature-trace-section"',
  '</ContentWrap>',
  'detail signature trace section'
)
for (const label of ['上传人', '四级审批人', '签名时间', '签名方式', '证据状态', '文件哈希', '盖章文件']) {
  assert.match(signatureTraceSection, new RegExp(label), `signature trace section must show ${label}`)
}
for (const removedLabel of ['导出', '打印', '汇总上传人']) {
  assert.doesNotMatch(
    signatureTraceSection,
    new RegExp(removedLabel),
    `signature trace yellow-box control must be removed: ${removedLabel}`
  )
}
for (const token of [
  'signatureTraceRows',
  'fileDetail?.signatureSummaries',
  'fileDetail?.requesterId'
]) {
  assert.match(detailPage, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `signature trace must use ${token}`)
}
for (const removedToken of ['exportSignatureTrace', 'printSignatureTrace']) {
  assert.doesNotMatch(detailPage, new RegExp(removedToken), `signature trace must remove ${removedToken}`)
}

assert.match(approvalSummary, /private List<String> businessContextTags/, 'approval summary must expose business context tags')
assert.match(approvalCenterApi, /businessContextTags\?: string\[\]/, 'approval-center API type must expose business context tags')
assert.match(dccApprovalAdapter, /buildDccBusinessContextTags/, 'DCC approval adapter must build business context tags')
for (const token of ['文件编号：', '版本：', '分类：', '当前节点：', '盖章：', '分发：']) {
  assert.match(dccApprovalAdapter, new RegExp(token), `DCC adapter must populate ${token}`)
}
const approvalBusinessSummary = extractBetween(
  approvalCenter,
  'v-if="isApprovalColumnVisible(\'businessSummary\')"',
  'v-if="isApprovalColumnVisible(\'node\')"',
  'approval-center business summary column'
)
assert.match(
  approvalBusinessSummary,
  /data-testid="approval-center-dcc-business-context"/,
  'approval center must render stable DCC business context tags'
)
assert.match(
  approvalBusinessSummary,
  /row\.businessContextTags/,
  'approval center row must render backend-provided business context tags'
)

for (const diagnostic of [
  '电子签名未授权',
  '签名图片失效',
  '当前密码错误',
  '证据快照失败'
]) {
  assert.match(approvalActions, new RegExp(diagnostic), `signature failure diagnostic must include ${diagnostic}`)
}
assert.match(
  approvalActions,
  /resolveDccApprovalSignatureErrorMessage/,
  'approval action helper must centralize signature failure diagnostics'
)
assert.match(
  detailPage,
  /resolveDccApprovalSignatureErrorMessage/,
  'detail approval submit must use diagnostic signature failure resolver'
)

assert.doesNotMatch(
  uploadPreflightPanel + controlledBrowserSection + signatureTraceSection + approvalBusinessSummary,
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'governance UX must not introduce mock, fallback, downgrade, swallowed errors, or default success'
)

console.log('PASS: DCC upload governance UX static contract')
