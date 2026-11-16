const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const cache = new Map()
function load(file) {
  if (cache.has(file)) return cache.get(file)
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const script = file.endsWith('.vue')
    ? compileScript(parse(source).descriptor, { id: file }).content
    : source
  const exports = {}
  vm.runInNewContext(ts.transpileModule(script, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, {
    exports,
    require(id) {
      if (id === 'vue') return vue
      if (id === 'element-plus') return { ElMessage: { warning: assert.fail, error: assert.fail } }
      if (id.startsWith('@/')) return load(`src/${id.slice(2)}.ts`)
      throw new Error(`Unexpected dependency: ${id}`)
    }
  })
  cache.set(file, exports)
  return exports
}
const { sortUserTableRows } = load('src/views/system/user/utils.ts')
const { useListColumnFilter } = load('src/hooks/web/useListColumnFilter.ts')
const field = load('src/components/TableMultiFilter/MultiFilterField.vue').default
const plain = value => JSON.parse(JSON.stringify(value))
function setupField(type) {
  const props = vue.reactive({ definition: { key: 'value', label: '范围', type }, condition: { key: 'value', operator: 'between' } })
  const state = field.setup(props, { expose() {}, emit(_, next) { props.condition = next } })
  return { props, state }
}

test('date picker emits separate endpoints accepted by actual column filtering, including end of day', async () => {
  const { props, state } = setupField('dateRange')
  state.updateDateRange(['2026-11-03', '2026-11-03'])
  assert.deepEqual(plain(props.condition), { key: 'value', operator: 'between', value: '2026-11-03', valueEnd: '2026-11-03' })
  const filter = useListColumnFilter([props.definition], () => [
    { id: 1, value: '2026-11-02 23:59:59' },
    { id: 2, value: '2026-11-03 00:00:00' },
    { id: 3, value: '2026-11-03 23:59:59' },
    { id: 4, value: '2026-11-04 00:00:00' }
  ], () => undefined)
  filter.state.value.conditions = [props.condition]
  await filter.apply()
  assert.deepEqual(Array.from(filter.rows.value, v => v.id), [2, 3])
  state.updateDateRange(null)
  filter.state.value.conditions = [props.condition]
  await filter.apply()
  assert.equal(filter.active.value, false)
  assert.equal(filter.rows.value.length, 4)
})

test('numeric ranges preserve zero and clear one endpoint without shifting the other', async () => {
  const { props, state } = setupField('numberRange')
  state.updateRangeValue(0, 0)
  state.updateRangeValue(1, 10)
  assert.equal(props.condition.value, 0)
  assert.equal(props.condition.valueEnd, 10)
  const filter = useListColumnFilter([props.definition], () => [{ value: 0 }, { value: 5 }, { value: 11 }], () => undefined)
  filter.state.value.conditions = [props.condition]
  await filter.apply()
  assert.equal(filter.rows.value.length, 2)
  state.updateRangeValue(1, undefined)
  filter.state.value.conditions = [props.condition]
  await assert.rejects(filter.apply(), /完整的筛选范围/)
  assert.equal(filter.rows.value.length, 2)
  state.updateRangeValue(1, 10)
  state.updateRangeValue(0, undefined)
  assert.equal(state.startNumberValue.value, undefined)
  assert.equal(state.endNumberValue.value, 10)
})

test('full-result sorting precedes pagination and preserves every identifier', () => {
  const rows = Array.from({ length: 21 }, (_, n) => ({ id: n + 1, username: `person${21 - n}` }))
  const sorted = sortUserTableRows(rows, 'username', 'ascending')
  assert.deepEqual(Array.from(sorted.slice(0, 10), r => r.username), Array.from({ length: 10 }, (_, n) => `person${n + 1}`))
  assert.deepEqual(Array.from(sorted.slice(20), r => r.username), ['person21'])
  assert.deepEqual(Array.from(sorted, r => r.id).sort((a, b) => a - b), rows.map(r => r.id))
  assert.equal(new Set(sorted.map(r => r.id)).size, 21)
  assert.equal(rows[0].username, 'person21')
  assert.equal(sortUserTableRows(rows, 'username', 'descending')[0].username, 'person21')
})

test('numeric, time and display columns sort consistently with stable ties and empty values last', () => {
  const rows = [{ id: 3, value: null }, { id: 2, value: 10 }, { id: 1, value: 2 }, { id: 4, value: 2 }]
  assert.deepEqual(Array.from(sortUserTableRows(rows, 'value', 'ascending'), r => r.id), [1, 4, 2, 3])
  assert.deepEqual(Array.from(sortUserTableRows(rows, 'value', 'descending'), r => r.id), [2, 1, 4, 3])
  const text = [{ id: 1, value: '2026-11-02' }, { id: 2, value: '2026-11-03' }, { id: 3, value: '' }]
  assert.deepEqual(Array.from(sortUserTableRows(text, 'value', 'descending'), r => r.id), [2, 1, 3])
  assert.deepEqual(plain(sortUserTableRows(rows, 'value', null)), rows)
})
