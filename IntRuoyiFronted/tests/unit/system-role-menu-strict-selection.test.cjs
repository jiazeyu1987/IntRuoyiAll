const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

function component() {
  const filename = 'src/views/system/role/RoleAssignMenuForm.vue'
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const script = compileScript(descriptor, { id: filename })
  const template = compileTemplate({
    id: filename,
    filename,
    source: descriptor.template.content,
    compilerOptions: { bindingMetadata: script.bindings }
  })
  assert.deepEqual(template.errors, [])
  const exports = {}, requests = []
  const source = ts.transpileModule(script.content, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  new Function('exports', 'require', 'ref', 'reactive', 'useI18n', 'useMessage', source)(
    exports,
    id => {
      if (id === 'vue') return vue
      if (id === '@/utils/tree') return { defaultProps: {}, handleTree: x => x }
      if (id === '@/api/system/permission') return { assignRoleMenu: async data => requests.push(data) }
      if (id === '@/api/system/role' || id === '@/api/system/menu') return {}
      throw Error('Unexpected import: ' + id)
    },
    vue.ref,
    vue.reactive,
    () => ({ t: key => key }),
    () => ({ success() {} })
  )
  const state = exports.default.setup({}, { expose() {}, emit() {} })
  return { state, requests }
}

const exact = [1202, 1221, 990248, 990230, 990231, 990232, 990238, 990239, 990246]
const staleParents = [990200, 1200, 1201]

for (const linked of [false, true]) {
  test(`actual role form ${linked ? 'retains half parents when linked' : 'submits exactly nine checked menus when unlinked'}`, async () => {
    const { state, requests } = component()
    let halfReads = 0
    state.formRef.value = { validate: async () => true }
    state.treeRef.value = {
      getCheckedKeys: leafOnly => { assert.equal(leafOnly, false); return [...exact] },
      getHalfCheckedKeys: () => { halfReads++; return [...staleParents] }
    }
    state.formData.id = 910484
    state.formData.reason = ' precise approved scope '
    state.checkStrictly.value = linked
    await state.submitForm()
    assert.equal(requests.length, 1)
    assert.deepEqual(requests[0].menuIds, linked ? [...exact, ...staleParents] : exact)
    assert.equal(halfReads, linked ? 1 : 0)
    assert.equal(requests[0].roleId, 910484)
    assert.equal(requests[0].reason, 'precise approved scope')
    assert.match(requests[0].idempotencyKey, /^SYSTEM-PERM-ROLE-MENU-910484-/)
    assert.equal(state.formLoading.value, false)
  })
}
