const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const { parse, compileTemplate } = require('vue/compiler-sfc')

// 隔离行为测试：执行页面真实处理函数，不访问 ERP 或生产数据库。
const filename = path.resolve(__dirname, '../../../src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(filename, 'utf8')
const { descriptor, errors } = parse(source, { filename })
assert.deepEqual(errors, [])
const start = descriptor.scriptSetup.content.indexOf('const analyzeErpMissingWorkOrders =')
const end = descriptor.scriptSetup.content.indexOf('const submitWorkOrderAdmission =', start)
assert.ok(start >= 0 && end > start)
const handlers = ts.transpileModule(descriptor.scriptSetup.content.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(preview) {
  const success = [], warning = [], failures = [], deleted = []
  const context = {
    Error,
    erpWorkOrderCleanupAnalyzing: { value: false },
    erpWorkOrderCleanupDeleting: { value: false },
    erpWorkOrderCleanupDialogVisible: { value: false },
    erpWorkOrderCleanupPreview: { value: undefined },
    erpWorkOrderCleanupError: { value: '' },
    selectedWorkOrders: { value: [1] },
    workOrderAdmissionQueryParams: { pageNo: 3 },
    message: { success: text => success.push(text), warning: text => warning.push(text), error: text => failures.push(text) },
    MesProScheduleOrderApi: {
      analyzeErpMissingWorkOrders: async () => preview,
      deleteErpMissingWorkOrders: async request => { deleted.push(request); return request.items.length }
    },
    refreshed: 0
  }
  context.getWorkOrderAdmissionList = async () => { context.refreshed++ }
  context.erpWorkOrderCleanupDeletableItems = { get value() {
    return context.erpWorkOrderCleanupPreview.value?.missingWorkOrders.filter(item => item.deletable) || []
  } }
  vm.createContext(context)
  vm.runInContext(handlers + '\nglobalThis.analyze = analyzeErpMissingWorkOrders; globalThis.confirm = confirmDeleteErpMissingWorkOrders', context)
  return { context, success, warning, failures, deleted }
}

const candidate = id => ({ workOrderId: id, workOrderCode: `MO-${id}`, syncRecordId: id + 10, sourceFid: `${id+100}`, sourceBillNo: `MO-${id}`, sourceMaterialNumber: 'ITEM', deletable: true })
const preview = items => ({ environmentKey: 'environment', analyzedCount: 30, unverifiableCount: 0, missingWorkOrders: items })

test('page template compiles with the cleanup button and confirmation dialog', () => {
  const compiled = compileTemplate({ source: descriptor.template.content, filename, id: 'erp-cleanup' })
  assert.deepEqual(compiled.errors, [])
})

test('confirmation dialog prevents dismissal while deletion is pending', () => {
  const template = descriptor.template.content
  const start = template.indexOf('<el-dialog\n      v-model="erpWorkOrderCleanupDialogVisible"')
  assert.ok(start >= 0, '使用原生弹框，关闭控制直接传给 Element Plus')
  const end = template.indexOf('</el-dialog>', start) + '</el-dialog>'.length
  const compiled = compileTemplate({ source: template.slice(start, end), filename, id: 'erp-cleanup-dialog' })
  assert.deepEqual(compiled.errors, [])
  const module = { exports: {} }
  const context = {
    module,
    exports: module.exports,
    require: () => ({ ...require('vue'), resolveComponent: name => name })
  }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(compiled.code, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  for (const deleting of [false, true]) {
    const node = module.exports.render({
      erpWorkOrderCleanupDialogVisible: true,
      erpWorkOrderCleanupDeleting: deleting
    }, [])
    assert.equal(node.props['close-on-click-modal'], false)
    assert.equal(node.props['close-on-press-escape'], !deleting)
    assert.equal(node.props['show-close'], !deleting)
  }
})

test('cleanup button requires list analysis and production work order deletion permissions', () => {
  const script = descriptor.scriptSetup.content
  const begin = script.indexOf('const hasErpWorkOrderCleanupPermission =')
  const end = script.indexOf('const erpWorkOrderCleanupDeletableItems =', begin)
  for (const permissions of [[], ['mes:pro-schedule-order:admission-diff'], ['mes:pro-work-order:delete'], ['mes:pro-schedule-order:admission-diff', 'mes:pro-work-order:delete']]) {
    const context = { computed: getter => getter(), checkPermi: requested => requested.every(permission => permissions.includes(permission)) }
    vm.createContext(context)
    vm.runInContext(script.slice(begin, end) + '\nglobalThis.allowed = hasErpWorkOrderCleanupPermission', context)
    assert.equal(context.allowed, permissions.length === 2)
  }
})

test('analysis and cancellation never call deletion; confirmation excludes retained orders', async () => {
  const fixture = setup(preview([candidate(1), { ...candidate(2), deletable: false, retainReason: '已进入排产' }]))
  await fixture.context.analyze()
  assert.equal(fixture.context.erpWorkOrderCleanupDialogVisible.value, true)
  assert.equal(fixture.deleted.length, 0)
  fixture.context.erpWorkOrderCleanupDialogVisible.value = false // 用户取消
  assert.equal(fixture.deleted.length, 0)
  await fixture.context.analyze()
  await fixture.context.confirm()
  assert.equal(fixture.deleted.length, 1)
  assert.deepEqual(Array.from(fixture.deleted[0].items, item => item.workOrderId), [1])
  assert.equal(fixture.deleted[0].environmentKey, 'environment')
  assert.equal(fixture.context.erpWorkOrderCleanupDialogVisible.value, false)
  assert.equal(fixture.context.workOrderAdmissionQueryParams.pageNo, 1)
  assert.equal(fixture.context.refreshed, 1)
})

test('empty or unverifiable analysis reports results without deleting', async () => {
  const fixture = setup({ ...preview([]), unverifiableCount: 3 })
  await fixture.context.analyze()
  assert.equal(fixture.context.erpWorkOrderCleanupDialogVisible.value, false)
  assert.equal(fixture.warning.length, 1)
  assert.equal(fixture.deleted.length, 0)
})

test('ERP analysis failure is visible and never opens confirmation or deletes', async () => {
  const fixture = setup(preview([]))
  fixture.context.MesProScheduleOrderApi.analyzeErpMissingWorkOrders = async () => { throw new Error('ERP 不可用') }
  await fixture.context.analyze()
  assert.match(fixture.failures[0], /ERP 不可用/)
  assert.equal(fixture.context.erpWorkOrderCleanupDialogVisible.value, false)
  assert.equal(fixture.context.erpWorkOrderCleanupAnalyzing.value, false)
  assert.equal(fixture.deleted.length, 0)
})

test('delete failure retains the preview and requires analysis again', async () => {
  const fixture = setup(preview([candidate(1)]))
  await fixture.context.analyze()
  fixture.context.MesProScheduleOrderApi.deleteErpMissingWorkOrders = async () => { throw new Error('ERP 订单已恢复') }
  await fixture.context.confirm()
  assert.equal(fixture.context.erpWorkOrderCleanupDialogVisible.value, true)
  assert.match(fixture.context.erpWorkOrderCleanupError.value, /ERP 订单已恢复/)
  assert.equal(fixture.context.erpWorkOrderCleanupDeleting.value, false)
  assert.equal(fixture.context.refreshed, 0)
})

test('duplicate confirmation cannot submit twice while deletion is pending', async () => {
  const fixture = setup(preview([candidate(1)]))
  await fixture.context.analyze()
  let resolve
  fixture.context.MesProScheduleOrderApi.deleteErpMissingWorkOrders = request => {
    fixture.deleted.push(request)
    return new Promise(done => { resolve = done })
  }
  const pending = fixture.context.confirm()
  await fixture.context.confirm()
  assert.equal(fixture.deleted.length, 1)
  resolve(1)
  await pending
})
