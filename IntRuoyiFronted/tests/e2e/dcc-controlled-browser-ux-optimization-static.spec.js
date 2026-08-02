const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
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
for (const actionLabel of ['预览当前有效版', '查看版本追溯', '查看签核证据']) {
  assert.match(tableTemplate, new RegExp(actionLabel), `browser row action must use explicit label: ${actionLabel}`)
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
  '高级信息：publishedFileId',
  '高级信息：stampedFileId'
]) {
  assert.match(viewerModeTemplate, new RegExp(token), `viewer metadata must be business-readable: ${token}`)
}

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
