const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const listPagePath = path.resolve(
  root,
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)
const source = fs.readFileSync(listPagePath, 'utf8')

const actionsSlotMatch = source.match(/<template\s+#actions>([\s\S]*?)<\/template>/)
assert.ok(actionsSlotMatch, '批次执行列表必须保留顶部 actions 插槽。')

const actionsSlot = actionsSlotMatch[1]
assert.equal(
  /<el-button[\s\S]*?>\s*重置\s*<\/el-button>/.test(actionsSlot),
  false,
  '批次执行列表顶部操作区不得继续渲染重置按钮。'
)
assert.equal(
  /<el-button[\s\S]*?>\s*演练预检\s*<\/el-button>/.test(actionsSlot),
  false,
  '批次执行列表顶部操作区不得继续渲染演练预检按钮。'
)
