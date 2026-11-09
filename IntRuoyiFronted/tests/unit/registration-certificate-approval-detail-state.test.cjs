const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const ts=require('typescript')
const vue=require('vue')
const {parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const transpile=source=>ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
function moduleFrom(path,imports=()=>({})){const out={};new Function('exports','require',transpile(fs.readFileSync(path,'utf8')))(out,imports);return out}
const facts=moduleFrom('src/views/dcc/registration-certificate/shared/facts.ts')
const previewApi=moduleFrom('src/api/common/filePreview.ts')
const drain=async()=>{for(let i=0;i<20;i++)await Promise.resolve();await vue.nextTick()}
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject}}
const snapshot=(name='本次冻结产品')=>({status:'BPM_BOUND',requestedAt:'2026-10-30 10:00:00',purpose:'本次说明',submission:{ownerCompanyName:'冻结公司',factsJson:JSON.stringify({certificateType:'DOMESTIC',certificateNo:'旧顶层数据',ceCertificates:[]}),draft:{productName:name,registrantName:'冻结注册人',factsJson:JSON.stringify({certificateType:'FDA',submissionType:'EXEMPT',registrationNumber:'FDA-冻结号',ceCertificates:[]})}},changes:[{itemType:'PRODUCT_NAME',beforeValueJson:'"变更前产品"',afterValueJson:'"变更后产品"'}],files:[{id:'71',fileKind:'REGISTRATION_CERTIFICATE',originalName:'批准信函.pdf'},{id:'72',fileKind:'CERTIFICATE_ATTACHMENT',originalName:'本次附件.pdf'},{id:'73',fileKind:'FDA_PRODUCT_DESCRIPTION',originalName:'产品描述.pdf'}]})
function mount(get){
 const path='src/views/dcc/registration-certificate/approval/RequestDetailPanel.vue',descriptor=parse(fs.readFileSync(path,'utf8')).descriptor
 const script=compileScript(descriptor,{id:path}),compiled=compileTemplate({id:path,source:descriptor.template.content,compilerOptions:{bindingMetadata:script.bindings}});assert.deepEqual(compiled.errors,[])
 const exports={},template={}
 const field=vue.defineComponent({props:['modelValue'],setup:p=>()=>vue.h('facts',{facts:p.modelValue},JSON.stringify(p.modelValue))})
 const viewer=vue.defineComponent({props:['previewSource'],setup:p=>()=>vue.h('viewer',{previewSource:p.previewSource})})
 new Function('exports','require','ref','computed','watch','shallowRef',transpile(script.content))(exports,id=>{
  if(id==='vue')return vue
  if(id==='@/config/axios')return {default:{get}}
  if(id.endsWith('/facts'))return facts
  if(id==='@/api/common/filePreview')return previewApi
  if(id==='@/utils/formatTime')return {formatDateTimeValue:v=>v}
  if(id.includes('FactsFields'))return {default:field}
  if(id.includes('controlled-file/view'))return {default:viewer}
  throw Error('unexpected import '+id)
 },vue.ref,vue.computed,vue.watch,vue.shallowRef)
 new Function('exports','require',transpile(compiled.code))(template,()=>vue);exports.default.render=template.render
 const node=(type,text='')=>({type,text,props:{},children:[],parent:null})
 const renderer=vue.createRenderer({createElement:tag=>node(tag),createText:s=>node('#text',s),createComment:s=>node('#comment',s),insert(c,p,anchor){if(c.parent){const old=c.parent.children.indexOf(c);if(old>=0)c.parent.children.splice(old,1)}const i=anchor?p.children.indexOf(anchor):-1;if(i<0)p.children.push(c);else p.children.splice(i,0,c);c.parent=p},remove(c){if(c.parent)c.parent.children.splice(c.parent.children.indexOf(c),1)},setText:(n,s)=>n.text=s,setElementText:(n,s)=>{n.text=s;n.children=[]},parentNode:n=>n.parent,nextSibling:n=>n.parent?.children[n.parent.children.indexOf(n)+1],patchProp:(n,k,old,v)=>n.props[k]=v})
 const props=vue.reactive({requestId:42,processInstanceId:'process-A'}),root=node('root'),app=renderer.createApp({render:()=>vue.h(exports.default,props)})
 const ordinary=tag=>vue.defineComponent({inheritAttrs:false,setup:(_,ctx)=>()=>vue.h(tag,ctx.attrs,[ctx.slots.header?.(),ctx.slots.default?.()])})
 for(const tag of ['el-card','el-descriptions','el-descriptions-item','el-form','el-button'])app.component(tag,ordinary(tag))
 app.component('el-alert',vue.defineComponent({props:['title'],setup:p=>()=>vue.h('alert',{},p.title)}))
 app.component('el-dialog',vue.defineComponent({props:['modelValue'],setup:(p,ctx)=>()=>p.modelValue?vue.h('dialog',{},ctx.slots.default?.()):null}))
 app.component('el-table',vue.defineComponent({props:['data'],setup:(p,ctx)=>{vue.provide('rows',()=>p.data);return()=>vue.h('table',{},ctx.slots.default?.())}}))
 app.component('el-table-column',vue.defineComponent({props:['label','prop'],setup:(p,ctx)=>{const rows=vue.inject('rows');return()=>vue.h('column',{},[p.label,...rows().map(row=>ctx.slots.default?ctx.slots.default({row}):String(row[p.prop]??''))])}}))
 app.directive('loading',{})
 app.mount(root)
 const nodes=(type,n=root)=>[...(n.type===type?[n]:[]),...n.children.flatMap(c=>nodes(type,c))]
 const text=(n=root)=>n.type==='#comment'?'':n.text+n.children.map(text).join(' ')
 return {props,app,nodes,text,state:app._instance.subTree.component.setupState}
}

test('actual approval render uses current frozen draft facts, all file kinds and change before/after',async()=>{
 const c=mount(async()=>snapshot())
 try{await drain();const text=c.text();for(const expected of ['本次冻结产品','冻结注册人','FDA-冻结号','批准信函.pdf','本次附件.pdf','产品描述.pdf','变更前产品','变更后产品'])assert.ok(text.includes(expected),expected);assert.ok(!text.includes('旧顶层数据'));assert.equal(c.nodes('facts')[0].props.facts.certificateType,'FDA');assert.equal(c.nodes('el-button').length,3);assert.ok(!text.includes('下载'))}finally{c.app.unmount()}
})

test('switching request and process drops late old response and clears old exact preview',async()=>{
 const pending=[],calls=[],c=mount(args=>{calls.push(args);const d=deferred();pending.push(d);return d.promise})
 try{pending[0].resolve(snapshot('旧申请'));await drain();c.nodes('el-button')[0].props.onClick();await drain();assert.equal(c.nodes('viewer').length,1)
  c.props.requestId=99;c.props.processInstanceId='process-B';await drain();assert.equal(c.nodes('viewer').length,0);assert.ok(!c.text().includes('旧申请'))
  c.props.requestId=100;c.props.processInstanceId='process-C';await drain();pending[2].resolve(snapshot('最终申请'));await drain();pending[1].resolve(snapshot('过时申请'));await drain();assert.ok(c.text().includes('最终申请'));assert.ok(!c.text().includes('过时申请'));assert.deepEqual(calls[1].params,{requestId:99,processInstanceId:'process-B'});assert.deepEqual(calls[2].params,{requestId:100,processInstanceId:'process-C'})
 }finally{c.app.unmount()}
})

test('online preview uses actual approval source builder with exact request process and file identity',async()=>{
 const c=mount(async()=>snapshot())
 try{await drain();c.nodes('el-button')[2].props.onClick();await drain();assert.deepEqual(c.nodes('viewer')[0].props.previewSource,{type:'DCC_REGISTRATION_CERTIFICATE_APPROVAL',businessFileId:'73',requestId:42,processInstanceId:'process-A'});assert.equal(c.nodes('a').length,0);assert.ok(!c.text().includes('下载'))}finally{c.app.unmount()}
})

test('authorization denial is explicit and removes another submission and its preview',async()=>{
 const c=mount(async({params})=>{if(params.requestId===42)return snapshot('可读旧申请');throw Error('当前任务无权查看该申请')})
 try{await drain();c.nodes('el-button')[0].props.onClick();await drain();c.props.requestId=43;await drain();assert.ok(c.text().includes('当前任务无权查看该申请'));assert.ok(!c.text().includes('可读旧申请'));assert.equal(c.nodes('facts').length,0);assert.equal(c.nodes('table').length,0);assert.equal(c.nodes('viewer').length,0);assert.equal(c.state.loading,false)}finally{c.app.unmount()}
})


test('actual change rendering decodes nested facts JSON and localized CE revisions enums and booleans',async()=>{
 const data=snapshot(),before={certificateType:'CE',certificateStatus:'SUSPENDED',ownProduct:true,ceCertificates:[{revision:'1',certificateNo:'CE-OLD',issueDate:''}]},after={certificateType:'CE',certificateStatus:'VALID',ownProduct:false,ceCertificates:[{revision:'2',certificateNo:'',issueDate:'2026-10-30'}]}
 data.changes=[{itemType:'CERTIFICATE_INFORMATION',beforeValueJson:JSON.stringify({value:JSON.stringify(before)}),afterValueJson:JSON.stringify({draft:{factsJson:JSON.stringify(after)}})}]
 const c=mount(async()=>data)
 try{await drain();const rendered=c.nodes('table')[0];const text=n=>n.text+n.children.map(text).join(' '),content=text(rendered);for(const expected of ['证件状态：暂停','证件状态：有效','是否瑛泰产品：是','是否瑛泰产品：否','证书Rev.记录','Rev.：1','Rev.：2','注册证号：CE-OLD','签发日期：未填写','签发日期：2026-10-30'])assert.ok(content.includes(expected),expected);assert.ok(!content.includes('factsJson'));assert.ok(!content.includes('SUSPENDED'));assert.ok(!content.includes('certificateStatus'));assert.ok(!content.includes('{"'))}finally{c.app.unmount()}
})
