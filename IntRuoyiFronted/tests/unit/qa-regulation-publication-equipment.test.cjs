const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const source = parse(fs.readFileSync('src/views/mes/pro/processpool/QaRegulationPage.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('qa.ts', source, ts.ScriptTarget.Latest, true)
function extract(names, context) {
  const statements = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
  assert.equal(statements.length, names.length)
  vm.runInNewContext(ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') + '\nglobalThis.actions={' + names.join(',') + '}', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.actions
}
const ref = value => ({ value })
function publishedHarness(status, fail = false) {
  const calls = []
  const context = { qaCurrentPublishedVersionLoadSerial: 0, qaCurrentPublishedVersion: ref({ stale: true }), qaCurrentPublishedVersionLoadError: ref('old'), qaCurrentPublishedVersionLoading: ref(false), qaRegulationProjectStatusByDccId: ref(new Map([['10', status]])), resolveDccProjectId: String, resolveDccProjectCodeErrorMessage: e => e.message, QcTemplateApi: { getPublishedQaRegulationVersion: async id => { calls.push(id); if (fail) throw Error('read failed'); return { dccProjectCodeId: 10, lifecycleStatus: 'PUBLISHED', publishedVersionId: 20 } } } }
  return { context, calls, ...extract(['loadCurrentPublishedQaRegulationVersion'], context) }
}
test('DRAFT editing status with a formal pointer still loads the published version', async () => {
  const h = publishedHarness({ lifecycleStatus: 'DRAFT', currentVersionId: 20 })
  await h.loadCurrentPublishedQaRegulationVersion({ id: 10 })
  assert.deepEqual(h.calls, ['10'])
  assert.equal(h.context.qaCurrentPublishedVersion.value.publishedVersionId, 20)
})
test('no formal pointer clears stale published state without requesting a version', async () => {
  const h = publishedHarness({ lifecycleStatus: 'DRAFT' })
  await h.loadCurrentPublishedQaRegulationVersion({ id: 10 })
  assert.equal(h.calls.length, 0)
  assert.equal(h.context.qaCurrentPublishedVersion.value, undefined)
})
test('published version read failure is exposed and clears stale data and loading', async () => {
  const h = publishedHarness({ currentVersionId: 20 }, true)
  await assert.rejects(() => h.loadCurrentPublishedQaRegulationVersion({ id: 10 }), /read failed/)
  assert.match(h.context.qaCurrentPublishedVersionLoadError.value, /read failed/)
  assert.equal(h.context.qaCurrentPublishedVersion.value, undefined)
  assert.equal(h.context.qaCurrentPublishedVersionLoading.value, false)
})
function equipmentHarness() {
  const calls = [], row = { itemName: '尺寸', itemCode: 'I1', equipmentOptions: [] }
  const context = { qaEquipmentBaseline: new Map(), qaEquipmentDirtyRows: new Set(), qaMachineryOptions: ref([{ id: 1, code: 'E1', name: '卡尺' }]), qaRegulationItems: ref([row]), qaCurrentConfigurationLoading: ref(false), selectedQaRegulationVersionStatus: ref('DRAFT'), selectedQaRegulationVersionId: ref(2), qaCurrentPublishedVersion: ref(), qaEquipmentAutoSaving: ref(false), qaRegulationSaving: ref(false), qaRegulationPublishing: ref(false), canEditCommonRegulationBindings: ref(true), qaRegulationDraft: { dccProjectCodeId: 10 }, resolveDccProjectId: String, resolvePositiveId: Number, resolveRequiredText: s => s, resolveDccProjectCodeErrorMessage: e => e.message, ElMessage: { success() {}, error() {}, warning() {} }, toQaEquipmentOptions: saved => saved.equipmentGroups.map(g => ({ equipmentId: g.equipmentId, equipmentNumber: g.equipmentNumbers[0].equipmentNumber })), QcTemplateApi: { savePqcItemEquipmentConfigBatch: async payload => { calls.push(structuredClone(payload)); return payload } } }
  const names = ['resolvePositiveId', 'resolveRequiredText', 'rememberQaEquipmentBaseline', 'discardQaItemEquipment', 'getQaItemNameGroup', 'applyQaEquipmentOptionsToItemNameGroup', 'addQaItemEquipment', 'removeQaItemEquipment', 'handleQaItemEquipmentChange', 'saveQaItemEquipment']
  return { row, context, calls, ...extract(names, context) }
}
test('equipment add, select and delete remain local until explicit save', async () => {
  const h = equipmentHarness()
  h.addQaItemEquipment(h.row)
  h.handleQaItemEquipmentChange(h.row, 0, 1)
  h.removeQaItemEquipment(h.row, 0)
  assert.equal(h.calls.length, 0)
  await h.saveQaItemEquipment(h.row)
  assert.equal(h.calls.length, 1)
  assert.deepEqual(h.calls[0].equipmentGroups, [])
  assert.equal(h.context.qaEquipmentDirtyRows.size, 0)
})
test('discard restores equipment baseline without sending a save request', () => {
  const h = equipmentHarness()
  h.addQaItemEquipment(h.row)
  h.handleQaItemEquipmentChange(h.row, 0, 1)
  h.discardQaItemEquipment(h.row)
  assert.equal(h.row.equipmentOptions.length, 0)
  assert.equal(h.context.qaEquipmentDirtyRows.size, 0)
  assert.equal(h.calls.length, 0)
})

test('saved equipment number uses a positive order accepted by publication', async () => {
  const h = equipmentHarness()
  h.addQaItemEquipment(h.row)
  h.handleQaItemEquipmentChange(h.row, 0, 1)
  await h.saveQaItemEquipment(h.row)
  assert.equal(h.calls.length, 1)
  assert.equal(h.calls[0].equipmentGroups[0].sort, 1)
  assert.equal(h.calls[0].equipmentGroups[0].equipmentNumbers[0].sort, 1)
})


test('production-ready status alone requests the formal version', async () => {
  const h = publishedHarness({ lifecycleStatus: 'DRAFT', productionReady: true })
  await h.loadCurrentPublishedQaRegulationVersion({ id: 10 })
  assert.deepEqual(h.calls, ['10'])
  assert.equal(h.context.qaCurrentPublishedVersion.value.publishedVersionId, 20)
})
test('blank new equipment is rejected before request and remains dirty for retry', async () => {
  const h = equipmentHarness()
  h.addQaItemEquipment(h.row)
  assert.equal(h.row.equipmentOptions[0].equipmentId, undefined)
  await assert.rejects(() => h.saveQaItemEquipment(h.row), /检验设备不能为空/)
  assert.equal(h.calls.length, 0)
  assert.equal(h.context.qaEquipmentDirtyRows.has(h.row), true)
  assert.equal(h.context.qaEquipmentAutoSaving.value, false)
  h.handleQaItemEquipmentChange(h.row, 0, 1)
  await h.saveQaItemEquipment(h.row)
  assert.equal(h.calls.length, 1)
  assert.equal(h.calls[0].equipmentGroups[0].equipmentId, 1)
  assert.equal(h.context.qaEquipmentDirtyRows.size, 0)
})
test('failed blank equipment save can be discarded including original unbound reason', async () => {
  const h = equipmentHarness()
  h.row.equipmentUnboundReason = '原规程未要求设备'
  h.addQaItemEquipment(h.row)
  await assert.rejects(() => h.saveQaItemEquipment(h.row), /检验设备不能为空/)
  h.handleQaItemEquipmentChange(h.row, 0, 1)
  assert.equal(h.row.equipmentUnboundReason, undefined)
  h.discardQaItemEquipment(h.row)
  assert.equal(h.row.equipmentOptions.length, 0)
  assert.equal(h.row.equipmentUnboundReason, '原规程未要求设备')
  assert.equal(h.context.qaEquipmentDirtyRows.size, 0)
  assert.equal(h.calls.length, 0)
})
