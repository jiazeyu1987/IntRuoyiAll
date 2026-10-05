const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = p => fs.readFileSync(p, 'utf8')
const transpile = s => ts.transpileModule(s, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const FILE='9007199254740993',MASTER='9007199254740994',REF='65592',HASH='a'.repeat(64),PRE='b'.repeat(64)
const preview={fileId:FILE,masterId:MASTER,versionRefId:REF,tenantId:'1',versionNo:'A/1',dccStatus:'ACTIVE',canonicalStatus:'FINALIZING',domainStatus:'FINALIZING',processInstanceId:'actual-round',controlledTime:'2026-10-05T12:00:00',activatedTime:'2026-10-05T12:00:01',approvedTime:'2026-10-05T11:59:00',publishedTime:'2026-10-05T12:00:00',effectiveDate:'2026-10-05',publishedFileId:'100',stampedFileId:'101',signatureIds:['406','408'],latestControlledFileId:FILE,currentActiveControlledFileId:FILE,sourceFactsHash:HASH,preimageHash:PRE,canRepair:true,expectedTargetStatus:'ACTIVE',expectedActions:['COMPLETE_CONTROL','ACTIVATE_CONTROLLED']}
const receipt={fileId:FILE,masterId:MASTER,versionRefId:REF,processInstanceId:'actual-round',status:'REPAIRED',canonicalStatus:'ACTIVE',domainStatus:'ACTIVE',sourceFactsHash:HASH,preimageHash:PRE,idempotencyKey:'actual-key',auditEventId:'700',repairedAt:'2026-10-05T12:01:00'}
function api({ result=preview, post=receipt, failure }={}){
 const calls=[], c={exports:{},Error,require:()=>({default:{get:async req=>{calls.push(req);return result},post:async req=>{calls.push(req);if(failure)throw new Error(failure);return post}}})}
 vm.runInNewContext(transpile(read('src/api/dcc/controlledFile/lifecycleProjectionRepair.ts')),c)
 return{...c.exports,calls}
}
function child({allowed=true,canRepair=true,confirm=async()=>{},write,readPreview}={}){
 const s=read('src/views/dcc/controlled-file/detail/DetailLifecycleProjectionRepairDialog.vue'),{descriptor}=parse(s)
 let liveContext='tenant1-user25-file'+FILE
 const props=vue.reactive({modelValue:true,fileId:FILE,allowed,contextKey:liveContext,readContextKey:()=>liveContext}),events=[],errors=[]
 const c={exports:{},Error,ref:vue.ref,computed:vue.computed,watch:()=>{},onBeforeUnmount:()=>{},defineProps:()=>props,defineEmits:()=>((...args)=>events.push(args)),ElMessageBox:{confirm},generateUUID:()=> 'actual-key',getLifecycleProjectionRepairPreview:readPreview||(async()=>({...preview,canRepair})),repairLifecycleProjection:write||(async(_id,payload)=>({...receipt,idempotencyKey:payload.idempotencyKey})),useMessage:()=>({success:()=>{}})}
 const scriptAST=ts.createSourceFile('dialog.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
 const actualScript=scriptAST.statements.filter(n=>!ts.isImportDeclaration(n)).map(n=>n.getText(scriptAST)).join('\n')
 vm.runInNewContext(transpile(actualScript+'\nexports.state={preview,reason,error,canSubmit,saving};exports.load=load;exports.submit=submit;'),c)
 return{...c.exports,props,events,errors,descriptor,setLiveContext:value=>{liveContext=value}}
}
test('actual management entry is absent in readonly contexts and requires all existing permissions plus doc-control role',()=>{
 const {descriptor}=parse(read('src/views/dcc/controlled-file/detail/index.vue')),ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
 const get=n=>{const node=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===n));assert.ok(node,'missing actual '+n);return node.getText(ast)}
 for(const options of [{viewer:true},{management:false},{role:false},{permission:false},{}]){
  const c={exports:{},computed:vue.computed,viewerMode:vue.ref(!!options.viewer),showDetailManagementActions:vue.ref(options.management!==false),fileDetail:vue.ref({id:FILE}),userStore:{getRoles:options.role===false?[]:['doc_control']},DOC_CONTROL_ROLE_CODE:'doc_control',checkPermi:p=>{assert.equal(p.length,1);assert.ok(['dcc:controlled-file:query','dcc:controlled-file:update','dcc:controlled-file:category:manage'].includes(p[0]));return options.permission!==false},lifecycleProjectionRepairVisible:vue.ref(false),message:{warning:()=>{}}}
  vm.runInNewContext(transpile(get('canInspectLifecycleProjection')+'\n'+get('openLifecycleProjectionRepair')+'\nexports.can=canInspectLifecycleProjection;exports.open=openLifecycleProjectionRepair;'),c)
  const expected=Object.keys(options).length===0;assert.equal(c.exports.can.value,expected);c.exports.open();assert.equal(c.lifecycleProjectionRepairVisible.value,expected)
 }
})
test('actual API preview/POST preserve precise Long identities, hashes and formal receipt without actor or time injection',async()=>{
 const h=api();const p=await h.getLifecycleProjectionRepairPreview(FILE);assert.equal(p.fileId,FILE)
 const command={masterId:MASTER,versionRefId:REF,processInstanceId:'actual-round',expectedCanonicalStatus:'FINALIZING',sourceFactsHash:HASH,preimageHash:PRE,reason:'真实投影维护原因',idempotencyKey:'actual-key'}
 const saved=await h.repairLifecycleProjection(FILE,command);assert.equal(saved.auditEventId,'700')
 assert.equal(h.calls[0].url,'/dcc/controlled-file/workflow-lifecycle/'+FILE+'/lifecycle-projection-repair-preview')
 assert.equal(h.calls[1].url,'/dcc/controlled-file/workflow-lifecycle/'+FILE+'/repair-lifecycle-projection')
 assert.deepEqual(JSON.parse(JSON.stringify(h.calls[1].data)),command)
 await assert.rejects(()=>api({result:{...preview,fileId:9007199254740992}}).getLifecycleProjectionRepairPreview(FILE),/身份/)
 await assert.rejects(()=>api({post:{...receipt,sourceFactsHash:PRE}}).repairLifecycleProjection(FILE,command),/不一致/)
})
test('actual dialog only posts after qualified preview, reason and confirmation; failures remain visible',async()=>{
 let writes=[];const h=child({write:async(id,data)=>{writes.push({id,data});return{...receipt,idempotencyKey:data.idempotencyKey}}})
 await h.load();await h.submit();assert.equal(writes.length,0)
 h.state.reason.value='真实原因';await h.submit();assert.equal(writes.length,1);assert.equal(writes[0].id,FILE);assert.equal(writes[0].data.preimageHash,PRE);assert.ok(h.events.some(e=>e[0]==='repaired'))
 const no=child({canRepair:false,write:async()=>{throw new Error('must not write')}});await no.load();no.state.reason.value='原因';await no.submit();assert.equal(no.state.canSubmit.value,false)
 const bad=child({write:async()=>{throw new Error('正式预像已变化')}});await bad.load();bad.state.reason.value='原因';await bad.submit();assert.match(bad.state.error.value,/正式预像已变化/)
})
test('actual dialog cancel and context changes during confirmation or late preview cause zero wrong-context writes',async()=>{
 let writes=0;const cancelled=child({confirm:async()=>{throw 'cancel'},write:async()=>{writes++;return receipt}});await cancelled.load();cancelled.state.reason.value='原因';await cancelled.submit();assert.equal(writes,0)
 let release;const h=child({confirm:()=>new Promise(r=>{release=r}),write:async()=>{writes++;return receipt}});await h.load();h.state.reason.value='原因';const pending=h.submit();h.props.contextKey='new-actor';release();await pending;assert.equal(writes,0)
 let tenantRelease;const drift=child({confirm:()=>new Promise(r=>{tenantRelease=r}),write:async()=>{writes++;return receipt}});await drift.load();drift.state.reason.value='原因';const tenantPending=drift.submit();drift.setLiveContext('tenant2-user25-file'+FILE);tenantRelease();await tenantPending;assert.equal(writes,0)
 let readRelease;const late=child({readPreview:()=>new Promise(r=>{readRelease=r})});const reading=late.load();late.props.fileId='9007199254740995';readRelease(preview);await reading;assert.equal(late.state.preview.value,undefined)
})
test('actual dialog renders original facts and server target while a read-only preview exposes no repair button',async()=>{
 const {descriptor}=parse(read('src/views/dcc/controlled-file/detail/DetailLifecycleProjectionRepairDialog.vue'))
 const compiled=compileScript(descriptor,{id:'actual-projection-repair',inlineTemplate:true})
 const seen=[],c={exports:{},require:id=>{
  if(id==='vue')return vue
  if(id==='element-plus')return{ElMessageBox:{confirm:async()=>{}}}
  if(id==='@/utils')return{generateUUID:()=> 'actual-key'}
  if(id==='@/api/dcc/controlledFile/lifecycleProjectionRepair')return{getLifecycleProjectionRepairPreview:async()=>({...preview,canRepair:false}),repairLifecycleProjection:async()=>{throw new Error('read-only must never write')}}
  throw new Error(id)
 }}
 vm.runInNewContext(transpile(compiled.content),c)
 const remove=n=>{if(n?.parent){const list=n.parent.children;list.splice(list.indexOf(n),1);n.parent=null}}
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p,anchor)=>{remove(n);n.parent=p;const at=p.children.indexOf(anchor);at<0?p.children.push(n):p.children.splice(at,0,n)},remove,setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:n=>n?.parent||null,nextSibling:n=>n?.parent?.children[n.parent.children.indexOf(n)+1]||null})
 const app=renderer.createApp(c.exports.default,{modelValue:true,fileId:FILE,contextKey:'actual-actor',readContextKey:()=> 'actual-actor',allowed:true})
 app.directive('loading',{})
 for(const tag of ['el-dialog','el-descriptions','el-descriptions-item','el-form','el-form-item','el-input','el-button'])app.component(tag,{inheritAttrs:false,setup:(_,{attrs,slots})=>()=>{if(tag==='el-button')seen.push(attrs['data-testid']);return vue.h('section',[slots.default?.(),slots.footer?.()])}})
 app.component('el-alert',{props:['title'],setup:props=>()=>vue.h('p',props.title)})
 const root={children:[]};app.mount(root);await new Promise(r=>setImmediate(r));await vue.nextTick()
 const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
 try{const actual=text(root);assert.match(actual,new RegExp(FILE));assert.match(actual,/FINALIZING/);assert.match(actual,/406、408/);assert.match(actual,/ACTIVATE_CONTROLLED/);assert.ok(actual.includes(HASH)&&actual.includes(PRE));assert.match(actual,/本窗口只读/);assert.equal(seen.includes('dcc-lifecycle-projection-repair-submit'),false)}finally{app.unmount()}
})
const command = reason => ({masterId:MASTER,versionRefId:REF,processInstanceId:'actual-round',expectedCanonicalStatus:'FINALIZING',sourceFactsHash:HASH,preimageHash:PRE,reason,idempotencyKey:'actual-key'})
test('actual wrapper and dialog enforce reason 500 before POST while the actual input renders the same bound',async()=>{
 const h=api();await h.repairLifecycleProjection(FILE,command('因'.repeat(500)));assert.equal(h.calls.length,1)
 const tooLong=api();await assert.rejects(()=>tooLong.repairLifecycleProjection(FILE,command('因'.repeat(501))),/500/);assert.equal(tooLong.calls.length,0)
 let writes=0;const d=child({write:async()=>{writes++;return receipt}});await d.load();d.state.reason.value='因'.repeat(501);await d.submit();assert.equal(writes,0)
 const {descriptor}=d;const find=n=>{if(n.type===1&&n.tag==='el-input'&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content==='dcc-lifecycle-projection-repair-reason'))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const findCurrent=n=>{if(n.type===1&&n.tag==='el-input'&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content==='dcc-lifecycle-projection-repair-reason'))return n;for(const child of [...(n.children||[]),...(n.branches||[])]){const found=findCurrent(child);if(found)return found}}
 const node=findCurrent(descriptor.template.ast);assert.ok(node)
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const reason="";const saving=false;</script>').descriptor,{id:'actual-repair-reason-limit',inlineTemplate:true})
 const c={exports:{},require:()=>vue};vm.runInNewContext(transpile(compiled.content),c);let actualMax
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default);app.component('el-input',{props:['maxlength'],setup:props=>{actualMax=props.maxlength;return()=>vue.h('textarea')}});app.mount({children:[]})
 try{assert.equal(actualMax,500)}finally{app.unmount()}
})
test('actual Long identity guard retains Java maximum and rejects overflow in paths and returned facts',async()=>{
 const maximum='9223372036854775807',overflow='9223372036854775808'
 const allowed=api({result:{...preview,fileId:maximum}});assert.equal((await allowed.getLifecycleProjectionRepairPreview(maximum)).fileId,maximum)
 const denied=api({result:{...preview,fileId:overflow}});await assert.rejects(()=>denied.getLifecycleProjectionRepairPreview(overflow),/身份/);assert.equal(denied.calls.length,0)
 await assert.rejects(()=>api({result:{...preview,masterId:overflow}}).getLifecycleProjectionRepairPreview(FILE),/身份/)
 const writeDenied=api();await assert.rejects(()=>writeDenied.repairLifecycleProjection(FILE,{...command('原因'),masterId:overflow}),/身份/);assert.equal(writeDenied.calls.length,0)
 await assert.rejects(()=>api({post:{...receipt,auditEventId:overflow}}).repairLifecycleProjection(FILE,command('原因')),/身份/)
})
test('actual receipt only accepts matching allowed canonical/domain and dialog validates the preview target before reporting completion',async()=>{
 for(const states of [{canonicalStatus:'FINALIZING',domainStatus:'FINALIZING'},{canonicalStatus:'ACTIVE',domainStatus:'CONTROLLED_PENDING_EFFECTIVE'}])await assert.rejects(()=>api({post:{...receipt,...states}}).repairLifecycleProjection(FILE,command('原因')),/状态|不一致/)
 const pendingReceipt={...receipt,canonicalStatus:'CONTROLLED_PENDING_EFFECTIVE',domainStatus:'CONTROLLED_PENDING_EFFECTIVE'}
 assert.equal((await api({post:pendingReceipt}).repairLifecycleProjection(FILE,command('原因'))).canonicalStatus,'CONTROLLED_PENDING_EFFECTIVE')
 const mismatch=child({write:async()=>pendingReceipt});await mismatch.load();mismatch.state.reason.value='原因';await mismatch.submit();assert.equal(mismatch.events.some(e=>e[0]==='repaired'),false);assert.match(mismatch.state.error.value,/目标|不一致/)
 const matching=child({readPreview:async()=>({...preview,expectedTargetStatus:'CONTROLLED_PENDING_EFFECTIVE'}),write:async()=>pendingReceipt});await matching.load();matching.state.reason.value='原因';await matching.submit();assert.ok(matching.events.some(e=>e[0]==='repaired'))
})
