const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const dialogStart = source.indexOf('title="待同步差异"')
assert.notEqual(dialogStart, -1, '待同步差异弹窗必须存在。')
const dialogEnd = source.indexOf('</Dialog>', dialogStart)
assert.notEqual(dialogEnd, -1, '待同步差异弹窗必须正确闭合。')
const dialog = source.slice(dialogStart, dialogEnd)

const extraFiltersStart = dialog.indexOf('<template #extra-filters>')
if (extraFiltersStart !== -1) {
  const extraFiltersEnd = dialog.indexOf('</template>', extraFiltersStart)
  assert.notEqual(extraFiltersEnd, -1, '额外筛选插槽存在时必须正确闭合。')
  const extraFilters = dialog.slice(extraFiltersStart, extraFiltersEnd)
  for (const label of ['工单编码', '产品编号', '入池状态', '阻断原因']) {
    assert.doesNotMatch(extraFilters, new RegExp(`label="${label}"`), `紫框额外筛选项不得显示：${label}`)
  }
}

const actionsStart = dialog.indexOf('<template #actions>')
assert.notEqual(actionsStart, -1, '待同步差异动作区必须存在。')
const actionsEnd = dialog.indexOf('</template>', actionsStart)
assert.notEqual(actionsEnd, -1, '待同步差异动作区必须正确闭合。')
const actions = dialog.slice(actionsStart, actionsEnd)

assert.doesNotMatch(actions, />\s*搜索\s*</, '紫框内独立搜索按钮不得显示。')
assert.match(actions, /重置/, '动作区必须保留重置按钮。')
assert.match(actions, /选中工单加入排产工单池/, '动作区必须保留加入排产工单池按钮。')
assert.match(dialog, /:filter-definitions="workOrderAdmissionQuickFilterDefinitions"/, '必须保留标准列表模板快速过滤配置。')
assert.match(dialog, /<template\s+#table\b[^>]*>[\s\S]*data-user-table-key="mes\.pro\.scheduleOrder\.admissionDiff"/, '必须保留待同步差异表格。')

console.log('PASS: MES schedule order admission purple controls are hidden')
