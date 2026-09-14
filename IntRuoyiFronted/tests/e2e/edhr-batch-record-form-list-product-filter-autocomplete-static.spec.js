const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/batchrecordreport/index.ts')
const hookPath = path.join(repoRoot, 'src/hooks/web/useTableQuickFilter.ts')
const quickFilterPath = path.join(repoRoot, 'src/components/TableQuickFilter/index.vue')

const page = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
const hook = fs.readFileSync(hookPath, 'utf8')
const quickFilter = fs.readFileSync(quickFilterPath, 'utf8')

assert.match(
  api,
  /getProductNameOptions:\s*async\s*\(\s*keyword\?:\s*string,\s*latestVersionOnly\?:\s*boolean\s*\)[\s\S]*?\/mes\/pro\/batch-record-report\/product-name-options[\s\S]*?params:\s*\{\s*keyword,\s*latestVersionOnly\s*\}/,
  '批记录表单 API 必须暴露当前表单目录产品名称候选接口。'
)

assert.match(
  hook,
  /triggerOnFocus\?:\s*boolean/,
  '快速过滤定义必须支持单字段控制 autocomplete 聚焦即展示候选。'
)

assert.match(
  quickFilter,
  /<el-autocomplete[\s\S]*:trigger-on-focus="selectedDefinition\.triggerOnFocus === true"[\s\S]*@select="handleAutocompleteSelect"/,
  'TableQuickFilter 的 autocomplete 必须默认不聚焦触发，仅由字段定义显式开启，并保留选择后查询。'
)

assert.match(
  quickFilter,
  /<el-autocomplete[\s\S]*:popper-class="selectedDefinition\.popperClass \|\| 'table-quick-filter-autocomplete-popper'"/,
  '产品名称 autocomplete 候选下拉必须使用专用 popper 样式，避免候选文本被默认宽度截断。'
)

assert.match(
  quickFilter,
  /\.table-quick-filter__field\s*\{[\s\S]*?flex:\s*0 0 120px;[\s\S]*?min-width:\s*120px;[\s\S]*?width:\s*120px;[\s\S]*?\}/,
  '快速过滤字段选择框必须固定足够宽度，不能把“产品名称”挤成省略号。'
)

assert.match(
  quickFilter,
  /\.table-quick-filter__operator\s*\{[\s\S]*?flex:\s*0 0 92px;[\s\S]*?min-width:\s*92px;[\s\S]*?width:\s*92px;[\s\S]*?\}/,
  '快速过滤条件选择框必须固定足够宽度，不能把“包含”挤成省略号。'
)

assert.match(
  quickFilter,
  /\.table-quick-filter__value\s*\{[\s\S]*?flex:\s*0 0 clamp\(280px,\s*32vw,\s*420px\);[\s\S]*?min-width:\s*280px;[\s\S]*?width:\s*clamp\(280px,\s*32vw,\s*420px\);[\s\S]*?\}/,
  '产品名称输入区必须有更宽且不收缩的宽度，保证常见产品名称完整显示。'
)

assert.match(
  quickFilter,
  /\.table-quick-filter-autocomplete-popper[\s\S]*?min-width:\s*320px;[\s\S]*?\.table-quick-filter-autocomplete-popper \.el-autocomplete-suggestion__list li[\s\S]*?white-space:\s*normal;[\s\S]*?overflow-wrap:\s*anywhere;/,
  '产品名称候选下拉项必须可换行显示完整名称，不能只靠省略号或 tooltip。'
)

assert.match(
  page,
  /const queryRecordFormProductNameSuggestions = async\s*\([\s\S]*?BatchRecordReportApi\.getProductNameOptions\([\s\S]*?queryParams\.latestVersionOnly[\s\S]*?callback\(\(data \|\| \[\]\)\.map\(\(productName\) => \(\{ value: productName \}\)\)\)/,
  '批记录表单页必须从当前批记录表单目录接口加载产品名称候选。'
)

assert.match(
  page,
  /\{\s*key:\s*'productName'[\s\S]*?label:\s*'产品名称'[\s\S]*?type:\s*'autocomplete'[\s\S]*?queryParamKey:\s*'productName'[\s\S]*?placeholder:\s*'请输入产品名称'[\s\S]*?triggerOnFocus:\s*true[\s\S]*?fetchSuggestions:\s*queryRecordFormProductNameSuggestions[\s\S]*?\}/,
  '批记录表单产品名称快速过滤必须是点击可出候选的 autocomplete，并仍写入 productName 查询参数。'
)

assert.match(
  quickFilter,
  /const handleAutocompleteSelect = \(item: TableQuickFilterSuggestion\) => \{\s*emitState\(\{ value: item\.value \}\)\s*emit\('query'\)\s*\}/,
  '选择产品名称候选必须立即触发查询，无需点击查询按钮。'
)

assert.match(
  quickFilter,
  /<el-button type="primary" @click="onQuery">[\s\S]*?查询[\s\S]*?<\/el-button>/,
  '快速过滤必须保留查询按钮，用于手动输入或复制产品名称后的过滤。'
)

assert.doesNotMatch(
  page,
  /getProjectCodePage\([\s\S]*queryRecordFormProductNameSuggestions|MdItemApi\.getItemPage\([\s\S]*queryRecordFormProductNameSuggestions/,
  '批记录表单产品名称筛选候选不得来自 DCC 项目代码或 MES 物料主数据。'
)

console.log('PASS: batch record form list product filter autocomplete static contract')
