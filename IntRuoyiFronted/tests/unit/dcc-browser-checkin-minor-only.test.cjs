const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const source = fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue', 'utf8')
const block = (name, next) => {
  const from = source.indexOf(`const ${name} =`), end = source.indexOf(`const ${next} =`, from)
  assert.ok(from > 0 && end > from); return source.slice(from, end)
}
const ref = value => ({ value })
const FILE = '9223372036854775701', NEXT = '9223372036854775702', MASTER = '9223372036854775700', USER = '9223372036854775707'
const stateFor = () => {
  const calls = { writes: [], errors: [], successes: [], merges: [], refreshes: 0 }
  const target = { id: FILE, masterId: MASTER, checkedOutBy: USER, checkedOut: true, versionNo: 'A/1', fileNumber: 'F1', status: 'ACTIVE', remark: 'old' }
  const state = { Error, JSON, Number, String, Promise,
    userStore: { getUser: { id: USER } }, checkinTarget: ref(target), checkinUpload: ref({ sessionId: 'dcc-checkin:ticket-session', uploadTicket: 'SOURCE', fileName: 'updated.docx' }),
    checkinUploadSessionId: ref('client-session'), checkinSourceState: ref('ready'), checkinDrawingPdfUpload: ref(undefined),
    checkinUploadLoading: ref(false), checkinDrawingPdfLoading: ref(false), checkinCleanupLoading: ref(false), checkinSubmitting: ref(false),
    checkinDialogVisible: ref(true), checkinForm: { versionChangeType: 'MINOR', changeDescription: 'real edit', remark: 'changed', needTraining: false },
    checkoutLoadingId: ref(undefined), checkinRefreshPending: ref(undefined), list: ref([{ ...target, projectFolderId: '9223372036854775790', versionHistory: [target] }]), total: ref(1),
    isValidBrowserOptionId: id => typeof id === 'string' && /^[1-9]\d*$/.test(id) || typeof id === 'number' && Number.isSafeInteger(id) && id > 0,
    isCheckedOutByCurrentUser: file => String(file.checkedOutBy) === USER,
    validateDrawingPdfUpload: () => ({ valid: true }), isDrawingSourceFile: () => false,
    findBrowserRowForVersion: id => String(id) === FILE ? { ...target, categoryId: '7', projectFolderId: '9223372036854775790' } : undefined,
    getList: async () => { calls.refreshes++ }, mergeCheckinResult: (...args) => calls.merges.push(args),
    checkinControlledFile: async (...args) => { calls.writes.push(args); return { ...target, id: NEXT, versionNo: 'A/1-1', status: 'WORKING', checkedOut: false, checkedOutBy: null } },
    message: { warning: value => calls.errors.push(value), error: value => calls.errors.push(value), success: value => calls.successes.push(value) },
    resolveBrowserErrorMessage: error => error.message,
    checkinSourceRequestSequence: 0, checkinDrawingPdfRequestSequence: 0, clearCheckinDrawingPdf: () => {},
    checkinDialogGeneration: 0, exports: {} }
  state.checkinUploadContext=ref({fileId:FILE,clientSessionId:'client-session',scopedSessionId:'dcc-checkin:ticket-session',ticket:'SOURCE'})
  state.route = { fullPath: '/dcc/controlled-file/browser' }
  state.browserMode = ref('storage')
  state.buildBrowserRouteStateKey = () => 'current-filter'
  state.getBrowserCacheContext = () => 'tenant1-user'
  vm.createContext(state)
  vm.runInContext(ts.transpileModule(block('submitCheckin', 'mergeCheckinResult') + '\nexports.submit = submitCheckin', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, state)
  return { state, calls }
}

test('actual submit always sends MINOR even if old form state carries MAJOR and no folder override', async () => {
  const { state, calls } = stateFor(); state.checkinForm.versionChangeType = 'MAJOR'
  await state.exports.submit()
  assert.equal(calls.writes.length, 1); assert.equal(calls.writes[0][0], FILE)
  const payload = calls.writes[0][1]
  assert.equal(payload.versionChangeType, 'MINOR'); assert.equal(payload.sessionId, 'dcc-checkin:ticket-session'); assert.equal(payload.uploadTicket, 'SOURCE')
  for (const forbidden of ['projectFolderId', 'directoryId', 'revisionChangeType', 'relatedControlledFileIds']) assert.equal(payload[forbidden], undefined)
})

test('actual checkin dialog renders working-only explanation and fixed minor submit action', () => {
  const from = source.indexOf('title="检入新版本"'), to = source.indexOf('</el-dialog>', from)
  const fragment = source.slice(source.lastIndexOf('<el-dialog', from), to + '</el-dialog>'.length)
  const declarations = { checkinDialogVisible: true, checkinSubmitting: false, checkinCleanupLoading: false, checkinUploadLoading: false,
    checkinForm: { versionChangeType: 'MAJOR', changeDescription: '', remark: '' }, checkinFileList: [], checkinUpload: undefined,
    checkinDrawingPdfFileList: [], checkinDrawingPdfLoading: false, canMajorCheckin: true,
    beforeCloseCheckin() {}, resetCheckinDialog() {}, cancelCheckin() {}, submitCheckin() {}, uploadCheckinSource() {}, clearCheckinUpload() {}, uploadCheckinDrawingPdf() {}, discardCheckinDrawingPdf() {}, isDrawingSourceFile: () => false }
  const script = Object.keys(declarations).map(key => `const ${key} = values.${key}`).join('\n')
  const compiled = compileScript(parse(`<template>${fragment}</template><script setup>const values = __values;${script}</script>`).descriptor, { id: 'checkin', inlineTemplate: true }).content
  const exported = {}; new Function('exports', 'require', '__values', ts.transpileModule(compiled, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(exported, () => vue, declarations)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: text => ({ text }),
    insert: (node, parent) => parent.children.push(node), remove() {}, setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text }, patchProp: (node, key, _old, value) => { node.props[key] = value }, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(exported.default); app.config.warnHandler = () => {}
  for (const tag of ['el-dialog', 'el-form', 'el-form-item', 'el-radio-group', 'el-radio', 'el-checkbox', 'el-upload', 'el-button', 'el-input', 'Icon']) app.component(tag, { setup: (_props, context) => () => vue.h(tag, context.attrs, [context.slots.default?.(), context.slots.footer?.()]) })
  const root = { children: [] }; app.mount(root)
  const text = node => (node.text || '') + (node.children || []).map(text).join('')
  try { assert.doesNotMatch(text(root), /大版本|下一修订版/); assert.match(text(root), /工作小版本/); assert.match(text(root), /局部变更.*换版变更.*审批/) }
  finally { app.unmount() }
})

test('another user checkout and unsafe target identities block actual submission', async () => {
  for (const change of [{ checkedOutBy: '8' }, { id: 9007199254740992 }]) {
    const { state, calls } = stateFor(); Object.assign(state.checkinTarget.value, change); await state.exports.submit()
    assert.equal(calls.writes.length, 0)
  }
})

test('late successful write does not close or merge into a newer checkin context', async () => {
  const { state, calls } = stateFor(); let finish
  state.checkinControlledFile = (...args) => { calls.writes.push(args); return new Promise(resolve => { finish = resolve }) }
  const old = state.exports.submit(); state.checkinTarget.value = { id: NEXT, masterId: MASTER, checkedOutBy: USER }
  state.checkinUploadSessionId.value = 'new-client-session'; state.checkinDialogGeneration++
  finish({ id: '9223372036854775703', masterId: MASTER, versionNo: 'A/1-1', status: 'WORKING' }); await old
  assert.equal(state.checkinDialogVisible.value, true); assert.equal(calls.merges.length, 0); assert.equal(calls.successes.length, 0)
})

test('public controlled-version terminology and formal selected-working entry stay independent', () => {
  assert.doesNotMatch(source, /当前有效版|当前有效文件|当前有效版本/)
  assert.match(block('handleSubmitWorkingIteration', 'getCheckoutDisplayName'), /const id = file.id[\s\S]*openManagement\(id\)/)
  assert.doesNotMatch(block('submitCheckin', 'mergeCheckinResult'), /submitControlledFileIteration|createMajorRevision|revisionChangeType/)
})

test('production identity validator rejects unsafe or nonpositive file and Master values', () => {
  const context = { exports: {} };vm.createContext(context)
  vm.runInContext(ts.transpileModule(block('isValidBrowserOptionId', 'categoryOptions') + '\nexports.check=isValidBrowserOptionId', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  assert.equal(context.exports.check(FILE), true)
  for (const invalid of [0, -1, 1.5, 9007199254740992, '0', '01', '9223372036854775808']) assert.equal(context.exports.check(invalid), false)
})

test('checkin succeeds for an officially listed history version whose VO omits Master but its row has exact Master', async () => {
  const { state, calls } = stateFor(); delete state.checkinTarget.value.masterId
  state.findBrowserRowForVersion = () => ({ id: NEXT, masterId: MASTER, categoryId: '7', versionHistory: [{ id: FILE }] })
  await state.exports.submit(); assert.equal(calls.writes.length, 1)
})

test('saved write plus list read failure closes completed form and explicitly says already checked in', async () => {
  const { state, calls } = stateFor(); state.getList = async () => { throw new Error('真实刷新失败') }
  await state.exports.submit()
  assert.equal(calls.writes.length, 1);assert.equal(state.checkinDialogVisible.value, false)
  assert.equal(calls.successes.length, 0);assert.ok(calls.errors.some(value => /已.*检入.*刷新列表失败/.test(value)))
  assert.equal(state.checkinRefreshPending.value.controlledFileId, NEXT)
})

test('new context entered during read after saved write cannot receive old merge or close', async () => {
  const { state, calls } = stateFor(); let finish
  state.getList = () => new Promise(resolve => { finish = resolve })
  const old=state.exports.submit();await new Promise(resolve=>setImmediate(resolve));assert.equal(typeof finish,'function')
  state.checkinTarget.value = { id: NEXT, checkedOutBy: USER };state.checkinDialogGeneration++
  state.checkinUploadSessionId.value = 'new-session';state.checkinDialogVisible.value=true;state.checkinSubmitting.value=true
  finish();await old
  assert.equal(calls.merges.length,0);assert.equal(state.checkinDialogVisible.value,true);assert.equal(state.checkinSubmitting.value,true)
})

test('source ticket from another file or client session cannot be submitted into the current checkout', async () => {
  for (const context of [{ fileId: NEXT, clientSessionId: 'client-session', scopedSessionId: 'dcc-checkin:ticket-session', ticket: 'SOURCE' },
    { fileId: FILE, clientSessionId: 'old-client-session', scopedSessionId: 'dcc-checkin:ticket-session', ticket: 'SOURCE' }]) {
    const {state,calls}=stateFor();state.checkinUploadContext=ref(context);await state.exports.submit();assert.equal(calls.writes.length,0)
  }
})

test('invalid working-version response is not announced as a successful controlled or formal version', async () => {
  for (const result of [{id:NEXT,masterId:MASTER,status:'ACTIVE',versionNo:'B/1'}, {id:NEXT,masterId:'7',status:'WORKING',versionNo:'A/1-1'},
    {id:NEXT,masterId:MASTER,status:'WORKING',versionNo:'B/1-1'}, {id:NEXT,masterId:MASTER,status:'WORKING',versionNo:'A/1-1'}]) {
    const {state,calls}=stateFor();state.checkinControlledFile=async()=>result;await state.exports.submit()
    assert.equal(calls.successes.length,0);assert.equal(calls.merges.length,0);assert.ok(calls.errors.length)
  }
})

test('drawing source and companion PDF must use the same actual scoped session', async () => {
  const {state,calls}=stateFor();state.checkinUpload.value.fileName='drawing.dwg';state.isDrawingSourceFile=()=>true
  state.checkinDrawingPdfUpload.value={sessionId:'other-scoped-session',uploadTicket:'DRAWING'}
  await state.exports.submit();assert.equal(calls.writes.length,0)
})

test('actual source upload binds exact target/client/scoped ticket and rejects an incomplete response', async () => {
  const {state}=stateFor();const uploads=[],success=[]
  state.checkinUploadContext.value=undefined
  state.uploadControlledFilePreview=async (...args)=>{uploads.push(args);return {sessionId:'scoped-new',uploadTicket:'SOURCE-NEW',fileName:'updated.docx'}}
  vm.runInContext(ts.transpileModule(block('uploadCheckinSource','uploadCheckinDrawingPdf')+'\nexports.upload=uploadCheckinSource',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,state)
  await state.exports.upload({file:{name:'updated.docx'},onSuccess:value=>success.push(value),onError:()=>{}})
  assert.equal(uploads[0][2].controlledFileId,FILE);assert.equal(uploads[0][2].sessionId,'client-session');assert.equal(uploads[0][2].uploadContext,'CHECKIN')
  assert.deepEqual(JSON.parse(JSON.stringify(state.checkinUploadContext.value)),{fileId:FILE,clientSessionId:'client-session',scopedSessionId:'scoped-new',ticket:'SOURCE-NEW'})
  state.uploadControlledFilePreview=async()=>({uploadTicket:'NO-SESSION',fileName:'updated.docx'})
  await state.exports.upload({file:{name:'updated.docx'},onSuccess:value=>success.push(value),onError:()=>{}})
  assert.equal(state.checkinUpload.value,undefined);assert.equal(state.checkinUploadContext.value,undefined);assert.equal(state.checkinSourceState.value,'failed')
})

test('remark-only checkin remains legal and sends no invented source or placement ticket', async () => {
  const {state,calls}=stateFor();state.checkinUpload.value=undefined;state.checkinUploadContext.value=undefined;state.checkinSourceState.value='idle'
  await state.exports.submit();assert.equal(calls.writes.length,1)
  assert.equal(calls.writes[0][1].versionChangeType,'MINOR');assert.equal(calls.writes[0][1].uploadTicket,undefined);assert.equal(calls.writes[0][1].sessionId,undefined)
})

test('duplicate click while writing creates one request and old failure does not pollute new dialog', async () => {
  const {state,calls}=stateFor();let fail
  state.checkinControlledFile=(...args)=>{calls.writes.push(args);return new Promise((_resolve,reject)=>{fail=reject})}
  const old=state.exports.submit();await state.exports.submit();assert.equal(calls.writes.length,1)
  state.checkinDialogGeneration++;state.checkinTarget.value={id:NEXT,checkedOutBy:USER};state.checkinSubmitting.value=true
  const before=calls.errors.length;fail(new Error('原文件保存失败'));await old
  assert.equal(calls.errors.length,before);assert.equal(state.checkinSubmitting.value,true);assert.equal(state.checkinDialogVisible.value,true)
})
