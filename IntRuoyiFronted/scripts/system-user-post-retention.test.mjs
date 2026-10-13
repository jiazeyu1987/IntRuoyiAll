import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const sfcPath = path.join(root, 'src/views/system/user/UserForm.vue')
const transpile = (source) => {
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
    reportDiagnostics: true
  })
  assert.deepEqual(compiled.diagnostics, [], 'actual TypeScript modules must transpile without syntax errors')
  return compiled.outputText
}
const parsed = parse(readFileSync(sfcPath, 'utf8'), { filename: sfcPath })
assert.deepEqual(parsed.errors, [], 'actual SFC must parse')
const descriptor = parsed.descriptor
const script = compileScript(descriptor, { id: sfcPath })
const template = compileTemplate({ source: descriptor.template.content, filename: sfcPath, id: sfcPath,
  compilerOptions: { bindingMetadata: script.bindings } })
assert.deepEqual(template.errors, [], 'actual SFC template must compile')
const compiledModules = new Map([[sfcPath, transpile(script.content)]])
const compiledRender = transpile(template.code)

// Capture/compile each actual source once. Every mount still evaluates fresh
// modules with its own explicit I/O boundary, avoiding repeated disk/compiler
// work without sharing fixture state or replacing production handlers.
function compiledCode(filename) {
  if (!compiledModules.has(filename)) compiledModules.set(filename, transpile(readFileSync(filename, 'utf8')))
  return compiledModules.get(filename)
}

// Execute the entire production SFC and actual user/post/dept/request wrappers.
// Substitute HTTP transport, i18n/dictionary inputs, messages and child rendering
// and form-validation boundaries. Actual Element Plus DOM/tag interaction is
// deliberately a separate live E2E gate; this renderer does not prove it.
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const activePosts = () => [{ id: 1, name: '生产岗位' }, { id: '3', name: '新增岗位' }]
const editUser = () => ({
  id: 101, username: 'task_user', nickname: '任务用户', deptId: 10,
  mobile: '13800000000', email: '', sex: 1, remark: '', status: 0,
  postIds: [1, '2'], assignedPosts: [
    { id: '1', name: '生产岗位', status: 0 },
    { id: 2, name: '历史岗位', status: 1 }
  ]
})

function mountForm(options = {}) {
  const requests = [], successMessages = [], emitted = []
  const responses = {
    '/system/user/get-for-update?id=101': editUser(),
    '/system/dept/simple-list': [{ id: 10, name: '任务部门', parentId: 0 }],
    '/system/post/simple-list': activePosts(), ...options.responses
  }
  const dependencies = {
    vue,
    '@/utils/dict': { DICT_TYPE: { SYSTEM_USER_SEX: 'SYSTEM_USER_SEX' }, getIntDictOptions: () => [] },
    '@/config/axios/service': {
      service: async (request) => {
        requests.push(JSON.parse(JSON.stringify(request)))
        if (request.method !== 'GET') {
          if (!(
            (request.method === 'PUT' && request.url === '/system/user/update') ||
            (request.method === 'POST' && request.url === '/system/user/create')
          )) throw new Error(`Unexpected mutation ${request.method} ${request.url}`)
          return { data: options.write ? await options.write(request) : true }
        }
        if (!Object.hasOwn(responses, request.url)) throw new Error(`Unexpected transport request ${request.url}`)
        const response = responses[request.url]
        return { data: typeof response === 'function' ? await response(request) : await response }
      }
    },
    '@/config/axios/config': { config: { default_headers: 'application/json' } }
  }
  const autoImports = {
    ref: vue.ref, reactive: vue.reactive, computed: vue.computed, watch: vue.watch,
    useI18n: () => ({ t: (key) => key }),
    useMessage: () => ({ success: (message) => successMessages.push(message) })
  }
  const cache = new Map()
  const ioModules = new Map([
    [path.join(root, 'src/config/axios/service.ts'), dependencies['@/config/axios/service']],
    [path.join(root, 'src/config/axios/config.ts'), dependencies['@/config/axios/config']]
  ])
  function resolveModule(name, filename) {
    const base = name.startsWith('@/') ? path.join(root, 'src', name.slice(2)) : path.resolve(path.dirname(filename), name)
    for (const candidate of [base, base + '.ts', path.join(base, 'index.ts')]) {
      if (existsSync(candidate) && (candidate.endsWith('.ts') || candidate.endsWith('.vue'))) return candidate
    }
    throw new Error(`Cannot resolve actual production dependency ${name}`)
  }
  function load(filename) {
    if (ioModules.has(filename)) return ioModules.get(filename)
    if (cache.has(filename)) return cache.get(filename).exports
    const module = { exports: {} }; cache.set(filename, module)
    const localRequire = (name) => {
      if (Object.hasOwn(dependencies, name)) return dependencies[name]
      if (name.startsWith('.') || name.startsWith('@/')) return load(resolveModule(name, filename))
      return require(name)
    }
    new Function('require', 'module', 'exports', ...Object.keys(autoImports), compiledCode(filename))(
      localRequire, module, module.exports, ...Object.values(autoImports)
    )
    return module.exports
  }
  const renderModule = { exports: {} }
  new Function('require', 'module', 'exports', compiledRender)(require, renderModule, renderModule.exports)
  const component = { ...load(sfcPath).default, render: renderModule.exports.render }
  const detach = (node) => {
    if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
    node.parent = null
  }
  const renderer = vue.createRenderer({
    createElement: (type) => ({ type, props: {}, children: [], parent: null }),
    createText: (text) => ({ type: '#text', text, children: [], parent: null }),
    createComment: (text) => ({ type: '#comment', text, children: [], parent: null }),
    insert(node, parent, anchor) {
      detach(node); node.parent = parent
      const index = anchor ? parent.children.indexOf(anchor) : -1
      if (index < 0) parent.children.push(node)
      else parent.children.splice(index, 0, node)
    },
    remove: detach,
    setText: (node, text) => { node.text = text },
    setElementText: (node, text) => { node.text = text; node.children = [] },
    parentNode: (node) => node.parent,
    nextSibling: (node) => node.parent?.children[node.parent.children.indexOf(node) + 1] ?? null,
    patchProp: (node, key, _old, value) => { node.props[key] = value }
  })
  const hostRoot = { type: 'root', children: [] }, formComponent = vue.ref()
  let model
  const app = renderer.createApp({ render: () => vue.h(component, { ref: formComponent, onSuccess: () => emitted.push('success') }) })
  for (const [name, type] of [
    ['Dialog', 'dialog'], ['ElAlert', 'alert'], ['ElForm', 'form'], ['ElRow', 'row'], ['ElCol', 'col'],
    ['ElFormItem', 'form-item'], ['ElInput', 'input'], ['ElSelect', 'select'], ['ElOption', 'option'],
    ['ElTreeSelect', 'tree-select'], ['ElButton', 'button']
  ]) {
    app.component(name, {
      inheritAttrs: false,
      props: {
        modelValue: { type: [String, Number, Boolean, Array, Object] },
        model: { type: Object },
        title: { type: String },
        label: { type: String },
        value: { type: [String, Number] },
        disabled: { type: Boolean },
        placeholder: { type: String },
        type: { type: String },
        rules: { type: Object }
      },
      setup(props, { slots, attrs, expose }) {
        if (name === 'ElForm') expose({ validate: async () => options.validate ? await options.validate() : true, resetFields() {} })
        return () => {
          if (name === 'ElForm') model = props.model
          return vue.h(type, { ...attrs, ...props }, [...(slots.default?.() ?? []), ...(slots.footer?.() ?? [])])
        }
      }
    })
  }
  app.directive('loading', {})
  app.mount(hostRoot)
  const nodes = (type) => {
    const result = []
    const walk = (node) => { if (node.type === type) result.push(node); node.children.forEach(walk) }
    walk(hostRoot); return result
  }
  const selectNode = () => nodes('select').find((node) => Array.isArray(node.props.modelValue))
  const saveNode = () => nodes('button').find((node) => node.props.type === 'primary')
  return {
    requests, successMessages, emitted, responses,
    api: {
      user: load(path.join(root, 'src/api/system/user/index.ts')),
      post: load(path.join(root, 'src/api/system/post/index.ts')),
      dept: load(path.join(root, 'src/api/system/dept/index.ts'))
    },
    get model() { return model },
    get postIds() { return selectNode().props.modelValue },
    get options() { return nodes('option').map((node) => ({ id: node.props.value, label: node.props.label })) },
    get saveDisabled() { return saveNode().props.disabled },
    get formDisabled() { return nodes('form')[0].props.disabled },
    get visible() { return nodes('dialog')[0].props.modelValue },
    get errors() { return nodes('alert').map((node) => node.props.title) },
    async open(type = 'update', id = 101) {
      const result = await formComponent.value.open(type, id)
      await vue.nextTick()
      return result
    },
    startOpen(type = 'update', id = 101) { return formComponent.value.open(type, id) },
    async select(ids) { selectNode().props['onUpdate:modelValue'](ids); await vue.nextTick() },
    async edit(placeholder, value) {
      nodes('input').find((node) => node.props.placeholder === placeholder).props['onUpdate:modelValue'](value)
      await vue.nextTick()
    },
    save() { return saveNode().props.onClick() },
    async cancel() { nodes('button').find((node) => node.props.type !== 'primary').props.onClick(); await vue.nextTick() },
    async close() { nodes('dialog')[0].props['onUpdate:modelValue'](false); await vue.nextTick() },
    writes() { return requests.filter((request) => request.method !== 'GET') },
    unmount() { app.unmount() }
  }
}

test('actual edit source retains disabled assignment and profile PUT sends the full exact set', async () => {
  const form = mountForm()
  try {
    await form.open()
    assert.equal(form.requests[0].url, '/system/user/get-for-update?id=101')
    assert.deepEqual(form.postIds, ['1', '2'])
    assert.ok(form.options.some((post) => post.id === '2' && post.label === '历史岗位（已停用）'))
    assert.equal(form.saveDisabled, false)
    await form.edit('请输入手机号码', '13900000000'); await form.save()
    assert.equal(form.writes().length, 1)
    assert.equal(form.writes()[0].method, 'PUT')
    assert.equal(form.writes()[0].url, '/system/user/update')
    assert.deepEqual(form.writes()[0].data.postIds, ['1', '2'])
    assert.equal(form.writes()[0].data.mobile, '13900000000')
    assert.equal(Object.hasOwn(form.writes()[0].data, 'assignedPosts'), false)
    assert.deepEqual(form.emitted, ['success'])
    assert.equal(form.visible, false)
  } finally { form.unmount() }
})
test('actual selection removes disabled fact, forbids re-add and permits enabled additions', async () => {
  const form = mountForm()
  try {
    await form.open(); await form.select(['1'])
    assert.deepEqual(form.postIds, ['1'])
    assert.equal(form.options.some((post) => post.id === '2'), false)
    await assert.rejects(form.select(['1', '2']), /岗位资料/)
    assert.deepEqual(form.postIds, ['1'])
    await form.select(['1', '3']); await form.save()
    assert.deepEqual(form.writes()[0].data.postIds, ['1', '3'])
  } finally { form.unmount() }
})
test('actual clear submits an explicit empty complete set', async () => {
  const form = mountForm()
  try { await form.open(); await form.select([]); await form.save(); assert.deepEqual(form.writes()[0].data.postIds, []) }
  finally { form.unmount() }
})
test('actual unsaved-removal cancellation writes nothing and reopening restores formal facts', async () => {
  const form = mountForm()
  try {
    await form.open(); await form.select(['1']); await form.cancel(); await form.save()
    assert.deepEqual(form.writes(), []); assert.deepEqual(form.emitted, [])
    await form.open(); assert.deepEqual(form.postIds, ['1', '2'])
  } finally { form.unmount() }
})
test('all formal load dependencies remain gated until the complete form is ready', async () => {
  const posts = deferred(), form = mountForm({ responses: { '/system/post/simple-list': posts.promise } })
  try {
    const loading = form.startOpen(); await vue.nextTick()
    assert.equal(form.saveDisabled, true); assert.equal(form.formDisabled, true); assert.deepEqual(form.postIds, [])
    await form.save(); assert.deepEqual(form.writes(), [])
    posts.resolve(activePosts()); await loading; await vue.nextTick()
    assert.equal(form.saveDisabled, false); assert.equal(form.formDisabled, false); assert.deepEqual(form.postIds, ['1', '2'])
  } finally { form.unmount() }
})

for (const [name, mutate] of [
  ['missing assigned facts', (user) => { delete user.assignedPosts }],
  ['missing complete IDs', (user) => { delete user.postIds }],
  ['null complete IDs', (user) => { user.postIds = null }],
  ['non-array complete IDs', (user) => { user.postIds = '1,2' }],
  ['duplicate assigned facts', (user) => { user.assignedPosts.push({ ...user.assignedPosts[0] }) }],
  ['duplicate complete IDs', (user) => { user.postIds.push('1') }],
  ['mismatched assigned IDs', (user) => { user.postIds = [1] }],
  ['blank formal name', (user) => { user.assignedPosts[1].name = '  ' }],
  ['invalid formal status', (user) => { user.assignedPosts[1].status = 2 }],
  ['unsafe numeric ID', (user) => { user.postIds[1] = 9007199254740992; user.assignedPosts[1].id = 9007199254740992 }],
  ['noncanonical ID', (user) => { user.postIds[1] = '02'; user.assignedPosts[1].id = '02' }],
  ['out-of-range Long ID', (user) => { user.postIds[1] = '9223372036854775808'; user.assignedPosts[1].id = '9223372036854775808' }],
  ['fractional ID', (user) => { user.postIds[1] = 2.5; user.assignedPosts[1].id = 2.5 }],
  ['zero ID', (user) => { user.postIds[1] = 0; user.assignedPosts[1].id = 0 }],
  ['negative ID', (user) => { user.postIds[1] = -2; user.assignedPosts[1].id = -2 }],
  ['boolean ID', (user) => { user.postIds[1] = true; user.assignedPosts[1].id = true }],
  ['null assigned ID', (user) => { user.assignedPosts[1].id = null }],
  ['wrong edit identity', (user) => { user.id = 102 }]
]) {
  test(`actual load rejects ${name} without activating or writing partial data`, async () => {
    const user = editUser(); mutate(user)
    const form = mountForm({ responses: { '/system/user/get-for-update?id=101': user } })
    try {
      assert.equal(await form.open(), false)
      assert.match(form.errors[0], /岗位资料|当前用户不一致/)
      assert.equal(form.saveDisabled, true); assert.equal(form.formDisabled, true); assert.ok(form.errors.length > 0)
      await form.save(); assert.deepEqual(form.writes(), []); assert.deepEqual(form.emitted, []); assert.deepEqual(form.postIds, [])
    } finally { form.unmount() }
  })
}
for (const [name, posts] of [
  ['enabled source conflicts with disabled fact', [...activePosts(), { id: 2, name: '历史岗位' }]],
  ['conflicting formal name', [{ id: 1, name: '不同名称' }]],
  ['missing enabled existing candidate', []],
  ['duplicate candidate identity', [...activePosts(), { id: '1', name: '生产岗位' }]],
  ['malformed candidates', null]
]) {
  test(`actual load rejects ${name}`, async () => {
    const form = mountForm({ responses: { '/system/post/simple-list': posts } })
    try {
      assert.equal(await form.open(), false)
      assert.match(form.errors[0], /岗位资料/)
      assert.equal(form.saveDisabled, true); await form.save(); assert.deepEqual(form.writes(), [])
    } finally { form.unmount() }
  })
}
for (const source of ['/system/user/get-for-update?id=101', '/system/dept/simple-list', '/system/post/simple-list']) {
  test(`actual load presents ${source} failure and terminates with zero writes`, async () => {
    const form = mountForm({ responses: { [source]: () => { throw new Error('正式查询失败') } } })
    try {
      assert.equal(await form.open(), false)
      assert.equal(form.saveDisabled, true); assert.deepEqual(form.errors, ['正式查询失败'])
      await form.save(); assert.deepEqual(form.writes(), []); assert.deepEqual(form.emitted, [])
    } finally { form.unmount() }
  })
}
test('actual create uses enabled candidates and optional empty selection', async () => {
  const form = mountForm()
  try {
    await form.open('create', undefined)
    assert.equal(form.requests.some((request) => request.url.includes('/system/user/get')), false)
    assert.deepEqual(form.options, activePosts().map((post) => ({ id: String(post.id), label: post.name })))
    assert.deepEqual(form.postIds, []); await form.save()
    assert.equal(form.writes()[0].url, '/system/user/create'); assert.deepEqual(form.writes()[0].data.postIds, [])
  } finally { form.unmount() }
})
test('actual request wrapper preserves precise Long strings through edit and PUT', async () => {
  const id = '9223372036854775807', user = editUser()
  user.postIds[1] = id; user.assignedPosts[1].id = id
  const form = mountForm({ responses: { '/system/user/get-for-update?id=101': user } })
  try {
    await form.open(); assert.deepEqual(form.postIds, ['1', id]); await form.save()
    assert.deepEqual(form.writes()[0].data.postIds, ['1', id]); assert.equal(form.writes()[0].headers['Content-Type'], 'application/json')
  } finally { form.unmount() }
})
test('actual submit waits for service success before closing or emitting', async () => {
  const write = deferred(), form = mountForm({ write: () => write.promise })
  try {
    await form.open(); const saving = form.save(); await vue.nextTick()
    assert.equal(form.visible, true); assert.deepEqual(form.emitted, []); assert.deepEqual(form.successMessages, []); assert.equal(form.saveDisabled, true)
    write.resolve(true); await saving; await vue.nextTick()
    assert.equal(form.visible, false); assert.deepEqual(form.emitted, ['success'])
  } finally { form.unmount() }
})
test('actual rejected save keeps form and emits no success', async () => {
  const form = mountForm({ write: () => { throw new Error('正式更新拒绝') } })
  try {
    await form.open(); assert.equal(await form.save(), false); await vue.nextTick()
    assert.equal(form.visible, true); assert.equal(form.saveDisabled, false); assert.deepEqual(form.postIds, ['1', '2'])
    assert.deepEqual(form.emitted, []); assert.deepEqual(form.successMessages, []); assert.deepEqual(form.errors, ['正式更新拒绝'])
  } finally { form.unmount() }
})
test('actual invalid form or cancellation during validation produces no write', async () => {
  const validation = deferred(), form = mountForm({ validate: () => validation.promise })
  try {
    await form.open(); const saving = form.save(); await form.cancel(); validation.resolve(true); await saving
    assert.deepEqual(form.writes(), []); assert.deepEqual(form.emitted, [])
  } finally { form.unmount() }
  const invalid = mountForm({ validate: () => false })
  try { await invalid.open(); await invalid.save(); assert.deepEqual(invalid.writes(), []) }
  finally { invalid.unmount() }
})
test('actual late cancelled load cannot reactivate a closed form', async () => {
  const user = deferred(), form = mountForm({ responses: { '/system/user/get-for-update?id=101': user.promise } })
  try {
    const loading = form.startOpen(); await form.close(); user.resolve(editUser()); await loading; await vue.nextTick()
    assert.equal(form.visible, false); assert.equal(form.saveDisabled, true); assert.deepEqual(form.postIds, []); assert.deepEqual(form.writes(), [])
  } finally { form.unmount() }
})
for (const failOld of [false, true]) {
  test(`actual overlapping opens isolate old ${failOld ? 'failure' : 'success'} from the new user`, async () => {
    const old = deferred(), newUser = { ...editUser(), id: 102, nickname: '另一个用户', postIds: ['3'], assignedPosts: [{ id: 3, name: '新增岗位', status: 0 }] }
    const form = mountForm({ responses: { '/system/user/get-for-update?id=101': old.promise, '/system/user/get-for-update?id=102': newUser } })
    try {
      const first = form.startOpen(); await form.open('update', 102)
      if (failOld) old.reject(new Error('过期查询失败'))
      else old.resolve(editUser())
      await first; await vue.nextTick()
      assert.deepEqual(form.postIds, ['3']); assert.equal(form.model.id, 102); assert.deepEqual(form.errors, [])
      await form.save(); assert.equal(form.writes()[0].data.id, 102); assert.deepEqual(form.writes()[0].data.postIds, ['3'])
    } finally { form.unmount() }
  })
}
test('actual submit presents reintroduced disabled ID failure and terminates without a write', async () => {
  const form = mountForm()
  try {
    await form.open(); await form.select(['1']); form.model.postIds = ['1', '2']
    assert.equal(await form.save(), false); await vue.nextTick()
    assert.match(form.errors[0], /岗位资料/)
    assert.deepEqual(form.writes(), []); assert.deepEqual(form.emitted, [])
  } finally { form.unmount() }
})
test('actual unassigned edit accepts formal empty sets without inventing bindings', async () => {
  const user = { ...editUser(), postIds: [], assignedPosts: [] }
  const form = mountForm({ responses: { '/system/user/get-for-update?id=101': user } })
  try {
    await form.open(); assert.deepEqual(form.postIds, []); await form.save()
    assert.deepEqual(form.writes()[0].data.postIds, [])
  } finally { form.unmount() }
})
test('actual malformed department response cannot activate the otherwise complete form', async () => {
  const form = mountForm({ responses: { '/system/dept/simple-list': null } })
  try {
    assert.equal(await form.open(), false)
    assert.match(form.errors[0], /部门资料不完整/)
    assert.equal(form.saveDisabled, true); await form.save(); assert.deepEqual(form.writes(), [])
  } finally { form.unmount() }
})
test('actual create selection rejects an unoffered disabled identity without mutating selection', async () => {
  const form = mountForm()
  try {
    await form.open('create', undefined)
    await assert.rejects(form.select(['2']), /岗位资料/); assert.deepEqual(form.postIds, [])
    await form.select(['3']); await form.save(); assert.deepEqual(form.writes()[0].data.postIds, ['3'])
  } finally { form.unmount() }
})
for (const failWrite of [false, true]) {
  test(`actual old submit ${failWrite ? 'failure' : 'success'} cannot change the reopened editor`, async () => {
    const write = deferred()
    const newUser = { ...editUser(), id: 102, nickname: '新用户', postIds: ['3'], assignedPosts: [{ id: 3, name: '新增岗位', status: 0 }] }
    const form = mountForm({ write: () => write.promise, responses: { '/system/user/get-for-update?id=102': newUser } })
    try {
      await form.open(); const saving = form.save(); await vue.nextTick()
      assert.equal(form.writes().length, 1)
      await form.close(); await form.open('update', 102)
      if (failWrite) write.reject(new Error('旧提交失败'))
      else write.resolve(true)
      await saving; await vue.nextTick()
      assert.equal(form.visible, true); assert.equal(form.saveDisabled, false)
      assert.equal(form.model.id, 102); assert.deepEqual(form.postIds, ['3'])
      assert.deepEqual(form.errors, []); assert.deepEqual(form.emitted, []); assert.deepEqual(form.successMessages, [])
    } finally { form.unmount() }
  })
}
test('actual submit blocks duplicate writes while validation is pending', async () => {
  const validation = deferred(), form = mountForm({ validate: () => validation.promise })
  try {
    await form.open(); const saving = form.save(); await form.save()
    validation.resolve(true); await saving
    assert.equal(form.writes().length, 1); assert.deepEqual(form.emitted, ['success'])
  } finally { form.unmount() }
})
test('actual string API rejection stays visible and does not become success', async () => {
  const form = mountForm({ write: () => Promise.reject('正式授权失败') })
  try {
    await form.open()
    assert.equal(await form.save(), false); await vue.nextTick()
    assert.deepEqual(form.errors, ['正式授权失败']); assert.equal(form.visible, true)
    assert.deepEqual(form.emitted, []); assert.deepEqual(form.successMessages, [])
  } finally { form.unmount() }
})

test('actual UM-04 load and write calls explicitly own visible errors', async () => {
  const form = mountForm()
  try {
    await form.open(); await form.save()
    assert.equal(form.requests.length, 4)
    assert.ok(form.requests.every((request) => request.ignoreErrorMessage === true))
    await form.open('create'); await form.save()
    assert.ok(form.requests.every((request) => request.ignoreErrorMessage === true))
  } finally { form.unmount() }
})

test('actual wrappers retain global ownership by default and still reject formal failures', async () => {
  const form = mountForm({ write: () => { throw new Error('正式写入拒绝') } })
  try {
    await form.api.post.getSimplePostList()
    await form.api.dept.getSimpleDeptList()
    await assert.rejects(form.api.user.createUser(editUser()), /正式写入拒绝/)
    await assert.rejects(form.api.user.updateUser(editUser()), /正式写入拒绝/)
    assert.ok(form.requests.every((request) => request.ignoreErrorMessage !== true))
    await form.api.post.getSimplePostList({ ignoreErrorMessage: true })
    await form.api.dept.getSimpleDeptList({ ignoreErrorMessage: true })
    await assert.rejects(form.api.user.createUser(editUser(), { ignoreErrorMessage: true }), /正式写入拒绝/)
    await assert.rejects(form.api.user.updateUser(editUser(), { ignoreErrorMessage: true }), /正式写入拒绝/)
    assert.ok(form.requests.slice(4).every((request) => request.ignoreErrorMessage === true))
    form.responses['/system/user/get-for-update?id=101'] = () => { throw new Error('正式编辑查询拒绝') }
    await assert.rejects(form.api.user.getUserForUpdate(101), /正式编辑查询拒绝/)
    assert.equal(form.requests.at(-1).ignoreErrorMessage, true)
    assert.deepEqual(form.successMessages, []); assert.deepEqual(form.emitted, [])
  } finally { form.unmount() }
})

test('actual rejected field validation remains visible and terminates without a write', async () => {
  const form = mountForm({ validate: () => { throw new Error('用户昵称不能为空') } })
  try {
    await form.open(); assert.equal(await form.save(), false); await vue.nextTick()
    assert.deepEqual(form.errors, ['用户昵称不能为空']); assert.equal(form.visible, true)
    assert.equal(form.saveDisabled, false); assert.deepEqual(form.writes(), [])
    assert.deepEqual(form.successMessages, []); assert.deepEqual(form.emitted, [])
  } finally { form.unmount() }
})
