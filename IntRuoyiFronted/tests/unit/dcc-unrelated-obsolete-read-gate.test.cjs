const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),{parse}=require('vue/compiler-sfc')
const ast=ts.createSourceFile('detail.ts',parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text==='loadActiveObsoleteAction'))
function host(){let reads=0;const c={controlledFileId:{value:'10'},detailLoadSequence:1,route:{fullPath:'same'},activeObsoleteAction:{value:null},activeObsoleteActionError:{value:''},viewerMode:{value:false},showDetailManagementActions:{value:true},isCurrentDetailLoad:()=>true,
 buildObsoleteBusinessActionContext:()=>({actionCode:'DCC_OBSOLETE'}),findActiveBusinessAction:async()=>{reads++;throw Error('actual protected action read denied')},resolveReadSideErrorMessage:e=>e.message}
 vm.runInNewContext(ts.transpileModule(declaration.getText(ast)+'\nexports=loadActiveObsoleteAction',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c);return {c,load:c.exports,reads:()=>reads}}
test('ordinary uploader with explicit category denial issues no obsolete action read or false failure',async()=>{const h=host();await h.load({status:'ACTIVE',canObsolete:false});assert.equal(h.reads(),0);assert.equal(h.c.activeObsoleteActionError.value,'')})
test('pending obsolete state comes from the authorized file projection without form-create permission',async()=>{
 const h=host(),action={id:'11',instanceCode:'FCI-ACTUAL',status:'IN_APPROVAL',bpmProcessInstanceId:'actual-bpm'};
 await h.load({status:'ACTIVE',canObsolete:true,actionProjection:{actionLockReason:'OBSOLETE_APPROVAL_PENDING',pendingRequestId:'11',activeObsoleteAction:action}});
 assert.equal(h.reads(),0);assert.equal(h.c.activeObsoleteAction.value,action);assert.equal(h.c.activeObsoleteActionError.value,'')
})
test('missing or mismatched actual request remains an explicit error without unrelated form API calls',async()=>{
 for(const activeObsoleteAction of [undefined,{id:'12',status:'IN_APPROVAL'}]){
  const h=host();await h.load({status:'ACTIVE',canObsolete:true,actionProjection:{actionLockReason:'OBSOLETE_APPROVAL_PENDING',pendingRequestId:'11',activeObsoleteAction}});
  assert.equal(h.reads(),0);assert.equal(h.c.activeObsoleteAction.value,null);assert.match(h.c.activeObsoleteActionError.value,/状态不完整/)
 }
})

test('authorized account without an active obsolete request never calls the unrelated protected form API',async()=>{
 for(const actionProjection of [undefined,{allowedActions:['OBSOLETE'],actionLocked:false},{actionLockReason:'OTHER_PENDING',pendingRequestId:'11'},{actionLockReason:'OBSOLETE_APPROVAL_PENDING'}]){
  const h=host();h.c.activeObsoleteActionError.value='previous failure';h.c.activeObsoleteAction.value={id:'old'};
  await h.load({status:'ACTIVE',canObsolete:true,actionProjection});assert.equal(h.reads(),0);assert.equal(h.c.activeObsoleteActionError.value,'');assert.equal(h.c.activeObsoleteAction.value,null)
 }
})

test('view-only or unmanaged detail never reads obsolete state even with category authorization',async()=>{
 for(const key of ['viewerMode','showDetailManagementActions']){const h=host();h.c[key].value=key==='viewerMode';
 await h.load({status:'ACTIVE',canObsolete:true,actionProjection:{actionLockReason:'OBSOLETE_APPROVAL_PENDING',pendingRequestId:'11'}});assert.equal(h.reads(),0);assert.equal(h.c.activeObsoleteActionError.value,'')}
})
