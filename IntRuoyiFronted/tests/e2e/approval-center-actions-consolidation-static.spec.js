const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')
const actionColumnStart = page.indexOf('<el-table-column\n                v-if="isApprovalColumnVisible(\'actions\')"')
const actionColumnEnd = page.indexOf('</el-table-column>', actionColumnStart)

assert.notEqual(actionColumnStart, -1, '审批中心必须保留操作列')
assert.notEqual(actionColumnEnd, -1, '审批中心操作列必须完整闭合')
const actionColumn = page.slice(actionColumnStart, actionColumnEnd)

for (const label of ['查看', '审核', '流程']) {
  assert.equal(
    (actionColumn.match(new RegExp(`>\\s*${label}\\s*<\\/el-button>`, 'g')) || []).length,
    1,
    `操作列必须只保留一个${label}按钮`
  )
}

for (const removedLabel of ['处理', '打开', '详情', '轨迹']) {
  assert.doesNotMatch(actionColumn, new RegExp(`>\\s*${removedLabel}\\s*<\\/el-button>`), `操作列不得继续显示${removedLabel}`)
}

assert.match(actionColumn, /@click="openModuleDetail\(row\)"[\s\S]*>\s*查看\s*</, '查看必须复用统一业务详情入口')
assert.match(actionColumn, /@click="openReviewAction\(row\)"[\s\S]*>\s*审核\s*</, '审核必须复用统一审核入口')
assert.match(actionColumn, /@click="openTimeline\(row\)"[\s\S]*>\s*流程\s*</, '流程必须复用统一流程入口')
for (const action of ['view', 'review', 'flow']) {
  assert.match(actionColumn, new RegExp(`data-approval-action="${action}"`), `${action} 必须提供稳定操作锚点`)
}

assert.match(page, /const canOpenView = \(row: ApprovalTaskSummaryVO\)/, '查看必须有独立可用性判断')
assert.match(page, /const canReviewAction = \(row: ApprovalTaskSummaryVO\)/, '审核必须合并直接审核和模块审核能力')
assert.match(page, /const canOpenFlow = \(row: ApprovalTaskSummaryVO\)/, '流程必须统一流程图和轨迹能力判断')
assert.match(page, /isBpmProcessDetailOnly\(row\)[\s\S]*?openProcessFlow\(row\)/, 'BPM 流程入口必须保留正式流程详情跳转')

console.log('approval center actions consolidation static contract passed')
