const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const vue = require('vue')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const script = ts.createSourceFile('BatchExecutionListPage.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const operationTemplate = descriptor.template.content.match(/<el-table-column\b[^>]*label="操作"[^>]*>\s*<template #default="\{ row \}">([\s\S]*?)<\/template>/)?.[1]
assert.ok(operationTemplate, '实际批次列表必须有可编译的操作区插槽')

function productionFunctions(names, context) {
  const statements = script.statements.filter(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(script))))
  assert.equal(statements.length, names.length, `必须找到实际页面函数 ${names.join(', ')}`)
  const code = ts.transpileModule(statements.map(statement => statement.getText(script)).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None }
  }).outputText
  return Function(...Object.keys(context), `${code}; return { ${names.join(', ')} };`)(...Object.values(context))
}

const makeNode = (type, text = '') => ({ type, text, props: {}, children: [], parentNode: null })
function detach(node) {
  if (!node.parentNode) return
  const siblings = node.parentNode.children
  siblings.splice(siblings.indexOf(node), 1)
  node.parentNode = null
}
function attach(node, parent, anchor) {
  detach(node)
  node.parentNode = parent
  parent.removeChild = detach
  const index = anchor ? parent.children.indexOf(anchor) : -1
  if (index < 0) parent.children.push(node)
  else parent.children.splice(index, 0, node)
}
const renderer = vue.createRenderer({
  insert: attach,
  remove: detach,
  createElement: type => makeNode(type),
  createText: text => makeNode('text', text),
  createComment: text => makeNode('comment', text),
  setText: (node, text) => { node.text = text },
  setComment: (node, text) => { node.text = text },
  setElementText: (node, text) => { node.text = text; node.children = [] },
  parentNode: node => node.parentNode,
  nextSibling: node => node.parentNode?.children[node.parentNode.children.indexOf(node) + 1] || null,
  patchProp: (node, key, previous, next) => { node.props[key] = next }
})
function descendants(node) {
  return [node, ...node.children.flatMap(descendants)]
}
const visibleText = node => node.text + node.children.filter(child => child.type !== 'comment').map(visibleText).join('')

function renderOperations(row, permissions) {
  const clicked = []
  const warnings = []
  const users = { permissions: new Set(permissions) }
  const permissionSource = fs.readFileSync(path.join(root, 'src/directives/permission/hasPermi.ts'), 'utf8')
  const permissionAst = ts.createSourceFile('hasPermi.ts', permissionSource, ts.ScriptTarget.Latest, true)
  const permissionBody = permissionAst.statements.filter(statement => !ts.isImportDeclaration(statement)).map(statement => statement.getText(permissionAst)).join('\n').replace(/\bexport\s+/g, '')
  const permissionCode = ts.transpileModule(permissionBody, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
  const installPermission = Function('useUserStore', 'useI18n', `${permissionCode}; return hasPermi;`)(() => users, () => ({ t: text => text }))
  const state = productionFunctions(['resolveBatchVoidOperationState'], {
    isVoidedBatchExecutionStatus: status => String(status) === '60',
    hasGoldenFingerActionBypass: vue.ref(false)
  })
  const app = renderer.createApp({
    render: vue.compile(operationTemplate),
    setup: () => ({
      row,
      ...state,
      getList: () => {},
      openActiveOrderDetail: value => clicked.push(['detail', value]),
      openReleaseDialog: value => clicked.push(['release', value]),
      handleRejectClick: value => clicked.push(['reject', value]),
      handleWithdrawVoidRequest: value => clicked.push(['withdraw', value]),
      openVoidDialog: value => clicked.push(['void', value]),
      openActiveOrderOtherUploadTab: value => clicked.push(['upload', value])
    })
  })
  app.component('ElButton', { setup: (props, { attrs, slots }) => () => vue.h('button', attrs, slots.default?.()) })
  app.component('ReleaseTaskNotificationEntry', {
    props: ['workTaskId', 'batchExecutionId', 'releaseTransactionId'],
    setup: (props, { attrs }) => () => vue.h('span', attrs)
  })
  app.component('MarketReleaseHandover', {
    setup: () => () => [vue.h('button', { 'data-test-handover': true }, '交接'), vue.h('span', '交接窗口')]
  })
  app.config.warnHandler = message => warnings.push(message)
  installPermission(app)
  const container = makeNode('root')
  app.mount(container)
  return { app, clicked, warnings, buttons: descendants(container).filter(node => node.type === 'button') }
}

const ownedBatch = () => ({ id: '900000001225', activeOrderId: '1009200409', batchExecutionCode: 'EDHR-OWNED-1225', status: 15, releaseActionLocked: true })
const approvePermission = 'mes:pro-edhr-release:approve'

test('handover action renders without applying a runtime directive to its fragment component', () => {
  const view = renderOperations(ownedBatch(), [approvePermission, 'mes:pro-edhr-release:query'])
  try {
    assert.equal(view.buttons.filter(button => button.props['data-test-handover']).length, 1)
    assert.equal(view.warnings.some(warning => warning.includes('Runtime directive used on component')), false)
  } finally { view.app.unmount() }
})

test('MANAGER-ENTRY01: release-locked owned batch exposes formal market-release entry and exact clicked identity', () => {
  const row = ownedBatch()
  const view = renderOperations(row, [approvePermission])
  const release = view.buttons.find(button => button.props['data-edhr-batch-action'] === 'release')
  assert.ok(release, '放行审批中不能因为 releaseActionLocked 隐藏已有正式上市放行入口')
  assert.equal(visibleText(release).trim(), '放行')
  release.props.onClick()
  assert.equal(view.clicked.length, 1)
  assert.equal(view.clicked[0][0], 'release')
  assert.equal(view.clicked[0][1], row, '入口必须传入当前批次整行，不得选择其他批次')
  view.app.unmount()
})

test('formal market-release entry remains protected by existing approve permission', () => {
  const view = renderOperations(ownedBatch(), [])
  assert.deepEqual(view.buttons.map(visibleText).map(text => text.trim()), ['详情'])
  view.app.unmount()
})

test('release lock blocks void and preserves the authorized formal dossier upload entry', () => {
  const view = renderOperations(ownedBatch(), [approvePermission, 'mes:pro-edhr-change:void', 'mes:pro-edhr-batch-execution:upload'])
  const upload = view.buttons.find(button => button.props['data-edhr-batch-action'] === 'upload')
  assert.ok(upload, '待上市放行仍须允许已有资料权限上传，实际生命周期由正式上传页面和后端核对')
  upload.props.onClick()
  assert.equal(view.clicked[0][0], 'upload')
  assert.equal(view.buttons.some(button => visibleText(button).trim() === '作废'), false)
  view.app.unmount()
})

test('formal dossier upload entry uses the three existing writer permissions without granting unrelated readers access', () => {
  const writerPermissions = [
    'mes:pro-process-pool-team-leader:maintain',
    'mes:pro-production-release:pqc-approve',
    'mes:pro-edhr-batch-execution:upload'
  ]
  const readerPermissions = [[], ['mes:pro-edhr-batch-execution:query'], [approvePermission],
    ['mes:pro-edhr-batch-execution:report-upload'], ['mes:pro-production-release:pqc-query']]
  for (const releaseActionLocked of [false, true]) {
    for (const permission of writerPermissions) {
      const row = { ...ownedBatch(), releaseActionLocked }
      const view = renderOperations(row, [permission])
      try {
        const uploads = view.buttons.filter(button => button.props['data-edhr-batch-action'] === 'upload')
        assert.equal(uploads.length, 1, permission)
        uploads[0].props.onClick()
        assert.equal(view.clicked[0][0], 'upload')
        assert.equal(view.clicked[0][1], row)
      } finally { view.app.unmount() }
    }
    for (const permissions of readerPermissions) {
      const view = renderOperations({ ...ownedBatch(), releaseActionLocked }, permissions)
      try {
        assert.equal(view.buttons.some(button => button.props['data-edhr-batch-action'] === 'upload'), false)
      } finally { view.app.unmount() }
    }
  }
  for (const state of [{ pendingVoidChangeEventId: '99', canWithdrawVoidRequest: true },
    { pendingVoidChangeEventId: '99', canWithdrawVoidRequest: false }, { status: 60 }]) {
    const view = renderOperations({ ...ownedBatch(), ...state }, writerPermissions)
    try {
      assert.equal(view.buttons.some(button => button.props['data-edhr-batch-action'] === 'upload'), false)
    } finally { view.app.unmount() }
  }
})

test('pending void and voided batches do not expose market-release entry', () => {
  for (const extra of [{ pendingVoidChangeEventId: '99', canWithdrawVoidRequest: true }, { pendingVoidChangeEventId: '99', canWithdrawVoidRequest: false }, { status: 60 }]) {
    const view = renderOperations({ ...ownedBatch(), ...extra }, [approvePermission])
    assert.equal(view.buttons.some(button => button.props['data-edhr-batch-action'] === 'release'), false)
    view.app.unmount()
  }
})

test('normal batch retains the existing formal market-release entry', () => {
  const view = renderOperations({ ...ownedBatch(), releaseActionLocked: false }, [approvePermission])
  assert.equal(view.buttons.filter(button => button.props['data-edhr-batch-action'] === 'release').length, 1)
  view.app.unmount()
})

function releaseHarness(rows) {
  const writes = [], queries = [], navigation = [], invalidations = []
  const sessionKey = 'ordinary-test-tenant-session'
  const context = {
    selectedReleaseBatch: vue.ref(), releaseContext: vue.ref(), releasePreview: vue.ref(), releaseCompletedReadOnly: vue.ref(false), releaseTransactionMissing: vue.ref(false), releaseError: vue.ref(''),
    releaseForm: vue.reactive({ password: '', reason: '', idempotencyKey: '' }), releaseDialogVisible: vue.ref(false), releaseContextLoading: vue.ref(false), releaseLoading: vue.ref(false),
    message: { error() {}, success() {}, warning() {} }, crypto: { randomUUID: () => '409ca1df-f5b1-4c8e-b506-dca2a21a0f65' },
    route: { fullPath: '/mes/pro/feedback/edhr-batch-execution' },
    approvalInvalidation: {
      syncSession: () => ({ key: sessionKey }),
      isCurrent: key => key === sessionKey,
      captureEdhrSuccess: source => ({ ...source, session: { key: sessionKey } }),
      publishEdhrSuccess: event => invalidations.push(event)
    },
    getEdhrReleasePage: async query => { queries.push(query); return { list: rows } },
    getApprovalReviewPreview: async () => formalPreview(),
    reviewApprovalTask: async request => { writes.push(request); return { receiptId: 'receipt-1', signatureId: 'signature-1', prepareId: request.prepareId, idempotencyKey: request.idempotencyKey } }, router: { push: async route => { navigation.push(route) } },
    resolveErrorMessage: (error, defaultMessage) => error.message || defaultMessage
  }
  return { ...productionFunctions(['marketReleaseRouteGeneration', 'marketReleaseWrites', 'marketReleaseWriteScope', 'isDefinitiveMarketReleaseFailure', 'resetMarketReleaseDialog', 'openReleaseDialog', 'submitRelease'], context), context, writes, queries, navigation, invalidations }
}
const formalContext = () => ({ batchExecutionId: '900000001225', releaseTransactionId: '226', releaseApprovalWorkTaskId: '2735', version: 7, approvalSignoffEvidenceHash: 'formal-signoff-hash' })
const formalPreview = () => ({ prepareId: 'prepared-2735', content: { subjectVersion: 'version-7', contentHash: 'preview-hash', contentPolicyVersion: 'approval-content-active-order-v2', canonicalBusinessContentJson: '{"batch":"900000001225"}', allowedActions: ['APPROVE'] } })

test('formal dialog signs the exact task and preview content after collecting the opinion', async () => {
  const h = releaseHarness([{ ...formalContext(), batchExecutionId: 'other', releaseTransactionId: 'wrong' }, formalContext()])
  await h.openReleaseDialog(ownedBatch())
  assert.equal(h.context.releaseContext.value.releaseTransactionId, '226')
  assert.equal(h.writes.length, 0, '打开入口只能读取，不得批准')
  h.context.releaseForm.password = 'test-only-password'
  h.context.releaseForm.reason = '  核对资料后同意放行  '
  await h.submitRelease()
  assert.deepEqual(h.writes, [{ moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '2735', result: 'APPROVE', reason: '核对资料后同意放行', idempotencyKey: '409ca1df-f5b1-4c8e-b506-dca2a21a0f65', signaturePassword: 'test-only-password', prepareId: 'prepared-2735', expectedSubjectVersion: 'version-7', expectedContentHash: 'preview-hash', contentPolicyVersion: 'approval-content-active-order-v2' }])
  assert.equal(h.navigation[0].query.batchExecutionId, '900000001225')
  assert.equal(h.invalidations.length, 1)
  assert.equal(h.invalidations[0].sourceTaskId, '2735', '成功后必须失效同一正式审批待办')
  assert.equal(h.context.releaseForm.password, '', '成功后清除签名密码')
  await h.submitRelease()
  assert.equal(h.writes.length, 1, '已关闭的成功对话框不得重复批准')
})

test('missing exact formal batch context and missing password cannot submit an approval', async () => {
  const missing = releaseHarness([{ ...formalContext(), batchExecutionId: 'other' }])
  await missing.openReleaseDialog(ownedBatch())
  assert.match(missing.context.releaseError.value, /未查询到此批次/)
  missing.context.releaseForm.password = 'test-only-password'
  await missing.submitRelease()
  assert.equal(missing.writes.length, 0)
  const noPassword = releaseHarness([formalContext()])
  await noPassword.openReleaseDialog(ownedBatch())
  noPassword.context.releaseForm.reason = '同意放行'
  await noPassword.submitRelease()
  assert.match(noPassword.context.releaseError.value, /电子签名密码/)
  assert.equal(noPassword.writes.length, 0)
})

test('missing preview or opinion blocks signing and a malformed receipt never claims success', async () => {
  const h = releaseHarness([formalContext()])
  await h.openReleaseDialog(ownedBatch())
  h.context.releaseForm.password = 'test-only-password'
  await h.submitRelease()
  assert.match(h.context.releaseError.value, /放行意见/)
  h.context.releaseForm.reason = '同意放行'
  h.context.releasePreview.value = undefined
  await h.submitRelease()
  assert.match(h.context.releaseError.value, /完整放行资料/)
  assert.equal(h.writes.length, 0)
  h.context.releasePreview.value = formalPreview()
  h.context.reviewApprovalTask = async () => true
  // Recreate with the actual production function and a response without a signed receipt.
  const action = productionFunctions(['submitRelease'], { ...h.context, marketReleaseWrites: new Map(), marketReleaseRouteGeneration: 0, marketReleaseWriteScope: () => 'task-2735', isDefinitiveMarketReleaseFailure: () => false })
  await action.submitRelease()
  assert.equal(h.context.releaseDialogVisible.value, true)
  assert.equal(h.navigation.length, 0)
  assert.equal(h.invalidations.length, 0)
  assert.match(h.context.releaseError.value, /回执未确认成功/)
})
