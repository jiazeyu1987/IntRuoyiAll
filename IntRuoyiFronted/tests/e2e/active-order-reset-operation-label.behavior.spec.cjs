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
