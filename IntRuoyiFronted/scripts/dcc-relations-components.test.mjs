import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import test from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'
const require=createRequire(import.meta.url)
const {parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const files=['DccFileSelector.vue','DccReferenceBadge.vue','DccProjectReferences.vue','DccRelationArrangements.vue','DccFileRelations.vue','DccRelationArrangementPanel.vue']
for(const name of files) test(`Vue compiles independent ${name}`,()=>{
  const filename=fileURLToPath(new URL(`../src/views/dcc/controlled-file/relations/${name}`,import.meta.url))
  const source=readFileSync(filename,'utf8');const {descriptor,errors}=parse(source,{filename})
  assert.equal(errors.length,0)
  const script=compileScript(descriptor,{id:name})
  const template=compileTemplate({id:name,filename,source:descriptor.template.content,compilerOptions:{bindingMetadata:script.bindings}})
  assert.deepEqual(template.errors,[])
})
test('reference visual distinguishes reference rows and restores source color at zero',()=>{
  const source=readFileSync(new URL('../src/views/dcc/controlled-file/relations/DccReferenceBadge.vue',import.meta.url),'utf8')
  assert.ok(source.includes('isReference || projectCount > 0'));assert.ok(source.includes('引用 · 来源项目'))
  assert.ok(source.includes('引用项目：'));assert.ok(source.includes('源文件已作废'))
})
test('reference cancellation has separate confirmation and preserves rows until success',()=>{
  const source=readFileSync(new URL('../src/views/dcc/controlled-file/relations/DccProjectReferences.vue',import.meta.url),'utf8')
  const handler=source.slice(source.indexOf('const cancelConfirmed ='),source.indexOf('watch(() => props.contextKey'))
  assert.ok(source.includes('二次确认：取消引用'));assert.ok(handler.indexOf('await props.cancelReference')<handler.indexOf('rows.value = rows.value.filter'))
  assert.ok(handler.includes('token !== generation'));assert.ok(handler.includes('error.value = String(e)'))
})
const referenceSetup=()=>{
  const source=readFileSync(new URL('../src/views/dcc/controlled-file/relations/DccProjectReferences.vue',import.meta.url),'utf8')
  const script=source.slice(source.indexOf('const props ='),source.indexOf('</script>'))
  const c={exports:{},Error,ref:value=>({value}),watch:()=>{},onBeforeUnmount:()=>{},props:{contextKey:'one',projectId:'2',folderId:'21',isProjectLeader:true},events:[]}
  c.defineProps=()=>c.props;c.defineEmits=()=>((...args)=>c.events.push(args));vm.createContext(c)
  vm.runInContext(ts.transpileModule(`${script}\nglobalThis.component={rows,error,busy,confirmVisible,reason,pending,prepareCancel,cancelConfirmed,load,createSelected,referenceReason,selectorVisible,savedReadFailure}`,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
  c.row={reference:{id:'1',masterId:'10'},selectedVersion:{fileName:'源.pdf'},referenceProjectCount:2,sourceProjectName:'源项目'}
  c.component.rows.value=[c.row];return c
}
test('reference handler failure preserves entry, successful cancellation returns server count',async()=>{
  const c=referenceSetup();c.component.prepareCancel(c.row);c.component.reason.value='取消测试'
  c.props.cancelReference=async()=>{throw new Error('负责人校验失败')};await c.component.cancelConfirmed()
  assert.equal(c.component.rows.value.length,1);assert.equal(c.component.confirmVisible.value,true);assert.match(c.component.error.value,/负责人校验失败/)
  c.props.cancelReference=async()=>0;await c.component.cancelConfirmed()
  assert.equal(c.component.rows.value.length,0);assert.equal(c.events[0][0],'changed');assert.equal(c.events[0][1],0)
})
test('reference handler ignores late cancellation response after directory reload',async()=>{
  const c=referenceSetup();let finish;c.component.prepareCancel(c.row);c.component.reason.value='取消测试'
  c.props.cancelReference=()=>new Promise(resolve=>{finish=resolve});const pending=c.component.cancelConfirmed()
  c.props.projectId='3';c.props.folderId='31';c.props.loadReferences=async()=>[{...c.row,reference:{id:'2',masterId:'20'}}]
  await c.component.load();finish(0);await pending
  assert.equal(c.component.rows.value[0].reference.masterId,'20');assert.equal(c.events.length,0)
})
test('reference handler sends the exact immutable reference identity captured in its confirmation',async()=>{
  const c=referenceSetup();let request;c.component.prepareCancel(c.row);c.component.reason.value='取消确认'
  c.props.cancelReference=async(...args)=>{request=args;return 0};await c.component.cancelConfirmed()
  assert.deepEqual(request,['2','21','10','1','取消确认'])
})
const selectorSetup=async()=>{
  const stateSource=readFileSync(new URL('../src/views/dcc/controlled-file/relations/selector-state.ts',import.meta.url),'utf8')
  const source=readFileSync(new URL('../src/views/dcc/controlled-file/relations/DccFileSelector.vue',import.meta.url),'utf8')
  const script=source.slice(source.indexOf('const props ='),source.indexOf('</script>'))
  const props={modelValue:true,purpose:'relations',source:{contextKey:'t1:f1',tenantId:'1',masterId:'10',projectId:'1',folderId:'11',fileName:'源.pdf'},selected:[],directories:[]}
  const c={exports:{},Error,props,events:[],ref:value=>({value}),reactive:value=>value,computed:fn=>({get value(){return fn()}}),watch:()=>{},onBeforeUnmount:()=>{}}
  const loadModule=(name,resolve=()=>{throw new Error('Unexpected module dependency')})=>{
    const exports={};new Function('exports','require',ts.transpileModule(readFileSync(new URL(`../src/views/dcc/controlled-file/${name}.ts`,import.meta.url),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,resolve);return exports
  }
  c.SelectorProjectDirectoryState=loadModule('relations/selector-project-directory',name=>{
    if(name==='@/api/dcc/controlledFile/projectDiscovery')return {getProjectDiscoveryPage:async()=>({list:[],total:0})}
    if(name==='@/api/dcc/controlledFile/projectAttributes')return {getProjectFolders:async project=>[{id:project==='2'?'22':'11',projectCodeId:project,parentId:'0',name:'正式目录',sortOrder:0,active:true}]}
    if(name==='../basic-data/components/project-folder-tree')return loadModule('basic-data/components/project-folder-tree')
    if(name==='./project-reference-contract')return loadModule('relations/project-reference-contract')
    throw new Error(name)
  }).SelectorProjectDirectoryState
  c.defineProps=()=>props;c.defineEmits=()=>((...args)=>c.events.push(args));vm.createContext(c)
  vm.runInContext(ts.transpileModule(stateSource,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
  c.FileSelectorState=c.exports.FileSelectorState
  vm.runInContext(ts.transpileModule(`${script}\nglobalThis.component={state,navigation,scope,keyword,pageNo,projectId,folderId,load,search,directoryClick,choose,confirm,title,confirmLabel,preview,close}`,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
  await c.component.navigation.open({...props.source,projectName:'项目A',folderName:'正式目录'})
  await c.component.navigation.selectProject({id:'2',projectName:'项目B'})
  c.row={tenantId:'1',masterId:'20',controlledFileId:'200',projectId:'2',projectName:'项目B',folderName:'质量',fileName:'B.pdf',versionNo:'A/1',controlled:true,canPreview:false}
  return c
}
test('selector actual handlers retain selected file and source while switching to paged global search',async()=>{
  const c=await selectorSetup();const query=[];c.props.loadPage=async(params)=>{query.push({...params});return {total:50,list:[c.row]}}
  await c.component.directoryClick(c.component.navigation.directories[0]);c.component.choose(c.row,true)
  c.component.scope.value='global';c.component.keyword.value='项目B';c.component.pageNo.value=2;await c.component.load()
  assert.equal(query[1].keyword,'项目B');assert.equal(query[1].projectId,undefined);assert.equal(query[1].pageNo,2)
  assert.equal(c.component.state.selected[0].masterId,'20');assert.equal(c.component.state.context.masterId,'10')
})
test('selector actual confirm waits for persistence and does not emit success on failed save',async()=>{
  const c=await selectorSetup();c.component.choose(c.row,true)
  c.props.persist=async()=>{throw new Error('正式保存失败')};await c.component.confirm()
  assert.equal(c.events.length,0);assert.equal(c.component.state.error,'正式保存失败');assert.equal(c.component.state.selected.length,1)
  let finish;c.props.persist=()=>new Promise(resolve=>{finish=resolve});const pending=c.component.confirm()
  assert.equal(c.events.length,0);finish();await pending
  assert.equal(c.events[0][0],'confirmed');assert.equal(c.events[0][1][0].controlledFileId,'200')
})
test('project reference chooser confirms all selected versions through one atomic create call',async()=>{
  const c=referenceSetup();c.component.referenceReason.value='确认多引用';let calls=0,request
  c.props.createReferences=async(...args)=>{calls++;request=args;return [{reference:{id:'20',masterId:'20'},selectedVersion:{controlledFileId:'200'},referenceProjectCount:1,sourceProjectName:'项目B'}]}
  const selected=[{controlledFileId:'200',masterId:'20'},{controlledFileId:'300',masterId:'30'}]
  await c.component.createSelected(selected);assert.equal(calls,1);assert.deepEqual(request,['2','21',['200','300'],'确认多引用'])
  assert.deepEqual(c.events[0],['changed',1,'20'])
})
test('project reference chooser rejects creation after leader permission is revoked and propagates failures',async()=>{
  const c=referenceSetup();c.component.referenceReason.value='确认引用';let calls=0
  c.props.createReferences=async()=>{calls++;throw new Error('批量保存失败')}
  c.props.isProjectLeader=false;await assert.rejects(()=>c.component.createSelected([{controlledFileId:'200',masterId:'20'}]),/负责人/);assert.equal(calls,0)
  c.props.isProjectLeader=true;await assert.rejects(()=>c.component.createSelected([{controlledFileId:'200',masterId:'20'}]),/批量保存失败/);assert.equal(calls,1)
})
test('project reference completion after context change is an invalidated save instead of a successful return',async()=>{
  const c=referenceSetup();c.component.referenceReason.value='确认引用';let finish
  c.props.createReferences=()=>new Promise(resolve=>{finish=resolve});const pending=c.component.createSelected([{controlledFileId:'200',masterId:'20'}])
  c.props.contextKey='new-context';finish([])
  await assert.rejects(()=>pending,/上下文/);assert.equal(c.events.length,0)
})

test('saved reference creation read failure closes the completed chooser and requires actual refresh before another batch',async()=>{
  const c=referenceSetup();c.component.referenceReason.value='正式保存引用';c.component.selectorVisible.value=true
  c.props.createReferences=async()=>{throw Object.assign(new Error('引用已保存，但权限读取失败'),{referenceSaved:true})}
  await c.component.createSelected([{controlledFileId:'200'}])
  assert.equal(c.component.selectorVisible.value,false);assert.equal(c.component.savedReadFailure.value,true)
  assert.equal(c.component.busy.value,false);assert.match(c.component.error.value,/已保存/)
  await assert.rejects(()=>c.component.createSelected([{controlledFileId:'200'}]),/刷新/)
  c.props.loadReferences=async()=>[c.row];await c.component.load();assert.equal(c.component.savedReadFailure.value,false)
})
test('selector preview failure after closing and reopening the same file cannot replace new dialog state',async()=>{
  const c=await selectorSetup();let fail;c.props.openPreview=()=>new Promise((_resolve,reject)=>{fail=reject})
  const row={...c.row,canPreview:true};const pending=c.component.preview(row)
  c.component.close();c.component.state.reset(c.props.source,[]);c.component.state.error='新窗口状态'
  fail(new Error('旧预览失败'));await pending;assert.equal(c.component.state.error,'新窗口状态')
})
