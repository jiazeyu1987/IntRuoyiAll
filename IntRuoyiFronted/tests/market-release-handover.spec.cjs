const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const { ref, reactive, watch, nextTick, effectScope } = require('vue')
const filename='src/views/mes/pro/production-release/components/MarketReleaseHandover.vue'
const descriptor=parse(fs.readFileSync(path.resolve(__dirname,'..',filename),'utf8')).descriptor
const ast=ts.createSourceFile('component.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const code=ts.transpileModule(ast.statements.filter(n=>!ts.isImportDeclaration(n)).map(n=>n.getText(ast)).join('\n'),{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText
function setup(load,change) {
 const props=reactive({batchExecutionId:'50',access:{original:true,active:false},closed:false})
 const events=[],calls=[],scope=effectScope()
 const deps={ref,watch,defineProps:()=>props,defineEmits:()=>(...args)=>events.push(args),generateUUID:()=> 'handover-unique-1',
 getMarketReleaseHandover:load,changeMarketReleaseHandover:async(...args)=>{calls.push(args);return change(...args)}}
 const state=scope.run(()=>Function(...Object.keys(deps),code+';return {open,submit,context,visible,reason,recipientUserId,error,submitting}')(...Object.values(deps)))
 return {state,props,events,calls,stop:()=>scope.stop()}
}
const context={batchExecutionId:'50',version:'NONE',canHandover:true,canRecall:false,candidates:[{id:'200',name:'接收人'}]}
test('handover view compiles and sends only selected recipient plus exact version',async()=>{
 assert.deepEqual(compileTemplate({source:descriptor.template.content,filename,id:'handover'}).errors,[])
 const s=setup(async()=>context,async()=>({...context,canHandover:false,canRecall:true}))
 await s.state.open();s.state.recipientUserId.value='200';s.state.reason.value='  工作交接  ';await s.state.submit()
 assert.deepEqual(s.calls,[['assign',{batchExecutionId:'50',recipientUserId:'200',reason:'工作交接',expectedVersion:'NONE',idempotencyKey:'handover-unique-1'}]])
 assert.deepEqual(s.events,[['changed']]);s.stop()
})
test('retry preserves idempotency key and failed writes stay visible',async()=>{
 let attempts=0
 const s=setup(async()=>context,async()=>{if(++attempts===1)throw new Error('状态已变化');return context})
 await s.state.open();s.state.recipientUserId.value='200';s.state.reason.value='交接'
 await s.state.submit();assert.equal(s.state.visible.value,true);assert.equal(s.state.error.value,'状态已变化')
 await s.state.submit();assert.equal(s.calls[0][1].idempotencyKey,s.calls[1][1].idempotencyKey);s.stop()
})
test('recall uses the authoritative context and concurrent double click is ignored',async()=>{
 let resolve
 const pending=new Promise(ok=>{resolve=ok})
 const s=setup(async()=>({...context,version:'1:ACTIVE',canHandover:false,canRecall:true}),()=>pending)
 await s.state.open();s.state.reason.value='收回办理权'
 const first=s.state.submit();await s.state.submit();assert.equal(s.calls.length,1);assert.equal(s.calls[0][0],'recall')
 resolve(context);await first;s.stop()
})
test('changing the selected batch invalidates a pending dialog response',async()=>{
 let resolve
 const pending=new Promise(ok=>{resolve=ok});const s=setup(()=>pending,async()=>context)
 const opening=s.state.open();s.props.batchExecutionId='51';await nextTick();resolve(context);await opening
 assert.equal(s.state.context.value,undefined);assert.equal(s.state.visible.value,false);s.stop()
})
