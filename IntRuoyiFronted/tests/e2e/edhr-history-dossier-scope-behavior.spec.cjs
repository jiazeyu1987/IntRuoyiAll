const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const panelSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const parentSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'), 'utf8')
const panel = parse(panelSource).descriptor
const parent = parse(parentSource).descriptor
const tick = async () => { await vue.nextTick(); for (let i = 0; i < 12; i++) await Promise.resolve() }
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
const detail = () => ({ activeOrderId: '1009200409', activeOrderStatus: { status: 'RELEASED' } })
const files = (marker = 'owned') => ({ activeOrderId: '1009200409', categories: [
  { key: 'INCOMING_INSPECTION_FILE', label: '来料检文件', files: [] },
  { key: 'STERILIZATION_FILE', label: '灭菌文件', files: [] },
  { key: 'FINISHED_PRODUCT_FILE', label: '成品检文件', files: [] },
  { key: 'OTHER_FILE', label: '其他文件', files: [{ attachmentId: 901, fileId: 902, fileName: `${marker}.pdf`, sha256: 'formal-file-hash', operatorId: 346, operatorName: '冻结资料上传人', operatedAt: '2026-10-03 12:20:00' }] }
] })

function compileDeclarations(descriptor, names, context, expressions = () => false) {
  const ast = ts.createSourceFile('actual-page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement)
    ? statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast)))
    : ts.isExpressionStatement(statement) && expressions(statement.getText(ast)))
  const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n'), {
    compilerOptions: { module: ts.ModuleKind.None, target: ts.ScriptTarget.ES2022 }
  }).outputText
  return Function(...Object.keys(context), `${code}; return { ${names.filter(name => statements.some(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => declaration.name.getText(ast) === name))).join(', ')} };`)(...Object.values(context))
}

// Compile the actual history detail child binding, rather than inventing its scope props.
function historyPanelProps() {
  const context = {
    ...vue, route: { query: { batchExecutionId: '900000001225', from: '/mes/pro/feedback/edhr-batch-history' } },
    detail: vue.ref(detail()), loading: vue.ref(false), error: vue.ref(''), initialActiveTab: vue.ref(undefined), loadDetail() {}
  }
  const values = compileDeclarations(parent, ['batchExecutionId', 'parseBatchExecutionId', 'parseActiveOrderId', 'resolveDetailQuery', 'auditDetailQuery'], context)
  const childBinding = parent.template.content.match(/<ActiveOrderSubmissionDetailPanel\b[\s\S]*?\/>/)?.[0]
  assert.ok(childBinding, '实际历史详情必须保留共享面板入口')
  const captured = {}
  const component = { render: vue.compile(childBinding), setup: () => ({ ...context, ...values }) }
  const makeNode = (type, text = '') => ({ type, text, children: [], parent: null })
  const renderer = vue.createRenderer({
    createElement: type => makeNode(type), createText: text => makeNode('text', text), createComment: text => makeNode('comment', text),
    setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text }, patchProp() {},
    insert: (node, parent) => { node.parent = parent; parent.children.push(node) }, remove() {}, parentNode: node => node.parent, nextSibling: () => null
  })
  const app = renderer.createApp(component)
  app.component('ActiveOrderSubmissionDetailPanel', {
    props: ['detail', 'loading', 'error', 'initialActiveTab', 'auditScopeType', 'auditScopeId', 'recordScope'],
    setup: (props, { attrs }) => { Object.assign(captured, props, attrs); return () => vue.h('section') }
  })
  app.mount(makeNode('root'))
  app.unmount()
  return captured
}

function dossierHarness(props = {}, overrides = {}) {
  const reads = [], writes = [], unmounted = [], stops = []
  const reactiveProps = vue.reactive({ detail: detail(), loading: false, error: '', ...props })
  const context = {
    ...vue, props: reactiveProps,
    watch: (...args) => { const stop = vue.watch(...args); stops.push(stop); return stop },
    onBeforeUnmount: callback => unmounted.push(callback),
    getActiveOrderDossierFiles: async query => { reads.push(['TEAM_OR_PQC', { ...query }]); if (query.applicationId === undefined && props.auditScopeType === 'BATCH') throw new Error('1040760409: 当前用户不是该活跃订单生产组长，不能读取资料文件。'); return files('legacy') },
    getEdhrBatchActiveOrderDossierFiles: async query => { reads.push(['BATCH', { ...query }]); return files() },
    uploadActiveOrderDossierFile: async query => writes.push(['upload', query]), deleteActiveOrderDossierFile: async query => writes.push(['delete', query]),
    ...overrides
  }
  const names = ['dossierFiles', 'dossierFileLoading', 'dossierFileError', 'dossierFileUploadingKey', 'dossierPreviewDialogVisible', 'selectedDossierPreviewSource', 'selectedDossierPreviewTitle',
    'createDossierRequestContext', 'dossierRequestContext', 'recordScope', 'auditScopeTypeValue', 'auditScopeIdValue', 'resolveDossierReadContext', 'loadDossierFiles']
  const values = compileDeclarations(panel, names, context, text => {
    return (text.startsWith('watch(') && text.includes('void loadDossierFiles()')) ||
      (text.startsWith('onBeforeUnmount(') && text.includes('dossierRequestContext.invalidate()'))
  })
  return { ...values, props: reactiveProps, reads, writes, unmount: () => { stops.forEach(stop => stop()); unmounted.forEach(callback => callback()) } }
}

test('HISTORY-DOSSIER01: actual history child scope reads owned batch dossier without team-leader request', async () => {
  const props = historyPanelProps()
  assert.equal(props.auditScopeType, 'BATCH')
  assert.deepEqual(props.auditScopeId, { batchExecutionId: '900000001225' })
  assert.equal(props.recordScope, 'FORMAL_BATCH_SOURCE_DETAIL')
  const h = dossierHarness(props)
  await tick()
  assert.deepEqual(h.reads, [['BATCH', { batchExecutionId: '900000001225' }]], '经理历史详情不得向生产组长 reader 重放资料读取')
  assert.equal(h.dossierFileError.value, '')
  assert.deepEqual(h.dossierFiles.value, files())
  assert.equal(h.writes.length, 0)
  h.unmount()
})

test('released formal dossier preserves categories, file identity, hash and original upload facts', async () => {
  const h = dossierHarness({ auditScopeType: 'BATCH', auditScopeId: { batchExecutionId: '900000001225' }, recordScope: 'FORMAL_BATCH_SOURCE_DETAIL' })
  await tick()
  assert.deepEqual(h.dossierFiles.value, files(), '终态只读应展示正式资料事实，不返回空成功或新建文件')
  assert.equal(h.dossierFileLoading.value, false)
  assert.equal(h.writes.length, 0)
  h.unmount()
})

test('batch dossier missing or ambiguous formal scope fails visibly before legacy or batch query', async () => {
  for (const auditScopeId of [undefined, '900000001225', {}, { batchExecutionId: '900000001225', activeOrderId: '1009200409' }, { batchExecutionId: '0' }]) {
    const h = dossierHarness({ auditScopeType: 'BATCH', auditScopeId, recordScope: 'FORMAL_BATCH_SOURCE_DETAIL' })
    await tick()
    assert.equal(h.reads.length, 0, '缺失或冲突的正式作用域不得降级成生产组长/PQC查询')
    assert.ok(h.dossierFileError.value)
    assert.equal(h.dossierFiles.value, undefined)
    assert.equal(h.writes.length, 0)
    h.unmount()
  }
})

test('explicit active-order batch scope keeps exact identity and rejects detail mismatch', async () => {
  const valid = dossierHarness({ auditScopeType: 'BATCH', auditScopeId: { activeOrderId: '1009200409' }, recordScope: 'FORMAL_BATCH_SOURCE_DETAIL' })
  await tick()
  assert.deepEqual(valid.reads, [['BATCH', { activeOrderId: '1009200409' }]])
  valid.unmount()
  const wrong = dossierHarness({ auditScopeType: 'BATCH', auditScopeId: { activeOrderId: '1009200410' }, recordScope: 'FORMAL_BATCH_SOURCE_DETAIL' })
  await tick()
  assert.equal(wrong.reads.length, 0)
  assert.ok(wrong.dossierFileError.value)
  wrong.unmount()
})

test('formal batch query failure stays visible without team or PQC retry', async () => {
  const reads = []
  const h = dossierHarness({ auditScopeType: 'BATCH', auditScopeId: { batchExecutionId: '900000001225' } }, {
    getEdhrBatchActiveOrderDossierFiles: async query => { reads.push(['BATCH', { ...query }]); throw new Error('正式批次资料冻结候选校验失败') }
  })
  await tick()
  assert.deepEqual(reads, [['BATCH', { batchExecutionId: '900000001225' }]])
  assert.equal(h.reads.length, 0, '正式失败不能改用 TEAM/PQC reader')
  assert.equal(h.dossierFileError.value, '正式批次资料冻结候选校验失败')
  assert.equal(h.dossierFiles.value, undefined)
  assert.equal(h.dossierFileLoading.value, false)
  assert.equal(h.writes.length, 0)
  h.unmount()
})

test('changing batch scope on same order invalidates earlier dossier response and preview', async () => {
  const old = deferred(), current = deferred(), reads = []
  const h = dossierHarness({ auditScopeType: 'BATCH', auditScopeId: { batchExecutionId: '900000001225' } }, {
    getEdhrBatchActiveOrderDossierFiles: query => { reads.push({ ...query }); return reads.length === 1 ? old.promise : current.promise }
  })
  await tick()
  assert.deepEqual(reads, [{ batchExecutionId: '900000001225' }])
  h.dossierPreviewDialogVisible.value = true
  h.selectedDossierPreviewTitle.value = 'old.pdf'
  h.props.auditScopeId = { batchExecutionId: '900000001226' }
  await tick()
  assert.equal(h.dossierPreviewDialogVisible.value, false)
  assert.equal(h.selectedDossierPreviewTitle.value, '')
  assert.deepEqual(reads, [{ batchExecutionId: '900000001225' }, { batchExecutionId: '900000001226' }])
  current.resolve(files('current'))
  await tick()
  old.resolve(files('old'))
  await tick()
  assert.deepEqual(h.dossierFiles.value, files('current'))
  h.unmount()
})

test('late batch dossier response after unmount cannot repopulate historical files', async () => {
  const response = deferred(), reads = []
  const h = dossierHarness({ auditScopeType: 'BATCH', auditScopeId: { batchExecutionId: '900000001225' } }, {
    getEdhrBatchActiveOrderDossierFiles: query => { reads.push({ ...query }); return response.promise }
  })
  await tick()
  assert.equal(reads.length, 1)
  h.unmount()
  response.resolve(files())
  await tick()
  assert.equal(h.dossierFiles.value, undefined)
  assert.equal(h.dossierFileLoading.value, false)
})

test('batch detail loading or formal detail error does not query dossier with stale identity', async () => {
  for (const state of [{ loading: true }, { error: '正式详情授权失败' }]) {
    const h = dossierHarness({ auditScopeType: 'BATCH', auditScopeId: { batchExecutionId: '900000001225' }, ...state })
    await tick()
    assert.equal(h.reads.length, 0)
    assert.equal(h.dossierFiles.value, undefined)
    assert.equal(h.writes.length, 0)
    h.unmount()
  }
})

test('team leader keeps existing dossier query contract', async () => {
  const h = dossierHarness()
  await tick()
  assert.deepEqual(h.reads, [['TEAM_OR_PQC', { activeOrderId: '1009200409', applicationId: undefined }]])
  assert.deepEqual(h.dossierFiles.value, files('legacy'))
  h.unmount()
})

test('PQC keeps existing explicit application query contract', async () => {
  const h = dossierHarness({ pqcReleaseApplicationId: '224' })
  await tick()
  assert.deepEqual(h.reads, [['TEAM_OR_PQC', { activeOrderId: '1009200409', applicationId: '224' }]])
  assert.equal(h.writes.length, 0)
  h.unmount()
})
