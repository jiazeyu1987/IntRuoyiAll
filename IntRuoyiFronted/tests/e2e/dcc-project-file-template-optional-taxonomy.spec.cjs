const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const source = fs.readFileSync('src/views/dcc/controlled-file/basic-data/components/ProjectFileTemplateEditor.vue', 'utf8')
const body = source.slice(source.indexOf('const validateRows ='), source.indexOf('const saveTemplate ='))
function validate(items) {
  const context = { rows: { value: items }, rowErrors: { value: [] }, message: { warning() {} } }
  vm.createContext(context)
  vm.runInContext(ts.transpile(body + ';globalThis.result = validateRows()'), context)
  return context.result
}
assert.equal(validate([{fileName: 'Only name'}]), true)
assert.equal(validate([{fileTypeTaxonomyId: 1, fileName: 'Root'}]), true)
assert.equal(validate([{fileName: ' Name '}, {fileTypeTaxonomyId: null, fileName: 'name'}]), false)
assert.equal(validate([{fileName: '   '}]), false)
assert.ok(!source.includes('taxonomyRows.value.length === 0'))
console.log('PASS: optional taxonomy editor validation (4 behavior cases)')
