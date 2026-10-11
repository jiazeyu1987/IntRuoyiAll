import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'
import axios from 'axios'
import { createPinia, defineStore, setActivePinia } from 'pinia'
import { effectScope, watch, computed, ref as vueRef, reactive } from 'vue'
import { createFormalActionGuard, createFormalWriteReceiptState, createApprovalInvalidationState, completeApprovalWrite, createApprovalListLoader, createApprovalBadgeLoader } from '../src/store/modules/approvalCenterInvalidationState.ts'
const read = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const withoutImports = source => {
  const ast = ts.createSourceFile('source.ts', source, ts.ScriptTarget.Latest, true)
  for (const statement of [...ast.statements].reverse()) {
    if (ts.isImportDeclaration(statement)) source = source.slice(0, statement.getStart(ast)) + source.slice(statement.end)
  }
  return source
}
const run = (source, runtime) => runInNewContext(stripTypeScriptTypes(source), runtime)
const loadApi = (file, request) => {
  const source = withoutImports(read(file)).replace(/\bexport\s+(?=(?:const|type|interface))/g, '')
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const names = ast.statements.filter(ts.isVariableStatement).flatMap(statement => statement.declarationList.declarations)
    .filter(declaration => ts.isIdentifier(declaration.name) && declaration.initializer && ts.isArrowFunction(declaration.initializer))
    .map(declaration => declaration.name.text)
  const runtime = { request, Error, FormData, Blob, console }
  run(source + `\nObject.assign(globalThis, { ${names.join(',')} })`, runtime)
  return runtime
}
const extract = (file, names) => {
  let source = read(file)
  if (file.endsWith('.vue')) source = source.slice(source.indexOf('>', source.indexOf('<script')) + 1, source.indexOf('</script>'))
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const selected = ast.statements.filter(ts.isVariableStatement).filter(statement =>
    statement.declarationList.declarations.some(declaration => ts.isIdentifier(declaration.name) && names.includes(declaration.name.text)))
  assert.equal(selected.length, names.length, `${file}: all production declarations found`)
  return selected.map(statement => statement.getText(ast)).join('\n') + `\nObject.assign(globalThis, { ${names.join(',')} })`
}
const ref = value => ({ value })
const flush = () => new Promise(setImmediate)
const network = (ports = {}) => {
  const notices = [], pending = [], all = []
  const runtime = {
    axios, Error, Promise, Response, console: { log() {}, error() {} },
    config: { result_code: 200, base_url: '/test-only', request_timeout: 30000, default_headers: 'application/json' },
    qs: { stringify: () => '' }, getAccessToken: () => '', getRefreshToken: () => '', getTenantId: () => '1', getVisitTenantId: () => '',
    removeToken() {}, setRefreshedToken() {}, deleteUserCache() {},
    ApiEncrypt: { getEncryptHeader: () => 'x-test-encryption' },
    errorCode: { default: 'business failure' }, useI18n: () => ({ t: key => key }),
    ElMessage: { error: x => notices.push(x), warning: x => notices.push(x) },
    ElNotification: { error: x => notices.push(x) }, ElMessageBox: { confirm: () => Promise.reject(Error('not authorized in test')) },
    window: { location: { href: '/test', pathname: '/test', search: '' } },
    ...ports
  }
  const source = withoutImports(read('src/config/axios/service.ts')).replace(/import\.meta\.env\.VITE_APP_TENANT_ENABLE/g, '"false"')
    .replace(/export const /g, 'const ').replace(/export \{ service \}/, 'globalThis.service = service')
  run(source, runtime)
  runtime.service.defaults.adapter = config => new Promise((resolve, reject) => {
    const item = {
      config,
      success: data => resolve({ config, data: { code: 200, data }, headers: {}, status: 200, statusText: 'OK', request: {} }),
      unauthorized: () => resolve({ config, data: { code: 401, msg: 'expired access token' }, headers: {}, status: 200, statusText: 'OK', request: {} }),
      fail: kind => {
        if (kind === 'business' || kind === 'business500' || kind === 'business901') resolve({ config, data: { code: kind === 'business901' ? 901 : kind === 'business500' ? 500 : 1001, msg: 'formal business rejection' }, headers: {}, status: 200, statusText: 'OK', request: {} })
        else {
          const message = kind === 'timeout' ? 'timeout of 30000ms exceeded' : kind === 'http' ? 'Request failed with status code 503' : 'Network Error'
          const error = new axios.AxiosError(message, kind === 'timeout' ? 'ECONNABORTED' : kind === 'http' ? 'ERR_BAD_RESPONSE' : 'ERR_NETWORK', config, {})
          if (kind === 'http') error.response = { status: 503, data: { msg: 'formal HTTP failure' }, config }
          reject(error)
        }
      }
    }
    pending.push(item); all.push(item)
  })
  const adapter = { ...runtime, service: runtime.service }
  run(withoutImports(read('src/config/axios/index.ts')).replace('export default {', 'globalThis.request = {'), adapter)
  return {
    request: adapter.request, notices, all, runtime,
    next: async () => { for (let i = 0; i < 20 && !pending.length; i++) await flush(); assert.ok(pending.length, 'production wrapper dispatched through Axios'); return pending.shift() }
  }
}
const handlerHarness = (target = 'center', n = network()) => {
  const notices = [], events = []
  let session = { key: 'session1', tenantId: '1', loginUserId: '7' }, key = 'route1:dialog1'
  const state = createApprovalInvalidationState(); state.setSession(session.key)
  const guard = createFormalActionGuard(() => JSON.stringify([session.key, key]))
  const inv = { syncSession: () => session, isCurrent: k => k === session.key,
    captureEdhrSuccess: identity => state.capture(session, identity),
    captureHandoffReceiptSuccess: (scope, type, id) => state.capture(scope, { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: type, businessSourceId: String(id) }),
    captureHandoffSourceSuccess: (type, id) => state.capture(session, { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: type, businessSourceId: String(id) }),
    publishEdhrSuccess: event => { events.push(event); state.publish(event) } }
  const runtime = { Error, Object, String, Date, Math, console, approvalInvalidation: inv,
    ElMessage: { error: x => notices.push(x), success: x => notices.push(x), warning: x => notices.push(x) },
    message: { error: x => notices.push(x), success: x => notices.push(x) },
    resolveErrorMessage: (error, text) => error?.response?.data?.msg || error?.message || text,
    ...loadApi('src/api/approval-center/index.ts', n.request),
    ...loadApi('src/api/mes/pro/edhr/approval.ts', n.request),
    ...loadApi('src/api/mes/pro/edhr/nonconformanceReview.ts', n.request),
    ...loadApi('src/api/mes/pro/edhr/release.ts', n.request),
    reviewPreview: ref({ prepareId: 'prepare-11', content: { allowedActions: ['APPROVE'], subjectVersion: 'EDHR1:test-version', contentHash: 'a'.repeat(64), contentPolicyVersion: 'EDHR_CONTENT_V1' } }), reviewIdempotencyKey: ref('review-11'), reviewPreviewError: ref(''),
    reviewSubmitting: ref(false), reviewTask: ref({ moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '11', businessKey: 'tx1', availableActions: ['APPROVE'] }),
    reviewForm: { result: 'APPROVE', reason: 'formal reason', signaturePassword: 'pw' }, reviewGeneration: 1,
    reviewDialogVisible: ref(true), canReview: () => true, approvalTaskIdentityKey: task => JSON.stringify(task),
    buildApprovalCenterRouteStateKey: () => key, completeApprovalWrite, getList: async () => true, loadError: ref(''),
    actionLoading: ref(false), actionError: ref(''), actionMode: ref('approve'), actionForm: { password: 'pw', comment: 'reason', rejectReason: 'reason' },
    detail: ref({ id: 11, executionId: 11, processInstanceId: 'bpm1', approvalSnapshotId: 12, approvalSnapshotHash: 'hash', bpmTaskId: 'task1', batchExecutionId: 4, status: 'OPEN', level: 'CRITICAL', canTransferToNcr: true }),
    workTaskId: ref('11'), actionDialogVisible: ref(true), approvalActionGuard: guard, resolveActionDisabledReason: () => '', buildSignatureTimePayload: () => ({}),
    props: { readonly: false }, selectedCritical: ref([{ id: 11, batchExecutionId: 4 }]), isCriticalOpen: () => true,
    batchNcrLoading: ref(false), batchNcrVisible: ref(true), batchNcrGuard: guard, batchNcrForm: { nonconformanceReason: 'reason', signaturePassword: 'pw', remark: '' },
    ncrCreateLoading: ref(false), ncrCreateVisible: ref(true), ncrCreateGuard: guard, ncrCreateError: ref(''), ncrCreateForm: { nonconformanceReason: 'reason', signaturePassword: 'pw', remark: '', idempotencyKey: 'request1' },
    releaseReturnSubmitting: ref(false), releaseReturnError: ref(''), releaseReturnDialogVisible: ref(true), releaseReturnGuard: guard,
    releaseReturnForm: { rejectReason: 'reason' }, workbench: ref({ releaseSummary: { releaseTransactionId: '11' } }),
    canReturnRelease: ref(true), ensureViewedReleaseStageWritable: () => true, buildReleaseIdempotencyKey: () => 'request1',
    loadDetail: async () => {}, load: async () => true
  }
  // API modules have their own UI-free VM globals; retain the handler UI ports here.
  runtime.Error = Error; runtime.ElMessage = { error: x => notices.push(x), success: x => notices.push(x), warning: x => notices.push(x) }
  const paths = {
    center: ['src/views/approval-center/index.vue', 'submitReview'],
    approve: ['src/views/mes/pro/edhr/ApprovalDetailPage.vue', 'submitAction'],
    ncrBatch: ['src/views/mes/pro/edhr-deviation/index.vue', 'submitBatchNcr'],
    ncrSingle: ['src/views/mes/pro/edhr-deviation/DeviationDetail.vue', 'submitNcrCreate'],
    releaseReturn: ['src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue', 'submitReleaseReturn']
  }
  const [file, handler] = paths[target]; run(extract(file, [handler]), runtime)
  return { n, runtime, notices, events, state, handler,
    change: kind => {
      if (kind === 'session' || kind === 'tenant') { session = { key: 'session2', tenantId: kind === 'tenant' ? '2' : '1', loginUserId: '8' }; state.setSession(session.key) }
      else if (kind === 'route') key = 'route2:dialog1'
      else { runtime.reviewGeneration++; guard.invalidate() }
    }
  }
}

for(const target of ['center','approve','releaseReturn']) for(const context of ['current','route','dialog','session','tenant']) test('formal receipt original session '+target+'/'+context,async()=>{
 const h=handlerHarness(target),r=h.runtime,p=r[h.handler](),item=await h.n.next();if(context!=='current')h.change(context);
 r.reviewForm.signaturePassword='current password';item.success(target === 'center' ? { receiptId: 'receipt-11', signatureId: 'signature-11', prepareId: 'prepare-11', idempotencyKey: 'review-11', evidenceHash: 'b'.repeat(64), resultCode: 'APPROVE', resultingStatus: 'RELEASED', committedAt: '2026-10-10T00:00:00Z' } : true);await p;
 assert.equal(h.events.length,['session','tenant'].includes(context)?0:1);assert.equal(h.state.revision('EDHR','TODO'),['session','tenant'].includes(context)?0:1);
 if(context!=='current'){assert.equal(h.notices.length,0);assert.equal(r.reviewForm.signaturePassword,'current password')}
});
const listHarness=()=>{const h=handlerHarness(),r=h.runtime;Object.assign(r,{createApprovalListLoader,queryParams:{pageNo:1,pageSize:10,viewType:'TODO',moduleCode:'EDHR',keyword:''},approvalTodoBadgeStore:{refreshTodoTotal:async()=>{}},list:ref([]),total:ref(0),loading:ref(false),listUiGeneration:0,isUnfilteredTodoQuery:()=>false});r.approvalInvalidation.revision=()=>h.state.revision('EDHR','TODO');run(extract('src/views/approval-center/index.vue',['approvalListLoader','getList']),r);return h};
test('actual same semantic getList shares Axios validity',async()=>{const h=listHarness(),r=h.runtime,a=r.getList(),b=r.getList(),item=await h.n.next();assert.equal(h.n.all.length,1);assert.equal(item.config.isRequestContextCurrent(),true);item.success({list:['fresh'],total:1});assert.deepEqual(await Promise.all([a,b]),[true,true]);assert.deepEqual(r.list.value,['fresh']);assert.equal(h.notices.length,0)});
for(const changed of ['route','session','query'])test('actual getList invalidates '+changed,async()=>{const h=listHarness(),r=h.runtime,p=r.getList(),item=await h.n.next();if(changed==='query')r.queryParams.keyword='new';else h.change(changed);item.success({list:['old'],total:1});assert.equal(await p,false);assert.equal(r.list.value.length,0);assert.equal(h.notices.length,0)});
test('actual getList revision changed in flight remains dirty and rereads',async()=>{const h=listHarness(),r=h.runtime,p=r.getList(),item=await h.n.next();h.state.publish(r.approvalInvalidation.captureEdhrSuccess({sourceTaskType:'EDHR_WORK_TASK',sourceTaskId:'11'}));item.success({list:['first'],total:1});await p;assert.equal(r.approvalListLoader.isDirty(),true);const q=r.getList();(await h.n.next()).success({list:['second'],total:1});await q;assert.equal(r.approvalListLoader.isDirty(),false);assert.equal(h.n.all.length,2)});
const feedbackApi=n=>{const runtime={request:n.request};run(withoutImports(read('src/api/mes/pro/feedback/index.ts')).replace(/export /g,'')+'\nglobalThis.api = ProFeedbackApi',runtime);return runtime.api};
for(const name of ['frontlineSubmit','submitFrontlinePqcInspection','getFrontlinePqcSubmitReceipt'])for(const kind of ['unauthorized','business','http','timeout','success'])test('actual frontline wrapper late '+name+'/'+kind,async()=>{const effects=[],n=network({removeToken:()=>effects.push('remove'),deleteUserCache:()=>effects.push('delete'),ElMessageBox:{confirm:()=>{effects.push('confirm');return Promise.resolve()}}}),api=feedbackApi(n);let current=true;const p=api[name]({}, {isRequestContextCurrent:()=>current}),rejected=assert.rejects(p,{code:'REQUEST_CONTEXT_EXPIRED'}),item=await n.next();current=false;if(kind==='success')item.success(true);else if(kind==='unauthorized')item.unauthorized();else item.fail(kind);await rejected;assert.deepEqual(effects,[]);assert.equal(n.notices.length,0)});
const correctionHarness=(mode='production')=>{const authEffects=[];const h=handlerHarness('center',network({removeToken:()=>authEffects.push('remove'),deleteUserCache:()=>authEffects.push('delete'),ElMessageBox:{confirm:()=>{authEffects.push('confirm');return Promise.resolve()}}})),r=h.runtime;Object.assign(r,{...loadApi('src/api/mes/pro/processpool/frontlineReturnCorrection.ts',h.n.request),computed:f=>({get value(){return f()}}),ref,props:{mode},checkPermi:()=>true,router:r.router||{currentRoute:{value:{fullPath:'route1'}}},watch:()=>{},onBeforeUnmount:()=>{},onDeactivated:()=>{},defineProps:()=>({mode}),defineEmits:()=>()=>{},defineExpose:()=>{}});let s=read('src/views/mes/pro/feedback/FrontlineReturnCorrectionPanel.vue');s=s.slice(s.indexOf('>',s.indexOf('<script'))+1,s.indexOf('</script>'));s=withoutImports(s)+'\nObject.assign(globalThis,{open,choose,resubmit,resetContext,visible,busy,draft,result,rows,password,errorText,reason,seed:()=>{originalValues="old"}})';r.useApprovalCenterInvalidationStore=()=>r.approvalInvalidation;r.useRouter=()=>r.router;run(s,r);return {...h,authEffects}};
for(const mode of ['production','pqc'])for(const action of ['open','choose','resubmit'])for(const context of ['current','session','route','close','switch'])for(const kind of ['success','business','http','timeout','unauthorized'])test('actual correction '+mode+'/'+action+'/'+context+'/'+kind,async()=>{const h=correctionHarness(mode),r=h.runtime,row={returnTaskId:'11',eventId:12,activeOrderId:13,rejectedReviewId:14,expectedRevisionId:15},detail={row,...(mode==='production'?{production:{outputQuantity:2,materialDetails:[],lossDetails:[],deviceParameterReadings:[]}}:{pqc:{actualInspectionQuantity:2,scrapQuantity:0,items:[]}})};r.visible.value=true;r.draft.value=detail;r.password.value='pw';r.reason.value='reason';r.seed();const p=r[action](row),item=await h.n.next();if(context==='session')h.change('session');if(context==='route')r.router.currentRoute.value.fullPath='new';if(['close','switch'].includes(context))r.resetContext();if(context!=='current'){r.password.value='new password';r.errorText.value='new error';r.busy.value=true;r.draft.value={newDraft:true}};if(kind==='success')item.success(action==='open'?[row]:action==='choose'?detail:{eventId:12,revisionId:16,changes:[{fieldName:'outputQuantity'}]});else if(kind==='unauthorized')item.unauthorized();else item.fail(kind);await p;if(context!=='current'){assert.equal(r.password.value,'new password');assert.equal(r.errorText.value,'new error');assert.equal(r.busy.value,true);assert.equal(r.draft.value.newDraft,true);assert.deepEqual(h.authEffects,[])}else if(kind!=='success'){assert.ok(r.errorText.value);assert.equal(r.busy.value,false)}assert.equal(h.n.notices.length,0);assert.equal(h.events.length,action==='resubmit'&&kind==='success'&&context!=='session'?1:0)});

const frontlineHarness=(kind)=>{const h=handlerHarness('center',network({ElMessageBox:{confirm:()=>Promise.resolve()}})),r=h.runtime;Object.assign(r,{ProFeedbackApi:feedbackApi(h.n),hasReturnNotification:ref(false),payloadLoading:ref(false),productionSubmitReadPending:ref(false),productionSubmitReceipts:createFormalWriteReceiptState(),submitConfirmationOpen:ref(false),productionSubmitSuccessOpen:ref(false),productionActionInProgress:ref(false),productionSubmitContextKey:()=> 'original-key',productionSubmitGuard:{capture:()=>{const key=r.approvalInvalidation.syncSession().key;const route=r.buildApprovalCenterRouteStateKey();return ()=>r.approvalInvalidation.isCurrent(key)&&route===r.buildApprovalCenterRouteStateKey()}},assertProductionSubmissionReady:()=>[],draft:{fieldValues:{}},buildProductionFieldValues:()=>({}),assertFormalPayloadContext:()=>{},buildFrontlineTemplatePayload:()=>({}),context:{},requestProductionFormalSubmitConfirmation:async()=>true,buildProductionFormalSubmitConfirmation:()=>({}),buildFrontlineFormalSubmitPayload:()=>({processPoolContext:{activeOrderId:13}}),productionSignaturePassword:ref('pw'),productionSubmitReceiptRevision:ref(0),rememberProductionMaterialDeviceDrafts:()=>{},resetProductionSubmissionDraft:()=>{},openProductionSubmitSuccessDialog:()=>h.notices.push('success'),showParameterAuditWarning:()=>{},clearFrontlineError:()=>{},refreshProductionSelectedProcessSubmittedQuantity:async()=>true,showFrontlineError:e=>h.notices.push(String(e)),isInitialHandoffBlocked:ref(false),pqcSubmitResultUncertain:ref(false),pqcSignaturePassword:ref('pw'),pqcSubmitGuard:{capture:()=>{const key=r.approvalInvalidation.syncSession().key;const route=r.buildApprovalCenterRouteStateKey();return ()=>r.approvalInvalidation.isCurrent(key)&&route===r.buildApprovalCenterRouteStateKey()}},assertPqcFormalSubmissionReady:()=>{},assertPqcSignatureAndQuantityReady:()=>{},assertPqcCurrentProcessAllMethodSubmissionReady:()=>{},cloneDeep:x=>structuredClone(x),buildPqcInspectionSubmitPayloads:()=>[{pqcTaskId:14}],pqcMethodKey:(key,p)=>key+':'+p.pqcTaskId,pqcMethodWrites:new Set(),pqcUncertainMethods:new Set(),pqcMethodReceipts:new Map(),pqcActionInProgress:ref(false),recoverPqcSubmitReceiptAfterUncertainError:async()=>false,resetPqcSubmissionDrafts:()=>{}});run(extract('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue',[kind==='production'?'handleProductionFormalSubmit':'handleConfirmPqcSubmit']),r);return h};
for(const mode of ['production','pqc'])for(const context of ['current','route','session'])for(const kind of ['success','business','http','unauthorized'])test('actual frontline handler wrapper Axios '+mode+'/'+context+'/'+kind,async()=>{const h=frontlineHarness(mode),r=h.runtime,p=r[mode==='production'?'handleProductionFormalSubmit':'handleConfirmPqcSubmit'](),item=await h.n.next();if(context!=='current')h.change(context);if(kind==='success')item.success({pqcEventId:16});else if(kind==='unauthorized')item.unauthorized();else item.fail(kind);await p;assert.equal(h.events.length,kind==='success'&&context!=='session'?1:0);assert.equal(h.n.notices.length,0);if(context!=='current')assert.equal(h.notices.length,0)});

for (const stale of [false, true]) test(`actual frontlineSubmit refresh await context recheck and queued replay: stale=${stale}`, async () => {
  let finishRefresh, current = true
  const effects = [], refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'original-refresh', getAccessToken: () => 'original-access',
    setRefreshedToken: tokens => effects.push(['set-token', tokens.accessToken]),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: async () => { effects.push(['refresh']); return refreshResult } } })
  const api = feedbackApi(n), options = { ignoreErrorMessage: true, isRequestContextCurrent: () => current }
  const first = api.frontlineSubmit({}, options), second = api.frontlineSubmit({}, options)
  const firstResult = first.then(value => ({ value }), error => ({ error })), secondResult = second.then(value => ({ value }), error => ({ error }))
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  if (stale) current = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'refreshed-access', refreshToken: 'refreshed-refresh' } } })
  if (!stale) { (await n.next()).success(true); (await n.next()).success(true) }
  const results = await Promise.all([firstResult, secondResult])
  if (stale) { assert.ok(results.every(x => x.error?.code === 'REQUEST_CONTEXT_EXPIRED')); assert.deepEqual(effects, [['refresh']]); assert.equal(n.all.length, 2) }
  else { assert.ok(results.every(x => x.value === true)); assert.deepEqual(effects, [['refresh'], ['set-token', 'refreshed-access']]); assert.equal(n.all.length, 4) }
  assert.equal(n.notices.length, 0)
})

test('actual frontlineSubmit refresh failure after context switch cannot trigger logout or replay', async () => {
  let failRefresh, current = true
  const effects = [], refreshResult = new Promise((_resolve, reject) => { failRefresh = reject })
  const n = network({ getRefreshToken: () => 'refresh', ElMessageBox: { confirm: () => { effects.push('confirm'); return Promise.resolve() } },
    removeToken: () => effects.push('remove-token'), setRefreshedToken: () => effects.push('set-token'),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = feedbackApi(n)
  const promise = api.frontlineSubmit({}, { isRequestContextCurrent: () => current }), rejection = assert.rejects(promise, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); current = false; failRefresh(Error('refresh failed')); await rejection
  assert.deepEqual(effects, []); assert.equal(n.all.length, 1)
})

test('actual frontlineSubmit stale queued request rejects while current refresh owner replays normally', async () => {
  let finishRefresh, queuedCurrent = true
  const refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'refresh', getAccessToken: () => 'access',
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = feedbackApi(n)
  const owner = api.frontlineSubmit({}, { isRequestContextCurrent: () => true })
  const queued = api.frontlineSubmit({}, { isRequestContextCurrent: () => queuedCurrent })
  const queuedRejection = assert.rejects(queued, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  queuedCurrent = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } } })
  ;(await n.next()).success(true); assert.equal(await owner, true); await queuedRejection
  assert.equal(n.all.length, 3); assert.equal(n.notices.length, 0)
})


for (const stale of [false, true]) test(`actual submitFrontlinePqcInspection refresh await context recheck and queued replay: stale=${stale}`, async () => {
  let finishRefresh, current = true
  const effects = [], refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'original-refresh', getAccessToken: () => 'original-access',
    setRefreshedToken: tokens => effects.push(['set-token', tokens.accessToken]),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: async () => { effects.push(['refresh']); return refreshResult } } })
  const api = feedbackApi(n), options = { ignoreErrorMessage: true, isRequestContextCurrent: () => current }
  const first = api.submitFrontlinePqcInspection({}, options), second = api.submitFrontlinePqcInspection({}, options)
  const firstResult = first.then(value => ({ value }), error => ({ error })), secondResult = second.then(value => ({ value }), error => ({ error }))
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  if (stale) current = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'refreshed-access', refreshToken: 'refreshed-refresh' } } })
  if (!stale) { (await n.next()).success(true); (await n.next()).success(true) }
  const results = await Promise.all([firstResult, secondResult])
  if (stale) { assert.ok(results.every(x => x.error?.code === 'REQUEST_CONTEXT_EXPIRED')); assert.deepEqual(effects, [['refresh']]); assert.equal(n.all.length, 2) }
  else { assert.ok(results.every(x => x.value === true)); assert.deepEqual(effects, [['refresh'], ['set-token', 'refreshed-access']]); assert.equal(n.all.length, 4) }
  assert.equal(n.notices.length, 0)
})

test('actual submitFrontlinePqcInspection refresh failure after context switch cannot trigger logout or replay', async () => {
  let failRefresh, current = true
  const effects = [], refreshResult = new Promise((_resolve, reject) => { failRefresh = reject })
  const n = network({ getRefreshToken: () => 'refresh', ElMessageBox: { confirm: () => { effects.push('confirm'); return Promise.resolve() } },
    removeToken: () => effects.push('remove-token'), setRefreshedToken: () => effects.push('set-token'),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = feedbackApi(n)
  const promise = api.submitFrontlinePqcInspection({}, { isRequestContextCurrent: () => current }), rejection = assert.rejects(promise, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); current = false; failRefresh(Error('refresh failed')); await rejection
  assert.deepEqual(effects, []); assert.equal(n.all.length, 1)
})

test('actual submitFrontlinePqcInspection stale queued request rejects while current refresh owner replays normally', async () => {
  let finishRefresh, queuedCurrent = true
  const refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'refresh', getAccessToken: () => 'access',
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = feedbackApi(n)
  const owner = api.submitFrontlinePqcInspection({}, { isRequestContextCurrent: () => true })
  const queued = api.submitFrontlinePqcInspection({}, { isRequestContextCurrent: () => queuedCurrent })
  const queuedRejection = assert.rejects(queued, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  queuedCurrent = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } } })
  ;(await n.next()).success(true); assert.equal(await owner, true); await queuedRejection
  assert.equal(n.all.length, 3); assert.equal(n.notices.length, 0)
})


for (const stale of [false, true]) test(`actual getFrontlinePqcSubmitReceipt refresh await context recheck and queued replay: stale=${stale}`, async () => {
  let finishRefresh, current = true
  const effects = [], refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'original-refresh', getAccessToken: () => 'original-access',
    setRefreshedToken: tokens => effects.push(['set-token', tokens.accessToken]),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: async () => { effects.push(['refresh']); return refreshResult } } })
  const api = feedbackApi(n), options = { ignoreErrorMessage: true, isRequestContextCurrent: () => current }
  const first = api.getFrontlinePqcSubmitReceipt({}, options), second = api.getFrontlinePqcSubmitReceipt({}, options)
  const firstResult = first.then(value => ({ value }), error => ({ error })), secondResult = second.then(value => ({ value }), error => ({ error }))
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  if (stale) current = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'refreshed-access', refreshToken: 'refreshed-refresh' } } })
  if (!stale) { (await n.next()).success(true); (await n.next()).success(true) }
  const results = await Promise.all([firstResult, secondResult])
  if (stale) { assert.ok(results.every(x => x.error?.code === 'REQUEST_CONTEXT_EXPIRED')); assert.deepEqual(effects, [['refresh']]); assert.equal(n.all.length, 2) }
  else { assert.ok(results.every(x => x.value === true)); assert.deepEqual(effects, [['refresh'], ['set-token', 'refreshed-access']]); assert.equal(n.all.length, 4) }
  assert.equal(n.notices.length, 0)
})

test('actual getFrontlinePqcSubmitReceipt refresh failure after context switch cannot trigger logout or replay', async () => {
  let failRefresh, current = true
  const effects = [], refreshResult = new Promise((_resolve, reject) => { failRefresh = reject })
  const n = network({ getRefreshToken: () => 'refresh', ElMessageBox: { confirm: () => { effects.push('confirm'); return Promise.resolve() } },
    removeToken: () => effects.push('remove-token'), setRefreshedToken: () => effects.push('set-token'),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = feedbackApi(n)
  const promise = api.getFrontlinePqcSubmitReceipt({}, { isRequestContextCurrent: () => current }), rejection = assert.rejects(promise, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); current = false; failRefresh(Error('refresh failed')); await rejection
  assert.deepEqual(effects, []); assert.equal(n.all.length, 1)
})

test('actual getFrontlinePqcSubmitReceipt stale queued request rejects while current refresh owner replays normally', async () => {
  let finishRefresh, queuedCurrent = true
  const refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'refresh', getAccessToken: () => 'access',
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = feedbackApi(n)
  const owner = api.getFrontlinePqcSubmitReceipt({}, { isRequestContextCurrent: () => true })
  const queued = api.getFrontlinePqcSubmitReceipt({}, { isRequestContextCurrent: () => queuedCurrent })
  const queuedRejection = assert.rejects(queued, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  queuedCurrent = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } } })
  ;(await n.next()).success(true); assert.equal(await owner, true); await queuedRejection
  assert.equal(n.all.length, 3); assert.equal(n.notices.length, 0)
})


for(const context of ['current','route','session'])test('formal uncertain center receipt cannot publish '+context,async()=>{const h=handlerHarness(),p=h.runtime.submitReview(),item=await h.n.next();if(context!=='current')h.change(context);item.success(false);await p;assert.equal(h.events.length,0)});

// R6: execute production Pagination, center handlers, real Pinia store and installed Axios.
const paginationListHarness = () => {
  const authEffects = [], h = handlerHarness('center', network({
    removeToken: () => authEffects.push('remove-token'), deleteUserCache: () => authEffects.push('delete-user'),
    ElMessageBox: { confirm: () => { authEffects.push('confirm'); return Promise.reject(Error('controlled cancellation')) } }
  })), r = h.runtime
  const pinia = createPinia(), user = reactive({ getUser: { id: 7 } })
  const storeRuntime = { computed, ref: vueRef, watch, defineStore, createApprovalInvalidationState,
    useUserStore: () => user, getAccessToken: () => 'test-session-present', getTenantId: () => '1', getVisitTenantId: () => '' }
  run(withoutImports(read('src/store/modules/approvalCenterInvalidation.ts')).replace(/export const /g, 'const ') + '\nglobalThis.useStore = useApprovalCenterInvalidationStore', storeRuntime)
  Object.assign(r, { approvalInvalidation: storeRuntime.useStore(pinia), createApprovalListLoader,
    queryParams: reactive({ pageNo: 1, pageSize: 10, viewType: 'TODO', moduleCode: 'EDHR', keyword: '' }),
    route: reactive({ name: 'ApprovalCenter', path: '/approval-center', query: {} }), approvalCenterRouteInstanceName: 'ApprovalCenter',
    approvalTodoBadgeStore: { refreshTodoTotal: async () => {} }, list: vueRef(['retained']), total: vueRef(100),
    loading: vueRef(false), loadError: vueRef(''), listUiGeneration: 0, isUnfilteredTodoQuery: () => false })
  run(extract('src/views/approval-center/index.vue', ['normalizeRouteQueryValue', 'buildApprovalCenterRouteStateKey', 'approvalListLoader', 'getList', 'handlePagination']), r)
  assert.match(read('src/views/approval-center/index.vue'), /<UnifiedListTemplate[\s\S]*?@pagination="handlePagination"[\s\S]*?>/)
  assert.match(read('src/views/approval-center/index.vue'), /v-loading="loading"/)
  assert.match(read('src/components/Pagination/index.vue'), /@current-change="handleCurrentChange"/)
  assert.match(read('src/components/Pagination/index.vue'), /@size-change="handleSizeChange"/)
  const forwarding = Object.fromEntries(['update:page', 'update:limit', 'pagination'].map(name => {
    const expression = read('src/components/UnifiedListTemplate/index.vue').match(new RegExp('@' + name + '="([^"]+)"'))?.[1]
    assert.ok(expression, 'actual UnifiedListTemplate forwarding exists: ' + name)
    return [name, expression]
  }))
  let paginationPromise
  const child = { computed, props: { get page() { return r.queryParams.pageNo }, get limit() { return r.queryParams.pageSize }, get total() { return r.total.value } },
    saveRememberedPageSize: () => {}, emit: (name, payload) => run(forwarding[name], { $event: payload, $emit: (name, payload) => {
      if (name === 'update:page') r.queryParams.pageNo = payload
      if (name === 'update:limit') r.queryParams.pageSize = payload
      if (name === 'pagination') paginationPromise = r.handlePagination(payload)
    } }) }
  run(extract('src/components/Pagination/index.vue', ['currentPage', 'pageSize', 'handleCurrentChange', 'handleSizeChange']), child)
  return { ...h, authEffects, user, dispose: () => r.approvalInvalidation.$dispose(),
    page: value => { child.currentPage.value = value; child.handleCurrentChange(value); return paginationPromise },
    size: value => { child.pageSize.value = value; child.handleSizeChange(value); return paginationPromise } }
}
const settleList = (item, kind, label = 'fresh') => {
  if (kind === 'success') item.success({ list: [label], total: 100 })
  else if (kind === 'unauthorized') item.unauthorized()
  else item.fail(kind)
}
const assertPendingList = h => {
  assert.equal(h.runtime.loading.value, true, 'older completion must leave current loading active')
  assert.equal(h.runtime.loadError.value, '')
  assert.deepEqual([...h.runtime.list.value], ['retained'])
  assert.equal(h.runtime.total.value, 100)
  assert.deepEqual(h.notices, []); assert.deepEqual(h.n.notices, []); assert.deepEqual(h.authEffects, [])
}
for (const change of ['page', 'size', 'queryABA', 'route', 'session'])
  for (const oldKind of ['success', 'business', 'unauthorized'])
    for (const currentKind of ['success', 'business'])
      test('R6 production Pagination/Pinia/Axios current UI ownership ' + change + '/' + oldKind + '/' + currentKind, async () => {
        const h = paginationListHarness(), r = h.runtime
        try {
          const old = h.page(1), oldItem = await h.n.next()
          let middle, middleItem, current
          if (change === 'page') current = h.page(2)
          if (change === 'size') current = h.size(20)
          if (change === 'queryABA') {
            r.queryParams.keyword = 'B'; middle = h.page(1); middleItem = await h.n.next()
            r.queryParams.keyword = ''; current = h.page(1)
          }
          if (change === 'route') { r.route.query.keyword = 'new-route'; current = h.page(2) }
          if (change === 'session') { h.user.getUser.id = 8; current = h.page(2) }
          const currentItem = await h.n.next()
          assert.equal(currentItem.config.params.pageNo, change === 'page' || change === 'route' || change === 'session' ? 2 : 1)
          assert.equal(currentItem.config.params.pageSize, change === 'size' ? 20 : 10)
          assert.equal(oldItem.config.isRequestContextCurrent(), false)
          assert.equal(currentItem.config.isRequestContextCurrent(), true)
          assertPendingList(h)
          settleList(oldItem, oldKind, 'old'); await old; assertPendingList(h)
          if (middle) { settleList(middleItem, oldKind, 'middle'); await middle; assertPendingList(h) }
          settleList(currentItem, currentKind); await current
          assert.equal(r.loading.value, false)
          assert.deepEqual(h.authEffects, []); assert.deepEqual(h.n.notices, [])
          if (currentKind === 'success') {
            assert.deepEqual([...r.list.value], ['fresh']); assert.equal(r.total.value, 100)
            assert.equal(r.loadError.value, ''); assert.deepEqual(h.notices, [])
          } else {
            assert.equal(r.list.value.length, 0); assert.equal(r.total.value, 0)
            assert.ok(r.loadError.value); assert.equal(h.notices.length, 1)
            assert.equal(h.notices[0], r.loadError.value)
          }
        } finally { h.dispose() }
      })
for (const kind of ['success', 'business']) test('R6 same semantic real Pagination subscriptions retain shared Axios ' + kind, async () => {
  const h = paginationListHarness(), r = h.runtime
  try {
    const first = h.page(1), item = await h.n.next(), firstGeneration = r.listUiGeneration
    const second = h.page(1)
    assert.ok(r.listUiGeneration > firstGeneration, 'new UI owner is independent of shared request owner')
    assert.equal(h.n.all.length, 1); assert.equal(item.config.isRequestContextCurrent(), true)
    assertPendingList(h); settleList(item, kind); await Promise.all([first, second])
    assert.equal(r.loading.value, false); assert.deepEqual(h.authEffects, []); assert.deepEqual(h.n.notices, [])
    if (kind === 'success') { assert.deepEqual([...r.list.value], ['fresh']); assert.equal(r.loadError.value, ''); assert.deepEqual(h.notices, []) }
    else { assert.equal(r.list.value.length, 0); assert.ok(r.loadError.value); assert.equal(h.notices.length, 1) }
  } finally { h.dispose() }
})
for (const oldKind of ['success', 'business', 'unauthorized']) test('R6 old Pagination finishes after current visible error ' + oldKind, async () => {
  const h = paginationListHarness(), r = h.runtime
  try {
    const old = h.page(1), oldItem = await h.n.next(), current = h.page(2), currentItem = await h.n.next()
    settleList(currentItem, 'business'); await current
    const error = r.loadError.value, notices = [...h.notices]
    assert.ok(error); assert.equal(r.loading.value, false)
    settleList(oldItem, oldKind, 'old'); await old
    assert.equal(r.loadError.value, error); assert.deepEqual(h.notices, notices)
    assert.equal(r.list.value.length, 0); assert.equal(r.total.value, 0); assert.equal(r.loading.value, false)
    assert.deepEqual(h.authEffects, []); assert.deepEqual(h.n.notices, [])
  } finally { h.dispose() }
})

// R7 additions only. The original 39946-byte test prefix remains intact.
const r7Vue = await import('vue')
const r7CryptoJS = (await import('crypto-js')).default
const { createHash: r7CreateHash } = await import('node:crypto')
const r7Deferred = () => {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}
const r7Script = file => {
  const s = read(file)
  return s.slice(s.indexOf('>', s.indexOf('<script')) + 1, s.indexOf('</script>'))
}
const r7Host = () => {
  const element = type => ({ type, children: [], parent: null })
  const renderer = r7Vue.createRenderer({
    createElement: element, createText: text => ({ text }), createComment: text => ({ text }),
    insert: (node, parent, anchor = null) => {
      if (node.parent) { const i = node.parent.children.indexOf(node); if (i >= 0) node.parent.children.splice(i, 1) }
      const i = anchor ? parent.children.indexOf(anchor) : -1
      if (i < 0) parent.children.push(node); else parent.children.splice(i, 0, node)
      node.parent = parent
    },
    remove: node => { const i = node.parent?.children.indexOf(node) ?? -1; if (i >= 0) node.parent.children.splice(i, 1); node.parent = null },
    setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text },
    parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null,
    patchProp: (node, key, _old, value) => { node[key] = value }
  })
  return { renderer, root: element('root') }
}
const r7Body = item => typeof item.config.data === 'string' ? JSON.parse(item.config.data) : item.config.data
const r7BackendHash = data => {
  const action = data.rejectReason !== undefined ? 'REJECT' : 'APPROVE'
  const fields = [action, data.applicationId, data.pqcReleaseWorkTaskId, String(data.expectedVersion), '7',
    action === 'APPROVE' ? data.approvalOpinion ?? null : data.rejectReason,
    action === 'APPROVE' ? data.udiControlDocumentNo : null]
  const hash = r7CreateHash('sha256')
  for (const field of fields) {
    const encoded = field === null ? null : Buffer.from(field, 'utf8'), size = Buffer.alloc(4)
    size.writeInt32BE(encoded === null ? -1 : encoded.length)
    hash.update(size); if (encoded) hash.update(encoded)
  }
  return hash.digest('hex')
}
const r7DecisionReceipt = item => {
  const data = r7Body(item), reject = data.rejectReason !== undefined
  return {
    applicationId: data.applicationId, pqcReleaseWorkTaskId: data.pqcReleaseWorkTaskId,
    decision: reject ? 'REJECT' : 'APPROVE', status: reject ? 'PQC_RELEASE_REJECTED' : 'REPORT_UPLOAD_PENDING',
    version: data.expectedVersion + 1, decidedBy: '7', decisionIdempotencyKey: data.idempotencyKey,
    decisionPayloadHash: r7BackendHash(data), udiControlDocumentNo: data.udiControlDocumentNo,
    rejectReason: data.rejectReason, batchExecutionId: '999',
    reportUploadTasks: reject ? [] : [1, 2, 3, 4].map(i => ({ nodeType: 'report' + i, batchTaskId: String(100 + i), workTaskId: String(200 + i) }))
  }
}
const r7Harness = (branch, networkPorts = {}, validation = null, sharedNetwork = null) => {
  const authEffects = [], confirmation = r7Deferred(), ui = [], acceptedEvents = [], publishAttempts = []
  const n = sharedNetwork || network({
    removeToken: () => authEffects.push('removeToken'), deleteUserCache: () => authEffects.push('deleteCache'),
    setRefreshedToken: () => authEffects.push('setToken'),
    ElMessageBox: { confirm: () => { authEffects.push('confirm'); return confirmation.promise } }, ...networkPorts
  })
  const identity = reactive({ tenant: '1', logged: true, user: '7' })
  const user = reactive({ getUser: { id: '7' } })
  const storePorts = { computed, ref: vueRef, watch, defineStore, createApprovalInvalidationState,
    getAccessToken: () => identity.logged ? 'test-only-access' : '', getTenantId: () => identity.tenant,
    getVisitTenantId: () => '', useUserStore: () => user }
  setActivePinia(createPinia())
  run(withoutImports(read('src/store/modules/approvalCenterInvalidation.ts')).replace('export const ', 'const ') +
    '\nglobalThis.factory = useApprovalCenterInvalidationStore', storePorts)
  const invalidation = storePorts.factory()
  const publish = invalidation.publishEdhrSuccess
  invalidation.publishEdhrSuccess = event => {
    const previous = invalidation.revision('EDHR', 'TODO')
    publishAttempts.push(event); publish(event)
    if (invalidation.revision('EDHR', 'TODO') > previous) acceptedEvents.push(event)
  }
  const route = reactive({ fullPath: '/original' }), task = {
    id: '90071992547409931', taskType: 'PQC_PRODUCTION_RELEASE', businessScopeType: 'PRODUCTION_RELEASE',
    businessScopeId: '90071992547409932', version: 3, batchExecutionId: '', executionId: '901', sourceExecutionId: '902', routeProcessId: '903'
  }
  const pendingReceipt = { applicationId: task.businessScopeId, pqcReleaseWorkTaskId: task.id, status: 'PQC_RELEASE_PENDING', version: 3 }
  const r = {
    Error, Object, String, Number, Date, Math, console, Promise, TextEncoder, Uint8Array, DataView,
    crypto: globalThis.crypto, CryptoJS: r7CryptoJS,
    ref: vueRef, reactive, watch, computed, onBeforeUnmount: r7Vue.onBeforeUnmount, onDeactivated: r7Vue.onDeactivated,
    createFormalActionGuard, createFormalWriteReceiptState, approvalInvalidation: invalidation, route,
    message: { error: x => ui.push(['error', String(x)]), success: x => ui.push(['success', String(x)]), warning: x => ui.push(['warning', String(x)]) },
    resolveErrorMessage: (error, text) => error?.response?.data?.msg || error?.message || text,
    loading: vueRef(false), loadError: vueRef(''), list: vueRef([]), total: vueRef(0),
    activeTab: vueRef('todo'), stats: reactive({ todoCount: 0 }), buildQuery: () => ({ pageNo: 1, pageSize: 10 }), resolveMyPageStatus: () => 'TODO',
    ...loadApi('src/api/mes/pro/edhr/workTask.ts', n.request),
    ...loadApi('src/api/mes/pro/edhr/nonconformanceReview.ts', n.request),
    ...loadApi('src/api/mes/pro/productionRelease/index.ts', n.request),
    ProFeedbackApi: feedbackApi(n),
    deviceState: reactive({ selectedActiveOrder: { activeOrderId: 13 }, selectedProcess: { activeOrderId: 13, routeId: 12, routeProcessId: 14, processId: 15 },
      selectedEmployee: { userId: 7, systemUserId: 7 }, loadingProcesses: false, productionProcessOptions: [], processOptions: [], lastError: undefined }),
    payloadLoading: vueRef(false), productionSubmitSuccessOpen: vueRef(false), submitConfirmationOpen: vueRef(false), productionSignaturePassword: vueRef('test-only'),
    hasReturnNotification: vueRef(false), draft: { fieldValues: {} }, context: {},
    assertProductionSubmissionReady: () => [], buildProductionFieldValues: () => ({}), assertFormalPayloadContext: () => {},
    buildFrontlineTemplatePayload: () => ({}), requestProductionFormalSubmitConfirmation: async () => true,
    buildProductionFormalSubmitConfirmation: () => ({}), buildFrontlineFormalSubmitPayload: () => ({ processPoolContext: { activeOrderId: 13 } }),
    rememberProductionMaterialDeviceDrafts: () => {}, resetProductionSubmissionDraft: () => {}, showParameterAuditWarning: () => {}, clearFrontlineError: () => {},
    showFrontlineError: error => ui.push(['error', String(error)]), resolveProductionFormalSubmitConfirmation: () => { r.submitConfirmationOpen.value = false },
    rejectLoading: vueRef(false), rejectError: vueRef(''), rejectDialogVisible: vueRef(false), selectedRejectBatch: vueRef(null),
    rejectForm: reactive({ nonconformanceReason: 'formal reason', signaturePassword: '' }), batchListReadGeneration: 0,
    pqcDecisionDialogVisible: vueRef(false), pqcDecisionLoading: vueRef(false), pqcDecisionSubmitting: vueRef(false),
    pqcDecisionAction: vueRef('APPROVE'), pqcDecisionTask: vueRef(null), pqcDecisionReceipt: vueRef(null),
    pqcDecisionBlockers: vueRef([]), pqcDecisionUncertainMessage: vueRef(''), pqcDecisionIdempotencyKey: vueRef(''),
    pqcDecisionLockedApplicationIds: reactive(new Set()),
    pqcDecisionForm: reactive({ signaturePassword: '', udiControlDocumentNo: '中文🔬UDI', approvalOpinion: '正式意见🔬', rejectReason: '正式拒绝原因🔬' }),
    pqcDecisionFormRef: vueRef({ validate: () => validation?.promise ?? Promise.resolve(), clearValidate: () => {} })
  }
  const production = 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue', board = 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'
  const batch = 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
  const batchApi = loadApi('src/api/mes/pro/edhr/batchExecution.ts', n.request)
  run('Object.assign(globalThis, { EDHR_BATCH_STATUS_VOIDED, EDHR_BATCH_STATUS_ARCHIVED, EDHR_BATCH_STATUS_REJECTED })', batchApi)
  Object.assign(r, {
    getEdhrBatchExecutionPage: batchApi.getEdhrBatchExecutionPage,
    EDHR_BATCH_STATUS_VOIDED: batchApi.EDHR_BATCH_STATUS_VOIDED,
    EDHR_BATCH_STATUS_ARCHIVED: batchApi.EDHR_BATCH_STATUS_ARCHIVED,
    EDHR_BATCH_STATUS_REJECTED: batchApi.EDHR_BATCH_STATUS_REJECTED,
    queryParams: reactive({ pageNo: 1, pageSize: 10, batchExecutionCode: ' test-batch ', workOrderCode: '', batchCode: '', productCode: '', routeCode: '', status: undefined, createTime: [], quickFilter: undefined })
  })
  const host = r7Host(), shown = vueRef(true)
  const component = { name: 'R7RealProductionHandlers', setup() {
    if (branch.startsWith('production')) {
      const s = r7Script(production)
      run(extract(production, ['isFrontlineProductionProcess', 'isFrontlinePqcProcess', 'isSameProcess']), r)
      run(s.slice(s.indexOf('const productionSubmitContextKey'), s.indexOf('const pqcSubmitContextKey')) +
        '\nObject.assign(globalThis, { productionSubmitContextKey, productionSubmitGuard, productionSubmitReceipts, productionSubmitReceiptRevision, productionSubmitReadPending, productionSubmitWritePending, productionActionInProgress })', r)
      run(extract(production, ['refreshProductionSelectedProcessSubmittedQuantity', 'retryProductionSubmitRead', 'handleProductionFormalSubmit', 'openProductionSubmitSuccessDialog']), r)
    } else if (branch === 'batchReject') {
      const s = r7Script(batch)
      run(extract(batch, ['resetRejectForm']), r)
      run(s.slice(s.indexOf('const rejectContextGuard'), s.indexOf('const handleRejectClick')) +
        '\nObject.assign(globalThis, { rejectContextGuard, resetRejectContext, openRejectDialog })', r)
      run(extract(batch, ['submitRejectBatchExecution']), r)
      run(extract(batch, ['EDHR_BATCH_EXECUTION_TRACE_ONLY_STATUSES', 'normalizeBatchStatusValue', 'isVoidedBatchExecutionStatus', 'isTraceOnlyBatchExecutionStatus', 'isVisibleBatchExecutionRow', 'normalizeBatchExecutionQuery', 'buildQuery', 'batchListReadGeneration', 'loadBatchList', 'getList']), r)
    } else {
      const s = r7Script(board)
      run(extract(board, ['requireJsonLongId', 'requirePositiveVersion', 'createPqcDecisionIdempotencyKey', 'resolvePqcProductionReleaseFailure', 'resolvePqcReleaseStatusLabel']), r)
      run(s.slice(s.indexOf('let pqcDecisionDialogGeneration'), s.indexOf('let managerReleaseDialogGeneration')) +
        '\nObject.assign(globalThis, { submitPqcProductionReleaseDecision, resetPqcDecisionDialog, capturePqcDecisionContext, pqcDecisionPayloadHash })', r)
    }
    if (branch !== 'batchReject') run(extract(board, ['refreshStats', 'workTaskListSequence', 'getList']), r)
    return () => r7Vue.h('div')
  } }
  // Mount through installed Vue, then KeepAlive deactivation/unmount executes real hooks.
  const app = host.renderer.createApp({ setup: () => () => r7Vue.h(r7Vue.KeepAlive, null, () => shown.value ? r7Vue.h(component) : null) })
  app.mount(host.root)
  let disposed = false
  const seed = () => {
    if (branch === 'batchReject') {
      r.openRejectDialog({ id: 13 }); r.rejectForm.nonconformanceReason = 'formal reason'; r.rejectForm.signaturePassword = 'test-only'
    } else if (branch.startsWith('pqc')) {
      r.pqcDecisionTask.value = { ...task }; r.pqcDecisionReceipt.value = { ...pendingReceipt }
      r.pqcDecisionAction.value = branch === 'pqcReject' ? 'REJECT' : 'APPROVE'
      r.pqcDecisionDialogVisible.value = true; r.pqcDecisionForm.signaturePassword = 'test-only'
      r.pqcDecisionForm.udiControlDocumentNo = '中文🔬UDI'; r.pqcDecisionForm.approvalOpinion = '正式意见🔬'; r.pqcDecisionForm.rejectReason = '正式拒绝原因🔬'
    } else if (branch === 'productionRead') {
      r.productionSubmitReceipts.complete(r.productionSubmitContextKey(), { finish: () => { ui.push(['read-finished']) } })
      r.productionSubmitReceiptRevision.value++
    }
  }
  seed()
  const start = () => branch.startsWith('pqc') ? r.submitPqcProductionReleaseDecision() : branch === 'batchReject' ? r.submitRejectBatchExecution() :
    branch === 'productionSubmit' ? r.handleProductionFormalSubmit() : r.retryProductionSubmitRead()
  const setNewState = () => {
    if (branch.startsWith('production')) { r.deviceState.loadingProcesses = true; r.deviceState.lastError = 'new-error'; r.payloadLoading.value = true; r.productionActionInProgress.value = true }
    else if (branch === 'batchReject') { r.rejectForm.signaturePassword = 'new-test-only'; r.rejectError.value = 'new-error'; r.rejectLoading.value = true }
    else { r.pqcDecisionForm.signaturePassword = 'new-test-only'; r.pqcDecisionUncertainMessage.value = 'new-error'; r.pqcDecisionSubmitting.value = true }
  }
  const snapshot = () => branch.startsWith('production') ? JSON.stringify([r.deviceState.loadingProcesses, r.deviceState.lastError, r.payloadLoading.value, r.productionActionInProgress.value]) :
    branch === 'batchReject' ? JSON.stringify([r.rejectForm.signaturePassword, r.rejectError.value, r.rejectLoading.value]) :
      JSON.stringify([r.pqcDecisionForm.signaturePassword, r.pqcDecisionUncertainMessage.value, r.pqcDecisionSubmitting.value, r.pqcDecisionReceipt.value])
  const change = async kind => {
    if (kind === 'route' || kind === 'routeABA') { route.fullPath = '/next'; if (kind === 'routeABA') route.fullPath = '/original' }
    else if (kind === 'session') { user.getUser.id = '8'; invalidation.syncSession() }
    else if (kind === 'tenant') { identity.tenant = '2'; invalidation.syncSession() }
    else if (kind === 'dialog') {
      if (branch.startsWith('pqc')) { r.pqcDecisionDialogVisible.value = false; seed() }
      else if (branch === 'batchReject') { r.rejectDialogVisible.value = false; seed() }
      else { r.deviceState.selectedProcess = { activeOrderId: 13, routeId: 12, routeProcessId: 99, processId: 98 }; r.deviceState.selectedProcess = { activeOrderId: 13, routeId: 12, routeProcessId: 14, processId: 15 } }
    } else if (kind === 'source' || kind === 'version' || kind === 'action') {
      if (branch.startsWith('pqc')) {
        if (kind === 'version') r.pqcDecisionReceipt.value.version++
        else if (kind === 'action') r.pqcDecisionAction.value = r.pqcDecisionAction.value === 'APPROVE' ? 'REJECT' : 'APPROVE'
        else r.pqcDecisionTask.value.businessScopeId = '90071992547409933'
      } else if (branch === 'batchReject') r.selectedRejectBatch.value = { id: 16 }
      else r.deviceState.selectedActiveOrder.activeOrderId = 16
    } else if (kind === 'deactivate') { shown.value = false; await r7Vue.nextTick() }
    else if (kind === 'unmount') { app.unmount(); disposed = true }
    setNewState()
  }
  const success = item => branch.startsWith('pqc') ? r7DecisionReceipt(item) : branch === 'batchReject' ? { id: 44, batchExecutionId: 13, reviewCode: 'formal-review' } :
    [{ activeOrderId: 13, routeId: 12, routeProcessId: 14, processId: 15, submittedQuantity: 9 }]
  const drainSuccessReads = async promise => {
    let done = false, thrown
    promise.then(() => { done = true }, error => { done = true; thrown = error })
    for (let i = 0; i < 25 && !done; i++) {
      await flush()
      for (const request of n.all) if (!request.r7Answered && request.config.method === 'get') {
        request.r7Answered = true
        request.success(request.config.url.includes('stats') ? { todoCount: 0 } : { list: [], total: 0 })
      }
    }
    assert.equal(done, true, 'real handler and its reads must settle')
    if (thrown) throw thrown
  }
  return { n, r, ui, authEffects, confirmation, acceptedEvents, publishAttempts, invalidation, task, pendingReceipt, start, change, snapshot, success, drainSuccessReads,
    dispose: () => { if (!disposed) { app.unmount(); disposed = true } } }
}

for (const branch of ['productionRead', 'batchReject', 'pqcApprove', 'pqcReject'])
  test('R7 original omitted call refuses stale401 ' + branch, async t => {
    const h = r7Harness(branch); t.after(h.dispose)
    const p = h.start(), item = await h.n.next(); await h.change('session')
    item.unauthorized(); await p; h.confirmation.resolve(); await flush()
    assert.deepEqual(h.authEffects, []); assert.equal(typeof item.config.isRequestContextCurrent, 'function')
  })

for (const branch of ['productionRead', 'batchReject', 'pqcApprove', 'pqcReject'])
  for (const context of ['current', 'route', 'routeABA', 'dialog', 'source', 'version', 'session', 'tenant', 'deactivate', 'unmount'])
    for (const kind of ['business', 'business500', 'business901', 'http', 'network', 'timeout', 'unauthorized'])
      test(`R7 real omitted chain ${branch}/${context}/${kind}`, async t => {
        const h = r7Harness(branch); t.after(h.dispose)
        const p = h.start(), item = await h.n.next()
        assert.equal(typeof item.config.isRequestContextCurrent, 'function')
        if (branch !== 'productionRead') assert.equal(typeof item.config.isSuccessReceiptContextCurrent, 'function')
        if (context !== 'current') await h.change(context)
        const state = h.snapshot()
        if (kind === 'unauthorized') item.unauthorized(); else item.fail(kind)
        // Current PQC timeout is the only existing read-recovery branch.
        if (context === 'current' && kind === 'timeout' && branch.startsWith('pqc')) (await h.n.next()).fail('business')
        await p; h.confirmation.resolve(); await flush()
        assert.equal(h.acceptedEvents.length, 0)
        assert.equal(h.n.notices.length, 0, 'local current errors must not duplicate global errors')
        if (context !== 'current') {
          assert.equal(h.snapshot(), state); assert.deepEqual(h.authEffects, []); assert.equal(h.ui.length, 0)
          assert.equal(item.config.isRequestContextCurrent(), false)
        } else {
          assert.ok(h.ui.some(x => x[0] === 'error'), 'current formal error remains visible')
          if (kind === 'unauthorized') assert.deepEqual(h.authEffects, ['confirm', 'deleteCache', 'removeToken'])
          else assert.deepEqual(h.authEffects, [])
        }
      })

for (const branch of ['batchReject', 'pqcApprove', 'pqcReject'])
  for (const context of ['current', 'route', 'routeABA', 'dialog', 'source', 'version', 'session', 'tenant', 'deactivate', 'unmount'])
    test(`R7 original formal success once ${branch}/${context}`, async t => {
      const h = r7Harness(branch); t.after(h.dispose)
      const p = h.start(), item = await h.n.next(), receipt = h.success(item)
      if (context !== 'current') await h.change(context)
      const state = h.snapshot(); item.success(receipt)
      if (context === 'current') await h.drainSuccessReads(p); else await p
      const crossSession = context === 'session' || context === 'tenant'
      assert.equal(h.acceptedEvents.length, crossSession ? 0 : 1)
      assert.equal(h.invalidation.revision('EDHR', 'TODO'), crossSession ? 0 : 1)
      assert.equal(h.invalidation.revision('EDHR', 'DONE'), crossSession ? 0 : 1)
      if (!crossSession) {
        const event = h.acceptedEvents[0]; h.invalidation.publishEdhrSuccess(event)
        assert.equal(h.invalidation.revision('EDHR', 'TODO'), 1)
        assert.equal(event.session.tenantId, '1'); assert.equal(event.session.loginUserId, '7')
        if (branch.startsWith('pqc')) assert.equal(event.identity.sourceTaskId, h.task.id)
        else assert.equal(event.identity.businessSourceId, '13')
      }
      if (context !== 'current') { assert.equal(h.snapshot(), state); assert.equal(h.ui.length, 0) }
      assert.deepEqual(h.authEffects, [])
    })

for (const branch of ['pqcApprove', 'pqcReject'])
  for (const context of ['routeABA', 'dialog', 'source', 'version', 'action', 'session', 'tenant', 'deactivate', 'unmount'])
    test(`R7 validation cannot dispatch old PQC ${branch}/${context}`, async t => {
      const validation = r7Deferred(), h = r7Harness(branch, {}, validation); t.after(h.dispose)
      const p = h.start(); await flush(); await h.change(context); const state = h.snapshot()
      validation.resolve(); await p
      assert.equal(h.n.all.length, 0); assert.equal(h.snapshot(), state); assert.equal(h.ui.length, 0)
      assert.equal(h.acceptedEvents.length, 0)
    })

for (const branch of ['productionRead', 'batchReject', 'pqcApprove', 'pqcReject'])
  for (const change of ['routeABA', 'dialog', 'session', 'tenant'])
    test(`R7 delayed current login confirmation becomes obsolete ${branch}/${change}`, async t => {
      const h = r7Harness(branch); t.after(h.dispose)
      const p = h.start(), item = await h.n.next(); item.unauthorized(); await p
      assert.deepEqual(h.authEffects, ['confirm'])
      await h.change(change); const state = h.snapshot(), before = h.ui.length
      h.confirmation.resolve(); await flush()
      assert.deepEqual(h.authEffects, ['confirm']); assert.equal(h.snapshot(), state); assert.equal(h.ui.length, before)
    })

test('R7 sixth API real Batch postwrite loader obsolete401 RED', async t => {
  const h = r7Harness('batchReject'); t.after(h.dispose)
  const p = h.start(), write = await h.n.next()
  write.success(h.success(write))
  const read = await h.n.next()
  assert.equal(read.config.method, 'get')
  assert.ok(read.config.url.endsWith('/page'))
  assert.equal(read.config.params.batchExecutionCode, 'test-batch')
  assert.equal(write.config.isRequestContextCurrent(), false, 'closing the old write dialog invalidates its immutable guard')
  await h.change('session'); const state = h.snapshot(), before = h.ui.length
  read.unauthorized(); await p; h.confirmation.resolve(); await flush()
  assert.deepEqual(h.authEffects, [])
  assert.equal(h.snapshot(), state); assert.equal(h.ui.length, before)
  assert.equal(h.acceptedEvents.length, 1)
  assert.equal(h.n.all.length, 2)
})

const r7SettleReplays = async (h, promises) => {
  let done = false
  const settled = Promise.all(promises).then(() => { done = true })
  for (let i = 0; i < 60 && !done; i++) {
    await flush()
    for (const item of h.n.all) if (!item.r7Answered) {
      item.r7Answered = true
      item.success(item.config.method === 'post' || item.config.url.includes('/active-order/processes')
        ? h.success(item) : { list: [], total: 0, todoCount: 0 })
    }
  }
  assert.equal(done, true, 'actual owner/queued handler chains settle without hidden requests')
  await settled
}

for (const branch of ['productionRead', 'batchReject', 'pqcApprove', 'pqcReject'])
  for (const context of ['current', 'routeABA', 'session', 'tenant', 'deactivate'])
    for (const outcome of ['success', 'failure'])
      test(`R7 true refresh owner ${branch}/${context}/${outcome}`, async t => {
        const refresh = r7Deferred(), effects = []
        const h = r7Harness(branch, { getRefreshToken: () => 'test-only-refresh', getAccessToken: () => 'test-only-access',
          axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => { effects.push('refresh'); return refresh.promise } } })
        t.after(h.dispose)
        const p = h.start(), original = await h.n.next(); original.r7Answered = true
        original.unauthorized(); await flush()
        assert.deepEqual(effects, ['refresh'])
        if (context !== 'current') await h.change(context)
        const state = h.snapshot()
        if (outcome === 'success') refresh.resolve({ data: { code: 200, data: { accessToken: 'test-only-new-access', refreshToken: 'test-only-new-refresh' } } })
        else refresh.reject(Error('test-only refresh failure'))
        if (context === 'current' && outcome === 'success') await r7SettleReplays(h, [p]); else await p
        if (context !== 'current') {
          assert.equal(h.n.all.length, 1); assert.deepEqual(h.authEffects, [])
          assert.equal(h.snapshot(), state); assert.equal(h.ui.length, 0); assert.equal(h.acceptedEvents.length, 0)
        } else if (outcome === 'success') {
          assert.deepEqual(h.authEffects, ['setToken'])
          assert.equal(h.acceptedEvents.length, branch === 'productionRead' ? 0 : 1)
          if (branch === 'productionRead') { assert.equal(h.r.deviceState.selectedProcess.submittedQuantity, 9); assert.equal(h.r.productionSubmitReadPending.value, false) }
          assert.equal(h.n.all.filter(x => x.config.method === original.config.method && x.config.url === original.config.url).length, 2)
        } else {
          assert.deepEqual(h.authEffects, ['confirm']); assert.ok(h.ui.some(x => x[0] === 'error'))
          assert.equal(h.n.all.length, 1); assert.equal(h.acceptedEvents.length, 0)
          h.confirmation.resolve(); await flush()
          assert.deepEqual(h.authEffects, ['confirm', 'deleteCache', 'removeToken'])
        }
        assert.equal(h.n.notices.length, 0)
      })

for (const branch of ['productionRead', 'batchReject', 'pqcApprove', 'pqcReject'])
  for (const context of ['current', 'bothStale', 'ownerStale', 'queuedStale'])
    for (const outcome of ['success', 'failure'])
      test(`R7 true queued refresh ${branch}/${context}/${outcome}`, async t => {
        const refresh = r7Deferred(), effects = []
        const owner = r7Harness(branch, { getRefreshToken: () => 'test-only-refresh', getAccessToken: () => 'test-only-access',
          axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => { effects.push('refresh'); return refresh.promise } } })
        const queued = r7Harness(branch, {}, null, owner.n); t.after(owner.dispose); t.after(queued.dispose)
        const a = owner.start(), first = await owner.n.next(); first.r7Answered = true; first.unauthorized(); await flush()
        const b = queued.start(), second = await owner.n.next(); second.r7Answered = true; second.unauthorized(); await flush()
        assert.deepEqual(effects, ['refresh'])
        if (context === 'bothStale' || context === 'ownerStale') await owner.change('routeABA')
        if (context === 'bothStale' || context === 'queuedStale') await queued.change('dialog')
        const ownerState = owner.snapshot(), queuedState = queued.snapshot()
        if (outcome === 'success') refresh.resolve({ data: { code: 200, data: { accessToken: 'test-only-new-access', refreshToken: 'test-only-new-refresh' } } })
        else refresh.reject(Error('test-only refresh failure'))
        await r7SettleReplays(owner, [a, b])
        const staleOwner = context === 'bothStale' || context === 'ownerStale', staleQueued = context === 'bothStale' || context === 'queuedStale'
        if (staleOwner) { assert.equal(owner.snapshot(), ownerState); assert.equal(owner.ui.length, 0); assert.deepEqual(owner.authEffects, []) }
        if (staleQueued) { assert.equal(queued.snapshot(), queuedState); assert.equal(queued.ui.length, 0); assert.equal(queued.acceptedEvents.length, 0) }
        const replays = owner.n.all.filter(x => x.config.method === first.config.method && x.config.url === first.config.url).length - 2
        assert.equal(replays, Number(!staleOwner && outcome === 'success') + Number(!staleQueued))
        if (!staleOwner && outcome === 'failure') {
          assert.deepEqual(owner.authEffects, ['confirm']); owner.confirmation.resolve(); await flush()
          assert.deepEqual(owner.authEffects, ['confirm', 'deleteCache', 'removeToken'])
        }
        assert.equal(owner.n.notices.length, 0)
      })

for (const branch of ['pqcApprove', 'pqcReject'])
  for (const mode of ['direct', 'recovery'])
    for (const mismatch of ['application', 'task', 'action', 'status', 'actor', 'key', 'hash', 'detail', 'reports'])
      test(`R7 formal PQC identity rejects ${branch}/${mode}/${mismatch}`, async t => {
        const h = r7Harness(branch); t.after(h.dispose)
        const p = h.start(), write = await h.n.next(), receipt = h.success(write)
        if (mismatch === 'application') receipt.applicationId = '90071992547409933'
        else if (mismatch === 'task') receipt.pqcReleaseWorkTaskId = '90071992547409934'
        else if (mismatch === 'action') receipt.decision = branch === 'pqcApprove' ? 'REJECT' : 'APPROVE'
        else if (mismatch === 'status') receipt.status = 'PQC_RELEASE_PENDING'
        else if (mismatch === 'actor') receipt.decidedBy = '8'
        else if (mismatch === 'key') receipt.decisionIdempotencyKey = 'different-request'
        else if (mismatch === 'hash') receipt.decisionPayloadHash = '0'.repeat(64)
        else if (mismatch === 'detail') { if (branch === 'pqcApprove') receipt.udiControlDocumentNo = 'different-UDI'; else receipt.rejectReason = 'different-reason' }
        else if (mismatch === 'reports') { if (branch === 'pqcApprove') receipt.reportUploadTasks[0].workTaskId = ''; else receipt.rejectReason = '' }
        if (mode === 'direct') write.success(receipt)
        else { write.fail('timeout'); const read = await h.n.next(); assert.equal(read.config.method, 'get'); read.success(receipt) }
        await p
        assert.equal(h.acceptedEvents.length, 0); assert.equal(h.invalidation.revision('EDHR', 'TODO'), 0)
        assert.ok(h.ui.some(x => x[0] === 'error')); assert.equal(h.n.all.length, mode === 'direct' ? 1 : 2)
        assert.equal(h.n.notices.length, 0)
      })

for (const branch of ['pqcApprove', 'pqcReject'])
  for (const context of ['current', 'routeABA', 'dialog', 'action', 'version', 'session', 'tenant', 'deactivate', 'unmount'])
    test(`R7 timeout recovery proves original formal identity ${branch}/${context}`, async t => {
      const h = r7Harness(branch); t.after(h.dispose)
      const p = h.start(), write = await h.n.next(), receipt = h.success(write)
      write.fail('timeout'); const read = await h.n.next()
      assert.equal(read.config.method, 'get'); assert.equal(typeof read.config.isRequestContextCurrent, 'function')
      if (context !== 'current') await h.change(context)
      const state = h.snapshot(); read.success(receipt)
      if (context === 'current') await h.drainSuccessReads(p); else await p
      const crossSession = context === 'session' || context === 'tenant'
      assert.equal(h.acceptedEvents.length, crossSession ? 0 : 1)
      assert.equal(h.invalidation.revision('EDHR', 'TODO'), crossSession ? 0 : 1)
      if (context !== 'current') { assert.equal(h.snapshot(), state); assert.equal(h.ui.length, 0) }
      else { assert.ok(h.ui.some(x => x[0] === 'warning')); assert.equal(h.r.pqcDecisionForm.signaturePassword, '') }
      assert.deepEqual(h.authEffects, []); assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
    })

test('R7 production confirmed write read failure retry remains read only', async t => {
  const h = r7Harness('productionSubmit'); t.after(h.dispose)
  const p = h.start(), write = await h.n.next(); assert.equal(write.config.method, 'post')
  write.success({ eventId: 71 }); const firstRead = await h.n.next()
  assert.equal(firstRead.config.method, 'get'); assert.equal(typeof firstRead.config.isRequestContextCurrent, 'function')
  firstRead.fail('http'); await p
  assert.equal(h.acceptedEvents.length, 1); assert.equal(h.r.productionSubmitReadPending.value, true)
  assert.equal(h.r.productionSubmitSuccessOpen.value, false); assert.ok(h.ui.some(x => x[0] === 'error'))
  await h.r.handleProductionFormalSubmit(); assert.equal(h.n.all.length, 2, 'pending confirmed receipt blocks another write')
  const retry = h.r.retryProductionSubmitRead(), secondRead = await h.n.next()
  secondRead.success(h.success(secondRead)); await retry
  assert.equal(h.r.deviceState.selectedProcess.submittedQuantity, 9)
  assert.equal(h.r.productionSubmitReadPending.value, false); assert.equal(h.r.productionSubmitSuccessOpen.value, true)
  assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1); assert.equal(h.acceptedEvents.length, 1)
  await h.r.retryProductionSubmitRead(); assert.equal(h.n.all.length, 3)
})

for (const context of ['routeABA', 'dialog', 'session', 'tenant', 'deactivate', 'unmount'])
  for (const kind of ['success', 'unauthorized', 'http', 'timeout'])
    test(`R7 production original success quantity read obsolete ${context}/${kind}`, async t => {
      const h = r7Harness('productionSubmit'); t.after(h.dispose)
      const p = h.start(), write = await h.n.next(); write.success({ eventId: 72 })
      const read = await h.n.next(); await h.change(context); const state = h.snapshot()
      if (kind === 'success') read.success(h.success(read)); else if (kind === 'unauthorized') read.unauthorized(); else read.fail(kind)
      await p; h.confirmation.resolve(); await flush()
      assert.equal(h.snapshot(), state); assert.deepEqual(h.authEffects, []); assert.equal(h.ui.length, 0)
      assert.equal(h.acceptedEvents.length, 1); assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
    })

const r7StartBatchRefresh = async h => {
  const promise = h.start(), write = await h.n.next(); write.r7Answered = true
  write.success(h.success(write))
  const read = await h.n.next()
  assert.equal(read.config.method, 'get'); assert.ok(read.config.url.endsWith('/page'))
  assert.equal(write.config.isRequestContextCurrent(), false)
  assert.equal(read.config.isRequestContextCurrent(), true)
  return { promise, write, read }
}

for (const context of ['current', 'routeABA', 'dialog', 'source', 'session', 'tenant', 'query', 'deactivate', 'unmount'])
  for (const kind of ['success', 'unauthorized', 'business', 'http', 'timeout'])
    test(`R7 real Batch refresh context ${context}/${kind}`, async t => {
      const h = r7Harness('batchReject'); t.after(h.dispose)
      const { promise, read } = await r7StartBatchRefresh(h)
      if (context === 'query') h.r.queryParams.batchExecutionCode = 'new-query'
      else if (context !== 'current') await h.change(context)
      if (context !== 'current') { h.r.loading.value = true; h.r.loadError.value = 'new-load-error'; h.r.list.value = [{ id: 77 }]; h.r.total.value = 1 }
      const state = h.snapshot(), listState = JSON.stringify([h.r.list.value, h.r.total.value, h.r.loading.value, h.r.loadError.value]), uiLength = h.ui.length
      if (kind === 'success') read.success({ list: [{ id: 55, status: 1 }, { id: 56, status: h.r.EDHR_BATCH_STATUS_VOIDED }], total: 2 })
      else if (kind === 'unauthorized') read.unauthorized(); else read.fail(kind)
      await promise; h.confirmation.resolve(); await flush()
      assert.equal(h.acceptedEvents.length, 1); assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
      assert.equal(h.n.notices.length, 0)
      if (context !== 'current') {
        assert.equal(h.snapshot(), state); assert.equal(JSON.stringify([h.r.list.value, h.r.total.value, h.r.loading.value, h.r.loadError.value]), listState)
        assert.equal(h.ui.length, uiLength); assert.deepEqual(h.authEffects, [])
      } else if (kind === 'success') {
        assert.equal(h.r.list.value.length, 1); assert.equal(h.r.list.value[0].id, 55); assert.equal(h.r.total.value, 2)
        assert.equal(h.r.loading.value, false); assert.equal(h.r.loadError.value, '')
      } else {
        assert.ok(h.r.loadError.value); assert.ok(h.ui.some(x => x[0] === 'error')); assert.equal(h.r.loading.value, false)
      }
    })

test('R7 real Batch loader newer read owns generation and older401 stays obsolete', async t => {
  const h = r7Harness('batchReject'); t.after(h.dispose)
  const { promise, read } = await r7StartBatchRefresh(h)
  const next = h.r.getList(), fresh = await h.n.next()
  assert.equal(read.config.isRequestContextCurrent(), false); assert.equal(fresh.config.isRequestContextCurrent(), true)
  read.unauthorized(); await promise; fresh.success({ list: [{ id: 88, status: 1 }], total: 1 }); await next
  assert.deepEqual(h.authEffects, []); assert.equal(h.r.list.value[0].id, 88); assert.equal(h.r.loading.value, false)
  assert.equal(h.acceptedEvents.length, 1); assert.equal(h.n.all.length, 3)
})

for (const context of ['current', 'routeABA', 'dialog', 'session', 'tenant'])
  for (const outcome of ['success', 'failure'])
    test(`R7 real Batch refresh owner ${context}/${outcome}`, async t => {
      const refresh = r7Deferred()
      const h = r7Harness('batchReject', { getRefreshToken: () => 'test-only-refresh', getAccessToken: () => 'test-only-access',
        axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refresh.promise } }); t.after(h.dispose)
      const { promise, read } = await r7StartBatchRefresh(h); read.r7Answered = true; read.unauthorized(); await flush()
      if (context !== 'current') await h.change(context)
      const state = h.snapshot(), before = h.ui.length
      if (outcome === 'success') refresh.resolve({ data: { code: 200, data: { accessToken: 'test-only-new-access', refreshToken: 'test-only-new-refresh' } } })
      else refresh.reject(Error('test-only refresh failure'))
      if (context === 'current' && outcome === 'success') await r7SettleReplays(h, [promise]); else await promise
      assert.equal(h.acceptedEvents.length, 1); assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
      if (context !== 'current') { assert.equal(h.n.all.length, 2); assert.deepEqual(h.authEffects, []); assert.equal(h.snapshot(), state); assert.equal(h.ui.length, before) }
      else if (outcome === 'success') { assert.deepEqual(h.authEffects, ['setToken']); assert.equal(h.r.loadError.value, ''); assert.equal(h.n.all.length, 3) }
      else { assert.deepEqual(h.authEffects, ['confirm']); assert.ok(h.r.loadError.value); assert.ok(h.ui.some(x => x[0] === 'error')) }
    })

for (const context of ['current', 'bothStale', 'ownerStale', 'queuedStale'])
  for (const outcome of ['success', 'failure'])
    test(`R7 real Batch refresh queued ${context}/${outcome}`, async t => {
      const refresh = r7Deferred()
      const owner = r7Harness('batchReject', { getRefreshToken: () => 'test-only-refresh', getAccessToken: () => 'test-only-access',
        axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refresh.promise } })
      const queued = r7Harness('batchReject', {}, null, owner.n); t.after(owner.dispose); t.after(queued.dispose)
      const a = await r7StartBatchRefresh(owner); a.read.r7Answered = true; a.read.unauthorized(); await flush()
      const b = await r7StartBatchRefresh(queued); b.read.r7Answered = true; b.read.unauthorized(); await flush()
      if (context === 'bothStale' || context === 'ownerStale') await owner.change('session')
      if (context === 'bothStale' || context === 'queuedStale') await queued.change('dialog')
      const ownerState = owner.snapshot(), queuedState = queued.snapshot(), ownerUi = owner.ui.length, queuedUi = queued.ui.length
      if (outcome === 'success') refresh.resolve({ data: { code: 200, data: { accessToken: 'test-only-new-access', refreshToken: 'test-only-new-refresh' } } })
      else refresh.reject(Error('test-only refresh failure'))
      await r7SettleReplays(owner, [a.promise, b.promise])
      const staleOwner = context === 'bothStale' || context === 'ownerStale', staleQueued = context === 'bothStale' || context === 'queuedStale'
      assert.equal(owner.n.all.filter(x => x.config.method === 'get').length, 2 + Number(!staleOwner && outcome === 'success') + Number(!staleQueued))
      assert.equal(owner.n.all.filter(x => x.config.method === 'post').length, 2)
      if (staleOwner) { assert.equal(owner.snapshot(), ownerState); assert.equal(owner.ui.length, ownerUi); assert.deepEqual(owner.authEffects, []) }
      if (staleQueued) { assert.equal(queued.snapshot(), queuedState); assert.equal(queued.ui.length, queuedUi) }
      assert.equal(owner.acceptedEvents.length, 1); assert.equal(queued.acceptedEvents.length, 1); assert.equal(owner.n.notices.length, 0)
    })

for (const branch of ['pqcApprove', 'pqcReject'])
  test(`R7 real PQC read refresh keeps old write guard immutable ${branch}`, async t => {
    const h = r7Harness(branch); t.after(h.dispose)
    const p = h.start(), write = await h.n.next(); write.success(h.success(write))
    const read = await h.n.next()
    assert.equal(write.config.isRequestContextCurrent(), false); assert.equal(read.config.isRequestContextCurrent(), true)
    await h.change('dialog'); const state = h.snapshot(), before = h.ui.length
    read.unauthorized(); await p; h.confirmation.resolve(); await flush()
    assert.deepEqual(h.authEffects, []); assert.equal(h.snapshot(), state); assert.equal(h.ui.length, before)
    assert.equal(h.acceptedEvents.length, 1); assert.equal(h.n.all.length, 2, 'no stats dispatch after obsolete list read')
  })


// R8: the reachable production-release page, including its real setup/watch/lifecycle.
// Controlled adapters execute installed Axios; test-only ports never use a network.
const r8Harness = async (networkPorts = {}, sharedNetwork = null) => {
  const authEffects = [], confirmation = r7Deferred(), ui = [], acceptedEvents = []
  const n = sharedNetwork || network({ removeToken: () => authEffects.push('removeToken'),
    deleteUserCache: () => authEffects.push('deleteCache'), setRefreshedToken: () => authEffects.push('setToken'),
    ElMessageBox: { confirm: () => { authEffects.push('confirm'); return confirmation.promise } }, ...networkPorts })
  const identity = reactive({ tenant: '1', logged: true }), user = reactive({ getUser: { id: '7' }, permissions: ['*:*:*'] })
  const storePorts = { computed, ref: vueRef, watch, defineStore, createApprovalInvalidationState,
    getAccessToken: () => identity.logged ? 'test-only-access' : '', getTenantId: () => identity.tenant,
    getVisitTenantId: () => '', useUserStore: () => user }
  setActivePinia(createPinia())
  run(withoutImports(read('src/store/modules/approvalCenterInvalidation.ts')).replace('export const ', 'const ') +
    '\nglobalThis.factory = useApprovalCenterInvalidationStore', storePorts)
  const invalidation = storePorts.factory(), publish = invalidation.publishEdhrSuccess
  invalidation.publishEdhrSuccess = event => {
    const before = invalidation.revision('EDHR', 'TODO'); publish(event)
    if (invalidation.revision('EDHR', 'TODO') > before) acceptedEvents.push(event)
  }
  const route = reactive({ fullPath: '/mes/pqc-production-release', query: {} })
  const api = loadApi('src/api/mes/pro/productionRelease/index.ts', n.request)
  run('Object.assign(globalThis, { PQC_RELEASE_VIEW_PENDING, PQC_RELEASE_VIEW_RELEASED, PQC_RELEASE_VIEW_CONCESSION_RELEASED, PQC_RELEASE_VIEW_REWORKED, PQC_RELEASE_VIEW_VOIDED })', api)
  const r = { ...api, Error, Object, String, Number, Date, Math, Promise, TextEncoder, Uint8Array, DataView,
    crypto: globalThis.crypto, CryptoJS: r7CryptoJS, console,
    ref: vueRef, reactive, computed, watch, onMounted: r7Vue.onMounted,
    onBeforeUnmount: r7Vue.onBeforeUnmount, onDeactivated: r7Vue.onDeactivated,
    defineOptions: () => {}, useRouter: () => ({ push: () => { throw Error('unexpected navigation') } }),
    useRoute: () => route, useUserStore: () => user, useApprovalCenterInvalidationStore: () => invalidation,
    useMessage: () => ({ error: x => ui.push(['error', String(x)]), success: x => ui.push(['success', String(x)]), warning: x => ui.push(['warning', String(x)]) }) }
  const host = r7Host(), shown = vueRef(true)
  const component = { name: 'R8ActualPqcReleasePage', setup() {
    run(withoutImports(r7Script('src/views/mes/pro/production-release/PqcProductionReleasePage.vue')) +
      '\nObject.assign(globalThis, { openReleaseDialog, resetReleaseDialog, submitRelease, recoverUncertainRelease, releaseRequestScopeKey, loading, loadError, list, total, selectedRow, releaseForm, releaseDialogVisible, releaseSubmitting, releaseError, releaseResult, releaseOutcomeUncertain, releaseRequestStates, activeView, getList })', r)
    return () => r7Vue.h('div')
  } }
  const app = host.renderer.createApp({ setup: () => () => r7Vue.h(r7Vue.KeepAlive, null, () => shown.value ? r7Vue.h(component) : null) })
  app.mount(host.root)
  let disposed = false
  const initial = await n.next(); assert.equal(initial.config.method, 'get'); initial.r8Answered = true
  initial.success({ list: [], total: 0 }); await flush()
  const row = { applicationId: '90071992547409932', pqcReleaseWorkTaskId: '90071992547409931', version: 3, status: 'PQC_RELEASE_PENDING', approvalReady: true }
  const seed = () => { r.openReleaseDialog({ ...row }); r.releaseForm.signaturePassword = 'test-only-signature';
    r.releaseForm.udiControlDocumentNo = '中文🔬UDI'; r.releaseForm.approvalOpinion = '正式意见🔬' }
  seed()
  const snapshot = () => JSON.stringify([r.loading.value, r.loadError.value, r.list.value, r.total.value,
    r.releaseDialogVisible.value, r.releaseSubmitting.value, r.releaseError.value, r.releaseResult.value,
    r.releaseOutcomeUncertain.value, r.releaseForm, r.selectedRow.value, r.activeView.value])
  const settleBackground = async () => {
    for (let i = 0; i < 5; i++) { await flush(); for (const item of n.all)
      if (!item.r8Answered && !item.r8Held && item.config.method === 'get') {
        item.r8Answered = true; item.success({ list: [], total: 0 })
      } }
  }
  const change = async context => {
    if (context === 'route' || context === 'routeABA') { route.fullPath = '/next'; if (context === 'routeABA') route.fullPath = '/mes/pqc-production-release' }
    else if (context === 'dialog') { r.releaseDialogVisible.value = false; seed() }
    else if (context === 'source') r.selectedRow.value.applicationId = '90071992547409933'
    else if (context === 'task') r.selectedRow.value.pqcReleaseWorkTaskId = '90071992547409934'
    else if (context === 'version') r.selectedRow.value.version++
    else if (context === 'session') { user.getUser.id = '8'; invalidation.syncSession() }
    else if (context === 'tenant') { identity.tenant = '2'; invalidation.syncSession() }
    else if (context === 'deactivate') { shown.value = false; await r7Vue.nextTick() }
    else if (context === 'unmount') { app.unmount(); disposed = true }
    await settleBackground()
    r.releaseError.value = 'new context error'; r.releaseSubmitting.value = true
    r.releaseForm.signaturePassword = 'new-test-only'; r.list.value = [{ applicationId: '77' }]; r.total.value = 1
  }
  const begin = async () => {
    const promise = r.submitRelease(), write = await n.next(); write.r8Answered = true
    assert.equal(write.config.method, 'post'); const data = r7Body(write)
    const receipt = { applicationId: data.applicationId, pqcReleaseWorkTaskId: data.pqcReleaseWorkTaskId,
      decision: 'APPROVE', status: 'MANAGER_RELEASE_PENDING', reportUploadTasks: [], version: data.expectedVersion + 2,
      signatureId: '501', decidedBy: '7', decidedAt: '2026-10-09T08:00:00Z', batchExecutionId: '999',
      sourceSnapshotHash: 'a'.repeat(64), decisionIdempotencyKey: data.idempotencyKey,
      decisionPayloadHash: r7BackendHash(data), udiControlDocumentNo: data.udiControlDocumentNo }
    write.fail('timeout'); const receiptRead = await n.next(); receiptRead.r8Held = true
    assert.equal(receiptRead.config.method, 'get'); assert.equal(receiptRead.config.url, '/mes/pro/production-release/get')
    assert.equal(String(receiptRead.config.params.applicationId), row.applicationId)
    assert.equal(receiptRead.config.isRequestContextCurrent(), true)
    return { promise, write, receiptRead, receipt, scopeKey: r.releaseRequestScopeKey(row) }
  }
  return { n, r, ui, identity, user, invalidation, authEffects, confirmation, acceptedEvents, row,
    snapshot, change, begin, settleBackground, dispose: () => { if (!disposed) { app.unmount(); disposed = true } } }
}

for (const context of ['current', 'route', 'routeABA', 'dialog', 'source', 'task', 'version', 'deactivate', 'unmount', 'session', 'tenant'])
  test(`R8 actual active PQC formal recovery ${context}`, async t => {
    const h = await r8Harness(); t.after(h.dispose)
    const a = await h.begin()
    if (context !== 'current') await h.change(context)
    const state = h.snapshot(), uiCount = h.ui.length, getCount = h.n.all.filter(x => x.config.method === 'get').length
    a.receiptRead.success(a.receipt)
    if (context === 'current') {
      const refresh = await h.n.next(); refresh.r8Answered = true
      assert.equal(refresh.config.url, '/mes/pro/production-release/pqc/page'); refresh.success({ list: [], total: 0 })
    }
    await a.promise
    const crossSession = ['session', 'tenant'].includes(context)
    assert.equal(h.acceptedEvents.length, crossSession ? 0 : 1, 'original same-session formal receipt invalidates exactly once')
    assert.equal(h.invalidation.revision('EDHR', 'TODO'), crossSession ? 0 : 1)
    assert.equal(h.invalidation.revision('EDHR', 'DONE'), crossSession ? 0 : 1)
    assert.equal(h.r.releaseRequestStates.get(a.scopeKey), crossSession ? 'UNCERTAIN' : 'CONFIRMED')
    assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
    if (!crossSession) {
      assert.equal(h.acceptedEvents[0].identity.sourceTaskId, h.row.pqcReleaseWorkTaskId)
      assert.equal(h.acceptedEvents[0].identity.sourceTaskType, 'EDHR_WORK_TASK')
    }
    if (context !== 'current') {
      assert.equal(h.snapshot(), state); assert.equal(h.ui.length, uiCount)
      assert.equal(h.n.all.filter(x => x.config.method === 'get').length, getCount, 'old handler does not refresh the new page')
      await h.r.submitRelease(); assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
      assert.deepEqual(h.authEffects, [])
    } else {
      assert.equal(h.r.releaseResult.value.status, 'MANAGER_RELEASE_PENDING')
      assert.equal(h.r.releaseResult.value.reportUploadTasks.length, 0)
      assert.equal(h.r.releaseOutcomeUncertain.value, false); assert.equal(h.r.releaseSubmitting.value, false)
      assert.equal(h.r.releaseForm.signaturePassword, ''); assert.ok(h.ui.some(x => x[0] === 'warning'))
      await h.r.submitRelease(); assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
    }
    assert.equal(h.acceptedEvents.length, crossSession ? 0 : 1)
  })

for (const context of ['current', 'routeABA', 'dialog', 'source', 'task', 'version', 'deactivate', 'unmount', 'session', 'tenant'])
  for (const kind of ['business', 'http', 'timeout', 'unauthorized'])
    test(`R8 actual active PQC recovery error ${context}/${kind}`, async t => {
      const h = await r8Harness(); t.after(h.dispose); const a = await h.begin()
      if (context !== 'current') await h.change(context)
      const state = h.snapshot(), uiCount = h.ui.length
      if (kind === 'unauthorized') a.receiptRead.unauthorized(); else a.receiptRead.fail(kind)
      await a.promise; h.confirmation.resolve(); await flush()
      assert.equal(h.acceptedEvents.length, 0); assert.equal(h.n.notices.length, 0)
      assert.equal(h.r.releaseRequestStates.get(a.scopeKey), 'UNCERTAIN')
      assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
      if (context !== 'current') { assert.equal(h.snapshot(), state); assert.equal(h.ui.length, uiCount); assert.deepEqual(h.authEffects, []) }
      else { assert.ok(h.r.releaseError.value.includes('正式回执无法证明')); assert.ok(h.r.releaseError.value.includes('写入错误：'))
        assert.ok(h.r.releaseError.value.includes('回执错误：')); assert.equal(h.r.releaseOutcomeUncertain.value, true)
        assert.equal(h.r.releaseSubmitting.value, false)
        assert.deepEqual(h.authEffects, kind === 'unauthorized' ? ['confirm', 'deleteCache', 'removeToken'] : []) }
    })

for (const field of ['applicationId', 'pqcReleaseWorkTaskId', 'decision', 'status', 'signatureId', 'batchExecutionId', 'decidedAt', 'sourceSnapshotHash', 'decisionIdempotencyKey', 'decisionPayloadHash', 'decidedBy', 'udiControlDocumentNo'])
  for (const context of ['current', 'dialog'])
    test(`R8 actual active PQC rejects unmatched recovery ${field}/${context}`, async t => {
      const h = await r8Harness(); t.after(h.dispose); const a = await h.begin()
      if (context !== 'current') await h.change(context)
      const state = h.snapshot(), uiCount = h.ui.length, receipt = { ...a.receipt }
      receipt[field] = field === 'decisionPayloadHash' ? 'b'.repeat(64) : field === 'sourceSnapshotHash' ? 'wrong-hash' : ['applicationId', 'pqcReleaseWorkTaskId', 'decidedBy'].includes(field) ? '8' : 'wrong'
      a.receiptRead.success(receipt); await a.promise
      assert.equal(h.acceptedEvents.length, 0); assert.equal(h.invalidation.revision('EDHR', 'TODO'), 0)
      assert.equal(h.r.releaseRequestStates.get(a.scopeKey), 'UNCERTAIN'); assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
      if (context === 'current') assert.ok(h.r.releaseError.value.includes('正式回执无法证明'))
      else { assert.equal(h.snapshot(), state); assert.equal(h.ui.length, uiCount) }
    })

for (const context of ['routeABA', 'dialog', 'session', 'tenant', 'unmount'])
  test(`R8 actual active PQC delayed login confirmation ${context}`, async t => {
    const h = await r8Harness(); t.after(h.dispose); const a = await h.begin()
    a.receiptRead.unauthorized(); await a.promise; assert.deepEqual(h.authEffects, ['confirm'])
    await h.change(context); const state = h.snapshot()
    h.confirmation.resolve(); await flush(); assert.deepEqual(h.authEffects, ['confirm'])
    assert.equal(h.snapshot(), state); assert.equal(h.acceptedEvents.length, 0)
  })

const r8RefreshPorts = (refresh, effects) => ({ getRefreshToken: () => 'test-only-refresh', getAccessToken: () => 'test-only-access',
  axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => { effects.push('refresh'); return refresh.promise } } })
const r8SettleRefresh = async (h, actions) => {
  let settled = 0
  for (const a of actions) a.promise.then(() => { settled++ }, () => { settled++ })
  for (let i = 0; i < 30 && settled < actions.length; i++) {
    await flush()
    for (const item of h.n.all) if (!item.r8Held && !item.r8Answered && item.config.method === 'get' && item.config.url === '/mes/pro/production-release/get') {
      item.r8Answered = true
      const action = actions.find(a => a.receiptRead.config.isRequestContextCurrent === item.config.isRequestContextCurrent)
      assert.ok(action, 'replayed recovery preserves the original real request guard')
      assert.equal(item.config.url, '/mes/pro/production-release/get')
      item.success(action.receipt)
    }
    // A successful current receipt legitimately refreshes its actual page loader.
    for (const item of h.n.all) if (!item.r8Held && !item.r8Answered && item.config.method === 'get' && item.config.url.endsWith('/pqc/page')) {
      item.r8Answered = true; item.success({ list: [], total: 0 })
    }
  }
  assert.equal(settled, actions.length, 'all true refresh/recovery handlers must settle')
  await Promise.all(actions.map(a => a.promise))
}

for (const context of ['current', 'dialog', 'session'])
  for (const outcome of ['success', 'failure'])
    test(`R8 actual active PQC recovery refresh owner ${context}/${outcome}`, async t => {
      const refresh = r7Deferred(), refreshEffects = []
      const h = await r8Harness(r8RefreshPorts(refresh, refreshEffects)); t.after(h.dispose)
      const a = await h.begin(); a.receiptRead.unauthorized(); await flush()
      assert.deepEqual(refreshEffects, ['refresh'])
      if (context !== 'current') await h.change(context)
      const state = h.snapshot(), uiCount = h.ui.length
      if (outcome === 'success') refresh.resolve({ data: { code: 200, data: { accessToken: 'test-only-new', refreshToken: 'test-only-new-refresh' } } })
      else refresh.reject(Error('test-only refresh failure'))
      await r8SettleRefresh(h, [a])
      assert.equal(h.acceptedEvents.length, context === 'current' && outcome === 'success' ? 1 : 0)
      assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
      if (context !== 'current') { assert.equal(h.snapshot(), state); assert.equal(h.ui.length, uiCount); assert.deepEqual(h.authEffects, []) }
      else if (outcome === 'success') { assert.deepEqual(h.authEffects, ['setToken']); assert.equal(h.r.releaseOutcomeUncertain.value, false) }
      else { assert.deepEqual(h.authEffects, ['confirm']); assert.ok(h.r.releaseError.value.includes('正式回执无法证明')) }
    })

for (const context of ['current', 'ownerStale', 'queuedStale', 'bothStale'])
  for (const outcome of ['success', 'failure'])
    test(`R8 actual active PQC recovery refresh queue ${context}/${outcome}`, async t => {
      const refresh = r7Deferred(), refreshEffects = []
      const owner = await r8Harness(r8RefreshPorts(refresh, refreshEffects)); t.after(owner.dispose)
      const a = await owner.begin(); a.receiptRead.unauthorized(); await flush()
      const queued = await r8Harness({}, owner.n); t.after(queued.dispose)
      const b = await queued.begin(); b.receiptRead.unauthorized(); await flush()
      assert.deepEqual(refreshEffects, ['refresh'], 'real interceptor shares one refresh owner')
      const ownerStale = ['ownerStale', 'bothStale'].includes(context), queuedStale = ['queuedStale', 'bothStale'].includes(context)
      if (ownerStale) await owner.change('dialog')
      if (queuedStale) await queued.change('dialog')
      const ownerState = owner.snapshot(), queuedState = queued.snapshot(), ownerUi = owner.ui.length, queuedUi = queued.ui.length
      if (outcome === 'success') refresh.resolve({ data: { code: 200, data: { accessToken: 'test-only-new', refreshToken: 'test-only-new-refresh' } } })
      else refresh.reject(Error('test-only refresh failure'))
      await r8SettleRefresh(owner, [a, b])
      assert.equal(owner.acceptedEvents.length, !ownerStale && outcome === 'success' ? 1 : 0)
      assert.equal(queued.acceptedEvents.length, queuedStale ? 0 : 1)
      assert.equal(owner.n.all.filter(x => x.config.method === 'post').length, 2)
      assert.equal(owner.n.notices.length, 0)
      if (ownerStale) { assert.equal(owner.snapshot(), ownerState); assert.equal(owner.ui.length, ownerUi); assert.deepEqual(owner.authEffects, []) }
      if (queuedStale) { assert.equal(queued.snapshot(), queuedState); assert.equal(queued.ui.length, queuedUi) }
    })

// R9: formal receipt association must precede every successful event.
// Full installed Vue/Pinia component setup and real wrapper/interceptors are reused.
const r9Begin = async (h, path, expectedVersion = 3) => {
  h.r.selectedRow.value.version = expectedVersion
  if (path === 'recovery') return h.begin()
  const promise = h.r.submitRelease(), write = await h.n.next()
  write.r8Answered = true
  assert.equal(write.config.method, 'post')
  assert.equal(write.config.url, '/mes/pro/production-release/pqc/approve')
  const data = r7Body(write)
  const receipt = { applicationId: data.applicationId, pqcReleaseWorkTaskId: data.pqcReleaseWorkTaskId,
    decision: 'APPROVE', status: 'MANAGER_RELEASE_PENDING', reportUploadTasks: [], version: 5,
    signatureId: '501', decidedBy: '7', decidedAt: '2026-10-09T08:00:00Z', batchExecutionId: '999',
    sourceSnapshotHash: 'a'.repeat(64), decisionIdempotencyKey: data.idempotencyKey,
    decisionPayloadHash: r7BackendHash(data), udiControlDocumentNo: data.udiControlDocumentNo }
  return { promise, write, receipt, scopeKey: h.r.releaseRequestScopeKey(h.row) }
}
const r9KeyMap = h => {
  run('globalThis.r9OriginalKeys = releaseIdempotencyKeys', h.r)
  return h.r.r9OriginalKeys
}
const r9Receipt = (a, path, receipt) => (path === 'direct' ? a.write : a.receiptRead).success(receipt)
const r9AssertEvents = (h, a, valid) => {
  assert.equal(h.acceptedEvents.length, valid ? 1 : 0, 'formal verification precedes accepting an original successful event')
  assert.equal(h.invalidation.revision('EDHR', 'TODO'), valid ? 1 : 0)
  assert.equal(h.invalidation.revision('EDHR', 'DONE'), valid ? 1 : 0)
  assert.equal(h.r.releaseRequestStates.get(a.scopeKey), valid ? 'CONFIRMED' : 'UNCERTAIN')
  if (valid) {
    assert.equal(h.acceptedEvents[0].identity.sourceTaskId, h.row.pqcReleaseWorkTaskId)
    assert.equal(h.acceptedEvents[0].session.loginUserId, '7')
    assert.equal(h.acceptedEvents[0].session.tenantId, '1')
  } else assert.equal(r9KeyMap(h).get(a.scopeKey), r7Body(a.write).idempotencyKey, 'uncertain request keeps the original key')
}
const r9Settle = async (h, a, path, context, receipt, valid, listFailure = false) => {
  const state = h.snapshot(), uiCount = h.ui.length
  const reads = h.n.all.filter(x => x.config.method === 'get').length
  r9Receipt(a, path, receipt)
  let settled = false
  a.promise.then(() => { settled = true }, () => { settled = true })
  for (let i = 0; i < 30 && !settled; i++) {
    await flush()
    for (const refresh of h.n.all) if (!refresh.r8Held && !refresh.r8Answered && refresh.config.method === 'get') {
      assert.equal(refresh.config.url, '/mes/pro/production-release/pqc/page')
      refresh.r8Answered = true
      if (listFailure) refresh.fail('business'); else refresh.success({ list: [], total: 0 })
    }
  }
  assert.equal(settled, true, 'actual submit handler naturally settles including any incorrectly dispatched list reads')
  await a.promise
  r9AssertEvents(h, a, valid)
  assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1)
  assert.equal(h.n.all.filter(x => x.config.url === '/mes/pro/production-release/get').length, path === 'recovery' ? 1 : 0, 'invalid direct envelopes never automatically recover')
  if (context !== 'current') {
    assert.equal(h.snapshot(), state); assert.equal(h.ui.length, uiCount)
    assert.equal(h.n.all.filter(x => x.config.method === 'get').length, reads)
  } else if (!valid) {
    assert.equal(h.r.releaseOutcomeUncertain.value, true)
    assert.equal(h.r.releaseSubmitting.value, false)
    assert.equal(h.r.releaseResult.value, undefined)
    assert.ok(h.r.releaseError.value.includes(path === 'recovery' ? '正式回执无法证明' : '结果无法确认'))
    if (Number.isSafeInteger(receipt.version) && receipt.version > 0 && receipt.version !== 5) assert.ok(h.r.releaseError.value.includes('版本'))
    if (path === 'direct') assert.equal(h.r.releaseForm.signaturePassword, '')
    else {
      assert.ok(h.r.releaseError.value.includes('写入错误：timeout'))
      assert.ok(h.r.releaseError.value.includes('回执错误：'))
    }
    assert.equal(h.ui.some(x => x[0] === 'success'), false)
  } else {
    assert.equal(h.r.releaseResult.value.decisionIdempotencyKey, r7Body(a.write).idempotencyKey)
    assert.equal(h.r.releaseOutcomeUncertain.value, false)
    assert.equal(h.r.releaseSubmitting.value, false)
    if (listFailure) {
      assert.ok(h.r.releaseError.value.includes('生产放行已成功，列表刷新失败'))
      assert.ok(h.r.loadError.value.includes('formal business rejection'))
    }
  }
  await h.r.submitRelease()
  if (!['session', 'tenant'].includes(context)) {
    h.r.openReleaseDialog({ ...h.row }); h.r.releaseForm.signaturePassword = 'test-only-retry'
    h.r.releaseForm.udiControlDocumentNo = 'different-new-draft'
    await h.r.submitRelease()
  }
  assert.equal(h.n.all.filter(x => x.config.method === 'post').length, 1, 'same original scope cannot be resubmitted after reopen')
  r9AssertEvents(h, a, valid)
  assert.equal(h.n.notices.length, 0); assert.deepEqual(h.authEffects, [])
}
for (const field of ['applicationId', 'pqcReleaseWorkTaskId', 'decision', 'status', 'signatureId', 'batchExecutionId', 'decidedAt', 'sourceSnapshotHash', 'decisionIdempotencyKey', 'decisionPayloadHash', 'decidedBy', 'udiControlDocumentNo'])
  for (const context of ['current', 'dialog'])
    test(`R9 actual active PQC invalid direct ${field}/${context}`, async t => {
      const h = await r8Harness(); t.after(h.dispose); const a = await r9Begin(h, 'direct')
      if (context !== 'current') await h.change(context)
      const receipt = { ...a.receipt }
      receipt[field] = field === 'decisionPayloadHash' ? 'b'.repeat(64) : field === 'sourceSnapshotHash' ? 'wrong-hash' : ['applicationId', 'pqcReleaseWorkTaskId', 'decidedBy'].includes(field) ? '8' : 'wrong'
      await r9Settle(h, a, 'direct', context, receipt, false)
    })
const r9VersionCases = [['unchanged', 3], ['only-one-CAS', 4], ['formal-two-CAS', 5], ['later', 6], ['unrelated', 999], ['null', null], ['absent', undefined], ['string', '5'], ['fraction', 4.5], ['max-safe', Number.MAX_SAFE_INTEGER], ['unsafe', Number.MAX_SAFE_INTEGER + 1], ['NaN', NaN]]
for (const path of ['direct', 'recovery'])
  for (const context of ['current', 'dialog'])
    for (const [name, version] of r9VersionCases)
      test(`R9 actual active PQC receipt version ${path}/${context}/${name}`, async t => {
        const h = await r8Harness(); t.after(h.dispose); const a = await r9Begin(h, path)
        if (context !== 'current') await h.change(context)
        await r9Settle(h, a, path, context, { ...a.receipt, version }, name === 'formal-two-CAS')
      })
for (const path of ['direct', 'recovery'])
  for (const context of ['route', 'routeABA', 'source', 'task', 'version', 'deactivate', 'unmount'])
    test(`R9 actual active PQC invalid version after ${path}/${context}`, async t => {
      const h = await r8Harness(); t.after(h.dispose); const a = await r9Begin(h, path)
      await h.change(context)
      await r9Settle(h, a, path, context, { ...a.receipt, version: 3 }, false)
    })
for (const context of ['current', 'route', 'routeABA', 'dialog', 'source', 'task', 'version', 'deactivate', 'unmount', 'session', 'tenant'])
  test(`R9 actual active PQC valid direct original receipt ${context}`, async t => {
    const h = await r8Harness(); t.after(h.dispose); const a = await r9Begin(h, 'direct')
    if (context !== 'current') await h.change(context)
    await r9Settle(h, a, 'direct', context, a.receipt, !['session', 'tenant'].includes(context))
  })
for (const path of ['direct', 'recovery'])
  test(`R9 actual active PQC confirmed ${path} survives list read failure`, async t => {
    const h = await r8Harness(); t.after(h.dispose); const a = await r9Begin(h, path)
    await r9Settle(h, a, path, 'current', a.receipt, true, true)
  })
for (const path of ['direct', 'recovery'])
  for (const [name, expectedVersion, version, valid] of [
    ['safe-two-CAS-limit', Number.MAX_SAFE_INTEGER - 2, Number.MAX_SAFE_INTEGER, true],
    ['unsafe-sum-one-short', Number.MAX_SAFE_INTEGER - 1, Number.MAX_SAFE_INTEGER, false],
    ['unsafe-sum-at-limit', Number.MAX_SAFE_INTEGER, Number.MAX_SAFE_INTEGER, false]
  ]) test(`R9 actual active PQC frozen version boundary ${path}/${name}`, async t => {
    const h = await r8Harness(); t.after(h.dispose); const a = await r9Begin(h, path, expectedVersion)
    assert.equal(r7Body(a.write).expectedVersion, expectedVersion)
    await r9Settle(h, a, path, 'current', { ...a.receipt, version }, valid)
  })

// R10: an HTTP 5xx and a page remount cannot release an unresolved write scope.
const r10BackendHash = (data, actorId) => {
  const fields = ['APPROVE', data.applicationId, data.pqcReleaseWorkTaskId, String(data.expectedVersion), actorId,
    data.approvalOpinion ?? null, data.udiControlDocumentNo]
  const hash = r7CreateHash('sha256')
  for (const field of fields) {
    const encoded = field === null ? null : Buffer.from(field, 'utf8'), size = Buffer.alloc(4)
    size.writeInt32BE(encoded === null ? -1 : encoded.length)
    hash.update(size); if (encoded) hash.update(encoded)
  }
  return hash.digest('hex')
}
const r10Receipt = (write, actorId = '7') => {
  const data = r7Body(write)
  return { applicationId: data.applicationId, pqcReleaseWorkTaskId: data.pqcReleaseWorkTaskId,
    decision: 'APPROVE', status: 'MANAGER_RELEASE_PENDING', reportUploadTasks: [], version: data.expectedVersion + 2,
    signatureId: '501', decidedBy: actorId, decidedAt: '2026-10-09T08:00:00Z', batchExecutionId: '999',
    sourceSnapshotHash: 'a'.repeat(64), decisionIdempotencyKey: data.idempotencyKey,
    decisionPayloadHash: r10BackendHash(data, actorId), udiControlDocumentNo: data.udiControlDocumentNo }
}
const r10MountPage = async h => {
  const runtime = { ...h.r }
  const host = r7Host()
  const component = { name: 'R10RemountedActualPqcReleasePage', setup() {
    run(withoutImports(r7Script('src/views/mes/pro/production-release/PqcProductionReleasePage.vue')) +
      '\nObject.assign(globalThis, { openReleaseDialog, submitRelease, releaseRequestScopeKey, releaseRequestStates, releaseOutcomeUncertain, releaseError, releaseForm })', runtime)
    return () => r7Vue.h('div')
  } }
  const app = host.renderer.createApp(component)
  app.mount(host.root)
  const initial = await h.n.next()
  assert.equal(initial.config.method, 'get')
  initial.success({ list: [], total: 0 })
  await flush()
  return { runtime, unmount: () => app.unmount() }
}
const r10Credentials = runtime => {
  runtime.releaseForm.signaturePassword = 'test-only-signature'
  runtime.releaseForm.udiControlDocumentNo = '中文🔬UDI'
  runtime.releaseForm.approvalOpinion = '正式意见🔬'
}
const r10NextWrite = async h => {
  for (let i = 0; i < 30; i++) {
    await flush()
    for (const item of h.n.all) if (!item.r8Answered && !item.r8Held && item.config.method === 'get') {
      item.r8Answered = true
      item.success({ list: [], total: 0 })
    }
    const write = h.n.all.find(item => item.config.method === 'post' && !item.r8Answered)
    if (write) { write.r8Answered = true; return write }
  }
  assert.fail('the active PQC submit handler did not dispatch its POST')
}

test('R10 actual active PQC HTTP 503 stays uncertain and blocks same-scope retry', async t => {
  const h = await r8Harness(); t.after(h.dispose)
  const firstPromise = h.r.submitRelease(), firstWrite = await h.n.next()
  assert.equal(firstWrite.config.method, 'post')
  const firstBody = r7Body(firstWrite), scopeKey = h.r.releaseRequestScopeKey(h.row)
  const firstKey = firstBody.idempotencyKey
  firstWrite.fail('http')
  await firstPromise
  const after503 = {
    state: h.r.releaseRequestStates.get(scopeKey),
    key: r9KeyMap(h).get(scopeKey),
    uncertain: h.r.releaseOutcomeUncertain.value,
    message: h.r.releaseError.value,
    events: h.acceptedEvents.length
  }
  h.r.openReleaseDialog({ ...h.row })
  r10Credentials(h.r)
  const retryPromise = h.r.submitRelease()
  for (let i = 0; i < 25; i++) await flush()
  const posts = h.n.all.filter(item => item.config.method === 'post')
  let retryBody
  if (posts.length > 1) {
    retryBody = r7Body(posts[1])
    posts[1].fail('business')
  }
  await retryPromise
  const observed = {
    state: after503.state,
    key: after503.key,
    uncertain: after503.uncertain,
    userMessageIsUncertainAndReadOnly: /(结果无法确认|尚未确认|结果不确定)/.test(after503.message) && /只读/.test(after503.message),
    events: after503.events,
    retryPosts: posts.length - 1,
    retryKey: retryBody?.idempotencyKey,
    retryBodyMatchesOriginal: retryBody ? JSON.stringify(retryBody) === JSON.stringify(firstBody) : null
  }
  assert.deepEqual(observed, {
    state: 'UNCERTAIN', key: firstKey, uncertain: true,
    userMessageIsUncertainAndReadOnly: true, events: 0, retryPosts: 0,
    retryKey: undefined, retryBodyMatchesOriginal: null
  })
  assert.equal(h.n.all.filter(item => item.config.method === 'post').length, 1)
})

test('R10 actual active PQC remount keeps the original scope until its formal receipt resolves', async t => {
  const h = await r8Harness(); t.after(h.dispose)
  const firstPromise = h.r.submitRelease(), firstWrite = await h.n.next()
  assert.equal(firstWrite.config.method, 'post')
  const firstBody = r7Body(firstWrite), firstKey = firstBody.idempotencyKey
  const scopeKey = h.r.releaseRequestScopeKey(h.row)
  await h.change('unmount')
  const remounted = await r10MountPage(h)
  t.after(remounted.unmount)
  remounted.runtime.openReleaseDialog({ ...h.row })
  r10Credentials(remounted.runtime)
  const remountUncertain = remounted.runtime.releaseOutcomeUncertain.value
  const remountMessage = remounted.runtime.releaseError.value
  const retryPromise = remounted.runtime.submitRelease()
  for (let i = 0; i < 25; i++) await flush()
  const posts = h.n.all.filter(item => item.config.method === 'post')
  if (posts.length > 1) {
    posts[1].fail('business')
    await retryPromise
  } else await retryPromise
  firstWrite.success(r10Receipt(firstWrite))
  await firstPromise
  const observed = {
    posts: posts.length,
    originalBody: r7Body(firstWrite),
    originalKey: firstKey,
    remountUncertain,
    remountMessage,
    finalState: remounted.runtime.releaseRequestStates.get(scopeKey),
    events: h.acceptedEvents.length,
    todoRevision: h.invalidation.revision('EDHR', 'TODO'),
    doneRevision: h.invalidation.revision('EDHR', 'DONE')
  }
  assert.equal(observed.posts, 1, 'a same-session remount cannot dispatch a second POST')
  assert.deepEqual(observed.originalBody, firstBody, 'the formal result resolves the original frozen body')
  assert.equal(observed.originalKey, firstBody.idempotencyKey)
  assert.equal(observed.remountUncertain, true)
  assert.match(observed.remountMessage, /(尚未确认|结果无法确认|结果不确定)/)
  assert.equal(observed.finalState, 'CONFIRMED')
  assert.equal(observed.events, 1)
  assert.equal(observed.todoRevision, 1)
  assert.equal(observed.doneRevision, 1)
})

test('R10 actual active PQC isolates another session from an unresolved original request', async t => {
  const h = await r8Harness(); t.after(h.dispose)
  const originalPromise = h.r.submitRelease(), originalWrite = await h.n.next()
  originalWrite.r8Answered = true
  const originalBody = r7Body(originalWrite), originalScope = h.r.releaseRequestScopeKey(h.row)
  await h.change('session')
  const newScope = h.r.releaseRequestScopeKey(h.row)
  h.r.openReleaseDialog({ ...h.row })
  r10Credentials(h.r)
  await h.settleBackground()
  await flush()
  const nextPromise = h.r.submitRelease(), nextWrite = await r10NextWrite(h)
  assert.equal(nextWrite.config.method, 'post')
  const nextBody = r7Body(nextWrite)
  nextWrite.success(r10Receipt(nextWrite, '8'))
  await h.settleBackground()
  await nextPromise
  originalWrite.success(r10Receipt(originalWrite))
  await originalPromise
  assert.notEqual(originalScope, newScope)
  assert.notEqual(originalBody.idempotencyKey, nextBody.idempotencyKey)
  assert.equal(h.n.all.filter(item => item.config.method === 'post').length, 2)
  assert.equal(h.r.releaseRequestStates.get(originalScope), 'UNCERTAIN')
  assert.equal(h.r.releaseRequestStates.get(newScope), 'CONFIRMED')
  assert.equal(h.acceptedEvents.length, 1)
  assert.equal(h.acceptedEvents[0].session.loginUserId, '8')
})
