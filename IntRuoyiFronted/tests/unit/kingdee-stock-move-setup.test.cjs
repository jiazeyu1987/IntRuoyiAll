const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

test('stock move setup registers a usable reload and submits the selected bill date range', async () => {
  const filename = path.resolve(__dirname, '../../src/views/erp/stock/kingdeeStockMove/index.vue')
  const script = parse(fs.readFileSync(filename, 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(filename, script, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(node => !ts.isImportDeclaration(node))
    .map(node => node.getText(ast)).join('\n')
  const code = ts.transpileModule(body, {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  let reload, mounted, request
  const deps = {
    ref: vue.ref, reactive: vue.reactive, computed: vue.computed,
    defineOptions() {},
    onMounted(callback) { mounted = callback },
    useLatestListRequest: () => ({
      loading: vue.ref(false),
      async run(load, commit) { commit(await load()); return true }
    }),
    useUserTableColumns: () => ({}),
    useTableQuickFilter(key, definitions, params, callback) {
      assert.equal(typeof callback, 'function')
      reload = callback
      return {}
    },
    ErpKingdeeStockMoveApi: {
      async getStockMovePage(params) {
        request = params
        return { list: [{ id: 123 }], total: 21 }
      }
    }
  }
  const page = new Function('deps', `with (deps) { ${code}; return { queryParams, list, total } }`)(deps)
  assert.equal(mounted, reload)
  page.queryParams.pageNo = 2
  page.queryParams.sourceBillNo = 'MOVE-123'
  page.queryParams.billDate = ['2026-11-01', '2026-11-13']
  await reload()
  assert.deepEqual(request.billDate, ['2026-11-01 00:00:00', '2026-11-13 23:59:59'])
  assert.equal(request.sourceBillNo, 'MOVE-123')
  assert.equal(request.pageNo, 2)
  assert.equal(page.list.value[0].id, 123)
  assert.equal(page.total.value, 21)
})
