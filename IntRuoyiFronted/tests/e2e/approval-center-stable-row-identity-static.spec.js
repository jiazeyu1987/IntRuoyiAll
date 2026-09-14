const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const approvalCenterPath = path.resolve(__dirname, '../../src/views/approval-center/index.vue')
const source = fs.readFileSync(approvalCenterPath, 'utf8')
const approvalTableOpeningTag = source.match(/<el-table\b[\s\S]*?>/)?.[0] || ''

assert.ok(
  approvalTableOpeningTag.includes('row-key="id"'),
  'Approval Center table must use the task id as a stable row key so filtered actions cannot retain an older row binding'
)

console.log('approval-center stable row identity static checks passed')
