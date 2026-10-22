const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const page = read('src/views/mes/pro/production-release/PqcProductionReleasePage.vue')
const api = read('src/api/mes/pro/productionRelease/index.ts')

const resultStart = page.indexOf('<el-result v-if="releaseResult" icon="success" title="生产放行完成">')
assert.notEqual(resultStart, -1, 'Missing production release success result')

const resultEnd = page.indexOf('</el-result>', resultStart)
assert.notEqual(resultEnd, -1, 'Missing production release success result close tag')

const successResult = page.slice(resultStart, resultEnd)

assert.match(successResult, /title="生产放行完成"/)
assert.match(successResult, /data-pqc-production-release-batch-execution-id/)
assert.doesNotMatch(successResult, /data-pqc-production-release-report-task-list/)
assert.doesNotMatch(successResult, /releaseResult\.reportUploadTasks/)
assert.doesNotMatch(successResult, /报告任务|工作待办|<el-table-column[^>]*label="状态"/)
assert.doesNotMatch(successResult, /openBatchRecord\(releaseResult\.batchExecutionId\)/)
assert.doesNotMatch(successResult, />\s*查看批记录\s*</)

assert.match(
  api,
  /reportUploadTasks:\s*MesProductionReleaseReportUploadTaskRespVO\[\]/,
  'The API response contract must keep reportUploadTasks for formal release compatibility'
)

assert.doesNotMatch(page, /openBatchRecord\(row\.batchExecutionId\)/)

console.log('pqc-production-release-success-dialog-trim-static: PASS')
