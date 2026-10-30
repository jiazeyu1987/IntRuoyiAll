const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const source = parse(fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/route/RouteProductList.vue'), 'utf8')).descriptor.scriptSetup.content

function setup({ bound = false, readError } = {}) {
  const calls = { reads: [], writes: [], warnings: [], errors: [] }
  const binding = { bound, dccProjectCodeId: bound ? 20 : null }
  const product = { id: 3, itemId: 2, itemCode: 'P2', itemName: '产品二' }
  const item = { id: 2, code: 'P2', name: '产品二', status: 0 }
  const products = []
  const recordWrite = async (action, payload) => {
    calls.writes.push({ action, payload })
    if (action === 'create') products.push({ ...product, ...payload })
  }
  const context = {
    ...vue, exports: {}, defineOptions() {}, watch() {},
    defineProps: () => ({ routeId: 7, formType: 'update',
      routeVersionEditContext: { routeVersionId: 12, lifecycleStatus: 'DRAFT' } }),
    defineEmits: () => () => {}, useI18n: () => ({ t: value => value }),
    useRouter: () => ({ push: async () => {} }),
    useMessage: () => ({ warning: text => calls.warnings.push(text),
      error: text => calls.errors.push(text), success() {}, delConfirm: async () => {} }),
    require(id) {
      if (id === '@/api/mes/pro/route') return { ProRouteApi: {
        getRouteDccProjectBinding: async routeId => {
          calls.reads.push(routeId)
          if (readError) throw readError
          return { ...binding }
        }
      } }
      if (id === '@/api/mes/pro/route/product') return { ProRouteProductApi: {
        getRouteProductListByRoute: async () => [...products],
        createRouteProduct: payload => recordWrite('create', payload),
        updateRouteProduct: payload => recordWrite('update', payload),
        copyCandidateRouteProduct: payload => recordWrite('copy', payload),
        deleteCandidateRouteProduct: (...args) => recordWrite('delete', args)
      } }
      if (id === '@/api/mes/md/item') return { MdItemApi: { getItemByCode: async () => item } }
      if (id === '@/utils/constants') return { CommonStatusEnum: { ENABLE: 0 } }
      if (id === './routeError') return {
        resolveRouteOperationErrorMessage: error => error.message,
        isRouteConfirmCancel: error => error === 'cancel'
      }
      return {}
    }
  }
  const probe = ['openForm', 'openCopyForm', 'submitSelectedProducts', 'submitForm',
    'submitCopyForm', 'handleDelete', 'productSelectorVisible', 'formVisible',
    'copyFormVisible', 'formRef', 'copyFormRef', 'batchSubmitting']
  vm.runInNewContext(ts.transpileModule(source +
    `\nObject.assign(exports, { ${probe.join(', ')} })`, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  const api = context.exports
  api.formRef.value = api.copyFormRef.value = { validate: async () => true }
  return { ...api, calls, binding, product, item }
}

test('unlinked project blocks add, edit and copy dialogs with a project prompt', async () => {
  const h = setup()
  await h.openForm('create')
  await h.openForm('update', h.product)
  await h.openCopyForm(h.product)
  assert.equal(h.productSelectorVisible.value, false)
  assert.equal(h.formVisible.value, false)
  assert.equal(h.copyFormVisible.value, false)
  assert.deepEqual(h.calls.warnings, Array(3).fill('请先关联项目'))
  assert.equal(h.calls.writes.length, 0)
})

test('a saved project allows product selection and association to the current draft', async () => {
  const h = setup({ bound: true })
  await h.openForm('create')
  assert.equal(h.productSelectorVisible.value, true)
  await h.submitSelectedProducts([h.item])
  assert.deepEqual(h.calls.reads, [7, 7])
  assert.deepEqual(JSON.parse(JSON.stringify(h.calls.writes)), [{
    action: 'create', payload: { routeId: 7, routeVersionId: 12, itemId: 2 }
  }])
  assert.equal(h.productSelectorVisible.value, false)
})

test('removing the project after opening selection blocks the association on confirm', async () => {
  const h = setup({ bound: true })
  await h.openForm('create')
  h.binding.bound = false
  h.binding.dccProjectCodeId = null
  await h.submitSelectedProducts([h.item])
  assert.deepEqual(h.calls.warnings, ['请先关联项目'])
  assert.equal(h.calls.writes.length, 0)
  assert.equal(h.batchSubmitting.value, false)
})

test('edit and copy confirmation also require a currently saved project', async () => {
  const h = setup()
  await h.submitForm()
  await h.submitCopyForm()
  assert.deepEqual(h.calls.warnings, ['请先关联项目', '请先关联项目'])
  assert.equal(h.calls.writes.length, 0)
})

test('project lookup failure reports the failure and prevents selection or writes', async () => {
  const h = setup({ readError: new Error('项目查询失败') })
  await h.openForm('create')
  await h.submitSelectedProducts([h.item])
  assert.equal(h.productSelectorVisible.value, false)
  assert.equal(h.calls.writes.length, 0)
  assert.deepEqual(h.calls.errors, ['项目查询失败', '项目查询失败'])
  assert.deepEqual(h.calls.warnings, [])
})

test('deleting an existing association does not require a project', async () => {
  const h = setup()
  await h.handleDelete(h.product)
  assert.deepEqual(h.calls.reads, [])
  assert.deepEqual(JSON.parse(JSON.stringify(h.calls.writes)), [{ action: 'delete', payload: [7, 2, 12] }])
})
