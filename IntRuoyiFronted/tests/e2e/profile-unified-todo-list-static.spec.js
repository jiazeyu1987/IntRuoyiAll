const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const workbench = read('src/views/Profile/components/ProfileWorkbench.vue')

const forbiddenWorkbenchTokens = [
  'BPM 审批中心',
  '审批任务',
  '待办审批',
  '已办审批',
  '我的申请',
  'openApprovalCenter',
  'getTaskTodoPage',
  'getTaskDonePage',
  'getProcessInstanceMyPage'
]

assert.doesNotMatch(
  profileIndex,
  /<ProfileUser|ProfileUser|profileUserRef|w-1\/3|w-2\/3/,
  '个人中心必须移除左侧个人信息卡片和刷新引用，改为单栏个人中心。'
)
assert.match(profileIndex, /<ProfileWorkbench\s*\/>/, '个人工作台页签必须保留 ProfileWorkbench。')
assert.doesNotMatch(
  workbench,
  /profile-workbench__header|profile-workbench__title|profile-workbench__subtitle|>\s*个人工作台\s*<|统一查看当前账号在文控、批记录、排产、展厅和行政中的业务待办/,
  '个人工作台页签内容不应显示顶部标题说明区。'
)

for (const token of forbiddenWorkbenchTokens) {
  assert.doesNotMatch(
    workbench,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `个人工作台不应继续暴露审批中心聚合内容：${token}`
  )
}

assert.match(workbench, /data-testid="profile-unified-todo-list"/, '个人工作台必须只有统一待办列表标识。')
assert.match(
  workbench,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  '个人工作台必须导入标准列表模板 UnifiedListTemplate。'
)
const unifiedTemplateMatch = workbench.match(
  /<UnifiedListTemplate[\s\S]*?table-key="profile\.workbench\.todo"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(unifiedTemplateMatch, '个人工作台待办列表必须使用 tableKey=profile.workbench.todo 接入 UnifiedListTemplate。')
const unifiedTemplate = unifiedTemplateMatch[0]
for (const [pattern, description] of [
  [/:filter-definitions="todoQuickFilterDefinitions"/, '快速过滤字段'],
  [/:quick-filter-state="todoQuickFilter\.state"/, '快速过滤状态'],
  [/@quick-filter-query="todoQuickFilter\.applyQuickFilter"/, '快速过滤查询'],
  [/:columns="todoColumns"/, '显示字段列配置'],
  [/@column-change="saveTodoColumnConfig"/, '显示字段保存'],
  [/@pagination="handleTodoPagination"/, '标准分页事件'],
  [/v-model:page="queryParams\.pageNo"/, '标准分页页码'],
  [/v-model:limit="queryParams\.pageSize"/, '标准分页条数']
]) {
  assert.match(unifiedTemplate, pattern, `UnifiedListTemplate 必须接入${description}。`)
}
assert.match(workbench, /<el-table[\s>]/, '个人工作台必须使用一张待办表格。')
assert.equal(
  (workbench.match(/<el-table[\s>]/g) || []).length,
  1,
  '个人工作台只能有一张待办表格。'
)
assert.match(
  unifiedTemplate,
  /data-user-table-column-explicit[\s\S]*data-user-table-key="profile\.workbench\.todo"/,
  '待办表格必须接入标准列表模板的显示字段和列宽持久化。'
)
assert.doesNotMatch(workbench, /profile-workbench__table-shell/, '表格外壳必须交给 UnifiedListTemplate 管理。')
assert.doesNotMatch(workbench, /<Pagination\b/, '个人工作台不得绕过 UnifiedListTemplate 单独放分页。')

for (const columnLabel of ['任务类型', '来源', '待办详情', '状态/时间', '操作']) {
  assert.match(workbench, new RegExp(`label="${columnLabel}"`), `待办表格必须包含列：${columnLabel}`)
}

assert.doesNotMatch(
  workbench,
  /到期：\s*\{\{\s*row\.dueAt\s*\}\}/,
  '个人工作台状态/时间列不得显示原始到期时间。'
)
assert.doesNotMatch(
  workbench,
  /时间：\s*\{\{\s*row\.createdAt\s*\}\}|时间未提供/,
  '个人工作台状态/时间列不得显示任何原始时间行。'
)

for (const taskType of ['文控', '批记录', '排产', '展厅', '行政']) {
  assert.match(workbench, new RegExp(`['"]${taskType}['"]`), `任务类型筛选必须包含：${taskType}`)
}

for (const apiToken of [
  'getMyDistributionTaskPage',
  'getMyTrainingTaskPage',
  'getEdhrWorkTaskMyPage',
  'ProWorkOrderApi.getWorkOrderPage',
  '/showroom/assignment/page'
]) {
  assert.match(workbench, new RegExp(apiToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `待办来源必须接入真实接口：${apiToken}`)
}

assert.doesNotMatch(
  workbench,
  /getTaskTodoPage|getTaskDonePage|getProcessInstanceMyPage|\/approval-center|moduleCode|viewType/,
  '个人工作台不得接入 BPM/OA/审批中心待办或跳转审批中心。'
)

assert.doesNotMatch(
  workbench,
  /const\s+requirePageList\s*=\s*<T>/,
  'ProfileWorkbench 的 PageResult 校验助手不得使用会触发 Vue ESLint 解析错误的泛型箭头函数。'
)
assert.match(
  workbench,
  /function\s+requirePageList<T>\(/,
  'ProfileWorkbench 的 PageResult 校验助手应使用普通泛型函数，确保 Vite ESLint 可解析。'
)

assert.match(
  workbench,
  /checkPermi\(\['mes:pro-edhr-work-task:query'\]\)[\s\S]*checkPermi\(\['mes:pro-edhr-batch-execution:query'\]\)/,
  '填写人只有批次执行 query 动态权益时，个人工作台也必须加载本人 eDHR 工作任务。'
)

console.log('PASS: profile unified todo list static contract')
