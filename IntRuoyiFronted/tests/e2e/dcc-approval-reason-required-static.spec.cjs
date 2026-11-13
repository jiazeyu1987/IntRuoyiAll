const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const approvalActions = readSource('src/views/dcc/controlled-file/detail/approval-actions.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')

assert.match(approvalActions, /mode === 'reject' && !form\.reason\?\.trim\(\)/,
  '仅驳回原因必填；批准意见可选。')
assert.match(approvalActions, /reason:\s*form\.reason\.trim\(\)/)
assert.match(detailPage, /审批意见（选填）/)
assert.match(detailPage, /驳回原因（必填）/)
console.log('PASS: DCC optional approval opinion and required rejection reason contract')
