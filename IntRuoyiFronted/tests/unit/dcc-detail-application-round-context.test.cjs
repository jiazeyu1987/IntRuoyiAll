const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const read = p => fs.readFileSync(p, 'utf8')
const clone = v => JSON.parse(JSON.stringify(v))
const deferred = () => { let resolve; const promise = new Promise(r => resolve = r); return { promise, resolve } }
const flush = async () => { for (let n = 0; n < 15; n++) await Promise.resolve(); await vue.nextTick() }
function moduleFrom(source, require = id => { throw Error(id) }) {
  const env = { exports: {}, require, Error, JSON, Set, BigInt, Number, String }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, env)
  return env.exports
}
function mount(props, { rounds = async () => clone(rows), evidence = async (id, type, bpm) => frozen(id, type, bpm) } = {}) {
  const file = 'src/views/dcc/controlled-file/detail/DetailApplicationHistory.vue', { descriptor } = parse(read(file))
  const script = compileScript(descriptor, { id: file })
  const compiledTemplate = compileTemplate({ source: descriptor.template.content, filename: file, id: file, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiledTemplate.errors, [])
  const calls = []
  const component = moduleFrom(script.content, id => {
    if (id === 'vue') return vue
    if (id === '@/api/dcc/controlledFile/applicationRead') return { getControlledFileApplicationRounds: async id => { calls.push(['rounds', String(id)]); return rounds(id) }, getControlledFileApplicationEvidence: async (...args) => { calls.push(['evidence', ...args]); return evidence(...args) } }
    if (id === './presentation') return { formatControlledFileDateTime: value => value }
    if (id === './application-round-context') return moduleFrom(read('src/views/dcc/controlled-file/detail/application-round-context.ts'))
    if (id.endsWith('.vue')) return {}
    throw Error(id)
  }).default
  component.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }), insert: (c, p) => p.children.push(c), remove() {}, setText() {}, setElementText() {}, parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const reactive = vue.reactive(props), app = renderer.createApp({ render: () => vue.h(component, reactive) }), host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, props: reactive, calls, app }
}
const rows = [{ controlledFileId: '10', applicationType: 'UPLOAD', bpmRound: 'native-U', attributeRound: 1 }, { controlledFileId: '10', applicationType: 'OBSOLETE', bpmRound: 'obsolete-O', attributeRound: 2 }]
const attrs = m => ({ targetMarkets: [m], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' })
const frozen = (id, type, bpm) => ({ controlledFileId: id, applicationType: type, bpmRound: bpm, attributeRound: bpm === 'obsolete-O' ? 2 : 1, recorded: true, defaultSource: attrs('CE'), actualAttributes: attrs(bpm === 'obsolete-O' ? 'FDA' : 'NMPA'), signatures: [{ processInstanceId: bpm }] })
test('approval route uses verified BPM read context rather than immediately trusting route or file native BPM', () => {
  const helper = moduleFrom(read('src/views/dcc/controlled-file/detail/application-round-context.ts'))
  const input = { fileId: '10', nativeBpmRound: 'native-U', requestedBpmRound: 'obsolete-O', requestedTaskId: 'task-O', contextKey: 'O:file10' }
  const pending = helper.resolveApplicationRoundSelection(input)
  assert.equal(pending.primaryBpmRound, null); assert.match(pending.blockedReason, /核验/)
  const verified = helper.validateApplicationApprovalRead('obsolete-O', 'task-O', [{ id: 'task-O', processInstanceId: 'obsolete-O' }])
  const ready = helper.resolveApplicationRoundSelection({ ...input, readContext: { contextKey: input.contextKey, ...verified } })
  assert.equal(ready.primaryBpmRound, 'obsolete-O'); assert.equal(ready.lockPrimaryRound, true); assert.equal(ready.blockedReason, '')
  const ordinary = helper.resolveApplicationRoundSelection({ fileId: '10', nativeBpmRound: 'native-U', contextKey: 'plain' })
  assert.equal(ordinary.primaryBpmRound, 'native-U'); assert.equal(ordinary.lockPrimaryRound, false)
  const changingFile = helper.resolveApplicationRoundSelection({ fileId: '10', requestedFileId: '11', nativeBpmRound: 'native-U', contextKey: 'next-file' })
  assert.equal(changingFile.primaryBpmRound, null); assert.match(changingFile.blockedReason, /文件/)
  assert.throws(() => helper.validateApplicationApprovalRead('obsolete-O', 'foreign-task', [{ id: 'task-O', processInstanceId: 'obsolete-O' }]), /任务/)
  assert.throws(() => helper.validateApplicationApprovalRead('obsolete-O', 'task-O', [{ id: 'task-O', processInstanceId: 'foreign-BPM' }]), /轮次/)
})
test('processing obsolete panel auto-selects actual formal round and rejects another manual round without fallback', async () => {
  const p = mount({ fileId: '10', primaryBpmRound: 'obsolete-O', lockPrimaryRound: true, contextKey: 'task-O' })
  try {
    await flush(); assert.equal(p.state.evidence.applicationType, 'OBSOLETE'); assert.deepEqual(clone(p.state.evidence.actualAttributes), attrs('FDA'))
    await p.state.selectRound(p.state.roundKey(rows[0])); assert.equal(p.state.evidence, undefined); assert.match(p.state.error, /办理/)
    assert.equal(p.calls.filter(call => call[0] === 'evidence').length, 1)
  } finally { p.app.unmount() }
})
test('missing primary mapping is an explicit failure and never fetches or silently selects native facts', async () => {
  const p = mount({ fileId: '10', primaryBpmRound: 'foreign-round', lockPrimaryRound: true })
  try { await flush(); assert.equal(p.state.evidence, undefined); assert.equal(p.state.selectedKey, ''); assert.match(p.state.error, /正式.*映射|轮次.*缺失/); assert.equal(p.calls.filter(call => call[0] === 'evidence').length, 0) }
  finally { p.app.unmount() }
})
test('route/task verification pending immediately clears the previous evidence and rejects a late response', async () => {
  const pending = deferred(), p = mount({ fileId: '10', primaryBpmRound: 'native-U', contextKey: 'native' }, { evidence: (id, type, bpm) => bpm === 'native-U' ? pending.promise : frozen(id, type, bpm) })
  try {
    await flush(); p.props.contextKey = 'task-O'; p.props.primaryBpmRound = null; p.props.contextError = '正在核验实际办理轮次'; await vue.nextTick()
    pending.resolve(frozen('10', 'UPLOAD', 'native-U')); await flush()
    assert.equal(p.state.evidence, undefined); assert.equal(p.state.rounds.length, 0); assert.match(p.state.error, /核验/)
    p.props.primaryBpmRound = 'obsolete-O'; p.props.contextError = ''; p.props.lockPrimaryRound = true; await flush()
    assert.equal(p.state.evidence.bpmRound, 'obsolete-O')
  } finally { p.app.unmount() }
})
test('duplicate or foreign file mapping clears prior evidence and exposes the response identity failure', async () => {
  let result = clone(rows), p = mount({ fileId: '10', primaryBpmRound: 'native-U', contextKey: 'first' }, { rounds: async () => result })
  try {
    await flush(); assert.equal(p.state.evidence.bpmRound, 'native-U')
    result = [clone(rows[1]), clone(rows[1])]; p.props.primaryBpmRound = 'obsolete-O'; p.props.contextKey = 'duplicate'; await flush()
    assert.equal(p.state.evidence, undefined); assert.equal(p.state.selectedKey, ''); assert.match(p.state.error, /重复|多个/)
    result = [{ ...clone(rows[1]), controlledFileId: '99' }]; p.props.contextKey = 'foreign'; await flush()
    assert.equal(p.state.evidence, undefined); assert.match(p.state.error, /文件|身份/)
  } finally { p.app.unmount() }
})
test('public detail feeds verified current approval context into the round panel and clears it before asynchronous reload', () => {
  const page = read('src/views/dcc/controlled-file/detail/index.vue')
  const panel = page.match(/<DetailApplicationHistory\b[^>]*>/)?.[0]
  assert.match(panel, /applicationRoundSelection\.primaryBpmRound/)
  assert.match(panel, /applicationRoundSelection\.blockedReason/)
  assert.match(panel, /applicationRoundSelection\.lockPrimaryRound/)
  assert.match(page, /validateApplicationApprovalRead\(processInstanceId, taskId, taskList\)/)
  assert.match(page, /applicationApprovalRead\.value = undefined/)
})
function approvalReader(transport) {
  const nativeProgress = moduleFrom(read('src/views/dcc/controlled-file/detail/native-approval-progress.ts'))
  const { descriptor } = parse(read('src/views/dcc/controlled-file/detail/index.vue'))
  const source = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const declaration = source.statements.find(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(d => d.name.getText(source) === 'loadApprovalDetail'))
  assert.ok(declaration)
  const helper = moduleFrom(read('src/views/dcc/controlled-file/detail/application-round-context.ts'))
  const env = { exports: {}, Error, String, Promise, route: { fullPath: '/detail/10?processInstanceId=obsolete-O&taskId=task-O', query: { processInstanceId: 'obsolete-O', taskId: 'task-O' } },
    detailLoadSequence: 1, controlledFileId: vue.ref('10'), fileDetail: vue.ref({ id: '10', processInstanceId: 'native-U' }),
    applicationApprovalRead: vue.ref({ contextKey: 'previous', processInstanceId: 'native-U', taskId: '' }),
    approvalProgressScope: vue.ref(), approvalProgressError: vue.ref(''), stageProgressList: vue.ref([]),
    isBrowserTraceabilityPage: vue.ref(false), approvalLoading: vue.ref(false), approvalTodoTask: vue.ref(null), approvalTaskList: vue.ref([]),
    checkPermi: () => false, TaskApi: { getTaskListByProcessInstanceId: transport }, ProcessInstanceApi: { getApprovalDetail: async () => { throw Error('generic BPM read should be permission-gated') } },
    findCurrentUserTodoTask: rows => rows[0], syncStageProgress() {}, validateApplicationApprovalRead: helper.validateApplicationApprovalRead,
    validateApplicationRoundMappings: helper.validateApplicationRoundMappings, resolveApprovalProgressScope: nativeProgress.resolveApprovalProgressScope,
    getControlledFileApplicationRounds: async () => [{ controlledFileId: env.controlledFileId.value, applicationType: 'OBSOLETE', bpmRound: env.route.query.processInstanceId, attributeRound: 2 }],
    resolveReadSideErrorMessage: cause => cause.message }
  env.applicationRoundContextKey = { get value() { return JSON.stringify([env.controlledFileId.value, env.route.fullPath]) } }
  env.isCurrentDetailLoad = (sequence, id, route) => sequence === env.detailLoadSequence && id === env.controlledFileId.value && route === env.route.fullPath
  vm.runInNewContext(ts.transpileModule(declaration.getText(source) + '\nexports.load = loadApprovalDetail', { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, env)
  return { env, load: (...args) => env.exports.load(...args) }
}
test('actual parent approval read gates generic API and sets obsolete context only after matching task response', async () => {
  const pending = deferred(), reader = approvalReader(() => pending.promise)
  const opening = reader.load()
  assert.equal(reader.env.applicationApprovalRead.value, undefined)
  pending.resolve([{ id: 'task-O', processInstanceId: 'obsolete-O', status: 1 }]); await opening
  assert.equal(reader.env.applicationApprovalRead.value.processInstanceId, 'obsolete-O')
  assert.equal(reader.env.applicationApprovalRead.value.taskId, 'task-O')
  assert.equal(reader.env.approvalTodoTask.value.id, 'task-O')
})
test('actual parent rejects a foreign task BPM and ignores an old successful response after route change', async () => {
  const bad = approvalReader(async () => [{ id: 'task-O', processInstanceId: 'foreign-F' }])
  await assert.rejects(bad.load(), /轮次/)
  assert.match(bad.env.applicationApprovalRead.value.error, /轮次/)
  const pending = deferred(), old = approvalReader(() => pending.promise), loading = old.load()
  old.env.route.fullPath = '/detail/10?processInstanceId=other-P&taskId=task-P'
  old.env.route.query = { processInstanceId: 'other-P', taskId: 'task-P' }; old.env.detailLoadSequence++
  pending.resolve([{ id: 'task-O', processInstanceId: 'obsolete-O' }]); await loading
  assert.equal(old.env.applicationApprovalRead.value, undefined)
  assert.equal(old.env.approvalTodoTask.value, null)
})
test('late previous round-list cannot publish any mappings into a different task context', async () => {
  const pending = deferred(), p = mount({ fileId: '10', primaryBpmRound: 'native-U', contextKey: 'first' }, { rounds: () => pending.promise })
  try {
    await flush(); p.props.contextKey = 'verify-O'; p.props.contextError = '正在核验实际办理轮次'; p.props.primaryBpmRound = null; await vue.nextTick()
    pending.resolve(clone(rows)); await flush()
    assert.equal(p.state.rounds.length, 0); assert.equal(p.state.evidence, undefined); assert.equal(p.state.selectedKey, '')
  } finally { p.app.unmount() }
})
test('ordinary native detail auto-selects its actual mapping without locking explicit history browsing', async () => {
  const p = mount({ fileId: '10', primaryBpmRound: 'native-U', lockPrimaryRound: false })
  try {
    await flush(); assert.equal(p.state.evidence.bpmRound, 'native-U')
    await p.state.selectRound(p.state.roundKey(rows[1])); assert.equal(p.state.evidence.bpmRound, 'obsolete-O')
  } finally { p.app.unmount() }
})
