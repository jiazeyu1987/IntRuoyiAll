const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const admissionStart = source.indexOf('table-key="mes.pro.scheduleOrder.admissionDiff"')
assert.notEqual(admissionStart, -1, '同步工单页签必须存在待同步差异标准列表。')
const admissionEnd = source.indexOf('</UnifiedListTemplate>', admissionStart)
assert.notEqual(admissionEnd, -1, '同步工单标准列表模板必须正确闭合。')
const admissionTemplate = source.slice(admissionStart, admissionEnd)
const beforeAdmission = source.slice(Math.max(0, admissionStart - 600), admissionStart)
const admissionDefinitionsStart = source.indexOf(
  'const workOrderAdmissionMultiFilterDefinitions: ListMultiFilterDefinition[] = ['
)
const admissionDefinitionsEnd = source.indexOf(
  'const replanDrawerVisible',
  admissionDefinitionsStart
)
assert.ok(
  admissionDefinitionsStart >= 0 && admissionDefinitionsEnd > admissionDefinitionsStart,
  '同步工单必须声明多维筛选定义。'
)
const admissionDefinitions = source.slice(admissionDefinitionsStart, admissionDefinitionsEnd)

assert.match(
  admissionTemplate,
  /table-key="mes\.pro\.scheduleOrder\.admissionDiff"/,
  '同步工单列表必须接入标准列表模板并使用独立 table key。'
)
assert.doesNotMatch(
  beforeAdmission,
  /<Dialog[\s\S]*title="待同步差异"/,
  '同步工单不能退回旧弹框承载方式。'
)
assert.match(admissionTemplate, /:query-model="workOrderAdmissionQueryParams"/)
assert.match(admissionTemplate, /:show-quick-filter="false"/)
assert.match(admissionTemplate, /:show-multi-filter="true"/)
assert.match(
  admissionTemplate,
  /:multi-filter-definitions="workOrderAdmissionMultiFilterDefinitions"/
)
assert.match(admissionTemplate, /:multi-filter-state="workOrderAdmissionMultiFilter\.state"/)
assert.match(admissionTemplate, /:show-multi-filter-operators="false"/)
assert.match(admissionTemplate, /:columns="workOrderAdmissionColumns"/)
assert.match(admissionTemplate, /:column-saving="workOrderAdmissionColumnSaving"/)
assert.match(admissionTemplate, /v-model:page="workOrderAdmissionQueryParams\.pageNo"/)
assert.match(admissionTemplate, /v-model:limit="workOrderAdmissionQueryParams\.pageSize"/)
assert.match(
  admissionTemplate,
  /@update:multi-filter-state="workOrderAdmissionMultiFilter\.updateState"/
)
assert.match(
  admissionTemplate,
  /@multi-filter-query="workOrderAdmissionMultiFilter\.applyMultiFilter"/
)
assert.match(
  admissionTemplate,
  /@multi-filter-reset="workOrderAdmissionMultiFilter\.resetMultiFilter"/
)
assert.match(
  admissionTemplate,
  /@multi-filter-remove="workOrderAdmissionMultiFilter\.removeCondition"/
)
assert.match(admissionTemplate, /@column-change="saveWorkOrderAdmissionColumnConfig"/)
assert.match(admissionTemplate, /@column-reset="resetWorkOrderAdmissionColumnConfig"/)
assert.match(admissionTemplate, /@pagination="getWorkOrderAdmissionList"/)

assert.doesNotMatch(
  admissionTemplate,
  /<template #extra-filters>/,
  '同步工单不再渲染额外筛选插槽，避免与标准列表模板筛选重复。'
)
assert.match(
  admissionTemplate,
  /<template #actions>[\s\S]*选中工单加入排产工单池[\s\S]*UserTableColumnSettings/
)
assert.doesNotMatch(
  admissionTemplate,
  /<template #actions>[\s\S]*>\s*搜索\s*</,
  '同步工单动作区不再显示独立搜索按钮。'
)
assert.match(
  admissionTemplate,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="mes\.pro\.scheduleOrder\.admissionDiff"/
)
assert.match(
  admissionTemplate,
  /<el-table[\s\S]*\bborder\b[\s\S]*@header-dragend="handleWorkOrderAdmissionHeaderDragend"/,
  '同步工单表格必须启用 border，否则 Element Plus 列宽拖拽手柄不会生效。'
)
assert.match(admissionTemplate, /@header-dragend="handleWorkOrderAdmissionHeaderDragend"/)
assert.doesNotMatch(
  admissionTemplate,
  /<Pagination[\s\S]*workOrderAdmissionTotal/,
  '同步工单分页必须由标准列表模板承载。'
)

for (const field of [
  'workOrderCode',
  'productCode',
  'productName',
  'productSpecification',
  'quantity',
  'requestDate',
  'admissionStatus',
  'analysis',
  'message',
  'ownerRole',
  'operation'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `缺少待同步差异字段配置：${field}`)
  assert.match(
    admissionTemplate,
    new RegExp(`isWorkOrderAdmissionColumnVisible\\('${field}'\\)`),
    `待同步差异列未绑定显示字段配置：${field}`
  )
  const widthFunction =
    field === 'message'
      ? 'getWorkOrderAdmissionColumnMinWidthString'
      : 'getWorkOrderAdmissionColumnWidthString'
  assert.match(
    admissionTemplate,
    new RegExp(`${widthFunction}\\('${field}'`),
    `待同步差异列未把保存后的列宽回填到 width：${field}`
  )
}

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
    new RegExp(
      `key:\\s*'${key}'[\\s\\S]*?label:\\s*'${label}'[\\s\\S]*?queryParamKey:\\s*'${queryParamKey}'`
    ),
    `同步工单筛选字段缺少正式参数映射：${label}`
  )
}

assert.match(admissionTemplate, /label="分析"/, '同步工单必须显示分析列。')
assert.match(
  source,
  /\{\s*key:\s*'analysis',\s*label:\s*'分析',\s*width:\s*220\s*\}/,
  '分析列必须作为可配置的业务列注册。'
)
assert.match(
  admissionTemplate,
  /row\.selectable[\s\S]*可加入[\s\S]*row\.message/,
  '分析列必须使用服务端 selectable/message 正式字段展示可加入结论和阻断原因。'
)
const analysisColumnPosition = admissionTemplate.indexOf('label="分析"')
const quantityColumnPosition = admissionTemplate.indexOf(
  "isWorkOrderAdmissionColumnVisible('quantity')"
)
assert.ok(
  analysisColumnPosition >= 0 && quantityColumnPosition > analysisColumnPosition,
  '分析列必须位于数量列之前，保证同步工单首屏可见。'
)
assert.match(admissionTemplate, /schedule-order-pool__admission-analysis--ready/)
assert.match(admissionTemplate, /schedule-order-pool__admission-analysis--blocked/)
assert.match(admissionTemplate, /v-if="isMissingRouteRow\(row\)"/)
assert.match(admissionTemplate, /一键加入/)
assert.match(
  admissionTemplate,
  /@click\.stop="openRouteBindingDialog\(row\)"/,
  '缺少工艺路线时必须提供一键绑定入口。'
)
assert.match(source, /ProRouteApi\.getRouteItemBindingList\(\)/)
assert.match(source, /ProRouteProductApi\.saveRouteProductByItem\(/)
assert.doesNotMatch(admissionTemplate, /:disabled="isRouteBindingOptionDisabled\(routeOption\)"/)
assert.doesNotMatch(admissionTemplate, /\u5DF2\u542F\u7528\uFF0C\u4EC5\u56DE\u663E|已启用，仅回显/)
assert.match(source, /await getWorkOrderAdmissionList\(\)/)
assert.match(source, /title="为产品绑定工艺路线"/)
assert.match(
  source,
  /<el-table-column[\s\S]*?type="selection"[\s\S]*?:selectable="isAdmissionRowSelectable"/,
  '不可加入工单不能被选择提交。'
)

for (const contract of [
  'const workOrderAdmissionMultiFilter = useTableMultiFilter',
  'getWorkOrderAdmissionList',
  'submitWorkOrderAdmission',
  'handleWorkOrderAdmissionSelectionChange',
  'canOpenIssueAction',
  'openIssueAction'
]) {
  assert.match(source, new RegExp(contract), `待同步差异业务处理丢失：${contract}`)
}

assert.doesNotMatch(
  admissionTemplate,
  /<TableQuickFilter|workOrderAdmissionQuickFilter/,
  '同步工单不得保留旧快速筛选。'
)
assert.doesNotMatch(source, /localStorage\.|sessionStorage\./, '字段持久化不得自行访问浏览器存储。')

console.log('PASS: MES schedule order admission diff unified list template static contract')
