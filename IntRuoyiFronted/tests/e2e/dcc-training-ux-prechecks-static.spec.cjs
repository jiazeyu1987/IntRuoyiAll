const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const packageJson = JSON.parse(readSource('package.json'))
const trainingTaskPage = readSource('src/views/dcc/controlled-file/training/task/index.vue')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const detailPresentation = readSource('src/views/dcc/controlled-file/detail/presentation.ts')
const trainingRulesReadonlyTab = readSource(
  'src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue'
)
const categoryTrainingRulesTab = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryTrainingRulesTab.vue'
)

assert.equal(
  packageJson.scripts['e2e:dcc:training-ux-prechecks:static'],
  'node tests/e2e/dcc-training-ux-prechecks-static.spec.cjs',
  'package.json must expose the DCC training UX prechecks static contract'
)

assert.match(
  trainingTaskPage,
  /data-testid="dcc-training-task-countability-state"/,
  'training task page must expose a stable countability status block'
)
assert.match(
  trainingTaskPage,
  /data-testid="dcc-training-task-acknowledge-reason"/,
  'training task page must expose a stable acknowledgement disabled reason block'
)
assert.match(
  trainingTaskPage,
  /trainingCountabilityState/,
  'training task page must compute a countability state'
)
assert.match(
  trainingTaskPage,
  /acknowledgeDisabledReason/,
  'training task page must compute the acknowledgement disabled reason'
)

const trainingTaskScript = extractBetween(trainingTaskPage, '<script lang="ts" setup>', '</script>')
for (const token of [
  'documentVisible',
  'windowFocused',
  'previewBlob.value',
  'remainingSeconds.value',
  'document.visibilityState',
  'window.document.hasFocus()'
]) {
  assert.match(trainingTaskScript, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `training countability logic must use ${token}`)
}
for (const readableText of [
  '页面不在前台或窗口未聚焦',
  '文件预览未加载完成',
  '还需阅读',
  '已达标，可确认培训完成'
]) {
  assert.match(trainingTaskPage, new RegExp(readableText), `training task page must explain: ${readableText}`)
}

const detailTrainingSection = extractBetween(
  detailPage,
  'data-testid="dcc-detail-training-section"',
  '</ContentWrap>'
)
assert.match(
  detailTrainingSection,
  /data-testid="dcc-detail-training-completion-overview"/,
  'detail training section must show a completion overview'
)
assert.match(
  detailTrainingSection,
  /data-testid="dcc-detail-training-pending-users"/,
  'detail training section must show pending users'
)
assert.match(
  detailPage,
  /trainingCompletionSummary/,
  'detail page must compute the training completion summary'
)
assert.match(
  detailPresentation,
  /export const getDetailTrainingCompletionSummary/,
  'detail presentation must export the training completion summary helper'
)
for (const token of ['totalCount', 'completedCount', 'pendingCount', 'pendingNamesText', 'latestAcknowledgedAtText']) {
  assert.match(detailPage + detailPresentation, new RegExp(token), `training completion summary must expose ${token}`)
}

assert.match(
  detailPage,
  /data-testid="dcc-manual-release-permission-gap"/,
  'detail page must expose a manual release permission gap hint'
)
assert.match(
  detailPage,
  /manualReleasePermissionGapVisible/,
  'detail page must compute manual release permission gap visibility'
)
assert.match(
  detailPage,
  /PENDING_MANUAL_DISTRIBUTION/,
  'manual release permission gap must only target pending manual distribution status'
)
assert.match(
  detailPage,
  /!detailActionState\.value\.canManualRelease/,
  'manual release permission gap must be shown when the formal release action is unavailable'
)
for (const text of ['DISTRIBUTE', '正式下发权限', '分发规则']) {
  assert.match(detailPage, new RegExp(text), `manual release permission hint must mention ${text}`)
}

for (const [source, label] of [
  [trainingRulesReadonlyTab, 'training rules readonly mapping'],
  [categoryTrainingRulesTab, 'category training rules editor']
]) {
  assert.match(
    source,
    /data-testid="dcc-training-rule-permission-precheck"/,
    `${label} must expose a stable training permission precheck hint`
  )
  assert.match(
    source,
    /dcc:controlled-file:training:mine/,
    `${label} must name the required my-training permission`
  )
  assert.match(
    source,
    /无法进入我的培训完成阅读确认/,
    `${label} must explain the business impact of missing permission`
  )
}

const forbiddenTerms = [/mock/i, /placeholder data/i, /吞异常/]
for (const term of forbiddenTerms) {
  assert.doesNotMatch(trainingTaskScript, term, `training task UX must not introduce ${term}`)
}

console.log('PASS: DCC training UX prechecks static contract')
