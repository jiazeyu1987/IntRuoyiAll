const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const compiler = require('vue/compiler-sfc')
const ts = require('typescript')
const d = compiler.parse(fs.readFileSync('src/views/iot/ota/firmware/index.vue', 'utf8')).descriptor
compiler.compileScript(d, { id: 'firmware' })
assert.deepEqual(compiler.compileTemplate({ source: d.template.content, filename: 'firmware.vue', id: 'firmware' }).errors, [])
const branch = d.template.content.match(/<el-link\s+@click="openProductDetail[\s\S]*?<\/el-link>\s*<span v-else>[\s\S]*?<\/span>/)[0]
assert.match(branch, /checkPermi\(\['iot:product:query'\]\)/)
assert.match(branch, /<span v-else>{{ getProductName\(scope.row.productId\)/)
const handler = ts.transpile(d.scriptSetup.content, { target: ts.ScriptTarget.ES2022 }).match(/const openProductDetail = [\s\S]*?\n};/)[0]
for (const allowed of [false, true]) {
  const calls = []
  const context = { checkPermi: permissions => { assert.deepEqual(Array.from(permissions), ['iot:product:query']); return allowed }, push: location => calls.push(location) }
  vm.createContext(context)
  vm.runInContext(handler + '\nopenProductDetail(42)', context)
  assert.equal(calls.length, allowed ? 1 : 0)
  if (allowed) assert.equal(calls[0].params.id, 42)
}
console.log('PASS firmware product permission and navigation')
