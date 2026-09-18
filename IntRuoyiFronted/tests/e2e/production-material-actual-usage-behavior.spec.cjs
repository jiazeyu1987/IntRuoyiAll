const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const panel = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const helpers = panel.slice(panel.indexOf('const formatProductionMaterialListQuantity ='), panel.indexOf('const formatDate ='))
const resolver = panel.slice(panel.indexOf('const resolveProductionMaterialListActualUsage ='), panel.indexOf('const resolveProductionMaterialListWarehouse ='))
const props = { detail: {
  inputMaterialUsages: [{ materialCode: 'A', actualQuantity: 100 }],
  processes: [
    { inputMaterials: [{ materialCode: 'A', actualQuantity: '2.25' }, { materialCode: 'Z', actualQuantity: 0 }] },
    { inputMaterials: [{ materialCode: ' A ', actualQuantity: '7.5' }, { materialCode: 'B', actualQuantity: '1,200.125' }] }
  ]
} }
const js = ts.transpileModule(`${helpers}\n${resolver}\nthis.resolveUsage = resolveProductionMaterialListActualUsage`, {
  compilerOptions: { target: ts.ScriptTarget.ES2020 }
}).outputText
const context = { props, computed: (fn) => ({ get value() { return fn() } }) }
vm.runInNewContext(js, context)
assert.equal(context.resolveUsage({ childMaterialCode: 'A' }), '7.5')
assert.equal(context.resolveUsage({ childMaterialCode: ' B ' }), '1200.125')
assert.equal(context.resolveUsage({ childMaterialCode: 'Z' }), '0')
assert.equal(context.resolveUsage({ childMaterialCode: 'MISSING', actualQuantity: 999 }), '')
props.detail = undefined
assert.equal(context.resolveUsage({ childMaterialCode: 'A' }), '')
console.log('PASS: actual usage is max production input quantity per code, preserving decimals and zero')
