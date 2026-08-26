const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const view = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')

assert.match(
  view,
  /v-if="canReviewAction\(row\)"[\s\S]*?@click="openReviewAction\(row\)"[\s\S]*?>\s*审核\s*</,
  '待办行内审核按钮必须复用统一审核入口并显示统一文案'
)
assert.match(
  view,
  /const canReviewAction = \(row: ApprovalTaskSummaryVO\) =>[\s\S]*?return canReview\(row\) \|\| canReviewInModule\(row\)/,
  '直接审核和模块审核必须合并为一个审核入口'
)
assert.match(view, /@click="openModuleDetail\(row\)"/, '查看必须保留统一业务详情入口')
assert.match(view, /@click="openTimeline\(row\)"/, '流程必须保留统一流程入口')

console.log('approval center upload quick review static contract passed')
