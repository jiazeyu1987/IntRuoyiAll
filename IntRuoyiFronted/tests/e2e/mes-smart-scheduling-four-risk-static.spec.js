const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')

const calendarPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/task/calendar/index.vue'),
  'utf8'
)
const scheduleOrderApi = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/scheduleorder/index.ts'),
  'utf8'
)
const autoScheduleApi = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/task/autoSchedule/index.ts'),
  'utf8'
)
const routeProcessPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)
const routeFlowConfigPanelPath = path.join(root, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const routeFlowConfigPanel = fs.existsSync(routeFlowConfigPanelPath)
  ? fs.readFileSync(routeFlowConfigPanelPath, 'utf8')
  : fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')
const routeScheduleStrategyEditor = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/components/RouteScheduleStrategyEditor.vue'),
  'utf8'
)
const routeResourcePage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteResourceTable.vue'),
  'utf8'
)

if (!calendarPage.includes('异常登记') || !calendarPage.includes('openIssueCreateDialog')) {
  throw new Error('排程日历必须提供生产异常登记入口')
}

if (!calendarPage.includes('关闭异常') || !calendarPage.includes('openIssueResolveDialog')) {
  throw new Error('排程日历必须提供生产异常关闭入口')
}

if (!autoScheduleApi.includes('/mes/pro/auto-schedule/issues') ||
    !autoScheduleApi.includes('/mes/pro/auto-schedule/issues/resolve')) {
  throw new Error('前端 API 必须暴露生产异常登记和关闭接口')
}

if (!scheduleOrderApi.includes('effectiveCompletedQuantity') ||
    !scheduleOrderApi.includes('pendingApprovalQuantity') ||
    !scheduleOrderApi.includes('pendingInspectionQuantity') ||
    !scheduleOrderApi.includes('overReportedQuantity')) {
  throw new Error('排产工单 API 类型必须暴露进度分层字段')
}

if (routeProcessPage.includes('MACHINERY_CAPACITY_SHIFT_HOURS = 10.5')) {
  throw new Error('排产路线设备产能不得使用 10.5 小时默认值')
}

if (routeFlowConfigPanel.includes('configVersion: `${props.useType}-${Date.now()}`') ||
    routeFlowConfigPanel.includes('configVersion: `SCHEDULE-${Date.now()}`')) {
  throw new Error('工艺流程排产配置保存不得继续依赖前端拼接时间戳版本号')
}

if (!routeProcessPage.includes('RouteScheduleStrategyEditor') ||
    !routeScheduleStrategyEditor.includes('RESOURCE_CALCULATED')) {
  throw new Error('工艺流程排产配置必须复用排产策略组件并默认支持资源计算')
}

if (routeResourcePage.includes('applyWorkbenchWorkerDefaults') ||
    routeResourcePage.includes('ProRouteResourceApi.saveResource')) {
  throw new Error('工艺路线资源页不得继续提供路线级资源编辑入口')
}

if (routeResourcePage.includes('v-model="row.singleStandardHourlyCapacity"') ||
    routeResourcePage.includes('v-model="row.workerQuantity"') ||
    routeResourcePage.includes('v-model="row.machineryQuantity"') ||
    routeResourcePage.includes('v-model="row.machineryStandardHourlyCapacity"')) {
  throw new Error('工艺路线资源页必须只读展示工作站派生资源，不得维护设备或人工产能')
}

if (!routeResourcePage.includes('维护请进入工作站详情')) {
  throw new Error('工艺路线资源页必须提示资源维护入口在工作站详情')
}

console.log('PASS: MES smart scheduling four risk static contract')
