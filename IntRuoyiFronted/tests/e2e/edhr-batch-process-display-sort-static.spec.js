const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(pagePath, 'utf8')

for (const [nodeType, displaySort] of [
  ['EDHR_BATCH_NODE_STERILIZATION_REPORT', '90'],
  ['EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_REPORT', '91'],
  ['EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_RECORD', '92']
]) {
  assert(
    detail.includes(`[${nodeType}]: '${displaySort}'`),
    `${nodeType} 的展示序号必须为 ${displaySort}。`
  )
}

assert(
  detail.includes("sort: '99'") && detail.includes("label: '放行'"),
  '放行虚拟工序必须显示序号 99，并保留名称“放行”。'
)

assert(
  detail.includes('specialNodeDisplaySorts[row.nodeType || \'\']') &&
    detail.includes('if (specialNodeDisplaySort) return specialNodeDisplaySort'),
  '待处理特殊节点必须优先使用前端展示序号映射，不改写后端真实排序字段。'
)

const sortStyleStart = detail.indexOf('.edhr-batch-detail__process-sort {')
assert.notEqual(sortStyleStart, -1, '必须保留工序序号徽标样式。')
const sortStyleEnd = detail.indexOf('}', sortStyleStart)
const sortStyle = detail.slice(sortStyleStart, sortStyleEnd)
assert(sortStyle.includes('color: #172033'), '工序序号徽标文字必须使用黑色。')
assert(!sortStyle.includes('color: #1677ff'), '工序序号徽标不得继续使用蓝色文字。')

console.log('PASS: eDHR batch process display sorts and number color static contract')
