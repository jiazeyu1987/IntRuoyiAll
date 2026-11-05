const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

function dialog(api) {
  const filename = 'src/views/mes/qc/template/SamplingTableDialog.vue'
  const source = fs.readFileSync(filename, 'utf8')
  const { descriptor, errors } = parse(source, { filename })
  assert.equal(errors.length, 0)
  const compiled = compileScript(descriptor, { id: 'sampling-test' })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'sampling-test',
    compilerOptions: { bindingMetadata: compiled.bindings } })
  assert.equal(template.errors.length, 0)
  const context = {
    exports: {}, defineExpose() {},
    require(name) { return name === 'vue' ? require('vue') : api }
  }
  const script = `${descriptor.scriptSetup.content}\nexports.state = { open, query, clearResult, totalQuantity, inspectionLevel, rows, result, error, querying };`
  vm.runInNewContext(ts.transpileModule(script, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports.state
}

test('query sends both inputs and clears displayed data when inputs change', async () => {
  const calls = []
  const state = dialog({
    getSamplingTable: async () => [{ minQuantity: 2 }],
    lookupSampling: async (quantity, level) => {
      calls.push([quantity, level])
      return { totalQuantity: quantity, inspectionLevel: level, sampleCode: 'K', nominalSampleSize: 125 }
    }
  })
  await state.open()
  assert.equal(state.rows.value.length, 1)
  state.totalQuantity.value = 3000
  await state.query()
  assert.deepEqual(calls, [[3000, 'II']])
  assert.equal(state.result.value.sampleCode, 'K')
  assert.equal(state.result.value.nominalSampleSize, 125)
  state.inspectionLevel.value = 'III'
  assert.equal(state.result.value, undefined)
  state.clearResult()
  assert.equal(state.result.value, undefined)
})

test('missing, fractional, too small and unsafe quantities cannot issue a query', async () => {
  let calls = 0
  const state = dialog({ lookupSampling: async () => { calls++ } })
  for (const quantity of [null, 0, 1, -2, 2.5, NaN, Infinity, Number.MAX_SAFE_INTEGER + 1]) {
    state.totalQuantity.value = quantity
    await state.query()
    assert.match(state.error.value, /不小于2的整数/)
    assert.equal(state.result.value, undefined)
  }
  assert.equal(calls, 0)
})

test('a stale pending response is discarded even when user returns to original inputs', async () => {
  let resolve
  const state = dialog({ lookupSampling: () => new Promise(done => { resolve = done }) })
  state.totalQuantity.value = 3000
  const pending = state.query()
  state.totalQuantity.value = 500
  state.clearResult()
  state.totalQuantity.value = 3000
  state.clearResult()
  resolve({ totalQuantity: 3000, inspectionLevel: 'II', sampleCode: 'K', nominalSampleSize: 125 })
  await pending
  assert.equal(state.result.value, undefined)
  assert.equal(state.querying.value, false)
})

test('table and lookup failures display errors with no substitute results', async () => {
  const failure = async () => { throw new Error('service unavailable') }
  const state = dialog({ getSamplingTable: failure, lookupSampling: failure })
  await state.open()
  assert.match(state.error.value, /加载失败/)
  assert.equal(state.rows.value.length, 0)
  state.totalQuantity.value = 3000
  await state.query()
  assert.match(state.error.value, /查询失败/)
  assert.equal(state.result.value, undefined)
  assert.equal(state.querying.value, false)
})
