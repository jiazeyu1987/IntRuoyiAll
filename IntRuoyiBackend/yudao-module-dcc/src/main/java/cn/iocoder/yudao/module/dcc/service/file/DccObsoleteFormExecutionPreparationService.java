package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormExactBpmSubmissionAdapter;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** DCC-only server preparation of independent obsolete form JSON; no native File preparation write. */
@Service
public class DccObsoleteFormExecutionPreparationService {
    private static final String PREPARATION="dccExecutionPreparation", PURPOSE="DCC_OBSOLETE_FORM_PREPARATION_V1";
    @Resource private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccProjectCodeMapper projects;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;
    @Resource private DccControlledFileCategoryPermissionSupport categoryPermissions;
    @Resource private DccApprovalMatrixSourceService sources;
    @Resource private DccApprovalMatrixTokenService tokens;
    @Resource private DccApplicationExecutionContractService executionContracts;
    @Resource private DccControlledFileApprovalRouteAssigneeResolver routeResolver;
    @Resource private DccControlledFileRouteReadinessService readiness;
    @Resource private ObjectProvider<DccDirectSupervisorSource> supervisors;
    @Resource private AdminUserApi users;
    @Resource private DccApprovalMatrixTokenProperties signing;
    @Resource private DccObsoleteFormPreparationProof proof;
    @Resource private DccApplicationRoundService rounds;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService projectAttributes;
    @Resource private cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper forms;
    @Resource private DccObsoleteFormRuntimeStore runtimeStore;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper taskSnapshots;
    @Resource private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService reworkProcesses;
    @Resource private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService reworkRelations;
    @Resource private DccBoundWorkflowContractResolver reworkBoundContracts;
    public record ReworkSource(Long formInstanceId,String formInstanceCode,String bpmProcessInstanceId,int attributeRound,String status,String versionNo,
            Long fileId,Long projectId,String reason,List<Long> selectedSignoffDepartmentIds,
            cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes defaultSource,
            cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes actualAttributes,
            String defaultSourceJson,String actualAttributesJson,String sourceApprovalMatrixJson,String runtimeApprovalNodesJson,
            String preparedProcessDefinitionId,String preparedDefinitionDigest,String executionContract,
            Map<String,Object> preparation,cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRoundEvidence relations,String sourceDigest) {}
    public static final class ReworkSourceUnavailableException extends IllegalArgumentException {
        public ReworkSourceUnavailableException(String message){super(message);}
    }
    /** Explicit terminated source facts. No provider lookup, current matrix, draft creation or source mutation. */
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ReworkSource readReworkSource(Long actor,Long fileId,Long formId,String code,String bpm,Integer expectedRound){
        return readReworkSource(actor,fileId,formId,code,bpm,expectedRound,false,true);
    }
    /** Already submitted histories retain their recorded provenance, with the same actor and scope checks. */
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ReworkSource readRecordedReworkSource(Long actor,Long fileId,Long formId,String code,String bpm,Integer expectedRound){
        return readReworkSource(actor,fileId,formId,code,bpm,expectedRound,false,false);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public ReworkSource readReworkSourceForUpdate(Long actor,Long fileId,Long formId,String code,String bpm,Integer expectedRound){
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw invalid("OUTER_TRANSACTION_REQUIRED");return readReworkSource(actor,fileId,formId,code,bpm,expectedRound,true,true);
    }
    private ReworkSource readReworkSource(Long actor,Long fileId,Long formId,String code,String bpm,Integer expectedRound,boolean locked,boolean currentEligibility){
        Long tenant=TenantContextHolder.getRequiredTenantId();var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(actor==null||actor<=0||login==null||!Objects.equals(login.getId(),actor)||!Objects.equals(login.getTenantId(),tenant)||!Integer.valueOf(2).equals(login.getUserType())
                ||fileId==null||fileId<=0||formId==null||formId<=0||code==null||code.isBlank()||!code.equals(code.strip())||bpm==null||bpm.isBlank()||!bpm.equals(bpm.strip())||expectedRound==null||expectedRound<=0)throw invalid("REWORK_SOURCE_INPUT_INVALID");
        var form=locked?forms.selectByIdAndTenantForUpdate(tenant,formId):forms.selectById(formId);var file=files.selectById(fileId);
        if(form==null||file==null||!Objects.equals(form.getId(),formId)||!Objects.equals(form.getTenantId(),tenant)||!Objects.equals(file.getTenantId(),tenant)||Boolean.TRUE.equals(form.getDeleted())||Boolean.TRUE.equals(file.getDeleted())
                ||!Objects.equals(form.getApplicantUserId(),actor)||!Objects.equals(form.getInstanceCode(),code)||!Objects.equals(form.getBpmProcessInstanceId(),bpm)||!Set.of("REJECTED","ABANDONED").contains(form.getStatus())
                ||!"DCC".equals(form.getDataDomain())||!"DCC".equals(form.getSystemCode())||!"CONTROLLED_FILE".equals(form.getObjectType())||!"OBSOLETE".equals(form.getActionCode())||!Objects.equals(form.getObjectId(),fileId.toString())||!Objects.equals(form.getObjectVersion(),file.getVersionNo()))throw invalid("REWORK_TERMINATED_FORM_REQUIRED");
        requireReworkFile(actor,file,currentEligibility);var history=reworkProcesses.getHistoricProcessInstance(bpm);
        int outcome="REJECTED".equals(form.getStatus())?3:4;var variables=history==null?null:history.getProcessVariables();
        if(reworkProcesses.getProcessInstance(bpm)!=null||history==null||history.getEndTime()==null||!Objects.equals(history.getId(),bpm)||!Objects.equals(history.getTenantId(),tenant.toString())||!Objects.equals(history.getStartUserId(),actor.toString())
                ||!Objects.equals(history.getBusinessKey(),"FORM_ACTION:"+code)||!DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(history.getProcessDefinitionKey())||variables==null||!Integer.valueOf(outcome).equals(variables.get("PROCESS_STATUS"))
                ||!Objects.equals(formId.toString(),String.valueOf(variables.get("formInstanceId")))||!Objects.equals(fileId.toString(),String.valueOf(variables.get("controlledFileId")))||!Objects.equals(fileId.toString(),String.valueOf(variables.get("objectId")))
                ||!Objects.equals(file.getVersionNo(),variables.get("objectVersion"))||!Objects.equals(tenant.toString(),String.valueOf(variables.get("tenantId")))||!"DCC".equals(variables.get("systemCode"))||!"CONTROLLED_FILE".equals(variables.get("objectType"))||!"OBSOLETE".equals(variables.get("actionCode")))throw invalid("REWORK_TERMINATED_BPM_REQUIRED");
        int number=rounds.require("OBSOLETE",fileId,bpm);if(number!=expectedRound)throw invalid("REWORK_SOURCE_ROUND_MISMATCH");
        var attrs=projectAttributes.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);requireNumericAttributes(attrs,file,number,true);if(!Boolean.TRUE.equals(attrs.getSubmitted()))throw invalid("REWORK_FROZEN_SOURCE_REQUIRED");
        var data=parsed(form.getFormDataJson());if(!(data.get(PREPARATION) instanceof Map<?,?>))throw invalid("REWORK_RECORDED_PREPARATION_REQUIRED");var preparation=new TreeMap<String,Object>((Map<String,Object>)data.get(PREPARATION));
        if("DCC_OBSOLETE_FORM_PREPARATION_V2".equals(preparation.get("formatVersion"))){DccObsoleteFormRuntimeStore.RuntimeEvidence runtime;
            try{runtime=runtimeStore.requireByBpm(tenant,fileId,bpm);}catch(DccObsoleteFormRuntimeStore.RuntimeSourceNotRecordedException absent){throw new ReworkSourceUnavailableException("DCC_OBSOLETE_REWORK_RUNTIME_SOURCE_NOT_RECORDED");}
            if(!Objects.equals(runtime.head().formInstanceId(),formId)||!Objects.equals(runtime.head().formInstanceCode(),code)||runtime.head().attributeRound()!=number)throw invalid("REWORK_RUNTIME_SOURCE_MISMATCH");}
        else if(PURPOSE.equals(preparation.get("formatVersion"))){var row=new DccApplicationRoundSummary();row.setControlledFileId(fileId);row.setApplicationType("OBSOLETE");row.setBpmRound(bpm);row.setAttributeRound(number);var bound=reworkBoundContracts.resolveRoundFlowContract(file,row);
            if(bound.status()!=DccBoundWorkflowContractResolver.RoundContractStatus.READY||!Objects.equals(bound.formInstanceId(),formId)||!Objects.equals(bound.formInstanceCode(),code)||!Objects.equals(bound.processDefinitionId(),history.getProcessDefinitionId()))throw invalid("REWORK_RECORDED_GRAPH_REQUIRED");}
        else throw invalid("REWORK_PREPARATION_VERSION_INVALID");
        proof.verifyRecorded(file,form,preparation,history.getProcessDefinitionId(),requiredText(preparation,"preparedDefinitionDigest"));
        var relations=reworkRelations.readRoundEvidence(actor,fileId,"OBSOLETE",bpm);
        if(relations==null||!Objects.equals(relations.tenantId(),tenant)||!Objects.equals(relations.fileId(),fileId)||!Objects.equals(relations.masterId(),file.getMasterId())||!Objects.equals(relations.projectId(),file.getDccProjectCodeId())||!"OBSOLETE".equals(relations.applicationType())||!Integer.valueOf(number).equals(relations.attributeRound())||!Objects.equals(relations.bpmRound(),bpm)||relations.relationDigest()==null||!relations.relationDigest().matches("[a-f0-9]{64}"))throw invalid("REWORK_RELATION_SOURCE_INVALID");
        if(relations.stages().stream().anyMatch(stage->!stage.signatureIdentityMatches()||!"VALID".equals(stage.signatureEvidenceStatus())))throw invalid("REWORK_RELATION_SIGNATURE_UNAVAILABLE");
        String raw=requiredText(preparation,"sourceApprovalMatrixJson");var baseline=DccApprovalMatrixSourceService.readCanonical(raw);
        if(!Objects.equals(baseline.getTenantId(),tenant)||!Objects.equals(baseline.getCategoryId(),file.getCategoryId())||!Objects.equals(baseline.getFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))throw invalid("REWORK_MATRIX_FAMILY_INVALID");
        String digest=sha256(canonical(Arrays.asList(tenant,actor,fileId,file.getVersionNo(),formId,code,bpm,number,form.getStatus(),data,attrs.getDefaultSourceJson(),attrs.getActualAttributesJson(),relations)));
        return new ReworkSource(formId,code,bpm,number,form.getStatus(),file.getVersionNo(),fileId,file.getDccProjectCodeId(),requiredText(data,"reason"),selectedDepartments(data),
                projectAttributes.readValue(attrs.getDefaultSourceJson()),projectAttributes.readValue(attrs.getActualAttributesJson()),attrs.getDefaultSourceJson(),attrs.getActualAttributesJson(),raw,requiredText(preparation,"runtimeApprovalNodesJson"),history.getProcessDefinitionId(),requiredText(preparation,"preparedDefinitionDigest"),requiredText(preparation,"executionContract"),Collections.unmodifiableMap(preparation),relations,digest);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRoundSource inheritReworkRelations(Long actor,ReworkSource source,int targetRound){
        return reworkRelations.inheritRoundSelections(actor,source.fileId(),"OBSOLETE",source.bpmProcessInstanceId(),source.fileId(),"OBSOLETE",targetRound,source.relations().relationDigest());
    }
    public Map<String,Object> reworkSourceIdentity(ReworkSource source){return Map.of("formInstanceId",source.formInstanceId(),"formInstanceCode",source.formInstanceCode(),"bpmProcessInstanceId",source.bpmProcessInstanceId(),"attributeRound",source.attributeRound(),"sourceDigest",source.sourceDigest(),"status",source.status(),"versionNo",source.versionNo(),"relationDigest",source.relations().relationDigest());}
    public ReworkSource requireSavedReworkSource(Long actor,Long fileId,Map<String,Object> data){
        return requireSavedReworkSource(actor,fileId,data,true);
    }
    public ReworkSource requireRecordedReworkSource(Long actor,Long fileId,Map<String,Object> data){
        return requireSavedReworkSource(actor,fileId,data,false);
    }
    private ReworkSource requireSavedReworkSource(Long actor,Long fileId,Map<String,Object> data,boolean currentEligibility){
        if(!(data.get("dccObsoleteReworkSource") instanceof Map<?,?>))throw invalid("REWORK_SAVED_SOURCE_REQUIRED");var identity=(Map<String,Object>)data.get("dccObsoleteReworkSource");
        if(!(identity.get("attributeRound") instanceof Number number))throw invalid("REWORK_SAVED_SOURCE_REQUIRED");var result=currentEligibility?readReworkSource(actor,fileId,positiveId(String.valueOf(identity.get("formInstanceId"))),requiredText(identity,"formInstanceCode"),requiredText(identity,"bpmProcessInstanceId"),number.intValue()):readRecordedReworkSource(actor,fileId,positiveId(String.valueOf(identity.get("formInstanceId"))),requiredText(identity,"formInstanceCode"),requiredText(identity,"bpmProcessInstanceId"),number.intValue());
        if(!sameBusinessPayload(reworkSourceIdentity(result),identity)||!Objects.equals(result.sourceApprovalMatrixJson(),data.get("dccObsoleteReworkSourceApprovalMatrixJson"))||!Objects.equals(result.runtimeApprovalNodesJson(),data.get("dccObsoleteReworkSourceRuntimeApprovalNodesJson"))
                ||!Objects.equals(result.preparedProcessDefinitionId(),data.get("dccObsoleteReworkSourceProcessDefinitionId"))||!Objects.equals(result.preparedDefinitionDigest(),data.get("dccObsoleteReworkSourceDefinitionDigest"))||!Objects.equals(result.executionContract(),data.get("dccObsoleteReworkSourceExecutionContract")))throw invalid("REWORK_SAVED_SOURCE_CHANGED");return result;
    }
    private void requireReworkFile(Long actor,DccControlledFileDO file,boolean currentEligibility){
        if(file.getDccProjectCodeId()==null||file.getMasterId()==null||file.getControlledTime()==null||currentEligibility&&!Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(file.getStatus()))throw invalid("REWORK_CONTROLLED_SOURCE_REQUIRED");
        var project=projects.selectById(file.getDccProjectCodeId());var master=masters.selectById(file.getMasterId());
        if(project==null||master==null||!Objects.equals(project.getTenantId(),file.getTenantId())||!Objects.equals(master.getTenantId(),file.getTenantId())||Boolean.TRUE.equals(project.getDeleted())||Boolean.TRUE.equals(master.getDeleted())||currentEligibility&&(!"ENABLE".equals(project.getStatus())||!Objects.equals(master.getLatestControlledFileId(),file.getId()))
                ||!Objects.equals(master.getDccProjectCodeId(),file.getDccProjectCodeId())||!Objects.equals(master.getCategoryId(),file.getCategoryId())||!Objects.equals(master.getFileTypeTaxonomyLeafId(),file.getFileTypeTaxonomyId())||!Objects.equals(master.getNormalizedFileNumber(),file.getFileNumber()))throw invalid("REWORK_SOURCE_FAMILY_INVALID");
        projectAccess.assertProjectEditorOrOwner(actor,file.getDccProjectCodeId());if(!categoryPermissions.hasCategoryPermission(file.getCategoryId(),actor,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.OBSOLETE))throw invalid("OBSOLETE_PERMISSION_DENIED");
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void onPendingApprovalStarted(FormActionInstance instance){
        if(!TransactionSynchronizationManager.isActualTransactionActive()||instance==null||instance.getBpmBinding()==null||instance.getStatus()!=cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus.IN_APPROVAL)throw invalid("SUBMITTED_CALLBACK_CONTEXT_REQUIRED");
        var data=instance.getFormData();if(data==null||!(data.get(PREPARATION) instanceof Map<?,?> raw)||!"DCC_OBSOLETE_FORM_PREPARATION_V2".equals(raw.get("formatVersion")))throw invalid("RESERVED_ATTRIBUTE_PROOF_REQUIRED");
        Long formId=positiveId(String.valueOf(raw.get("formInstanceId")));Long actor=instance.getApplicantUserId();String bpm=instance.getBpmBinding().getProcessInstanceId();
        var form=forms.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),formId);
        if(form==null||!Objects.equals(form.getInstanceCode(),instance.getInstanceCode())||!Objects.equals(form.getApplicantUserId(),actor)||!Objects.equals(form.getBpmProcessInstanceId(),bpm)||!"IN_APPROVAL".equals(form.getStatus())
                ||!sameBusinessPayload(parsed(form.getFormDataJson()),data))throw invalid("ACTUAL_SUBMITTED_FORM_REQUIRED");
        verifySavedAuthorization(formId,form.getInstanceCode(),actor,data);Long fileId=positiveId(instance.getBusinessContext().getObjectId());
        if(!(raw.get("attributeRound") instanceof Number n)||n.intValue()<=0)throw invalid("EXACT_ATTRIBUTE_ROUND_REQUIRED");int number=n.intValue();
        rounds.bindObsoleteFormDraft(actor,fileId,formId,form.getInstanceCode(),number,bpm);var file=files.selectById(fileId);
        var attrs=projectAttributes.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);requireNumericAttributes(attrs,file,number,true);
        if(!Objects.equals(attrs.getDefaultSourceJson(),raw.get("defaultSourceJson"))||!Objects.equals(attrs.getActualAttributesJson(),raw.get("actualAttributesJson"))
                ||!Objects.equals(sha256(canonical(Arrays.asList(number,attrs.getDefaultSourceJson(),attrs.getActualAttributesJson()))),raw.get("attributeDigest")))throw invalid("RESERVED_ATTRIBUTE_PROOF_MISMATCH");
        if(!Boolean.TRUE.equals(attrs.getSubmitted()))projectAttributes.freeze(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);
        var runtime=runtimeStore.appendVerifiedSubmitted(form,file,number,bpm,(Map<String,Object>)raw);freezeDepartmentAssignments(runtime);
    }
    private void freezeDepartmentAssignments(DccObsoleteFormRuntimeStore.RuntimeEvidence runtime){
        var head=runtime.head();var members=runtime.obligations().stream().filter(r->"MATRIX_REVIEW".equals(r.stageCode())).toList();
        var existing=taskSnapshots.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO>()
                .eq("tenant_id",head.tenantId()).eq("controlled_file_id",head.controlledFileId()).eq("process_instance_id",head.bpmProcessInstanceId()).eq("stage_code","MATRIX_REVIEW").eq("deleted",false));
        if(!existing.isEmpty()){
            if(existing.size()!=members.size())throw invalid("COMPLETE_DEPARTMENT_ASSIGNMENTS_REQUIRED");
            for(var member:members){var matching=existing.stream().filter(r->Objects.equals(r.getObligationId(),member.obligationId())).toList();var leaders=JsonUtils.parseArray(member.frozenUserIdsJson(),Long.class);
                if(matching.size()!=1||leaders.size()!=1||!Objects.equals(matching.get(0).getDepartmentId(),member.departmentId())||!Objects.equals(matching.get(0).getLeaderUserId(),leaders.get(0)))throw invalid("DEPARTMENT_ASSIGNMENT_SOURCE_CHANGED");}return;
        }
        for(var member:members){var ids=JsonUtils.parseArray(member.frozenUserIdsJson(),Long.class);if(ids.size()!=1)throw invalid("SINGLE_DEPARTMENT_LEADER_REQUIRED");var user=users.getUser(ids.get(0));
            if(user==null||!Objects.equals(user.getId(),ids.get(0))||!Objects.equals(user.getTenantId(),head.tenantId())||!Integer.valueOf(0).equals(user.getStatus()))throw invalid("FROZEN_LEADER_ACCOUNT_INVALID");
            var row=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO.builder().tenantId(head.tenantId()).controlledFileId(head.controlledFileId()).stageCode(member.stageCode()).stageNo(member.stageNo())
                    .departmentId(member.departmentId()).assigneeUserId(ids.get(0)).leaderUserId(ids.get(0)).assigneeName(user.getNickname()).leaderConfigDigest(head.preparationDigest()).processInstanceId(head.bpmProcessInstanceId()).obligationId(member.obligationId()).build();
            if(taskSnapshots.insert(row)!=1)throw invalid("DEPARTMENT_ASSIGNMENT_INSERT_FAILED");
        }
    }
    public DccObsoleteFormRuntimeStore.RuntimeEvidence requireSubmittedRuntime(Long tenant,Long file,String bpm){return runtimeStore.requireByBpm(tenant,file,bpm);}
    public FormExactBpmSubmissionAdapter.VerifiedSubmission verifyReservedExactSubmission(FormExactBpmSubmissionAdapter.PersistedDraft persisted,FormActionInstance requested,Long actor){
        var saved=parsed(persisted.formDataJson());if(!(saved.get(PREPARATION) instanceof Map<?,?> p)||!"DCC_OBSOLETE_FORM_PREPARATION_V2".equals(p.get("formatVersion")))throw invalid("RESERVED_ATTRIBUTE_PROOF_REQUIRED");
        return verifyExactSubmission(persisted,requested,actor);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccApprovalMatrixBaselineService.Comparison previewReservedForDraft(FormExactBpmSubmissionAdapter.PersistedDraft persisted,FormActionInstance requested,Long actor,String contextKey,String beforeToken){
        Long id=positiveId(requested.getBusinessContext().getObjectId());int number=rounds.requireObsoleteFormDraft(actor,id,persisted.formInstanceId(),persisted.formInstanceCode());
        var file=requireContext(persisted,requested,actor);var attrs=projectAttributes.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",id,number);requireNumericAttributes(attrs,file,number,false);
        if(!contextKey(persisted).equals(contextKey))throw invalid("MATRIX_CONTEXT_REQUIRED");
        var draftData=parsed(persisted.formDataJson());if(draftData.containsKey("dccObsoleteReworkSource")){
            var original=requireSavedReworkSource(actor,id,draftData);requireReworkAttributeProvenance(attrs,original);var inherited=sourceFromSaved(original.sourceApprovalMatrixJson());
            var context=new DccApprovalMatrixTokenService.Context(file.getTenantId(),actor,file.getDccProjectCodeId(),file.getCategoryId(),file.getFileTypeTaxonomyId(),"OBSOLETE",file.getId(),contextKey);
            if(beforeToken!=null&&!Objects.equals(tokens.verifyBaseline(context,beforeToken),original.sourceApprovalMatrixJson()))throw invalid("REWORK_MATRIX_MUST_RETAIN_SOURCE");
            return new DccApprovalMatrixBaselineService.Comparison(context,original.sourceApprovalMatrixJson(),inherited.baseline(),inherited,tokens.issueBaseline(context,original.sourceApprovalMatrixJson()),null,false);
        }var current=sources.readForUpdate(file.getCategoryId());
        var context=new DccApprovalMatrixTokenService.Context(file.getTenantId(),actor,file.getDccProjectCodeId(),file.getCategoryId(),file.getFileTypeTaxonomyId(),"OBSOLETE",file.getId(),contextKey);
        var stored=parsed(persisted.formDataJson());String before=current.canonical();
        if(stored.get(PREPARATION) instanceof Map<?,?> payload){verifySavedAuthorization(persisted.formInstanceId(),persisted.formInstanceCode(),actor,stored);before=requiredText(payload,"sourceApprovalMatrixJson");}
        else if(beforeToken!=null)before=tokens.verifyBaseline(context,beforeToken);
        var original=DccApprovalMatrixSourceService.readCanonical(before);if(!Objects.equals(original.getTenantId(),file.getTenantId())||!Objects.equals(original.getCategoryId(),file.getCategoryId())||!Objects.equals(original.getFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))throw invalid("MATRIX_FAMILY_INVALID");
        boolean changed=!Objects.equals(original.getMatrixDigest(),current.baseline().getMatrixDigest());return new DccApprovalMatrixBaselineService.Comparison(context,before,original,current,tokens.issueBaseline(context,before),changed?tokens.issueConfirmation(context,before,current.baseline().getRouteId(),current.baseline().getRouteVersionNo(),current.baseline().getMatrixDigest()):null,changed);
    }
    public String preparationUnavailableReason(){return supervisors.getIfAvailable()==null?"DCC_OBSOLETE_SUPERVISOR_SOURCE_NOT_READY":null;}
    public String preparationUnavailableReason(DccControlledFileDO file,Long actor){
        String missing=preparationUnavailableReason();if(missing!=null)return missing;
        try{requireSupervisor(file,actor);return null;}
        catch(DccUserManagementDirectSupervisorSource.RelationUnavailableException unavailable){return unavailable.getMessage();}
    }
    public boolean sameBusinessPayload(Map<String,Object> left,Map<String,Object> right){
        return canonical(JsonUtils.parseObject(JsonUtils.toJsonString(left),Map.class)).equals(canonical(JsonUtils.parseObject(JsonUtils.toJsonString(right),Map.class)));
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public String prepareReservedForDraft(FormExactBpmSubmissionAdapter.PersistedDraft persisted,FormActionInstance requested,Long actor,DccApprovalMatrixBaselineService.Expectations expected){
        Long fileId=positiveId(requested.getBusinessContext().getObjectId());
        int number=rounds.requireObsoleteFormDraft(actor,fileId,persisted.formInstanceId(),persisted.formInstanceCode());
        var file=requireContext(persisted,requested,actor);var attrs=projectAttributes.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",fileId,number);requireNumericAttributes(attrs,file,number,false);
        if(requested.getFormData().get("projectAttributes")==null||!Objects.equals(projectAttributes.encode(JsonUtils.parseObject(JsonUtils.toJsonString(requested.getFormData().get("projectAttributes")),cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes.class)),attrs.getActualAttributesJson()))throw invalid("SAVED_ACTUAL_ATTRIBUTES_MISMATCH");
        String prepared=parsed(persisted.formDataJson()).containsKey("dccObsoleteReworkSource")?prepareReworkForDraft(persisted,requested,actor,expected,file,attrs):prepareForDraft(persisted,requested,actor,expected);var data=parsed(prepared);var payload=new TreeMap<String,Object>((Map<String,Object>)data.get(PREPARATION));payload.remove("authorizationToken");
        payload.put("formatVersion","DCC_OBSOLETE_FORM_PREPARATION_V2");payload.put("attributeState","RESERVED_FORM_DRAFT");payload.put("attributeRound",number);
        payload.put("defaultSourceJson",attrs.getDefaultSourceJson());payload.put("actualAttributesJson",attrs.getActualAttributesJson());payload.put("attributeDigest",sha256(canonical(Arrays.asList(number,attrs.getDefaultSourceJson(),attrs.getActualAttributesJson()))));
        payload.put("authorizationToken",sign(payload));data.put(PREPARATION,payload);return canonical(data);
    }
    private String prepareReworkForDraft(FormExactBpmSubmissionAdapter.PersistedDraft persisted,FormActionInstance requested,Long actor,DccApprovalMatrixBaselineService.Expectations expected,DccControlledFileDO file,cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO attrs){
        var data=normalizedFormData(requested.getFormData(),file);var stored=parsed(persisted.formDataJson());if(stored.containsKey(PREPARATION)||data.containsKey(PREPARATION)||!sameBusinessPayload(stored,data))throw invalid("PERSISTED_REQUEST_MISMATCH");
        var original=requireSavedReworkSource(actor,file.getId(),stored);requireReworkAttributeProvenance(attrs,original);var raw=sourceFromSaved(original.sourceApprovalMatrixJson());
        String key=contextKey(persisted);var context=new DccApprovalMatrixTokenService.Context(file.getTenantId(),actor,file.getDccProjectCodeId(),file.getCategoryId(),file.getFileTypeTaxonomyId(),"OBSOLETE",file.getId(),key);
        if(expected==null||!Objects.equals(expected.applicationContextKey(),key)||!Objects.equals(expected.routeId(),raw.baseline().getRouteId())||!Objects.equals(expected.routeVersionNo(),raw.baseline().getRouteVersionNo())||!Objects.equals(expected.digest(),raw.baseline().getMatrixDigest())
                ||!Objects.equals(tokens.verifyBaseline(context,expected.baselineToken()),original.sourceApprovalMatrixJson())||Boolean.TRUE.equals(expected.changeConfirmed())||expected.confirmationToken()!=null)throw invalid("REWORK_MATRIX_MUST_RETAIN_SOURCE");
        var prepared=executionContracts.requireOrdinaryPublic(original.executionContract(),original.preparedProcessDefinitionId(),original.preparedDefinitionDigest(),DccControlledFileProcessDefinitionKeys.OBSOLETE);
        var supervisor=requireSupervisor(file,actor);if(!Objects.equals(canonical(supervisorMap(supervisor)),canonical(original.preparation().get("supervisorProvenance"))))throw invalid("SUPERVISOR_SOURCE_CHANGED");
        var frozen=restoreServerCandidates(raw,selectedDepartments(data),supervisor,original.runtimeApprovalNodesJson());
        var payload=new TreeMap<String,Object>();payload.put("formatVersion",PURPOSE);payload.put("tenantId",file.getTenantId().toString());payload.put("formInstanceId",persisted.formInstanceId().toString());payload.put("formInstanceCode",persisted.formInstanceCode());payload.put("controlledFileId",file.getId().toString());payload.put("controlledFileVersionNo",file.getVersionNo());payload.put("applicantUserId",actor.toString());payload.put("projectId",file.getDccProjectCodeId().toString());payload.put("categoryId",file.getCategoryId().toString());payload.put("taxonomyId",file.getFileTypeTaxonomyId().toString());
        payload.put("executionContract",prepared.executionContract());payload.put("preparedProcessDefinitionId",prepared.processDefinitionId());payload.put("preparedDefinitionDigest",prepared.definitionDigest());payload.put("sourceApprovalMatrixJson",original.sourceApprovalMatrixJson());payload.put("sourceApprovalMatrixDigest",raw.baseline().getMatrixDigest());payload.put("originalMatrixBeforeJson",original.sourceApprovalMatrixJson());payload.put("runtimeApprovalNodesJson",runtimeJson(frozen));payload.put("supervisorProvenance",supervisorMap(supervisor));payload.put("attributeState","UNBOUND_FORM_DRAFT");payload.put("reworkSource",reworkSourceIdentity(original));payload.put("businessPayloadDigest",sha256(canonical(data)));payload.put("authorizationToken",sign(payload));data.put(PREPARATION,payload);return canonical(data);
    }
    private DccApprovalMatrixSourceService.Source sourceFromSaved(String raw){var baseline=DccApprovalMatrixSourceService.readCanonical(raw);var document=JsonUtils.parseObject(raw,Map.class);var nodes=JsonUtils.parseArray(JsonUtils.toJsonString(document.get("nodes")),cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.class);var route=cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).categoryId(baseline.getCategoryId()).actionType("LEGACY").versionNo(baseline.getRouteVersionNo()).build();return new DccApprovalMatrixSourceService.Source(route,nodes,baseline,raw);}
    private void requireReworkAttributeProvenance(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO attrs,ReworkSource source){
        if(!Objects.equals(attrs.getSourceApplicationId(),source.fileId())||!Integer.valueOf(source.attributeRound()).equals(attrs.getSourceApplicationRound())||!Objects.equals(attrs.getDefaultSourceJson(),source.defaultSourceJson()))throw invalid("REWORK_ATTRIBUTE_SOURCE_MISMATCH");
        var inherited=rounds.readRelationSource(source.projectId(),"OBSOLETE",source.fileId(),attrs.getApplicationRound());if(inherited==null||!"OBSOLETE".equals(inherited.sourceApplicationType())||!Objects.equals(inherited.sourceApplicationId(),source.fileId())||!Objects.equals(inherited.sourceBpmRound(),source.bpmProcessInstanceId())||!Objects.equals(inherited.sourceRelationDigest(),source.relations().relationDigest()))throw invalid("REWORK_RELATION_BINDING_REQUIRED");
    }
    public void verifySavedAuthorization(Long formId,String code,Long actor,Map<String,Object> data){
        if(!(data.get(PREPARATION) instanceof Map<?,?>))throw invalid("PREPARATION_REQUIRED");var payload=new TreeMap<String,Object>((Map<String,Object>)data.get(PREPARATION));String token=requiredText(payload,"authorizationToken");payload.remove("authorizationToken");verifyToken(payload,token);
        identity(payload,"formInstanceId",formId);identity(payload,"applicantUserId",actor);identity(payload,"tenantId",TenantContextHolder.getRequiredTenantId());if(!Objects.equals(code,payload.get("formInstanceCode")))throw invalid("PREPARATION_IDENTITY_INVALID");
        var business=new TreeMap<>(data);business.remove(PREPARATION);if(!Objects.equals(sha256(canonical(business)),payload.get("businessPayloadDigest")))throw invalid("BUSINESS_PAYLOAD_CHANGED");
    }
    public void verifyReservedSavedAuthorization(FormExactBpmSubmissionAdapter.PersistedDraft persisted,DccControlledFileDO file,Long actor,Map<String,Object> data){
        verifySavedAuthorization(persisted.formInstanceId(),persisted.formInstanceCode(),actor,data);var payload=(Map<String,Object>)data.get(PREPARATION);
        if(!"DCC_OBSOLETE_FORM_PREPARATION_V2".equals(payload.get("formatVersion")))throw invalid("RESERVED_ATTRIBUTE_PROOF_REQUIRED");
        int number=rounds.requireObsoleteFormDraft(actor,file.getId(),persisted.formInstanceId(),persisted.formInstanceCode());var attrs=projectAttributes.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",file.getId(),number);requireNumericAttributes(attrs,file,number,false);
        if(!(payload.get("attributeRound") instanceof Number stored)||stored.intValue()!=number||!"RESERVED_FORM_DRAFT".equals(payload.get("attributeState"))
                ||!Objects.equals(attrs.getDefaultSourceJson(),payload.get("defaultSourceJson"))||!Objects.equals(attrs.getActualAttributesJson(),payload.get("actualAttributesJson")))throw invalid("RESERVED_ATTRIBUTE_PROOF_MISMATCH");
    }
    private void requireNumericAttributes(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO attrs,DccControlledFileDO file,int number,boolean bound){
        if(attrs==null||!Objects.equals(attrs.getTenantId(),file.getTenantId())||!Objects.equals(attrs.getProjectCodeId(),file.getDccProjectCodeId())||!Objects.equals(attrs.getApplicationId(),file.getId())||!"OBSOLETE".equals(attrs.getApplicationType())||!Integer.valueOf(number).equals(attrs.getApplicationRound())||!bound&&Boolean.TRUE.equals(attrs.getSubmitted()))throw invalid("RESERVED_ATTRIBUTE_IDENTITY_INVALID");
        projectAttributes.readValue(attrs.getDefaultSourceJson());projectAttributes.readValue(attrs.getActualAttributesJson());
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public String prepareForDraft(FormExactBpmSubmissionAdapter.PersistedDraft persisted,FormActionInstance requested,Long actor,
            DccApprovalMatrixBaselineService.Expectations expected){
        var file=requireContext(persisted,requested,actor);var requestedData=normalizedFormData(requested.getFormData(),file);
        var stored=parsed(persisted.formDataJson());
        if(stored.containsKey(PREPARATION)||requestedData.containsKey(PREPARATION))throw invalid("PREPARATION_ALREADY_EXISTS");
        if(!canonical(stored).equals(canonical(requestedData)))throw invalid("PERSISTED_REQUEST_MISMATCH");
        var evidence=requireSupervisor(file,actor);
        if(expected==null||expected.routeId()==null||expected.routeVersionNo()==null||expected.digest()==null)throw invalid("MATRIX_EXPECTATION_REQUIRED");
        var source=sources.readForUpdate(file.getCategoryId());String contextKey=contextKey(persisted);
        if(!contextKey.equals(expected.applicationContextKey()))throw invalid("MATRIX_CONTEXT_REQUIRED");
        var context=new DccApprovalMatrixTokenService.Context(file.getTenantId(),actor,file.getDccProjectCodeId(),file.getCategoryId(),file.getFileTypeTaxonomyId(),"OBSOLETE",file.getId(),contextKey);
        String before=tokens.verifyBaseline(context,expected.baselineToken());var original=DccApprovalMatrixSourceService.readCanonical(before);
        if(!Objects.equals(original.getTenantId(),file.getTenantId())||!Objects.equals(original.getCategoryId(),file.getCategoryId())||!Objects.equals(original.getFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))throw invalid("MATRIX_FAMILY_INVALID");
        if(!Objects.equals(expected.routeId(),source.baseline().getRouteId())||!Objects.equals(expected.routeVersionNo(),source.baseline().getRouteVersionNo())||!Objects.equals(expected.digest(),source.baseline().getMatrixDigest()))
            throw new DccApprovalMatrixChangeException("APPROVAL_MATRIX_EXPECTATION_STALE",original,source.baseline(),tokens.issueBaseline(context,before),tokens.issueConfirmation(context,before,source.baseline().getRouteId(),source.baseline().getRouteVersionNo(),source.baseline().getMatrixDigest()));
        if(!Objects.equals(original.getMatrixDigest(),source.baseline().getMatrixDigest())){
            if(!Boolean.TRUE.equals(expected.changeConfirmed()))throw new DccApprovalMatrixChangeException("APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED",original,source.baseline(),tokens.issueBaseline(context,before),tokens.issueConfirmation(context,before,source.baseline().getRouteId(),source.baseline().getRouteVersionNo(),source.baseline().getMatrixDigest()));
            tokens.requireConfirmation(context,expected.confirmationToken(),before,expected.routeId(),expected.routeVersionNo(),expected.digest());
        }
        var prepared=executionContracts.preparePublicPreview(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        var resolved=resolveSource(source,actor,selectedDepartments(requestedData),evidence);
        var payload=new TreeMap<String,Object>();payload.put("formatVersion",PURPOSE);payload.put("tenantId",file.getTenantId().toString());payload.put("formInstanceId",persisted.formInstanceId().toString());payload.put("formInstanceCode",persisted.formInstanceCode());
        payload.put("controlledFileId",file.getId().toString());payload.put("controlledFileVersionNo",file.getVersionNo());payload.put("applicantUserId",actor.toString());payload.put("projectId",file.getDccProjectCodeId().toString());
        payload.put("categoryId",file.getCategoryId().toString());payload.put("taxonomyId",file.getFileTypeTaxonomyId().toString());payload.put("executionContract",prepared.executionContract());payload.put("preparedProcessDefinitionId",prepared.processDefinitionId());payload.put("preparedDefinitionDigest",prepared.definitionDigest());
        payload.put("sourceApprovalMatrixJson",source.canonical());payload.put("sourceApprovalMatrixDigest",source.baseline().getMatrixDigest());payload.put("originalMatrixBeforeJson",before);
        payload.put("runtimeApprovalNodesJson",runtimeJson(resolved));payload.put("supervisorProvenance",supervisorMap(evidence));payload.put("attributeState","UNBOUND_FORM_DRAFT");
        payload.put("businessPayloadDigest",sha256(canonical(requestedData)));payload.put("authorizationToken",sign(payload));requestedData.put(PREPARATION,payload);
        return canonical(requestedData);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public FormExactBpmSubmissionAdapter.VerifiedSubmission verifyExactSubmission(FormExactBpmSubmissionAdapter.PersistedDraft persisted,FormActionInstance requested,Long actor){
        var initialData=parsed(persisted.formDataJson());var savedPreparation=initialData.get(PREPARATION);
        if(savedPreparation instanceof Map<?,?> p&&"DCC_OBSOLETE_FORM_PREPARATION_V2".equals(p.get("formatVersion")))
            rounds.requireObsoleteFormDraft(actor,positiveId(requested.getBusinessContext().getObjectId()),persisted.formInstanceId(),persisted.formInstanceCode());
        var file=requireContext(persisted,requested,actor);var stored=parsed(persisted.formDataJson());var data=normalizedFormData(requested.getFormData(),file);
        ReworkSource rework=null;if(stored.containsKey("dccObsoleteReworkSource")){
            rework=requireSavedReworkSource(actor,file.getId(),stored);int number=rounds.requireObsoleteFormDraft(actor,file.getId(),persisted.formInstanceId(),persisted.formInstanceCode());var attributes=projectAttributes.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",file.getId(),number);requireReworkAttributeProvenance(attributes,rework);
        }
        if(!canonical(stored).equals(canonical(data)))throw invalid("PERSISTED_REQUEST_MISMATCH");
        if(!(stored.get(PREPARATION) instanceof Map<?,?>))throw invalid("PREPARATION_REQUIRED");
        var payload=new TreeMap<String,Object>((Map<String,Object>)stored.get(PREPARATION));String token=requiredText(payload,"authorizationToken");payload.remove("authorizationToken");verifyToken(payload,token);
        identity(payload,"tenantId",file.getTenantId());identity(payload,"formInstanceId",persisted.formInstanceId());identity(payload,"applicantUserId",actor);identity(payload,"controlledFileId",file.getId());identity(payload,"projectId",file.getDccProjectCodeId());identity(payload,"categoryId",file.getCategoryId());identity(payload,"taxonomyId",file.getFileTypeTaxonomyId());
        if(!Objects.equals(persisted.formInstanceCode(),payload.get("formInstanceCode"))||!Objects.equals(file.getVersionNo(),payload.get("controlledFileVersionNo")))throw invalid("PREPARATION_IDENTITY_INVALID");
        if("DCC_OBSOLETE_FORM_PREPARATION_V2".equals(payload.get("formatVersion"))){
            int number=rounds.requireObsoleteFormDraft(actor,file.getId(),persisted.formInstanceId(),persisted.formInstanceCode());var attrs=projectAttributes.readSaved(actor,file.getDccProjectCodeId(),"OBSOLETE",file.getId(),number);requireNumericAttributes(attrs,file,number,false);
            if(!(payload.get("attributeRound") instanceof Number storedRound)||storedRound.intValue()!=number||!"RESERVED_FORM_DRAFT".equals(payload.get("attributeState"))
                    ||!Objects.equals(attrs.getDefaultSourceJson(),payload.get("defaultSourceJson"))||!Objects.equals(attrs.getActualAttributesJson(),payload.get("actualAttributesJson"))
                    ||!Objects.equals(sha256(canonical(Arrays.asList(number,attrs.getDefaultSourceJson(),attrs.getActualAttributesJson()))),payload.get("attributeDigest")))throw invalid("RESERVED_ATTRIBUTE_PROOF_MISMATCH");
        }else if(!PURPOSE.equals(payload.get("formatVersion"))||!"UNBOUND_FORM_DRAFT".equals(payload.get("attributeState")))throw invalid("PREPARATION_IDENTITY_INVALID");
        var business=new TreeMap<>(data);business.remove(PREPARATION);if(!Objects.equals(sha256(canonical(business)),payload.get("businessPayloadDigest")))throw invalid("BUSINESS_PAYLOAD_CHANGED");
        if(rework!=null&&(!sameBusinessPayload(reworkSourceIdentity(rework),(Map<String,Object>)payload.get("reworkSource"))||!Objects.equals(rework.sourceApprovalMatrixJson(),payload.get("sourceApprovalMatrixJson"))||!Objects.equals(rework.preparedProcessDefinitionId(),payload.get("preparedProcessDefinitionId"))||!Objects.equals(rework.preparedDefinitionDigest(),payload.get("preparedDefinitionDigest"))))throw invalid("REWORK_SAVED_SOURCE_CHANGED");
        var prepared=executionContracts.requireOrdinaryPublic(requiredText(payload,"executionContract"),requiredText(payload,"preparedProcessDefinitionId"),requiredText(payload,"preparedDefinitionDigest"),DccControlledFileProcessDefinitionKeys.OBSOLETE);
        var supervisor=requireSupervisor(file,actor);if(!canonical(supervisorMap(supervisor)).equals(canonical(payload.get("supervisorProvenance"))))throw invalid("SUPERVISOR_SOURCE_CHANGED");
        String raw=requiredText(payload,"sourceApprovalMatrixJson");var baseline=DccApprovalMatrixSourceService.readCanonical(raw);
        if(!Objects.equals(baseline.getMatrixDigest(),payload.get("sourceApprovalMatrixDigest"))||!Objects.equals(baseline.getTenantId(),file.getTenantId())||!Objects.equals(baseline.getCategoryId(),file.getCategoryId())||!Objects.equals(baseline.getFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))throw invalid("RAW_MATRIX_PROVENANCE_INVALID");
        var rawMap=JsonUtils.parseObject(raw,Map.class);var rawNodes=JsonUtils.parseArray(JsonUtils.toJsonString(rawMap.get("nodes")),cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.class);
        var root=cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).categoryId(baseline.getCategoryId()).actionType("LEGACY").versionNo(baseline.getRouteVersionNo()).build();
        var resolved=restoreServerCandidates(new DccApprovalMatrixSourceService.Source(root,rawNodes,baseline,raw),selectedDepartments(data),supervisor,requiredText(payload,"runtimeApprovalNodesJson"));
        return new FormExactBpmSubmissionAdapter.VerifiedSubmission(DccControlledFileProcessDefinitionKeys.OBSOLETE,prepared.processDefinitionId(),canonical(data),serverStart(resolved),serverApprove(resolved));
    }
    private DccControlledFileDO requireContext(FormExactBpmSubmissionAdapter.PersistedDraft saved,FormActionInstance req,Long actor){
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw invalid("OUTER_TRANSACTION_REQUIRED");
        Long tenant=TenantContextHolder.getRequiredTenantId();var authenticated=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(authenticated==null||!Objects.equals(authenticated.getId(),actor)||!Objects.equals(authenticated.getTenantId(),tenant)||!Integer.valueOf(2).equals(authenticated.getUserType()))throw invalid("AUTHENTICATED_ACTOR_MISMATCH");
        if(saved==null||req==null||actor==null||actor<=0||!Objects.equals(saved.tenantId(),tenant)||!Objects.equals(saved.applicantUserId(),actor)
                ||saved.formInstanceId()==null||saved.formInstanceId()<=0||saved.formInstanceCode()==null||saved.formInstanceCode().isBlank()||!"DRAFT".equals(saved.status())||saved.processInstanceId()!=null
                ||!Objects.equals(saved.formInstanceCode(),req.getInstanceCode())||!Objects.equals(actor,req.getApplicantUserId())||req.getStatus()!=cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus.DRAFT||req.getBpmBinding()!=null)
            throw invalid("FORM_DRAFT_IDENTITY_INVALID");
        var ctx=req.getBusinessContext();if(ctx==null||!Objects.equals(ctx.getTenantId(),tenant)||!"DCC".equals(ctx.getDataDomain())||!"DCC".equals(ctx.getSystemCode())||!"CONTROLLED_FILE".equals(ctx.getObjectType())||!"OBSOLETE".equals(ctx.getActionCode())
                ||req.getResolution()==null||!DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(req.getResolution().getBpmProcessKey())||!DccControlledFileObsoleteFormEffectExecutor.EXECUTOR_CODE.equals(req.getResolution().getEffectExecutorCode()))throw invalid("FORM_ACTION_CONTEXT_INVALID");
        Long id=positiveId(ctx.getObjectId());var routing=files.selectById(id);if(routing==null||!Objects.equals(routing.getTenantId(),tenant)||routing.getDccProjectCodeId()==null||routing.getMasterId()==null)throw invalid("FILE_IDENTITY_INVALID");
        var project=projects.selectByIdForUpdate(routing.getDccProjectCodeId());var master=masters.selectByIdForUpdate(routing.getMasterId());var file=files.selectByIdAndTenantForUpdate(tenant,id);
        if(project==null||!Objects.equals(project.getTenantId(),tenant)||!"ENABLE".equals(project.getStatus())||master==null||!Objects.equals(master.getTenantId(),tenant)||file==null||!Objects.equals(file.getId(),id)||!Objects.equals(file.getTenantId(),tenant)||Boolean.TRUE.equals(file.getDeleted())
                ||!Objects.equals(file.getMasterId(),master.getId())||!Objects.equals(file.getDccProjectCodeId(),project.getId())||!Objects.equals(master.getDccProjectCodeId(),file.getDccProjectCodeId())
                ||!Objects.equals(master.getCategoryId(),file.getCategoryId())||!Objects.equals(master.getFileTypeTaxonomyLeafId(),file.getFileTypeTaxonomyId())||!Objects.equals(master.getNormalizedFileNumber(),file.getFileNumber())
                ||Boolean.TRUE.equals(master.getDeleted())||Boolean.TRUE.equals(project.getDeleted())||file.getControlledTime()==null||!Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(file.getStatus())
                ||!Objects.equals(file.getVersionNo(),ctx.getObjectVersion())||!Objects.equals(file.getStatus(),ctx.getObjectState())||!Objects.equals(master.getLatestControlledFileId(),file.getId()))throw invalid("CONTROLLED_SOURCE_IDENTITY_INVALID");
        projectAccess.assertProjectEditorOrOwner(actor,file.getDccProjectCodeId());if(!categoryPermissions.hasCategoryPermission(file.getCategoryId(),actor,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.OBSOLETE))throw invalid("OBSOLETE_PERMISSION_DENIED");
        var applicant=users.getUser(actor);if(applicant==null||!Objects.equals(applicant.getId(),actor)||!Objects.equals(applicant.getTenantId(),tenant)||!Integer.valueOf(0).equals(applicant.getStatus()))throw invalid("APPLICANT_INVALID");return file;
    }
    private DccDirectSupervisorSource.Evidence requireSupervisor(DccControlledFileDO file,Long actor){
        var provider=supervisors.getIfAvailable();if(provider==null)throw new IllegalStateException("DCC_OBSOLETE_SUPERVISOR_SOURCE_NOT_READY");
        var evidence=provider.require(new DccDirectSupervisorSource.Context(file.getTenantId(),actor,file.getId(),file.getDccProjectCodeId(),"OBSOLETE"));
        if(evidence==null||!Objects.equals(evidence.tenantId(),file.getTenantId())||!Objects.equals(evidence.requesterId(),actor)||evidence.supervisorUserId()==null||evidence.supervisorUserId()<=0
                ||evidence.sourceSystem()==null||evidence.sourceSystem().isBlank()||evidence.sourceIdentity()==null||evidence.sourceIdentity().isBlank()||evidence.sourceVersion()==null||evidence.sourceVersion().isBlank())throw invalid("SUPERVISOR_SOURCE_INVALID");
        var user=users.getUser(evidence.supervisorUserId());if(user==null||!Objects.equals(user.getId(),evidence.supervisorUserId())||!Objects.equals(user.getTenantId(),file.getTenantId())||!Integer.valueOf(0).equals(user.getStatus()))throw invalid("SUPERVISOR_ACCOUNT_INVALID");return evidence;
    }
    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolveSource(DccApprovalMatrixSourceService.Source raw,Long actor,List<Long> departments,DccDirectSupervisorSource.Evidence supervisor){
        var route=routeResolver.resolvePublicMatrix(raw,actor,"OBSOLETE",departments);var nodes=new ArrayList<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode>();
        nodes.add(new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(0,"SUPERVISOR_REVIEW","直接主管审核",0,"USER",supervisor.supervisorUserId(),List.of(supervisor.supervisorUserId()),"ANY",null,false,List.of(supervisor.supervisorUserId())));nodes.addAll(route.nodes());
        if(!nodes.stream().map(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageCode).toList().equals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL")))throw invalid("OBSOLETE_STAGE_CONTRACT_INVALID");
        var resolved=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(route.route(),List.copyOf(nodes),route.sourceApprovalMatrix(),route.derivationVersion(),route.runtimeGroups());readiness.requireReadyResolvedRoute(resolved);return resolved;
    }
    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute restoreServerCandidates(DccApprovalMatrixSourceService.Source source,List<Long> departments,DccDirectSupervisorSource.Evidence supervisor,String runtime){
        var document=JsonUtils.parseObject(runtime,Map.class);
        if(document==null||!"DCC_RUNTIME_APPROVAL_NODES_V1".equals(document.get("formatVersion"))||!(document.get("nodes") instanceof List<?>)||!(document.get("groups") instanceof List<?>))throw invalid("FROZEN_RUNTIME_INVALID");
        var nodes=JsonUtils.parseArray(JsonUtils.toJsonString(document.get("nodes")),DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode.class);
        var groups=JsonUtils.parseArray(JsonUtils.toJsonString(document.get("groups")),cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.RuleGroup.class);
        var projection=cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.project(source.rawNodes(),"OBSOLETE",departments);
        if(nodes.size()!=3||!groups.equals(projection.groups())||!nodes.stream().map(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageCode).toList().equals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL")))throw invalid("FROZEN_GROUPS_INVALID");
        var first=nodes.get(0);if(!Integer.valueOf(0).equals(first.stageNo())||!Integer.valueOf(0).equals(first.stageOrder())||!"USER".equals(first.candidateSourceType())||!Objects.equals(first.candidateSourceId(),supervisor.supervisorUserId())
                ||!first.candidateSourceIds().equals(List.of(supervisor.supervisorUserId()))||!first.resolvedUserIds().equals(List.of(supervisor.supervisorUserId()))||!"ANY".equals(first.approveMethod())||Boolean.TRUE.equals(first.requireAllApprovals())||first.approveRatio()!=null)throw invalid("FROZEN_SUPERVISOR_INVALID");
        for(int i=0;i<projection.nodes().size();i++){var expected=projection.nodes().get(i);var saved=nodes.get(i+1);
            if(!Objects.equals(saved.stageNo(),expected.getStageNo())||!Objects.equals(saved.stageCode(),expected.getStageCode())||!Objects.equals(saved.stageOrder(),expected.getStageOrder())
                    ||!Objects.equals(saved.candidateSourceType(),expected.getCandidateSourceType())||!Objects.equals(saved.candidateSourceId(),expected.getCandidateSourceId())
                    ||!saved.candidateSourceIds().equals(Arrays.stream(expected.getCandidateSourceIds().split(",")).map(Long::valueOf).toList())
                    ||!Objects.equals(saved.approveMethod(),expected.getApproveMethod())||!Objects.equals(saved.approveRatio(),expected.getApproveRatio())||!Objects.equals(saved.requireAllApprovals(),expected.getRequireAllApprovals())
                    ||saved.resolvedUserIds()==null||saved.resolvedUserIds().isEmpty()||saved.resolvedUserIds().stream().anyMatch(id->id==null||id<=0)
                    ||"DEPT".equals(saved.candidateSourceType())&&saved.candidateSourceIds().size()!=saved.resolvedUserIds().size())throw invalid("FROZEN_MATRIX_CANDIDATES_INVALID");
        }
        var resolved=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(source.route(),nodes,source.baseline(),cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.VERSION,groups);
        // Freeze leaders selected at preparation. Recheck current enabled account/post/signature eligibility,
        // not current department leader or mutable matrix configuration.
        readiness.requireReadyResolvedRoute(resolved);return resolved;
    }
    private Map<String,List<Long>> serverStart(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route){var result=new TreeMap<String,List<Long>>();for(var node:route.nodes())if(Set.of("SUPERVISOR_REVIEW","MATRIX_REVIEW").contains(node.stageCode()))result.put(node.stageCode(),List.copyOf(node.resolvedUserIds()));return Collections.unmodifiableMap(result);}
    private Map<String,List<Long>> serverApprove(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route){var result=new TreeMap<String,List<Long>>();for(var node:route.nodes())if("MATRIX_APPROVAL".equals(node.stageCode()))result.put(node.stageCode(),List.copyOf(node.resolvedUserIds()));return Collections.unmodifiableMap(result);}
    private Map<String,Object> supervisorMap(DccDirectSupervisorSource.Evidence e){return Map.of("tenantId",e.tenantId().toString(),"requesterId",e.requesterId().toString(),"supervisorUserId",e.supervisorUserId().toString(),"sourceSystem",e.sourceSystem(),"sourceIdentity",e.sourceIdentity(),"sourceVersion",e.sourceVersion());}
    private String runtimeJson(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route){return canonical(Map.of("formatVersion","DCC_RUNTIME_APPROVAL_NODES_V1","nodes",route.nodes(),"groups",route.runtimeGroups()));}
    private Map<String,Object> normalizedFormData(Map<String,Object> original,DccControlledFileDO file){var data=new TreeMap<String,Object>(original);
        identity(data,"controlledFileId",file.getId());if(!Objects.equals(file.getFileNumber(),data.get("fileCode"))||!Objects.equals(file.getVersionNo(),data.get("fileVersion")))throw invalid("FORM_FILE_VERSION_INVALID");String reason=requiredText(data,"reason");if(reason.isBlank())throw invalid("REASON_REQUIRED");selectedDepartments(data);return data;
    }
    private List<Long> selectedDepartments(Map<String,Object> data){if(!(data.get("dccSignoffDepartmentIds") instanceof List<?> values)||values.isEmpty())throw invalid("EXPLICIT_SIGNOFF_DEPARTMENTS_REQUIRED");var result=new ArrayList<Long>();for(Object value:values){Long id=positiveId(value.toString());if(result.contains(id))throw invalid("DUPLICATE_SIGNOFF_DEPARTMENT");result.add(id);}return List.copyOf(result);}
    private void identity(Map<?,?> data,String field,Long id){if(!Objects.equals(String.valueOf(id),String.valueOf(data.get(field))))throw invalid("IDENTITY_"+field);}
    private Long positiveId(String text){if(text==null||!text.matches("[1-9][0-9]*"))throw invalid("EXACT_ID_REQUIRED");return Long.valueOf(text);}
    private String requiredText(Map<?,?> data,String key){if(!(data.get(key) instanceof String value)||value.isBlank())throw invalid("REQUIRED_"+key);return value;}
    private Map<String,Object> parsed(String text){var map=JsonUtils.parseObject(text,Map.class);if(map==null)throw invalid("FORM_JSON_INVALID");return new TreeMap<>(map);}
    private String contextKey(FormExactBpmSubmissionAdapter.PersistedDraft saved){return "DCC_OBSOLETE:"+saved.formInstanceId()+":"+saved.formInstanceCode();}
    private String canonical(Object object){Object raw=object instanceof Map<?,?>||object instanceof List<?>?object:JsonUtils.parseObject(JsonUtils.toJsonString(object),Object.class);return JsonUtils.toJsonString(sorted(raw));}
    private Object sorted(Object input){if(input instanceof Map<?,?> map){var result=new TreeMap<String,Object>();map.forEach((k,v)->result.put(k.toString(),sorted(v)));return result;}if(input instanceof List<?> list)return list.stream().map(this::sorted).toList();return input;}
    private String sha256(String text){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(text);}
    private String sign(Map<String,Object> payload){return proof.sign(payload);}
    private void verifyToken(Map<String,Object> payload,String token){proof.verify(payload,token);}
    private byte[] mac(String text){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signing.requireSigningKey(),"HmacSHA256"));return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));}catch(java.security.GeneralSecurityException failure){throw new IllegalStateException("DCC_OBSOLETE_PREPARATION_SIGNING_UNAVAILABLE",failure);}}
    private IllegalArgumentException invalid(String code){return Set.of("REWORK_RECORDED_PREPARATION_REQUIRED","REWORK_FROZEN_SOURCE_REQUIRED","REWORK_RELATION_SIGNATURE_UNAVAILABLE").contains(code)?new ReworkSourceUnavailableException("DCC_OBSOLETE_"+code):new IllegalArgumentException("DCC_OBSOLETE_"+code);}
}
