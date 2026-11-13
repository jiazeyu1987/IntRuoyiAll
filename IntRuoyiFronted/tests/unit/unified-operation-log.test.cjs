const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const sfc = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const logRoot = path.join(root, 'src/views/system/operatelog')

function evaluate(source, modules, globals = {}) {
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const module = { exports: {} }
  new Function('require', 'exports', 'module', ...Object.keys(globals), code)(
    id => {
      if (id in modules) return modules[id]
      if (id.endsWith('.vue')) return { default: {} }
      throw new Error(`Unexpected dependency: ${id}`)
    }, module.exports, module, ...Object.values(globals)
  )
  return module.exports
}
function sources(get) {
  return evaluate(fs.readFileSync(path.join(logRoot, 'logSources.ts'), 'utf8'), {
    vue: { defineAsyncComponent: loader => ({ loader }) },
    '@/config/axios': { default: { get } }
  }).logSources
}
function setup(filename, modules, globals, props = {}) {
  const source = fs.readFileSync(path.join(logRoot, filename), 'utf8')
  const { descriptor, errors } = sfc.parse(source, { filename })
  assert.deepEqual(errors, [])
  const compiled = sfc.compileScript(descriptor, { id: 'log-test' })
  const result = evaluate(compiled.content, { vue, ...modules }, {
    computed: vue.computed, ref: vue.ref, reactive: vue.reactive,
    onMounted: () => {}, useRoute: () => ({ query: {} }), ...globals
  })
  return result.default.setup(props, { expose() {} })
}
const record = { id: 17, actorUserId: 3, occurredAt: 1730000000000, reason: '实际变更原因', beforeSnapshotJson: '{"state":"DRAFT"}', afterSnapshotJson: '{"state":"ACTIVE"}', signatureId: 9, metadata: { original: '原始证据' } }

test('all sources have unique identities and keep original data query permissions in the menu migration', () => {
  const all = sources(() => { throw new Error('Registry loading must not query APIs') })
  assert.equal(new Set(all.map(source => source.id)).size, all.length)
  assert.ok(all.length >= 40)
  const migration = fs.readFileSync(path.join(root, '../IntRuoyiBackend/sql/mysql/20261112_unified_operation_log_navigation.sql'), 'utf8')
  for (const source of all) {
    assert.ok(source.component || source.read, source.id)
    if (!source.permissions.length) assert.equal(source.id, 'workflow')
    for (const permission of source.permissions) assert.ok(migration.includes(`'${permission}'`), `${source.id}: ${permission}`)
  }
  assert.match(migration, /START TRANSACTION/)
  assert.match(migration, /ROLLBACK; RESIGNAL/)
  assert.doesNotMatch(migration, /DELETE FROM|TRUNCATE TABLE|INSERT INTO system_operate_log/i)
})

test('every contextual source retains the complete original record, including before/after and signature evidence', async () => {
  const requests = []
  const all = sources(async request => {
    requests.push(request)
    if (request.url.endsWith('/order-detail')) return { detail: { operationFacts: [record] } }
    if (request.url.endsWith('/active-order/detail') || request.url.endsWith('/active-order-detail')) return { operationFacts: [record] }
    if (request.url.startsWith('/trade/')) return { logs: [record] }
    if (request.url.endsWith('/submission/detail') || request.url.endsWith('/log')) return record
    if (request.url.endsWith('/page')) return { list: [record], total: 1 }
    return [record]
  })
  for (const source of all.filter(source => source.read)) {
    const params = { pageNo: 1, pageSize: 20 }
    for (const field of source.fields ?? []) params[field.key] = field.options?.[0].value ?? '17'
    const result = await source.read(params)
    assert.deepEqual(result, { list: [record], total: 1 }, source.id)
    assert.equal(requests.at(-1).params.pageNo, source.id === 'runtime-operation' ? undefined : 1)
  }
})

test('array sources paginate all original history instead of discarding rows after the first page', async () => {
  const history = Array.from({ length: 47 }, (_, id) => ({ ...record, id }))
  const source = sources(async () => history).find(source => source.id === 'schedule')
  const result = await source.read({ scheduleOrderId: 9, pageNo: 3, pageSize: 20 })
  assert.equal(result.total, 47)
  assert.deepEqual(result.list.map(row => row.id), [40, 41, 42, 43, 44, 45, 46])
})

test('CRM history uses its original type-specific endpoint and preserves later pages', async () => {
  let sent
  const history = Array.from({ length: 25 }, (_, id) => ({ ...record, id }))
  const source = sources(async request => { sent = request; return { list: history, total: 25 } }).find(source => source.id === 'crm-product')
  assert.equal((await source.read({ bizId: 7, pageNo: 2, pageSize: 20 })).list.length, 5)
  assert.deepEqual(sent, { url: '/crm/operate-log/page', params: { bizId: 7, pageNo: 2, pageSize: 20, bizType: 6 } })
})

test('GxP detail reads retain the original authorized scope and event identity', async () => {
  let sent
  const all = sources(async request => { sent = request; return record })
  for (const [id, identity] of [['team-gxp', 'activeOrderId'], ['pqc-gxp', 'applicationId'], ['batch-gxp', 'batchExecutionId'], ['batch-order-gxp', 'activeOrderId']]) {
    assert.equal(await all.find(source => source.id === id).detail(record, { [identity]: 42 }), record)
    assert.deepEqual(sent.params, { [identity]: 42, eventId: 17 })
    assert.ok(sent.url.endsWith('/audit/get'))
  }
})

test('batch operation audits and certificate history retain their original context and full records', async () => {
  const requests = []
  const all = sources(async request => {
    requests.push(request)
    if (request.url.endsWith('/history')) return [record]
    if (request.url.endsWith('/page')) return { list: [record], total: 1 }
    return record
  })
  const batch = all.find(source => source.id === 'batch-operation-audit')
  assert.deepEqual(await batch.read({ batchExecutionId: 23, pageNo: 1, pageSize: 20 }), { list: [record], total: 1 })
  assert.equal(requests.at(-1).params.batchExecutionId, 23)
  assert.equal(await batch.detail(record), record)
  assert.equal(requests.at(-1).url, '/mes/pro/edhr-operation-audit/17')
  assert.deepEqual(await all.find(source => source.id === 'certificate-history').read({ certificateId: 21, versionId: 5, pageNo: 1, pageSize: 20 }), { list: [record], total: 1 })
  assert.equal(requests.at(-1).url, '/dcc/registration-certificates/21/history')
  assert.equal(requests.at(-1).params.versionId, 5)
})

test('source API errors and invalid results fail explicitly without fabricated empty logs', async () => {
  for (const id of ['schedule', 'permission', 'pqc-order', 'runtime-operation']) {
    await assert.rejects(sources(async () => { throw new Error('service unavailable') }).find(source => source.id === id).read({ userId: 2 }), /service unavailable/)
    await assert.rejects(sources(async () => null).find(source => source.id === id).read({ userId: 2 }), /响应无效/)
  }
})

function portal(permissions, query = {}) {
  const all = sources(() => { throw new Error('Navigation must not query APIs') })
  const replacements = []
  const state = setup('index.vue', {
    '@/directives/permission/hasPermi': { hasPermission: codes => codes.some(code => permissions.includes(code)) },
    './logSources': { logSources: all }
  }, { useRoute: () => ({ query, path: '/system/log/operate-log', fullPath: '/system/log/operate-log' }), useRouter: () => ({ replace: target => replacements.push(target) }) })
  return { state, replacements }
}
test('business query permission grants its original log source without granting system operation data', () => {
  const { state } = portal(['trade:order:query'])
  assert.deepEqual(state.allowed.value.map(source => source.id), ['workflow', 'trade-order'])
  assert.equal(state.selected.value.id, 'workflow')
})
test('an explicit inaccessible or unknown source is rejected without switching to another data source', () => {
  assert.equal(portal(['trade:order:query'], { logSource: 'system' }).state.selected.value, undefined)
  assert.equal(portal(['system:operate-log:query'], { logSource: 'unknown' }).state.selected.value, undefined)
})
test('switching sources clears unrelated object identifiers and filters', async () => {
  const { state, replacements } = portal(['trade:order:query'], { logSource: 'workflow', id: '7' })
  await state.selectSource('trade-order')
  assert.deepEqual(replacements, [{ path: '/system/log/operate-log', query: { logSource: 'trade-order' } }])
})

function panel(source, granted = true) {
  return setup('BusinessLogPanel.vue', {
    '@/directives/permission/hasPermi': { hasPermission: () => granted },
    '@/utils/formatTime': { formatDate: value => String(value) },
    '@/views/mes/pro/processpool/components/businessEvidenceDisplay': { businessFieldLabel: key => key, businessValueLabel: (_, value) => String(value) }
  }, {}, { source })
}
test('missing object identity and revoked permissions block the query before making any request', async () => {
  let calls = 0
  const source = { id: 'schedule', label: '排产', permissions: ['mes:pro-schedule-order:query'], fields: [{ key: 'scheduleOrderId', label: '排产工单编号', required: true }], read: async () => { calls++; return { list: [record], total: 1 } } }
  const state = panel(source)
  await state.load()
  assert.equal(calls, 0)
  assert.match(state.error.value, /请输入排产工单编号/)
  const denied = panel(source, false)
  denied.query.scheduleOrderId = 1
  await denied.load()
  assert.equal(calls, 0)
  assert.match(denied.error.value, /无权/)
})
test('detail uses the exact query identity of the displayed rows even if form inputs change later', async () => {
  let detailQuery
  const source = { id: 'scope', label: '审计', permissions: ['query'], fields: [{ key: 'activeOrderId', label: '活跃订单', required: true }], read: async () => ({ list: [record], total: 1 }), detail: async (_, query) => { detailQuery = query; return record } }
  const state = panel(source)
  state.query.activeOrderId = 'old-order'
  await state.load()
  state.query.activeOrderId = 'new-order'
  await state.showDetail(record)
  assert.equal(detailQuery.activeOrderId, 'old-order')
  assert.deepEqual(state.detail.value, record)
})
test('failed query clears old rows and reports the actual failure', async () => {
  let fail = false
  const state = panel({ id: 'log', label: '日志', permissions: [], read: async () => { if (fail) throw new Error('tenant access revoked'); return { list: [record], total: 1 } } })
  await state.load()
  assert.equal(state.rows.value.length, 1)
  fail = true
  await state.load()
  assert.equal(state.rows.value.length, 0)
  assert.equal(state.total.value, 0)
  assert.match(state.error.value, /tenant access revoked/)
})

test('a slower previous object query cannot overwrite the latest history or its detail scope', async () => {
  const complete = [], calls = []
  const state = panel({ id: 'scope', label: '审计', permissions: [], fields: [{ key: 'activeOrderId', label: '订单', required: true }],
    read: params => new Promise(resolve => { calls.push(params.activeOrderId); complete.push(resolve) }),
    detail: async (_, params) => ({ activeOrderId: params.activeOrderId }) })
  state.query.activeOrderId = 'old'
  const previous = state.load()
  state.query.activeOrderId = 'new'
  const latest = state.load()
  complete[1]({ list: [{ id: 2 }], total: 1 })
  await latest
  complete[0]({ list: [{ id: 1 }], total: 1 })
  await previous
  assert.deepEqual(calls, ['old', 'new'])
  assert.equal(state.rows.value[0].id, 2)
  await state.showDetail(state.rows.value[0])
  assert.equal(state.detail.value.activeOrderId, 'new')
})

test('native operation log forwards content/module filters and clears previous evidence on query failure', async () => {
  let sent, fail = false
  const state = setup('SystemOperateLogList.vue', {
    '@/hooks/web/useListColumnFilter': { loadAllListPages() { throw new Error('Column filtering is outside this query test') } },
    '@/utils/formatTime': { dateFormatter() {} },
    '@/utils/download': { default: {} },
    '@/api/system/operatelog': { getOperateLogPage: async query => {
      sent = { ...query }
      if (fail) throw new Error('operation log query unavailable')
      return { list: [record], total: 1 }
    } },
    '@/api/system/user': {}
  }, { useMessage: () => ({}) })
  state.queryParams.action = 'QA'
  state.queryParams.pageNo = 8
  // Exercise the search button handler with content as the only filter.
  // The shared empty-form detector excludes the field named "action".
  await state.handleQuery()
  assert.equal(sent.type, undefined)
  assert.equal(sent.action, 'QA')
  assert.equal(sent.pageNo, 1)
  assert.equal(state.queryParams.action, 'QA')
  state.queryParams.type = '工艺'
  state.queryParams.action = '发布'
  await state.handleQuery()
  assert.equal(sent.type, '工艺')
  assert.equal(sent.action, '发布')
  assert.equal(state.list.value.length, 1)
  fail = true
  await state.getList()
  assert.deepEqual(state.list.value, [])
  assert.equal(state.total.value, 0)
  assert.match(state.loadError.value, /operation log query unavailable/)
})
