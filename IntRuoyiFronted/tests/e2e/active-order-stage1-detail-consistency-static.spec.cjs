const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

assert.doesNotMatch(page, /stage1GeneratedDetailTargets/, 'Stage1 不得再维护源订单到生成订单的详情跳转映射。')
assert.doesNotMatch(page, /resolveStage1GeneratedDetailTarget/, '详情入口不得再解析 Stage1 生成订单。')
assert.match(
  page,
  /const\s+openActiveOrderSubmissionDetail\s*=\s*\(row:\s*TeamLeaderActiveOrderRespVO\)[\s\S]*sourceActiveOrderId[\s\S]*navigateActiveOrderSubmissionDetail\(sourceActiveOrderId\)/,
  '手工点击源订单详情时，必须打开当前行自己的活跃订单详情。'
)
assert.match(
  page,
  /const\s+handleSimulateStage1\s*=\s*async\s*\(row:[\s\S]*simulateStage1ActiveOrderCompletion\(\{[\s\S]*activeOrderId[\s\S]*await loadActiveOrders\(\)[\s\S]*\n}\n\nconst\s+handleGenerateStage1Forms/,
  'P1 模拟成功后必须只刷新活跃订单列表。'
)
const p1Handler = page.match(/const\s+handleSimulateStage1\s*=\s*async\s*\(row:[\s\S]*?\n}\n\nconst\s+handleGenerateStage1Forms/)?.[0] || ''
assert.doesNotMatch(
  p1Handler,
  /navigateActiveOrderSubmissionDetail/,
  'P1 模拟成功后不得自动打开详情。'
)
assert.match(
  page,
  /const\s+handleGenerateStage1Forms\s*=\s*async\s*\(row:[\s\S]*const\s+activeOrderId\s*=\s*requirePositiveNumber\(row\.id[\s\S]*simulateStage2_5BackfillBatchExecution\(\{[\s\S]*activeOrderId[\s\S]*expectedVersion:\s*row\.version[\s\S]*await loadActiveOrders\(\)/,
  'P2 生成必须执行正式回填并刷新列表。'
)

console.log('PASS: active-order Stage1 detail consistency static contract')
