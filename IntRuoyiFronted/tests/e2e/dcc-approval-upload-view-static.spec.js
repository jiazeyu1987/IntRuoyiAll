const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readWorkspace = (relativePath) => {
  const absolutePath = path.join(workspaceRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required workspace file: ${relativePath}`)
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
const approvalCenter = readSource('src/views/approval-center/index.vue')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const dccQueryService = readWorkspace(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java'
)

assert.equal(
  packageJson.scripts['e2e:dcc:approval-upload-view:static'],
  'node tests/e2e/dcc-approval-upload-view-static.spec.js',
  'package.json must expose the DCC approval upload-view static contract'
)

const dccLocationResolver = extractBetween(
  approvalCenter,
  'const resolveDccApprovalDetailLocation = (',
  'const resolveDecisionDetailRoute = (row: ApprovalTaskSummaryVO) => {',
  'approval-center DCC detail resolver'
)
const handlingBranch = extractBetween(
  dccLocationResolver,
  'if (isDccModuleHandling) {',
  "    return {\n      path: normalizedPath,\n      query: {\n        ...nextQuery,\n        viewer: '1'",
  'approval-center DCC handling branch'
)

assert.match(
  handlingBranch,
  /handling:\s*DCC_APPROVAL_HANDLING_MODE/,
  'DCC TODO module handling must navigate with handling=approval'
)
assert.doesNotMatch(
  handlingBranch,
  /viewer:\s*'1'|traceability:\s*'1'/,
  'DCC TODO module handling must not navigate to readonly viewer or traceability mode'
)

assert.match(
  detailPage,
  /const isApprovalUploadHandlingPage = computed/,
  'DCC detail must explicitly model the approval upload handling page'
)
assert.match(
  detailPage,
  /String\(route\.query\.handling \|\| ''\) === 'approval'[\s\S]{0,180}String\(route\.query\.from \|\| ''\) === 'approval-center'/,
  'approval upload handling mode must be parsed from handling=approval and from=approval-center'
)
assert.match(
  detailPage,
  /const showFullDetailSections = computed\(\(\) => !isApprovalUploadHandlingPage\.value\)/,
  'DCC detail must centralize full-detail section visibility away from the approval upload page'
)
assert.match(
  detailPage,
  /const showDetailManagementActions = computed\(\(\) => !isBrowserTraceabilityPage\.value && showFullDetailSections\.value\)/,
  'general detail actions must be hidden from the upload approval page'
)
assert.match(
  detailPage,
  /const showLifecycleTraceSections = computed\([\s\S]*showFullDetailSections\.value[\s\S]*traceabilityScope\.value === 'trace'/,
  'lifecycle trace sections must not render in approval upload handling mode'
)
assert.match(
  detailPage,
  /const showSignatureTraceSections = computed\([\s\S]*showFullDetailSections\.value[\s\S]*traceabilityScope\.value === 'signature'/,
  'signature trace sections must not render in approval upload handling mode'
)

const approvalUploadTemplate = extractBetween(
  detailPage,
  '<template v-if="isApprovalUploadHandlingPage">',
  '<template v-if="showLifecycleTraceSections">',
  'DCC approval upload view template'
)
const approvalUploadView = extractBetween(
  approvalUploadTemplate,
  'data-testid="dcc-approval-upload-view"',
  'data-testid="dcc-approval-upload-action-panel"',
  'DCC approval upload information block'
)
const approvalUploadActionPanel = extractBetween(
  approvalUploadTemplate,
  'data-testid="dcc-approval-upload-action-panel"',
  '</template>',
  'DCC approval upload action panel'
)

for (const requiredText of [
  '上传提交信息',
  '提交范围',
  '文件信息',
  '审批要求',
  '附件预览',
  'DCC 项目',
  '文件分类',
  '提交目录',
  '文件名称',
  '文件编号',
  '产品编号',
  '版本号',
  '生效日期',
  '提交备注',
  '培训要求',
  '审批阶段'
]) {
  assert.ok(approvalUploadView.includes(requiredText), `approval upload view must show upload-page field: ${requiredText}`)
}

assert.match(
  approvalUploadView,
  /data-testid="dcc-approval-upload-submission-summary"/,
  'approval upload page must expose a stable upload information test id'
)
assert.match(
  approvalUploadView,
  /data-testid="dcc-approval-upload-file-preview"[\s\S]*<ProtectedPdfViewer[\s\S]*:controlled-file-id="controlledFileId"/,
  'approval upload page must render inline file preview through the formal controlled-file preview chain'
)
assert.doesNotMatch(
  approvalUploadView,
  /preview-blob|onlyofficeDocumentUrl|publishedFileId|stampedFileId/,
  'approval upload page must not guess preview blobs or trace evidence file ids on the frontend'
)
assert.match(
  approvalUploadActionPanel,
  /approvalTodoTask[\s\S]*openActionDialog\('approve'\)[\s\S]*openActionDialog\('reject'\)/,
  'approval upload page must keep the current task approval/reject actions visible'
)
assert.match(
  approvalUploadActionPanel,
  /openTaskActionDialog\('return'\)[\s\S]*openTaskActionDialog\('transfer'\)[\s\S]*openTaskActionDialog\('sign'\)/,
  'approval upload page must keep return/transfer/sign task actions governed by the existing handlers'
)

for (const forbiddenMarker of [
  'dcc-detail-lifecycle-timeline',
  'dcc-detail-route-snapshot-section',
  'dcc-detail-version-history',
  'dcc-detail-distribution-section',
  'dcc-controlled-print-records',
  'dcc-detail-training-section',
  'dcc-detail-signature-trace-section',
  'dcc-detail-signature-section',
  'dcc-detail-controlled-browser-linkage',
  'dcc-detail-publish-completion-summary'
]) {
  assert.ok(
    !approvalUploadTemplate.includes(forbiddenMarker),
    `approval upload page must not render full traceability marker: ${forbiddenMarker}`
  )
}

assert.match(
  dccQueryService,
  /if \(accessType == DccAccessTypeEnum\.PREVIEW && isPendingPreviewStatus\(file\.getStatus\(\)\)\) \{[\s\S]*return file\.getOriginalFileId\(\)/,
  'backend preview contract must keep pending approval preview on the uploaded original file'
)

assert.doesNotMatch(
  approvalUploadTemplate,
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'approval upload view must not introduce mock data, fallback, downgrade, swallowed errors, or default success'
)

console.log('PASS: DCC approval upload view static contract')
