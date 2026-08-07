const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const homeShortcutPath = path.resolve(process.cwd(), 'src/views/mes/home/HomeShortcuts.vue')
const routeEditPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteEditPage.vue')
const routeFormContentPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFormContent.vue')
const routeFlowConfigPanelPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const calendarPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')
const puhuiPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/puhui-schedule/index.vue')
const columnSettingsPath = path.resolve(process.cwd(), 'src/components/UserTableColumnSettings/index.vue')

assert(fs.existsSync(apiPath), 'Schedule order API module must exist.')
assert(fs.existsSync(pagePath), 'Schedule order pool page must exist.')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')
const homeShortcutSource = fs.readFileSync(homeShortcutPath, 'utf8')
const routeEditPageSource = fs.readFileSync(routeEditPagePath, 'utf8')
const routeFormContentSource = fs.readFileSync(routeFormContentPath, 'utf8')
const routeFlowConfigPanelSource = fs.readFileSync(routeFlowConfigPanelPath, 'utf8')
const calendarPageSource = fs.readFileSync(calendarPagePath, 'utf8')
const puhuiPageSource = fs.readFileSync(puhuiPagePath, 'utf8')
const columnSettingsSource = fs.readFileSync(columnSettingsPath, 'utf8')

assert(
  apiSource.includes('/mes/pro/schedule-order/create-from-work-order'),
  'API must call backend create-from-work-order endpoint.'
)
assert(apiSource.includes('/mes/pro/schedule-order/page'), 'API must expose schedule order page query.')
assert(
  apiSource.includes('/mes/pro/schedule-order/process-list'),
  'API must expose schedule order process snapshot query.'
)
assert(
  apiSource.includes('/mes/pro/schedule-order/sync-progress'),
  'API must expose schedule order progress sync endpoint.'
)
assert(
  apiSource.includes('/mes/pro/schedule-order/daily-compare'),
  'API must expose schedule order daily compare endpoint.'
)
assert(
  apiSource.includes('/mes/pro/schedule-order/update'),
  'API must expose schedule order update endpoint.'
)
assert(apiSource.includes('/mes/pro/schedule-order/freeze'), 'API must expose schedule order freeze endpoint.')
assert(apiSource.includes('/mes/pro/schedule-order/batch-delete'), 'API must expose schedule order batch delete endpoint.')
assert(apiSource.includes('/mes/pro/schedule-order/operation-log'), 'API must expose schedule order operation log endpoint.')
assert(apiSource.includes('updateScheduleOrder'), 'API must expose updateScheduleOrder for schedule order pool.')

assert(pageSource.includes('排产工单'), 'Page must show the schedule order area.')
assert(!pageSource.includes('ContentWrap title="来源生产工单"'), 'Schedule order tab must not render the source production work order card.')
assert(!pageSource.includes('ContentWrap title="差异提示"'), 'Schedule order tab must not render the difference hint card.')
assert(!pageSource.includes('activeDiffOrders'), 'Page must not keep a hidden difference hint data path.')
assert(!pageSource.includes('ERP工单编码'), 'Visible copy must use 工单编码 instead of ERP工单编码.')
assert(!pageSource.includes('请输入ERP工单编码'), 'Visible copy must not use ERP工单编码 placeholder.')
const queryFormStart = pageSource.indexOf('<UnifiedListTemplate')
const queryToolbarStart = pageSource.indexOf('<template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">', queryFormStart)
assert(queryFormStart >= 0 && queryToolbarStart > queryFormStart, 'Schedule order query form must exist.')
const queryFilterSource = pageSource.slice(queryFormStart, queryToolbarStart)
assert(!queryFilterSource.includes('label="排产编码"'), 'Schedule order query form must fold duplicate 排产编码 into quick filter.')
assert(!queryFilterSource.includes('placeholder="请输入排产编码"'), 'Schedule order query form must not render duplicate 排产编码 input.')
assert(!queryFilterSource.includes('label="工单编码"'), 'Schedule order query form must fold duplicate 工单编码 into quick filter.')
assert(!queryFilterSource.includes('placeholder="请输入工单编码"'), 'Schedule order query form must not render duplicate 工单编码 input.')
assert(!queryFilterSource.includes('label="承诺交期"'), 'Schedule order query form must fold duplicate 承诺交期 into quick filter.')
assert(
  pageSource.includes('UnifiedListTemplate') &&
    pageSource.includes(':quick-filter-state="scheduleOrderQuickFilter.state"'),
  'Schedule order page must keep the unified quick filter entry through the list template.'
)
assert(!queryFilterSource.includes('label="快速过滤"'), 'Schedule order query form must not render duplicate quick filter label.')
assert(pageSource.includes(':show-quick-filter-label="false"'), 'Schedule order quick filter must hide its inner 快速过滤 label.')
assert(pageSource.includes("{ key: 'code', label: '排产编码'"), 'Quick filter must still cover 排产编码.')
assert(pageSource.includes("key: 'completionFilter'"), 'Quick filter must cover 完成状态.')
assert(pageSource.includes("queryParamKey: 'completionFilter'"), '完成状态 must keep the backend completionFilter query contract.')
assert(pageSource.includes("{ key: 'erpWorkOrderCode', label: '生产工单号'"), 'Quick filter must still cover 生产工单号.')
assert(pageSource.includes("{ key: 'promiseDate', label: '承诺交期'"), 'Quick filter must still cover 承诺交期.')
assert(!queryFilterSource.includes('label="完成状态"'), 'Schedule order query form must fold 完成状态 into quick filter.')
assert(!pageSource.includes('schedule-order-pool__toolbar-group--query'), 'Schedule order page must not render duplicate search/reset query buttons.')
assert(pageSource.includes(':show-column-reset="false"'), 'Schedule order page must hide column reset action.')
assert(columnSettingsSource.includes('v-if="showReset"'), 'Column settings component must support hiding reset action.')
assert(pageSource.includes('label="数量/进度"'), 'Schedule order list must show a combined quantity/progress column.')
assert(pageSource.includes('schedule-order-pool__quantity-progress'), 'Schedule order list must render quantity/progress composite content.')
assert(pageSource.includes('<el-progress'), 'Schedule order list must render a progress bar.')
assert(pageSource.includes('formatQuantity(row.totalQuantity ?? row.quantity)'), 'Schedule order list must show total quantity.')
assert(!pageSource.includes('formatQuantity(row.completedQuantity)'), 'Schedule order list must hide completed quantity detail.')
assert(!pageSource.includes('formatQuantity(row.uncompletedQuantity)'), 'Schedule order list must hide uncompleted quantity detail.')
assert(pageSource.includes('label="最晚开工"'), 'Schedule order list must show latest start time.')
assert(pageSource.includes('label="计划开工"'), 'Schedule order list must show planned start time.')
assert(pageSource.includes('label="计划完成"'), 'Schedule order list must show planned finish time.')
assert(pageSource.includes('路线版本'), 'Schedule order list must show the frozen route version.')
assert(pageSource.includes('label="差异"'), 'Schedule order list must keep the difference status column.')
assert(
  pageSource.includes('schedule-order-pool__risk-text') &&
    pageSource.includes('schedule-order-pool__warning-text'),
  'Schedule order list must keep start and delivery risk visual indicators.'
)
assert(pageSource.includes('label="当前工序"'), 'Schedule order list must show current running process.')
assert(pageSource.includes('currentProcessName'), 'Schedule order list must render current process name.')
assert(
  pageSource.includes('openCurrentProcessRouteDetail') && pageSource.includes('openRouteDetail'),
  'Schedule order current process name must keep the route detail link.'
)
assert(pageSource.includes("name: 'MesProRouteEdit'"), 'Schedule order route link must open the route edit page.')
assert(pageSource.includes("tab: 'schedule-config'"), 'Schedule order route link must open the schedule config tab.')
assert(
  pageSource.includes('routeProcessId: row.currentRouteProcessId ? String(row.currentRouteProcessId) : undefined'),
  'Schedule order route link must pass the current route process id.'
)
assert(routeEditPageSource.includes('route.query.routeProcessId'), 'Route edit page must consume routeProcessId query.')
assert(
  routeEditPageSource.includes(':target-route-process-id="targetRouteProcessId"'),
  'Route edit page must pass target route process id into shared content.'
)
assert(
  routeFormContentSource.includes(':target-route-process-id="targetRouteProcessId"'),
  'Route form content must pass target route process id into the schedule config panel.'
)
assert(routeFlowConfigPanelSource.includes('highlight-current-row'), 'Schedule config panel must highlight current rows.')
assert(routeFlowConfigPanelSource.includes('setCurrentRow'), 'Schedule config panel must mark the targeted process row current.')
assert(routeFlowConfigPanelSource.includes('scrollIntoView'), 'Schedule config panel must scroll the targeted process row into view.')
assert(routeFlowConfigPanelSource.includes('未找到对应路线工序'), 'Schedule config panel must expose missing target process errors.')
assert(
  routeFlowConfigPanelSource.includes('route-flow-config-panel__row--target'),
  'Schedule config panel must style the targeted process row.'
)
assert(
  pageSource.includes("action.targetRouteName === 'MesProRouteEdit'") &&
    pageSource.includes('params: { id: routeId }') &&
    pageSource.includes('delete targetQuery.routeId'),
  'Issue action route config entries must pass routeId as MesProRouteEdit route params.'
)
assert(!pageSource.includes('openAdjustDialog'), 'Schedule order list must not expose legacy adjust action.')
assert(!pageSource.includes('submitScheduleOrderAdjust'), 'Schedule order page must not keep adjust submit code.')
assert(!pageSource.includes('调整排产工单'), 'Schedule order page must not keep the adjust dialog.')
assert(pageSource.includes('调整'), 'Schedule order list must expose priority adjust action.')
assert(pageSource.includes('openPriorityDialog'), 'Schedule order page must open a priority adjust dialog.')
assert(pageSource.includes('submitPriorityAdjust'), 'Schedule order page must submit priority updates.')
assert(pageSource.includes('updatePriority'), 'Schedule order page must use the dedicated priority update API.')
assert(pageSource.includes('交期'), 'Schedule order list must expose promise date action.')
assert(pageSource.includes('冻结'), 'Schedule order list must expose freeze action.')
assert(pageSource.includes('强制完成'), 'Schedule order list must expose force-finish action.')
assert(pageSource.includes('撤销强制完成'), 'Schedule order list must expose revoke force-finish action.')
assert(pageSource.includes('openOperationLogDialog'), 'Schedule order list must keep operation trace handler.')
assert(pageSource.includes('openPromiseDateDialog'), 'Schedule order page must open a promise date dialog.')
assert(pageSource.includes('submitPromiseDateReset'), 'Schedule order page must submit promise date updates.')
assert(pageSource.includes('openFreezeDialog'), 'Schedule order page must open a freeze dialog.')
assert(pageSource.includes('submitScheduleOrderFreeze'), 'Schedule order page must submit freeze requests.')
assert(pageSource.includes('submitScheduleOrderDelete'), 'Schedule order page must submit delete requests.')
assert(pageSource.includes('openOperationLogDialog'), 'Schedule order page must open an operation trace dialog.')
assert(pageSource.includes('manualFinishDialogVisible'), 'Schedule order page must expose the manual finish dialog state.')
assert(pageSource.includes("label: '完成状态'"), 'Schedule order page must expose completion filtering through quick filter.')
assert(
  (pageSource.match(/v-hasPermi="\['mes:pro-schedule-order:update'\]"/g) || []).length >= 2,
  'Promise date and freeze actions must be protected by mes:pro-schedule-order:update permission.'
)
assert(
  pageSource.includes("v-hasPermi=\"['mes:pro-auto-schedule:replan']\""),
  'Manual replan entry must be protected by mes:pro-auto-schedule:replan permission.'
)
assert(pageSource.includes('label="班次产能"'), 'Process snapshot dialog must show shift capacity total.')
assert(pageSource.includes('label="需要多少个"'), 'Process snapshot dialog must show planned quantity.')
assert(pageSource.includes('label="做了多少个"'), 'Process snapshot dialog must show real completed quantity.')
assert(pageSource.includes('label="状态"'), 'Process snapshot dialog must show per-process status.')
assert(pageSource.includes('label="预计结束"'), 'Process snapshot dialog must show planned end time.')
assert(
  pageSource.includes('该工单已由有权限人员强制关闭；汇总按 100% 展示，以下工序仍保留真实进度，可撤销强制完成。'),
  'Process snapshot dialog must explain the force-finish summary override.'
)
assert(
  pageSource.includes('生成排产工单') || pageSource.includes('加入排产工单池'),
  'Schedule order page must expose a source work order admission action.'
)
for (const field of [
  'progressPercent',
  'routeVersion',
  'hourlyCapacityTotal',
  'shiftHours',
  'shiftCapacityTotal',
  'resourceSnapshotJson',
  'latestStartTime',
  'plannedStartTime',
  'plannedEndTime',
  'totalQuantity',
  'completedQuantity',
  'uncompletedQuantity',
  'manualFinished',
  'manualFinishedTime',
  'manualFinishedBy',
  'manualFinishedReason',
  'currentProcessId',
  'currentProcessName',
  'currentProcessProgressPercent',
  'plannedQuantity',
  'actualQuantity',
  'diffQuantity'
]) {
  assert(apiSource.includes(field), `Schedule order API type must include ${field}.`)
}
assert(
  !/v-model="createForm\.quantity"|quantity\s*:\s*createForm\.quantity/.test(pageSource),
  'Schedule quantity must not be user editable or submitted by the frontend.'
)
const admissionSubmitStart = pageSource.indexOf('const submitWorkOrderAdmission')
const admissionSubmitEnd = pageSource.indexOf('const openReplanDrawer')
assert(admissionSubmitStart >= 0 && admissionSubmitEnd > admissionSubmitStart, 'Admission submit handler must exist.')
const admissionSubmitSource = pageSource.slice(admissionSubmitStart, admissionSubmitEnd)
assert(
  !admissionSubmitSource.includes('priorityNo'),
  'Admission submit must not submit priorityNo.'
)
assert(
  !admissionSubmitSource.includes('promiseDate'),
  'Admission submit must not require or submit promiseDate.'
)
assert(
  !apiSource.includes('totalQuantity: Number(row.totalQuantity ?? row.quantity') &&
    !apiSource.includes('completedQuantity: Number(row.completedQuantity ?? 0') &&
    !apiSource.includes('uncompletedQuantity: Number(row.uncompletedQuantity ?? row.quantity'),
  'Schedule order quantity summary fields must not be synthesized when backend omits them.'
)
assert(
  apiSource.includes('排产工单接口缺少必需字段'),
  'Schedule order API must fail fast when required quantity/progress fields are missing.'
)
assert(
  apiSource.includes('/mes/pro/schedule-order/manual-finish') &&
    apiSource.includes('/mes/pro/schedule-order/revoke-manual-finish'),
  'Schedule order API must expose manual finish and revoke endpoints.'
)
assert(
  pageSource.includes(':row-key="getDailyCompareRowKey"') &&
    pageSource.includes('`${row.planDate}-${row.scheduleOrderProcessId}`'),
  'Daily compare table must use a composite row key for same-day multi-process rows.'
)
assert(
  !/catch\s*\{\s*return null\s*\}/.test(pageSource),
  'Schedule order page must not silently swallow replan request build errors.'
)
assert(
  pageSource.includes('replanPreviewHasBlockedIssue'),
  'Schedule order page must derive whether replan preview still has blocking issues.'
)
assert(
  pageSource.includes('const hasReplanPermission = computed(() => checkPermi([\'mes:pro-auto-schedule:replan\']))'),
  'Schedule order page must derive a reusable replan permission state from mes:pro-auto-schedule:replan.'
)
assert(
  /const openReplanDrawer = \(\) => \{[\s\S]*hasReplanPermission\.value[\s\S]*message\.warning\('当前账号没有手动重排权限'\)[\s\S]*return/.test(
    pageSource
  ),
  'Programmatic manual replan opening must fail fast when the current user lacks replan permission.'
)
assert(
  /v-if="hasReplanPermission"[\s\S]*预览重排/.test(pageSource),
  'Preview replan button must only render when the current user has replan permission.'
)
assert(
  /v-if="hasReplanPermission"[\s\S]*应用重排/.test(pageSource),
  'Apply replan button must only render when the current user has replan permission.'
)
assert(
  pageSource.includes('replanIssueRows') &&
    pageSource.includes('label="严重度"') &&
    pageSource.includes('label="问题"') &&
    pageSource.includes('label="备注"'),
  'Replan preview issues table must show severity, problem, and remark columns.'
)
assert(
  pageSource.includes('openReplanIssueCalendar'),
  'Schedule order page must keep a dedicated jump action for missing replan shifts from the remark column.'
)
assert(pageSource.includes('buildIssueRemarkParts'), 'Replan preview remarks must derive structured issue context.')
assert(
  pageSource.includes('runtimeCapacityBasisDifferenceText'),
  'Replan drawer must derive the visible difference between planned and actual capacity modes.'
)
assert(
  pageSource.includes('当前按计划产能预估') && pageSource.includes('当前按实际产能预估'),
  'Replan drawer alert must explain the selected capacity mode when users switch planned/actual capacity.'
)
assert(
  pageSource.includes('计划产能是排产日历中维护的班次可用产能') &&
    pageSource.includes('实际产能是根据已报工或实际完成记录统计出的真实产出能力'),
  'Replan drawer alert must define what planned capacity and actual capacity mean.'
)
assert(pageSource.includes('formatIssueDate(issue.calendarDate)'), 'Replan preview remarks must include issue dates.')
assert(pageSource.includes('issue.shiftName || issue.shiftId'), 'Replan preview remarks must include issue shifts.')
assert(
  /name:\s*'MesProScheduleCalendar'[\s\S]*openShiftEditor:\s*'1'/.test(pageSource),
  'Missing replan shifts must jump to the schedule calendar page and open shift editing context.'
)
assert(
  /preflight\.result === 'BLOCKED'[\s\S]*preflightHasBlockedIssue\.value/.test(pageSource) &&
    /freshPreview\.summary\?\.blockingIssueCount[\s\S]*freshPreview\.issues\?\.some\(\(issue\) => issue\.severity === 'BLOCKING'\)/.test(
      pageSource
    ),
  'Schedule order page must block apply when preflight or fresh preview still contains blocking issues.'
)
assert(pageSource.includes('const route = useRoute()'), 'Schedule order page must read route query.')
assert(
  pageSource.includes('autoOpenReplan') && pageSource.includes('openReplanDrawer()'),
  'Schedule order page must auto-open manual replan drawer when autoOpenReplan=1 is present.'
)
assert(
  /watch\(\s*\(\)\s*=>\s*route\.query\.autoOpenReplan[\s\S]*openReplanDrawer\(\)/.test(pageSource),
  'Schedule order page must react to autoOpenReplan query changes after navigation.'
)
assert(
  /function replanFromToday\(\)[\s\S]*applyScenario\(\(prev\) => \(\{ \.\.\.prev, horizonStart: today \}\), `已从今天 \$\{today\} 开始重排。`\)/.test(
    puhuiPageSource
  ),
  'Puhui top-level manual replan entry must keep the local scenario mutation flow.'
)
assert(
  /<el-button[\s\S]*@click="replanFromToday"[\s\S]*ep:refresh-right[\s\S]*从今天重排/.test(puhuiPageSource),
  'Puhui top-level manual replan entry must keep the original local replan copy and icon.'
)
assert(
  /<el-button type="primary" @click="submitInsertOrder">插入并重排<\/el-button>/.test(puhuiPageSource) &&
    /async function submitInsertOrder\(\)[\s\S]*buildInsertOrderMutation/.test(puhuiPageSource),
  'Puhui insert-and-replan dialog action must keep its local scenario mutation behavior.'
)
assert(
  calendarPageSource.includes('route.query.date') &&
    calendarPageSource.includes('route.query.openShiftEditor'),
  'Schedule calendar page must consume incoming date and shift-editor query context.'
)
assert(
  calendarPageSource.includes('calendarShiftEditorDate.value = editable ? nextDate :') ||
    calendarPageSource.includes('calendarShiftEditorDate.value = openShiftEditor && canEditCalendarDate'),
  'Schedule calendar page must reopen the targeted date in editable shift mode when requested.'
)
assert(
  homeShortcutSource.includes('MesProScheduleOrder'),
  'MES home shortcuts must include the schedule order pool entry.'
)

console.log('PASS: MES schedule order pool static contract')
