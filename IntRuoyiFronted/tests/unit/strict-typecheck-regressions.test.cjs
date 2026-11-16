const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

function actions(file, names, context) {
  const source = parse(fs.readFileSync(file, 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file + '.ts', source, ts.ScriptTarget.Latest, true)
  const statements = names.map(name => ast.statements.find(statement =>
    ts.isVariableStatement(statement) && statement.declarationList.declarations.some(
      declaration => declaration.name.getText(ast) === name)))
  statements.forEach((statement, i) => assert.ok(statement, names[i]))
  const code = statements.map(statement => statement.getText(ast)).join('\n')
  vm.runInNewContext(ts.transpileModule(code + '\nglobalThis.actions = {' + names.join(',') + '}', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.actions
}

const qaPage = 'src/views/mes/pro/processpool/QaRegulationPage.vue'
function commonVersions(versions, failure) {
  const calls = []
  const api = actions(qaPage, ['resolvePositiveId', 'incrementVersionNo', 'resolveCommonMemberNextVersionNo'], {
    QcTemplateApi: { listCommonRegulationVersions: async id => {
      calls.push(id)
      if (failure) throw failure
      return versions
    } }
  })
  const member = { commonRegulationId: 12, commonRegulationName: '包装检验', versionNo: 'A/01' }
  return { next: api.resolveCommonMemberNextVersionNo, calls, member }
}

test('standalone common regulation selects its own versions without a DCC project', async () => {
  const h = commonVersions([
    { regulationId: 12, versionNo: 'A/01' }, { regulationId: 12, versionNo: 'A/02' },
    { regulationId: 99, versionNo: 'A/03' }
  ])
  assert.equal(await h.next(h.member), 'A/03')
  assert.deepEqual(h.calls, [12])
})

test('missing common regulation identity blocks the query', async () => {
  const h = commonVersions([])
  await assert.rejects(() => h.next({ ...h.member, commonRegulationId: undefined }), /ID不能为空/)
  assert.deepEqual(h.calls, [])
})

test('unrelated version records cannot invent a common regulation version', async () => {
  const h = commonVersions([{ regulationId: 99, versionNo: 'A/01' }])
  await assert.rejects(() => h.next(h.member), /缺少正式版本记录/)
})

test('common version service failure remains an error', async () => {
  const failure = new Error('version service unavailable')
  const h = commonVersions([], failure)
  await assert.rejects(() => h.next(h.member), error => error === failure)
})

function releaseFilter(id, failure) {
  const calls = []
  const api = actions('src/views/mes/pro/edhr/components/ReleaseEventListPane.vue', ['loadColumnFilterRows0'], {
    currentReleaseTransactionId: { value: id },
    queryParams: { pageNo: 7, pageSize: 20, eventType: 'RELEASED' },
    loadAllListPages: async load => (await load({ pageNo: 1, pageSize: 100 })).list,
    getEdhrReleaseEventPage: async request => {
      calls.push(JSON.parse(JSON.stringify(request)))
      if (failure) throw failure
      return { list: [{ id: 1 }], total: 1 }
    }
  })
  return { load: api.loadColumnFilterRows0, calls }
}

test('release filtering refuses to query without a release transaction', async () => {
  const h = releaseFilter(undefined)
  await assert.rejects(h.load, /缺少有效的放行事务编号/)
  assert.deepEqual(h.calls, [])
})

test('release filtering keeps the exact Long identity and filter across pagination', async () => {
  const id = '9007199254740993', h = releaseFilter(id)
  assert.equal((await h.load())[0].id, 1)
  assert.deepEqual(h.calls, [{ pageNo: 1, pageSize: 100, releaseTransactionId: id, eventType: 'RELEASED' }])
})

test('release filtering propagates service failure', async () => {
  const failure = new Error('release service unavailable'), h = releaseFilter('123', failure)
  await assert.rejects(h.load, error => error === failure)
})

test('project reference actions fail explicitly before the component is ready', () => {
  const component = { value: undefined }
  const api = actions('src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue',
    ['requireProjectReferences'], { projectReferences: component })
  assert.throws(api.requireProjectReferences, /引用列表尚未就绪/)
  const instance = { openSelector() {}, cancelSelectedReference() {}, refresh() {} }
  component.value = instance
  assert.equal(api.requireProjectReferences(), instance)
})
