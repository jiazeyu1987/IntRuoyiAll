const assert = require('node:assert/strict')
const fs = require('node:fs')
const Module = require('node:module')
const path = require('node:path')
const ts = require('typescript')

const root = path.resolve(__dirname, '../..')
const unifiedListTemplatePath = path.join(root, 'src/components/UnifiedListTemplate/index.vue')
const multiFilterComponentPath = path.join(root, 'src/components/TableMultiFilter/index.vue')
const multiFilterFieldPath = path.join(root, 'src/components/TableMultiFilter/MultiFilterField.vue')
const multiFilterHookPath = path.join(root, 'src/hooks/web/useTableMultiFilter.ts')
const quickFilterHookPath = path.join(root, 'src/hooks/web/useTableQuickFilter.ts')

assert.equal(fs.existsSync(unifiedListTemplatePath), true, '统一列表模板组件必须存在。')
assert.equal(fs.existsSync(multiFilterComponentPath), true, '多维度筛选组件必须存在。')
assert.equal(fs.existsSync(multiFilterFieldPath), true, '多维度筛选字段组件必须存在。')
assert.equal(fs.existsSync(multiFilterHookPath), true, '多维度筛选 hook 必须存在。')
assert.equal(fs.existsSync(quickFilterHookPath), true, '标准列表快速筛选 hook 必须存在。')

const unifiedListTemplateSource = fs.readFileSync(unifiedListTemplatePath, 'utf8')
const multiFilterComponentSource = fs.readFileSync(multiFilterComponentPath, 'utf8')
const multiFilterFieldSource = fs.readFileSync(multiFilterFieldPath, 'utf8')
const multiFilterHookSource = fs.readFileSync(multiFilterHookPath, 'utf8')
const quickFilterHookSource = fs.readFileSync(quickFilterHookPath, 'utf8')

const loadTypeScriptModule = (filename, source) => {
  const compiled = ts.transpileModule(source, {
    compilerOptions: {
      esModuleInterop: true,
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  }).outputText
  const loadedModule = new Module(filename, module)
  loadedModule.filename = filename
  loadedModule.paths = Module._nodeModulePaths(path.dirname(filename))
  const originalRequire = loadedModule.require.bind(loadedModule)
  loadedModule.require = (request) =>
    request === 'element-plus'
      ? { ElMessage: { error() {}, warning() {} } }
      : originalRequire(request)
  loadedModule._compile(compiled, filename)
  return loadedModule.exports
}

assert.match(unifiedListTemplateSource, /import TableMultiFilter from '@\/components\/TableMultiFilter\/index\.vue'/, '标准列表模板必须导入多维度筛选组件。')
assert.match(unifiedListTemplateSource, /showMultiFilter/, '标准列表模板必须提供多维度筛选开关。')
assert.match(unifiedListTemplateSource, /multiFilterDefinitions/, '标准列表模板必须接收多维度筛选定义。')
assert.match(unifiedListTemplateSource, /multiFilterState/, '标准列表模板必须接收多维度筛选状态。')
assert.doesNotMatch(
  unifiedListTemplateSource,
  /<TableQuickFilter[\s\S]*?<\/TableQuickFilter>/,
  '标准列表模板不得再渲染旧快速筛选组件，默认筛选必须复用条件 Tab。'
)
assert.match(
  unifiedListTemplateSource,
  /<TableMultiFilter[\s\S]*:filter-definitions="resolvedStandardFilterDefinitions"[\s\S]*:state="resolvedStandardFilterState"/,
  '标准列表模板必须通过统一解析后的定义和状态渲染多维条件 Tab。'
)
assert.match(
  unifiedListTemplateSource,
  /const quickDefinitionsAsMultiFilterDefinitions = computed<ListMultiFilterDefinition\[\]>/,
  '标准列表模板必须把历史快速筛选定义转换成条件 Tab 定义。'
)
assert.match(
  unifiedListTemplateSource,
  /const shouldRenderStandardConditionFilter = computed\(\(\) => \{[\s\S]*props\.showMultiFilter === true[\s\S]*props\.showQuickFilter !== false/,
  '标准列表模板必须统一判断显式多维筛选和默认快速筛选是否展示条件 Tab。'
)
assert.match(
  unifiedListTemplateSource,
  /const resolvedStandardFilterDefinitions = computed\(\(\) =>[\s\S]*props\.showMultiFilter === true[\s\S]*props\.multiFilterDefinitions[\s\S]*quickDefinitionsAsMultiFilterDefinitions\.value/,
  '显式多维筛选必须继续使用页面提供的 multiFilterDefinitions，默认列表必须复用 quick filter definitions。'
)
assert.match(
  unifiedListTemplateSource,
  /const resolvedStandardFilterState = computed\(\(\) =>[\s\S]*props\.showMultiFilter === true[\s\S]*resolvedMultiFilterState\.value[\s\S]*resolvedQuickFilterStateAsMultiFilter\.value/,
  '标准列表模板必须按筛选模式选择多维状态或快速筛选桥接状态。'
)
assert.match(
  unifiedListTemplateSource,
  /@update:state="handleStandardFilterStateUpdate"/,
  '标准列表模板必须通过统一处理函数分发筛选状态更新。'
)
assert.match(
  unifiedListTemplateSource,
  /@query="handleStandardFilterQuery"/,
  '标准列表模板必须通过统一处理函数分发查询事件。'
)
assert.match(
  unifiedListTemplateSource,
  /@reset="handleStandardFilterReset"/,
  '标准列表模板必须通过统一处理函数分发重置事件。'
)
assert.match(
  unifiedListTemplateSource,
  /@remove="handleStandardFilterRemove"/,
  '标准列表模板必须通过统一处理函数分发删除条件事件。'
)
assert.match(
  unifiedListTemplateSource,
  /const handleStandardFilterStateUpdate = \(state: ListMultiFilterState\) => \{[\s\S]*props\.showMultiFilter === true[\s\S]*emit\('update:multiFilterState', state\)[\s\S]*emit\('update:quickFilterState', toQuickFilterState\(state\)\)/,
  '显式多维筛选和默认快速筛选桥接必须分别回写各自状态。'
)
assert.match(
  unifiedListTemplateSource,
  /const handleStandardFilterQuery = \(\) => \{[\s\S]*props\.showMultiFilter === true[\s\S]*emit\('multi-filter-query'\)[\s\S]*emit\('quick-filter-query'\)/,
  '条件 Tab 查询必须按当前模式分发正式查询事件。'
)
assert.match(
  unifiedListTemplateSource,
  /const handleStandardFilterReset = async \(\) => \{[\s\S]*props\.showMultiFilter === true[\s\S]*emit\('multi-filter-reset'\)[\s\S]*emit\('update:quickFilterState'[\s\S]*conditions: \[\][\s\S]*emit\('quick-filter-query'\)/,
  '条件 Tab 重置必须支持显式多维重置，并让默认列表回到无筛选条件后查询。'
)
assert.match(
  unifiedListTemplateSource,
  /const handleStandardFilterRemove = \(conditionId: string\) => \{[\s\S]*props\.showMultiFilter === true[\s\S]*emit\('multi-filter-remove', conditionId\)/,
  '显式多维筛选删除条件时必须继续透出 multi-filter-remove 事件。'
)
assert.match(
  unifiedListTemplateSource,
  /\.unified-list-template__multi-filter\s*\{[\s\S]*flex:\s*1 1 100%;[\s\S]*min-width:\s*min\(720px,\s*100%\);/,
  '标准列表模板必须防止多维筛选在复杂工具栏页面被压缩成 0 宽。'
)

assert.match(multiFilterComponentSource, /name:\s*'TableMultiFilter'/, '多维度筛选组件必须声明稳定组件名。')
assert.match(multiFilterComponentSource, /table-multi-filter__tabs-row/, '多维度筛选必须在红框区域提供条件 Tab 行。')
assert.match(multiFilterComponentSource, /<el-tabs[\s\S]*<el-tab-pane/, '多维度筛选必须用 Tab 承载每一条筛选条件。')
assert.match(multiFilterComponentSource, /addConditionTab/, '多维度筛选必须支持点击加号新增条件 Tab。')
assert.match(multiFilterComponentSource, /removeActiveConditionTab/, '多维度筛选必须支持点击减号删除当前条件 Tab。')
assert.match(multiFilterComponentSource, /table-multi-filter__field-select/, '多维度筛选当前 Tab 必须先选择筛选字段。')
assert.match(multiFilterComponentSource, /clearAllConditions/, '多维度筛选组件必须支持一键清空筛选。')
assert.doesNotMatch(multiFilterComponentSource, /<el-popover[\s\S]*更多筛选/, '条件 Tab 方案不得保留旧的更多筛选弹层。')
assert.doesNotMatch(multiFilterComponentSource, /table-multi-filter__chips/, '条件 Tab 本身承载已选条件，不得保留旧 chip 汇总。')
assert.doesNotMatch(
  multiFilterComponentSource,
  /点击右侧加号新增筛选条件。|table-multi-filter__condition-empty/,
  '条件为空时只保留 Tab 行的“暂无筛选条件”，不得额外显示第二行新增提示。'
)
assert.match(
  multiFilterComponentSource,
  /v-if="hasUnappliedChanges"[\s\S]*class="table-multi-filter__pending-status"[\s\S]*筛选条件待应用/,
  '编辑条件但未查询时，筛选区必须明确显示“筛选条件待应用”，不能让草稿 Tab 冒充已执行结果。'
)
assert.match(
  multiFilterComponentSource,
  /const hasUnappliedChanges = computed\(\(\) =>[\s\S]*hasMultiFilterDraftChanges\([\s\S]*props\.filterDefinitions[\s\S]*props\.state/,
  '待应用状态必须比较当前草稿与最后一次成功查询快照。'
)
assert.match(
  multiFilterComponentSource,
  /appliedConditions:\s*props\.state\.appliedConditions/,
  '条件 Tab 编辑回写时必须保留已执行快照。'
)
assert.doesNotMatch(multiFilterComponentSource, /localStorage|sessionStorage/, '多维度筛选组件不得使用本地存储兜底。')

assert.match(multiFilterFieldSource, /showLabel/, '多维度筛选字段组件必须支持在 Tab 条件行中隐藏固定字段标签。')
assert.match(multiFilterFieldSource, /type === 'multiSelect'/, '多维度筛选字段必须支持多选下拉。')
assert.match(multiFilterFieldSource, /type === 'date'/, '多维度筛选字段必须支持单日期。')
assert.match(multiFilterFieldSource, /type === 'dateRange'/, '多维度筛选字段必须支持日期范围。')
assert.match(multiFilterFieldSource, /type === 'numberRange'/, '多维度筛选字段必须支持数字范围。')
assert.match(multiFilterFieldSource, /type === 'autocomplete'/, '多维度筛选字段必须支持自动补全。')
assert.match(multiFilterFieldSource, /updateRangeValue/, '多维度筛选字段必须独立更新范围起止值。')

assert.match(multiFilterHookSource, /export type ListMultiFilterFieldType = 'text' \| 'select' \| 'multiSelect' \| 'date' \| 'dateRange' \| 'autocomplete' \| 'numberRange'/, '多维度筛选字段类型必须覆盖文本、单选、多选、单日期、日期范围、自动补全和数字范围。')
assert.match(multiFilterHookSource, /export interface ListMultiFilterDefinition/, '多维度筛选 hook 必须导出筛选定义类型。')
assert.match(multiFilterHookSource, /export interface ListMultiFilterCondition/, '多维度筛选 hook 必须导出条件类型。')
assert.match(multiFilterHookSource, /id\?:\s*string/, '多维度筛选条件必须支持稳定 Tab id。')
assert.match(multiFilterHookSource, /conditions:\s*ListMultiFilterCondition\[\]/, '多维度筛选状态必须使用条件数组。')
assert.match(multiFilterHookSource, /activeConditionId\?:\s*string/, '多维度筛选状态必须记录当前编辑的条件 Tab。')
assert.match(
  multiFilterHookSource,
  /appliedConditions:\s*ListMultiFilterCondition\[\]/,
  '多维度筛选状态必须记录最后一次成功查询的条件快照。'
)
assert.match(
  multiFilterHookSource,
  /export const hasMultiFilterDraftChanges = \([\s\S]*normalizeMultiFilterConditions[\s\S]*state\.conditions[\s\S]*state\.appliedConditions/,
  '草稿/已执行判定必须基于正规化条件比较。'
)
assert.match(
  multiFilterHookSource,
  /export const getDefaultMultiFilterOperator = \(definition: ListMultiFilterDefinition\) =>\s*definition\.operators\?\.\[0\]\s*\|\|\s*DEFAULT_OPERATOR\[definition\.type\]/,
  '多维度筛选默认操作符必须优先使用字段显式声明的第一个操作符。'
)
assert.match(multiFilterHookSource, /queryParamKeys\?: \[string, string\]/, '多维度筛选定义必须支持范围字段映射到正式 query 起止参数。')
assert.match(multiFilterHookSource, /queryParams\.pageNo = 1/, '多维度筛选查询和重置必须回到第一页。')
assert.match(multiFilterHookSource, /delete queryParamTarget\.multiFilters/, '多维度筛选必须清理旧 multiFilters 参数。')
assert.match(multiFilterHookSource, /unmappedConditions/, '没有正式 queryParamKey 的条件必须进入显式 multiFilters，不得静默丢失。')
assert.match(multiFilterHookSource, /validateDuplicateMappedConditions/, '多维度筛选必须校验重复正式 query 参数，避免交集条件被覆盖。')
assert.match(
  multiFilterHookSource,
  /const appliedConditions = cloneMultiFilterConditions\(state\.conditions\)[\s\S]*await reload\(\)[\s\S]*state\.appliedConditions = appliedConditions/,
  '只有列表重载成功后才能把本次草稿标记为已执行。'
)
assert.match(
  multiFilterHookSource,
  /const previousQueryParams = snapshotMultiFilterParams\(\)[\s\S]*try \{[\s\S]*await reload\(\)[\s\S]*\} finally \{[\s\S]*restoreMultiFilterParams\(previousQueryParams\)/,
  '列表重载失败时必须回滚正式 query 参数，同时保留原始异常。'
)
assert.match(
  multiFilterHookSource,
  /const resetMultiFilter = async \(\) => \{[\s\S]*await reload\(\)[\s\S]*state\.appliedConditions = \[\]/,
  '重置查询成功后必须同步清空已执行快照。'
)
assert.doesNotMatch(multiFilterHookSource, /localStorage|sessionStorage/, '多维度筛选 hook 不得使用本地存储兜底。')
assert.doesNotMatch(multiFilterHookSource, /catch\s*\(/, '多维度筛选 hook 不得吞异常或降级。')

assert.match(
  quickFilterHookSource,
  /appliedConditions:\s*\[\]/,
  '默认标准列表也必须初始化已执行条件快照。'
)
assert.match(
  quickFilterHookSource,
  /const appliedConditions = cloneMultiFilterConditions\(state\.conditions \|\| \[\]\)[\s\S]*await reload\(\)[\s\S]*state\.appliedConditions = appliedConditions/,
  '快速筛选桥接的条件 Tab 也必须在重载成功后才更新已执行快照。'
)

const runDraftAndAppliedStateContract = async () => {
  const {
    hasMultiFilterDraftChanges,
    useTableMultiFilter
  } = loadTypeScriptModule(multiFilterHookPath, multiFilterHookSource)
  const definitions = [
    {
      key: 'admissionStatus',
      label: '入池状态',
      type: 'select',
      queryParamKey: 'admissionStatus',
      options: [
        { label: '可入池', value: 'READY_TO_ADMIT' },
        { label: '阻断', value: 'BLOCKED' }
      ]
    }
  ]
  const queryParams = { pageNo: 3 }
  let reloadShouldFail = true
  const filter = useTableMultiFilter(
    'mes.pro.scheduleOrder.admissionDiff',
    definitions,
    queryParams,
    async () => {
      if (reloadShouldFail) throw new Error('expected reload failure')
    }
  )

  filter.setCondition({
    id: 'admissionStatus',
    key: 'admissionStatus',
    operator: 'eq',
    value: 'READY_TO_ADMIT'
  })
  assert.equal(
    hasMultiFilterDraftChanges(definitions, filter.state),
    true,
    '未查询的可入池条件必须保持待应用状态。'
  )
  await assert.rejects(filter.applyMultiFilter(), /expected reload failure/)
  assert.deepEqual(
    filter.state.appliedConditions,
    [],
    '列表重载失败时不得把草稿条件冒充为已执行。'
  )
  assert.equal(
    Object.hasOwn(queryParams, 'admissionStatus'),
    false,
    '首次查询失败后必须回滚未成功应用的正式参数。'
  )

  reloadShouldFail = false
  await filter.applyMultiFilter()
  assert.equal(queryParams.admissionStatus, 'READY_TO_ADMIT', '成功查询必须提交正式入池状态参数。')
  assert.equal(
    hasMultiFilterDraftChanges(definitions, filter.state),
    false,
    '成功重载后草稿必须与已执行快照一致。'
  )

  filter.setCondition({
    id: 'admissionStatus',
    key: 'admissionStatus',
    operator: 'eq',
    value: 'BLOCKED'
  })
  assert.equal(
    hasMultiFilterDraftChanges(definitions, filter.state),
    true,
    '从可入池改为阻断但未查询时，必须显示待应用。'
  )
  assert.equal(
    filter.state.appliedConditions[0]?.value,
    'READY_TO_ADMIT',
    '未查询时最后一次已执行口径必须仍为可入池。'
  )

  reloadShouldFail = true
  await assert.rejects(filter.applyMultiFilter(), /expected reload failure/)
  assert.equal(
    queryParams.admissionStatus,
    'READY_TO_ADMIT',
    '阻断查询失败后，后续分页仍必须使用上一次成功的可入池参数。'
  )

  reloadShouldFail = false
  await filter.resetMultiFilter()
  assert.deepEqual(filter.state.conditions, [], '重置必须清空草稿条件。')
  assert.deepEqual(filter.state.appliedConditions, [], '重置重载成功后必须清空已执行条件。')
}

runDraftAndAppliedStateContract()
  .then(() => console.log('PASS: unified list template multi-filter static and state contract'))
  .catch((error) => {
    console.error(error)
    process.exitCode = 1
  })
