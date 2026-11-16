const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const start = source.indexOf('const activeOrderOperationTypeLabels:')
const end = source.indexOf('const formatActiveOrderOperationResultStatus', start)
assert.ok(start >= 0 && end > start)
const body = ts.transpileModule(source.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const format = Function(`${body}; return formatActiveOrderOperationType`)()

test('reset-generated formal operation facts render in upload and history details', () => {
  assert.equal(format({ operationType: 'RESET_FIXED_TEST_ORDER' }), '重置指定测试订单')
  assert.equal(format({ operationType: 'RESET_FIXED_TEST_ORDER', operationName: 'RESET_FIXED_TEST_ORDER' }),
    '重置指定测试订单')
})

test('unrecognized operation remains an explicit contract error', () => {
  assert.throws(() => format({ operationType: 'UNRECOGNIZED_OPERATION' }),
    /ACTIVE_ORDER_OPERATION_LABEL_UNMAPPED/)
})

test('execution facts preserve the Chinese process and inspection round', () => {
  assert.equal(format({ operationType: 'PRODUCTION_SUBMIT', operationName: '提交生产记录：大包装' }), '提交生产记录：大包装')
  assert.equal(format({ operationType: 'PQC_REVIEW', operationName: '复核过程检验记录：中大包装（第3轮巡检）' }), '复核过程检验记录：中大包装（第3轮巡检）')
  assert.equal(format({ operationType: 'PQC_PATROL_CANCEL', operationName: '撤销巡检轮次（第2轮）' }), '撤销巡检轮次（第2轮）')
  assert.equal(format({ operationType: 'REVERSE_TRACE_QUERY', operationName: '反查关联批次' }), '反查关联批次')
})
