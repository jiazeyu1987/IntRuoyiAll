import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const repoRoot = process.cwd()
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const detailPage = fs.readFileSync(detailPath, 'utf8')

assert.match(
  detailPage,
  /const\s+releasePrecheckPassed\s*=\s*computed\(\(\)\s*=>\s*releaseStatus\.value\s*===\s*'PRECHECK_PASSED'\)/,
  '提交放行投影必须显式识别 PRECHECK_PASSED。'
)

assert.match(
  detailPage,
  /hasReleaseTransaction\.value\s*&&\s*releasePrecheckPassed\.value\s*&&\s*releaseCanSubmitBatchStatus\.value/,
  '放行预检通过、存在放行事务且批次处于待关闭或已关闭状态时必须允许提交放行。'
)

assert.match(
  detailPage,
  /const\s+releaseCanSubmitBatchStatus\s*=\s*computed\([\s\S]*EDHR_BATCH_STATUS_READY_TO_CLOSE[\s\S]*EDHR_BATCH_STATUS_CLOSED/,
  '负责人签名直接放行必须允许 PRECHECK_PASSED 的待关闭批次，并由后端同步冻结批次。'
)

assert.match(
  detailPage,
  /pendingInstanceId:\s*[\s\S]*releasePendingApproval\.value[\s\S]*\?\s*workbench\.value\?\.releaseSummary\?\.releaseTransactionId[\s\S]*:\s*undefined/,
  'pendingInstanceId 只能在 PENDING_APPROVAL 状态设置，不能让 PRECHECK_PASSED 放行事务误判为审批中。'
)

assert.match(
  detailPage,
  /const\s+canSubmitRelease\s*=\s*computed\(\(\)\s*=>\s*edhrReleaseActionProjection\.value\.allowed\)/,
  '提交放行按钮必须继续由动作投影 allowed 统一驱动。'
)

console.log('PASS: eDHR release submit projection static contract')
