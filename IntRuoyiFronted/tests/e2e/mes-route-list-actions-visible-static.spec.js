const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(source, /const ROUTE_LIST_TABLE_KEY = 'mes\.pro\.route\.main[^']*'/, '工艺路线列表必须使用稳定表格 key。')
const templateMatch = source.match(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/)
assert.ok(templateMatch, '工艺路线列表必须继续使用标准列表模板。')
const template = templateMatch[0]

const actionsLabelIndex = template.indexOf('label="操作"')
assert.notEqual(actionsLabelIndex, -1, '工艺路线列表必须渲染操作列。')
const actionsColumnStart = template.lastIndexOf('<el-table-column', actionsLabelIndex)
const actionsColumnEnd = template.indexOf('</el-table-column>', actionsLabelIndex)
assert.notEqual(actionsColumnStart, -1, '操作列必须是 el-table-column。')
assert.notEqual(actionsColumnEnd, -1, '操作列必须有完整闭合标签。')
const actionsColumn = template.slice(actionsColumnStart, actionsColumnEnd + '</el-table-column>'.length)

assert.doesNotMatch(
  actionsColumn,
  /v-if="isRouteColumnVisible\('actions'\)"/,
  '操作列不能再受显示字段配置控制，避免历史列配置把操作面板隐藏。'
)
assert.match(actionsColumn, /fixed="right"/, '操作列必须固定在右侧。')
assert.match(
  source,
  /key:\s*'actions'[\s\S]*width:\s*220[\s\S]*hideable:\s*false[\s\S]*business:\s*false/,
  '操作列默认宽度应保持紧凑且不可隐藏。'
)

for (const actionText of ['产品', '编辑', '复制', '版本', '删除']) {
  assert.match(actionsColumn, new RegExp(`>\\s*${actionText}\\s*<`), `操作列必须保留「${actionText}」操作。`)
}

console.log('PASS: mes route list actions column remains visible static contract')
