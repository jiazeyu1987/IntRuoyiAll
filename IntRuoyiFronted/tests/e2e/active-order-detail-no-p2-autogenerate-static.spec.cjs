const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../..')
const detailPage = fs.readFileSync(
  path.join(
    repoRoot,
    'IntRuoyiFronted/src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue'
  ),
  'utf8'
)
const detailPanel = fs.readFileSync(
  path.join(
    repoRoot,
    'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
  ),
  'utf8'
)

assert.doesNotMatch(
  detailPage,
  /simulateStage2_5BackfillBatchExecution|DETAIL-FORM|resolveBatchExecutionOpenResult|batchExecutionOpenResultCache/,
  '活跃订单详情页不得自行触发 P2 回填；P2 只能由活跃订单池 P2 按钮执行'
)

assert.doesNotMatch(
  detailPanel,
  /data-active-order-open-batch-execution-production-form|data-active-order-open-batch-execution-pqc-form|open-batch-execution-production-form|open-batch-execution-pqc-form/,
  '活跃订单详情面板不得暴露可在 P1 后绕过 P2 按钮生成批次执行的入口'
)

console.log('active-order-detail-no-p2-autogenerate-static.spec.cjs PASS')
