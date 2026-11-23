const assert = require('node:assert/strict')
const fs = require('node:fs')
const { test } = require('node:test')
const ts = require('typescript')
const path = require('node:path')
const base = path.resolve(__dirname, '../../src/views/mes/pro/processpool/components')
const panel = require('vue/compiler-sfc').parse(fs.readFileSync(path.join(base, 'ActiveOrderSubmissionDetailPanel.vue'), 'utf8')).descriptor.scriptSetup.content
function label(name) {
 const ast = ts.createSourceFile('panel.ts', panel, ts.ScriptTarget.Latest, true)
 const d = ast.statements.filter(ts.isVariableStatement).flatMap(s => [...s.declarationList.declarations]).find(d => d.name.getText(ast) === name)
 return Function(ts.transpileModule('return ' + d.initializer.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)()
}
const action = label('signatureActionLabel'), auth = label('signatureAuthenticationLabel')
const source = fs.readFileSync(path.join(base, 'activeOrderSignatureEvidenceViewer.ts'), 'utf8').replace(/^import .*$/gm, '')
const exportsObject = {}
Function('exports', 'reactive', ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(exportsObject, v => v)
test('BPM approval label is limited to market release contract', () => {
 const evidence = { actionCode: 'APPROVE', moduleCode: 'BPM', subjectType: 'BPM_APPROVAL_TASK', subjectId: 'EDHR_RELEASE:2124640812268879873' }
 assert.equal(action(evidence), '批准上市放行')
 for (const patch of [{ moduleCode: 'MES' }, { subjectType: 'OTHER' }, { subjectId: 'OTHER:1' }, { actionCode: 'UNKNOWN' }]) assert.throws(() => action({ ...evidence, ...patch }))
 for (const actionCode of ['PRODUCTION_SUBMIT','TEAM_LEADER_REVIEW','PQC_SUBMIT','PQC_RELEASE','MARKET_RELEASE','FIELD_CHANGE','PQC_PATROL_ROUND_CORRECTION','QA_DISPOSITION']) assert.ok(action({ actionCode }))
 for (const method of ['PIN','SESSION_PLUS_PIN','SESSION_PLUS_SELECTED_USER_PIN','SESSION_PLUS_EMPLOYEE_PROFILE_PIN','PASSWORD','SESSION_PLUS_PASSWORD','SESSION_PLUS_SELECTED_USER_PASSWORD','SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD']) assert.ok(auth(method))
})
test('display failures clear selection and finish loading with explicit error', async () => {
 for (const validate of [r => action(r.evidence), r => auth('UNKNOWN'), () => { throw new Error('核验显示失败') }]) {
  const viewer = exportsObject.createActiveOrderSignatureEvidenceViewer(() => ({ scope: 'TEAM', identity: 1, activeOrderId: 1 }), async () => ({ activeOrderId: 1, evidence: { id: 2, actionCode: 'UNKNOWN' }, verification: { signatureId: 2 } }), validate)
  await viewer.open(2)
  assert.equal(viewer.state.loading, false); assert.equal(viewer.state.selected, undefined); assert.ok(viewer.state.error)
 }
})

function loadModule(file) {
 const text = fs.readFileSync(path.join(base, file), 'utf8')
 const ast = ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true)
 const body = ast.statements.filter(n => !ts.isImportDeclaration(n)).map(n => n.getText(ast)).join('\n')
 const result = {}
 Function('exports', ts.transpileModule(body, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(result)
 return result
}
test('Panel wires display validation into real viewer before selecting evidence', async () => {
 const ast = ts.createSourceFile('panel.ts', panel, ts.ScriptTarget.Latest, true)
 const declaration = ast.statements.filter(ts.isVariableStatement).flatMap(s => [...s.declarationList.declarations]).find(d => d.name.getText(ast) === 'signatureEvidenceViewer')
 const callback = declaration.initializer.arguments[2]
 assert.ok(callback, 'Panel must provide display validation')
 const { signatureVerificationDisplay } = loadModule('activeOrderSignatureVerificationDisplay.ts')
 const { verificationStatusLabel } = loadModule('activeOrderDisplayLabels.ts')
 let formatted = 0
 const validate = Function('signatureActionLabel', 'signatureAuthenticationLabel', 'verificationStatusLabel', 'signatureVerificationDisplay', 'formatDateTime', ts.transpileModule('return ' + callback.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)(action, auth, verificationStatusLabel, signatureVerificationDisplay, () => { formatted++; return '签署时间' })
 const result = {
  activeOrderId: 1,
  evidence: { id: 2, actionCode: 'APPROVE', moduleCode: 'BPM', subjectType: 'BPM_APPROVAL_TASK', subjectId: 'EDHR_RELEASE:2124640812268879873', authenticationMethod: 'SESSION_PLUS_PIN', verificationStatus: 'VALID', signedAt: 123 },
  verification: { signatureId: 2, verificationStatus: 'VALID', digestStatus: 'CONSISTENT', timeConsistencyStatus: 'CONSISTENT', auditBindingStatus: 'CONSISTENT', reasonCodes: [] }
 }
 const viewer = exportsObject.createActiveOrderSignatureEvidenceViewer(() => ({ scope: 'BATCH', identity: { batchExecutionId: 3 }, activeOrderId: 1 }), async () => result, validate)
 await viewer.open(2)
 assert.equal(viewer.state.selected, result); assert.equal(viewer.state.error, ''); assert.equal(viewer.state.loading, false); assert.equal(formatted, 1)
 for (const patch of [{ actionCode: 'UNKNOWN' }, { authenticationMethod: 'UNKNOWN' }, { subjectId: 'EDHR_RELEASE:0' }, { subjectId: 'EDHR_RELEASE:' }, { subjectId: 'OTHER:1' }, { subjectType: 'OTHER' }]) {
  const original = { ...result.evidence }
  Object.assign(result.evidence, patch)
  await viewer.open(2)
  assert.equal(viewer.state.selected, undefined); assert.equal(viewer.state.loading, false); assert.match(viewer.state.error, /缺少中文描述/)
  result.evidence = original
 }
 const template = require('vue/compiler-sfc').parse(fs.readFileSync(path.join(base, 'ActiveOrderSubmissionDetailPanel.vue'), 'utf8')).descriptor.template.content
 assert.match(template, /<el-alert v-if="signatureEvidenceViewer.state.error" :title="signatureEvidenceViewer.state.error"/)
})
