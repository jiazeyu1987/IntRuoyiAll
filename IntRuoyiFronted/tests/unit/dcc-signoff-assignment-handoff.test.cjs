const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript'), vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.join(__dirname, '../../src/views/dcc/controlled-file/detail')
const ast = name => ts.createSourceFile(name, parse(fs.readFileSync(path.join(root, name), 'utf8')).descriptor.scriptSetup.content,
  ts.ScriptTarget.Latest, true)
const parent = ast('index.vue'), child = ast('DetailSignoffAssignment.vue')
function declaration(source, name) {
  const node = source.statements.find(s => ts.isVariableStatement(s)
    && s.declarationList.declarations.some(d => d.name.getText(source) === name))
  assert.ok(node, name)
  return node.getText(source)
}
function evaluate(source, env) {
  vm.runInNewContext(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, env)
}
const deferred = () => {
  let resolve
  const promise = new Promise(done => { resolve = done })
  return { promise, resolve }
}
const settle = async () => { await vue.nextTick(); await Promise.resolve(); await vue.nextTick() }

function handoff() {
  const navigation = deferred(), reload = deferred(), requests = [], successes = []
  let reloads = 0, saved
  const task = { id: 'department-task', taskDefinitionKey: 'MATRIX_REVIEW', assigneeUserId: 'leader' }
  const env = { computed: vue.computed, approvalTodoTask: vue.ref(task), signoffAssignmentState: vue.ref(undefined),
    fileDetail: vue.ref({ id: '101' }), isExternalReviewProcess: vue.ref(false), isApprovalUploadHandlingPage: vue.ref(true),
    syncStageProgress: () => {}, message: { success: text => successes.push(text) },
    returnToCompletedTaskInbox: () => navigation.promise,
    reloadAll: () => { reloads++; return reload.promise } }
  evaluate(['isSignoffTask', 'signoffReadyForReview', 'isActualSignoffReviewPage',
    'handleSignoffAssignmentState', 'handleSignoffAssignmentSaved'].map(name => declaration(parent, name)).join('\n')
    + '\nglobalThis.page = { isActualSignoffReviewPage, handleSignoffAssignmentState, handleSignoffAssignmentSaved }', env)

  const descriptor = parse(fs.readFileSync(path.join(root, 'SupervisorReviewActions.vue'), 'utf8')).descriptor
  const module = { exports: {}, require: name => {
    if (name === 'vue') return vue
    if (name === '@/api/dcc/controlledFile/workflow') return { getControlledFileTaskActionReadiness: async (fileId, payload) => {
      requests.push({ fileId, taskId: payload.taskId })
      return { ready: true, blockers: [] }
    } }
    if (name === './presentation') return { resolveReadSideErrorMessage: error => error.message }
    throw new Error(`Unexpected dependency: ${name}`)
  }, ResizeObserver: class { observe() {} disconnect() {} } }
  evaluate(compileScript(descriptor, { id: 'handoff-actions' }).content, module)
  const Actions = module.exports.default
  Actions.render = () => vue.h('section')
  const renderer = vue.createRenderer({
    createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}),
    insert: (node, target) => { node.parent = target; target.children.push(node) },
    remove: node => { node.parent.children = node.parent.children.filter(child => child !== node) },
    setText() {}, setElementText() {}, patchProp() {}, parentNode: node => node.parent, nextSibling: () => null
  })
  const app = renderer.createApp({ render: () => env.page.isActualSignoffReviewPage.value
    ? vue.h(Actions, { fileId: '101', taskId: env.approvalTodoTask.value.id, taskName: '实际会签人会签',
      contextKey: 'current-round', blockedReason: '', submitting: false }) : vue.h('section') })
  app.mount({ children: [] })

  const event = { fileId: '101', taskId: task.id, processInstanceId: 'round-1' }
  const assignment = { props: event, context: vue.ref({ assigned: false }), emit: (name, value) => {
    if (name === 'state') env.page.handleSignoffAssignmentState(value)
    if (name === 'saved') saved = env.page.handleSignoffAssignmentSaved()
  } }
  evaluate(declaration(child, 'onSaved') + '\nglobalThis.saved = onSaved', assignment)
  return { env, requests, successes, navigation, reload, app, get reloads() { return reloads },
    saved: () => saved, complete: () => assignment.saved(event),
    refreshForSigner: user => {
      env.approvalTodoTask.value = { ...task, assigneeUserId: user }
      env.signoffAssignmentState.value = { fileId: '101', taskId: task.id, assigned: true }
    } }
}

test('successful assignment does not mount review actions with the leader task while inbox navigation is pending', async () => {
  const h = handoff()
  try {
    h.complete()
    await settle()
    assert.equal(h.env.approvalTodoTask.value, null)
    assert.equal(h.env.signoffAssignmentState.value, undefined)
    assert.equal(h.requests.length, 0, '不能以旧负责人身份请求实际会签就绪检查')
    assert.equal(h.successes.length, 1)
    h.navigation.resolve(true)
    await h.saved()
    assert.equal(h.reloads, 0)
  } finally { h.app.unmount() }
})

test('direct-detail assignment waits for reload and the actual recipient still gets readiness checks', async () => {
  const h = handoff()
  try {
    h.complete()
    h.navigation.resolve(false)
    await settle()
    assert.equal(h.reloads, 1)
    assert.equal(h.requests.length, 0)
    h.reload.resolve()
    await h.saved()
    assert.equal(h.env.page.isActualSignoffReviewPage.value, false)
    h.refreshForSigner('recipient')
    await settle()
    assert.deepEqual(h.requests, [{ fileId: '101', taskId: 'department-task' }])
  } finally { h.app.unmount() }
})

test('self assignment also requires a fresh task read before enabling independent review', async () => {
  const h = handoff()
  try {
    h.complete()
    h.navigation.resolve(false)
    await settle()
    assert.equal(h.requests.length, 0)
    h.refreshForSigner('leader')
    h.reload.resolve()
    await h.saved()
    await settle()
    assert.equal(h.env.page.isActualSignoffReviewPage.value, true)
    assert.equal(h.requests.length, 1, '刷新后本人仍可独立会签，不能禁掉正常就绪检查')
  } finally { h.app.unmount() }
})
