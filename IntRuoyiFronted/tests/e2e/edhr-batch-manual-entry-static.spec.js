const fs = require('fs')
const path = require('path')
const assert = require('node:assert/strict')
const root = path.resolve(__dirname, '..', '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')
const api = read('src/api/mes/pro/edhr/batchExecution.ts')
const list = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const feedback = read('src/views/mes/pro/feedback/FeedbackForm.vue')
for (const source of [api, list, feedback]) {
  assert.doesNotMatch(source, /openOrCreateManualEdhrBatchExecution|open-or-create-manual|EdhrBatchExecutionManualOpenOrCreateReqVO/)
}
assert.doesNotMatch(list, /openCreateDialog|createDialogVisible|submitOpenOrCreate|prefillWorkOrderCode/)
assert.doesNotMatch(feedback, /handleOpenEdhr["\s=]|edhrOpening|buildEdhrEntryContext/)
assert.match(feedback, /@click="handleOpenEdhrBatchExecution"/)
assert.match(list, /edhr-batch-execution\/active-order-detail/)
assert.doesNotMatch(read('src/views/mes/pro/workorder/index.vue'), /prefillWorkOrderCode/)
assert.match(read('src/views/mes/pro/workorder/index.vue'), /workOrderCode: row.code/)

console.log('edhr-batch-manual-entry-static: PASS')
