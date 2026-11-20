const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const test = require('node:test'), ts = require('typescript'), vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const filename = path.join(root, 'src/views/mes/pro/route/RouteTreeQuickConfig.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const clone = value => JSON.parse(JSON.stringify(value))
const draft = { id: 11, routeId: 1, sourceRouteVersionId: 10, lifecycleStatus: 'DRAFT', versionNo: 'V2', active: false }
const initialLeader = { productionLineId: 1, candidateSourceType: 'USERS', candidateSourceIds: [7], candidateSourceNames: ['原组长'], remark: '原备注' }
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function setup(kind = 'coefficient') {
  const state = vue.reactive({ versions: [], route: { id: 1, activeRouteVersionId: 10 }, creates: 0,
    coefficients: { 10: 1.1, 11: 1.1 }, leaderRows: { 10: [clone(initialLeader)], 11: [clone(initialLeader)] },
    writes: [], denied: [], reads: [] })
  const props = vue.reactive({ kind, route: state.route, disabled: false })
  const out = {}, events = [], errors = [], successes = []
  let dispose
  const api = {
    getRoute: async () => clone(state.route), getRouteVersionList: async () => clone(state.versions),
    createRouteCandidateVersion: async () => { state.creates++; state.versions = [clone(draft)]; return clone(draft) },
    getRouteLossCoefficient: async id => {
      if (state.readFailure) throw Error(state.readFailure)
      state.reads.push(id)
      return { routeVersionId: id, routeSnapshotSha256: `hash-${id}`, lossCoefficient: state.coefficients[id] }
    },
    saveRouteLossCoefficient: async data => {
      if (state.saveFailure) throw Error(state.saveFailure)
      assert.equal(data.routeVersionId, 11)
      state.writes.push(clone(data)); state.coefficients[11] = data.lossCoefficient
    }
  }
  const flowApi = {
    getRouteStartProductionLeaders: async (_route, version) => clone(state.leaderRows[version]),
    getRouteStartProductionLeaderProductionLines: async () => [{ productionLineId: 1, productionLineName: '测试流程' }],
    saveRouteStartProductionLeaders: async data => {
      if (state.saveFailure) throw Error(state.saveFailure)
      assert.equal(data.routeVersionId, 11)
      state.writes.push(clone(data)); state.leaderRows[11] = clone(data.items)
    }
  }
  const queryIds = {}
  vm.runInNewContext(transpile(fs.readFileSync(path.join(root, 'src/utils/routeQueryId.ts'), 'utf8')), { exports: queryIds })
  const entry = {}
  vm.runInNewContext(transpile(fs.readFileSync(path.join(root, 'src/views/mes/pro/route/routeCandidateEntry.ts'), 'utf8')), {
    exports: entry, Error, require: name => name.endsWith('/route') ? { ProRouteApi: api } : queryIds
  })
  vm.runInNewContext(transpile(descriptor.scriptSetup.content + '\nObject.assign(exports,{open,save,visible,ready,error,coefficient,leaders,addLeader,canEdit,saving})'), {
    ...vue, exports: out, Error, defineProps: () => props, defineEmits: () => (...args) => events.push(args),
    onBeforeUnmount: callback => { dispose = callback },
    useMessage: () => ({ error: value => errors.push(value), success: value => successes.push(value) }),
    require: name => {
      if (name === '@/api/mes/pro/route') return { ProRouteApi: api }
      if (name.endsWith('/flowconfig')) return { ProRouteFlowConfigApi: flowApi }
      if (name === './routeCandidateEntry') return entry
      if (name.endsWith('/hasPermi')) return { hasPermission: list => list.every(permission => !state.denied.includes(permission)) }
      if (name === '@/api/system/user') return { getSimpleUserList: async () => [{ id: 7, nickname: '原组长', username: 'old' }, { id: 8, nickname: '新组长', username: 'new' }] }
      if (name === '@/api/system/role') return { getSimpleRoleList: async () => [{ id: 9, name: '组长角色' }] }
      if (name === './routeError') return { resolveRouteOperationErrorMessage: failure => failure.message }
      return {}
    }
  })
  return { ...out, state, props, events, errors, successes, api, dispose: () => dispose() }
}

test('quick editors and parent compile', () => {
  for (const file of [filename, path.join(root, 'src/views/mes/pro/route/RouteTreePanel.vue')]) {
    const { descriptor: sfc, errors } = parse(fs.readFileSync(file, 'utf8'))
    assert.deepEqual(errors, [])
    const script = compileScript(sfc, { id: file })
    assert.deepEqual(compileTemplate({ source: sfc.template.content, filename: file, id: file, compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  }
})

test('opening, cancelling and unchanged save do not create a draft or write', async () => {
  for (const kind of ['coefficient', 'leaders']) {
    const h = setup(kind); await h.open()
    assert.equal(h.ready.value, true)
    assert.equal(h.state.creates, 0)
    await h.save()
    assert.equal(h.visible.value, false)
    assert.equal(h.state.creates, 0)
    assert.equal(h.state.writes.length, 0)
    await h.open(); h.visible.value = false
    assert.equal(h.events.at(-1)[1], false)
  }
})

test('coefficient saves only to a new or existing draft using its current snapshot hash', async () => {
  for (const existing of [false, true]) {
    const h = setup(); if (existing) h.state.versions = [draft]
    await h.open(); h.coefficient.value = 1.25; await h.save()
    assert.equal(h.state.creates, existing ? 0 : 1)
    assert.equal(h.state.writes[0].routeVersionId, 11)
    assert.equal(h.state.writes[0].expectedRouteSnapshotSha256, 'hash-11')
    assert.equal(h.state.coefficients[10], 1.1)
    assert.equal(h.state.coefficients[11], 1.25)
    assert.equal(h.events.at(-1)[0], 'changed')
    assert.match(h.successes[0], /发布/)
  }
})

test('leaders support accounts and roles and preserve the responsible range and remark', async () => {
  for (const source of ['USERS', 'ROLE']) {
    const h = setup('leaders'); await h.open()
    h.leaders.value[0].candidateSourceType = source
    h.leaders.value[0].candidateSourceIds = source === 'USERS' ? [8] : [9]
    await h.save()
    const saved = h.state.writes[0]
    assert.equal(saved.routeId, 1)
    assert.equal(saved.routeVersionId, 11)
    assert.equal(saved.items[0].productionLineId, '1')
    assert.equal(saved.items[0].remark, '原备注')
    assert.equal(saved.items[0].candidateSourceNames[0], source === 'USERS' ? '新组长（new）' : '组长角色')
    assert.equal(h.state.leaderRows[10][0].candidateSourceIds[0], 7)
  }
})

test('invalid coefficients and incomplete or stale leader selections fail before creating a draft', async () => {
  for (const value of [undefined, NaN, 0.9]) {
    const h = setup(); await h.open(); h.coefficient.value = value; await h.save()
    assert.equal(h.state.creates, 0)
    assert.equal(h.state.writes.length, 0)
    assert.match(h.error.value, /大于等于1/)
  }
  for (const mutate of [h => { h.leaders.value = [] }, h => { h.leaders.value[0].candidateSourceIds = [] },
    h => { h.leaders.value[0].productionLineId = '99' }, h => { h.leaders.value[0].candidateSourceIds = [999] }]) {
    const h = setup('leaders'); await h.open(); mutate(h); await h.save()
    assert.equal(h.state.creates, 0)
    assert.equal(h.state.writes.length, 0)
    assert.ok(h.error.value)
  }
})

test('approval, ambiguous drafts and outdated draft sources block editing', async () => {
  for (const versions of [[{ ...draft, lifecycleStatus: 'PENDING_APPROVAL' }], [{ ...draft, lifecycleStatus: 'READY_TO_PUBLISH' }],
    [draft, { ...draft, id: 12 }], [{ ...draft, sourceRouteVersionId: 9 }]]) {
    const h = setup(); h.state.versions = versions; await h.open()
    assert.equal(h.ready.value, false)
    assert.ok(h.error.value)
    assert.equal(h.state.creates, 0)
  }
})

test('concurrent configuration or active-version changes cannot be overwritten', async () => {
  for (const kind of ['coefficient', 'leaders']) {
    const h = setup(kind); h.state.versions = [draft]; await h.open()
    if (kind === 'coefficient') { h.coefficient.value = 1.2; h.state.coefficients[11] = 1.3 }
    else { h.leaders.value[0].candidateSourceIds = [8]; h.state.leaderRows[11] = [] }
    await h.save()
    assert.equal(h.state.writes.length, 0)
    assert.match(h.error.value, /其他操作修改/)
  }
  const h = setup(); h.state.versions = [draft]; await h.open(); h.coefficient.value = 1.2
  h.state.route.activeRouteVersionId = 12
  h.state.versions = [{ ...draft, sourceRouteVersionId: 12 }]
  await h.save()
  assert.equal(h.state.writes.length, 0)
  assert.match(h.error.value, /版本已变化/)
})

test('permissions, active operations and disposed windows prevent writes', async () => {
  for (const kind of ['coefficient', 'leaders']) {
    const h = setup(kind); h.state.denied = [kind === 'coefficient' ? 'mes:pro-route:version-update' : 'mes:pro-route:batch-record-config:update']
    await h.open(); assert.equal(h.visible.value, false)
  }
  const h = setup(); await h.open(); h.coefficient.value = 1.2; h.props.disabled = true; await h.save()
  assert.equal(h.state.writes.length, 0)
  h.props.disabled = false; h.dispose(); await h.save()
  assert.equal(h.state.writes.length, 0)
})

test('read/save errors remain explicit and never report success', async () => {
  const h = setup(); h.state.readFailure = '读取失败'; await h.open()
  assert.equal(h.ready.value, false); assert.equal(h.error.value, '读取失败')
  const s = setup(); await s.open(); s.coefficient.value = 1.2; s.state.saveFailure = '保存失败'; await s.save()
  assert.equal(s.error.value, '保存失败')
  assert.equal(s.errors.at(-1), '保存失败')
  assert.equal(s.successes.length, 0)
  assert.equal(s.saving.value, false)
})
