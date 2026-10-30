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
const stub = (tag) => ({
  inheritAttrs: false,
  setup:
    (_, { attrs, slots }) =>
    () =>
      vue.h(tag, attrs, slots.default?.())
})

function load(relativePath) {
  if (cache.has(relativePath)) return cache.get(relativePath)
  const source = fs.readFileSync(path.join(root, relativePath), 'utf8')
  const script = relativePath.endsWith('.vue')
    ? compileScript(parse(source).descriptor, { id: relativePath, inlineTemplate: true }).content
    : source
  const exports = {}
  vm.runInNewContext(
    ts.transpileModule(script, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText,
    {
      exports,
      require(id) {
        if (id === 'vue') return vue
        if (id === 'element-plus')
          return { ElMessage: { error: assert.fail, warning: assert.fail } }
        if (id.includes('MultiFilterField')) return { default: stub('filter-field') }
        if (id.includes('UserTableColumnSettings')) return { default: stub('column-settings') }
        if (id.startsWith('@/')) return load(`src/${id.slice(2)}${path.extname(id) ? '' : '.ts'}`)
        throw new Error(`Unexpected dependency: ${id}`)
      }
    },
    { filename: relativePath }
  )
  cache.set(relativePath, exports)
  return exports
}

function mount(component) {
  const makeNode = (type, text = '') => ({ type, text, props: {}, children: [], parent: null })
  const renderer = vue.createRenderer({
    createElement: makeNode,
    createText: (text) => makeNode('#text', text),
    createComment: (text) => makeNode('#comment', text),
    setText: (node, text) => {
      node.text = text
    },
    setElementText: (node, text) => {
      node.text = text
      node.children = []
    },
    patchProp: (node, key, _, value) => {
      node.props[key] = value
    },
    parentNode: (node) => node.parent,
    nextSibling: (node) => node.parent?.children[node.parent.children.indexOf(node) + 1],
    insert(node, parent, anchor = null) {
      if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
      const index = anchor ? parent.children.indexOf(anchor) : parent.children.length
      parent.children.splice(index, 0, node)
      node.parent = parent
    },
    remove(node) {
      node.parent.children.splice(node.parent.children.indexOf(node), 1)
      node.parent = null
    }
  })
  const app = renderer.createApp(component)
  for (const [name, tag] of Object.entries({
    ElButton: 'button',
    ElForm: 'form',
    ElFormItem: 'form-item',
    ElSelect: 'select',
    ElOption: 'option',
    ElTabs: 'tabs',
    ElTabPane: 'tab',
    ElTag: 'tag',
    Icon: 'icon',
    Pagination: 'pagination',
    ElAlert: 'alert'
  }))
    app.component(name, stub(tag))
  const container = makeNode('root')
  app.mount(container)
  return { container, stop: () => app.unmount() }
}

function findAll(node, predicate) {
  return [
    ...(predicate(node) ? [node] : []),
    ...node.children.flatMap((child) => findAll(child, predicate))
  ]
}
const textOf = (node) => node.text + node.children.map(textOf).join('')
const actionsOf = (container) =>
  findAll(container, (node) => node.props.class === 'list-filter-actions')[0]
const buttonsOf = (container) =>
  actionsOf(container)?.children.filter((node) => node.type === 'button') || []
const flush = async () => {
  await vue.nextTick()
  await vue.nextTick()
  await vue.nextTick()
}

test('condition filter actions appear only after adding a condition and disappear after reset', async () => {
  const TableMultiFilter = load('src/components/TableMultiFilter/index.vue').default
  const state = vue.reactive({ conditions: [], appliedConditions: [] })
  const events = []
  const definitions = [{ key: 'code', label: '编号', type: 'text' }]
  const ctx = mount({
    setup: () => () =>
      vue.h(TableMultiFilter, {
        tableKey: 'test.actions',
        filterDefinitions: definitions,
        state,
        'onUpdate:state': (next) => {
          Object.assign(state, next)
          events.push('update')
        },
        onQuery: () => events.push('query'),
        onReset: () => events.push('reset')
      })
  })
  try {
    assert.equal(buttonsOf(ctx.container).length, 0)
    state.conditions = [{ id: 'code', key: 'code', operator: 'contains' }]
    state.activeConditionId = 'code'
    await flush()
    assert.deepEqual(
      buttonsOf(ctx.container)
        .map(textOf)
        .map((text) => text.trim()),
      ['查询', '重置']
    )
    buttonsOf(ctx.container)[0].props.onClick()
    assert.equal(events.at(-1), 'query')
    state.conditions[0].value = 'GY'
    state.appliedConditions = [...state.conditions]
    state.activeConditionId = 'code'
    await flush()
    const editor = findAll(
      ctx.container,
      (node) => node.props.class === 'table-multi-filter__condition-row'
    )[0]
    const actions = actionsOf(ctx.container)
    assert.equal(actions.parent, editor.parent)
    assert.ok(actions.parent.children.indexOf(actions) > editor.parent.children.indexOf(editor))
    buttonsOf(ctx.container)[1].props.onClick()
    await flush()
    assert.equal(state.conditions.length, 0)
    assert.equal(state.activeConditionId, undefined)
    assert.deepEqual(events.slice(-2), ['update', 'reset'])
    assert.equal(buttonsOf(ctx.container).length, 0)
  } finally {
    ctx.stop()
  }
})

for (const [mode, action] of ['quick', 'multi'].flatMap((mode) =>
  ['reset', 'remove'].map((action) => [mode, action])
))
  test(`${mode} template ${action} clears applied parameters, returns to page one and hides empty filter actions`, async () => {
    const Template = load('src/components/UnifiedListTemplate/index.vue').default
    const hook =
      mode === 'quick'
        ? load('src/hooks/web/useTableQuickFilter.ts').useTableQuickFilter
        : load('src/hooks/web/useTableMultiFilter.ts').useTableMultiFilter
    const query = vue.reactive({ pageNo: 4, pageSize: 20, projectId: 19 })
    const snapshots = []
    const definitions = [{ key: 'code', label: '编号', type: 'text', queryParamKey: 'code' }]
    const filter = hook('test.actions', definitions, query, () => {
      snapshots.push({ ...query })
    })
    const ctx = mount({
      setup: () => () =>
        vue.h(Template, {
          tableKey: 'test.actions',
          queryModel: query,
          ...(mode === 'quick'
            ? {
                filterDefinitions: definitions,
                quickFilterState: filter.state,
                'onUpdate:quickFilterState': filter.updateState,
                onQuickFilterQuery: filter.applyQuickFilter
              }
            : {
                showMultiFilter: true,
                multiFilterDefinitions: definitions,
                multiFilterState: filter.state,
                'onUpdate:multiFilterState': filter.updateState,
                onMultiFilterQuery: filter.applyMultiFilter,
                onMultiFilterReset: filter.resetMultiFilter
              })
        })
    })
    try {
      assert.equal(buttonsOf(ctx.container).length, 0)
      filter.updateState({
        conditions: [{ id: 'code', key: 'code', operator: 'contains', value: 'GY' }],
        activeConditionId: 'code'
      })
      await flush()
      buttonsOf(ctx.container)[0].props.onClick()
      await flush()
      assert.equal(query.code, 'GY')
      assert.equal(query.pageNo, 1)
      assert.equal(filter.state.appliedConditions.length, 1)
      query.pageNo = 3
      if (action === 'reset') buttonsOf(ctx.container)[1].props.onClick()
      else
        findAll(
          ctx.container,
          (node) => node.props['aria-label'] === '删除当前筛选条件'
        )[0].props.onClick()
      await flush()
      assert.equal(query.code, undefined)
      assert.equal(query.quickFilter, undefined)
      assert.equal(query.multiFilters, undefined)
      assert.equal(query.projectId, 19)
      assert.equal(query.pageNo, 1)
      assert.equal(filter.state.conditions.length, 0)
      assert.equal(filter.state.appliedConditions.length, 0)
      assert.equal(snapshots.length, 2)
      assert.equal(buttonsOf(ctx.container).length, 0)
    } finally {
      ctx.stop()
    }
  })

test('custom filters use the same actions, preserve loading/selector and apply the first page before querying', async () => {
  const Template = load('src/components/UnifiedListTemplate/index.vue').default
  const events = []
  const page = vue.ref(4)
  const ctx = mount({
    setup: () => () =>
      vue.h(
        Template,
        {
          tableKey: 'test.custom',
          page: page.value,
          queryLoading: true,
          queryButtonAttrs: { 'data-test-query': '' },
          'onUpdate:page': (next) => {
            page.value = next
          },
          onQuery: () => events.push(['query', page.value]),
          onReset: () => events.push(['reset'])
        },
        { filters: () => vue.h('input'), actions: () => vue.h('business-action') }
      )
  })
  try {
    assert.equal(buttonsOf(ctx.container)[0].props.loading, true)
    assert.equal(buttonsOf(ctx.container)[0].props['data-test-query'], '')
    buttonsOf(ctx.container)[0].props.onClick()
    await flush()
    buttonsOf(ctx.container)[1].props.onClick()
    assert.deepEqual(events, [['query', 1], ['reset']])
    assert.equal(findAll(ctx.container, (node) => node.type === 'business-action').length, 1)
  } finally {
    ctx.stop()
  }
})

test('supplemental filters precede the only action row and reset through their page handler exactly once', async () => {
  const Template = load('src/components/UnifiedListTemplate/index.vue').default
  const state = vue.reactive({
    conditions: [{ id: 'code', key: 'code', operator: 'contains', value: 'GY' }],
    appliedConditions: []
  })
  const range = vue.ref('2026-10-30')
  let resets = 0
  const ctx = mount({
    setup: () => () =>
      vue.h(
        Template,
        {
          tableKey: 'test.extra',
          filterDefinitions: [{ key: 'code', label: '编号', type: 'text' }],
          quickFilterState: state,
          'onUpdate:quickFilterState': (next) => Object.assign(state, next),
          onQuickFilterQuery: () => assert.fail('reset handler must own the single reload'),
          resetFilters: () => {
            resets++
            range.value = ''
          }
        },
        { 'extra-filters': () => vue.h('date-filter', { value: range.value }) }
      )
  })
  try {
    assert.equal(
      findAll(ctx.container, (node) => node.props.class === 'list-filter-actions').length,
      1
    )
    const actions = actionsOf(ctx.container)
    const extras = findAll(
      ctx.container,
      (node) => node.props.class === 'unified-list-template__extra-filters'
    )[0]
    assert.equal(actions.parent, extras.parent)
    assert.ok(actions.parent.children.indexOf(actions) > extras.parent.children.indexOf(extras))
    buttonsOf(ctx.container)[1].props.onClick()
    await flush()
    assert.equal(resets, 1)
    assert.equal(state.conditions.length, 0)
    assert.equal(range.value, '')
  } finally {
    ctx.stop()
  }
})

test('column filtering applies to the complete source, pages results and refreshes after a list change', async () => {
  const Template = load('src/components/UnifiedListTemplate/index.vue').default
  const originalRows = vue.ref([{ id: 99, count: 1 }])
  const page = vue.ref(3)
  let data = [
    { id: 1, count: 0 },
    { id: 2, count: 0 },
    { id: 3, count: 1 }
  ]
  let calls = 0
  let resets = 0
  const ctx = mount({
    setup: () => () =>
      vue.h(
        Template,
        {
          tableKey: 'test.all-columns',
          columns: [{ key: 'count', label: '参数数量', visible: false }],
          listRows: originalRows.value,
          loadFilterRows: async () => {
            calls++
            return data
          },
          total: 50,
          page: page.value,
          limit: 1,
          'onUpdate:page': (value) => {
            page.value = value
          },
          onFilteredRows: (value) => {
            originalRows.value = value
          },
          onColumnFilterReset: () => {
            resets++
            originalRows.value = [{ id: 99, count: 1 }]
          }
        },
        {
          table: ({ resolveRows }) =>
            vue.h(
              'result',
              {},
              resolveRows(originalRows.value).map((row) => vue.h('record', { id: row.id }))
            )
        }
      )
  })
  try {
    assert.equal(buttonsOf(ctx.container).length, 0)
    findAll(ctx.container, (node) => node.props['aria-label'] === '新增筛选条件')[0].props.onClick()
    await flush()
    const field = findAll(ctx.container, (node) => node.type === 'filter-field')[0]
    field.props.onUpdate({ ...field.props.condition, operator: 'eq', value: 0 })
    await flush()
    buttonsOf(ctx.container)[0].props.onClick()
    await flush()
    await flush()
    assert.equal(calls, 1)
    assert.equal(page.value, 1)
    assert.deepEqual(
      findAll(ctx.container, (node) => node.type === 'record').map((node) => node.props.id),
      [1]
    )
    let pager = findAll(ctx.container, (node) => node.type === 'pagination')[0]
    assert.equal(pager.props.total, 2)
    pager.props['onUpdate:page'](2)
    await flush()
    assert.deepEqual(
      findAll(ctx.container, (node) => node.type === 'record').map((node) => node.props.id),
      [2]
    )
    assert.equal(calls, 1)
    data = [{ id: 4, count: 0 }]
    originalRows.value = [{ id: 100, count: 1 }]
    await flush()
    await flush()
    assert.equal(calls, 2)
    assert.deepEqual(
      findAll(ctx.container, (node) => node.type === 'record').map((node) => node.props.id),
      [4]
    )
    buttonsOf(ctx.container)[1].props.onClick()
    await flush()
    assert.equal(resets, 1)
    assert.equal(page.value, 1)
    assert.equal(buttonsOf(ctx.container).length, 0)
    assert.deepEqual(
      findAll(ctx.container, (node) => node.type === 'record').map((node) => node.props.id),
      [99]
    )
  } finally {
    ctx.stop()
  }
})

test('every standard list avoids duplicate actions and custom filters wire query/reset', () => {
  function walk(dir) {
    return fs
      .readdirSync(dir, { withFileTypes: true })
      .flatMap((entry) =>
        entry.isDirectory()
          ? walk(path.join(dir, entry.name))
          : entry.name.endsWith('.vue')
            ? [path.join(dir, entry.name)]
            : []
      )
  }
  let checked = 0
  for (const file of walk(path.join(root, 'src'))) {
    const source = fs.readFileSync(file, 'utf8')
    assert.doesNotMatch(source, /:show-filter-reset="false"/, file)
    if (!source.includes('<UnifiedListTemplate')) continue
    const ast = parse(source).descriptor.template.ast
    function visit(node) {
      if (node.tag === 'UnifiedListTemplate') {
        const bound = (name) => node.props.some(prop => prop.name === 'bind' && prop.arg?.content === name)
        assert.ok(bound('columns'), `${file} must provide its own columns`)
        assert.ok(['rows', 'filter-rows', 'load-filter-rows'].some(bound), `${file} must provide complete filter data`)
        assert.ok(!node.props.some(prop => prop.arg?.content === 'show-query-form' && prop.exp?.content === 'false'), `${file} must expose its standard filter`)
        if (
          node.children.some(
            (child) =>
              child.tag === 'template' &&
              child.props?.some((prop) => prop.name === 'slot' && prop.arg?.content === 'filters')
          )
        ) {
          for (const event of ['query', 'reset'])
            assert.ok(
              node.props.some((prop) => prop.name === 'on' && prop.arg?.content === event),
              `${file} must wire ${event}`
            )
        }
        const actions = node.children.find(
          (child) =>
            child.tag === 'template' &&
            child.props?.some((prop) => prop.name === 'slot' && prop.arg?.content === 'actions')
        )
        for (const child of actions?.children || [])
          if (child.tag === 'el-button') {
            const label = child.children
              .filter((item) => item.type === 2)
              .map((item) => item.content)
              .join('')
              .trim()
            assert.ok(
              !['搜索', '查询', '重置', '查找项目'].includes(label),
              `${file} has duplicate ${label}`
            )
          }
        checked++
      }
      for (const child of node.children || []) visit(child)
    }
    visit(ast)
  }
  assert.ok(checked > 0)
})
