const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const mainListPath = path.join(
  root,
  'src/views/mes/pro/scheduleorder/components/ScheduleOrderMainList.vue'
)
const apiPath = path.join(root, 'src/api/mes/pro/scheduleorder/index.ts')

assert.equal(fs.existsSync(pagePath), true, '排产工单页面必须存在。')
assert.equal(fs.existsSync(mainListPath), true, '排产工单主列表包装组件必须存在。')
assert.equal(fs.existsSync(apiPath), true, '排产工单前端 API 类型必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')
const mainListSource = fs.readFileSync(mainListPath, 'utf8').replace(/\r\n/g, '\n')
const apiSource = fs.readFileSync(apiPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  mainListSource,
  /import type \{[\s\S]*ListMultiFilterDefinition[\s\S]*ListMultiFilterState[\s\S]*\} from '@\/hooks\/web\/useTableMultiFilter'/,
  '排产工单主列表包装组件必须接收多维筛选正式类型。'
)
assert.match(
  mainListSource,
  /<UnifiedListTemplate[\s\S]*:show-multi-filter="showMultiFilter"[\s\S]*:multi-filter-definitions="multiFilterDefinitions"[\s\S]*:multi-filter-state="multiFilterState"/,
  '排产工单主列表包装组件必须把多维筛选开关、定义和状态传给标准列表模板。'
)
assert.match(
  mainListSource,
  /:show-quick-filter="!showMultiFilter"/,
  '排产工单启用右侧条件 Tab 多维筛选时，必须关闭左侧旧 quick filter 区域。'
)
assert.match(
  mainListSource,
  /@update:multi-filter-state="emit\('update:multiFilterState', \$event\)"[\s\S]*@multi-filter-query="emit\('multiFilterQuery'\)"[\s\S]*@multi-filter-reset="emit\('multiFilterReset'\)"[\s\S]*@multi-filter-remove="emit\('multiFilterRemove', \$event\)"/,
  '排产工单主列表包装组件必须透传多维筛选查询、重置和单项移除事件。'
)
assert.doesNotMatch(mainListSource, /multiFilterMaxInlineFilters/, '条件 Tab 方案不允许排产工单包装组件保留 inline 可见数量特例。')
assert.match(mainListSource, /showMultiFilter:\s*\{ type: Boolean/, '包装组件必须显式声明 showMultiFilter。')
assert.match(mainListSource, /multiFilterDefinitions:\s*\{ type: Array as PropType<ListMultiFilterDefinition\[\]>/, '包装组件必须显式声明 multiFilterDefinitions。')
assert.match(mainListSource, /multiFilterState:\s*\{[\s\S]*type: Object as PropType<ListMultiFilterState>/, '包装组件必须显式声明 multiFilterState。')

assert.match(
  pageSource,
  /useTableMultiFilter,[\s\S]*type ListMultiFilterDefinition/,
  '排产工单页面必须导入 useTableMultiFilter 和多维筛选定义类型。'
)
assert.match(
  pageSource,
  /const scheduleOrderMultiFilterDefinitions:\s*ListMultiFilterDefinition\[\] = \[/,
  '排产工单页面必须定义主列表多维筛选字段。'
)

const multiDefinitionStart = pageSource.indexOf('const scheduleOrderMultiFilterDefinitions')
const multiDefinitionEnd = pageSource.indexOf('\n\nconst processDialogVisible', multiDefinitionStart)
assert.ok(multiDefinitionStart >= 0 && multiDefinitionEnd > multiDefinitionStart, '排产工单多维筛选定义区块必须存在。')
const multiDefinitionBlock = pageSource.slice(multiDefinitionStart, multiDefinitionEnd)

for (const [key, label, queryParamKey] of [
  ['code', '排产工单号', 'code'],
  ['erpWorkOrderCode', '来源生产工单号', 'erpWorkOrderCode'],
  ['completionFilter', '完成状态', 'completionFilter'],
  ['promiseDate', '承诺交期', 'promiseDate']
]) {
  assert.match(
    multiDefinitionBlock,
    new RegExp(`key:\\s*'${key}'[\\s\\S]*label:\\s*'${label}'[\\s\\S]*queryParamKey:\\s*'${queryParamKey}'`),
    `排产工单多维筛选字段 ${key} 必须映射到正式 query 参数 ${queryParamKey}。`
  )
}

assert.doesNotMatch(
  multiDefinitionBlock,
  /productName|productSpecification|multiFilters/,
  '排产工单多维筛选 pilot 不得把缺少正式分页 query 参数的产品字段或临时 multiFilters 放入主链路。'
)
assert.match(
  pageSource,
  /const scheduleOrderMultiFilter = useTableMultiFilter\(\s*'mes\.pro\.scheduleOrder\.main',\s*scheduleOrderMultiFilterDefinitions,\s*scheduleOrderQueryParams,\s*getScheduleOrderList\s*\)/,
  '排产工单页面必须使用正式主列表 tableKey 创建多维筛选状态。'
)
assert.match(
  pageSource,
  /<ScheduleOrderMainList[\s\S]*:show-multi-filter="true"[\s\S]*:multi-filter-definitions="scheduleOrderMultiFilterDefinitions"[\s\S]*:multi-filter-state="scheduleOrderMultiFilter\.state"/,
  '排产工单主列表必须开启多维筛选并绑定状态。'
)
assert.doesNotMatch(
  pageSource,
  /<ScheduleOrderMainList[\s\S]{0,800}:show-quick-filter="true"/,
  '排产工单主列表启用多维筛选后不得显式打开旧 quick filter。'
)
assert.match(
  pageSource,
  /@update:multi-filter-state="scheduleOrderMultiFilter\.updateState"[\s\S]*@multi-filter-query="scheduleOrderMultiFilter\.applyMultiFilter"[\s\S]*@multi-filter-reset="scheduleOrderMultiFilter\.resetMultiFilter"[\s\S]*@multi-filter-remove="scheduleOrderMultiFilter\.removeCondition"/,
  '排产工单页面必须绑定多维筛选状态更新、查询、重置和移除事件。'
)
assert.doesNotMatch(pageSource, /multi-filter-max-inline-filters/, '条件 Tab 方案不允许排产工单页面保留 inline 可见数量特例。')
assert.match(
  pageSource,
  /scheduleOrderMultiFilter\.setCondition\(\{[\s\S]*id:\s*'completionFilter'[\s\S]*key:\s*'completionFilter'[\s\S]*value:\s*scheduleOrderQueryParams\.completionFilter/,
  '排产工单多维筛选必须把页面默认完成状态筛选显式展示为稳定条件 Tab。'
)
assert.doesNotMatch(
  pageSource,
  /scheduleOrderQueryParams[\s\S]{0,220}multiFilters/,
  '排产工单查询参数不得新增后端未声明的 multiFilters 字段。'
)
assert.doesNotMatch(pageSource, /localStorage|sessionStorage/, '排产工单多维筛选不得使用本地存储兜底。')

for (const token of ['code?: string', 'erpWorkOrderCode?: string', "completionFilter?: 'INCOMPLETE' | 'ALL' | 'COMPLETED'", 'promiseDate?: string[]']) {
  assert.ok(apiSource.includes(token), `排产工单 API 请求类型必须保留正式字段：${token}`)
}
assert.doesNotMatch(apiSource, /multiFilters/, '排产工单 API 请求类型不得声明临时 multiFilters 参数。')

console.log('PASS: schedule order main multi-filter static contract')
