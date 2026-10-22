const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const file = path.resolve(__dirname, '../../src/api/dcc/controlledFile/workflow.ts')
const source = fs.readFileSync(file, 'utf8')
const ast = ts.createSourceFile('workflow.ts', source, ts.ScriptTarget.Latest, true)
const statements = ast.statements.filter(statement => {
  if (ts.isClassDeclaration(statement)) return statement.name?.text === 'DccControlledFileContractError'
  return ts.isVariableStatement(statement) && statement.declarationList.declarations.some(item =>
    ts.isIdentifier(item.name) && (item.name.text.startsWith('assert') || [
      'DCC_FORBIDDEN_FILE_CAPABILITY_FIELDS', 'createWorkingControlledFile', 'submitControlledFile'
    ].includes(item.name.text)))
})
const load = () => {
  const calls = []
  const context = { exports: {}, Error, request: { post: async request => { calls.push(request); return 73 } } }
  vm.runInNewContext(ts.transpileModule(statements.map(node => node.getText(ast)).join('\n'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return { wrappers: context.exports, calls }
}
const request = overrides => ({
  processType: 'CONTROLLED_FILE', changeType: 'NEW', sessionId: 'g12-session',
  originalUploadTicket: 'g12-ticket', dccProjectCodeId: '5', projectFolderId: '9007199254740993',
  fileTypeTaxonomyId: '6', relatedControlledFileIds: [], selectedSignoffDepartmentIds: ['7'], ...overrides
})

for (const [method, url] of [
  ['createWorkingControlledFile', '/dcc/controlled-files/working'],
  ['submitControlledFile', '/dcc/controlled-files/submit']
]) {
  test(`G12 actual ${method} accepts empty/omitted explanations without injecting text`, async () => {
    const { wrappers, calls } = load()
    for (const reason of [undefined, '', ' \t ']) {
      const payload = request(reason === undefined ? {} : { projectFolderChangeReason: reason })
      assert.equal(await wrappers[method](payload), 73)
      assert.equal(calls.at(-1).url, url)
      assert.strictEqual(calls.at(-1).data, payload)
      assert.equal(calls.at(-1).data.projectFolderChangeReason, reason)
    }
    assert.equal(calls.length, 3)
  })

  test(`G12 actual ${method} validates optional explanation type and length before HTTP`, async () => {
    const { wrappers, calls } = load()
    assert.equal(await wrappers[method](request({ projectFolderChangeReason: '真'.repeat(500) })), 73)
    for (const reason of ['长'.repeat(501), 123, true, {}, []]) {
      await assert.rejects(wrappers[method](request({ projectFolderChangeReason: reason })))
    }
    assert.equal(calls.length, 1)
  })

  test(`G12 actual ${method} keeps exact project/folder identity and caller-directory rejection`, async () => {
    const { wrappers, calls } = load()
    for (const change of [
      { projectFolderId: undefined }, { projectFolderId: '0' }, { projectFolderId: '9223372036854775808' },
      { dccProjectCodeId: undefined }, { dccProjectCodeId: 'not-an-id' }, { directoryId: 101 },
      { originalUploadTicket: '' }
    ]) {
      await assert.rejects(wrappers[method](request({ projectFolderChangeReason: '', ...change })))
    }
    assert.equal(calls.length, 0)
  })
}
