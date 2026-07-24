const assert = require('node:assert/strict')
const { existsSync, readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const repoRoot = resolve(__dirname, '../..')
const pagePath = resolve(repoRoot, 'src/views/mes/pro/task/index.vue')
const componentPath = resolve(repoRoot, 'src/views/mes/pro/task/components/GanttChart.vue')
const helperPath = resolve(repoRoot, 'src/views/mes/pro/task/components/ganttReadability.ts')

assert.ok(existsSync(pagePath), 'missing production task page')
assert.ok(existsSync(componentPath), 'missing production task gantt component')
assert.ok(existsSync(helperPath), 'missing gantt readability helper')

const loadTsModule = (path) => {
  const source = readFileSync(path, 'utf8')
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      strict: true
    }
  }).outputText
  const module = { exports: {} }
  vm.runInNewContext(
    output,
    {
      module,
      exports: module.exports,
      require,
      console
    },
    { filename: path }
  )
  return module.exports
}

const {
  escapeGanttHtml,
  getGanttWorkOrderProcessLabel,
  getGanttTaskDurationHours,
  normalizeGanttTasksForReadability
} = loadTsModule(helperPath)

const tasks = normalizeGanttTasksForReadability([
  {
    id: '301_1',
    type: 'project',
    text: '冠状动脉棘突球囊扩张导管100支',
    workOrderCode: 'MO-READABLE'
  },
  {
    id: '303_1',
    type: 'task',
    parent: '301_1',
    text: '冠状动脉棘突球囊扩张导管100支',
    workOrderCode: 'MO-READABLE',
    process: 'welding',
    startDate: '2026-10-12 00:00:00',
    endDate: '2026-10-12 00:00:00',
    duration: 1,
    progress: 0.5
  },
  {
    id: '303_2',
    type: 'task',
    parent: '301_1',
    text: '冠状动脉棘突球囊扩张导管100支',
    workOrderCode: 'MO-READABLE',
    process: 'pack',
    startDate: '2026-10-13',
    endDate: '2026-10-13',
    duration: 2,
    progress: 0.25
  }
])

const sameDayTask = tasks.find((task) => task.id === '303_1')
assert.ok(sameDayTask, 'same-day task must be normalized')
assert.equal(sameDayTask.start_date.getFullYear(), 2026)
assert.equal(sameDayTask.start_date.getMonth(), 9)
assert.equal(sameDayTask.start_date.getDate(), 12)
assert.equal(sameDayTask.start_date.getHours(), 0)
assert.equal(sameDayTask.end_date.getDate(), 12)
assert.equal(sameDayTask.end_date.getHours(), 8)
assert.equal(getGanttTaskDurationHours(sameDayTask), 8)
assert.equal(sameDayTask.readabilityCompact, true)
assert.equal(getGanttWorkOrderProcessLabel(sameDayTask), 'MO-READABLE / welding')

const projectTask = tasks.find((task) => task.id === '301_1')
assert.ok(projectTask, 'project task must be kept')
assert.equal(projectTask.start_date.getDate(), 12)
assert.equal(projectTask.end_date.getDate(), 13)
assert.equal(projectTask.end_date.getHours(), 16)
assert.equal(projectTask.readabilityCompact, false)
assert.equal(getGanttWorkOrderProcessLabel(projectTask), 'MO-READABLE')

const unscheduled = normalizeGanttTasksForReadability([
  {
    id: '303_missing',
    type: 'task',
    text: 'bad task',
    workOrderCode: 'MO-MISSING',
    process: 'welding',
    startDate: '2026-10-12'
  }
])[0]
assert.equal(unscheduled.unscheduled, true)
assert.equal(unscheduled.readabilityMissingScheduleReason, '缺少结束时间')
assert.equal(unscheduled.end_date, undefined)
assert.throws(
  () => getGanttWorkOrderProcessLabel({ id: '303_no_code', type: 'task', process: 'welding' }),
  /missing workOrderCode/,
  'gantt display must fail fast when API omits workOrderCode'
)
assert.throws(
  () => getGanttWorkOrderProcessLabel({ id: '303_no_process', type: 'task', workOrderCode: 'MO-001' }),
  /missing process/,
  'task display must fail fast when API omits process'
)

assert.equal(escapeGanttHtml('task <one> & "two"'), 'task &lt;one&gt; &amp; &quot;two&quot;')

const component = readFileSync(componentPath, 'utf8')
const page = readFileSync(pagePath, 'utf8')
assert.doesNotMatch(component, /fallbackDate/, 'gantt must not invent today as fallback dates')
assert.match(component, /normalizeGanttTasksForReadability/, 'component must use readability normalization')
assert.match(component, /readonly \? 'day' : 'shift'/, 'readonly gantt must default to readable day scale')
assert.match(component, /gantt\.config\.min_column_width/, 'gantt must configure timeline column width')
assert.match(component, /READABLE_DAY_COLUMN_WIDTH/, 'readonly day scale must use readable day width')
assert.match(component, /READABLE_SHIFT_COLUMN_WIDTH/, 'edit shift scale must keep readable shift width')
assert.match(component, /show_unscheduled/, 'gantt must explicitly show unscheduled rows without fake dates')
assert.match(component, /gantt-short-task/, 'short tasks must receive a compact class')
assert.match(component, /gantt_task_link/, 'dependency links must be visually de-emphasized')
assert.match(component, /label:\s*'生产工单编码 \/ 工序'/, 'left grid must only expose work order code and process')
assert.match(component, /gantt\.config\.grid_width\s*=\s*420/, 'single identity column should keep the left grid readable')
assert.match(component, /getGanttWorkOrderProcessLabel/, 'component must use the centralized work order/process label')
assert.doesNotMatch(component, /name:\s*'workstation'/, 'left grid must not show workstation column')
assert.doesNotMatch(component, /label:\s*'工作站'/, 'left grid must not show workstation label')
assert.doesNotMatch(component, /label:\s*'开始时间'/, 'left grid must not show start time label')
assert.doesNotMatch(component, /label:\s*'结束时间'/, 'left grid must not show end time label')
assert.doesNotMatch(component, /gantt-task-label__progress/, 'gantt bars must not show progress percentage in labels')
assert.match(component, /leftside_text/, 'short gantt bars must render readable identity labels beside the bar')
assert.match(component, /rightside_text/, 'short gantt bars near the left edge must render identity labels on the right')
assert.match(component, /shouldPlaceSideLabelOnLeft/, 'short gantt side labels must avoid clipping at timeline edges')
assert.match(component, /shouldRenderSideLabel/, 'side labels must not bleed in from offscreen task bars')
assert.match(component, /SIDE_LABEL_VIEWPORT_PADDING/, 'side label placement must reserve viewport padding')
assert.match(component, /gantt\.getScrollState/, 'side label placement must use current timeline scroll state')
assert.match(component, /\$task/, 'side label placement must use the visible timeline width instead of the full grid scrollbar')
assert.match(component, /gantt\.posFromDate/, 'side label placement must compare task position with visible timeline')
assert.match(component, /refreshGanttTextAfterTimelineSettles/, 'side labels must be refreshed after gantt applies initial timeline scroll')
assert.match(component, /gantt-task-side-label/, 'short gantt bar side labels must have stable readable styling')
assert.match(
  component,
  /syncCollapsedProjectOverflowLayer/,
  'collapsed project rows outside the viewport must render a visible edge summary layer'
)
assert.match(
  component,
  /isRenderedProjectBarVisible[\s\S]*getTaskNode[\s\S]*getBoundingClientRect/,
  'collapsed project summaries must check the actual rendered project bar DOM before deciding no overlay is needed'
)
assert.doesNotMatch(
  component,
  /taskBounds\.left\s*>=\s*visibleBounds\.left[\s\S]{0,180}return null/,
  'collapsed project summaries must not disappear when the project starts inside the visible timeline but dhtmlx does not render a project bar'
)
assert.doesNotMatch(
  component,
  /gantt\.addTaskLayer/,
  'current dhtmlx runtime does not expose addTaskLayer, so collapse visibility must use the owned overlay layer'
)
assert.match(component, /gantt-collapsed-project-overflow/, 'collapsed offscreen project summaries must have stable styling')
assert.match(component, /onTaskClosed/, 'collapsed project summaries must refresh after project rows are folded')
assert.match(component, /onTaskOpened/, 'collapsed project summaries must refresh after project rows are expanded')
assert.match(component, /onGanttScroll/, 'collapsed project summaries must follow horizontal timeline scrolling')
assert.match(component, /\$open/, 'collapsed summary logic must read dhtmlx project open state')
assert.match(component, /dateIntervalDays/, 'readonly gantt must accept a date interval prop')
assert.match(component, /normalizeGanttDateIntervalDays/, 'date interval changes must be clamped to supported day steps')
assert.match(
  component,
  /SUPPORTED_GANTT_DATE_INTERVAL_DAYS\s*=\s*Array\.from\(\{\s*length:\s*15\s*\}/,
  'readonly gantt must support every day step from 1 to 15'
)
assert.match(component, /applyGanttScaleConfig/, 'gantt scale configuration must be reapplied when the interval slider changes')
assert.match(
  component,
  /unit:\s*'day'[\s\S]*step:\s*normalizeGanttDateIntervalDays\(props\.dateIntervalDays\)/,
  'readonly day scale must use the slider selected date interval as the day step'
)
assert.match(
  component,
  /scaleMode\s*===\s*'day'[\s\S]*\?\s*\[\s*\{\s*unit:\s*'day'[\s\S]*step:\s*normalizeGanttDateIntervalDays\(props\.dateIntervalDays\)[\s\S]*format:\s*dateIntervalScaleTemplate/,
  'readonly gantt must make the bottom visible grid scale use the selected date interval'
)
assert.doesNotMatch(
  component,
  /scaleMode\s*===\s*'day'[\s\S]*\?\s*\[\s*\{\s*unit:\s*'week'[\s\S]{0,180}weekScaleTemplate/,
  'readonly gantt must not leave the bottom visible grid scale fixed to weekly cells'
)
assert.match(component, /dateFromPos/, 'date interval redraw must capture the current visible date before changing scale')
assert.match(component, /showDate/, 'date interval redraw must keep the current visible date in view after changing scale')
assert.match(component, /watch\(\s*\(\)\s*=>\s*props\.dateIntervalDays/, 'gantt must react when the slider changes')
assert.match(component, /collapseAllProjects/, 'gantt must expose a batch collapse method')
assert.match(component, /expandAllProjects/, 'gantt must expose a batch expand method')
assert.match(
  component,
  /defineExpose\(\{\s*loadData,\s*collapseAllProjects,\s*expandAllProjects\s*\}\)/,
  'parent page must be able to call loadData, collapseAllProjects and expandAllProjects'
)
assert.match(component, /GANTT_ORDER_COLOR_PALETTE/, 'gantt must define a stable work-order color palette')
assert.match(component, /getGanttOrderColorClass/, 'gantt must assign a stable color class from workOrderCode')
assert.match(
  component,
  /gantt-project-bar[\s\S]*getGanttOrderColorClass\(task\)/,
  'project bars must include the work-order color class'
)
assert.match(
  component,
  /classes\.push\(getGanttOrderColorClass\(task\)\)/,
  'task bars must include the work-order color class'
)
assert.match(
  component,
  /gantt-collapsed-project-overflow[\s\S]*getGanttOrderColorClass\(task\)/,
  'collapsed project overflow summaries must keep the same work-order color'
)
assert.match(component, /\.gantt-order-color-0/, 'gantt order color classes must be styled')
assert.match(component, /\.gantt-order-color-1/, 'gantt must expose at least two distinguishable order colors')
assert.doesNotMatch(
  component,
  /\.gantt_task_line\.gantt-auto-task\s*\{[^}]*background:/,
  'schedule source styling must not override the work-order color'
)
assert.doesNotMatch(
  component,
  /\.gantt_task_line\.gantt-manual-task\s*\{[^}]*background:/,
  'manual source styling must not override the work-order color'
)
assert.doesNotMatch(
  component,
  /\.gantt_task_line\.gantt-risk-task\s*\{[^}]*background:/,
  'risk styling must not override the work-order color'
)
assert.doesNotMatch(component, /工作站<\/span>/, 'tooltip must not show workstation details')
assert.doesNotMatch(component, /开始<\/span>/, 'tooltip must not show start time details')
assert.doesNotMatch(component, /结束<\/span>/, 'tooltip must not show end time details')
assert.match(component, /tooltip_text/, 'task identity detail must remain available in tooltip')
assert.match(
  component,
  /\.gantt-readable-tooltip[\s\S]*color:\s*#fff/i,
  'tooltip body text must be white on the dark tooltip background'
)
assert.match(
  component,
  /\.gantt-readable-tooltip__title[\s\S]*color:\s*#fff/i,
  'tooltip title text must be white on the dark tooltip background'
)
assert.match(
  component,
  /\.gantt-readable-tooltip__row span[\s\S]*color:\s*#fff/i,
  'tooltip field labels must be white on the dark tooltip background'
)
assert.match(
  component,
  /\.gantt-readable-tooltip__row strong[\s\S]*color:\s*#fff/i,
  'tooltip field values must be white on the dark tooltip background'
)

assert.match(page, /<template\s+#header>/, 'production gantt card must use the header slot for controls')
assert.match(page, /ref="ganttChartRef"/, 'page must keep a ref to call exposed gantt methods')
assert.match(page, /:date-interval-days="ganttDateIntervalDays"/, 'page must pass slider selected day interval to gantt')
assert.match(page, /mes\.pro\.task\.gantt\.dateIntervalDays/, 'page must persist the date interval in browser storage')
assert.doesNotMatch(page, /ganttDateIntervalOptions\s*=/, 'page must not keep the old non-linear date interval options array')
assert.doesNotMatch(page, /ganttDateIntervalIndex/, 'page must not keep the old slider index indirection')
assert.match(page, /handleCollapseAllProjects/, 'page must wire a collapse-all handler')
assert.match(page, /handleExpandAllProjects/, 'page must wire an expand-all handler')
assert.match(page, /全部折叠/, 'page must expose a collapse-all button')
assert.match(page, /全部展开/, 'page must expose an expand-all button')
assert.match(page, /日期间隔/, 'page must label the slider as the date interval control')
assert.match(page, /天\/格/, 'page must show the selected day interval unit')
assert.match(
  page,
  /<el-slider[\s\S]*v-model="ganttDateIntervalDays"[\s\S]*:min="1"[\s\S]*:max="15"[\s\S]*:step="1"/,
  'page must render a 1~15 day Element Plus slider for date interval'
)
assert.match(page, /:show-tooltip="false"/, 'date interval slider should rely on stable marks and label instead of transient tooltip text')

const realE2e = readFileSync(resolve(repoRoot, 'tests/e2e/mes-pro-task-gantt-readability-real.e2e.js'), 'utf8')
assert.match(realE2e, /\/mes\/pro\/task\/gantt-list/, 'real E2E must wait for the current gantt API')
assert.match(realE2e, /\/mes\/pro\/schedule-order\/page/, 'real E2E must load the current schedule-order pool')
assert.match(realE2e, /completionFilter['"]\s*,\s*'INCOMPLETE'/, 'real E2E must compare against incomplete current schedule orders')
assert.match(realE2e, /scheduleOrderWorkOrderCodes/, 'real E2E must build the current schedule-order work-order code set')
assert.match(realE2e, /projectRowsOutsideScheduleOrderPool/, 'real E2E must fail fast when gantt project rows exceed current schedule-order pool')
assert.match(realE2e, /taskRowsOutsideScheduleOrderPool/, 'real E2E must fail fast when gantt task rows exceed current schedule-order pool')
assert.match(realE2e, /rowsMissingWorkOrderCode/, 'real E2E must fail fast when runtime API omits workOrderCode')
assert.match(realE2e, /gantt-list rows must include workOrderCode/, 'real E2E must explain the stale backend contract failure')
assert.match(realE2e, /rowsMissingProcess/, 'real E2E must fail fast when runtime API omits task process')
assert.match(realE2e, /gantt-list task rows must include process/, 'real E2E must explain missing process contract failures')
assert.match(realE2e, /rowsMissingScheduleOrderProcessId/, 'real E2E must fail fast when runtime API returns unscheduled task rows')
assert.match(realE2e, /projectRowsWithoutScheduledTask/, 'real E2E must fail fast when runtime API returns project rows without scheduled task children')
assert.match(realE2e, /page\.mouse\.move\(0,\s*0\)/, 'real E2E must move from outside before hovering a compact task')
assert.match(realE2e, /waitForReadableTooltip/, 'real E2E must use the dedicated compact-task hover helper for dhtmlx tooltip reliability')
assert.match(realE2e, /steps:\s*12/, 'real E2E hover helper must use stepped mouse movement inside the compact task bar')
assert.match(realE2e, /scrollGanttToBottomIfScrollable/, 'real E2E must scroll lower virtual rows when scheduled-only data overflows')
assert.match(realE2e, /bottomScrollResult/, 'real E2E must record whether scheduled-only data still has lower virtual rows')
assert.match(realE2e, /no vertical scroll after scheduled-only filtering/, 'real E2E must explain when current scheduled-only data has no lower rows to scroll')
assert.match(realE2e, /pageErrors/, 'real E2E must fail if dhtmlx template rendering throws after refresh and collapse')
assert.match(realE2e, /collapseOpenProjectRows/, 'real E2E must collapse project rows through the visible tree control')
assert.match(realE2e, /collapsedProjectOverflowCount/, 'real E2E must assert folded offscreen work orders remain visible')
assert.match(realE2e, /collectVisibleOrderBarColors/, 'real E2E must assert work-order bars use distinguishable colors')
assert.match(realE2e, /distinctOrderColorCount/, 'real E2E must report the count of distinct visible order colors')
assert.match(realE2e, /getByRole\('button', \{ name: '全部折叠' \}\)/, 'real E2E must use the collapse-all toolbar button')
assert.match(realE2e, /getByRole\('button', \{ name: '全部展开' \}\)/, 'real E2E must use the expand-all toolbar button')
assert.match(realE2e, /日期间隔/, 'real E2E must verify the date interval slider label')
assert.match(realE2e, /setDateIntervalSlider/, 'real E2E must change the date interval through the real slider')
assert.match(realE2e, /dateIntervalAfter/, 'real E2E must record the timeline metrics after slider changes')
assert.match(realE2e, /bottomScaleCellDaySteps/, 'real E2E must measure bottom visible grid cell date steps')
assert.match(
  realE2e,
  /bottom visible grid cells must advance by 15 days/,
  'real E2E must assert the selected 15 day interval reaches the visible grid cells'
)
assert.match(realE2e, /天\/格/, 'real E2E must assert the selected day interval unit remains visible')

const backendRoot = resolve(repoRoot, '..', 'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes')
const ganttVo = readFileSync(
  resolve(backendRoot, 'controller/admin/pro/task/vo/GanttDataRespVO.java'),
  'utf8'
)
const taskController = readFileSync(
  resolve(backendRoot, 'controller/admin/pro/task/MesProTaskController.java'),
  'utf8'
)
const ganttRespVo = readFileSync(
  resolve(backendRoot, 'controller/admin/pro/task/vo/GanttDataRespVO.java'),
  'utf8'
)
const autoScheduleService = readFileSync(
  resolve(backendRoot, 'service/pro/schedule/MesProAutoScheduleServiceImpl.java'),
  'utf8'
)
assert.match(ganttVo, /private String workOrderCode;/, 'gantt API must expose workOrderCode')
assert.match(
  taskController,
  /\.setWorkOrderCode\(workOrder\.getCode\(\)\)/,
  'current gantt API must populate workOrderCode'
)
assert.match(
  taskController,
  /resolveGanttTaskProcessName/,
  'current gantt API must populate process through the central resolver'
)
assert.match(
  taskController,
  /MesProScheduleOrderProcessDO/,
  'current gantt API must use schedule-order process snapshots for scheduled task names'
)
assert.match(
  ganttRespVo,
  /private Long scheduleOrderProcessId;/,
  'current gantt API must expose scheduleOrderProcessId for scheduled-only verification'
)
assert.match(
  ganttRespVo,
  /private Long scheduleOrderId;/,
  'current gantt API must expose scheduleOrderId for latest-replan-scope verification'
)
assert.match(
  taskController,
  /List<MesProTaskDO> scheduledTasks = allTasks\.stream\(\)/,
  'current gantt API must filter task rows to scheduled tasks before rendering'
)
assert.match(
  taskController,
  /isScheduledGanttProcessTask/,
  'current gantt API must use a central scheduled participation guard'
)
assert.match(
  taskController,
  /activeScheduleOrderMap/,
  'current gantt API must filter task rows by the current active schedule-order pool'
)
assert.match(
  taskController,
  /resolveLatestAppliedReplanScheduleOrderIds/,
  'current gantt API must use the latest successful replan scope after page refresh'
)
assert.match(
  taskController,
  /latestAppliedReplanScheduleOrderIds\.contains\(entry\.getKey\(\)\)/,
  'current gantt API must narrow active schedule orders to the latest applied replan selection'
)
assert.match(
  taskController,
  /MesProScheduleOrderStatusEnum\.FINISHED/,
  'current gantt API must exclude stale schedule orders from current gantt rows'
)
assert.match(
  taskController,
  /getScheduleOrderId\(\)/,
  'current gantt API must use task scheduleOrderId for current schedule-order membership'
)
assert.doesNotMatch(
  taskController,
  /\|\| task\.getProcessId\(\) != null && task\.getProcessId\(\) > 0/,
  'current gantt API must not treat ordinary positive processId as scheduled participation'
)
assert.match(
  autoScheduleService,
  /\.setWorkOrderCode\(workOrder\.getCode\(\)\)/,
  'schedule preview project rows must populate workOrderCode'
)
assert.match(
  autoScheduleService,
  /\.setWorkOrderCode\(workOrderCode\)/,
  'schedule preview task rows must inherit workOrderCode'
)

console.log('mes-pro-task gantt readability static contract passed')
