const { test } = require('node:test'), assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..'), read = file => fs.readFileSync(path.join(root, file), 'utf8')
function moduleFrom(code, requireModule, extras = {}) {
  const context = { exports: {}, require: requireModule, Error, ...extras }
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
function api(response) {
  const calls = []
  const transport = { default: { get: async input => { calls.push(input); if (response instanceof Error) throw response; return response }, post: async input => { calls.push(input); return 91 }, put: async input => { calls.push(input); return true } } }
  const categories = moduleFrom(read('src/api/dcc/controlledFile/fileCategories.ts'), name => { assert.equal(name, '@/config/axios'); return transport })
  const taxonomy = moduleFrom(read('src/api/dcc/controlledFile/fileTypeTaxonomies.ts'), name => {
    if (name === '@/config/axios') return transport
    if (name === './fileCategories') return categories
    throw Error(name)
  })
  return { categories, taxonomy, calls }
}
const category = mode => ({ id: 8, parentId: 2, code: 'OTHER-NORMAL-NAME', name: '任意正式类别', lifecycleStage: 'OUTPUT',
  fileTypeTaxonomyId: 6, directoryId: 7, active: true, sort: 3, approvalSourceMode: mode, source: 'LOCAL', distributionRequired: false, trainingRequired: false })

test('formal category read/save carries only explicit source modes and does not infer other from names', async () => {
  for (const mode of ['PUBLIC_MATRIX', 'APPLICATION_CUSTOM']) {
    const h = api([category(mode)]), rows = await h.categories.getFileCategoryList()
    assert.equal(rows[0].approvalSourceMode, mode)
    await h.categories.updateFileCategory(8, category(mode))
    assert.equal(h.calls[1].data.approvalSourceMode, mode)
    assert.equal(h.calls[1].url, '/dcc/file-categories/8')
  }
  for (const mode of [undefined, null, '', 'other', ' PUBLIC_MATRIX']) {
    await assert.rejects(api([{ ...category(mode), name: '其他' }]).categories.getFileCategoryList(), /审批来源.*未配置|审批来源.*无效/)
    const h = api([])
    await assert.rejects(h.categories.createFileCategory(category(mode)), /审批来源/)
    await assert.rejects(h.categories.updateFileCategory(8, category(mode)), /审批来源/)
    assert.equal(h.calls.length, 0)
  }
})

test('upload options preserve the strict parent/valid/blocked shapes and do not reject legal BC for blocked A', async () => {
  const rows = [{ id: '1', name: '设计', active: true, approvalSourceMode: null, uploadBlocker: null, children: [
    { id: '2', parentId: '1', name: '阶段', active: true, approvalSourceMode: null, uploadBlocker: null, children: [
      { id: '3', parentId: '2', name: '坏A', active: true, approvalSourceMode: null, uploadBlocker: '文件类型3未配置启用文件类别' },
      { id: '4', parentId: '2', name: '合法B', active: true, approvalSourceMode: 'PUBLIC_MATRIX', uploadBlocker: null },
      { id: '5', parentId: '2', name: '合法C', active: true, approvalSourceMode: 'APPLICATION_CUSTOM', uploadBlocker: null }
    ] }
  ] }]
  const snapshot = JSON.stringify(rows)
  const h = api(rows), actual = await h.taxonomy.getFileTypeTaxonomyUploadOptions()
  assert.equal(actual[0].approvalSourceMode, null)
  assert.equal(actual[0].uploadBlocker, null)
  const leaves = actual[0].children[0].children
  assert.equal(leaves[0].approvalSourceMode, null); assert.match(leaves[0].uploadBlocker, /未配置/)
  assert.equal(leaves[1].approvalSourceMode, 'PUBLIC_MATRIX'); assert.equal(leaves[1].uploadBlocker, null)
  assert.equal(leaves[2].approvalSourceMode, 'APPLICATION_CUSTOM'); assert.equal(leaves[2].uploadBlocker, null)
  assert.equal(JSON.stringify(rows), snapshot)
  const flat = await api([{ id: '1', active: true, name: '父', approvalSourceMode: null, uploadBlocker: null },
    { id: '3', parentId: '1', active: true, name: '叶', approvalSourceMode: null, uploadBlocker: '不足三级，不能提交' }]).taxonomy.getFileTypeTaxonomyUploadOptions()
  assert.equal(flat[0].approvalSourceMode, null); assert.match(flat[1].uploadBlocker, /不足三级/)
})

test('invalid candidate contracts still reject rather than become a guessed public source or a silent blocker', async () => {
  for (const patch of [{ approvalSourceMode: undefined }, { approvalSourceMode: 'OTHER' }, { uploadBlocker: undefined },
    { uploadBlocker: '' }, { uploadBlocker: '  ' }, { approvalSourceMode: 'PUBLIC_MATRIX', uploadBlocker: '却被阻塞' },
    { approvalSourceMode: null, uploadBlocker: null }]) {
    const row = { id: '3', active: true, name: '其他', approvalSourceMode: null, uploadBlocker: '配置问题', ...patch }
    await assert.rejects(api([row]).taxonomy.getFileTypeTaxonomyUploadOptions(), /审批来源|阻断|合同/)
  }
  const parent = { id: '1', name: '父', active: true, approvalSourceMode: 'PUBLIC_MATRIX', uploadBlocker: null,
    children: [{ id: '3', name: '叶', active: true, approvalSourceMode: 'APPLICATION_CUSTOM', uploadBlocker: null }] }
  await assert.rejects(api([parent]).taxonomy.getFileTypeTaxonomyUploadOptions(), /父节点|路径节点/)
})

test('actual type path helper preserves explicit blocker/source for the disabled option without erasing legal types', async () => {
  const input = [
    { id: '1', parentId: '0', name: '设计', active: true, approvalSourceMode: null, uploadBlocker: null },
    { id: '2', parentId: '1', name: '输出', active: true, approvalSourceMode: null, uploadBlocker: null },
    { id: '3', parentId: '2', name: '坏A', active: true, approvalSourceMode: null, uploadBlocker: 'A审批来源未知，请维护' },
    { id: '4', parentId: '2', name: '合法B', active: true, approvalSourceMode: 'PUBLIC_MATRIX', uploadBlocker: null },
    { id: '5', parentId: '2', name: '合法C', active: true, approvalSourceMode: 'APPLICATION_CUSTOM', uploadBlocker: null }
  ]
  const rows = await api(input).taxonomy.getFileTypeTaxonomyUploadOptions()
  const paths = moduleFrom(read('src/views/dcc/controlled-file/upload/file-type-options.ts')).buildUploadFileTypePaths(rows)
  assert.equal(paths.size, 5)
  assert.equal(paths.get('3').approvalSourceMode, null); assert.match(paths.get('3').uploadBlocker, /A审批来源未知/)
  assert.equal(paths.get('4').approvalSourceMode, 'PUBLIC_MATRIX'); assert.equal(paths.get('4').uploadBlocker, null)
  assert.equal(paths.get('5').approvalSourceMode, 'APPLICATION_CUSTOM'); assert.equal(paths.get('5').uploadBlocker, null)
  assert.equal(paths.get('3').names.join('/'), '设计/输出/坏A')
})

test('technical upload candidate transport failure remains the original error rather than empty or blocked rows', async () => {
  const failure = new Error('候选目录数据库连接失败')
  await assert.rejects(api(failure).taxonomy.getFileTypeTaxonomyUploadOptions(), error => error === failure)
})

const renderer = vue.createRenderer({
  createElement: type => ({ type, props: {}, children: [], parent: null }), createText: text => ({ text }), createComment: () => ({ text: '' }),
  insert(node, parent, anchor) { if (node.parent) this.remove(node); const i = anchor ? parent.children.indexOf(anchor) : -1; parent.children.splice(i < 0 ? parent.children.length : i, 0, node); node.parent = parent },
  remove(node) { if (node.parent) { node.parent.children.splice(node.parent.children.indexOf(node), 1); node.parent = null } },
  patchProp: (n, k, _o, v) => { n.props[k] = v }, setText: (n, t) => { n.text = t }, setElementText: (n, t) => { n.text = t; n.children = [] },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null
})
const walk = (node, predicate, result = []) => { if (predicate(node)) result.push(node); for (const n of node.children || []) walk(n, predicate, result); return result }
const content = node => [node.text || '', ...(node.children || []).map(content)].join('')
async function form() {
  const writes = [], saved = [], exposed = vue.ref(), host = { children: [] }
  const source = read('src/views/dcc/controlled-file/categories/components/CategoryForm.vue')
  const { descriptor } = parse(source)
  const script = compileScript(descriptor, { id: 'g03-category', inlineTemplate: true })
  const categoryApi = api([]).categories
  const component = moduleFrom(script.content, name => {
    if (name === 'vue') return vue
    if (name.endsWith('/fileCategories')) return { ...categoryApi, createFileCategory: async value => { writes.push(value); return 91 }, updateFileCategory: async (id, value) => { writes.push({ ...value, id }); return true } }
    if (name.endsWith('/fileTypeTaxonomies')) return { getFileTypeTaxonomyList: async () => [] }
    if (name.endsWith('/tree')) return { defaultProps: {}, handleTree: rows => rows }
    if (name.endsWith('/options')) return { ACTIVE_STATUS_OPTIONS: [] }
    if (name.endsWith('/file-type-taxonomy-stage')) return { buildDccFileTypeTaxonomyStageNameMap: () => new Map(), resolveDccFileTypeTaxonomyStageName: () => 'OUTPUT' }
    throw Error(name)
  }, { ref: vue.ref, reactive: vue.reactive, computed: vue.computed, useMessage: () => ({ success: value => saved.push(value) }), useI18n: () => ({ t: value => value }) }).default
  const app = renderer.createApp({ render: () => vue.h(component, { ref: exposed }) })
  app.component('Dialog', { props: ['modelValue'], setup: (props, { slots }) => () => props.modelValue ? vue.h('dialog', {}, [slots.default?.(), slots.footer?.()]) : null })
  app.component('el-form', { setup: (_p, { slots, expose }) => { expose({ validate: async () => true, resetFields: () => {} }); return () => vue.h('form', {}, slots.default?.()) } })
  app.component('el-form-item', { props: ['label'], setup: (p, { slots }) => () => vue.h('field', { label: p.label }, slots.default?.()) })
  app.component('el-select', { props: ['modelValue', 'disabled'], emits: ['update:modelValue'], setup: (p, { slots, attrs, emit }) => () => vue.h('selector', { ...attrs, value: p.modelValue, onChoose: value => emit('update:modelValue', value) }, slots.default?.()) })
  app.component('el-option', { props: ['label', 'value'], setup: p => () => vue.h('option', { value: p.value }, p.label) })
  app.component('el-alert', { props: ['title'], setup: p => () => vue.h('alert', {}, p.title) })
  for (const tag of ['el-tree-select', 'el-input', 'el-cascader', 'el-radio-group', 'el-radio', 'el-switch', 'el-input-number', 'el-button']) app.component(tag, { setup: (_p, { slots, attrs }) => () => vue.h(tag, attrs, slots.default?.()) })
  app.directive('loading', {})
  app.mount(host); await vue.nextTick()
  const field = () => { const rows = walk(host, n => n.type === 'selector' && n.props['data-testid'] === 'dcc-category-approval-source'); assert.equal(rows.length, 1); return rows[0] }
  const open = async row => { exposed.value.open(row ? 'update' : 'create', { row, categories: [], directories: [] }); await vue.nextTick(); await vue.nextTick() }
  const save = async () => { const button = walk(host, n => n.type === 'el-button' && content(n).includes('确定'))[0]; await button.props.onClick(); await vue.nextTick() }
  return { app, host, writes, saved, field, open, save }
}

test('real category SFC explicitly restores custom source and saves an administrator choice on existing resource', async () => {
  const h = await form()
  try {
    await h.open(category('APPLICATION_CUSTOM'))
    assert.equal(h.field().props.value, 'APPLICATION_CUSTOM')
    h.field().props.onChoose('PUBLIC_MATRIX'); await vue.nextTick(); await h.save()
    assert.equal(h.writes[0].approvalSourceMode, 'PUBLIC_MATRIX')
    assert.equal(h.writes[0].fileTypeTaxonomyId, 6); assert.equal(h.writes[0].directoryBinding.directoryId, 7)
    assert.equal(h.writes[0].distributionRequired, false); assert.equal(h.writes[0].trainingRequired, false)
  } finally { h.app.unmount() }
})

test('unknown saved category source produces visible feedback and zero write rather than create-default restoration', async () => {
  const h = await form()
  try {
    await h.open(category(undefined)); assert.match(content(h.host), /审批来源.*未配置|审批来源.*无效/)
    await h.save(); assert.equal(h.writes.length, 0)
    await h.open(); assert.equal(h.field().props.value, 'PUBLIC_MATRIX')
  } finally { h.app.unmount() }
})
