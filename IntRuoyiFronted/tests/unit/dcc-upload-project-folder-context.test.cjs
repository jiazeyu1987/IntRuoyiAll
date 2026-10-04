const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),ts=require('typescript')
const root=path.resolve(__dirname,'../..'),source=fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/upload/index.vue'),'utf8')
const block=(start,end)=>source.slice(source.indexOf(start),source.indexOf(end,source.indexOf(start)))
const ref=value=>({value}),computed=fn=>({get value(){return fn()}})
const treeExports={};new Function('exports',ts.transpileModule(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(treeExports)
const oldFolder={id:'500',projectCodeId:'5',parentId:'0',name:'质量',sortOrder:0,active:true}
function setup(){
  const state={formData:{dccProjectCodeId:'5',projectFolderId:'500',relatedControlledFileIds:['1']},projectFolders:ref([oldFolder]),projectFoldersProjectId:ref('5'),projectFoldersLoading:ref(false),projectFoldersError:ref(''),projectFolderRequestSequence:0,
    projectAttributesSelectionSequence:0,acceptedProjectCodeId:ref('5'),projectAttributesLoading:ref(false),projectAttributesPanelKey:ref(0),projectAttributesPanel:ref({selectProject:async()=>true}),
    message:{confirm:async()=>{},error:()=>{}},selectedUploadRelations:ref([]),uploadRelationsVisible:ref(false),uploadRelationSource:ref(),resetProjectFileTemplateSelection(){},applyDccProjectCodeProductNumber(){},loadProjectFileTemplate:async()=>{},getProjectFolders:async()=>[],computed,buildProjectFolderTree:treeExports.buildProjectFolderTree}
  const code=block('const handleProjectCodeChange =','const buildUploadRelationSource =')+'\nreturn {handleProjectCodeChange,projectFolderTree,selectedProjectFolder,loadUploadProjectFolders}'
  const compiled=ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
  const handlers=new Function(...Object.keys(state),compiled)(...Object.values(state))
  return {state,handlers}
}
test('pending project confirmation never interprets old folders as the new project and cancellation restores them',async()=>{
  const {state,handlers}=setup();state.projectAttributesPanel.value.selectProject=async()=>false;state.formData.dccProjectCodeId='6'
  assert.doesNotThrow(()=>handlers.projectFolderTree.value);assert.equal(handlers.projectFolderTree.value.length,0);assert.equal(handlers.selectedProjectFolder.value,undefined)
  await handlers.handleProjectCodeChange();assert.equal(state.formData.dccProjectCodeId,'5');assert.equal(handlers.projectFolderTree.value[0].id,'500');assert.equal(handlers.selectedProjectFolder.value.id,'500')
})
test('clearing the project invalidates an in-progress folder request and releases loading',async()=>{
  const {state,handlers}=setup();state.projectFoldersLoading.value=true;state.formData.dccProjectCodeId=null
  await handlers.handleProjectCodeChange();assert.equal(state.projectFoldersLoading.value,false);assert.equal(state.projectFoldersProjectId.value,undefined);assert.equal(state.projectFolders.value.length,0)
})
test('a matching folder ID owned by another project cannot become a selected folder',()=>{
  const {state,handlers}=setup();state.projectFolders.value=[{...oldFolder,projectCodeId:'6'}]
  assert.equal(handlers.selectedProjectFolder.value,undefined)
})
