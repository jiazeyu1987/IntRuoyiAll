const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const { createRequire } = require('node:module')
const vm = require('node:vm')
const appRequire = createRequire(require.resolve('vue/package.json'))
const compiler = appRequire('@vue/compiler-sfc')
const ts = require('typescript')
const { reactive, nextTick } = require('vue')
const filename = 'src/views/form-center/parser/components/SimplePqcTrialPreview.vue'
const { descriptor } = compiler.parse(fs.readFileSync(filename, 'utf8'), { filename })
const compiled = compiler.compileScript(descriptor, { id: 'pqc-trial-test' })
const ctx = {
  exports: {},
  require: (name) =>
    name === '@/store/modules/user'
      ? { useUserStore: () => ({ getUser: { nickname: '测试用户' } }) }
      : require(name)
}
vm.runInNewContext(
  ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
    .outputText,
  ctx
)

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
            patrolInspectionRatio: 2,
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
  state.selectRule('PATROL_AM')
  await nextTick()
  assert.equal(state.quantity.value, 2)
  assert.match(state.resultSummary.value, /未填写 2 件/)
  state.bulk('不合格')
  state.selectRule('FIRST')
  await nextTick()
  assert.match(state.resultSummary.value, /合格 5 件/)
  state.selectedCode.value = 'I2'
  await nextTick()
  state.state.value.values = [10, 21]
  assert.match(state.resultSummary.value, /合格 1 件 · 不合格 1 件/)
  state.state.value.values = [undefined, undefined]
  assert.match(state.resultSummary.value, /未填写 2 件/)
  state.selectRule('FINAL')
  await nextTick()
  assert.equal(state.quantity.value, 2)
  assert.equal(JSON.stringify(draft), before)
})
