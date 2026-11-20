const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = 'src/views/form-center/parser/components/SimpleReuseDialog.vue'
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
const script = compileScript(descriptor, { id: 'reuse-default' })
function dialog() {
  const context = { exports: {}, require: name => {
    assert.equal(name, 'vue')
    return vue
  } }
  vm.runInNewContext(ts.transpileModule(script.content, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports.default.setup({}, { expose() {}, emit() {} })
}
const candidate = (processId, currentWorkshop = false, crafts = []) => ({
  processId, currentWorkshop, name: '清洗', code: `P${processId}`, workshops: [],
  groups: [{ groupIndex: 0, parameters: [], crafts }]
})
const row = (candidates, processIndex = 0) => ({ processIndex, name: '清洗', candidates })
const selection = (processId, processIndex = 0) => ({ processIndex, processId, craftSelections: [] })

test('defaults to current-workshop process and its unique matching craft without mutating input', () => {
  const state = dialog(), input = [selection()]
  state.open([row([candidate(1), candidate(2, true, [{ id: 20 }])])], input)
  assert.equal(state.selections.value[0].processId, 2)
  assert.equal(state.selections.value[0].craftSelections[0].craftId, 20)
  assert.equal(input[0].processId, undefined)
  assert.equal(input[0].craftSelections.length, 0)
})
test('reuses a process even when parameters require a new craft', () => {
  const state = dialog()
  state.open([row([candidate(1)])], [selection()])
  assert.equal(state.selections.value[0].processId, 1)
  assert.equal(state.selections.value[0].craftSelections.length, 0)
})
test('keeps explicit new choice when reopening and preserves existing selections', () => {
  const state = dialog(), matches = [row([candidate(1), candidate(2, true)])]
  state.open(matches, [selection()], [0])
  assert.equal(state.selections.value[0].processId, undefined)
  state.open(matches, [selection(1)])
  assert.equal(state.selections.value[0].processId, 1)
})
test('does not silently replace a stale selection', () => {
  const state = dialog()
  state.open([row([candidate(2)])], [selection(1)])
  assert.equal(state.selections.value[0].processId, 1)
  assert.equal(state.invalidSelection.value, true)
})
test('does not recommend the same process twice in one route', () => {
  const state = dialog(), candidates = [candidate(1, true), candidate(2)]
  state.open([row(candidates), row(candidates, 1)], [selection(), selection(1, 1)])
  assert.equal(state.selections.value[0].processId, 2)
  assert.equal(state.selections.value[1].processId, 1)
})
test('leaves unmatched processes new and compiles the dialog template', () => {
  const state = dialog()
  state.open([row([])], [selection()])
  assert.equal(state.selections.value[0].processId, undefined)
  const compiled = compileTemplate({ source: descriptor.template.content, filename, id: 'reuse-default',
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiled.errors, [])
})
