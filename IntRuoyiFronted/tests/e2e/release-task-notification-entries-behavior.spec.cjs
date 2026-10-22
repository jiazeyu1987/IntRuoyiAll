const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const pagePaths = {
  pqc: 'src/views/mes/pro/production-release/PqcProductionReleasePage.vue',
  manager: 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue',
  history: 'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue'
}
const queryPermission = 'mes:pro-edhr-work-task:query'
const page = key => parse(fs.readFileSync(path.join(root, pagePaths[key]), 'utf8')).descriptor
const makeNode = (type, text = '') => ({ type, text, props: {}, children: [], parent: null, get parentNode() { return this.parent } })
const detach = node => { if (node.parent) { const rows = node.parent.children; rows.splice(rows.indexOf(node), 1); node.parent = null } }
const renderer = vue.createRenderer({
  createElement: makeNode, createText: text => makeNode('text', text), createComment: text => makeNode('comment', text),
  insert(node, parent, anchor) { detach(node); node.parent = parent; parent.removeChild = detach; const index = anchor ? parent.children.indexOf(anchor) : -1; if (index < 0) parent.children.push(node); else parent.children.splice(index, 0, node) },
  remove: detach, parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null,
  setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text; node.children = [] }, patchProp(node, key, oldValue, value) { node.props[key] = value }
})
const allNodes = node => [node, ...node.children.flatMap(allNodes)]

function operationTemplate(descriptor) {
  const columns = []
  const visit = node => {
    if (node.tag === 'el-table-column' && node.props?.some(prop => prop.type === 6 && prop.name === 'label' && prop.value?.content === '操作')) columns.push(node)
    for (const child of node.children || []) visit(child)
  }
  visit(descriptor.template.ast)
  assert.equal(columns.length, 1, '实际正式列表应有唯一操作列')
  const slot = columns[0].children.find(node => node.tag === 'template' && node.props?.some(prop => prop.type === 7 && prop.name === 'slot' && prop.arg?.content === 'default'))
  assert.ok(slot, '实际操作列必须按正式row渲染')
  return slot.children.map(node => node.loc.source).join('')
}

function installActualPermission(app, permissions) {
  const source = fs.readFileSync(path.join(root, 'src/directives/permission/hasPermi.ts'), 'utf8')
  const ast = ts.createSourceFile('permission.ts', source, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(statement => !ts.isImportDeclaration(statement)).map(statement => statement.getText(ast)).join('\n').replace(/\bexport\s+/g, '')
  const code = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
  Function('useUserStore', 'useI18n', `${code}; return hasPermi;`)(() => ({ permissions: new Set(permissions) }), () => ({ t: text => text }))(app)
}

function renderRow(kind, row, { permissions = [queryPermission], activeView = 'RELEASED' } = {}) {
  const descriptor = page(kind)
  const sourceAst = ts.createSourceFile(`${kind}.ts`, descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const entryImport = sourceAst.statements.find(statement => ts.isImportDeclaration(statement) && statement.importClause?.name?.text === 'ReleaseTaskNotificationEntry')
  assert.ok(entryImport, '真实页面必须导入通知组件，不能只在测试中注册入口')
  assert.equal(path.resolve(path.dirname(path.join(root, pagePaths[kind])), entryImport.moduleSpecifier.text), path.join(root, 'src/views/mes/pro/production-release/components/ReleaseTaskNotificationEntry.vue'), '真实页面必须导入同一正式组件')
  const captured = [], clicks = []
  let resolveBatchVoidOperationState
  if (kind === 'manager') {
    const ast = ts.createSourceFile('manager.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
    const statement = ast.statements.find(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => declaration.name.getText(ast) === 'resolveBatchVoidOperationState'))
    assert.ok(statement)
    const code = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
    resolveBatchVoidOperationState = Function('isVoidedBatchExecutionStatus', 'hasGoldenFingerActionBypass', `${code}; return resolveBatchVoidOperationState;`)(status => Number(status) === 60, vue.ref(false))
  }
  const app = renderer.createApp({
    render: vue.compile(operationTemplate(descriptor)),
    setup: () => ({ row, activeView, PQC_RELEASE_VIEW_PENDING: 'PENDING', reverseTraceAllowed: false, resolveBatchVoidOperationState,
      hasActiveOrderDetail: value => Boolean(value.activeOrderId), openActiveOrderDetail: value => clicks.push(['detail', value]), openReleaseDialog: value => clicks.push(['release', value]),
      openReverseTrace: value => clicks.push(['reverse', value]), openNonconformanceReview: value => clicks.push(['nonconformance', value]), handleRejectClick: value => clicks.push(['reject', value]),
      handleWithdrawVoidRequest: value => clicks.push(['withdraw', value]), openVoidDialog: value => clicks.push(['void', value]), openActiveOrderOtherUploadTab: value => clicks.push(['upload', value]) })
  })
  app.component('ElButton', { setup: (props, { attrs, slots }) => () => vue.h('button', attrs, slots.default?.()) })
  app.component('ReleaseTaskNotificationEntry', {
    props: ['workTaskId', 'batchExecutionId', 'releaseTransactionId'],
    setup(props, { attrs }) { captured.push(props); return () => vue.h('button', { ...attrs, 'data-release-task-notification-entry': '' }, '交接通知') }
  })
  installActualPermission(app, permissions)
  const container = makeNode('root')
  app.mount(container)
  return { app, captured, clicks, nodes: () => allNodes(container), entries: () => allNodes(container).filter(node => Object.hasOwn(node.props, 'data-release-task-notification-entry')) }
}

for (const status of ['PENDING', 'RELEASED', 'VOIDED', 'REWORKED', 'CONCESSION_RELEASED']) {
  test(`NOTIFY-UI01: actual PQC ${status} row exposes exact formal task notification entry`, () => {
    const row = { applicationId: '224', pqcReleaseWorkTaskId: '9007199254740993', activeOrderId: '1009200409', viewStatus: status }
    const view = renderRow('pqc', row, { activeView: status })
    try {
      assert.equal(view.entries().length, 1, '交接通知必须在当前行可达，不能只存在于待放行签名弹框')
      assert.equal(view.captured[0].workTaskId, row.pqcReleaseWorkTaskId)
      assert.equal(view.captured[0].releaseTransactionId, undefined)
      assert.equal(view.captured[0].batchExecutionId, undefined)
      assert.equal(view.clicks.length, 0, '只渲染通知入口不得执行放行等业务动作')
    } finally { view.app.unmount() }
  })
}

for (const kind of ['manager', 'history']) {
  test(`NOTIFY-UI01: actual ${kind} row passes exact formal batch and transaction, never a guessed work task`, () => {
    const row = { id: '900000001225', releaseTransactionId: '226', activeOrderId: '1009200409', status: kind === 'history' ? 40 : 15, releaseStatus: kind === 'history' ? 'RELEASED' : 'PENDING_APPROVAL', releaseActionLocked: true }
    const view = renderRow(kind, row)
    try {
      assert.equal(view.entries().length, 1, '经理普通列表及已放行历史均须有独立交接通知入口')
      assert.equal(String(view.captured[0].batchExecutionId), row.id)
      assert.equal(String(view.captured[0].releaseTransactionId), row.releaseTransactionId)
      assert.equal(view.captured[0].workTaskId, undefined, '此VO没有taskId；只能经正式txn精确读回')
      assert.equal(view.clicks.length, 0)
    } finally { view.app.unmount() }
  })
}

test('NOTIFY-UI01 control: all three actual row surfaces keep notification entry inaccessible without query permission', () => {
  for (const kind of ['pqc', 'manager', 'history']) {
    const view = renderRow(kind, { id: '900000001225', releaseTransactionId: '226', pqcReleaseWorkTaskId: '2734', activeOrderId: '1009200409', status: 40 }, { permissions: [] })
    try { assert.equal(view.entries().length, 0); assert.equal(view.clicks.length, 0) } finally { view.app.unmount() }
  }
})

test('NOTIFY-UI01 control: actual released history detail still opens only the selected formal row', () => {
  const row = { id: '900000001225', releaseTransactionId: '226', status: 40, releaseStatus: 'RELEASED' }
  const view = renderRow('history', row)
  try {
    const detail = view.nodes().find(node => Object.hasOwn(node.props, 'data-edhr-history-detail-action'))
    assert.ok(detail)
    detail.props.onClick()
    assert.deepEqual(view.clicks, [['detail', row]])
  } finally { view.app.unmount() }
})
