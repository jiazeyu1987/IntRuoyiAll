const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const dialogStart = source.indexOf('title="待同步差异"')
assert.notEqual(dialogStart, -1, '待同步差异弹窗必须存在。')
const dialogEnd = source.indexOf('</Dialog>', dialogStart)
assert.notEqual(dialogEnd, -1, '待同步差异弹窗必须正确闭合。')
const dialog = source.slice(dialogStart, dialogEnd)

assert.match(
  dialog,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.scheduleOrder\.admissionDiff"/,
  '待同步差异列表必须接入标准列表模板并使用独立 table key。'
)
assert.match(dialog, /:query-model="workOrderAdmissionQueryParams"/)
assert.match(dialog, /:filter-definitions="workOrderAdmissionQuickFilterDefinitions"/)
assert.match(dialog, /:quick-filter-state="workOrderAdmissionQuickFilter\.state"/)
assert.match(dialog, /:selected-filter-definition="workOrderAdmissionQuickFilter\.selectedDefinition\.value"/)
assert.match(dialog, /:operator-options="workOrderAdmissionQuickFilter\.operatorOptions\.value"/)
assert.match(dialog, /:columns="workOrderAdmissionColumns"/)
assert.match(dialog, /:column-saving="workOrderAdmissionColumnSaving"/)
assert.match(dialog, /v-model:page="workOrderAdmissionQueryParams\.pageNo"/)
assert.match(dialog, /v-model:limit="workOrderAdmissionQueryParams\.pageSize"/)
assert.match(dialog, /@quick-filter-query="workOrderAdmissionQuickFilter\.applyQuickFilter"/)
assert.match(dialog, /@column-change="saveWorkOrderAdmissionColumnConfig"/)
assert.match(dialog, /@column-reset="resetWorkOrderAdmissionColumnConfig"/)
assert.match(dialog, /@pagination="getWorkOrderAdmissionList"/)

assert.doesNotMatch(dialog, /<template #extra-filters>/, '待同步差异不再渲染额外筛选插槽，避免与标准列表模板筛选重复。')
assert.match(dialog, /<template #actions>[\s\S]*重置[\s\S]*选中工单加入排产工单池/)
assert.doesNotMatch(dialog, /<template #actions>[\s\S]*>\s*搜索\s*</, '待同步差异动作区不再显示独立搜索按钮。')
assert.match(dialog, /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="mes\.pro\.scheduleOrder\.admissionDiff"/)
assert.match(
  dialog,
  /<el-table[\s\S]*\bborder\b[\s\S]*@header-dragend="handleWorkOrderAdmissionHeaderDragend"/,
  '待同步差异表格必须启用 border，否则 Element Plus 列宽拖拽手柄不会生效。'
)
assert.match(dialog, /@header-dragend="handleWorkOrderAdmissionHeaderDragend"/)
assert.doesNotMatch(dialog, /<Pagination[\s\S]*workOrderAdmissionTotal/, '待同步差异分页必须由标准列表模板承载。')

for (const field of [
  'workOrderCode',
  'productCode',
  'productName',
  'productSpecification',
  'quantity',
  'requestDate',
  'admissionStatus',
  'message',
  'ownerRole',
  'operation'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `缺少待同步差异字段配置：${field}`)
  assert.match(
    dialog,
    new RegExp(`isWorkOrderAdmissionColumnVisible\\('${field}'\\)`),
    `待同步差异列未绑定显示字段配置：${field}`
  )
  assert.match(
    dialog,
    new RegExp(`getWorkOrderAdmissionColumnWidthString\\('${field}'`),
    `待同步差异列未把保存后的列宽回填到 width：${field}`
  )
}

for (const contract of [
  'const workOrderAdmissionQuickFilter = useTableQuickFilter',
  'getWorkOrderAdmissionList',
  'handleWorkOrderAdmissionQuery',
  'resetWorkOrderAdmissionQuery',
  'submitWorkOrderAdmission',
  'handleWorkOrderAdmissionSelectionChange',
  'canOpenIssueAction',
  'openIssueAction'
]) {
  assert.match(source, new RegExp(contract), `待同步差异业务处理丢失：${contract}`)
}

assert.doesNotMatch(dialog, /<TableQuickFilter/, '快速过滤应由标准列表模板统一承载。')
assert.doesNotMatch(dialog, /<UserTableColumnSettings/, '显示字段配置应由标准列表模板统一承载。')
assert.doesNotMatch(source, /localStorage\.|sessionStorage\./, '字段持久化不得自行访问浏览器存储。')

console.log('PASS: MES schedule order admission diff unified list template static contract')
