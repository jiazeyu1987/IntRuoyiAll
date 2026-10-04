const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { createRouter, createMemoryHistory } = require('vue-router')

function actualGuard() {
  const source = ts.createSourceFile('remaining.ts', fs.readFileSync('src/router/modules/remaining.ts', 'utf8'), ts.ScriptTarget.Latest, true)
  let guard
  function visit(node) {
    if (ts.isObjectLiteralExpression(node) && node.properties.some(property => ts.isPropertyAssignment(property) &&
        property.name.getText(source) === 'name' && ts.isStringLiteral(property.initializer) && property.initializer.text === 'DccControlledFileDetail')) {
      const property = node.properties.find(property => ts.isPropertyAssignment(property) && property.name.getText(source) === 'beforeEnter')
      assert.ok(property); guard = property.initializer.getText(source)
    }
    ts.forEachChild(node, visit)
  }
  visit(source); assert.ok(guard, 'actual DCC detail guard missing')
  return new Function(ts.transpileModule(`return (${guard})`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)()
}

async function navigate(query) {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/', component: {} },
    { path: '/dcc/controlled-file/browser', name: 'DccControlledFileBrowser', component: {} },
    { path: '/dcc/controlled-file/detail/:id(\\d+)', name: 'DccControlledFileDetail', beforeEnter: actualGuard(), component: {} }
  ] })
  await router.push({ path: '/dcc/controlled-file/detail/9223372036854775700', query })
  return router.currentRoute.value
}

test('actual router accepts workbench and project-browser management targets without truncating selected Long', async () => {
  for (const query of [
    { mode: 'manage', from: 'workbench', returnTo: '/dcc/controlled-file/workbench' },
    { management: '1', from: 'project-browser', returnTo: '/dcc/controlled-file/browser?tab=project' }
  ]) {
    const route = await navigate(query)
    assert.equal(route.name, 'DccControlledFileDetail')
    assert.equal(route.params.id, '9223372036854775700')
  }
})

test('existing viewer, browser-management, approval and trace paths remain accepted', async () => {
  for (const query of [
    { viewer: '1', from: 'notification' },
    { management: '1', from: 'browser', returnTo: '/dcc/controlled-file/browser' },
    { handling: 'approval', from: 'approval-center', processInstanceId: 'real-bpm' },
    { traceability: '1', from: 'browser', returnTo: '/dcc/controlled-file/browser' }
  ]) assert.equal((await navigate(query)).name, 'DccControlledFileDetail')
})

test('new management contexts require known source, management marker and local DCC return path', async () => {
  for (const query of [
    {}, { from: 'workbench', returnTo: '/dcc/controlled-file/workbench' },
    { mode: 'manage', from: 'unknown', returnTo: '/dcc/controlled-file/browser' },
    { mode: 'manage', from: 'workbench' },
    { mode: 'manage', from: 'workbench', returnTo: 'https://example.com' },
    { management: '1', from: 'project-browser', returnTo: '//example.com' }
  ]) assert.equal((await navigate(query)).name, 'DccControlledFileBrowser')
})
