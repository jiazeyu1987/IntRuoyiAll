const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const origin = 'http://localhost:8081'
const actionPath = '/mes/production-release/pqc'
const menuPath = '/mes/pro/production-release/pqc'

function loadModule(file, dependencies) {
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const exports = {}
  Function('require', 'exports', code)(name => {
    assert.ok(Object.hasOwn(dependencies, name), `Unexpected dependency: ${name}`)
    return dependencies[name]
  }, exports)
  return exports
}
const identities = loadModule('src/utils/exactJavaLong.ts', {})
const navigation = loadModule('src/utils/edhrWorkTaskNavigation.ts', {
  '@/utils/exactJavaLong': identities,
  '@/api/mes/pro/edhr/batchExecution': {
    openEdhrBatchTask: async () => { throw Error('Release navigation must not open a fill task') }
  }
})
const task = (overrides = {}) => ({
  id: '9007199254740993', taskType: 'PQC_PRODUCTION_RELEASE',
  businessScopeType: 'RELEASE_APPLICATION', businessScopeId: '319',
  actionUrl: `${actionPath}?applicationId=319#release`, ...overrides
})

test('workbench release action opens the production release menu route with exact task context', async () => {
  const routes = []
  await navigation.navigateToEdhrWorkTask({ push: async route => routes.push(route) }, task(), origin)
  assert.deepEqual(routes, [{ path: menuPath, query: { applicationId: '319', workTaskId: '9007199254740993' } }])
  assert.equal(navigation.normalizeEdhrWorkTaskRoute(task(), origin),
    `${menuPath}?applicationId=319&workTaskId=9007199254740993#release`)
})

test('notification entry and menu entry resolve to the same production release route', () => {
  for (const entry of [actionPath, menuPath]) {
    assert.equal(navigation.EDHR_WORK_TASK_NOTIFY_PATHS.has(entry), true)
    const actionUrl = `${entry}?applicationId=319&workTaskId=2734`
    const route = navigation.normalizeEdhrWorkTaskRouteParts({ actionUrl }, origin)
    assert.equal(route.path, menuPath)
    assert.deepEqual(route.query, { applicationId: '319', workTaskId: '2734' })
    assert.equal(navigation.normalizeEdhrWorkTaskRouteParts(task({ actionUrl: `${entry}?applicationId=319` }), origin).path, menuPath)
  }
})

test('invalid release identity, path and origin still fail before navigation', async () => {
  for (const overrides of [
    { businessScopeType: 'BATCH_TASK' },
    { actionUrl: `${actionPath}?applicationId=320` },
    { actionUrl: `${menuPath}?applicationId=319&applicationId=319` },
    { actionUrl: `${actionPath}?applicationId=319&workTaskId=2734` },
    { actionUrl: `${actionPath}/forged?applicationId=319` },
    { actionUrl: `https://external.invalid${actionPath}?applicationId=319` }
  ]) {
    const routes = []
    await assert.rejects(navigation.navigateToEdhrWorkTask({ push: async route => routes.push(route) }, task(overrides), origin))
    assert.equal(routes.length, 0)
  }
})
