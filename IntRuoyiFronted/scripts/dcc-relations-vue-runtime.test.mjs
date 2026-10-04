import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'
import test from 'node:test'
import path from 'node:path'

const require=createRequire(import.meta.url)
const Vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const components=new Map()
let selectorNavigationFixture={source:null,directories:[]}
const navigationModules=new Map()
const navigationModule=file=>{
  if(navigationModules.has(file))return navigationModules.get(file)
  const exports={};navigationModules.set(file,exports)
  const code=ts.transpileModule(readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
  const resolve=module=>{
    if(module==='@/config/axios')return {default:{get:async request=>{
      const fixture=selectorNavigationFixture,source=fixture.source
      if(request.url==='/dcc/project-codes/page')return {list:source?[{id:source.projectId,projectName:source.projectName,projectCode:'fixture',status:'ENABLE'}]:[],total:source?1:0}
      const project=request.url.match(/\/project-codes\/([0-9]+)\/folders$/)?.[1]
      if(!project)throw new Error('Unexpected selector navigation request '+request.url)
      const rows=[]
      const visit=(nodes,parent='0')=>nodes.forEach(node=>{if(node.projectId!==project)return;const folder=node.folderId;if(folder)rows.push({id:folder,projectCodeId:project,parentId:parent,name:node.name,active:true,sortOrder:rows.length});visit(node.children||[],folder||parent)})
      visit(fixture.directories)
      if(!rows.length&&source?.projectId===project&&source.folderId)rows.push({id:source.folderId,projectCodeId:project,parentId:'0',name:source.folderName,active:true,sortOrder:0})
      return rows
    }}}
    const root=fileURLToPath(new URL('../src/',import.meta.url))
    return navigationModule(module.startsWith('@/')?path.join(root,module.slice(2)+'.ts'):path.resolve(path.dirname(file),module+'.ts'))
  }
  new Function('exports','require',code)(exports,resolve);return exports
}
const loadComponent=name=>{
  if(components.has(name))return components.get(name)
  const filename=fileURLToPath(new URL(`../src/views/dcc/controlled-file/relations/${name}`,import.meta.url))
  const {descriptor}=parse(readFileSync(filename,'utf8'),{filename})
  const source=compileScript(descriptor,{id:name,inlineTemplate:true}).content
  const context={exports:{},Error,Map,Array,require:module=>{
    if(module==='vue')return Vue
    if(module==='./selector-project-directory')return navigationModule(fileURLToPath(new URL('../src/views/dcc/controlled-file/relations/selector-project-directory.ts',import.meta.url)))
    if(module==='./selector-state'||module==='./relation-editor-state'||module==='./arrangement-form'){
      const exports={}
      const compiled=ts.transpileModule(readFileSync(new URL(`../src/views/dcc/controlled-file/relations/${module.slice(2)}.ts`,import.meta.url),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
      new Function('exports',compiled)(exports)
      return exports
    }
    if(module.endsWith('.vue'))return {__esModule:true,default:loadComponent(module.slice(2))}
    throw new Error(`Unexpected component dependency ${module}`)
  },crypto:globalThis.crypto}
  // Evaluate generated owned SFC code in the same realm as Vue. Foreign realm reactive Array.map
  // returns a proxied children array whose vnode normalization mutates its own render dependencies.
  const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
  new Function('exports','require','crypto',compiled)(context.exports,context.require,globalThis.crypto)
  components.set(name,context.exports.default);return context.exports.default
}
const makeNode=(type,text='')=>({type,text,children:[],props:{},parent:null})
const renderer=Vue.createRenderer({
  createElement:type=>makeNode(type),createText:text=>makeNode('#text',text),createComment:text=>makeNode('#comment',text),
  setText:(node,text)=>{node.text=text},setElementText:(node,text)=>{node.text=text;node.children=[]},
  patchProp:(node,key,oldValue,value)=>{node.props[key]=value},
  insert:(node,parent,anchor)=>{if(node.parent){const i=node.parent.children.indexOf(node);if(i>=0)node.parent.children.splice(i,1)}node.parent=parent;const i=anchor?parent.children.indexOf(anchor):-1;i<0?parent.children.push(node):parent.children.splice(i,0,node)},
  remove:node=>{if(node.parent){const i=node.parent.children.indexOf(node);if(i>=0)node.parent.children.splice(i,1)}node.parent=null},
  parentNode:node=>node.parent,nextSibling:node=>node.parent?.children[node.parent.children.indexOf(node)+1]??null,
  querySelector:()=>null,setScopeId:()=>{},insertStaticContent:(content,parent,anchor)=>{const node=makeNode('#static',content);node.parent=parent;parent.children.push(node);return [node,node]}
})
const textContent=node=>node.text+node.children.map(textContent).join('')
const nodes=(node,predicate)=>[...(predicate(node)?[node]:[]),...node.children.flatMap(child=>nodes(child,predicate))]
const find=(root,type,label)=>nodes(root,node=>node.type===type&&node.props['data-testid']!=='dcc-selector-project-pagination'&&(!label||textContent(node).includes(label)))[0]
const mount=(name,props)=>{
  selectorNavigationFixture={get source(){return props.source||props.selectorSource},get directories(){return props.directories||[]}}
  const root=makeNode('root'),app=renderer.createApp({setup:()=>()=>Vue.h(loadComponent(name),{...props})})
  app.config.warnHandler=()=>{}
  for(const type of ['el-dialog','el-alert','el-button','el-input','el-tree','el-tabs','el-tab-pane','el-pagination','el-tag','el-checkbox','el-date-picker','el-select','el-option']){
    app.component(type,{name:type,inheritAttrs:false,setup:(_,context)=>()=>Vue.h(type,context.attrs,[context.slots.default?.(),context.slots.footer?.()])})
  }
  app.component('el-table',{name:'DTestTable',props:['data'],setup:(props,{slots})=>{Vue.provide('test-table-rows',Vue.toRef(props,'data'));return ()=>Vue.h('el-table',{},slots.default?.())}})
  app.component('el-table-column',{name:'DTestColumn',inheritAttrs:false,setup:(_,context)=>{const rows=Vue.inject('test-table-rows');return ()=>Vue.h('el-table-column',{...context.attrs},context.slots.default?rows.value.map(row=>context.slots.default({row})):rows.value.map(row=>{
    const field=context.attrs.prop;return field?String(field.split('.').reduce((value,key)=>value?.[key],row)??''):''
  }))}})
  app.directive('loading',{})
  app.mount(root);return {root,exposed:()=>app._instance.subTree.component.exposed,unmount:()=>app.unmount()}
}
const flush=async()=>{await Promise.resolve();await Vue.nextTick();await Promise.resolve();await Vue.nextTick()}
const settle=async()=>{for(let n=0;n<10;n++)await flush()}
// Actual B state exports satisfy the synchronized H07 runtime dependency without substituting validation.
const projectAttributeFixture=(()=>{
  const exports={};const compiled=ts.transpileModule(readFileSync(new URL('../src/views/dcc/controlled-file/project-attributes/state.ts',import.meta.url),'utf8'),
    {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
  new Function('exports',compiled)(exports);return exports
})()
const rootSelectorLoader=(transport,tenantId='1')=>{
  const exports={};const compiled=ts.transpileModule(readFileSync(new URL('../src/api/dcc/controlledFile/applicationRead.ts',import.meta.url),'utf8'),
    {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
  new Function('exports','require',compiled)(exports,name=>{
    if(name==='@/config/axios')return {default:{get:transport}}
    if(name==='@/views/dcc/controlled-file/project-attributes/state')return projectAttributeFixture
    throw new Error(`Unexpected formal Root dependency ${name}`)
  })
  return query=>exports.loadDccSelectorPage(tenantId,query)
}
const source={contextKey:'tenant1:source10',tenantId:'1',masterId:'10',projectId:'1',folderId:'11',projectName:'项目A',folderName:'主文件',fileName:'源.pdf',fileNumber:'S-10',versionNo:'A/1'}
const candidate={tenantId:'1',controlledFileId:'200',masterId:'20',projectId:'2',projectName:'项目B',folderName:'质量',fileName:'跨项目.pdf',fileNumber:'N-200',versionNo:'B/1',status:'ACTIVE',controlled:true,pendingEffect:true,executable:false,canPreview:false}
test('mounted badge restores source color at zero while retaining obsolete trace and reference identity',async()=>{
  const props=Vue.reactive({isReference:false,projectCount:1,sourceProjectName:'正式源项目',obsolete:false,pendingEffect:false})
  const mounted=mount('DccReferenceBadge.vue',props)
  try{
    await settle();assert.equal(find(mounted.root,'span').props.class,'referenced');assert.ok(textContent(mounted.root).includes('引用项目：1'))
    props.projectCount=0;props.obsolete=true;await settle();assert.equal(find(mounted.root,'span').props.class,'');assert.ok(textContent(mounted.root).includes('源文件已作废'))
    props.isReference=true;props.projectCount=2;props.pendingEffect=true;await settle()
    assert.equal(find(mounted.root,'span').props.class,'referenced');assert.ok(textContent(mounted.root).includes('引用 · 来源项目：正式源项目'))
    assert.ok(textContent(mounted.root).includes('源文件已作废'));assert.ok(!textContent(mounted.root).includes('待生效'))
  }finally{mounted.unmount()}
})
test('list association entry opens the same selector once after actual editable current-context read, with no write',async()=>{
 let reads=0,writes=0
 const mounted=mount('DccFileRelations.vue',{mode:'current',source:{...source,controlledFileId:'100'},directories:[],canEdit:true,autoOpenEditor:true,modelValue:[],
  loadPage:async()=>{reads++;return{total:1,list:[candidate]}},loadCurrent:async()=>({sourceControlledFileId:'100',rowVersion:'1',files:[candidate]}),
  loadHistory:async()=>[],persistCurrent:async()=>{writes++},openPreview:async()=>{}})
 try{await settle();assert.equal(find(mounted.root,'el-dialog').props['model-value'],true);assert.equal(reads,1);assert.equal(writes,0);assert.ok(textContent(mounted.root).includes('已选 1 项'))}
 finally{mounted.unmount()}
})
test('read-only list relation entry never auto-opens editing or reads candidate pages',async()=>{
 let reads=0
 const mounted=mount('DccFileRelations.vue',{mode:'current',source:{...source,controlledFileId:'100'},directories:[],canEdit:false,autoOpenEditor:true,modelValue:[],
  loadPage:async()=>{reads++;return{total:0,list:[]}},loadCurrent:async()=>({sourceControlledFileId:'100',rowVersion:'1',files:[candidate]}),
  loadHistory:async()=>[],persistCurrent:async()=>{throw Error('readonly')},openPreview:async()=>{}})
 try{await settle();assert.equal(find(mounted.root,'el-dialog').props['model-value'],false);assert.equal(reads,0)}finally{mounted.unmount()}
})
test('mounted Vue selector keeps initial file, displays source directory and pending effect, and saves only on confirm',async()=>{
  let saved=0,result;const props={modelValue:true,source,purpose:'relations',directories:[],selected:[],loadPage:async()=>({total:1,list:[candidate]}),persist:async(rows)=>{saved++;result=rows},openPreview:async()=>{throw new Error('must not preview without content permission')}}
  const mounted=mount('DccFileSelector.vue',props)
  try{
    await settle();assert.ok(textContent(mounted.root).includes('源.pdf'));assert.ok(textContent(mounted.root).includes('项目B'));assert.ok(textContent(mounted.root).includes('质量'));assert.ok(textContent(mounted.root).includes('待生效'))
    const preview=find(mounted.root,'el-button','查看');assert.equal(preview.props.disabled,true)
    const select=find(mounted.root,'el-checkbox');select.props.onChange(true);await flush();assert.equal(saved,0)
    find(mounted.root,'el-button','确认关联').props.onClick();await flush();assert.equal(saved,1);assert.equal(result[0].controlledFileId,'200')
  }finally{mounted.unmount()}
})
test('mounted Vue selector failure keeps selected state and exposes server error without success event',async()=>{
  const events=[];const mounted=mount('DccFileSelector.vue',{modelValue:true,source,purpose:'relations',directories:[],selected:[candidate],loadPage:async()=>({total:1,list:[candidate]}),persist:async()=>{throw new Error('真实保存拒绝')},openPreview:async()=>{},onConfirmed:rows=>events.push(rows)})
  try{await flush();find(mounted.root,'el-button','确认关联').props.onClick();await flush();assert.equal(events.length,0);assert.equal(find(mounted.root,'el-alert').props.title,'真实保存拒绝');assert.ok(textContent(mounted.root).includes('已选 1 项'))}
  finally{mounted.unmount()}
})
test('mounted Vue project references open the shared chooser and execute one batch call with real counts',async()=>{
  let writes=0;const events=[];const mounted=mount('DccProjectReferences.vue',{contextKey:'tenant1:p2:f21',projectId:'2',folderId:'21',folderName:'质量',isProjectLeader:true,selectorSource:{...source,contextKey:'tenant1:p2:f21'},directories:[],loadReferences:async()=>[],loadPage:async()=>({total:1,list:[candidate]}),openPreview:async()=>{},cancelReference:async()=>0,
    createReferences:async(project,folder,ids,reason)=>{writes++;assert.equal(project,'2');assert.equal(folder,'21');assert.deepEqual([...ids],['200']);assert.equal(reason,'批准引用');return [{reference:{id:'1',masterId:'20'},selectedVersion:candidate,referenceProjectCount:3,sourceProjectName:'项目B'}]},onChanged:(...args)=>events.push(args)})
  try{
    await settle();find(mounted.root,'el-button','引用').props.onClick();await settle()
    const select=find(mounted.root,'el-checkbox');select.props.onChange(true);await flush()
    const input=nodes(mounted.root,node=>node.type==='el-input'&&node.props.placeholder==='引用原因（必填）')[0];input.props['onUpdate:modelValue']('批准引用');await flush()
    find(mounted.root,'el-button','确认引用').props.onClick();await flush();assert.equal(writes,1);assert.deepEqual(events[0],[3,'20']);assert.ok(textContent(mounted.root).includes('引用项目：3'))
  }finally{mounted.unmount()}
})
test('mounted references prevent starting a mutation while the folder list is loading',async()=>{
  let finish,pageLoads=0
  const mounted=mount('DccProjectReferences.vue',{contextKey:'loading-folder',projectId:'2',folderId:'21',folderName:'质量',isProjectLeader:true,selectorSource:source,directories:[],
    loadReferences:()=>new Promise(resolve=>{finish=resolve}),loadPage:async()=>{pageLoads++;return {total:1,list:[candidate]}},openPreview:async()=>{},cancelReference:async()=>0,createReferences:async()=>[]})
  try{
    await settle();const open=find(mounted.root,'el-button','引用');assert.equal(open.props.disabled,true)
    open.props.onClick();await settle();assert.equal(pageLoads,0)
    finish([]);await settle();assert.equal(find(mounted.root,'el-button','引用').props.disabled,false)
  }finally{finish?.([]);mounted.unmount()}
})
test('mounted reference batch locks sibling mutations and releases the lock after a visible failure',async()=>{
  let rejectCreate,cancelCalls=0
  const row={reference:{id:'1',masterId:'20'},selectedVersion:candidate,referenceProjectCount:1,sourceProjectName:'项目B'}
  const mounted=mount('DccProjectReferences.vue',{contextKey:'saving-folder',projectId:'2',folderId:'21',folderName:'质量',isProjectLeader:true,selectorSource:source,directories:[],
    loadReferences:async()=>[row],loadPage:async()=>({total:1,list:[candidate]}),openPreview:async()=>{},cancelReference:async()=>{cancelCalls++;return 0},createReferences:()=>new Promise((_resolve,reject)=>{rejectCreate=reject})})
  try{
    await settle();find(mounted.root,'el-button','引用').props.onClick();await settle();find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    nodes(mounted.root,node=>node.type==='el-input'&&node.props.placeholder==='引用原因（必填）')[0].props['onUpdate:modelValue']('引用提交');await settle()
    find(mounted.root,'el-button','确认引用').props.onClick();await settle()
    assert.equal(find(mounted.root,'el-button','引用').props.disabled,true);const cancel=find(mounted.root,'el-button','取消引用');assert.equal(cancel.props.disabled,true)
    cancel.props.onClick();await settle();assert.equal(nodes(mounted.root,node=>node.type==='el-dialog'&&node.props.title==='二次确认：取消引用')[0].props.modelValue,false)
    assert.equal(cancelCalls,0);rejectCreate(new Error('正式批量引用拒绝'));await settle()
    assert.equal(find(mounted.root,'el-button','引用').props.disabled,false);assert.equal(find(mounted.root,'el-button','取消引用').props.disabled,false)
    assert.ok(nodes(mounted.root,node=>node.type==='el-alert').some(node=>node.props.title==='正式批量引用拒绝'));assert.ok(textContent(mounted.root).includes('已选 1 项'))
  }finally{rejectCreate?.(new Error('test cleanup'));mounted.unmount()}
})
test('mounted old reference completion cannot unlock or alter a new folder batch',async()=>{
  const finish=new Map(),events=[]
  const props=Vue.reactive({contextKey:'folder2',projectId:'2',folderId:'21',folderName:'质量',isProjectLeader:true,selectorSource:{...source,contextKey:'folder2'},directories:[],
    loadReferences:async()=>[],loadPage:async()=>({total:1,list:[candidate]}),openPreview:async()=>{},cancelReference:async()=>0,
    createReferences:project=>new Promise(resolve=>finish.set(project,resolve)),onChanged:(...args)=>events.push(args)})
  const mounted=mount('DccProjectReferences.vue',props)
  const submit=async()=>{
    find(mounted.root,'el-button','引用').props.onClick();await settle();find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    nodes(mounted.root,node=>node.type==='el-input'&&node.props.placeholder==='引用原因（必填）')[0].props['onUpdate:modelValue']('正式目录引用');await settle()
    find(mounted.root,'el-button','确认引用').props.onClick();await settle()
  }
  try{
    await settle();await submit();assert.ok(finish.has('2'))
    props.contextKey='folder3';props.projectId='3';props.folderId='31';props.selectorSource={...source,contextKey:'folder3'};await settle();await submit();assert.ok(finish.has('3'))
    finish.get('2')([{reference:{id:'old',masterId:'20'},selectedVersion:candidate,referenceProjectCount:1,sourceProjectName:'旧目录'}]);await settle()
    assert.equal(find(mounted.root,'el-button','引用').props.disabled,true);assert.equal(events.length,0);assert.ok(!textContent(mounted.root).includes('旧目录'))
    finish.get('3')([{reference:{id:'new',masterId:'20'},selectedVersion:candidate,referenceProjectCount:2,sourceProjectName:'新目录'}]);await settle()
    assert.equal(find(mounted.root,'el-button','引用').props.disabled,false);assert.deepEqual(events,[[2,'20']]);assert.ok(textContent(mounted.root).includes('新目录'))
  }finally{finish.forEach(resolve=>resolve([]));mounted.unmount()}
})
test('mounted Vue current relations use formal source version/revision/key and retain immutable historic target separately',async()=>{
  let command;const changes=[];const initial={...source,controlledFileId:'100'}
  const mounted=mount('DccFileRelations.vue',{mode:'current',source:initial,directories:[],canEdit:true,modelValue:[],loadPage:async()=>({total:1,list:[candidate]}),loadCurrent:async()=>({sourceControlledFileId:'100',rowVersion:'0',files:[]}),loadHistory:async()=>{throw new Error('must not read history for current')},persistCurrent:async(id,value)=>{assert.equal(id,'100');command=value;return {sourceControlledFileId:'100',rowVersion:'1',relatedMasterIds:['20']}},openPreview:async()=>{},onChanged:value=>changes.push(value)})
  try{
    await settle();find(mounted.root,'el-button','关联').props.onClick();await settle();find(mounted.root,'el-checkbox').props.onChange(true);await flush()
    const input=nodes(mounted.root,node=>node.type==='el-input'&&node.props.placeholder==='关联操作原因（必填）')[0];input.props['onUpdate:modelValue']('本次关联变更');await flush()
    find(mounted.root,'el-button','确认关联').props.onClick();await flush()
    assert.equal(command.expectedVersion,'0');assert.equal(command.reason,'本次关联变更');assert.equal(command.selectedFileIds[0],'200');assert.ok(command.idempotencyKey)
    assert.equal(changes[0].rowVersion,'1')
  }finally{mounted.unmount()}
})
test('mounted Vue upload relation selection updates application only while history never calls mutable current query',async()=>{
  const updates=[];let writes=0
  const props=Vue.reactive({mode:'upload',source:{...source,unsubmitted:true},directories:[],canEdit:true,modelValue:[],loadPage:async()=>({total:1,list:[candidate]}),loadCurrent:async()=>{throw new Error('upload has no server source')},loadHistory:async()=>{throw new Error('upload has no history')},persistCurrent:async()=>{writes++;throw new Error('must not persist a source-less upload')},openPreview:async()=>{},'onUpdate:modelValue':rows=>{updates.push(rows);props.modelValue=rows}})
  const upload=mount('DccFileRelations.vue',props)
  try{await settle();find(upload.root,'el-button','关联').props.onClick();await settle();find(upload.root,'el-checkbox').props.onChange(true);await settle();find(upload.root,'el-button','确认关联').props.onClick();await settle();assert.equal(writes,0);assert.equal(updates[0][0].controlledFileId,'200');assert.ok(textContent(upload.root).includes('跨项目.pdf'))}
  finally{upload.unmount()}
})
test('mounted Vue frozen history only loads the immutable history and exposes no edit chooser',async()=>{
  const history=mount('DccFileRelations.vue',{mode:'history',source:{...source,controlledFileId:'100'},directories:[],canEdit:false,modelValue:[],loadPage:async()=>({total:0,list:[]}),loadCurrent:async()=>{throw new Error('history must never query current')},loadHistory:async()=>[{...candidate,controlledFileId:'199',versionNo:'A/1'}],persistCurrent:async()=>{throw new Error('history read only')},openPreview:async()=>{}})
  try{await flush();assert.ok(textContent(history.root).includes('本次审批当时的关联版本'));assert.ok(textContent(history.root).includes('A/1'));assert.ok(textContent(history.root).includes('历史审批版本'));assert.equal(find(history.root,'el-button','关联'),undefined)}finally{history.unmount()}
})
test('mounted current relation editor rejects a server source version different from the initial file',async()=>{
  const mounted=mount('DccFileRelations.vue',{mode:'current',source:{...source,controlledFileId:'100'},directories:[],canEdit:true,modelValue:[],loadPage:async()=>({total:0,list:[]}),loadCurrent:async()=>({sourceControlledFileId:'101',rowVersion:'2',files:[]}),loadHistory:async()=>[],persistCurrent:async()=>{throw new Error('must not save wrong source')},openPreview:async()=>{}})
  try{await settle();assert.ok(find(mounted.root,'el-alert')?.props.title.includes('版本已变化'));assert.equal(find(mounted.root,'el-button','关联')?.props.disabled,true)}finally{mounted.unmount()}
})
test('mounted relation panel content rejection is visible and does not become an unhandled error',async()=>{
  const props=Vue.reactive({mode:'history',source:{...source,controlledFileId:'100'},directories:[],canEdit:false,modelValue:[],loadPage:async()=>({total:0,list:[]}),loadCurrent:async()=>{throw new Error('history only')},loadHistory:async()=>[{...candidate,controlledFileId:'199',versionNo:'A/1',canPreview:true}],persistCurrent:async()=>{throw new Error('history read only')},openPreview:async()=>{throw new Error('正文权限已撤销')}})
  const mounted=mount('DccFileRelations.vue',props)
  try{await settle();find(mounted.root,'el-button','查看').props.onClick();await settle();assert.equal(find(mounted.root,'el-alert')?.props.title,'Error: 正文权限已撤销')}
  finally{mounted.unmount()}
})
test('mounted arrangement panel loads exact version round, validates explicit selections, and leaves unselected files empty',async()=>{
  const calls=[],changes=[]
  const mounted=mount('DccRelationArrangementPanel.vue',{contextKey:'t1:f100:r1',sourceFileId:'100',applicationRound:'r1',readonly:false,relations:[candidate],assignees:[{id:'8',name:'责任甲'}],loadArrangements:async(...args)=>{calls.push(args);return []},onChange:value=>changes.push(value)})
  try{
    await settle();assert.deepEqual(calls[0],['100','r1']);assert.equal(mounted.exposed().validate().length,0)
    find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    assert.throws(()=>mounted.exposed().validate(),/负责人/)
    find(mounted.root,'el-select').props['onUpdate:modelValue']('8');await settle()
    assert.throws(()=>mounted.exposed().validate(),/期限/)
    find(mounted.root,'el-date-picker').props['onUpdate:modelValue']('2026-10-03 12:00:00');await settle()
    const result=mounted.exposed().validate();assert.equal(result[0].relatedMasterId,'20');assert.equal(result[0].assigneeUserId,'8');assert.equal(result[0].dueAt,'2026-10-03 12:00:00')
    assert.equal(changes.at(-1)[0].assigneeUserId,'8')
  }finally{mounted.unmount()}
})
test('mounted arrangement panel refuses use after load failure and discards previous round late response',async()=>{
  let finish
  const props=Vue.reactive({contextKey:'r1',sourceFileId:'100',applicationRound:'r1',readonly:false,relations:[candidate],assignees:[{id:'8',name:'责任甲'}],loadArrangements:async(_id,round)=>round==='r1'?new Promise(resolve=>{finish=resolve}):[{relatedMasterId:'20',assigneeUserId:'8',dueAt:'2026-10-04 12:00:00'}]})
  const mounted=mount('DccRelationArrangementPanel.vue',props)
  try{
    await settle();assert.throws(()=>mounted.exposed().validate(),/尚未读取/)
    props.applicationRound='r2';props.contextKey='r2';await settle()
    finish([{relatedMasterId:'20',assigneeUserId:'8',dueAt:'2026-10-03 12:00:00'}]);await settle()
    assert.equal(mounted.exposed().validate()[0].dueAt,'2026-10-04 12:00:00')
    props.contextKey='r3';props.applicationRound='r3';props.loadArrangements=async()=>{throw new Error('审批上下文拒绝')};await settle()
    assert.ok(find(mounted.root,'el-alert')?.props.title.includes('审批上下文拒绝'));assert.throws(()=>mounted.exposed().validate(),/尚未读取/)
  }finally{mounted.unmount()}
})
test('mounted historical arrangement displays preserved selected identity and never allows a new signed payload',async()=>{
  const changes=[];const mounted=mount('DccRelationArrangementPanel.vue',{contextKey:'history-r1',sourceFileId:'100',applicationRound:'r1',readonly:true,relations:[candidate],assignees:[{id:'8',name:'原责任甲'}],loadArrangements:async()=>[{relatedMasterId:'20',assigneeUserId:'8',dueAt:'2026-10-03 12:00:00'}],onChange:value=>changes.push(value)})
  try{await settle();assert.equal(find(mounted.root,'el-select').props['model-value'],'8');assert.equal(find(mounted.root,'el-select').props.disabled,true);assert.equal(find(mounted.root,'el-date-picker').props['model-value'],'2026-10-03 12:00:00');assert.throws(()=>mounted.exposed().validate(),/只读/);assert.equal(changes.length,0)}finally{mounted.unmount()}
})
test('mounted selector locates and expands exact project folder path instead of identically named other project',async()=>{
  const directories=[{key:'p1',name:'项目A',projectId:'1',children:[{key:'nested1',name:'层级',projectId:'1',folderId:'10',children:[{key:'folder11',name:'质量',projectId:'1',folderId:'11'}]}]},{key:'p2',name:'项目B',projectId:'2',children:[{key:'other11',name:'质量',projectId:'2',folderId:'11'}]}]
  const mounted=mount('DccFileSelector.vue',{modelValue:true,source,purpose:'relations',directories,selected:[],loadPage:async()=>({total:0,list:[]}),persist:async()=>{},openPreview:async()=>{}})
  try{await settle();const tree=find(mounted.root,'el-tree');assert.equal(tree.props['current-node-key'],JSON.stringify(['1','1','11']));assert.deepEqual([...tree.props['default-expanded-keys']],[JSON.stringify(['1','1','10'])]);assert.ok('highlight-current' in tree.props);assert.notEqual(tree.props['highlight-current'],false)}finally{mounted.unmount()}
})
if(process.argv[2])test('mounted D selector uses formal Root loader and actual C response across directory global pagination and confirm',async()=>{
  const actual=JSON.parse(readFileSync(process.argv[2],'utf8')),calls=[],confirmed=[]
  const initial={...source,masterId:'2',controlledFileId:'2',fileName:'初始文件.pdf',projectId:'5',folderId:'500',projectName:'正式来源项目',folderName:'逻辑文件夹'}
  const loadPage=rootSelectorLoader(async request=>{
    calls.push(request.params);assert.equal(request.url,'/dcc/controlled-files/browser-page');assert.equal(request.params.directoryId,undefined)
    return request.params.selectorScope==='PROJECT_FOLDER'?actual.projectFolder:request.params.pageNo===1?actual.globalFirst:actual.globalSecond
  })
  const mounted=mount('DccFileSelector.vue',{modelValue:true,source:initial,purpose:'relations',directories:[],selected:[],loadPage,
    persist:async rows=>{confirmed.push(rows.map(row=>row.controlledFileId))},openPreview:async()=>{throw new Error('正文未授权不应打开')}})
  try{
    await settle();assert.equal(calls[0].projectFolderId,'500');find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    const tabs=find(mounted.root,'el-tabs');tabs.props['onUpdate:modelValue']('global');tabs.props.onTabChange('global');await settle()
    assert.equal(calls[1].selectorScope,'GLOBAL');assert.equal(calls[1].dccProjectCodeId,undefined);assert.equal(calls[1].projectFolderId,undefined)
    const pagination=find(mounted.root,'el-pagination');pagination.props['onUpdate:currentPage'](2);pagination.props.onCurrentChange(2);await settle()
    assert.equal(calls[2].pageNo,2);assert.equal(find(mounted.root,'el-pagination').props.total,3)
    assert.ok(textContent(mounted.root).includes('已选 1 项'));assert.ok(textContent(mounted.root).includes('SOURCE-21.pdf'))
    assert.ok(textContent(mounted.root).includes(initial.fileName));assert.ok(textContent(mounted.root).includes('未记录'))
    assert.equal(find(mounted.root,'el-button','查看').props.disabled,true)
    find(mounted.root,'el-button','确认关联').props.onClick();await settle();assert.deepEqual(confirmed,[['21']])
  }finally{mounted.unmount()}
})
if(process.argv[2])test('mounted formal Root loader failure stays visible and old directory response cannot overwrite the new context',async()=>{
  const actual=JSON.parse(readFileSync(process.argv[2],'utf8'));let finish,fail=false
  const props=Vue.reactive({modelValue:true,source:{...source,projectId:'5',folderId:'500'},purpose:'relations',directories:[],selected:[],
    loadPage:rootSelectorLoader(async request=>{
      if(request.params.projectFolderId==='500')return new Promise(resolve=>{finish=resolve})
      if(fail)throw new Error('正式Root查询拒绝')
      return {total:1,list:[actual.globalFirst.list[1]]}
    }),persist:async()=>{},openPreview:async()=>{}})
  const mounted=mount('DccFileSelector.vue',props)
  try{
    await settle();props.source={...source,contextKey:'tenant1:project6:folder501',projectId:'6',folderId:'501'};await settle()
    assert.ok(textContent(mounted.root).includes('SOURCE-22.pdf'));finish(actual.projectFolder);await settle()
    assert.ok(!textContent(mounted.root).includes('SOURCE-21.pdf'));assert.ok(textContent(mounted.root).includes('SOURCE-22.pdf'))
    fail=true;find(mounted.root,'el-button','搜索').props.onClick();await settle()
    assert.equal(find(mounted.root,'el-alert').props.title,'正式Root查询拒绝');assert.equal(find(mounted.root,'el-pagination').props.total,0)
  }finally{finish?.(actual.projectFolder);mounted.unmount()}
})

const unsubmittedSource=(contextKey='tenant1:upload-session:source.pdf')=>({contextKey,tenantId:'1',projectId:'5',folderId:'500',
  projectName:'编制项目',folderName:'逻辑文件夹',fileName:'真实选中的新上传.pdf',fileNumber:'NEW-1',versionNo:'A/1',unsubmitted:true})
const browserRow=(id='9007199254740993',master='9007199254740995',project='6',extras={})=>({tenantId:'1',id,masterId:master,latestControlledFileId:id,
  dccProjectCodeId:project,projectName:'跨项目B',projectFolderId:'600',projectFolderName:'项目逻辑文件夹',sourceOriginalFileName:'跨项目待生效.pdf',fileName:'模板标题',fileNumber:'RELATED-1',versionNo:'B/1',
  status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canPreview:false,...extras})
const folderTree=[{key:'tenant1:project5',name:'编制项目',projectId:'5',children:[{key:'tenant1:project5:folder500',name:'逻辑文件夹',projectId:'5',folderId:'500'}]},
  {key:'tenant1:project6',name:'跨项目B',projectId:'6',children:[{key:'tenant1:project6:folder600',name:'项目逻辑文件夹',projectId:'6',folderId:'600'}]}]
const cancelInSelector=root=>nodes(root,node=>node.type==='el-button'&&textContent(node)==='取消')[0]

test('unsubmitted file without persisted file or master IDs displays real source and loads B folder through Root',async()=>{
  const initial=unsubmittedSource(),calls=[],result=browserRow('21','10','5',{projectFolderId:'500',projectName:'编制项目',projectFolderName:'逻辑文件夹'})
  const loadPage=rootSelectorLoader(async request=>{calls.push(request.params);return {total:1,list:[result]}})
  const mounted=mount('DccFileSelector.vue',{modelValue:true,source:initial,purpose:'relations',directories:folderTree,selected:[],loadPage,persist:async()=>{},openPreview:async()=>{throw new Error('正文无权限')}})
  try{
    await settle();assert.equal('masterId' in initial,false);assert.equal('controlledFileId' in initial,false)
    assert.ok(textContent(mounted.root).includes(initial.fileName));assert.ok(textContent(mounted.root).includes('尚未提交'))
    assert.equal(calls[0].selectorScope,'PROJECT_FOLDER');assert.equal(calls[0].projectFolderId,'500');assert.equal(calls[0].directoryId,undefined)
    assert.equal(find(mounted.root,'el-tree').props['current-node-key'],JSON.stringify(['1','5','500']))
    assert.ok(textContent(mounted.root).includes('待生效'));assert.equal(find(mounted.root,'el-button','查看').props.disabled,true)
    assert.equal(find(mounted.root,'el-checkbox').props.disabled,false)
    const tree = find(mounted.root,'el-tree')
    const otherProject=folderTree[1];await nodes(mounted.root,node=>node.type==='el-table'&&node.props['data-testid']==='dcc-selector-project-list')[0].props.onRowClick({id:otherProject.projectId,projectName:otherProject.name});await settle();const newTree=find(mounted.root,'el-tree');newTree.props.onNodeClick(newTree.props.data[0]);await settle()
    assert.equal(calls.at(-1).dccProjectCodeId,'6');assert.equal(calls.at(-1).projectFolderId,'600')
    assert.equal(calls.at(-1).directoryId,undefined);assert.ok(textContent(mounted.root).includes(initial.fileName))
  }finally{mounted.unmount()}
})
test('new upload confirms cross-project future latest Long strings to parent form only, including across pages',async()=>{
  let puts=0,reads=0;const updates=[],calls=[]
  const first=browserRow(),second=browserRow('9007199254740997','9007199254740999','7',{projectName:'跨项目C',projectFolderId:'700',projectFolderName:'C目录',sourceOriginalFileName:'第二文件.pdf'})
  const props=Vue.reactive({mode:'upload',source:unsubmittedSource(),directories:folderTree,canEdit:true,modelValue:[],
    loadPage:rootSelectorLoader(async request=>{calls.push(request.params);if(request.params.selectorScope==='PROJECT_FOLDER')return {total:0,list:[]};return {total:2,list:[request.params.pageNo===1?first:second]}}),
    loadCurrent:async()=>{reads++;throw new Error('新上传不能读现存关联')},loadHistory:async()=>{reads++;throw new Error('无历史审批')},persistCurrent:async()=>{puts++;throw new Error('新上传不能关系PUT')},openPreview:async()=>{throw new Error('无正文权限')},
    'onUpdate:modelValue':rows=>{updates.push(rows);props.modelValue=rows}})
  const mounted=mount('DccFileRelations.vue',props)
  try{
    await settle();find(mounted.root,'el-button','关联').props.onClick();await settle()
    const tabs=find(mounted.root,'el-tabs');tabs.props['onUpdate:modelValue']('global');tabs.props.onTabChange('global');await settle()
    assert.equal(calls.at(-1).dccProjectCodeId,undefined);assert.equal(calls.at(-1).projectFolderId,undefined)
    assert.ok(textContent(mounted.root).includes('待生效'));assert.equal(find(mounted.root,'el-button','查看').props.disabled,true)
    find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    const pagination=find(mounted.root,'el-pagination');pagination.props['onUpdate:currentPage'](2);pagination.props.onCurrentChange(2);await settle()
    assert.ok(textContent(mounted.root).includes('已选 1 项'));find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    assert.equal(updates.length,0);assert.equal(puts,0);assert.equal(reads,0)
    find(mounted.root,'el-button','确认关联').props.onClick();await settle()
    assert.deepEqual(updates[0].map(row=>row.controlledFileId),['9007199254740993','9007199254740997'])
    assert.equal(JSON.stringify({relatedControlledFileIds:updates[0].map(row=>row.controlledFileId)}),'{"relatedControlledFileIds":["9007199254740993","9007199254740997"]}')
    assert.equal(puts,0);assert.equal(reads,0);assert.ok(textContent(mounted.root).includes(props.source.fileName))
    find(mounted.root,'el-button','关联').props.onClick();await settle();assert.ok(textContent(mounted.root).includes('已选 2 项'))
    find(mounted.root,'el-tag','跨项目待生效.pdf').props.onClose();await settle();assert.equal(props.modelValue.length,2)
    cancelInSelector(mounted.root).props.onClick();await settle();assert.equal(updates.length,1);assert.equal(props.modelValue.length,2)
    find(mounted.root,'el-button','关联').props.onClick();await settle();assert.ok(textContent(mounted.root).includes('已选 2 项'))
  }finally{mounted.unmount()}
})
test('cancel and dialog close preserve original upload collection while empty confirmation clears only the form',async()=>{
  const initial={...candidate,controlledFileId:'9007199254740993',masterId:'9007199254740995',projectId:'6'},updates=[]
  const props=Vue.reactive({mode:'upload',source:unsubmittedSource(),directories:folderTree,canEdit:true,modelValue:[initial],
    loadPage:rootSelectorLoader(async()=>({total:0,list:[]})),loadCurrent:async()=>{throw new Error('must not read')},loadHistory:async()=>{throw new Error('must not read')},persistCurrent:async()=>{throw new Error('must not PUT')},openPreview:async()=>{},
    'onUpdate:modelValue':rows=>{updates.push(rows);props.modelValue=rows}})
  const mounted=mount('DccFileRelations.vue',props)
  try{
    await settle();find(mounted.root,'el-button','关联').props.onClick();await settle();find(mounted.root,'el-tag','跨项目.pdf').props.onClose();await settle()
    cancelInSelector(mounted.root).props.onClick();await settle();assert.equal(updates.length,0);assert.equal(props.modelValue[0].controlledFileId,initial.controlledFileId)
    find(mounted.root,'el-button','关联').props.onClick();await settle();find(mounted.root,'el-tag','跨项目.pdf').props.onClose();await settle()
    const dialog=nodes(mounted.root,node=>node.type==='el-dialog'&&String(node.props.title).includes('关联文件'))[0];dialog.props.onClose();await settle()
    assert.equal(updates.length,0);assert.equal(props.modelValue.length,1)
    find(mounted.root,'el-button','关联').props.onClick();await settle();find(mounted.root,'el-tag','跨项目.pdf').props.onClose();await settle()
    find(mounted.root,'el-button','确认关联').props.onClick();await settle();assert.equal(updates.length,1);assert.equal(props.modelValue.length,0)
  }finally{mounted.unmount()}
})
test('unsubmitted selector reopens same source without allowing previous pending response to overwrite it',async()=>{
  const page=browserRow('21','10','5',{projectFolderId:'500',projectName:'编制项目',projectFolderName:'逻辑文件夹'});let finish,calls=0
  const props=Vue.reactive({modelValue:true,source:unsubmittedSource(),purpose:'relations',directories:folderTree,selected:[],
    loadPage:rootSelectorLoader(async()=>{calls++;if(calls===1)return new Promise(resolve=>{finish=resolve});return {total:1,list:[page]}}),persist:async()=>{},openPreview:async()=>{},
    'onUpdate:modelValue':open=>{props.modelValue=open}})
  const mounted=mount('DccFileSelector.vue',props)
  try{
    await settle();cancelInSelector(mounted.root).props.onClick();await settle();props.modelValue=true;await settle()
    assert.ok(textContent(mounted.root).includes(page.sourceOriginalFileName));finish({total:0,list:[]});await settle()
    assert.equal(find(mounted.root,'el-pagination').props.total,1);assert.ok(textContent(mounted.root).includes(props.source.fileName))
  }finally{finish?.({total:0,list:[]});mounted.unmount()}
})
test('upload session and tenant change invalidate old pages and retain no foreign candidates',async()=>{
  let finish;const first=browserRow('21','10','5',{projectFolderId:'500',projectName:'编制项目',projectFolderName:'逻辑文件夹'})
  const props=Vue.reactive({modelValue:true,source:unsubmittedSource(),purpose:'relations',directories:folderTree,selected:[],
    loadPage:rootSelectorLoader(async()=>new Promise(resolve=>{finish=resolve})),persist:async()=>{},openPreview:async()=>{}})
  const mounted=mount('DccFileSelector.vue',props)
  try{
    await settle();props.source={...unsubmittedSource('tenant2:upload-other'),tenantId:'2',fileName:'另一租户新文件.pdf'}
    props.loadPage=rootSelectorLoader(async()=>({total:1,list:[{...first,tenantId:'2',sourceOriginalFileName:'租户2候选.pdf'}]}),'2');await settle()
    finish({total:1,list:[first]});await settle();assert.ok(textContent(mounted.root).includes('另一租户新文件.pdf'))
    assert.ok(textContent(mounted.root).includes('租户2候选.pdf'));assert.ok(!textContent(mounted.root).includes(first.sourceOriginalFileName))
    props.loadPage=rootSelectorLoader(async()=>({total:1,list:[first]}),'2');await settle();find(mounted.root,'el-button','搜索').props.onClick();await settle()
    const alert=find(mounted.root,'el-alert');assert.ok(alert,textContent(mounted.root))
    assert.ok(alert.props.title.includes('权限事实'));assert.equal(find(mounted.root,'el-pagination').props.total,0)
  }finally{finish?.({total:0,list:[]});mounted.unmount()}
})
test('H07 Root test dependency is actual B attribute validator with real invalid-value rejection',()=>{
  assert.throws(()=>projectAttributeFixture.validateAttributes({targetMarkets:['NA','CE'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'}),/互斥/)
  const actual={targetMarkets:['CE'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'}
  assert.equal(projectAttributeFixture.validateAttributes(actual),actual)
})
test('direct unsubmitted selector calls persist then confirmed with copied string selections, never a file relation request',async()=>{
  const initial=unsubmittedSource(),events=[],first=browserRow('9007199254740993','9007199254740995','5',{projectFolderId:'500',projectName:'编制项目',projectFolderName:'逻辑文件夹'})
  const draft=Vue.reactive({relatedControlledFileIds:[]});let selected=[]
  const props=Vue.reactive({modelValue:true,source:initial,purpose:'relations',directories:folderTree,selected:[],
    loadPage:rootSelectorLoader(async()=>({total:1,list:[first]})),openPreview:async()=>{throw new Error('不可预览')},
    persist:async rows=>{events.push('persist');selected=rows.map(row=>({...row}));draft.relatedControlledFileIds=rows.map(row=>row.controlledFileId)},
    onConfirmed:rows=>{events.push('confirmed');assert.deepEqual(rows.map(row=>row.controlledFileId),['9007199254740993'])},
    'onUpdate:modelValue':open=>{props.modelValue=open}})
  const mounted=mount('DccFileSelector.vue',props)
  try{
    await settle();find(mounted.root,'el-checkbox').props.onChange(true);find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    assert.ok(textContent(mounted.root).includes('已选 1 项'));assert.equal(draft.relatedControlledFileIds.length,0)
    find(mounted.root,'el-button','确认关联').props.onClick();await settle()
    assert.deepEqual(events,['persist','confirmed']);assert.deepEqual([...draft.relatedControlledFileIds],['9007199254740993'])
    assert.equal(props.modelValue,false);assert.equal('masterId' in props.source,false);assert.equal(selected[0].pendingEffect,true)
    props.selected=selected;props.modelValue=true;await settle();find(mounted.root,'el-tag','跨项目待生效.pdf').props.onClose();await settle()
    cancelInSelector(mounted.root).props.onClick();await settle();assert.deepEqual(events,['persist','confirmed']);assert.equal(draft.relatedControlledFileIds[0],'9007199254740993')
  }finally{mounted.unmount()}
})

const referenceReviewModules=transport=>{
  const cache=new Map()
  const load=(key,path)=>{
    if(cache.has(key))return cache.get(key)
    const exports={};new Function('exports','require',ts.transpileModule(readFileSync(new URL(path,import.meta.url),'utf8'),
      {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,name=>{
        if(name==='@/config/axios')return {default:transport}
        if(name==='./applicationRead')return load('root','../src/api/dcc/controlledFile/applicationRead.ts')
        if(name==='@/views/dcc/controlled-file/project-attributes/state')return projectAttributeFixture
        if(name.endsWith('/project-folder-tree'))return load('tree','../src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts')
        throw new Error('Unexpected reference review dependency '+name)
      });cache.set(key,exports);return exports
  }
  return {b:load('b','../src/api/dcc/controlledFile/projectAttributes.ts'),tree:load('tree','../src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts'),
    root:load('root','../src/api/dcc/controlledFile/applicationRead.ts'),d:load('d','../src/api/dcc/controlledFile/relations.ts'),
    mapping:load('mapping','../src/views/dcc/controlled-file/relations/project-reference-contract.ts')}
}
if(process.argv[3])test('reference review formal B wrapper Root loader D batch and exact cancel map real HTTP evidence into Vue',async()=>{
  const actual=JSON.parse(readFileSync(process.argv[3],'utf8')),calls=[],changes=[];let list=actual.listed,cancelFailure=true
  const api=referenceReviewModules({
    get:async request=>{
      calls.push(request)
      if(request.url.endsWith('/folders'))return actual.folders
      if(request.url==='/dcc/project-file-references')return list
      if(request.url==='/dcc/controlled-files/browser-page')return {total:1,list:[{
        id:actual.created[0].selectedVersion.controlledFileId,latestControlledFileId:actual.created[0].selectedVersion.controlledFileId,
        ...actual.created[0].selectedVersion,dccProjectCodeId:actual.created[0].selectedVersion.projectId,sourceOriginalFileName:actual.created[0].selectedVersion.fileName,
        projectName:actual.sourceProjectName,projectFolderId:null,projectFolderName:null,canPreview:false
      }]}
      throw new Error('unexpected reference read')
    },
    post:async request=>{
      calls.push(request)
      if(request.url==='/dcc/project-file-references/batch'){list=actual.created;return actual.created}
      if(request.url==='/dcc/project-file-references/cancel'){
        if(cancelFailure)throw new Error('正式负责人权限已变')
        list=[];return actual.lastCancel
      }
      throw new Error('unexpected reference write')
    }
  })
  const folders=await api.b.getProjectFolders(actual.projectId),tree=api.tree.buildProjectFolderTree(actual.projectId,folders)
  const nodes=api.mapping.mapReferenceDirectoryNodes('1',actual.projectId,tree),folder=String(folders[0].id)
  const context={tenantId:'1',projectId:actual.projectId,folderId:folder}
  const props={contextKey:JSON.stringify(['1',actual.projectId,folder]),projectId:actual.projectId,folderId:folder,folderName:folders[0].name,
    isProjectLeader:api.mapping.isEnabledTargetProjectLeader(actual.leaderAccount,actual),
    selectorSource:{...unsubmittedSource(),contextKey:'reference-selected-target',projectId:actual.projectId,folderId:folder},directories:nodes,
    loadPage:query=>api.root.loadDccSelectorPage('1',query),openPreview:async()=>{throw new Error('无正文权限')},
    loadReferences:async(project,folder)=>api.mapping.mapReferenceRows(await api.d.listProjectReferences(project,folder),{...context,projectId:project,folderId:folder},{[actual.sourceProjectId]:actual.sourceProjectName}),
    createReferences:async(project,folder,ids,reason)=>api.mapping.mapReferenceRows(await api.d.createProjectReferences(project,folder,ids,reason),{...context,projectId:project,folderId:folder},{[actual.sourceProjectId]:actual.sourceProjectName}),
    cancelReference:api.d.cancelProjectReference,onChanged:(...args)=>changes.push(args)}
  const mounted=mount('DccProjectReferences.vue',props)
  try{
    await settle();assert.ok(textContent(mounted.root).includes(actual.sourceProjectName));assert.ok(textContent(mounted.root).includes('引用项目：1'))
    find(mounted.root,'el-button','引用').props.onClick();await settle()
    // Directory transport must first receive the target B folder; search is deliberately global for a cross-project source.
    assert.equal(calls.find(call=>call.url==='/dcc/controlled-files/browser-page').params.projectFolderId,folder)
    const tabs=find(mounted.root,'el-tabs');tabs.props['onUpdate:modelValue']('global');tabs.props.onTabChange('global');await settle()
    assert.equal(calls.filter(call=>call.url==='/dcc/controlled-files/browser-page').at(-1).params.projectFolderId,undefined)
    find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    nodesInReferenceInput(mounted.root,'引用原因（必填）').props['onUpdate:modelValue']('引用本轮选定版本');await settle()
    find(mounted.root,'el-button','确认引用').props.onClick();await settle()
    const batch=calls.find(call=>call.url==='/dcc/project-file-references/batch')
    assert.equal(batch.data.projectId,actual.projectId);assert.equal(batch.data.folderId,folder)
    assert.deepEqual(batch.data.selectedFileIds,[actual.created[0].selectedVersion.controlledFileId]);assert.deepEqual(changes[0],[1,actual.created[0].reference.masterId])
    find(mounted.root,'el-button','取消引用').props.onClick();await settle()
    assert.equal(calls.filter(call=>call.url==='/dcc/project-file-references/cancel').length,0)
    find(mounted.root,'el-button','返回').props.onClick();await settle()
    assert.ok(textContent(mounted.root).includes('Pinned source.pdf'));assert.equal(calls.filter(call=>call.url==='/dcc/project-file-references/cancel').length,0)
    find(mounted.root,'el-button','取消引用').props.onClick();await settle()
    nodesInReferenceInput(mounted.root,'取消原因（必填）').props['onUpdate:modelValue']('二次确认取消');await settle()
    find(mounted.root,'el-button','确认取消引用').props.onClick();await settle()
    assert.ok(textContent(mounted.root).includes('Pinned source.pdf'));assert.ok(find(mounted.root,'el-alert').props.title.includes('正式负责人权限已变'))
    const cancelled=calls.filter(call=>call.url==='/dcc/project-file-references/cancel').at(-1).data
    assert.equal(cancelled.referenceId,actual.created[0].reference.id);assert.equal(cancelled.projectId,actual.projectId);assert.equal(cancelled.folderId,folder)
    assert.equal(cancelled.masterId,actual.created[0].reference.masterId);assert.equal(cancelled.confirmed,true)
    cancelFailure=false;find(mounted.root,'el-button','确认取消引用').props.onClick();await settle()
    assert.deepEqual(changes.at(-1),[0,actual.created[0].reference.masterId])
  }finally{mounted.unmount()}
})
const nodesInReferenceInput=(root,placeholder)=>nodes(root,node=>node.type==='el-input'&&node.props.placeholder===placeholder)[0]
if(process.argv[3])test('reference review VIEW member reads actual B folders and list but never gets reference write or body controls',async()=>{
  const actual=JSON.parse(readFileSync(process.argv[3],'utf8')),writes=[]
  const api=referenceReviewModules({get:async request=>request.url.endsWith('/folders')?actual.folders:actual.listed,
    post:async request=>{writes.push(request);throw new Error('VIEW写入不可调用')}})
  const folders=await api.b.getProjectFolders(actual.projectId),folder=String(folders[0].id),context={tenantId:'1',projectId:actual.projectId,folderId:folder}
  const mapped=api.mapping.mapReferenceDirectoryNodes('1',actual.projectId,api.tree.buildProjectFolderTree(actual.projectId,folders))
  const mounted=mount('DccProjectReferences.vue',{contextKey:'view:'+actual.projectId+':'+folder,projectId:actual.projectId,folderId:folder,folderName:folders[0].name,
    isProjectLeader:api.mapping.isEnabledTargetProjectLeader({id:'8',status:0},actual),selectorSource:{...unsubmittedSource(),projectId:actual.projectId,folderId:folder},directories:mapped,
    loadPage:query=>api.root.loadDccSelectorPage('1',query),openPreview:async()=>{throw new Error('VIEW未获得正文')},
    loadReferences:async(project,folder)=>api.mapping.mapReferenceRows(await api.d.listProjectReferences(project,folder),context,{[actual.sourceProjectId]:actual.sourceProjectName}),
    createReferences:api.d.createProjectReferences,cancelReference:api.d.cancelProjectReference})
  try{
    await settle();assert.ok(textContent(mounted.root).includes(actual.sourceProjectName));assert.ok(textContent(mounted.root).includes('引用项目：1'))
    assert.equal(nodes(mounted.root,node=>node.type==='el-button'&&textContent(node)==='引用').length,0)
    assert.equal(nodes(mounted.root,node=>node.type==='el-button'&&textContent(node)==='取消引用').length,0)
    assert.equal(find(mounted.root,'el-button','查看'),undefined);assert.equal(writes.length,0)
    assert.ok(nodes(mounted.root,node=>node.type==='span'&&node.props.class==='referenced').length>0)
  }finally{mounted.unmount()}
})
if(process.argv[3])test('reference review cancelled confirmation and late cancel response cannot remove another project context',async()=>{
  const actual=JSON.parse(readFileSync(process.argv[3],'utf8'));let finish
  const oldFolder=String(actual.folders[0].id),nextProject='9223372036854775700',nextFolder='9223372036854775701',changes=[]
  const next={...actual.listed[0],reference:{...actual.listed[0].reference,id:'9223372036854775702',projectId:nextProject,folderId:nextFolder}}
  const api=referenceReviewModules({
    get:async request=>request.url==='/dcc/project-file-references'?(request.params.projectId===nextProject?[next]:actual.listed):actual.folders,
    post:async request=>{assert.equal(request.url,'/dcc/project-file-references/cancel');return new Promise(resolve=>{finish=resolve})}
  })
  const props=Vue.reactive({contextKey:'old-project-folder',projectId:actual.projectId,folderId:oldFolder,folderName:actual.folders[0].name,isProjectLeader:true,
    selectorSource:{...unsubmittedSource(),projectId:actual.projectId,folderId:oldFolder},directories:[],loadPage:query=>api.root.loadDccSelectorPage('1',query),openPreview:async()=>{},
    loadReferences:async(project,folder)=>api.mapping.mapReferenceRows(await api.d.listProjectReferences(project,folder),{tenantId:'1',projectId:project,folderId:folder},{[actual.sourceProjectId]:actual.sourceProjectName}),
    createReferences:api.d.createProjectReferences,cancelReference:api.d.cancelProjectReference,onChanged:(...args)=>changes.push(args)})
  const mounted=mount('DccProjectReferences.vue',props)
  try{
    await settle();find(mounted.root,'el-button','取消引用').props.onClick();await settle()
    nodesInReferenceInput(mounted.root,'取消原因（必填）').props['onUpdate:modelValue']('旧目录二次确认');await settle()
    find(mounted.root,'el-button','确认取消引用').props.onClick();await settle();assert.ok(finish)
    props.contextKey='new-project-folder';props.projectId=nextProject;props.folderId=nextFolder;props.folderName='新项目目录'
    props.selectorSource={...unsubmittedSource('new-ref-selector'),projectId:nextProject,folderId:nextFolder};await settle()
    finish(0);await settle();assert.equal(changes.length,0);assert.equal(find(mounted.root,'el-button','引用').props.disabled,false)
    const confirmation=nodes(mounted.root,node=>node.type==='el-dialog'&&node.props.title==='二次确认：取消引用')[0]
    assert.equal(confirmation.props.modelValue,false);assert.ok(textContent(mounted.root).includes('Pinned source.pdf'))
    // Confirming the new row freezes its new exact reference and current target context.
    find(mounted.root,'el-button','取消引用').props.onClick();await settle();assert.ok(textContent(mounted.root).includes('新项目目录'))
    find(mounted.root,'el-button','返回').props.onClick();await settle();assert.equal(changes.length,0)
  }finally{finish?.(0);mounted.unmount()}
})
if(process.argv[3])test('reference review global pagination keeps a fixed selected version and old create completion cannot publish into new folder',async()=>{
  const actual=JSON.parse(readFileSync(process.argv[3],'utf8')),folder=String(actual.folders[0].id),changes=[],calls=[];let finish
  const selected=actual.created[0].selectedVersion
  const browser={...selected,id:selected.controlledFileId,latestControlledFileId:selected.controlledFileId,dccProjectCodeId:selected.projectId,
    sourceOriginalFileName:selected.fileName,projectName:actual.sourceProjectName,projectFolderId:null,projectFolderName:null,canPreview:false}
  const api=referenceReviewModules({get:async request=>{
    calls.push(request)
    if(request.url==='/dcc/project-file-references')return []
    if(request.url==='/dcc/controlled-files/browser-page')return {total:2,list:request.params.pageNo===1?[browser]:[{...browser,id:'9223372036854775709',latestControlledFileId:'9223372036854775709',masterId:'9223372036854775710'}]}
    return actual.folders
  },post:async request=>{calls.push(request);return new Promise(resolve=>{finish=resolve})}})
  const props=Vue.reactive({contextKey:'pagination-old',projectId:actual.projectId,folderId:folder,folderName:'原文件夹',isProjectLeader:true,
    selectorSource:{...unsubmittedSource('ref-page-context'),projectId:actual.projectId,folderId:folder},directories:[],
    loadPage:query=>api.root.loadDccSelectorPage('1',query),openPreview:async()=>{},
    loadReferences:async(project,folder)=>api.mapping.mapReferenceRows(await api.d.listProjectReferences(project,folder),{tenantId:'1',projectId:project,folderId:folder},{[actual.sourceProjectId]:actual.sourceProjectName}),
    createReferences:async(project,folder,ids,reason)=>api.mapping.mapReferenceRows(await api.d.createProjectReferences(project,folder,ids,reason),{tenantId:'1',projectId:project,folderId:folder},{[actual.sourceProjectId]:actual.sourceProjectName}),
    cancelReference:api.d.cancelProjectReference,onChanged:(...args)=>changes.push(args)})
  const mounted=mount('DccProjectReferences.vue',props)
  try{
    await settle();find(mounted.root,'el-button','引用').props.onClick();await settle()
    const tabs=find(mounted.root,'el-tabs');tabs.props['onUpdate:modelValue']('global');tabs.props.onTabChange('global');await settle()
    find(mounted.root,'el-checkbox').props.onChange(true);await settle()
    const pagination=find(mounted.root,'el-pagination');pagination.props['onUpdate:currentPage'](2);pagination.props.onCurrentChange(2);await settle()
    assert.ok(textContent(mounted.root).includes('已选 1 项'));assert.equal(find(mounted.root,'el-pagination').props.total,2)
    nodesInReferenceInput(mounted.root,'引用原因（必填）').props['onUpdate:modelValue']('只引用原选定版本');await settle()
    find(mounted.root,'el-button','确认引用').props.onClick();await settle()
    const write=calls.find(call=>call.url==='/dcc/project-file-references/batch')
    assert.deepEqual(write.data.selectedFileIds,[selected.controlledFileId]);assert.equal(write.data.folderId,folder)
    props.contextKey='new-folder-context';props.folderId=String(actual.folders[1].id);props.folderName='新目录'
    props.selectorSource={...unsubmittedSource('new-ref-context'),projectId:props.projectId,folderId:props.folderId};await settle()
    finish(actual.created);await settle();assert.equal(changes.length,0);assert.equal(find(mounted.root,'el-button','引用').props.disabled,false)
    find(mounted.root,'el-button','引用').props.onClick();await settle();assert.ok(textContent(mounted.root).includes('已选 0 项'))
  }finally{finish?.(actual.created);mounted.unmount()}
})
