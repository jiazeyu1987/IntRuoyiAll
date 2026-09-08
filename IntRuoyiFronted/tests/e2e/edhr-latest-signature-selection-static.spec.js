const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const helperPath = 'src/views/mes/pro/edhr/signatureSelection.ts'
const helperSource = read(helperPath)
const compiled = ts.transpileModule(helperSource, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
const helperModule = { exports: {} }
new Function('module', 'exports', compiled)(helperModule, helperModule.exports)

const { selectLatestSignature } = helperModule.exports
const apiOrderedRows = [
  { id: 102, signedAt: '2026-09-08T10:00:00+08:00', actorName: '新签名人' },
  { id: 101, signedAt: '2026-09-08T10:00:00+08:00', actorName: '旧签名人' }
]
const selected = selectLatestSignature(apiOrderedRows, (row) => Date.parse(row.signedAt))
assert.equal(selected.actorName, '新签名人', '同秒签名必须显示更高 ID 对应的签名人')
assert.throws(
  () => selectLatestSignature([{ id: 103, actorName: '缺失时间' }], (row) => Date.parse(row.signedAt)),
  /服务器签署时间/,
  '缺少服务器签署时间的签名不得参与最新签名选择'
)

for (const relativePath of [
  'src/views/mes/pro/edhr/ExecutionPage.vue',
  'src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
]) {
  assert.match(read(relativePath), /selectLatestSignature/, `${relativePath} 必须复用稳定最新签名选择器`)
}

console.log('PASS: active execution form latest signature selection contract')
