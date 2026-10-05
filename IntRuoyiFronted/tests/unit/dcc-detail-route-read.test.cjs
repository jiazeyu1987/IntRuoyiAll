const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const source = parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('detail.ts', source, ts.ScriptTarget.Latest, true)
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => {resolve=a;reject=b}); return {promise,resolve,reject} }
function host() {
  const reads=[],messages=[], c={exports:{}, Error,String,BigInt,Number,Array,Object,Promise,JSON,
    route:{path:'/dcc/controlled-file/detail/10',fullPath:'/dcc/controlled-file/detail/10',params:{id:'10'}},
    controlledFileId:vue.ref('10'),detailLoadSequence:0,dccSignatureEvidenceRequestSequence:0,
    workingBrowserLoadedContext:vue.ref(),applicationApprovalRead:vue.ref(),fileDetail:vue.ref({id:'old'}),
    approvalProgressScope:vue.ref(),approvalProgressError:vue.ref(''),
    fileAccessExplanation:vue.ref(null),accessExplanationError:vue.ref(''),paperDistributionRecords:vue.ref(['old']),
    approvalTodoTask:vue.ref(),approvalTaskList:vue.ref([]),stageProgressList:vue.ref([]),approvalLoading:vue.ref(false),
    dccSignatureEvidenceList:vue.ref([]),dccSignatureEvidenceTotal:vue.ref(0),dccSignatureEvidenceLoading:vue.ref(false),dccSignatureEvidenceError:vue.ref(''),
    controlledPrintRecords:vue.ref([]),controlledPrintRecordsLoading:vue.ref(false),controlledPrintRecordsError:vue.ref(''),
    checkPermi:()=>true,viewerMode:vue.ref(false),showLifecycleTraceSections:vue.ref(true),
    categories:vue.ref([]),directories:vue.ref([]),activeApprovalPrintTemplate:vue.ref(null),categoryNameMap:vue.ref(),directoryNameMap:vue.ref(),directoryPathMap:vue.ref(),userNameMap:vue.ref(),departmentList:vue.ref([]),deptNameMap:vue.ref(),
    getControlledFile:async id=>{reads.push(['file',id]);return{id}},getFileCategoryList:async()=>[],getDirectoryTree:async()=>[],getSimpleUserList:async()=>[],getSimpleDeptList:async()=>[],
    getPaperDistributionRecords:async id=>{reads.push(['paper',id]);return[]},getActiveApprovalPrintTemplate:async()=>null,
    getControlledFileAccessExplanation:async id=>{reads.push(['access',id]);return{reason:'真实权限说明'}},
    loadActiveObsoleteAction:async()=>{},loadActivePublishAction:async()=>{},loadControlledPrintRecords:async()=>{},
    loadDccSignatureEvidenceList:async()=>reads.push(['signatures']),loadApprovalDetail:async()=>reads.push(['tasks']),openControlledPrintDialogFromRoute:async()=>{},
    flattenTree:()=>[],buildDetailUserDisplayName:()=>'',resolveReadSideErrorMessage:e=>e.message,message:{error:value=>messages.push(value)}
  }
  const names=['isActiveControlledFileDetailRoute','isCurrentDetailLoad','loadData','loadAccessExplanationOnly','reloadAll']
  const declarations=names.flatMap(name=>{
    const st=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text===name))
    if(!st){assert.equal(name,'isActiveControlledFileDetailRoute');return[]}
    return[st.getText(ast)]
  })
  vm.runInNewContext(ts.transpileModule(declarations.join('\n')+'\nexports.reload=reloadAll;exports.load=loadData;exports.access=loadAccessExplanationOnly',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
  const setRoute=(path,id,query='')=>{c.route.path=path;c.route.fullPath=path+query;c.route.params={id};c.controlledFileId.value=id==null?'':String(id)}
  return{c,reads,messages,setRoute,...c.exports}
}
test('actual detail reload on browser navigation or invalid Long ID issues no detail/paper/access requests',async()=>{
  for(const [path,id] of [['/dcc/controlled-file/browser',''],['/dcc/controlled-file/detail/0','0'],['/dcc/controlled-file/detail/01','01'],['/dcc/controlled-file/detail/9223372036854775808','9223372036854775808']]){
    const h=host();h.setRoute(path,id);await h.reload()
    assert.deepEqual(h.reads,[]);assert.equal(h.c.fileDetail.value,undefined);assert.equal(h.c.paperDistributionRecords.value.length,0);assert.deepEqual(h.messages,[])
  }
})
test('actual direct loaders reject a non-detail scope before any API and valid same-ID refresh remains complete',async()=>{
  const h=host();h.setRoute('/dcc/controlled-file/browser','')
  await h.load(h.c.detailLoadSequence,'',h.c.route.fullPath);await h.access()
  assert.deepEqual(h.reads,[])
  h.setRoute('/dcc/controlled-file/detail/10','10');await h.reload();await h.reload()
  assert.deepEqual(h.reads.filter(r=>r[0]==='file'),[['file','10'],['file','10']]);assert.equal(h.c.fileDetail.value.id,'10')
  h.setRoute('/dcc/controlled-file/detail/11','11','?from=approval-center');await h.reload()
  assert.equal(h.c.fileDetail.value.id,'11');assert.equal(h.reads.filter(r=>r[0]==='tasks').length,3)
})
test('late old failure after leaving detail cannot read blank access explanation or notify the new page',async()=>{
  const h=host(),p=deferred();h.c.getControlledFile=id=>{h.reads.push(['file',id]);return id==='10'?p.promise:Promise.resolve({id})}
  const loading=h.reload();h.setRoute('/dcc/controlled-file/browser','');await h.reload();p.reject(new Error('旧详情真实失败'));await loading
  assert.equal(h.reads.filter(r=>r[0]==='access').length,0);assert.deepEqual(h.messages,[]);assert.equal(h.c.fileDetail.value,undefined)
})
test('late old success cannot replace another valid selected file and current real failure remains visible',async()=>{
  const h=host(),p=deferred();h.c.getControlledFile=id=>{h.reads.push(['file',id]);return id==='10'?p.promise:Promise.resolve({id})}
  const loading=h.reload();h.setRoute('/dcc/controlled-file/detail/11','11');await h.reload();p.resolve({id:'10'});await loading
  assert.equal(h.c.fileDetail.value.id,'11');assert.equal(h.reads.filter(r=>r[0]==='tasks').length,1)
  h.c.getControlledFile=async()=>{throw new Error('当前文件读取拒绝')};await h.reload()
  assert.equal(h.reads.at(-1)[0],'access');assert.equal(h.reads.at(-1)[1],'11');assert.equal(h.messages[0],'当前文件读取拒绝')
})
