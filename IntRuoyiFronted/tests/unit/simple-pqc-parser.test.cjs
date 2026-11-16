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
const { makeDraft, nextVersion, validateDraft, differences, inspectionLevelIssue, formatInspectionLevel } =
  ctx.exports
const commonCtx = { exports: {}, require: () => ctx.exports }
vm.runInNewContext(
  ts.transpileModule(
    fs.readFileSync('src/views/form-center/simple-common-parser/draft.ts', 'utf8'),
    {
      compilerOptions: { module: ts.ModuleKind.CommonJS }
    }
  ).outputText,
  commonCtx
)
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
  assert.equal(validateDraft(d).some((x) => x.message.includes('末检')), false)
  d.finalInspectionNotApplicableReason = '规程未要求'
  assert.equal(validateDraft(d).length, 0)
  d.processes[0].items[0].inspectionTool = '量具'
  assert.ok(validateDraft(d).some((x) => x.message.includes('设备编号')))
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
  assert.ok(validateDraft(d).some((x) => x.message.includes('设备编号')))
  assert.equal(validateDraft(d, ['I1']).length, 0)
  assert.ok(validateDraft(d, ['OTHER']).some((x) => x.message.includes('设备编号')))
})

test('a manual item reason permits publication without inventing an equipment binding', () => {
  const d = makeDraft(parsed, 129)
  d.finalInspectionNotApplicableReason = '原文未规定末检'
  const item = d.processes[0].items[0]
  item.inspectionTool = '15N砝码、秒表'
  item.equipmentUnboundReason = '  '
  assert.ok(validateDraft(d).some((issue) => issue.field === 'equipment'))
  item.equipmentUnboundReason = '器具台账尚未登记，执行前由设备管理员补齐'
  assert.equal(validateDraft(d).length, 0)
  assert.equal(item.inspectionTool, '15N砝码、秒表')
  assert.equal(item.equipmentOptions.length, 0)
  const other = structuredClone(item)
  other.itemCode = 'I2'
  other.equipmentUnboundReason = ''
  other.itemName = '另一检验项目'
  d.processes[0].items.push(other)
  assert.equal(validateDraft(d).filter((issue) => issue.field === 'equipment').length, 1)
  const before = structuredClone(d)
  item.equipmentUnboundReason = '已变更原因'
  assert.ok(differences(d, before).some((difference) => difference.includes('equipmentUnboundReason')))
  item.equipmentOptions.push({ equipmentId: 1, equipmentNumber: 'EQ1' })
  assert.ok(validateDraft(d).some((issue) => issue.message.includes('清除器具未绑定原因')))
})

test('publication issues locate duplicate item names without guessing the target', () => {
  const d = makeDraft(parsed, 129)
  const other = JSON.parse(JSON.stringify(d.processes[0]))
  other.processCode = 'P2'
  other.items[0].itemCode = 'I2'
  other.items[0].inspectionTool = '量具'
  d.processes.push(other)
  const issues = validateDraft(d)
  assert.equal(issues.some((i) => i.field === 'final'), false)
  const equipment = issues.find((i) => i.field === 'equipment')
  assert.equal(equipment.processIndex, 1)
  assert.equal(equipment.itemIndex, 0)
  assert.match(equipment.message, /设备编号/)
  assert.equal(d.finalInspectionApplicable, false)
})

test('inspection level is required independently of AQL', () => {
  const d = makeDraft(parsed, 255)
  d.finalInspectionNotApplicableReason = '规程未要求'
  const item = d.processes[0].items[0]
  item.applicableInspectionTypes = ['FIRST', 'PATROL']
  item.samplingPlanText = '首件5件；巡检：按正常检验一次抽样方案Ⅱ，AQL=0.4'
  const original = item.samplingPlanText
  for (const ratio of [undefined, null, 0, "S-", 0.4]) {
    item.inspectionLevel = ratio
    assert.equal(inspectionLevelIssue(item), '请选择有效的巡检检验水平')
    assert.equal(formatInspectionLevel(item), '待确认检验水平')
    for (const validate of [validateDraft, commonCtx.exports.validateCommonDraft]) {
      const issue = validate(d).find((i) => i.message.includes('请选择有效的巡检检验水平'))
      assert.ok(issue)
      assert.equal(issue.processIndex, 0)
      assert.equal(issue.itemIndex, 0)
    }
  }
  item.inspectionLevel = 'S3'
  item.aql = 0.4
  assert.equal(formatInspectionLevel(item), 'S-3')
  assert.equal(validateDraft(d).length, 0)
  assert.equal(commonCtx.exports.validateCommonDraft(d).length, 0)
  assert.equal(item.samplingPlanText, original)
  item.applicableInspectionTypes = ['FIRST']
  item.inspectionLevel = null
  assert.equal(formatInspectionLevel(item), '不适用')
  assert.equal(inspectionLevelIssue(item), undefined)
})

test('common regulations validate without a project while product PQC still requires one', () => {
  const d = makeDraft(parsed, 129)
  d.ownerModule = 'MES_QA_COMMON'
  d.processes[0].items[0].equipmentUnboundReason = '目测项目不需要设备'
  d.finalInspectionNotApplicableReason = '经确认无需末检'
  delete d.dccProjectCodeId
  assert.equal(commonCtx.exports.validateCommonDraft(d).some((e) => /项目|产品/.test(e.message)), false)
  assert.equal(validateDraft(d).some((e) => /请选择所属产品/.test(e.message)), true)
})

test('new PQC drafts retain the complete parsed failure rule without rewriting its meaning', () => {
  const source = JSON.parse(JSON.stringify(parsed))
  source.ruleSourceText = '如有不合格品，隔离该批次并按《不合格品控制程序》处理。'
  const d = makeDraft(source, 255)
  assert.equal(d.processes[0].items[0].failureRule, source.ruleSourceText)
  assert.equal(source.ruleSourceText, '如有不合格品，隔离该批次并按《不合格品控制程序》处理。')
  assert.equal(
    d.processes[0].items[0].samplingPlanText,
    source.processes[0].items[0].samplingPlanText
  )
})

test('parsed level and AQL are preserved independently by draft mapping', () => {
  for (const ratio of [0.25, 0.4, 2.5]) {
    const source = JSON.parse(JSON.stringify(parsed))
    Object.assign(source.processes[0].items[0], {
      applicableInspectionTypes: ['PATROL'],
      samplingPlanText: `GB/T 2828.1，AQL=${ratio}`,
      inspectionLevel: 'I',
      aql: ratio
    })
    const item = makeDraft(source, 255).processes[0].items[0]
    assert.equal(item.inspectionLevel, 'I')
    assert.equal(item.aql, ratio)
    assert.equal(formatInspectionLevel(item), 'I')
    assert.equal(inspectionLevelIssue(item), undefined)
  }
})
