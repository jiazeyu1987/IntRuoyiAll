const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const workbenchPath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const routeFlowGraphDesignerPath = path.join(
  repoRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const scheduleOrderApiPath = path.join(repoRoot, 'src/api/mes/pro/scheduleorder/index.ts')

const workbench = fs.readFileSync(workbenchPath, 'utf8')
const routeFlowGraphDesigner = fs.readFileSync(routeFlowGraphDesignerPath, 'utf8')
const scheduleOrderApi = fs.readFileSync(scheduleOrderApiPath, 'utf8')

function expectIncludes(source, needle, message) {
  assert(
    source.includes(needle),
    `${message}\nExpected to find: ${needle}`
  )
}

function expectNotIncludes(source, needle, message) {
  assert(
    !source.includes(needle),
    `${message}\nUnexpected content: ${needle}`
  )
}

expectIncludes(workbench, "key: 'nightShiftEnabled'", '工序在制列设置必须包含夜班列')
expectIncludes(workbench, "key: 'plannedStartDate'", '工序在制列设置必须包含开排日期列')
expectIncludes(workbench, 'label="夜班"', '工序在制列表必须显示夜班列')
expectIncludes(workbench, 'label="开排日期"', '工序在制列表必须显示开排日期列')
expectIncludes(workbench, '@change="handleProcessWipNightShiftChange', '夜班列必须行内保存')
expectIncludes(workbench, '@change="handleProcessWipPlannedStartDateChange', '开排日期列必须行内保存')
expectIncludes(
  workbench,
  'v-model="processWipPlannedStartDateDrafts[getProcessWipRowKey(row)]"',
  '开排日期必须按路线工序使用独立可写模型'
)
expectIncludes(workbench, ':row-key="getProcessWipRowKey"', '工序在制表格必须按路线工序使用稳定行键')
expectIncludes(
  workbench,
  '`${row.routeVersionId}:${row.routeProcessId}`',
  '路线版本和路线工序必须共同组成独立设置键'
)
expectIncludes(
  workbench,
  'row.routeVersionId == null || row.routeProcessId == null',
  '缺少路线工序标识时必须失败，禁止生成共享 undefined 行键'
)
expectIncludes(
  workbench,
  '工序在制数据缺少路线工序标识',
  '运行后端契约过旧时必须显示明确错误'
)
expectIncludes(
  workbench,
  'processWipSettingsSavingId.value = getProcessWipRowKey(row)',
  '保存中状态必须只锁定目标路线工序'
)
expectNotIncludes(
  workbench,
  'processWipPlannedStartDateDrafts[row.processId]',
  '相同基础工序跨路线不得共享开排日期草稿'
)
expectIncludes(workbench, 'syncProcessWipPlannedStartDateDrafts', '工序在制数据刷新后必须同步开排日期草稿值')
expectIncludes(workbench, 'saveProcessWipSettings', '工序在制设置必须调用保存接口')
expectIncludes(workbench, "{ key: 'nightShiftEnabled'", '快速过滤必须覆盖夜班列')
expectIncludes(workbench, "{ key: 'plannedStartDate'", '快速过滤必须覆盖开排日期列')
expectIncludes(workbench, 'prop="defaultNightShiftEnabled"', '工作台默认策略必须继续保留夜班入口')

expectIncludes(scheduleOrderApi, 'nightShiftEnabled?: boolean', 'API 类型必须暴露夜班状态')
expectNotIncludes(scheduleOrderApi, 'nightShiftMixed?: boolean', '路线工序设置不得继续暴露跨路线混合夜班状态')
expectIncludes(scheduleOrderApi, 'plannedStartDate?: string', 'API 类型必须暴露开排日期')
expectIncludes(scheduleOrderApi, 'plannedStartDateMixed?: boolean', 'API 类型必须暴露开排日期混合状态')
expectIncludes(scheduleOrderApi, 'routeVersionId: number', '保存请求必须携带路线版本唯一标识')
expectIncludes(scheduleOrderApi, 'routeProcessId: number', '保存请求必须携带路线工序唯一标识')
expectIncludes(scheduleOrderApi, 'saveProcessWipSettings', 'API 必须提供工序在制设置保存方法')
expectIncludes(scheduleOrderApi, '/mes/pro/schedule-order/process-wip-settings', 'API 必须调用后端保存接口')
expectIncludes(scheduleOrderApi, 'normalizeProcessWipStatistics', '工序在制统计接口必须归一化后端数组日期')
expectIncludes(scheduleOrderApi, 'plannedStartDate:', '工序在制统计归一化必须覆盖开排日期字段')
expectIncludes(scheduleOrderApi, 'data.map(normalizeProcessWipStatistics)', '工序在制统计返回列表必须逐行归一化')

expectNotIncludes(
  routeFlowGraphDesigner,
  '<el-table-column v-if="configType === \'SCHEDULE\'" label="夜班"',
  '工艺流程排产配置不再显示夜班列'
)
expectNotIncludes(routeFlowGraphDesigner, 'label="夜班"', '工艺流程排产配置不再显示夜班列')

console.log('mes-scheduler-workbench-process-wip-night-shift-start-date-static passed')
