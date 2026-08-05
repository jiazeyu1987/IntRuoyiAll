const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const unifiedListTemplatePath = path.join(root, 'src/components/UnifiedListTemplate/index.vue')
const multiFilterComponentPath = path.join(root, 'src/components/TableMultiFilter/index.vue')
const multiFilterFieldPath = path.join(root, 'src/components/TableMultiFilter/MultiFilterField.vue')
const multiFilterHookPath = path.join(root, 'src/hooks/web/useTableMultiFilter.ts')

assert.equal(fs.existsSync(unifiedListTemplatePath), true, '统一列表模板组件必须存在。')
assert.equal(fs.existsSync(multiFilterComponentPath), true, '多维度筛选组件必须存在。')
assert.equal(fs.existsSync(multiFilterFieldPath), true, '多维度筛选字段组件必须存在。')
assert.equal(fs.existsSync(multiFilterHookPath), true, '多维度筛选 hook 必须存在。')

const unifiedListTemplateSource = fs.readFileSync(unifiedListTemplatePath, 'utf8')
const multiFilterComponentSource = fs.readFileSync(multiFilterComponentPath, 'utf8')
const multiFilterFieldSource = fs.readFileSync(multiFilterFieldPath, 'utf8')
const multiFilterHookSource = fs.readFileSync(multiFilterHookPath, 'utf8')

assert.match(unifiedListTemplateSource, /import TableMultiFilter from '@\/components\/TableMultiFilter\/index\.vue'/, '标准列表模板必须导入多维度筛选组件。')
assert.match(unifiedListTemplateSource, /showMultiFilter/, '标准列表模板必须提供多维度筛选开关。')
assert.match(unifiedListTemplateSource, /multiFilterDefinitions/, '标准列表模板必须接收多维度筛选定义。')
assert.match(unifiedListTemplateSource, /multiFilterState/, '标准列表模板必须接收多维度筛选状态。')
assert.match(unifiedListTemplateSource, /<TableMultiFilter[\s\S]*:filter-definitions="multiFilterDefinitions"/, '标准列表模板必须把筛选定义传入多维度筛选组件。')
assert.match(unifiedListTemplateSource, /@update:state="\$emit\('update:multiFilterState', \$event\)"/, '标准列表模板必须透传多维度筛选状态更新。')
assert.match(unifiedListTemplateSource, /@query="\$emit\('multi-filter-query'\)"/, '标准列表模板必须透传多维度查询事件。')
assert.match(unifiedListTemplateSource, /@reset="\$emit\('multi-filter-reset'\)"/, '标准列表模板必须透传多维度重置事件。')
assert.match(unifiedListTemplateSource, /@remove="\$emit\('multi-filter-remove', \$event\)"/, '标准列表模板必须透传单项筛选清除事件。')
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
assert.doesNotMatch(multiFilterComponentSource, /localStorage|sessionStorage/, '多维度筛选组件不得使用本地存储兜底。')

assert.match(multiFilterFieldSource, /showLabel/, '多维度筛选字段组件必须支持在 Tab 条件行中隐藏固定字段标签。')
assert.match(multiFilterFieldSource, /type === 'multiSelect'/, '多维度筛选字段必须支持多选下拉。')
assert.match(multiFilterFieldSource, /type === 'dateRange'/, '多维度筛选字段必须支持日期范围。')
assert.match(multiFilterFieldSource, /type === 'numberRange'/, '多维度筛选字段必须支持数字范围。')
assert.match(multiFilterFieldSource, /type === 'autocomplete'/, '多维度筛选字段必须支持自动补全。')
assert.match(multiFilterFieldSource, /updateRangeValue/, '多维度筛选字段必须独立更新范围起止值。')

assert.match(multiFilterHookSource, /export type ListMultiFilterFieldType = 'text' \| 'select' \| 'multiSelect' \| 'dateRange' \| 'autocomplete' \| 'numberRange'/, '多维度筛选字段类型必须覆盖文本、单选、多选、日期范围、自动补全和数字范围。')
assert.match(multiFilterHookSource, /export interface ListMultiFilterDefinition/, '多维度筛选 hook 必须导出筛选定义类型。')
assert.match(multiFilterHookSource, /export interface ListMultiFilterCondition/, '多维度筛选 hook 必须导出条件类型。')
assert.match(multiFilterHookSource, /id\?:\s*string/, '多维度筛选条件必须支持稳定 Tab id。')
assert.match(multiFilterHookSource, /conditions:\s*ListMultiFilterCondition\[\]/, '多维度筛选状态必须使用条件数组。')
assert.match(multiFilterHookSource, /activeConditionId\?:\s*string/, '多维度筛选状态必须记录当前编辑的条件 Tab。')
assert.match(multiFilterHookSource, /queryParamKeys\?: \[string, string\]/, '多维度筛选定义必须支持范围字段映射到正式 query 起止参数。')
assert.match(multiFilterHookSource, /queryParams\.pageNo = 1/, '多维度筛选查询和重置必须回到第一页。')
assert.match(multiFilterHookSource, /delete queryParamTarget\.multiFilters/, '多维度筛选必须清理旧 multiFilters 参数。')
assert.match(multiFilterHookSource, /unmappedConditions/, '没有正式 queryParamKey 的条件必须进入显式 multiFilters，不得静默丢失。')
assert.match(multiFilterHookSource, /validateDuplicateMappedConditions/, '多维度筛选必须校验重复正式 query 参数，避免交集条件被覆盖。')
assert.doesNotMatch(multiFilterHookSource, /localStorage|sessionStorage/, '多维度筛选 hook 不得使用本地存储兜底。')
assert.doesNotMatch(multiFilterHookSource, /catch\s*\(/, '多维度筛选 hook 不得吞异常或降级。')

console.log('PASS: unified list template multi-filter static contract')
