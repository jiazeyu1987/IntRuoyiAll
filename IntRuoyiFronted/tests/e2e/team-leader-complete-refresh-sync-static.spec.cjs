const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs
  .readFileSync(path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const releaseFlowStart = page.indexOf('const releaseApplicationIdempotencyKeys')
const releaseFlowEnd = page.indexOf('const submitMoveActiveOrder', releaseFlowStart)
assert.notStrictEqual(releaseFlowStart, -1, 'missing active-order release flow start')
assert.notStrictEqual(releaseFlowEnd, -1, 'missing active-order release flow end')
const releaseFlow = page.slice(releaseFlowStart, releaseFlowEnd)

assert.match(
  releaseFlow,
  /const applyActiveOrderReleaseReceiptToRow\s*=\s*\(\s*row:\s*TeamLeaderActiveOrderRespVO,\s*result:\s*TeamLeaderActiveOrderReleaseApplyRespVO\s*\)\s*=>\s*\{[\s\S]*row\.releaseApplicationId\s*=\s*result\.applicationId[\s\S]*row\.pqcReleaseWorkTaskId\s*=\s*result\.pqcReleaseWorkTaskId[\s\S]*row\.releaseApplicationStatus\s*=\s*result\.status[\s\S]*row\.releaseSourceSnapshotHash\s*=\s*result\.sourceSnapshotHash[\s\S]*row\.releaseApplicationVersion\s*=\s*result\.version/,
  'formal completion receipt must be projected into the active-order row'
)
assert.match(
  releaseFlow,
  /await loadActiveOrders\(\)[\s\S]*applyActiveOrderReleaseReceiptToRow\(refreshedReceipt,\s*result\)/,
  'the refreshed row must retain the formal receipt when the list projection is briefly stale'
)
assert.doesNotMatch(
  releaseFlow,
  /CONFIRMED_NOT_PROJECTED|申请已提交，但列表状态尚未同步，请刷新页面/,
  'a stale list projection must not be shown as a user-facing error after a confirmed write'
)

console.log('PASS: team leader complete refresh sync static contract')
