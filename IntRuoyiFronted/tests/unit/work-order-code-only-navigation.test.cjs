const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const { createRouter, createMemoryHistory } = require('vue-router')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/components/WorkOrderCodeLink.vue'), 'utf8')
const { descriptor } = parse(source)
const script = compileScript(descriptor, { id: 'work-order-code-only' })
const code = ts.transpileModule(script.content, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

for (const workOrderId of [undefined, '9007199254740993']) {
  test(`work order navigation uses supplied code and ${workOrderId ? 'the exact supplied ID' : 'no invented ID'}`, async () => {
    const router = createRouter({ history: createMemoryHistory(), routes: [
      { path: '/source', component: {} }, { path: '/mes/pro/work-order', component: {} }
    ] })
    await router.push('/source')
    const exports = {}
    new Function('exports', 'require', code)(exports, name => {
      if (name === 'vue') return vue
      assert.equal(name, 'vue-router')
      return { useRouter: () => router }
    })
    const state = exports.default.setup({ code: ' 工单 A/B & 1 ', workOrderId }, { expose() {} })
    assert.equal(state.destination.value.query.code, '工单 A/B & 1')
    assert.equal(state.destination.value.query.openId, workOrderId)
    assert.equal(Object.hasOwn(state.destination.value.query, 'openId'), workOrderId !== undefined)
    let prevented = false
    await state.navigate({ button: 0, preventDefault() { prevented = true } })
    assert.equal(prevented, true)
    assert.equal(router.currentRoute.value.path, '/mes/pro/work-order')
    assert.equal(router.currentRoute.value.query.code, '工单 A/B & 1')
    assert.equal(router.currentRoute.value.query.openId, workOrderId)
  })
}
