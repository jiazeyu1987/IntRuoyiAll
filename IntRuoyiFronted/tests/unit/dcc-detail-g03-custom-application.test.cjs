const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue'),{parse}=require('vue/compiler-sfc')
const root=path.resolve(__dirname,'../..'),prefix='src/views/dcc/controlled-file/',source=fs.readFileSync(path.join(root,prefix+'detail/DetailApplicationPanel.vue'),'utf8'),ast=ts.createSourceFile('panel.ts',parse(source).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=name=>ast.statements.find(node=>ts.isVariableStatement(node)&&node.declarationList.declarations.some(d=>d.name.getText(ast)===name)).getText(ast)
function load(file,deps={}){const c={exports:{},Error,require:name=>{if(deps[name])return deps[name];if(name==='../project-attributes/state')return {validateAttributes:x=>x};throw Error(name)}};vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root,prefix+file),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const matrix=load('upload/approval-matrix.ts'),people=load('upload/signoff-departments.ts'),effects=load('upload/submitter.ts',{'./approval-matrix':matrix,'./signoff-departments':people}),revision=load('revision/revision-model.ts')
const attrs={targetMarkets:['CE'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'}
const custom={approverUserId:'77',documentControlUserId:'66'}
function host({cancel=false,changedMode=false,publicVersion,revisionFlow=false}={}){
 const calls={previews:[],saves:[],submits:[],confirmations:[]},ref=vue.ref
 let saved={controlledFileId:'20',projectId:'5',applicationType:revisionFlow?'REVISION':'UPLOAD',defaultSource:attrs,actual:attrs,effectiveMode:'DELAYED',effectiveDate:null,selectedSignoffDepartmentIds:['51'],selectedDistributionDepartmentIds:['52'],needTraining:false,changeDescription:'真实本次说明',canSubmit:!changedMode,unavailableReason:changedMode?'APPLICATION_APPROVAL_SOURCE_CHANGED：原自定义申请来源已变化':null,draftApprovalMatrix:null,approvalSourceMode:'APPLICATION_CUSTOM',applicationCustomMatrix:custom,applicationContextKey:'saved-context',attributeRound:1,applicationCustomMatrixDigest:'d'.repeat(64),executionContract:'PUBLIC_MATRIX_NATIVE_V2',preparedProcessDefinitionId:'exact-v2',preparedDefinitionDigest:'a'.repeat(64)}
 if(publicVersion){saved={...saved,approvalSourceMode:'PUBLIC_MATRIX',applicationCustomMatrix:null,applicationCustomMatrixDigest:null,draftApprovalMatrix:require('./dcc-upload-test-loader.cjs').matrixFixture({taxonomyId:'6',approvalIds:['77']}),executionContract:publicVersion,preparedProcessDefinitionId:'saved-exact-definition',effectiveMode:'AFTER_STANDARDIZATION_SIGNATURE'}}
 const c={exports:{},Error,JSON,Promise,structuredClone,...vue,...matrix,...people,...effects,...revision,validateAttributes:x=>x,
 props:{file:{id:'20',masterId:'10',dccProjectCodeId:'5',categoryId:'2',fileTypeTaxonomyId:'6',status:'WORKING',versionNo:'A/1',fileNumber:'N-1',fileName:'Source',requesterId:'99',ordinaryAttachmentManifestHash:'f'.repeat(64),attachments:[{attachmentId:'71',controlledFileId:'20',fileName:'One.pdf',attachmentPurpose:'ATTACHMENT',sortNo:0},{attachmentId:'72',controlledFileId:'20',fileName:'Training.pdf',attachmentPurpose:'TRAINING_RECORD',sortNo:1}]}},
 generation:0,selectionGeneration:0,working:ref(true),initial:ref(!revisionFlow),opened:ref(false),loading:ref(false),busy:ref(false),error:ref(''),snapshot:ref(),selectedApplicationFile:ref(),revisionOptions:ref(),approvalMatrixState:ref(),applicationCustomMatrix:ref(null),selectedDepartments:ref(null),selectedDistributionDepartments:ref(null),departments:ref([]),approvalAccounts:ref([]),approverNames:ref(''),approvalRule:ref(''),approvalGroups:ref([]),approvalCapabilityLimitations:ref([]),preparationBlockers:ref([]),effectiveMode:ref(null),effectiveDate:ref(null),needTraining:ref(false),description:ref(''),canSubmit:ref(false),idempotencyKey:ref(''),previewSource:ref(),selectedIterationId:ref(''),selectedIntent:ref(),snapshotIntent:ref(),submitted:ref(false),newActionPreparationEligible:ref(false),preparedNewActionBaseline:ref(false),newActionPreparationFacts:ref(),selectionLoading:ref(false),
 getVisitTenantId:()=>undefined,getTenantId:()=>1,generateUUID:()=> 'new-scope-should-not-replace-saved',getDccVersionDisplayText:x=>x,
 getControlledFileRevisionOptions:async()=>({masterId:'10',controlledBaselineId:'10',baselineVersionNo:'A/1',partialTarget:{versionNo:'A/2'},replacementTarget:{versionNo:'B/1'},iterations:[{id:'20',versionNo:'A/2',canPartial:true,canReplacement:true}]}),getControlledFileReplacementAttributes:async()=>structuredClone(saved),getWorkingApplicationAttributes:async()=>structuredClone(saved),getProjectDefaults:()=>{throw Error('Custom saved draft must not reload project defaults')},getControlledFile:async()=>c.props.file,
 getSimpleDeptList:async()=>[{id:'51',name:'质量',status:0},{id:'52',name:'研发',status:0}],getSimpleUserList:async()=>[{id:'77',nickname:'批准甲',status:0},{id:'66',nickname:'文控乙',status:0}],
 checkControlledFileRouteReadiness:async request=>{calls.previews.push(request);return {ready:true,nodes:[{stageCode:'MATRIX_REVIEW',candidateSourceType:'DEPT',candidateSourceIds:request.selectedSignoffDepartmentIds},{stageCode:'MATRIX_APPROVAL',candidateSourceType:'USER',candidateSourceIds:[request.applicationCustomMatrix.approverUserId],resolvedUserIds:[request.applicationCustomMatrix.approverUserId],approveMethod:'ANY',requireAllApprovals:false,approveRatio:null}],blockers:[],approvalSourceMode:'APPLICATION_CUSTOM',applicationCustomMatrix:request.applicationCustomMatrix,applicationCustomMatrixDigest:'d'.repeat(64),applicationCustomMatrixToken:'signed-reserved-config',applicationCustomMatrixPhase:'RESERVED',applicationActionType:revisionFlow?'REVISION':'NEW',derivationVersion:'DCC_APPLICATION_CUSTOM_MATRIX_V1',executionContract:'PUBLIC_MATRIX_NATIVE_V2',preparedProcessDefinitionId:'exact-v2',preparedDefinitionDigest:'a'.repeat(64),supervisorSourceDigest:'c'.repeat(64),supervisorSourceToken:'signed-supervisor',runtimeRuleGroups:[],runtimeCapabilityLimitations:[]}},
 saveWorkingApplicationAttributes:async(id,actual,selection)=>{calls.saves.push({id,actual,selection});saved={...saved,...selection,applicationCustomMatrix:selection.applicationCustomMatrix,actual,canSubmit:true};return true},
 submitControlledFileWorkingIteration:async(id,payload)=>{calls.submits.push({id,payload});return '20'},message:{success(){}},emit(){},isWorkflowId:value=>value==='20',ElMessageBox:{confirm:async text=>{calls.confirmations.push(text);if(cancel)throw 'cancel'}}}
 const previewPort=c.checkControlledFileRouteReadiness
 c.checkControlledFileRouteReadiness=async request=>{
  if(publicVersion){calls.previews.push(request);if(request.expectedProcessDefinitionId!=='saved-exact-definition'||request.expectedDefinitionDigest!=='a'.repeat(64)||request.executionContract!==publicVersion)throw Error('未保留已保存的真实准备定义');const actual=require('./dcc-upload-test-loader.cjs').matrixReadiness({ready:true,nodes:[{stageCode:'MATRIX_REVIEW',candidateSourceType:'DEPT',candidateSourceIds:['51']},{stageCode:'MATRIX_APPROVAL',candidateSourceType:'USER',candidateSourceIds:['77'],resolvedUserIds:['77'],approveMethod:'ALL',approveRatio:100,requireAllApprovals:true}],blockers:[]},{taxonomyId:'6',approvalIds:['77']});return {...actual,executionContract:publicVersion,preparedProcessDefinitionId:'saved-exact-definition',preparedDefinitionDigest:'a'.repeat(64)}}
  const response=await previewPort(request)
  response.applicationCustomMatrixCanonical=JSON.stringify({formatVersion:'DCC_APPLICATION_CUSTOM_MATRIX_V1',approvalSourceMode:'APPLICATION_CUSTOM',applicationCustomMatrix:request.applicationCustomMatrix,selectedSignoffDepartmentIds:request.selectedSignoffDepartmentIds,context:{tenantId:'1',projectId:'5',categoryId:'2',taxonomyId:'6',requesterId:'99',controlledFileId:'20',attributeRound:1,applicationType:revisionFlow?'REVISION':'UPLOAD',applicationContextKey:'saved-context',processDefinitionId:'exact-v2',definitionDigest:'a'.repeat(64)}})
  response.nodes[0]={...response.nodes[0],approveMethod:'ALL',approveRatio:100,requireAllApprovals:true}
  response.nodes.push({stageCode:'SUPERVISOR_REVIEW',candidateSourceType:'USER',candidateSourceIds:['88'],resolvedUserIds:['88'],approveMethod:'ANY',requireAllApprovals:false,approveRatio:null},
    {stageCode:'DOC_CONTROL_REVIEW',candidateSourceType:'USER',candidateSourceIds:[request.applicationCustomMatrix.documentControlUserId],resolvedUserIds:[request.applicationCustomMatrix.documentControlUserId],approveMethod:'ANY',requireAllApprovals:false,approveRatio:null})
  return response
 }
 if(revisionFlow){c.props.file.revisionBaseActiveControlledFileId='10';c.props.file.versionNo='A/2';c.props.file.attachments[1]={...c.props.file.attachments[1],fileName:'Two.txt',attachmentPurpose:'ATTACHMENT'}}
 c.departmentChoiceMissing=vue.ref(false)
 const names=['isPublicMatrixExecution','applicationProjection','applicationFacts','applicationSelectionError','applicationMatrixScope','displaySavedCustom','displaySavedNewAction','open','refreshPanelMatrix','preparePanelMatrix','confirmation','saveAttributes','submitInitial','selectIteration','selectIntent','readRevisionTargetVersion','submitRevision']
 vm.runInNewContext(ts.transpileModule(names.map(declaration).join('\n')+'\nexports={open,saveAttributes,submitInitial,refreshPanelMatrix,selectIntent,selectIteration,submitRevision,applicationFacts};',{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
 return {c,calls,...c.exports}
}
test('actual panel saved custom opens, saves and confirms exact reserved choices and server attachment expectation',async()=>{
 const h=host();await h.open();assert.ok(h.c.snapshot.value,h.c.error.value);assert.equal(h.c.error.value,'');assert.equal(h.c.applicationCustomMatrix.value.approverUserId,'77');assert.equal(h.c.idempotencyKey.value,'saved-context')
 assert.deepEqual(Array.from(h.c.approvalAccounts.value).map(row=>row.id),['77','66'])
 await h.saveAttributes();assert.equal(h.calls.saves.length,1,h.c.error.value+' accounts='+JSON.stringify(h.c.approvalAccounts.value));assert.equal(h.calls.saves[0].selection.applicationCustomMatrixPhase,'RESERVED')
 await h.submitInitial();assert.equal(h.calls.submits.length,1,h.c.error.value);const p=h.calls.submits[0].payload;assert.equal(p.expectedOrdinaryAttachmentManifestHash,'f'.repeat(64));assert.deepEqual(Array.from(p.expectedOrdinaryAttachmentIds),['71']);assert.equal(p.applicationCustomMatrix.documentControlUserId,'66');assert.equal(p.expectedApprovalRouteId,undefined)
 assert.match(h.calls.confirmations.at(-1),/One.pdf/);assert.match(h.calls.confirmations.at(-1),/Training.pdf/)
})
test('cancel keeps saved custom and never submits; source changed still shows original saved custom without public refresh',async()=>{
 const h=host({cancel:true});await h.open();await h.submitInitial();assert.equal(h.calls.submits.length,0);assert.equal(h.c.applicationCustomMatrix.value.approverUserId,'77')
 const changed=host({changedMode:true});await changed.open();assert.ok(changed.c.snapshot.value);assert.equal(changed.c.applicationCustomMatrix.value.documentControlUserId,'66');assert.equal(changed.calls.previews.length,0);assert.equal(changed.c.canSubmit.value,false);assert.match(changed.c.error.value,/来源已变化/)
})
test('actual PUBLIC V1 and V2 panel handlers preserve saved exact definition and original approval groups',async()=>{
 for(const publicVersion of ['PUBLIC_MATRIX_NATIVE_V1','PUBLIC_MATRIX_NATIVE_V2']){
  const h=host({publicVersion});await h.open();assert.equal(h.c.error.value,'');assert.equal(h.calls.previews[0].executionContract,publicVersion);assert.equal(h.calls.previews[0].expectedProcessDefinitionId,'saved-exact-definition');assert.ok(h.c.approvalGroups.value.length);assert.equal(h.c.approverNames.value,'批准甲')
 }
})

for(const intent of ['PARTIAL','REPLACEMENT'])test('saved custom revision '+intent+' uses actual selection and submission handlers',async()=>{
 const h=host({revisionFlow:true});await h.open();assert.equal(h.c.error.value,'');await h.selectIntent(intent)
 assert.equal(h.c.error.value,'');assert.equal(h.c.canSubmit.value,true)
 const command=revision.buildRevisionCommand('10','20',intent,'不同正文和双附件的本次变更','saved-context',h.applicationFacts.value)
 await h.submitRevision(command)
 assert.equal(h.calls.submits.length,1)
 assert.equal(h.calls.submits[0].payload.revisionChangeType,intent)
 assert.deepEqual(Array.from(h.calls.submits[0].payload.expectedOrdinaryAttachmentIds),['71','72'])
 assert.match(h.calls.confirmations.at(-1),/One.pdf.*Two.txt/)
 assert.equal(h.c.snapshotIntent.value,intent)
})

test('saved custom revision keeps strict intent mismatch rejection and cancel semantics',async()=>{
 const h=host({revisionFlow:true,cancel:true});await h.open();await h.selectIntent('PARTIAL')
 const facts=h.applicationFacts.value
 const wrong=revision.buildRevisionCommand('10','20','REPLACEMENT','不匹配类型','saved-context',facts)
 await assert.rejects(h.submitRevision(wrong),/请先读取并核对/)
 assert.equal(h.calls.confirmations.length,0);assert.equal(h.calls.submits.length,0)
 const right=revision.buildRevisionCommand('10','20','PARTIAL','本次说明','saved-context',facts)
 await assert.rejects(h.submitRevision(right),/已取消提交/)
 assert.equal(h.calls.confirmations.length,1);assert.equal(h.calls.submits.length,0)
 assert.equal(h.c.selectedIterationId.value,'20');assert.equal(h.c.applicationCustomMatrix.value.approverUserId,'77')
 assert.deepEqual(Array.from(h.c.selectedApplicationFile.value.attachments,row=>row.attachmentId),['71','72'])
})
