const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const exportsObject = {}
const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/project-attributes/state.ts'), 'utf8')
vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
  { exports: exportsObject, Error })
assert.equal(typeof exportsObject.fromApplicationSnapshot, 'function', '正式申请回读必须映射两份保存快照而非今日默认')
const defaults = { targetMarkets: ['NMPA'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
for (const type of ['UPLOAD', 'REVISION', 'OBSOLETE']) {
  const row = { projectCodeId: '5', applicationType: type, applicationId: '20', applicationRound: 3,
    defaultSourceJson: JSON.stringify(defaults), actualAttributesJson: JSON.stringify({ ...defaults, targetMarkets: ['CE'] }), submitted: true }
  const projected = exportsObject.fromApplicationSnapshot(row, '5', type, '20')
  assert.deepEqual(JSON.parse(JSON.stringify(projected.defaultSource.targetMarkets)), ['NMPA'])
  assert.deepEqual(JSON.parse(JSON.stringify(projected.actual.targetMarkets)), ['CE'])
  assert.throws(() => exportsObject.fromApplicationSnapshot(row, '6', type, '20'))
  assert.throws(() => exportsObject.fromApplicationSnapshot(row, '5', type, '21'))
  assert.throws(() => exportsObject.fromApplicationSnapshot({ ...row, applicationRound: 0 }, '5', type, '20'))
  assert.throws(() => exportsObject.fromApplicationSnapshot({ ...row, actualAttributesJson: '' }, '5', type, '20'))
}
console.log('PASS: 三动作正式轮次两份快照映射、错项目/文件/轮次/历史缺值拒绝')
