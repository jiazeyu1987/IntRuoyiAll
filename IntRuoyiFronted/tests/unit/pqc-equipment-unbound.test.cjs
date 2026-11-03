const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const ref = value => ({ value })
function functionsFrom(filename, names, context) {
  const descriptor = parse(fs.readFileSync(filename, 'utf8'), { filename }).descriptor
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'unbound-test' }).errors, [])
  const source = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const statements = source.statements.filter(statement =>
    ts.isFunctionDeclaration(statement) ? names.includes(statement.name.text) :
      ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(source))))
  assert.equal(statements.length, names.length)
  const code = ts.transpileModule(statements.map(statement => statement.getText(source)).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code + '\nglobalThis.actions = {' + names.join(',') + '}', context)
  return context.actions
}
function parserHarness() {
  const first = { itemCode: 'I1', inspectionTool: '砝码', equipmentOptions: [], equipmentUnboundReason: '待登记' }
  const other = { ...structuredClone(first), itemCode: 'I2', equipmentUnboundReason: '另一项单独确认' }
  const draft = ref({ processes: [{ items: [first, other] }] })
  const saved = new Map(), messages = []
  const context = {
    item: ref(first), draft, confirmedUnbound: ref(['I1', 'I2']),
    machinery: ref([{ id: 10, code: 'EQ10', name: '15N砝码' }]),
    copy: value => structuredClone(value),
    message: { confirm: async text => messages.push(text), success() {}, error: text => messages.push(text) },
    localStorage: { setItem: (key, value) => saved.set(key, value), getItem: key => saved.get(key) },
    storageKey: () => 'own-test', projectId: ref(255), projects: ref([]), published: ref(false), busy: ref(false),
    dirty: ref(false), viewMode: ref('edit'), processIndex: ref(0), itemIndex: ref(0), trialSession: ref(0),
    error: ref(''), markSaved() {}, resetEditing() {}, confirmReplace: async () => true,
    run: async action => action(), loadResources: async () => {}
  }
  return { context, actions: functionsFrom('src/views/form-center/simple-pqc-parser/index.vue',
    ['selectEquipment', 'applyEquipment', 'reuseItem', 'saveLocal', 'restoreLocal'], context), saved, messages, first, other }
}
test('reusing a newly reasoned item never grants the historical unbound exemption', () => {
  const h = parserHarness()
  h.context.matchingItem = ref({ equipmentOptions: [], equipmentUnboundReason: '台账待登记' })
  h.actions.reuseItem()
  assert.equal(h.first.equipmentUnboundReason, '台账待登记')
  assert.equal(h.context.confirmedUnbound.value.includes('I1'), false)
  h.first.equipmentUnboundReason = '  '
  Object.assign(h.first, { itemName: '强度', standardText: '合格', inspectionMethod: '悬吊', samplingPlanText: '首检5件', applicableInspectionTypes: ['FIRST'], firstInspectionQuantity: 5 })
  Object.assign(h.context.draft.value, { dccProjectCodeId: 255, regulationName: '规程', regulationCode: 'PQC', finalInspectionNotApplicableReason: '原文无末检' })
  h.context.draft.value.processes[0].processName = '包装'
  h.context.draft.value.processes[0].items = [h.first]
  const target = { exports: {} }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/form-center/parser/simplePqc.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, target)
  assert.ok(target.exports.validateDraft(h.context.draft.value, h.context.confirmedUnbound.value).some(issue => issue.field === 'equipment'))
  h.context.matchingItem.value.equipmentUnboundReason = ''
  h.actions.reuseItem()
  assert.equal(h.context.confirmedUnbound.value.includes('I1'), true, 'only old empty-reason unbound items retain historical confirmation')
  h.context.matchingItem.value = { equipmentOptions: [{ equipmentId: 10, equipmentNumber: 'EQ10' }], equipmentUnboundReason: '历史残留' }
  h.actions.reuseItem()
  assert.equal(h.first.equipmentUnboundReason, undefined)
  assert.equal(h.context.confirmedUnbound.value.includes('I1'), false)
})
test('PQC selection clears only its reason; batch binding clears reasons and cannot batch exempt', async () => {
  const h = parserHarness()
  await assert.rejects(h.actions.applyEquipment(), /未绑定原因需逐项确认/)
  assert.equal(h.messages.length, 0)
  assert.equal(h.other.equipmentUnboundReason, '另一项单独确认')
  h.actions.selectEquipment([10])
  assert.equal(h.first.equipmentUnboundReason, undefined)
  assert.equal(h.other.equipmentUnboundReason, '另一项单独确认')
  assert.equal(h.context.confirmedUnbound.value.includes('I1'), false)
  await h.actions.applyEquipment()
  assert.equal(h.other.equipmentUnboundReason, undefined)
  assert.equal(h.other.equipmentOptions[0].equipmentNumber, 'EQ10')
  assert.equal(h.context.confirmedUnbound.value.length, 0)
  h.actions.selectEquipment([])
  assert.equal(h.first.equipmentUnboundReason, undefined, 'removing binding must not auto-fill a reason')
})
test('browser save and restore retain each manual reason without applying it to other items', async () => {
  const h = parserHarness()
  h.actions.saveLocal()
  h.first.equipmentUnboundReason = '未保存的修改'
  h.context.draft.value = undefined
  await h.actions.restoreLocal()
  assert.equal(h.context.draft.value.processes[0].items[0].equipmentUnboundReason, '待登记')
  assert.equal(h.context.draft.value.processes[0].items[1].equipmentUnboundReason, '另一项单独确认')
  assert.equal(h.context.draft.value.processes[0].items[0].inspectionTool, '砝码')
})
test('formal QA and common read-save mappings retain reasons and selected binding clears them', () => {
  const sourceItem = { itemCode: 'I1', itemSort: 1, itemName: '强度', inspectionMethod: '悬吊',
    inspectionTool: '砝码', samplingPlanText: '首检5件', standardText: '不脱落', resultType: 'BOOLEAN',
    failureRule: '', sourceNote: '', equipmentOptions: [], equipmentUnboundReason: '台账待登记' }
  const context = {
    finalInspectionRequired: ref(false),
    createQaItemInspectionState: () => ({}),
    resolveQaItemInspectionPayload: () => ({ applicableInspectionTypes: ['FIRST'], firstInspectionQuantity: 5 }),
    resolveRequiredText: value => value, resolvePositiveId: value => value,
    selectedCommonRegulationSetVersionDocuments: ref([{ documentKey: 'D1', processes: [{ processCode: 'P1', processName: '包装', sort: 1, items: [sourceItem] }] }]),
    createCommonRegulationItemSourceNote: () => '', formatCommonRegulationSetDocumentTitle: () => '通用',
    requireCommonMemberFinalInspectionApplicable: () => false,
    resolveCommonMemberInspectionRules: () => [],
    qaMachineryOptions: ref([{ id: 10, code: 'EQ10', name: '15N砝码' }]), autoSaveQaItemEquipment: () => {}
  }
  const actions = functionsFrom('src/views/mes/pro/processpool/QaRegulationPage.vue', [
    'flattenQaRegulationProcesses', 'buildQaRegulationSaveItem', 'buildCommonRegulationItemsFromDocuments',
    'buildCommonRegulationSaveItem', 'buildCommonRegulationItemEquipmentOptions',
    'handleQaItemEquipmentChange', 'handleCommonRegulationItemEquipmentChange'
  ], context)
  const row = actions.flattenQaRegulationProcesses([{ processCode: 'P1', processName: '包装', sort: 1, items: [sourceItem] }])[0]
  assert.equal(row.equipmentUnboundReason, sourceItem.equipmentUnboundReason)
  assert.equal(actions.buildQaRegulationSaveItem(row, 1).equipmentUnboundReason, sourceItem.equipmentUnboundReason)
  const common = actions.buildCommonRegulationItemsFromDocuments()[0]
  assert.equal(common.equipmentUnboundReason, sourceItem.equipmentUnboundReason)
  assert.equal(actions.buildCommonRegulationSaveItem(common, 1).equipmentUnboundReason, sourceItem.equipmentUnboundReason)
  row.equipmentOptions.push({ equipmentId: 10 })
  actions.handleQaItemEquipmentChange(row, 0, 10)
  assert.equal(row.equipmentUnboundReason, undefined)
  common.equipmentOptions.push({ equipmentId: 10 })
  actions.handleCommonRegulationItemEquipmentChange(common, 0, 10)
  assert.equal(common.equipmentUnboundReason, undefined)
})

test('PQC preflight reports missing MDM binding, retains selected metadata and refreshes stale project data', async () => {
  const { computed, ref: reactiveRef } = require('vue')
  let current = { id: 255, projectName: '测试项目', productMasterId: null }
  const context = {
    computed, projectId: reactiveRef(255), projects: reactiveRef([current]), error: ref(''),
    getProjectCodePage: async () => ({ list: [{ id: 300, projectName: '其他候选', productMasterId: 10 }] }),
    getProjectCode: async id => { assert.equal(id, 255); return current },
    QcTemplateApi: { getCurrentQaRegulation: async () => null, listQaRegulationVersions: async () => [] },
    DvMachineryApi: { getMachineryPage: async () => ({ list: [], total: 0 }) },
    baseline: ref(null), machinery: ref([])
  }
  const actions = functionsFrom('src/views/form-center/simple-pqc-parser/index.vue',
    ['projectIssue', 'searchProjects', 'loadResources'], context)
  assert.match(actions.projectIssue.value.message, /尚未绑定MDM产品/)
  assert.equal(actions.projectIssue.value.target, 'project')
  await actions.searchProjects('其他候选')
  assert.equal(context.projects.value[0].id, 255)
  assert.match(actions.projectIssue.value.message, /尚未绑定MDM产品/)
  current = { ...current, productMasterId: 9 }
  await actions.loadResources()
  assert.equal(actions.projectIssue.value, undefined, 'fresh server project data replaces stale null metadata')
  context.projects.value = [{ ...current, productMasterId: undefined }]
  assert.match(actions.projectIssue.value.message, /信息未载入/)
  assert.doesNotMatch(actions.projectIssue.value.message, /尚未绑定/)
  current = { ...current, productMasterId: null }
  await actions.loadResources()
  assert.match(actions.projectIssue.value.message, /尚未绑定MDM产品/, 'fresh unbinding cannot reuse a cached product ID')
})
