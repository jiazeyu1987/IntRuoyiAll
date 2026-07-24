const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')

const preProcessStart = detail.indexOf(
  '<template v-for="task in preProcessSpecialTaskEntries"'
)
const normalProcessStart = detail.indexOf('v-for="processGroup in processTaskGroups"')
assert.notEqual(preProcessStart, -1, '来料检报告必须直接按任务渲染。')
assert.notEqual(normalProcessStart, -1, '来料检报告必须位于普通工序之前。')

const preProcessTemplate = detail.slice(preProcessStart, normalProcessStart)
assert(
  preProcessTemplate.includes(
    'class="edhr-batch-detail__process-task-group edhr-batch-detail__special-process-task-group"'
  ),
  '来料检报告必须直接复用普通工序卡片容器。'
)
assert(
  preProcessTemplate.includes('class="edhr-batch-detail__process-task-group-head"'),
  '来料检报告必须直接复用普通工序卡片按钮。'
)
assert(
  !preProcessTemplate.includes('edhr-batch-detail__pending-task-list') &&
    !preProcessTemplate.includes('edhr-batch-detail__pending-task-item'),
  '来料检报告不得继续使用独立的前置节点卡片结构。'
)

const postProcessStart = detail.indexOf(
  '<template v-for="task in postProcessSpecialTaskEntries"'
)
const releaseProcessStart = detail.indexOf(
  'class="edhr-batch-detail__review-item edhr-batch-detail__release-process-item"'
)
assert.notEqual(postProcessStart, -1, '收尾特殊节点必须直接按任务渲染。')
assert.notEqual(releaseProcessStart, -1, '收尾特殊节点必须位于放行节点之前。')

const postProcessTemplate = detail.slice(postProcessStart, releaseProcessStart)
assert(
  postProcessTemplate.includes(
    'class="edhr-batch-detail__process-task-group edhr-batch-detail__special-process-task-group"'
  ),
  '灭菌报告和成品检节点必须直接复用普通工序卡片容器。'
)
assert(
  postProcessTemplate.includes('class="edhr-batch-detail__process-task-group-head"'),
  '灭菌报告和成品检节点必须直接复用普通工序卡片按钮。'
)
assert(
  !postProcessTemplate.includes('edhr-batch-detail__pending-task-list') &&
    !postProcessTemplate.includes('edhr-batch-detail__pending-task-item'),
  '收尾特殊节点不得继续放在独立分组列表中。'
)

const readStyleBlock = (selector) => {
  const start = detail.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = detail.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return detail.slice(start, end)
}

const pendingListStyle = readStyleBlock('.edhr-batch-detail__pending-task-list')
assert(pendingListStyle.includes('gap: 6px'), '特殊节点之间必须保持与普通工序一致的 6px 间距。')
assert(
  pendingListStyle.includes('margin-bottom: 0'),
  '特殊节点分组不得叠加额外下边距，避免与相邻卡片间距不一致。'
)

const pendingItemStyle = readStyleBlock('.edhr-batch-detail__pending-task-item')
for (const declaration of [
  'flex: 0 0 var(--edhr-process-item-height)',
  'width: 100%',
  'height: var(--edhr-process-item-height)',
  'border: 1px solid #dbe3ef',
  'border-radius: 6px',
  'background: #f7f9fc',
  'padding: 7px 8px',
  'overflow: hidden'
]) {
  assert(
    pendingItemStyle.includes(declaration),
    `特殊节点必须复用普通工序卡片的基础视觉：${declaration}`
  )
}

const pendingMainStyle = readStyleBlock('.edhr-batch-detail__pending-task-main')
assert(
  pendingMainStyle.includes('font-weight: 600'),
  '特殊节点名称字重必须与普通工序名称一致。'
)

const reviewItemStyle = readStyleBlock('.edhr-batch-detail__review-item')
assert(
  reviewItemStyle.includes('background: #f7f9fc'),
  '放行节点基础背景必须与普通未开始工序一致。'
)

const reviewItemActiveStyle = readStyleBlock('.edhr-batch-detail__review-item.is-active')
assert(
  reviewItemActiveStyle.includes('box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12)'),
  '放行节点选中态必须使用与普通工序一致的蓝色外框。'
)
assert(
  !detail.includes('.edhr-batch-detail__release-process-item {') &&
    !detail.includes('.edhr-batch-detail__release-process-item.is-active {'),
  '放行节点不得继续维护虚线边框、渐变背景或独立选中态样式。'
)

console.log('PASS: eDHR special process items match regular process card styling')
