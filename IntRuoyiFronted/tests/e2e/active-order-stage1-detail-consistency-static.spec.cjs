const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

assert.doesNotMatch(page, /stage1GeneratedDetailTargets/, 'Stage1 不得再维护源订单到生成订单的详情跳转映射。')
assert.doesNotMatch(page, /resolveStage1GeneratedDetailTarget/, '详情入口不得再解析 Stage1 生成订单。')
assert.match(
  page,
  /const\s+openActiveOrderSubmissionDetail\s*=\s*\(row:\s*TeamLeaderActiveOrderRespVO\)[\s\S]*sourceActiveOrderId[\s\S]*navigateActiveOrderSubmissionDetail\(sourceActiveOrderId\)/,
  '手工点击源订单详情时，必须打开当前行自己的活跃订单详情。'
)
assert.match(
  page,
  /const\s+activeOrderId\s*=\s*requirePositiveNumber\(row\.id[\s\S]*simulateStage1ActiveOrderCompletion\(\{[\s\S]*activeOrderId[\s\S]*navigateActiveOrderSubmissionDetail\(activeOrderId\)/,
  'Stage1 模拟成功后的自动详情必须打开当前点击活跃订单。'
)

console.log('PASS: active-order Stage1 detail consistency static contract')
