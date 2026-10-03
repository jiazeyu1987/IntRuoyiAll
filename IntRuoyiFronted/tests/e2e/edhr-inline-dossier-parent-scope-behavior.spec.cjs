const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const sourcePath = path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const descriptor = parse(fs.readFileSync(sourcePath, 'utf8')).descriptor
const ast = ts.createSourceFile('actual-inline-parent.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const tick = async () => { await vue.nextTick(); await Promise.resolve() }
const ownedBatch = (id = '900000001225') => ({ id, activeOrderId: '1009200409', status: 40 })
const formalDetail = () => ({ activeOrderId: '1009200409', activeOrderStatus: { status: 'RELEASED' }, processes: [{ routeProcessId: '721', processName: '正式生产工序', pqcSubmissions: [] }] })

function actualChildTemplate() {
  const candidates = []
  const visit = node => {
    if (node.tag === 'component' && node.props?.some(prop => prop.type === 7 && prop.name === 'bind' && prop.arg?.content === 'is' && prop.exp?.content === 'ActiveOrderSubmissionDetailPanel')) candidates.push(node)
    for (const child of node.children || []) visit(child)
  }
  visit(descriptor.template.ast)
  assert.equal(candidates.length, 1, '实际批次详情应有唯一内嵌正式一线表单入口')
  return candidates[0].loc.source
}

function compileActualState(context) {
  const names = [
    'detail', 'inlineActiveOrderSubmissionDetail', 'inlineActiveOrderSubmissionDetailLoading', 'inlineActiveOrderSubmissionDetailError',
    'batchDetailRequestSerial', 'selectedInlineSubmissionFormMode', 'selectedInlineProductionRouteProcessId',
    'resolveErrorMessage', 'isStaleBatchDetailRequest', 'loadInlineActiveOrderSubmissionDetail', 'reloadInlineActiveOrderSubmissionDetail'
  ]
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length, '必须执行正式页面现有状态与读取函数，不能在测试里复制实现')
  const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
  return Function(...Object.keys(context), `${code}; return { ${names.join(', ')} };`)(...Object.values(context))
}

function createHarness({ slot = 'MAIN', read } = {}) {
  const queries = []
  const context = {
    ...vue,
    isReleaseProcessSelected: vue.ref(false),
    selectedTaskForEvidence: vue.ref({ formSlotType: slot, routeProcessId: '721' }),
    getEdhrBatchActiveOrderDetail: async query => { queries.push({ ...query }); return read ? await read(query) : formalDetail() }
  }
  const state = compileActualState(context)
  let captured
  const child = vue.markRaw(vue.defineComponent({
    props: ['detail', 'loading', 'error', 'embedded', 'displayMode', 'recordScope', 'productionRouteProcessId', 'auditScopeType', 'auditScopeId'],
    setup(props) { captured = props; return () => vue.h('section') }
  }))
  const makeNode = type => ({ type, children: [], parent: null })
  const renderer = vue.createRenderer({
    createElement: makeNode, createText: () => makeNode('text'), createComment: () => makeNode('comment'),
    setText() {}, setElementText() {}, patchProp() {},
    insert(node, parent) { node.parent = parent; parent.children.push(node) },
    remove() {}, parentNode: node => node.parent, nextSibling: () => null
  })
  const app = renderer.createApp({
    setup: () => ({ ...state, ActiveOrderSubmissionDetailPanel: child }),
    render: vue.compile(actualChildTemplate())
  })
  app.mount(makeNode('root'))
  return {
    ...state, queries,
    get props() { return captured },
    async load(batch) {
      state.detail.value = batch
      await state.loadInlineActiveOrderSubmissionDetail(state.detail.value)
      await tick()
    },
    unmount: () => app.unmount()
  }
}

test('INLINE-DOSSIER01 control: actual parent fetches formal detail with the selected batch ID and renders the returned facts', async () => {
  const h = createHarness()
  try {
    await h.load(ownedBatch())
    assert.deepEqual(h.queries, [{ batchExecutionId: '900000001225' }])
    assert.deepEqual(h.props.detail, formalDetail())
    assert.equal(h.props.recordScope, 'FORMAL_BATCH_SOURCE_DETAIL')
    assert.equal(h.props.displayMode, 'production')
    assert.equal(h.props.productionRouteProcessId, '721')
    assert.equal(h.props.loading, false)
    assert.equal(h.props.error, '')
  } finally { h.unmount() }
})

test('INLINE-DOSSIER01 production: actual child receives explicit BATCH identity from its formal batch', async () => {
  const h = createHarness()
  try {
    await h.load(ownedBatch())
    assert.deepEqual(h.queries, [{ batchExecutionId: '900000001225' }])
    assert.equal(h.props.auditScopeType, 'BATCH', '生产内嵌表单必须显式声明正式批次作用域')
    assert.deepEqual(h.props.auditScopeId, { batchExecutionId: '900000001225' }, '资料身份须为取得该正式详情的批次，不能猜 activeOrderId 或 applicationId')
  } finally { h.unmount() }
})

test('INLINE-DOSSIER01 PQC: actual child preserves the exact formal batch ID without numeric rounding', async () => {
  const h = createHarness({ slot: 'PROCESS_INSPECTION' })
  try {
    await h.load(ownedBatch('9007199254740993'))
    assert.deepEqual(h.queries, [{ batchExecutionId: '9007199254740993' }])
    assert.equal(h.props.displayMode, 'pqc')
    assert.equal(h.props.productionRouteProcessId, undefined)
    assert.equal(h.props.auditScopeType, 'BATCH', 'PQC 内嵌表单仍属于正式批次读取，不能猜成 PQC 申请')
    assert.deepEqual(h.props.auditScopeId, { batchExecutionId: '9007199254740993' })
  } finally { h.unmount() }
})

test('INLINE-DOSSIER01 switching formal batches on the same order updates the actual child identity', async () => {
  const h = createHarness()
  try {
    await h.load(ownedBatch())
    await h.load(ownedBatch('900000001226'))
    assert.deepEqual(h.queries, [{ batchExecutionId: '900000001225' }, { batchExecutionId: '900000001226' }])
    assert.deepEqual(h.props.detail, formalDetail())
    assert.deepEqual(h.props.auditScopeId, { batchExecutionId: '900000001226' }, '不能按相同 activeOrderId 缓存前一批次的资料身份')
  } finally { h.unmount() }
})

test('INLINE-DOSSIER01 control: missing formal active-order source is visible and cannot render a dossier detail', async () => {
  const h = createHarness()
  try {
    await h.load({ id: '900000001225' })
    assert.deepEqual(h.queries, [])
    assert.equal(h.props.detail, undefined)
    assert.match(h.props.error, /缺少正式活跃订单来源/)
    assert.equal(h.props.loading, false)
  } finally { h.unmount() }
})

test('INLINE-DOSSIER01 control: formal reader rejection clears prior detail and retains the authoritative error', async () => {
  let denied = false
  const h = createHarness({ read: async () => { if (denied) throw new Error('正式批次冻结候选拒绝读取'); return formalDetail() } })
  try {
    await h.load(ownedBatch())
    denied = true
    await h.load(ownedBatch('900000001226'))
    assert.deepEqual(h.queries, [{ batchExecutionId: '900000001225' }, { batchExecutionId: '900000001226' }])
    assert.equal(h.props.detail, undefined)
    assert.equal(h.props.error, '正式批次冻结候选拒绝读取')
    assert.equal(h.props.loading, false)
  } finally { h.unmount() }
})
