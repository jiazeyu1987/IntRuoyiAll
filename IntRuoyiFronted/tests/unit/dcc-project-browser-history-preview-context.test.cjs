const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const source = parse(fs.readFileSync('src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue', 'utf8'))
  .descriptor.scriptSetup.content
const ast = ts.createSourceFile('ProjectBrowserPanel.ts', source, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const statement = ast.statements.find(item => ts.isVariableStatement(item)
    && item.declarationList.declarations.some(entry => ts.isIdentifier(entry.name) && entry.name.text === name))
  assert.ok(statement, `missing ${name}`)
  return statement.getText(ast)
}

test('historical row preview passes its exact VERSION_HISTORY identity to the viewer path', async () => {
  let previewArgs
  const context = {
    exports: {},
    state: { error: '' },
    serializeDccBodyReadContext: (id, body) => JSON.stringify({ id, ...body }),
    preview: async (...args) => { previewArgs = args }
  }
  const code = ts.transpileModule(`${declaration('previewRow')}\nexports.previewRow = previewRow`, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code, context)
  const row = { controlledFileId: '9007199254740993', versionNo: 'A/1', status: 'OBSOLETE', canPreview: true }

  await context.exports.previewRow(row)

  assert.equal(previewArgs[0], row)
  assert.deepEqual(JSON.parse(previewArgs[1]), {
    id: row.controlledFileId,
    intent: 'VERSION_HISTORY',
    controlledFileId: row.controlledFileId,
    expectedVersionNo: row.versionNo
  })
})
