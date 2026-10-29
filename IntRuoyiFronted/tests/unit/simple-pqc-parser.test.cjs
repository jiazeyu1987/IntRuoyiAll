const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const ctx = { exports: {} }
vm.runInNewContext(
  ts.transpileModule(fs.readFileSync('src/views/form-center/parser/simplePqc.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText,
  ctx
)
const { makeDraft, nextVersion, validateDraft, differences } = ctx.exports
const parsed = {
  regulationCode: 'PQC-TEST',
  regulationName: '检验规程',
  versionNo: 'B/2',
  effectiveDate: '2026-01-04',
  processes: [
    {
      processCode: 'P1',
      processName: '清洗',
      sort: 1,
      items: [
        {
          itemCode: 'I1',
          itemSort: 1,
          itemName: '外观',
          inspectionMethod: '观察',
          inspectionTool: '目测',
          samplingPlanText: '首件5件',
          standardText: '无异物',
          resultType: 'BOOLEAN',
          applicableInspectionTypes: ['FIRST'],
          firstInspectionQuantity: 5
        }
      ]
    }
  ]
}
test('Word content remains authoritative and missing decisions block publication', () => {
  const d = makeDraft(parsed, 129)
  assert.equal(d.processes[0].items[0].standardText, '无异物')
  assert.ok(validateDraft(d).some((x) => x.includes('末检')))
  d.finalInspectionNotApplicableReason = '规程未要求'
  assert.equal(validateDraft(d).length, 0)
  d.processes[0].items[0].inspectionTool = '量具'
  assert.ok(validateDraft(d).some((x) => x.includes('设备编号')))
})
test('existing immutable versions are never reused', () => {
  assert.equal(nextVersion('B/2', ['B/2', 'B/3', 'B/4', 'B/5']), 'B/6')
})
test('comparison identifies edited standards and removed processes', () => {
  const d = makeDraft(parsed, 129)
  const before = JSON.parse(JSON.stringify(d))
  d.processes[0].items[0].standardText = '已修改'
  assert.ok(differences(d, before).some((x) => x.includes('standardText')))
  d.processes = []
  assert.ok(differences(d, before).some((x) => x.includes('删除工序')))
})

test('same effective date uses the API date representation', () => {
  const d = makeDraft(parsed, 129)
  const before = JSON.parse(JSON.stringify(d))
  before.effectiveDate = [2026, 1, 4]
  assert.equal(differences(d, before).length, 0)
})

test('an explicit existing-QA unbound choice only exempts that item', () => {
  const d = makeDraft(parsed, 129)
  d.finalInspectionNotApplicableReason = '当前QA不适用末检'
  d.processes[0].items[0].inspectionTool = '气密性检测工装'
  assert.ok(validateDraft(d).some((x) => x.includes('设备编号')))
  assert.equal(validateDraft(d, ['I1']).length, 0)
  assert.ok(validateDraft(d, ['OTHER']).some((x) => x.includes('设备编号')))
})
