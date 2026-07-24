const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const headerStyleStart = detail.lastIndexOf('.edhr-batch-detail__process-header {')
const headerStyleEnd = detail.indexOf('}', headerStyleStart)
assert.notEqual(headerStyleStart, -1, '必须保留最终生效的工序栏顶部样式。')
const headerStyleBlock = detail.slice(headerStyleStart, headerStyleEnd)

assert.ok(headerStyleBlock.includes('background: transparent'), '最终生效的工序栏顶部样式必须覆盖为透明背景。')
assert.ok(!headerStyleBlock.includes('background: #f7f9fc'), '最终生效的工序栏顶部样式不得继续使用淡蓝背景。')
assert.ok(headerStyleBlock.includes('justify-content: flex-start'), '工序栏顶部必须继续从左侧铺满显示。')

console.log('PASS: EDHR process header context has no blue background.')
