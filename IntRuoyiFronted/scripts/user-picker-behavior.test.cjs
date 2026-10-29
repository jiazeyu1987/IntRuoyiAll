const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const root = path.resolve(__dirname, '..')
const ts = require(path.join(root, 'node_modules/typescript'))
const vue = require(path.join(root, 'node_modules/vue'))
const compiler = require(
  path.join(root, 'node_modules/.pnpm/@vue+compiler-sfc@3.5.12/node_modules/@vue/compiler-sfc')
)
const dir = path.join(root, 'src/views/system/user/components')
function evaluate(source, dependencies) {
  const module = { exports: {} }
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code, {
    module,
    exports: module.exports,
    require: (id) => dependencies[id] || {},
    ...vue,
    Error,
    useMessage: () => ({ warning: (text) => warnings.push(text) }),
    console
  })
  return module.exports
}
const core = evaluate(fs.readFileSync(path.join(dir, 'userPicker.ts'), 'utf8'), {})
const warnings = []
let globalCalls = 0
const api = {
  getSelectionPage: async () => {
    globalCalls++
    return { list: [], total: 0 }
  },
  getSelectionList: async () => {
    globalCalls++
    return []
  }
}
const filename = path.join(dir, 'UserSelectDialogV2.vue')
const parsed = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
const compiled = compiler.compileScript(parsed.descriptor, {
  id: 'test',
  fs: { fileExists: fs.existsSync, readFile: (f) => fs.readFileSync(f, 'utf8') }
})
const component = evaluate(compiled.content, {
  vue,
  './userPicker': core,
  '@/api/system/user': api,
  '@/utils/tree': { handleTree: (rows) => rows },
  '@/directives/permission/hasPermi': { hasPermission: () => false },
  '@/hooks/web/useUserTableColumns': { useUserTableColumns: () => ({}) },
  '@/store/modules/user': { useUserStoreWithOut: () => ({ getUser: { id: 1 } }) }
}).default
function create(extra = {}) {
  const props = vue.reactive({ multiple: true, min: 0, userOptions: [], ...extra })
  const events = []
  const state = component.setup(props, { expose() {}, emit: (...args) => events.push(args) })
  return { props, state, events }
}
;(async () => {
  const maxLong = '9223372036854775807'
  assert.equal(core.normalizeOptions([{ id: maxLong }])[0].id, maxLong)
  assert.throws(() => core.normalizeOptions([{ id: Number(maxLong) }]))
  assert.throws(() => core.normalizeOptions([{ id: 1 }, { id: '1' }]))
  assert.deepEqual(
    Array.from(
      core.departmentIds(1, true, [
        { id: 2, parentId: 1 },
        { id: 3, parentId: 2 }
      ])
    ),
    [1, 2, 3]
  )
  const rows = Array.from({ length: 24 }, (_, i) => ({
    id: i + 1,
    nickname: `用户${i + 1}`,
    deptId: i < 20 ? 1 : 2,
    status: 0
  }))
  const { state, props, events } = create({ userOptions: rows, deptId: 1, max: 2 })
  state.queryParams.pageSize = 10
  await state.open([15])
  assert.equal(state.selected.value[0].id, 15, 'off-page preselection retained')
  state.toggle(rows[0])
  assert.equal(state.selected.value.length, 2)
  state.toggle(rows[1])
  assert.equal(state.selected.value.length, 2, 'max enforced')
  state.queryParams.pageNo = 2
  await state.getList()
  state.remove(15)
  await state.getList()
  assert.equal(state.isSelected(15), false, 'removed initial selection must not reappear')
  await state.resetQuery()
  assert.equal(state.total.value, 20, 'reset preserves fixed scope')
  props.userOptions = [{ ...rows[0], disabled: true }]
  await vue.nextTick()
  await vue.nextTick()
  state.confirm()
  assert.equal(
    events.filter((e) => e[0] === 'selected').length,
    0,
    'latest disabled candidate rejected'
  )
  state.remove(1)
  state.confirm()
  assert.equal(events.filter((e) => e[0] === 'selected').length, 1, 'empty allowed when min zero')
  const missing = create({ userOptions: rows })
  await missing.state.open([maxLong])
  assert.equal(missing.state.error.value, '')
  missing.state.remove(maxLong)
  missing.state.toggle(rows[0])
  missing.state.confirm()
  assert.equal(
    missing.events.filter((e) => e[0] === 'selected').length,
    1,
    'invalid old selection can be removed'
  )
  const fail = create({
    loadUserOptions: async () => {
      throw new Error('业务候选失败')
    }
  })
  await fail.state.open()
  assert.equal(fail.state.error.value, '业务候选失败')
  fail.state.confirm()
  assert.equal(fail.events.filter((e) => e[0] === 'selected').length, 0)
  assert.equal(globalCalls, 0, 'business empty/error never requests global candidates')
  const relationshipOnly = create({ allowRelationships: true, relationships: ['DIRECT_SUPERVISOR'], min: 1 })
  await relationshipOnly.state.open()
  relationshipOnly.state.confirm()
  const relationshipEvent = relationshipOnly.events.find(event => event[0] === 'selected')
  assert.equal(relationshipEvent[1].length, 0, 'dynamic relationship is never emitted as a user ID')
  assert.deepEqual(Array.from(relationshipEvent[3]), ['DIRECT_SUPERVISOR'])
  relationshipOnly.state.chosenRelationships.value.push('DEPT_LEADER')
  assert.equal(relationshipOnly.props.relationships.length, 1, 'editing relationships does not mutate bound configuration before confirmation')
  await relationshipOnly.state.open()
  assert.equal(relationshipOnly.state.chosenRelationships.value.length, 1, 'reopen discards cancelled relationship edits')
  const relationLimit = create({ allowRelationships: true, relationships: ['DIRECT_SUPERVISOR', 'DEPT_LEADER'], max: 1 })
  await relationLimit.state.open()
  relationLimit.state.confirm()
  assert.equal(relationLimit.events.filter(event => event[0] === 'selected').length, 0, 'combined selection limit includes relationships')
  const fixedPeopleOnly = create({ relationships: ['DIRECT_SUPERVISOR'] })
  await fixedPeopleOnly.state.open()
  assert.equal(fixedPeopleOnly.state.chosenRelationships.value.length, 0, 'ordinary account fields do not enable relationship rules')
  const constrained = create({ userOptions: [{ id: 1, roleIds: [10], postIds: [20] }, { id: 2, roleIds: [10], postIds: [30] }], scopeRoleId: 10, scopePostId: 20, min: 1 })
  await constrained.state.open()
  assert.equal(constrained.state.total.value, 1)
  await constrained.state.resetQuery()
  assert.equal(constrained.state.total.value, 1, 'role/post scope survives reset')
  constrained.state.confirm()
  assert.equal(constrained.events.filter(e => e[0] === 'selected').length, 0, 'min enforced')
  const excluded = create({ userOptions: rows, excludeCurrentUser: true, disabledIds: [2] })
  await excluded.state.open()
  excluded.state.toggle(rows[0]); excluded.state.toggle(rows[1])
  assert.equal(excluded.state.selected.value.length, 0)
  let resolveFirst, resolveSecond
  let loads = 0
  const race = create({ loadUserOptions: () => new Promise(resolve => { if (++loads === 1) resolveFirst = resolve; else resolveSecond = resolve }) })
  const first = race.state.open()
  const second = race.state.open()
  resolveSecond([{ id: 2 }]); await second
  resolveFirst([{ id: 1 }]); await first
  assert.equal(race.state.candidates.value[0].id, 2, 'late request cannot replace current candidates')
  const directDepts = create({ userOptions: [{ id: 1, deptId: 10, deptName: '生产部' }, { id: 2, deptId: 20, deptName: '质量部' }] })
  await directDepts.state.open()
  assert.equal(directDepts.state.departments.value.length, 2)
  assert.equal(directDepts.state.queryParams.includeChildDepts, false, 'flat candidate metadata does not claim child hierarchy')
  directDepts.state.queryParams.deptIds = [10]; await directDepts.state.getList()
  assert.equal(directDepts.state.total.value, 1)
  directDepts.state.queryParams.deptIds = [10, 20]; await directDepts.state.getList()
  assert.equal(directDepts.state.total.value, 2, 'selected departments are combined as a union')
  const departmentUnion = create({ userOptions: [
    { id: 1, deptId: 10 }, { id: 2, deptId: 11 },
    { id: 3, deptId: 20 }, { id: 4, deptId: 30 }
  ], departmentOptions: [{ id: 10, name: 'A' }, { id: 11, parentId: 10, name: 'A1' }, { id: 20, name: 'B' }, { id: 30, name: 'C' }] })
  await departmentUnion.state.open([1])
  departmentUnion.state.queryParams.deptIds = [10, 11, 20]
  await departmentUnion.state.getList()
  assert.equal(departmentUnion.state.total.value, 3, 'overlapping parent/child selections do not duplicate users')
  departmentUnion.state.queryParams.includeChildDepts = false
  departmentUnion.state.queryParams.deptIds = [10, 20]
  await departmentUnion.state.getList()
  assert.equal(departmentUnion.state.total.value, 2, 'exact department union excludes unselected children')
  assert.equal(departmentUnion.state.selected.value[0].id, 1, 'department filtering keeps selected users')
  const fixedDepartments = create({ userOptions: [{ id: 1, deptId: 10 }, { id: 2, deptId: 20 }], deptId: 10 })
  await fixedDepartments.state.open()
  fixedDepartments.state.queryParams.deptIds = [10, 20]
  await fixedDepartments.state.getList()
  assert.equal(fixedDepartments.state.total.value, 1, 'multiple department filters cannot widen a fixed department scope')
  fixedDepartments.state.queryParams.deptIds = []
  await fixedDepartments.state.getList()
  assert.equal(fixedDepartments.state.total.value, 1, 'clearing department filter keeps fixed scope')
  const departmentalPeople = [
    { id: 100, nickname: '共同主管', deptId: 99 },
    { id: 101, nickname: '甲部门组员', deptId: 10 },
    { id: 102, nickname: '乙部门组员', deptId: 20 },
    { id: 103, nickname: '丙部门组员', deptId: 30 },
    { id: 104, nickname: '停用组员', deptId: 10, status: 1 },
    { id: 105, nickname: '禁选组员', deptId: 20, disabled: true }
  ]
  const leaderMetadata = [
    { id: 10, name: '甲', leaderUserId: 100 },
    { id: 20, name: '乙', leaderUserId: 100 },
    { id: 30, name: '丙', leaderUserId: null }
  ]
  const personnel = create({ userOptions: departmentalPeople, departmentOptions: leaderMetadata })
  await personnel.state.open()
  personnel.state.queryParams.deptIds = [10, 20, 30]
  personnel.state.queryParams.departmentMemberType = 'LEADER'
  await personnel.state.getList()
  assert.deepEqual(Array.from(personnel.state.list.value, row => row.id), [100], 'shared cross-department leader appears once')
  await personnel.state.selectAllFiltered()
  assert.deepEqual(Array.from(personnel.state.selected.value, row => row.id), [100], 'bulk leader selection uses formal department assignments')
  personnel.state.queryParams.departmentMemberType = 'MEMBER'
  await personnel.state.getList()
  await personnel.state.selectAllFiltered()
  assert.deepEqual(Array.from(personnel.state.selected.value, row => row.id), [100, 101, 102, 103], 'bulk members preserve earlier selections and exclude inactive/disabled people')
  personnel.state.queryParams.departmentMemberType = 'ALL'
  await personnel.state.getList()
  assert.equal(personnel.state.total.value, 4, 'all mode retains existing home department filtering and status constraint')
  personnel.state.queryParams.deptIds = [30]
  personnel.state.queryParams.departmentMemberType = 'LEADER'
  await personnel.state.getList()
  assert.equal(personnel.state.total.value, 0, 'department with no configured leader has no invented candidates')
  const limitedBulk = create({ userOptions: departmentalPeople, departmentOptions: leaderMetadata, max: 1 })
  await limitedBulk.state.open([100])
  limitedBulk.state.queryParams.deptIds = [10, 20]
  await limitedBulk.state.selectAllFiltered()
  assert.deepEqual(Array.from(limitedBulk.state.selected.value, row => row.id), [100], 'bulk selection exceeding max must not partly modify selection')
  const unavailableLeaders = create({ userOptions: [{ id: 1, deptId: 10, deptName: '部门' }] })
  await unavailableLeaders.state.open()
  unavailableLeaders.state.queryParams.deptIds = [10]
  unavailableLeaders.state.queryParams.departmentMemberType = 'MEMBER'
  await unavailableLeaders.state.getList()
  assert.ok(unavailableLeaders.state.error.value.includes('未提供部门主管配置'), 'missing formal supervisor metadata cannot silently become all members')
  const bulkRequests = []
  api.getSelectionPage = async query => {
    bulkRequests.push({ ...query })
    return { total: 201, list: query.pageNo === 1
      ? Array.from({ length: 200 }, (_, i) => ({ id: i + 1 })) : [{ id: 201 }] }
  }
  const remoteBulk = create({ userOptions: undefined })
  remoteBulk.state.queryParams.deptIds = [10, 20, 30]
  remoteBulk.state.queryParams.departmentMemberType = 'LEADER'
  await remoteBulk.state.selectAllFiltered()
  assert.equal(remoteBulk.state.selected.value.length, 201, 'remote bulk selection includes all pages')
  assert.equal(bulkRequests.length, 2)
  assert.ok(bulkRequests.every(q => q.deptLeaderMode === 'LEADER' && q.deptIds.length === 3), 'every bulk request keeps selected department and personnel constraints')
  api.getSelectionPage = async () => { throw new Error('真实查询失败') }
  await remoteBulk.state.selectAllFiltered()
  assert.equal(remoteBulk.state.selected.value.length, 201, 'failed bulk request does not partially change previous selection')
  const changedScope = create({ userOptions: [{ id: 1, roleIds: [10], postIds: [20] }], scopeRoleId: 10, scopePostId: 20 })
  await changedScope.state.open([1])
  changedScope.props.userOptions = [{ id: 1, roleIds: [30], postIds: [20] }]
  await vue.nextTick(); await vue.nextTick()
  changedScope.state.confirm()
  assert.equal(changedScope.events.filter(event => event[0] === 'selected').length, 0, 'latest candidate outside fixed role scope cannot submit old seed')
  const invalidLimit = create({ min: 2, max: 1 })
  await invalidLimit.state.open(); assert.ok(invalidLimit.state.error.value.includes('最少数量'))
  const batchCalls = []
  const actualApi = evaluate(fs.readFileSync(path.join(root, 'src/api/system/user/index.ts'), 'utf8'), {
    '@/config/axios': { default: { get: async request => { batchCalls.push(request.params); return request.params.ids.split(',').map(id => ({ id })) } } }
  })
  const manyIds = Array.from({ length: 405 }, (_, i) => String(i + 1))
  const allRows = await actualApi.getSelectionList(manyIds, { scopeRoleId: 10, scopePostId: 20, scopeDeptId: 30 })
  assert.equal(allRows.length, 405)
  assert.deepEqual(batchCalls.map(call => call.ids.split(',').length), [200, 200, 5])
  assert.ok(batchCalls.every(call => call.scopeRoleId === 10 && call.scopePostId === 20 && call.scopeDeptId === 30))

  // Mount the real Element Plus form-disabled hook under a provided disabled form.
  const element = require(path.join(root, 'node_modules/element-plus'))
  const inputFilename = path.join(dir, 'UserSelectV2.vue')
  const inputParsed = compiler.parse(fs.readFileSync(inputFilename, 'utf8'), { filename: inputFilename })
  const inputScript = compiler.compileScript(inputParsed.descriptor, { id: 'input-test', fs: { fileExists: fs.existsSync, readFile: f => fs.readFileSync(f, 'utf8') } })
  const inputComponent = evaluate(inputScript.content, { vue, 'element-plus': element, './userPicker': core, '@/api/system/user': api, '@/store/modules/user': { useUserStoreWithOut: () => ({ getUser: { id: 1 } }) } }).default
  const form = vue.reactive({ disabled: true })
  let inputState
  const inputEvents = []
  const inputProps = vue.reactive({ modelValue: 1, clearable: false, userOptions: [{ id: 1, nickname: '员工' }] })
  const renderer = vue.createRenderer({ insert() {}, remove() {}, createElement: () => ({}), createText: () => ({}), createComment: () => ({}), setText() {}, setElementText() {}, parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const child = { ...inputComponent, setup(props, context) { inputState = inputComponent.setup(props, { ...context, emit: (...args) => inputEvents.push(args) }); return () => null } }
  const app = renderer.createApp({ setup() { vue.provide(element.formContextKey, form); return () => vue.h(child, inputProps) } })
  app.mount({})
  let opened = 0
  inputState.dialog.value = { open: async () => { opened++ } }
  await inputState.open(); inputState.clear(); inputState.onSelected([{ id: 2 }])
  assert.equal(opened, 0, 'disabled ancestor blocks opening')
  assert.equal(inputEvents.length, 0, 'disabled ancestor blocks clear and late confirmation')
  form.disabled = false; await vue.nextTick()
  assert.equal(inputState.pickerProps.value.min, 1, 'clearable false requires at least one in dialog')
  inputState.clear(); inputState.onSelected([])
  assert.equal(inputEvents.length, 0, 'clearable false cannot be bypassed via empty dialog confirmation')
  await inputState.open(); assert.equal(opened, 1)
  inputState.onSelected([{ id: 2 }])
  assert.ok(inputEvents.some(event => event[0] === 'update:modelValue' && event[1] === 2), 'nonclearable permits replacement')
  inputProps.allowRelationships = true
  inputProps.relationships = ['DIRECT_SUPERVISOR']
  await vue.nextTick()
  assert.ok(inputState.label.value.includes('申请人的直接主管'), 'input displays the relationship without resolving it to a fixed name')
  inputState.onSelected([], undefined, ['DEPT_LEADER'])
  const relationSelection = inputEvents.filter(event => event[0] === 'selection-change').at(-1)[1]
  assert.equal(relationSelection.users.length, 0)
  assert.deepEqual(Array.from(relationSelection.relationships), ['DEPT_LEADER'])
  app.unmount()
  const inactiveRow = { id: 9, nickname: '历史员工', status: 1 }
  const activeOnly = create({ userOptions: [inactiveRow] })
  await activeOnly.state.open(); activeOnly.state.toggle(inactiveRow)
  assert.equal(activeOnly.state.selected.value.length, 0, 'active business selection rejects inactive accounts by default')
  const historicalQuery = create({ userOptions: [inactiveRow], allowInactive: true })
  await historicalQuery.state.open()
  historicalQuery.state.queryParams.status = 1; await historicalQuery.state.getList()
  historicalQuery.state.toggle(inactiveRow); historicalQuery.state.confirm()
  assert.equal(historicalQuery.events.filter(event => event[0] === 'selected').length, 1, 'explicit historical query permits known inactive account')
  const missingHistorical = create({ userOptions: [], allowInactive: true })
  await missingHistorical.state.open([9]); missingHistorical.state.confirm()
  assert.equal(missingHistorical.events.filter(event => event[0] === 'selected').length, 0, 'allowInactive never enables invalid or removed identities')
  // Render both real SFCs together: parent input value must never override dialog visibility.
  const renderSfc = (file, deps) => {
    const descriptor = compiler.parse(fs.readFileSync(file, 'utf8'), { filename: file }).descriptor
    const script = compiler.compileScript(descriptor, { id: 'mounted-picker', inlineTemplate: true, fs: { fileExists: fs.existsSync, readFile: f => fs.readFileSync(f, 'utf8') } })
    return evaluate(script.content, { vue, ...deps }).default
  }
  const renderedDialog = renderSfc(filename, {
    './userPicker': core, '@/api/system/user': api,
    '@/utils/tree': { handleTree: rows => rows },
    '@/directives/permission/hasPermi': { hasPermission: () => false },
    '@/hooks/web/useUserTableColumns': { useUserTableColumns: () => ({}) },
    '@/store/modules/user': { useUserStoreWithOut: () => ({ getUser: { id: 1 } }) }
  })
  const renderedInput = renderSfc(inputFilename, {
    'element-plus': element, './userPicker': core, '@/api/system/user': api,
    './UserSelectDialogV2.vue': { default: renderedDialog },
    '@/store/modules/user': { useUserStoreWithOut: () => ({ getUser: { id: 1 } }) }
  })
  const mountedNodes = []
  const clickRenderer = vue.createRenderer({
    insert(node, parent, anchor) { parent.children ??= []; const index = anchor ? parent.children.indexOf(anchor) : -1; parent.children.splice(index < 0 ? parent.children.length : index, 0, node); node.parent = parent },
    remove(node) { const children = node.parent?.children; if (children) children.splice(children.indexOf(node), 1) },
    createElement: tag => { const node = { tag, props: {} }; mountedNodes.push(node); return node },
    createText: () => ({}), createComment: () => ({}), setText() {}, setElementText() {}, parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1],
    patchProp: (node, key, _old, value) => { node.props[key] = value }
  })
  let visibleDialogValue
  const dialogShell = { props: { modelValue: Boolean }, setup(props) { vue.watchEffect(() => { visibleDialogValue = props.modelValue }); return () => null } }
  for (const value of [undefined, 1]) {
    mountedNodes.length = 0
    const mountedApp = clickRenderer.createApp(renderedInput, { modelValue: value, userOptions: [{ id: 1, nickname: '员工' }] })
    mountedApp.component('Dialog', dialogShell)
    for (const name of ['el-alert', 'el-form-item', 'el-tree-select', 'el-checkbox', 'el-checkbox-group', 'el-tabs', 'el-tab-pane', 'el-option', 'el-select', 'el-button', 'el-tag', 'el-radio', 'el-radio-button', 'el-radio-group', 'el-table-column', 'el-table']) mountedApp.component(name, { render: () => null })
    mountedApp.directive('loading', {})
    mountedApp.component('ElInput', { props: ['modelValue'], setup() { return () => vue.h('input') } })
    mountedApp.component('ElIcon', { setup() { return () => null } })
    mountedApp.mount({})
    assert.equal(visibleDialogValue, false)
    const clickable = mountedNodes.find(node => node.tag === 'div' && node.props.onClick)
    assert.ok(clickable, 'rendered input wrapper has a click handler')
    await clickable.props.onClick({})
    await vue.nextTick()
    assert.equal(visibleDialogValue, true, 'click opens actual child Dialog for empty and preselected input values')
    mountedApp.unmount()
  }
  console.log(
    'PASS: unified user picker identity, scope, cross-page selection, limits, invalidation and failure behavior'
  )
})().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
