const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const batchList = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const history = read('src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const router = read('src/router/modules/remaining.ts')

assert.match(
  router,
  /BatchExecutionActiveOrderDetailPage\.vue/,
  '批次执行和历史追溯必须有独立的活跃订单详情页面路由，不能复用 eDHR 批次表单详情。'
)

assert.match(
  batchList,
  /data-edhr-batch-active-order-detail/,
  '批次执行列表操作列必须提供“详情”按钮。'
)
assert.match(
  batchList,
  /openActiveOrderDetail\(row\)/,
  '批次执行列表“详情”按钮必须打开活跃订单详情批记录。'
)
assert.doesNotMatch(
  batchList,
  />\s*查看批记录\s*</,
  '批次执行列表不得保留“查看批记录”控制按钮。'
)

assert.match(
  history,
  /data-edhr-history-active-order-detail/,
  '历史追溯列表必须提供“详情”按钮。'
)
assert.match(
  history,
  /openActiveOrderDetail\(batch\)/,
  '历史追溯“详情”按钮必须打开活跃订单详情批记录。'
)
assert.doesNotMatch(
  history,
  />\s*查看批记录\s*</,
  '历史追溯列表不得保留“查看批记录”控制按钮。'
)

console.log('active-order fact frontend static checks passed')
