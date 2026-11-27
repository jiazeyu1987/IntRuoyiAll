const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const source = parse(fs.readFileSync('src/views/mes/pro/processpool/QaRegulationPage.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('qa.ts', source, ts.ScriptTarget.Latest, true)
function action(name, context) {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement)
  vm.runInNewContext(ts.transpileModule(statement.getText(ast) + `\nglobalThis.action = ${name}`,
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.action
}
test('history retains snapshot equipment without sharing mutable objects', () => {
  const flatten = action('flattenQaRegulationProcesses', { createQaItemInspectionState: () => ({}) })
  const equipment = { equipmentId: 10, equipmentNumber: 'E10' }
  const rows = flatten([{ sort: 1, items: [{ itemSort: 1, equipmentOptions: [equipment] }] }])
  assert.equal(rows[0].equipmentOptions[0].equipmentNumber, 'E10')
  rows[0].equipmentOptions[0].equipmentNumber = 'changed'
  assert.equal(equipment.equipmentNumber, 'E10')
})
function resetHarness({ cancel = false, refreshFails = false, refuse = true } = {}) {
  const alerts = [], successes = [], requests = []
  let cleared = 0
  const context = {
    qaEquipmentAutoSaving: { value: false }, qaEquipmentDirtyRows: new Set(),
    qaRegulationDraft: { dccProjectCodeId: '10' }, selectedDccProjectCodeLabel: { value: 'ID' },
    qaRegulationResetting: { value: false }, resolveDccProjectId: String,
    resolveDccProjectCodeErrorMessage: e => e.message,
    ElMessageBox: { confirm: async () => { if (cancel) throw 'cancel' },
      alert: async (...args) => { alerts.push(args) } },
    ElMessage: { success: text => successes.push(text), warning() {} },
    QcTemplateApi: { resetQaRegulationForTesting: async id => {
      requests.push(id)
      if (refuse) throw Error('生产订单引用 1 条，PQC 任务引用 2 条。本次未删除任何规程或版本')
      return { versionCount: 6, processCount: 2, itemCount: 19 }
    } },
    resetQaRegulationConfiguration: () => { cleared++ },
    qaCurrentPublishedVersion: { value: { publishedVersionId: 5 } },
    qaCurrentPublishedVersionLoadError: { value: '' }, qaCurrentPublishedVersionLoading: { value: false },
    qaActiveTab: { value: 'items' }, qaRegulationProjectStatusByDccId: { value: new Map() },
    selectedDccProjectCode: { value: { id: '10' } },
    selectDccProjectCode: async () => { if (refreshFails) throw Error('刷新失败') }
  }
  return { run: action('resetQaRegulationForTesting', context), context, alerts, successes, requests,
    cleared: () => cleared }
}
test('refused reset keeps page data and shows a persistent reason instead of success', async () => {
  const h = resetHarness()
  await h.run()
  assert.equal(h.cleared(), 0)
  assert.equal(h.successes.length, 0)
  assert.equal(h.context.qaCurrentPublishedVersion.value.publishedVersionId, 5)
  assert.match(h.alerts[0][0], /生产订单引用 1 条.*未删除任何/)
  assert.equal(h.alerts[0][1], '测试重置未执行')
  assert.equal(h.alerts[0][2].closeOnClickModal, false)
  assert.equal(h.alerts[0][2].showClose, false)
  assert.equal(h.context.qaRegulationResetting.value, false)
})
test('cancelled confirmation sends no reset request or error alert', async () => {
  const h = resetHarness({ cancel: true })
  await h.run()
  assert.equal(h.requests.length, 0)
  assert.equal(h.alerts.length, 0)
})
test('successful reset clears data and reports success after refresh', async () => {
  const h = resetHarness({ refuse: false })
  await h.run()
  assert.equal(h.cleared(), 1)
  assert.equal(h.alerts.length, 0)
  assert.match(h.successes[0], /清理 6 个版本/)
})
test('refresh failure after reset is distinguished from deletion refusal', async () => {
  const h = resetHarness({ refuse: false, refreshFails: true })
  await h.run()
  assert.equal(h.cleared(), 1)
  assert.equal(h.successes.length, 0)
  assert.equal(h.alerts[0][1], '重置已完成，页面刷新失败')
})
