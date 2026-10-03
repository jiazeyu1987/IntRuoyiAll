const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const componentPath = path.join(root, 'src/views/mes/pro/production-release/components/ReleaseTaskNotificationEntry.vue')
const apiPath = path.join(root, 'src/api/mes/pro/productionRelease/notification.ts')
const queryPermission = 'mes:pro-edhr-work-task:query'
const listUrl = '/mes/pro/production-release-task-notification/list'
const retryUrl = '/mes/pro/production-release-task-notification/retry'
const tick = async () => { for (let i = 0; i < 16; i++) await Promise.resolve(); await vue.nextTick() }
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
const receipt = (extra = {}) => ({ id: '9007199254741001', workTaskId: '2735', userId: '347', status: 'FAILED', rowVersion: 4, attemptCount: 2, lastAttemptAt: '2026-10-03 16:00:00', sentAt: null, systemMessageId: null, lastErrorSummary: '通知模板当前不可用', ...extra })
const formalRelease = () => ({ releaseTransactionId: '226', batchExecutionId: '900000001225', releaseApprovalWorkTaskId: '2735', releaseStatus: 'RELEASED' })
const node = (type, text = '') => ({ type, text, props: {}, children: [], parent: null, get parentNode() { return this.parent } })
const detach = item => { if (item.parent) { const children = item.parent.children; children.splice(children.indexOf(item), 1); item.parent = null } }
const renderer = vue.createRenderer({
  createElement: node, createText: text => node('text', text), createComment: text => node('comment', text),
  insert(item, parent, anchor) { detach(item); item.parent = parent; parent.removeChild = detach; const index = anchor ? parent.children.indexOf(anchor) : -1; if (index < 0) parent.children.push(item); else parent.children.splice(index, 0, item) },
  remove: detach, parentNode: item => item.parent, nextSibling: item => item.parent?.children[item.parent.children.indexOf(item) + 1] || null,
  setText(item, text) { item.text = text }, setElementText(item, text) { item.text = text; item.children = [] }, patchProp(item, key, oldValue, value) { item.props[key] = value }
})
const descendants = item => [item, ...item.children.flatMap(descendants)]
const textOf = item => item.type === 'comment' ? '' : item.text + item.children.map(textOf).join('')

function executeModule(source, imports, filename) {
  const code = ts.transpileModule(source, { fileName: filename, compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
  const module = { exports: {} }
  Function('require', 'module', 'exports', 'useI18n', code)(id => {
    assert.ok(Object.hasOwn(imports, id), `测试没有为此真实依赖提供明确边界：${id}`)
    return imports[id]
  }, module, module.exports, () => ({ t: value => value }))
  return module.exports
}

function loadActualComponent(request, userStore) {
  // Missing component is an explicit feature RED, never a mock-success replacement.
  assert.ok(fs.existsSync(componentPath), 'NOTIFY-UI01 缺少可查询并恢复失败回执的真实共享组件')
  assert.ok(fs.existsSync(apiPath), 'NOTIFY-UI01 缺少正式 list/retry API wrapper')
  const notificationApi = executeModule(fs.readFileSync(apiPath, 'utf8'), { '@/config/axios': request }, apiPath)
  const releaseApiPath = path.join(root, 'src/api/mes/pro/edhr/release.ts')
  const releaseApi = executeModule(fs.readFileSync(releaseApiPath, 'utf8'), { '@/config/axios': request }, releaseApiPath)
  const { descriptor, errors } = parse(fs.readFileSync(componentPath, 'utf8'))
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'actual-notification-entry', inlineTemplate: true })
  return executeModule(script.content, {
    vue,
    '@/api/mes/pro/productionRelease/notification': notificationApi,
    '@/api/mes/pro/edhr/release': releaseApi,
    '@/store/modules/user': { useUserStore: () => userStore }
  }, componentPath + '.ts').default
}

function installPermission(app, userStore) {
  const permissionPath = path.join(root, 'src/directives/permission/hasPermi.ts')
  const permission = executeModule(fs.readFileSync(permissionPath, 'utf8'), {
    '@/store/modules/user': { useUserStore: () => userStore },
    '@/hooks/web/useI18n': { useI18n: () => ({ t: value => value }) }
  }, permissionPath)
  permission.hasPermi(app)
}

function createHarness({ props = { workTaskId: '2735' }, rows, list, release, retry, userId = '347', permissions = [queryPermission] } = {}) {
  const queries = [], posts = []
  const userStore = vue.reactive({ getUser: { id: userId }, permissions: new Set(permissions) })
  const sourceProps = vue.reactive({ ...props })
  const request = {
    get: async options => {
      queries.push({ url: options.url, params: { ...options.params } })
      if (options.url === listUrl) return list ? await list(options.params.workTaskId) : (rows ?? [receipt()])
      if (options.url === '/mes/pro/edhr-release/get') return release ? await release(options.params.id) : formalRelease()
      assert.fail(`通知查询不得选择额外数据源：${options.url}`)
    },
    post: async options => {
      posts.push({ url: options.url, data: { ...options.data } })
      assert.equal(options.url, retryUrl, '不得重新批准/创建任务或调用平台发送接口')
      return retry ? await retry(options.data) : true
    }
  }
  const actualComponent = loadActualComponent(request, userStore)
  const app = renderer.createApp({ setup: () => () => vue.h(actualComponent, sourceProps) })
  const passthrough = tag => ({ setup: (props, { attrs, slots }) => () => vue.h(tag, attrs, slots.default?.()) })
  app.component('ElButton', passthrough('button'))
  app.component('ElInput', {
    props: ['modelValue'], emits: ['update:modelValue'],
    setup: (props, { attrs, emit }) => () => vue.h('input', { ...attrs, value: props.modelValue, onInput: event => emit('update:modelValue', event.target.value) })
  })
  app.component('Dialog', {
    props: ['modelValue'], emits: ['update:modelValue', 'close', 'closed'],
    setup: (props, { attrs, slots }) => () => props.modelValue ? vue.h('section', attrs, [slots.default?.(), slots.footer?.()]) : null
  })
  app.component('ElDialog', {
    props: ['modelValue'], setup: (props, { attrs, slots }) => () => props.modelValue ? vue.h('section', attrs, [slots.default?.(), slots.footer?.()]) : null
  })
  app.component('ElAlert', { setup: (props, { attrs }) => () => vue.h('div', attrs, [attrs.title, attrs.description]) })
  app.component('ElEmpty', { setup: (props, { attrs }) => () => vue.h('div', attrs, attrs.description) })
  for (const name of ['ElForm', 'ElFormItem', 'ElTag', 'ElDescriptions', 'ElDescriptionsItem']) app.component(name, passthrough('div'))
  app.component('ElTable', {
    props: ['data'], setup(props, { slots }) { vue.provide('notification-test-table', props); return () => vue.h('table', slots.default?.()) }
  })
  app.component('ElTableColumn', {
    props: ['prop', 'label'], setup(props, { slots }) { const table = vue.inject('notification-test-table'); return () => vue.h('tbody', table.data.map(row => vue.h('tr', slots.default?.({ row }) ?? String(row[props.prop] ?? '')))) }
  })
  installPermission(app, userStore)
  app.directive('loading', {})
  const container = node('root')
  app.mount(container)
  const find = (marker, value) => descendants(container).find(item => Object.hasOwn(item.props, marker) && (value === undefined || String(item.props[marker]) === value))
  const click = async (marker, value) => {
    const target = find(marker, value)
    assert.ok(target, `真实组件缺少用户可操作入口 ${marker}`)
    assert.equal(Boolean(target.props.disabled), false, `当前合法操作不应被禁用：${marker}`)
    assert.equal(typeof target.props.onClick, 'function')
    target.props.onClick()
    await tick()
  }
  return {
    queries, posts, userStore, sourceProps, find, click,
    text: () => textOf(container),
    setReason: async value => { const input = find('data-release-task-notification-reason'); assert.ok(input); input.props.onInput({ target: { value } }); await tick() },
    open: () => click('data-release-task-notification-entry'),
    unmount: () => app.unmount()
  }
}

test('NOTIFY-UI01 actual component lazily queries exact task, preserves receipt facts and performs no writes', async () => {
  const h = createHarness()
  try {
    assert.deepEqual(h.queries, [])
    await h.open()
    assert.deepEqual(h.queries, [{ url: listUrl, params: { workTaskId: '2735' } }])
    assert.match(h.text(), /通知模板当前不可用/)
    assert.match(h.text(), /2026-10-03 16:00:00/)
    assert.deepEqual(h.posts, [])
  } finally { h.unmount() }
})

test('NOTIFY-UI01 released history resolves only its exact formal transaction and task', async () => {
  const h = createHarness({ props: { batchExecutionId: '900000001225', releaseTransactionId: '226' } })
  try {
    await h.open()
    assert.deepEqual(h.queries, [{ url: '/mes/pro/edhr-release/get', params: { id: '226' } }, { url: listUrl, params: { workTaskId: '2735' } }])
    assert.deepEqual(h.posts, [])
  } finally { h.unmount() }
})

test('NOTIFY-UI01 wrong formal batch, transaction or absent task cannot query an inferred task', async () => {
  for (const wrong of [{ batchExecutionId: '900000001226' }, { releaseTransactionId: '227' }, { releaseApprovalWorkTaskId: undefined }]) {
    const h = createHarness({ props: { batchExecutionId: '900000001225', releaseTransactionId: '226' }, release: async () => ({ ...formalRelease(), ...wrong }) })
    try {
      await h.open()
      assert.equal(h.queries.length, 1)
      assert.ok(h.find('data-release-task-notification-error'))
      assert.deepEqual(h.posts, [])
    } finally { h.unmount() }
  }
})

test('NOTIFY-UI01 legacy task with no receipts displays explicit empty state without creating or sending', async () => {
  const h = createHarness({ rows: [] })
  try { await h.open(); assert.ok(h.find('data-release-task-notification-empty')); assert.equal(h.posts.length, 0) } finally { h.unmount() }
})

test('NOTIFY-UI01 authoritative list rejection is visible and has no alternate reader or send', async () => {
  const h = createHarness({ list: async () => { throw new Error('当前登录用户不在该任务冻结候选中') } })
  try { await h.open(); assert.match(h.text(), /不在该任务冻结候选中/); assert.equal(h.queries.length, 1); assert.equal(h.posts.length, 0) } finally { h.unmount() }
})

for (const status of ['FAILED', 'PENDING']) {
  test(`NOTIFY-UI01 current recipient can manually retry ${status} after DONE with exact ID, version and reason`, async () => {
    const h = createHarness({ props: { batchExecutionId: '900000001225', releaseTransactionId: '226' }, rows: [receipt({ status })] })
    try {
      await h.open()
      await h.click('data-release-task-notification-retry', receipt().id)
      await h.setReason('模板已恢复，补发本人原通知')
      await h.click('data-release-task-notification-submit-retry')
      assert.deepEqual(h.posts, [{ url: retryUrl, data: { deliveryId: receipt().id, expectedVersion: 4, reason: '模板已恢复，补发本人原通知' } }])
      assert.equal(h.queries.filter(query => query.url === listUrl).length, 2)
    } finally { h.unmount() }
  })
}

test('NOTIFY-UI01 SENT and another recipient never expose an enabled retry action', async () => {
  const h = createHarness({ rows: [receipt({ id: '100', status: 'SENT', systemMessageId: '999' }), receipt({ id: '101', userId: '348' })] })
  try {
    await h.open()
    for (const id of ['100', '101']) { const button = h.find('data-release-task-notification-retry', id); assert.ok(!button || button.props.disabled) }
    assert.deepEqual(h.posts, [])
  } finally { h.unmount() }
})

test('NOTIFY-UI01 blank or overlength reason cannot submit the selected receipt', async () => {
  for (const reason of ['  ', '审'.repeat(1001)]) {
    const h = createHarness()
    try {
      await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason(reason)
      const submit = h.find('data-release-task-notification-submit-retry')
      assert.ok(submit)
      if (!submit.props.disabled) { submit.props.onClick(); await tick() }
      assert.deepEqual(h.posts, [])
    } finally { h.unmount() }
  }
})

test('NOTIFY-UI01 failed or uncertain retry queries fresh state without automatically posting again', async () => {
  let calls = 0
  const h = createHarness({ list: async () => [receipt({ rowVersion: ++calls === 1 ? 4 : 5 })], retry: async () => { throw new Error('投递响应不确定，请查看最新回执') } })
  try {
    await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('人工确认后补发')
    await h.click('data-release-task-notification-submit-retry')
    assert.equal(h.posts.length, 1)
    assert.equal(calls, 2)
    assert.match(h.text(), /投递响应不确定/)
    await tick()
    assert.equal(h.posts.length, 1)
  } finally { h.unmount() }
})

test('NOTIFY-UI01 duplicate click while retry is pending produces only one request', async () => {
  const pending = deferred()
  const h = createHarness({ retry: () => pending.promise })
  try {
    await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('补发本人通知')
    const submit = h.find('data-release-task-notification-submit-retry')
    submit.props.onClick(); submit.props.onClick(); await tick()
    assert.equal(h.posts.length, 1)
    pending.resolve(true); await tick()
    assert.equal(h.posts.length, 1)
  } finally { h.unmount() }
})

test('NOTIFY-UI01 late old task receipt cannot overwrite a newly opened task', async () => {
  const old = deferred()
  const h = createHarness({ list: taskId => taskId === '2735' ? old.promise : Promise.resolve([receipt({ id: '222', workTaskId: '2736', lastErrorSummary: '当前任务通知失败' })]) })
  try {
    await h.open()
    h.sourceProps.workTaskId = '2736'; await tick()
    await h.open()
    old.resolve([receipt({ lastErrorSummary: '旧任务晚到错误' })]); await tick()
    assert.match(h.text(), /当前任务通知失败/)
    assert.doesNotMatch(h.text(), /旧任务晚到错误/)
    assert.deepEqual(h.posts, [])
  } finally { h.unmount() }
})

test('NOTIFY-UI01 foreign task receipts are rejected rather than rendered as the current task', async () => {
  const h = createHarness({ rows: [receipt({ workTaskId: '2736', lastErrorSummary: '不应显示的另一任务通知' })] })
  try { await h.open(); assert.ok(h.find('data-release-task-notification-error')); assert.doesNotMatch(h.text(), /不应显示的另一任务通知/); assert.equal(h.posts.length, 0) } finally { h.unmount() }
})

test('NOTIFY-UI01 permission absence keeps standalone entry inaccessible without any reads or writes', async () => {
  const h = createHarness({ permissions: [] })
  try { assert.equal(h.find('data-release-task-notification-entry'), undefined); assert.deepEqual(h.queries, []); assert.deepEqual(h.posts, []) } finally { h.unmount() }
})

test('NOTIFY-UI01 unmount invalidates late read and never emits a delivery write', async () => {
  const pending = deferred()
  const h = createHarness({ list: () => pending.promise })
  await h.open(); h.unmount(); pending.resolve([receipt()]); await tick()
  assert.equal(h.find('data-release-task-notification-dialog'), undefined)
  assert.deepEqual(h.posts, [])
})

test('NOTIFY-UI01 invalid, ambiguous or incomplete formal input fails before querying', async () => {
  for (const props of [{}, { workTaskId: '0' }, { workTaskId: 9007199254740992 }, { batchExecutionId: '900000001225' }, { workTaskId: '2735', batchExecutionId: '900000001225', releaseTransactionId: '226' }]) {
    const h = createHarness({ props })
    try { await h.open(); assert.ok(h.find('data-release-task-notification-error')); assert.deepEqual(h.queries, []); assert.deepEqual(h.posts, []) } finally { h.unmount() }
  }
})

test('NOTIFY-UI01 malformed receipt versions or states cannot become a retry candidate', async () => {
  for (const invalid of [{ rowVersion: -1 }, { rowVersion: 1.5 }, { status: 'UNKNOWN' }, { id: 100 }, { userId: undefined }]) {
    const h = createHarness({ rows: [receipt(invalid)] })
    try { await h.open(); assert.ok(h.find('data-release-task-notification-error')); assert.equal(h.find('data-release-task-notification-retry', receipt().id), undefined); assert.deepEqual(h.posts, []) } finally { h.unmount() }
  }
})

test('NOTIFY-UI01 refresh invalidates previously selected version and requires explicit selection of the fresh receipt', async () => {
  let queryCount = 0
  const h = createHarness({ list: async () => [receipt({ rowVersion: ++queryCount === 1 ? 4 : 5 })] })
  try {
    await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('旧版本原因')
    const staleSubmit = h.find('data-release-task-notification-submit-retry').props.onClick
    await h.click('data-release-task-notification-refresh')
    staleSubmit(); await tick()
    assert.deepEqual(h.posts, [])
    await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('核对新版本后补发')
    await h.click('data-release-task-notification-submit-retry')
    assert.deepEqual(h.posts, [{ url: retryUrl, data: { deliveryId: receipt().id, expectedVersion: 5, reason: '核对新版本后补发' } }])
  } finally { h.unmount() }
})

test('NOTIFY-UI01 identity or permission change after selecting a receipt prevents stale submission', async () => {
  for (const change of [h => { h.userStore.getUser.id = '348' }, h => { h.userStore.permissions.clear() }]) {
    const h = createHarness()
    try {
      await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('身份变更前原因')
      const staleSubmit = h.find('data-release-task-notification-submit-retry').props.onClick
      change(h); await tick(); staleSubmit(); await tick()
      assert.deepEqual(h.posts, [])
      assert.equal(h.find('data-release-task-notification-dialog'), undefined)
    } finally { h.unmount() }
  }
})

test('NOTIFY-UI01 an old pending retry cannot refresh or overwrite the newly opened task', async () => {
  const pending = deferred()
  const h = createHarness({ list: async taskId => [receipt({ workTaskId: taskId, lastErrorSummary: taskId === '2735' ? '旧任务' : '当前任务' })], retry: () => pending.promise })
  try {
    await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('原任务补发')
    await h.click('data-release-task-notification-submit-retry')
    h.sourceProps.workTaskId = '2736'; await tick(); await h.open()
    const count = h.queries.length
    pending.reject(new Error('旧补发晚到异常')); await tick()
    assert.equal(h.queries.length, count)
    assert.match(h.text(), /当前任务/)
    assert.doesNotMatch(h.text(), /旧补发晚到异常/)
    assert.equal(h.posts.length, 1)
  } finally { h.unmount() }
})

test('NOTIFY-UI01 closing and reopening while a retry is pending cannot resubmit that delivery', async () => {
  const pending = deferred()
  const h = createHarness({ retry: () => pending.promise })
  try {
    await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('本人补发')
    await h.click('data-release-task-notification-submit-retry')
    await h.click('data-release-task-notification-close'); await h.open()
    const retryButton = h.find('data-release-task-notification-retry', receipt().id)
    assert.ok(!retryButton || retryButton.props.disabled)
    assert.equal(h.posts.length, 1)
    pending.resolve(true); await tick()
    assert.equal(h.posts.length, 1)
  } finally { h.unmount() }
})

test('NOTIFY-UI01 false acknowledgement remains visible and queries receipts without another POST', async () => {
  const h = createHarness({ retry: async () => false })
  try {
    await h.open(); await h.click('data-release-task-notification-retry', receipt().id); await h.setReason('核对通知')
    await h.click('data-release-task-notification-submit-retry')
    assert.equal(h.posts.length, 1)
    assert.equal(h.queries.length, 2)
    assert.match(h.text(), /未收到明确的投递结果/)
  } finally { h.unmount() }
})
