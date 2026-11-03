const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/batchrecordformlist/index.vue'), 'utf8')
const selectionBlock = source.slice(source.indexOf('const buildWordImportSelection ='), source.indexOf('const confirmWordImportUpgradeSelections ='))
const selectedOptions = [
  { optionKey: 'large', routeProductId: '2116486795646726145', productName: '当前项目' },
  { optionKey: 'small', routeProductId: 123, productName: '数字 ID 项目' },
  { optionKey: 'new', productName: '新项目' }
]
const context = vm.createContext({
  wordImportDialog: {
    preflight: { routeProductOptions: selectedOptions, routeUpgradeRequired: true, currentRouteId: '2116486795512508424' },
    selectedRouteProductOptionKeys: selectedOptions.map(option => option.optionKey),
    selectedAction: 'REBUILD_V1',
    rebuildBatchRecord: false
  }
})
vm.runInContext(ts.transpileModule(selectionBlock, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText + '\nthis.selection = buildWordImportSelection(); this.confirmed = buildWordImportConfirmedSelection(selection, false, selection.selectedOptions);', context)
for (const result of [context.selection, context.confirmed]) {
  assert.deepEqual(Array.from(result.selectedRouteProductIds), ['2116486795646726145', 123], '已有路线产品 ID 必须完整保留，不得丢弃或将长整型转为有损数字')
  assert.deepEqual(Array.from(result.selectedProductNames), ['新项目'])
}
assert.equal(context.confirmed.routeUpgradeConfirmed, true)
assert.equal(context.confirmed.expectedRouteId, '2116486795512508424')
console.log('PASS: Word import preserves route product IDs through selection and confirmation')
