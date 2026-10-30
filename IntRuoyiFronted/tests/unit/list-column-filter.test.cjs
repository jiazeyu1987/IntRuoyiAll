const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const exportsObject = {}
vm.runInNewContext(
  ts.transpileModule(
    fs.readFileSync(
      require('node:path').resolve(__dirname, '../../src/hooks/web/useListColumnFilter.ts'),
      'utf8'
    ),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }
  ).outputText,
  {
    exports: exportsObject,
    require: (id) => {
      assert.equal(id, 'vue')
      return vue
    }
  }
)
const { buildListColumnFilters, filterListColumns, loadAllListPages, useListColumnFilter } =
  exportsObject
const plain = (value) => JSON.parse(JSON.stringify(value))

test('all data columns, including hidden and fixed columns, are filterable', () => {
  const options = [
    { label: '启用', value: 0 },
    { label: '停用', value: 1 }
  ]
  const fields = buildListColumnFilters(
    [
      { key: 'name', label: '名称' },
      { key: 'parameterCount', label: '参数数量', visible: false, hideable: false },
      { key: 'status', label: '状态' },
      { key: 'createTime', label: '创建时间' },
      { key: 'operation', label: '操作' },
      { key: 'selection', label: '选择' }
    ],
    [{ key: 'status', label: '状态', type: 'select', options }]
  )
  assert.deepEqual(plain(fields.map((field) => field.key)), [
    'name',
    'parameterCount',
    'status',
    'createTime'
  ])
  assert.equal(fields[2].options, options)
  assert.equal(fields[3].type, 'dateRange')
})

test('multiple columns combine with AND, including zero and false', () => {
  const fields = ['name', 'count', 'enabled'].map((key) => ({ key, label: key, type: 'text' }))
  const rows = [
    { id: 1, name: '车间 A', count: 0, enabled: false },
    { id: 2, name: '车间 B', count: 1, enabled: false }
  ]
  const actual = filterListColumns(
    rows,
    [
      { key: 'name', operator: 'contains', value: '车间' },
      { key: 'count', operator: 'eq', value: 0 },
      { key: 'enabled', operator: 'eq', value: false }
    ],
    fields
  )
  assert.deepEqual(plain(actual.map((row) => row.id)), [1])
})

test('nested fields and display getters match the list values', () => {
  const rows = [
    { file: { name: '受控文件' }, status: 1 },
    { file: { name: '其他' }, status: 0 }
  ]
  const fields = [
    { key: 'file.name', label: '文件', type: 'text' },
    {
      key: 'status',
      label: '状态',
      type: 'text',
      getValue: (row) => (row.status ? '已生效' : '草稿')
    }
  ]
  assert.equal(
    filterListColumns(
      rows,
      [
        { key: 'file.name', operator: 'contains', value: '受控' },
        { key: 'status', operator: 'contains', value: '生效' }
      ],
      fields
    ).length,
    1
  )
})

test('column aliases use their display getter and multi-value columns match any selected value', () => {
  const fields = buildListColumnFilters(
    [
      { key: 'product', label: '产品' },
      { key: 'types', label: '检测种类' }
    ],
    [
      {
        key: 'productCode',
        label: '产品',
        type: 'text',
        getValue: (row) => `${row.code} ${row.name}`
      }
    ]
  )
  const rows = [
    { code: 'P1', name: '样品', types: ['A', 'B'] },
    { code: 'P2', name: '其他', types: ['C'] }
  ]
  assert.equal(
    filterListColumns(
      rows,
      [
        { key: 'product', operator: 'contains', value: '样品' },
        { key: 'types', operator: 'eq', value: 'B' }
      ],
      fields
    ).length,
    1
  )
})

test('date ranges include the entire last day and numeric ranges compare numbers', () => {
  const rows = [
    { at: '2026-10-30 23:59:59', count: 10 },
    { at: '2026-10-31 00:00:00', count: 10 },
    { at: null, count: 3 }
  ]
  const result = filterListColumns(
    rows,
    [
      { key: 'at', operator: 'between', value: '2026-10-30', valueEnd: '2026-10-30' },
      { key: 'count', operator: 'between', value: 9, valueEnd: 11 }
    ],
    [
      { key: 'at', label: '时间', type: 'dateRange' },
      { key: 'count', label: '数量', type: 'numberRange' }
    ]
  )
  assert.equal(result.length, 1)
  assert.equal(result[0].at, rows[0].at)
})

test('server pages are collected before filtering, with no current-page truncation', async () => {
  const calls = []
  const rows = await loadAllListPages(async (pagination) => {
    calls.push(pagination)
    return {
      list: pagination.pageNo === 1 ? [{ name: 'A' }, { name: 'B' }] : [{ name: '目标' }],
      total: 3
    }
  })
  const matches = filterListColumns(
    rows,
    [{ key: 'name', operator: 'eq', value: '目标' }],
    [{ key: 'name', label: '名称', type: 'text' }]
  )
  assert.equal(matches.length, 1)
  assert.deepEqual(plain(calls.map((call) => call.pageNo)), [1, 2])
})

test('tree filtering keeps matching descendants and their ancestor path', () => {
  const rows = [{ name: '根节点', children: [{ name: '目标' }, { name: '其他' }] }]
  const result = filterListColumns(
    rows,
    [{ key: 'name', operator: 'eq', value: '目标' }],
    [{ key: 'name', label: '名称', type: 'text' }]
  )
  assert.equal(result.length, 1)
  assert.deepEqual(plain(result[0].children), [{ name: '目标' }])
  assert.equal(rows[0].children.length, 2)
})

test('incomplete pages and changing totals fail explicitly', async () => {
  await assert.rejects(
    loadAllListPages(async () => ({ list: [], total: 1 })),
    /数量与总数不一致/
  )
  await assert.rejects(
    loadAllListPages(async ({ pageNo }) => ({
      list: [{ id: pageNo }],
      total: pageNo === 1 ? 3 : 4
    })),
    /已变更/
  )
})

test('draft edits do not apply; failure preserves results; reset clears conditions', async () => {
  let fail = false
  const filter = useListColumnFilter(
    [{ key: 'name', label: '名称', type: 'text' }],
    () => undefined,
    () => async () => {
      if (fail) throw new Error('读取失败')
      return [{ name: 'A' }, { name: 'B' }]
    }
  )
  filter.state.value.conditions = [{ key: 'name', operator: 'eq', value: 'A' }]
  assert.equal(filter.active.value, false)
  await filter.apply()
  filter.state.value.conditions = [{ key: 'name', operator: 'eq', value: 'B' }]
  assert.equal(filter.rows.value[0].name, 'A')
  fail = true
  await assert.rejects(filter.apply(), /读取失败/)
  assert.equal(filter.rows.value[0].name, 'A')
  filter.reset()
  assert.equal(filter.active.value, false)
  assert.equal(filter.state.value.conditions.length, 0)
})

test('reset cancels a pending query result', async () => {
  let finish
  const filter = useListColumnFilter(
    [{ key: 'name', label: '名称', type: 'text' }],
    () => undefined,
    () => () =>
      new Promise((resolve) => {
        finish = resolve
      })
  )
  filter.state.value.conditions = [{ key: 'name', operator: 'eq', value: 'A' }]
  const pending = filter.apply()
  filter.reset()
  finish([{ name: 'A' }])
  await pending
  assert.equal(filter.active.value, false)
})
