const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),ts=require('typescript')
const source=fs.readFileSync(path.resolve(__dirname,'../../src/views/mes/pro/processpool/workflowInteractionLog.ts'),'utf8')
const javascript=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
function setup(fail=false){
  const posts=[],listeners=new Map(),timers=[],state={id:7,tenant:168,token:'private-token'}
  const document={body:{},addEventListener:(k,v)=>listeners.set(k,v),removeEventListener:k=>listeners.delete(k),querySelectorAll:()=>[]}
  class Element{classList=[];closest(){return null}matches(){return false}getClientRects(){return []}}
  const exports={}
  const modules={
    '@/config/axios':{default:{post:async request=>{posts.push(request);if(fail)throw Error('network')},get:async()=>[]}},
    vue:{ref:value=>({value}),nextTick:async()=>{},onMounted:fn=>fn(),onActivated:()=>{},onDeactivated:()=>{},onBeforeUnmount:()=>{}},
    '@/store/modules/user':{useUserStore:()=>({get getUser(){return{id:state.id}}})},
    '@/utils/auth':{getTenantId:()=>state.tenant,getAccessToken:()=>state.token}}
  Function('exports','require','document','window','MutationObserver','Element','requestAnimationFrame',javascript)(exports,
    name=>{assert.ok(modules[name],name);return modules[name]},document,{addEventListener(){},removeEventListener(){}},
    class{observe(){}disconnect(){}},Element,fn=>timers.push(fn))
  const log=exports.useWorkflowInteractionLog('PRODUCTION')
  return{log,posts,listeners,timers,state,Element}
}
test('page only sends cancellation or validation observation and never actor time or secrets',async()=>{
  const f=setup();await f.log.record('CANCELED','关闭操作确认或编辑窗口');await f.log.record('BLOCKED','页面校验未通过')
  assert.deepEqual(f.posts.map(p=>p.data),[
    {scope:'PRODUCTION',outcome:'CANCELED',control:'关闭操作确认或编辑窗口'},
    {scope:'PRODUCTION',outcome:'BLOCKED',control:'页面校验未通过'}])
  assert.equal(f.log.saving.value,0);assert.equal(f.log.saveError.value,'')
  assert.ok(!JSON.stringify(f.posts).includes('private-token'))
  assert.equal(f.posts[0].skipAuthRefreshReplay,true)
})
test('logging failure is surfaced and unauthenticated events are not sent as another person',async()=>{
  const f=setup(true);await f.log.record('CANCELED','取消')
  assert.match(f.log.saveError.value,/取消\/拦截交互日志保存未确认/)
  assert.match(f.log.saveError.value,/业务提交结果请查看提交记录/)
  f.state.id=0;await f.log.record('CANCELED','取消');assert.equal(f.posts.length,1)
  assert.match(f.log.saveError.value,/身份已失效/)
})
test('cancel during an account change is never attributed to the next account',async()=>{
  const f=setup();const button=new f.Element();button.textContent='取消'
  const target=new f.Element();target.closest=selector=>selector==='button'?button:null
  f.listeners.get('click')({target});await Promise.resolve();f.state.id=8
  f.timers.forEach(fn=>fn());await new Promise(resolve=>setImmediate(resolve))
  assert.equal(f.posts.length,0);assert.match(f.log.saveError.value,/未以其他人员身份补记/)
})
test('unclosed confirmation is logged as blocked instead of canceled',async()=>{
  const f=setup();const dialog=new f.Element();dialog.isConnected=true;dialog.getClientRects=()=>[{}]
  dialog.querySelector=()=>({textContent:'重置固定测试订单'})
  const button=new f.Element();button.textContent='取消';button.closest=()=>dialog
  const target=new f.Element();target.closest=selector=>selector==='button'?button:null
  f.listeners.get('click')({target});await Promise.resolve();f.timers.forEach(fn=>fn());await new Promise(resolve=>setImmediate(resolve))
  assert.equal(f.posts[0].data.outcome,'BLOCKED')
  assert.equal(f.posts[0].data.control,'关闭被拦截：重置固定测试订单')
})

test('cancellation identifies the action from the title without reading form contents',async()=>{
  const f=setup();const dialog=new f.Element();dialog.querySelector=()=>({textContent:'重置固定测试订单'})
  const button=new f.Element();button.textContent='取消';button.closest=()=>dialog
  const target=new f.Element();target.closest=selector=>selector==='button'?button:null
  f.listeners.get('click')({target});await Promise.resolve();f.timers.forEach(fn=>fn());await new Promise(resolve=>setImmediate(resolve))
  assert.equal(f.posts[0].data.outcome,'CANCELED');assert.equal(f.posts[0].data.control,'重置固定测试订单')
})

test('an accepted animated drawer close is cancellation even while its rectangle remains visible',async()=>{
  const f=setup();const dialog=new f.Element();dialog.isConnected=true;dialog.getClientRects=()=>[{}]
  dialog.querySelector=()=>({textContent:'反查关联批次'})
  dialog.parentElement={classList:['el-drawer-fade-leave-active'],parentElement:null}
  const button=new f.Element();button.textContent='关闭';button.closest=()=>dialog
  const target=new f.Element();target.closest=selector=>selector==='button'?button:null
  f.listeners.get('click')({target});await Promise.resolve();f.timers.forEach(fn=>fn());await new Promise(resolve=>setImmediate(resolve))
  assert.equal(f.posts[0].data.outcome,'CANCELED');assert.equal(f.posts[0].data.control,'反查关联批次')
})
