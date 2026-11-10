const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const file = path.resolve(__dirname, '../src/utils/notifyMessageNavigation.ts')
const ast = ts.createSourceFile(file, fs.readFileSync(file, 'utf8'), ts.ScriptTarget.Latest, true)
const entry = ast.statements.find(s => ts.isVariableStatement(s)
  && s.declarationList.declarations.some(d => d.name.getText(ast) === 'getNotifyMessageTargets'))
const code = ts.transpileModule(entry.getText(ast).replace(/^export\s+/, ''), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const targets = Function('normalizeTemplateParams', code + '; return getNotifyMessageTargets;')(value => value)
const id = '9007199254740993'
const message = { templateCode: 'dcc_related_file_revision', templateParams: {
  sourceControlledFileId: id, detailUrl: `/dcc/controlled-file/detail/${id}?viewer=1&from=notification`
} }
assert.deepEqual(targets(message), [{ type: 'dccPublication', label: '查看关联文件', targetId: id }])
for (const bad of [
  { ...message.templateParams, sourceControlledFileId: Number(id) },
  { ...message.templateParams, detailUrl: 'https://evil.invalid/dcc/controlled-file/detail/1' },
  { ...message.templateParams, detailUrl: message.templateParams.detailUrl + '&management=1' },
  { ...message.templateParams, detailUrl: '/dcc/controlled-file/detail/123?viewer=1&from=notification' }
]) assert.deepEqual(targets({ ...message, templateParams: bad }), [])
console.log('PASS: revision notification preserves file identity and opens only the related file viewer')
