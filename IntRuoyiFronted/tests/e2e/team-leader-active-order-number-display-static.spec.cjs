const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts'),
  'utf8'
)

const tableStart = page.indexOf('data-team-leader-active-order-list')
const tableEnd = page.indexOf('</el-table>', tableStart)
assert.ok(tableStart >= 0 && tableEnd > tableStart, '必须定位活跃订单表格。')
const table = page.slice(tableStart, tableEnd)

assert.match(
  table,
  /<el-table-column\s+label="生产订单号"\s+prop="workOrderCode"\s+min-width="\d+">/,
  '活跃订单可见列必须标注为生产订单号并绑定正式 workOrderCode。'
)
assert.match(
  table,
  /data-team-leader-active-order-work-order-code[\s\S]*\{\{\s*row\.workOrderCode\s*\}\}/,
  '生产订单号单元格必须直接显示 row.workOrderCode。'
)
assert.doesNotMatch(
  table,
  /label="生产订单ID"|data-team-leader-active-order-work-order-id|\{\{\s*row\.workOrderId\s*\}\}/,
  '活跃订单可见列不得继续展示内部 workOrderId。'
)

assert.match(
  page,
  /const\s+activeOrderColumns:[\s\S]*?\{\s*key:\s*'workOrderCode',\s*label:\s*'生产订单号',\s*visible:\s*true\s*\}/,
  '统一列表列元数据必须同步使用 workOrderCode 和生产订单号。'
)
assert.doesNotMatch(
  page,
  /\{\s*key:\s*'workOrderId',\s*label:\s*'生产订单ID',\s*visible:\s*true\s*\}/,
  '统一列表列元数据不得保留错误的生产订单 ID 可见列。'
)

const responseStart = api.indexOf('export interface TeamLeaderActiveOrderRespVO')
const responseEnd = api.indexOf('\nexport interface ', responseStart + 1)
assert.ok(responseStart >= 0 && responseEnd > responseStart, '必须定位活跃订单响应类型。')
const response = api.slice(responseStart, responseEnd)
assert.match(response, /workOrderId:\s*number/, '响应必须保留内部 workOrderId 供业务操作使用。')
assert.match(response, /workOrderCode\?:\s*string/, '响应必须提供正式 workOrderCode 供列表展示。')

console.log('PASS: team leader active order displays production order number')
