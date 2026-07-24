const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const headerStyleStart = detail.indexOf('.edhr-batch-detail__process-header {')
const headerStyleEnd = detail.indexOf('}', headerStyleStart)
assert.notEqual(headerStyleStart, -1, '必须保留工序栏顶部容器样式。')
const headerStyleBlock = detail.slice(headerStyleStart, headerStyleEnd)
assert.ok(!headerStyleBlock.includes('justify-content: flex-end'), '工序栏顶部不得把上下文挤到右侧。')

const contextStyleStart = detail.indexOf('.edhr-batch-detail__process-context {')
const contextStyleEnd = detail.indexOf('}', contextStyleStart)
assert.notEqual(contextStyleStart, -1, '必须保留当前批记录上下文样式。')
const contextStyleBlock = detail.slice(contextStyleStart, contextStyleEnd)
assert.ok(contextStyleBlock.includes('width: 100%'), '上下文必须占满顶部宽度，避免红框位置留空。')
assert.ok(contextStyleBlock.includes('text-align: left'), '上下文必须从左侧开始显示。')
assert.ok(!contextStyleBlock.includes('text-align: right'), '上下文不得继续右对齐。')

const processHeaderStart = detail.indexOf('<div class="edhr-batch-detail__process-header">')
const pendingListStart = detail.indexOf('<div v-if="pendingTaskEntries.length" class="edhr-batch-detail__pending-task-list"', processHeaderStart)
const processHeaderBlock = detail.slice(processHeaderStart, pendingListStart)
assert.ok(processHeaderBlock.includes('edhr-batch-detail__process-context'), '顶部必须保留上下文容器。')
assert.ok(!processHeaderBlock.includes('edhr-batch-detail__review-subtitle'), '顶部不得恢复工序标题。')

console.log('PASS: EDHR process header context fills from the left.')
