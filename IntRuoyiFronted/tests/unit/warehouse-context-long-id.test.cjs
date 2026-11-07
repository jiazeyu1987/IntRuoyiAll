const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const idModule = { exports: {} }
vm.runInNewContext(transpile(fs.readFileSync('src/utils/routeQueryId.ts', 'utf8')), idModule)
const parsePositiveRouteQueryId = idModule.exports.parsePositiveRouteQueryId

for (const [folder, action, key, state, api, method] of [
  ['location', 'loadWarehouseContext', 'warehouseId', 'currentWarehouse', 'WmWarehouseApi', 'getWarehouse'],
  ['area', 'loadLocationContext', 'locationId', 'currentLocation', 'WmWarehouseLocationApi', 'getWarehouseLocation']
]) {
  const source = parse(fs.readFileSync(`src/views/mes/wm/warehouse/${folder}/index.vue`, 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('context.ts', source, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === action))
  const build = (value, fetch) => {
    const context = { route: { query: { [key]: value } }, [state]: { value: {} }, queryParams: {},
      parsePositiveRouteQueryId, [api]: { [method]: fetch } }
    vm.runInNewContext(transpile(statement.getText(ast)) + `\nglobalThis.run = ${action}`, context)
    return context
  }
  test(`${folder}: exact long ID reaches detail lookup and list filtering`, async () => {
    const longId = '2118566488787140609'
    const calls = []
    const h = build(longId, async id => { calls.push(id); return { name: '测试', warehouseId: longId, warehouseName: '测试仓' } })
    await h.run()
    assert.deepEqual(calls, [longId])
    assert.equal(h[state].value.id, longId)
    assert.equal(h.queryParams[key], longId)
  })
  test(`${folder}: invalid ID never requests a different parent`, async () => {
    for (const input of ['0', '-1', 'abc', '2e18']) {
      const h = build(input, async () => assert.fail('invalid parent requested'))
      await h.run()
      assert.equal(h.queryParams[key], undefined)
    }
  })
  test(`${folder}: parent lookup failure remains visible`, async () => {
    const h = build('2118566488787140609', async () => { throw new Error('parent unavailable') })
    await assert.rejects(h.run(), /parent unavailable/)
  })
}
