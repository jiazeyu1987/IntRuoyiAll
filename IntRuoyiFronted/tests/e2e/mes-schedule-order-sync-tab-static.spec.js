const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')

assert.equal(fs.existsSync(pagePath), true, '排产工单页面必须存在。')
assert.equal(fs.existsSync(apiPath), true, '排产工单 API 类型必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

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
const admissionEnd = source.indexOf('</UnifiedListTemplate>', admissionStart)
assert.ok(admissionEnd > admissionStart, '同步工单页签必须完整渲染标准列表模板。')
const admissionTemplate = source.slice(admissionStart, admissionEnd)
assert.doesNotMatch(beforeAdmission, /<Dialog[\s\S]*title="待同步差异"/, '待同步差异不能继续放在 Dialog 弹框中。')
assert.doesNotMatch(source, /v-model="workOrderAdmissionVisible"/, '同步工单不能保留弹框显隐状态。')
assert.doesNotMatch(source, /const workOrderAdmissionVisible = ref\(false\)/, '同步工单不能保留弹框状态变量。')
assert.match(source, /class="schedule-order-pool__admission-tab"/, '同步工单页签需要独立样式容器。')
assert.match(admissionTemplate, /:show-quick-filter="false"/, '同步工单启用条件 Tab 后必须关闭旧 quick filter。')
assert.match(admissionTemplate, /:show-multi-filter="true"/, '同步工单必须启用标准列表条件 Tab 多维筛选。')
assert.match(
  admissionTemplate,
  /:multi-filter-definitions="workOrderAdmissionMultiFilterDefinitions"/,
  '同步工单必须传入正式多维筛选定义。'
)
assert.match(
  admissionTemplate,
  /:multi-filter-state="workOrderAdmissionMultiFilter\.state"/,
  '同步工单必须绑定独立的多维筛选状态。'
)
assert.match(
  admissionTemplate,
  /@update:multi-filter-state="workOrderAdmissionMultiFilter\.updateState"/,
  '同步工单必须接收条件 Tab 状态更新。'
)
assert.match(
  admissionTemplate,
  /@multi-filter-query="workOrderAdmissionMultiFilter\.applyMultiFilter"/,
  '同步工单查询必须应用所有已填写条件的交集。'
)
assert.match(
  admissionTemplate,
  /@multi-filter-reset="workOrderAdmissionMultiFilter\.resetMultiFilter"/,
  '同步工单重置必须清空正式多维筛选参数。'
)
assert.doesNotMatch(admissionTemplate, /@quick-filter-query|workOrderAdmissionQuickFilter/, '同步工单不得继续绑定旧 quick filter。')
assert.doesNotMatch(admissionTemplate, /显示已入池订单/, '入池状态必须统一由条件 Tab 表达。')
assert.doesNotMatch(admissionTemplate, /resetWorkOrderAdmissionQuery/, '同步工单不得保留重复的筛选重置按钮。')

const admissionDefinitionsStart = source.indexOf(
  'const workOrderAdmissionMultiFilterDefinitions: ListMultiFilterDefinition[] = ['
)
const admissionDefinitionsEnd = source.indexOf('const replanDrawerVisible', admissionDefinitionsStart)
assert.ok(admissionDefinitionsStart >= 0 && admissionDefinitionsEnd > admissionDefinitionsStart, '同步工单必须声明多维筛选定义。')
const admissionDefinitions = source.slice(admissionDefinitionsStart, admissionDefinitionsEnd)
for (const [key, label, queryParamKey] of [
  ['workOrderCode', '工单编码', 'workOrderCode'],
  ['productCode', '产品编号', 'productCode'],
  ['productName', '产品名称', 'productName'],
  ['productSpecification', '规格型号', 'productSpecification'],
  ['quantity', '总数量', 'quantity'],
  ['requestDate', '需求日期', 'requestDate'],
  ['admissionStatus', '入池状态', 'admissionStatus'],
  ['reasonCode', '不可排原因', 'reasonCode'],
  ['ownerRole', '建议处理', 'ownerRole']
]) {
  assert.match(
    admissionDefinitions,
    new RegExp(`key: '${key}'[\\s\\S]*?label: '${label}'[\\s\\S]*?queryParamKey: '${queryParamKey}'`),
    `同步工单 ${key} 必须以“${label}”映射后端正式查询参数 ${queryParamKey}。`
  )
}
assert.match(
  admissionDefinitions,
  /key: 'quantity'[\s\S]*?type: 'numberRange'[\s\S]*?queryParamKey: 'quantity'/,
  '同步工单总数量必须使用数字范围筛选并映射正式 quantity 参数。'
)
assert.match(
  admissionDefinitions,
  /key: 'reasonCode'[\s\S]*?type: 'select'[\s\S]*?label: '缺路线'[\s\S]*?value: 'BLOCKED_MISSING_ROUTE'/,
  '同步工单不可排原因必须提供中文原因选项并映射正式 reasonCode。'
)
for (const field of ['productName', 'productSpecification', 'quantity', 'reasonCode', 'ownerRole']) {
  assert.match(
    source,
    new RegExp(`const workOrderAdmissionQueryParams = reactive\\([\\s\\S]*?${field}:\\s*undefined`),
    `同步工单查询参数必须声明新增字段：${field}`
  )
}
for (const token of [
  'productName?: string',
  'productSpecification?: string',
  'quantity?:',
  'reasonCode?: string',
  'ownerRole?: string'
]) {
  assert.ok(apiSource.includes(token), `同步工单 API 请求类型必须声明正式字段：${token}`)
}
assert.match(
  source,
  /const workOrderAdmissionMultiFilter = useTableMultiFilter\([\s\S]*?'mes\.pro\.scheduleOrder\.admissionDiff'[\s\S]*?workOrderAdmissionMultiFilterDefinitions[\s\S]*?workOrderAdmissionQueryParams[\s\S]*?getWorkOrderAdmissionList[\s\S]*?\)/,
  '同步工单必须使用标准多维筛选 hook 驱动正式列表请求。'
)
assert.match(
  source,
  /const workOrderAdmissionQueryParams = reactive\(\{[\s\S]*?admissionStatus:\s*undefined/,
  '同步工单首屏必须保持正式入池状态参数为空。'
)
assert.doesNotMatch(
  source,
  /DEFAULT_WORK_ORDER_ADMISSION_STATUS|workOrderAdmissionMultiFilter\.setCondition\(/,
  '同步工单不得恢复页面级默认“可入池”条件，首屏标准条件 Tab 必须为空。'
)
assert.doesNotMatch(source, /const workOrderAdmissionQuickFilter = useTableQuickFilter/, '同步工单不得继续创建旧快捷筛选 hook。')
assert.doesNotMatch(source, /const workOrderAdmissionShowAdmitted = ref/, '同步工单不得保留独立的显示已入池筛选状态。')

console.log('PASS: MES schedule order sync tab static contract')
