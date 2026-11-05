const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { ref, nextTick, h } = require('vue')

const confirmationModule = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.resolve(__dirname,
  '../../src/views/system/user/user-change-confirmation.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, { exports: confirmationModule.exports })
const visibleText = node => typeof node === 'string' ? node
  : Array.isArray(node) ? node.map(visibleText).join(' ') : visibleText(node?.children || '')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/system/role/RoleAssignUserForm.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('form.ts', script, ts.ScriptTarget.Latest, true)
const code = ts.transpileModule(ast.statements.filter(s => !ts.isImportDeclaration(s))
  .map(s => s.getText(ast)).join('\n') +
  '\nexports.form = { open, submitForm, unassignUsers, pickerRef, saving, role };', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

async function setup(options = {}) {
  const writes = [], events = [], messages = [], confirmations = [], confirmationDialogs = [], pickerOpens = [], marks = []
  let reads = 0
  const context = {
    exports: {}, ref, nextTick, h,
    crypto: { randomUUID: () => 'test-key' },
    defineExpose() {}, defineEmits: () => e => events.push(e),
    useMessage: () => ({ confirm() {}, success: m => messages.push(m), info: m => messages.push(m), error: m => messages.push(m) }),
    confirmUserChange: confirmationModule.exports.confirmUserChange,
    ElMessageBox: {
      confirm: async (content, title, dialogOptions) => {
        confirmations.push(visibleText(content))
        confirmationDialogs.push({ content, title, options: dialogOptions })
        if (options.confirm === false) throw 'cancel'
        if (options.confirmError) throw options.confirmError
      }
    },
    PermissionApi: {
      getUserRoleList: async userId => {
        reads++
        if (options.pendingRead) await options.pendingRead
        if (options.readError || options.readErrorUserId === userId) throw new Error('read failed')
        return options.rolesByUser?.[userId] || options.roles || [11, 12]
      },
      assignUserRole: async data => {
        if (options.writeError || options.writeErrorUserId === data.userId) throw new Error('write failed')
        writes.push(JSON.parse(JSON.stringify(data)))
        if (options.rolesByUser) options.rolesByUser[data.userId] = [...data.roleIds]
      }
    }
  }
  vm.runInNewContext(code, context)
  const form = context.exports.form
  form.pickerRef.value = {
    open: async (...args) => pickerOpens.push(args),
    markRoleAssignment: (id, assigned) => marks.push({ id, assigned })
  }
  await form.open({ id: 99, name: '待分配角色' })
  const users = options.users || [{ id: 123, nickname: '测试人员', username: 'test-user' }]
  return { ...form, submitForm: (rows = users) => form.submitForm(rows), unassignUsers: (rows = users) => form.unassignUsers(rows), writes, events, messages, confirmations, confirmationDialogs, pickerOpens, marks, reads: () => reads }
}

test('adds selected role, preserves existing roles, supplies audit evidence and refreshes list', async () => {
  const p = await setup()
  await p.submitForm()
  assert.deepEqual(p.writes, [{ userId: 123, roleIds: [11, 12, 99], reason: '权限角色页面分配角色（ID:99）给用户（ID:123）',
    idempotencyKey: 'SYSTEM-PERM-USER-ROLE-123-test-key' }])
  assert.deepEqual(p.events, ['success'])
  assert.equal(p.saving.value, false)
})

test('already assigned users cause no permission write', async () => {
  const p = await setup({ roles: [11, 99] })
  await p.submitForm()
  assert.equal(p.writes.length, 0)
  assert.match(p.messages[0], /已拥有/)
})

test('cancelled confirmation causes no permission reads or writes', async () => {
  const p = await setup({ confirm: false })
  await p.submitForm()
  assert.equal(p.reads(), 0)
  assert.equal(p.writes.length, 0)
})

test('read and save failures remain failures and allow reopening the selector for retry', async () => {
  for (const options of [{ readError: true }, { writeError: true }]) {
    const p = await setup(options)
    await assert.rejects(p.submitForm(), /failed/)
    assert.equal(p.writes.length, 0)
    assert.equal(p.events.length, 0)
    assert.equal(p.saving.value, false)
    await p.open({ id: 99, name: '待分配角色' })
    assert.equal(p.pickerOpens.length, 2)
  }
})

test('clicking another role during submission cannot change the assignment target', async () => {
  let release
  const p = await setup({ pendingRead: new Promise(resolve => { release = resolve }) })
  const pending = p.submitForm()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(p.reads(), 1)
  await p.open({ id: 100, name: '其他角色' })
  release()
  await pending
  assert.equal(p.pickerOpens.length, 1)
  assert.equal(p.role.value.id, 99)
  assert.deepEqual(p.writes[0].roleIds, [11, 12, 99])
})

test('repeated clicks cannot submit duplicate permission commands', async () => {
  const p = await setup()
  await Promise.all([p.submitForm(), p.submitForm()])
  assert.equal(p.reads(), 1)
  assert.equal(p.writes.length, 1)
})

test('assignment opens the multiple user selector directly with no intermediate form or previous selection', async () => {
  const p = await setup()
  assert.equal(p.pickerOpens.length, 1)
  assert.equal(p.reads(), 0)
  assert.equal(p.writes.length, 0)
  await p.open({ id: 100, name: '其他角色' })
  assert.deepEqual(p.pickerOpens, [[], []])
  assert.equal(p.role.value.id, 100)
  const template = source.match(/<template>([\s\S]*?)<\/template>/)[1]
  assert.match(template, /<UserSelectDialogV2/)
  assert.match(template, /align-center/)
  assert.match(template, /:assigned-role-id="role\?\.id"/)
  assert.match(template, /multiple/)
  assert.match(template, /@selected="submitForm"/)
  assert.doesNotMatch(template, /<Dialog|<el-form|<el-input|<UserSelectV2\s/)
})

test('assignment needs no manual reason and shows no reason field or explanation alert', async () => {
  const p = await setup()
  await p.submitForm()
  assert.equal(p.writes.length, 1)
  assert.doesNotMatch(p.confirmations[0], /原因/)
  const template = source.match(/<template>([\s\S]*?)<script/)[1]
  assert.doesNotMatch(template, /变更原因|form.reason|el-alert|保留其已有角色/)
})

test('confirmation displays only names and permission name in a dedicated layout', async () => {
  const p = await setup({ users: [
    { id: 123, nickname: '张光荣', username: 'zhangguangrong' },
    { id: 124, nickname: '孙桂芹', username: 'sunguiqin' }
  ] })
  await p.submitForm()
  assert.equal(p.confirmations[0], '权限 待分配角色 人员 张光荣 孙桂芹')
  assert.doesNotMatch(p.confirmations[0], /zhangguangrong|sunguiqin|ID|123|124|99/)
  const dialog = p.confirmationDialogs[0]
  assert.equal(dialog.title, '确认分配权限')
  assert.equal(dialog.options.customClass, 'role-assignment-confirmation')
  assert.equal(dialog.options.confirmButtonText, '确认分配')
  assert.equal(dialog.content.props.class, 'role-assignment-summary')
})

test('unexpected confirmation errors stop assignment and remain errors', async () => {
  const error = new Error('confirmation failed')
  const p = await setup({ confirmError: error })
  await assert.rejects(p.submitForm(), cause => cause === error)
  assert.equal(p.reads(), 0)
  assert.equal(p.writes.length, 0)
  assert.equal(p.saving.value, false)
})

const users = [
  { id: 123, nickname: '人员甲', username: 'user-a' },
  { id: 124, nickname: '人员乙', username: 'user-b' },
  { id: 125, nickname: '人员丙', username: 'user-c' }
]

test('multiple users keep their own roles and already assigned users do not block later users', async () => {
  const p = await setup({ users, rolesByUser: { 123: [11], 124: [99], 125: [12, 13] } })
  await p.submitForm()
  assert.deepEqual(p.writes.map(({ userId, roleIds }) => ({ userId, roleIds })), [
    { userId: 123, roleIds: [11, 99] },
    { userId: 125, roleIds: [12, 13, 99] }
  ])
  assert.equal(p.reads(), 3)
  assert.deepEqual(p.events, ['success'])
  assert.match(p.messages[0], /成功分配 2 人.*1 人已拥有/)
  for (const user of users) assert.ok(p.confirmations[0].includes(user.nickname))
})

test('partial failures stop subsequent users, report progress and refresh actual successes; retry skips saved users', async () => {
  for (const failure of ['readErrorUserId', 'writeErrorUserId']) {
    const options = { users, rolesByUser: { 123: [11], 124: [12], 125: [13] }, [failure]: 124 }
    const p = await setup(options)
    await assert.rejects(p.submitForm(), /failed/)
    assert.deepEqual(p.writes.map(write => write.userId), [123])
    assert.equal(p.reads(), 2)
    assert.deepEqual(p.events, ['success'])
    assert.equal(p.saving.value, false)
    assert.match(p.messages[0], /人员乙.*已成功分配 1 人.*后续分配已停止/)
    delete options[failure]
    await p.submitForm()
    assert.deepEqual(p.writes.map(write => write.userId), [123, 124, 125])
  }
})

test('empty or invalid selected users fail before permission reads or writes', async () => {
  for (const ids of [[], [0], [123, 'invalid']]) {
    const p = await setup()
    await assert.rejects(p.submitForm(ids.map(id => ({ id }))), /请选择有效用户/)
    assert.equal(p.reads(), 0)
    assert.equal(p.writes.length, 0)
  }
})

test('duplicate ids produce one assignment per user', async () => {
  const p = await setup()
  await p.submitForm([{ id: 123 }, { id: '123' }])
  assert.equal(p.reads(), 1)
  assert.equal(p.writes.length, 1)
})

test('unassign removes only the current role, records the action and clears its green mark', async () => {
  const p = await setup({ roles: [11, 99, 12] })
  await p.unassignUsers()
  assert.deepEqual(p.writes, [{ userId: 123, roleIds: [11, 12],
    reason: '权限角色页面解除用户（ID:123）的角色（ID:99）',
    idempotencyKey: 'SYSTEM-PERM-USER-ROLE-123-test-key' }])
  assert.deepEqual(p.marks, [{ id: 123, assigned: false }])
  assert.equal(p.confirmationDialogs[0].title, '确认解除权限')
  assert.equal(p.confirmationDialogs[0].options.confirmButtonText, '确认解除')
  assert.equal(p.confirmations[0], '权限 待分配角色 人员 测试人员')
  assert.deepEqual(p.events, ['success'])
  assert.match(p.messages[0], /成功解除 1 人/)
})

test('unassign permits removing the last role and skips roles already removed', async () => {
  const p = await setup({ users, rolesByUser: { 123: [99], 124: [], 125: [12, 99] } })
  await p.unassignUsers()
  assert.deepEqual(p.writes.map(({ userId, roleIds }) => ({ userId, roleIds })), [
    { userId: 123, roleIds: [] }, { userId: 125, roleIds: [12] }
  ])
  assert.deepEqual(p.marks, users.map(user => ({ id: user.id, assigned: false })))
  assert.match(p.messages[0], /成功解除 2 人.*1 人未拥有/)
})

test('cancelled removal never reads or writes roles or clears green marks', async () => {
  const p = await setup({ confirm: false, roles: [99] })
  await p.unassignUsers()
  assert.equal(p.reads(), 0)
  assert.equal(p.writes.length, 0)
  assert.equal(p.marks.length, 0)
})

test('partial removal stops at the failed user and updates marks only for successful removals; retry preserves other roles', async () => {
  const options = { users, rolesByUser: { 123: [99, 11], 124: [99, 12], 125: [99, 13] }, writeErrorUserId: 124 }
  const p = await setup(options)
  await assert.rejects(p.unassignUsers(), /write failed/)
  assert.deepEqual(p.writes.map(write => write.userId), [123])
  assert.deepEqual(p.marks, [{ id: 123, assigned: false }])
  assert.equal(p.reads(), 2)
  assert.equal(p.saving.value, false)
  assert.match(p.messages[0], /人员乙.*已成功解除 1 人.*后续解除已停止/)
  assert.deepEqual(p.events, ['success'])
  delete options.writeErrorUserId
  await p.unassignUsers()
  assert.deepEqual(p.writes.map(({ userId, roleIds }) => ({ userId, roleIds })), [
    { userId: 123, roleIds: [11] }, { userId: 124, roleIds: [12] }, { userId: 125, roleIds: [13] }
  ])
})

test('assignment and removal cannot run together or submit twice', async () => {
  const p = await setup({ roles: [99, 11] })
  await Promise.all([p.unassignUsers(), p.unassignUsers(), p.submitForm()])
  assert.equal(p.reads(), 1)
  assert.equal(p.writes.length, 1)
  assert.deepEqual(p.writes[0].roleIds, [11])
})
