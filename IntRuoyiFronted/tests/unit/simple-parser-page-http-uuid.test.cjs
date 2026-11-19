const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { ref } = require('vue')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const page = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/form-center/parser/SimpleParserPage.vue'), 'utf8')
const script = page.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const nodes = ast.statements.filter(node =>
  ts.isFunctionDeclaration(node) && ['importWord', 'restoreDraft'].includes(node.name.text)
  || ts.isVariableStatement(node) && node.declarationList.declarations.some(d => d.name.getText(ast) === 'requestId')
  || ts.isExpressionStatement(node) && ts.isCallExpression(node.expression)
    && node.expression.expression.getText(ast) === 'watch'
    && node.expression.arguments[0].getText(ast) === '[processes, routeName, projectId, workshopId]')
assert.equal(nodes.length, 4)
const code = ts.transpileModule(nodes.map(n => n.getText(ast)).join('\n')
  + '\nexports.api = { requestId, importWord, restoreDraft };', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const uuidV4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  let changed, draft, requests = 0
  const state = {
    exports: {}, ...loadUuid(crypto), ref,
    busy: ref(false), dirty: ref(false), projectId: ref(42), workshopId: ref(7),
    projects: ref([]), recognition: ref(), processes: ref([]), routeName: ref(''),
    filename: ref(''), selectedId: ref(''), mode: ref('edit'), error: ref(''), publishError: ref(''),
    loadingText: ref(''), loading: ref(false), deviceError: ref(''),
    trialSession: ref(0), reuseVisible: ref(false), craftOptions: ref([]), craftError: ref(''),
    current: ref(), selectedProject: ref({ projectName: '生产项目' }), restoring: false,
    watch: (_sources, callback) => { changed = callback },
    confirmAction: async () => true, resetEditing() {}, loadCrafts: async () => {},
    readSimpleWord: async () => undefined, loadDevices: async () => {},
    parseRecognition: JSON.parse, clone: data => JSON.parse(JSON.stringify(data)),
    knownDevice: () => undefined, errorText: error => error.message,
    message: { success() {}, info() {}, warning() {} },
    BatchRecordReportApi: { parseProductionBatchRecordTotalRecognitionJson: async () => {
      requests++
      return JSON.stringify({ processes: [{ name: '工序一' }, { name: '工序二' }] })
    } },
    draftKey: () => 'draft', localStorage: { getItem: () => JSON.stringify(draft) },
    getSimpleParserProject: async id => ({ id, status: 'ENABLE', projectName: '生产项目' })
  }
  vm.runInNewContext(code, state)
  return { ...state.exports.api, state, changed: () => changed(), requests: () => requests,
    draft: value => { draft = value } }
}

test('HTTP import and editing produce UUID v4 keys while draft restore preserves its saved command', async () => {
  const p = setup()
  assert.match(p.requestId.value, uuidV4)
  const initial = p.requestId.value
  await p.importWord({ raw: { name: '生产.doc', arrayBuffer: async () => new ArrayBuffer(0) } })
  assert.equal(p.state.error.value, '')
  assert.equal(p.requests(), 1)
  assert.equal(p.state.processes.value.length, 2)
  for (const item of p.state.processes.value) assert.match(item.id, uuidV4)
  assert.match(p.requestId.value, uuidV4)
  assert.notEqual(p.requestId.value, initial)
  const imported = p.requestId.value
  p.changed()
  assert.notEqual(p.requestId.value, imported)
  const saved = p.requestId.value
  const draft = { version: 1, project: { id: 42 }, recognition: {},
    processes: p.state.processes.value, requestId: saved }
  p.draft(draft)
  await p.restoreDraft()
  assert.equal(p.state.error.value, '')
  assert.equal(p.requestId.value, saved)
  p.draft({ ...draft, requestId: undefined })
  await p.restoreDraft()
  assert.equal(p.state.error.value, '')
  assert.match(p.requestId.value, uuidV4)
  assert.notEqual(p.requestId.value, saved)
})

test('missing secure random source stops page setup before requests', () => {
  assert.throws(() => setup({}), /不支持安全随机数/)
})
