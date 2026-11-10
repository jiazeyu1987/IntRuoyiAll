const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const source = parse(fs.readFileSync('src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('operation.ts', source, ts.ScriptTarget.Latest, true)
const names = ['initNextAssigneesFormField', 'selectNextAssigneesConfirm', 'validateNextAssignees', 'closePopover', 'loadTodoTask', 'handleAudit']
const declarations = ast.statements.filter(statement => ts.isVariableStatement(statement)
  && statement.declarationList.declarations.some(declaration => names.includes(declaration.name.text)))
const code = ts.transpileModule(declarations.map(statement => statement.getText(ast)).join('\n')
  + '\nexports = {' + names.join(',') + '}', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const plain = value => JSON.parse(JSON.stringify(value))
const candidate = (id, users = [{ id: '910516', nickname: '部门负责人' }], strategy = 34) => ({ id, candidateUsers: users, candidateStrategy: strategy })

function host() {
  const displays = [], warnings = [], submissions = []
  const c = {
    nextApprovalRequestId: 0, pendingNextNodesTask: null,
    props: { processInstance: { id: 'actual-instance' } },
    runningTask: { value: { id: 'actual-task' } },
    nextAssigneesActivityNode: { value: [] }, manualNextAssignees: { value: {} },
    nextAssigneesTimelineRef: { value: { batchSetCustomApproveUsers: value => displays.push(plain(value)) } },
    approveReasonForm: { nextAssignees: {}, reason: '同意' },
    CandidateStrategy: { START_USER_SELECT: 35, APPROVE_USER_SELECT: 34 },
    ProcessInstanceApi: { getNextApprovalNodes: async () => [] },
    getUpdatedProcessInstanceVariables: () => ({}), nextTick: async () => {},
    isEmpty: value => value == null || Object.keys(value).length === 0,
    message: { warning: value => warnings.push(value), success: () => {} },
    popOverVisible: { value: { approve: true } },
    approveForm: { value: {} }, approveFormFApi: { value: {} }, reasonRequire: { value: false }, nodeTypeName: { value: '' },
    NodeType: { TRANSACTOR_NODE: 99 },
    isDccControlledFileProcess: { value: false }, formLoading: { value: false },
    validateNormalForm: async () => true, reload: () => {},
    TaskApi: { approveTask: async data => submissions.push(plain(data)) }
  }
  vm.runInNewContext(code, c)
  return { c, displays, warnings, submissions, ...c.exports,
    async predict(nodes) { c.ProcessInstanceApi.getNextApprovalNodes = async () => nodes; await c.exports.initNextAssigneesFormField() } }
}

test('displayed matrix assignees pass validation and reach the approval request without reselection', async () => {
  const h = host()
  await h.predict([candidate('MATRIX_REVIEW')])
  assert.equal(h.validateNextAssignees(), true)
  assert.deepEqual(h.displays.at(-1), { MATRIX_REVIEW: [{ id: '910516', nickname: '部门负责人' }] })
  await h.handleAudit(true, { validate: async () => {}, resetFields: () => {} })
  assert.equal(h.submissions.length, 1)
  assert.deepEqual(h.submissions[0].nextAssignees, { MATRIX_REVIEW: ['910516'] })
  assert.deepEqual(h.warnings, [])
})

test('manual choices survive recalculation in both display and request; deliberate empty choice remains empty', async () => {
  const h = host()
  await h.predict([candidate('A')])
  h.selectNextAssigneesConfirm('A', [{ id: 'selected', nickname: '手选人员' }])
  await h.predict([candidate('A')])
  assert.deepEqual(plain(h.c.approveReasonForm.nextAssignees), { A: ['selected'] })
  assert.equal(h.displays.at(-1).A[0].id, 'selected')
  h.selectNextAssigneesConfirm('A', [])
  await h.predict([candidate('A')])
  assert.deepEqual(h.displays.at(-1), { A: [] })
  assert.equal(h.validateNextAssignees(), false)
  await h.handleAudit(true, { validate: async () => {} })
  assert.equal(h.submissions.length, 0)
})

test('new branches discard old selections and refresh server defaults without sending fixed-node candidates', async () => {
  const h = host()
  await h.predict([candidate('A')])
  h.selectNextAssigneesConfirm('A', [{ id: 'old' }])
  await h.predict([candidate('B', [{ id: 'new' }]), candidate('FIXED', [{ id: 'fixed' }], 10)])
  assert.deepEqual(plain(h.c.approveReasonForm.nextAssignees), { B: ['new'] })
  assert.deepEqual(plain(h.c.manualNextAssignees.value), {})
  await h.predict([candidate('A')])
  assert.deepEqual(plain(h.c.approveReasonForm.nextAssignees), { A: ['910516'] })
  await h.predict([])
  assert.deepEqual(plain(h.c.approveReasonForm.nextAssignees), {})
  assert.equal(h.validateNextAssignees(), true)
})

test('missing required assignees still block approval', async () => {
  const h = host()
  await h.predict([candidate('A', []), candidate('START', [], 35)])
  assert.equal(h.validateNextAssignees(), false)
  h.selectNextAssigneesConfirm('A', [{ id: 'first' }])
  assert.equal(h.validateNextAssignees(), false)
  h.selectNextAssigneesConfirm('START', [{ id: 'second' }])
  assert.equal(h.validateNextAssignees(), true)
})

test('older predictions cannot restore stale nodes or overwrite a newer display', async () => {
  const h = host()
  let resolveOld
  h.c.ProcessInstanceApi.getNextApprovalNodes = () => new Promise(resolve => { resolveOld = resolve })
  const old = h.initNextAssigneesFormField()
  await h.predict([candidate('NEW')])
  resolveOld([candidate('OLD')])
  await old
  assert.deepEqual(plain(h.c.approveReasonForm.nextAssignees), { NEW: ['910516'] })
  assert.deepEqual(Object.keys(h.displays.at(-1)), ['NEW'])
})

test('closing or changing tasks clears selections and invalidates in-flight predictions', async () => {
  for (const reset of [h => h.closePopover('approve', { resetFields: () => {} }), h => h.loadTodoTask({ id: 'new-task' })]) {
    const h = host()
    await h.predict([candidate('A')])
    h.selectNextAssigneesConfirm('A', [{ id: 'old' }])
    let resolveOld
    h.c.ProcessInstanceApi.getNextApprovalNodes = () => new Promise(resolve => { resolveOld = resolve })
    const old = h.initNextAssigneesFormField()
    reset(h)
    resolveOld([candidate('A')])
    await old
    assert.deepEqual(plain(h.c.approveReasonForm.nextAssignees), {})
    assert.deepEqual(plain(h.c.manualNextAssignees.value), {})
    assert.equal(h.c.nextAssigneesActivityNode.value.length, 0)
  }
})
