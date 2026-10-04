const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync('src/api/dcc/controlledFile/workflow.ts', 'utf8')
const node = ts.createSourceFile('workflow.ts', source, ts.ScriptTarget.Latest, true)
const declarations = []
for (const statement of node.statements) {
  if (ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration =>
    ts.isIdentifier(declaration.name) && (declaration.name.text.startsWith('assert')
      || declaration.name.text === 'DCC_FORBIDDEN_FILE_CAPABILITY_FIELDS'))) declarations.push(statement.getText(node))
  if (ts.isClassDeclaration(statement) && statement.name?.text === 'DccControlledFileContractError') declarations.push(statement.getText(node))
}
const context = { exports: {}, Error, JSON, BigInt, Number, String, Object, Array }
vm.runInNewContext(ts.transpileModule(declarations.join('\n') + '\nexports.validate=assertControlledFileSubmitRequest',
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
const request = { sessionId: 'upload', originalUploadTicket: 'ticket', changeType: 'NEW', processType: 'CONTROLLED_FILE',
  dccProjectCodeId: '9007199254740993', fileTypeTaxonomyId: 6, projectFolderId: '9223372036854775807',
  projectFolderChangeReason: '归入项目文件夹', relatedControlledFileIds: ['9007199254740997'] }
test('formal submit contract accepts exact project folder and relation Long strings without conversion', () => {
  assert.doesNotThrow(() => context.exports.validate(request, 'upload'))
  assert.equal(request.dccProjectCodeId, '9007199254740993')
})
test('unsafe numeric and overflowing identities reject before transport', () => {
  for (const id of [Number.MAX_SAFE_INTEGER + 1, '9223372036854775808', 0, -1, '5.1']) {
    assert.throws(() => context.exports.validate({ ...request, dccProjectCodeId: id }, 'upload'))
  }
})

test('explicit departments preserve full Long IDs and reject empty, duplicate or unsafe identities', () => {
  const departments = ['9223372036854775701', '9223372036854775702']
  assert.doesNotThrow(() => context.exports.validate({ ...request, selectedSignoffDepartmentIds: departments }, 'upload'))
  for (const value of [[], ['7', 7], [9007199254740992], ['9223372036854775808'], null])
    assert.throws(() => context.exports.validate({ ...request, selectedSignoffDepartmentIds: value }, 'upload'))
  assert.deepEqual(departments, ['9223372036854775701', '9223372036854775702'])
})
