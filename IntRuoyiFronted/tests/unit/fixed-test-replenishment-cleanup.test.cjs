const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const assert = require('node:assert/strict')
const ts = require('typescript')
const source = fs.readFileSync(path.join(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const start = source.indexOf('const handlePrepareFixedTestOrderData =')
const end = source.indexOf('const handleResetFixedSimulationActiveOrder =', start)
const code = ts.transpileModule(source.slice(start, end) + '\nglobalThis.run = handlePrepareFixedTestOrderData', {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

for (const removed of [0, 1, -1]) {
  test(`fixed test preparation reports removed simulation count ${removed}`, async () => {
    const ref = value => ({ value })
    const notices = []
    const context = {
      activeOrderTestResetBlocked: ref(false), activeOrderTestDataSet: ref(1),
      activeOrderTestDataSubmitting: ref(false), activeOrderTestResetSubmitting: ref(false),
      activeOrderTestDataReceipt: ref(undefined),
      prepareFixedTestOrderData: async () => ({ workOrderCode: 'FIXED', documents:
        ['生产订单', '领料单明细', '补料单明细', '用料清单'].map((name, i) => ({
          name, createdCount: 0, deletedCount: i === 2 ? removed : 0, skippedCount: 0, repairedCount: 0
        })) }),
      resolveErrorMessage: e => e.message,
      ElMessage: { success: text => notices.push(text), error: text => notices.push(text) }
    }
    vm.runInNewContext(code, context)
    await context.run(1)
    assert.equal(context.activeOrderTestDataReceipt.value.success, removed >= 0)
    assert.equal(context.activeOrderTestResetSubmitting.value, false)
    assert.equal(context.activeOrderTestDataSubmitting.value, false)
    if (removed === 1) {
      assert.match(context.activeOrderTestDataReceipt.value.message, /补料单明细新增0条、移除旧模拟数据1条/)
      assert.match(notices[0], /旧模拟补料已移除/)
    }
  })
}
