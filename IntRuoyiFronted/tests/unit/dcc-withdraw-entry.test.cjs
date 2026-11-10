const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const filename = 'src/views/dcc/controlled-file/detail/index.vue'
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const ast = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const node = ast.statements.find(n => ts.isVariableStatement(n)
    && n.declarationList.declarations.some(d => d.name.text === name))
  assert.ok(node, name)
  return node.getText(ast)
}
const transpile = source => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const projection = { exports: {} }
vm.runInNewContext(transpile(fs.readFileSync('src/api/form-center/actionProjection.ts', 'utf8')), projection)
const lifecycle = { exports: {}, require: () => projection.exports }
vm.runInNewContext(transpile(fs.readFileSync('src/views/dcc/controlled-file/shared/lifecycle.ts', 'utf8')), lifecycle)

function host(overrides = {}) {
  const calls = [], successes = [], errors = []
  const ports = {
    exports: {}, computed: vue.computed,
    isWorkingBrowserDetailCurrent: vue.ref(true), checkPermi: () => true,
    fileStatus: vue.ref('PENDING_MATRIX_REVIEW'), currentUserId: vue.ref(71), approvalTodoTask: vue.ref(null),
    fileDetail: vue.ref({ id: '10', requesterId: 71,
      actionProjection: { canWithdraw: true, allowedActions: ['WITHDRAW'], actionLocked: true } }),
    ...lifecycle.exports,
    withdrawLoading: vue.ref(false), controlledFileId: vue.ref('10'), route: { fullPath: '/detail/10' },
    ensureDetailActionAllowed: () => true,
    message: { confirm: async () => {}, success: value => successes.push(value), error: value => errors.push(value) },
    withdrawControlledFile: async (...args) => calls.push(args), reloadAll: async () => calls.push('reload'),
    isDialogCancelled: error => error === 'cancel', resolveReadSideErrorMessage: error => error.message,
    ...overrides
  }
  vm.runInNewContext(transpile(declaration('toExactUserId') + '\n' + declaration('isCurrentFileRequester')
    + '\n' + declaration('canWithdraw') + '\n' + declaration('isReturnedApplicantTask') + '\n' + declaration('handleWithdraw')
    + '\nexports.canWithdraw=canWithdraw;exports.isReturnedApplicantTask=isReturnedApplicantTask;exports.withdraw=handleWithdraw;'), ports)
  return { ports, calls, successes, errors, ...ports.exports }
}

// Keep the real ancestors of each withdraw button, including page-mode guards.
function entryTemplate(node, root = false) {
  if (node.type !== 1) return ''
  if (node.props.some(p => p.name === 'data-testid' && p.value?.content === 'dcc-withdraw-application'))
    return node.loc.source
  const children = (node.children || []).map(child => entryTemplate(child)).join('')
  if (!children && !root) return ''
  if (!node.children?.length) return node.loc.source
  const start = node.children[0].loc.start.offset - node.loc.start.offset
  const end = node.children.at(-1).loc.end.offset - node.loc.start.offset
  return node.loc.source.slice(0, start) + children + node.loc.source.slice(end)
}
const pages = descriptor.template.ast.children.filter(node => node.type === 1)
const lastPage = pages.findIndex(node => node.tag === 'template' && node.props.some(p => p.name === 'else'))
assert.ok(lastPage >= 0)
const template = pages.slice(0, lastPage + 1).map(node => entryTemplate(node, true)).join('')

function renderEntry(mode, allowed) {
  const buttons = []
  const renderer = vue.createRenderer({
    createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}),
    insert: (child, parent) => parent.children.push(child), remove() {}, setText() {},
    setElementText: (node, text) => { node.text = text },
    patchProp: (node, key, previous, value) => {
      node[key] = value
      if (key === 'data-testid' && value === 'dcc-withdraw-application') buttons.push(node)
    }, parentNode: () => null, nextSibling: () => null
  })
  const render = vue.compile(template)
  const app = renderer.createApp({
    data: () => ({ distributionReadMode: false, accessExplanationError: '', fileDetail: {},
      viewerMode: mode === 'viewer', isBrowserTraceabilityPage: mode === 'trace',
      showDetailManagementActions: false, isApprovalUploadHandlingPage: true,
      canWithdraw: allowed, withdrawLoading: false, handleWithdraw() {} }), render
  })
  app.config.warnHandler = () => {}
  app.mount({ children: [] })
  app.unmount()
  return buttons
}

test('requester can see and click withdrawal in preview, trace and approval handling despite management actions being hidden', () => {
  for (const mode of ['viewer', 'trace', 'approval']) {
    const buttons = renderEntry(mode, true)
    assert.equal(buttons.length, 1, mode)
    assert.equal(typeof buttons[0].onClick, 'function')
    assert.equal(renderEntry(mode, false).length, 0, mode)
  }
})

test('withdrawal fails closed for other users, missing permission/projection, stale detail and completed statuses', async () => {
  for (const overrides of [
    { currentUserId: vue.ref(72) }, { checkPermi: () => false },
    { fileDetail: vue.ref({ id: '10', requesterId: 71 }) },
    { fileDetail: vue.ref({ id: '10', requesterId: 71,
      actionProjection: { canWithdraw: true, allowedActions: [] } }) },
    { isWorkingBrowserDetailCurrent: vue.ref(false) }, { fileStatus: vue.ref('ACTIVE') }
  ]) {
    const h = host(overrides)
    assert.equal(h.canWithdraw.value, false)
    await h.withdraw()
    assert.deepEqual(h.calls, [])
  }
})

test('initiator can withdraw partial and replacement revisions throughout supported pending stages', async () => {
  for (const versionChangeType of ['MINOR', 'MAJOR']) {
    for (const status of ['PENDING_SUPERVISOR_REVIEW', 'PENDING_DOC_CONTROL_REVIEW',
      'PENDING_MATRIX_REVIEW', 'PENDING_MATRIX_APPROVAL', 'PENDING_DOC_CONTROL_APPROVAL',
      'PENDING_APPLICANT_TRAINING_RECORD']) {
      const h = host({ fileStatus: vue.ref(status) })
      h.ports.fileDetail.value.changeType = 'REVISION'
      h.ports.fileDetail.value.versionChangeType = versionChangeType
      assert.equal(h.canWithdraw.value, true, `${versionChangeType}/${status}`)
      await h.withdraw()
      assert.equal(h.calls[0][0], '10')
      assert.equal(h.successes.length, 1)
    }
  }
})

test('initiator retains withdrawal when also the department lead and countersigner across exact ID representations', async () => {
  for (const [requesterId, userId] of [[910516, 910516], ['910516', 910516], [910516, '910516'],
    ['9007199254740993', '9007199254740993']]) {
    const h = host({ currentUserId: vue.ref(userId), fileDetail: vue.ref({
      id: '10', requesterId, departmentLeaderId: requesterId, actualSignoffUserIds: [requesterId],
      actionProjection: { canWithdraw: true, allowedActions: ['WITHDRAW'], actionLocked: true }
    }) })
    for (const mode of ['viewer', 'trace', 'approval'])
      assert.equal(renderEntry(mode, h.canWithdraw.value).length, 1, mode)
    await h.withdraw()
    assert.equal(h.calls[0][0], '10')
    assert.equal(h.successes.length, 1)
  }
})

test('missing, invalid or imprecise identities cannot authorize withdrawal', async () => {
  for (const [requesterId, userId] of [[undefined, undefined], [null, null], [0, 0], ['', ''],
    ['0910516', 910516], ['910516x', 910516], [9007199254740992, 9007199254740992],
    ['9007199254740993', '9007199254740992']]) {
    const h = host({ currentUserId: vue.ref(userId), fileDetail: vue.ref({ id: '10', requesterId,
      actionProjection: { canWithdraw: true, allowedActions: ['WITHDRAW'], actionLocked: true } }) })
    assert.equal(h.canWithdraw.value, false)
    await h.withdraw()
    assert.deepEqual(h.calls, [])
  }
})

test('returned applicant handling uses the same exact requester identity', () => {
  const h = host({ currentUserId: vue.ref('71'), fileStatus: vue.ref('PENDING_APPLICANT_REWORK'),
    approvalTodoTask: vue.ref({ id: 'applicant-rework' }) })
  assert.equal(h.isReturnedApplicantTask.value, true)
  assert.equal(h.canWithdraw.value, false)
  h.ports.currentUserId.value = 72
  assert.equal(h.isReturnedApplicantTask.value, false)
})

test('confirmation submits the current application and refreshes; cancel, navigation and server errors never report success', async () => {
  const h = host()
  await h.withdraw()
  assert.equal(h.calls[0][0], '10')
  assert.equal(h.calls[0][1].reason, '提交人主动撤回')
  assert.equal(h.calls[1], 'reload')
  assert.equal(h.successes.length, 1)
  assert.equal(h.ports.withdrawLoading.value, false)
  for (const outcome of ['cancel', 'navigate', 'denied', 'switch-user', 'server']) {
    const h = host()
    h.ports.message.confirm = async () => {
      if (outcome === 'cancel') throw 'cancel'
      if (outcome === 'navigate') h.ports.controlledFileId.value = '11'
      if (outcome === 'denied') h.ports.fileDetail.value.actionProjection.allowedActions = []
      if (outcome === 'switch-user') h.ports.currentUserId.value = 72
    }
    if (outcome === 'server') h.ports.withdrawControlledFile = async () => { throw new Error('撤回被拒绝') }
    await h.withdraw()
    assert.deepEqual(h.calls, [], outcome)
    assert.deepEqual(h.successes, [], outcome)
    assert.equal(h.errors.length, outcome === 'server' ? 1 : 0)
    assert.equal(h.ports.withdrawLoading.value, false)
  }
})

test('modified detail SFC compiles', () => {
  const result = compileTemplate({ filename, id: 'withdraw-entry', source: descriptor.template.content })
  assert.deepEqual(result.errors, [])
})
