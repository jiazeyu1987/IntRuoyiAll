const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
function load() {
  const host = { exports: {}, Error, Promise }
  const source = fs.readFileSync('src/views/dcc/controlled-file/view/reading-lifecycle.ts', 'utf8')
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, host)
  return host.exports.createPreviewReadingLifecycle
}
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
function setup(startGate, heartbeatGate, stopGate) {
  const calls = [], errors = []
  let tick, visible = true, client = 0
  const api = {
    start: async id => { calls.push(['start', id]); return startGate ? startGate.promise : { sessionId: '9223372036854775001', heartbeatIntervalSeconds: 10, endedAt: null } },
    heartbeat: async id => { calls.push(['heartbeat', id]); return heartbeatGate ? heartbeatGate.promise : { sessionId: id, heartbeatIntervalSeconds: 10, endedAt: null } },
    stop: async id => { calls.push(['stop', id]); if (stopGate) await stopGate.promise; return { sessionId: id, heartbeatIntervalSeconds: 10, endedAt: null } }
  }
  const life = load()({ api, visible: () => visible, newClientId: () => 'client-' + (++client),
    schedule: callback => { tick = callback; return () => { tick = undefined } }, onError: error => errors.push(error.message) })
  return { life, api, calls, errors, tick: () => tick?.(), hide: () => { visible = false }, show: () => { visible = true } }
}

for (const finish of ['visible', 'hidden', 'disposed']) {
  test(`resume during late-start self-stop respects final ${finish} state`, async () => {
    const start = deferred(), stop = deferred(), stopped = deferred(), r = setup(start, undefined, stop)
    const stopApi = r.api.stop
    r.api.stop = id => { const pending = stopApi(id); stopped.resolve(); return pending }
    const original = r.life.resume(); r.hide()
    start.resolve({ sessionId: '9223372036854775001', heartbeatIntervalSeconds: 10, endedAt: null })
    await stopped.promise
    r.show()
    const resumed = r.life.resume(), duplicate = r.life.resume()
    assert.deepEqual(r.calls.map(c => c[0]), ['start', 'stop'])
    let closed
    if (finish === 'hidden') r.hide()
    if (finish === 'disposed') closed = r.life.dispose()
    stop.resolve()
    await original; await resumed; await duplicate; await closed; await r.tick()
    assert.deepEqual(r.calls.map(c => c[0]), finish === 'visible'
      ? ['start', 'stop', 'start', 'heartbeat'] : ['start', 'stop'])
    assert.deepEqual(r.calls.filter(c => c[0] === 'start').map(c => c[1]), finish === 'visible'
      ? ['client-1', 'client-2'] : ['client-1'])
    assert.deepEqual(r.errors, [])
    await r.life.dispose()
  })
}
test('late-start self-stop failure rejects queued resume without retrying', async () => {
  const start = deferred(), stop = deferred(), stopped = deferred(), r = setup(start, undefined, stop)
  const stopApi = r.api.stop
  r.api.stop = id => { const pending = stopApi(id); stopped.resolve(); return pending }
  const original = r.life.resume(); r.hide()
  start.resolve({ sessionId: '9223372036854775001', heartbeatIntervalSeconds: 10, endedAt: null })
  await stopped.promise
  r.show(); const resumed = r.life.resume()
  const rejected = Promise.all([
    assert.rejects(original, /stop rejected/), assert.rejects(resumed, /stop rejected/)
  ])
  stop.reject(new Error('stop rejected'))
  await rejected; await r.tick()
  assert.deepEqual(r.calls.map(c => c[0]), ['start', 'stop'])
  assert.deepEqual(r.errors, ['stop rejected'])
  await r.life.dispose()
})

test('disposal while start is pending stops only the returned old session and installs no heartbeat', async () => {
  const gate = deferred(), r = setup(gate)
  const pending = r.life.resume()
  const closed = r.life.dispose()
  gate.resolve({ sessionId: '9223372036854775001', heartbeatIntervalSeconds: 10, endedAt: null })
  await pending; await closed; await r.tick()
  assert.deepEqual(r.calls.map(c => c[0]), ['start', 'stop'])
})
test('hidden before a delayed start response never starts background heartbeat', async () => {
  const gate = deferred(), r = setup(gate)
  const pending = r.life.resume(); r.hide()
  gate.resolve({ sessionId: '9223372036854775001', heartbeatIntervalSeconds: 10, endedAt: null })
  await pending; await r.tick()
  assert.deepEqual(r.calls.map(c => c[0]), ['start', 'stop'])
})
test('pause waits for in-flight heartbeat before stopping; duplicate ticks do not overlap', async () => {
  const gate = deferred(), r = setup(undefined, gate)
  await r.life.resume(); const heartbeat = r.tick(); await r.tick()
  const close = r.life.pause()
  assert.deepEqual(r.calls.map(c => c[0]), ['start', 'heartbeat'])
  gate.resolve({ sessionId: '9223372036854775001', heartbeatIntervalSeconds: 10, endedAt: null })
  await heartbeat; await close
  assert.deepEqual(r.calls.map(c => c[0]), ['start', 'heartbeat', 'stop'])
})
test('resume after pause gets a fresh client generation and preserves long session IDs', async () => {
  const r = setup(); await r.life.resume(); await r.life.pause(); await r.life.resume(); await r.tick(); await r.life.dispose()
  assert.deepEqual(r.calls.filter(c => c[0] === 'start').map(c => c[1]), ['client-1', 'client-2'])
  assert.equal(r.calls.find(c => c[0] === 'heartbeat')[1], '9223372036854775001')
})
test('heartbeat failure stops scheduling and reports the actual error', async () => {
  const gate = deferred(), r = setup(undefined, gate)
  await r.life.resume(); const pending = r.tick()
  gate.reject(new Error('permission revoked')); await pending; await r.tick()
  assert.deepEqual(r.errors, ['permission revoked'])
  assert.equal(r.calls.filter(c => c[0] === 'heartbeat').length, 1)
  await r.life.dispose()
})
test('visible again during pending pause waits for old closure then starts a fresh generation', async () => {
  const gate = deferred(), r = setup(gate)
  const original = r.life.resume(), pause = r.life.pause(), resumed = r.life.resume()
  gate.resolve({ sessionId: '9223372036854775001', heartbeatIntervalSeconds: 10, endedAt: null })
  await original; await pause; await resumed
  assert.deepEqual(r.calls.map(c => c[0]), ['start', 'stop', 'start'])
  assert.deepEqual(r.calls.filter(c => c[0] === 'start').map(c => c[1]), ['client-1', 'client-2'])
  await r.life.dispose()
})
test('idempotent start replay of an ended server session is rejected before scheduling heartbeat', async () => {
  const gate=deferred(),r=setup(gate)
  const pending=r.life.resume()
  gate.resolve({sessionId:'9223372036854775001',heartbeatIntervalSeconds:10,endedAt:'2026-10-15T08:00:00Z'})
  await assert.rejects(pending,/已结束/)
  await r.tick()
  assert.equal(r.calls.filter(c=>c[0]==='heartbeat').length,0)
  assert.equal(r.errors.length,1)
  await r.life.dispose()
})
test('heartbeat reply for a different or already ended session is rejected and cannot become active', async () => {
  for (const invalid of [
    {sessionId:'different-session',heartbeatIntervalSeconds:10,endedAt:null},
    {sessionId:'9223372036854775001',heartbeatIntervalSeconds:10,endedAt:'2026-10-15T08:00:15Z'}
  ]) {
    const gate=deferred(), r=setup(undefined,gate)
    await r.life.resume()
    const heartbeat=r.tick()
    gate.resolve(invalid)
    await heartbeat
    assert.equal(r.calls.filter(c=>c[0]==='heartbeat').length,1)
    assert.equal(r.errors.length,1)
    await r.tick()
    assert.equal(r.calls.filter(c=>c[0]==='heartbeat').length,1,'invalid reply must cancel the timer')
    await r.life.resume()
    assert.equal(r.calls.filter(c=>c[0]==='start').length,1,'failed heartbeat must not leave the ended/mismatched session active')
    await r.life.dispose()
    assert.equal(r.calls.find(c=>c[0]==='stop')[1],'9223372036854775001','cleanup must retain the original capability session')
  }
})
