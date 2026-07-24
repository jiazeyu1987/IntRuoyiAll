const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert.equal(fs.existsSync(pagePath), true, '排产工单页面必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')

assert.match(source, /<el-tabs[\s\S]*v-model="scheduleOrderActiveTab"/, '排产工单页面必须使用页签承载同步工单。')
assert.match(source, /<el-tab-pane[\s\S]*label="排产工单"[\s\S]*name="scheduleOrders"/, '主列表必须保留为排产工单页签。')
assert.match(source, /<el-tab-pane[\s\S]*label="同步工单"[\s\S]*name="workOrderAdmission"/, '同步工单必须是独立页签。')
assert.match(source, /const scheduleOrderActiveTab = ref<'scheduleOrders' \| 'workOrderAdmission'>\('scheduleOrders'\)/, '页面必须维护稳定的排产工单页签状态。')
assert.match(
  source,
  /@tab-change="handleScheduleOrderTabChange"/,
  '同步工单必须通过页签切换事件加载待同步差异。'
)
assert.match(
  source,
  /const handleScheduleOrderTabChange = async \(tabName: string \| number\) => \{[\s\S]*tabName !== 'workOrderAdmission'[\s\S]*getWorkOrderAdmissionList\(\)/,
  '打开同步工单页签必须加载待同步差异。'
)

const admissionStart = source.indexOf('table-key="mes.pro.scheduleOrder.admissionDiff"')
assert.ok(admissionStart >= 0, '同步工单页签必须保留待同步差异列表。')
const beforeAdmission = source.slice(Math.max(0, admissionStart - 600), admissionStart)
assert.doesNotMatch(beforeAdmission, /<Dialog[\s\S]*title="待同步差异"/, '待同步差异不能继续放在 Dialog 弹框中。')
assert.doesNotMatch(source, /v-model="workOrderAdmissionVisible"/, '同步工单不能保留弹框显隐状态。')
assert.doesNotMatch(source, /const workOrderAdmissionVisible = ref\(false\)/, '同步工单不能保留弹框状态变量。')
assert.match(source, /class="schedule-order-pool__admission-tab"/, '同步工单页签需要独立样式容器。')

console.log('PASS: MES schedule order sync tab static contract')
