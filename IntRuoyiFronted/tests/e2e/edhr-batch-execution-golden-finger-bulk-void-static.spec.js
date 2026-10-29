const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const root = path.resolve(__dirname, '../..')
const listPage = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'), 'utf8')
const batchApi = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/batchExecution.ts'), 'utf8')

assert.doesNotMatch(listPage, /goldenFingerBulkVoid|GoldenFingerBulkVoid|直通生效/,
  'Batch mutation UI must expose no direct bulk void path.')
assert.doesNotMatch(batchApi, /goldenFingerBulkVoid|GoldenFingerBulkVoid|golden-finger\/bulk-void|closeEdhrBatchExecution/,
  'Batch APIs must expose neither direct bulk void nor manual close.')
const start = listPage.indexOf('const submitVoidBatchExecution = async () => {')
const end = listPage.indexOf('const handleViewArchive =', start)
assert.ok(start >= 0 && end > start)
assert.match(listPage.slice(start, end), /requestVoidBatchExecution\(\{[\s\S]*startUserSelectAssignees/,
  'Independent void must retain the formal approval submission and reviewer selection.')
console.log('PASS: obsolete direct bulk void entry removed; formal void approval retained')
