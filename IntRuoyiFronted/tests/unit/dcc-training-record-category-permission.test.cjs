const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = p => fs.readFileSync(p, 'utf8')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function moduleFrom(source, dependencies = {}) {
  const context = { exports: {}, require: id => { assert.ok(id in dependencies, id); return dependencies[id] } }
  vm.runInNewContext(transpile(source), context)
  return context.exports
}
const actionProjection = moduleFrom(read('src/api/form-center/actionProjection.ts'))
const lifecycle = moduleFrom(read('src/views/dcc/controlled-file/shared/lifecycle.ts'), { '@/api/form-center/actionProjection': actionProjection })
const categoryAPI = read('src/api/dcc/controlledFile/fileCategories.ts')
const { descriptor } = parse(read('src/views/dcc/controlled-file/categories/components/CategoryPermissionRulesTab.vue'))
const ast = ts.createSourceFile('permission-tab.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const node = ast.statements.find(n => ts.isVariableStatement(n) && n.declarationList.declarations.some(d => ts.isIdentifier(d.name) && d.name.text === name))
  assert.ok(node, name)
  return node.getText(ast)
}
function tab({ failure } = {}) {
  const requests = [], errors = [], success = []
  const rules = [
    { actionType: 'APPROVE', subjectType: 'USER', subjectId: 1, active: true },
    { actionType: 'REVIEW', subjectType: 'DEPT', subjectId: 100, active: true },
    { actionType: 'TRAINING_RECORD', subjectType: 'ROLE', subjectId: 25, scopeType: 'GLOBAL', active: true, remark: '线下记录文控' }
  ]
  const api = moduleFrom(categoryAPI, { '@/config/axios': { default: { put: async request => {
    requests.push(JSON.parse(JSON.stringify(request)))
    if (failure) throw new Error(failure)
    return request.data
  } } } })
  const c = {
    exports: {}, Error, ref: vue.ref, DCC_CATEGORY_PERMISSION_OPTIONS: lifecycle.DCC_CATEGORY_PERMISSION_OPTIONS,
    useMessage: () => ({ error: value => errors.push(value), success: value => success.push(value) }),
    getCategoryPermissionRules: async id => { assert.equal(id, 908710); return rules },
    getSimpleUserList: async () => [], getSimpleDeptList: async () => [], getSimpleRoleList: async () => [{ id: 25, code: 'doc_control', name: '文控' }], getSimplePostList: async () => [],
    replaceCategoryPermissionRules: api.replaceCategoryPermissionRules
  }
  const names = ['CATEGORY_PERMISSION_RULE_MATRIX_ACTIONS', 'configurableActionOptions', 'message', 'rulesLoading', 'saving', 'errorMessage', 'drawerVisible', 'selectedCategory', 'editingRules', 'matrixRules', 'users', 'departments', 'roles', 'posts', 'toDraft', 'openRules', 'saveRules']
  vm.runInNewContext(transpile('let nextLocalId=0;\n' + names.map(declaration).join('\n') + '\nexports.state={configurableActionOptions,editingRules,matrixRules,errorMessage};exports.open=openRules;exports.save=saveRules;'), c)
  return { ...c.exports, requests, errors, success }
}
test('actual category action contract and label include the independent training record permission', () => {
  const apiAST = ts.createSourceFile('api.ts', categoryAPI, ts.ScriptTarget.Latest, true)
  const action = apiAST.statements.find(n => ts.isTypeAliasDeclaration(n) && n.name.text === 'ControlledFileCategoryPermissionAction')
  assert.ok(action.type.types.some(n => ts.isLiteralTypeNode(n) && n.literal.text === 'TRAINING_RECORD'))
  assert.ok(lifecycle.DCC_CATEGORY_PERMISSIONS.includes('TRAINING_RECORD'))
  assert.equal(lifecycle.getDccCategoryPermissionLabel('TRAINING_RECORD'), '上传线下培训记录')
})
test('actual permission action select renders training record while keeping review and approval matrix-only', () => {
  const find = node => {
    if (node.type === 1 && node.tag === 'el-select' && node.props.some(p => p.type === 7 && p.name === 'model' && p.exp?.content === 'row.actionType')) return node
    for (const child of node.children || []) { const found = find(child); if (found) return found }
  }
  const node = find(descriptor.template.ast); assert.ok(node)
  const h = tab(), seen = []
  const compiled = compileScript(parse('<template>' + node.loc.source + '</template><script setup>const row={actionType:"TRAINING_RECORD"};const configurableActionOptions=__options;</script>').descriptor, { id: 'actual-category-actions', inlineTemplate: true })
  const c = { exports: {}, require: () => vue, __options: h.state.configurableActionOptions }
  vm.runInNewContext(transpile(compiled.content), c)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}), insert: (n, p) => p.children.push(n), remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(c.exports.default)
  app.component('el-select', { setup: (_, { slots }) => () => vue.h('section', slots.default?.()) })
  app.component('el-option', { props: ['label', 'value'], setup: props => { seen.push({ label: props.label, value: props.value }); return () => vue.h('span') } })
  app.mount({ children: [] })
  try {
    assert.ok(seen.some(o => o.value === 'TRAINING_RECORD' && o.label === '上传线下培训记录'))
    assert.equal(seen.some(o => ['REVIEW', 'APPROVE'].includes(o.value)), false)
    assert.ok(seen.some(o => o.value === 'DISTRIBUTE'))
  } finally { app.unmount() }
})
test('actual load/save handlers send training record to formal PUT without changing matrix rules or swallowing errors', async () => {
  const h = tab()
  await h.open({ id: 908710, name: '技术调研报告' })
  assert.equal(h.state.matrixRules.value.length, 2)
  assert.equal(h.state.editingRules.value.length, 1)
  await h.save()
  assert.deepEqual(h.requests, [{ url: '/dcc/file-categories/908710/permission-rules', data: [{ actionType: 'TRAINING_RECORD', subjectType: 'ROLE', subjectId: 25, scopeType: 'GLOBAL', active: true, remark: '线下记录文控' }] }])
  assert.equal(h.state.matrixRules.value.length, 2)
  assert.deepEqual(h.errors, [])
  const denied = tab({ failure: '正式类别配置拒绝' })
  await denied.open({ id: 908710, name: '技术调研报告' })
  await denied.save()
  assert.deepEqual(denied.errors, ['正式类别配置拒绝'])
  assert.deepEqual(denied.success, [])
})
