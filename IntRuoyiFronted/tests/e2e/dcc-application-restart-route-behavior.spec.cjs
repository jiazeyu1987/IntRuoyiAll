const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/router/modules/remaining.ts'), 'utf8')
const ast = ts.createSourceFile('remaining.ts', source, ts.ScriptTarget.Latest, true)
let route
function visit(node) {
  if (ts.isObjectLiteralExpression(node) && node.properties.some(property =>
    ts.isPropertyAssignment(property) && property.name.getText(ast) === 'name' && property.initializer.getText(ast) === "'DccControlledFileDetail'")) route = node
  ts.forEachChild(node, visit)
}
visit(ast)
assert.ok(route, 'actual detail route must exist')
const guard = route.properties.find(property => property.name?.getText(ast) === 'beforeEnter').initializer
const js = ts.transpileModule(`const guard = ${guard.getText(ast)}`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const beforeEnter = Function(`${js}; return guard;`)()

test('existing notify/detail restart callers reach their own management form without a forged browser origin', () => {
  for (const extra of [{}, { revisionIntent: 'PARTIAL' }, { revisionIntent: 'REPLACEMENT' }, { operation: 'obsolete' }]) {
    assert.equal(beforeEnter({ query: { management: '1', restartApplication: '1', ...extra } }), true)
  }
})

test('missing or incorrect restart/management marker still redirects bare detail navigation', () => {
  for (const query of [{}, { management: '1' }, { restartApplication: '1' },
    { management: '0', restartApplication: '1' }, { management: '1', restartApplication: 'true' },
    { from: 'application-restart', management: '1' },
    { from: 'application-restart', returnTo: '/dcc/controlled-file/browser' },
    { from: 'application-restart', management: '1', returnTo: '/dcc/controlled-file/workbench' }]) {
    assert.deepEqual(beforeEnter({ query }), { name: 'DccControlledFileBrowser' })
  }
})

test('existing approval, browser, history and project contexts retain their prior entry contract', () => {
  for (const query of [
    { viewer: '1' },
    { handling: 'approval', from: 'approval-center', taskId: 'current-task' },
    { traceability: '1', from: 'browser', returnTo: '/dcc/controlled-file/browser' },
    { traceability: '1', from: 'version-history', returnTo: '/dcc/controlled-file/browser' },
    { management: '1', from: 'browser', returnTo: '/dcc/controlled-file/browser' },
    { management: '1', from: 'project-browser', returnTo: '/dcc/controlled-file/project/322' },
    { mode: 'manage', from: 'workbench', returnTo: '/dcc/controlled-file/workbench' },
    { management: '1', from: 'application-restart', returnTo: '/dcc/controlled-file/browser' }
  ]) assert.equal(beforeEnter({ query }), true)
})
