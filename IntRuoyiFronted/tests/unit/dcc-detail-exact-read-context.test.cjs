const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const path = require('node:path')
const root = path.resolve(__dirname, '../..')
function load(file, dependencies = {}) {
  const host = { exports: {}, TextEncoder, TextDecoder, btoa, atob,
    require: id => { if (!(id in dependencies)) throw Error(id); return dependencies[id] } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return host.exports
}
const body = load('src/api/dcc/controlledFile/bodyReadContext.ts')
const detail = () => load('src/views/dcc/controlled-file/detail/detail-read-context.ts', {
  '@/api/dcc/controlledFile/bodyReadContext': body
})
const id = '9007199254740993'
const context = { intent: 'APPROVAL_HISTORY', controlledFileId: id, expectedVersionNo: 'A/1-2',
  applicationType: 'REVISION', bpmRound: 'old-round', taskId: 'old-task' }
const query = () => body.buildDccBodyReadQuery(id, context)

test('encoded approval history remains the selected round/task after body to records navigation', () => {
  const result = detail().resolveDetailReadSelection(id, query())
  assert.equal(result.bpmRound, 'old-round')
  assert.equal(result.taskId, 'old-task')
  assert.equal(result.applicationType, 'REVISION')
  assert.equal(result.expectedVersionNo, 'A/1-2')
  assert.equal(result.lockRound, true)
  assert.equal(result.bodyReadContext.controlledFileId, id)
})

test('duplicate, foreign or conflicting route identities fail without choosing a native/current round', () => {
  const h = detail()
  for (const extra of [{processInstanceId:'new-round'}, {taskId:'new-task'}, {applicationType:'UPLOAD'},
    {processInstanceId:['old-round']}, {taskId:['old-task']}, {processInstanceId:''}])
    assert.throws(() => h.resolveDetailReadSelection(id, {...query(), ...extra}))
  assert.throws(() => h.resolveDetailReadSelection('9007199254740995', query()))
  assert.throws(() => h.resolveDetailReadSelection(Number(id), query()))
  assert.throws(() => h.resolveDetailReadSelection(id, {taskId:'orphan-task'}))
})

test('a plain exact task route is preserved and version-only context never acquires an application', () => {
  const h = detail()
  const task = h.resolveDetailReadSelection(id, {processInstanceId:'r', taskId:'t'})
  assert.equal(task.bpmRound, 'r'); assert.equal(task.taskId, 't'); assert.equal(task.lockRound, true)
  const version = h.resolveDetailReadSelection(id, body.buildDccBodyReadQuery(id,
    {intent:'VERSION_HISTORY', controlledFileId:id, expectedVersionNo:'A/1'}))
  assert.equal(version.bpmRound, undefined); assert.equal(version.lockRound, false)
  assert.equal(version.expectedVersionNo, 'A/1')
})

test('actual task selection compares Long actors exactly and retains encoded requested task', () => {
  const { parse } = require('vue/compiler-sfc'), vue = require('vue')
  const source = parse(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/detail/index.vue'),'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('detail.ts',source,ts.ScriptTarget.Latest,true)
  const declarations = ['toExactUserId','findCurrentUserTodoTask'].map(name => ast.statements.find(n =>
    ts.isVariableStatement(n) && n.declarationList.declarations.some(d => d.name.text === name)).getText(ast))
  const h = {exports:{},currentUserId:vue.ref('9007199254740992'),
    approvalProcessInstanceId:vue.ref('old-round'),detailReadSelectionState:vue.ref({selection:detail().resolveDetailReadSelection(id,query())}),
    DCC_BPM_TASK_STATUS:{RUNNING:1,APPROVING:2}}
  vm.runInNewContext(ts.transpileModule(declarations.join('\n')+'\nexports.find=findCurrentUserTodoTask',{
    compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h)
  const tasks = [{id:'old-task',processInstanceId:'old-round',assignee:'9007199254740993',status:1}]
  assert.equal(h.exports.find(tasks),null)
  h.currentUserId.value='9007199254740993'
  assert.equal(h.exports.find(tasks),tasks[0])
  assert.equal(h.exports.find([{...tasks[0],id:'different-task'}]),null)
})
