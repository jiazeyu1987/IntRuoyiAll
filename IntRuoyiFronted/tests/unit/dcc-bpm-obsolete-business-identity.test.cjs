const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const source = parse(fs.readFileSync('src/views/bpm/processInstance/detail/index.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('bpm.ts', source, ts.ScriptTarget.Latest, true)
const computedSource = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === 'dccControlledFileBusinessId')).getText(ast)
function resolve(instance, key = 'dcc-controlled-file-obsolete') {
  let identity = {}
  const helper = 'src/views/bpm/processInstance/detail/dcc-business-identity.ts'
  if (fs.existsSync(helper)) {
    const c = { exports: {} }
    vm.runInNewContext(ts.transpileModule(fs.readFileSync(helper, 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, c)
    identity = c.exports
  }
  const c = { exports: {}, computed: vue.computed, processInstance: vue.ref(instance), processDefinition: vue.ref({ key }), ...identity }
  vm.runInNewContext(ts.transpileModule(computedSource + '\nexports.id=dccControlledFileBusinessId', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c.exports.id.value
}
const id = '2054545668044084111'
const vars = { systemCode: 'DCC', objectType: 'CONTROLLED_FILE', actionCode: 'OBSOLETE', objectId: id, objectVersion: 'A/1' }
test('actual BPM detail resolves exact frozen obsolete object behind FORM_ACTION business key', () => {
  for (const key of ['dcc-controlled-file-obsolete', 'dcc-controlled-file-obsolete-approval'])
    assert.equal(resolve({ businessKey: 'FORM_ACTION:F-7', formVariables: vars }, key), id)
})
test('upload and revision retain their exact native numeric business identity', () => {
  for (const key of ['dcc-controlled-file-upload', 'dcc-controlled-file-revision', 'dcc-controlled-file-approval'])
    assert.equal(resolve({ businessKey: id }, key), id)
})
test('form identity cannot be borrowed from another module, action, object or invalid frozen facts', () => {
  for (const changes of [{ systemCode: 'MES' }, { objectType: 'PROJECT' }, { actionCode: 'UPLOAD' },
    { objectId: undefined }, { objectId: Number(id) }, { objectId: '0' }, { objectId: ' 20' },
    { objectId: '9223372036854775808' }, { objectVersion: undefined }])
    assert.equal(resolve({ businessKey: 'FORM_ACTION:F-7', formVariables: { ...vars, ...changes } }), '')
  assert.equal(resolve({ businessKey: 'FORM_ACTION:', formVariables: vars }), '')
  assert.equal(resolve({ businessKey: 'OTHER:F-7', formVariables: vars }), '')
  assert.equal(resolve({ businessKey: 'FORM_ACTION:F-7', formVariables: vars }, 'unrelated-process'), '')
})
