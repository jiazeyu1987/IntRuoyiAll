const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.resolve(frontendRoot, 'src/views/mes/pro/production-release/PqcProductionReleasePage.vue'),
  'utf8'
)

const actionStart = source.indexOf('<el-table-column label="操作"')
assert.notEqual(actionStart, -1, 'Missing operation column')
const actionEnd = source.indexOf('</el-table-column>', actionStart)
assert.notEqual(actionEnd, -1, 'Missing operation column close tag')
const actionColumn = source.slice(actionStart, actionEnd)

assert.match(actionColumn, /data-pqc-production-release-detail/, 'Detail action must remain')
assert.match(actionColumn, /data-pqc-production-release-approve/, 'Release action must remain')
assert.match(
  actionColumn,
  /data-pqc-production-release-nonconformance/,
  'Nonconformance action must remain'
)

assert.doesNotMatch(actionColumn, />\s*查看批记录\s*</)
assert.doesNotMatch(actionColumn, /openBatchRecord\(row\.batchExecutionId\)/)
assert.doesNotMatch(actionColumn, /v-else-if="row\.batchExecutionId"/)
assert.doesNotMatch(source, /const openBatchRecord\s*=/)
assert.doesNotMatch(source, /MesProEdhrBatchExecutionDetail/)

console.log('pqc-production-release-tab-batch-record-controls-static: PASS')
