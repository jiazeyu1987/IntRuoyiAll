const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const origin = 'http://localhost:8081'
const oldPath = '/mes/pro/feedback/edhr-deviation'
const detailPath = `${oldPath}/detail`

function load(file, dependencies = {}) {
  const exports = {}
  const code = ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  Function('require', 'exports', 'window', code)(name => {
    assert.ok(Object.hasOwn(dependencies, name), `Unexpected dependency: ${name}`)
    return dependencies[name]
  }, exports, { location: { origin } })
  return exports
}
const navigation = load('src/utils/edhrWorkTaskNavigation.ts', {
  '@/utils/exactJavaLong': load('src/utils/exactJavaLong.ts'),
  '@/api/mes/pro/edhr/batchExecution': {
    openEdhrBatchTask: async () => { throw Error('Deviation task must not open a batch form') }
  }
})
const task = (overrides = {}) => ({
  id: '9007199254740993', taskType: 'DEVIATION_SIGN', businessScopeType: 'EDHR_DEVIATION',
  businessScopeId: '9007199254740995', actionUrl: `${oldPath}?deviationId=9007199254740995`,
  ...overrides
})

test('formal deviation responsibilities select their actual work area', () => {
  for (const taskType of ['DEVIATION_RECEIVE', 'DEVIATION_SIGN', 'DEVIATION_APPROVE', 'DEVIATION_CLOSE']) {
    assert.equal(navigation.resolveDeviationTaskTab(task({ taskType }), task().id, task().businessScopeId), 'disposition')
  }
  for (const taskType of ['DEVIATION_CORRECT', 'DEVIATION_VERIFY']) {
    assert.equal(navigation.resolveDeviationTaskTab(task({ taskType }), task().id, task().businessScopeId), 'corrective')
  }
})

test('work area cannot be selected by an unrelated, forged or unknown responsibility', () => {
  for (const changes of [{ id: '1' }, { businessScopeId: '2' }, { businessScopeType: 'OTHER' }, { taskType: 'FILL' }, { taskType: undefined }]) {
    assert.throws(() => navigation.resolveDeviationTaskTab(task(changes), task().id, task().businessScopeId))
  }
})

test('all deviation responsibilities upgrade the known legacy entry using the exact formal scope', async () => {
  for (const taskType of ['DEVIATION_RECEIVE', 'DEVIATION_CORRECT', 'DEVIATION_VERIFY',
    'DEVIATION_SIGN', 'DEVIATION_APPROVE', 'DEVIATION_CLOSE']) {
    const routes = []
    await navigation.navigateToEdhrWorkTask({ push: async route => routes.push(route) }, task({ taskType }), origin)
    assert.deepEqual(routes, [{ path: detailPath, query: {
      deviationId: '9007199254740995', workTaskId: '9007199254740993'
    } }])
  }
})

test('new deviation entry preserves its hash and exact identity', () => {
  assert.equal(navigation.normalizeEdhrWorkTaskRoute(task({
    actionUrl: `${detailPath}?deviationId=9007199254740995&workTaskId=9007199254740993#evidence`
  }), origin), `${detailPath}?deviationId=9007199254740995&workTaskId=9007199254740993#evidence`)
})

test('missing, conflicting, duplicate, foreign and forged identities fail before navigation', async () => {
  for (const entry of [oldPath, detailPath]) {
    for (const changes of [
      { id: undefined }, { businessScopeType: undefined }, { businessScopeType: 'BATCH_TASK' },
      { businessScopeId: undefined }, { businessScopeId: 9007199254740995 },
      { taskType: undefined }, { taskType: 'FILL' },
      { actionUrl: `${entry}` }, { actionUrl: `${entry}?deviationId=1` },
      { actionUrl: `${entry}?deviationId=9007199254740995&deviationId=9007199254740995` },
      { actionUrl: `${entry}?deviationId=9007199254740995&workTaskId=2` },
      { actionUrl: `${entry}?deviationId=9007199254740995&workTaskId=9007199254740993&workTaskId=9007199254740993` },
      { actionUrl: `https://external.invalid${entry}?deviationId=9007199254740995` },
      { actionUrl: `${entry}/forged?deviationId=9007199254740995` }
    ]) {
      const routes = []
      await assert.rejects(navigation.navigateToEdhrWorkTask({ push: async route => routes.push(route) },
        task({ actionUrl: `${entry}?deviationId=9007199254740995`, ...changes }), origin))
      assert.equal(routes.length, 0)
    }
  }
})

test('nondeviation routes retain existing behavior', () => {
  const route = navigation.normalizeEdhrWorkTaskRouteParts({
    id: '10', taskType: 'ARCHIVE', actionUrl: '/mes/pro/feedback/edhr-batch-execution/detail?id=3'
  }, origin)
  assert.equal(route.path, '/mes/pro/feedback/edhr-batch-execution/detail')
  assert.deepEqual(route.query, { id: '3', workTaskId: '10' })
})

function notifications(resolveTask, session = { syncSession: () => ({ key: 'session-1' }), isCurrent: key => key === 'session-1' }) {
  return load('src/utils/notifyMessageNavigation.ts', {
    '@/utils/exactJavaLong': load('src/utils/exactJavaLong.ts'),
    '@/utils/activeOrderHandoffNavigation': {}, './dccDistributionNavigation': {},
    './dccOfflineTrainingRecord': { DCC_OFFLINE_TRAINING_RECORD: 'DCC_OFFLINE_TRAINING_RECORD' },
    '@/utils/edhrWorkTaskNavigation': navigation,
    '@/store/modules/approvalCenterInvalidation': { useApprovalCenterInvalidationStore: () => session },
    '@/api/mes/pro/edhr/deviationTaskNavigation': { getDeviationTaskNavigation: resolveTask }
  })
}

test('historical notification resolves its actual assigned task before opening the scoped detail', async () => {
  const lookups = [], routes = []
  const api = notifications(async id => { lookups.push(id); return task() })
  const target = api.getNotifyMessageTarget({ templateParams: {
    actionUrl: task().actionUrl, workTaskId: task().id
  } })
  assert.equal(target.label, '处理偏差任务')
  await api.navigateToNotifyMessageTarget({ currentRoute: { value: {} }, push: async route => routes.push(route) }, target)
  assert.deepEqual(lookups, [task().id])
  assert.equal(routes[0].path, detailPath)
  assert.deepEqual(routes[0].query, { deviationId: task().businessScopeId, workTaskId: task().id })
})

test('notification cannot override formal task or deviation identity and lookup errors remain visible', async () => {
  for (const formal of [task({ id: '1' }), task({ businessScopeId: '1' }), task({ taskType: 'FILL' }), null]) {
    const api = notifications(async () => { if (!formal) throw Error('无权查看任务'); return formal })
    const routes = []
    const target = api.getNotifyMessageTarget({ templateParams: { actionUrl: task().actionUrl, workTaskId: task().id } })
    await assert.rejects(api.navigateToNotifyMessageTarget({ currentRoute: { value: {} }, push: async route => routes.push(route) }, target))
    assert.equal(routes.length, 0)
  }
})

test('deviation notification refuses imprecise, missing or conflicting task identity', () => {
  const api = notifications(async () => assert.fail('Invalid notification must not resolve a task'))
  for (const params of [
    { actionUrl: task().actionUrl },
    { actionUrl: task().actionUrl, workTaskId: 9007199254740993 },
    { actionUrl: `${task().actionUrl}&workTaskId=1`, workTaskId: task().id },
    { actionUrl: `${task().actionUrl}&workTaskId=${task().id}&workTaskId=${task().id}`, workTaskId: task().id }
  ]) assert.throws(() => api.getNotifyMessageTarget({ templateParams: params }))
})

test('a delayed deviation lookup cannot navigate after the login session or source route changes', async () => {
  for (const change of ['session', 'route']) {
    let resolveTask, sessionKey = 'session-1'
    const response = new Promise(resolve => { resolveTask = resolve })
    const routes = [], router = { currentRoute: { value: {} }, push: async route => routes.push(route) }
    let started
    const start = new Promise(resolve => { started = resolve })
    const api = notifications(async () => { started(); return response }, {
      syncSession: () => ({ key: sessionKey }), isCurrent: key => key === sessionKey
    })
    const target = api.getNotifyMessageTarget({ templateParams: { actionUrl: task().actionUrl, workTaskId: task().id } })
    const pending = api.navigateToNotifyMessageTarget(router, target)
    await start
    if (change === 'session') sessionKey = 'session-2'
    else router.currentRoute.value = {}
    resolveTask(task())
    await assert.rejects(pending, /导航上下文已变化/)
    assert.equal(routes.length, 0)
  }
})

test('the shared deviation navigator rechecks context immediately before router push', async () => {
  const routes = []
  let checks = 0
  await assert.rejects(navigation.navigateToEdhrWorkTask({ push: async route => routes.push(route) },
    task(), origin, () => ++checks === 1), /导航上下文已变化/)
  assert.equal(routes.length, 0)
})

test('a session change while closing the notification prevents even starting its lookup', async () => {
  let sessionKey = 'session-1'
  const api = notifications(async () => assert.fail('Changed session must not request the task'), {
    syncSession: () => ({ key: sessionKey }), isCurrent: key => key === sessionKey
  })
  const target = api.getNotifyMessageTarget({ templateParams: { actionUrl: task().actionUrl, workTaskId: task().id } })
  await assert.rejects(api.navigateToNotifyMessageTarget({ currentRoute: { value: {} }, push: async () => assert.fail() },
    target, { beforeNavigate: async () => { sessionKey = 'session-2' } }), /导航上下文已变化/)
})

test('the initiating session is captured synchronously before the first asynchronous boundary', async () => {
  let sessionKey = 'session-1', captures = 0
  const api = notifications(async () => assert.fail('New session must not inherit the prior navigation'), {
    syncSession: () => { captures++; return { key: sessionKey } }, isCurrent: key => key === sessionKey
  })
  const target = api.getNotifyMessageTarget({ templateParams: { actionUrl: task().actionUrl, workTaskId: task().id } })
  const pending = api.navigateToNotifyMessageTarget({ currentRoute: { value: {} }, push: async () => assert.fail() }, target)
  assert.equal(captures, 1, 'session must be captured before returning the initial navigation promise')
  sessionKey = 'session-2'
  await assert.rejects(pending, /导航上下文已变化/)
})
