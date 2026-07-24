const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const batchApi = readSource('src/api/mes/pro/edhr/batchExecution.ts')
const batchDetailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const batchListPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')

assert.match(batchApi, /EDHR_BATCH_STATUS_REWORK_REQUIRED\s*=\s*25/, '批次 API 必须保留普通返工状态 25。')
assert.match(batchApi, /EDHR_BATCH_STATUS_REJECTED\s*=\s*50/, '批次 API 必须声明质量拒收状态 50。')
assert.match(batchApi, /EdhrBatchExecutionQualityRejectReqVO/, '批次 API 必须声明质量拒收请求类型。')
assert.match(batchApi, /qualityRejectEdhrBatchExecution/, '批次 API 必须提供质量拒收方法。')
assert.match(batchApi, /\/quality-reject/, '批次 API 必须调用专用质量拒收接口。')
assert.match(batchApi, /rejectedBy\?:\s*number/, '批次 API 响应必须暴露质量拒收人。')
assert.match(batchApi, /rejectedAt\?:\s*string/, '批次 API 响应必须暴露质量拒收时间。')
assert.match(batchApi, /rejectReason\?:\s*string/, '批次 API 响应必须暴露质量拒收原因。')

assert.match(batchDetailPage, /mes:pro-edhr-batch-execution:quality-reject/, '批次详情页必须使用质量拒收权限。')
assert.match(batchDetailPage, /质量拒收/, '批次详情页必须展示质量拒收入口。')
assert.match(batchDetailPage, /canQualityReject/, '批次详情页必须用独立条件控制质量拒收入口。')
assert.match(
  batchDetailPage,
  /const\s+canQualityReject\s*=\s*computed\([\s\S]*?qualityRejectProjectionState\.value\.allowed[\s\S]*?resolveReleaseStageKey\(\)\s*===\s*'precheck'/,
  '质量拒收只能在第 2 步“放行预检”阶段可点，不能在关闭、放行审批、归档打印或已归档阶段开放。'
)
assert.match(
  batchDetailPage,
  /if \(stageKey === 'precheck'\) \{[\s\S]*qualityRejectActionItem\(\)/,
  '质量拒收按钮必须归属第 2 步“放行预检”阶段动作列表。'
)
const terminalActionStart = batchDetailPage.indexOf('const terminalReleaseActionItems = (): ReleaseStageActionItem[] => [')
const terminalActionEnd = batchDetailPage.indexOf('const qualityRejectActionItem', terminalActionStart)
assert.ok(terminalActionStart > 0 && terminalActionEnd > terminalActionStart, '必须能定位关闭批次阶段动作列表。')
const terminalActionBlock = batchDetailPage.slice(terminalActionStart, terminalActionEnd)
assert.doesNotMatch(
  terminalActionBlock,
  /quality-reject|label: '质量拒收'/,
  '质量拒收按钮不得出现在第 1 步“关闭批次”阶段动作列表。'
)
assert.match(batchDetailPage, /qualityRejectEdhrBatchExecution/, '批次详情页必须调用质量拒收 API。')
assert.match(batchDetailPage, /拒收原因不能为空/, '质量拒收必须校验原因。')
assert.match(batchDetailPage, /签名密码不能为空/, '质量拒收必须校验签名密码。')
assert.match(batchDetailPage, /质量已拒收/, '批次详情页必须把 REJECTED=50 标记为质量已拒收。')
assert.match(batchDetailPage, /需返工\/需修订/, '批次详情页必须继续区分普通返工修订状态。')
assert.match(batchDetailPage, /质量拒收原因/, '批次详情页必须展示质量拒收原因。')
assert.match(batchListPage, /需返工/, '批次列表必须保留普通返工文案。')
assert.match(batchListPage, /质量终态/, '批次列表必须展示质量终态状态。')
assert.doesNotMatch(batchDetailPage, /mock|fixture|demo|defaultSuccess/i, '质量拒收前端不得使用 mock、fixture、demo 或默认成功路径。')

console.log('PASS: eDHR quality final reject static contract')
