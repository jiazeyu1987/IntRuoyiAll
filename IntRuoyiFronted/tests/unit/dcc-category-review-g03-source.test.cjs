const { test } = require('node:test'), assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const file = path.resolve(__dirname, '../../src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue')
const plain = value => JSON.parse(JSON.stringify(value))
const saved = (id = 8, mode = 'APPLICATION_CUSTOM') => ({ id, code: 'CATEGORY-' + id, name: '已保存类别' + id,
  approvalSourceMode: mode, lifecycleStage: 'OUTPUT', active: true, sort: 17,
  fileTypeTaxonomyId: 6, directoryId: 7, distributionRequired: false, trainingRequired: false })
const review = id => ({ categoryId: id, code: 'OLD-' + id, name: '旧矩阵摘要' + id, lifecycleStage: 'OUTPUT',
  active: true, configured: true, rules: [] })
function pending() { let resolve, reject; const promise = new Promise((y, n) => { resolve = y; reject = n }); return { promise, resolve, reject } }
function traverse(node, match, found = []) { if (match(node)) found.push(node); for (const child of node.children || []) traverse(child, match, found); return found }
const text = node => [node.text || '', ...(node.children || []).map(text)].join('')
const renderer = vue.createRenderer({
  createElement: type => ({ type, props: {}, children: [], parent: null }), createText: text => ({ text, parent: null }), createComment: () => ({ text: '', parent: null }),
  insert(node, parent, anchor) { if (node.parent) this.remove(node); const index = anchor ? parent.children.indexOf(anchor) : -1; parent.children.splice(index < 0 ? parent.children.length : index, 0, node); node.parent = parent },
  remove(node) { if (node.parent) { const index = node.parent.children.indexOf(node); if (index >= 0) node.parent.children.splice(index, 1); node.parent = null } },
  patchProp: (n, k, _old, value) => { n.props[k] = value }, setText: (n, value) => { n.text = value }, setElementText: (n, value) => { n.text = value; n.children = [] },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null
})
async function mount(readCategory = async () => [saved(), saved(9, 'PUBLIC_MATRIX')]) {
  const opens = [], reads = [], errors = [], host = { children: [] }, props = vue.reactive({ active: true, categoryRevision: 0 })
  const MatrixDialog = { setup: (_props, { expose }) => { expose({ open: (...args) => opens.push(plain(args)) }); return () => vue.h('matrix-dialog') } }
  const descriptor = parse(fs.readFileSync(file, 'utf8'), { filename: file }).descriptor
  const compiled = compileScript(descriptor, { id: 'g03-matrix-source', inlineTemplate: true })
  const context = { exports: {}, Error, ref: vue.ref, reactive: vue.reactive, computed: vue.computed, watch: vue.watch, onMounted: vue.onMounted,
    useMessage: () => ({ error: error => errors.push(error), success: () => {}, delConfirm: async () => {} }),
    require: name => {
      if (name === 'vue') return vue
      if (name.endsWith('/fileCategories')) return {
        getCategoryReviewMatrixRows: async () => [review(8), review(9)],
        getFileCategoryList: async () => { reads.push('saved-categories'); return readCategory() },
        requireCategoryApprovalSourceMode: value => { if (!['PUBLIC_MATRIX', 'APPLICATION_CUSTOM'].includes(value)) throw Error('文件类别审批来源未配置或无效'); return value },
        deleteCategoryApprovalMatrix: () => { throw Error('No matrix write authorized in this test') }
      }
      if (name.endsWith('/UnifiedListTemplate/index.vue')) return { default: { setup: (_p, { slots }) => () => vue.h('list', {}, [slots.actions?.(), slots.table?.({ sortColumnAttrs: () => ({}), handleSortChange: () => {} })]) } }
      if (name.endsWith('/useUserTableColumns')) return { useUserTableColumns: () => ({ columns: vue.ref([]), saving: vue.ref(false), isColumnVisible: () => true, getColumnWidthString: () => '', getColumnMinWidthString: () => '', saveColumnConfig: () => {}, handleHeaderDragend: () => {} }) }
      if (name.endsWith('/useTableQuickFilter')) return { useTableQuickFilter: () => ({ state: {}, selectedDefinition: vue.ref(), operatorOptions: vue.ref([]), updateState: () => {}, applyQuickFilter: () => {} }) }
      if (name === './CategoryMatrixDialog.vue') return { default: MatrixDialog }
      if (name === './CategoryReviewMatrixUserLookupDialog.vue') return { default: { render: () => null } }
      throw Error('Unexpected production dependency ' + name)
    }
  }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  const app = renderer.createApp({ render: () => vue.h(context.exports.default, props) })
  app.component('el-table', { props: ['data'], setup: (p, { slots }) => { vue.provide('rows', vue.computed(() => p.data)); return () => vue.h('table', { rowCount: p.data?.length }, slots.default?.()) } })
  app.component('el-table-column', { setup: (_p, { slots }) => { const rows = vue.inject('rows'); return () => vue.h('column', {}, rows.value?.map(row => vue.h('row', { categoryId: row.categoryId }, slots.default?.({ row })))) } })
  app.component('el-button', { setup: (_p, { slots, attrs }) => () => vue.h('button', attrs, slots.default?.()) })
  app.component('el-tooltip', { setup: (_p, { slots }) => () => vue.h('tip', {}, slots.default?.()) })
  app.component('el-alert', { props: ['title'], setup: p => () => vue.h('alert', {}, p.title) })
  app.component('Icon', { render: () => null })
  app.directive('loading', {}); app.directive('hasPermi', {})
  app.mount(host); await new Promise(resolve => setImmediate(resolve)); await vue.nextTick()
  const click = (id, label = '编辑') => {
    const matches = traverse(host, n => n.type === 'row' && n.props.categoryId === id)
      .flatMap(row => traverse(row, n => n.type === 'button' && text(n).trim() === label))
    assert.equal(matches.length, 1, `${id}/${label}; visible=${text(host)}; rowCounts=${traverse(host, n => n.type === 'table').map(n => n.props.rowCount)}; buttons=${traverse(host, n => n.type === 'button').map(text).join('|')}`)
    return matches[0].props.onClick()
  }
  return { app, host, props, opens, reads, errors, click, text: () => text(host) }
}

test('real review-table edit and preview load the exact saved category mode and complete metadata', async () => {
  const h = await mount()
  try {
    await h.click(8); await vue.nextTick()
    assert.equal(h.reads.length, 1)
    assert.deepEqual(h.opens[0], [saved(), 'edit'])
    await h.click(9, '预览'); await vue.nextTick()
    assert.deepEqual(h.opens[1], [saved(9, 'PUBLIC_MATRIX'), 'preview'])
  } finally { h.app.unmount() }
})

test('missing, duplicated or unknown saved source blocks opening with visible concrete feedback', async () => {
  for (const rows of [[], [saved(), saved()], [saved(8, undefined)]]) {
    if (rows.length === 1) rows[0].approvalSourceMode = undefined
    const h = await mount(async () => rows)
    try { await h.click(8); await vue.nextTick(); assert.equal(h.opens.length, 0); assert.match(h.text(), /正式.*类别.*缺失|重复|审批来源.*未配置/) }
    finally { h.app.unmount() }
  }
})

test('actual category transport error stays visible and never opens a default public category', async () => {
  const h = await mount(async () => { throw new Error('正式类别目录读取失败') })
  try { await h.click(8); await vue.nextTick(); assert.equal(h.opens.length, 0); assert.match(h.text(), /正式类别目录读取失败/) }
  finally { h.app.unmount() }
})

test('a later click wins and inactive, revised or unmounted context cannot open a late dialog', async () => {
  const first = pending(), next = pending(); let count = 0
  const h = await mount(() => ++count === 1 ? first.promise : next.promise)
  try {
    const old = h.click(8), current = h.click(9); next.resolve([saved(), saved(9, 'PUBLIC_MATRIX')]); await current
    first.resolve([saved()]); await old; assert.deepEqual(h.opens, [[saved(9, 'PUBLIC_MATRIX'), 'edit']])
  } finally { h.app.unmount() }
  for (const action of ['inactive', 'revision', 'unmount']) {
    const gate = pending(), h = await mount(() => gate.promise)
    try {
      const work = h.click(8)
      if (action === 'inactive') h.props.active = false
      if (action === 'revision') h.props.categoryRevision++
      if (action === 'unmount') h.app.unmount()
      await vue.nextTick(); gate.resolve([saved()]); await work; assert.deepEqual(h.opens, [])
    } finally { if (action !== 'unmount') h.app.unmount() }
  }
})

test('late old category error cannot overwrite the new selection or reopen a successful dialog', async () => {
  const old = pending(); let calls = 0
  const h = await mount(() => ++calls === 1 ? old.promise : Promise.resolve([saved(), saved(9, 'PUBLIC_MATRIX')]))
  try {
    const first = h.click(8); await h.click(9); old.reject(new Error('旧类别目录错误')); await first; await vue.nextTick()
    assert.deepEqual(h.opens, [[saved(9, 'PUBLIC_MATRIX'), 'edit']])
    assert.doesNotMatch(h.text(), /旧类别目录错误/)
  } finally { h.app.unmount() }
})
