const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const test = require('node:test'), assert = require('node:assert/strict')
const ts = require('typescript'), vue = require('vue'), { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
function evaluate(code, imports, globals = {}) {
  const context = { exports: {}, Error, ...globals, require: name => {
    if (name === 'vue') return vue
    if (Object.hasOwn(imports, name)) return imports[name]
    throw Error('Unexpected dependency: ' + name)
  } }
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const tree = evaluate(fs.readFileSync(path.join(root, 'src/utils/tree.ts'), 'utf8'), {})
const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue'), 'utf8')
const renderer = vue.createRenderer({ createElement: () => ({ children: [] }), createText: text => ({ text }), createComment: () => ({}),
  insert: (child, parent) => parent.children.push(child), remove() {}, patchProp() {}, setText() {}, setElementText() {}, parentNode() {}, nextSibling() {} })
const row = (id, parentId, name, levelNo) => ({ id, parentId, name, code: 'FORMAL_' + id, levelNo, active: true, sort: id })
async function fixture() {
  let rows = [row(1, 0, '技术文档', 1), row(5, 0, '另一个根', 1), row(2, 1, '设计阶段', 2), row(3, 2, '目标类型', 3), row(4, 2, '同胞类型', 3)]
  const creates = [], updates = []
  const hooks = {
    useUserTableColumns: () => ({ columns: vue.ref([]), saving: vue.ref(false), isColumnVisible: () => true, getColumnWidthString() {}, getColumnMinWidthString() {}, handleHeaderDragend() {}, saveConfig() {} }),
    useTableQuickFilter: () => ({ state: {}, selectedDefinition: vue.ref(), operatorOptions: vue.ref([]), updateState() {}, applyQuickFilter() {} })
  }
  const api = { getFileTypeTaxonomyList: async () => rows, deleteFileTypeTaxonomy: async () => true,
    createFileTypeTaxonomy: async data => { creates.push(JSON.parse(JSON.stringify(data))); rows = [...rows, row(6, data.parentId || 0, data.name, 3)]; return 6 },
    updateFileTypeTaxonomy: async (id, data) => { updates.push({ id, data: JSON.parse(JSON.stringify(data)) }); return true } }
  const component = evaluate(compileScript(parse(source).descriptor, { id: 'word23-taxonomy' }).content, {
    '@/components/UnifiedListTemplate/index.vue': {}, './FileTypeCategoryMappingDialog.vue': {},
    '../../categories/components/CategoryMatrixDialog.vue': {},
    '@/api/dcc/controlledFile/fileCategories': { getCategoryReviewMatrixRows: async () => [{ categoryId: 77, name: '正式矩阵', routeVersionNo: 1, effectiveTime: Date.now() - 1000 }] },
    '@/api/dcc/controlledFile/directories': { getDirectoryTree: async () => [{ id: 99, name: '测试存储目录', active: true }] },
    '@/hooks/web/useTableQuickFilter': hooks, '@/hooks/web/useUserTableColumns': hooks, '@/utils/tree': tree,
    '@/api/dcc/controlledFile/fileTypeTaxonomies': api
  }, { ref: vue.ref, reactive: vue.reactive, computed: vue.computed, watch: vue.watch, onMounted: vue.onMounted,
    useMessage: () => ({ warning() {}, success() {}, delConfirm: async () => {} }) }).default
  component.render = () => null
  const app = renderer.createApp(component)
  app.mount({ children: [] })
  const state = app._instance.setupState
  state.formRef = { resetFields() {}, validate: async () => true }
  await new Promise(resolve => setImmediate(resolve))
  return { app, state, creates, updates }
}
const numberedIds = state => Array.from(state.taxonomyRowNumberMap, ([id, number]) => [id, number])
test('visible tree rows have consecutive list numbers through collapse, expand, filter and root pagination', async () => {
  const f = await fixture(), s = f.state
  try {
    assert.deepEqual(numberedIds(s), [[1, 1], [2, 2], [3, 3], [4, 4], [5, 5]])
    s.handleTaxonomyExpandChange({ id: 2 }, false)
    assert.deepEqual(numberedIds(s), [[1, 1], [2, 2], [5, 3]])
    assert.ok(!s.taxonomyExpandedRowKeys.includes(2))
    s.handleTaxonomyExpandChange({ id: 1 }, false)
    assert.deepEqual(numberedIds(s), [[1, 1], [5, 2]])
    s.handleTaxonomyExpandChange({ id: 1 }, true)
    s.handleTaxonomyExpandChange({ id: 2 }, true)
    s.query.keyword = '目标类型'
    assert.deepEqual(numberedIds(s), [[1, 1], [2, 2], [3, 3]])
    s.query.keyword = ''
    s.query.pageSize = 1
    s.query.pageNo = 2
    assert.deepEqual(numberedIds(s), [[5, 1]])
    assert.deepEqual(s.rows.map(row => row.levelNo), [1, 1, 2, 3, 3], 'real depth remains independent from list numbers')
  } finally { f.app.unmount() }
})
test('creating a child sends no manual code and the refreshed inserted row gets a consecutive number', async () => {
  const f = await fixture(), s = f.state
  try {
    await s.openForm('create', undefined, s.rows.find(row => row.id === 2))
    assert.equal(s.formData.parentId, 2)
    assert.equal(s.formData.code, '')
    assert.equal(s.formRules.code, undefined)
    s.formData.name = '新增类型'
    s.storageDirectoryId = 99
    s.matrixSourceCategoryId = 77
    await s.submitForm()
    assert.equal(f.creates.length, 1)
    assert.equal(f.creates[0].approvalSourceMode, 'PUBLIC_MATRIX')
    assert.equal(f.creates[0].matrixSourceCategoryId, 77)
    assert.equal(f.creates[0].directoryBinding.directoryId, 99)
    assert.equal(Object.hasOwn(f.creates[0], 'code'), false)
    assert.equal(s.formVisible, false)
    assert.deepEqual(numberedIds(s), [[1, 1], [2, 2], [3, 3], [4, 4], [6, 5], [5, 6]])
    assert.equal(s.rows.find(row => row.id === 6).parentId, 2)
  } finally { f.app.unmount() }
})
test('root creation also omits code and editing retains the formal existing code', async () => {
  const f = await fixture(), s = f.state
  try {
    s.openForm('create')
    s.formData.name = '新增根'
    await s.submitForm()
    assert.equal(f.creates[0].parentId, null)
    assert.equal(Object.hasOwn(f.creates[0], 'code'), false)
    s.openForm('update', s.rows.find(row => row.id === 3))
    s.formData.name = '改名'
    await s.submitForm()
    assert.equal(f.updates[0].data.code, 'FORMAL_3')
  } finally { f.app.unmount() }
})

test('new applicable types cannot submit until a matrix is selected', async () => {
  const f = await fixture(), s = f.state
  try {
    await s.openForm('create', undefined, s.rows.find(row => row.id === 2))
    s.formData.name = '无矩阵类型'
    s.storageDirectoryId = 99
    await s.submitForm()
    assert.equal(f.creates.length, 0)
    assert.equal(s.formVisible, true)
    s.matrixMode = 'custom'
    await s.submitForm()
    assert.equal(f.creates.length, 0, 'ordinary types cannot bypass matrix selection using custom mode')
    s.formData.name = '其他'; s.matrixMode = 'custom'
    await s.submitForm()
    assert.equal(f.creates[0].approvalSourceMode, 'APPLICATION_CUSTOM')
    assert.equal(Object.hasOwn(f.creates[0], 'approvalMatrix'), false)
    assert.equal(Object.hasOwn(f.creates[0], 'matrixSourceCategoryId'), false)
  } finally { f.app.unmount() }
})
test('path-only nodes remain explicitly inapplicable and draft rules save with the type', async () => {
  const f = await fixture(), s = f.state
  try {
    await s.openForm('create', undefined, s.rows.find(row => row.id === 2))
    s.formData.name = '四级路径'; s.pathNodeOnly = true
    await s.submitForm()
    assert.equal(f.creates[0].pathNodeOnly, true)
    assert.equal(Object.hasOwn(f.creates[0], 'approvalSourceMode'), false)
    await s.openForm('create', undefined, s.rows.find(row => row.id === 2))
    s.formData.name = '新矩阵类型'; s.matrixMode = 'new'
    s.storageDirectoryId = 99
    s.matrixDraft = { effectiveTime: '2026-11-06 10:00:00', rules: [{ stageType: 'SIGNOFF', active: true }] }
    await s.submitForm()
    assert.equal(f.creates[1].approvalMatrix.rules.length, 1)
    assert.equal(f.creates[1].approvalSourceMode, 'PUBLIC_MATRIX')
  } finally { f.app.unmount() }
})
