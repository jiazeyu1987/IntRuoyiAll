const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/QaRegulationPage.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const source = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['createEmptyQaInspectionTypeRules', 'buildQaInspectionTypeRules',
  'replaceQaInspectionTypeRules', 'applyQaRegulationConfiguration',
  'flattenQaRegulationProcesses', 'buildQaRegulationSavePayload', 'resolveRequiredText',
  'resolveCommonMemberInspectionRules', 'buildCommonRegulationItemsUpgradePayload']
const functions = source.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(source))))
assert.equal(functions.length, names.length)
const code = ts.transpileModule(functions.map(statement => statement.getText(source)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const inspectionModule = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(path.dirname(filename),
  'qaRegulationItemInspection.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, inspectionModule)

function harness() {
  const ref = value => ({ value })
  const context = {
    qaRegulationDraft: { dccProjectCodeId: 10 }, qaInspectionTypeRules: [],
    commonRegulationBindingRows: ref([]), commonRegulationBindingRowSerial: 0,
    qaCurrentConfigurationLoading: ref(false), qaCurrentConfigurationLoadError: ref(''),
    commonRegulationBindingLoading: ref(false), commonRegulationBindingLoadError: ref(''),
    commonRegulationVersionOptionsLoading: ref(false), commonRegulationVersionOptionsLoadError: ref(''),
    qaRegulationItems: ref([]), qaConfigurationExists: ref(false),
    qaPublishedVersionNo: ref(''), selectedQaRegulationVersionId: ref(), qaItemsQuery: {},
    createQaItemInspectionState: inspectionModule.exports.createQaItemInspectionState,
    resolvePositiveId: Number,
    resolveDccProjectId: String,
    resolveQaRegulationDraftVersionNoForSave: () => 'B/3', buildQaRegulationProcesses: () => [],
    selectedCommonRegulationSet: ref({ id: 20 }),
    selectedCommonRegulationSetVersionPreview: ref({ id: 21, versionNo: 'A/1' }),
    selectedCommonRegulationSetVersionMembers: ref([]), commonRegulationItems: ref([]),
    commonMemberFinalInspectionReasons: ref({}),
    resolveCommonSetNextVersionNo: () => 'A/2', resolveCommonMemberNextVersionNo: async () => 'B/3',
    buildCommonRegulationSaveProcesses: () => []
  }
  vm.runInNewContext(code + '\nglobalThis.actions={applyQaRegulationConfiguration,buildQaRegulationSavePayload,buildCommonRegulationItemsUpgradePayload}', context)
  return { ...context.actions, context }
}

function configuration(overrides = {}) {
  return { dccProjectCodeId: 10, regulationId: 79, regulationCode: 'PQC-IDI-001',
    regulationName: '压力泵组装过程检验规程', versionNo: 'B/2', lifecycleStatus: 'PUBLISHED',
    publishedVersionId: 101, finalInspectionApplicable: false,
    finalInspectionNotApplicableReason: '原文仅规定首检和过程巡检，未规定末检项目。',
    inspectionTypeRules: [{ key: 'FIRST', required: true }, { key: 'FINAL', required: false }],
    processes: [{ items: [{ applicableInspectionTypes: overrides.finalInspectionApplicable
      ? ['FIRST', 'FINAL'] : ['FIRST'] }] }], ...overrides }
}

test('reloading version selections replaces association order and clears rows between products', () => {
  const h = harness()
  h.applyQaRegulationConfiguration(configuration({ commonRegulationVersions: [
    { commonRegulationVersionId: 22 }, { commonRegulationVersionId: 11 }
  ] }))
  assert.deepEqual(Array.from(h.buildQaRegulationSavePayload().commonRegulationVersionIds), [22, 11])
  h.context.qaRegulationDraft.dccProjectCodeId = 20
  h.applyQaRegulationConfiguration(configuration({ dccProjectCodeId: 20, commonRegulationVersions: [] }))
  assert.deepEqual(Array.from(h.buildQaRegulationSavePayload().commonRegulationVersionIds), [])
  assert.equal(h.buildQaRegulationSavePayload().dccProjectCodeId, '20')
  assert.throws(() => h.applyQaRegulationConfiguration(configuration({ dccProjectCodeId: 10 })), /不一致/)
})

test('published top-level evidence survives reload and the next association publication payload', () => {
  const h = harness(), original = configuration()
  h.applyQaRegulationConfiguration(original)
  const payload = h.buildQaRegulationSavePayload()
  assert.equal(payload.finalInspectionNotApplicableReason, original.finalInspectionNotApplicableReason)
  assert.equal(payload.finalInspectionApplicable, false)
  assert.equal(payload.inspectionTypeRules.find(rule => rule.key === 'FINAL').notApplicableReason,
    original.finalInspectionNotApplicableReason)
  assert.equal(original.inspectionTypeRules[1].notApplicableReason, undefined)
})

test('switching documents replaces the prior evidence and honors explicit applicability', () => {
  const h = harness()
  h.applyQaRegulationConfiguration(configuration())
  h.applyQaRegulationConfiguration(configuration({ finalInspectionApplicable: true,
    finalInspectionNotApplicableReason: undefined }))
  const payload = h.buildQaRegulationSavePayload()
  assert.equal(payload.finalInspectionApplicable, true)
  assert.equal(payload.finalInspectionNotApplicableReason, undefined)
  assert.equal(payload.inspectionTypeRules.find(rule => rule.key === 'FINAL').required, true)
})

test('missing evidence still blocks publication rather than inventing a reason', () => {
  const h = harness()
  h.applyQaRegulationConfiguration(configuration({ finalInspectionNotApplicableReason: '' }))
  assert.throws(() => h.buildQaRegulationSavePayload(), /末检不适用依据不能为空/)
})

test('item switches determine final applicability and disabling the last process requires explicit evidence', () => {
  const h = harness()
  h.applyQaRegulationConfiguration(configuration({ finalInspectionApplicable: true,
    finalInspectionNotApplicableReason: undefined }))
  h.context.qaRegulationItems.value[0].finalInspectionEnabled = false
  assert.throws(() => h.buildQaRegulationSavePayload(), /末检不适用依据不能为空/)
  h.context.qaRegulationDraft.finalInspectionNotApplicableReason = '  原规程修订后取消末检  '
  const payload = h.buildQaRegulationSavePayload()
  assert.equal(payload.finalInspectionApplicable, false)
  assert.equal(payload.finalInspectionNotApplicableReason, '原规程修订后取消末检')
  assert.equal(payload.inspectionTypeRules.find(rule => rule.key === 'FINAL').notApplicableReason,
    '原规程修订后取消末检')
  h.context.qaRegulationItems.value[0].finalInspectionEnabled = true
  assert.equal(h.buildQaRegulationSavePayload().finalInspectionNotApplicableReason, undefined)
})

test('common set upgrade preserves each document evidence and does not invent missing reasons', async () => {
  const h = harness()
  const member = { commonRegulationId: 79, commonRegulationVersionId: 101,
    commonRegulationCode: 'COMMON-1', commonRegulationName: '初包装',
    inspectionTypeRules: configuration().inspectionTypeRules,
    finalInspectionNotApplicableReason: '原规程仅要求首检和巡检' }
  h.context.selectedCommonRegulationSetVersionMembers.value = [member]
  h.context.commonMemberFinalInspectionReasons.value[101] = member.finalInspectionNotApplicableReason
  h.context.commonRegulationItems.value = [{ commonRegulationVersionId: 101, finalInspectionEnabled: false }]
  let payload = await h.buildCommonRegulationItemsUpgradePayload()
  assert.equal(payload.members[0].finalInspectionNotApplicableReason, member.finalInspectionNotApplicableReason)
  assert.equal(payload.members[0].inspectionTypeRules.find(rule => rule.key === 'FINAL').notApplicableReason,
    member.finalInspectionNotApplicableReason)
  h.context.commonMemberFinalInspectionReasons.value[101] = ''
  await assert.rejects(() => h.buildCommonRegulationItemsUpgradePayload(), /初包装末检不适用依据不能为空/)
  h.context.commonMemberFinalInspectionReasons.value[101] = '修订后取消末检，按首检和巡检执行'
  payload = await h.buildCommonRegulationItemsUpgradePayload()
  assert.equal(payload.members[0].finalInspectionNotApplicableReason, '修订后取消末检，按首检和巡检执行')
  assert.equal(member.finalInspectionNotApplicableReason, '原规程仅要求首检和巡检')
  h.context.commonRegulationItems.value[0].finalInspectionEnabled = true
  payload = await h.buildCommonRegulationItemsUpgradePayload()
  assert.equal(payload.members[0].finalInspectionApplicable, true)
  assert.equal(payload.members[0].finalInspectionNotApplicableReason, undefined)
})
