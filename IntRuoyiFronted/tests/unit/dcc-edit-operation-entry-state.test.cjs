const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = 'src/views/dcc/controlled-file/'
const script = file => parse(fs.readFileSync(file, 'utf8')).descriptor.scriptSetup.content
function declaration(file, names) {
  const ast = ts.createSourceFile(file, script(file), ts.ScriptTarget.Latest, true)
  return names.map(name => {
    const node = ast.statements.find(node => ts.isVariableStatement(node) && node.declarationList.declarations.some(item => item.name.text === name))
    assert.ok(node, `${file}: ${name}`)
    return node.getText(ast)
  }).join('\n')
}
function gates(file, names, state) {
  const host = { ...vue, exports: {}, fileChangesLocked: vue.ref(false), detailReadSelectionState: vue.ref({ selection: undefined, error: '' }), ...state }
  vm.runInNewContext(ts.transpileModule(declaration(file, names) + '\nexports.gates={' + names.join(',') + '}', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return host.exports.gates
}
const detail = root + 'detail/index.vue'
const erpPermissionModule = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/utils/erpDataPermission.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, { exports: erpPermissionModule.exports })
const hasPermissionSource = fs.readFileSync('src/directives/permission/hasPermi.ts', 'utf8')
const permissionAst = ts.createSourceFile('hasPermi.ts', hasPermissionSource, ts.ScriptTarget.Latest, true)
const permissionNode = permissionAst.statements.find(node => ts.isVariableStatement(node) && node.declarationList.declarations.some(item => item.name.text === 'hasPermission')).getText(permissionAst)
function permissionCheck(permissions) {
  const host = { exports: {}, requiresExplicitErpGrant: erpPermissionModule.exports.requiresExplicitErpGrant, userStore: { permissions: new Set(permissions) }, all_permission: '*:*:*' }
  vm.runInNewContext(ts.transpileModule(permissionNode, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, host)
  return host.exports.hasPermission
}
function elements(file) {
  const template = parse(fs.readFileSync(file, 'utf8')).descriptor.template.content
  const result = compileTemplate({ id: file, source: template })
  assert.deepEqual(result.errors, [])
  const nodes = []
  function visit(node) {
    if (node.type === 1) nodes.push(node)
    for (const child of node.children || []) visit(child)
    for (const branch of node.branches || []) visit(branch)
  }
  visit(parse(fs.readFileSync(file, 'utf8')).descriptor.template.ast)
  return nodes
}
const prop = (node, name) => node.props.find(item => item.type === 7 && item.name === name)
const attr = (node, name) => node.props.find(item => item.type === 6 && item.name === name)?.value?.content
const versionDisplay = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(root + 'shared/version-display.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText, versionDisplay)

test('actual print gates accept global EDIT or old print, while server action projection still denies ungranted files', () => {
  for (const permission of ['dcc:controlled-file:edit', 'dcc:controlled-file:print']) {
    const state = { checkPermi: permissionCheck([permission]), detailActionState: vue.ref({ canPrint: true }) }
    const gate = gates(detail, ['hasControlledPrintMenuPermission', 'controlledPrintAllowed'], state)
    assert.equal(gate.controlledPrintAllowed.value, true)
    state.detailActionState.value.canPrint = false
    assert.equal(gate.controlledPrintAllowed.value, false)
  }
  assert.equal(gates(detail, ['hasControlledPrintMenuPermission'], { checkPermi: permissionCheck(['dcc:controlled-file:query']) }).hasControlledPrintMenuPermission.value, false)
})

test('actual withdraw gate requires EDIT/submit plus requester, server WITHDRAW action and permitted status', () => {
  const state = { checkPermi: permissionCheck(['dcc:controlled-file:edit']), isWorkingBrowserDetailCurrent: vue.ref(true), fileStatus: vue.ref('PENDING_MATRIX_REVIEW'),
    isDccControlledFileWithdrawableStatus: value => value === 'PENDING_MATRIX_REVIEW', isCurrentFileRequester: vue.ref(true),
    fileDetail: vue.ref({ actionProjection: { canWithdraw: true } }), isDccControlledFileActionAllowed: (_, action) => action === 'WITHDRAW' }
  const gate = gates(detail, ['canWithdraw'], state)
  assert.equal(gate.canWithdraw.value, true)
  state.isCurrentFileRequester.value = false; assert.equal(gate.canWithdraw.value, false)
  state.isCurrentFileRequester.value = true; state.fileDetail.value.actionProjection.canWithdraw = false
  assert.equal(gate.canWithdraw.value, false)
})

test('actual distribution gates accept EDIT without an obsolete role gate, retaining exact process and ready context', () => {
  const roles = vue.reactive(['ordinary_editor'])
  const state = { checkPermi: permissionCheck(['dcc:controlled-file:edit']), userStore: { getRoles: roles }, DOC_CONTROL_ROLE_CODE: 'doc_control', fileDetail: vue.ref({ processInstanceId: 'exact-round' }) }
  const detailGate = gates(detail, ['canReadDistributionReceipts'], state)
  assert.equal(detailGate.canReadDistributionReceipts.value, true)
  state.fileDetail.value = {}; assert.equal(detailGate.canReadDistributionReceipts.value, false)
  const ready = vue.ref('tenant31/user99')
  const workbench = gates(root + 'workbench/index.vue', ['canHandleWorkflowDistribution'], {
    userStore: { getIsSetUser: true, getRoles: ['ordinary_editor'], getPermissions: new Set(['dcc:controlled-file:edit']) }, workflowDistributionContextKey: ready
  })
  assert.equal(workbench.canHandleWorkflowDistribution.value, true)
  ready.value = ''; assert.equal(workbench.canHandleWorkflowDistribution.value, false)
})

test('detail print directives use existing OR permission semantics', () => {
  const nodes = [
    ...elements(detail).filter(node => prop(node, 'hasPermi')?.exp?.content.includes('dcc:controlled-file:print'))
  ]
  assert.equal(nodes.length, 2)
  for (const node of nodes) {
    const permission = vm.runInNewContext(prop(node, 'hasPermi').exp.content)
    assert.equal(permissionCheck(['dcc:controlled-file:edit'])(permission), true)
    assert.equal(permissionCheck([permission[0]])(permission), true)
    assert.equal(permissionCheck(['dcc:controlled-file:query'])(permission), false)
  }
})

test('both browser revision entries stay visible and explain denied clicks without opening management', () => {
  const browser = root + 'browser/index.vue'
  const nodes = elements(browser).filter(node => ['dcc-browser-partial-revision', 'dcc-browser-replacement-revision'].includes(attr(node, 'data-testid')))
  assert.equal(nodes.length, 2)
  for (const node of nodes) {
    assert.equal(prop(node, 'hasPermi'), undefined)
    assert.equal(node.props.some(item => item.name === 'disabled' || item.arg?.content === 'disabled'), false)
    const file = { id: '12', status: 'ACTIVE', canEditFile: false }
    const warnings = [], opened = []
    const state = { checkPermi: permissionCheck(['dcc:controlled-file:edit']),
      getDccVersionDisplayText: versionDisplay.exports.getDccVersionDisplayText,
      message: { warning: value => warnings.push(value) }, openManagement: (...args) => opened.push(args) }
    const gate = gates(browser, ['hasRevisionBaseline', 'showRevisionActions', 'canStartRevision', 'openRevision', 'hasBrowserRowActions'], {
      ...state, getSelectedVersion: row => row, getBrowserRowActionState: () => ({}),
      isValidBrowserOptionId: id => id === '12'
    })
    const context = { ...gate, row: file, getSelectedVersion: row => row, isValidBrowserOptionId: id => id === '12' }
    const visible = () => vm.runInNewContext(prop(node, 'if').exp.content, context)
    const click = () => vm.runInNewContext(prop(node, 'on').exp.content, context)
    assert.equal(visible(), true)
    assert.equal(gate.hasBrowserRowActions(file), true)
    click()
    assert.equal(warnings.pop(), '当前账号没有本文件编辑权限')
    assert.equal(opened.length, 0)
    state.checkPermi = permissionCheck([])
    const noGlobalGrant = gates(browser, ['hasRevisionBaseline', 'canStartRevision', 'openRevision'], state)
    noGlobalGrant.openRevision({ ...file, canEditFile: true }, 'PARTIAL')
    assert.equal(warnings.pop(), '当前账号没有本文件编辑权限')
    assert.equal(opened.length, 0)
    file.canEditFile = true
    file.checkedOut = true
    click()
    assert.match(warnings.pop(), /文件已检出/)
    assert.equal(opened.length, 0)
    file.checkedOut = false
    click()
    assert.equal(opened.length, 1)
    assert.equal(opened[0][0], '12')
    assert.equal(opened[0][1].revisionIntent, attr(node, 'data-testid').includes('partial') ? 'PARTIAL' : 'REPLACEMENT')
    file.status = 'WORKING'
    file.versionNo = 'A/1'
    assert.equal(visible(), true)
    click()
    assert.equal(warnings.pop(), '当前文件状态不支持发起变更')
    assert.equal(opened.length, 1)
    for (const versionNo of ['A/1-1', 'AAA/1-3']) {
      file.versionNo = versionNo
      for (const status of ['WORKING', 'SUBMITTED', 'REJECTED', 'WITHDRAWN', 'IN_APPROVAL', 'OBSOLETE']) {
        file.status = status
        assert.equal(visible(), true)
        click()
        assert.equal(warnings.pop(), '当前文件状态不支持发起变更')
        assert.equal(opened.length, 1)
      }
    }
    file.status = 'WORKING'
    file.revisionBaseActiveControlledFileId = '11'
    click()
    assert.equal(opened[1][0], '11')
    assert.equal(opened[1][1].revisionIterationId, '12')
    file.versionNo = 'A/1'
    file.status = 'OBSOLETE'
    assert.equal(visible(), false)
    for (const versionNo of [undefined, '', 'A/1', 'A/1-0']) {
      file.versionNo = versionNo
      assert.equal(visible(), false)
    }
  }
})

test('checkout entries follow checkout state without hiding for permissions and reject denied or stale clicks', async () => {
  const browser = root + 'browser/index.vue'
  const nodes = elements(browser).filter(node => ['dcc-controlled-browser-checkout', 'dcc-controlled-browser-checkin', 'dcc-controlled-browser-cancel-checkout'].includes(attr(node, 'data-testid')))
  assert.equal(nodes.length, 3)
  const warnings = []
  let granted = true
  const state = { isValidBrowserOptionId: id => id === '12',
    checkPermi: () => granted, canEditVersion: file => file.canEditFile === true,
    canCheckoutVersion: file => file.canEditFile === true,
    isCheckedOutByCurrentUser: file => file.checkedOutBy === 'me',
    message: { warning: text => warnings.push(text) } }
  const gate = gates(browser, ['getCheckoutActionBlockReason', 'handleCheckout', 'handleCheckin', 'handleCancelCheckout'], state)
  for (const node of nodes) {
    const checkout = attr(node, 'data-testid') === 'dcc-controlled-browser-checkout'
    const file = { id: '12', status: 'ACTIVE', canEditFile: false, checkedOut: !checkout }
    const context = { ...gate, ...state, row: file, getSelectedVersion: row => row }
    const visible = () => Boolean(vm.runInNewContext(prop(node, 'if').exp.content, context))
    const click = () => vm.runInNewContext(prop(node, 'on').exp.content, context)
    assert.equal(prop(node, 'hasPermi'), undefined)
    assert.equal(node.props.some(item => item.name === 'disabled' || item.arg?.content === 'disabled'), false)
    assert.equal(visible(), true)
    await click()
    assert.equal(warnings.pop(), '当前账号没有本文件编辑权限')
    file.canEditFile = true
    granted = false
    await click()
    assert.equal(warnings.pop(), '当前账号没有本文件编辑权限')
    granted = true
    file.checkedOutBy = checkout ? 'me' : 'other'
    assert.equal(visible(), !checkout)
    await click()
    assert.match(warnings.pop(), checkout ? /文件已检出/ : /仅检出人可以/)
    if (!checkout) {
      file.checkedOutBy = null
      file.checkedOut = false
      assert.equal(visible(), false)
      await click()
      assert.match(warnings.pop(), /文件尚未检出/)
    }
  }
  const own = { id: '12', canEditFile: true, checkedOutBy: 'me' }
  assert.equal(gate.getCheckoutActionBlockReason(own, 'CHECKIN'), '')
  assert.equal(gate.getCheckoutActionBlockReason(own, 'CANCEL_CHECKOUT'), '')
  assert.equal(gate.getCheckoutActionBlockReason({ ...own, checkedOutBy: null }, 'CHECKOUT'), '')
  assert.match(gate.getCheckoutActionBlockReason({ ...own, id: null }, 'CHECKIN'), /有效编号/)
})

test('actual resubmit and revision sections retain status/requester conditions with EDIT instead of old submit leaf', () => {
  const nodes = elements(detail).filter(node => prop(node, 'if')?.exp?.content.includes("checkPermi(['dcc:controlled-file:submit'"))
  assert.equal(nodes.length, 2)
  const state = { fileChangesLocked: false, checkPermi: permissionCheck(['dcc:controlled-file:edit']), fileDetail: { status: 'REJECTED', canEditFile: true, processDefinitionKey: 'dcc-controlled-file-upload' },
    showDetailManagementActions: true, isCurrentFileRequester: true, pendingRevisions: [], isExternalReviewProcess: false, canPrepareFileApplication: true }
  const evaluate = node => vm.runInNewContext(prop(node, 'if').exp.content, state)
  assert.equal(evaluate(nodes[0]), true)
  state.isCurrentFileRequester = false; assert.equal(evaluate(nodes[0]), false)
  state.fileDetail.status = 'ACTIVE'; assert.equal(evaluate(nodes[1]), true)
  state.showDetailManagementActions = false; assert.equal(evaluate(nodes[1]), false)
})

 test('actual metadata entry accepts global EDIT without doc-control role and preserves read-only view restrictions', () => {
  const state = { checkPermi: permissionCheck(['dcc:controlled-file:edit']), DOC_CONTROL_ROLE_CODE: 'doc_control',
    viewerMode: vue.ref(false), showDetailManagementActions: vue.ref(true), userStore: { getRoles: ['ordinary_editor'] },
    fileDetail: vue.ref({ id: '70', canEditFile: true }), hasDccControlledFileActionProjection: () => true }
  const gate = gates(detail, ['hasMetadataEditorRole', 'canEditMetadata'], state)
  assert.equal(gate.canEditMetadata.value, true)
  state.viewerMode.value = true; assert.equal(gate.canEditMetadata.value, false)
  state.viewerMode.value = false; state.showDetailManagementActions.value = false
  assert.equal(gate.canEditMetadata.value, false)
})

test('actual approval-history buttons require global EDIT and their own file-bound projection, independently of ACTIVE body printing', () => {
  const nodes = elements(detail).filter(node => prop(node, 'on')?.exp?.content.includes("handleDetailMoreCommand('print-process')")
    || prop(node, 'on')?.exp?.content.includes("handleDetailMoreCommand('export-process-word')"))
  assert.equal(nodes.length, 2)
  assert.deepEqual(nodes.map(node => prop(node, 'if').exp.content), ['approvalHistoryPrintAllowed', 'approvalHistoryExportAllowed'])
  for (const permissions of [[], ['dcc:controlled-file:query'], ['dcc:controlled-file:print'], ['dcc:controlled-file:edit']]) {
    const state = { checkPermi: permissionCheck(permissions), fileDetail: vue.ref({ status: 'PENDING_MATRIX_REVIEW', canPrintApprovalHistory: true, canExportApprovalHistory: true }) }
    const gate = gates(detail, ['approvalHistoryPrintAllowed', 'approvalHistoryExportAllowed'], state)
    assert.equal(gate.approvalHistoryPrintAllowed.value, permissions.includes('dcc:controlled-file:edit'))
    assert.equal(gate.approvalHistoryExportAllowed.value, permissions.includes('dcc:controlled-file:edit'))
    state.fileDetail.value.canPrintApprovalHistory = false
    assert.equal(gate.approvalHistoryPrintAllowed.value, false)
    state.fileDetail.value.canExportApprovalHistory = undefined
    assert.equal(gate.approvalHistoryExportAllowed.value, false)
  }
})

test('actual approval-history handlers block both template and local output before opening a window or loading/exporting data', async () => {
  for (const active of [false, true]) {
    const calls = []
    const printWindow = { document: { open: () => {}, write: html => calls.push(['write', html]), close: () => {} },
      focus: () => {}, print: () => calls.push(['print']), close: () => calls.push(['close']) }
    const state = { approvalHistoryPrintAllowed: vue.ref(false), approvalHistoryExportAllowed: vue.ref(false),
      processPrintLoading: vue.ref(false), processExportLoading: vue.ref(false), activeApprovalPrintTemplate: vue.ref({ active }),
      window: { open: () => { calls.push(['window']); return printWindow } },
      message: { warning: value => calls.push(['denied', value]), error: value => { throw Error(value) } },
      controlledFileId: vue.ref('777'), fileDetail: vue.ref({ fileNumber: 'TASK-777' }),
      getControlledFileApprovalPrintHtml: async id => { calls.push(['html-api', id]); return { html: 'server-html' } },
      exportControlledFileApprovalWord: async id => { calls.push(['word-api', id]); return 'server-docx' },
      loadProcessPrintData: async () => { calls.push(['bpm-data']); return 'bpm-facts' },
      buildProcessPrintHtml: data => { calls.push(['local-html', data]); return 'local-html' },
      downloadByData: (...args) => calls.push(['download', ...args]) }
    const handlers = gates(detail, ['handlePrintProcess', 'handleExportProcessWord'], state)
    await handlers.handlePrintProcess()
    await handlers.handleExportProcessWord()
    assert.deepEqual(calls.map(call => call[0]), ['denied', 'denied'])
    assert.equal(state.processPrintLoading.value, false)
    assert.equal(state.processExportLoading.value, false)
    calls.length = 0
    state.approvalHistoryPrintAllowed.value = true
    state.approvalHistoryExportAllowed.value = true
    await handlers.handlePrintProcess()
    await handlers.handleExportProcessWord()
    assert.equal(calls.filter(call => call[0] === 'print').length, 1)
    assert.equal(calls.filter(call => call[0] === 'download').length, 1)
    assert.equal(calls.filter(call => call[0] === 'html-api').length, active ? 1 : 0)
    assert.equal(calls.filter(call => call[0] === 'word-api').length, active ? 1 : 0)
    assert.equal(calls.filter(call => call[0] === 'bpm-data').length, active ? 0 : 2)
    assert.equal(state.processPrintLoading.value, false)
    assert.equal(state.processExportLoading.value, false)
  }
})


test('actual application section requires exact canEditFile for WORKING, ACTIVE and future controlled files', () => {
  const state = { fileDetail: vue.ref({ status: 'ACTIVE', canEditFile: false }) }
  const gate = gates(detail, ['canPrepareFileApplication'], state)
  for (const status of ['WORKING', 'ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE']) {
    state.fileDetail.value.status = status
    for (const granted of [false, undefined, true]) {
      state.fileDetail.value.canEditFile = granted
      assert.equal(gate.canPrepareFileApplication.value, granted === true)
    }
  }
  for (const status of ['SUBMITTED', 'OBSOLETE']) {
    state.fileDetail.value = { status, canEditFile: true }
    assert.equal(gate.canPrepareFileApplication.value, false)
  }
  const node = elements(detail).find(node => node.tag === 'DetailApplicationPanel')
  const context = { fileDetail: {status:'ACTIVE'}, pendingRevisions: [], showDetailManagementActions: true, isExternalReviewProcess: false,
    checkPermi: permissionCheck(['dcc:controlled-file:edit']), canPrepareFileApplication: false }
  assert.equal(vm.runInNewContext(prop(node, 'if').exp.content, context), false)
  context.canPrepareFileApplication = true
  assert.equal(vm.runInNewContext(prop(node, 'if').exp.content, context), true)
})

test('actual application open handler rejects absent or false file edit projection before reading a route or changing the form', async () => {
  const panel = root + 'detail/DetailApplicationPanel.vue'
  elements(panel)
  for (const value of [false, undefined]) {
    const state = { props:{file:{canEditFile:value}}, error:vue.ref(''), busy:vue.ref(false), submitted:vue.ref(false) }
    const handlers = gates(panel,['open'],state)
    await handlers.open('PARTIAL')
    assert.equal(state.error.value,'当前账号未获本文件编辑授权，不能准备申请')
  }
})

test('actual handling summary does not advertise downloads denied by the server', () => {
  const state = { fileDetail: vue.ref({status:'ACTIVE',canDownload:false}), approvalProgressScope:vue.ref(null),
    getControlledFileHandlingSummary: () => ({nextStep:'download-allowed',responsibilityHint:'normal'}) }
  const gate=gates(detail,['detailHandlingSummary'],state)
  assert.equal(gate.detailHandlingSummary.value.nextStep, '可查看当前受控版本')
  state.fileDetail.value.canDownload=true
  assert.equal(gate.detailHandlingSummary.value.nextStep,'download-allowed')
})

test('actual receipt buttons and local output handlers require exact file edit authorization', () => {
  const buttons = elements(detail).filter(node => ['handleExportDistributionReceipts', 'handlePrintDistributionReceipts'].includes(prop(node, 'on')?.exp?.content))
  assert.equal(buttons.length, 2)
  for (const granted of [undefined, false, true]) {
    const calls = []
    const state = { fileDetail: vue.ref({ canEditFile: granted, fileNumber: 'TASK-4509' }), controlledFileId: vue.ref('4509'),
      distributionReceiptRows: vue.ref([{}]), buildDistributionReceiptRows: () => { calls.push('rows'); return [{}] },
      escapeCsvCell: String, downloadByData: () => calls.push('download'), buildDistributionReceiptPrintHtml: () => 'receipt',
      message: { warning: () => calls.push('denied'), error: () => calls.push('error') },
      window: { open: () => { calls.push('window'); return { document: { open() {}, write() {}, close() {} }, focus() {}, print: () => calls.push('print') } } } }
    for (const node of buttons) assert.equal(vm.runInNewContext(prop(node, 'if').exp.content, { fileDetail: state.fileDetail.value }), granted === true)
    const handlers = gates(detail, ['handleExportDistributionReceipts', 'handlePrintDistributionReceipts'], state)
    handlers.handleExportDistributionReceipts(); handlers.handlePrintDistributionReceipts()
    assert.deepEqual(calls, granted === true ? ['rows', 'download', 'window', 'print'] : ['denied', 'denied'])
  }
})

test('actual returned-application wrapper and restart handler retain original conditions and exact edit authorization', async () => {
  const node = elements(detail).find(node => node.tag === 'ContentWrap' && prop(node, 'if')?.exp?.content.includes("['REJECTED', 'WITHDRAWN']"))
  for (const granted of [undefined, false, true]) {
    for (const status of ['REJECTED', 'WITHDRAWN', 'ACTIVE']) {
      const file = { id: '4509', status, canEditFile: granted, processDefinitionKey: 'dcc-controlled-file-upload' }
      const context = { fileChangesLocked: false, fileDetail: file, showDetailManagementActions: true, isCurrentFileRequester: true, checkPermi: permissionCheck(['dcc:controlled-file:edit']) }
      assert.equal(vm.runInNewContext(prop(node, 'if').exp.content, context), granted === true && status !== 'ACTIVE')
      context.isCurrentFileRequester = false
      assert.equal(vm.runInNewContext(prop(node, 'if').exp.content, context), false)
    }
    const calls = []
    const state = { fileDetail: vue.ref({ id: '4509', canEditFile: granted }), applicationRestartLoading: vue.ref(false),
      ensureCurrentDetailContext: () => true, message: { warning: () => calls.push('denied') },
      prepareControlledFileApplicationRestart: async id => { calls.push(['prepare', id]); return '4513' }, router: { push: async route => calls.push(['route', route.path]) } }
    await gates(detail, ['restartCurrentApplication'], state).restartCurrentApplication('PARTIAL')
    assert.deepEqual(calls, granted === true ? [['prepare', '4509'], ['route', '/dcc/controlled-file/detail/4513']] : ['denied'])
    assert.equal(state.applicationRestartLoading.value, false)
  }
})

test('actual metadata initialization matches Long wire project identity and preserves registered storage without legacy subtree assumptions', async () => {
  const file = root + 'shared/ControlledFileMetadataDialog.vue'
  elements(file)
  const sourceFile = { id: '4509', dccProjectCodeId: '9007199254740993', categoryId: 10, fileTypeTaxonomyId: 198,
    hasProjectStorageMapping: true, directoryId: 913952, directoryPath: '质量管理/项目#322 文件夹#66' }
  const state = { props: vue.reactive({ file: sourceFile }), metadataForm: { fileName: 'source.pdf', dccProjectCodeId: sourceFile.dccProjectCodeId,
    categoryId: 10, fileTypeTaxonomyId: 198, directoryId: 913952, productCode: '', productName: '', needTraining: false },
    metadataDialog: { fieldErrors: {}, inlineError: '' }, projectCodeOptions: vue.ref([]), projectCodeOptionsLoading: vue.ref(false),
    DCC_PROJECT_CODE_STATUS_ENABLE: 'ENABLE', getProjectCodeOptions: async () => [{ id: '9007199254740993', projectCode: 'REAL-CODE', projectName: '真实项目' }],
    categoryOptions: vue.ref([{ id: 10, directoryId: 123 }]), directoryOptions: vue.ref([{ value: 123 }]) }
  const names = ['hasRegisteredStorage', 'selectedCategory', 'selectedProjectCode', 'selectedCategoryUsesUnclassifiedDirectory',
    'applyDccProjectCodeProductNumber', 'loadProjectCodeOptions', 'validateMetadataDialog']
  const h = gates(file, names, state)
  await h.loadProjectCodeOptions()
  assert.equal(state.metadataForm.dccProjectCodeId, '9007199254740993')
  assert.equal(state.metadataForm.productCode, 'REAL-CODE')
  assert.equal(state.metadataForm.productName, '真实项目')
  assert.equal(h.validateMetadataDialog(), true)
  state.props.file.directoryPath = null
  assert.equal(h.validateMetadataDialog(), false)
  assert.match(state.metadataDialog.inlineError, /登记目录身份/)
  state.props.file.hasProjectStorageMapping = false
  assert.equal(h.validateMetadataDialog(), false)
  assert.match(state.metadataDialog.fieldErrors.directoryId, /类别绑定范围/)
  state.metadataForm.directoryId = 123
  assert.equal(h.validateMetadataDialog(), true)
  assert.equal(state.metadataForm.dccProjectCodeId, '9007199254740993')
  const mixedIdentity = gates(file, ['selectedProjectCode'], { metadataForm: { dccProjectCodeId: '322' },
    projectCodeOptions: vue.ref([{ id: 322, projectCode: 'EXACT-322' }]) })
  assert.equal(mixedIdentity.selectedProjectCode.value.projectCode, 'EXACT-322')
})

test('actual body download entry requires global EDIT, explicit server download flag and DOWNLOAD action', async () => {
  const node = elements(detail).find(node => prop(node, 'on')?.exp?.content === "handleDetailMoreCommand('download-body')")
  assert.ok(node)
  for (const [globalEdit, flag, action] of [[false, true, true], [true, false, true], [true, undefined, true], [true, true, false], [true, true, true]]) {
    const state = { checkPermi: permissionCheck(globalEdit ? ['dcc:controlled-file:edit'] : ['dcc:controlled-file:query']),
      fileDetail: vue.ref({ canDownload: flag }), isDccControlledFileActionAllowed: (_, requested) => requested === 'DOWNLOAD' && action }
    const g = gates(detail, ['controlledDownloadAllowed'], state)
    assert.equal(g.controlledDownloadAllowed.value, globalEdit && flag === true && action)
    assert.equal(vm.runInNewContext(prop(node, 'if').exp.content, { checkoutDownloadAllowed: false, controlledDownloadAllowed: g.controlledDownloadAllowed.value }), globalEdit && flag === true && action)
  }
  for (const [allowed, context] of [[false, true], [true, false], [true, true]]) {
    const calls = []
    const state = { checkoutDownloadAllowed: vue.ref(false), controlledDownloadAllowed: vue.ref(allowed), controlledDownloadLoading: vue.ref(false), controlledFileId: vue.ref('4509'),
      message: { warning: () => calls.push('denied'), error: () => calls.push('error') },
      ensureDetailActionAllowed: action => { calls.push(['context', action]); return context },
      triggerControlledFileDownload: async id => calls.push(['existing-confirm-audited-download', id]) }
    await gates(detail, ['handleDownloadControlledBody'], state).handleDownloadControlledBody()
    assert.deepEqual(calls, !allowed ? ['denied'] : !context ? [['context', 'DOWNLOAD']] : [['context', 'DOWNLOAD'], ['existing-confirm-audited-download', '4509']])
    assert.equal(state.controlledDownloadLoading.value, false)
  }
})
