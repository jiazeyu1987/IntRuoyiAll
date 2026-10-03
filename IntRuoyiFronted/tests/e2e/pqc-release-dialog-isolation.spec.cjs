const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const { ref, reactive, computed, watch } = require('vue')
const deferred = () => { let resolve, reject; const promise = new Promise((a,b)=>{resolve=a;reject=b}); return {promise,resolve,reject} }
const tick = async () => { for(let i=0;i<8;i++) await Promise.resolve() }
function execute(relative, names, context, exports) {
 const text=fs.readFileSync(path.resolve(__dirname,'../..',relative),'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
 const file=ts.createSourceFile('component.ts',text,ts.ScriptTarget.Latest,true)
 const selected=file.statements.filter(s=>ts.isVariableStatement(s)?s.declarationList.declarations.some(d=>names.includes(d.name.getText(file))):ts.isExpressionStatement(s)&&((names[0].endsWith('DialogVisible')&&s.getText(file).startsWith('watch(')&&s.getText(file).includes(names[0]))||s.getText(file).startsWith('onBeforeUnmount(')))
 const unmount=[];Object.assign(context,{ref,reactive,computed,watch,onBeforeUnmount:fn=>unmount.push(fn)})
 const script=selected.map(s=>s.getText(file)).join('\n')+'\nglobalThis.api={'+exports.join(',')+'}'
 vm.runInNewContext(ts.transpileModule(script,{compilerOptions:{target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.None}}).outputText,context)
 return {...context.api,unmount:()=>unmount.forEach(fn=>fn())}
}

const row=id=>({applicationId:id,pqcReleaseWorkTaskId:'task'+id,version:1})
const receipt=id=>({applicationId:id,decision:'APPROVE',status:'REPORT_UPLOAD_PENDING',batchExecutionId:'batch'+id,signatureId:'sig'})
function harness(overrides={}) {
 const messages=[]
 const context={message:{error:x=>messages.push(x),success:x=>messages.push(x),warning:x=>messages.push(x)},crypto:{randomUUID:()=> 'key'},activeView:ref('PENDING'),queryParams:reactive({pageNo:1}),getList:async()=>{},getPqcProductionRelease:async id=>receipt(id),approvePqcProductionRelease:async data=>receipt(data.applicationId),resolveErrorMessage:(e,d)=>e?.message||d,PQC_RELEASE_VIEW_CONCESSION_RELEASED:'CONCESSION',PQC_RELEASE_VIEW_RELEASED:'RELEASED',PQC_RELEASE_VIEW_REWORKED:'REWORKED',PQC_RELEASE_VIEW_VOIDED:'VOIDED',...overrides}
 const names=['releaseDialogVisible','releaseSubmitting','releaseError','selectedRow','releaseResult','releaseOutcomeUncertain','releaseIdempotencyKeys','releaseDialogGeneration','pqcReleaseRouteGeneration','captureReleaseContext','openReleaseDialog','resetReleaseDialog','getOrCreateReleaseIdempotencyKey','isDefinitiveReleaseBusinessFailure','assertReleasedReceipt','applyReleaseSuccess','recoverUncertainRelease','submitRelease','releaseForm']
 const h=execute('src/views/mes/pro/production-release/PqcProductionReleasePage.vue',names,context,names.filter(n=>!['releaseDialogGeneration','pqcReleaseRouteGeneration','captureReleaseContext'].includes(n)))
 const open=id=>{h.openReleaseDialog(row(id));Object.assign(h.releaseForm,{signaturePassword:'p',udiControlDocumentNo:'udi',approvalOpinion:id})}
 return {...h,open,messages,context}
}
for(const same of [false,true]) test(`PQC old success cannot complete ${same?'reopened same':'different'} order`,async()=>{const d=deferred();const h=harness({approvePqcProductionRelease:()=>d.promise});h.open('A');const a=h.submitRelease();h.releaseDialogVisible.value=false;h.open(same?'A':'B');d.resolve(receipt('A'));await a;assert.equal(h.releaseResult.value,undefined);assert.equal(h.releaseForm.signaturePassword,'p');assert.equal(h.messages.length,0)})
test('PQC old error/finally do not clear current submitting state',async()=>{const a=deferred(),b=deferred();let n=0;const h=harness({approvePqcProductionRelease:()=>++n===1?a.promise:b.promise});h.open('A');const p=h.submitRelease();h.releaseDialogVisible.value=false;h.open('B');const q=h.submitRelease();a.reject({code:10,message:'old failure'});await p;assert.equal(h.releaseError.value,'');assert.equal(h.releaseSubmitting.value,true);b.resolve(receipt('B'));await q;assert.equal(h.releaseResult.value.applicationId,'B')})
test('PQC late recovery cannot replace current dialog',async()=>{const read=deferred();const h=harness({approvePqcProductionRelease:async()=>{throw Error('network')},getPqcProductionRelease:()=>read.promise});h.open('A');const p=h.submitRelease();await tick();h.releaseDialogVisible.value=false;h.open('B');read.resolve(receipt('A'));await p;assert.equal(h.releaseResult.value,undefined);assert.equal(h.releaseForm.signaturePassword,'p')})
test('PQC current failure remains visible and ordinary success works',async()=>{const h=harness({approvePqcProductionRelease:async()=>{throw {code:10,message:'current failure'}}});h.open('A');await h.submitRelease();assert.equal(h.releaseError.value,'current failure');assert.equal(h.releaseSubmitting.value,false);const ok=harness();ok.open('A');await ok.submitRelease();assert.equal(ok.releaseResult.value.applicationId,'A')})
test('PQC unmount invalidates success',async()=>{const d=deferred();const h=harness({approvePqcProductionRelease:()=>d.promise});h.open('A');const p=h.submitRelease();h.unmount();d.resolve(receipt('A'));await p;assert.equal(h.releaseResult.value,undefined);assert.equal(h.messages.length,0)})
test('PQC late list refresh cannot announce old completion',async()=>{const d=deferred();const h=harness({getList:()=>d.promise});h.open('A');const p=h.submitRelease();await tick();h.releaseDialogVisible.value=false;h.open('B');d.resolve();await p;assert.equal(h.messages.length,0);assert.equal(h.releaseResult.value,undefined)})

test('PQC late recovery failure cannot mark new session uncertain',async()=>{const d=deferred();const h=harness({approvePqcProductionRelease:async()=>{throw Error('network')},getPqcProductionRelease:()=>d.promise});h.open('A');const p=h.submitRelease();await tick();h.releaseDialogVisible.value=false;h.open('B');d.reject(Error('old read'));await p;assert.equal(h.releaseError.value,'');assert.equal(h.releaseOutcomeUncertain.value,false);assert.equal(h.messages.length,0)})
test('PQC actual list query discards closed dialog result and failure',async()=>{for(const fails of [false,true]){const d=deferred();let current=true;const context={loading:ref(false),loadError:ref(''),list:ref(['new']),total:ref(1),queryParams:reactive({pageNo:1,pageSize:10,workOrderCode:'',batchCode:''}),activeView:ref('PENDING'),getPqcProductionReleasePage:()=>d.promise,resolveErrorMessage:e=>e.message};const h=execute('src/views/mes/pro/production-release/PqcProductionReleasePage.vue',['getList','listRequestSequence'],context,['getList']);const p=h.getList(()=>current);current=false;fails?d.reject(Error('old')):d.resolve({list:['old'],total:2});await p;assert.equal(context.list.value[0],'new');assert.equal(context.loadError.value,'')}})
