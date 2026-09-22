const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../../src')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')
const loadScript = (file, context, exports) => {
  const { descriptor, errors } = parse(read(file))
  assert.deepEqual(errors, [])
  const compiled = compileScript(descriptor, { id: file })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: file, compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
  const source = ts.createSourceFile(file + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const body = source.statements.filter((node) => !ts.isImportDeclaration(node)).map((node) => node.getText(source)).join('\n')
  const js = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return vm.runInNewContext(`(() => { ${js}; return { ${exports} }; })()`, context)
}
async function main() {
  const calls = []
  const navigation = []
  let failure = false
  const api = loadScript('views/mes/pro/edhr-batch/BatchVoidedPage.vue', {
    ref: (value) => ({ value }), reactive: (value) => value, defineOptions() {}, onMounted() {}, Error,
    useRouter: () => ({ push: async (target) => navigation.push(target) }),
    EDHR_BATCH_STATUS_VOIDED: 60,
    getEdhrBatchExecutionPage: async (query) => {
      calls.push(query)
      if (failure) throw new Error('正式接口失败')
      return { list: [{ id: 42, status: 60 }], total: 13 }
    }
  }, 'getList, handleQuery, resetQuery, openDetail, queryParams, list, total, loadError, loading')
  await api.getList()
  assert.equal(calls[0].status, 60)
  assert.equal(calls[0].completedTraceOnly, true)
  assert.equal(api.total.value, 13)
  api.queryParams.pageNo = 2
  await api.getList()
  assert.equal(calls[1].pageNo, 2)
  api.queryParams.batchCode = 'B-42'
  await api.handleQuery()
  assert.equal(calls[2].batchCode, 'B-42')
  assert.equal(calls[2].pageNo, 1)
  await api.resetQuery()
  assert.equal(calls[3].batchCode, '')
  assert.equal(calls[3].status, 60)
  assert.equal(calls[3].completedTraceOnly, true)
  await api.openDetail({ id: 42 })
  assert.equal(navigation[0].query.batchExecutionId, '42')
  assert.equal(navigation[0].query.from, '/mes/pro/feedback/edhr-batch-voided')
  failure = true
  await api.getList()
  assert.equal(api.loadError.value, '正式接口失败')
  assert.equal(api.list.value.length, 0)
  assert.equal(api.loading.value, false)
  const helper = read('utils/routerHelper.ts')
  const body = helper.slice(helper.indexOf('const applyRouteMetaOverrides'), helper.indexOf('export const registerComponent'))
  const routeBlock = body.slice(body.indexOf('  if ('), body.indexOf('  if (', body.indexOf('  if (') + 1))
  for (const input of [
    { routePath: 'mes/pro/feedback/edhr-nonconformance-review', componentPath: '' },
    { routePath: 'pro/feedback/edhr-nonconformance-review', componentPath: '' },
    { routePath: 'other', componentPath: 'mes/pro/edhr-nonconformance/NonconformanceReviewPage' },
    { routePath: 'other', componentPath: 'other', unrelated: true }
  ]) {
    const meta = { hidden: false }
    vm.runInNewContext(routeBlock, { ...input, meta })
    assert.equal(meta.hidden, !input.unrelated)
    if (!input.unrelated) assert.equal(meta.activeMenu, '/mes/pro/feedback/edhr-batch-execution')
  }
  console.log('PASS voided list behavior, menu projection and Vue compilation')
}
main().catch((error) => { console.error(error); process.exitCode = 1 })
