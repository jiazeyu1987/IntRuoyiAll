const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const { parse, compileTemplate, compileStyle } = require('vue/compiler-sfc')
const filename = 'src/views/mes/pro/in-process-order/index.vue'
const descriptor = parse(fs.readFileSync(path.resolve(__dirname, '..', filename), 'utf8')).descriptor
const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const js = ts.transpileModule(ast.statements.filter(n => !ts.isImportDeclaration(n))
  .map(n => n.getText(ast)).join('\n'), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const statusModule = { exports: {} }
const statusSource = fs.readFileSync(path.resolve(__dirname, '../src/views/mes/pro/in-process-order/inProcessOrderStatus.ts'), 'utf8')
Function('module', 'exports', ts.transpileModule(statusSource, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText)(statusModule, statusModule.exports)
const { resolveInProcessOrderStatus } = statusModule.exports
function setup(api) {
  const calls = []
  const filter = { state: { conditions: [] }, applied: 0, updateState(next) { Object.assign(this.state, next) }, async applyQuickFilter() { this.applied++ } }
  const deps = {
    ref, reactive, computed, resolveInProcessOrderStatus, defineOptions() {}, onMounted() {}, onActivated() {},
    useTableQuickFilter: () => filter,
    useUserTableColumns: (key, defaults) => ({ columns: ref(defaults), isColumnVisible: () => true }),
    getInProcessOrderPage: query => { calls.push(query); return api(query) }
  }
  return { calls, filter, ...Function(...Object.keys(deps), `${js}; return { getList, list, total, loadError, loading, queryParams, updateQuickFilterState, statusColumnFilter }`)(...Object.values(deps)) }
}

test('completed cycles retain each real release stage in the table and status filter', () => {
  const s = setup(async () => ({ list: [], total: 0 }))
  for (const [releaseApplicationStatus, expected] of [
    ['PQC_RELEASE_PENDING', '待PQC放行'], ['PQC_RELEASE_REJECTED', 'PQC放行已驳回'],
    ['REPORT_UPLOAD_PENDING', '待补放行资料'], ['MANAGER_RELEASE_PENDING', '待上市放行'],
    ['MARKET_RELEASE_REJECTED', '上市放行已驳回'], ['MARKET_RELEASE_WITHDRAWN', '上市放行已撤回'],
    ['RELEASED', '已上市放行'], ['NONCONFORMANCE_VOID', 'QA已作废'],
    ['NONCONFORMANCE_REWORK', '已结束并转入返工']
  ]) {
    const row = { businessStatus: 'COMPLETED', releaseApplicationStatus }
    assert.equal(resolveInProcessOrderStatus(row).label, expected)
    assert.equal(s.statusColumnFilter[0].getValue(row), expected)
  }
})

test('QA terminal state takes priority over older abnormal flags without concealing read errors', () => {
  const row = { businessStatus: 'COMPLETED', releaseApplicationStatus: 'NONCONFORMANCE_VOID', abnormal: true }
  assert.equal(resolveInProcessOrderStatus(row).label, 'QA已作废')
  assert.deepEqual(resolveInProcessOrderStatus({ ...row, readBlocked: true, readBlockReason: '正式快照缺失' }),
    { label: '数据异常', type: 'danger', reason: '正式快照缺失' })
  assert.equal(resolveInProcessOrderStatus({ businessStatus: 'ACTIVE' }).label, '在制')
  assert.equal(resolveInProcessOrderStatus({ businessStatus: 'VERSION_UPGRADE_PENDING' }).label, '升版审批中')
  assert.equal(resolveInProcessOrderStatus({ businessStatus: 'COMPLETED', releaseApplicationStatus: 'UNRECOGNIZED' }).label,
    '未知放行状态：UNRECOGNIZED')
})
test('list template and scoped layout compile', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'test' }).errors, [])
  assert.deepEqual(compileStyle({ source: descriptor.styles[0].content, filename, id: 'data-v-test', scoped: true }).errors, [])
})
test('combined filters and pagination reach the service unchanged', async () => {
  const s = setup(async () => ({ list: [{ id: '9223372036854775800' }], total: 41 }))
  Object.assign(s.queryParams, { productName: '导管', equipment: '设备甲', batchCode: 'LOT-A', pageNo: 2 })
  await s.getList()
  assert.deepEqual(s.calls[0], { pageNo: 2, pageSize: 20, productName: '导管', equipment: '设备甲', batchCode: 'LOT-A' })
  assert.equal(s.total.value, 41)
  assert.equal(s.list.value[0].id, '9223372036854775800')
})
test('failed query removes old results and supports retry', async () => {
  let fail = false
  const s = setup(async () => { if (fail) throw new Error('无查询权限'); return { list: [{ id: 1 }], total: 1 } })
  await s.getList(); fail = true; await s.getList()
  assert.equal(s.loadError.value, '无查询权限')
  assert.deepEqual(s.list.value, [])
  assert.equal(s.total.value, 0)
  fail = false; await s.getList()
  assert.equal(s.loadError.value, '')
  assert.equal(s.list.value.length, 1)
})
test('outdated response cannot overwrite a newer filter query', async () => {
  let resolve
  const pending = new Promise(ok => { resolve = ok })
  const s = setup(query => query.pageNo === 1 ? pending : Promise.resolve({ list: [{ id: 2 }], total: 2 }))
  const old = s.getList()
  s.queryParams.pageNo = 2; await s.getList()
  resolve({ list: [{ id: 1 }], total: 99 }); await old
  assert.deepEqual(s.list.value, [{ id: 2 }])
  assert.equal(s.total.value, 2)
  assert.equal(s.loading.value, false)
})

test('removing a filter refreshes the list including removal of the final condition', async () => {
  const s = setup(async () => ({ list: [], total: 0 }))
  await s.updateQuickFilterState({ conditions: [{ key: 'productName' }, { key: 'equipment' }] })
  assert.equal(s.filter.applied, 0)
  await s.updateQuickFilterState({ conditions: [{ key: 'productName' }] })
  assert.equal(s.filter.applied, 1)
  await s.updateQuickFilterState({ conditions: [] })
  assert.equal(s.filter.applied, 2)
})
