const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

assert.match(
  page,
  /const\s+stage1GeneratedDetailTargets\s*=\s*ref\(\s*new Map<number,\s*\{\s*activeOrderId:\s*number;\s*sourceWorkOrderCode:\s*string\s*\}>/,
  'Stage1 生成目标必须保存在当前页面状态中，供后续点击同一源订单详情复用。'
)
assert.match(
  page,
  /const\s+resolveStage1GeneratedDetailTarget\s*=\s*\(row:\s*TeamLeaderActiveOrderRespVO\)[\s\S]*row\.stage1GeneratedActiveOrderId[\s\S]*stage1GeneratedDetailTargets\.value\.get\(sourceActiveOrderId\)/,
  '详情入口必须优先解析持久化或本次运行的 Stage1 生成目标。'
)
assert.match(
  page,
  /const\s+openActiveOrderSubmissionDetail\s*=\s*\(row:\s*TeamLeaderActiveOrderRespVO\)[\s\S]*resolveStage1GeneratedDetailTarget\(row\)[\s\S]*stage1GeneratedTarget\?\.activeOrderId[\s\S]*sourceActiveOrderId[\s\S]*sourceWorkOrderCode/,
  '手工点击源订单详情时，若存在 Stage1 生成目标，必须打开与模拟成功后一致的生成订单详情。'
)
assert.match(
  page,
  /stage1GeneratedDetailTargets\.value\.set\(templateActiveOrderId,\s*\{[\s\S]*activeOrderId:\s*generatedActiveOrderId[\s\S]*sourceWorkOrderCode:\s*row\.workOrderCode\s*\|\|\s*''[\s\S]*\}\)/,
  'Stage1 模拟成功后必须记录源订单到生成订单的映射。'
)
assert.match(
  page,
  /navigateActiveOrderSubmissionDetail\(generatedActiveOrderId,\s*row\.workOrderCode\s*\|\|\s*''\)/,
  'Stage1 模拟成功后的自动详情仍必须打开新生成测试订单。'
)

console.log('PASS: active-order Stage1 detail consistency static contract')
