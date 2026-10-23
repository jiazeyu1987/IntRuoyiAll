// Isolated server response port: real canonical-shaped data and a test HMAC;
// client transport tests never claim this proves server authorization/signature qualification.
const crypto = require('node:crypto')
const scope = { tenantId:'1',projectId:'5',categoryId:'2',taxonomyId:'4',requesterId:'71',actionType:'NEW',applicationContextKey:'scope',approvalSourceMode:'APPLICATION_CUSTOM' }
const config = { approverUserId:'9223372036854775709',documentControlUserId:'66' }
const sorted = value => Array.isArray(value) ? value.map(sorted) : value && typeof value === 'object'
  ? Object.fromEntries(Object.keys(value).sort().map(key=>[key,sorted(value[key])])) : value
function response(s=scope,value=config,ids=['7'],ready=true) {
  const phase=s.controlledFileId?'RESERVED':'PREVIEW'
  const context={tenantId:s.tenantId,projectId:s.projectId,categoryId:s.categoryId,taxonomyId:s.taxonomyId,requesterId:s.requesterId||'71',
    applicationType:s.actionType==='NEW'?'UPLOAD':s.actionType,applicationContextKey:s.applicationContextKey,
    processDefinitionId:'exact-v2-definition',definitionDigest:'a'.repeat(64),
    ...(phase==='RESERVED'?{controlledFileId:s.controlledFileId,attributeRound:s.attributeRound||1}:{})}
  const selected=ids===null?null:[...ids].sort((a,b)=>BigInt(a)<BigInt(b)?-1:BigInt(a)>BigInt(b)?1:0)
  const canonical=JSON.stringify(sorted({formatVersion:phase==='PREVIEW'?'DCC_APPLICATION_CUSTOM_MATRIX_PREVIEW_V1':'DCC_APPLICATION_CUSTOM_MATRIX_V1',
    approvalSourceMode:'APPLICATION_CUSTOM',context,applicationCustomMatrix:value,selectedSignoffDepartmentIds:selected}))
  const signoff={stageCode:'MATRIX_REVIEW',candidateSourceType:'DEPT',candidateSourceIds:selected,approveMethod:'ALL',approveRatio:100,requireAllApprovals:true,resolvedUserIds:selected?.map(()=>'90')}
  const single=(stage,id)=>({stageCode:stage,candidateSourceType:'USER',candidateSourceIds:[id],resolvedUserIds:[id],approveMethod:'ANY',approveRatio:null,requireAllApprovals:false})
  return {ready,nodes:ready?[single('SUPERVISOR_REVIEW','72'),signoff,single('MATRIX_APPROVAL',value.approverUserId),single('DOC_CONTROL_REVIEW',value.documentControlUserId)]:[],
    blockers:ready?[]:[{reasonCode:'CUSTOM_MATRIX_APPROVER_REQUIRED',message:'请选择本次批准人'}],approvalSourceMode:'APPLICATION_CUSTOM',applicationCustomMatrix:value,
    applicationCustomMatrixCanonical:canonical,applicationCustomMatrixDigest:crypto.createHash('sha256').update(canonical).digest('hex'),
    applicationCustomMatrixToken:crypto.createHmac('sha256','a32-isolated-server-port').update(canonical).digest('base64url'),applicationCustomMatrixPhase:phase,
    derivationVersion:'DCC_APPLICATION_CUSTOM_MATRIX_V1',applicationActionType:s.actionType,routeId:null,routeVersionNo:null,approvalMatrix:null,matrixBefore:null,matrixAfter:null,
    executionContract:'PUBLIC_MATRIX_NATIVE_V2',preparedProcessDefinitionId:context.processDefinitionId,preparedDefinitionDigest:context.definitionDigest,
    supervisorSourceDigest:ready?'c'.repeat(64):null,supervisorSourceToken:ready?'server-supervisor-proof':null,runtimeRuleGroups:[],runtimeCapabilityLimitations:[]}
}
module.exports={scope,config,response}
