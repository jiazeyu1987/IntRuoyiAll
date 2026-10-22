const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const {scope,config,response}=require('./dcc-upload-a32-fixture.cjs')
const plain=value=>JSON.parse(JSON.stringify(value))
const matrix=require('./dcc-upload-test-loader.cjs').loadUploadMatrix(),deps=require('./dcc-upload-test-loader.cjs').loadUploadDepartments()
const c={exports:{},Error,require:name=>name.endsWith('approval-matrix')?matrix:name.endsWith('signoff-departments')?deps:{validateAttributes:value=>value}}
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
const h=c.exports
const prepare=(s=scope,value=config,ids=['7'],ready=true)=>{const state=new matrix.UploadApprovalMatrixState(s);state.setApplicationCustomMatrix(value);state.accept(state.begin(ids),response(s,value,ids,ready));return state}
const draft=()=>({categoryId:'2',dccProjectCodeId:'5',fileTypeTaxonomyId:'4',projectFolderId:'500',projectFolderChangeReason:'',processType:'CONTROLLED_FILE',changeType:'NEW',
 fileName:'自定义',fileNumber:'N-1',versionNo:'A/1',effectiveMode:'DELAYED',effectiveDate:null,needTraining:true,productCode:'',relatedControlledFileIds:['8'],
 projectAttributes:{targetMarkets:['CE'],licenseHolder:'N',actualManufacturer:'Y',documentTransfer:'N'},selectedSignoffDepartmentIds:['7'],selectedDistributionDepartmentIds:['7'],
 applicationCustomMatrix:plain(config),approvalMatrixSelection:prepare().selection('SUBMIT')})
const uploaded={sessionId:'source',uploadTicket:'source-ticket',fileName:'源文件.pdf'}
test('A32 canonical context phase and chosen departments must match actual PREVIEW request',()=>{
 for(const change of [r=>r.applicationCustomMatrixPhase='RESERVED',r=>r.applicationActionType='REVISION',r=>{const v=JSON.parse(r.applicationCustomMatrixCanonical);v.context.projectId='6';r.applicationCustomMatrixCanonical=JSON.stringify(v)},
 r=>{const v=JSON.parse(r.applicationCustomMatrixCanonical);v.selectedSignoffDepartmentIds=['9'];r.applicationCustomMatrixCanonical=JSON.stringify(v)},r=>r.applicationCustomMatrixCanonical='',r=>{r.nodes[1].approveMethod='ANY';r.nodes[1].requireAllApprovals=false}]){
  const state=new matrix.UploadApprovalMatrixState(scope);state.setApplicationCustomMatrix(config);const req=state.begin(['7']),r=response();change(r);assert.throws(()=>state.accept(req,r))
 }
})
test('A32 current real Vue upload script and template compile together',()=>{
 const {parse,compileScript}=require('vue/compiler-sfc'),source=fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue','utf8')
 const parsed=parse(source);assert.deepEqual(parsed.errors,[])
 assert.ok(compileScript(parsed.descriptor,{id:'a32-actual-upload',inlineTemplate:true}).content.includes('CustomApplicationMatrixEditor'))
})
test('A32 reserved restore keeps saved file round and context, never creates public defaults',()=>{
 const s={...scope,controlledFileId:'9223372036854775701',attributeRound:3,applicationContextKey:'saved-context'}
 const state=prepare(s),selected=state.selection('SAVE');assert.equal(selected.applicationCustomMatrixPhase,'RESERVED');assert.equal(selected.applicationContextKey,'saved-context');assert.equal(selected.expectedApprovalRouteId,undefined)
 const r=response(s);r.applicationCustomMatrixPhase='PREVIEW';assert.throws(()=>state.accept(state.begin(['7']),r))
})
test('A32 saved CUSTOM exact prepared definition cannot silently upgrade during reserved preview',()=>{
 const s={...scope,controlledFileId:'20',attributeRound:2},state=new matrix.UploadApprovalMatrixState(s,null,
  {controlledFileId:'20',projectId:'5',applicationType:'UPLOAD',executionContract:'PUBLIC_MATRIX_NATIVE_V1',preparedProcessDefinitionId:'saved-v1',preparedDefinitionDigest:'f'.repeat(64)})
 state.setApplicationCustomMatrix(config);assert.throws(()=>state.accept(state.begin(['7']),response(s)),/定义|合同|替换/)
})
test('A32 payload rejects modified actors or departments after server preflight and keeps genuine partial SAVE',()=>{
 for(const alter of [d=>d.applicationCustomMatrix.approverUserId='78',d=>d.applicationCustomMatrix.documentControlUserId=null,d=>d.selectedSignoffDepartmentIds=['9']]){
  const d=draft();alter(d);assert.throws(()=>h.buildSubmitPayload(d,uploaded))
 }
 const d=draft();d.applicationCustomMatrix={approverUserId:null,documentControlUserId:'66'};d.selectedSignoffDepartmentIds=[];
 d.approvalMatrixSelection=prepare(scope,d.applicationCustomMatrix,[],false).selection('SAVE');const result=h.buildWorkingDraftPayload(d,uploaded)
 assert.deepEqual(plain(result.applicationCustomMatrix),d.applicationCustomMatrix);assert.deepEqual(plain(result.selectedSignoffDepartmentIds),[]);assert.equal(result.expectedApprovalRouteId,undefined)
})
test('A32 confirmation lists exact custom approver document control and matrix with all business facts',()=>{
 const d=draft(),person=(id,name)=>({id,name,deptId:'7',deptName:'质量'}),facts={projectName:'甲项目',folderName:'正式文件夹',fileTypeName:'其他',relatedFiles:[{fileName:'关联.pdf',versionNo:'B/1'}],
  signoffDepartments:[{id:'7',name:'质量'}],distributionDepartments:[{id:'7',name:'质量'}],compiler:person('71','编制甲'),supervisor:person('72','主管乙'),
  approvers:[{id:config.approverUserId,name:'批准丙'}],documentControl:person('66','文控丁'),approvalRule:'本次批准人通过',attachments:[{fileName:'附件1.pdf'},{fileName:'附件2.docx'}]}
 const text=h.buildUploadConfirmationSummary(d,uploaded,facts).map(r=>r.label+'：'+r.value).join('\n')
 for(const value of ['本次自定义','文控丁','66','批准丙',config.approverUserId,'编制甲','主管乙','CE','关联.pdf','附件1.pdf','附件2.docx','需要培训','全部人员'])assert.ok(text.includes(value),value)
 assert.doesNotMatch(text,/server-supervisor-proof|applicationCustomMatrixToken|server-canonical/)
 assert.throws(()=>h.buildUploadConfirmationSummary(d,uploaded,{...facts,documentControl:undefined}),/文控/)
})
