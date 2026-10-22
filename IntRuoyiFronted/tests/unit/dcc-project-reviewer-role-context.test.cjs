const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const file = 'src/views/dcc/controlled-file/basic-data/components/ProjectReviewerConfiguration.vue'
const actorId = '9223372036854775707', selectedId = '9223372036854775709'
const config = { configured: true, enabled: true, reviewerUserId: selectedId, reviewerUsername: 'reviewer', reviewerNickname: '审核人员' }
const load = (path, imports, globals = {}) => {
  const exports = {}
  const source = fs.readFileSync(path, 'utf8')
  const names = Object.keys(globals)
  new Function('exports', 'require', ...names, ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText)(exports, imports, ...Object.values(globals))
  return exports
}
const node = (type, text = '') => ({ type, text, children: [], props: {}, parent: null })
const renderer = vue.createRenderer({ createElement: type => node(type), createText: text => node('#text', text), createComment: text => node('#comment', text),
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] }, patchProp: (n, key, _old, value) => { n.props[key] = value },
  insert(n, parent, anchor) { if (n.parent) { const old = n.parent.children.indexOf(n); if (old >= 0) n.parent.children.splice(old, 1) } n.parent = parent; const at = anchor ? parent.children.indexOf(anchor) : -1; if (at < 0) parent.children.push(n); else parent.children.splice(at, 0, n) },
  remove(n) { if (n.parent) { const at = n.parent.children.indexOf(n); if (at >= 0) n.parent.children.splice(at, 1) } n.parent = null }, parentNode: n => n.parent,
  nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1], setScopeId() {}, insertStaticContent(text, parent) { const n = node('#static', text); n.parent = parent; parent.children.push(n); return [n, n] } })
const nodes = (n, predicate) => [...(predicate(n) ? [n] : []), ...n.children.flatMap(child => nodes(child, predicate))]
const text = n => n.text + n.children.map(text).join('')
const settle = async () => { for (let n = 0; n < 7; n++) { await Promise.resolve(); await vue.nextTick() } }
const defer = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
function mount({ roles = ['doc_control'], permissions = ['dcc:project-code:update'], actor = actorId,
  confirm = async () => {}, read = async () => config, save = async () => config, directory = async () => [{ id: selectedId, nickname: '审核人员', username: 'reviewer' }] } = {}) {
  const user = vue.reactive({ roles, permissions: new Set(permissions), isSetUser: true, user: { id: actor, username: 'operator' },
    get getRoles() { return this.roles }, get getPermissions() { return this.permissions }, get getIsSetUser() { return this.isSetUser }, get getUser() { return this.user } })
  const tenant = { id: '1' }, calls = { reads: [], puts: [], users: 0, confirmations: [], successes: [], events: [] }
  const i18n = () => ({ t: value => value })
  // Run unchanged production permission code, substituting only its actual store/cache providers.
  const permissionDirective = load('src/directives/permission/hasPermi.ts', name => {
    if (name === '@/store/modules/user') return { useUserStore: () => user }; throw new Error(name)
  }, { useI18n: i18n })
  const permission = load('src/utils/permission.ts', name => {
    if (name === '@/hooks/web/useCache') return { CACHE_KEY: { USER: 'user' }, useCache: () => ({ wsCache: { get: () => ({ roles: user.roles }) } }) }
    if (name === '@/directives/permission/hasPermi') return permissionDirective
    throw new Error(name)
  }, { useI18n: i18n })
  const model = load('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts', name => { throw new Error(name) })
  const api = load('src/api/dcc/controlledFile/projectProductRequests.ts', name => {
    if (name.endsWith('/project-reviewer')) return model
    if (name === '@/config/axios') return { default: { get: async req => { calls.reads.push(req); return read() }, put: async req => { calls.puts.push(req); return save() } } }
    throw new Error(name)
  })
  const { descriptor } = parse(fs.readFileSync(file, 'utf8'), { filename: file })
  const compile = inline => {
    const exports = {}, compiled = compileScript(descriptor, { id: 'reviewer-context', inlineTemplate: inline })
    new Function('exports', 'require', 'useMessage', ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText)(exports, name => {
      if (name === 'vue') return vue
      if (name === 'element-plus') return { ElMessageBox: { confirm: async (...args) => { calls.confirmations.push(args); return confirm() } } }
      if (name === '@/utils/permission') return permission
      if (name === '@/store/modules/user') return { useUserStore: () => user }
      if (name === '@/utils/auth') return { getTenantId: () => tenant.id, getVisitTenantId: () => undefined }
      if (name === '@/api/system/user') return { getSimpleUserList: async () => { calls.users++; return directory() } }
      if (name === '@/api/dcc/controlledFile/projectProductRequests') return api
      if (name === './project-reviewer') return model
      throw new Error(name)
    }, () => ({ success: value => calls.successes.push(value), warning: value => calls.successes.push(value) }))
    return exports.default
  }
  const rendered = compile(true), root = node('root'), app = renderer.createApp({ render: () => vue.h(rendered, { onChanged: value => calls.events.push(value) }) })
  app.config.warnHandler = () => {}
  for (const tag of ['el-button', 'el-dialog', 'el-form', 'el-form-item', 'el-select', 'el-option', 'el-alert', 'el-input']) app.component(tag, { inheritAttrs: false, setup: (_p, ctx) => () => vue.h(tag, ctx.attrs, [ctx.slots.default?.(), ctx.slots.footer?.()]) })
  app.directive('loading', {}); app.mount(root)
  // A second actual setup exposes unmodified handlers to test bypasses/reentrancy; no behavior helper replaces them.
  const handled = compile(false); handled.render = () => vue.h('section')
  const handlerApp = renderer.createApp({ render: () => vue.h(handled, { onChanged: value => calls.events.push(value) }) })
  const handlerHost = handlerApp.mount(node('root')), state = handlerHost.$.subTree.component.setupState
  return { app, handlerApp, root, state, user, tenant, permission, calls, unmount() { app.unmount(); handlerApp.unmount() } }
}

test('real shared helper promotes super_admin but the public reviewer entry and handlers require actual doc_control', async () => {
  const view = mount({ roles: ['super_admin'] })
  try {
    assert.equal(view.permission.checkRole(['doc_control']), true, 'RED must use actual shared promotion behavior')
    await settle(); assert.equal(nodes(view.root, n => n.props['data-testid'] === 'dcc-project-reviewer-config-open').length, 0)
    await view.state.open(); await view.state.save()
    assert.equal(view.calls.reads.length, 0); assert.equal(view.calls.users, 0); assert.equal(view.calls.puts.length, 0)
  } finally { view.unmount() }
})

test('missing update permission or uninitialized login has no entry, read or write', async () => {
  for (const alter of [view => { view.user.permissions = new Set() }, view => { view.user.isSetUser = false }]) {
    const view = mount(); try { alter(view); await settle(); await view.state.open(); await view.state.save()
      assert.equal(nodes(view.root, n => n.props['data-testid'] === 'dcc-project-reviewer-config-open').length, 0)
      assert.equal(view.calls.reads.length, 0); assert.equal(view.calls.users, 0); assert.equal(view.calls.puts.length, 0)
    } finally { view.unmount() }
  }
})

test('logged-in doc_control and update permission read formal enabled accounts and save acknowledged exact Long', async () => {
  const view = mount(); try {
    await settle(); assert.equal(nodes(view.root, n => n.props['data-testid'] === 'dcc-project-reviewer-config-open').length, 1)
    await view.state.open(); view.state.reason = ' 配置实际审核人 '; await view.state.save()
    assert.equal(view.calls.puts.length, 1); assert.deepEqual(view.calls.puts[0].data, { reviewerUserId: selectedId, reason: '配置实际审核人' })
    assert.equal(view.calls.events.length, 1); assert.equal(view.calls.successes.length, 1); assert.equal(view.state.visible, false)
  } finally { view.unmount() }
})

test('permission or actor changes while confirmation waits prevent any write, including loss and restore', async () => {
  for (const change of [view => { view.user.roles = ['super_admin'] }, view => { view.user.permissions = new Set() },
    view => { view.user.user.id = '8' }, view => { view.user.permissions = new Set(); view.user.permissions = new Set(['dcc:project-code:update']) }]) {
    const pending = defer(), view = mount({ confirm: () => pending.promise })
    try { await view.state.open(); view.state.reason = '确认期间变化'; const saving = view.state.save()
      await settle(); assert.equal(view.calls.confirmations.length, 1); change(view); pending.resolve(); await saving
      assert.equal(view.calls.puts.length, 0); assert.equal(view.calls.events.length, 0); assert.equal(view.calls.successes.length, 0)
    } finally { view.unmount() }
  }
})

test('switching the actor during a directory/config read closes old input and ignores its late response', async () => {
  const pending = defer(), view = mount({ read: () => pending.promise })
  try { const opening = view.state.open(); view.user.user.id = '8'; await settle(); pending.resolve(config); await opening
    assert.equal(view.state.visible, false); assert.equal(view.state.configuration, undefined); assert.equal(view.state.users.length, 0); assert.equal(view.state.loading, false)
  } finally { view.unmount() }
})

test('closing or unmounting during confirmation prevents a late acknowledged write', async () => {
  for (const close of [view => { view.state.visible = false }, view => { view.handlerApp.unmount() }]) {
    const pending = defer(), view = mount({ confirm: () => pending.promise })
    try { await view.state.open(); view.state.reason = '关闭前填写'; const saving = view.state.save(); close(view); pending.resolve(); await saving
      assert.equal(view.calls.puts.length, 0); assert.equal(view.calls.events.length, 0)
    } finally { view.app.unmount(); if (view.handlerApp._container) view.handlerApp.unmount() }
  }
})

test('late save success and failure cannot close or overwrite a newly opened actor dialog', async () => {
  for (const reject of [false, true]) {
    const pending = defer(), view = mount({ save: () => pending.promise })
    try { await view.state.open(); view.state.reason = '旧账号写入'; const saving = view.state.save(); await settle(); assert.equal(view.calls.puts.length, 1)
      view.user.user.id = '8'; await settle(); await view.state.open(); view.state.reason = '新账号输入'; view.state.error = '新账号提示'; view.state.busy = true
      reject ? pending.reject(new Error('旧保存失败')) : pending.resolve(config); await saving
      assert.equal(view.state.visible, true); assert.equal(view.state.reason, '新账号输入'); assert.equal(view.state.error, '新账号提示'); assert.equal(view.state.busy, true)
      assert.equal(view.calls.events.length, 0); assert.equal(view.calls.successes.length, 0)
    } finally { view.unmount() }
  }
})

test('real rendered entry click opens official config and directory only for current allowed account', async () => {
  const view = mount(); try {
    await settle(); const entry = nodes(view.root, n => n.props['data-testid'] === 'dcc-project-reviewer-config-open')[0]
    await entry.props.onClick(); await settle()
    assert.equal(view.calls.reads.length, 1); assert.equal(view.calls.users, 1)
    assert.ok(text(view.root).includes('审核人员'))
    view.user.roles = ['super_admin']; await settle()
    assert.equal(nodes(view.root, n => n.props['data-testid'] === 'dcc-project-reviewer-config-open').length, 0)
  } finally { view.unmount() }
})

test('directory/network failure stays visible and cannot submit from partial config facts', async () => {
  const view = mount({ directory: async () => { throw new Error('启用账号目录网络失败') } })
  try { await view.state.open();view.state.selectedUserId=selectedId;view.state.reason='手填不能绕目录';await view.state.save()
    assert.match(view.state.error,/目录网络失败/);assert.equal(view.calls.puts.length,0);assert.equal(view.state.ready,false)
  } finally { view.unmount() }
})

test('form validation failure can be corrected and real cancel preserves current selected account and reason', async () => {
  const view = mount({confirm:async()=>{throw 'cancel'}})
  try { await view.state.open();await view.state.save();assert.match(view.state.error,/原因/)
    view.state.reason='保留实际输入';await view.state.save();assert.equal(view.calls.confirmations.length,1)
    assert.equal(view.state.selectedUserId,selectedId);assert.equal(view.state.reason,'保留实际输入');assert.equal(view.state.visible,true);assert.equal(view.calls.puts.length,0)
  } finally { view.unmount() }
})

test('closing then reopening while old read waits keeps the newly loaded directory and does not remain busy', async () => {
  const pending=defer();let reads=0
  const view=mount({read:()=>++reads===1?pending.promise:Promise.resolve({...config,reviewerNickname:'当前新配置'})})
  try {const old=view.state.open();view.state.visible=false;await view.state.open();view.state.reason='新窗口输入'
    pending.resolve({...config,reviewerNickname:'旧配置'});await old
    assert.equal(view.state.configuration.reviewerNickname,'当前新配置');assert.equal(view.state.reason,'新窗口输入');assert.equal(view.state.loading,false)
  }finally{view.unmount()}
})

test('account switch without reopening suppresses old saved event and unmount suppresses late read error', async () => {
  const pending=defer(),view=mount({save:()=>pending.promise})
  try{await view.state.open();view.state.reason='原账号配置';const old=view.state.save();await settle();view.user.user.id='8';pending.resolve(config);await old
    assert.equal(view.calls.events.length,0);assert.equal(view.calls.successes.length,0);assert.equal(view.state.visible,false);assert.equal(view.state.users.length,0)
  }finally{view.unmount()}
  const read=defer(),unmounted=mount({read:()=>read.promise})
  const opening=unmounted.state.open();unmounted.unmount();read.reject(new Error('旧读取网络错误'));await opening
  assert.equal(unmounted.calls.events.length,0);assert.equal(unmounted.state.error,'')
})

test('live tenant cache drift is rechecked after confirmation even if no reactive store field changes', async () => {
  const pending=defer(),view=mount({confirm:()=>pending.promise})
  try{await view.state.open();view.state.reason='原租户配置';const saving=view.state.save();await settle();view.tenant.id='2';pending.resolve();await saving
    assert.equal(view.calls.puts.length,0);assert.equal(view.calls.events.length,0);assert.equal(view.state.visible,false)
  }finally{view.unmount()}
})

test('real role loss while config read waits closes old dialog and prevents later facts from reauthorizing it', async () => {
  const pending=defer(),view=mount({read:()=>pending.promise})
  try{const opening=view.state.open();view.user.roles=['super_admin'];await settle();pending.resolve(config);await opening
    assert.equal(view.state.visible,false);assert.equal(view.state.users.length,0);assert.equal(view.state.ready,false)
    view.user.roles=['doc_control'];await settle();await view.state.save();assert.equal(view.calls.puts.length,0)
  }finally{view.unmount()}
})

test('duplicate save clicks during the actual confirmation create one prompt and one acknowledged write', async () => {
  const pending=defer(),view=mount({confirm:()=>pending.promise})
  try{await view.state.open();view.state.reason='真实一次确认';const first=view.state.save();await view.state.save()
    assert.equal(view.calls.confirmations.length,1);assert.equal(view.calls.puts.length,0)
    assert.equal(view.calls.confirmations[0][2].modalClass,'app-confirm-message-box-overlay')
    pending.resolve();await first;assert.equal(view.calls.puts.length,1);assert.equal(view.calls.events.length,1)
  }finally{view.unmount()}
})

test('enabled account identity corruption and unavailable directory cannot reach confirmation or transport', async () => {
  for(const directory of [undefined,[{id:9007199254740992,nickname:'失精度账号'}],[{id:selectedId,nickname:'甲'},{id:selectedId,nickname:'乙'}]]){
    const view=mount({directory:async()=>directory})
    try{await view.state.open();view.state.selectedUserId=selectedId;view.state.reason='目录损坏';await view.state.save()
      assert.equal(view.calls.confirmations.length,0);assert.equal(view.calls.puts.length,0);assert.ok(view.state.error);assert.equal(view.state.ready,false)
    }finally{view.unmount()}
  }
})
