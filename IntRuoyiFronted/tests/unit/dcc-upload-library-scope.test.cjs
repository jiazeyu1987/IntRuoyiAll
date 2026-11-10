const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const ctx = { exports: {}, Error }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/project-attributes/state.ts','utf8'), {compilerOptions:{ module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022 }}).outputText,ctx)
test('library application round preserves explicit scope without project attribute values', () => {
  const marker = { libraryScope:'QMS',targetMarkets:null,otherMarket:null,licenseHolder:null,actualManufacturer:null,documentTransfer:null,transferTo:null }
  assert.equal(ctx.exports.validateAttributes(marker), marker)
  assert.throws(() => ctx.exports.validateAttributes({...marker,targetMarkets:['NA']}), /独立文件库/)
  assert.throws(() => ctx.exports.validateAttributes({...marker,libraryScope:'PROJECT'}), /独立文件库/)
  const saved = ctx.exports.createAttributeState('UPLOAD',{projectId:'91',applicationType:'UPLOAD',defaultSource:marker,actual:marker})
  assert.equal(ctx.exports.buildSnapshot(saved).actual.libraryScope,'QMS')
})
