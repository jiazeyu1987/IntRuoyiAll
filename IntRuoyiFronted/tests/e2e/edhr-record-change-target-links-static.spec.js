const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/RecordChangePage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert.doesNotMatch(
  source,
  /批次编号：\s*\{\{\s*row\.batchExecutionId/,
  '变更记录对象列不得把 batchExecutionId 标成批次编号。'
)

assert.ok(
  source.includes('批次ID') && source.includes('执行ID'),
  '变更记录对象列和详情必须使用批次ID、执行ID的准确标签。'
)

assert.ok(
    source.includes('openBatchExecution(row)') &&
    source.includes("path: '/mes/pro/feedback/edhr-batch-execution/detail'") &&
    source.includes('openExecution(row)') &&
    source.includes("path: '/mes/pro/feedback/edhr-execution/form'"),
  '变更记录必须提供批次执行详情和执行表单的对象追溯入口。'
)

assert.ok(
  source.includes('canOpenBatchExecution(row)') &&
    source.includes('canOpenExecution(row)') &&
    source.includes('edhr-record-change__object-link--disabled'),
  '缺少对象 ID 时必须展示不可点击状态，不能跳转到错误详情。'
)

assert.match(
  source,
  /const router = useRouter\(\)/,
  '变更记录对象入口必须使用 router 走真实前端详情路由。'
)

console.log('PASS: EDHR record change target links static contract')
