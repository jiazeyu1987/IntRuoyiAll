package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Leaf read port: BPM reads + exact numeric round + immutable catalog. Never depends on prepare or lifecycle resolver. */
@Service
public class DccBoundWorkflowContractResolver {
    @Resource private BpmProcessDefinitionService definitions;
    @Resource private BpmProcessInstanceService processes;
    @Resource private DccApplicationRoundService rounds;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper attributes;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper routes;
    @Resource private cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper forms;
    @Resource private DccObsoleteFormPreparationProof formPreparationProof;
    @Resource private DccObsoleteFormRuntimeStore obsoleteRuntimeStore;
    @Resource private DccApprovalMatrixTokenService nativeSourceTokens;
    @Resource private DccControlledFileApprovalRouteAssigneeResolver customRouteResolver;
    public enum RoundContractStatus { READY, NOT_RECORDED, UNSUPPORTED_DEFINITION }
    public record RoundFlowContract(@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long tenantId,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long controlledFileId,String applicationType,Integer attributeRound,String bpmRound,
            String processDefinitionKey,String processDefinitionId,String deploymentId,String definitionDigest,String executionContract,
            DccWorkflowDefinitionCatalog.FlowRule flowRule,Boolean needTraining,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Long formInstanceId,String formInstanceCode,
            RoundContractStatus status,String unavailableReason) { }
    private record ExactRound(String key,String definitionId,String deploymentId,String digest,String businessKey,String startUserId,Map<String,Object> variables,boolean completed) { }
    /** The already-authorized persisted row is independently re-proved. Typed unavailable is limited
     * to identity-correct completed history; identity/dependency/read failures are never suppressed. */
    public RoundFlowContract resolveRoundFlowContract(DccControlledFileDO file,DccApplicationRoundSummary row){
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(file==null||file.getId()==null||!Objects.equals(file.getTenantId(),tenant)||Boolean.TRUE.equals(file.getDeleted())||file.getDccProjectCodeId()==null
                ||row==null||!Objects.equals(row.getControlledFileId(),file.getId())||row.getApplicationType()==null
                ||!Set.of("UPLOAD","REVISION","OBSOLETE").contains(row.getApplicationType())||row.getAttributeRound()==null||row.getAttributeRound()<=0
                ||row.getBpmRound()==null||row.getBpmRound().isBlank()||!row.getBpmRound().equals(row.getBpmRound().strip()))
            throw new IllegalArgumentException("ROUND_FLOW_PERSISTED_IDENTITY_INVALID");
        String type=row.getApplicationType(),bpm=row.getBpmRound();
        if(rounds.require(type,file.getId(),bpm)!=row.getAttributeRound())throw new IllegalStateException("ROUND_FLOW_EXACT_ATTRIBUTE_ROUND_MISMATCH");
        rounds.assertBoundRound(file.getDccProjectCodeId(),type,file.getId(),row.getAttributeRound());
        var saved=attributes.find(type,file.getId(),row.getAttributeRound());
        if(saved!=null&&(!Objects.equals(saved.getTenantId(),tenant)||!Objects.equals(saved.getProjectCodeId(),file.getDccProjectCodeId())
                ||!Objects.equals(saved.getApplicationId(),file.getId())||!Objects.equals(saved.getApplicationType(),type)||!Objects.equals(saved.getApplicationRound(),row.getAttributeRound())))
            throw new IllegalStateException("ROUND_FLOW_ATTRIBUTE_RETURN_IDENTITY_INVALID");
        String key="UPLOAD".equals(type)?DccControlledFileProcessDefinitionKeys.UPLOAD:"REVISION".equals(type)?DccControlledFileProcessDefinitionKeys.REVISION:DccControlledFileProcessDefinitionKeys.OBSOLETE;
        var exact=exactRound(tenant,bpm,key);
        if("OBSOLETE".equals(type)){
            // Form identity and actual graph are proved even when the later historical attribute was not frozen.
            var result=resolveObsoleteRound(file,row,exact);
            if(saved==null||!Boolean.TRUE.equals(saved.getSubmitted())){if(!exact.completed())throw new IllegalStateException("ROUND_FLOW_CURRENT_SUBMITTED_ATTRIBUTE_REQUIRED");return result(file,row,exact,null,null,result.formInstanceId(),result.formInstanceCode(),RoundContractStatus.NOT_RECORDED,"FORM_PREPARATION_NOT_RECORDED");}
            return result;
        }
        if(!file.getId().toString().equals(exact.businessKey())||!file.getId().toString().equals(String.valueOf(exact.variables().get("controlledFileId"))))
            throw new IllegalStateException("ROUND_FLOW_NATIVE_BUSINESS_IDENTITY_INVALID");
        var graph=DccWorkflowDefinitionCatalog.find(key,exact.digest());
        boolean current=Objects.equals(bpm,file.getProcessInstanceId());
        if(current){
            if(!Objects.equals(file.getProcessDefinitionKey(),key))throw new IllegalStateException("ROUND_FLOW_CURRENT_NATIVE_TYPE_MISMATCH");
            boolean any=file.getPreparedWorkflowContract()!=null||file.getPreparedProcessDefinitionId()!=null||file.getPreparedDefinitionDigest()!=null;
            if(any){if(file.getPreparedWorkflowContract()==null||!Objects.equals(file.getPreparedProcessDefinitionId(),exact.definitionId())
                    ||!Objects.equals(file.getPreparedDefinitionDigest(),exact.digest()))throw new IllegalStateException("ROUND_FLOW_CURRENT_PREPARED_MISMATCH");
                graph=DccWorkflowDefinitionCatalog.require(file.getPreparedWorkflowContract(),key,exact.digest());}
            else if(graph!=null&&!"LEGACY_V4_ACTION".equals(graph.executionContract()))throw new IllegalStateException("ROUND_FLOW_CURRENT_PUBLIC_PREPARATION_REQUIRED");
        }
        if(saved==null||!Boolean.TRUE.equals(saved.getSubmitted())){
            boolean recordedCurrent=current&&(file.getPreparedWorkflowContract()!=null||file.getPreparedProcessDefinitionId()!=null||file.getPreparedDefinitionDigest()!=null);
            if(!exact.completed()||recordedCurrent||graph!=null&&(graph.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL
                    ||graph.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM))
                throw new IllegalStateException("ROUND_FLOW_CURRENT_SUBMITTED_ATTRIBUTE_REQUIRED");
            return result(file,row,exact,null,null,null,null,RoundContractStatus.NOT_RECORDED,"HISTORICAL_CONTRACT_NOT_RECORDED");
        }
        if(graph==null){if(!exact.completed())throw new IllegalStateException("ROUND_FLOW_CURRENT_UNREGISTERED_DEFINITION");
            return result(file,row,exact,null,null,null,null,RoundContractStatus.UNSUPPORTED_DEFINITION,"UNSUPPORTED_HISTORICAL_GRAPH");}
        if(!(exact.variables().get("needTraining") instanceof Boolean training))throw new IllegalStateException("ROUND_FLOW_EXACT_TRAINING_BOOLEAN_REQUIRED");
        if(graph.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL
                ||graph.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM){
            if(!current){if(!exact.completed())throw new IllegalStateException("ROUND_FLOW_ACTIVE_HISTORICAL_SOURCE_NOT_RECORDED");
                // Existing route rows have only File identity, not a per-round immutable source key.
                // They must never stand in for this different historical BPM's provenance.
                return result(file,row,exact,null,training,null,null,RoundContractStatus.NOT_RECORDED,"HISTORICAL_CONTRACT_NOT_RECORDED");}
            requirePublicRoundProvenance(file);
            requireNativeSupervisorBpmSource(file,exact.variables());
        }
        return result(file,row,exact,graph,training,null,null,RoundContractStatus.READY,null);
    }
    private ExactRound exactRound(Long tenant,String bpm,String expectedKey){
        var runtime=processes.getProcessInstance(bpm);String id,tenantText,business,key,definitionId,startUser;Map<String,Object> variables;boolean completed;
        if(runtime!=null){id=runtime.getId();tenantText=runtime.getTenantId();business=runtime.getBusinessKey();key=runtime.getProcessDefinitionKey();definitionId=runtime.getProcessDefinitionId();startUser=runtime.getStartUserId();variables=runtime.getProcessVariables();completed=runtime.isEnded();}
        else{var history=processes.getHistoricProcessInstance(bpm);if(history==null)throw new IllegalStateException("ROUND_FLOW_ACTUAL_PROCESS_REQUIRED");id=history.getId();tenantText=history.getTenantId();business=history.getBusinessKey();key=history.getProcessDefinitionKey();definitionId=history.getProcessDefinitionId();startUser=history.getStartUserId();variables=history.getProcessVariables();completed=history.getEndTime()!=null;}
        if(!Objects.equals(id,bpm)||!Objects.equals(tenant.toString(),tenantText)||!Objects.equals(expectedKey,key)||definitionId==null||definitionId.isBlank()||variables==null)
            throw new IllegalStateException("ROUND_FLOW_ACTUAL_PROCESS_IDENTITY_INVALID");
        var definition=definitions.getProcessDefinition(definitionId);
        if(definition==null||!Objects.equals(definitionId,definition.getId())||!Objects.equals(tenant.toString(),definition.getTenantId())||!Objects.equals(key,definition.getKey())
                ||definition.getDeploymentId()==null||definition.getDeploymentId().isBlank())throw new IllegalStateException("ROUND_FLOW_EXACT_DEFINITION_IDENTITY_INVALID");
        var model=definitions.getProcessDefinitionBpmnModel(definitionId);
        if(model==null||model.getProcesses().size()!=1||model.getMainProcess()==null||!Objects.equals(key,model.getMainProcess().getId()))throw new IllegalStateException("ROUND_FLOW_EXACT_FULL_MODEL_INVALID");
        return new ExactRound(key,definitionId,definition.getDeploymentId(),DccLifecycleEffectiveContractResolver.definitionDigest(model),business,startUser,variables,completed);
    }
    private RoundFlowContract resolveObsoleteRound(DccControlledFileDO file,DccApplicationRoundSummary row,ExactRound exact){
        var found=forms.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO>()
                .eq("tenant_id",file.getTenantId()).eq("bpm_process_instance_id",row.getBpmRound()).eq("deleted",false));
        if(found==null||found.size()!=1)throw new IllegalStateException("ROUND_FLOW_UNIQUE_OBSOLETE_FORM_REQUIRED");var form=found.get(0);
        if(form.getId()==null||form.getId()<=0||!Objects.equals(form.getTenantId(),file.getTenantId())||!Objects.equals(form.getBpmProcessInstanceId(),row.getBpmRound())||Boolean.TRUE.equals(form.getDeleted())
                ||form.getApplicantUserId()==null||form.getApplicantUserId()<=0||!Objects.equals(form.getApplicantUserId().toString(),exact.startUserId())
                ||form.getInstanceCode()==null||form.getInstanceCode().isBlank()||!"DCC".equals(form.getSystemCode())||!"CONTROLLED_FILE".equals(form.getObjectType())
                ||!"OBSOLETE".equals(form.getActionCode())||!Objects.equals(file.getId().toString(),form.getObjectId())||!Objects.equals(file.getVersionNo(),form.getObjectVersion())
                ||!Objects.equals("FORM_ACTION:"+form.getInstanceCode(),exact.businessKey())||!Objects.equals(form.getId().toString(),String.valueOf(exact.variables().get("formInstanceId")))
                ||!Objects.equals(form.getSystemCode(),exact.variables().get("systemCode"))||!Objects.equals(form.getObjectType(),exact.variables().get("objectType"))
                ||!Objects.equals(form.getActionCode(),exact.variables().get("actionCode"))||!Objects.equals(form.getObjectId(),String.valueOf(exact.variables().get("objectId")))
                ||!Objects.equals(form.getObjectVersion(),exact.variables().get("objectVersion"))||!Objects.equals(file.getId().toString(),String.valueOf(exact.variables().get("controlledFileId")))
                ||!Objects.equals(file.getTenantId().toString(),String.valueOf(exact.variables().get("tenantId"))))
            throw new IllegalStateException("ROUND_FLOW_OBSOLETE_FORM_BUSINESS_IDENTITY_INVALID");
        var data=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(form.getFormDataJson(),Map.class);
        if(data==null)throw new IllegalStateException("ROUND_FLOW_OBSOLETE_FORM_JSON_INVALID");Object raw=data.get("dccExecutionPreparation");
        if(raw==null){if(!exact.completed())throw new IllegalStateException("ROUND_FLOW_ACTIVE_OBSOLETE_PREPARATION_REQUIRED");
            return result(file,row,exact,null,null,form.getId(),form.getInstanceCode(),RoundContractStatus.NOT_RECORDED,"FORM_PREPARATION_NOT_RECORDED");}
        if(!(raw instanceof Map<?,?> prepared))throw new IllegalStateException("ROUND_FLOW_OBSOLETE_PREPARATION_SHAPE_INVALID");
        if(!Objects.equals(String.valueOf(file.getTenantId()),String.valueOf(prepared.get("tenantId")))||!Objects.equals(String.valueOf(form.getId()),String.valueOf(prepared.get("formInstanceId")))
                ||!Objects.equals(form.getInstanceCode(),prepared.get("formInstanceCode"))||!Objects.equals(file.getId().toString(),String.valueOf(prepared.get("controlledFileId")))
                ||!Objects.equals(file.getVersionNo(),prepared.get("controlledFileVersionNo"))||!Objects.equals(exact.definitionId(),prepared.get("preparedProcessDefinitionId"))
                ||!Objects.equals(exact.digest(),prepared.get("preparedDefinitionDigest")))throw new IllegalStateException("ROUND_FLOW_OBSOLETE_PREPARATION_IDENTITY_INVALID");
        var graph=formPreparationProof.verifyRecorded(file,form,prepared,exact.definitionId(),exact.digest());
        if("DCC_OBSOLETE_FORM_PREPARATION_V2".equals(prepared.get("formatVersion"))){
            var runtime=obsoleteRuntimeStore.requireByBpm(file.getTenantId(),file.getId(),row.getBpmRound());var head=runtime.head();
            if(!Objects.equals(head.formInstanceId(),form.getId())||!Objects.equals(head.formInstanceCode(),form.getInstanceCode())||!Objects.equals(head.attributeRound(),row.getAttributeRound())
                    ||!Objects.equals(head.preparedProcessDefinitionId(),exact.definitionId())||!Objects.equals(head.preparedDefinitionDigest(),exact.digest()))throw new IllegalStateException("ROUND_FLOW_OBSOLETE_RUNTIME_IDENTITY_INVALID");
        }
        return result(file,row,exact,graph,null,form.getId(),form.getInstanceCode(),RoundContractStatus.READY,null);
    }
    private RoundFlowContract result(DccControlledFileDO file,DccApplicationRoundSummary row,ExactRound exact,DccWorkflowDefinitionCatalog.Graph graph,Boolean training,Long formId,String code,RoundContractStatus status,String reason){
        return new RoundFlowContract(file.getTenantId(),file.getId(),row.getApplicationType(),row.getAttributeRound(),row.getBpmRound(),exact.key(),exact.definitionId(),exact.deploymentId(),exact.digest(),
                graph==null?null:graph.executionContract(),graph==null?null:graph.flowRule(),training,formId,code,status,reason);
    }
    private void requirePublicRoundProvenance(DccControlledFileDO file){
        if("APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode())){customRouteResolver.restoreCustomFrozenRoute(file,routes.selectListByControlledFileId(file.getId()));return;}
        var rows=routes.selectListByControlledFileId(file.getId());
        if(rows==null||rows.size()!=4)throw new IllegalStateException("ROUND_FLOW_PUBLIC_FROZEN_PROVENANCE_REQUIRED");
        var first=rows.get(0);String raw=first.getSourceApprovalMatrixJson(),runtime=first.getRuntimeApprovalNodesJson();
        if(raw==null||runtime==null)throw new IllegalStateException("ROUND_FLOW_PUBLIC_FROZEN_PROVENANCE_REQUIRED");
        var baseline=DccApprovalMatrixSourceService.readCanonical(raw);
        if(!Objects.equals(file.getTenantId(),baseline.getTenantId())||!Objects.equals(file.getCategoryId(),baseline.getCategoryId())||!Objects.equals(file.getFileTypeTaxonomyId(),baseline.getFileTypeTaxonomyId())
                ||!"LEGACY".equals(baseline.getActionType()))throw new IllegalStateException("ROUND_FLOW_PUBLIC_MATRIX_FAMILY_INVALID");
        var seen=new java.util.HashSet<String>();var actual=new java.util.ArrayList<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode>();
        for(var stage:rows){if(stage==null||!Objects.equals(file.getId(),stage.getControlledFileId())||!seen.add(stage.getStageCode())||stage.getStageOrder()==null
                ||!Objects.equals(raw,stage.getSourceApprovalMatrixJson())||!Objects.equals(runtime,stage.getRuntimeApprovalNodesJson())
                ||!Objects.equals(baseline.getRouteId(),stage.getSourceApprovalMatrixId())||!Objects.equals(baseline.getRouteVersionNo(),stage.getSourceApprovalMatrixVersion())
                ||!Objects.equals(baseline.getMatrixDigest(),stage.getSourceApprovalMatrixDigest())||!cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.VERSION.equals(stage.getDerivationVersion()))
            throw new IllegalStateException("ROUND_FLOW_PUBLIC_FROZEN_PROVENANCE_INVALID");
            var candidates=exactIds(stage.getCandidateSourceIds());var users=exactIds(stage.getResolvedUserIds());
            if(!Objects.equals(stage.getCandidateSourceId(),candidates.get(0)))throw new IllegalStateException("ROUND_FLOW_PUBLIC_FROZEN_CANDIDATE_INVALID");
            if("SUPERVISOR_REVIEW".equals(stage.getStageCode())&&(!Integer.valueOf(0).equals(stage.getStageNo())||stage.getStageOrder()!=0||!"USER".equals(stage.getCandidateSourceType())
                    ||candidates.size()!=1||!candidates.equals(users)))throw new IllegalStateException("ROUND_FLOW_PUBLIC_SUPERVISOR_SNAPSHOT_INVALID");
            actual.add(new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(stage.getStageNo(),stage.getStageCode(),stage.getStageName(),stage.getStageOrder(),stage.getCandidateSourceType(),stage.getCandidateSourceId(),candidates,stage.getApproveMethod(),stage.getApproveRatio(),stage.getRequireAllApprovals(),users));
        }
        if(!seen.equals(Set.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")))throw new IllegalStateException("ROUND_FLOW_PUBLIC_FROZEN_STAGE_INVALID");
        actual.sort(java.util.Comparator.comparing(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageOrder));
        var document=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(runtime,Map.class);
        if(document==null||!"DCC_RUNTIME_APPROVAL_NODES_V1".equals(document.get("formatVersion"))||!(document.get("groups") instanceof java.util.List<?>))throw new IllegalStateException("ROUND_FLOW_PUBLIC_RUNTIME_PROVENANCE_INVALID");
        var stored=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(document.get("nodes")),DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode.class);
        if(!actual.equals(stored))throw new IllegalStateException("ROUND_FLOW_PUBLIC_RUNTIME_PROVENANCE_INVALID");
        if(document.containsKey("nativeSupervisorSource")){
            if(!(document.get("nativeSupervisorSource") instanceof Map<?,?> frozen))throw new IllegalStateException("ROUND_FLOW_NATIVE_SUPERVISOR_PROVENANCE_INVALID");
            nativeSourceTokens.requireRecordedNativeSupervisor(file,baseline,actual,(Map<String,Object>)frozen);
        }
    }
    private java.util.List<Long> exactIds(String text){if(text==null||text.isBlank())throw new IllegalStateException("ROUND_FLOW_EXACT_CANDIDATE_IDS_REQUIRED");var ids=new java.util.ArrayList<Long>();
        for(var id:text.split(",",-1)){if(!id.matches("[1-9][0-9]*"))throw new IllegalStateException("ROUND_FLOW_EXACT_CANDIDATE_IDS_INVALID");ids.add(Long.valueOf(id));}return java.util.List.copyOf(ids);}
    public record BoundExecution(Long tenantId,Long controlledFileId,String processInstanceId,Integer attributeRound,
            String processKey,String processDefinitionId,String deploymentId,String definitionDigest,String executionContract,
            DccWorkflowDefinitionCatalog.FlowRule flowRule,boolean active){
        public boolean trainingAfterDocumentControl(){return flowRule==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL
                || flowRule==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM;}
        public boolean delayedConfirmationSupported(){return flowRule==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM;}
    }
    public BoundExecution resolve(DccControlledFileDO file){
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(file==null||file.getId()==null||file.getId()<=0||!Objects.equals(file.getTenantId(),tenant)||Boolean.TRUE.equals(file.getDeleted())
                ||file.getProcessInstanceId()==null||file.getProcessInstanceId().isBlank()||!file.getProcessInstanceId().equals(file.getProcessInstanceId().strip())
                ||!Set.of(DccControlledFileProcessDefinitionKeys.UPLOAD,DccControlledFileProcessDefinitionKeys.REVISION).contains(file.getProcessDefinitionKey()==null?"":file.getProcessDefinitionKey()))
            throw new IllegalArgumentException("EXECUTION_CONTRACT_BOUND_FILE_IDENTITY_INVALID");
        String bpm=file.getProcessInstanceId();var runtime=processes.getProcessInstance(bpm);String id,tenantText,business,key,definitionId;Map<String,Object> variables;
        if(runtime!=null){id=runtime.getId();tenantText=runtime.getTenantId();business=runtime.getBusinessKey();key=runtime.getProcessDefinitionKey();definitionId=runtime.getProcessDefinitionId();variables=runtime.getProcessVariables();}
        else{var history=processes.getHistoricProcessInstance(bpm);if(history==null)throw new IllegalStateException("EXECUTION_CONTRACT_BOUND_SOURCE_PROCESS_REQUIRED");
            id=history.getId();tenantText=history.getTenantId();business=history.getBusinessKey();key=history.getProcessDefinitionKey();definitionId=history.getProcessDefinitionId();variables=history.getProcessVariables();}
        if(!bpm.equals(id)||!tenant.toString().equals(tenantText)||!file.getId().toString().equals(business)||!file.getProcessDefinitionKey().equals(key)
                ||definitionId==null||definitionId.isBlank()||variables==null||!file.getId().toString().equals(String.valueOf(variables.get("controlledFileId"))))
            throw new IllegalStateException("EXECUTION_CONTRACT_BOUND_SOURCE_IDENTITY_INVALID");
        var definition=definitions.getProcessDefinition(definitionId);
        if(definition==null||!definitionId.equals(definition.getId())||!tenant.toString().equals(definition.getTenantId())||!key.equals(definition.getKey())
                ||definition.getDeploymentId()==null||definition.getDeploymentId().isBlank())throw new IllegalStateException("EXECUTION_CONTRACT_DEFINITION_IDENTITY_INVALID");
        var model=definitions.getProcessDefinitionBpmnModel(definitionId);
        if(model==null||model.getMainProcess()==null||model.getProcesses().size()!=1||!key.equals(model.getMainProcess().getId()))throw new IllegalStateException("EXECUTION_CONTRACT_FULL_MODEL_INVALID");
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(model);
        boolean anyPrepared=file.getPreparedWorkflowContract()!=null||file.getPreparedProcessDefinitionId()!=null||file.getPreparedDefinitionDigest()!=null;
        String contract=anyPrepared?file.getPreparedWorkflowContract():"LEGACY_V4_ACTION";
        if(anyPrepared&&(!definitionId.equals(file.getPreparedProcessDefinitionId())||!digest.equals(file.getPreparedDefinitionDigest())||contract==null))
            throw new IllegalStateException("EXECUTION_CONTRACT_BOUND_PREPARED_DEFINITION_MISMATCH");
        var graph=DccWorkflowDefinitionCatalog.require(contract,key,digest);
        Integer round=null;
        if(graph.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL
                || graph.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM){
            String type=DccControlledFileProcessDefinitionKeys.UPLOAD.equals(key)?"UPLOAD":"REVISION";
            round=rounds.require(type,file.getId(),bpm);rounds.assertBoundRound(file.getDccProjectCodeId(),type,file.getId(),round);
            var saved=attributes.find(type,file.getId(),round);
            if(saved==null||!Objects.equals(saved.getTenantId(),tenant)||!Objects.equals(saved.getProjectCodeId(),file.getDccProjectCodeId())
                    ||!Boolean.TRUE.equals(saved.getSubmitted()))throw new IllegalStateException("EXECUTION_CONTRACT_BOUND_ATTRIBUTE_IDENTITY_INVALID");
            var currentRoutes=routes==null?null:routes.selectListByControlledFileId(file.getId());
            if("APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode())){requirePublicRoundProvenance(file);requireNativeSupervisorBpmSource(file,variables);}
            else if(variables.containsKey("DCC_NATIVE_SUPERVISOR_SOURCE")){requirePublicRoundProvenance(file);requireNativeSupervisorBpmSource(file,variables);}
            else if(currentRoutes!=null&&currentRoutes.stream().anyMatch(row->row.getRuntimeApprovalNodesJson()!=null&&cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getRuntimeApprovalNodesJson(),Map.class).containsKey("nativeSupervisorSource")))throw new IllegalStateException("NATIVE_SUPERVISOR_BPM_SOURCE_REQUIRED");
        }
        if(graph.flowRule()==DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM){
            if(!Set.of("DELAYED","AFTER_STANDARDIZATION_SIGNATURE").contains(file.getEffectiveMode()==null?"":file.getEffectiveMode())
                    || file.getEffectiveDate()!=null || !Objects.equals(file.getEffectiveMode(),variables.get("effectiveMode"))
                    || file.getRequesterId()==null || !String.valueOf(file.getRequesterId()).equals(String.valueOf(variables.get("dccOriginalRequesterId")))
                    || !Objects.equals(file.getNeedTraining(),variables.get("needTraining")))
                throw new IllegalStateException("DELAYED_CONFIRMATION_BOUND_SELECTION_IDENTITY_INVALID");
            Object maps=variables.get(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES);
            if(!(maps instanceof Map<?,?> candidates) || !(candidates.get("DELAYED_EFFECTIVE_CONFIRM") instanceof java.util.List<?> selected)
                    || selected.size()!=1 || !String.valueOf(file.getRequesterId()).equals(String.valueOf(selected.get(0))))
                throw new IllegalStateException("DELAYED_CONFIRMATION_ORIGINAL_REQUESTER_CANDIDATE_REQUIRED");
        }
        return new BoundExecution(tenant,file.getId(),bpm,round,key,definitionId,definition.getDeploymentId(),digest,graph.executionContract(),graph.flowRule(),runtime!=null);
    }
    private void requireNativeSupervisorBpmSource(DccControlledFileDO file,Map<String,Object> variables){
        if("APPLICATION_CUSTOM".equals(file.getDraftApprovalSourceMode())){
            var restored=customRouteResolver.restoreCustomFrozenRoute(file,routes.selectListByControlledFileId(file.getId()));
            if(variables.containsKey("DCC_NATIVE_SUPERVISOR_SOURCE")||!Objects.equals(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(restored.applicationCustomSource())),
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(variables.get("DCC_APPLICATION_CUSTOM_SOURCE")))))throw new IllegalArgumentException("CUSTOM_BPM_FROZEN_SOURCE_MISMATCH");
            return;
        }
        if(!variables.containsKey("DCC_NATIVE_SUPERVISOR_SOURCE")){
            var recorded=routes.selectListByControlledFileId(file.getId());
            if(recorded!=null&&recorded.stream().anyMatch(row->row.getRuntimeApprovalNodesJson()!=null&&cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getRuntimeApprovalNodesJson(),Map.class).containsKey("nativeSupervisorSource")))
                throw new IllegalStateException("NATIVE_SUPERVISOR_BPM_SOURCE_REQUIRED");
            return;
        }
        var frozen=variables.get("DCC_NATIVE_SUPERVISOR_SOURCE");if(!(frozen instanceof Map<?,?>))throw new IllegalStateException("NATIVE_SUPERVISOR_BPM_SOURCE_INVALID");
        var current=routes.selectListByControlledFileId(file.getId());if(current==null||current.size()!=4)throw new IllegalStateException("NATIVE_SUPERVISOR_COMPLETE_CURRENT_SOURCE_REQUIRED");
        for(var row:current){var runtime=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getRuntimeApprovalNodesJson(),Map.class);if(runtime==null||!Objects.equals(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(runtime.get("nativeSupervisorSource"))),cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(frozen))))throw new IllegalStateException("NATIVE_SUPERVISOR_ACTUAL_BPM_SOURCE_MISMATCH");}
    }
}
