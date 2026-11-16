const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { createRouter, createMemoryHistory } = require('vue-router')

test('project applicant without a product catalog menu can resolve both shared request entries', () => {
  const context = {
    exports: {},
    useI18n: () => ({ t: value => value }),
    require: id => id === '@/utils/routerHelper' ? { Layout: {} } : { default: [] }
  }
  const source = fs.readFileSync('src/router/modules/remaining.ts', 'utf8')
  vm.runInNewContext(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  // Only the actual initial routes: this account has no /mdm dynamic menu.
  const router = createRouter({ history: createMemoryHistory(), routes: context.exports.default })
  for (const mode of ['create', 'records']) {
    const resolved = router.resolve({
      path: '/mdm/product-catalog', query: { requestOpen: mode, from: 'project-code' }
    })
    assert.equal(resolved.name, 'DccProductCatalogBasicDataPage')
    assert.equal(resolved.meta.hidden, true)
    assert.deepEqual(Array.from(resolved.meta.permission), ['dcc:project-code:query'])
    assert.equal(resolved.query.requestOpen, mode)
  }
})
