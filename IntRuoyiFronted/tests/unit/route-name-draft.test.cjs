const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const formScript = parse(fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFormContent.vue'), 'utf8')).descriptor.scriptSetup.content

function setup({ status = 'DRAFT', withContext = true, permission = true, versions, editing = true } = {}) {
  const candidate = { id: 12, routeId: 7, versionNo: 'V2', lifecycleStatus: status, active: false, sourceRouteVersionId: 11 }
  const props = vue.reactive({ mode: 'page', basicReadonly: true, pageEditing: editing,
    routeVersionEditContext: withContext ? { routeVersionId: 12, versionNo: 'V2', lifecycleStatus: status } : undefined })
  const calls = []
  let draftName = '草稿名称'
  const api = {
    getRoute: async () => ({ id: 7, code: 'R7', name: '生效名称', activeRouteVersionId: 11, activeRouteVersionNo: 'V1' }),
    getRouteVersion: async () => candidate,
    getRouteVersionList: async () => versions || [candidate],
    createRouteCandidateVersion: async () => { calls.push('create'); return candidate },
    getRouteVersionName: async () => ({ routeVersionId: 12, routeId: 7, name: draftName, routeSnapshotSha256: 'current-hash' }),
    saveRouteVersionName: async (data) => { calls.push(data); draftName = data.name }
  }
  const load = (source) => {
    const exports = {}
    const context = { ...vue, exports, console, defineOptions() {}, defineProps: () => props,
      withDefaults: (value) => value, defineAsyncComponent: () => ({}), watch() {}, defineExpose() {},
      defineEmits: () => (...args) => calls.push(args), useMessage: () => ({ error() {}, success() {} }) }
    context.require = (id) => {
      if (id === '@/api/mes/pro/route') return { ProRouteApi: api }
      if (id === '@/utils/permission') return { checkPermi: () => permission }
      if (id === './routeCandidateEntry') return load(fs.readFileSync(path.join(root, 'src/views/mes/pro/route/routeCandidateEntry.ts'), 'utf8'))
      if (id === '@/utils/routeQueryId') return { parsePositiveRouteQueryId: (value) => Number(value) > 0 ? Number(value) : undefined }
      return {}
    }
    vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
    return exports
  }
  const probe = load(formScript + '\nObject.assign(exports, { formData, savedRouteName, formType, formRef, routeFlowGraphDesignerRef, activeTab, isRouteNameReadonly, productionConfigFormType, canEditStartSettings, canRequestCandidateEdit, hasRouteCandidateDraftChanges, discardRouteCandidateDraftChanges, persistRouteNameDraft, submitForm, open })')
  probe.formType.value = 'update'
  probe.formData.value = { id: 7, code: 'R7', name: '草稿名称', activeRouteVersionId: 11 }
  probe.savedRouteName.value = '草稿名称'
  probe.formRef.value = { resetFields() {}, validate: async () => {}, validateField: async () => {
    if (!probe.formData.value.name.trim()) throw new Error('名称不能为空')
  } }
  return { ...probe, api, calls, props }
}

test('draft rename saves to the selected version and clears unsaved state', async () => {
  const h = setup()
  h.formData.value.name = ' 新名称 '
  assert.equal(h.hasRouteCandidateDraftChanges(), true)
  await h.persistRouteNameDraft()
  assert.equal(h.calls[0].routeVersionId, 12)
  assert.equal(h.calls[0].name, '新名称')
  assert.equal(h.calls[0].expectedRouteSnapshotSha256, 'current-hash')
  assert.equal(h.hasRouteCandidateDraftChanges(), false)
  await h.persistRouteNameDraft()
  assert.equal(h.calls.length, 1)
})

test('browsing the active version cannot rename or create a draft implicitly', async () => {
  for (const withContext of [true, false]) {
    const h = setup({ status: 'ACTIVE', withContext, editing: false })
    h.formData.value.name = '新名称'
    await assert.rejects(h.persistRouteNameDraft(), /不允许修改/)
    assert.equal(h.calls.length, 0)
  }
})

test('the flow toolbar save also persists the name into the same draft', async () => {
  const h = setup()
  h.activeTab.value = 'flow'
  h.routeFlowGraphDesignerRef.value = { validateBeforeSubmit: async () => {}, saveFromParent: async () => {} }
  h.formData.value.name = '新名称'
  await h.submitForm()
  assert.equal(h.calls[0].routeVersionId, 12)
  assert.equal(h.calls[0].name, '新名称')
  assert.equal(h.hasRouteCandidateDraftChanges(), false)
})

test('active snapshots stay read only and only current active offers the edit entry', () => {
  const current = setup({ status: 'ACTIVE', editing: false })
  current.props.routeVersionEditContext.routeVersionId = 11
  assert.equal(current.isRouteNameReadonly.value, true)
  assert.equal(current.canRequestCandidateEdit.value, true)
  current.props.routeVersionEditContext.routeVersionId = 10
  assert.equal(current.isRouteNameReadonly.value, true)
  assert.equal(current.canRequestCandidateEdit.value, false)
})

test('browsing a draft blocks all parent save entry points and enables them only after editing', async () => {
  const h = setup({ editing: false })
  assert.equal(h.productionConfigFormType.value, 'detail')
  assert.equal(h.isRouteNameReadonly.value, true)
  assert.equal(h.canEditStartSettings.value, false)
  assert.equal(h.canRequestCandidateEdit.value, true)
  h.formData.value.name = '新名称'
  await assert.rejects(h.persistRouteNameDraft(), /不允许修改/)
  await assert.rejects(h.submitForm(), /只读浏览/)
  assert.equal(h.calls.length, 0)
  h.props.pageEditing = true
  assert.equal(h.productionConfigFormType.value, 'update')
  assert.equal(h.isRouteNameReadonly.value, false)
  assert.equal(h.canEditStartSettings.value, true)
  assert.equal(h.canRequestCandidateEdit.value, false)
  await h.persistRouteNameDraft()
  assert.equal(h.calls[0].routeVersionId, 12)
})

test('reopening a draft reads its saved name instead of the active name', async () => {
  const h = setup()
  h.formData.value.name = '新名称'
  await h.persistRouteNameDraft()
  await h.open('update', 7, 'basic')
  assert.equal(h.formData.value.name, '新名称')
  assert.equal(h.savedRouteName.value, '新名称')
})

test('discarding changes restores the saved draft name', () => {
  const h = setup()
  h.formData.value.name = '未保存'
  h.discardRouteCandidateDraftChanges()
  assert.equal(h.formData.value.name, '草稿名称')
  assert.equal(h.hasRouteCandidateDraftChanges(), false)
})

test('non-draft versions and missing permission reject writes', async () => {
  for (const options of [{ status: 'PENDING_APPROVAL' }, { status: 'READY_TO_PUBLISH' }, { permission: false }]) {
    const h = setup(options)
    h.formData.value.name = '新名称'
    assert.equal(h.isRouteNameReadonly.value, true)
    await assert.rejects(h.persistRouteNameDraft(), /不允许修改/)
    assert.equal(h.calls.length, 0)
  }
})

test('blank names, stale names and wrong-route versions fail without saving', async () => {
  const blank = setup()
  blank.formData.value.name = '  '
  await assert.rejects(blank.persistRouteNameDraft(), /不能为空/)
  const stale = setup()
  stale.formData.value.name = '新名称'
  stale.api.getRouteVersionName = async () => ({ name: '他人修改', routeSnapshotSha256: 'new-hash' })
  await assert.rejects(stale.persistRouteNameDraft(), /已被修改/)
  const wrong = setup()
  wrong.formData.value.name = '新名称'
  wrong.api.getRouteVersion = async () => ({ id: 12, routeId: 99, lifecycleStatus: 'DRAFT' })
  await assert.rejects(wrong.persistRouteNameDraft(), /不属于当前路线/)
  for (const h of [blank, stale, wrong]) assert.equal(h.calls.length, 0)
})

test('backend save failure preserves unsaved name and reports no success', async () => {
  const h = setup()
  h.formData.value.name = '新名称'
  h.api.saveRouteVersionName = async () => { throw new Error('冲突') }
  await assert.rejects(h.persistRouteNameDraft(), /冲突/)
  assert.equal(h.hasRouteCandidateDraftChanges(), true)
  assert.equal(h.calls.length, 0)
})
