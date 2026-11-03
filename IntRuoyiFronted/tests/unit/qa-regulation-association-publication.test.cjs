const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/QaRegulationPage.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const script = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['addCommonRegulationBindingRow', 'removeCommonRegulationBindingRow',
  'changeCommonRegulationBindingRow', 'runQaPublishPrecheck']
const functions = script.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(script))))
const code = ts.transpileModule(functions.map(statement => statement.getText(script)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const ref = value => ({ value })
function harness({ failPublish = false, failRefresh = false } = {}) {
  const calls = [], messages = []
  const context = {
    commonRegulationBindingRows: ref([{ rowKey: 1, commonRegulationVersionId: 11 }]),
    commonRegulationBindingRowSerial: 1,
    commonRegulationVersionOptions: ref([{ commonRegulationVersionId: 22 }]),
    commonRegulationBindingLoading: ref(false), commonRegulationBindingLoadError: ref(''),
    commonRegulationVersionOptionsLoading: ref(false), commonRegulationVersionOptionsLoadError: ref(''),
    qaPublishBlockers: ref([]), qaRegulationPublishing: ref(false),
    qaCurrentPublishedVersion: ref(null), qaRegulationProjectStatusByDccId: ref(new Map()),
    buildQaRegulationSavePayload: () => ({ dccProjectCodeId: 10, versionNo: 'B/3' }),
    resolvePositiveId: value => { if (!(Number(value) > 0)) throw Error('请选择规程'); return Number(value) },
    resolveDccProjectCodeErrorMessage: error => error.message,
    ElMessage: { success: text => messages.push(text), error: text => messages.push(text), warning: text => messages.push(text) },
    QcTemplateApi: { publishQaRegulation: async payload => {
      calls.push(structuredClone(payload))
      if (failPublish) throw Error('server rejected')
      return { regulationId: 1, publishedVersionId: 30, versionNo: 'B/3' }
    } },
    applyQaRegulationConfiguration: () => {},
    loadCurrentCommonRegulationBinding: async () => { if (failRefresh) throw Error('refresh unavailable') },
    loadQaRegulationVersionOptions: async () => {}
  }
  vm.runInNewContext(code + '\nglobalThis.actions = {' + names.join(',') + '}', context)
  return { ...context.actions, context, calls, messages }
}
test('add, replace and delete stay local until the single publication action', async () => {
  const h = harness()
  h.addCommonRegulationBindingRow()
  h.changeCommonRegulationBindingRow(h.context.commonRegulationBindingRows.value[1], 22)
  h.removeCommonRegulationBindingRow(1)
  assert.equal(h.calls.length, 0)
  await h.runQaPublishPrecheck()
  assert.deepEqual(h.calls[0].commonRegulationVersionIds, [22])
})
test('deleting all rows publishes an explicit empty association snapshot', async () => {
  const h = harness()
  h.removeCommonRegulationBindingRow(1)
  await h.runQaPublishPrecheck()
  assert.deepEqual(h.calls[0].commonRegulationVersionIds, [])
})
test('an incomplete new row blocks publication and preserves the edit', async () => {
  const h = harness()
  h.addCommonRegulationBindingRow()
  await h.runQaPublishPrecheck()
  assert.equal(h.calls.length, 0)
  assert.equal(h.context.commonRegulationBindingRows.value.length, 2)
})
test('publication failure preserves pending associations and clears the loading state', async () => {
  const h = harness({ failPublish: true })
  h.removeCommonRegulationBindingRow(1)
  await h.runQaPublishPrecheck()
  assert.equal(h.context.commonRegulationBindingRows.value.length, 0)
  assert.equal(h.context.qaRegulationPublishing.value, false)
  assert.match(h.messages[0], /发布失败/)
})
test('refresh failure after commit is reported as published with a refresh error', async () => {
  const h = harness({ failRefresh: true })
  await h.runQaPublishPrecheck()
  assert.match(h.messages[0], /已发布，但页面刷新失败/)
  assert.equal(h.context.qaCurrentPublishedVersion.value.versionNo, 'B/3')
})
