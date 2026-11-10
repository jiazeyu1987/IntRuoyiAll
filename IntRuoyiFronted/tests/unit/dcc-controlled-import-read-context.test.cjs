const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const { body } = require('./dcc-detail-read-test-loader.cjs')
const filename = 'src/views/dcc/controlled-file/detail/index.vue'
const source = parse(fs.readFileSync(filename, 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile(filename, source, ts.ScriptTarget.Latest, true)
const declarations = ['resolveDetailBodyReadContext', 'openHistoryDetail'].map(name => ast.statements.find(node =>
  ts.isVariableStatement(node) && node.declarationList.declarations.some(item => item.name.text === name)).getText(ast)).join('\n')
function host(file, query = {}) {
  const navigation = []
  const state = {
    exports: {}, ...body,
    fileDetail: vue.ref(file), controlledFileId: vue.ref('70'), route: { query, fullPath: '/detail/70' },
    approvalProgressScope: vue.ref(), approvalTodoTask: vue.ref(), viewerMode: vue.ref(false),
    currentUserId: vue.ref('8'), canUploadApplicantTrainingRecord: vue.ref(false),
    isDccControlledFileActionAllowed: () => false, ensureCurrentDetailContext: () => true,
    router: { push: value => navigation.push(value) },
    buildControlledFileViewerPath: (id, from, returnTo, context) => ({ id, from, returnTo, context })
  }
  vm.runInNewContext(ts.transpileModule(declarations + '\nexports.read=resolveDetailBodyReadContext;exports.history=openHistoryDetail', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, state)
  return { state, navigation }
}
const imported = () => ({ id: '70', versionNo: 'A/1', status: 'CONTROLLED_PENDING_EFFECTIVE', importSource: 'CONTROLLED_IMPORT' })
test('actual main preview chooses exact VERSION_HISTORY for future controlled import without fabricating BPM', () => {
  const { state } = host(imported()), result = state.exports.read()
  assert.equal(result.intent, 'VERSION_HISTORY')
  assert.equal(result.controlledFileId, '70'); assert.equal(result.expectedVersionNo, 'A/1')
  assert.equal(result.applicationType, undefined); assert.equal(result.bpmRound, undefined)
})
test('actual history navigation uses the selected imported pending version exact identity', () => {
  const { state, navigation } = host({ id: '70', versionHistory: [{ ...imported(), id: '71', versionNo: 'B/1' }] })
  state.exports.history('71')
  const result = body.readDccBodyReadContextRoute('71', { bodyReadContext: navigation[0].context })
  assert.equal(result.intent, 'VERSION_HISTORY'); assert.equal(result.expectedVersionNo, 'B/1')
  assert.equal(result.bpmRound, undefined)
})
test('ordinary native pending history retains its exact approval round and main preview is not relabeled', () => {
  const pending = { id: '71', versionNo: 'B/1', status: 'CONTROLLED_PENDING_EFFECTIVE', changeType: 'REVISION', processInstanceId: 'native-round' }
  const { state, navigation } = host({ ...pending, id: '70', versionHistory: [pending] })
  assert.equal(state.exports.read().intent, 'DAILY')
  state.exports.history('71')
  const result = body.readDccBodyReadContextRoute('71', { bodyReadContext: navigation[0].context })
  assert.equal(result.intent, 'APPROVAL_HISTORY'); assert.equal(result.bpmRound, 'native-round')
  assert.equal(result.applicationType, 'REVISION')
})
test('explicit daily context remains daily and an active import still uses daily rights', () => {
  const dailyQuery = body.buildDccBodyReadQuery('70', { intent: 'DAILY', controlledFileId: '70' })
  assert.equal(host(imported(), dailyQuery).state.exports.read().intent, 'DAILY')
  assert.equal(host({ ...imported(), status: 'ACTIVE' }).state.exports.read().intent, 'DAILY')
})
