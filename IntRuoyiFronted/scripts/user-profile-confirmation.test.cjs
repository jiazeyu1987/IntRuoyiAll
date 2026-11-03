const fs = require('node:fs')
const ts = require('typescript')
const assert = require('node:assert/strict')
const source = fs.readFileSync('src/views/system/user/user-change-confirmation.ts', 'utf8')
const moduleOutput = { exports: {} }
new Function('exports', ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(moduleOutput.exports)
const { sameSelection, confirmUserChange } = moduleOutput.exports
;(async () => {
  assert.equal(sameSelection(['1', '2'], ['2', '1']), true)
  assert.equal(sameSelection(['1'], []), false)
  assert.equal(sameSelection([], ['1']), false)
  assert.equal(sameSelection(['1'], ['2']), false)
  let calls = 0
  assert.equal(await confirmUserChange(async (content) => { calls++; assert.equal(content, '<账号>') }, '<账号>'), true)
  assert.equal(calls, 1)
  for (const reason of ['cancel', 'close']) assert.equal(await confirmUserChange(async () => { throw reason }, ''), false)
  const failure = new Error('network')
  await assert.rejects(confirmUserChange(async () => { throw failure }, ''), (error) => error === failure)
  console.log('User change confirmation tests passed')
})().catch((error) => { console.error(error); process.exitCode = 1 })
// Exercise the actual UserForm submit implementation with controlled UI and API boundaries.
const vueSource = fs.readFileSync('src/views/system/user/UserForm.vue', 'utf8')
const submitSource = vueSource.slice(vueSource.indexOf('const submitForm = async () =>'), vueSource.indexOf('/** 重置表单 */'))
const executable = ts.transpileModule(submitSource + '\nreturn submitForm', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
async function submitScenario({ changed = true, cancel = false, stale = false, invalid = false, create = false } = {}) {
  let writes = 0
  let confirmations = 0
  const dialogVisible = { value: true }
  const data = { id: 7, username: 'target', deptId: changed ? 2 : null, postIds: [] }
  const context = {
    dialogVisible, postSelectionReady: { value: true }, formLoading: { value: false },
    formRef: { value: { validate: async () => !invalid } }, openGeneration: 1,
    formType: { value: create ? 'create' : 'update' }, formData: { value: data },
    validateSelection: (ids) => [...ids], originalDepartment: null,
    originalDepartmentLabel: '无', originalPosts: [], originalPostLabels: [], originalAccount: 'target（ID:7）',
    departmentNames: new Map([[2, '部门二']]), sameSelection, postList: { value: [] }, assignedDisabledPosts: { value: [] },
    postIntegrityError: () => new Error('invalid posts'),
    message: { confirm: async (content) => { confirmations++; assert.match(content, /target/); if (stale) dialogVisible.value = false; if (cancel) throw 'cancel' }, success: () => {} },
    confirmUserChange, UserApi: { updateUser: async () => { writes++ }, createUser: async () => { writes++ } },
    t: (s) => s, emit: () => {}, formError: { value: '' }
  }
  await new Function(...Object.keys(context), executable)(...Object.values(context))()
  return { writes, confirmations }
}
;(async () => {
  assert.deepEqual(await submitScenario(), { writes: 1, confirmations: 1 })
  assert.deepEqual(await submitScenario({ cancel: true }), { writes: 0, confirmations: 1 })
  assert.deepEqual(await submitScenario({ stale: true }), { writes: 0, confirmations: 1 })
  assert.deepEqual(await submitScenario({ invalid: true }), { writes: 0, confirmations: 0 })
  assert.deepEqual(await submitScenario({ changed: false }), { writes: 1, confirmations: 0 })
  assert.deepEqual(await submitScenario({ create: true }), { writes: 1, confirmations: 0 })
  console.log('Actual UserForm submit scenarios passed')
})().catch((error) => { console.error(error); process.exitCode = 1 })
