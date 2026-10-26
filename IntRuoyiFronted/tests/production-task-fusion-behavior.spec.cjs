const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const root = path.resolve(__dirname, '..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const evaluate = (source, dependencies, names) => {
  const program = ts.createSourceFile('test.ts', source, ts.ScriptTarget.Latest, true)
  const stripped = program.statements.filter(node => !ts.isImportDeclaration(node)).map(node => node.getText(program)).join('\n')
  const code = ts.transpileModule(stripped + `\nglobalThis.result = { ${names} }`, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  const context = { exports: {}, URL, URLSearchParams, ...dependencies }
  vm.runInNewContext(code, context)
  return context.result
}
const navigation = () => {
  const opened = [], pushes = []
  const api = evaluate(read('src/utils/edhrWorkTaskNavigation.ts'), {
    requireExactJavaLong: (value) => { if (!/^[1-9]\d*$/.test(String(value))) throw Error('invalid id'); return String(value) },
    openEdhrBatchTask: async params => { opened.push(params); return { executionId: '99', workTaskId: params.workTaskId } }
  }, 'navigateToEdhrWorkTask')
  return { ...api, opened, pushes, router: { push: async location => pushes.push(location) } }
}
test('PQC workbench entry carries exact task and application identity', async () => {
  const h = navigation()
  await h.navigateToEdhrWorkTask(h.router, { id: '9007199254740999', taskType: 'PQC_PRODUCTION_RELEASE', businessScopeType: 'RELEASE_APPLICATION', businessScopeId: '9', actionUrl: '/mes/production-release/pqc?applicationId=9' }, 'https://test.local')
  assert.equal(h.pushes[0].query.workTaskId, '9007199254740999')
  assert.equal(h.pushes[0].query.applicationId, '9')
  assert.equal(h.opened.length, 0)
})
test('conflicting task identity fails before navigation', async () => {
  const h = navigation()
  await assert.rejects(h.navigateToEdhrWorkTask(h.router, { id: '12', taskType: 'ARCHIVE', actionUrl: '/mes/pro/feedback/edhr-batch-execution/detail?id=8&workTaskId=13' }, 'https://test.local'), /不一致/)
  assert.equal(h.pushes.length, 0)
})
test('report and archive entries do not execute the form-opening action', async () => {
  for (const taskType of ['ARCHIVE', 'FILL_REPORT']) {
    const h = navigation()
    await h.navigateToEdhrWorkTask(h.router, { id: '12', taskType, batchExecutionId: '8', batchTaskId: '4', actionUrl: '/mes/pro/feedback/edhr-batch-execution/detail?id=8&batchTaskId=4&nodeType=STERILIZATION_REPORT' }, 'https://test.local')
    assert.equal(h.opened.length, 0)
    assert.equal(h.pushes[0].query.workTaskId, '12')
  }
})
test('notification report entry is read navigation without opening a form', async () => {
  const h = navigation()
  await h.navigateToEdhrWorkTask(h.router, { id: '12', batchExecutionId: '8', batchTaskId: '4', actionUrl: '/mes/pro/feedback/edhr-batch-execution/detail?id=8&batchTaskId=4&nodeType=STERILIZATION_REPORT' }, 'https://test.local')
  assert.equal(h.opened.length, 0)
})
test('fill entry still opens its formal form and preserves task identity', async () => {
  const h = navigation()
  await h.navigateToEdhrWorkTask(h.router, { id: '12', taskType: 'FILL', batchExecutionId: '8', batchTaskId: '4' }, 'https://test.local')
  assert.equal(h.opened.length, 1)
  assert.equal(h.pushes[0].query.workTaskId, '12')
  assert.equal(h.pushes[0].query.executionId, '99')
})
const records = (handlers = {}) => {
  const calls = []
  const source = read('src/views/Profile/components/ProductionTaskRecords.vue').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const dependencies = { ref: value => ({ value }), reactive: value => value, useRouter: () => ({}), useMessage: () => ({}), useApprovalCenterInvalidationStore: () => ({ syncSession: () => ({ key: 'tenant1:user1' }) }), watch: () => {}, onBeforeUnmount: () => {}, defineExpose: () => {} }
  for (const name of ['getEdhrWorkTaskDonePage', 'getEdhrWorkTaskCandidateTodoPage', 'getEdhrWorkTaskMyPage']) {
    dependencies[name] = async query => { calls.push({ name, query }); return handlers[name] ? handlers[name](query) : { list: [{ id: name }], total: 1 } }
  }
  const api = evaluate(source, dependencies, 'load, view, visible, query, rows, total, error')
  api.visible.value = true
  return { ...api, calls }
}
test('history, candidate and overdue views use server-scoped queries', async () => {
  const h = records()
  await h.load()
  assert.equal(h.calls[0].name, 'getEdhrWorkTaskDonePage')
  h.view.value = 'candidate'; await h.load()
  assert.equal(h.calls[1].name, 'getEdhrWorkTaskCandidateTodoPage')
  h.view.value = 'overdue'; await h.load()
  assert.equal(h.calls[2].query.status, 'OVERDUE')
})
test('late previous-tab response cannot replace current candidate records', async () => {
  let finish
  const h = records({ getEdhrWorkTaskDonePage: () => new Promise(resolve => { finish = resolve }) })
  const old = h.load()
  h.view.value = 'candidate'; await h.load()
  finish({ list: [{ id: 'stale' }], total: 99 }); await old
  assert.equal(h.rows.value[0].id, 'getEdhrWorkTaskCandidateTodoPage')
  assert.equal(h.total.value, 1)
})
test('retired page is removed and rule configuration remains reachable', () => {
  assert.equal(fs.existsSync(path.join(root, 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')), false)
  assert.doesNotMatch(read('src/router/modules/remaining.ts'), /WorkTaskBoardPage/)
  assert.match(read('src/views/mes/pro/route/index.vue'), /RouteTaskRulesDialog ref="taskRulesRef"/)
  assert.match(read('src/views/Profile/components/ProfileWorkbench.vue'), /ProductionTaskRecords ref="productionRecordsRef"/)
})
