const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const graphPath = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const source = fs.readFileSync(graphPath, 'utf8')
const schedulerWorkbenchApi = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/schedulerWorkbench/index.ts'),
  'utf8'
)

const assertIncludes = (needle, message) => {
  assert(
    source.includes(needle),
    `${message}\nMissing snippet: ${needle}`
  )
}

const assertMatches = (pattern, message) => {
  assert(pattern.test(source), message)
}

const boundOptionsStart = source.indexOf('const boundRouteProcessOptions = computed')
const boundOptionsEnd = source.indexOf('const selectedNodeFullName', boundOptionsStart)
const boundOptionsSource =
  boundOptionsStart >= 0 && boundOptionsEnd > boundOptionsStart
    ? source.slice(boundOptionsStart, boundOptionsEnd)
    : ''
const mergedRowsStart = source.indexOf('const mergeRouteProcessRowWithNode')
const mergedRowsEnd = source.indexOf('const selectedRouteProcess = computed', mergedRowsStart)
const mergedRowsSource =
  mergedRowsStart >= 0 && mergedRowsEnd > mergedRowsStart
    ? source.slice(mergedRowsStart, mergedRowsEnd)
    : ''
const bindWorkstationStart = source.indexOf('const bindCapacityWorkstationToRouteProcess')
const bindWorkstationEnd = source.indexOf(
  'const createCapacityWorkstationForRouteProcess',
  bindWorkstationStart
)
const bindWorkstationSource =
  bindWorkstationStart >= 0 && bindWorkstationEnd > bindWorkstationStart
    ? source.slice(bindWorkstationStart, bindWorkstationEnd)
    : ''
const createWorkstationStart = source.indexOf('const createCapacityWorkstationForRouteProcess')
const createWorkstationEnd = source.indexOf(
  'const refreshCapacityWorkstationRepairBinding',
  createWorkstationStart
)
const createWorkstationSource =
  createWorkstationStart >= 0 && createWorkstationEnd > createWorkstationStart
    ? source.slice(createWorkstationStart, createWorkstationEnd)
    : ''
const formatBoundOptionStart = source.indexOf('const formatBoundRouteProcessOption')
const formatBoundOptionEnd = source.indexOf('const isProcessDetailLinkLabelVisible', formatBoundOptionStart)
const formatBoundOptionSource =
  formatBoundOptionStart >= 0 && formatBoundOptionEnd > formatBoundOptionStart
    ? source.slice(formatBoundOptionStart, formatBoundOptionEnd)
    : ''

assertIncludes(
  'data-testid="route-flow-capacity-workstation-repair-dialog"',
  '缺少班次小时应打开工作站绑定修复弹框，而不是只显示最终错误。'
)
assert(
  !source.includes('route-flow-capacity-workstation-repair__alert'),
  '工作站修复弹框不应显示顶部橙色说明条。'
)
assert(
  !source.includes('当前工序还没有可用于计算班次产能的工作站'),
  '工作站修复弹框不应显示“当前工序还没有可用于计算班次产能的工作站”提示文案。'
)
assertIncludes('capacityWorkstationRepairDialogVisible', '修复弹框必须有显式打开状态。')
assertIncludes('capacityWorkstationRepairMode', '修复弹框必须支持绑定已有和新建绑定两种模式。')
assertIncludes('绑定已有工作站', '修复弹框必须提供绑定已有工作站模式。')
assertIncludes('新建工作站并绑定', '修复弹框必须提供新建工作站并绑定模式。')
assertIncludes(
  'v-model="capacityWorkstationRepairForm.sourceRouteProcessId"',
  '绑定已有工作站必须选择已绑定的路线工序，而不是直接选择工作站编号。'
)
assertIncludes(
  'boundRouteProcessOptions',
  '绑定已有工作站下拉必须来自已绑定工作站的路线工序列表。'
)
assertIncludes(
  'capacityWorkstationRepairWorkstationOptions',
  '绑定已有工作站下拉必须加载全局启用工作站来源，不能只取当前路线已有绑定。'
)
assertMatches(
  /CAPACITY_WORKSTATION_REPAIR_WORKSTATION_PAGE_SIZE = 200[\s\S]*MdWorkstationApi\.getWorkstationPage\([\s\S]*pageSize:\s*CAPACITY_WORKSTATION_REPAIR_WORKSTATION_PAGE_SIZE[\s\S]*status:\s*CommonStatusEnum\.ENABLE/,
  '绑定已有工作站下拉必须读取全局启用工作站主数据，避免当前路线只绑定 1 个时候选不足。'
)
assert(
  mergedRowsSource.includes('routeNodes.value') && mergedRowsSource.includes('routeProcessRows.value'),
  '已绑定工序下拉必须合并候选关系图节点和路线工序列表，不能只依赖 list-by-route；否则候选/快照场景会显示 No data。'
)
assert(
  mergedRowsSource.includes('workstationId') &&
    mergedRowsSource.includes('workstationCode') &&
    mergedRowsSource.includes('workstationName'),
  '候选关系图节点中的工作站字段必须参与已绑定工序候选合并。'
)
assert(
  mergedRowsSource.includes('workstationId: node.workstationId') &&
    !mergedRowsSource.includes('node.workstationId ?? row?.workstationId'),
  '候选关系图节点必须是工作站绑定字段的唯一来源，不能用列表行的同工序推导工作站绕过显式绑定。'
)
assert(
  source.includes('routeProcessWorkstationId') &&
    mergedRowsSource.includes('routeProcessWorkstationId'),
  '前端必须保留路线工序显式绑定工作站字段，不能把展示用 workstationId 当成绑定依据。'
)
assertMatches(
  /const getBoundRouteProcessWorkstationId = \(routeProcess\?: ProRouteProcessVO\) =>[\s\S]*props\.routeVersionEditContext[\s\S]*numericValue\(routeProcess\?\.routeProcessWorkstationId\)[\s\S]*numericValue\(routeProcess\?\.routeProcessWorkstationId \?\? routeProcess\?\.workstationId\)/,
  '候选版本必须只按 routeProcessWorkstationId 判断显式绑定，不能 fallback 到展示用 workstationId。'
)
assertMatches(
  /boundRouteProcessOptions[\s\S]*routeProcessId !== targetRouteProcessId/,
  '已绑定工序选项必须排除当前工序。'
)
assertMatches(
  /boundRouteProcessOptions[\s\S]*getBoundRouteProcessWorkstationId\(row\)/,
  '已绑定工序选项必须要求 route process 显式绑定 workstationId，不能把同工序推导工作站算作已绑定。'
)
assert(
  !/\.filter\([\s\S]*shiftHours/.test(boundOptionsSource),
  '已绑定工序选项不能按 route process shiftHours 过滤；班次小时固定在工作站/排产配置侧，缺失时仍应允许选择已绑定工作站的工序。'
)
assertMatches(
  /const buildGlobalWorkstationRepairSourceOptions = \(\)[\s\S]*capacityWorkstationRepairWorkstationOptions\.value[\s\S]*workstationId/,
  '已绑定工序选项必须把全局启用工作站转换为可选来源。'
)
assertMatches(
  /const boundRouteProcessOptions = computed\([\s\S]*buildRouteProcessRepairSourceOptions\(\)[\s\S]*buildGlobalWorkstationRepairSourceOptions\(\)/,
  '已绑定工序下拉必须合并当前路线显式绑定来源和全局启用工作站来源。'
)
assert(
  source.includes('产能未配置'),
  '已绑定工序选项缺少工序产能时应显示简洁的“产能未配置”，不能显示为 No data 或冗长说明。'
)
assertIncludes(
  'shiftCapacity?: number',
  '已绑定工序来源必须携带工序产能字段，不能只携带班次小时。'
)
assertMatches(
  /const buildRouteProcessRepairSourceOptions = \(\)[\s\S]*shiftCapacity:\s*row\.processShiftCapacityTotal/,
  '路线内已绑定工序选项必须使用 route process 的 processShiftCapacityTotal 作为展示产能。'
)
assertMatches(
  /const buildGlobalWorkstationRepairSourceOptions = \(\)[\s\S]*shiftCapacity:\s*numericValue\(workstation\.todayCapacity\)/,
  '全局工作站来源必须使用工作站返回的 todayCapacity 作为展示产能。'
)
assert(
  !formatBoundOptionSource.includes('工作站：') &&
    !formatBoundOptionSource.includes('workstationCode') &&
    !formatBoundOptionSource.includes('workstationName'),
  '绑定已有工作站下拉选项只显示工序名称和工序产能，不应显示工作站编码或工作站名称。'
)
assert(
  !formatBoundOptionSource.includes('sortLabel') &&
    !formatBoundOptionSource.includes('processCode') &&
    !formatBoundOptionSource.includes('全局工序'),
  '绑定已有工作站下拉选项不应显示排序、工序编码或“全局工序”前缀。'
)
assert(
  !formatBoundOptionSource.includes('shiftHoursLabel') &&
    !formatBoundOptionSource.includes('routeProcess.shiftHours') &&
    !formatBoundOptionSource.includes('h/班次'),
  '绑定已有工作站下拉选项必须显示工序产能，不能把固定班次小时显示为产能。'
)
assert(
  formatBoundOptionSource.includes('formatRouteProcessIntegerShiftCapacity') &&
    !formatBoundOptionSource.includes('formatRouteProcessShiftCapacity(shiftCapacity)'),
  '绑定已有工作站下拉选项的产能必须按整数班次产能展示，避免浮点尾数导致内容不简洁。'
)
assertMatches(
  /return `\$\{processName\}（\$\{shiftCapacityLabel\}）`/,
  '绑定已有工作站下拉选项必须保持“工序名称（工序产能）”的简洁格式。'
)
assert(
  !source.includes('ProRouteProcessApi.updateRouteProcess'),
  '候选版本工作站绑定不得绕过 routeVersionId 直接更新生效路线工序主表。'
)
assert(
  bindWorkstationSource.includes("requireCandidateRouteVersionId('工作站绑定')") &&
    bindWorkstationSource.includes('routeNodes.value = routeNodes.value.map') &&
    bindWorkstationSource.includes('workstationId') &&
    bindWorkstationSource.includes('await persistRouteProcessDraftChanges()'),
  '绑定已有工序工作站后必须更新候选节点，并通过候选关系图保存接口写入 DRAFT 快照。'
)
assertMatches(
  /const sourceWorkstationId = sourceOption\?\.workstationId[\s\S]*bindCapacityWorkstationToRouteProcess\([\s\S]*targetRouteProcess[\s\S]*sourceWorkstationId/,
  '绑定已有工序时必须复用所选来源对应的 workstationId。'
)
assertIncludes('MdWorkshopApi.getWorkshopSimpleList', '新建工作站模式必须加载车间下拉。')
assert(
  schedulerWorkbenchApi.includes('getShiftHoursSetting') &&
    schedulerWorkbenchApi.includes('/mes/pro/scheduler-workbench/shift-hours'),
  '新建工作站模式读取班次小时必须复用排产员工作台班次小时接口。'
)
assertIncludes(
  'SchedulerWorkbenchApi.getShiftHoursSetting()',
  '新建工作站模式必须读取排产员工作台班次小时设置，不能让用户手填班次小时。'
)
assert(
  !source.includes('v-model="capacityWorkstationRepairForm.shiftHours"') &&
    !source.includes('data-flow-field="capacity-workstation-repair-shift-hours"'),
  '先绑定工作站弹框的新建模式不得渲染可手填的班次小时输入框。'
)
assertIncludes(
  'data-flow-field="capacity-workstation-repair-shift-hours-readonly"',
  '先绑定工作站弹框的新建模式必须只读展示来自排产员工作台的班次小时。'
)
assert(
  createWorkstationSource.includes('resolveCapacityWorkstationRepairShiftHoursForCreate()') &&
    !createWorkstationSource.includes('numericValue(capacityWorkstationRepairForm.shiftHours)'),
  '新建工作站创建 payload 的 shiftHours 必须来自排产员工作台设置，不能来自修复弹框表单输入。'
)
assertIncludes(
  '请先在排产员工作台统一保存班次小时',
  '排产员工作台没有有效统一班次小时时必须 fail fast，不能默认 1 小时或继续手填。'
)
assertIncludes('AutoCodeRecordApi.generateAutoCode(MesAutoCodeRuleCode.MD_WORKSTATION_CODE)', '新建工作站必须自动生成工作站编码。')
assertIncludes('MdWorkstationApi.createWorkstation', '新建模式必须调用工作站创建接口。')
assertMatches(
  /const buildCapacityWorkstationName = \([\s\S]*targetRouteProcess: ProRouteProcessVO[\s\S]*workstationCode: string[\s\S]*`\$\{processName\}-工作站-\$\{workstationCode\}`/,
  '自动创建工作站名称必须包含自动生成编码，避免同一工序已有“工序名-工作站”时被名称唯一性校验阻断。'
)
assertMatches(
  /const code = await AutoCodeRecordApi\.generateAutoCode\(MesAutoCodeRuleCode\.MD_WORKSTATION_CODE\)[\s\S]*MdWorkstationApi\.createWorkstation\([\s\S]*name:\s*buildCapacityWorkstationName\(targetRouteProcess,\s*code\)/,
  '自动创建工作站必须用刚生成的编码参与名称生成，保证新建后可继续绑定候选路线工序。'
)
assertMatches(
  /MdWorkstationApi\.createWorkstation\([\s\S]*processId:\s*targetRouteProcess\.processId[\s\S]*shiftHours(?:\s*:\s*shiftHours)?[\s\S]*status:\s*CommonStatusEnum\.ENABLE/,
  '自动创建工作站必须绑定当前工序、写入班次小时并启用。'
)
assertIncludes(
  'openCapacityWorkstationRepairDialog',
  '产能覆盖保存发现路线工序未绑定工作站或缺少班次小时时必须进入修复弹框。'
)
assertMatches(
  /const targetRouteProcess = capacityWorkstationRepairTargetRouteProcess\.value[\s\S]*const routeProcessWorkstationId = getBoundRouteProcessWorkstationId\(targetRouteProcess\)/,
  '产能覆盖保存必须先检查当前路线工序自身的 workstationId，不能只因同工序主数据存在工作站就跳过绑定。'
)
assertMatches(
  /routeProcessWorkstationId === undefined[\s\S]*routeProcessWorkstationId <= 0[\s\S]*shiftHours === undefined[\s\S]*shiftHours <= 0[\s\S]*await openCapacityWorkstationRepairDialog/,
  '路线工序未绑定工作站或缺少班次小时分支必须打开修复弹框，而不是直接保存产能。'
)
assertIncludes(
  'await refreshCapacityWorkstationRepairBinding',
  '绑定完成后必须刷新路线工序和选中工序属性。'
)
assertIncludes(
  'await openCapacityOverrideDialogForDraft()',
  '绑定完成后必须恢复产能覆盖弹框，方便用户继续设定产能。'
)
assertIncludes(
  'capacityOverrideRepairHourlyCapacity',
  '进入工作站修复前必须暂存用户已输入的产能/h，避免弹框关闭后被重置。'
)
assertMatches(
  /capacityOverrideRepairHourlyCapacity\.value = normalizeHourlyCapacity\([\s\S]*capacityOverrideForm\.hourlyCapacity[\s\S]*await openCapacityWorkstationRepairDialog/,
  '缺少班次小时进入修复弹框前必须先暂存当前产能/h。'
)
assertMatches(
  /await openCapacityOverrideDialogForDraft\(\)[\s\S]*capacityOverrideForm\.hourlyCapacity = previousHourlyCapacity/,
  '工作站绑定完成并重开产能弹框后必须恢复用户原先输入的产能/h。'
)

console.log('mes-route-flow-capacity-workstation-repair-static: PASS')
