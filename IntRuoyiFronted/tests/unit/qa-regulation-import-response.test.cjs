const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync('src/api/mes/qc/template/index.ts', 'utf8')
const ast = ts.createSourceFile('api.ts', source, ts.ScriptTarget.Latest, true)
const declaration = ast.statements.flatMap(s => ts.isVariableStatement(s) ? [...s.declarationList.declarations] : [])
  .find(d => d.name.getText(ast) === 'QcTemplateApi')
const method = declaration.initializer.properties.find(p => p.name.getText(ast) === 'importQaRegulationWordDraft')
function load(upload) {
  const context = { request: { upload } }
  vm.runInNewContext(ts.transpileModule(`globalThis.run = ${method.initializer.getText(ast)}`,
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.run
}
test('Word upload unwraps the envelope so saved version and counts reach the page', async () => {
  const payload = { route: 'CREATE', regulationId: 144, versionNo: 'G/0', processCount: 9, itemCount: 19 }
  const data = new FormData()
  const run = load(async options => {
    assert.equal(options.url, '/mes/qa/inspection-regulation/import-word-draft')
    assert.equal(options.data.get('ownerModule'), 'MES_QA')
    assert.equal(options.data.get('publishAfterImport'), 'false')
    return { code: 0, data: payload }
  })
  assert.equal(await run(data, { ownerModule: 'MES_QA', publishAfterImport: false }), payload)
})
test('failed upload remains an error and cannot produce a saved result', async () => {
  const error = new Error('导入失败')
  await assert.rejects(load(async () => { throw error })(new FormData()), error)
})
