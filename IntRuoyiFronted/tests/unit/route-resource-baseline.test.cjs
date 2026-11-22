const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const root = path.resolve(__dirname, '../..')
const transpile = source => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

test('graph and resource baseline read one identical version context with exact Long IDs', () => {
  const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('designer.ts', script, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === 'loadGraph'))
  assert.ok(statement)
  const calls = []
  const pending = new Promise(() => {})
  let resolutions = 0
  const context = {
    exports: {}, loading: { value: false },
    clearSelectedProcessRouteConfigCache() {},
    props: { routeId: '2121453182031056995' },
    resolveRouteFlowGraphReadRouteVersionId: () => {
      resolutions++
      return resolutions === 1 ? '2121453182031057259' : '2121453182031057260'
    },
    ProRouteApi: { getRouteProcessFlowGraph: (...args) => { calls.push(args); return pending } },
    ProRouteProcessApi: { getRouteProcessListByRoute: (...args) => { calls.push(args); return pending } }
  }
  vm.runInNewContext(transpile(statement.getText(ast) + '\nexports.loadGraph = loadGraph'), context)
  context.exports.loadGraph()
  assert.equal(resolutions, 1)
  assert.deepEqual(calls, [
    ['2121453182031056995', '2121453182031057259'],
    ['2121453182031056995', '2121453182031057259']
  ])
})

test('resource API sends version and route IDs without numeric conversion', async () => {
  const source = fs.readFileSync(path.join(root, 'src/api/mes/pro/route/process/index.ts'), 'utf8')
  const ast = ts.createSourceFile('api.ts', source, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.filter(ts.isVariableStatement)
    .flatMap(s => [...s.declarationList.declarations])
    .find(d => d.name.getText(ast) === 'ProRouteProcessApi')
  const property = declaration.initializer.properties
    .find(p => p.name.getText(ast) === 'getRouteProcessListByRoute')
  let captured
  const context = { exports: {}, request: { get: async options => { captured = options; return [] } } }
  vm.runInNewContext(transpile('exports.read = ' + property.initializer.getText(ast)), context)
  await context.exports.read('2121453182031056995', '2121453182031057259')
  assert.deepEqual(JSON.parse(JSON.stringify(captured)), {
    url: '/mes/pro/route-process/list-by-route',
    params: { routeId: '2121453182031056995', routeVersionId: '2121453182031057259' }
  })
})
