const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

const projectionStart = source.indexOf('const edhrReleaseActionProjection = computed')
const projectionEnd = source.indexOf('const edhrVoidActionProjection = computed', projectionStart)
assert.ok(projectionStart >= 0 && projectionEnd > projectionStart, '必须存在 eDHR 放行动作投影。')
const projectionBlock = source.slice(projectionStart, projectionEnd)

assert.ok(
  projectionBlock.includes('pending: releasePendingApproval.value'),
  '只有 PENDING_APPROVAL 才能把放行动作标记为 pending。'
)
assert.ok(
  /pendingInstanceId:\s*[\s\S]*releasePendingApproval\.value[\s\S]*\?\s*workbench\.value\?\.releaseSummary\?\.releaseTransactionId\s*:\s*undefined/.test(projectionBlock),
  '直签放行预检通过时 releaseTransactionId 只是提交所需事务 ID，不得作为 pendingInstanceId 禁用按钮。'
)
assert.ok(
  /pendingStatus:\s*releasePendingApproval\.value\s*\?\s*releaseStatus\.value\s*:\s*undefined/.test(projectionBlock),
  '直签放行预检通过时 pendingStatus 不得保持 PRECHECK_PASSED 造成动作被判定为 pending。'
)
assert.ok(
  !projectionBlock.includes('pendingInstanceId: workbench.value?.releaseSummary?.releaseTransactionId,'),
  '放行动作不得无条件把 releaseTransactionId 传给 pendingInstanceId。'
)

console.log('PASS: eDHR release direct signature action static contract')
