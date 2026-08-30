const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const source = fs.readFileSync(path.join(repoRoot, 'src/views/bpm/model/index.vue'), 'utf8')

const actionStart = source.indexOf('<template #actions>')
const actionEnd = source.search(/<template\s+#table\b[^>]*>/)
assert.ok(actionStart > -1 && actionEnd > actionStart, '流程模型页必须保留动作工具栏插槽。')
const actionSlot = source.slice(actionStart, actionEnd)

assert.doesNotMatch(source, />\s*Word 打印模板\s*</, '流程模型页标题旁不应显示 Word 打印模板标签。')
assert.doesNotMatch(actionSlot, />\s*重置\s*</, '流程模型页工具栏不应显示重置按钮。')
assert.doesNotMatch(actionSlot, />\s*排序\s*</, '流程模型页工具栏不应显示排序按钮。')
assert.doesNotMatch(actionSlot, /<el-dropdown[\s\S]*handleCategoryAdd[\s\S]*openCategoryManager/, '流程模型页工具栏不应显示新建模型旁设置下拉入口。')
assert.match(source, /:show-column-settings="false"/, '流程模型页应关闭标准列表模板右侧显示字段入口。')
assert.doesNotMatch(source, /:show-column-reset="true"/, '流程模型页不应启用标准列表模板重置列按钮。')

assert.match(actionSlot, /openCreateApprovalParticipantConfig/, '流程模型页仍应保留新建模型按钮。')
assert.match(source, /@quick-filter-query="handleQuery"/, '流程模型页仍应保留快速过滤查询。')
assert.match(source, /<el-table[\s\S]*:data="pagedModelList"/, '流程模型页仍应渲染模型列表。')

console.log('PASS: BPM model red-box controls hidden static contract')
