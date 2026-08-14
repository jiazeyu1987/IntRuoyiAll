const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '..', '..')

const read = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const source = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const multiFilterHook = read('src/hooks/web/useTableMultiFilter.ts')
const multiFilterField = read('src/components/TableMultiFilter/MultiFilterField.vue')
const quickFilterHook = read('src/hooks/web/useTableQuickFilter.ts')
const quickFilterField = read('src/components/TableQuickFilter/index.vue')
const unifiedListTemplate = read('src/components/UnifiedListTemplate/index.vue')

const reportMarker = 'data-team-leader-report-workbench'
const reportStart = source.indexOf(reportMarker)
assert.ok(reportStart >= 0, 'PQC/生产组长提交列表区域必须保留稳定 data-team-leader-report-workbench 标记。')
const reportEnd = source.indexOf('<ContentWrap', reportStart + reportMarker.length)
assert.ok(reportEnd > reportStart, '必须能定位提交列表 ContentWrap 的结束边界。')
const reportBlock = source.slice(reportStart, reportEnd)

assert.match(
  reportBlock,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.processPool\.teamLeader\.submissions"[\s\S]*<\/UnifiedListTemplate>/,
  'PQC 管理提交列表必须改用标准 UnifiedListTemplate，并使用稳定 table-key。'
)
assert.doesNotMatch(
  reportBlock,
  /<el-form[\s\S]*ref="queryFormRef"/,
  'PQC 管理提交列表区域不得保留旧手写查询 el-form。'
)
assert.doesNotMatch(
  reportBlock,
  /<Pagination[\s\S]*getSubmissionList/,
  'PQC 管理提交列表区域不得保留旧手写分页，分页必须由 UnifiedListTemplate 承载。'
)
assert.match(
  reportBlock,
  /:show-multi-filter="true"[\s\S]*:multi-filter-definitions="submissionMultiFilterDefinitions"[\s\S]*:multi-filter-state="submissionMultiFilterState"/,
  'PQC 管理列表必须启用标准多条件筛选并绑定正式 definitions/state。'
)
assert.match(
  reportBlock,
  /@update:multi-filter-state="updateSubmissionMultiFilterState"[\s\S]*@multi-filter-query="applySubmissionMultiFilter"[\s\S]*@multi-filter-reset="resetSubmissionMultiFilter"[\s\S]*@multi-filter-remove="removeSubmissionMultiFilterCondition"/,
  'PQC 管理列表必须通过标准多条件事件更新、查询和重置。'
)
assert.match(
  reportBlock,
  /data-user-table-column-explicit[\s\S]*data-user-table-key="mes\.processPool\.teamLeader\.submissions"/,
  'PQC 管理表格必须接入标准列配置标记。'
)
assert.match(
  reportBlock,
  /<UnifiedListTemplate[\s\S]*single-line-toolbar/,
  'PQC 管理列表必须显式启用标准模板单行工具栏布局。'
)

assert.match(
  unifiedListTemplate,
  /singleLineToolbar\?:\s*boolean/,
  '标准列表模板必须提供可复用的单行工具栏开关。'
)
assert.match(
  unifiedListTemplate,
  /'unified-list-template--single-line-toolbar':\s*singleLineToolbar/,
  '标准列表模板必须通过稳定修饰类启用单行布局。'
)
assert.match(
  unifiedListTemplate,
  /@media \(min-width:\s*1181px\)[\s\S]*\.unified-list-template--single-line-toolbar \.unified-list-template__query-form\s*\{[\s\S]*display:\s*grid;[\s\S]*grid-template-columns:\s*minmax\(720px,\s*1fr\)\s+auto;/,
  '桌面端单行模式必须使用左侧最小 720px 弹性筛选列和右侧自动宽度工具列。'
)
assert.match(
  unifiedListTemplate,
  /\.unified-list-template--single-line-toolbar \.unified-list-template__multi-filter\s*\{[\s\S]*grid-column:\s*1;[\s\S]*grid-row:\s*1;/,
  '单行模式必须把多条件筛选固定在左侧第一列。'
)
assert.match(
  unifiedListTemplate,
  /\.unified-list-template--single-line-toolbar \.unified-list-template__toolbar-actions\s*\{[\s\S]*grid-column:\s*2;[\s\S]*grid-row:\s*1;[\s\S]*margin-left:\s*0;/,
  '单行模式必须把显示字段工具区固定在右侧第二列。'
)

assert.match(
  source,
  /useTableMultiFilter\([\s\S]*'mes\.processPool\.teamLeader\.submissions'[\s\S]*submissionMultiFilterDefinitions[\s\S]*queryParams[\s\S]*getSubmissionList/,
  'PQC 管理列表必须使用 useTableMultiFilter 直接写入正式查询参数。'
)
assert.match(
  source,
  /const submissionMultiFilterDefinitions = computed<[^>]*ListMultiFilterDefinition\[\][\s\S]*key:\s*'submitDate'[\s\S]*label:\s*'提交日期'[\s\S]*type:\s*'date'[\s\S]*queryParamKey:\s*'submitDate'/,
  '提交日期必须是标准多条件的正式单日期字段。'
)

for (const [key, label, queryParamKey] of [
  ['employeeUserId', 'PQC检验员', 'employeeUserId'],
  ['processId', '工序', 'processId'],
  ['templateType', '模板类型', 'templateType'],
  ['workOrderCode', '生产工单', 'workOrderCode'],
  ['productKeyword', '产品', 'productKeyword'],
  ['inspectionType', '检验类型', 'inspectionType'],
  ['roundNo', '轮次', 'roundNo'],
  ['submissionReviewStatus', '复核状态', 'submissionReviewStatus']
]) {
  assert.match(
    source,
    new RegExp(`key:\\s*'${key}'[\\s\\S]*label:\\s*'${label}'[\\s\\S]*queryParamKey:\\s*'${queryParamKey}'`),
    `标准多条件筛选必须声明 ${label} -> ${queryParamKey} 正式参数映射。`
  )
}

assert.match(
  source,
  /const applySubmissionMultiFilter = async \(\) => \{[\s\S]*await applySubmissionMultiFilterState\(\)[\s\S]*\}/,
  'PQC 管理查询必须直接调用标准多条件 apply，只有显式日期条件才写入 submitDate。'
)
assert.doesNotMatch(
  source,
  /hasSubmissionDateCondition|提交日期是必填筛选条件/,
  'PQC 管理默认筛选为空时不得要求用户先添加可见提交日期条件。'
)
assert.match(
  source,
  /const resetSubmissionMultiFilter = async \(\) => \{[\s\S]*clearSubmissionVisibleFilterState\(\)[\s\S]*clearSubmissionFilterParams\(\)[\s\S]*resetSubmissionQueryParams\(leaderType\)[\s\S]*submissionList\.value = \[\][\s\S]*submissionTotal\.value = 0[\s\S]*await getSubmissionList\(\)/,
  'PQC 管理重置必须保持可见筛选和正式日期参数都为空后重新查询。'
)
assert.doesNotMatch(
  source,
  /submissionMultiFilter\.setCondition|syncSubmissionDefaultConditions/,
  '标准多条件搜索不得通过隐藏 setCondition 预置日期或模板筛选。'
)
assert.match(
  source,
  /submitDate:\s*getInitialSubmissionDate\(activeLeaderTab\.value\)[\s\S]*templateType:\s*undefined/,
  'PQC 管理首屏 query 参数必须按组长类型初始化，PQC 不得预置隐藏日期或模板类型。'
)

assert.match(
  multiFilterHook,
  /export type ListMultiFilterFieldType = 'text' \| 'select' \| 'multiSelect' \| 'date' \| 'dateRange' \| 'autocomplete' \| 'numberRange'/,
  '标准多条件 hook 必须支持正式单日期字段类型。'
)
assert.match(
  multiFilterField,
  /definition\.type === 'date'[\s\S]*type="date"[\s\S]*value-format="YYYY-MM-DD"/,
  '标准多条件字段组件必须为单日期字段渲染 Element Plus date picker。'
)
assert.match(
  quickFilterHook,
  /export type TableQuickFilterFieldType = 'text' \| 'select' \| 'date' \| 'dateRange' \| 'autocomplete'/,
  '标准 quick filter hook 必须同步支持单日期字段。'
)
assert.match(
  quickFilterField,
  /selectedDefinition\?\.type === 'date'[\s\S]*type="date"[\s\S]*value-format="YYYY-MM-DD"/,
  '标准 quick filter 组件必须同步渲染单日期控件。'
)

console.log('PASS: PQC leader standard list template static contract')
