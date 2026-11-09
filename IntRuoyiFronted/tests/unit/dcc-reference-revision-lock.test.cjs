const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

test('reference revision lock explains cancellation before source revision', () => {
  const filename = path.resolve(__dirname, '../../src/views/dcc/controlled-file/revision/revision-model.ts')
  const context = { exports: {}, require: () => ({}) }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  const hint = context.exports.revisionLockLabel('REFERENCES_MUST_BE_CANCELLED')
  assert.match(hint, /源文件仍被引用/)
  assert.match(hint, /取消指向该文件的全部引用关系/)
  assert.match(hint, /再按源文件升版流程办理/)
})
