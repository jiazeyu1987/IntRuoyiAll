const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert.equal(fs.existsSync(pagePath), true, '排产工单页面必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')
const admissionTabStart = source.indexOf('<el-tab-pane label="同步工单" name="workOrderAdmission">')
assert.ok(admissionTabStart >= 0, '同步工单页签必须存在。')

const admissionTabSource = source.slice(admissionTabStart)
const admissionActionsStart = admissionTabSource.indexOf('<template #actions>')
const admissionActionsEnd = admissionTabSource.indexOf('</template>', admissionActionsStart)
assert.ok(
  admissionActionsStart >= 0 && admissionActionsEnd > admissionActionsStart,
  '同步工单页签必须存在 actions 工具栏。'
)
const admissionActionsSource = admissionTabSource.slice(admissionActionsStart, admissionActionsEnd)

assert.match(
  admissionActionsSource,
  /class="schedule-order-pool__admission-show-admitted"[\s\S]*显示已入池订单[\s\S]*<el-switch[\s\S]*v-model="workOrderAdmissionShowAdmitted"[\s\S]*@change="handleWorkOrderAdmissionShowAdmittedChange"/,
  '同步工单 actions 工具栏必须在重置和入池按钮旁渲染显示已入池订单开关。'
)
assert.match(
  source,
  /const workOrderAdmissionShowAdmitted = ref\(false\)/,
  '显示已入池订单开关必须默认关闭，默认隐藏已入池订单。'
)
assert.match(
  source,
  /const resolveWorkOrderAdmissionStatus = \(\) =>\s*workOrderAdmissionShowAdmitted\.value\s*\?\s*undefined\s*:\s*DEFAULT_WORK_ORDER_ADMISSION_STATUS/,
  '开关必须通过正式 admissionStatus 查询参数控制是否纳入已入池订单。'
)
assert.match(
  source,
  /const handleWorkOrderAdmissionShowAdmittedChange = \(\) => \{[\s\S]*workOrderAdmissionQueryParams\.admissionStatus = resolveWorkOrderAdmissionStatus\(\)[\s\S]*handleWorkOrderAdmissionQuery\(\)[\s\S]*\}/,
  '切换显示已入池订单时必须更新查询参数并重新查询第一页。'
)
assert.match(
  source,
  /const resetWorkOrderAdmissionQuery = \(\) => \{[\s\S]*workOrderAdmissionShowAdmitted\.value = false[\s\S]*workOrderAdmissionQueryParams\.admissionStatus = resolveWorkOrderAdmissionStatus\(\)[\s\S]*handleWorkOrderAdmissionQuery\(\)/,
  '同步工单重置必须关闭显示已入池订单开关并恢复隐藏已入池订单。'
)
assert.match(
  source,
  /const data = await MesProScheduleOrderApi\.getAdmissionDiff\(workOrderAdmissionQueryParams\)/,
  '同步工单列表必须继续使用正式查询参数请求后端，不能改成本地过滤。'
)
assert.doesNotMatch(
  source,
  /workOrderAdmissionList\.value\s*=\s*\(data\.list\s*\|\|\s*\[\]\)\.filter/,
  '不得用当前页本地 filter 冒充隐藏已入池订单，否则分页总数会错误。'
)
assert.match(
  source,
  /\.schedule-order-pool__admission-show-admitted\s*\{[\s\S]*display:\s*inline-flex;[\s\S]*white-space:\s*nowrap;/,
  '显示已入池订单开关需要专用布局，避免 Switch 文案在红框位置被裁切。'
)

console.log('PASS: MES schedule order admission show admitted switch static contract')
