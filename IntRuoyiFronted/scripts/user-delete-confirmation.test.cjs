const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync('src/views/system/user/index.vue', 'utf8')

function setup(confirm) {
  const calls = []
  const messages = []
  const context = {
    ref: (value) => ({ value }),
    UserApi: {
      getSupervisorImpact: async () => [{ name: '试验部门' }],
      deleteUser: async (id) => calls.push(id),
      deleteUserList: async (ids) => calls.push([...ids])
    },
    message: { delConfirm: async (text) => { messages.push(text); await confirm() }, success() {}, warning() {} },
    loadDeptMetadata: async () => {}, getList: async () => {}, t: (key) => key
  }
  vm.createContext(context)
  const impact = source.slice(source.indexOf('const supervisorImpactText ='), source.indexOf('const refreshAfterLifecycleChange ='))
  const handlers = source.slice(source.indexOf('type DeleteTarget ='), source.indexOf('/** 删除当前选中组织 */'))
  vm.runInContext(ts.transpile(impact + handlers + '\nglobalThis.handlers = { handleDelete, handleDeleteBatch, handleRowCheckboxChange, deleteLoading }', { target: ts.ScriptTarget.ES2022 }), context)
  return { ...context.handlers, calls, messages }
}
const user = { id: 7, username: 'account7', nickname: '目标用户' }
test('single and batch deletion cancellation never write', async () => {
  for (const reason of ['cancel', 'close']) {
    const x = setup(async () => { throw reason })
    await x.handleDelete(user)
    x.handleRowCheckboxChange([user])
    await x.handleDeleteBatch()
    assert.deepEqual(x.calls, [])
    assert.equal(x.deleteLoading.value, false)
  }
})
test('single delete confirms actual identity and does not promise automatic handover', async () => {
  const x = setup(async () => {})
  await x.handleDelete(user)
  assert.deepEqual(x.calls, [7])
  for (const text of ['account7', '目标用户', '编号 7', '试验部门', '已有待办不会自动转移']) assert.ok(x.messages[0].includes(text))
  assert.ok(!x.messages[0].includes('停用后将自动打开'))
})
test('batch request keeps confirmed selection and rejects duplicate execution', async () => {
  let release
  const gate = new Promise((resolve) => { release = resolve })
  const x = setup(() => gate)
  x.handleRowCheckboxChange([user, { id: 8, username: 'account8', nickname: '第二用户' }])
  const pending = x.handleDeleteBatch()
  await Promise.resolve()
  x.handleRowCheckboxChange([{ id: 9, username: 'other', nickname: '其他用户' }])
  await x.handleDeleteBatch()
  release()
  await pending
  assert.deepEqual(x.calls, [[7, 8]])
  assert.ok(x.messages[0].includes('2 位用户'))
  assert.ok(x.messages[0].includes('account8'))
  assert.ok(!x.messages[0].includes('other'))
})
test('real confirmation errors remain visible', async () => {
  const error = new Error('dialog failed')
  const x = setup(async () => { throw error })
  await assert.rejects(x.handleDelete(user), (actual) => actual === error)
  assert.deepEqual(x.calls, [])
  assert.equal(x.deleteLoading.value, false)
})
test('password reset prompt explains session and credential effects; cancel does not reset', async () => {
  let prompt, writes = 0
  const context = {
    message: { prompt: async (text) => { prompt = text; throw 'cancel' } },
    UserApi: { resetUserPassword: async () => { writes++ } }, t: (key) => key
  }
  vm.createContext(context)
  vm.runInContext(ts.transpile(source.slice(source.indexOf('const handleResetPwd ='), source.indexOf('/** 锁定用户 */')) + '\nglobalThis.reset = handleResetPwd', { target: ts.ScriptTarget.ES2022 }), context)
  await context.reset(user)
  assert.equal(writes, 0)
  for (const text of ['account7', '现有登录会话将失效', '重新登录并修改密码', '退出登录']) assert.ok(prompt.includes(text))
})
