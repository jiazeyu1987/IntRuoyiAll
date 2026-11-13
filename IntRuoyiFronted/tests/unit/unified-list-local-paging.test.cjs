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

test('entering full-result local mode keeps the selected server page size', async () => {
  const ctx = setup({ page: 2, limit: 10, total: 45 })
  try {
    ctx.props.rows = Array.from({ length: 45 }, (_, id) => ({ id }))
    await vue.nextTick()
    assert.equal(ctx.state.resolvedPage.value, 1)
    assert.equal(ctx.state.resolvedLimit.value, 10)
    assert.equal(ctx.state.pageRows.value.length, 10)
  } finally { ctx.stop() }
})


test('explicit server filters keep external total/page/limit and request once without local scanning', async () => {
  let calls = 0
  const ctx = setup({ serverFilter: true, page: 4, total: 200, rows: [{ id: 1 }],
    columns: [{ key: 'name', label: 'Name' }],
    serverFilterQuery: async () => { calls++; return true },
    loadFilterRows: async () => { throw Error('must not scan') } })
  try {
    ctx.state.columnFilter.state.value.conditions = [{ key: 'name', operator: 'contains', value: 'A' }]
    await ctx.state.handleStandardFilterQuery()
    assert.equal(calls, 1)
    assert.equal(ctx.state.resolvedTotal.value, 200)
    assert.equal(ctx.state.resolvedPage.value, 4)
    assert.equal(ctx.state.pageRows.value, undefined)
    assert.equal(ctx.state.resolveRows(ctx.props.rows), ctx.props.rows)
    assert.equal(JSON.parse(ctx.props.queryModel.listFilters)[0].value, 'A')
    ctx.state.handlePagination({page: 2, limit: 20})
    assert.equal(ctx.events.at(-1)[0], 'pagination')
    await ctx.state.handleStandardFilterReset()
    assert.equal(calls, 2)
    assert.equal(ctx.props.queryModel.listFilters, undefined)
    assert.equal(ctx.state.columnFilter.active.value, false)
  } finally { ctx.stop() }
})

test('server reset failure retains the last successful scope and reports failure', async () => {
  let fail = false
  const ctx = setup({ serverFilter: true, columns: [{key:'name',label:'Name'}],
    serverFilterQuery: async () => { if (fail) throw Error('reset rejected'); return true } })
  try {
    ctx.state.columnFilter.state.value.conditions = [{key:'name',operator:'eq',value:'A'}]
    await ctx.state.handleStandardFilterQuery()
    const oldFilters = ctx.props.queryModel.listFilters
    fail = true
    await assert.rejects(ctx.state.handleStandardFilterReset(), /reset rejected/)
    assert.equal(ctx.props.queryModel.listFilters, oldFilters)
    assert.equal(ctx.state.columnFilter.active.value, true)
    assert.equal(ctx.state.columnFilter.state.value.appliedConditions[0].value, 'A')
    assert.equal(ctx.state.columnFilter.state.value.conditions.length, 0)
  } finally {ctx.stop()}
})
