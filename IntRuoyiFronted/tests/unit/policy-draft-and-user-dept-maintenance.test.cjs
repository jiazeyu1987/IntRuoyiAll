const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

function loadFunctions(file, names, bindings) {
  const filename = path.resolve(__dirname, '../../src', file)
  const source = fs.readFileSync(filename, 'utf8')
  const { descriptor, errors } = parse(source, { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: file })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: file,
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const ast = ts.createSourceFile(filename, descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const declarations = ast.statements.filter(ts.isVariableStatement).flatMap(s => [...s.declarationList.declarations])
  const functions = names.map(name => {
    const node = declarations.find(d => d.name.getText(ast) === name)
    assert.ok(node, name)
    return `const ${node.getText(ast)}; globalThis.${name} = ${name};`
  }).join('\n')
  const context = vm.createContext(bindings)
  vm.runInContext(ts.transpileModule(functions, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context
}

function deletionContext(confirm, remove) {
  const events = []
  const bindings = {
    message: { confirm, success: value => events.push(value), error: value => events.push(value) },
    BusinessApprovalPolicyApi: { deleteDraftPolicy: remove },
    deletingPolicyId: { value: undefined }, list: { value: [{ id: 12 }] },
    queryParams: { pageNo: 2 }, getList: async () => events.push('refresh'),
    resolveBusinessApprovalPolicyErrorMessage: e => e.message
  }
  return { context: loadFunctions('views/bpm/businessApprovalPolicy/index.vue', ['deleteDraftPolicy'], bindings), events }
}

test('取消清理不调用删除接口，不修改分页', async () => {
  let calls = 0
  const { context } = deletionContext(async () => { throw 'cancel' }, async () => { calls++ })
  await context.deleteDraftPolicy(12)
  assert.equal(calls, 0)
  assert.equal(context.queryParams.pageNo, 2)
})

test('删除当前页最后一条草稿后回到上一页刷新，结束加载状态', async () => {
  const ids = []
  const { context, events } = deletionContext(async () => {}, async id => ids.push(id))
  await context.deleteDraftPolicy(12)
  assert.deepEqual(ids, [12])
  assert.equal(context.queryParams.pageNo, 1)
  assert.equal(context.deletingPolicyId.value, undefined)
  assert.deepEqual(events, ['草稿已删除', 'refresh'])
})

test('删除失败准确报错，不提示成功或修改分页', async () => {
  const failure = new Error('仅允许删除草稿策略')
  const { context, events } = deletionContext(async () => {}, async () => { throw failure })
  await assert.rejects(context.deleteDraftPolicy(12), failure)
  assert.equal(context.queryParams.pageNo, 2)
  assert.equal(context.deletingPolicyId.value, undefined)
  assert.deepEqual(events, [failure.message])
})

test('选中部门可直接编辑，新增以所选部门为上级；未选中不打开编辑', () => {
  const calls = []
  const context = loadFunctions('views/system/user/index.vue', ['openDeptForm'], {
    selectedDeptId: { value: undefined }, deptFormRef: { value: { open: (...args) => calls.push(args) } }
  })
  context.openDeptForm('update')
  assert.equal(calls.length, 0)
  context.selectedDeptId.value = 7
  context.openDeptForm('create')
  context.openDeptForm('update')
  assert.deepEqual(calls, [['create', undefined, 7], ['update', 7, undefined]])
})

test('部门维护保存后刷新组织及用户，所选部门停用后清空失效筛选', async () => {
  const calls = []
  const context = loadFunctions('views/system/user/index.vue', ['refreshAfterDeptChange'], {
    loadDeptMetadata: async () => calls.push('metadata'), getList: async () => calls.push('users'),
    selectedDeptId: { value: 7 }, deptNameById: { value: new Map() },
    queryParams: { deptId: 7, pageNo: 3 }, deptTreeRenderKey: { value: 0 }
  })
  await context.refreshAfterDeptChange()
  assert.deepEqual(calls, ['metadata', 'users'])
  assert.equal(context.queryParams.deptId, undefined)
  assert.equal(context.queryParams.pageNo, 1)
  assert.equal(context.deptTreeRenderKey.value, 1)
})

test('部门表单新增时接收上级部门，编辑保留已有详情加载', async () => {
  const data = { value: {} }
  const context = loadFunctions('views/system/dept/DeptForm.vue', ['open'], {
    dialogVisible: { value: false }, dialogTitle: { value: '' }, formType: { value: '' },
    formData: data, existingSupervisor: { value: undefined }, formLoading: { value: false },
    userList: { value: [] }, resetForm: () => { data.value = {} }, t: s => s,
    UserApi: { getSimpleUserList: async () => [] }, getTree: async () => {},
    DeptApi: { getDept: async id => ({ id, parentId: 9, leaderUserId: 17 }) }
  })
  await context.open('create', undefined, 7)
  assert.equal(data.value.parentId, 7)
  await context.open('update', 11)
  assert.equal(data.value.id, 11)
  assert.equal(data.value.parentId, 9)
  assert.equal(data.value.leaderUserId, 17)
})
