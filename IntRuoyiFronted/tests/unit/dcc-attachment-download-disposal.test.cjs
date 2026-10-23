const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')

const deferred = () => {
  let resolve
  const promise = new Promise(accept => { resolve = accept })
  return { promise, resolve }
}
const load = (code, requireModule) => {
  const context = { exports: {}, require: requireModule, Error, BigInt, Set, String }
  vm.runInNewContext(ts.transpileModule(code, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}
async function mount(confirm, readFile) {
  const filename = path.join(root, 'src/views/dcc/controlled-file/detail/ControlledFileAttachments.vue')
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
  const script = compileScript(descriptor, { id: 'attachment-download-disposal' })
  const requests = [], saves = [], confirmations = []
  const authState = { tenantId: '31', visitTenantId: undefined,
    userStore: vue.reactive({ getUser: { id: '99' } }) }
  const component = load(script.content, name => {
    if (name === 'vue') return vue
    if (name.endsWith('/auth')) return { getTenantId: () => authState.tenantId, getVisitTenantId: () => authState.visitTenantId }
    if (name.endsWith('/user')) return { useUserStoreWithOut: () => authState.userStore }
    if (name === 'element-plus') return { ElMessageBox: { confirm: (...args) => { confirmations.push(args); return confirm(...args) } } }
    if (name.endsWith('/workflow')) return { downloadControlledFileAttachmentWithName: async (...args) => {
      requests.push(args)
      return await readFile()
    } }
    if (name === './attachment-facts') return load(fs.readFileSync(path.join(root,
      'src/views/dcc/controlled-file/detail/attachment-facts.ts'), 'utf8'), () => { throw Error('Unexpected facts dependency') })
    if (name.endsWith('/filt')) return { downloadByData: (...args) => saves.push(args) }
    if (name.endsWith('/filePreview')) return { buildDccControlledFileAttachmentPreviewSource: (controlledFileId, attachmentId) => ({ type: 'DCC_CONTROLLED_FILE_ATTACHMENT', controlledFileId, attachmentId }) }
    if (name.endsWith('.vue')) return {}
    throw Error(`Unexpected dependency ${name}`)
  }).default
  component.render = () => vue.h('section')
  const renderer = vue.createRenderer({
    createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}),
    insert: (node, parent) => parent.children.push(node), remove() {}, setText() {}, setElementText() {},
    patchProp() {}, parentNode: () => null, nextSibling: () => null
  })
  const props = vue.reactive({ file: { id: '20', canDownload: true, attachments: [
    { attachmentId: '700', controlledFileId: '20', fileName: 'One.pdf', canDownload: true },
    { attachmentId: '701', controlledFileId: '20', fileName: 'Two.pdf', canDownload: true }
  ] } })
  const app = renderer.createApp({ render: () => vue.h(component, props) })
  const host = app.mount({ children: [] })
  await vue.nextTick()
  return { app, props, requests, saves, confirmations, authState, state: host.$.subTree.component.setupState }
}
const binary = () => ({ blob: new Blob(['actual attachment'], { type: 'application/pdf' }), fileName: 'Two.pdf' })

test('unmount while attachment confirmation is pending prevents the obsolete download request', async () => {
  const confirmation = deferred()
  const h = await mount(() => confirmation.promise, async () => binary())
  const pending = h.state.download(h.props.file.attachments[1])
  assert.equal(h.requests.length, 0)
  h.app.unmount()
  confirmation.resolve()
  await pending
  assert.equal(h.requests.length, 0, 'Unmounted version must not issue a download after its old confirmation')
  assert.equal(h.saves.length, 0)
})

test('unmount while attachment bytes are pending prevents saving the obsolete response', async () => {
  const response = deferred(), requested = deferred()
  const h = await mount(async () => {}, () => { requested.resolve(); return response.promise })
  const pending = h.state.download(h.props.file.attachments[1])
  await requested.promise
  assert.equal(h.requests.length, 1)
  assert.deepEqual(Array.from(h.requests[0].slice(0, 2)), ['20', '701'])
  h.app.unmount()
  response.resolve(binary())
  await pending
  assert.equal(h.saves.length, 0, 'Unmounted version must not save bytes arriving for its previous attachment')
})

test('cancel or close of attachment download confirmation sends no request and keeps both attachments', async () => {
  for (const action of ['cancel', 'close']) {
    const h = await mount(async () => { throw action }, async () => binary())
    try {
      await h.state.download(h.props.file.attachments[1])
      assert.equal(h.requests.length, 0)
      assert.equal(h.saves.length, 0)
      assert.equal(h.props.file.attachments.length, 2)
      assert.equal(h.state.actionError, '')
    } finally { h.app.unmount() }
  }
})

test('accepted download for the current authorized version preserves exact identities and server filename', async () => {
  const h = await mount(async () => {}, async () => binary())
  try {
    await h.state.download(h.props.file.attachments[1])
    assert.equal(h.requests.length, 1)
    assert.deepEqual(Array.from(h.requests[0].slice(0, 2)), ['20', '701'])
    assert.equal(h.requests[0][2].nonControlledWarningConfirmed, true)
    assert.equal(h.saves.length, 1)
    assert.equal(h.saves[0][1], 'Two.pdf')
    assert.equal(await h.saves[0][0].text(), 'actual attachment')
  } finally { h.app.unmount() }
})

test('revoked download permission still blocks requests in the mounted current version', async () => {
  const h = await mount(async () => {}, async () => binary())
  try {
    h.props.file.attachments[1].canDownload = false
    await vue.nextTick()
    await h.state.download(h.props.file.attachments[1])
    assert.equal(h.requests.length, 0)
    assert.equal(h.saves.length, 0)
    assert.match(h.state.actionError, /不可下载/)
  } finally { h.app.unmount() }
})

test('same file object with an in-place attachment list change invalidates the old confirmation', async () => {
  const confirmation = deferred()
  const h = await mount(() => confirmation.promise, async () => binary())
  const pending = h.state.download(h.props.file.attachments[1])
  h.props.file.attachments[1].fileName = 'Changed.pdf'
  confirmation.resolve()
  await pending
  assert.equal(h.requests.length, 0, 'In-place attachment fact changes must invalidate the old download')
  assert.equal(h.saves.length, 0)
  h.app.unmount()
})

test('two clicks before the first confirmation resolves do not create parallel downloads', async () => {
  const first = deferred()
  const h = await mount(() => first.promise, async () => binary())
  const one = h.state.download(h.props.file.attachments[1])
  const two = h.state.download(h.props.file.attachments[1])
  first.resolve()
  await Promise.all([one, two])
  assert.equal(h.requests.length, 1, 'A second click must not create another attachment request')
  assert.equal(h.saves.length, 1)
  h.app.unmount()
})

test('actor or tenant cache changes while confirmation is pending invalidate the old download', async () => {
  for (const change of [state => { state.userStore.getUser.id = '100' },
    state => { state.tenantId = '32' }, state => { state.visitTenantId = '32' }]) {
    const confirmation = deferred()
    const h = await mount(() => confirmation.promise, async () => binary())
    try {
      const pending = h.state.download(h.props.file.attachments[1])
      change(h.authState)
      confirmation.resolve()
      await pending
      assert.equal(h.requests.length, 0, 'Actor/tenant changes must invalidate the old attachment confirmation')
      assert.equal(h.saves.length, 0)
    } finally { h.app.unmount() }
  }
})

test('same ID refreshed object and in-place permission changes reject a pending confirmation', async () => {
  for (const change of [props => { props.file = { ...props.file, attachments: [...props.file.attachments] } },
    props => { props.file.attachments[1].canDownload = false }]) {
    const confirmation = deferred()
    const h = await mount(() => confirmation.promise, async () => binary())
    try {
      const pending = h.state.download(h.props.file.attachments[1])
      change(h.props)
      await vue.nextTick()
      confirmation.resolve()
      await pending
      assert.equal(h.requests.length, 0)
      assert.equal(h.saves.length, 0)
    } finally { h.app.unmount() }
  }
})

test('in-place facts close the old preview and an old binary cannot release the new download lock', async () => {
  const first = deferred(), second = deferred(), started = [deferred(), deferred()]
  let request = 0
  const h = await mount(async () => {}, () => {
    const index = request++
    started[index].resolve()
    return index === 0 ? first.promise : second.promise
  })
  try {
    const row = h.props.file.attachments[1]
    h.state.preview(row)
    assert.equal(h.state.previewVisible, true)
    const old = h.state.download(row)
    await started[0].promise
    row.fileName = 'Updated.pdf'
    assert.equal(h.state.previewVisible, false)
    assert.equal(h.state.selected, undefined)
    const current = h.state.download(row)
    await started[1].promise
    first.resolve(binary())
    await old
    assert.equal(h.saves.length, 0)
    assert.equal(h.state.downloading, '701')
    second.resolve(binary())
    await current
    assert.equal(h.saves.length, 1)
    assert.equal(h.state.downloading, '')
  } finally { h.app.unmount() }
})

test('cache-only tenant changes after a request prevent saving its old binary response', async () => {
  const response = deferred(), started = deferred()
  const h = await mount(async () => {}, () => { started.resolve(); return response.promise })
  try {
    const pending = h.state.download(h.props.file.attachments[1])
    await started.promise
    h.authState.tenantId = '32'
    response.resolve(binary())
    await pending
    assert.equal(h.saves.length, 0)
  } finally { h.app.unmount() }
})

test('loaded T1 facts cannot open a preview after identity changes before the first click', async () => {
  for (const change of [state => { state.userStore.getUser.id = '100' },
    state => { state.tenantId = '32' }, state => { state.visitTenantId = '32' }]) {
    const h = await mount(async () => {}, async () => binary())
    try {
      change(h.authState)
      h.state.preview(h.props.file.attachments[1])
      assert.equal(h.state.previewVisible, false, 'Old loaded facts must not be reauthorized at first preview click')
      assert.equal(h.state.selected, undefined)
      assert.match(h.state.actionError, /身份|账号|租户/)
      assert.equal(h.requests.length, 0)
      h.props.file.attachments[1].fileName = 'Still-loaded-under-T1.pdf'
      h.state.preview(h.props.file.attachments[1])
      assert.equal(h.state.previewVisible, false, 'A new fingerprint must not rebind the old file authority')
      assert.equal(h.state.selected, undefined)
    } finally { h.app.unmount() }
  }
})

test('loaded T1 download permission cannot open confirmation under a different identity', async () => {
  for (const change of [state => { state.userStore.getUser.id = '100' },
    state => { state.tenantId = '32' }, state => { state.visitTenantId = '32' }]) {
    const h = await mount(async () => {}, async () => binary())
    try {
      h.state.preview(h.props.file.attachments[0])
      change(h.authState)
      await h.state.download(h.props.file.attachments[1])
      assert.equal(h.confirmations.length, 0, 'Reject the old loaded grant before opening a confirmation')
      assert.equal(h.requests.length, 0)
      assert.equal(h.saves.length, 0)
      assert.equal(h.state.selected, undefined)
      assert.equal(h.state.previewVisible, false)
    } finally { h.app.unmount() }
  }
})

test('same authority remains valid and an explicit new file object binds its newly loaded authority', async () => {
  const h = await mount(async () => {}, async () => binary())
  try {
    h.state.preview(h.props.file.attachments[0])
    assert.equal(h.state.previewVisible, true)
    await h.state.download(h.props.file.attachments[1])
    assert.equal(h.requests.length, 1)
    h.authState.userStore.getUser.id = '100'
    h.authState.tenantId = '32'
    h.props.file = { ...h.props.file, attachments: h.props.file.attachments.map(row => ({ ...row })) }
    await vue.nextTick()
    h.state.preview(h.props.file.attachments[1])
    assert.equal(h.state.previewVisible, true)
    await h.state.download(h.props.file.attachments[1])
    assert.equal(h.requests.length, 2)
    assert.equal(h.saves.length, 2)
  } finally { h.app.unmount() }
})
