const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

test('actual detail watcher initializes its user, file, mode and application-round dependencies before reading them', async () => {
  const file = path.resolve(__dirname, '../../src/views/dcc/controlled-file/detail/index.vue')
  const source = parse(fs.readFileSync(file, 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file + '.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const names = new Set(['userStore', 'currentUserId', 'controlledFileId', 'viewerMode', 'isBrowserTraceabilityPage',
    'applicationRoundContextKey', 'applicationRoundSelection', 'approvalProcessInstanceId',
    'isDelayedEffectiveTask', 'delayedConfirmationIdentity'])
  const statements = ast.statements.filter(node => ts.isVariableStatement(node)
    ? node.declarationList.declarations.some(d => names.has(d.name.getText(ast)))
    : ts.isExpressionStatement(node) && ts.isCallExpression(node.expression)
      && node.expression.expression.getText(ast) === 'watch'
      && node.expression.arguments[0]?.getText(ast) === 'delayedConfirmationIdentity')
  assert.equal(statements.length, names.size + 1, 'exercise all actual dependency declarations and watcher in source order')
  const user = vue.reactive({ getUser: { id: 71 } })
  const scope = vue.effectScope()
  const roundModule = { exports: {} }
  const roundFile = path.resolve(__dirname, '../../src/views/dcc/controlled-file/detail/application-round-context.ts')
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(roundFile, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, roundModule)
  const ports = {
    computed: vue.computed, watch: vue.watch, useUserStore: () => user,
    route: vue.reactive({ fullPath: '/dcc/controlled-file/detail/2054545668044084181', params: { id: '2054545668044084181' }, query: {} }),
    fileDetail: vue.ref(), approvalTodoTask: vue.ref(), approvalProgressScope: vue.ref(),
    applicationApprovalRead: vue.ref(), detailReadSelectionState: vue.ref({ error: '', selection: undefined }),
    resolveApplicationRoundSelection: roundModule.exports.resolveApplicationRoundSelection,
    isControlledFileViewerMode: () => false, loadDelayedConfirmation: () => {}
  }
  try {
    const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n')
      + '\n globalThis.readIdentity = () => delayedConfirmationIdentity.value;', {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
    scope.run(() => vm.runInNewContext(code, ports, { filename: file }))
    assert.equal(JSON.parse(ports.readIdentity())[2], '71')
    user.getUser.id = 72
    await vue.nextTick()
    assert.equal(JSON.parse(ports.readIdentity())[2], '72', 'keep reactive account identity changes')
  } finally { scope.stop() }
})
