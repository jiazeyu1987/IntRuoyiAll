const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const base = 'src/views/mes/pro/processpool/components/'
function load(file, imports = {}) {
  const source = read(file)
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(n => !ts.isImportDeclaration(n)).map(n => n.getText(ast)).join('\n')
  const js = ts.transpileModule(body, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const exports = {}
  Function('exports', ...Object.keys(imports), js)(exports, ...Object.values(imports))
  return exports
}
const { parseExactIntegerJson } = load('src/utils/exactIntegerJson.ts')
const { readBusinessEvidence } = load(base + 'businessEvidenceDisplay.ts', { parseExactIntegerJson })
const { verificationStatusLabel, nonconformanceDispositionLabel, pickDocumentStatusLabel } = load(base + 'activeOrderDisplayLabels.ts')

test('active order statuses display Chinese and reject unmapped English states', () => {
  assert.equal(verificationStatusLabel('VALID'), '核验通过')
  assert.equal(verificationStatusLabel('INCOMPLETE'), '证据不完整')
  assert.equal(verificationStatusLabel('MISMATCH'), '核验不一致')
  assert.equal(nonconformanceDispositionLabel('rework'), '返工')
  assert.equal(nonconformanceDispositionLabel('pending_review'), '待评审')
  assert.equal(pickDocumentStatusLabel('C'), '已审核')
  assert.equal(pickDocumentStatusLabel('已审核'), '已审核')
  assert.throws(() => verificationStatusLabel('NEW_STATUS'), /核验状态缺少中文描述/)
})

test('evidence translates status descriptions while preserving business codes', () => {
  const result = readBusinessEvidence(JSON.stringify({
    verificationStatus: 'VALID', eventType: 'PQC_INSPECTION', disposition: 'rework',
    actionCode: 'QA_DISPOSITION', workOrderCode: 'SIM-COPY-001'
  }))
  assert.equal(result.error, '')
  assert.deepEqual(result.rows.map(row => row.value), ['核验通过', '过程质量检验', '返工', '质量保证处置', 'SIM-COPY-001'])
})
test('nested hashes are omitted while business values and exact IDs remain', () => {
  const result = readBusinessEvidence('{"contentHash":"secret","businessContent":{"quantity":12,"evidenceHash":"secret","userId":9223372036854775806}}')
  assert.equal(result.error, '')
  assert.deepEqual(result.rows.map(r => r.value), ['12', '9223372036854775806'])
})
test('deviation categories are readable Chinese names', () => {
  assert.deepEqual(readBusinessEvidence('["PRODUCTION_PROCESS","INSPECTION"]', 'categoryCodesJson').rows.map(r => r.value), ['生产过程', '检验'])
})
test('nested JSON strings become business fields, not serialized text', () => {
  const result = readBusinessEvidence(JSON.stringify({ investigationMembersJson: JSON.stringify([{ name: '张三' }]), capaAttachmentsJson: JSON.stringify([{ fileName: '调查报告.pdf', contentHash: 'secret' }]) }))
  assert.equal(result.error, '')
  assert.deepEqual(result.rows.map(r => r.value), ['张三', '调查报告.pdf'])
})
test('malformed evidence reports failure without leaking raw data', () => {
  for (const source of ['{bad', '{"investigationMembersJson":"{bad"}']) {
    const result = readBusinessEvidence(source)
    assert.ok(result.error)
    assert.ok(!result.rows.some(r => r.value.includes('{bad')))
  }
})

test('release remarks starting with brackets remain literal business text', () => {
  const remark = '[固定测试订单初始化，参考租户1；仅用于测试]'
  const result = readBusinessEvidence(JSON.stringify({
    formalSourceSnapshotJsonRaw: JSON.stringify({
      formalProductIssues: [{ remark }],
      parameterRules: [{ optionValuesJson: '["自来水","纯化水"]' }]
    }),
    reason: '{测试放行备注}'
  }))
  assert.equal(result.error, '')
  assert.deepEqual(result.rows.map(row => row.value), [remark, '自来水', '纯化水', '{测试放行备注}'])
})
test('detail components compile and expose no raw JSON or hash cells', () => {
  const { parse, compileTemplate } = require('vue/compiler-sfc')
  for (const file of [base + 'BusinessEvidenceDetails.vue', base + 'ActiveOrderSubmissionDetailPanel.vue', base + 'ActiveOrderReworkSourcePanel.vue', base + 'ActiveOrderCorrectionHistoryPanel.vue', 'src/views/mes/pro/edhr-deviation/DeviationDetail.vue']) {
    const { descriptor, errors } = parse(read(file))
    assert.deepEqual(errors, [])
    assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: 'evidence' }).errors, [])
    assert.doesNotMatch(descriptor.template.content, /<pre\b|查看原始技术证据|prop="(?:contentHash|evidenceHash)"|\{\{[^}]*\.(?:contentHash|evidenceHash|aggregateHash|auditHash|initiatorContentHash)/)
  }
})
