const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const { ref, computed } = require('vue')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/route/RouteEditPage.vue'), 'utf8'
)
const dirtyChecks = source.slice(
  source.indexOf('const hasRouteCandidateDraftChanges ='),
  source.indexOf('const buildRouteRequestKey =')
)
const exitHandlers = source.slice(
  source.indexOf('const discardListEditCandidateDraft ='),
  source.indexOf('const handleBackToList =')
)

const setup = ({ created = true, changed = false, saved = false, discarded = false,
  status = 'DRAFT', editing = true, decision = 'close', cancelError } = {}) => {
  const calls = []
  const changedRef = ref(changed)
  const context = vm.createContext({
    ref, computed,
    contentRef: ref({
      hasRouteCandidateDraftChanges: () => changedRef.value,
      submitForm: async () => { calls.push('save'); changedRef.value = false },
      discardRouteCandidateDraftChanges: () => { calls.push('discard-local'); changedRef.value = false },
      confirmFlowGraphDraftSaveBeforeExit: async () => { calls.push('check-workspace'); return true }
    }),
    routeVersionEditContext: ref({ routeVersionId: 805, lifecycleStatus: status }),
    isRouteEditing: ref(editing),
    isListEditCandidateDraftExitGuardEnabled: ref(created),
    listEditCandidateDraftSaved: ref(saved),
    listEditCandidateDraftDiscarded: ref(discarded),
    routeVersionActionLoading: ref(false),
    markListEditCandidateDraftSaved: () => { context.listEditCandidateDraftSaved.value = true },
    ProRouteApi: { cancelRouteCandidateVersion: async (id) => {
      calls.push(['cancel-draft', id])
      if (cancelError) throw new Error(cancelError)
    } },
    ElMessageBox: { confirm: async () => {
      calls.push('prompt')
      if (decision !== 'save') throw decision
    } },
    resolveRouteOperationErrorMessage: (error) => error.message,
    message: { error: (text) => calls.push(['error', text]), success: () => calls.push('success') }
  })
  vm.runInContext(`${dirtyChecks}\n${exitHandlers}\nglobalThis.leave = confirmRouteEditPageLeave`, context)
  return { context, calls, changedRef }
}

test('列表新建候选版本未修改直接返回，无保存提示并丢弃空草稿', async () => {
  const h = setup()
  assert.equal(await h.context.leave(), true)
  assert.deepEqual(h.calls, [['cancel-draft', 805]])
  assert.equal(h.context.listEditCandidateDraftDiscarded.value, true)
  assert.equal(h.context.routeVersionActionLoading.value, false)
})

test('已有草稿未修改返回，不提示且不取消版本', async () => {
  const h = setup({ created: false })
  assert.equal(await h.context.leave(), true)
  assert.deepEqual(h.calls, ['check-workspace'])
})

test('新建和已有草稿修改后都提示保存，关闭提示保留编辑', async () => {
  for (const created of [true, false]) {
    const h = setup({ created, changed: true })
    assert.equal(await h.context.leave(), false)
    assert.deepEqual(h.calls, ['prompt'])
    assert.equal(h.changedRef.value, true)
  }
})

test('修改后选择保存，保存成功才离开且不取消版本', async () => {
  const h = setup({ changed: true, decision: 'save' })
  assert.equal(await h.context.leave(), true)
  assert.deepEqual(h.calls, ['prompt', 'save'])
  assert.equal(h.context.listEditCandidateDraftSaved.value, true)
})

test('新建草稿修改后选择不保存，取消本次新建版本', async () => {
  const h = setup({ changed: true, decision: 'cancel' })
  assert.equal(await h.context.leave(), true)
  assert.deepEqual(h.calls, ['prompt', ['cancel-draft', 805], 'success'])
})

test('已有草稿修改后选择不保存，只丢弃本地修改', async () => {
  const h = setup({ created: false, changed: true, decision: 'cancel' })
  assert.equal(await h.context.leave(), true)
  assert.deepEqual(h.calls, ['prompt', 'discard-local', 'success'])
})

test('已经保存或丢弃的列表草稿未再修改，不重复提示或取消版本', async () => {
  for (const flags of [{ saved: true }, { discarded: true }]) {
    const h = setup(flags)
    assert.equal(await h.context.leave(), true)
    assert.deepEqual(h.calls, ['check-workspace'])
  }
})

test('查看模式和非草稿版本不执行候选草稿保存提示或取消', async () => {
  for (const flags of [{ editing: false }, { status: 'ACTIVE' }]) {
    const h = setup(flags)
    assert.equal(await h.context.leave(), true)
    assert.deepEqual(h.calls, ['check-workspace'])
  }
})

test('取消空草稿失败时阻止离开并显示真实错误', async () => {
  const h = setup({ cancelError: '版本已被其他用户修改' })
  assert.equal(await h.context.leave(), false)
  assert.deepEqual(h.calls, [['cancel-draft', 805], ['error', '版本已被其他用户修改']])
  assert.equal(h.context.listEditCandidateDraftDiscarded.value, false)
  assert.equal(h.context.routeVersionActionLoading.value, false)
})
