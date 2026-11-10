const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

test('role search matches name, ID and code without changing selected roles', () => {
  const filename = 'src/views/system/user/UserAssignRoleForm.vue'
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'role-search' })
  assert.deepEqual(compileTemplate({
    source: descriptor.template.content, id: 'role-search',
    compilerOptions: { bindingMetadata: script.bindings }
  }).errors, [])
  const source = ts.transpileModule(script.content, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  const exports = {}
  new Function('exports', 'require', 'ref', 'computed', 'useI18n', 'useMessage', source)(
    exports, id => {
      if (id === 'vue') return vue
      if (id.startsWith('@/api/system/')) return {}
      if (id === './role-option-label') return { roleOptionLabel: role => role.name }
      throw Error('Unexpected import: ' + id)
    }, vue.ref, vue.computed, () => ({ t: value => value }), () => ({}))
  const state = exports.default.setup({}, { expose() {} })
  state.roleList.value = [
    { id: 910233, name: '文控', code: 'doc_control' },
    { id: 910305, name: '文控', code: 'wenkong' },
    { id: 7, name: '审核员', code: 'reviewer' }
  ]
  state.formData.value.roleIds = [910305]
  const search = keyword => {
    state.roleKeyword.value = keyword
    return state.filteredRoleList.value.map(role => role.id)
  }
  assert.deepEqual(search('文控'), [910233, 910305])
  assert.deepEqual(search('910233'), [910233])
  assert.deepEqual(search(' DOC_CONTROL '), [910233])
  assert.deepEqual(search('不存在'), [])
  assert.deepEqual(search(''), [910233, 910305, 7])
  assert.deepEqual(state.formData.value.roleIds, [910305])
})
