import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const parse = (name, text) => ts.createSourceFile(name, text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const js = text => ts.transpileModule(text, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
function find(source, predicate) {
  let value
  function walk(node) { if (value) return; if (predicate(node)) value = node; else ts.forEachChild(node, walk) }
  walk(source); assert.ok(value, 'actual source node must exist'); return value
}
const routerSource = fs.readFileSync(path.join(root, 'src/router/modules/remaining.ts'), 'utf8')
const routerAst = parse('remaining.ts', routerSource)
const detailObject = find(routerAst, node => ts.isObjectLiteralExpression(node) && node.properties.some(p => ts.isPropertyAssignment(p) && p.name.getText(routerAst) === 'path' && ts.isStringLiteral(p.initializer) && p.initializer.text.startsWith('controlled-file/detail/')))
const guardNode = detailObject.properties.find(p => ts.isPropertyAssignment(p) && p.name.getText(routerAst) === 'beforeEnter')
assert.ok(guardNode && ts.isArrowFunction(guardNode.initializer), 'actual detail guard required')
const guard = vm.runInNewContext(js('const guard = ' + guardNode.initializer.getText(routerAst) + '; globalThis.result = guard;') + '\nresult')
const vue = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
const script = vue.match(/<script\b(?=[^>]*setup)(?=[^>]*lang="ts")[^>]*>([\s\S]*?)<\/script>/)?.[1]
assert.ok(script, 'actual Vue setup script required')
const vueAst = parse('detail.vue.ts', script)
const handler = find(vueAst, node => ts.isVariableDeclaration(node) && node.name.getText(vueAst) === 'handleApplicationSubmitted')
let helper = {}
const helperFile = path.join(root, 'src/views/dcc/controlled-file/shared/submitted-application-navigation.ts')
if (fs.existsSync(helperFile)) {
  const sandbox = { exports: {}, module: { exports: {} } }
  vm.runInNewContext(js(fs.readFileSync(helperFile, 'utf8')), sandbox)
  helper = sandbox.exports
}
async function invoke(id, context) {
  const routes = []; let reloads = 0
  const sandbox = { ...helper, route: context, controlledFileId: { value: '9198354931001' }, message: { success() {} },
    router: { async push(value) { const gate = guard({ query: value.query }); routes.push({ requested: JSON.parse(JSON.stringify(value)), guardResult: gate === true ? true : JSON.parse(JSON.stringify(gate)) }) } },
    reloadAll: async () => { reloads++ } }
  vm.runInNewContext(js('const handleApplicationSubmitted = ' + handler.initializer.getText(vueAst) + '; globalThis.handler = handleApplicationSubmitted;'), sandbox)
  await sandbox.handler(id)
  return { routes, reloads }
}
const context = { fullPath: '/dcc/controlled-file/detail/9198354931001?from=project-browser&management=1', query: { from: 'project-browser', management: '1', returnTo: '/dcc/controlled-file/browser?browserMode=project&keyword=task-file' } }
const tests = []
const test = (name, fn) => tests.push([name, fn])
test('actual submitted candidate caller is accepted by actual route guard and preserves original return', async () => {
  const result = await invoke('9198354931099', context)
  assert.equal(result.reloads, 0)
  assert.equal(result.routes.length, 1)
  assert.equal(result.routes[0].guardResult, true, 'candidate detail must not redirect to browser')
  assert.equal(result.routes[0].requested.path, '/dcc/controlled-file/detail/9198354931099')
  assert.equal(result.routes[0].requested.query.returnTo, context.query.returnTo)
  assert.equal(result.routes[0].requested.query.from, 'project-browser')
})
test('same submitted file reloads without navigation', async () => {
  const result = await invoke('9198354931001', context)
  assert.equal(result.reloads, 1); assert.deepEqual(result.routes, [])
})
test('browser and workbench management contexts remain accepted by the actual guard', async () => {
  for (const from of ['browser', 'workbench']) {
    const source = { fullPath: context.fullPath, query: { from, mode: 'manage', management: '1', returnTo: '/dcc/controlled-file/' + (from === 'browser' ? 'browser?keyword=task' : 'workbench') } }
    const result = await invoke('9223372036854775807', source)
    assert.equal(result.routes[0].guardResult, true)
    assert.equal(result.routes[0].requested.path, '/dcc/controlled-file/detail/9223372036854775807')
    assert.equal(result.routes[0].requested.query.returnTo, source.query.returnTo)
    assert.equal(result.routes[0].requested.query.from, from)
  }
})
test('new candidate never inherits predecessor BPM/task/handling/viewer or other query identity', async () => {
  const source = { ...context, query: { ...context.query, taskId: 'old-task', processInstanceId: 'old-bpm', handling: 'approval', viewer: '1', projectId: 'old-project', traceability: '1' } }
  const result = await invoke('9198354931099', source)
  assert.deepEqual(Object.keys(result.routes[0].requested.query).sort(), ['from', 'management', 'returnTo'])
  assert.equal(result.routes[0].guardResult, true)
})
test('unsafe or non-string formal identities reject without navigation or reload', async () => {
  for (const id of [0, 9198354931099, true, null, '', '0', '-1', '01', '1.5', '9223372036854775808', '1/other', ['1']]) {
    await assert.rejects(() => invoke(id, context))
  }
})
test('arbitrary submit/approval and missing foreign return contexts never grant management navigation', async () => {
  for (const query of [
    { from: 'application-submit', returnTo: context.query.returnTo, management: '1' },
    { from: 'approval-center', returnTo: context.query.returnTo, management: '1' },
    { from: 'project-browser', returnTo: context.query.returnTo },
    { from: 'project-browser', management: '1' },
    { from: 'project-browser', management: '1', returnTo: 'https://foreign.invalid/' },
    { from: 'project-browser', management: '1', returnTo: '/system/users' },
    { from: 'project-browser', management: '1', returnTo: ['/dcc/controlled-file/browser'] },
    { from: 'project-browser', management: '1', returnTo: '/dcc/controlled-file/\\foreign' }
  ]) await assert.rejects(() => invoke('9198354931099', { fullPath: context.fullPath, query }))
})
test('the unchanged actual guard still rejects bare application-submit navigation', () => {
  assert.equal(JSON.stringify(guard({ query: { from: 'application-submit' } })), JSON.stringify({ name: 'DccControlledFileBrowser' }))
})
let failed = 0
for (const [name, fn] of tests) { try { await fn(); console.log('PASS ' + name) } catch (_) { failed++; console.log('FAIL ' + name) } }
console.log(JSON.stringify({ tests: tests.length, failures: failed, actualGuardSourceExecuted: true, actualVueHandlerSourceExecuted: true, actualBrowserRun: false }))
if (failed) process.exitCode = 1
