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
  /hasReleaseTransaction\.value\s*&&\s*releasePrecheckPassed\.value\s*&&\s*batchStatus\.value\s*===\s*EDHR_BATCH_STATUS_CLOSED/,
  '放行预检通过、存在放行事务且批次已关闭时必须允许提交放行。'
)

assert.match(
  detailPage,
  /if\s*\(\s*releaseStatus\.value\s*!==\s*'PENDING_APPROVAL'\s*\)\s*\{\s*return\s*\}/,
  'PRECHECK_PASSED 已有放行事务时不能加载平台审批实例。'
)

assert.match(
  detailPage,
  /pendingInstanceId:\s*activeReleaseAction\.value\?\.id/,
  'pendingInstanceId 必须来自审批中的平台动作实例，不能直接使用 PRECHECK_PASSED 放行事务 ID。'
)

assert.match(
  detailPage,
  /const\s+canSubmitRelease\s*=\s*computed\(\(\)\s*=>\s*edhrReleaseActionProjection\.value\.allowed\)/,
  '提交放行按钮必须继续由动作投影 allowed 统一驱动。'
)

console.log('PASS: eDHR release submit projection static contract')
