const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/route/RouteFormContent.vue'), 'utf8')
const start = source.indexOf('const saveDccProjectBinding =')
const end = source.indexOf('const getActiveTab =', start)
assert.ok(start >= 0 && end > start)
const script = ts.transpile(source.slice(start, end) +
  '\nglobalThis.save = saveDccProjectBinding', { target: ts.ScriptTarget.ES2020 })

function setup({ products = [{ itemId: 1001 }], draftProducts = [], routeVersionId,
  confirmError, saveError, readError, draftReadError, editable = true } = {}) {
  const calls = { saves: [], confirmations: [], successes: [], errors: [] }
  const savedProject = { id: 200, projectCode: 'OLD', projectName: '原项目' }
  const selectedProject = { id: 201, projectCode: 'NEW', projectName: '新项目' }
  const state = {
    dccProjectBindingLoading: { value: false },
    canEditStartSettings: { value: editable },
    formData: { value: { id: 100 } },
    isDraftCandidateVersion: { value: Boolean(routeVersionId) },
    routeVersionEditContext: { value: routeVersionId ? { routeVersionId } : undefined },
    dccProjectBindingForm: { dccProjectCodeId: 201 },
    dccProjectBinding: { value: { version: 3, dccProjectCodeId: 200 } },
    savedDccProjectCode: { value: savedProject },
    getProjectCode: async () => selectedProject,
    ProRouteProductApi: { getRouteProductListByRoute: async (routeId, versionId) => {
      assert.equal(routeId, 100)
      if (versionId !== undefined) {
        assert.equal(versionId, routeVersionId)
        if (draftReadError) throw draftReadError
        return draftProducts
      }
      if (readError) throw readError
      return products
    } },
    ElMessageBox: { confirm: async (...args) => {
      calls.confirmations.push(args)
      assert.equal(state.savedDccProjectCode.value, savedProject)
      assert.equal(calls.saves.length, 0)
      if (confirmError) throw confirmError
    } },
    ProRouteApi: { saveRouteDccProjectBinding: async payload => {
      calls.saves.push(payload)
      if (saveError) throw saveError
      return { version: 4, dccProjectCodeId: 201, bound: true }
    } },
    message: {
      success: text => calls.successes.push(text),
      error: text => calls.errors.push(text)
    },
    isRouteConfirmCancel: error => error === 'cancel' || error === 'close',
    resolveRouteOperationErrorMessage: error => error.message
  }
  vm.createContext(state)
  vm.runInContext(script, state)
  return { state, calls, savedProject, selectedProject }
}

for (const action of ['cancel', 'close']) {
  test(`${action} leaves saved project and product relations unchanged`, async () => {
    const { state, calls, savedProject } = setup({ confirmError: action })
    await state.save()
    assert.equal(calls.confirmations.length, 1)
    assert.equal(calls.saves.length, 0)
    assert.equal(calls.errors.length, 0)
    assert.equal(state.savedDccProjectCode.value, savedProject)
    assert.equal(state.dccProjectBinding.value.version, 3)
    assert.equal(state.dccProjectBindingLoading.value, false)
  })
}

test('confirmation submits exactly the displayed product IDs with project save', async () => {
  const { state, calls, selectedProject } = setup({ products: [{ itemId: 1001 }, { itemId: 1002 }] })
  await state.save()
  assert.match(calls.confirmations[0][0], /2 个产品/)
  assert.match(calls.confirmations[0][0], /NEW.*新项目/)
  assert.deepEqual(JSON.parse(JSON.stringify(calls.saves)), [{
    routeId: 100, dccProjectCodeId: 201, expectedVersion: 3,
    confirmedProductItemIds: [1001, 1002]
  }])
  assert.equal(state.savedDccProjectCode.value, selectedProject)
  assert.equal(calls.successes.length, 1)
})

test('no products saves directly without a destructive confirmation', async () => {
  const { state, calls } = setup({ products: [] })
  await state.save()
  assert.equal(calls.confirmations.length, 0)
  assert.equal(calls.saves.length, 1)
  assert.equal(calls.saves[0].confirmedProductItemIds.length, 0)
})

test('save failure retains saved project and reports failure', async () => {
  const { state, calls, savedProject } = setup({ saveError: new Error('save rejected') })
  await assert.rejects(state.save(), /save rejected/)
  assert.equal(state.savedDccProjectCode.value, savedProject)
  assert.equal(state.dccProjectBinding.value.version, 3)
  assert.equal(calls.successes.length, 0)
  assert.deepEqual(calls.errors, ['save rejected'])
  assert.equal(state.dccProjectBindingLoading.value, false)
})

test('failed product read stops confirmation and save', async () => {
  const { state, calls } = setup({ readError: new Error('read rejected') })
  await assert.rejects(state.save(), /read rejected/)
  assert.equal(calls.confirmations.length, 0)
  assert.equal(calls.saves.length, 0)
})

test('duplicate save is ignored while confirmation or save is pending', async () => {
  const { state, calls } = setup()
  state.dccProjectBindingLoading.value = true
  await state.save()
  assert.equal(calls.confirmations.length, 0)
  assert.equal(calls.saves.length, 0)
})

test('draft-only products require confirmation and carry the edited version', async () => {
  const { state, calls } = setup({ products: [], draftProducts: [{ itemId: 1002 }], routeVersionId: 500 })
  await state.save()
  assert.equal(calls.confirmations.length, 1)
  assert.match(calls.confirmations[0][0], /当前草稿.*1 个产品/)
  assert.deepEqual(JSON.parse(JSON.stringify(calls.saves[0])), {
    routeId: 100, routeVersionId: 500, dccProjectCodeId: 201,
    expectedVersion: 3, confirmedProductItemIds: [1002]
  })
})

test('cancel with draft products preserves the project and performs no save', async () => {
  const { state, calls, savedProject } = setup({ products: [], draftProducts: [{ itemId: 1002 }],
    routeVersionId: 500, confirmError: 'cancel' })
  await state.save()
  assert.equal(calls.confirmations.length, 1)
  assert.equal(calls.saves.length, 0)
  assert.equal(state.savedDccProjectCode.value, savedProject)
})

test('confirmation covers the union of formal and draft products without double counting', async () => {
  const { state, calls } = setup({ products: [{ itemId: 1001 }, { itemId: 1002 }],
    draftProducts: [{ itemId: 1002 }, { itemId: 1003 }], routeVersionId: 500 })
  await state.save()
  assert.match(calls.confirmations[0][0], /3 个产品/)
  assert.deepEqual(Array.from(calls.saves[0].confirmedProductItemIds), [1001, 1002, 1003])
})

test('failed draft product read prevents confirmation and saving', async () => {
  const { state, calls, savedProject } = setup({ routeVersionId: 500,
    draftReadError: new Error('draft read rejected') })
  await assert.rejects(state.save(), /draft read rejected/)
  assert.equal(calls.confirmations.length, 0)
  assert.equal(calls.saves.length, 0)
  assert.equal(state.savedDccProjectCode.value, savedProject)
})

test('browsing a draft rejects project writes before reading or confirming products', async () => {
  const { state, calls, savedProject } = setup({ editable: false, routeVersionId: 500,
    readError: new Error('must not read products') })
  await assert.rejects(state.save(), /只读浏览.*修改/)
  assert.equal(calls.confirmations.length, 0)
  assert.equal(calls.saves.length, 0)
  assert.equal(state.savedDccProjectCode.value, savedProject)
  assert.equal(state.dccProjectBindingLoading.value, false)
})
