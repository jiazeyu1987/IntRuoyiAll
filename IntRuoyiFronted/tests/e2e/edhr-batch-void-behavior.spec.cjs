const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../../src')
const file = 'views/mes/pro/edhr-batch/BatchVoidedPage.vue'
const source = fs.readFileSync(path.join(root, file), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const compiled = compileScript(descriptor, { id: file })
assert.deepEqual(
  compileTemplate({
    source: descriptor.template.content,
    filename: file,
    id: file,
    compilerOptions: { bindingMetadata: compiled.bindings }
  }).errors,
  []
)

const scriptSource = ts.createSourceFile(
  `${file}.ts`,
  descriptor.scriptSetup.content,
  ts.ScriptTarget.Latest,
  true
)
const scriptBody = scriptSource.statements
  .filter((node) => !ts.isImportDeclaration(node))
  .map((node) => node.getText(scriptSource))
  .join('\n')
const transpiled = ts.transpileModule(scriptBody, {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const calls = []
const detailCalls = []
const api = Function(
  'getPqcProductionReleasePage',
  'getPqcProductionReleaseOrderDetail',
  'PQC_RELEASE_VIEW_VOIDED',
  'onMounted',
  'reactive',
  'ref',
  'defineOptions',
  `return (() => { ${transpiled}; return { getList, openDetail, list, total, queryParams }; })()`
)(
  async (query) => {
    calls.push(query)
    return {
      list: [
        {
          applicationId: '88',
          viewStatus: 'VOIDED',
          workOrderCode: 'WO-VOID',
          batchCode: 'BA-VOID',
          nonconformanceDisposition: 'void',
          nonconformanceReason: '检验结果不合格'
        }
      ],
      total: 1
    }
  },
  async (applicationId) => {
    detailCalls.push(applicationId)
    return { detail: { workOrderCode: 'WO-VOID' }, productionMaterialLists: [] }
  },
  'VOIDED',
  () => {},
  (value) => value,
  (value) => ({ value }),
  () => {}
)

async function main() {
  await api.getList()
  assert.equal(calls[0].viewStatus, 'VOIDED')
  assert.equal(calls[0].workOrderCode, undefined)
  assert.equal(api.list.value[0].applicationId, '88')
  assert.equal(api.list.value[0].nonconformanceDisposition, 'void')
  await api.openDetail(api.list.value[0])
  assert.deepEqual(detailCalls, ['88'])
  assert.equal(api.total.value, 1)
  assert.match(source, /PQC_RELEASE_VIEW_VOIDED/)
  assert.match(source, /getPqcProductionReleaseOrderDetail/)
  assert.doesNotMatch(source, /getEdhrBatchExecutionPage/)
  assert.doesNotMatch(source, /getTeamLeaderVoidedActiveOrderPage/)
  assert.doesNotMatch(source, /readBlocked/)
  assert.doesNotMatch(source, /数据异常/)
  console.log('PASS voided page uses PQC release applications and disposition detail')
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
