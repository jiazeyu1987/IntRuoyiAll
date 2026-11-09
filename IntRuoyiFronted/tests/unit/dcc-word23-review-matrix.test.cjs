const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const assert = require('node:assert/strict')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const directory = path.join(root, 'src/views/dcc/controlled-file/categories')
const source = fs.readFileSync(path.join(directory, 'components/CategoryMatrixDialog.vue'), 'utf8')
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
const department = evaluate(fs.readFileSync(path.join(directory, 'components/departmentTreeScope.ts'), 'utf8'), { '@/utils/tree': tree })
const governance = evaluate(fs.readFileSync(path.join(directory, 'governance.ts'), 'utf8'), { '@/utils/formatTime': { formatDateTimeValue: value => value || '' } })
const rules = () => [
  { stageType: 'SIGNOFF', active: true, subjectLabel: '研发', marker: '●', subjectType: 'DEPT', subjectId: 11 },
  { stageType: 'APPROVAL', active: true, subjectLabel: '批准人', marker: '●', subjectType: 'USER', subjectId: 8 }
]
const departments = [{ id: 1, parentId: 0, name: '顶级部门' }, { id: 10, parentId: 1, name: '公司甲' },
  { id: 11, parentId: 10, name: '研发' }, { id: 20, parentId: 1, name: '公司乙' }, { id: 21, parentId: 20, name: '质量' }]
const renderer = vue.createRenderer({
  createElement: () => ({ children: [] }), createText: text => ({ text }), createComment: () => ({}), insert: (child, parent) => parent.children.push(child),
  remove() {}, patchProp() {}, setText() {}, setElementText() {}, parentNode() {}, nextSibling() {}
})
async function fixture(overrides = {}) {
  const saves = [], messages = [], events = [], reads = []
  const api = {
    getCategoryApprovalMatrix: async id => { reads.push(id); return { effectiveTime: '2026-10-26 10:00:00', remark: '', rules: rules() } },
    previewCategoryApprovalMatrixEffectiveAccess: async () => ({ blocking: false, stages: [] }),
    saveCategoryApprovalMatrix: async (id, payload) => { saves.push({ id, payload: JSON.parse(JSON.stringify(payload)) }); return 100 },
    ...overrides
  }
  const compiled = compileScript(parse(source).descriptor, { id: 'word23-matrix' })
  const component = evaluate(compiled.content, {
    '@/api/dcc/controlledFile/fileCategories': api,
    '@/api/dcc/controlledFile/approvalPositions': { getApprovalPositionList: async () => [] },
    '@/api/system/dept': { getSimpleDeptList: async () => departments }, '@/api/system/post': { getSimplePostList: async () => [] },
    '@/utils/tree': tree, '@/views/system/role/components/RoleSelect.vue': {}, '@/views/system/user/components/UserSelect.vue': {},
    './viewMatrixDepartmentMatcher': { applyDepartmentAutoMatchToViewMatrixRules: rows => ({ rules: rows }) },
    '../governance': governance, './departmentTreeScope': department
  }, { ref: vue.ref, reactive: vue.reactive, computed: vue.computed, watch: vue.watch,
    useMessage: () => ({ success: message => messages.push(message), warning: message => messages.push(message), error: message => messages.push(message) }) }).default
  component.render = () => null
  const app = renderer.createApp(component, { onSuccess: () => events.push('success') })
  app.mount({ children: [] })
  const state = app._instance.setupState
  await app._instance.exposed.open({ id: 6, code: 'TYPE', name: '正式类别' }, 'create')
  return { app, state, saves, messages, events, reads }
}
test('empty department rules expose the formal directory without inferring company from sibling rules', async () => {
  const f = await fixture()
  try {
    f.state.formData.rules = []
    const tree = f.state.getDepartmentTreeForRule()
    assert.deepEqual(JSON.parse(JSON.stringify(tree)).map(row => [row.id, row.children.map(child => child.id)]), [[10, [11]], [20, [21]]])
    f.state.addRule()
    const row = f.state.formData.rules[0]
    row.subjectType = 'DEPT'
    f.state.handleSubjectTypeChange(row)
    assert.equal(row.marker, '●')
    f.state.handleDepartmentChange(row, 21)
    assert.equal(row.subjectId, 21)
    assert.equal(row.subjectName, '质量')
    assert.equal(row.subjectDepartmentPath, '顶级部门-公司乙-质量')
    assert.equal(row.marker, '●')
    f.state.handleDepartmentChange(row, undefined)
    assert.equal(row.subjectId, undefined)
  } finally { f.app.unmount() }
})
test('delete removes the chosen rule, and repeated delete never removes another row', async () => {
  const f = await fixture()
  try {
    const first = f.state.formData.rules[1], target = f.state.formData.rules[0]
    f.state.removeRule(target)
    assert.equal(f.state.formData.rules.length, 1)
    assert.equal(f.state.formData.rules[0], first)
    f.state.removeRule(target)
    assert.equal(f.state.formData.rules.length, 1)
    await f.state.submitForm()
    assert.equal(f.saves.length, 0, 'removing the last department signoff rule still enforces mandatory stage validation')
  } finally { f.app.unmount() }
})
test('successful save closes once and tells the parent to refresh without rereading inside the dialog', async () => {
  const f = await fixture()
  try {
    await f.state.submitForm()
    assert.equal(f.saves.length, 1)
    assert.equal(f.state.dialogVisible, false)
    assert.equal(f.state.saving, false)
    assert.deepEqual(f.events, ['success'])
    assert.deepEqual(f.reads, [6])
    assert.equal(f.saves[0].payload.rules.length, 2)
  } finally { f.app.unmount() }
})
test('blocked preview or actual write failure keeps the draft open and never signals success', async () => {
  for (const overrides of [
    { previewCategoryApprovalMatrixEffectiveAccess: async () => ({ blocking: true, stages: [] }) },
    { saveCategoryApprovalMatrix: async () => { throw Error('正式矩阵写入失败') } }
  ]) {
    const f = await fixture(overrides)
    try {
      await f.state.submitForm()
      assert.equal(f.state.dialogVisible, true)
      assert.equal(f.state.saving, false)
      assert.deepEqual(f.events, [])
      assert.equal(f.state.formData.rules.length, 2)
      assert.ok(f.messages.some(message => message.includes('阻塞')) || f.state.previewError.includes('正式矩阵写入失败'))
    } finally { f.app.unmount() }
  }
})
test('a save completing after another category opens cannot close the new dialog', async () => {
  let release
  const pending = new Promise(resolve => { release = resolve })
  const f = await fixture({ saveCategoryApprovalMatrix: () => pending })
  try {
    const saving = f.state.submitForm()
    await new Promise(resolve => setImmediate(resolve))
    await f.app._instance.exposed.open({ id: 7, code: 'NEXT', name: '下一类别' }, 'create')
    release(100)
    await saving
    assert.equal(f.state.dialogVisible, true)
    assert.equal(f.state.currentCategory.id, 7)
    assert.deepEqual(f.events, [])
  } finally { f.app.unmount() }
})

test('non-department signoff is rejected by preview and save including new-type drafts', async () => {
  for (const subjectType of ['USER', 'ROLE', 'POST', 'DCC_POSITION', 'POSITION']) {
    let previews = 0
    const f = await fixture({ previewCategoryApprovalMatrixEffectiveAccess: async () => { previews++; return { blocking: false, stages: [] } } })
    try {
      f.state.formData.rules[0].subjectType = subjectType
      await f.state.refreshPreview()
      assert.match(f.state.previewError, /会签规则必须选择正式部门/)
      await f.state.submitForm()
      assert.equal(f.saves.length, 0)
      assert.equal(previews, 0)
      f.state.draftMode = true
      await f.state.submitForm()
      assert.equal(f.saves.length, 0)
      assert.deepEqual(f.events, [])
      assert.ok(f.messages.some(message => message.includes('会签规则必须选择正式部门')))
    } finally { f.app.unmount() }
  }
})

test('application custom category cannot open the shared public matrix dialog', async () => {
  const f = await fixture()
  try {
    f.state.dialogVisible = false
    await f.app._instance.exposed.open({ id: 7, name: '其他', approvalSourceMode: 'APPLICATION_CUSTOM' }, 'create')
    assert.equal(f.state.dialogVisible, false)
    assert.deepEqual(f.reads, [6])
    assert.ok(f.messages.some(message => message.includes('本次申请自定义矩阵')))
  } finally { f.app.unmount() }
})
