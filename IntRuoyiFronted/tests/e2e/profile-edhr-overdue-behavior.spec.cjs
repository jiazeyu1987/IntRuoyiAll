const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')

// Execute the production loader AST, with the network boundary isolated for unit verification.
function loader(file, names, dependencies) {
  let source = fs.readFileSync(path.join(root, file), 'utf8')
  if (file.endsWith('.vue')) source = parse(source).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
    statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length, 'production loader declarations must exist')
  const selected = statements.map(statement => statement.getText(ast)).join('\n')
  const compiled = ts.transpileModule(selected, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(dependencies), `${compiled}\nreturn ${names[names.length - 1]}`)(...Object.values(dependencies))
}

function requestBoundary(calls) {
  const tasks = [{ id: 'TODO-1', status: 'TODO' }, { id: 'OVERDUE-2', status: 'OVERDUE' }]
  return async (params, options) => {
    calls.push({ params, options })
    const list = params.includeOverdue ? tasks : tasks.filter(task => task.status === (params.status || 'TODO'))
    return { list: list.slice(0, params.pageSize), total: list.length }
  }
}

test('profile eDHR loader asks once for TODO and OVERDUE and retains both formal rows', async () => {
  const calls = []
  const load = loader('src/views/Profile/components/ProfileWorkbench.vue', ['loadEdhrRows'], {
    getEdhrWorkTaskMyPage: requestBoundary(calls), TODO_PAGE_SIZE: 50,
    requirePageList: page => page.list, mapEdhrWorkTaskRow: task => ({ businessId: task.id, status: task.status })
  })
  assert.deepEqual(await load(), [{ businessId: 'TODO-1', status: 'TODO' }, { businessId: 'OVERDUE-2', status: 'OVERDUE' }])
  assert.deepEqual(calls, [{ params: { pageNo: 1, pageSize: 50, includeOverdue: true }, options: { ignoreErrorMessage: true } }])
})

test('profile badge counts the same open statuses using server total rather than first-page length', async () => {
  const calls = []
  const load = loader('src/store/modules/profileWorkbenchTodoBadge.ts', ['loadEdhrWorkTaskTodoTotal'], {
    getEdhrWorkTaskMyPage: requestBoundary(calls), PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE: 1,
    EDHR_WORK_TASK_STATUS_TODO: 'TODO', normalizePageTotal: page => page.total
  })
  assert.equal(await load(), 2)
  assert.deepEqual(calls, [{ params: { pageNo: 1, pageSize: 1, includeOverdue: true }, options: { ignoreErrorMessage: true } }])
})

test('failed eDHR requests propagate from both loaders', async () => {
  const failure = new Error('query failed')
  for (const [file, name] of [
    ['src/views/Profile/components/ProfileWorkbench.vue', 'loadEdhrRows'],
    ['src/store/modules/profileWorkbenchTodoBadge.ts', 'loadEdhrWorkTaskTodoTotal']
  ]) {
    const load = loader(file, [name], {
      getEdhrWorkTaskMyPage: async () => { throw failure }, TODO_PAGE_SIZE: 50,
      PROFILE_WORKBENCH_TODO_BADGE_PAGE_SIZE: 1, EDHR_WORK_TASK_STATUS_TODO: 'TODO',
      requirePageList: () => assert.fail('must not map a failed response'),
      mapEdhrWorkTaskRow: () => assert.fail('must not invent a row'),
      normalizePageTotal: () => assert.fail('must not invent a count')
    })
    await assert.rejects(load(), error => error === failure)
  }
})
