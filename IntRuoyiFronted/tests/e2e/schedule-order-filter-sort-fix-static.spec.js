const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(workspaceRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const tableMultiFilter = readSource('IntRuoyiFronted/src/components/TableMultiFilter/index.vue')
const multiFilterHook = readSource('IntRuoyiFronted/src/hooks/web/useTableMultiFilter.ts')
const page = readSource('IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue')
const mainList = readSource(
  'IntRuoyiFronted/src/views/mes/pro/scheduleorder/components/ScheduleOrderMainList.vue'
)
const api = readSource('IntRuoyiFronted/src/api/mes/pro/scheduleorder/index.ts')
const backendReq = readSource(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/scheduleorder/vo/MesProScheduleOrderPageReqVO.java'
)
const backendMapper = readSource(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/scheduleorder/MesProScheduleOrderMapper.java'
)

const removeTabMatch = tableMultiFilter.match(
  /const removeActiveConditionTab = \(\) => \{([\s\S]*?)\n\}/
)
assert.ok(removeTabMatch, 'TableMultiFilter 必须实现 removeActiveConditionTab。')
const removeTabBody = removeTabMatch[1]
assert.match(
  removeTabBody,
  /const removedConditionId = activeConditionId\.value/,
  '删除当前筛选条件时必须先捕获被删除 condition id，避免状态更新后取到新的 active id。'
)
assert.match(
  removeTabBody,
  /emit\('remove', removedConditionId\)/,
  '删除当前筛选条件事件必须发送被删除的 condition id。'
)
assert.doesNotMatch(
  removeTabBody,
  /emit\('remove', activeConditionId\.value\)/,
  '删除当前筛选条件不得在状态更新后再读取 activeConditionId。'
)

assert.match(
  multiFilterHook,
  /const removeConditionAndApply = async \(conditionIdOrKey: string\) => \{/,
  '多维筛选 hook 必须提供删除并立即应用剩余条件的正式方法。'
)
assert.match(
  multiFilterHook,
  /removeConditionAndApply[\s\S]*await applyMultiFilter\(\)/,
  '删除已应用条件后必须复用正式 applyMultiFilter 重新写入参数并刷新列表。'
)
assert.match(
  multiFilterHook,
  /if \(!normalizeMultiFilterCondition\(definition, condition\)\) \{[\s\S]*ElMessage\.warning\(`请填写\$\{definition\.label\}筛选条件。`\)[\s\S]*return false[\s\S]*\}/,
  '存在已选择字段但值为空的条件 Tab 时，必须阻断查询，不能静默清空条件后恢复全量数据。'
)

assert.match(
  page,
  /@multi-filter-remove="scheduleOrderMultiFilter\.removeConditionAndApply"/,
  '排产工单删除单个多维筛选条件必须立即应用剩余条件并刷新列表。'
)
assert.match(
  mainList,
  /:sort-state="sortState"/,
  '排产工单主列表包装组件必须把受控排序状态传入 UnifiedListTemplate。'
)
assert.match(
  mainList,
  /@update:sort-state="emit\('update:sortState', \$event\)"[\s\S]*@sort-change="emit\('sortChange', \$event\)"/,
  '排产工单主列表包装组件必须透传排序状态更新和标准排序事件。'
)
assert.match(
  page,
  /v-model:sort-state="scheduleOrderSortState"[\s\S]*@sort-change="handleScheduleOrderSortChange"/,
  '排产工单页面必须接管标准列表排序状态和排序事件。'
)
assert.match(
  page,
  /const scheduleOrderSortState = ref<\{[\s\S]*order\?: 'ascending' \| 'descending' \| null[\s\S]*\}>\(\{\}\)/,
  '排产工单页面必须维护受控排序状态。'
)
assert.match(
  page,
  /const handleScheduleOrderSortChange = async \(\{ prop, order \}: \{ prop\?: string; order\?: string \| null \}\) => \{/,
  '排产工单页面必须实现正式排序事件处理器。'
)
assert.match(
  page,
  /scheduleOrderQueryParams\.sortField = prop === 'priorityNo' \? 'priorityNo' : undefined[\s\S]*scheduleOrderQueryParams\.sortOrder = order === 'ascending' \? 'asc' : 'desc'[\s\S]*await getScheduleOrderList\(\)/,
  '优先级排序必须写入正式请求参数并触发排产工单列表请求。'
)
assert.match(
  page,
  /const syncScheduleOrderPriorityAriaSort = async \(\) => \{[\s\S]*setAttribute\('aria-sort', ariaSort\)[\s\S]*\}/,
  '排产工单页面必须把优先级表头 th 的 aria-sort 同步到当前排序方向。'
)
assert.match(
  page,
  /watch\(\s*\(\) => scheduleOrderSortState\.value\.order,[\s\S]*syncScheduleOrderPriorityAriaSort/,
  '优先级排序方向变化后必须重新同步 aria-sort。'
)
assert.match(
  page,
  /<el-input-number[\s\S]*v-model="priorityForm\.priorityNo"[\s\S]*aria-label="新优先级"/,
  '优先级编辑输入必须具备可访问名称。'
)

for (const token of ['sortField?: string', "sortOrder?: 'asc' | 'desc'"]) {
  assert.ok(api.includes(token), `排产工单前端 API 请求类型必须包含正式排序字段：${token}`)
}
assert.match(backendReq, /private String sortField;/, '后端分页请求 VO 必须声明排序字段。')
assert.match(backendReq, /private String sortOrder;/, '后端分页请求 VO 必须声明排序方向。')
assert.match(
  backendMapper,
  /applyPageSort\(queryWrapper, reqVO\)/,
  '排产工单分页 Mapper 必须在 selectPage 中应用正式排序白名单。'
)
assert.match(
  backendMapper,
  /"priorityNo"\.equals\(reqVO\.getSortField\(\)\)[\s\S]*MesProScheduleOrderDO::getPriorityNo/,
  '后端排序白名单必须支持 priorityNo。'
)
assert.match(
  backendMapper,
  /throw new IllegalArgumentException\("排产工单排序字段不支持: " \+ reqVO\.getSortField\(\)\)/,
  '后端遇到不支持排序字段必须 fail fast，不能静默降级为默认排序。'
)

console.log('PASS: schedule order filter removal, promise date validation, and priority sort contract')
