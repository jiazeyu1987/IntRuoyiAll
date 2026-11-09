const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
function model() {
  const source = fs.readFileSync('src/views/dcc/controlled-file/detail/approval-actions.ts','utf8'), exports = {}, sent = []
  new Function('exports','require',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,name=>{
    if(name.endsWith('/workflow'))return {approveControlledFileTask:async(id,data)=>{sent.push(data);return{controlledFileId:id}},rejectControlledFileTask:async()=>({controlledFileId:'10'}),DccTaskActionError:Error,isControlledFileTaskPasswordInvalidError:()=>false}
    if(name==='../shared/lifecycle')return{getDccControlledFileStageByKey:()=>undefined}
    throw Error(name)
  })
  return{...exports,sent}
}
test('approval uses uploader compiler from server without requesting a new person',async()=>{
 const m=model(),result=await m.submitDccApprovalAction({fileId:'10',taskId:'task-1',action:'approve',form:{password:'secret',reason:'批准'}})
 assert.equal(result.success,true);assert.equal(m.sent.length,1);assert.equal(Object.hasOwn(m.sent[0],'fileOwnerUserId'),false)
})
test('stale caller supplied owner fields never enter approval transport',async()=>{
 const m=model()
 await m.submitDccApprovalAction({fileId:'10',taskId:'task-1',action:'approve',form:{password:'secret',reason:'批准',fileOwnerRequired:true,fileOwnerUserId:'9007199254740993'}})
 assert.equal(Object.hasOwn(m.sent[0],'fileOwnerUserId'),false)
})
test('native approval dialog displays compiler identity without a person picker',()=>{
 const source=fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')
 assert.doesNotMatch(source,/<ApprovalFileOwnerPicker\b/)
 assert.match(source,/fileOwnerNicknameSnapshot/)
 assert.match(source,/文件编制人/)
})
function picker(readUsers, selected) {
 const vue=require('vue'),{parse,compileScript}=require('vue/compiler-sfc')
 const file='src/views/dcc/controlled-file/detail/ApprovalFileOwnerPicker.vue'
 const compiled=compileScript(parse(fs.readFileSync(file,'utf8')).descriptor,{id:'owner-picker'}).content
 const events=[],exports={}
 const identity={};new Function('exports',ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/relations/project-reference-contract.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(identity)
 new Function('exports','require',ts.transpileModule(compiled,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,name=>{
  if(name==='vue')return vue
  if(name==='@/api/system/user')return{getSimpleUserList:readUsers}
  if(name==='../relations/project-reference-contract')return identity
  throw Error(name)
 })
 const component=exports.default;component.render=()=>vue.h('section')
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:text=>({text}),insert:(child,parent)=>parent.children.push(child),remove(){},setText(){},setElementText(){},parentNode:()=>null,nextSibling:()=>null,patchProp(){}})
 const app=renderer.createApp({render:()=>vue.h(component,{modelValue:selected,'onUpdate:modelValue':id=>events.push(id)})})
 const mounted=app.mount({children:[]})
 return{app,events,state:mounted.$.subTree.component.setupState}
}
const settle=async()=>{for(let n=0;n<12;n++){await Promise.resolve();await require('vue').nextTick()}}
test('actual owner picker emits exact enabled account identity and never selects a default person',async()=>{
 const p=picker(async()=>[{id:'9007199254740993',nickname:'正式负责人',username:'owner'}])
 try{await settle();assert.equal(p.events.length,0);assert.equal(p.state.users.length,1);p.state.choose('9007199254740993');assert.equal(p.events[0],'9007199254740993')}
 finally{p.app.unmount()}
})
test('owner directory failure or unsafe IDs stay visible and cannot emit a valid choice',async()=>{
 for(const read of [async()=>{throw Error('正式账号权限拒绝')},async()=>[{id:9007199254740992,nickname:'不精确账号'}]]){
  const p=picker(read)
  try{await settle();assert.ok(p.state.error);assert.equal(p.events.length,0);p.state.choose('7');assert.equal(p.events.length,0)}finally{p.app.unmount()}
 }
})
test('unmounted owner picker ignores its pending account response',async()=>{
 let release
 const p=picker(()=>new Promise(resolve=>{release=resolve}))
 p.app.unmount();release([{id:'7',nickname:'迟到负责人'}]);await settle()
 assert.equal(p.state.users.length,0);assert.equal(p.events.length,0)
})
