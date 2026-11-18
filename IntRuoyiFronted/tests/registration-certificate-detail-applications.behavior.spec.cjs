const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const root = path.resolve(__dirname, '..')
const read = file => readFileSync(path.join(root, file), 'utf8')
const compile = source => ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const display = {}
Function('exports', compile(read('src/views/dcc/registration-certificate/shared/applicationDisplay.ts')))(display)

test('application labels keep file-review states separate and format Beijing time across host zones', () => {
  assert.equal(display.formatApplicationCertificateType('DOMESTIC'), '国内')
  assert.equal(display.formatApplicationCertificateType('CE'), 'CE')
  assert.equal(display.formatApplicationFileReview('REQUESTED'), '待审批')
  assert.equal(display.formatApplicationFileReview('APPROVED'), '已通过')
  assert.equal(display.formatApplicationFileReview('BPM_BOUND'), '文件审批状态异常')
  const originalZone = process.env.TZ
  try {
    for (const zone of ['America/New_York', 'UTC', 'Asia/Shanghai']) {
      process.env.TZ = zone
      assert.equal(display.formatApplicationSubmittedAt(1794933475000), '2026-11-18 00:37:55')
      assert.equal(display.formatApplicationSubmittedAt('1794882709000'), '2026-11-17 10:31:49')
    }
  } finally {
    if (originalZone === undefined) delete process.env.TZ
    else process.env.TZ = originalZone
  }
  assert.equal(display.formatApplicationSubmittedAt('bad'), '提交时间格式异常')
  assert.equal(display.formatApplicationSubmittedAt(null), '未返回提交时间')
})

const source = read('src/views/dcc/registration-certificate/detail/index.vue').split('<script setup lang="ts">')[1].split('</script>')[0]
const ast = ts.createSourceFile('detail.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const loader = ast.statements.find(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => declaration.name.getText(ast) === 'loadDetail'))
const watcher = ast.statements.find(statement => ts.isExpressionStatement(statement) &&
  ts.isCallExpression(statement.expression) && statement.expression.expression.getText(ast) === 'watch')
assert.ok(loader && watcher)
const tick = async () => { await vue.nextTick(); await new Promise(resolve => setImmediate(resolve)) }

test('cached approval material loads formal detail on query removal, isolates other paths and late results', async () => {
  const route = vue.reactive({ path: '/detail/1', fullPath: '/detail/1?requestId=9', query: { requestId: '9', processInstanceId: 'p' } })
  const props = vue.reactive({})
  const refs = Object.fromEntries(['routeIdentityError', 'accessRequestCertificateId', 'detail', 'history',
    'downloadAuthorizedFileIds', 'requestPendingFileIds', 'loading'].map(key => [key, vue.ref()]))
  const calls = []
  let pending
  let failure
  const env = { ...refs, route, props, detailPagePath: '/detail/1', watch: vue.watch,
    approvalRequestId: vue.computed(() => route.query.requestId),
    approvalProcessInstanceId: vue.computed(() => route.query.processInstanceId),
    invalidRoute: vue.ref(false), detailVersionId: vue.computed(() => route.query.versionId),
    resolveCertificateIdForDetail: async () => '1',
    getRegistrationCertificateDetail: async (id, version) => {
      calls.push([id, version])
      if (failure) throw failure
      return pending || { certificateId: id, versionId: version || 'current' }
    },
    getRegistrationCertificateHistory: async () => [], loadDownloadGrantStatuses: async () => {}
  }
  const scope = vue.effectScope()
  scope.run(() => Function(...Object.keys(env), compile(loader.getText(ast) + '\n' + watcher.getText(ast)))(...Object.values(env)))
  try {
    await tick()
    assert.equal(calls.length, 0, 'approval material does not request formal detail')
    route.query = {}; route.fullPath = '/detail/1'
    await tick()
    assert.deepEqual(calls, [['1', undefined]])
    assert.equal(refs.detail.value.versionId, 'current')
    let resolvePending
    pending = new Promise(resolve => { resolvePending = resolve })
    route.query = { versionId: 'old' }; route.fullPath = '/detail/1?versionId=old'
    await tick()
    assert.deepEqual(calls[1], ['1', 'old'], 'version query must select the requested version')
    route.path = '/list'; route.fullPath = '/list'
    await tick()
    resolvePending({ certificateId: '1', versionId: 'old' })
    await tick()
    assert.equal(refs.detail.value, undefined, 'late response from inactive identity is discarded')
    assert.equal(calls.length, 2, 'inactive cached page must not fetch for list')
    pending = undefined
    route.path = '/detail/1'; route.query = {}; route.fullPath = '/detail/1'
    await tick()
    assert.equal(refs.detail.value.versionId, 'current')
    assert.equal(calls.length, 3)
    failure = new Error('无权访问该注册证')
    route.query = { versionId: 'denied' }; route.fullPath = '/detail/1?versionId=denied'
    await tick()
    assert.equal(refs.detail.value, undefined)
    assert.match(refs.routeIdentityError.value, /无权访问该注册证/)
    assert.equal(refs.loading.value, false)
  } finally { scope.stop() }
})
