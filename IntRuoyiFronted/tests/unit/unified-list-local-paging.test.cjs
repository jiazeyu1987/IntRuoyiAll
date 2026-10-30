const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const { descriptor } = parse(fs.readFileSync('src/components/UnifiedListTemplate/index.vue', 'utf8'))
const source = compileScript(descriptor, { id: 'paging-contract' }).content
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(overrides) {
  const exports = {}
  vm.runInNewContext(compiled, {
    exports,
    require: id => {
      if (id === 'vue') return vue
      if (id.endsWith('.vue')) return { default: {} }
      if (id === '@/hooks/web/useListColumnFilter') {
        const result = {}
        const helper = ts.transpileModule(fs.readFileSync('src/hooks/web/useListColumnFilter.ts', 'utf8'), {
          compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
        }).outputText
        vm.runInNewContext(helper, { exports: result, require: () => vue })
        return result
      }
      throw new Error(`Unexpected runtime dependency: ${id}`)
    }
  })
  const props = vue.reactive({ tableKey: 'test.local', queryModel: {}, columns: [], filterDefinitions: [],
    quickFilterState: { conditions: [], appliedConditions: [] }, showMultiFilter: false,
    multiFilterState: undefined, multiFilterDefinitions: [], page: 1, limit: 20, total: 0, ...overrides })
  const events = []
  const scope = vue.effectScope()
  const state = scope.run(() => exports.default.setup(props, { expose() {}, emit: (...args) => events.push(args) }))
  return { props, state, events, stop: () => scope.stop() }
}

test('local arrays page the full dataset exactly once and changing page size restarts at page one', () => {
  const ctx = setup({ rows: Array.from({ length: 45 }, (_, id) => ({ id })) })
  try {
    assert.equal(ctx.state.resolvedTotal.value, 45)
    assert.deepEqual(Array.from(ctx.state.pageRows.value, row => row.id), Array.from({ length: 20 }, (_, id) => id))
    ctx.state.updatePage(3)
    assert.deepEqual(Array.from(ctx.state.pageRows.value, row => row.id), [40, 41, 42, 43, 44])
    ctx.state.updateLimit(10)
    assert.equal(ctx.state.resolvedPage.value, 1)
    assert.equal(ctx.state.pageRows.value.length, 10)
    assert.deepEqual(ctx.events, [['update:page', 3], ['update:limit', 10]])
  } finally { ctx.stop() }
})

test('changing the local search result resets pagination, including replacement with the same total', async () => {
  const ctx = setup({ rows: Array.from({ length: 45 }, (_, id) => ({ id })) })
  try {
    ctx.state.updatePage(3)
    ctx.props.rows = Array.from({ length: 45 }, (_, id) => ({ id: id + 100 }))
    await vue.nextTick()
    assert.equal(ctx.state.resolvedPage.value, 1)
    assert.equal(ctx.state.pageRows.value[0].id, 100)
    ctx.state.updatePage(3)
    ctx.props.rows.splice(1)
    await vue.nextTick()
    assert.equal(ctx.state.resolvedPage.value, 1)
    assert.equal(ctx.state.resolvedTotal.value, 1)
  } finally { ctx.stop() }
})

test('server paging remains externally controlled and the template does not slice server results', () => {
  const ctx = setup({ page: 4, limit: 50, total: 233 })
  try {
    assert.equal(ctx.state.resolvedTotal.value, 233)
    assert.equal(ctx.state.resolvedPage.value, 4)
    assert.equal(ctx.state.resolvedLimit.value, 50)
    assert.equal(ctx.state.pageRows.value, undefined)
    ctx.state.updatePage(5)
    ctx.state.updateLimit(20)
    assert.equal(ctx.props.page, 4)
    assert.equal(ctx.props.limit, 50)
    assert.deepEqual(ctx.events, [['update:page', 5], ['update:limit', 20]])
  } finally { ctx.stop() }
})
