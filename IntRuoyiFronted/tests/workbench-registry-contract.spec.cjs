const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const root = path.resolve(__dirname, '..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const evaluate = (source, deps = {}) => {
  const module = { exports: {} }
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  Function('require', 'module', 'exports', 'window', js)(id => {
    assert.ok(Object.hasOwn(deps, id), `unexpected import ${id}`)
    return deps[id]
  }, module, module.exports, {location:{origin:'https://local.test'}})
  return module.exports
}
const { createSourceRegistry } = evaluate(read('src/workbench/registryContract.ts'))
const contracts = evaluate(read('src/workbench/contracts.ts'))
const manifest = JSON.parse(read('src/workbench/manifest.json'))
let formalCalls = []
const units = fs.readdirSync(path.join(root, 'src/workbench-sources')).map(module =>
  evaluate(read(`src/workbench-sources/${module}/registration.ts`), {
    '@/workbench/contracts': contracts,
    '@/utils/exactJavaLong': evaluate(read('src/utils/exactJavaLong.ts')),
    '@/utils/edhrWorkTaskNavigation': { navigateToEdhrWorkTask: async (...args) => formalCalls.push(args) }
  }).default)
const clone = x => structuredClone(x)
const descriptors = units.flatMap(unit => unit.sources.map(source => source.descriptor))
const directory = () => ({ contractVersion: 1, registryRevision: manifest.registryRevision, sources: clone(descriptors) })

test('actual discovered five modules match both build manifests and keep labels/ranks', () => {
  const registry = createSourceRegistry(units, manifest)
  assert.deepEqual(JSON.parse(fs.readFileSync(path.join(root, '../IntRuoyiBackend/yudao-server/src/main/resources/workbench-manifest.json'), 'utf8')), manifest)
  assert.equal(registry.validateDirectory(directory()).sources.length, 5)
  assert.deepEqual(descriptors.map(d => [d.sourceId, d.sortRank]).sort((a,b) => a[1]-b[1]), [
    ['DCC_DISTRIBUTION',0],['DCC_TRAINING',1],['EDHR_WORK_TASK',2],['WORK_ORDER',3],['SHOWROOM_ASSIGNMENT',4]])
})
test('neutral Java API import whitelist and shared frontend consumers have no business source enum or policy/action switches', () => {
  const apiRoot = path.join(root, '../IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/api/workbench')
  for (const file of fs.readdirSync(apiRoot).filter(file => file.endsWith('.java'))) {
    const source = fs.readFileSync(path.join(apiRoot,file),'utf8')
    for (const imported of source.matchAll(/^import\s+([^;]+);/gm)) assert.match(imported[1],/^(?:java\.|lombok\.|com\.fasterxml\.jackson\.(?:annotation\.|core\.JsonGenerator$|databind\.(?:annotation\.JsonSerialize$|SerializerProvider$|ser\.std\.StdSerializer$))|cn\.iocoder\.yudao\.framework\.common\.(?:exception|util)\.)/)
  }
  for (const file of ['src/views/Profile/components/ProfileWorkbench.vue', 'src/store/modules/profileWorkbenchTodoBadge.ts', 'src/api/system/profileWorkbenchTodo/index.ts', 'src/workbench/registry.ts']) {
    assert.doesNotMatch(read(file), /DCC_DISTRIBUTION|DCC_TRAINING|EDHR_WORK_TASK|WORK_ORDER|SHOWROOM_ASSIGNMENT|dcc:controlled-file|mes:pro-work-order|ShowroomAdminAssignment/)
  }
  assert.match(read('src/workbench/registry.ts'), /import\.meta\.glob<[^\n]+workbench-sources\/\*\/registration\.ts[^\n]+eager: true/)
})
test('sixth module unit really registers and executes handler without central edits', async () => {
  let payload
  const sixth = { ownerModule: 'sixth', contractVersion: 1, sources: [{ descriptor: {
    sourceId: 'SIXTH_QUEUE', ownerModule: 'sixth', contractVersion: 1, sortRank: 5, label: '第六队列',
    taskType: '独立第六类别', policyId: 'sixth.policy', actionKind: 'sixth.open', payloadVersion: 1 },
    open: async (_router, value) => { payload = value } }] }
  const enabled = clone(manifest); enabled.modules.push({ ownerModule: 'sixth', enabled: true, sourceIds: ['SIXTH_QUEUE'] })
  const registry = createSourceRegistry([...units, sixth], enabled)
  registry.validateDirectory({ ...directory(), sources: [...descriptors, sixth.sources[0].descriptor] })
  const descriptor = sixth.sources[0].descriptor
  const context={contractVersion:3,registryRevision:manifest.registryRevision,sources:[descriptor.sourceId],queryVersion:'a'.repeat(43),contextEpoch:'sixth-epoch',visibilityVersion:'1',querySpec:{visibility:'visible'}}
  const api = evaluate(read('src/api/system/profileWorkbenchTodo/index.ts'), {
    '@/workbench/queryContract': {workbenchQueryContractVersion:3},
    '@/config/axios': {default:{get:async () => ({ ...context, rows:[{sourceId:descriptor.sourceId,taskKey:descriptor.sourceId+':opaque:work-A',businessId:'opaque:work-A',taskType:descriptor.taskType,source:descriptor.label,detail:'独立任务',statusLabel:'待办',createdAt:null,dueAt:null,action:{actionKind:descriptor.actionKind,payloadVersion:1,payload:{id:'9007199254740993'}}}],filteredTotal:1,businessTotal:1,hiddenTotal:0,effectivePageNo:1,pageSize:10,readAt:'2026-10-09T12:00:00'})}}
  })
  const page = await api.getProfileWorkbenchTodoPage(context,1,10,[descriptor])
  assert.equal(page.rows[0].taskType,'独立第六类别')
  assert.equal(page.rows[0].businessId,'opaque:work-A')
  await registry.require('SIXTH_QUEUE').open({}, { id: '9007199254740993' })
  assert.deepEqual(payload, { id: '9007199254740993' })
})
test('enabled missing unit fails', () => assert.throws(() => createSourceRegistry(units.slice(1), manifest)))
test('enabled missing source/handler fails', () => {
  const changed = units.map(unit => ({ ...unit, sources: [...unit.sources] }))
  changed[0].sources.pop()
  assert.throws(() => createSourceRegistry(changed, manifest))
  changed[0].sources = [{ ...units[0].sources[0], open: undefined }]
  assert.throws(() => createSourceRegistry(changed, manifest))
})
test('duplicate source/action/rank and unit fail separately', () => {
  for (const field of ['sourceId', 'actionKind', 'sortRank']) {
    const changed = units.map(unit => ({ ...unit, sources: unit.sources.map(s => ({ ...s, descriptor: { ...s.descriptor } })) }))
    const flat = changed.flatMap(unit => unit.sources)
    flat[1].descriptor[field] = flat[0].descriptor[field]
    assert.throws(() => createSourceRegistry(changed, manifest), field)
  }
  assert.throws(() => createSourceRegistry([...units, units[0]], manifest))
})
test('manifest alone disables compiled module; disabled request fails and authorized empty directory succeeds', () => {
  const disabled = clone(manifest); disabled.modules[0].enabled = false
  const registry = createSourceRegistry(units, disabled)
  assert.throws(() => registry.require('DCC_TRAINING'), /MODULE_DISABLED/)
  assert.equal(registry.validateDirectory({ ...directory(), sources: [] }).sources.length, 0)
})
test('local/remote contract/payload/revision mismatch and unknown directory source fail', () => {
  const registry = createSourceRegistry(units, manifest)
  for (const change of [d => d.contractVersion++, d => {d.registryRevision='other'},
    d => d.sources[0].payloadVersion++, d => {d.sources[0].sourceId='UNKNOWN'}, d => d.sources.push(d.sources[0])]) {
    const d = directory(); change(d); assert.throws(() => registry.validateDirectory(d))
  }
})
test('five real module handlers carry formal identities and MES invokes formal navigator', async () => {
  const registry = createSourceRegistry(units, manifest), pushes = [], router = { push: async route => pushes.push(route) }
  await registry.require('DCC_DISTRIBUTION').open(router, { controlledFileId:'9007199254740993', distributionId:'2', recipientId:'3' })
  await registry.require('DCC_TRAINING').open(router, { controlledFileId:'1', progressId:'4' })
  await registry.require('WORK_ORDER').open(router, { code:'WO-001' })
  await registry.require('SHOWROOM_ASSIGNMENT').open(router, { assignmentId:'5' })
  const payload = { id:'6', taskType:'FILL', batchExecutionId:'7', batchTaskId:'8' }
  await registry.require('EDHR_WORK_TASK').open(router, payload)
  assert.equal(pushes[0].params.id, '9007199254740993')
  assert.equal(pushes[0].query.recipientId, '3')
  assert.equal(pushes[1].params.progressId, '4')
  assert.equal(pushes[2].query.code, 'WO-001')
  assert.equal(pushes[3].query.assignmentId, '5')
  assert.deepEqual(formalCalls.at(-1).slice(0,2), [router, payload])
  assert.equal(formalCalls.at(-1)[2], 'https://local.test')
  await assert.rejects(() => registry.require('DCC_DISTRIBUTION').open(router, {}))
  await assert.rejects(() => registry.require('SHOWROOM_ASSIGNMENT').open(router, {assignmentId:'0'}))
})

test('actual eDHR navigator calls formal open for FILL/REWORK, preserves Long strings and selects MAIN/non-MAIN output', async () => {
  let opened, calls = [], pushes = []
  const nav = evaluate(read('src/utils/edhrWorkTaskNavigation.ts'), {
    '@/utils/exactJavaLong': evaluate(read('src/utils/exactJavaLong.ts')),
    '@/api/mes/pro/edhr/batchExecution': { openEdhrBatchTask: async data => { calls.push(data); return opened } }
  })
  const router = { push: async route => pushes.push(route) }, origin = 'https://local.test'
  opened = { executionId: '999', workTaskId: '9007199254740993', executionPageQuery: { formSlotType:'MAIN' } }
  await nav.navigateToEdhrWorkTask(router, { id:'9007199254740993', taskType:'FILL', batchExecutionId:'9007199254740994', batchTaskId:'9007199254740995' }, origin)
  assert.deepEqual(calls[0], { batchExecutionId:'9007199254740994', taskId:'9007199254740995', workTaskId:'9007199254740993' })
  assert.equal(pushes[0].path, nav.EDHR_EXECUTION_FORM_PATH)
  opened = { formCenterInstanceId:'1', formTemplateId:'2', taskId:'8', workTaskId:'6', executionPageQuery: { formSlotType:'AUXILIARY' } }
  await nav.navigateToEdhrWorkTask(router, { id:'6', taskType:'REWORK', batchExecutionId:'7', batchTaskId:'8', actionUrl:'/mes/pro/feedback/edhr-batch-execution/detail?batchExecutionId=7&batchTaskId=8&workTaskId=6' }, origin)
  assert.equal(pushes[1].path, nav.EDHR_BATCH_EXECUTION_DETAIL_PATH)
  assert.equal(pushes[1].query.openRouteForm, '1')
  const before = calls.length
  await assert.rejects(() => nav.navigateToEdhrWorkTask(router, { id:Number('9007199254740993'), taskType:'FILL', batchExecutionId:'7', batchTaskId:'8' }, origin), /精确标识/)
  assert.equal(calls.length, before)
  await assert.rejects(() => nav.navigateToEdhrWorkTask(router, { id:'6', taskType:'RELEASE_APPROVE', batchExecutionId:'7', businessScopeType:'RELEASE_TRANSACTION', businessScopeId:'8', actionUrl:'/mes/pro/feedback/edhr-batch-execution?action=marketRelease&batchExecutionId=9' }, origin), /正式任务身份不一致/)
})

test('actual Vue directory coalesces page/badge and rejects stale authorization; missing does not mean zero',async()=>{
 const {fixture,dir}=require('./helpers/workbench-s3-fixture.cjs'),f=fixture(),waiting=[]
 f.setTransport(r=>new Promise((resolve,reject)=>waiting.push({resolve,reject})))
 assert.throws(()=>f.registry.requireWorkbenchDirectory())
 const first=f.registry.refreshWorkbenchDirectory(),same=f.registry.refreshWorkbenchDirectory()
 assert.equal(first,same);assert.equal(f.requests.length,1)
 waiting[0].resolve(dir);await first
 const stale=f.registry.refreshWorkbenchDirectory();f.user.getPermissions.push('changed');waiting[1].resolve(dir)
 await assert.rejects(stale,/上下文已变化/);assert.throws(()=>f.registry.requireWorkbenchDirectory())
 const failed=f.registry.refreshWorkbenchDirectory();waiting[2].reject(new Error('backend failed'));await assert.rejects(failed,/backend failed/)
 assert.equal(f.registry.workbenchDirectoryState.value,'error');assert.equal(f.registry.workbenchDirectoryError.value,'backend failed')
 const empty=f.registry.refreshWorkbenchDirectory();waiting[3].resolve({...dir,sources:[]});await empty
 assert.equal(f.registry.requireWorkbenchDirectory().sources.length,0);f.scope.stop()
})
test('actual Pinia badge old directory success/error never starts count or commits',async()=>{
 const {fixture,deferred,tick}=require('./helpers/workbench-s3-fixture.cjs')
 for(const reject of [true,false]){
  const f=fixture(),gate=deferred();let once=true
  f.setTransport((r,next)=>{if(once&&r.url.endsWith('/sources')){once=false;return gate.promise.then(()=>{if(reject)throw new Error('old error');return next(r)})}return next(r)})
  const old=f.badge.refreshTodoTotal();f.context.invalidateWorkbenchContext();await f.badge.refreshTodoTotal()
  gate.resolve();await old;assert.equal(f.badge.todoTotal,3);assert.equal(f.badge.loaded,true);assert.equal(f.badge.error,'')
  assert.equal(f.requests.filter(r=>r.url.endsWith('/count')).length,1);f.scope.stop()
 }
})
test('actual full Vue page handler owns directory generation identity and query',async()=>{
 const {fixture,deferred}=require('./helpers/workbench-s3-fixture.cjs'),f=fixture(),gate=deferred();let once=true
 f.setTransport((r,next)=>{if(once&&r.url.endsWith('/sources')){once=false;return gate.promise.then(()=>{throw new Error('old error')})}return next(r)})
 const old=f.page.loadWorkbench();f.identity(2);await f.page.loadWorkbench();gate.resolve();await old
 assert.equal(f.page.todoRows.value.length,1);assert.deepEqual(f.page.loadErrorMessages.value,[])
 const wait=deferred();f.setTransport((r,next)=>r.url.endsWith('/sources')?wait.promise.then(()=>next(r)):next(r))
 const changed=f.page.loadWorkbench();const before=f.requests.filter(r=>r.url.endsWith('/page')).length
 f.page.queryParams.pageNo=3;wait.resolve();await changed
 assert.equal(f.requests.filter(r=>r.url.endsWith('/page')).length,before);f.scope.stop()
})
