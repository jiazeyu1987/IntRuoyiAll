const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const pendingNameStyleStart = detail.indexOf('.edhr-batch-detail__pending-task-name {')
assert.notEqual(pendingNameStyleStart, -1, '待处理工序标题必须有独立样式，避免被复盘标题换行样式误覆盖。')
const pendingNameStyleEnd = detail.indexOf('}', pendingNameStyleStart)
assert.notEqual(pendingNameStyleEnd, -1, '待处理工序标题样式必须正确闭合。')
const pendingNameStyleBlock = detail.slice(pendingNameStyleStart, pendingNameStyleEnd)

for (const requiredStyle of ['overflow: hidden', 'text-overflow: ellipsis', 'white-space: nowrap']) {
  assert.ok(pendingNameStyleBlock.includes(requiredStyle), `待处理工序标题必须单行省略：${requiredStyle}`)
}
for (const forbiddenStyle of ['white-space: normal', 'overflow-wrap: anywhere', 'overflow: visible', 'text-overflow: clip']) {
  assert.ok(!pendingNameStyleBlock.includes(forbiddenStyle), `待处理工序标题不得换行或显示溢出：${forbiddenStyle}`)
}

const combinedStyleStart = detail.indexOf('.edhr-batch-detail__pending-task-name,')
if (combinedStyleStart !== -1) {
  const combinedStyleEnd = detail.indexOf('}', combinedStyleStart)
  const combinedStyleBlock = detail.slice(combinedStyleStart, combinedStyleEnd)
  assert.ok(!combinedStyleBlock.includes('white-space: normal'), '待处理工序标题不得与复盘工序标题共享换行样式。')
}

console.log('PASS: EDHR pending task name stays single-line with ellipsis.')
