const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript'), vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const plain = value => JSON.parse(JSON.stringify(value))
function moduleFrom(source, dependencies = {}) {
  const context = { exports: {}, Error, String, Number, BigInt, JSON, Array, Object, Set, Map, Promise,
    require: id => id === 'vue' ? vue : Object.hasOwn(dependencies, id) ? dependencies[id] : (() => { throw Error('Unmapped test dependency: ' + id) })() }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
function component(file, dependencies) {
  const compiled = compileScript(parse(read(file)).descriptor, { id: file, inlineTemplate: true })
  return moduleFrom(compiled.content, dependencies).default
}
const actions = moduleFrom(read('src/views/dcc/controlled-file/workflow/workflow-actions.ts'))
const { FileSelectorState } = moduleFrom(read('src/views/dcc/controlled-file/relations/selector-state.ts'))
const MAIN = '2054545668044084249', REL1 = '2054545668044084243', REL2 = '2054545668044084248'
const PROJECT = '310', ASSIGNEE = '910513', TASK = '3481664b-cb00-11f1-ad52-b082e25ec548'
const ROUND = '1842903f-cb00-11f1-ad52-b082e25ec548'
const rel1 = Object.freeze({ tenantId: '1', controlledFileId: REL1, masterId: '101', projectId: PROJECT,
  fileName: 'DCC241016C22Rel1-A.pdf', fileNumber: 'DCC241016C22REL1', versionNo: 'A/1',
  projectName: 'DCC241016C22Source', folderName: 'DCC241016C22F1', status: 'ACTIVE',
  controlled: true, pendingEffect: false, executable: true, canPreview: true })
const rel2 = Object.freeze({ ...rel1, controlledFileId: REL2, masterId: '102',
  fileName: 'DCC241016C22Rel2-A.pdf', fileNumber: 'DCC241016C22REL2' })
const remediation = Object.freeze({ relatedMasterId: rel1.masterId, assigneeUserId: ASSIGNEE, dueAt: '2026-12-01 12:00:00' })
const drain = async () => { for (let i = 0; i < 20; i++) { await Promise.resolve(); await vue.nextTick() } }

function renderer() {
  const remove = node => { if (node.parent) { const at = node.parent.children.indexOf(node); if (at >= 0) node.parent.children.splice(at, 1) } }
  return vue.createRenderer({
    createElement: type => ({ type, props: {}, children: [] }), createText: text => ({ text }), createComment: () => ({ comment: true }),
    insert(node, parent, anchor) { remove(node); parent.children ||= []; const at = parent.children.indexOf(anchor); parent.children.splice(at < 0 ? parent.children.length : at, 0, node); node.parent = parent },
    remove, setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text; node.children = [] },
    patchProp(node, key, before, value) { node.props[key] = value }, parentNode: node => node.parent,
    nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1]
  })
}
const text = node => (node.text || '') + (node.children || []).map(text).join('')
const find = (node, predicate) => [...(predicate(node) ? [node] : []), ...(node.children || []).flatMap(child => find(child, predicate))]
const control = (tag, props) => ({ inheritAttrs: false, props,
  emits: ['update:modelValue'], setup(value, { attrs, emit, slots }) {
    return () => vue.h(tag, { ...attrs, placeholder: value.placeholder, disabled: value.disabled, type: value.type, value: value.modelValue,
      onInput: next => emit('update:modelValue', next), onChange: next => emit('update:modelValue', next) }, slots.default?.())
  } })

function fixture(processDefinitionKey = 'dcc-controlled-file-upload') {
  const writes = [], selectorQueries = [], selectorErrors = [], selectors = [], history = [rel1]
  let choice = rel2
  const context = { controlledFileId: MAIN, processInstanceId: ROUND, taskId: TASK,
    obligationId: MAIN + ':MATRIX_REVIEW:910379', departmentId: '910379', departmentName: 'DCC241016C22Dept',
    assigned: false, canAssign: true, processDefinitionKey, assigneeOptions: [{ id: ASSIGNEE, name: 'DCC241016C22C' }] }
  const metadata = { controlledFileId: MAIN, tenantId: '1', masterId: '103', projectId: PROJECT,
    projectName: rel1.projectName, projectFolderId: '51', projectFolderName: rel1.folderName,
    fileName: 'DCC241016C22Main-A.pdf', fileNumber: 'DCC241016C22MAIN', versionNo: 'A/1',
    status: 'PENDING_MATRIX_REVIEW', controlled: false, pendingEffect: false, executable: false,
    canEdit: false, canPreview: true, hasCurrentControlledSource: false }
  const loadPage = async (tenant, query) => { selectorQueries.push({ tenant, ...plain(query) }); return { list: [rel2], total: 1 } }

  // Keep the established selector's public boundary and real selection rules; Element Plus is outside this SFC unit.
  const Selector = { props: ['modelValue', 'source', 'purpose', 'selected', 'directories', 'loadPage', 'persist', 'openPreview'],
    emits: ['update:modelValue', 'confirmed'], setup(props, { emit }) {
      const selection = new FileSelectorState({ contextKey: 'not-open', tenantId: '1' })
      selectors.push(props)
      vue.watch(() => props.modelValue, async open => {
        if (!open) return
        selection.reset({ ...props.source, relationProjectId: props.purpose === 'relations' ? props.source.projectId : undefined,
          forbidSelfRelation: props.purpose === 'relations', allowEmptySelection: props.purpose === 'relations' }, props.selected || [])
        await props.loadPage({ projectId: props.source.projectId, folderId: props.source.folderId, keyword: 'DCC241016C22Rel2', pageNo: 1, pageSize: 20 })
      }, { immediate: true })
      const choose = () => { try { selection.select(choice) } catch (error) { selectorErrors.push(error.message) } }
      const confirm = async () => {
        const saved = await selection.save(props.persist, true)
        if (!saved) { selectorErrors.push(selection.error); return }
        emit('confirmed', selection.selected.map(row => ({ ...row }))); emit('update:modelValue', false)
      }
      return () => props.modelValue ? vue.h('section', { 'data-unit-selector': true }, [
        vue.h('button', { onClick: choose }, 'unit:选择候选'),
        vue.h('button', { onClick: () => selection.selected.slice().forEach(row => selection.remove(row.masterId)) }, 'unit:清空候选'),
        vue.h('button', { onClick: confirm }, 'unit:确认候选'),
        vue.h('button', { onClick: () => emit('update:modelValue', false) }, 'unit:取消候选')
      ]) : null
    } }
  const Signing = component('src/views/dcc/controlled-file/workflow/SignoffAssignmentPanel.vue', { './workflow-actions': actions })
  const Arrangements = component('src/views/dcc/controlled-file/detail/DetailRelationArrangements.vue', {
    '../shared/version-display': { getDccVersionDisplayText: value => value }, '../workflow/workflow-actions': actions })
  const lifecycle = moduleFrom(read('src/api/dcc/controlledFile/workflowLifecycle.ts'), {
    '@/config/axios': { default: { post: async request => { writes.push(plain(request)); return true } } } })
  const forbiddenWrite = () => { throw Error('Relations must be saved in the existing signed assignment, not a separate relation write') }
  const Detail = component('src/views/dcc/controlled-file/detail/DetailSignoffAssignment.vue', {
    '@/api/dcc/controlledFile/applicationRead': { getSignoffAssignmentContext: async () => context,
      getControlledFileRelationPermissions: async () => metadata, loadDccSelectorPage: loadPage },
    '@/api/dcc/controlledFile/workflowLifecycle': lifecycle,
    '@/api/dcc/controlledFile/relations': { listHistoricalRelations: async () => history,
      listRelationArrangements: async () => processDefinitionKey === 'dcc-controlled-file-revision' ? [remediation] : [],
      loadSelectorBrowserPage: loadPage, replaceCurrentRelations: forbiddenWrite },
    '@/api/dcc/controlledFile/projectAttributes': { getProjectFolders: async () => [
      { id: '51', projectCodeId: PROJECT, parentId: '0', name: rel1.folderName, active: true, sortOrder: 0 }] },
    '@/api/system/user': { getSimpleUserList: async () => [{ id: ASSIGNEE, nickname: 'DCC241016C22C' }] },
    '@/utils/auth': { getTenantId: () => 1, getVisitTenantId: () => undefined },
    '../workflow/SignoffAssignmentPanel.vue': { default: Signing },
    './DetailRelationArrangements.vue': { default: Arrangements },
    '../relations/DccFileSelector.vue': { default: Selector },
    '../relations/selector-state': { FileSelectorState },
    '../relations/project-reference-contract': moduleFrom(read('src/views/dcc/controlled-file/relations/project-reference-contract.ts')),
    '../basic-data/components/project-folder-tree': moduleFrom(read('src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts')),
    '../workflow/workflow-actions': actions
  })
  const host = { children: [] }, app = renderer().createApp(Detail, { fileId: MAIN, processInstanceId: ROUND, taskId: TASK })
  app.directive('loading', {})
  app.component('ContentWrap', { setup: (props, { slots }) => () => vue.h('section', slots.default?.()) })
  app.component('ElButton', { inheritAttrs: false, props: ['disabled', 'loading'], setup: (props, { attrs, slots }) =>
    () => vue.h('button', { ...attrs, disabled: props.disabled || props.loading }, slots.default?.()) })
  app.component('ElSelect', control('select', ['modelValue', 'placeholder', 'disabled']))
  app.component('ElInput', control('input', ['modelValue', 'placeholder', 'disabled', 'type']))
  app.component('ElOption', { props: ['label'], setup: props => () => vue.h('option', props.label) })
  app.component('ElAlert', { props: ['title'], setup: props => () => vue.h('aside', props.title) })
  app.component('ElTag', { setup: (props, { slots }) => () => vue.h('span', slots.default?.()) })
  app.component('ElTable', { setup: () => () => vue.h('table') })
  app.component('ElTableColumn', { setup: () => () => vue.h('td') })
  app.component('ElCheckbox', control('input', ['modelValue', 'disabled']))
  app.component('ElDatePicker', control('input', ['modelValue', 'placeholder', 'disabled']))
  app.mount(host)
  const one = predicate => { const rows = find(host, predicate); assert.equal(rows.length, 1, 'Expected one rendered control: ' + rows.map(text)); return rows[0] }
  const click = async label => { const node = one(node => node.type === 'button' && (label instanceof RegExp ? label.test(text(node)) : text(node) === label)); assert.notEqual(node.props.disabled, true); await node.props.onClick(); await drain() }
  const input = async (placeholder, value) => { one(node => ['select', 'input'].includes(node.type) && node.props.placeholder === placeholder).props.onInput(value); await drain() }
  const open = () => click(/补充.*关联|后补.*关联/)
  const sign = async () => {
    await input('搜索并选择本部门会签人', ASSIGNEE)
    await input('指派意见', 'DCC241016C22 本人签名指派')
    await input('当前账号签名密码', 'unit-only')
    await click('签名确认指派')
  }
  return { app, host, writes, selectorQueries, selectorErrors, selectors, history, context, click, open, sign,
    choose: row => { choice = row }, async selectRel2() { await open(); await click('unit:选择候选'); await click('unit:确认候选') } }
}
function assertSigned(h, extras, revision = false) {
  assert.equal(h.writes.length, 1, 'The real assignment click must reach the existing lifecycle request once')
  const request = h.writes[0], payload = request.data
  assert.equal(request.url, `/dcc/controlled-file/workflow-lifecycle/${MAIN}/assign-signoff`)
  assert.equal(payload.taskId, TASK); assert.equal(payload.assigneeUserId, ASSIGNEE)
  assert.equal(payload.password, 'unit-only'); assert.equal(payload.reason, 'DCC241016C22 本人签名指派')
  assert.deepEqual(payload.supplementalRelatedControlledFileIds || [], extras)
  assert.deepEqual(h.history, [rel1], 'Original Rel1 fixed selection must remain unchanged')
  assert.equal(payload.supplementalRelatedControlledFileIds?.includes(REL1) || false, false)
  if (revision) assert.deepEqual(payload.relationArrangements, [remediation])
  else assert.equal(Object.hasOwn(payload, 'relationArrangements'), false)
}

for (const process of ['dcc-controlled-file-upload', 'dcc-controlled-file-revision']) {
  test(`R22-01 actual ${process} selection and signature handler carry fixed Rel2 without losing original/signature/remediation facts`, async () => {
    const h = fixture(process)
    try {
      await drain(); await h.selectRel2()
      assert.deepEqual(h.selectorErrors, [])
      await h.sign(); assertSigned(h, [REL2], process.endsWith('revision'))
      assert.equal(h.selectors.at(-1).purpose, 'relations')
      assert.equal(h.selectors.at(-1).source.controlledFileId, MAIN)
      assert.equal(h.selectors.at(-1).source.projectId, PROJECT)
      assert.ok(h.selectorQueries.length > 0)
      assert.ok(h.selectorQueries.every(query => query.projectId === PROJECT))
    } finally { h.app.unmount() }
  })
}
test('R22-01 no selection preserves the original upload/revision/obsolete signature contracts', async () => {
  for (const process of ['dcc-controlled-file-upload', 'dcc-controlled-file-revision', 'dcc-controlled-file-obsolete']) {
    const h = fixture(process)
    try { await drain(); await h.sign(); assertSigned(h, [], process.endsWith('revision')) }
    finally { h.app.unmount() }
  }
})
test('R22-01 cancelling a provisional clear preserves the previously confirmed Rel2 selection', async () => {
  const h = fixture()
  try {
    await drain(); await h.selectRel2(); await h.open()
    await h.click('unit:清空候选'); await h.click('unit:取消候选')
    await h.sign(); assertSigned(h, [REL2])
  } finally { h.app.unmount() }
})
test('R22-01 explicitly confirming an empty supplemental selection clears only Rel2', async () => {
  const h = fixture()
  try {
    await drain(); await h.selectRel2(); await h.open()
    await h.click('unit:清空候选'); await h.click('unit:确认候选')
    assert.deepEqual(h.selectorErrors, [])
    await h.sign(); assertSigned(h, [])
  } finally { h.app.unmount() }
})
test('R22-01 the real parent wires the existing relations selector to its exact project and refuses foreign choices', async () => {
  const h = fixture()
  try {
    await drain(); await h.open()
    assert.equal(h.selectors.at(-1).purpose, 'relations')
    assert.equal(h.selectors.at(-1).source.projectId, PROJECT)
    h.choose({ ...rel2, controlledFileId: '2054545668044084250', masterId: '104', projectId: '312' })
    await h.click('unit:选择候选')
    assert.match(h.selectorErrors.at(-1), /PROJECT_MISMATCH/)
    assert.equal(h.writes.length, 0)
    await h.click('unit:取消候选'); await h.sign(); assertSigned(h, [])
  } finally { h.app.unmount() }
})
