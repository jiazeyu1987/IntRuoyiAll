import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'

const panel = readFileSync(new URL('../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue', import.meta.url), 'utf8')
const source = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const names = new Set([
  'cloneProductionDeviceParameters', 'replaceReactiveRecord',
  'buildProductionMaterialDeviceMemoryKey', 'buildProductionMaterialDeviceMemoryStorageKey',
  'buildProductionProcessDeviceMemoryKey', 'restoreProductionDeviceDraft',
  'parseProductionMaterialDeviceMemory', 'loadProductionMaterialDeviceMemory',
  'saveProductionMaterialDeviceMemory', 'createProductionMaterialDraftState',
  'persistActiveProductionMaterialDraft', 'restoreProductionMaterialDraft',
  'clearProductionMaterialDrafts', 'syncProductionMaterialDrafts', 'resetProductionMaterialDrafts',
  'switchProductionMaterial', 'rememberProductionMaterialDeviceDrafts', 'resetProductionSubmissionDraft',
  'syncProductionDeviceParameterDraft', 'ensureProductionDefaultDeviceSelection',
  'resetProductionDeviceParameterDraft', 'resetProductionDeviceMeteringValidityDraft'
])
const declarations = ast.statements.filter(statement =>
  ts.isFunctionDeclaration(statement) ? names.has(statement.name?.text) :
    ts.isVariableStatement(statement) && statement.declarationList.declarations.some(value => names.has(value.name.getText(ast))))
assert.equal(declarations.length, names.size)
const code = ts.transpileModule(declarations.map(value => value.getText(ast)).join('\n') +
  '\nObject.assign(globalThis, { resetProductionMaterialDrafts, switchProductionMaterial, rememberProductionMaterialDeviceDrafts, resetProductionSubmissionDraft })',
{ compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
const ref = value => ({ value })
const plain = value => JSON.parse(JSON.stringify(value))

function harness(storage = new Map(), materials = [{ key: '101' }, { key: '102' }]) {
  const r = {
    productionMaterialDrafts: {}, productionMaterialLastDeviceDrafts: {},
    PRODUCTION_MATERIAL_DEVICE_MEMORY_STORAGE_PREFIX: 'mes.frontline.production-device-memory.v1',
    userStore: { getUser: { id: 7 } },
    localStorage: { getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value) },
    deviceState: { selectedActiveOrder: { routeVersionId: 10, productId: 50 }, selectedProcess: { routeProcessId: 20, processId: 30 }, runtimeConfig: {} },
    isPqcMode: ref(false),
    isFrontlineProductionProcess: process => Boolean(process?.routeProcessId),
    configuredProductionMaterials: ref(materials),
    configuredDefectReasons: ref([{ key: 'loss' }]),
    selectedProductionMaterialKey: ref(), selectedProductionDeviceKey: ref(), selectedProductionDeviceKeys: ref([]),
    productionDraft: {}, productionDefectDraft: {}, deviceParameterDraft: {}, deviceMeteringValidityDraft: {},
    visibleDeviceCards: ref(['first', 'second'].map(key => ({ key, parameters: [{ parameterCode: 'temperature', defaultValue: 25 }] }))),
    getProductionSubmittableParameters: device => device.parameters,
    isTextStandardParameter: () => false, isSelectParameter: () => false, isBooleanParameter: () => false,
    normalizeProductionParameter: (_parameter, value) => value,
    syncProductionDeviceMeteringValidityDraft(devices) {
      for (const device of devices) this.deviceMeteringValidityDraft[device.key] ??= true
    },
    resetProductionClearanceConfirmations() {}, createFrontlineDefaultValues: () => ({}),
    createProductionSubmitDraftKey: () => 'next', context: { templateCode: 'production' },
    draft: { fieldValues: {} }, payloadPreview: ref(), productionSubmitDraftKey: ref()
  }
  // These helpers are invoked as plain functions by the production script.
  r.syncProductionDeviceMeteringValidityDraft = r.syncProductionDeviceMeteringValidityDraft.bind(r)
  runInNewContext(code, r)
  r.resetProductionMaterialDrafts()
  return r
}

function enter(r, temperature) {
  r.selectedProductionDeviceKey.value = 'second'
  r.selectedProductionDeviceKeys.value = ['second']
  r.deviceParameterDraft.second = { temperature, enabled: false, mode: '低温' }
  r.productionDraft.outputQuantity = 12
  r.productionDefectDraft.loss = 2
}

test('successful submission retains the selected device and exact values for the next entry, clearing quantities', () => {
  const r = harness()
  enter(r, 0)
  r.rememberProductionMaterialDeviceDrafts([{ materialId: 101 }])
  r.resetProductionSubmissionDraft()
  assert.deepEqual(plain(r.selectedProductionDeviceKeys.value), ['second'])
  assert.equal(r.selectedProductionDeviceKey.value, 'second')
  assert.deepEqual(plain(r.deviceParameterDraft.second), { temperature: 0, enabled: false, mode: '低温' })
  assert.equal(r.productionDraft.outputQuantity, undefined)
  assert.equal(r.productionDefectDraft.loss, 0)
})

test('reopening uses the last submitted values and unsaved edits do not replace them', () => {
  const storage = new Map(), r = harness(storage)
  enter(r, 68)
  r.rememberProductionMaterialDeviceDrafts([{ materialId: 101 }])
  r.deviceParameterDraft.second.temperature = 99
  r.resetProductionSubmissionDraft()
  assert.equal(r.deviceParameterDraft.second.temperature, 68)
  const reopened = harness(storage)
  assert.deepEqual(plain(reopened.selectedProductionDeviceKeys.value), ['second'])
  assert.equal(reopened.deviceParameterDraft.second.temperature, 68)
  assert.equal(reopened.productionDraft.outputQuantity, undefined)
})

test('products and process versions keep separate memories and blank tabs do not overwrite a submitted product', () => {
  const storage = new Map(), r = harness(storage)
  enter(r, 60)
  r.switchProductionMaterial('102')
  enter(r, 80)
  r.rememberProductionMaterialDeviceDrafts([{ materialId: 101 }, { materialId: 102 }])
  r.resetProductionSubmissionDraft()
  assert.equal(r.deviceParameterDraft.second.temperature, 60)
  enter(r, 65)
  r.rememberProductionMaterialDeviceDrafts([{ materialId: 101 }])
  r.resetProductionSubmissionDraft()
  r.switchProductionMaterial('102')
  assert.equal(r.deviceParameterDraft.second.temperature, 80)
  r.deviceState.selectedActiveOrder.routeVersionId = 11
  r.resetProductionMaterialDrafts()
  assert.equal(r.deviceParameterDraft.second.temperature, 25)
  assert.deepEqual(plain(r.selectedProductionDeviceKeys.value), ['first'])
})

test('processes without output material tabs remember the product device and parameters after reset and reopening', () => {
  const storage = new Map(), r = harness(storage, [])
  enter(r, 72)
  r.rememberProductionMaterialDeviceDrafts([])
  // A successful submission refreshes the process before clearing the form.
  r.resetProductionMaterialDrafts()
  r.resetProductionSubmissionDraft()
  assert.equal(r.selectedProductionDeviceKey.value, 'second')
  assert.deepEqual(plain(r.selectedProductionDeviceKeys.value), ['second'])
  assert.equal(r.deviceParameterDraft.second.temperature, 72)
  assert.equal(r.productionDraft.outputQuantity, undefined)
  const reopened = harness(storage, [])
  assert.equal(reopened.deviceParameterDraft.second.temperature, 72)
  reopened.deviceState.selectedActiveOrder.productId = 51
  reopened.resetProductionMaterialDrafts()
  assert.deepEqual(plain(reopened.selectedProductionDeviceKeys.value), ['first'])
  assert.equal(reopened.deviceParameterDraft.second.temperature, 25)
})

test('formal success saves device memory before refreshing the selected process or resetting drafts', () => {
  const handler = source.slice(source.indexOf('const handleProductionFormalSubmit = async'), source.indexOf('const assertPqcSignatureAndQuantityReady ='))
  const receipt = handler.indexOf("submitResult.submissionStatus !== 'SUBMITTED'")
  const remember = handler.indexOf('rememberProductionMaterialDeviceDrafts(materialDetails)')
  const refresh = handler.indexOf('await refreshProductionSelectedProcessSubmittedQuantity')
  const reset = handler.indexOf('resetProductionSubmissionDraft()')
  assert.ok(receipt >= 0 && remember > receipt && refresh > remember && reset > remember)
})

test('string Long product IDs load without blocking and preserve distinct product memories beyond JS integer precision', () => {
  const storage = new Map(), r = harness(storage, [])
  r.deviceState.selectedActiveOrder.productId = '2050000000000000001'
  assert.doesNotThrow(() => r.resetProductionMaterialDrafts())
  enter(r, 73)
  r.rememberProductionMaterialDeviceDrafts([])
  r.resetProductionSubmissionDraft()
  assert.equal(r.selectedProductionDeviceKey.value, 'second')
  assert.equal(r.deviceParameterDraft.second.temperature, 73)
  assert.ok([...storage.keys()].some(key => key.endsWith('product:2050000000000000001')))
  r.deviceState.selectedActiveOrder.productId = '2050000000000000002'
  r.resetProductionMaterialDrafts()
  assert.deepEqual(plain(r.selectedProductionDeviceKeys.value), ['first'])
  assert.equal(r.deviceParameterDraft.second.temperature, 25)
  r.deviceState.selectedActiveOrder.productId = '2050000000000000001'
  r.resetProductionMaterialDrafts()
  assert.equal(r.deviceParameterDraft.second.temperature, 73)
})

test('missing or malformed product identities still report an explicit error', () => {
  const r = harness(new Map(), [])
  for (const productId of [undefined, null, '', 0, -1, NaN, 'invalid']) {
    r.deviceState.selectedActiveOrder.productId = productId
    assert.throws(() => r.resetProductionMaterialDrafts(), /设备参数记忆缺少当前订单的产品身份/)
  }
})
