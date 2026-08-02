const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

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

const packageJson = JSON.parse(readSource('package.json'))
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const processViewer = readSource('src/components/bpmnProcessDesigner/package/designer/ProcessViewer.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:revision-publish-ux-final:static'],
  'node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs',
  'package.json must expose the final DCC revision publish UX static contract'
)

const inlineVersionHistory = extractBetween(
  detailPage,
  'v-if="isVersionHistoryVisibleToReader(fileDetail?.status)"',
  '</ContentWrap>',
  'inline version history table'
)
const previewVersionDialog = extractBetween(
  detailPage,
  '<el-dialog\n    v-model="previewInfoDialogs.version"',
  '</el-dialog>',
  'preview version history dialog'
)

assert.match(previewVersionDialog, /title="版本历史"/, 'preview version dialog title must be 版本历史')
for (const block of [inlineVersionHistory, previewVersionDialog]) {
  assert.match(block, /label="升版原因\/变更说明"/, 'version history must show revision reason/change description')
  assert.match(
    block,
    /data-testid="dcc-detail-version-history-change-reason"/,
    'version history reason cells must expose a stable test id'
  )
  assert.match(
    block,
    /getVersionChangeReasonText\(row\)/,
    'version history reason cells must render from the formal row remark helper'
  )
}
assert.match(
  detailPage,
  /const getVersionChangeReasonText = \(version: ControlledFileVersionHistoryVO\) =>/,
  'detail page must define a version change reason helper'
)
assert.match(
  detailPage,
  /String\(version\.remark \|\| ''\)\.trim\(\)/,
  'version change reason helper must use versionHistory row remark'
)

const publishCompletionSummary = extractBetween(
  detailPage,
  'data-testid="dcc-detail-publish-completion-summary"',
  '</ContentWrap>',
  'publish completion summary'
)
for (const label of ['发布完成结果', '新版 ACTIVE', '旧版 SUPERSEDED', 'master 当前生效版本', '受控浏览落位']) {
  assert.match(publishCompletionSummary, new RegExp(label), `publish completion summary must show ${label}`)
}
for (const token of [
  'isPublishCompletionSummaryVisible',
  'publishCompletionSummaryItems',
  'supersededPredecessorVersions',
  'currentActiveVersionNo',
  'publishedFileId',
  'stampedFileId',
  'controlledBrowserDirectoryPath',
  'supersededByFileId'
]) {
  assert.match(detailPage, new RegExp(token), `publish completion summary must use ${token}`)
}
assert.match(
  publishCompletionSummary,
  /v-for="item in publishCompletionSummaryItems"/,
  'publish completion summary must render a stable item list'
)

assert.match(
  processViewer,
  /data-testid="bpm-process-viewer-warning"/,
  'BPM process viewer must expose visible warning for incomplete highlighting'
)
for (const token of [
  'processViewWarning',
  'missingProcessMarkerIds',
  'recordMissingProcessMarker',
  'safeAddProcessMarker',
  'safeRemoveProcessMarker',
  'elementRegistry.get'
]) {
  assert.match(processViewer, new RegExp(token), `BPM process viewer marker guard must include ${token}`)
}
assert.doesNotMatch(
  processViewer,
  /finishedTaskActivityIds\.forEach\(\(item: any\) => canvas\.addMarker\(item, 'success'\)\)/,
  'BPM process viewer must not call canvas.addMarker directly for unchecked task ids'
)

console.log('PASS: DCC revision publish final UX static contract')
