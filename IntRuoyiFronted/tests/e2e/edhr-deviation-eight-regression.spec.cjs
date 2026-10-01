const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-deviation/DeviationDetail.vue'), 'utf8')
function harness(overrides = {}) {
 const messages = [], calls = []
 const context = { ref: value => ({value}), reactive: value => value, computed: fn => ({get value(){return fn()}}), watch:()=>{}, useRouter:()=>({push:()=>{}}), defineProps:()=>({id:1,readonly:false}), withDefaults:v=>v, ElMessage:{error:v=>messages.push(v),success:()=>{},warning:v=>messages.push(v)}, ElMessageBox:{confirm:async()=>{}}, getDeviation:async id=>({id,status:'OPEN'}), getDeviationHandling:async()=>undefined, saveDeviationHandling:async(id,body)=>{calls.push(body);return {id:1,deviationId:id,...body,contentVersion:body.expectedContentVersion+1,contentHash:'hash'}}, signDeviationHandling:async body=>calls.push(body), ...overrides }
 context.useMessage = () => ({confirm: (...args) => context.ElMessageBox.confirm(...args)})
 let script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'')
 script += '\nglobalThis.api={props,detail,handling,handlingForm,load,loadHandling,saveHandling,startHandling,signHandling,openSignDialog,signVisible,handlingSaving,closeHandling};'
 vm.runInNewContext(ts.transpileModule(script,{compilerOptions:{target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.None}}).outputText,context)
 return {...context.api,messages,calls}
}
test('save refreshes version and clears reason',async()=>{const h=harness();h.detail.value={id:1,status:'OPEN'};h.startHandling();await h.saveHandling();assert.equal(h.handlingForm.expectedContentVersion,1);h.handlingForm.revisionReason='revision';await h.saveHandling();assert.equal(h.calls[1].expectedContentVersion,1);assert.equal(h.handlingForm.expectedContentVersion,2);assert.equal(h.handlingForm.revisionReason,'')})
test('missing revision reason prevents submit',async()=>{const h=harness();h.handling.value={id:1,contentVersion:1};h.handlingForm.expectedContentVersion=1;await h.saveHandling();assert.equal(h.calls.length,0);assert.ok(h.messages.length)})
test('cleared date submits null',async()=>{const h=harness();h.handlingForm.completedAt=null;await h.saveHandling();assert.equal(h.calls.length,1);assert.equal(h.calls[0].completedAt,null)})
test('switch resets and ignores late detail',async()=>{let resolve;const h=harness({getDeviation:id=>id===1?new Promise(r=>resolve=r):Promise.resolve({id,status:'OPEN'})});h.handlingForm.rootCauseAnalysis='old';const old=h.load();h.props.id=2;await h.load();resolve({id:1,status:'OPEN'});await old;assert.equal(h.detail.value.id,2);assert.equal(h.handlingForm.rootCauseAnalysis,'');assert.equal(h.handlingForm.expectedContentVersion,0)})
test('switch ignores late handling',async()=>{let resolve;const h=harness({getDeviationHandling:id=>id===1?new Promise(r=>resolve=r):Promise.resolve(undefined)});h.detail.value={id:1,status:'OPEN'};const old=h.loadHandling();h.props.id=2;await h.load();resolve({id:9,deviationId:1,contentVersion:4,rootCauseAnalysis:'old'});await old;assert.equal(h.handling.value,undefined);assert.equal(h.handlingForm.rootCauseAnalysis,'')})
test('unsaved content cannot sign',async()=>{const h=harness();h.detail.value={id:1,status:'OPEN'};h.startHandling();await h.saveHandling();h.handlingForm.rootCauseAnalysis='new';h.openSignDialog('QA');assert.equal(h.signVisible.value,false);await h.signHandling();assert.equal(h.calls.length,1);assert.ok(h.messages.length)})
test('signature sends reviewed identity',async()=>{const h=harness();await h.saveHandling();await h.signHandling();assert.equal(h.calls[1].expectedContentVersion,1);assert.equal(h.calls[1].expectedContentHash,'hash')})
test('revision reason field exists',()=>assert.ok(source.includes('v-model="handlingForm.revisionReason"')))
test('single formal trace tab',()=>{const p=fs.readFileSync(path.join(root,'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),'utf8');assert.equal((p.match(/name="deviation"/g)||[]).length,1);assert.match(p,/<DeviationTracePane/);assert.match(p,/getDeviationBatchOptionsByActiveOrder/);assert.doesNotMatch(p,/nonconformanceOperationFacts/)})

test('old save completion cannot clear new save loading',async()=>{const pending=[];const h=harness({saveDeviationHandling:(id,body)=>new Promise(resolve=>pending.push(()=>resolve({id,deviationId:id,contentVersion:1,contentHash:'h'})))});const old=h.saveHandling();h.props.id=2;await h.load();const current=h.saveHandling();pending[0]();await old;assert.equal(h.handlingSaving.value,true);pending[1]();await current})
test('switch during close confirmation does not close another deviation',async()=>{let confirm;const closes=[];const h=harness({ElMessageBox:{confirm:()=>new Promise(r=>confirm=r)},closeDeviationHandling:async id=>closes.push(id)});await h.saveHandling();const close=h.closeHandling();h.props.id=2;await h.load();confirm();await close;assert.equal(closes.length,0)})
test('close rejects unsaved edits',async()=>{const closes=[];const h=harness({closeDeviationHandling:async id=>closes.push(id)});await h.saveHandling();h.handlingForm.rootCauseAnalysis='unsaved';await h.closeHandling();assert.equal(closes.length,0);assert.ok(h.messages.length)})

function traceHarness(getBatches) {
 const p=fs.readFileSync(path.join(root,'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),'utf8')
 const script=p.slice(p.indexOf('const deviationLoading ='),p.indexOf('const activeOrderStatusTagType'))
 const context={ref:value=>({value}),watch:()=>{},props:{detail:{activeOrderId:12}},activeTab:{value:'deviation'},getDeviationBatchOptionsByActiveOrder:getBatches}
 vm.runInNewContext(ts.transpileModule(script+'\nglobalThis.api={props,loadDeviationBatches,deviationLoading,deviationBatches,deviationError}',{compilerOptions:{target:ts.ScriptTarget.ES2020}}).outputText,context)
 return context.api
}
test('trace uses authoritative active order and preserves multiple formal batches',async()=>{const ids=[];const h=traceHarness(async id=>{ids.push(id);return [{batchExecutionId:1},{batchExecutionId:2}]});await h.loadDeviationBatches();assert.deepEqual(ids,[12]);assert.equal(h.deviationBatches.value.length,2);assert.equal(h.deviationError.value,'')})
test('trace distinguishes successful no-batch from failed query and invalid identity',async()=>{const empty=traceHarness(async()=>[]);await empty.loadDeviationBatches();assert.equal(empty.deviationError.value,'');const failed=traceHarness(async()=>{throw Error('network')});await failed.loadDeviationBatches();assert.ok(failed.deviationError.value);const invalid=traceHarness(async()=>{assert.fail('must not query')});invalid.props.detail=undefined;await invalid.loadDeviationBatches();assert.ok(invalid.deviationError.value)})
test('trace ignores a late previous order response',async()=>{let resolve;const h=traceHarness(id=>id===12?new Promise(r=>resolve=r):Promise.resolve([{batchExecutionId:20}]));const old=h.loadDeviationBatches();h.props.detail.activeOrderId=13;await h.loadDeviationBatches();resolve([{batchExecutionId:10}]);await old;assert.equal(h.deviationBatches.value[0].batchExecutionId,20)})

test('trace preserves exact Long active-order identity',async()=>{const ids=[];const h=traceHarness(async id=>{ids.push(id);return []});h.props.detail.activeOrderId='9007199254740993';await h.loadDeviationBatches();assert.deepEqual(ids,['9007199254740993'])})

test('formal deviation pane preserves exact Long batch identity',async()=>{
 const p=fs.readFileSync(path.join(root,'src/views/mes/pro/edhr/components/DeviationTracePane.vue'),'utf8')
 const ids=[];const context={ref:value=>({value}),watch:()=>{},defineProps:()=>({batchExecutionId:'9007199254740993'}),getDeviationPage:async q=>{ids.push(q.batchExecutionId);return {list:[],total:0}}}
 const script=p.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'')+'\nglobalThis.api={load}'
 vm.runInNewContext(ts.transpileModule(script,{compilerOptions:{target:ts.ScriptTarget.ES2020}}).outputText,context)
 await context.api.load();assert.deepEqual(ids,['9007199254740993'])
})
