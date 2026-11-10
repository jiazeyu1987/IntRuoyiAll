const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const script = file => read(file).match(/<script[^>]*setup[^>]*>([\s\S]*?)<\/script>/)[1]
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const facts = { exports: {} }
vm.runInNewContext(transpile(read('src/views/dcc/controlled-file/workflow/distribution-facts.ts')), facts)
const ref = value => ({ value })
function receipt(save) {
  const calls = [], watchers = [], props = { controlledFileId: '9007199254740993', distributionId: '72', recipientId: '73',
    versionNo: 'A/2', contextKey: 'one', canAcknowledge: true, save }
  const context = { exports: {}, defineProps: () => props, defineEmits: () => (...args) => calls.push(args),
    require: name => name === 'vue' ? { ref, watch: (source, handler) => watchers.push(handler), onBeforeUnmount: () => {} }
      : name === './distribution-facts' ? facts.exports : (() => { throw Error('Unexpected module ' + name) })() }
  vm.runInNewContext(transpile(script('src/views/dcc/controlled-file/workflow/DistributionReceiptConfirm.vue')
    + '\nexports.state = { password, passwordFocused, comment, error, saved, confirmReceipt }'), context)
  return { state: context.exports.state, calls, watchers, props }
}
test('receipt requires the actual successful save, clears password and emits the exact recipient identity once', async () => {
  const saves = [], h = receipt(async command => { saves.push(command); return true })
  assert.equal(h.state.password.value, '')
  h.state.password.value = 'typed-at-signing'; h.state.comment.value = ' 收到 '
  await h.state.confirmReceipt(); await h.state.confirmReceipt()
  assert.equal(saves.length, 1); assert.equal(saves[0].controlledFileId, '9007199254740993')
  assert.equal(saves[0].comment, '收到'); assert.equal(h.state.password.value, '')
  assert.equal(h.calls.length, 1); assert.equal(h.calls[0][0], 'saved')
  assert.equal(h.calls[0][1].recipientId, '73')
})
test('failed or unconfirmed receipt never emits success or marks the task complete', async () => {
  for (const save of [async () => false, async () => { throw new Error('签名权限已撤销') }]) {
    const h = receipt(save); h.state.password.value = 'typed'
    await h.state.confirmReceipt()
    assert.equal(h.calls.length, 0); assert.equal(h.state.saved.value, false); assert.ok(h.state.error.value)
  }
})
test('changing the exact task clears secrets and ignores a previous successful in-flight receipt', async () => {
  let finish; const h = receipt(() => new Promise(resolve => { finish = resolve }))
  h.state.password.value = 'typed'; h.state.passwordFocused.value = true
  const pending = h.state.confirmReceipt(); h.props.contextKey = 'two'; h.watchers[0]()
  finish(true); await pending
  assert.equal(h.state.password.value, ''); assert.equal(h.state.passwordFocused.value, false)
  assert.equal(h.calls.length, 0); assert.equal(h.state.saved.value, false)
})
function declarations(names, file = 'src/views/dcc/controlled-file/browser/index.vue') {
  const source = script(file), ast = ts.createSourceFile('component.ts', source, ts.ScriptTarget.ES2022, true, ts.ScriptKind.TS)
  return ast.statements.filter(row => row.declarationList?.declarations.some(item => names.includes(item.name.text)))
    .map(row => row.getText(ast)).join('\n')
}
test('a new checkin session leaves the remark empty even when the selected version has an old remark', () => {
  const c = { checkinSubmitting: ref(false), checkinCleanupLoading: ref(false), checkinUploadLoading: ref(false),
    checkinDrawingPdfLoading: ref(false), checkinAttachmentState: ref({}), checkinTarget: ref(), checkinDialogVisible: ref(false),
    checkinForm: { remark: 'previous-dialog-text' }, isValidBrowserOptionId: () => true, isCheckedOutByCurrentUser: () => true,
    requireCheckinTrainingDefault: () => true, resetCheckinDialog: () => { c.checkinForm.remark = '' }, message: { warning: assert.fail, error: assert.fail } }
  vm.runInNewContext(transpile(declarations(['handleCheckin']) + '\nthis.open = handleCheckin'), c)
  c.open({ id: '55', remark: 'last-version-note' })
  assert.equal(c.checkinForm.remark, ''); assert.equal(c.checkinForm.needTraining, true); assert.equal(c.checkinDialogVisible.value, true)
})
test('revision entry keeps the formal baseline and selected working ID exact and blocks checked-out or unrelated work', () => {
  const c = { canEditVersion: file => file.requesterId === '9', isDccControlledFileActionAllowed: file => file.major === true,
    calls: [], openManagement: (...args) => c.calls.push(args), Error }
  vm.runInNewContext(transpile(declarations(['hasRevisionBaseline', 'canStartRevision', 'openRevision'])
    + '\nthis.visible = hasRevisionBaseline; this.allowed = canStartRevision; this.open = openRevision'), c)
  const work = { id: '9007199254740993', requesterId: 'other-author', canEditFile: true, status: 'WORKING', revisionBaseActiveControlledFileId: '9007199254740991' }
  c.open(work, 'PARTIAL')
  assert.equal(c.calls[0][0], '9007199254740991'); assert.equal(c.calls[0][1].revisionIterationId, '9007199254740993')
  assert.equal(c.allowed({ ...work, checkedOutBy: '9' }, 'PARTIAL'), false)
  assert.equal(c.allowed({ ...work, revisionBaseActiveControlledFileId: undefined }, 'PARTIAL'), false)
  assert.equal(c.allowed(work, 'REPLACEMENT'), true)
  assert.equal(c.visible(work), true)
  c.open(work, 'REPLACEMENT')
  assert.equal(c.calls.length, 2)
  assert.equal(c.allowed({ ...work, major: true }, 'REPLACEMENT'), true)
  for (const intent of ['PARTIAL', 'REPLACEMENT']) {
    assert.equal(c.allowed({ ...work, requesterId: '9', canEditFile: false, major: true }, intent), false)
    assert.equal(c.allowed({ ...work, canEditFile: undefined }, intent), false)
    assert.equal(c.allowed({ ...work, checkedOut: true }, intent), false)
    assert.equal(c.allowed({ ...work, status: 'IN_APPROVAL' }, intent), false)
    assert.equal(c.allowed({ ...work, status: 'ACTIVE' }, intent), true)
  }
  c.open({ ...work, major: true }, 'REPLACEMENT')
  assert.equal(c.calls[2][0], '9007199254740991')
  assert.equal(c.calls[2][1].revisionIntent, 'REPLACEMENT')
  assert.equal(c.calls[2][1].revisionIterationId, '9007199254740993')
  assert.equal(c.visible({ ...work, status: 'ACTIVE' }), true)
  assert.equal(c.visible({ ...work, status: 'OBSOLETE' }), false)
})
test('receipt parent connects a confirmed saved event to the registered todo page', () => {
  const source = read('src/views/dcc/controlled-file/detail/DistributionReadPanel.vue')
  assert.match(source.match(/<DistributionReceiptConfirm[\s\S]*?\/>/)[0], /@saved="returnToTodo"/)
  const calls = [], c = { router: { push: route => calls.push(route) } }
  vm.runInNewContext(transpile(declarations(['returnToTodo'], 'src/views/dcc/controlled-file/detail/DistributionReadPanel.vue')
    + '\nthis.back = returnToTodo'), c)
  c.back(); assert.equal(calls[0].name, 'ApprovalCenterTodo')
})

test('browser version mapping keeps explicit edit authorization scoped to the exact version', () => {
 const c = {}
 vm.runInNewContext(transpile(declarations(['buildCurrentVersionOption','hydrateCurrentBrowserVersionActionState'])
   + '\nthis.build=buildCurrentVersionOption;this.hydrate=hydrateCurrentBrowserVersionActionState'), c)
 const row={id:'55',canEditFile:true}
 assert.equal(c.build(row).canEditFile,true)
 assert.equal(c.hydrate(row,{id:'55'}).canEditFile,true)
 assert.equal(c.hydrate(row,{id:'55',canEditFile:false}).canEditFile,false)
 assert.equal(c.hydrate(row,{id:'56'}).canEditFile,undefined)
 assert.equal(c.hydrate(row,{id:'56',canEditFile:false}).canEditFile,false)
})

test('checkin selects the refreshed exact draft without overwriting its edit grant with the mutation response', () => {
  for (const granted of [true, false]) {
    const baseline = { id: '900', status: 'ACTIVE' }
    const draft = Object.freeze({ id: '902', status: 'WORKING', canEditFile: granted,
      revisionBaseActiveControlledFileId: '900', checkedOut: false })
    const row = { id: '902', versionHistory: [draft, baseline] }
    const c = { list: ref([row]), browserMode: ref('working') }
    vm.runInNewContext(transpile(declarations(['mergeCheckinResult', 'buildCurrentVersionOption', 'hasRevisionBaseline', 'canStartRevision'])
      + '\nthis.merge=mergeCheckinResult;this.allowed=canStartRevision'), c)
    c.merge({ ...draft, canEditFile: !granted }, '900')
    assert.equal(row.selectedVersionId, '902')
    assert.equal(row.versionHistory.length, 2)
    assert.equal(row.versionHistory[0], draft)
    for (const intent of ['PARTIAL', 'REPLACEMENT']) assert.equal(c.allowed(draft, intent), granted)
  }
})
