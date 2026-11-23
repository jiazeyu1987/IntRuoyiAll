const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { test } = require('node:test')
const ts = require('typescript')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const source = relative => fs.readFileSync(path.join(__dirname, '../../src/views/mes/pro', relative), 'utf8')
const frontline = source('feedback/FrontlineFixedTemplatePanel.vue')
const leader = source('processpool/TeamLeaderWorkbenchPage.vue')
const correction = source('feedback/FrontlineReturnCorrectionPanel.vue')
const detail = source('processpool/components/ActiveOrderSubmissionDetailPanel.vue')
function load(text, name, bindings) {
  const script = parse(text).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  let declaration
  for (const statement of ast.statements) {
    if (!ts.isVariableStatement(statement)) continue
    declaration = statement.declarationList.declarations.find(item => item.name.getText(ast) === name)
    if (declaration) break
  }
  assert.ok(declaration, name)
  const js = ts.transpileModule('const ' + declaration.getText(ast) + ';', {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  return new Function(...Object.keys(bindings), js + '; return ' + name)(...Object.values(bindings))
}
function result(values, scrap, item = { key: 'A', type: 'choice' }) {
  return load(frontline, 'resolvePqcResultForTask', {
    FRONTLINE_PQC_RESULTS: { DETECTION_FAILED: 'FAILURE', DETECTION_SUCCESS: 'SUCCESS' },
    getPqcTaskDraft: () => ({ scrapQuantity: scrap }), normalizePqcQuantity: Number,
    mapPqcInspectionItem: value => value, getPqcCurrentChoiceValuesForTask: () => values
  })({ inspectionItems: [item] })
}
test('passing samples remain passing with zero or positive loss', () => {
  for (const scrap of [0, 1, 5]) assert.equal(result(['合格', '合格'], scrap), 'SUCCESS')
})
test('failed samples fail independently of loss', () => {
  for (const scrap of [0, 1]) assert.equal(result(['合格', '不合格'], scrap), 'FAILURE')
})
test('numeric limits still determine the result', () => {
  const item = { key: 'A', type: 'number', standardLowerLimit: 1, standardUpperLimit: 2 }
  assert.equal(result(['1.5'], 1, item), 'SUCCESS')
  assert.equal(result(['2.1'], 0, item), 'FAILURE')
})
test('correction accepts failed samples with zero loss and preserves both facts', () => {
  const build = load(leader, 'buildPqcCorrectionRequest', {
    getPqcCorrectionActualQuantity: () => 2,
    correctionForm: { eventId: 7, pqcScrapQuantity: 0, pqcItemResults: [
      { itemCode: 'A', sampleQuantity: 2, sampleValuesText: '不合格,合格' }
    ] },
    requireCorrectionAuditFields: () => ({ reason: '修正检验记录' }),
    parsePqcSampleText: text => text.split(','), normalizePositiveNumber: value => value,
    requirePositiveNumber: value => value
  })
  assert.deepEqual(build(), { eventId: 7, actualInspectionQuantity: 2, scrapQuantity: 0,
    itemResults: [{ itemCode: 'A', selectedEquipmentId: undefined, selectedEquipmentNumber: undefined,
      sampleValues: ['不合格', '合格'] }], reason: '修正检验记录' })
  assert.doesNotMatch(leader, /损耗数量不能小于逐件不合格数量/)
})
test('edited Vue scripts and templates compile', () => {
  for (const [filename, text] of [['FrontlineFixedTemplatePanel.vue', frontline], ['TeamLeaderWorkbenchPage.vue', leader], ['FrontlineReturnCorrectionPanel.vue', correction], ['ActiveOrderSubmissionDetailPanel.vue', detail]]) {
    const parsed = parse(text, { filename })
    assert.deepEqual(parsed.errors, [])
    const script = ts.transpileModule(parsed.descriptor.scriptSetup.content, {
      compilerOptions: { target: ts.ScriptTarget.ES2022 }, reportDiagnostics: true
    })
    assert.deepEqual(script.diagnostics, [])
    assert.deepEqual(compileTemplate({ source: parsed.descriptor.template.content, filename, id: 'pqc-loss' }).errors, [])
  }
})

test('historical recorded judgement stays distinct from sample summary', () => {
  assert.match(leader, /v-if="detail.pqcResult \|\| detail.pqcSummary" label="本条记录的检验结论"/)
  assert.match(leader, /formatPqcJudgement\(detail.pqcSummary \|\| detail.pqcResult\)/)
  assert.match(detail, /样本检验结论[\s\S]*pqcInspectionRecordSummary.judgementText/)
  assert.match(frontline, /人工发起NCR并上传资料，办理让步放行、作废或返工/)
})
