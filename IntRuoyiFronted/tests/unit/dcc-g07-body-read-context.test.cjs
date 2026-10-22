const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const helperPath = 'src/api/dcc/controlledFile/bodyReadContext.ts'
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const plain = value => value === undefined ? undefined : JSON.parse(JSON.stringify(value))
const FILE = '9223372036854775000'
const taskContext = () => ({ intent: 'APPROVAL_TASK', controlledFileId: FILE,
  expectedVersionNo: 'A/1-2', applicationType: 'REVISION', bpmRound: 'round-exact', taskId: 'task-exact' })
function load(file, dependencies = {}) {
  const host = { exports: {}, Error, TypeError, TextEncoder, TextDecoder, URLSearchParams, btoa, atob,
    require: id => { if (id in dependencies) return dependencies[id]; throw new Error('undeclared dependency: ' + id) } }
  vm.runInNewContext(ts.transpileModule(read(file), { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, host, { filename: file })
  return host.exports
}
const helper = () => load(helperPath)
function previewPort() {
  const calls = [], failure = new Error('正文权限已撤销')
  const workflow = {
    getControlledFilePreviewMetadata: async (...args) => { calls.push(['metadata', ...args]); return { viewerToken: 'token' } },
    previewControlledFileWithWatermark: async (...args) => { calls.push(['binary', ...args]); return { blob: 'selected-body' } }
  }
  const api = load('src/api/common/filePreview.ts', {
    axios: { get: () => { throw new Error('DCC正文必须通过既有wrapper') } },
    '@/config/axios': { get: () => { throw new Error('unexpected request') } },
    '@/config/axios/config': { config: {} }, '@/utils': { generateUUID: () => 'G07' },
    '@/api/dcc/controlledFile/workflow': workflow,
    '@/api/dcc/controlledFile/bodyReadContext': fs.existsSync(path.join(root, helperPath)) ? helper() : {}
  })
  return { api, calls, workflow, failure }
}

test('actual DCC source forwards the same exact task context to existing metadata and binary wrappers', async () => {
  const port = previewPort(), context = taskContext()
  const source = port.api.buildDccControlledFilePreviewSource(FILE, context)
  assert.deepEqual(plain(source.bodyReadContext), context)
  const metadata = await port.api.getOnlineFilePreviewMetadata(source)
  await port.api.previewOnlineFileWithWatermark(source, metadata)
  assert.deepEqual(plain(port.calls[0]), ['metadata', FILE, null, context])
  assert.deepEqual(plain(port.calls[1]), ['binary', FILE, metadata, null, context])
  assert.equal(context.expectedVersionNo, 'A/1-2')
})

test('daily source is explicit; from and client flags cannot turn it into a task context', () => {
  const port = previewPort()
  assert.deepEqual(plain(port.api.buildDccControlledFilePreviewSource(FILE).bodyReadContext),
    { intent: 'DAILY', controlledFileId: FILE })
  for (const injected of [{ ...taskContext(), from: 'approval-center' }, { intent: 'DAILY', controlledFileId: FILE, isApproval: true }])
    assert.throws(() => port.api.buildDccControlledFilePreviewSource(FILE, injected))
})

test('source rejects another file or mixed distribution before either request; request failure propagates', async () => {
  const port = previewPort()
  assert.throws(() => port.api.buildDccControlledFilePreviewSource('7', taskContext()))
  await assert.rejects(port.api.getOnlineFilePreviewMetadata({ type: 'DCC_CONTROLLED_FILE', controlledFileId: FILE,
    distribution: { controlledFileId: FILE, distributionId: '2', recipientId: '3', versionNo: 'A/1', approvalProcessInstanceId: 'r' },
    bodyReadContext: taskContext() }))
  assert.equal(port.calls.length, 0)
  port.workflow.getControlledFilePreviewMetadata = async () => { throw port.failure }
  await assert.rejects(port.api.getOnlineFilePreviewMetadata(port.api.buildDccControlledFilePreviewSource(FILE, taskContext())),
    error => error === port.failure)
})

test('strict intent union roundtrips canonical wire with exact IDs, raw version and explicit round', () => {
  const h = helper()
  for (const context of [
    { intent: 'DAILY', controlledFileId: FILE },
    { intent: 'AUTHORING', controlledFileId: FILE, expectedVersionNo: 'A/1-2' },
    { intent: 'AUTHORING', controlledFileId: FILE, expectedVersionNo: 'A/1-2', applicationType: 'REVISION', bpmRound: 'r' },
    taskContext(),
    { ...taskContext(), intent: 'APPROVAL_HISTORY' },
    { intent: 'APPROVAL_HISTORY', controlledFileId: FILE, expectedVersionNo: 'A/1', applicationType: 'UPLOAD', bpmRound: 'history-r' },
    { intent: 'VERSION_HISTORY', controlledFileId: FILE, expectedVersionNo: 'A/1' },
    { intent: 'TRAINING_APPLICATION', controlledFileId: FILE, expectedVersionNo: 'A/1', applicationType: 'UPLOAD', bpmRound: 'training-r' }
  ]) {
    const normalized = h.normalizeDccBodyReadContext(FILE, context)
    const wire = h.serializeDccBodyReadContext(FILE, normalized)
    assert.match(wire, /^[A-Za-z0-9_-]+$/)
    assert.deepEqual(plain(h.readDccBodyReadContextRoute(FILE, { bodyReadContext: wire })), context)
    assert.deepEqual(plain(h.buildDccBodyReadQuery(FILE, context)), { bodyReadContext: wire })
    assert.deepEqual(plain(h.buildDccBodyReadHeaders(FILE, context)), { 'X-DCC-Body-Read-Context': wire })
    assert.ok(Object.isFrozen(normalized))
    assert.equal(h.serializeDccBodyReadContext(FILE, Object.fromEntries(Object.entries(context).reverse())), wire)
  }
})

test('strict context refuses malformed, missing, unknown or cross-file facts without a daily fallback', () => {
  const h = helper(), good = taskContext()
  for (const bad of [null, false, [], 'APPROVAL_TASK', {}, { ...good, intent: 'future' }, { ...good, taskId: undefined },
    { ...good, taskId: ['task-exact'] }, { ...good, taskId: ' task-exact' }, { ...good, bpmRound: 'r\n' },
    { ...good, applicationType: 'LEGACY' }, { ...good, applicationType: ['REVISION'] }, { ...good, expectedVersionNo: '' }, { ...good, expectedVersionNo: ' A/1' },
    { ...good, controlledFileId: Number(FILE) }, { ...good, controlledFileId: '7' }, { ...good, canPreview: true },
    { intent: 'AUTHORING', controlledFileId: FILE, expectedVersionNo: 'A/1', bpmRound: 'r' },
    { intent: 'VERSION_HISTORY', controlledFileId: FILE, expectedVersionNo: 'A/1', taskId: 't' },
    { intent: 'DAILY', controlledFileId: FILE, expectedVersionNo: 'A/1' }])
    assert.throws(() => h.normalizeDccBodyReadContext(FILE, bad), JSON.stringify(bad))
  for (const query of [{ bodyReadContext: ['x'] }, { bodyReadContext: null }, { bodyReadContext: '' }, { bodyReadContext: 'garbage' }])
    assert.throws(() => h.readDccBodyReadContextRoute(FILE, query))
  assert.deepEqual(plain(h.readDccBodyReadContextRoute(FILE, { from: 'approval-center', taskId: 'fake' })),
    { intent: 'DAILY', controlledFileId: FILE })
})

function bpmHelper() {
  return load('src/views/bpm/processInstance/detail/dcc-business-identity.ts',
    { '@/api/dcc/controlledFile/bodyReadContext': helper() })
}
test('BPM body source selects the exact task or selected history; a different todo cannot replace it', () => {
  const h = bpmHelper(), instance = { id: 'round-exact', businessKey: FILE }, file = { id: FILE, versionNo: 'A/1-2' }
  const todo = { id: 'task-exact', processInstanceId: 'round-exact' }
  assert.deepEqual(plain(h.resolveDccApprovalBodyReadContext(instance, 'dcc-controlled-file-revision', file, 'task-exact', todo)), taskContext())
  assert.deepEqual(plain(h.resolveDccApprovalBodyReadContext(instance, 'dcc-controlled-file-revision', file, 'history-task', todo)),
    { ...taskContext(), intent: 'APPROVAL_HISTORY', taskId: 'history-task' })
  assert.deepEqual(plain(h.resolveDccApprovalBodyReadContext(instance, 'dcc-controlled-file-revision', file, undefined, null)),
    { intent: 'APPROVAL_HISTORY', controlledFileId: FILE, expectedVersionNo: 'A/1-2', applicationType: 'REVISION', bpmRound: 'round-exact' })
  assert.throws(() => h.resolveDccApprovalBodyReadContext(instance, 'dcc-controlled-file-revision', { ...file, id: '9' }, undefined, todo))
  assert.throws(() => h.resolveDccApprovalBodyReadContext(instance, 'unknown', file, undefined, todo))
  assert.throws(() => h.resolveDccApprovalBodyReadContext(instance, 'dcc-controlled-file-revision', file, undefined,
    { ...todo, processInstanceId: 'other-round' }))
})

test('obsolete body source retains the precise Form Center object and version', () => {
  const h = bpmHelper(), facts = { systemCode: 'DCC', objectType: 'CONTROLLED_FILE', actionCode: 'OBSOLETE', objectId: FILE, objectVersion: 'A/1' }
  const instance = { id: 'round-obsolete', businessKey: 'FORM_ACTION:F-1', formVariables: facts }
  const result = h.resolveDccApprovalBodyReadContext(instance, 'dcc-controlled-file-obsolete', { id: FILE, versionNo: 'A/1' }, 'historic-task', null)
  assert.equal(result.applicationType, 'OBSOLETE')
  assert.equal(result.expectedVersionNo, 'A/1')
  assert.throws(() => h.resolveDccApprovalBodyReadContext(instance, 'dcc-controlled-file-obsolete', { id: FILE, versionNo: 'B/1' }, 'historic-task', null))
})

test('actual viewer navigation preserves the exact body context while from is only a display source', async () => {
  const h = helper()
  const presentation = load('src/views/dcc/controlled-file/view/presentation.ts', { '@/api/dcc/controlledFile/bodyReadContext': h })
  const nav = load('src/views/dcc/controlled-file/shared/viewer-navigation.ts', {
    '../view/presentation': presentation, '@/api/dcc/controlledFile/bodyReadContext': h
  })
  const locations = [], router = { push: async location => locations.push(location) }, route = { fullPath: '/dcc/controlled-file/detail/' + FILE }
  await nav.openControlledFileViewer(router, route, FILE, 'approval-center', taskContext())
  const url = new URL(locations[0], 'https://test.invalid')
  assert.equal(url.pathname, '/dcc/controlled-file/detail/' + FILE)
  assert.deepEqual(plain(h.readDccBodyReadContextRoute(FILE, Object.fromEntries(url.searchParams))), taskContext())
  await assert.rejects(async () => nav.openControlledFileViewer(router, route, '7', 'approval-center', taskContext()))
  assert.equal(locations.length, 1)
})

test('actual browser preview selects AUTHORING only for its exact dedicated working identity', () => {
  const script = parse(read('src/views/dcc/controlled-file/browser/index.vue')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('browser.ts', script, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(statement => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some(value => value.name.text === 'openPreview'))
  assert.ok(declaration)
  for (const dedicated of [false, true]) {
    const calls = []
    const h = helper(), presentation = load('src/views/dcc/controlled-file/view/presentation.ts')
    const host = { exports: {}, ...h, ...presentation, route: { query: dedicated ? { workingFileId: FILE, workingMasterId: '700' } : {} },
      workingBrowserIdentityQuery: query => query, buildBrowserReturnPath: () => '/dcc/controlled-file/browser',
      window: { open: (url, target) => { calls.push({ url, target }); return {} } }, message: { error: message => { throw new Error(message) } } }
    vm.runInNewContext(ts.transpileModule(declaration.getText(ast) + '\nexports.open=openPreview', {
      compilerOptions: { target: ts.ScriptTarget.ES2022 }
    }).outputText, host)
    const file = { id: FILE, versionNo: 'A/1-2', status: 'WORKING', processInstanceId: null }
    host.exports.open(file)
    const url = new URL(calls[0].url, 'https://test.invalid')
    assert.deepEqual(plain(h.readDccBodyReadContextRoute(FILE, Object.fromEntries(url.searchParams))), dedicated
      ? { intent: 'AUTHORING', controlledFileId: FILE, expectedVersionNo: 'A/1-2' }
      : { intent: 'DAILY', controlledFileId: FILE })
    assert.equal(file.versionNo, 'A/1-2')
    assert.equal(calls[0].target, '_blank')
  }
})

test('actual detail body context keeps route version, exact task/history and metadata separate', () => {
  const script = parse(read('src/views/dcc/controlled-file/detail/index.vue')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('detail.ts', script, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(statement => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some(value => value.name.text === 'resolveDetailBodyReadContext'))
  assert.ok(declaration)
  const h = helper(), file = { id: FILE, versionNo: 'A/1-2', status: 'WORKING', changeType: 'REVISION', processInstanceId: null }
  const host = { exports: {}, ...h, fileDetail: vue.ref(file), controlledFileId: vue.ref(FILE),
    route: { query: { from: 'approval-center' } }, approvalProgressScope: vue.ref(), approvalTodoTask: vue.ref(),
    viewerMode: vue.ref(true), canUploadApplicantTrainingRecord: vue.ref(false) }
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast) + '\nexports.resolve=resolveDetailBodyReadContext', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  assert.equal(host.exports.resolve().intent, 'DAILY')
  host.route.query = { bodyReadContext: h.serializeDccBodyReadContext(FILE, taskContext()) }
  assert.deepEqual(plain(host.exports.resolve()), taskContext())
  host.fileDetail.value.versionNo = 'B/1'
  assert.throws(host.exports.resolve)
  host.fileDetail.value.versionNo = 'A/1-2'
  host.route.query = { processInstanceId: 'round-exact', taskId: 'task-exact' }
  assert.throws(host.exports.resolve)
  host.approvalProgressScope.value = { fileId: FILE, applicationType: 'REVISION', bpmRound: 'round-exact' }
  host.approvalTodoTask.value = { id: 'task-exact', processInstanceId: 'round-exact' }
  assert.equal(host.exports.resolve().intent, 'APPROVAL_TASK')
  host.route.query.taskId = 'historic-task'
  assert.equal(host.exports.resolve().intent, 'APPROVAL_HISTORY')
  host.route.query = { management: '1' }; host.viewerMode.value = false
  assert.equal(host.exports.resolve().intent, 'AUTHORING')
})

function trainingOwnerDetail() {
  const script = parse(read('src/views/dcc/controlled-file/detail/index.vue')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('detail.ts', script, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(statement => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some(value => value.name.text === 'resolveDetailBodyReadContext'))
  const lifecycle = load('src/views/dcc/controlled-file/shared/lifecycle.ts', {
    '@/api/form-center/actionProjection': load('src/api/form-center/actionProjection.ts')
  })
  const host = { exports: {}, ...helper(), isDccControlledFileActionAllowed: lifecycle.isDccControlledFileActionAllowed,
    fileDetail: vue.ref({ id: FILE, versionNo: 'A/1', status: 'PENDING_APPLICANT_TRAINING_RECORD', changeType: 'NEW',
      processDefinitionKey: 'dcc-controlled-file-upload', processInstanceId: 'round-exact', fileOwnerUserId: '99',
      fileOwnerProcessInstanceId: 'round-exact', fileOwnerSignatureId: '860', canReadTrainingApplicationProgress: true,
      actionProjection: { allowedActions: ['VIEW', 'PREVIEW'], actionLocked: false, canExecute: true } }),
    controlledFileId: vue.ref(FILE), currentUserId: vue.ref('99'),
    route: { query: { management: '1', from: 'workbench', processInstanceId: 'round-exact' } },
    approvalProgressScope: vue.ref({ fileId: FILE, applicationType: 'UPLOAD', bpmRound: 'round-exact' }),
    approvalTodoTask: vue.ref(), viewerMode: vue.ref(false), canUploadApplicantTrainingRecord: vue.ref(false) }
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast) + '\nexports.resolve=resolveDetailBodyReadContext', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return host
}

test('current confirmed training owner uses exact training body context from the original workbench route without upload duty', () => {
  const host = trainingOwnerDetail()
  assert.deepEqual(plain(host.exports.resolve()), { intent: 'TRAINING_APPLICATION', controlledFileId: FILE,
    expectedVersionNo: 'A/1', applicationType: 'UPLOAD', bpmRound: 'round-exact' })
  assert.equal(host.canUploadApplicantTrainingRecord.value, false)
})

test('training body context never inherits owner metadata for other rounds, tasks, files, ended duty or absent body grant', () => {
  for (const invalid of ['route-round', 'scope-round', 'scope-file', 'file', 'owner', 'owner-round', 'unsigned', 'ended', 'no-preview', 'no-progress', 'task']) {
    const host = trainingOwnerDetail(), file = host.fileDetail.value
    switch (invalid) {
      case 'route-round': host.route.query.processInstanceId = 'other-round'; break
      case 'scope-round': host.approvalProgressScope.value.bpmRound = 'other-round'; break
      case 'scope-file': host.approvalProgressScope.value.fileId = '7'; break
      case 'file': file.id = '7'; break
      case 'owner': host.currentUserId.value = '88'; break
      case 'owner-round': file.fileOwnerProcessInstanceId = 'other-round'; break
      case 'unsigned': file.fileOwnerSignatureId = null; break
      case 'ended': file.status = 'ACTIVE'; break
      case 'no-preview': file.actionProjection.allowedActions = ['VIEW']; break
      case 'no-progress': file.canReadTrainingApplicationProgress = false; break
      case 'task': host.route.query.taskId = 'historic-task'; break
    }
    let result
    try { result = host.exports.resolve() }
    catch (error) {
      assert.match(error.message, /正文所选版本尚未读取或与当前文件不一致|正文任务或历史轮次尚未通过正式申请身份核验/, invalid)
      continue
    }
    assert.notEqual(result.intent, 'TRAINING_APPLICATION', invalid)
  }
  const task = trainingOwnerDetail()
  task.route.query.taskId = 'task-exact'
  task.approvalTodoTask.value = { id: 'task-exact', processInstanceId: 'round-exact' }
  assert.equal(task.exports.resolve().intent, 'APPROVAL_TASK')
  const explicit = trainingOwnerDetail()
  explicit.route.query.bodyReadContext = helper().serializeDccBodyReadContext(FILE, { intent: 'DAILY', controlledFileId: FILE })
  assert.equal(explicit.exports.resolve().intent, 'DAILY')
})

test('actual shared workflow wrappers use identical canonical metadata params and binary headers; distribution remains separate', async () => {
  const h = helper(), source = read('src/api/dcc/controlledFile/workflow.ts')
  const ast = ts.createSourceFile('workflow.ts', source, ts.ScriptTarget.Latest, true)
  const names = ['getControlledFilePreviewMetadata', 'previewControlledFileWithWatermark']
  const code = ast.statements.filter(statement => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some(value => names.includes(value.name.text)))
    .map(statement => statement.getText(ast)).join('\n')
  assert.equal(code.length > 0, true)
  const calls = [], metadata = { viewerToken: 'token', viewerTokenId: 'token-id', viewerTokenNonce: 'nonce', accessEventCode: 'event', watermarkTraceCode: 'trace' }
  const host = { exports: {}, ...h, axiosConfig: { base_url: '/admin-api', request_timeout: 1000 },
    request: { get: async request => { calls.push(['metadata', request]); return metadata } },
    axios: { get: async (url, request) => { calls.push(['binary', { url, ...request }]); return { data: 'exact-blob', headers: {} } } },
    parseControlledFilePreviewMetadata: data => data, assertDistributionPreviewRequest: () => {}, assertDistributionPreviewMetadata: () => {},
    buildControlledFileBinaryHeaders: headers => headers, getAxiosHeader: () => 'watermark', decodePreviewWatermark: () => ({ traceCode: 'trace' }),
    DCC_VIEWER_TOKEN_HEADER: 'X-DCC-Viewer-Token', DCC_VIEWER_TOKEN_ID_HEADER: 'X-DCC-Viewer-Token-Id', DCC_VIEWER_TOKEN_NONCE_HEADER: 'X-DCC-Viewer-Token-Nonce',
    DCC_ACCESS_EVENT_CODE_HEADER: 'X-DCC-Access-Event-Code', DCC_WATERMARK_TRACE_CODE_HEADER: 'X-DCC-Watermark-Trace-Code',
    CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER: 'X-DCC-Preview-Watermark' }
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, host)
  const context = taskContext()
  await host.exports.previewControlledFileWithWatermark(FILE, undefined, undefined, context)
  assert.equal(calls[0][0], 'metadata'); assert.equal(calls[1][0], 'binary')
  assert.equal(calls[0][1].params.bodyReadContext, calls[1][1].headers['X-DCC-Body-Read-Context'])
  assert.deepEqual(plain(h.readDccBodyReadContextRoute(FILE, calls[0][1].params)), context)
  const count = calls.length
  const distribution = { controlledFileId: FILE, distributionId: '2', recipientId: '3', versionNo: 'A/1', approvalProcessInstanceId: 'r' }
  await assert.rejects(host.exports.getControlledFilePreviewMetadata(FILE, distribution, context))
  await assert.rejects(host.exports.previewControlledFileWithWatermark(FILE, metadata, distribution, context))
  assert.equal(calls.length, count)
  await host.exports.previewControlledFileWithWatermark(FILE, metadata, distribution)
  assert.equal(calls[count][1].headers['X-DCC-Body-Read-Context'], undefined)
  assert.deepEqual(plain(calls[count][1].params), { distributionId: '2', recipientId: '3' })
})

test('actual BPM preview template uses the explicit source and never mounts on missing source', () => {
  const descriptor = parse(read('src/views/bpm/processInstance/detail/index.vue')).descriptor
  function find(node) { if (node.type === 1 && node.tag === 'ProtectedPdfViewer') return node;
    for (const child of node.children || []) { const found = find(child); if (found) return found } }
  const node = find(descriptor.template.ast)
  assert.ok(node)
  const component = '<template>' + node.loc.source + '</template><script setup>const dccApprovalPreviewSource=__host.source;const dccControlledFileBusinessId=__host.fileId;const dccApprovalFileDetail=__host.file;</script>'
  const compiled = compileScript(parse(component).descriptor, { id: 'g07-real-bpm-preview', inlineTemplate: true }).content
  for (const source of [undefined, { type: 'DCC_CONTROLLED_FILE', controlledFileId: FILE, bodyReadContext: taskContext() }]) {
    const host = { exports: {}, require: () => vue, __host: { source, fileId: FILE, file: { title: '所选正文' } } }
    vm.runInNewContext(ts.transpileModule(compiled, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, host)
    const render = host.exports.default.setup({}, { expose() {} })
    const captured = []
    const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }),
      createText: text => ({ text }), createComment: text => ({ comment: text }), insert: (node, parent) => parent.children.push(node),
      remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
    const app = renderer.createApp(host.exports.default)
    app.component('ProtectedPdfViewer', { props: ['previewSource'], setup: props => { captured.push(props.previewSource); return () => vue.h('div') } })
    const root = { children: [] }; app.mount(root)
    if (source) assert.equal(captured[0], source)
    else assert.equal(captured.length, 0)
    app.unmount()
  }
})
