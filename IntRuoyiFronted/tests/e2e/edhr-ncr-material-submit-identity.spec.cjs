const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', source.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)[1], ts.ScriptTarget.Latest, true)
const declaration = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'buildReviewMaterials'))
assert(declaration, 'production builder exists')
function build(form) {
  const context = { disposeForm: form }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(declaration.getText(ast) + '\nglobalThis.build = buildReviewMaterials;', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
  return context.build()
}
test('materials preserve independent opaque file IDs and names', () => {
  const result = build({ reviewMaterialUrls: ['a', 'b'], reviewMaterialNames: { a: ' same.pdf ', b: ' same.pdf ' },
    reviewMaterialIds: { a: '9007199254740993', b: '9007199254740994' } })
  assert.equal(result[0].fileId, '9007199254740993')
  assert.equal(result[1].fileId, '9007199254740994')
  assert.equal(result[0].fileName, ' same.pdf ')
})
test('legacy material without formal identity explicitly requires reupload', () => {
  assert.throws(() => build({ reviewMaterialUrls: ['a'], reviewMaterialNames: { a: 'old.pdf' }, reviewMaterialIds: {} }), /重新上传/)
})
