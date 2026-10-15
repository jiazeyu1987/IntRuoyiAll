const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const src = path.resolve('src'), read = p => fs.readFileSync(path.join(src, p), 'utf8')
const clone = v => JSON.parse(JSON.stringify(v)), attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const P = '9007199254740993', U = '9223372036854775707'
const deferred = () => { let resolve; const promise = new Promise(r => resolve = r); return { promise, resolve } }
const tick = async () => { for (let n = 0; n < 15; n++) { await Promise.resolve(); await vue.nextTick() } }
const TOKEN = 'a'.repeat(64)
const project = (id = P, leader = U) => ({ id, projectLeaderUserId: leader, defaultAttributesJson: JSON.stringify(attrs), configurationToken: TOKEN })
const account = (id = U) => ({ id, nickname: '正式负责人', username: 'leader' })
const config = { configured: true, reviewerUserId: U, reviewerUsername: 'reviewer', reviewerNickname: '正式审核人', enabled: true }
function runtime({ roles = ['doc_control'], permissions = ['dcc:project-code:update'], actorId = '6', transport = {}, users = async () => [account()] } = {}) {
  const store = vue.reactive({ roles, permissions: new Set(permissions), isSetUser: true, user: { id: actorId, username: 'operator' }, get getRoles() { return this.roles }, get getPermissions() { return this.permissions }, get getIsSetUser() { return this.isSetUser }, get getUser() { return this.user } })
  const calls = [], feedback = [], cache = new Map()
  const request = Object.fromEntries(['get', 'put'].map(method => [method, async q => { calls.push([method, clone(q)]); return transport[method] ? transport[method](q) : (method === 'get' ? config : true) }]))
  const load = p => {
    if (cache.has(p)) return cache.get(p)
    const exports = {}; cache.set(p, exports)
    const require = id => {
      if (id === 'vue') return vue
      if (id === '@/config/axios') return { default: request }
      if (id === '@/store/modules/user') return { useUserStore: () => store }
      if (id === '@/utils/auth') return { getTenantId: () => '1', getVisitTenantId: () => undefined }
      if (id === '@/hooks/web/useCache') return { CACHE_KEY: { USER: 'USER' }, useCache: () => ({ wsCache: { get: () => ({ roles: store.roles }) } }) }
      if (id === '@/api/system/user') return { getSimpleUserList: users }
      if (id === 'element-plus') return { ElMessageBox: { confirm: transport.confirm || (async () => true) } }
      if (id.endsWith('.vue')) return { name: 'ImportedFields', render: () => vue.h('div') }
      let next = id.startsWith('@/') ? id.slice(2) : path.posix.normalize(path.posix.join(path.posix.dirname(p), id))
      if (!fs.existsSync(path.join(src, next + '.ts'))) next += '/index'
      return load(next + '.ts')
    }
    new Function('exports', 'require', 'useI18n', 'useMessage', ts.transpileModule(read(p), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText)(exports, require, () => ({ t: x => x }), () => ({ success: s => feedback.push(s), warning: s => feedback.push(s), info: s => feedback.push(s), error: s => feedback.push(s) }))
    return exports
  }
  return { load, calls, store, feedback }
}
function mount(name, options) {
  const rt = runtime(options), file = options?.componentPath || `views/dcc/controlled-file/basic-data/components/${name}.vue`, { descriptor } = parse(read(file)), compiled = compileScript(descriptor, { id: name })
  assert.equal(parse(read(file)).errors.length, 0)
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: name, compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
  const exports = {}, dependencies = id => {
    if (id === 'vue') return vue
    if (id === 'element-plus') return { ElMessageBox: { confirm: options?.transport?.confirm || (async () => true) } }
    if (id === '@/store/modules/user') return { useUserStore: () => rt.store }
    if (id === '@/utils/auth') return { getTenantId: () => '1', getVisitTenantId: () => undefined }
    if (id === '@/api/system/user') return { getSimpleUserList: options?.users || (async () => [account()]) }
    if (id.endsWith('.vue')) return {}
    let p = id.startsWith('@/') ? id.slice(2) : path.posix.normalize(path.posix.join(path.posix.dirname(file), id))
    if (!fs.existsSync(path.join(src, p + '.ts'))) p += '/index'
    return rt.load(p + '.ts')
  }
  new Function('exports', 'require', 'useMessage', ts.transpileModule(compiled.content, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText)(exports, dependencies, () => ({ success: s => rt.feedback.push(s), warning: s => rt.feedback.push(s), info: s => rt.feedback.push(s), error: s => rt.feedback.push(s) }))
  const component = exports.default; component.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: t => ({ t, children: [] }), createText: t => ({ t }), createComment: t => ({ t }), insert: (c, p) => p.children.push(c), remove() {}, setText() {}, setElementText() {}, parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const events = [], app = renderer.createApp({ render: () => vue.h(component, { ...options?.props, onSaved: id => events.push(['saved', id]), onChanged: data => events.push(['changed', data]) }) }), host = app.mount({ children: [] })
  return { ...rt, app, events, state: host.$.subTree.component.setupState }
}
test('project attribute API preserves exact project/leader Long strings and rejects unsafe identities before PUT', async () => {
  const rt = runtime(), api = rt.load('api/dcc/controlledFile/projectAttributes.ts')
  await api.configureProjectAttributes(P, { projectLeaderUserId: U, defaultAttributes: attrs, changeReason: ' 修改属性 ', expectedConfigurationToken: TOKEN })
  assert.equal(rt.calls[0][1].url, `/dcc/project-codes/${P}/attributes/configuration`); assert.equal(rt.calls[0][1].data.projectLeaderUserId, U)
  for (const [id, leader] of [[9007199254740992, U], [P, 9007199254740992], [P, '9223372036854775808'], [P, ' 6']])
    await assert.rejects(api.configureProjectAttributes(id, { projectLeaderUserId: leader, defaultAttributes: attrs, changeReason: '修改' }), /身份|精度/)
  assert.equal(rt.calls.length, 1)
})
test('project configuration normalizes account and project IDs to strings and only saves formal selected account', async () => {
  const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => project() } })
  try { await p.state.open(P); assert.equal(p.state.projectId, P); assert.equal(p.state.leaderId, U); assert.equal(p.state.users[0].id, U); p.state.changeReason = '修改'; await p.state.save(); assert.equal(p.calls.filter(c => c[0] === 'put')[0][1].data.projectLeaderUserId, U); assert.deepEqual(p.events, [['saved', P]]) }
  finally { p.app.unmount() }
})
test('safe numeric responses become exact string option values and saved IDs through the actual SFC and API', async () => {
  const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => project(20, 7) }, users: async () => [account(7)] })
  try {
    await p.state.open(20); assert.equal(p.state.projectId, '20'); assert.equal(p.state.leaderId, '7'); assert.equal(p.state.users[0].id, '7')
    p.state.changeReason = '保存原值'; await p.state.save()
    assert.equal(p.calls.find(c => c[0] === 'put')[1].data.projectLeaderUserId, '7'); assert.deepEqual(p.events, [['saved', '20']])
  } finally { p.app.unmount() }
})
test('unsafe project/leader, foreign project response or duplicated directory never make attribute configuration writable', async () => {
  const variants = [
    { id: 9007199254740992, response: project() },
    { response: project('22') }, { response: project(P, 9007199254740992) },
    { response: project(), users: async () => [account(U), account(U)] },
    { response: project(), users: async () => [account(9007199254740992)] }
  ]
  for (const v of variants) {
    const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => v.response }, users: v.users })
    try { await p.state.open(v.id ?? P); assert.equal(p.state.ready, false); assert.ok(p.state.error); p.state.changeReason = '修改'; await p.state.save(); assert.equal(p.calls.filter(c => c[0] === 'put').length, 0) }
    finally { p.app.unmount() }
  }
})
test('attribute configuration rejects invalid directory and never claims a false receipt succeeded', async () => {
  for (const [response, users] of [[project(), async () => [{ ...account(), status: 1 }]]]) {
    const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => response }, users })
    try { await p.state.open(P); assert.equal(p.state.ready, false); assert.ok(p.state.error); await p.state.save(); assert.equal(p.calls.filter(c => c[0] === 'put').length, 0) }
    finally { p.app.unmount() }
  }
  const failed = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => project(), put: async () => false } })
  try { await failed.state.open(P); failed.state.changeReason = '保存'; await failed.state.save(); assert.equal(failed.events.length, 0); assert.equal(failed.state.visible, true); assert.match(failed.state.error, /未确认成功/); assert.equal(failed.state.leaderId, U) }
  finally { failed.app.unmount() }
})
test('inactive or missing previous leader permits explicit replacement without guessing an account', async () => {
  const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => project(P, '7') } })
  try {
    await p.state.open(P)
    assert.equal(p.state.ready, true)
    assert.equal(p.state.leaderId, undefined)
    assert.match(p.state.repairNotice, /负责人/)
    p.state.changeReason = '更换已失效负责人'; await p.state.save()
    assert.equal(p.calls.filter(c => c[0] === 'put').length, 0)
    p.state.leaderId = U; await p.state.save()
    const write = p.calls.find(c => c[0] === 'put')[1]
    assert.equal(write.data.projectLeaderUserId, U)
    assert.equal(write.data.expectedConfigurationToken, TOKEN)
  } finally { p.app.unmount() }
})
test('malformed saved attributes remain visible as a repair requirement and require complete new values', async () => {
  for (const raw of ['{invalid', JSON.stringify({ ...attrs, targetMarkets: [] })]) {
    const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => ({ ...project(), defaultAttributesJson: raw }) } })
    try {
      await p.state.open(P); assert.equal(p.state.ready, true)
      assert.match(p.state.repairNotice, /属性/)
      assert.deepEqual(clone(p.state.attributes), { targetMarkets: [] })
      p.state.changeReason = '人工修正旧属性'; await p.state.save()
      assert.equal(p.calls.filter(c => c[0] === 'put').length, 0)
      p.state.attributes = clone(attrs); await p.state.save()
      assert.equal(p.calls.find(c => c[0] === 'put')[1].data.expectedConfigurationToken, TOKEN)
    } finally { p.app.unmount() }
  }
})
test('configuration read rejects missing baseline and save conflict preserves current manual edits', async () => {
  const missing = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => ({ ...project(), configurationToken: undefined }) } })
  try { await missing.state.open(P); assert.equal(missing.state.ready, false); assert.ok(missing.state.error) }
  finally { missing.app.unmount() }
  const conflict = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => project(), put: async () => { throw new Error('项目配置已变化，请重新读取') } } })
  try {
    await conflict.state.open(P); conflict.state.attributes = { ...attrs, targetMarkets: ['FDA'] }; conflict.state.changeReason = '修改'
    await conflict.state.save(); assert.equal(conflict.state.visible, true); assert.equal(conflict.events.length, 0)
    assert.match(conflict.state.error, /已变化/); assert.deepEqual(clone(conflict.state.attributes.targetMarkets), ['FDA'])
  } finally { conflict.app.unmount() }
})
test('manipulated selected account or unsafe project reference fails locally with zero writes', async () => {
  for (const mutate of [p => p.state.leaderId = 9007199254740992, p => p.state.leaderId = '7', p => p.state.projectId = '22']) {
    const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => project() } })
    try { await p.state.open(P); p.state.changeReason = '修改'; mutate(p); await p.state.save(); assert.equal(p.calls.filter(c => c[0] === 'put').length, 0); assert.ok(p.state.error) }
    finally { p.app.unmount() }
  }
})
test('closed attribute configuration drops late read and cannot be saved; switching project drops earlier response', async () => {
  const pending = deferred(), p = mount('ProjectAttributeConfigurationDialog', { transport: { get: q => q.url.includes(P) ? pending.promise : Promise.resolve(project('22', '7')) }, users: async () => [account(), account('7')] })
  try {
    const first = p.state.open(P); p.state.visible = false; await tick(); pending.resolve(project()); await first
    assert.equal(p.state.ready, false); assert.equal(p.state.leaderId, undefined); assert.equal(p.state.users.length, 0)
    p.state.changeReason = 'closed'; await p.state.save(); assert.equal(p.calls.filter(c => c[0] === 'put').length, 0)
    await p.state.open('22'); assert.equal(p.state.projectId, '22'); assert.equal(p.state.leaderId, '7')
  } finally { p.app.unmount() }
})
test('late old project write cannot close or emit success into a newly opened project', async () => {
  const pending = deferred(), p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async q => project(q.url.includes(P) ? P : '22'), put: () => pending.promise } })
  try { await p.state.open(P); p.state.changeReason = '保存A'; const saving = p.state.save(); await p.state.open('22'); pending.resolve(true); await saving; assert.equal(p.state.visible, true); assert.equal(p.state.projectId, '22'); assert.equal(p.state.loading, false); assert.equal(p.events.length, 0) }
  finally { p.app.unmount() }
})
test('project switching and unmounting discard old read or write results without publishing old attributes', async () => {
  const read = deferred(), p = mount('ProjectAttributeConfigurationDialog', { transport: { get: q => q.url.includes(P) ? read.promise : Promise.resolve({ ...project('22', '7'), defaultAttributesJson: JSON.stringify({ ...attrs, targetMarkets: ['FDA'] }) }) }, users: async () => [account(), account('7')] })
  try {
    const first = p.state.open(P); await p.state.open('22'); read.resolve(project()); await first
    assert.equal(p.state.projectId, '22'); assert.equal(p.state.leaderId, '7'); assert.deepEqual(clone(p.state.attributes.targetMarkets), ['FDA'])
  } finally { p.app.unmount() }
  const late = deferred(), unmounted = mount('ProjectAttributeConfigurationDialog', { transport: { get: () => late.promise } })
  const opening = unmounted.state.open(P); unmounted.app.unmount(); late.resolve(project()); await opening
  assert.equal(unmounted.state.ready, false); assert.equal(unmounted.state.users.length, 0); assert.equal(unmounted.events.length, 0)
})
test('same-project close/reopen invalidates old saved receipt and never overwrites or unlocks a later save', async () => {
  const first = deferred(), second = deferred(); let writes = 0
  const p = mount('ProjectAttributeConfigurationDialog', { transport: { get: async () => project(), put: () => (++writes === 1 ? first.promise : second.promise) } })
  try {
    await p.state.open(P); p.state.changeReason = '第一次'; const old = p.state.save(); p.state.visible = false; await p.state.open(P)
    p.state.changeReason = '第二次'; const current = p.state.save(); first.resolve(true); await old
    assert.equal(p.state.visible, true); assert.equal(p.state.loading, true); assert.equal(p.state.changeReason, '第二次'); assert.equal(p.events.length, 0)
    second.resolve(true); await current; assert.deepEqual(p.events, [['saved', P]])
  } finally { p.app.unmount() }
})
test('attribute wrapper freezes actual attributes and validates reason before sending any request', async () => {
  const pending = deferred(), rt = runtime({ transport: { put: () => pending.promise } }), api = rt.load('api/dcc/controlledFile/projectAttributes.ts')
  const actual = clone(attrs), writing = api.configureProjectAttributes(P, { projectLeaderUserId: U, defaultAttributes: actual, changeReason: '原因', expectedConfigurationToken: TOKEN })
  actual.targetMarkets.push('FDA'); assert.deepEqual(rt.calls[0][1].data.defaultAttributes.targetMarkets, ['CE']); pending.resolve(true); await writing
  for (const changeReason of ['', ' ', 'a'.repeat(501)]) await assert.rejects(api.configureProjectAttributes(P, { projectLeaderUserId: U, defaultAttributes: attrs, changeReason }), /原因/)
  assert.equal(rt.calls.length, 1)
})
test('configuration API refuses missing or malformed baseline before any write', async () => {
  const rt = runtime(), api = rt.load('api/dcc/controlledFile/projectAttributes.ts')
  for (const token of [undefined, '', 'f'.repeat(63), 'Z'.repeat(64)]) {
    await assert.rejects(api.configureProjectAttributes(P, { projectLeaderUserId: U, defaultAttributes: attrs, changeReason: '修改', expectedConfigurationToken: token }), /基线|配置/)
  }
  assert.equal(rt.calls.length, 0)
})
test('actual attribute component retains valid project after failed switch or restore and still rejects first-load failure', async () => {
  let fail = false
  const p = mount('ProjectApplicationAttributes', {
    componentPath: 'views/dcc/controlled-file/project-attributes/ProjectApplicationAttributes.vue', props: { action: 'UPLOAD' },
    transport: { get: async () => { if (fail) throw new Error('读取失败'); return clone(attrs) } }
  })
  try {
    assert.equal(await p.state.selectProject('5'), true)
    p.state.edit({ ...attrs, targetMarkets: ['FDA'] }); fail = true
    assert.equal(await p.state.selectProject('6'), false)
    const stateApi = p.load('views/dcc/controlled-file/project-attributes/state.ts')
    assert.equal(stateApi.buildSnapshot(p.state.state).projectId, '5')
    assert.equal(p.state.state.error, ''); assert.equal(p.state.state.requestError, '读取失败')
    await p.state.restore()
    assert.deepEqual(clone(stateApi.buildSnapshot(p.state.state).actual.targetMarkets), ['FDA'])
  } finally { p.app.unmount() }
  const empty = mount('ProjectApplicationAttributes', {
    componentPath: 'views/dcc/controlled-file/project-attributes/ProjectApplicationAttributes.vue', props: { action: 'UPLOAD' },
    transport: { get: async () => { throw new Error('首次读取失败') } }
  })
  try {
    assert.equal(await empty.state.selectProject('5'), false)
    assert.throws(() => empty.load('views/dcc/controlled-file/project-attributes/state.ts').buildSnapshot(empty.state.state), /首次读取失败/)
  } finally { empty.app.unmount() }
})
test('real permission helper super_admin bypass does not authorize reviewer business-role maintenance', async () => {
  const p = mount('ProjectReviewerConfiguration', { roles: ['super_admin'], permissions: ['*:*:*'] })
  try {
    assert.equal(p.load('utils/permission.ts').checkRole(['doc_control']), true, 'reproduce real shared helper bypass')
    assert.equal(p.state.canConfigure, false); await p.state.open(); assert.equal(p.calls.length, 0); assert.equal(p.state.visible, false)
    p.state.selectedUserId = U; p.state.reason = '不能绕过'; await p.state.save(); assert.equal(p.calls.filter(c => c[0] === 'put').length, 0)
  } finally { p.app.unmount() }
})
test('reviewer maintenance requires actual doc_control AND permission, preserving exact selected Long', async () => {
  for (const permissions of [[], ['dcc:project-code:update']]) {
    const p = mount('ProjectReviewerConfiguration', { permissions, transport: { get: async () => config, put: async () => config } })
    try { await p.state.open(); if (!permissions.length) { assert.equal(p.calls.length, 0); assert.equal(p.state.canConfigure, false) } else { assert.equal(p.state.canConfigure, true); p.state.reason = '明确配置'; await p.state.save(); const put = p.calls.find(c => c[0] === 'put'); assert.equal(put[1].data.reviewerUserId, U); assert.equal(p.events.length, 1) } }
    finally { p.app.unmount() }
  }
})
test('reviewer confirmation rechecks revoked doc_control role, permission and actor context with zero writes', async () => {
  for (const change of [s => s.roles = ['super_admin'], s => s.permissions.clear(), s => s.user.id = '7']) {
    const pending = deferred(), p = mount('ProjectReviewerConfiguration', { transport: { get: async () => config, put: async () => config, confirm: () => pending.promise } })
    try { await p.state.open(); p.state.reason = '修改'; const saving = p.state.save(); await tick(); change(p.store); pending.resolve(true); await saving; assert.equal(p.calls.filter(c => c[0] === 'put').length, 0); assert.equal(p.events.length, 0) }
    finally { p.app.unmount() }
  }
})
