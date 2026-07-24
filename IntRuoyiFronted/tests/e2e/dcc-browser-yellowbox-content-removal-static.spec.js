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

assert.match(
  template,
  /:show-column-reset="false"/,
  '截图黄框内“重置列”入口必须通过标准模板配置隐藏。'
)

const extraFiltersMatch = template.match(/<template #extra-filters>([\s\S]*?)<\/template>/)
assert.ok(extraFiltersMatch, '文件查阅列表必须保留标准模板附加筛选区。')
const extraFilters = extraFiltersMatch[1]

assert.doesNotMatch(
  extraFilters,
  /label="类别"[\s\S]*v-model="queryParams\.categoryId"/,
  '截图黄框内“类别 / 全部类别”筛选项不得继续显示。'
)
assert.doesNotMatch(
  extraFilters,
  /categoryOptions/,
  '类别筛选选项不得继续出现在标准模板附加筛选区。'
)
assert.match(
  extraFilters,
  /v-model="searchScope"[\s\S]*:options="browserSearchScopeOptions"[\s\S]*@change="handleSearchScopeChange"/,
  '删除类别筛选后必须继续保留当前目录/全域切换。'
)

const actionsStart = template.indexOf('<template #actions>')
const tableStart = template.search(/<template\s+#table\b[^>]*>/)
assert.notEqual(actionsStart, -1, '文件查阅列表必须保留标准模板操作区。')
assert.ok(tableStart > actionsStart, '文件查阅列表操作区必须位于表格插槽之前。')
const actions = template.slice(actionsStart, tableStart)
const advancedMatch = actions.match(/<div class="browser-advanced-actions">([\s\S]*?)<\/div>/)
assert.ok(advancedMatch, '高级弹框内必须继续包含按钮网格。')
const advancedActions = advancedMatch[1]

for (const removed of ['查询', '重置', '刷新']) {
  assert.doesNotMatch(advancedActions, new RegExp(`>\\s*${removed}\\s*<`), `高级弹框黄框内按钮不得继续显示：${removed}`)
}
assert.doesNotMatch(
  advancedActions,
  /handleAdvancedAction\(handleQuery\)|handleAdvancedAction\(resetQuery\)|handleAdvancedAction\(refreshList\)/,
  '高级弹框不得继续保留查询、重置、刷新处理入口。'
)

for (const required of ['导出名编', '导入名编', '导出记录', '导出迁移', '导入迁移', '批量识别']) {
  assert.match(advancedActions, new RegExp(required), `高级弹框必须继续保留业务按钮：${required}`)
}

const buttonMatches = [...advancedActions.matchAll(/<el-button[\s\S]*?>([\s\S]*?)<\/el-button>/g)]
assert.equal(buttonMatches.length, 6, '删除黄框内容后高级弹框必须只保留 6 个业务按钮。')
assert.match(template, /<template #actions>[\s\S]*advancedActionsVisible[\s\S]*高级/, '高级按钮入口必须继续保留。')
assert.match(template, /<template\s+#table\b[^>]*>[\s\S]*label="文件名称"[\s\S]*label="文件编号"[\s\S]*label="操作"/, '表格核心列必须继续保留。')

console.log('PASS: dcc browser yellowbox content removal static contract')
