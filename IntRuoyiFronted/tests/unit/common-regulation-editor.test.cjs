const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
function load(file, dependencies = {}) {
  const ctx = { exports: {}, require: name => {
    if (!(name in dependencies)) throw new Error(`Unexpected dependency ${name}`)
    return dependencies[name]
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, ctx)
  return ctx.exports
}
const pqc = load('src/views/form-center/parser/simplePqc.ts')
const common = load('src/views/form-center/simple-common-parser/draft.ts', { '../parser/simplePqc': pqc })
const { createCommonEditorDraft, commonEditorIssues, normalizeCommonEditorDraft } = load(
  'src/views/mes/pro/processpool/commonRegulationEditor.ts',
  { '@/views/form-center/parser/simplePqc': pqc, '@/views/form-center/simple-common-parser/draft': common }
)
const options = [
  { versionNo: 'A/1', lifecycleStatus: 'RETIRED' },
  { versionNo: 'A/2', lifecycleStatus: 'PUBLISHED' }
]
function version(status = 'PUBLISHED') {
  return {
    regulationId: 12, publishedVersionId: 22, regulationCode: 'PQC-CR-003', regulationName: '初包装过程检验规程',
    versionNo: 'A/2', lifecycleStatus: status, draftRevision: 5, effectiveDate: '2025-04-10',
    finalInspectionApplicable: false, finalInspectionNotApplicableReason: '原规程无末检',
    inspectionTypeRules: [{key: 'FINAL', inspectionType: 'FINAL', required: false, notApplicableReason: '原规程无末检'}],
    processes: [{ processCode: 'P1', processName: '包装', sort: 1, items: [{
      itemCode: 'I1', itemName: '密封', itemSort: 1, standardText: '密封完整', inspectionMethod: '检查',
      inspectionTool: '测试仪', samplingPlanText: '首检5件', resultType: 'BOOLEAN', applicableInspectionTypes: ['FIRST'],
      firstInspectionQuantity: 5, sourceOriginalPage: 3, sourceOriginalExcerpt: '原文要求',
      equipmentOptions: [{equipmentId: 8, equipmentCode: 'E8', equipmentName: '测试仪', equipmentNumber: 'N8', defaultFlag: true, sort: 1}]
    }] }]
  }
}
test('editing a published regulation creates a new version and preserves equipment and provenance', () => {
  const original = version()
  const draft = createCommonEditorDraft(original, options)
  assert.equal(draft.versionNo, 'A/3')
  assert.equal(draft.expectedDraftRevision, 0)
  assert.equal(draft.ownerModule, 'MES_QA_COMMON')
  assert.equal(draft.dccProjectCodeId, undefined)
  draft.processes[0].items[0].standardText = '新要求'
  assert.equal(original.processes[0].items[0].standardText, '密封完整')
  const payload = normalizeCommonEditorDraft(draft)
  assert.equal(payload.processes[0].items[0].equipmentOptions[0].equipmentNumber, 'N8')
  assert.equal(payload.processes[0].items[0].sourceOriginalPage, 3)
  assert.equal(commonEditorIssues(payload).length, 0)
  assert.equal('commonRegulationVersionIds' in payload, false)
})
test('resuming a draft preserves its version and concurrency revision', () => {
  const draft = createCommonEditorDraft(version('DRAFT'), options)
  assert.equal(draft.versionNo, 'A/2')
  assert.equal(draft.expectedDraftRevision, 5)
})

test('common editor preserves patrol matrix sampling and keeps AQL when patrol is removed', () => {
  const original = version()
  const item = original.processes[0].items[0]
  item.applicableInspectionTypes = ['FIRST', 'PATROL']
  item.inspectionLevel = 'S3'
  item.aql = 0.4
  const draft = createCommonEditorDraft(original, options)
  const patrol = normalizeCommonEditorDraft(draft).processes[0].items[0]
  assert.equal(patrol.inspectionLevel, 'S3')
  assert.equal(patrol.aql, 0.4)
  assert.equal(commonEditorIssues(draft).length, 0)
  draft.processes[0].items[0].applicableInspectionTypes = ['FIRST']
  const firstOnly = normalizeCommonEditorDraft(draft).processes[0].items[0]
  assert.equal(firstOnly.inspectionLevel, undefined)
  assert.equal(firstOnly.aql, 0.4)
  assert.equal(original.processes[0].items[0].inspectionLevel, 'S3')
})
test('validation rejects incomplete equipment snapshots, duplicate item codes and missing final inspection items', () => {
  const draft = createCommonEditorDraft(version(), options)
  draft.processes[0].items[0].equipmentOptions[0].equipmentNumber = ''
  assert.ok(commonEditorIssues(draft).some(message => message.includes('设备编号')), JSON.stringify(commonEditorIssues(draft)))
  draft.processes[0].items[0].equipmentOptions = []
  assert.equal(commonEditorIssues(draft).length, 0)
  draft.processes[0].items.push({...draft.processes[0].items[0]})
  assert.ok(commonEditorIssues(draft).some(message => message.includes('检验项目编码')))
  draft.finalInspectionApplicable = true
  assert.ok(commonEditorIssues(draft).some(message => message.includes('末检项目')))
})
