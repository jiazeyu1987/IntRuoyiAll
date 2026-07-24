const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const detailComponentPath = path.join(root, 'src/views/mes/pro/scheduleorder/components/ScheduleOrderProcessDetail.vue')
const source = fs.readFileSync(sourcePath, 'utf8')
const detailComponentSource = fs.readFileSync(detailComponentPath, 'utf8')

assert.match(source, /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/)
assert.match(source, /import ScheduleOrderProcessDetail from '\.\/components\/ScheduleOrderProcessDetail\.vue'/)
assert.match(detailComponentSource, /<Dialog[\s\S]*title="工艺流程"/)

const dialogMatch = source.match(
  /<ScheduleOrderProcessDetail\s+v-model="processDialogVisible"[\s\S]*?<\/ScheduleOrderProcessDetail>/
)
assert.ok(dialogMatch, '工艺流程排产配置弹窗必须存在')

const dialog = dialogMatch[0]

assert.match(dialog, /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.scheduleOrder\.processRoute"/)
assert.match(dialog, /:quick-filter-state="processRouteQuickFilter\.state"/)
assert.match(dialog, /@quick-filter-query="processRouteQuickFilter\.applyQuickFilter"/)
assert.match(dialog, /:columns="processRouteColumns"/)
assert.match(dialog, /@column-change="saveProcessRouteColumnConfig"/)
assert.match(dialog, /#table[\s\S]*<el-table[\s\S]*data-user-table-key="mes\.pro\.scheduleOrder\.processRoute"/)
assert.match(dialog, /@header-dragend="handleProcessRouteHeaderDragend"/)
assert.match(dialog, /:data="processRouteFilteredList"/)
assert.match(dialog, /:total="processRouteFilteredTotal"/)

for (const field of [
  'expand',
  'processCode',
  'processName',
  'shiftCapacityTotal',
  'plannedQuantity',
  'effectiveCompletedQuantity',
  'status',
  'shiftStatus',
  'feedbackCount',
  'latestFeedbackTime',
  'estimatedCompletionTime'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 列必须注册到工艺流程排产配置列配置`)
  assert.match(
    dialog,
    new RegExp(`isProcessRouteColumnVisible\\('${field}'\\)`),
    `${field} 列必须受工艺流程排产配置显示字段配置控制`
  )
}

assert.match(dialog, /label="班次状态"[\s\S]*getProcessRouteShiftStatusText\(row\)/)
assert.match(dialog, /label="预计结束"[\s\S]*getProcessRouteEstimatedCompletionTime\(row\)/)
assert.match(source, /const processRouteQuickFilterDefinitions/)
assert.match(source, /const processRouteQuickFilter = useTableQuickFilter\(/)
assert.match(source, /useUserTableColumns\('mes\.pro\.scheduleOrder\.processRoute'/)
assert.match(source, /const processRouteFilteredList = computed\(/)
assert.match(source, /const getProcessRouteShiftStatusText = \(row: MesProScheduleOrderProcessVO\)/)
assert.match(source, /const getProcessRouteEstimatedCompletionTime = \(row: MesProScheduleOrderProcessVO\)/)

console.log('PASS: mes schedule order process route unified list static contract')
