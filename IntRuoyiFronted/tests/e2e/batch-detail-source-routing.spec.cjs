const fs = require('node:fs')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ts = require('typescript')
const path = require('node:path')
;(async () => {
  for (const file of ['BatchExecutionListPage.vue', 'BatchRecordHistoryPage.vue']) {
    const source = fs.readFileSync(path.join(__dirname, '../../src/views/mes/pro/edhr-batch', file), 'utf8')
    const handler = source.match(/const openActiveOrderDetail = async[\s\S]*?\n}/)[0]
    const routes = []
    const context = { router: { push: async route => routes.push(route) }, message: {error() {}}, ElMessage: {error() {}} }
    vm.createContext(context)
    vm.runInContext(ts.transpile(handler + '\nglobalThis.open = openActiveOrderDetail'), context)
    await context.open({id: '900000001163'})
    assert.equal(routes[0].path, '/mes/pro/feedback/edhr-batch-execution/active-order-detail', file + ': all batch details use active order detail')
    assert.equal(routes[0].query.batchExecutionId, '900000001163')
    await context.open({id: '900000001164', activeOrderId: 300})
    assert.equal(routes[1].path, '/mes/pro/feedback/edhr-batch-execution/active-order-detail')
    assert.equal(routes[1].query.batchExecutionId, '900000001164')
    await context.open({})
    assert.equal(routes.length, 2, 'missing batch ID must not navigate')
  }
  console.log('PASS batch detail source routing')
})().catch(error => { console.error(error); process.exitCode = 1 })
