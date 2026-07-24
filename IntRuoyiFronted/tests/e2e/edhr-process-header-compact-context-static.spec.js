const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const processHeaderStart = detail.indexOf('<div class="edhr-batch-detail__process-header">')
const processHeaderEnd = detail.indexOf('</div>', detail.indexOf('</div>', processHeaderStart) + 1)
assert.notEqual(processHeaderStart, -1, '工序栏顶部容器必须存在。')
assert.notEqual(processHeaderEnd, -1, '工序栏顶部容器必须正确闭合。')

const processHeaderBlock = detail.slice(processHeaderStart, processHeaderEnd)
assert.ok(!processHeaderBlock.includes('edhr-batch-detail__review-subtitle'), '红框位置不得继续渲染工序标题节点。')
assert.ok(!processHeaderBlock.includes('>工序<'), '红框位置不得继续显示“工序”文本。')
assert.ok(processHeaderBlock.includes('edhr-batch-detail__process-context'), '绿框位置必须保留上下文容器。')
assert.ok(processHeaderBlock.includes("detail?.workOrderCode || '--'"), '绿框位置必须显示生产工单号值。')
assert.ok(processHeaderBlock.includes('resolveCurrentBatchRecordNo()'), '绿框位置必须显示批记录号值。')

const contextStyleStart = detail.indexOf('.edhr-batch-detail__process-context {')
const contextStyleEnd = detail.indexOf('}', contextStyleStart)
assert.notEqual(contextStyleStart, -1, '必须保留上下文样式块。')
const contextStyleBlock = detail.slice(contextStyleStart, contextStyleEnd)
assert.ok(contextStyleBlock.includes('font-size: 11px'), '绿框上下文字号必须调小为 11px。')
assert.ok(!contextStyleBlock.includes('font-size: 12px'), '绿框上下文不得继续使用 12px 字号。')

console.log('PASS: EDHR process header hides title and uses compact context text.')
