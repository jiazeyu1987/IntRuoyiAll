const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/dcc/controlled-file/browser/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

const mainListEnd = source.indexOf('<el-dialog')
assert.ok(mainListEnd > 0, '文件查阅页面必须保留后续弹窗结构。')
const mainListSource = source.slice(0, mainListEnd)

assert.doesNotMatch(
  mainListSource,
  /class="browser-list-header"/,
  '文件查阅页顶部标题/说明/工作台入口区域不得继续显示。'
)
assert.doesNotMatch(
  mainListSource,
  /browser-list-title|browser-list-subtitle|browserListTitle|browserListSubtitle/,
  '文件查阅页不得保留顶部标题和说明的渲染或计算属性。'
)
assert.doesNotMatch(
  mainListSource,
  /<ControlledFileWorkbenchEntry\s*\/>/,
  '文件查阅页右上角 DCC 工作台入口不得继续显示在红框区域。'
)
assert.doesNotMatch(
  source,
  /import ControlledFileWorkbenchEntry from/,
  '文件查阅页移除工作台入口后不得保留无用导入。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.controlledFile\.browser\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '文件查阅列表必须继续使用标准列表模板。')
const template = templateMatch[0]

const extraFiltersMatch = template.match(/<template #extra-filters>([\s\S]*?)<\/template>/)
assert.ok(extraFiltersMatch, '文件查阅列表必须保留标准模板附加筛选区。')
const extraFilters = extraFiltersMatch[1]

assert.doesNotMatch(
  extraFilters,
  /label="搜索"[\s\S]*v-model="queryParams\.keyword"/,
  '文件查阅页中间重复“搜索”标签和搜索输入框不得继续显示。'
)
assert.doesNotMatch(
  extraFilters,
  /class="browser-search-input"/,
  '文件查阅页不得保留中间重复搜索输入框样式。'
)
assert.match(
  extraFilters,
  /v-model="searchScope"[\s\S]*:options="browserSearchScopeOptions"/,
  '当前目录/全域切换必须继续保留。'
)
assert.doesNotMatch(
  extraFilters,
  /label="范围"|label="状态"[\s\S]*v-model="queryParams\.status"|label="类别"[\s\S]*v-model="queryParams\.categoryId"/,
  '截图红框内范围文字标签、状态筛选和类别筛选不得继续显示。'
)
assert.match(
  template,
  /:filter-definitions="dccBrowserQuickFilterDefinitions"[\s\S]*@quick-filter-query="dccBrowserQuickFilter\.applyQuickFilter"/,
  '快速过滤必须继续由标准列表模板承载。'
)
assert.match(
  template,
  /<template #actions>[\s\S]*advancedActionsVisible[\s\S]*高级/,
  '高级按钮入口必须继续保留。'
)
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*data-user-table-key="dcc\.controlledFile\.browser\.main"[\s\S]*label="文件名称"[\s\S]*label="文件编号"[\s\S]*label="操作"/,
  '文件表格核心列必须继续保留。'
)

console.log('PASS: dcc browser hide red box static contract')
