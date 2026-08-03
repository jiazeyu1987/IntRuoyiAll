const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')

const readSource = (relativePath, root = repoRoot) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const browserPresentation = readSource('src/views/dcc/controlled-file/browser/presentation.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const controlledFileRespVo = readSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java',
  workspaceRoot
)
const queryService = readSource(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java',
  workspaceRoot
)

for (const token of [
  'data-testid="dcc-controlled-browser-filter-summary"',
  '当前筛选条件',
  '受控浏览目录路径',
  '普通受控浏览默认仅展示当前有效版'
]) {
  assert.match(browserPage, new RegExp(token), `browser page must show filter/path summary: ${token}`)
}

const tableTemplate = extractBetween(browserPage, '<el-table', '</el-table>', 'browser table')
for (const token of [
  'data-testid="dcc-browser-current-active-row-summary"',
  '版本号',
  '目录路径',
  '发布文件',
  '盖章文件'
]) {
  assert.match(tableTemplate, new RegExp(token), `browser row must show current active metadata: ${token}`)
}
const browserRowActionBlock = extractBetween(
  tableTemplate,
  '<div class="browser-row-actions">',
  '<el-button\n                  v-if="getBrowserRowActionState(getSelectedVersion(row)).canPrint"',
  'browser row primary actions'
)
const expectedActionLabels = ['预览', '追溯', '签核', '下载']
let previousActionLabelIndex = -1
for (const actionLabel of expectedActionLabels) {
  const actionLabelIndex = browserRowActionBlock.indexOf(`>${actionLabel}\n`)
  assert.notEqual(actionLabelIndex, -1, `browser row primary action must use compact label: ${actionLabel}`)
  assert.ok(
    actionLabelIndex > previousActionLabelIndex,
    `browser row primary action label order must be ${expectedActionLabels.join(' -> ')}`
  )
  previousActionLabelIndex = actionLabelIndex
}
for (const forbiddenActionLabel of ['预览当前有效版', '查看版本追溯', '查看签核证据']) {
  assert.doesNotMatch(
    browserRowActionBlock,
    new RegExp(forbiddenActionLabel),
    `browser row primary action must not keep old long label: ${forbiddenActionLabel}`
  )
}
for (const handlerName of ['openPreview', 'openDetail', 'openSignatureEvidence', 'openDownload']) {
  assert.match(browserRowActionBlock, new RegExp(`${handlerName}\\(`), `browser row action must keep handler: ${handlerName}`)
}
const fileNumberColumn = extractBetween(
  browserPage,
  'prop="fileNumber"',
  '<el-table-column\n            v-if="isDccBrowserColumnVisible(\'directory\')"',
  'browser file number column'
)
for (const token of ['版本号', '目录路径', '发布文件', '盖章文件', 'currentVersionSource']) {
  assert.match(
    fileNumberColumn,
    new RegExp(token),
    `file number column must keep current-active metadata visible when optional columns are hidden: ${token}`
  )
}
assert.match(
  browserPresentation,
  /getBrowserPublishedFileStatusText/,
  'browser presentation helper must provide business-readable published file status'
)
assert.match(
  browserPresentation,
  /getBrowserStampedFileStatusText/,
  'browser presentation helper must provide business-readable stamped file status'
)
const currentActiveSummaryBlock = extractBetween(
  browserPage,
  'const getBrowserCurrentActiveRowSummary = (row: ControlledFileBrowserRow) => {',
  'const hasBrowserMoreActions',
  'browser current active row summary'
)
assert.match(
  currentActiveSummaryBlock,
  /getBrowserPublishedFileStatusText\(selectedVersion\)/,
  'browser current active row summary must read published file status from selected current version'
)
assert.match(
  currentActiveSummaryBlock,
  /getBrowserStampedFileStatusText\(selectedVersion\)/,
  'browser current active row summary must read stamped file status from selected current version'
)
assert.match(
  currentActiveSummaryBlock,
  /getBrowserCurrentVersionSourceText\(selectedVersion\)/,
  'browser current active row summary must read current version source from selected current version'
)
const currentVersionOptionBlock = extractBetween(
  browserPage,
  'const buildCurrentVersionOption = (row: ControlledFileVO): ControlledFileBrowserVersion => ({',
  'const getVersionOptions',
  'browser current version option'
)
for (const field of ['publishedFileId', 'stampedFileId', 'currentActiveVersionNo']) {
  assert.match(
    currentVersionOptionBlock,
    new RegExp(`${field}: row\\.${field}`),
    `browser current version option must carry ${field} from browser-page row`
  )
}
const browserRespBlock = extractBetween(
  queryService,
  'private DccControlledFileRespVO toBrowserRespVO(Long userId, DccControlledFileDO file,',
  'private DccControlledFileActionProjectionRespVO buildActionProjection',
  'backend controlled browser response projection'
)
for (const field of ['PublishedFileId', 'StampedFileId', 'CurrentActiveVersionNo']) {
  assert.match(
    browserRespBlock,
    new RegExp(`respVO\\.set${field}\\(`),
    `backend browser-page response must project ${field}`
  )
}
assert.match(
  controlledFileRespVo,
  /private String directoryPath;/,
  'controlled file response must expose formal directoryPath for viewer linkage'
)
assert.match(
  workflowApi,
  /directoryPath\?: string \| null/,
  'frontend controlled file API type must carry formal directoryPath'
)
const detailRespBlock = extractBetween(
  queryService,
  'private DccControlledFileRespVO toRespVO(Long userId, DccControlledFileDO file, boolean includeRouteSnapshots,',
  'private DccControlledFileRespVO toBrowserRespVO(Long userId, DccControlledFileDO file)',
  'backend controlled file detail response projection'
)
for (const block of [
  ['detail response', detailRespBlock],
  ['browser response', browserRespBlock]
]) {
  assert.match(
    block[1],
    /respVO\.setDirectoryPath\(/,
    `backend ${block[0]} must project directoryPath`
  )
}
assert.match(
  browserPage,
  /data-testid="dcc-browser-permission-empty-state"/,
  'browser empty state must have a stable permission/no-match test id'
)
assert.match(
  browserPage,
  /无权限或无匹配当前有效文件/,
  'browser empty state must explicitly mention permission or no current-active match'
)

const viewerModeTemplate = extractBetween(
  detailPage,
  '<ContentWrap v-if="viewerMode">',
  '<template v-else>',
  'controlled browser viewer template'
)
for (const token of [
  '发布文件',
  '盖章文件',
  '当前有效版来源',
  '最终目录路径',
  '高级信息：publishedFileId',
  '高级信息：stampedFileId'
]) {
  assert.match(viewerModeTemplate, new RegExp(token), `viewer metadata must be business-readable: ${token}`)
}
const controlledBrowserDirectoryPathBlock = extractBetween(
  detailPage,
  'const controlledBrowserDirectoryPath = computed(() => {',
  'const publishedFileBusinessText',
  'controlled browser directory path computed'
)
assert.match(
  controlledBrowserDirectoryPathBlock,
  /fileDetail\.value\?\.directoryPath/,
  'viewer controlled browser directory path must prefer formal detail directoryPath before tree fallback'
)

const publishSummaryTemplate = extractBetween(
  detailPage,
  'data-testid="dcc-detail-publish-completion-summary"',
  '</ContentWrap>',
  'publish completion summary'
)
assert.match(publishSummaryTemplate, /可见范围说明/, 'publish completion summary must show visibility scope')
assert.match(detailPage, /publishVisibilityScopeText/, 'publish completion summary must use visibility scope source text')

const uploadPreflightPanel = extractBetween(
  uploadPage,
  'data-testid="dcc-upload-preflight-panel"',
  '</section>',
  'upload preflight readiness panel'
)
assert.match(uploadPreflightPanel, /浏览权限范围/, 'upload preflight must show browse permission scope')
assert.match(uploadPage, /controlledBrowserPermissionScopeText/, 'upload preflight must use formal browse permission scope text')

assert.doesNotMatch(
  browserPage + browserPresentation + detailPage + uploadPage,
  /mock|placeholder data|默认成功|吞异常/i,
  'controlled browser UX optimization must not introduce mock data, default success, or swallowed errors'
)

console.log('PASS: DCC controlled browser UX optimization static contract')
