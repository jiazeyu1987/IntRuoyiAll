const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => {
  const fullPath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(fullPath), `${relativePath} must exist`)
  return fs.readFileSync(fullPath, 'utf8')
}

const api = read('src/api/mes/pro/edhr/release.ts')
const tracePage = read('src/views/mes/pro/edhr-release/ReleasePage.vue')
const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

for (const fragment of [
  '/mes/pro/edhr-release/page',
  '/mes/pro/edhr-release/get',
  '/mes/pro/edhr-release/precheck',
  '/mes/pro/edhr-release/check-item/page',
  'EdhrReleaseRowVO',
  'EdhrReleaseCheckItemVO',
  'batchExecutionId',
  'releaseTransactionId',
  'blockingCheckCount'
]) {
  assert.ok(api.includes(fragment), `API contract missing ${fragment}`)
}

for (const forbidden of ['interveneRelease', '人工豁免']) {
  assert.ok(!api.includes(forbidden), `API must not expose ${forbidden}`)
  assert.ok(!tracePage.includes(forbidden), `Trace page must not expose ${forbidden}`)
  assert.ok(!detailPage.includes(forbidden), `Batch detail page must not expose ${forbidden}`)
}

for (const fragment of [
  '电子批记录放行追溯',
  '检查项',
  'blockingCheckCount',
  'precheckSummary',
  'getEdhrReleasePage',
  'getEdhrReleaseCheckItemPage',
  'resolveErrorMessage',
  'loadError',
  'actionError',
  'edhr-release-page__toolbar',
  'edhr-release-page__table',
  'edhr-release-page__drawer'
]) {
  assert.ok(tracePage.includes(fragment), `Release trace page missing ${fragment}`)
}

for (const fragment of [
  '放行预检',
  '执行放行预检',
  'precheckEdhrRelease',
  'canRunReleasePrecheck',
  'handleReleasePrecheck',
  'releaseActionLocked',
  'releaseActionLockMessage',
  'pendingVoidChangeEventId',
  'pendingVoidActionLocked',
  'pendingVoidActionLockMessage',
  '作废申请待处理，只能撤回作废申请。',
  'batchActionLocked',
  'batchActionLockMessage',
  'canGenerateArchive',
  'canQualityReject',
  'canAttemptClose',
  'canRequestReopen',
  'canOpenArchivePrintDrawer',
  'releaseCheckDrawerVisible'
]) {
  assert.ok(detailPage.includes(fragment), `Batch detail release action area missing ${fragment}`)
}

const closeProjectionBlock = detailPage.match(/const closeProjectionState = computed\([\s\S]*?\n\)/)?.[0] || ''
assert.ok(
  closeProjectionBlock.includes('detail.value?.canClose === true'),
  '关闭批次投影必须以批次详情 canClose 作为批次级门禁，不能只依赖任务级 CLOSE action'
)
assert.ok(
  detailPage.includes('const hasBatchCloseAction = computed') &&
    closeProjectionBlock.includes('hasBatchCloseAction.value'),
  '关闭批次投影仍需兼容任务级 CLOSE action 投影'
)

const canAttemptCloseBlock = detailPage.match(/const canAttemptClose = computed\([\s\S]*?\n\)/)?.[0] || ''
assert.ok(
  canAttemptCloseBlock.includes('releasePrecheckPassed.value'),
  '关闭批次按钮必须要求放行预检通过，不能回退成先关闭后预检'
)
assert.ok(
  detailPage.includes('const closePrecheckRequiredBeforeClose = computed') &&
    detailPage.includes('请先执行并通过放行预检，再关闭批次。'),
  '预检未通过时，关闭批次必须给出明确前置门禁提示'
)

const canRunReleasePrecheckBlock = detailPage.match(/const canRunReleasePrecheck = computed\([\s\S]*?\n\)/)?.[0] || ''
assert.ok(
  canRunReleasePrecheckBlock.includes('batchStatus.value === EDHR_BATCH_STATUS_READY_TO_CLOSE'),
  '放行预检必须在待关闭状态执行，先于关闭批次'
)
assert.ok(
  canRunReleasePrecheckBlock.includes("'PRECHECK_PASSED', 'PENDING_APPROVAL', 'RELEASED'"),
  '已预检通过、审批中或已放行状态不得重复执行普通放行预检'
)

for (const forbidden of [
  'catch {}',
  'catch (e) {}',
  'interveneRelease',
  '标记完成'
]) {
  assert.ok(!tracePage.includes(forbidden), `Release trace page must not contain forbidden shortcut ${forbidden}`)
  assert.ok(!detailPage.includes(forbidden), `Batch detail page must not contain forbidden shortcut ${forbidden}`)
}

console.log('PASS: eDHR release precheck base static contract')
