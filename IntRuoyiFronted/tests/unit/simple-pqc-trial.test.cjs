const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const { createRequire } = require('node:module')
const vm = require('node:vm')
const appRequire = createRequire(require.resolve('vue/package.json'))
const compiler = appRequire('@vue/compiler-sfc')
const ts = require('typescript')
const samplingCtx = { exports: {} }
vm.runInNewContext(
  ts.transpileModule(fs.readFileSync('src/views/form-center/parser/simplePqc.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText,
  samplingCtx
)
const { reactive, nextTick } = require('vue')
const filename = 'src/views/form-center/parser/components/SimplePqcTrialPreview.vue'
const { descriptor } = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(
  compiler.compileTemplate({ source: descriptor.template.content, filename, id: 'pqc-trial-test' })
    .errors,
  []
)
const compiled = compiler.compileScript(descriptor, { id: 'pqc-trial-test' })
const ctx = {
  exports: {},
  require: (name) =>
    name === '@/store/modules/user'
      ? { useUserStore: () => ({ getUser: { nickname: '测试用户' } }) }
      : name === '@/api/mes/qc/sampling' ? { getSamplingTable: async () => [] }
      : name === '../simplePqc'
        ? samplingCtx.exports
        : require(name)
}
vm.runInNewContext(
  ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
    .outputText,
  ctx
)

test('trial displays pending equipment binding without discarding the source requirement or reason', () => {
  const draft = reactive({
    inspectionTypeRules: [{ key: 'FIRST', inspectionType: 'FIRST', label: '首检' }],
    processes: [{ processCode: 'P1', items: [{ itemCode: 'I1', itemName: '强度', resultType: 'BOOLEAN',
      applicableInspectionTypes: ['FIRST'], firstInspectionQuantity: 5, equipmentOptions: [],
      inspectionTool: '15N砝码', equipmentUnboundReason: '台账待登记，执行前补齐' }] }]
  })
  const original = JSON.stringify(draft)
  const state = ctx.exports.default.setup(reactive({ draft, processIndex: 0 }), { expose() {}, emit() {} })
  assert.equal(state.selectedEquipmentLabel.value, '器具待绑定')
  assert.equal(state.item.value.equipmentUnboundReason, '台账待登记，执行前补齐')
  assert.equal(state.item.value.inspectionTool, '15N砝码')
  assert.equal(state.quantity.value, 5)
  state.bulk('合格')
  state.submitTrial()
  assert.equal(JSON.stringify(draft), original)
})

test('PQC trial isolates results by item/type and leaves the publication draft unchanged', async () => {
  const draft = reactive({
    finalInspectionApplicable: true,
    inspectionTypeRules: [
      { key: 'FIRST', inspectionType: 'FIRST', label: '首检' },
      { key: 'PATROL_AM', inspectionType: 'PATROL', label: '上午巡检' },
      { key: 'PATROL_PM', inspectionType: 'PATROL', label: '下午巡检' },
      { key: 'FINAL', inspectionType: 'FINAL', label: '末检', fixedQuantity: 2 }
    ],
    processes: [
      {
        processCode: 'P1',
        processName: '清洗',
        items: [
          {
            itemCode: 'I1',
            itemName: '外观',
            resultType: 'BOOLEAN',
            applicableInspectionTypes: ['FIRST', 'PATROL', 'FINAL'],
            firstInspectionQuantity: 5,
            inspectionLevel: 'I', aql: 0.4,
            equipmentOptions: []
          },
          {
            itemCode: 'I2',
            itemName: '压力',
            resultType: 'NUMERIC',
            standardLowerLimit: 10,
            standardUpperLimit: 20,
            applicableInspectionTypes: ['FIRST'],
            firstInspectionQuantity: 2,
            equipmentOptions: []
          }
        ]
      }
    ]
  })
  const before = JSON.stringify(draft)
  const props = reactive({ draft, processIndex: 0 })
  const state = ctx.exports.default.setup(props, { expose() {}, emit() {} })
  assert.equal(state.quantity.value, 5)
  state.bulk('合格')
  assert.match(state.resultSummary.value, /合格 5 件/)
  state.samplingRows.value = [{ minQuantity: 91, maxQuantity: 150, samples: { I: { sampleCode: 'E', nominalSampleSize: 13 } } }]
  state.selectRule('PATROL_AM')
  await nextTick()
  assert.equal(state.quantity.value, 13)
  assert.match(state.resultSummary.value, /未填写 13 件/)
  state.bulk('不合格')
  state.selectRule('FIRST')
  await nextTick()
  assert.match(state.resultSummary.value, /合格 5 件/)
  state.selectedCode.value = 'I2'
  await nextTick()
  state.state.value.values = [10, 21]
  assert.match(state.resultSummary.value, /合格 1 件 · 不合格 1 件/)
  state.state.value.values = [10, undefined]
  state.submitTrial()
  assert.match(state.trialMessage.value, /请完成/)
  state.openPieces()
  state.pieceValues.value[1] = 12
  assert.equal(state.state.value.values[1], undefined)
  state.confirmPieces()
  state.submitTrial()
  assert.match(state.trialMessage.value, /检查完成/)
  state.reset()
  assert.equal(state.trialMessage.value, '')
  state.state.value.values = [undefined, undefined]
  assert.match(state.resultSummary.value, /未填写 2 件/)
  state.selectRule('FINAL')
  await nextTick()
  assert.equal(state.quantity.value, 2)
  assert.equal(JSON.stringify(draft), before)
})

test('missing inspection level still blocks trial quantity and submission', async () => {
  const draft = reactive({
    finalInspectionApplicable: false,
    inspectionTypeRules: [{ key: 'PATROL_AM', inspectionType: 'PATROL', label: '上午巡检' }],
    processes: [
      {
        processCode: 'P1',
        items: [
          {
            itemCode: 'I1',
            itemName: '气密性',
            resultType: 'BOOLEAN',
            applicableInspectionTypes: ['PATROL'],
            inspectionLevel: null,
            samplingPlanText: '巡检：正常检验一次抽样方案Ⅱ，AQL=0.4',
            equipmentOptions: []
          }
        ]
      }
    ]
  })
  const original = JSON.stringify(draft)
  const state = ctx.exports.default.setup(reactive({ draft, processIndex: 0 }), {
    expose() {},
    emit() {}
  })
  await nextTick()
  assert.equal(state.quantity.value, undefined)
  assert.match(state.resultSummary.value, /请选择有效的巡检检验水平/)
  assert.equal(state.state.value.values.length, 0)
  state.stepQuantity(1)
  state.setQuantity({ target: { value: '10' } })
  assert.equal(state.quantity.value, undefined)
  state.bulk('合格')
  state.submitTrial()
  assert.match(state.trialMessage.value, /请选择有效的巡检检验水平/)
  assert.doesNotMatch(state.trialMessage.value, /检查完成/)
  assert.equal(JSON.stringify(draft), original)
})

test('matrix trial uses the returned level independently of AQL and freezes patrol quantity', async () => {
  for (const aql of [0.25, 0.4, 2.5]) {
    for (const [level, expected] of [['I', 50], ['S3', 13]]) {
      const draft = reactive({ finalInspectionApplicable: false,
        inspectionTypeRules: [{ key: 'PATROL_AM', inspectionType: 'PATROL', label: '上午巡检' }],
        processes: [{ processCode: 'P1', items: [{ itemCode: 'I1', itemName: '外观', resultType: 'BOOLEAN',
          applicableInspectionTypes: ['PATROL'], inspectionLevel: level, aql,
          samplingPlanText: `GB/T 2828.1，${level}，AQL=${aql}`, equipmentOptions: [] }] }] })
      const original = JSON.stringify(draft)
      const state = ctx.exports.default.setup(reactive({ draft, processIndex: 0 }), { expose() {}, emit() {} })
      state.samplingRows.value = [{ minQuantity: 1201, maxQuantity: 3200,
        samples: { I: { sampleCode: 'H', nominalSampleSize: 50 }, S3: { sampleCode: 'E', nominalSampleSize: 13 } } }]
      state.batchQuantity.value = 3000
      await nextTick()
      assert.equal(state.quantity.value, expected)
      state.setQuantity({ target: { value: '12' } })
      assert.equal(state.quantity.value, expected)
      state.bulk('合格')
      state.submitTrial()
      assert.match(state.trialMessage.value, /检查完成/)
      state.batchQuantity.value = 1
      await nextTick()
      assert.equal(state.quantity.value, 1)
      assert.equal(JSON.stringify(draft), original)
    }
  }
})
test('missing or failed matrix blocks preview without percentage substitute', async () => {
  const draft = reactive({ inspectionTypeRules: [{ key: 'PATROL_AM', inspectionType: 'PATROL' }],
    processes: [{ processCode: 'P1', items: [{ itemCode: 'I1', resultType: 'BOOLEAN', applicableInspectionTypes: ['PATROL'],
      inspectionLevel: 'I', aql: 0.4, equipmentOptions: [] }] }] })
  const state = ctx.exports.default.setup(reactive({ draft, processIndex: 0 }), { expose() {}, emit() {} })
  await nextTick()
  assert.equal(state.quantity.value, undefined)
  assert.match(state.samplingIssue.value, /加载中/)
  state.samplingLoadError.value = '矩阵服务不可用'
  assert.match(state.samplingIssue.value, /不可用/)
  assert.equal(state.quantity.value, undefined)
})
