const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const source = fs.readFileSync('src/views/member/user/detail/areaDisplay.ts', 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
const loaded = { exports: {} }
new Function('exports', 'module', compiled.outputText)(loaded.exports, loaded)
const { getAddressAreaName } = loaded.exports
const areas = [
  { id: 1, name: '无关省', children: [{ id: 11, name: '无关市' }] },
  { id: 2, name: '目标省', children: [
    { id: 21, name: '无关兄弟市', children: [{ id: 211, name: '无关区' }] },
    { id: 22, name: '目标市', children: [{ id: 221, name: '目标区' }] }
  ] }
]
assert.equal(getAddressAreaName(areas, 221), '目标省 / 目标市 / 目标区')
assert.equal(getAddressAreaName(areas, 2), '目标省')
assert.throws(() => getAddressAreaName(areas, 999), /行政区信息不存在/)
assert.throws(() => getAddressAreaName([], 221), /行政区信息不存在/)
console.log('PASS: unrelated siblings, deep match, root match, unknown ID, empty tree')
