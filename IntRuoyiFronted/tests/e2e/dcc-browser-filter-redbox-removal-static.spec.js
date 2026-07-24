const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/dcc/controlled-file/browser/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

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
  /label="范围"/,
  '截图红框内“范围”文字标签不得继续显示。'
)
assert.match(
  extraFilters,
  /v-model="searchScope"[\s\S]*:options="browserSearchScopeOptions"[\s\S]*@change="handleSearchScopeChange"/,
  '删除范围文字标签后必须继续保留当前目录/全域切换。'
)
assert.doesNotMatch(
  extraFilters,
  /label="状态"[\s\S]*v-model="queryParams\.status"/,
  '截图红框内“状态 / 全部状态”筛选项不得继续显示。'
)
assert.doesNotMatch(
  extraFilters,
  /BROWSER_STATUS_FILTER_OPTIONS/,
  '状态筛选选项不得继续出现在标准模板附加筛选区。'
)
assert.doesNotMatch(
  extraFilters,
  /label="类别"[\s\S]*v-model="queryParams\.categoryId"|categoryOptions/,
  '类别筛选不得继续出现在标准模板附加筛选区。'
)
assert.match(
  template,
  /<template #actions>[\s\S]*advancedActionsVisible[\s\S]*高级/,
  '删除红框筛选内容后高级按钮入口必须继续保留。'
)
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*label="文件名称"[\s\S]*label="文件编号"[\s\S]*label="操作"/,
  '删除红框筛选内容后表格核心列必须继续保留。'
)

console.log('PASS: dcc browser filter redbox removal static contract')
