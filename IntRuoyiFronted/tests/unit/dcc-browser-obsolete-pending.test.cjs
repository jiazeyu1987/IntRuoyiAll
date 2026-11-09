const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const FILE = '9223372036854775701'
const browser = fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue', 'utf8')
function declaration(source, name, context) {
  const ast = ts.createSourceFile('page.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  let selected
  for (const statement of ast.statements) {
    if (ts.isVariableStatement(statement) && statement.declarationList.declarations.some(d => d.name.getText(ast) === name)) selected = statement.getText(ast)
  }
  assert.ok(selected, 'actual public declaration missing: ' + name)
  const compiled = ts.transpileModule(selected + '\nexports.value=' + name, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const state = { exports: {}, Error, String, Number, ...context }
  vm.runInNewContext(compiled, state)
  return state.exports.value
}

const presentation = fs.readFileSync('src/views/dcc/controlled-file/browser/presentation.ts', 'utf8')
const getBrowserRowActionState = declaration(presentation, 'getBrowserRowActionState', {
  hasDccControlledFileActionProjection: row => !!row.actionProjection,
  isDccControlledFileActionAllowed: (row, action) => row.actionProjection?.allowedActions?.includes(action) === true,
  resolveDccActionProjectionReadonlyReason: () => '',
  DCC_ACTION_PROJECTION_MISSING_REASON: 'missing', isBrowserHistoryVisible: () => false
})
function invoke(projection) {
  const warnings = [], opened = []
  const row = {id: FILE, actionProjection: projection}
  const click = declaration(browser, 'handleObsolete', {
    getBrowserRowActionState, message: {warning: text => warnings.push(text)},
    openManagement: (...args) => opened.push(args)
  })
  click(row)
  return {state: getBrowserRowActionState(row), warnings, opened}
}
test('pending obsolete remains visible and clicking only explains the pending application', () => {
  const result = invoke({actionLockReason: 'OBSOLETE_APPROVAL_PENDING', allowedActions: ['VIEW']})
  assert.equal(result.state.showObsolete, true)
  assert.equal(result.state.canObsolete, false)
  assert.equal(result.opened.length, 0)
  assert.deepEqual(result.warnings, ['该文件已有作废申请正在审批中，请勿重复提交。'])
})
test('authorized obsolete still opens the existing application entry', () => {
  const result = invoke({actionLockReason: null, allowedActions: ['VIEW', 'OBSOLETE']})
  assert.equal(result.state.showObsolete, true)
  assert.equal(result.warnings.length, 0)
  assert.equal(result.opened[0][0], FILE)
  assert.equal(result.opened[0][1].operation, 'obsolete')
})
test('missing permission without a pending application does not expose obsolete', () => {
  for (const projection of [undefined, {allowedActions: ['VIEW'], actionLockReason: null}]) {
    const result = invoke(projection)
    assert.equal(result.state.showObsolete, false)
    assert.equal(result.opened.length, 0)
  }
})
