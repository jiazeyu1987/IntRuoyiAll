const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/CustomApplicationMatrixEditor.vue')
const DEPT = '9223372036854775701', APPROVER = '9223372036854775702', CONTROL = '9223372036854775703'
const plain = value => JSON.parse(JSON.stringify(value))
const matrix = () => ({ approverUserId: APPROVER, documentControlUserId: CONTROL })

// Real SFC template/setup and Vue updates. Element Plus widgets are explicit isolated UI ports.
const ElSelect = vue.defineComponent({
  props: ['modelValue', 'multiple', 'disabled', 'placeholder', 'userOptions'], emits: ['update:modelValue'],
  setup: (props, { attrs, slots, emit }) => () => vue.h('selector', {
    ...attrs, modelValue: props.modelValue, multiple: props.multiple, disabled: props.disabled, userOptions: props.userOptions,
    onUserChange: value => emit('update:modelValue', value)
  }, slots.default?.())
})
const ElOption = vue.defineComponent({
  props: ['value', 'label', 'disabled'],
  setup: props => () => vue.h('option', { value: props.value, disabled: props.disabled }, props.label)
})
const ElFormItem = vue.defineComponent({
  props: ['label', 'required'],
  setup: (props, { slots }) => () => vue.h('field', { label: props.label }, slots.default?.())
})
function component() {
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(errors, [])
  const source = compileScript(descriptor, { id: 'g03-custom-editor', inlineTemplate: true })
  const context = { exports: {}, Error, require: name => {
    if (name === 'vue') return vue
    if (name === '@/views/system/user/components/UserSelectV2.vue') return { default: ElSelect }
    if (name === 'element-plus') return { ElSelect, ElOption, ElFormItem }
    throw new Error('Unexpected production dependency: ' + name)
  } }
  vm.runInNewContext(ts.transpileModule(source.content, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports.default
}
const renderer = vue.createRenderer({
  createElement: type => ({ type, children: [], props: {}, parent: null }),
  createText: text => ({ text, parent: null }), createComment: text => ({ text: '', parent: null }),
  insert(node, parent, anchor) {
    if (node.parent) this.remove(node)
    const index = anchor ? parent.children.indexOf(anchor) : -1
    parent.children.splice(index < 0 ? parent.children.length : index, 0, node); node.parent = parent
  },
  remove(node) { const parent = node.parent; if (parent) { parent.children.splice(parent.children.indexOf(node), 1); node.parent = null } },
  setText: (node, text) => { node.text = text },
  setElementText: (node, text) => { node.text = text; node.children = [] },
  patchProp: (node, key, _old, value) => { node.props[key] = value },
  parentNode: node => node.parent,
  nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null
})
function nodes(root, predicate, found = []) {
  if (predicate(root)) found.push(root)
  for (const child of root.children || []) nodes(child, predicate, found)
  return found
}
const text = root => [root.text || '', ...(root.children || []).map(text)].join('')
async function mount(overrides = {}, commit = true) {
  const events = [], exposed = vue.ref(), root = { children: [] }
  const props = vue.reactive({
    approvalSourceMode: 'APPLICATION_CUSTOM', modelValue: matrix(), selectedSignoffDepartmentIds: [DEPT],
    departments: [{ id: DEPT, name: '质量部' }, { id: '51', name: '研发部' }],
    users: [{ id: APPROVER, name: '批准人甲', deptName: '质量部' }, { id: CONTROL, name: '文控乙' }, { id: '99', name: '新批准人' }],
    disabled: false, loading: false, directoryError: '', ...overrides
  })
  const editor = component()
  const app = renderer.createApp({ render: () => vue.h(editor, {
    ...props, ref: exposed,
    'onUpdate:modelValue': value => { events.push({ kind: 'matrix', value: plain(value) }); if (commit) props.modelValue = value },
    'onUpdate:selectedSignoffDepartmentIds': value => { events.push({ kind: 'departments', value: plain(value) }); if (commit) props.selectedSignoffDepartmentIds = value }
  }) })
  app.mount(root); await vue.nextTick()
  const field = id => { const found = nodes(root, n => n.type === 'selector' && n.props['data-testid'] === id); assert.equal(found.length, 1, id); return found[0] }
  const choose = async (id, value) => { field(id).props.onUserChange(value); await vue.nextTick() }
  return { props, app, events, root, field, choose, api: exposed.value, text: () => text(root) }
}

test('saved custom accounts and departments render unchanged without automatic selection or emissions', async () => {
  const h = await mount()
  try {
    assert.equal(h.field('dcc-custom-approver').props.modelValue, APPROVER)
    assert.equal(h.field('dcc-custom-document-control').props.modelValue, CONTROL)
    assert.deepEqual(plain(h.field('dcc-custom-signoff-departments').props.modelValue), [DEPT])
    assert.deepEqual(h.events, [])
    assert.deepEqual(plain(h.api.validate('SUBMIT')), { applicationCustomMatrix: matrix(), selectedSignoffDepartmentIds: [DEPT] })
  } finally { h.app.unmount() }
})

test('template account edit/clear emits one scoped immutable choice and leaves directories and other fields intact', async () => {
  const original = matrix(), users = [{ id: APPROVER, name: '甲' }, { id: CONTROL, name: '乙' }, { id: '99', name: '丙' }]
  const h = await mount({ modelValue: original, users })
  try {
    await h.choose('dcc-custom-approver', '99')
    assert.deepEqual(h.events[0], { kind: 'matrix', value: { approverUserId: '99', documentControlUserId: CONTROL } })
    assert.deepEqual(original, matrix()); assert.equal(users.length, 3)
    await h.choose('dcc-custom-document-control', '')
    assert.equal(h.props.modelValue.documentControlUserId, null)
    assert.equal(h.props.modelValue.approverUserId, '99')
    assert.deepEqual(plain(h.props.selectedSignoffDepartmentIds), [DEPT])
  } finally { h.app.unmount() }
})

test('every signoff department can be removed, empty draft stays empty and submission fails exactly', async () => {
  const h = await mount()
  try {
    await h.choose('dcc-custom-signoff-departments', ['51'])
    assert.deepEqual(plain(h.props.selectedSignoffDepartmentIds), ['51'])
    await h.choose('dcc-custom-signoff-departments', [])
    assert.deepEqual(plain(h.api.validate('SAVE').selectedSignoffDepartmentIds), [])
    assert.throws(() => h.api.validate('SUBMIT'), /请至少选择一个会签部门/)
    await vue.nextTick(); assert.match(h.text(), /请至少选择一个会签部门/)
    assert.deepEqual(plain(h.props.modelValue), matrix())
  } finally { h.app.unmount() }
})

test('partial drafts keep genuine nulls and missing approval or document-control accounts have distinct errors', async () => {
  const h = await mount({ modelValue: null, selectedSignoffDepartmentIds: null })
  try {
    assert.deepEqual(plain(h.api.validate('SAVE')), { applicationCustomMatrix: null, selectedSignoffDepartmentIds: null })
    assert.deepEqual(h.events, [])
    h.props.selectedSignoffDepartmentIds = [DEPT]; await vue.nextTick()
    assert.throws(() => h.api.validate('SUBMIT'), /请选择本次批准人/)
    await h.choose('dcc-custom-approver', APPROVER)
    assert.throws(() => h.api.validate('SUBMIT'), /请选择本次文控审核人/)
    await h.choose('dcc-custom-document-control', CONTROL)
    assert.doesNotThrow(() => h.api.validate('SUBMIT'))
  } finally { h.app.unmount() }
})

test('restored unavailable account remains selected and visible, is not replaced, and cannot submit', async () => {
  const h = await mount()
  try {
    h.props.users = [{ id: APPROVER, name: '批准人甲', status: 1 }, { id: CONTROL, name: '文控乙' }]; await vue.nextTick()
    assert.equal(h.field('dcc-custom-approver').props.modelValue, APPROVER)
    assert.match(h.text(), new RegExp(APPROVER + '.*当前不可用'))
    assert.throws(() => h.api.validate('SUBMIT'), /本次批准人.*不可用/)
    assert.deepEqual(h.events, [])
    h.props.modelValue = { approverUserId: CONTROL, documentControlUserId: '88' }; await vue.nextTick()
    assert.throws(() => h.api.validate('SUBMIT'), /本次文控审核人.*88.*不可用/)
  } finally { h.app.unmount() }
})

test('source switch hides editor and rejects exposed custom validation without erasing controlled saved values', async () => {
  const h = await mount()
  try {
    h.props.approvalSourceMode = 'PUBLIC_MATRIX'; await vue.nextTick()
    assert.equal(nodes(h.root, n => n.type === 'selector').length, 0)
    assert.throws(() => h.api.validate('SUBMIT'), /当前文件类型不使用本次自定义矩阵/)
    assert.deepEqual(h.events, []); assert.deepEqual(plain(h.props.modelValue), matrix())
    h.props.approvalSourceMode = 'APPLICATION_CUSTOM'; await vue.nextTick()
    assert.equal(h.field('dcc-custom-approver').props.modelValue, APPROVER)
  } finally { h.app.unmount() }
})

test('loading, disabled and failed directory prevent even direct widget emissions and failed directory blocks submission', async () => {
  for (const override of [{ loading: true }, { disabled: true }, { directoryError: '启用账号目录读取失败' }]) {
    const h = await mount(override)
    try {
      assert.equal(h.field('dcc-custom-approver').props.disabled, true)
      await h.choose('dcc-custom-approver', '99'); await h.choose('dcc-custom-signoff-departments', ['51'])
      assert.deepEqual(h.events, [])
      if (override.directoryError) { assert.match(h.text(), /启用账号目录读取失败/); assert.throws(() => h.api.validate('SUBMIT'), /启用账号目录读取失败/) }
      if (override.loading) assert.throws(() => h.api.validate('SUBMIT'), /等待.*目录/)
    } finally { h.app.unmount() }
  }
})

test('unsafe, duplicate or unknown identities are rejected rather than silently cast or accepted', async () => {
  const h = await mount()
  try {
    for (const bad of [9007199254740992, '0', '9223372036854775808', '88']) {
      await h.choose('dcc-custom-approver', bad)
      assert.equal(h.events.length, 0); assert.match(h.text(), /身份|不可用/)
    }
    await h.choose('dcc-custom-signoff-departments', [DEPT, DEPT])
    assert.deepEqual(h.events, []); assert.match(h.text(), /会签部门不能重复/)
    h.props.users = [{ id: APPROVER, name: '甲' }, { id: APPROVER, name: '乙' }]; await vue.nextTick()
    assert.throws(() => h.api.validate('SUBMIT'), /账号目录身份重复/)
  } finally { h.app.unmount() }
})

test('restored model and department changes rerender exact new values without carrying local old choice', async () => {
  const h = await mount()
  try {
    await h.choose('dcc-custom-approver', '99')
    h.events.length = 0
    h.props.modelValue = matrix(); h.props.selectedSignoffDepartmentIds = ['51']; await vue.nextTick()
    assert.equal(h.field('dcc-custom-approver').props.modelValue, APPROVER)
    assert.deepEqual(plain(h.field('dcc-custom-signoff-departments').props.modelValue), ['51'])
    assert.deepEqual(h.events, [])
    h.props.departments = [{ id: DEPT, name: '质量部' }]; await vue.nextTick()
    assert.throws(() => h.api.validate('SUBMIT'), /本次会签部门.*51.*不可用/)
    assert.deepEqual(plain(h.props.selectedSignoffDepartmentIds), ['51'])
  } finally { h.app.unmount() }
})

test('malformed restored selections display a specific error and cannot crash the real template or submit', async () => {
  const h = await mount()
  try {
    h.props.selectedSignoffDepartmentIds = { unexpected: DEPT }; await vue.nextTick()
    assert.match(h.text(), /本次会签部门实际选择缺失/)
    assert.throws(() => h.api.validate('SAVE'), /本次会签部门实际选择缺失/)
    h.props.selectedSignoffDepartmentIds = [DEPT]
    h.props.modelValue = { approverUserId: APPROVER, documentControlUserId: CONTROL, unexpected: true }; await vue.nextTick()
    assert.match(h.text(), /未知字段/)
    assert.throws(() => h.api.validate('SUBMIT'), /未知字段/)
  } finally { h.app.unmount() }
})

test('available safe numeric directory identities normalize to exact strings in rendered restored choices', async () => {
  const h = await mount({
    modelValue: { approverUserId: 99, documentControlUserId: 100 }, selectedSignoffDepartmentIds: [51],
    users: [{ id: 99, name: '甲' }, { id: 100, name: '乙' }], departments: [{ id: 51, name: '质量' }]
  })
  try {
    assert.equal(h.field('dcc-custom-approver').props.modelValue, '99')
    assert.deepEqual(plain(h.field('dcc-custom-signoff-departments').props.modelValue), ['51'])
    assert.deepEqual(plain(h.api.validate('SUBMIT')), {
      applicationCustomMatrix: { approverUserId: '99', documentControlUserId: '100' }, selectedSignoffDepartmentIds: ['51']
    })
    assert.deepEqual(h.events, [])
  } finally { h.app.unmount() }
})


test('unified account pickers receive only enabled business accounts with exact Long identities', async () => {
  const h = await mount({ users: [{ id: APPROVER, name: '批准人甲' }, { id: CONTROL, name: '文控乙' }, { id: '77', name: '停用人', status: 1 }] })
  try {
    for (const field of ['dcc-custom-approver', 'dcc-custom-document-control']) {
      const candidates = h.field(field).props.userOptions
      assert.deepEqual(plain(candidates.map(user => user.id)), [APPROVER, CONTROL])
      assert.deepEqual(plain(candidates.map(user => user.nickname)), ['批准人甲', '文控乙'])
      assert.ok(candidates.every(user => typeof user.id === 'string'))
      assert.ok(!candidates.some(user => user.id === DEPT))
    }
  } finally { h.app.unmount() }
})
