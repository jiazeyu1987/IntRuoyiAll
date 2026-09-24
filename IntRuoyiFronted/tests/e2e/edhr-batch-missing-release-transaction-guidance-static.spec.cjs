const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const batchPagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const pqcPagePath = path.join(repoRoot, 'src/views/mes/pro/production-release/PqcProductionReleasePage.vue')
const batchPage = fs.readFileSync(batchPagePath, 'utf8')
const pqcPage = fs.readFileSync(pqcPagePath, 'utf8')

const requireMatch = (source, expression, message) => {
  assert.ok(expression.test(source), message)
}

requireMatch(
  batchPage,
  /v-if="releaseTransactionMissing"[\s\S]*?正式上市放行事务尚未生成[\s\S]*?让步处置只关闭不合格评审/,
  '缺少正式事务时必须说明让步处置不替代最终 PQC 放行。'
)
requireMatch(
  batchPage,
  /v-else-if="releaseContext\?\.releaseTransactionId"/,
  '只有已加载正式事务时才允许显示上市签名表单。'
)
requireMatch(
  batchPage,
  /v-if="releaseContext\?\.releaseTransactionId"[\s\S]*?确认上市放行/,
  '只有已加载正式事务时才允许显示上市放行确认按钮。'
)
requireMatch(
  batchPage,
  /const openPqcReleasePending = async \(\) =>[\s\S]*?name: 'MesPqcProductionRelease'[\s\S]*?query: \{ workOrderCode \}/,
  '活跃订单批次必须能按当前工单导航到 PQC 待放行列表。'
)
requireMatch(
  batchPage,
  /if \(!context\.releaseTransactionId\)\s*\{\s*releaseTransactionMissing\.value = true\s*return\s*\}/,
  '缺少事务时必须进入明确的阻断状态，不能继续进入签名提交。'
)
requireMatch(
  pqcPage,
  /const route = useRoute\(\)[\s\S]*?workOrderCode:\s*typeof route\.query\.workOrderCode === 'string' \? route\.query\.workOrderCode : ''/,
  'PQC 页面必须读取导航带入的工单筛选条件。'
)
requireMatch(
  pqcPage,
  /watch\(\s*\(\) => route\.query\.workOrderCode/,
  '复用中的 PQC 页面必须响应新的工单筛选路由参数。'
)

console.log('PASS: eDHR missing release transaction guidance static contract')
