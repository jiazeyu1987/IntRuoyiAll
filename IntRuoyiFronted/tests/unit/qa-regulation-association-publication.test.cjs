const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/QaRegulationPage.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const script = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['addCommonRegulationBindingRow', 'removeCommonRegulationBindingRow',
  'changeCommonRegulationBindingRow', 'runQaPublishPrecheck', 'buildQaRegulationSavePayload',
  'previewQaRegulationDraft']
const functions = script.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(script))))
const code = ts.transpileModule(functions.map(statement => statement.getText(script)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const ref = value => ({ value })
function harness({ failPublish = false, failRefresh = false } = {}) {
  const calls = [], messages = []
  const context = {
    qaEquipmentAutoSaving: ref(false), qaEquipmentDirtyRows: new Set(), loadedQaVersionId: { value: undefined }, selectedDccProjectCodeLabel: { value: '项目' }, ElMessageBox: { confirm: async () => {} },
    commonRegulationBindingRows: ref([{ rowKey: 1, commonRegulationVersionId: 11,
      commonRegulationCode: 'PQC-C-001', commonRegulationName: '通用检验规程', versionNo: 'A/1' }]),
    commonRegulationBindingRowSerial: 1,
    commonRegulationVersionOptions: ref([
      { commonRegulationVersionId: 11, commonRegulationCode: 'PQC-C-001', commonRegulationName: '通用检验规程', versionNo: 'A/1' },
      { commonRegulationVersionId: 22, commonRegulationCode: 'PQC-C-001', commonRegulationName: '通用检验规程', versionNo: 'A/2' }
    ]),
    commonRegulationBindingLoading: ref(false), commonRegulationBindingLoadError: ref(''),
    commonRegulationVersionOptionsLoading: ref(false), commonRegulationVersionOptionsLoadError: ref(''),
    qaPublishBlockers: ref([]), qaRegulationPublishing: ref(false),
    qaRegulationSaving: ref(false), qaConfigurationExists: ref(false), selectedQaRegulationVersionId: ref(),
    qaCurrentPublishedVersion: ref(null), qaRegulationProjectStatusByDccId: ref(new Map()),
    qaCurrentConfigurationLoading: ref(false), qaCurrentConfigurationLoadError: ref(''),
    qaRegulationDraft: { dccProjectCodeId: 10, regulationCode: 'QA-10', regulationName: 'QA',
      loadedVersionNo: 'B/3', lifecycleStatus: 'DRAFT', draftRevision: 2 },
    qaRegulationItems: ref([{ finalInspectionEnabled: true }]), qaInspectionTypeRules: [],
    resolveRequiredText: value => value, resolveQaRegulationDraftVersionNoForSave: () => 'B/3',
    resolveDccProjectId: String,
    buildQaRegulationProcesses: () => [],
    resolvePositiveId: value => { if (!(Number(value) > 0)) throw Error('请选择规程'); return Number(value) },
    resolveDccProjectCodeErrorMessage: error => error.message,
    ElMessage: { success: text => messages.push(text), error: text => messages.push(text), warning: text => messages.push(text) },
    QcTemplateApi: { getCommonRegulationPublishedVersion: async id => ({ publishedVersionId: id,
      regulationCode: 'PQC-C-001', regulationName: '通用检验规程', versionNo: id === 11 ? 'A/1' : 'A/2' }), saveQaRegulationDraft: async payload => {
      calls.push(structuredClone(payload))
      return { regulationId: 1, draftVersionId: 30, versionNo: 'B/3', draftRevision: 3, lifecycleStatus: 'DRAFT' }
    }, publishQaRegulation: async payload => {
      calls.push(structuredClone(payload))
      if (failPublish) throw Error('server rejected')
      return { regulationId: 1, publishedVersionId: 30, versionNo: 'B/3' }
    } },
    applyQaRegulationConfiguration: () => {},
    loadCurrentCommonRegulationBinding: async () => { if (failRefresh) throw Error('refresh unavailable') },
    loadQaRegulationVersionOptions: async () => {}
  }
  vm.runInNewContext(code + '\nglobalThis.actions = {' + names.join(',') + '}', context)
  return { ...context.actions, context, calls, messages }
}
test('add, replace and delete stay local until the single publication action', async () => {
  const h = harness()
  h.addCommonRegulationBindingRow()
  h.changeCommonRegulationBindingRow(h.context.commonRegulationBindingRows.value[1], 22)
  h.removeCommonRegulationBindingRow(1)
  assert.equal(h.calls.length, 0)
  await h.runQaPublishPrecheck()
  assert.deepEqual(h.calls[0].commonRegulationVersionIds, [22])
})
test('deleting all rows publishes an explicit empty association snapshot', async () => {
  const h = harness()
  h.removeCommonRegulationBindingRow(1)
  await h.runQaPublishPrecheck()
  assert.deepEqual(h.calls[0].commonRegulationVersionIds, [])
})
test('an incomplete new row blocks publication and preserves the edit', async () => {
  const h = harness()
  h.addCommonRegulationBindingRow()
  await h.runQaPublishPrecheck()
  assert.equal(h.calls.length, 0)
  assert.equal(h.context.commonRegulationBindingRows.value.length, 2)
})
test('publication failure preserves pending associations and clears the loading state', async () => {
  const h = harness({ failPublish: true })
  h.removeCommonRegulationBindingRow(1)
  await h.runQaPublishPrecheck()
  assert.equal(h.context.commonRegulationBindingRows.value.length, 0)
  assert.equal(h.context.qaRegulationPublishing.value, false)
  assert.match(h.messages[0], /发布失败/)
})
test('refresh failure after commit is reported as published with a refresh error', async () => {
  const h = harness({ failRefresh: true })
  await h.runQaPublishPrecheck()
  assert.match(h.messages[0], /已发布，但页面刷新失败/)
  assert.equal(h.context.qaCurrentPublishedVersion.value.versionNo, 'B/3')
})

test('the shared draft/publication payload retains exact versions, order, replacements and explicit clearing', () => {
  const h = harness()
  h.context.commonRegulationBindingRows.value = [
    { rowKey: 1, commonRegulationVersionId: 22 }, { rowKey: 2, commonRegulationVersionId: 11 }
  ]
  const ids = () => Array.from(h.buildQaRegulationSavePayload().commonRegulationVersionIds)
  assert.deepEqual(ids(), [22, 11])
  h.changeCommonRegulationBindingRow(h.context.commonRegulationBindingRows.value[1], 22)
  h.removeCommonRegulationBindingRow(1)
  assert.deepEqual(ids(), [22])
  h.removeCommonRegulationBindingRow(2)
  assert.deepEqual(ids(), [])
})

test('loading or failed association/configuration reads block saving to avoid accidental clearing', () => {
  for (const key of ['commonRegulationBindingLoading', 'commonRegulationVersionOptionsLoading',
    'qaCurrentConfigurationLoading', 'commonRegulationBindingLoadError',
    'commonRegulationVersionOptionsLoadError', 'qaCurrentConfigurationLoadError']) {
    const h = harness()
    h.context[key].value = key.endsWith('Error') ? 'read failed' : true
    assert.throws(() => h.buildQaRegulationSavePayload())
  }
})

test('save draft submits selected associations and subsequent explicit clearing without publication', async () => {
  const h = harness()
  h.addCommonRegulationBindingRow()
  h.changeCommonRegulationBindingRow(h.context.commonRegulationBindingRows.value[1], 22)
  await h.previewQaRegulationDraft()
  assert.deepEqual(h.calls[0].commonRegulationVersionIds, [11, 22])
  assert.equal(h.calls[0].expectedDraftRevision, 2)
  h.removeCommonRegulationBindingRow(1)
  await h.previewQaRegulationDraft()
  assert.deepEqual(h.calls[1].commonRegulationVersionIds, [22])
  assert.equal(h.calls[1].expectedDraftRevision, 3)
  h.removeCommonRegulationBindingRow(2)
  await h.previewQaRegulationDraft()
  assert.deepEqual(h.calls[2].commonRegulationVersionIds, [])
  assert.equal(h.context.qaCurrentPublishedVersion.value, null)
})

test('canceling publication sends no publish request and releases the lock', async () => {
  const h = harness()
  h.context.ElMessageBox.confirm = async () => { throw 'cancel' }
  await h.runQaPublishPrecheck()
  assert.equal(h.calls.length, 0)
  assert.equal(h.context.qaRegulationPublishing.value, false)
})

test('repeated publish clicks share one pending confirmation and one request', async () => {
  const h = harness()
  let confirmCount = 0, accept
  h.context.ElMessageBox.confirm = () => { confirmCount++; return new Promise(resolve => { accept = resolve }) }
  const first = h.runQaPublishPrecheck()
  await h.runQaPublishPrecheck()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(confirmCount, 1)
  assert.equal(h.calls.length, 0)
  accept()
  await first
  assert.equal(h.calls.length, 1)
  assert.equal(h.context.qaRegulationPublishing.value, false)
})

test('historical associations absent from current options retain their exact version in preview and publication', async () => {
  const h = harness()
  h.context.commonRegulationVersionOptions.value = []
  let queried, confirmation
  h.context.QcTemplateApi.getCommonRegulationPublishedVersion = async id => {
    queried = id
    return { publishedVersionId: id, regulationCode: 'PQC-CR-003', regulationName: '初包装', versionNo: 'A/7', lifecycleStatus: 'RETIRED' }
  }
  h.context.ElMessageBox.confirm = async text => { confirmation = text }
  await h.runQaPublishPrecheck()
  assert.equal(queried, 11)
  assert.match(confirmation, /PQC-CR-003 初包装 A\/7/)
  assert.deepEqual(h.calls[0].commonRegulationVersionIds, [11])
})

test('historical association lookup failure is visible and releases the publication lock', async () => {
  const h = harness()
  h.context.QcTemplateApi.getCommonRegulationPublishedVersion = async () => { throw Error('history unavailable') }
  await h.runQaPublishPrecheck()
  assert.equal(h.calls.length, 0)
  assert.equal(h.context.qaRegulationPublishing.value, false)
  assert.match(h.messages[0], /history unavailable/)
})
