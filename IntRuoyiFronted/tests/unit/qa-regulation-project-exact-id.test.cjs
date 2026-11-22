const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/QaRegulationPage.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const source = ts.createSourceFile(filename + '.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['resolveDccProjectId', 'createQaRegulationProjectStatusMap', 'isDccProjectCodeConfigured',
  'sortDccProjectCodeOptionsByQaStatus', 'mergeDccProjectCodeOptions', 'handleDccProjectCodeChange']
const functions = source.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(source))))
assert.equal(functions.length, names.length)
const helper = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.resolve(__dirname,
  '../../src/utils/exactJavaLong.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, helper)

function harness(projects = []) {
  const selected = [], queries = []
  const context = {
    requireExactJavaLong: helper.exports.requireExactJavaLong,
    qaRegulationProjectStatusByDccId: { value: new Map() },
    dccProjectCodeOptions: { value: projects }, DCC_PROJECT_CODE_STATUS_ENABLE: 'ENABLE',
    selectDccProjectCode: async project => selected.push(project),
    getProjectCode: async id => { queries.push(id); throw Error('unavailable project') }
  }
  vm.runInNewContext(ts.transpileModule(functions.map(statement => statement.getText(source)).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText + '\nglobalThis.actions={' + names.join(',') + '}', context)
  return { ...context.actions, context, selected, queries }
}

test('adjacent 19-digit projects retain separate options, status and exact selection', async () => {
  const first = { id: '2121453181393523028', status: 'ENABLE' }
  const second = { id: '2121453181393523029', status: 'ENABLE' }
  const h = harness([first, second])
  assert.equal(h.mergeDccProjectCodeOptions([first, second, first]).length, 2)
  h.context.qaRegulationProjectStatusByDccId.value = h.createQaRegulationProjectStatusMap([
    { dccProjectCodeId: first.id, configured: false },
    { dccProjectCodeId: second.id, configured: true }
  ])
  assert.equal(h.isDccProjectCodeConfigured(first), false)
  assert.equal(h.isDccProjectCodeConfigured(second), true)
  assert.deepEqual(Array.from(h.sortDccProjectCodeOptionsByQaStatus([first, second]), p => p.id),
    [second.id, first.id])
  await h.handleDccProjectCodeChange(second.id)
  assert.equal(h.selected[0], second)
  assert.deepEqual(h.queries, [])
})

test('small numeric IDs match their exact string selection and sort numerically', async () => {
  const h = harness([{ id: 10, status: 'ENABLE' }, { id: '2', status: 'ENABLE' }])
  await h.handleDccProjectCodeChange('10')
  assert.equal(h.selected[0].id, 10)
  assert.deepEqual(Array.from(h.sortDccProjectCodeOptionsByQaStatus(h.context.dccProjectCodeOptions.value),
    p => String(p.id)), ['2', '10'])
})

test('full Java Long IDs stay exact while invalid or already rounded identities fail', () => {
  const h = harness()
  assert.equal(h.resolveDccProjectId('9223372036854775807'), '9223372036854775807')
  for (const value of [undefined, null, '', 0, -1, '1.5', '9223372036854775808',
    Number('2121453181393523028')]) {
    assert.throws(() => h.resolveDccProjectId(value), /DCC 项目代码 ID/)
  }
})
