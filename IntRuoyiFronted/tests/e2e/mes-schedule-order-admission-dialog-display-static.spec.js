const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), `排产工单页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')

assert(
  source.includes('<el-tab-pane label="同步工单" name="workOrderAdmission">') &&
    source.includes('class="schedule-order-pool__admission-tab"') &&
    source.includes('table-key="mes.pro.scheduleOrder.admissionDiff"') &&
    !source.includes('v-model="workOrderAdmissionVisible"'),
  '待同步差异必须在同步工单页签中展示，不得退回弹窗阻塞首屏。'
)

assert(
  source.includes('class="schedule-order-pool__admission-table-shell"') &&
    source.includes('class="schedule-order-pool__admission-table"') &&
    source.includes(':height="scheduleOrderTableHeight"') &&
    source.includes('style="width: 100%"'),
  '待同步差异表必须使用自适应表格高度和横向滚动容器，避免列表右侧列与底部滚动条显示不全。'
)

assert(
  source.includes('.schedule-order-pool__admission-table-shell') &&
    source.includes('overflow-x: auto') &&
    source.includes('max-width: 100%') &&
    source.includes('.schedule-order-pool__admission-table') &&
    source.includes('min-width: 1568px'),
  '待同步差异表必须显式声明横向滚动容器和不小于完整列宽的表格最小宽度。'
)

console.log('PASS: MES schedule order admission tab display static contract')
