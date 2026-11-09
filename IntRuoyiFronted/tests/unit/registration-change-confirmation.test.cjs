const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const source = fs.readFileSync('src/views/dcc/registration-certificate/components/ChangeConfirmationsPanel.vue','utf8')
const script = compileScript(parse(source).descriptor,{id:'change-confirmations'})
const row = id => ({id,certificateId:'100',versionId:'200',notifyMessageId:id,recipientUserId:'21',sentAt:'2026-10-30 10:00:00'})
const drain = async () => {for(let i=0;i<30;i++)await Promise.resolve();await vue.nextTick()}
function mount(props,api){
 const exports={}
 new Function('exports','require',ts.transpileModule(script.content,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,id=>{
  if(id==='vue')return vue
  if(id==='@/api/dcc/registrationCertificate/changeConfirmations')return api
  throw Error('unexpected import '+id)
 })
 exports.default.render=()=>vue.h('section')
 const renderer=vue.createRenderer({createElement:()=>({children:[]}),createComment:()=>({children:[]}),createText:()=>({children:[]}),insert:(c,p)=>p.children.push(c),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const reactive=vue.reactive(props)
 const app=renderer.createApp({render:()=>vue.h(exports.default,reactive)})
 const host=app.mount({children:[]})
 return {props:reactive,state:host.$.subTree.component.setupState,app}
}
test('actual component loads own notification and confirms only through authenticated message endpoint',async()=>{
 const calls=[]
 const c=mount({messageId:'1001'},{getMyChangeConfirmation:async id=>{calls.push(['get',id]);return row(id)},confirmMyCertificateChange:async id=>{calls.push(['confirm',id]);return {...row(id),confirmedAt:'2026-10-30 11:00:00'}},listChangeConfirmations:()=>{throw Error('daily certificate read must not run')}})
 try{
  await drain();assert.equal(c.state.records[0].notifyMessageId,'1001')
  await c.state.confirm();assert.equal(c.state.records[0].confirmedAt,'2026-10-30 11:00:00')
  assert.deepEqual(calls,[['get','1001'],['confirm','1001']])
 }finally{c.app.unmount()}
})
test('foreign recipient and send/confirmation failures stay explicit without claiming success',async()=>{
 const c=mount({messageId:'1001'},{getMyChangeConfirmation:async()=>{throw Error('当前账号不是接收人')}})
 try{await drain();assert.match(c.state.error,/不是接收人/);assert.equal(c.state.records.length,0)}finally{c.app.unmount()}
 const own=mount({messageId:'1001'},{getMyChangeConfirmation:async id=>row(id),confirmMyCertificateChange:async()=>{throw Error('确认保存失败')}})
 try{await drain();await own.state.confirm();assert.match(own.state.error,/保存失败/);assert.equal(own.state.records[0].confirmedAt,undefined)}finally{own.app.unmount()}
})
test('switching messages cannot display a stale confirmation response',async()=>{
 let finish
 const c=mount({messageId:'1001'},{getMyChangeConfirmation:async id=>row(id),confirmMyCertificateChange:()=>new Promise(resolve=>{finish=resolve})})
 try{
  await drain();const pending=c.state.confirm();c.props.messageId='1002';await drain()
  finish({...row('1001'),confirmedAt:'old'});await pending
  assert.equal(c.state.records[0].notifyMessageId,'1002');assert.equal(c.state.records[0].confirmedAt,undefined)
 }finally{c.app.unmount()}
})
test('real template compiles and settings provide an explicit separate change confirmation recipient list',()=>{
 const result=compileTemplate({id:'change-confirmations',source:parse(source).descriptor.template.content,compilerOptions:{bindingMetadata:script.bindings}})
 assert.deepEqual(result.errors,[])
 const config=fs.readFileSync('src/views/dcc/registration-certificate/config/ReminderConfigDialog.vue','utf8')
 assert.match(config,/CHANGE_CONFIRMATION.*变更生效确认人员/)
 assert.match(config,/CHANGE_CONFIRMATION: normalizeUserIds\(formData.thresholdRecipientUserIds.CHANGE_CONFIRMATION\)/)
})
test('changing reminder schedule preserves configured confirmation recipients and rejects malformed identities',()=>{
 const schedule=fs.readFileSync('src/views/dcc/registration-certificate/config/components/ReminderScheduleSetting.vue','utf8')
 const logic=schedule.slice(schedule.indexOf('const assertConfigShape ='),schedule.indexOf('const loadConfig ='))
 const form={},exports={}
 new Function('form','loaded','exports',ts.transpileModule(logic+'\nexports.apply=applyConfig',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText)(form,{value:false},exports)
 const config={rowVersion:2,dailyRunTime:'09:00',timezone:'Asia/Shanghai',thresholdDaysJson:'[30,8,2,1]',thresholdRecipientUserIds:{T_30:[1],T_8:[2],T_2:[3],T_1:[4],CHANGE_CONFIRMATION:[21,22]}}
 exports.apply(config)
 assert.deepEqual(form.thresholdRecipientUserIds.CHANGE_CONFIRMATION,[21,22])
 assert.notEqual(form.thresholdRecipientUserIds.CHANGE_CONFIRMATION,config.thresholdRecipientUserIds.CHANGE_CONFIRMATION)
 assert.throws(()=>exports.apply({...config,thresholdRecipientUserIds:{...config.thresholdRecipientUserIds,CHANGE_CONFIRMATION:'bad'}}),/变更确认收件人必须为数组/)
})
