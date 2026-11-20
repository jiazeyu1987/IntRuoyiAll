const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { createRouter, createMemoryHistory } = require('vue-router')

test('product catalog resolves to its own page and sidebar menu', () => {
  const exports = {}
  const code = ts.transpileModule(fs.readFileSync('src/router/modules/remaining.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const dependencies = {
    '@/utils/routerHelper': { Layout: {} },
    './businessConfig': { default: [] }
  }
  vm.runInNewContext(code, {
    exports,
    useI18n: () => ({ t: key => key }),
    require: id => {
      assert.ok(id in dependencies, `Unexpected dependency: ${id}`)
      return dependencies[id]
    }
  })
  const masterData = exports.default.find(route => route.name === 'MdmHidden')
  const router = createRouter({ history: createMemoryHistory(), routes: [masterData] })
  const resolved = router.resolve('/mdm/product-catalog')
  assert.equal(resolved.name, 'DccProductCatalogBasicDataPage')
  assert.equal(resolved.meta.activeMenu, '/mdm/product-catalog')
  assert.equal(resolved.meta.title, '产品目录')
  assert.equal(resolved.meta.permission.join(','), 'dcc:project-code:query')
  assert.match(String(resolved.matched.at(-1).components.default), /basic-data\/product-catalog\/index.vue/)
})
