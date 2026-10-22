import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'
import test from 'node:test'
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const modulePath = resolve(root, 'src/views/dcc/controlled-file/browser/project-browser.ts')
const projectId='9223372036854775700', folderId='9223372036854775701', fileId='9007199254740995', masterId='9007199254740993', actorId='9223372036854775707'
const project={id:projectId,projectName:'正式项目',projectCode:'P1',status:'ENABLE',projectLeaderUserId:actorId,projectLeader:'负责人',associatedFileCount:0}
const candidate={id:fileId,masterId,tenantId:'1',dccProjectCodeId:projectId,latestControlledFileId:fileId,projectName:'正式项目',projectFolderId:folderId,projectFolderName:'空目录',fileName:'原文.pdf',sourceOriginalFileName:'原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canPreview:false}
const reference={reference:{id:'19',projectId,folderId,masterId,selectedControlledFileId:fileId,createdBy:actorId},selectedVersion:{tenantId:'1',controlledFileId:fileId,masterId,projectId,fileName:'原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false},referenceProjectCount:2}
function fixture(overrides={}) {
  const calls=[], cache=new Map()
  const request={get:async args=>{calls.push(['get',args]);if(overrides.get)return overrides.get(args)
    if(args.url==='/dcc/project-codes/page')return {list:[project],total:48}
    if(args.url===`/dcc/project-codes/${projectId}`)return project
    if(args.url.endsWith('/folders'))return [{id:folderId,projectCodeId:projectId,parentId:'0',name:'空目录',sortOrder:0,active:true}]
    if(args.url==='/dcc/controlled-files/browser-page')return overrides.page || {list:[args.params.status?{...candidate,status:args.params.status,
      controlled:['ACTIVE','CONTROLLED_PENDING_EFFECTIVE'].includes(args.params.status),pendingEffect:args.params.status==='CONTROLLED_PENDING_EFFECTIVE',executable:false}:candidate],total:83}
    if(args.url==='/system/user/simple-list')return [{id:actorId,nickname:'负责人'}]
    if(args.url==='/dcc/project-file-references/usage')return {masterId,referenceProjectCount:2,referenced:true}
    if(args.url==='/dcc/project-file-references')return [reference]
    if(args.url===`/dcc/controlled-files/${fileId}/relation-permissions`)return {controlledFileId:fileId,tenantId:'1',masterId,projectId,
      projectName:'正式项目',projectFolderId:folderId,projectFolderName:'空目录',fileName:'原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canEdit:false,canPreview:false}
    throw new Error(`Unexpected GET ${args.url}`)
  },post:async args=>{calls.push(['post',args]);return args.url.endsWith('/cancel')?0:[reference]}}
  const load=filename=>{filename=resolve(filename);if(!existsSync(filename) && filename.endsWith('.ts'))filename=filename.slice(0,-3)+'/index.ts';if(cache.has(filename))return cache.get(filename)
    const exports={};cache.set(filename,exports)
    const compiled=ts.transpileModule(readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
    new Function('exports','require',compiled)(exports,name=>{
      if(name==='@/config/axios')return {default:request}
      if(name==='@/api/dcc/controlledFile/workflow'){
        const source=readFileSync(resolve(root,'src/api/dcc/controlledFile/workflow.ts'),'utf8');const from=source.indexOf('export const getControlledFile =');const next=source.indexOf('\nexport const ',from+1)
        const exports={};new Function('exports','request',ts.transpileModule(source.slice(from,next),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports,request);return exports
      }
      return load(resolve(name.startsWith('@/')?resolve(root,'src'):dirname(filename),(name.startsWith('@/')?name.slice(2):name)+'.ts'))
    })
    return exports
  }
  assert.ok(existsSync(modulePath),'public project browser coordinator must exist')
  return {calls,module:load(modulePath)}
}

test('actual project/folder/selector wrappers keep Long IDs, empty folders and server totals',async()=>{
  const {module,calls}=fixture(), state=new module.ProjectBrowserState('1',actorId)
  await state.loadProjects();assert.equal(state.projectsTotal,48);assert.equal(state.projects[0].id,projectId)
  await state.selectProject(projectId);assert.equal(state.directories[0].folderId,folderId)
  await state.selectFolder(state.directories[0]);assert.equal(state.total,83);assert.equal(state.rows[0].controlledFileId,fileId)
  const query=calls.filter(([verb,args])=>args.url==='/dcc/controlled-files/browser-page').at(-1)[1].params
  assert.equal(query.projectFolderId,folderId);assert.equal(query.dccProjectCodeId,projectId);assert.equal(query.directoryId,undefined)
  await state.search('global','原文');const global=calls.at(-2)?.[1]
  const globalQuery=calls.filter(([,args])=>args.url==='/dcc/controlled-files/browser-page').at(-1)[1].params
  assert.equal(globalQuery.browserScope,'GLOBAL');assert.equal(globalQuery.selectorScope,undefined);assert.equal(globalQuery.projectFolderId,undefined);assert.equal(globalQuery.dccProjectCodeId,undefined)
  assert.equal(state.usage[masterId].referenceProjectCount,2);assert.equal(state.rows[0].canPreview,false)
})
test('full project browser keeps every authorized version while reference selection remains latest-controlled',async()=>{
  const earlier={...candidate,id:'9007199254740996',versionNo:'A/1-1',status:'WORKING',controlled:false,pendingEffect:false,executable:false}
  const pending={...candidate,id:'9007199254740997',versionNo:'A/2',status:'PENDING_MATRIX_REVIEW',controlled:false,pendingEffect:false,executable:false}
  const {module,calls}=fixture({page:{list:[earlier,pending,candidate],total:123}}), state=new module.ProjectBrowserState('1',actorId)
  state.versionView='ALL'
  await state.search('global','原文')
  assert.equal(state.error,'');assert.equal(state.rows.length,3);assert.equal(state.total,123)
  assert.deepEqual(state.rows.map(row=>row.controlledFileId),[earlier.id,pending.id,candidate.id])
  assert.equal(state.rows[0].controlled,false);assert.equal(state.rows[2].pendingEffect,true)
  const browserQuery=calls.find(([,args])=>args.url==='/dcc/controlled-files/browser-page')[1].params
  assert.equal(browserQuery.browserScope,'GLOBAL');assert.equal(browserQuery.latestVersionOnly,false)
  const selectorFixture=fixture(),selector=new selectorFixture.module.ProjectBrowserState('1',actorId)
  await selector.loadPage({keyword:'',pageNo:1,pageSize:20})
  assert.equal(selectorFixture.calls.at(-1)[1].params.selectorScope,'GLOBAL')
  assert.equal(selectorFixture.calls.at(-1)[1].params.browserScope,undefined)
})
test('full project browser rejects foreign tenant and unsafe version identities before usage loading',async()=>{
  for(const bad of [{...candidate,tenantId:'2'},{...candidate,id:9007199254740992}]){
    const {module,calls}=fixture({page:{list:[bad],total:1}}),state=new module.ProjectBrowserState('1',actorId)
    await state.search('global','')
    assert.notEqual(state.error,'');assert.equal(state.rows.length,0)
    assert.equal(calls.filter(([,a])=>a.url==='/dcc/project-file-references/usage').length,0)
  }
})
test('official enabled actor and formal target leader gate reference writes without role bypass',async()=>{
  const {module}=fixture(),state=new module.ProjectBrowserState('1',actorId)
  await state.loadActor();await state.selectProject(projectId);await state.selectFolder(state.directories[0]);assert.equal(state.canReference,true)
  const other=new module.ProjectBrowserState('1','1');await other.loadActor();await other.selectProject(projectId);assert.equal(other.canReference,false)
  await assert.rejects(()=>other.createReferences(projectId,folderId,[fileId],'引用'),/负责人/)
})
test('reference callbacks map pinned selected version, exact cancel identity and real project count',async()=>{
  const {module,calls}=fixture(),state=new module.ProjectBrowserState('1',actorId)
  await state.loadActor();await state.selectProject(projectId);await state.selectFolder(state.directories[0])
  const rows=await state.loadReferences(projectId,folderId);assert.equal(rows[0].selectedVersion.controlledFileId,fileId);assert.equal(rows[0].sourceProjectName,'正式项目')
  await state.createReferences(projectId,folderId,[fileId],'引用');assert.deepEqual(calls.find(([verb,args])=>verb==='post'&&args.url.endsWith('/batch'))[1].data.selectedFileIds,[fileId])
  assert.equal(await state.cancelReference(projectId,folderId,masterId,'19','取消'),0)
  assert.deepEqual(calls.at(-1)[1].data,{projectId,folderId,masterId,referenceId:'19',confirmed:true,reason:'取消'})
})
test('errors are visible and a late global response cannot populate a changed folder context',async()=>{
  let release
  const {module}=fixture({get:args=>{if(args.url==='/dcc/controlled-files/browser-page')return new Promise(resolve=>{release=resolve});throw new Error('目录读取拒绝')}})
  const state=new module.ProjectBrowserState('1',actorId);await state.loadProjects();assert.match(state.projectError,/目录读取拒绝/)
  const pending=state.search('global','旧');state.invalidateFiles();release({list:[candidate],total:83});await pending;assert.equal(state.rows.length,0)
})
test('public browser mounts project panel and older WORKING entry keeps owner and checkout gates',()=>{
  const source=readFileSync(resolve(root,'src/views/dcc/controlled-file/browser/index.vue'),'utf8')
  assert.ok(source.includes('<ProjectBrowserPanel'),'project browser is mounted')
  const match=source.match(/const canSubmit(?:Latest)?WorkingIteration = \([\s\S]*?(?=const browserMutationIdempotencyKeys)/)
  assert.ok(match);assert.doesNotMatch(match[0],/isLatestWorkingIteration/);assert.match(match[0],/canEditVersion\(file\)/);assert.match(match[0],/!file.checkedOut/)
})

test('actual older-WORKING handler opens the exact selected Long version and retains checkout/owner guards',async()=>{
  const source=readFileSync(resolve(root,'src/views/dcc/controlled-file/browser/index.vue'),'utf8')
  const block=(start,end)=>source.slice(source.indexOf(start),source.indexOf(end,source.indexOf(start)))
  const code=block('const isValidBrowserOptionId =','const categoryOptions =')+block('const canEditVersion =','const canCheckoutVersion =')+block('const canSubmitWorkingIteration =','const browserMutationIdempotencyKeys =')+block('const handleSubmitWorkingIteration =','const getCheckoutDisplayName =')+'\nreturn handleSubmitWorkingIteration'
  const opened=[],handler=new Function('userStore','openManagement',ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)({getUser:{id:actorId}},id=>opened.push(id))
  const earlier={id:fileId,status:'WORKING',versionNo:'A/1-1',requesterId:actorId,checkedOut:false}
  const newer={...earlier,id:'9007199254740999',versionNo:'A/1-2'}
  await handler({versionHistory:[earlier,newer]},earlier);assert.deepEqual(opened,[fileId])
  await handler({}, {...earlier,checkedOut:true});await handler({}, {...earlier,requesterId:'1'})
  await handler({}, {...earlier,actionProjection:{actionLocked:true}});assert.equal(opened.length,1)
})

test('public browser defaults latest controlled and uses exact explicit status filters without touching selectorScope',async()=>{
  const {module,calls}=fixture(),state=new module.ProjectBrowserState('1',actorId)
  await state.loadFiles()
  let query=calls.find(([,args])=>args.url==='/dcc/controlled-files/browser-page')[1].params
  assert.equal(query.latestVersionOnly,true)
  assert.equal(query.status,undefined)
  for(const [view,status] of [['ALL',undefined],['WORKING','WORKING'],['PENDING_MATRIX_APPROVAL','PENDING_MATRIX_APPROVAL'],['OBSOLETE','OBSOLETE']]){
    await state.changeVersionView(view)
    query=calls.filter(([,args])=>args.url==='/dcc/controlled-files/browser-page').at(-1)[1].params
    assert.equal(query.browserScope,'GLOBAL');assert.equal(query.latestVersionOnly,false);assert.equal(query.status,status)
    assert.equal(state.total,83)
  }
  await state.loadPage({keyword:'',pageNo:1,pageSize:20})
  assert.equal(calls.at(-1)[1].params.selectorScope,'GLOBAL')
  assert.equal(calls.at(-1)[1].params.browserScope,undefined)
})

test('a late old version-view query cannot replace the newly selected working view',async()=>{
  let release
  const {module}=fixture({get:args=>{
    if(args.url==='/dcc/controlled-files/browser-page')return args.params.latestVersionOnly
      ? new Promise(resolve=>{release=resolve}) : Promise.resolve({list:[{...candidate,id:'9007199254740996',versionNo:'A/1-1',status:'WORKING',controlled:false,pendingEffect:false}],total:52})
    if(args.url==='/dcc/project-file-references/usage')return {masterId,referenceProjectCount:2,referenced:true}
    throw new Error('Unexpected fixture request')
  }}),state=new module.ProjectBrowserState('1',actorId)
  const old=state.loadFiles()
  await state.changeVersionView('WORKING')
  assert.equal(state.rows[0].versionNo,'A/1-1');assert.equal(state.total,52)
  release({list:[candidate],total:83});await old
  assert.equal(state.rows[0].versionNo,'A/1-1');assert.equal(state.total,52)
})

test('saved references compose pinned permission metadata without reading the source project detail',async()=>{
  const sourceProject='8',selectedId='9223372036854775709'
  const saved={...reference,reference:{...reference.reference,selectedControlledFileId:selectedId},selectedVersion:{...reference.selectedVersion,controlledFileId:selectedId,projectId:sourceProject}}
  const {module,calls}=fixture({get:args=>{
    if(args.url==='/dcc/project-file-references')return [saved]
    if(args.url===`/dcc/controlled-files/${selectedId}/relation-permissions`)return {controlledFileId:selectedId,tenantId:'1',masterId,projectId:sourceProject,
      projectName:'名称授权源项目',projectFolderId:'13',projectFolderName:'源逻辑文件夹',fileName:'原文.pdf',fileNumber:'F1',versionNo:'A/1',status:'CONTROLLED_PENDING_EFFECTIVE',controlled:true,pendingEffect:true,executable:false,canEdit:false,canPreview:false}
    throw new Error('强项目详情读取被拒绝：'+args.url)
  }}),state=new module.ProjectBrowserState('1',actorId)
  const rows=await state.loadReferences(projectId,folderId)
  assert.equal(rows[0].selectedVersion.controlledFileId,selectedId);assert.equal(rows[0].sourceProjectName,'名称授权源项目')
  assert.equal(rows[0].selectedVersion.folderName,'源逻辑文件夹');assert.equal(rows[0].selectedVersion.canPreview,false)
  assert.equal(calls.some(([,args])=>args.url.includes('/project-codes/')),false)
})

test('saved reference mutation reports a subsequent permission projection failure as already saved',async()=>{
  const {module}=fixture({get:args=>{
    if(args.url==='/system/user/simple-list')return [{id:actorId}]
    if(args.url===`/dcc/project-codes/${projectId}`)return project
    if(args.url.endsWith('/folders'))return [{id:folderId,projectCodeId:projectId,parentId:'0',name:'空目录',sortOrder:0,active:true}]
    if(args.url==='/dcc/controlled-files/browser-page')return {list:[candidate],total:1}
    if(args.url==='/dcc/project-file-references/usage')return {masterId,referenceProjectCount:2,referenced:true}
    if(args.url.endsWith('/relation-permissions'))throw new Error('权限投影网络故障')
    throw new Error(args.url)
  }}),state=new module.ProjectBrowserState('1',actorId)
  await state.loadActor();await state.selectProject(projectId);await state.selectFolder(state.directories[0])
  await assert.rejects(()=>state.createReferences(projectId,folderId,[fileId],'正式引用'),error=>error.referenceSaved===true&&/已保存.*权限投影网络故障/.test(error.message))
})
