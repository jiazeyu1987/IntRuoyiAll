const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const root = path.resolve(__dirname, '../../src/views/form-center/simple-common-parser')
const context = { exports: {}, require: () => ({ inspectionLevelIssue: () => undefined }) }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, 'draft.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, context)
const validate = context.exports.validateCommonDraft
const draft = (regulationId, regulationCode) => ({ regulationId, regulationCode,
  regulationName: '初包装', finalInspectionApplicable: true,
  processes: [{ processName: '初包装', items: [{ itemName: '外观', standardText: '完整',
    inspectionMethod: '目视', inspectionTool: '目视', samplingPlanText: '全检',
    applicableInspectionTypes: ['PATROL'], resultType: 'QUALITATIVE', equipmentOptions: [] }] }] })

test('existing common document requires a verified immutable code before publication', () => {
  assert.equal(validate(draft(3, 'PQC-CR-003'), 'PQC-CR-003').length, 0)
  assert.match(validate(draft(3, 'TEST'), 'PQC-CR-003')[0].message, /不可修改/)
  assert.match(validate(draft(3, 'PQC-CR-003'))[0].message, /尚未核实/)
  assert.equal(validate(draft(undefined, 'NEW-COMMON')).length, 0)
})

const source = fs.readFileSync(path.join(root, 'index.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const load = ast.statements.find(node => ts.isFunctionDeclaration(node)
  && node.name?.text === 'loadExistingRegulationCode').getText(ast)

test('identity lookup reads server code and rejects another regulation identity', async () => {
  const queried = []
  const ctx = { QcTemplateApi: { getCommonRegulationEditorVersion: async id => {
    queried.push(id)
    return { regulationId: id, regulationCode: 'PQC-CR-003' }
  } } }
  vm.runInNewContext(ts.transpileModule(load, { compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, ctx)
  assert.equal(await ctx.loadExistingRegulationCode(draft(3, 'INVALID-LOCAL-CODE')), 'PQC-CR-003')
  assert.deepEqual(queried, [3])
  ctx.QcTemplateApi.getCommonRegulationEditorVersion = async () => ({ regulationId: 4, regulationCode: 'PQC-CR-003' })
  await assert.rejects(ctx.loadExistingRegulationCode(draft(3, 'PQC-CR-003')), /身份核实失败/)
})

test('restored wrong local identity has a recovery action that restores the verified code', () => {
  assert.match(source, /function restoreExistingRegulationCode\(\)[\s\S]*draft\.value\.regulationCode = existingRegulationCode\.value/)
  assert.match(source, /恢复既有编号/)
  assert.match(source, /draft\.regulationCode\.trim\(\) !== existingRegulationCode/)
})
