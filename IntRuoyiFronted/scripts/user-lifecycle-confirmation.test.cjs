const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
function setup(confirm,validate=async()=>true){
const source=fs.readFileSync('src/views/system/user/UserLifecycleDeactivateForm.vue','utf8'),calls=[]
const c={formRef:{value:{validate}},formLoading:{value:false},dialogVisible:{value:true},targetUser:{value:{id:7}},targetUserText:{value:'employee'},supervisorImpact:{value:'impact'},formData:{value:{id:7,documentType:'TRANSFER',documentNo:'T1',effectiveTime:'2026-11-01T12:00:00'}},message:{confirm,success(){}},UserApi:{async recordUserLifecycleDeactivation(p){calls.push(p)}},emit(){}}
vm.createContext(c);vm.runInContext(ts.transpile(source.slice(source.indexOf('const submitForm ='),source.indexOf('</script>'))+'\nglobalThis.submit=submitForm',{target:ts.ScriptTarget.ES2022}),c);return {c,calls}
}
test('cancel and close do not register',async()=>{for(const a of ['cancel','close']){const x=setup(async()=>{throw a});await x.c.submit();assert.equal(x.calls.length,0);assert.equal(x.c.formLoading.value,false)}})
test('validation lock blocks duplicates; confirmed snapshot only',async()=>{let release;const gate=new Promise(r=>release=r);let count=0;const x=setup(()=>gate,async()=>{count++;return true});const p=x.c.submit();await x.c.submit();await Promise.resolve();x.c.formData.value.documentNo='changed';release();await p;assert.equal(count,1);assert.equal(x.calls.length,1);assert.equal(x.calls[0].documentNo,'T1')})
test('closing during validation prevents registration',async()=>{let release;const gate=new Promise(r=>release=r);const x=setup(async()=>{},()=>gate);const p=x.c.submit();x.c.dialogVisible.value=false;release(true);await p;assert.equal(x.calls.length,0)})
test('supervisor actual confirmation callback cancel and snapshot',async()=>{
const source=fs.readFileSync('src/views/system/user/UserDirectSupervisorDialog.vue','utf8');const callback=source.slice(source.indexOf('  save: async (payload) => {')+8,source.indexOf('  candidates: getDirectSupervisorCandidates')).trim().replace(/,$/,'')
for(const cancel of [true,false]){let release;const gate=new Promise((r,j)=>release=()=>cancel?j('cancel'):r()),calls=[];const state={sequence:1,visible:true,relation:{username:'employee',nickname:'E',supervisorUserId:null},candidates:[{id:'9',username:'boss',nickname:'B'}]};const c={SUPERVISOR_SAVE_CANCELLED:Symbol('cancel'),state,message:{confirm:()=>gate},saveDirectSupervisor:async p=>{calls.push(p);return p}};vm.createContext(c);vm.runInContext(ts.transpile('globalThis.save='+callback,{target:ts.ScriptTarget.ES2022}),c);const p=c.save({supervisorUserId:'9',operation:'SET',changeReason:'reason'});state.selectedId='10';release();await p;assert.equal(calls.length,cancel?0:1);if(!cancel)assert.equal(calls[0].supervisorUserId,'9')}
})

test('state distinguishes explicit cancellation from missing server response',async()=>{
const source=fs.readFileSync('src/views/system/user/direct-supervisor-state.ts','utf8')
const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const c={exports:{}};vm.createContext(c);vm.runInContext(compiled,c)
const {createSupervisorState,submitSupervisor,SUPERVISOR_SAVE_CANCELLED}=c.exports
for(const cancelled of [true,false]){
const s=createSupervisorState();Object.assign(s,{visible:true,ready:true,userId:'7',tenantId:'1',reason:'reason',selectedId:'9',candidates:[{id:'9',username:'boss',nickname:'B'}],relation:{userId:'7',tenantId:'1',username:'employee',nickname:'E',supervisorUserId:null,relationVersion:'0'}})
await submitSupervisor(s,{save:async()=>cancelled?SUPERVISOR_SAVE_CANCELLED:undefined})
assert.equal(s.saving,false);assert.equal(s.visible,true);assert.equal(Boolean(s.error),!cancelled)
}
})
