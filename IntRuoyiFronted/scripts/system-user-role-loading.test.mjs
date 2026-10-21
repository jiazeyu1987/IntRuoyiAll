import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'

const require = createRequire(import.meta.url)
const vue = require('vue')
const ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')
const filename = new URL('../src/views/system/user/UserAssignRoleForm.vue', import.meta.url)
const { descriptor, errors } = parse(readFileSync(filename, 'utf8'))
assert.deepEqual(errors, [])
const compiled = ts.transpileModule(compileScript(descriptor, { id: 'role-loading' }).content, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function form(roles) {
  const module = { exports: {} }
  let writes = 0
  const imports = {
    vue,
    '@/api/system/permission': { getUserRoleList: async () => [41], assignUserRole: async () => { writes++ } },
    '@/api/system/role': { getSimpleRoleList: () => roles },
    '@/api/system/user': {}
  }
  new Function('require', 'module', 'exports', 'ref', 'useI18n', 'useMessage', compiled)(
    (name) => { assert.ok(Object.hasOwn(imports, name)); return imports[name] },
    module, module.exports, vue.ref, () => ({ t: (v) => v }), () => ({ success() {} })
  )
  let exposed
  const state = module.exports.default.setup({}, { expose(value) { exposed = value }, emit() {} })
  return { state, open: exposed.open, writes: () => writes }
}
test('actual role form stays loading until names arrive, then publishes IDs with matching labels', async () => {
  const pending = deferred()
  const f = form(pending.promise)
  const opening = f.open({ id: 11, username: 'unit', nickname: 'unit' })
  await Promise.resolve()
  await Promise.resolve()
  assert.equal(f.state.formLoading.value, true)
  assert.equal(f.state.roleOptionsReady.value, false)
  assert.deepEqual(f.state.formData.value.roleIds, [])
  await assert.rejects(f.state.submitForm(), /角色资料尚未加载完成/)
  assert.equal(f.writes(), 0)
  pending.resolve([{ id: 41, name: '本轮角色' }])
  await opening
  assert.equal(f.state.formLoading.value, false)
  assert.equal(f.state.roleOptionsReady.value, true)
  assert.deepEqual(f.state.formData.value.roleIds, [41])
  assert.equal(f.state.roleList.value[0].name, '本轮角色')
})
test('failed role-name lookup stays an error and cannot submit an empty replacement assignment', async () => {
  const pending = deferred()
  const f = form(pending.promise)
  const opening = f.open({ id: 11, username: 'unit', nickname: 'unit' })
  pending.reject(new Error('role lookup failed'))
  await assert.rejects(opening, /role lookup failed/)
  assert.equal(f.state.roleOptionsReady.value, false)
  await assert.rejects(f.state.submitForm(), /角色资料尚未加载完成/)
  assert.equal(f.writes(), 0)
})
