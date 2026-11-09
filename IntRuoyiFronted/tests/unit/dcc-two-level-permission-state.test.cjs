const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const drain = async () => { for (let i = 0; i < 30; i++) await Promise.resolve(); await vue.nextTick() }

function mount(api) {
  const filename = 'src/views/dcc/controlled-file/categories/components/CategoryPermissionRulesTab.vue'
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const script = compileScript(descriptor, { id: filename })
  assert.deepEqual(compileTemplate({ id: filename, source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  const exports = {}, messages = []
  const source = ts.transpileModule(script.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  new Function('exports', 'require', 'ref', 'computed', 'watch', 'useMessage', source)(exports, id => {
    if (id === 'vue') return vue
    if (id === '@/api/dcc/controlledFile/fileCategories') return { getFileCategoryList: async () => [{ id: 5, code: 'OTHER', name: '其他' }], ...api }
    if (id === '@/api/system/user') return { getSimpleUserList: async () => [{ id: 8, username: 'actor', nickname: '用户' }] }
    if (id === '@/api/system/dept') return { getSimpleDeptList: async () => [] }
    if (id === '@/api/system/role') return { getSimpleRoleList: async () => [] }
    if (id === '@/api/system/post') return { getSimplePostList: async () => [] }
    if (id === '../../shared/lifecycle') return { DCC_CATEGORY_PERMISSION_OPTIONS: ['EDIT', 'VIEW', 'UPLOAD', 'REVIEW', 'APPROVE'].map(value => ({ value, label: value })) }
    throw Error('Unexpected import: ' + id)
  }, vue.ref, vue.computed, vue.watch, () => ({ success: x => messages.push(x), error: x => messages.push(x) }))
  exports.default.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: () => ({ children: [] }), createComment: () => ({}), createText: () => ({}), insert: (c, p) => p.children.push(c), remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(exports.default, { active: true, categoryRevision: 0 })
  const host = app.mount({ children: [] })
  return { state: host.$.setupState, app, messages }
}

const rule = actionType => ({ actionType, subjectType: 'USER', subjectId: 8, scopeType: 'GLOBAL', active: true, remark: '原记录' })
test('actual category form preserves legacy and matrix rules as read-only, saves only EDIT/VIEW', async () => {
  const calls = [], rules = ['UPLOAD', 'REVIEW', 'APPROVE', 'EDIT', 'VIEW'].map(rule)
  const c = mount({ getCategoryPermissionRules: async () => rules, replaceCategoryPermissionRules: async (...args) => calls.push(args) })
  try {
    await drain(); await c.state.openRules({ id: 5, name: '其他' })
    assert.deepEqual(c.state.configurableActionOptions.map(x => x.value), ['EDIT', 'VIEW'])
    assert.deepEqual(c.state.legacyRules.map(x => x.actionType), ['UPLOAD'])
    assert.deepEqual(c.state.matrixRules.map(x => x.actionType), ['REVIEW', 'APPROVE'])
    assert.deepEqual(c.state.editingRules.map(x => x.actionType), ['EDIT', 'VIEW'])
    await c.state.saveRules()
    assert.equal(calls.length, 1)
    assert.equal(calls[0][0], 5)
    assert.deepEqual(calls[0][1], [rule('EDIT'), rule('VIEW')])
    assert.equal(rules[0].actionType, 'UPLOAD')
  } finally { c.app.unmount() }
})
test('new user rule defaults to browse and requires a real selected user before saving', async () => {
  const calls = [], c = mount({ getCategoryPermissionRules: async () => [], replaceCategoryPermissionRules: async (...args) => calls.push(args) })
  try {
    await drain(); await c.state.openRules({ id: 5, name: '其他' }); c.state.addRule()
    assert.equal(c.state.editingRules[0].actionType, 'VIEW')
    await c.state.saveRules(); assert.equal(calls.length, 0)
    c.state.editingRules[0].subjectId = 8
    await c.state.saveRules(); assert.equal(calls.length, 1)
    assert.equal(calls[0][1][0].actionType, 'VIEW')
  } finally { c.app.unmount() }
})
test('denied rules load cannot submit an empty destructive replacement', async () => {
  const calls = [], c = mount({ getCategoryPermissionRules: async () => { throw Error('无权限') }, replaceCategoryPermissionRules: async (...args) => calls.push(args) })
  try {
    await drain(); await c.state.openRules({ id: 5, name: '其他' })
    assert.match(c.state.errorMessage, /无权限/); assert.equal(c.state.rulesLoaded, false)
    await c.state.saveRules(); assert.equal(calls.length, 0)
  } finally { c.app.unmount() }
})
test('switching categories discards a late response for the previous category', async () => {
  let resolveFirst
  const c = mount({ getCategoryPermissionRules: id => id === 5 ? new Promise(resolve => { resolveFirst = resolve }) : Promise.resolve([rule('VIEW')]) })
  try {
    await drain(); const first = c.state.openRules({ id: 5, name: '前类别' })
    await c.state.openRules({ id: 6, name: '后类别' })
    resolveFirst([rule('EDIT')]); await first
    assert.equal(c.state.selectedCategory.id, 6)
    assert.deepEqual(c.state.editingRules.map(x => x.actionType), ['VIEW'])
    assert.equal(c.state.rulesLoaded, true)
  } finally { c.app.unmount() }
})
