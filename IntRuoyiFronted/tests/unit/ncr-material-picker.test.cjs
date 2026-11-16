const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewWorkspace.vue'), 'utf8')
const ast = ts.createSourceFile('workspace.ts', source.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)[1], ts.ScriptTarget.Latest, true)
const handler = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'handleFilesChange'))
function harness() {
  const events = []
  const context = { editable: { value: true }, busy: { value: false }, emit: (...args) => events.push(args) }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(handler.getText(ast) + '\nglobalThis.change = handleFilesChange', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, context)
  return { context, events }
}
test('file selection delivers files and resets the native input for same-file reselection', () => {
  const h = harness(), file = { name: 'review.pdf' }, input = { files: [file], value: 'selected' }
  h.context.change({ target: input })
  assert.equal(input.value, '')
  assert.equal(h.events[0][0], 'upload')
  assert.equal(h.events[0][1][0], file)
  h.context.change({ target: input })
  assert.equal(h.events.length, 2)
})
test('cancelling the picker starts no upload', () => {
  const h = harness()
  h.context.change({ target: { files: [], value: '' } })
  assert.equal(h.events.length, 0)
})
test('readonly or submitting workspaces cannot emit uploads', () => {
  for (const [state, value] of [['editable', false], ['busy', true]]) {
    const h = harness()
    h.context[state].value = value
    h.context.change({ target: { files: [{ name: 'review.pdf' }], value: 'selected' } })
    assert.equal(h.events.length, 0)
  }
})
test('the upload control is a visible native input without scripted picker activation', () => {
  assert.match(source, /<input[\s\S]*?type="file"[\s\S]*?data-edhr-ncr-select-material/)
  assert.doesNotMatch(source, /showPicker|\.click\(|\bhidden\b|opacity:\s*0\b|display:\s*none/)
})
