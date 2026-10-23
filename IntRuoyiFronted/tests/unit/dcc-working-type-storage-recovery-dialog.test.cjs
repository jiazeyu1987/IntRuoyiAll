const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm')
const ts = require('typescript'), vue = require('vue'), { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const FILE = '2054545668044084206', OTHER = '2054545668044084225'
const facts = (id = FILE) => ({ controlledFileId: id, masterId: '2054545668044079175', fileNumber: 'DCC241016C10D01', projectId: '298',
  projectFolderId: '36', categoryId: '908748', taxonomyId: '91', currentTypePath: 'Documents / T2', originalProofId: '8712',
  originalProofSha256: 'a'.repeat(64), observedSha256: 'b'.repeat(64), sourceSha256: 'c'.repeat(64), matrixDigest: 'd'.repeat(64),
  oldDirectoryId: '913909', targetBaseDirectoryId: '913887', targetDirectoryId: null, recovered: false, recoveryEvidenceId: null })
const receipt = (id = FILE) => ({ ...facts(id), targetDirectoryId: '913910', recovered: true, recoveryEvidenceId: '9000' })
const compile = code => ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const flush = async () => { for (let i = 0; i < 8; i++) { await Promise.resolve(); await vue.nextTick() } }

async function mount(ports = {}) {
  const filename = 'src/views/dcc/controlled-file/browser/WorkingTypeStorageRecoveryDialog.vue'
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8')), script = compileScript(descriptor, { id: 'c10-recovery-dialog' })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'c10-recovery-dialog', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const calls = { previews: [], confirms: [], recovered: [], visible: [] }; let sequence = 0
  const deps = { vue, '@/utils': { generateUUID: () => `c10-request-${++sequence}` }, '@/api/dcc/controlledFile/workingTypeStorageRecovery': {
    previewWorkingTypeStorageRecovery: async id => { calls.previews.push(id); return ports.preview ? ports.preview(id) : facts(id) },
    confirmWorkingTypeStorageRecovery: async (id, command) => { calls.confirms.push({ id, command: JSON.parse(JSON.stringify(command)) }); return ports.confirm ? ports.confirm(id, command) : receipt(id) } } }
  const c = { exports: {}, require: id => { assert.ok(id in deps, id); return deps[id] }, Error, String }
  vm.runInNewContext(compile(script.content), c)
  const t = { exports: {}, require: () => vue }; vm.runInNewContext(compile(template.code), t); c.exports.default.render = t.exports.render
  const remove = n => { if (n.parent) { const i = n.parent.children.indexOf(n); if (i >= 0) n.parent.children.splice(i, 1); n.parent = null } }
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: text => ({ text }),
    insert: (n, p, a) => { remove(n); n.parent = p; const i = a ? p.children.indexOf(a) : -1; if (i < 0) p.children.push(n); else p.children.splice(i, 0, n) }, remove,
    setText: (n, text) => n.text = text, setElementText: (n, text) => { n.text = text; n.children = [] }, patchProp: (n, key, _old, value) => n.props[key] = value,
    parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null })
  const props = vue.reactive({ modelValue: true, fileId: FILE }), root = { children: [] }
  const app = renderer.createApp({ render: () => vue.h(c.exports.default, { ...props,
    'onUpdate:modelValue': value => { calls.visible.push(value); props.modelValue = value }, onRecovered: id => calls.recovered.push(id) }) })
  app.config.warnHandler = () => {}
  for (const tag of ['el-dialog', 'el-alert', 'el-form', 'el-form-item', 'el-input', 'el-button'])
    app.component(tag, { setup: (_, c) => () => vue.h(tag, c.attrs, [c.slots.default?.(), c.slots.footer?.()]) })
  const host = app.mount(root), state = host.$.subTree.component.setupState
  const find = id => { const visit = n => n.props?.['data-testid'] === id ? n : (n.children || []).map(visit).find(Boolean); return visit(root) }
  const click = async suffix => { const n = find('dcc-working-type-storage-recovery-' + suffix); assert.ok(n, suffix); await n.props.onClick(); await flush() }
  await flush()
  return { app, props, calls, state, find, click }
}

test('actual dialog previews same original identity, renders frozen facts and closes with zero confirmation writes', async () => {
  const h = await mount()
  try { assert.deepEqual(h.calls.previews, [FILE]); assert.ok(h.find('dcc-working-type-storage-recovery-facts')); await h.click('cancel'); assert.equal(h.calls.confirms.length, 0); assert.equal(h.props.modelValue, false) }
  finally { h.app.unmount() }
})
test('actual confirmation sends both proof and observed expectations once and accepts a matching receipt', async () => {
  let finish; const h = await mount({ confirm: () => new Promise(resolve => finish = resolve) })
  try {
    h.state.reason = ' Restore original working storage '; const pending = h.state.confirm(); await h.state.confirm()
    assert.equal(h.calls.confirms.length, 1); assert.equal(h.state.submitting, true)
    await h.click('cancel'); assert.equal(h.props.modelValue, true)
    const sent = h.calls.confirms[0]; assert.equal(sent.id, FILE); assert.equal(sent.command.expectedOriginalProofId, '8712')
    assert.equal(sent.command.expectedOriginalProofSha256, facts().originalProofSha256); assert.equal(sent.command.expectedObservedSha256, facts().observedSha256)
    assert.equal(sent.command.changeReason, 'Restore original working storage'); assert.equal(sent.command.confirmed, true)
    finish(receipt()); await pending; await flush(); assert.deepEqual(h.calls.recovered, [FILE]); assert.equal(h.props.modelValue, false)
  } finally { h.app.unmount() }
})
test('server error remains inline and retry preserves the exact request identity and reason', async () => {
  let fail = true; const h = await mount({ confirm: async () => { if (fail) throw Error('RECOVERY_SOURCE_CHANGED'); return receipt() } })
  try {
    h.state.reason = 'Original reason'; await h.click('confirm'); assert.match(h.state.error, /RECOVERY_SOURCE_CHANGED/)
    assert.ok(h.find('dcc-working-type-storage-recovery-error')); assert.equal(h.props.modelValue, true); assert.equal(h.calls.recovered.length, 0)
    fail = false; await h.click('confirm'); assert.equal(h.calls.confirms.length, 2); assert.deepEqual(h.calls.confirms[0], h.calls.confirms[1])
    assert.deepEqual(h.calls.recovered, [FILE])
  } finally { h.app.unmount() }
})
test('cancelled preview ignores its late result and reopening reads fresh original facts', async () => {
  let finish; const h = await mount({ preview: () => new Promise(resolve => finish = resolve) })
  try {
    await h.click('cancel'); finish(facts()); await flush(); assert.equal(h.state.preview, undefined); assert.equal(h.calls.confirms.length, 0)
    h.props.modelValue = true; await flush(); assert.equal(h.calls.previews.length, 2)
    finish(facts()); await flush(); assert.equal(h.state.preview.controlledFileId, FILE)
  } finally { h.app.unmount() }
})
test('file switch isolates both pending success and pending failure from the new dialog', async () => {
  for (const outcome of ['success', 'failure']) {
    let resolve, reject; const h = await mount({ confirm: () => new Promise((yes, no) => { resolve = yes; reject = no }) })
    try {
      h.state.reason = 'Restore original'; const old = h.state.confirm(); h.props.fileId = OTHER; await flush()
      if (outcome === 'success') resolve(receipt()); else reject(Error('OLD_FILE_FAILED'))
      await old; await flush(); assert.equal(h.state.preview.controlledFileId, OTHER); assert.equal(h.state.error, ''); assert.equal(h.calls.recovered.length, 0); assert.equal(h.props.modelValue, true)
    } finally { h.app.unmount() }
  }
})
test('unmounting during confirmation prevents a late receipt from emitting navigation or refresh', async () => {
  let finish; const h = await mount({ confirm: () => new Promise(resolve => finish = resolve) })
  h.state.reason = 'Restore'; const pending = h.state.confirm(); h.app.unmount()
  finish(receipt()); await pending; await flush()
  assert.deepEqual(h.calls.recovered, []); assert.deepEqual(h.calls.visible, [])
})
test('blank reason, already recovered object and mismatched receipt never report a new success', async () => {
  const blank = await mount()
  try { blank.state.reason = ' '; await blank.state.confirm(); assert.equal(blank.calls.confirms.length, 0) } finally { blank.app.unmount() }
  const done = await mount({ preview: async () => receipt() })
  try { assert.ok(done.find('dcc-working-type-storage-recovery-receipt')); assert.equal(done.find('dcc-working-type-storage-recovery-confirm'), undefined) } finally { done.app.unmount() }
  const wrong = await mount({ confirm: async () => ({ ...receipt(), originalProofId: '8713' }) })
  try { wrong.state.reason = 'Restore'; await wrong.click('confirm'); assert.match(wrong.state.error, /回执/); assert.equal(wrong.calls.recovered.length, 0); assert.equal(wrong.props.modelValue, true) } finally { wrong.app.unmount() }
})

test('actual API validates exact Long string identity, proof hashes and confirmation receipt', async () => {
  const calls = [], value = { exports: {}, require: () => ({ default: { get: async r => { calls.push(r); return facts() }, post: async r => { calls.push(r); return receipt() } } }), Error, String, BigInt }
  vm.runInNewContext(compile(fs.readFileSync('src/api/dcc/controlledFile/workingTypeStorageRecovery.ts', 'utf8')), value)
  const api = value.exports
  assert.equal((await api.previewWorkingTypeStorageRecovery(FILE)).controlledFileId, FILE)
  assert.equal(calls[0].url, `/dcc/controlled-files/${FILE}/working-type-storage-recovery/preview`)
  await assert.rejects(() => api.previewWorkingTypeStorageRecovery(2054545668044084206), /身份/)
  await api.confirmWorkingTypeStorageRecovery(FILE, { expectedOriginalProofId: '8712', expectedOriginalProofSha256: facts().originalProofSha256 })
  await assert.rejects(() => api.confirmWorkingTypeStorageRecovery(FILE, { expectedOriginalProofId: '8713', expectedOriginalProofSha256: facts().originalProofSha256 }), /回执/)
})
