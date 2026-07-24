const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const processContextStart = detail.indexOf('<div class="edhr-batch-detail__process-context" aria-label="当前批记录上下文">')
const processContextEnd = detail.indexOf('</div>', processContextStart)
assert.notEqual(processContextStart, -1, '工序栏顶部必须保留当前批记录上下文容器。')
assert.notEqual(processContextEnd, -1, '当前批记录上下文容器必须正确闭合。')

const processContextBlock = detail.slice(processContextStart, processContextEnd)
for (const forbiddenToken of ['生产工单：', '批记录号：']) {
  assert.ok(!processContextBlock.includes(forbiddenToken), `上下文区域不得显示标签文案：${forbiddenToken}`)
}

for (const requiredToken of [
  '<span :title="detail?.workOrderCode || \'\'">{{ detail?.workOrderCode || \'--\' }}</span>',
  '<span :title="resolveCurrentBatchRecordNo()">{{ resolveCurrentBatchRecordNo() }}</span>'
]) {
  assert.ok(processContextBlock.includes(requiredToken), `上下文区域必须直接显示值并保留 title：${requiredToken}`)
}

const processContextStyleStart = detail.indexOf('.edhr-batch-detail__process-context span {')
const processContextStyleEnd = detail.indexOf('}', processContextStyleStart)
assert.notEqual(processContextStyleStart, -1, '必须保留上下文值样式。')
const processContextStyleBlock = detail.slice(processContextStyleStart, processContextStyleEnd)
for (const forbiddenStyle of ['overflow: hidden', 'text-overflow: ellipsis', 'white-space: nowrap']) {
  assert.ok(!processContextStyleBlock.includes(forbiddenStyle), `上下文值必须完整显示，不得截断：${forbiddenStyle}`)
}
for (const requiredStyle of ['overflow-wrap: anywhere', 'white-space: normal']) {
  assert.ok(processContextStyleBlock.includes(requiredStyle), `上下文值必须支持完整换行：${requiredStyle}`)
}

console.log('PASS: EDHR process header context shows raw values without truncation.')
