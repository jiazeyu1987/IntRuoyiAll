import assert from 'node:assert/strict'
import {readFileSync,existsSync} from 'node:fs'
import {resolve,dirname} from 'node:path'
import {fileURLToPath} from 'node:url'
import {createRequire} from 'node:module'
import ts from 'typescript'
import test from 'node:test'
const require=createRequire(import.meta.url),Vue=require('vue'),{parse,compileScript}=require('vue/compiler-sfc')
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),make=(type,text='')=>({type,text,children:[],props:{},parent:null})
const renderer=Vue.createRenderer({createElement:type=>make(type),createText:text=>make('#text',text),createComment:text=>make('#comment',text),setText:(n,t)=>n.text=t,setElementText:(n,t)=>{n.text=t;n.children=[]},patchProp:(n,k,o,v)=>n.props[k]=v,insert:(n,p,a)=>{if(n.parent){const i=n.parent.children.indexOf(n);if(i>=0)n.parent.children.splice(i,1)}n.parent=p;const i=a?p.children.indexOf(a):-1;i<0?p.children.push(n):p.children.splice(i,0,n)},remove:n=>{if(n.parent){const i=n.parent.children.indexOf(n);if(i>=0)n.parent.children.splice(i,1)}n.parent=null},parentNode:n=>n.parent,nextSibling:n=>n.parent?.children[n.parent.children.indexOf(n)+1],querySelector:()=>null,setScopeId(){},insertStaticContent:(s,p)=>{const n=make('#static',s);p.children.push(n);n.parent=p;return[n,n]}})
const nodes=(n,p)=>[...(p(n)?[n]:[]),...n.children.flatMap(x=>nodes(x,p))],content=n=>n.text+n.children.map(content).join(''),find=(r,t,s)=>nodes(r,n=>n.type===t&&(!s||content(n).includes(s)))[0]
const settle=async()=>{for(let n=0;n<18;n++){await Promise.resolve();await Vue.nextTick()}}
const P='9223372036854775700',F='9223372036854775701',U='9223372036854775707',M='9007199254740993',ID='9007199254740995'
const project={id:P,projectName:'正式项目',projectCode:'P1',status:'ENABLE',projectLeaderUserId:U,projectLeader:'负责人',associatedFileCount:1}
const row={id:ID,masterId:M,tenantId:'1',dccProjectCodeId:P,latestControlledFileId:ID,projectName:'正式项目',projectFolderId:F,projectFolderName:'质量文件',fileName:'正式原文.pdf',sourceOriginalFileName:'正式原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canPreview:false}
function mount(actor=U, browserRows=[row],options={}){
 let usageCount=options.usageCount??2
 const reference={reference:{id:'19',projectId:P,folderId:F,masterId:M,selectedControlledFileId:ID,createdBy:U},selectedVersion:{tenantId:'1',controlledFileId:ID,masterId:M,projectId:P,fileName:'引用原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false},referenceProjectCount:usageCount}
 const calls=[],routes=[],previews=[],cache=new Map(),request={get:async args=>{calls.push(['get',args]);if(args.url==='/dcc/project-codes/page')return{list:[project],total:33};if(args.url===`/dcc/project-codes/${P}`)return project;if(args.url.endsWith('/folders'))return[{id:F,projectCodeId:P,parentId:'0',name:'质量文件',sortOrder:0,active:true}];if(args.url==='/system/user/simple-list')return[{id:U}];if(args.url==='/dcc/project-file-references/usage-page')return options.usagePage?options.usagePage(args):{tenantId:'1',sourceControlledFileId:ID,masterId:M,referenceProjectCount:2,visibleReferenceProjectCount:1,total:1,detailsRestricted:true,list:[{referenceId:'19',projectId:P,projectName:'被引用目的项目',folderId:F,folderName:'目的质量文件夹',masterId:M,selectedControlledFileId:ID,fileName:'引用原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canPreview:false}]};if(args.url===`/dcc/controlled-files/${ID}/relation-permissions`)return options.permissions?options.permissions():{controlledFileId:ID,tenantId:'1',masterId:M,projectId:P,projectName:'正式项目',projectFolderId:F,projectFolderName:'质量文件',fileName:'引用原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canEdit:false,canPreview:options.canPreview??false};if(args.url===`/dcc/controlled-files/${ID}`){if(options.traceDenied)throw new Error('正式详情独立授权拒绝');return {...row,id:ID,fileName:'引用原文.pdf',versionNo:'A/1'}};if(args.url==='/dcc/controlled-files/browser-page'){
   let list=typeof browserRows==='function'?browserRows(args.params):browserRows
   if(args.params.status)list=[{...row,status:args.params.status,controlled:['ACTIVE','CONTROLLED_PENDING_EFFECTIVE'].includes(args.params.status),pendingEffect:args.params.status==='CONTROLLED_PENDING_EFFECTIVE',executable:false}]
   else if(args.params.latestVersionOnly)list=list.filter(file=>file.id===file.latestControlledFileId)
   return{list,total:81}
  }if(args.url==='/dcc/project-file-references/usage')return{masterId:M,referenceProjectCount:usageCount,referenced:usageCount>0};if(args.url==='/dcc/project-file-references')return options.references?[reference]:[];throw new Error(args.url)},post:async args=>{calls.push(['post',args]);if(args.url.endsWith('/cancel')){usageCount=0;return 0}return[]}}
 const load=filename=>{if(!existsSync(filename)&&!filename.endsWith('.vue'))filename=filename.replace(/\.ts$/,'/index.ts');if(cache.has(filename))return cache.get(filename);const exports={};cache.set(filename,exports);if(filename.endsWith('/detail/DetailRelationsPanel.vue')||filename.endsWith('\\detail\\DetailRelationsPanel.vue')){exports.default={props:['file','allowEdit'],setup:p=>()=>Vue.h('section',{'data-testid':'relation-child-boundary'},JSON.stringify(p))};return exports}let source=readFileSync(filename,'utf8');if(filename.endsWith('.vue'))source=compileScript(parse(source,{filename}).descriptor,{id:filename,inlineTemplate:true}).content
 new Function('exports','require','crypto','window',ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,name=>{
 if(name==='vue')return Vue;if(name==='vue-router')return{useRoute:()=>({fullPath:'/dcc/controlled-file/browser'}),useRouter:()=>({push:async r=>routes.push(r)})};if(name==='@/config/axios')return{default:request};if(name==='@/api/dcc/controlledFile/workflow'){
  const apiSource=readFileSync(resolve(root,'src/api/dcc/controlledFile/workflow.ts'),'utf8');const from=apiSource.indexOf('export const getControlledFile =');const next=apiSource.indexOf('\nexport const ',from+1)
  const exports={};new Function('exports','request',ts.transpileModule(apiSource.slice(from,next),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,request);return exports
 };if(name==='@/utils/auth')return{getTenantId:()=>1,getVisitTenantId:()=>undefined};if(name==='@/store/modules/user')return{useUserStore:()=>({getUser:{id:actor}})}
 return load(resolve(name.startsWith('@/')?resolve(root,'src'):dirname(filename),(name.startsWith('@/')?name.slice(2):name)+(name.endsWith('.vue')?'':'.ts')))
 },globalThis.crypto,{open:url=>{previews.push(url);return{opener:'test'}}});return exports}
 const r=make('root'),app=renderer.createApp(load(resolve(root,'src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue')).default);app.config.warnHandler=(warning)=>{if(!warning.startsWith("Failed to resolve")&&!warning.startsWith("Runtime directive"))throw new Error(warning)}
 for(const t of ['el-dialog','el-alert','el-button','el-input','el-tree','el-tabs','el-tab-pane','el-pagination','el-tag','el-checkbox','el-empty','el-select','el-option','el-option-group'])app.component(t,{inheritAttrs:false,setup:(_,c)=>()=>Vue.h(t,c.attrs,[c.slots.default?.(),c.slots.footer?.()])})
 app.component('el-table',{props:['data'],setup:(p,{slots})=>{Vue.provide('rows',Vue.toRef(p,'data'));return()=>Vue.h('el-table',{},slots.default?.())}})
 app.component('el-table-column',{inheritAttrs:false,setup:(_,c)=>{const rows=Vue.inject('rows');return()=>Vue.h('el-table-column',c.attrs,rows.value.map(row=>c.slots.default?c.slots.default({row}):String(c.attrs.prop?.split('.').reduce((v,k)=>v?.[k],row)??'')))}})
 app.directive('loading',{});app.mount(r);return{r,calls,routes,previews,app}
}
test('mounted public panel uses global query and server totals, name visibility never enables body',async()=>{const m=mount();try{await settle();assert.ok(content(m.r).includes('正式原文.pdf'));assert.equal(find(m.r,'el-button','正文').props.disabled,true);assert.equal(nodes(m.r,n=>n.type==='el-pagination').at(-1).props.total,81);const q=m.calls.find(([,a])=>a.url==='/dcc/controlled-files/browser-page')[1].params;assert.equal(q.browserScope,'GLOBAL');assert.equal(q.selectorScope,undefined);assert.equal(q.dccProjectCodeId,undefined);find(m.r,'el-button','操作面板').props.onClick();await settle();assert.equal(m.routes[0].path,`/dcc/controlled-file/detail/${ID}`)}finally{m.app.unmount();}})
test('mounted panel connects real folder identities into D reference selector and leader permission',async()=>{const m=mount();try{await settle();find(m.r,'el-table').props.onRowClick(project);await settle();const tree=find(m.r,'el-tree');tree.props.onNodeClick(tree.props.data[0]);await settle();assert.ok(find(m.r,'el-button','引用'));find(m.r,'el-button','引用').props.onClick();await settle();assert.ok(content(m.r).includes('确认引用'));const last=m.calls.filter(([,a])=>a.url==='/dcc/controlled-files/browser-page').at(-1)[1].params;assert.equal(last.projectFolderId,F);assert.equal(last.directoryId,undefined);assert.ok(content(m.r).includes('引用项目：2'))}finally{m.app.unmount();}})
test('mounted panel never shows write entry to a different enabled/administrator identity',async()=>{const m=mount('1');try{await settle();find(m.r,'el-table').props.onRowClick(project);await settle();const tree=find(m.r,'el-tree');tree.props.onNodeClick(tree.props.data[0]);await settle();assert.equal(nodes(m.r,n=>n.type==='el-button'&&content(n)==='引用').length,0)}finally{m.app.unmount();}})

test('mounted project list displays each version of the same Master and opens the clicked version',async()=>{
 const working={...row,id:'9007199254740996',versionNo:'A/1-1',status:'WORKING',controlled:false,pendingEffect:false,executable:false}
 const pending={...row,id:'9007199254740997',versionNo:'A/2',status:'PENDING_MATRIX_REVIEW',controlled:false,pendingEffect:false,executable:false}
 const m=mount(U,[working,pending,row])
 try{
  await settle()
  assert.equal(nodes(m.r,n=>n.type==='el-button'&&content(n)==='操作面板').length,1,'default query shows only the latest controlled identity')
  const filter=nodes(m.r,n=>n.type==='el-select'&&n.props['data-testid']==='dcc-project-browser-version-view')[0]
  assert.ok(filter,'version view picker must remain a real select')
  filter.props['onUpdate:modelValue']('ALL');await filter.props.onChange('ALL');await settle()
  assert.ok(content(m.r).includes('工作版本'));assert.ok(content(m.r).includes('待会签审核'));assert.ok(content(m.r).includes('受控（待生效）'))
  const actions=nodes(m.r,n=>n.type==='el-button'&&content(n)==='操作面板')
  assert.equal(actions.length,3)
  actions[1].props.onClick();await settle();assert.equal(m.routes[0].path,`/dcc/controlled-file/detail/${pending.id}`)
  assert.equal(nodes(m.r,n=>n.type==='el-button'&&content(n)==='正文').every(n=>n.props.disabled),true)
 }finally{m.app.unmount()}
})

test('mounted version view starts latest controlled and switches to formal working, approval and history statuses',async()=>{
 const m=mount()
 try{
  await settle();const initial=m.calls.find(([,a])=>a.url==='/dcc/controlled-files/browser-page')[1].params
  assert.equal(initial.latestVersionOnly,true);assert.equal(initial.status,undefined)
  const filter=nodes(m.r,n=>n.type==='el-select'&&n.props['data-testid']==='dcc-project-browser-version-view')[0]
  assert.ok(filter)
  for(const [view,status] of [['WORKING','WORKING'],['PENDING_MATRIX_REVIEW','PENDING_MATRIX_REVIEW'],['SUPERSEDED','SUPERSEDED'],['ALL',undefined]]){
   filter.props['onUpdate:modelValue'](view);await filter.props.onChange(view);await settle()
   const query=m.calls.filter(([,a])=>a.url==='/dcc/controlled-files/browser-page').at(-1)[1].params
   assert.equal(query.latestVersionOnly,false);assert.equal(query.status,status);assert.equal(query.selectorScope,undefined)
  }
 }finally{m.app.unmount()}
})

test('source and reference filename itself are orange and final exact cancellation restores source name',async()=>{
 const m=mount(U,[row],{references:true,usageCount:1})
 try{
  await settle();let sourceName=nodes(m.r,n=>n.props['data-testid']==='dcc-project-browser-file-name')[0]
  assert.ok(sourceName);assert.ok(sourceName.props.class.includes('is-referenced'))
  find(m.r,'el-table').props.onRowClick(project);await settle();const tree=find(m.r,'el-tree');tree.props.onNodeClick(tree.props.data[0]);await settle()
  const referenceName=nodes(m.r,n=>n.props['data-testid']==='dcc-project-reference-file-name')[0]
  assert.ok(referenceName);assert.ok(referenceName.props.class.includes('is-reference'));assert.equal(content(referenceName),'引用原文.pdf')
  find(m.r,'el-button','取消引用').props.onClick();await settle()
  nodes(m.r,n=>n.type==='el-input'&&n.props.placeholder==='取消原因（必填）')[0].props['onUpdate:modelValue']('正式取消最后引用');await settle()
  find(m.r,'el-button','确认取消引用').props.onClick();await settle()
  const cancelled=m.calls.find(([verb,a])=>verb==='post'&&a.url.endsWith('/cancel'))[1].data
  assert.equal(cancelled.referenceId,'19');assert.equal(cancelled.masterId,M);assert.equal(cancelled.folderId,F)
  sourceName=nodes(m.r,n=>n.props['data-testid']==='dcc-project-browser-file-name')[0]
  assert.ok(!sourceName.props.class.includes('is-referenced'))
  assert.equal(nodes(m.r,n=>n.props['data-testid']==='dcc-project-reference-file-name').length,0)
  assert.ok(content(m.r).includes('引用项目：0'))
 }finally{m.app.unmount()}
})

test('filename orange styles are scoped to formal source usage and actual reference rows',()=>{
 const source=readFileSync(resolve(root,'src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue'),'utf8')
 const reference=readFileSync(resolve(root,'src/views/dcc/controlled-file/relations/DccProjectReferences.vue'),'utf8')
 assert.match(source, /\.project-file-name\.is-referenced\s*\{\s*color:\s*#b85a00;/)
 assert.match(reference, /\.reference-file-name\.is-reference\s*\{\s*color:\s*#b85a00;/)
 assert.match(source, /state\.usage\[row\.masterId\]\?\.referenced === true/)
})

const openFolder=async m=>{await settle();find(m.r,'el-table').props.onRowClick(project);await settle();const tree=find(m.r,'el-tree');tree.props.onNodeClick(tree.props.data[0]);await settle()}
test('source reference count opens authorized usage details with separate global and visible counts',async()=>{
 const m=mount()
 try{
  await settle()
  const button=nodes(m.r,n=>n.props['data-testid']==='dcc-project-reference-usage')[0]
  assert.ok(button,'reference count needs a real usage-detail entry')
  await button.props.onClick();await settle()
  const call=m.calls.find(([,args])=>args.url==='/dcc/project-file-references/usage-page')
  assert.ok(call);assert.equal(call[1].params.selectedFileId,ID)
  assert.ok(content(m.r).includes('被引用目的项目'));assert.ok(content(m.r).includes('目的质量文件夹'))
  assert.ok(content(m.r).includes('全部引用项目：2'));assert.ok(content(m.r).includes('可查看引用项目：1'))
  assert.ok(nodes(m.r,n=>n.type==='el-alert'&&String(n.props.title).includes('权限范围')).length)
  assert.equal(nodes(m.r,n=>n.props['data-testid']==='dcc-reference-usage-preview')[0].props.disabled,true)
 }finally{m.app.unmount()}
})
test('usage details failed read is visible without rendering zero counts or enabling body',async()=>{
 const m=mount(U,[row],{usagePage:async()=>{throw Error('引用明细正式读取失败')}})
 try{
  await settle();nodes(m.r,n=>n.props['data-testid']==='dcc-project-reference-usage')[0].props.onClick();await settle()
  assert.ok(nodes(m.r,n=>n.type==='el-alert'&&String(n.props.title).includes('正式读取失败')).length)
  assert.ok(!content(m.r).includes('全部引用项目：0'))
  assert.equal(nodes(m.r,n=>n.props['data-testid']==='dcc-reference-usage-preview').length,0)
 }finally{m.app.unmount()}
})
test('a closed or switched usage context ignores the late authorized page',async()=>{
 for(const cancel of ['close','scope','unmount']){
  let release,unmounted=false
  const m=mount(U,[row],{usagePage:()=>new Promise(resolve=>{release=resolve})})
  try{
   await settle();nodes(m.r,n=>n.props['data-testid']==='dcc-project-reference-usage')[0].props.onClick();await settle()
   if(cancel==='close')find(m.r,'el-button','关闭').props.onClick()
   else if(cancel==='scope')find(m.r,'el-tabs').props.onTabChange('directory')
   else{m.app.unmount();unmounted=true}
   await settle()
   release({tenantId:'1',sourceControlledFileId:ID,masterId:M,referenceProjectCount:1,visibleReferenceProjectCount:1,total:1,detailsRestricted:false,
    list:[{referenceId:'19',projectId:P,projectName:'迟到的引用目的项目',folderId:F,folderName:'目录',masterId:M,selectedControlledFileId:ID,fileName:'引用原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'ACTIVE',controlled:true,pendingEffect:false,executable:true,canPreview:false}]})
   await settle();assert.ok(!content(m.r).includes('迟到的引用目的项目'));assert.equal(m.previews.length,0)
  }finally{if(!unmounted)m.app.unmount()}
 }
})
test('usage body rechecks the fixed selected version and a revoked permission opens no window',async()=>{
 const m=mount(U,[row],{canPreview:false,usagePage:async()=>({tenantId:'1',sourceControlledFileId:ID,masterId:M,referenceProjectCount:1,visibleReferenceProjectCount:1,total:1,detailsRestricted:false,
  list:[{referenceId:'19',projectId:P,projectName:'目的项目',folderId:F,folderName:'目录',masterId:M,selectedControlledFileId:ID,fileName:'引用原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canPreview:true}]})})
 try{
  await settle();nodes(m.r,n=>n.props['data-testid']==='dcc-project-reference-usage')[0].props.onClick();await settle()
  const body=nodes(m.r,n=>n.props['data-testid']==='dcc-reference-usage-preview')[0];assert.equal(body.props.disabled,false)
  await body.props.onClick();await settle();assert.equal(m.previews.length,0)
  assert.ok(nodes(m.r,n=>n.type==='el-alert'&&String(n.props.title).includes('没有')).length)
  assert.ok(m.calls.some(([,args])=>args.url===`/dcc/controlled-files/${ID}/relation-permissions`))
 }finally{m.app.unmount()}
})
test('saved reference usage entry keeps its exact fixed source identity and introduces no edit action',async()=>{
 const m=mount(U,[row],{references:true})
 try{
  await openFolder(m)
  const button=nodes(m.r,n=>n.props['data-testid']==='dcc-saved-reference-usage')[0];assert.ok(button)
  button.props.onClick();await settle()
  assert.equal(m.calls.find(([,args])=>args.url==='/dcc/project-file-references/usage-page')[1].params.selectedFileId,ID)
  assert.ok(content(m.r).includes('引用使用明细')||nodes(m.r,n=>n.type==='el-dialog'&&n.props.title==='引用使用明细').length)
  assert.equal(m.calls.filter(([verb])=>verb==='post').length,0)
 }finally{m.app.unmount()}
})
test('a name-visible list row opens the exact lightweight association panel without management detail read',async()=>{
 const m=mount(U,[row],{traceDenied:true,permissions:async()=>({controlledFileId:ID,tenantId:'1',masterId:M,projectId:P,projectName:'正式项目',projectFolderId:F,projectFolderName:'质量文件',fileName:row.fileName,fileNumber:row.fileNumber,versionNo:row.versionNo,status:row.status,controlled:true,pendingEffect:true,executable:false,canEdit:false,canPreview:false})})
 try{
  await settle()
  const button=nodes(m.r,n=>n.type==='el-button'&&n.props['data-testid']==='dcc-project-file-relations')[0]
  assert.ok(button,'list association button must exist')
  await button.props.onClick();await settle()
  const boundary=nodes(m.r,n=>n.props['data-testid']==='relation-child-boundary')[0]
  assert.ok(boundary)
  const file=JSON.parse(content(boundary)).file
  assert.equal(file.id,ID);assert.equal(file.masterId,M);assert.equal(file.dccProjectCodeId,P)
  assert.equal(file.versionNo,row.versionNo)
  assert.equal(m.calls.filter(([,args])=>args.url===`/dcc/controlled-files/${ID}`).length,0)
  assert.equal(m.routes.length,0)
 }finally{m.app.unmount()}
})
test('association metadata with a different tenant or master fails visibly without mounting the panel',async()=>{
 const m=mount(U,[row],{permissions:async()=>({controlledFileId:ID,tenantId:'2',masterId:'700',projectId:P,projectName:'正式项目',projectFolderId:F,projectFolderName:'质量文件',fileName:row.fileName,fileNumber:row.fileNumber,versionNo:row.versionNo,status:row.status,controlled:true,pendingEffect:true,executable:false,canEdit:true,canPreview:true})})
 try{
  await settle();await nodes(m.r,n=>n.props['data-testid']==='dcc-project-file-relations')[0].props.onClick();await settle()
  assert.equal(nodes(m.r,n=>n.props['data-testid']==='relation-child-boundary').length,0)
  assert.ok(nodes(m.r,n=>n.type==='el-alert'&&String(n.props.title).includes('所选文件')).length)
  assert.equal(m.routes.length,0)
 }finally{m.app.unmount()}
})
test('closing or switching list context prevents a pending association read from mounting old file evidence',async()=>{
 for(const cancel of ['close','scope','unmount']){
  let release
  const m=mount(U,[row],{permissions:()=>new Promise(resolve=>{release=resolve})})
  let unmounted=false
  try{
   await settle();await nodes(m.r,n=>n.props['data-testid']==='dcc-project-file-relations')[0].props.onClick();await settle()
   if(cancel==='close')find(m.r,'el-button','关闭').props.onClick()
   else if(cancel==='scope')find(m.r,'el-tabs').props.onTabChange('directory')
   else{m.app.unmount();unmounted=true}
   await settle()
   release({controlledFileId:ID,tenantId:'1',masterId:M,projectId:P,projectName:'正式项目',projectFolderId:F,projectFolderName:'质量文件',fileName:row.fileName,fileNumber:row.fileNumber,versionNo:row.versionNo,status:row.status,controlled:true,pendingEffect:true,executable:false,canEdit:true,canPreview:false})
   await settle()
   assert.equal(nodes(m.r,n=>n.props['data-testid']==='relation-child-boundary').length,0)
   assert.equal(m.routes.length,0)
  }finally{if(!unmounted)m.app.unmount()}
 }
})
test('saved reference body opens the pinned version while stronger read-only trace authorization can reject',async()=>{
 const m=mount(U,[row],{references:true,canPreview:true,traceDenied:true})
 try{
  await openFolder(m)
  const body=nodes(m.r,n=>n.type==='el-button'&&n.props['data-testid']==='dcc-project-reference-preview')[0]
  assert.ok(body);assert.equal(body.props.disabled,false);await body.props.onClick();await settle()
  assert.ok(m.previews[0].includes(`/detail/${ID}?viewer=1`));assert.equal(m.routes.length,0)
  const trace=nodes(m.r,n=>n.type==='el-button'&&n.props['data-testid']==='dcc-project-reference-trace')[0]
  assert.ok(trace);await trace.props.onClick();await settle()
  assert.equal(m.routes.length,0);assert.ok(nodes(m.r,n=>n.type==='el-alert').some(n=>String(n.props.title).includes('正式详情独立授权拒绝')))
  assert.ok(content(m.r).includes('引用原文.pdf'))
 }finally{m.app.unmount()}
})

test('name-only saved reference stays visible with body disabled and trace success uses independent readonly route',async()=>{
 const m=mount(U,[row],{references:true,canPreview:false})
 try{
  await openFolder(m)
  const body=nodes(m.r,n=>n.type==='el-button'&&n.props['data-testid']==='dcc-project-reference-preview')[0]
  assert.ok(body);assert.equal(body.props.disabled,true)
  const trace=nodes(m.r,n=>n.type==='el-button'&&n.props['data-testid']==='dcc-project-reference-trace')[0]
  assert.ok(trace);await trace.props.onClick();await settle()
  assert.equal(m.previews.length,0);assert.ok(m.routes[0].includes(`/detail/${ID}?traceability=1`));assert.ok(m.routes[0].includes('from=browser'))
 }finally{m.app.unmount()}
})

test('unmounted saved-reference read cannot open a late preview window',async()=>{
 let finish,reads=0
 const projection={controlledFileId:ID,tenantId:'1',masterId:M,projectId:P,projectName:'正式项目',projectFolderId:F,projectFolderName:'质量文件',fileName:'引用原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canEdit:false,canPreview:true}
 const m=mount(U,[row],{references:true,permissions:()=>++reads===1?projection:new Promise(resolve=>{finish=resolve})})
 await openFolder(m)
 const body=nodes(m.r,n=>n.type==='el-button'&&n.props['data-testid']==='dcc-project-reference-preview')[0]
 const reading=body.props.onClick();await settle();m.app.unmount();finish(projection);await reading;await settle()
 assert.equal(m.previews.length,0)
})

test('reference action rechecks current selected-version permission and rejects revoked body read without hiding its name',async()=>{
 let reads=0
 const projection={controlledFileId:ID,tenantId:'1',masterId:M,projectId:P,projectName:'正式项目',projectFolderId:F,projectFolderName:'质量文件',fileName:'引用原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canEdit:false,canPreview:true}
 const m=mount(U,[row],{references:true,permissions:()=>({...projection,canPreview:++reads===1})})
 try{
  await openFolder(m);await nodes(m.r,n=>n.type==='el-button'&&n.props['data-testid']==='dcc-project-reference-preview')[0].props.onClick();await settle()
  assert.equal(m.previews.length,0);assert.ok(nodes(m.r,n=>n.type==='el-alert').some(n=>String(n.props.title).includes('正文查看权限')))
  assert.ok(content(m.r).includes('引用原文.pdf'))
 }finally{m.app.unmount()}
})
