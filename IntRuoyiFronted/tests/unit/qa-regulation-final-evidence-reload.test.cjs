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
const names = ['replaceQaInspectionTypeRules', 'applyQaRegulationConfiguration',
  'buildQaRegulationSavePayload', 'resolveRequiredText']
const functions = source.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(source))))
assert.equal(functions.length, names.length)
const code = ts.transpileModule(functions.map(statement => statement.getText(source)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function harness() {
  const ref = value => ({ value })
  const context = {
    qaRegulationDraft: { dccProjectCodeId: 10 }, qaInspectionTypeRules: [],
    commonRegulationBindingRows: ref([]), commonRegulationBindingRowSerial: 0,
    qaRegulationItems: ref([]), qaConfigurationExists: ref(false),
    qaPublishedVersionNo: ref(''), selectedQaRegulationVersionId: ref(), qaItemsQuery: {},
    flattenQaRegulationProcesses: () => [], resolvePositiveId: Number,
    resolveQaRegulationDraftVersionNoForSave: () => 'B/3', buildQaRegulationProcesses: () => []
  }
  vm.runInNewContext(code + '\nglobalThis.actions={applyQaRegulationConfiguration,buildQaRegulationSavePayload}', context)
  return { ...context.actions, context }
}

function configuration(overrides = {}) {
  return { dccProjectCodeId: 10, regulationId: 79, regulationCode: 'PQC-IDI-001',
    regulationName: '压力泵组装过程检验规程', versionNo: 'B/2', lifecycleStatus: 'PUBLISHED',
    publishedVersionId: 101, finalInspectionApplicable: false,
    finalInspectionNotApplicableReason: '原文仅规定首检和过程巡检，未规定末检项目。',
    inspectionTypeRules: [{ key: 'FIRST', required: true }, { key: 'FINAL', required: false }],
    processes: [], ...overrides }
}

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
