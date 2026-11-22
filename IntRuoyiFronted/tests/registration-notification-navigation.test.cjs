const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const assert = require('node:assert/strict')
const { test } = require('node:test')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const load = file => {
 const exports = {}
 vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root,file),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText,{exports,require: name => name.includes('exactJavaLong') ? load('src/utils/exactJavaLong.ts') : {}, URL, window:{location:{origin:'http://localhost'}}, Error, Set, BigInt})
 return exports
}
const nav = load('src/utils/notifyMessageNavigation.ts')
const message = (eventType, extra={}) => ({id:'9223372036854775807',templateCode:'DCC_REGISTRATION_CERTIFICATE_BUSINESS_EVENT',templateParams:{certificateId:'42',versionId:'9007199254740993',eventType,...extra}})
test('certificate notification preserves exact version and known business event',async()=>{
 for (const event of ['NEW_CERTIFICATE_FORMALIZED','RENEWAL_CANDIDATE_UPLOADED','RENEWAL_CANDIDATE_ACTIVATED']) {
  const target=nav.getNotifyMessageTarget(message(event));assert.equal(target.versionId,'9007199254740993')
  let pushed
  await nav.navigateToNotifyMessageTarget({resolve:()=>({matched:[{path:'/mdm/registration-certificate/detail/:id'}]}),push:async x=>{pushed=x}},target)
  assert.equal(pushed.query.versionId,'9007199254740993')
 }
})
test('invalid historic notification remains renderable and prohibits navigation',async()=>{
 for(const data of [message('UNKNOWN'),message('NEW_CERTIFICATE_FORMALIZED',{versionId:9007199254740992}),{...message('X'),templateParams:null}]){
  const target=nav.getNotifyMessageTarget(data);assert.equal(target.type,'invalidRegistration');assert.ok(nav.getRegistrationNavigationError(data))
  await assert.rejects(nav.navigateToNotifyMessageTarget({push:()=>assert.fail()},target))
 }
})
test('confirmation keeps original message and repeated clicks create distinct route changes',async()=>{
 const target=nav.getNotifyMessageTarget(message('CHANGE_APPROVAL_RECORDED'));assert.equal(target.label,'查看变更并确认')
 const pushes=[];const router={currentRoute:{value:{query:{messageOpen:'1'}}},push:async x=>pushes.push(x)}
 await nav.navigateToNotifyMessageTarget(router,target);await nav.navigateToNotifyMessageTarget(router,target)
 assert.equal(pushes[0].query.messageId,'9223372036854775807');assert.notEqual(pushes[0].query.messageOpen,pushes[1].query.messageOpen)
})
test('Vue inbox openDetail never opens stale message after pending read completes',async()=>{
 const source=fs.readFileSync(path.join(root,'src/views/system/notify/my/components/MyNotifyMessageList.vue'),'utf8').split('<script lang="ts" setup>')[1].split('</script>')[0]
 const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText
 const ast=ts.createSourceFile('inbox.js',js,ts.ScriptTarget.ES2020,true)
 const declaration=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.getText(ast)==='openDetail')).getText(ast)
 let resolveRead;let current=true;const opened=[]
 const open=vm.runInNewContext('let detailOpenSequence = 0; let messageOpenSequence = 0;'+declaration+';openDetail',{handleReadOne:()=>new Promise(r=>resolveRead=r),detailRef:{value:{open:x=>opened.push(x)}},buildReadDetailData:x=>x})
 const pending=open({id:1,readStatus:false},()=>current);current=false;resolveRead({readAt:new Date()});await pending;assert.equal(opened.length,0)
 await open({id:2,readStatus:true},()=>true);assert.equal(opened[0].id,2)
 const old = open({id:3,readStatus:false});const finishOld=resolveRead
 await open({id:4,readStatus:true});finishOld({readAt:new Date()});await old
 assert.equal(opened.length,2);assert.equal(opened[1].id,4)
})

test('handling tasks and threshold reminders use their respective targets',()=>{
 assert.equal(nav.getNotifyMessageTarget({id:'1',templateCode:'DCC_REGISTRATION_HANDLING_TASK',templateParams:{handlingTaskId:'9007199254740993'}}).type,'registrationMessage')
 const target=nav.getNotifyMessageTarget({templateCode:'DCC_REGISTRATION_CERTIFICATE_THRESHOLD_REMINDER',templateParams:{certificateId:'33'}})
 assert.equal(target.certificateId,'33');assert.equal(target.versionId,undefined)
})

test('Vue mounted inbox watches route changes and cancels stale, manual, inactive and unmounted requests',async()=>{
 const vue=require('vue')
 const source=fs.readFileSync(path.join(root,'src/views/system/notify/my/components/MyNotifyMessageList.vue'),'utf8').split('<script lang="ts" setup>')[1].split('</script>')[0]
 const ast=ts.createSourceFile('inbox.ts',source,ts.ScriptTarget.Latest,true)
 const names=new Set(['detailOpenSequence','openDetail','route','messageOpenSequence'])
 const selected=ast.statements.filter(n=>(ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>names.has(d.name.getText(ast))))||(ts.isExpressionStatement(n)&&n.getText(ast).startsWith('watch(() => [route.fullPath'))||(ts.isExpressionStatement(n)&&n.getText(ast).startsWith('onBeforeUnmount(() => { messageOpenSequence'))).map(n=>n.getText(ast)).join('\n')
 const compiled=ts.transpileModule(selected,{compilerOptions:{target:ts.ScriptTarget.ES2020}}).outputText
 const route=vue.reactive({fullPath:'/user/profile?messageId=1',query:{messageId:'1'}}),props=vue.reactive({active:true}),pending=[],opened=[],errors=[],closed=[]
 let manual
 const node=()=>({children:[]})
 const renderer=vue.createRenderer({createElement:node,createText:node,createComment:node,setText(){},setElementText(){},patchProp(){},insert(){},remove(){},parentNode(){},nextSibling(){}})
 const app=renderer.createApp({setup(){
  manual=Function('watch','nextTick','onBeforeUnmount','useRoute','props','userStore','NotifyMessageApi','requireExactJavaLong','detailRef','handleReadOne','buildReadDetailData','message',compiled+';return openDetail')(vue.watch,vue.nextTick,vue.onBeforeUnmount,()=>route,props,{getUser:{id:9}},{getMyNotifyMessage:id=>new Promise(resolve=>pending.push({id,resolve}))},load('src/utils/exactJavaLong.ts').requireExactJavaLong,{value:{open:data=>opened.push(data.id),close:()=>closed.push(true)}},async()=>({readAt:new Date()}),data=>data,{error:x=>errors.push(x)})
  return ()=>vue.h('div')
 }})
 app.mount(node())
 const flush=async()=>{for(let i=0;i<8;i++)await vue.nextTick()}
 pending.shift().resolve({id:'1',readStatus:true});await flush();assert.deepEqual(opened,['1'])
 const go=id=>{route.query.messageId=id;route.fullPath='/user/profile?messageId='+id}
 const beforeClose=closed.length;go('2');assert.equal(closed.length,beforeClose+1);const a=pending.shift();go('3');pending.shift().resolve({id:'3',readStatus:true});await flush();a.resolve({id:'2',readStatus:true});await flush();assert.deepEqual(opened,['1','3'])
 go('4');const old=pending.shift();await manual({id:'5',readStatus:true});old.resolve({id:'4',readStatus:true});await flush();assert.deepEqual(opened,['1','3','5'])
 go('6');const inactive=pending.shift();props.active=false;inactive.resolve({id:'6',readStatus:true});await flush();assert.deepEqual(opened,['1','3','5'])
 props.active=true;const unmounted=pending.shift();app.unmount();unmounted.resolve({id:'6',readStatus:true});await flush();assert.deepEqual(opened,['1','3','5']);assert.deepEqual(errors,[])
})

test('bell clicking invalid registration never marks read or navigates',async()=>{
 const source=fs.readFileSync(path.join(root,'src/layout/components/Message/src/Message.vue'),'utf8')
 const script=source.split('<script lang="ts" setup>')[1].split('</script>')[0]
 const ast=ts.createSourceFile('bell.ts',script,ts.ScriptTarget.Latest,true)
 const declaration=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.getText(ast)==='handleMessageCardClick')).getText(ast)
 const compiled=ts.transpileModule(declaration,{compilerOptions:{target:ts.ScriptTarget.ES2020}}).outputText
 const click=Function('getNotifyMessageTarget','NotifyMessageApi','navigateToNotifyMessageTarget',compiled+';return handleMessageCardClick')(nav.getNotifyMessageTarget,{updateNotifyMessageRead:()=>assert.fail('invalid message marked read')},()=>assert.fail('invalid message navigated'))
 await click(message('UNKNOWN'))
})
