const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),ts=require('typescript')
const root=path.resolve(__dirname,'../..'),source=fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/upload/index.vue'),'utf8')
const block=(start,end)=>source.slice(source.indexOf(start),source.indexOf(end,source.indexOf(start)))
const ref=value=>({value}),computed=fn=>({get value(){return fn()}})
const treeExports={};new Function('exports',ts.transpileModule(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(treeExports)
const oldFolder={id:'500',projectCodeId:'5',parentId:'0',name:'质量',sortOrder:0,active:true}
test('project folder has one refresh button and invokes the handler without a DOM event as project identity',()=>{
 const field=source.match(/<el-form-item[^>]*label="项目文件夹"[\s\S]*?<\/el-form-item>/)[0]
 assert.equal((field.match(/刷新目录/g)||[]).length,1)
 assert.match(field,/@click="refreshUploadProjectFolders\(\)"/)
})
function setup(overrides={}){
  const state={formData:{dccProjectCodeId:'5',projectFolderId:'500',relatedControlledFileIds:['1']},projectFolders:ref([oldFolder]),projectFoldersProjectId:ref('5'),projectFoldersLoading:ref(false),projectFoldersError:ref(''),projectFolderRequestSequence:0,
    approvalMatrixState:ref(),approvalMatrixScopeKey:'',routeReadinessRequestSeq:0,routeReadiness:ref(),routeReadinessSelectionKey:ref(''),signoffSelectionCategoryKey:'',
    projectAttributesSelectionSequence:0,acceptedProjectCodeId:ref('5'),projectAttributesLoading:ref(false),projectAttributesPanelKey:ref(0),projectAttributesPanel:ref({selectProject:async()=>true}),
    message:{confirm:async()=>{},error:()=>{},warning:()=>{}},defineExpose(){},hasUploadApplicationInput:()=>false,hasTemporaryUploadState:()=>false,cleanupCurrentUploadSession:async()=>true,projectAttributesRestoreSnapshot:ref(),selectedUploadRelations:ref([]),uploadRelationsVisible:ref(false),uploadRelationSource:ref(),resetProjectFileTemplateSelection(){},applyDccProjectCodeProductNumber(){},loadProjectFileTemplate:async()=>{},getProjectFolders:async()=>[],computed,buildProjectFolderTree:treeExports.buildProjectFolderTree}
  Object.assign(state,overrides)
  const code=block('const handleProjectCodeChange =','const buildUploadRelationSource =')+'\nreturn {handleProjectCodeChange,projectFolderTree,selectedProjectFolder,loadUploadProjectFolders,refreshUploadProjectFolders}'
  const compiled=ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
  const handlers=new Function(...Object.keys(state),compiled)(...Object.values(state))
  return {state,handlers}
}
test('pending project confirmation never interprets old folders as the new project and cancellation restores them',async()=>{
  const {state,handlers}=setup();state.projectAttributesPanel.value.selectProject=async()=>false;state.formData.dccProjectCodeId='6'
  assert.doesNotThrow(()=>handlers.projectFolderTree.value);assert.equal(handlers.projectFolderTree.value.length,0);assert.equal(handlers.selectedProjectFolder.value,undefined)
  await handlers.handleProjectCodeChange();assert.equal(state.formData.dccProjectCodeId,'5');assert.equal(handlers.projectFolderTree.value[0].id,'500');assert.equal(handlers.selectedProjectFolder.value.id,'500')
})

test('same-project folder refresh invalidates projection while pending and preserves valid selection and application choices',async()=>{
 let release;const {state,handlers}=setup({getProjectFolders:()=>new Promise(resolve=>{release=resolve}),submitLoading:ref(false)})
 state.formData.projectAttributes={targetMarkets:['CE']};state.formData.selectedSignoffDepartmentIds=['8'];state.formData.selectedDistributionDepartmentIds=['9']
 const pending=handlers.refreshUploadProjectFolders('5');assert.equal(handlers.selectedProjectFolder.value,undefined);assert.equal(state.projectFoldersLoading.value,true)
 release([oldFolder]);await pending
 assert.equal(state.formData.projectFolderId,'500');assert.equal(handlers.selectedProjectFolder.value.id,'500')
 assert.deepEqual(state.formData.projectAttributes,{targetMarkets:['CE']});assert.deepEqual(state.formData.selectedSignoffDepartmentIds,['8']);assert.deepEqual(state.formData.selectedDistributionDepartmentIds,['9'])
})
test('deleted or disabled selected folder is cleared and reported without guessing a replacement',async()=>{
 for(const rows of [[],[{...oldFolder,active:false}]]){
  const warnings=[];const {state,handlers}=setup({getProjectFolders:async()=>rows,message:{warning:value=>warnings.push(value)}})
  await handlers.loadUploadProjectFolders('5');assert.equal(state.formData.projectFolderId,null);assert.equal(warnings.length,1)
 }
})
test('an older folder response never replaces the latest refresh, and another project event does nothing',async()=>{
 const pending=[];const {state,handlers}=setup({getProjectFolders:()=>new Promise(resolve=>pending.push(resolve)),submitLoading:ref(false)})
 await handlers.refreshUploadProjectFolders('6');assert.equal(pending.length,0)
 const first=handlers.refreshUploadProjectFolders('5');const second=handlers.refreshUploadProjectFolders('5')
 pending[1]([oldFolder]);await second;pending[0]([]);await first
 assert.equal(state.formData.projectFolderId,'500');assert.equal(handlers.selectedProjectFolder.value.id,'500')
})
test('invalid complete folder response becomes an explicit error and cannot supply a selected folder',async()=>{
 const {state,handlers}=setup({getProjectFolders:async()=>[{...oldFolder,projectCodeId:'6'}]})
 await handlers.loadUploadProjectFolders('5');assert.ok(state.projectFoldersError.value);assert.equal(handlers.selectedProjectFolder.value,undefined)
})
test('clearing the project invalidates an in-progress folder request and releases loading',async()=>{
  const {state,handlers}=setup();state.projectFoldersLoading.value=true;state.formData.dccProjectCodeId=null
  await handlers.handleProjectCodeChange();assert.equal(state.projectFoldersLoading.value,false);assert.equal(state.projectFoldersProjectId.value,undefined);assert.equal(state.projectFolders.value.length,0)
})
test('a matching folder ID owned by another project cannot become a selected folder',()=>{
  const {state,handlers}=setup();state.projectFolders.value=[{...oldFolder,projectCodeId:'6'}]
  assert.equal(handlers.selectedProjectFolder.value,undefined)
})
