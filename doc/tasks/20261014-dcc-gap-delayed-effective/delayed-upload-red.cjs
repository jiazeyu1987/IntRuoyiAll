// Isolated production-helper probe. No HTTP, DB, Maven, service, or production mutation.
const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const root = path.resolve(__dirname, '../../..')
const frontendRequire = createRequire(path.join(root, 'IntRuoyiFronted/package.json'))
const ts = frontendRequire('typescript')
const source = fs.readFileSync(path.join(root, 'IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts'), 'utf8')
const ast = ts.createSourceFile('submitter.ts', source, ts.ScriptTarget.Latest, true)
const names = ['validateUploadEffectiveDate', 'buildUploadEffectiveChoice']
const declarations = names.map(name => {
  const statement = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, 'Actual production declaration missing: ' + name)
  return statement.getText(ast)
})
const context = { exports: {}, Error, Date }
vm.runInNewContext(ts.transpileModule(declarations.join('\n'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, context)
const build = context.exports.buildUploadEffectiveChoice

test('BASELINE: immediate selection keeps the date absent', () => {
  const result = build('AFTER_STANDARDIZATION_SIGNATURE', null, '2026-10-14')
  assert.equal(result.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE')
  assert.equal(result.effectiveDate, null)
})
test('BASELINE: current preset mode rejects the user leaving the date empty', () => {
  assert.throws(() => build('PRESET_DATE', null, '2026-10-14'))
})
test('G01 proposed contract RED: delayed upload accepts no concrete time', () => {
  const result = build('DELAYED', null, '2026-10-14')
  assert.equal(result.effectiveMode, 'DELAYED')
  assert.equal(result.effectiveDate, null)
})
