import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import { webcrypto, createHash } from 'node:crypto'
import CryptoJS from 'crypto-js'
import { createApprovalInvalidationState } from '../src/store/modules/approvalCenterInvalidationState.ts'

const source = readFileSync(new URL('../src/views/mes/pro/production-release/PqcProductionReleasePage.vue', import.meta.url), 'utf8')
const block = (start, end) => { const a = source.indexOf(start), b = source.indexOf(end, a); assert.ok(a >= 0 && b > a); return source.slice(a, b) }
const ref = value => ({ value })
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return { promise, resolve, reject } }
const timeout = () => ({ isAxiosError: true, request: {}, code: 'ECONNABORTED', message: 'timeout' })
const formalPayloadHash = (payload, actor = '7') => {
  const digest = createHash('sha256')
  for (const part of ['APPROVE', payload.applicationId, payload.pqcReleaseWorkTaskId, String(payload.expectedVersion), actor, payload.approvalOpinion ?? null, payload.udiControlDocumentNo]) {
    const data = part === null ? null : Buffer.from(part, 'utf8'), length = Buffer.alloc(4)
    length.writeInt32BE(data === null ? -1 : data.length); digest.update(length); if (data) digest.update(data)
  }
  return digest.digest('hex')
}
const harness = () => {
  let session = { key: 's1', tenantId: '1', loginUserId: '7' }, writes = 0, reads = 0, listReads = 0
  const state = createApprovalInvalidationState(); state.setSession(session.key)
  const events = [], notices = [], watchers = [], deactivated = [], unmounted = [], start = deferred()
  const row = { applicationId: '9007199254740993', pqcReleaseWorkTaskId: '9007199254740995', version: 3 }
  let lastPayload
  const receipt = () => ({
    applicationId: row.applicationId, pqcReleaseWorkTaskId: row.pqcReleaseWorkTaskId, decision: 'APPROVE', status: 'MANAGER_RELEASE_PENDING',
    signatureId: '9007199254740997', batchExecutionId: '9007199254740999', decidedBy: '7', decidedAt: '2026-10-07T12:00:00',
    version: 5, sourceSnapshotHash: 'a'.repeat(64), decisionIdempotencyKey: lastPayload.idempotencyKey,
    decisionPayloadHash: formalPayloadHash(lastPayload), udiControlDocumentNo: lastPayload.udiControlDocumentNo
  })
  const runtime = {
    Number, String, Object, Date, Map, Set, TextEncoder, Uint8Array, DataView, Array,
    CryptoJS, crypto: { getRandomValues: values => webcrypto.getRandomValues(values) }, route: { fullPath: '/pqc?applicationId=9007199254740993&workTaskId=9007199254740995' },
    approvalInvalidation: { syncSession: () => session, captureEdhrSuccess: identity => state.capture(session, identity), publishEdhrSuccess: event => { events.push(event); state.publish(event) } },
    loading: ref(false), loadError: ref(''), list: ref([]), total: ref(0), selectedRow: ref(row),
    releaseDialogVisible: ref(true), releaseSubmitting: ref(false), releaseError: ref(''), releaseResult: ref(undefined), releaseOutcomeUncertain: ref(false),
    releaseForm: { signaturePassword: 'old-password', udiControlDocumentNo: ' UDI-01 ', approvalOpinion: ' 批准意见🙂 ' },
    queryParams: { pageNo: 1, pageSize: 10, workOrderCode: '', batchCode: '' }, activeView: ref('PENDING'),
    detailVisible: ref(false), detailLoading: ref(false), detailError: ref(''), detailRow: ref(undefined), detailApplicationId: ref(undefined), detailReadOnly: ref(false), orderDetail: ref(undefined),
    watch: (getter, callback) => watchers.push({getter,callback}), onDeactivated: callback => deactivated.push(callback), onBeforeUnmount: callback => unmounted.push(callback), openPqcReleaseFromRoute: () => {},
    PQC_RELEASE_VIEW_CONCESSION_RELEASED: 'CONCESSION_RELEASED', PQC_RELEASE_VIEW_RELEASED: 'RELEASED',
    message: { success: text => notices.push(['success',text]), warning: text => notices.push(['warning',text]) }, resolveErrorMessage: e => e?.message ?? String(e),
    approvePqcProductionRelease: async payload => { writes++; lastPayload = payload; start.resolve(payload); return receipt() },
    getPqcProductionRelease: async id => { reads++; assert.equal(id, row.applicationId); return receipt() },
    getPqcProductionReleasePage: async () => { listReads++; return { list: [{ applicationId: 'new-list' }], total: 1 } }
  }
  const declarations = 'let pqcReleaseRouteGeneration = 0; let listRequestSequence = 0; let detailRequestSequence = 0;'
  const code = declarations + block('const releaseRequestOwner =', 'let listRequestSequence =') + block('const getList =', 'type PqcReleaseRouteContext') + block('const clearOrderDetail =', 'const isExactPositiveId =') +
    block('const isExactPositiveId =', 'const openCompletedPqcReleaseDetail =') + block('let releaseDialogGeneration =', 'const openNonconformanceReview =')
  runInNewContext(stripTypeScriptTypes(code + '\nObject.assign(globalThis,{submitRelease,recoverUncertainRelease,captureReleaseContext,openReleaseDialog,invalidatePqcReleasePage,pqcReleasePayloadHash,freezeReleaseRequest,getList,resetReleaseDialog,releaseRequestScopeKey,releaseIdempotencyKeys,releaseRequestStates})'), runtime)
  const navigate = mode => {
    if (mode === 'session') { session = { key: 's2', tenantId: '2', loginUserId: '8' }; state.setSession(session.key); watchers[1].callback() }
    else if (mode === 'deactivate') deactivated[0]()
    else if (mode === 'unmount') unmounted[0]()
    else if (mode === 'task') runtime.selectedRow.value = { ...row, pqcReleaseWorkTaskId: '9007199254740996' }
    else if (mode === 'application') runtime.selectedRow.value = { ...row, applicationId: '9007199254740994' }
    else if (mode === 'reopen') runtime.openReleaseDialog({ ...row })
    else { runtime.route.fullPath += '&otherTab=1'; runtime.invalidatePqcReleasePage() }
    runtime.releaseForm.signaturePassword = 'new-password'; runtime.releaseForm.udiControlDocumentNo = 'new-udi'; runtime.releaseForm.approvalOpinion = 'new-opinion'
    runtime.releaseResult.value = { applicationId: 'new-result' }; runtime.releaseSubmitting.value = true; runtime.releaseError.value = 'new-error'
    runtime.list.value = [{ applicationId: 'new-list' }]; runtime.loading.value = true
  }
  return { runtime, row, state, events, notices, receipt, start, navigate, counts: () => ({ writes,reads,listReads }) }
}

test('actual PQC payload hash matches the Java UTF8 length-frame vector', async () => {
  const h = harness()
  assert.equal(await h.runtime.pqcReleasePayloadHash(['APPROVE','9007199254740993','9007199254740995','3','7','批准意见🙂','UDI-01']), 'a916b4a5f964f94a0ba7d4e133819a30f8ed4b54cde52163f99a380edb5d93f2')
  assert.notEqual(await h.runtime.pqcReleasePayloadHash(['a',null]), await h.runtime.pqcReleasePayloadHash(['a','']))
})

test('actual HTTP PQC submit works without secure-context randomUUID or subtle', async () => {
  const h = harness()
  assert.equal(h.runtime.crypto.randomUUID, undefined)
  assert.equal(h.runtime.crypto.subtle, undefined)
  await h.runtime.submitRelease()
  const payload = await h.start.promise
  assert.match(payload.idempotencyKey, /^pqc-release-9007199254740993-\d+-[a-f0-9]{32}$/)
  assert.equal(h.counts().writes, 1)
  assert.equal(h.events.length, 1)
  assert.equal(h.runtime.releaseResult.value.decisionPayloadHash, formalPayloadHash(payload))
})

test('actual SHA256 uses length framing for empty, null, Unicode and ambiguous concatenations without any crypto API', async () => {
  const h = harness()
  h.runtime.crypto = {}
  for (const parts of [[], [''], [null], ['🙂中文', null, ''], ['ab', 'c'], ['a', 'bc']]) {
    const digest = createHash('sha256')
    for (const part of parts) {
      const bytes = part === null ? null : Buffer.from(part, 'utf8'), size = Buffer.alloc(4)
      size.writeInt32BE(bytes === null ? -1 : bytes.length)
      digest.update(size); if (bytes) digest.update(bytes)
    }
    assert.equal(await h.runtime.pqcReleasePayloadHash(parts), digest.digest('hex'))
  }
  assert.notEqual(await h.runtime.pqcReleasePayloadHash(['ab','c']), await h.runtime.pqcReleasePayloadHash(['a','bc']))
})
for (const variant of ['match','old-key','empty-key','missing-key','wrong-app','wrong-task','wrong-actor','wrong-payload','missing-payload','wrong-udi','pending','rejected','unknown','empty','read-failed']) {
  test(`actual PQC submit timeout recovery: ${variant}`, async () => {
    const h = harness(), approve = h.runtime.approvePqcProductionRelease, read = h.runtime.getPqcProductionRelease
    h.runtime.approvePqcProductionRelease = async payload => { await approve(payload); throw timeout() }
    h.runtime.getPqcProductionRelease = async id => {
      const r = await read(id)
      if (variant === 'read-failed') throw Error('get failed')
      if (variant === 'empty') return null
      if (variant === 'old-key') r.decisionIdempotencyKey = 'other-decision'
      if (variant === 'empty-key') r.decisionIdempotencyKey = ''
      if (variant === 'missing-key') delete r.decisionIdempotencyKey
      if (variant === 'wrong-app') r.applicationId = '9007199254740992'
      if (variant === 'wrong-task') r.pqcReleaseWorkTaskId = '9007199254740994'
      if (variant === 'wrong-actor') r.decidedBy = '8'
      if (variant === 'wrong-payload') r.decisionPayloadHash = 'b'.repeat(64)
      if (variant === 'missing-payload') delete r.decisionPayloadHash
      if (variant === 'wrong-udi') r.udiControlDocumentNo = 'other-udi'
      if (variant === 'pending') { r.status = 'PQC_RELEASE_PENDING'; delete r.decision }
      if (variant === 'rejected') { r.status = 'PQC_RELEASE_REJECTED'; r.decision = 'REJECT' }
      if (variant === 'unknown') r.status = 'UNKNOWN'
      return r
    }
    await h.runtime.submitRelease()
    assert.equal(h.counts().writes,1); assert.equal(h.counts().reads,1); assert.equal(h.events.length,variant === 'match' ? 1 : 0)
    const key = h.runtime.releaseRequestScopeKey(h.row)
    assert.equal(h.runtime.releaseRequestStates.get(key),variant === 'match' ? 'CONFIRMED' : 'UNCERTAIN')
    assert.equal(h.runtime.releaseIdempotencyKeys.has(key),variant !== 'match')
    assert.equal(h.runtime.releaseOutcomeUncertain.value,variant !== 'match')
    assert.equal(h.runtime.releaseSubmitting.value,false)
    h.runtime.openReleaseDialog({ ...h.row }); await h.runtime.submitRelease(); assert.equal(h.counts().writes,1)
  })
}
for (const [kind,error] of [ ['http',{...timeout(),response:{status:500}}], ['business',{code:100,message:'rejected'}], ['audit',{code:500,message:'audit failed'}], ['plain',Error('timeout')], ['string','timeout'], ['network',{...timeout(),code:'ERR_NETWORK'}], ['missing-request',{isAxiosError:true,code:'ECONNABORTED'}] ]) {
  test(`actual PQC ${kind} failure never queries a success receipt`,async () => {
    const h = harness(); let attemptedWrites = 0
    h.runtime.approvePqcProductionRelease = async payload => { attemptedWrites++; h.start.resolve(payload); throw error }
    await h.runtime.submitRelease(); assert.equal(h.counts().reads,0); assert.equal(h.events.length,0); assert.equal(h.runtime.releaseResult.value,undefined)
    const scope = h.runtime.releaseRequestScopeKey(h.row)
    assert.equal(h.runtime.releaseOutcomeUncertain.value,true)
    assert.equal(h.runtime.releaseRequestStates.has(scope),true)
    if (['http','business','audit'].includes(kind)) {
      const payload = await h.start.promise
      assert.equal(h.runtime.releaseRequestStates.get(scope),'UNCERTAIN')
      assert.equal(h.runtime.releaseIdempotencyKeys.get(scope),payload.idempotencyKey)
      await h.runtime.submitRelease()
      h.runtime.openReleaseDialog({ ...h.row })
      h.runtime.releaseForm.signaturePassword = 'retry-password'
      h.runtime.releaseForm.udiControlDocumentNo = 'UDI-01'
      h.runtime.releaseForm.approvalOpinion = '批准意见🙂'
      await h.runtime.submitRelease()
      assert.equal(attemptedWrites,1)
      assert.equal(h.runtime.releaseIdempotencyKeys.get(scope),payload.idempotencyKey)
      assert.equal(h.counts().reads,0); assert.equal(h.events.length,0)
      assert.equal(h.runtime.releaseResult.value,undefined)
    }
  })
}
for (const mode of ['route','session','task','application','reopen','deactivate','unmount']) {
  for (const outcome of ['direct-success','direct-failure','recovery-success','recovery-failure']) {
    test(`actual PQC ${outcome} after ${mode} cannot touch new UI`, async () => {
      const h = harness(), write = deferred(), readWait = deferred(), readStarted = deferred()
      const approve = h.runtime.approvePqcProductionRelease, read = h.runtime.getPqcProductionRelease
      h.runtime.approvePqcProductionRelease = async payload => { const r = await approve(payload); await write.promise; return r }
      h.runtime.getPqcProductionRelease = async id => { const r = await read(id); readStarted.resolve(); await readWait.promise; return r }
      const pending = h.runtime.submitRelease(); await h.start.promise
      if (outcome.startsWith('recovery')) { write.reject(timeout()); await readStarted.promise }
      h.navigate(mode)
      if (outcome === 'direct-success') write.resolve()
      if (outcome === 'direct-failure') write.reject({code:500,message:'old error'})
      if (outcome === 'recovery-success') readWait.resolve()
      if (outcome === 'recovery-failure') readWait.reject(Error('old GET failure'))
      await pending
      const succeeded = outcome.endsWith('success')
      assert.equal(h.events.length,succeeded ? 1 : 0); assert.equal(h.state.revision('EDHR','TODO'),succeeded && mode !== 'session' ? 1 : 0)
      assert.equal(h.runtime.releaseForm.signaturePassword,'new-password'); assert.equal(h.runtime.releaseForm.udiControlDocumentNo,'new-udi')
      assert.equal(h.runtime.releaseResult.value.applicationId,'new-result'); assert.equal(h.runtime.releaseSubmitting.value,true)
      assert.equal(h.runtime.releaseError.value,'new-error'); assert.equal(h.runtime.loading.value,true)
      assert.equal(h.runtime.list.value[0].applicationId,'new-list'); assert.equal(h.notices.length,0); assert.equal(h.counts().listReads,0)
      if (succeeded) { assert.equal(h.events[0].session.key,'s1'); assert.equal(h.events[0].identity.sourceTaskId,h.row.pqcReleaseWorkTaskId) }
    })
  }
}
test('actual PQC route switch before timeout still performs exact recovery and publishes original event', async () => {
  const h=harness(), write=deferred(), approve=h.runtime.approvePqcProductionRelease
  h.runtime.approvePqcProductionRelease=async payload=>{await approve(payload);await write.promise}
  const pending=h.runtime.submitRelease();await h.start.promise;h.navigate('route');write.reject(timeout());await pending
  assert.equal(h.counts().reads,1);assert.equal(h.events.length,1);assert.equal(h.runtime.releaseSubmitting.value,true)
})
test('actual PQC completed write plus failed list read reports success and blocks repeat writing', async()=>{
  const h=harness();h.runtime.getPqcProductionReleasePage=async()=>{throw Error('list read failed')}
  await h.runtime.submitRelease(); assert.equal(h.events.length,1);assert.equal(h.runtime.releaseResult.value.decision,'APPROVE')
  assert.match(h.runtime.releaseError.value,/已成功.*列表刷新失败/);assert.equal(h.notices[0][0],'success');assert.equal(h.notices[1][0],'warning')
  await h.runtime.submitRelease();assert.equal(h.counts().writes,1)
})
for(const outcome of ['success','failure'])test(`actual PQC old list ${outcome} cannot change new session loading/list`,async()=>{
  const h=harness(),wait=deferred();h.runtime.getPqcProductionReleasePage=()=>wait.promise
  const pending=h.runtime.getList();h.navigate('session')
  outcome==='success'?wait.resolve({list:[{applicationId:'old-list'}],total:1}):wait.reject(Error('old list error'))
  await pending;assert.equal(h.runtime.list.value[0].applicationId,'new-list');assert.equal(h.runtime.loading.value,true);assert.equal(h.runtime.loadError.value,'')
})
test('actual PQC duplicate submit and reopen during write cannot issue a second formal write',async()=>{
  const h=harness(),wait=deferred(),approve=h.runtime.approvePqcProductionRelease
  h.runtime.approvePqcProductionRelease=async payload=>{const r=await approve(payload);await wait.promise;return r}
  const pending=h.runtime.submitRelease();await h.start.promise;await h.runtime.submitRelease();h.runtime.openReleaseDialog({...h.row});await h.runtime.submitRelease()
  assert.equal(h.counts().writes,1);wait.resolve();await pending;assert.equal(h.events.length,1)
})

test('actual PQC ETIMEDOUT recovery preserves null-opinion payload identity',async()=>{
  const h=harness(),approve=h.runtime.approvePqcProductionRelease;h.runtime.releaseForm.approvalOpinion='   '
  h.runtime.approvePqcProductionRelease=async payload=>{await approve(payload);throw {...timeout(),code:'ETIMEDOUT'}}
  await h.runtime.submitRelease();assert.equal(h.events.length,1);assert.equal(h.counts().reads,1)
})
for(const field of ['applicationId','pqcReleaseWorkTaskId','version'])test(`actual PQC invalid ${field} refuses before formal write`,async()=>{
  const h=harness();h.row[field]=field==='version'?1.5:9007199254740992
  await h.runtime.submitRelease();assert.equal(h.counts().writes,0);assert.equal(h.counts().reads,0);assert.equal(h.events.length,0)
})
test('actual PQC lifecycle reset clears owned fields and revokes old dialog context',()=>{
  const h=harness(),current=h.runtime.captureReleaseContext();h.runtime.invalidatePqcReleasePage()
  assert.equal(current(),false);assert.equal(h.runtime.releaseSubmitting.value,false);assert.equal(h.runtime.releaseDialogVisible.value,false)
  assert.equal(h.runtime.releaseForm.signaturePassword,'');assert.equal(h.runtime.releaseForm.udiControlDocumentNo,'');assert.equal(h.runtime.selectedRow.value,undefined)
  assert.equal(h.runtime.detailLoading.value,false);assert.equal(h.runtime.loading.value,false)
})
for(const outcome of ['success','failure'])test(`actual PQC old refresh ${outcome} after successful write preserves new UI`,async()=>{
  const h=harness(),read=deferred(),readStarted=deferred();h.runtime.getPqcProductionReleasePage=()=>{readStarted.resolve();return read.promise}
  const pending=h.runtime.submitRelease();await readStarted.promise;const notices=h.notices.length;h.navigate('route')
  outcome==='success'?read.resolve({list:[{applicationId:'old-list'}],total:1}):read.reject(Error('old list failure'))
  await pending;assert.equal(h.events.length,1);assert.equal(h.notices.length,notices);assert.equal(h.runtime.releaseForm.signaturePassword,'new-password')
  assert.equal(h.runtime.releaseSubmitting.value,true);assert.equal(h.runtime.releaseError.value,'new-error');assert.equal(h.runtime.list.value[0].applicationId,'new-list')
})

test('actual PQC recovery helper rejects HTTP errors before reading an old decision',async()=>{
  const h=harness(),frozen=h.runtime.freezeReleaseRequest(h.row),request={...frozen,payloadHash:'a'.repeat(64)}
  await assert.rejects(h.runtime.recoverUncertainRelease(request,{...timeout(),response:{status:500}},()=>true),/恢复条件/)
  assert.equal(h.counts().reads,0);assert.equal(h.events.length,0)
})
test('actual PQC source switch during hash preparation stops before formal writer',async()=>{
  const h=harness()
  // The sole production hash is synchronous CryptoJS inside an async helper;
  // submit still yields at await and must recheck the frozen source before writing.
  const pending=h.runtime.submitRelease();h.navigate('route');await pending
  assert.equal(h.counts().writes,0);assert.equal(h.events.length,0);assert.equal(h.runtime.releaseSubmitting.value,true);assert.equal(h.runtime.releaseForm.signaturePassword,'new-password')
})
