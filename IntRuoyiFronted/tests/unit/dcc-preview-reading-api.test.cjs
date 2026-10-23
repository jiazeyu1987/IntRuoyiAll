const { test } = require('node:test')
const assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
function load() {
  const calls = [], host = { exports: {}, Error, BigInt, Object, Number, Date,
    require: name => {
      if (name === '@/config/axios') return { default: { post: async req => { calls.push(req); return { sessionId: '9223372036854775001', openedAt: '2026-10-15T08:00:00Z', startedAt:'2026-10-15T08:00:01Z',lastHeartbeatAt:'2026-10-15T08:00:11Z',endedAt:null, accumulatedSeconds:10, heartbeatIntervalSeconds:10,maxHeartbeatGapSeconds:15 } }, get: async req => { calls.push(req); return { firstOpenedAt:null,lastOpenedAt:null, accumulatedSeconds:0, openedSessionCount:0 } } } }
      if (name === './bodyReadContext') return { buildDccBodyReadHeaders: (id, context) => ({ 'X-DCC-Body-Read-Context': JSON.stringify({ id, ...context }) }) }
      throw new Error(name)
    } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/api/dcc/controlledFile/readingSession.ts','utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target:ts.ScriptTarget.ES2022 } }).outputText, host)
  return { api: host.exports.createPreviewReadingApi, calls }
}
const metadata=()=>({ viewerToken:'secret-header-token',accessEventCode:'event-1',watermarkTraceCode:'trace-1',viewerTokenId:'id-1',viewerTokenNonce:'nonce-1' })
test('DCC lifecycle captures exact source and sends token only in header', async()=>{
  const r=load(), source={kind:'CONTROLLED_FILE',fileId:'9223372036854775000',bodyReadContext:{intent:'APPROVAL_TASK',controlledFileId:'9223372036854775000',expectedVersionNo:'A/1',applicationType:'UPLOAD',bpmRound:'round-1',taskId:'task-1'}}, meta=metadata()
  const api=r.api(source,meta); source.fileId='7';source.bodyReadContext.taskId='other';meta.accessEventCode='event-other'
  const session=await api.start('client-1');await api.heartbeat(session.sessionId);await api.stop(session.sessionId)
  for(const req of r.calls) {
    assert.match(req.url,/controlled-files\/9223372036854775000\/reading-session/)
    assert.equal(req.headers['X-DCC-Viewer-Token'],'secret-header-token')
    assert.match(req.headers['X-DCC-Body-Read-Context'],/task-1/)
    assert.equal(req.data.accessEventCode,'event-1')
    assert.equal(JSON.stringify(req.data).includes('secret-header-token'),false)
  }
  assert.equal(r.calls[1].data.sessionId,'9223372036854775001')
})
test('unified preview has its own route; summary uses authenticated headers without a fake opened time',async()=>{
  const r=load(),api=r.api({kind:'ONLINE_FILE',fileId:'7001'},metadata())
  const sum=await api.summary();assert.equal(sum.firstOpenedAt,null);assert.equal(sum.accumulatedSeconds,0)
  assert.equal(r.calls[0].url,'/dcc/file-preview/files/7001/reading-summary')
  assert.equal(r.calls[0].headers['X-DCC-Access-Event-Code'],'event-1')
})
test('unsafe IDs and incomplete token evidence fail before HTTP',()=>{
  const r=load()
  assert.throws(()=>r.api({kind:'ONLINE_FILE',fileId:9223372036854775000},metadata()))
  assert.throws(()=>r.api({kind:'ONLINE_FILE',fileId:'7001'},{...metadata(),viewerTokenNonce:''}))
  assert.equal(r.calls.length,0)
})
