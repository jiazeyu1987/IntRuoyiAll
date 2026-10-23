package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Immutable, form/BPM-scoped source evidence. Never reads or writes a File's mutable route. */
@Service
public class DccObsoleteFormRuntimeStore {
    @Resource private DccObsoleteFormRuntimeSnapshotMapper snapshots;
    @Resource private DccObsoleteFormRuntimeObligationMapper obligations;
    @Resource private FormActionInstanceMapper forms;
    @Resource private DccControlledFileMapper files;
    @Resource private DccProjectApplicationAttributesMapper attributes;
    @Resource private DccApplicationRoundService rounds;
    @Resource private BpmProcessDefinitionService definitions;
    @Resource private BpmProcessInstanceService processes;
    @Resource private DccObsoleteFormPreparationProof proof;
    public record RuntimeEvidence(DccObsoleteFormRuntimeSnapshotDO head,List<DccObsoleteFormRuntimeObligationDO> obligations) {}
    public static final class RuntimeSourceNotRecordedException extends IllegalStateException {
        public RuntimeSourceNotRecordedException(){super("DCC_OBSOLETE_RUNTIME_SOURCE_NOT_RECORDED");}
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public RuntimeEvidence appendVerifiedSubmitted(FormActionInstanceDO input,DccControlledFileDO file,int number,String bpm,Map<String,Object> preparation){
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw invalid("OUTER_TRANSACTION_REQUIRED");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(input==null||input.getId()==null)throw invalid("FORM_REQUIRED");
        var form=forms.selectByIdAndTenantForUpdate(tenant,input.getId());
        var actor=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(form==null||actor==null||!Objects.equals(actor.getId(),form.getApplicantUserId())||!Objects.equals(actor.getTenantId(),tenant)||!Integer.valueOf(2).equals(actor.getUserType())
                ||!Objects.equals(form.getInstanceCode(),input.getInstanceCode())||!"IN_APPROVAL".equals(form.getStatus()))throw invalid("SUBMITTED_FORM_ACTOR_INVALID");
        requireSubmitted(form,file,number,bpm,preparation);
        var existing=snapshots.listByForm(tenant,form.getId());
        if(!existing.isEmpty()){
            if(existing.size()!=1)throw invalid("UNIQUE_FORM_HEAD_REQUIRED");var evidence=requireByBpm(tenant,file.getId(),bpm);
            if(!existing.equals(List.of(evidence.head()))||!Objects.equals(evidence.head().canonicalPreparationJson(),proof.canonical(preparation)))throw invalid("REPLAY_CHANGED");return evidence;
        }
        if(!snapshots.listByBpm(tenant,file.getId(),bpm).isEmpty())throw invalid("BPM_ALREADY_OWNED");
        String canonical=proof.canonical(preparation);var now=LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        var row=new DccObsoleteFormRuntimeSnapshotDO(null,tenant,form.getId(),file.getId(),file.getDccProjectCodeId(),form.getApplicantUserId(),form.getInstanceCode(),file.getVersionNo(),bpm,number,
                text(preparation,"preparedProcessDefinitionId"),text(preparation,"preparedDefinitionDigest"),text(preparation,"executionContract"),canonical,sha(canonical),now);
        long headId=snapshots.insert(row);if(headId<=0)throw invalid("HEAD_ID_REQUIRED");
        var head=new DccObsoleteFormRuntimeSnapshotDO(headId,row.tenantId(),row.formInstanceId(),row.controlledFileId(),row.projectId(),row.applicantUserId(),row.formInstanceCode(),row.controlledFileVersionNo(),row.bpmProcessInstanceId(),row.attributeRound(),row.preparedProcessDefinitionId(),row.preparedDefinitionDigest(),row.executionContract(),row.canonicalPreparationJson(),row.preparationDigest(),row.createdAt());
        for(var member:expectedObligations(head,preparation))if(obligations.insert(member)<=0)throw invalid("OBLIGATION_ID_REQUIRED");
        return requireByBpm(tenant,file.getId(),bpm);
    }
    @Transactional(readOnly=true)
    public RuntimeEvidence requireByBpm(Long tenant,Long fileId,String bpm){
        if(!Objects.equals(TenantContextHolder.getRequiredTenantId(),tenant)||fileId==null||fileId<=0||bpm==null||bpm.isBlank())throw invalid("READ_IDENTITY_INVALID");
        var rows=snapshots.listByBpm(tenant,fileId,bpm);if(rows.isEmpty())throw new RuntimeSourceNotRecordedException();if(rows.size()!=1)throw invalid("UNIQUE_BPM_HEAD_REQUIRED");var head=rows.get(0);
        if(head.id()==null||head.id()<=0||head.formInstanceId()==null||head.attributeRound()==null||head.attributeRound()<=0||!Objects.equals(head.tenantId(),tenant)||!Objects.equals(head.controlledFileId(),fileId)||!Objects.equals(head.bpmProcessInstanceId(),bpm)||!Objects.equals(sha(head.canonicalPreparationJson()),head.preparationDigest()))throw invalid("HEAD_IDENTITY_INVALID");
        var form=forms.selectById(head.formInstanceId());var file=files.selectById(fileId);var preparation=JsonUtils.parseObject(head.canonicalPreparationJson(),Map.class);
        requireSubmitted(form,file,head.attributeRound(),bpm,preparation);
        if(!Objects.equals(form.getInstanceCode(),head.formInstanceCode())||!Objects.equals(form.getApplicantUserId(),head.applicantUserId())||!Objects.equals(file.getDccProjectCodeId(),head.projectId())||!Objects.equals(file.getVersionNo(),head.controlledFileVersionNo())
                ||!Objects.equals(preparation.get("preparedProcessDefinitionId"),head.preparedProcessDefinitionId())||!Objects.equals(preparation.get("preparedDefinitionDigest"),head.preparedDefinitionDigest())||!Objects.equals(preparation.get("executionContract"),head.executionContract()))throw invalid("HEAD_FORM_PROOF_MISMATCH");
        var actual=obligations.listByHead(tenant,head.id());var byBpm=obligations.listByBpm(tenant,fileId,bpm);var expected=expectedObligations(head,preparation);
        if(!actual.equals(byBpm)||actual.size()!=expected.size()||actual.stream().anyMatch(r->r.id()==null||r.id()<=0)||!memberFacts(actual).equals(memberFacts(expected)))throw invalid("COMPLETE_OBLIGATIONS_REQUIRED");
        return new RuntimeEvidence(head,List.copyOf(actual));
    }
    private void requireSubmitted(FormActionInstanceDO form,DccControlledFileDO file,int number,String bpm,Map<String,Object> preparation){
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(form==null||file==null||preparation==null||!Objects.equals(form.getTenantId(),tenant)||!Objects.equals(file.getTenantId(),tenant)||Boolean.TRUE.equals(form.getDeleted())||Boolean.TRUE.equals(file.getDeleted())
                ||!Objects.equals(form.getBpmProcessInstanceId(),bpm)||"DRAFT".equals(form.getStatus())||!"DCC".equals(form.getSystemCode())||!"CONTROLLED_FILE".equals(form.getObjectType())||!"OBSOLETE".equals(form.getActionCode())
                ||!Objects.equals(file.getId().toString(),form.getObjectId())||!Objects.equals(file.getVersionNo(),form.getObjectVersion())||!"DCC_OBSOLETE_FORM_PREPARATION_V2".equals(preparation.get("formatVersion")))throw invalid("SUBMITTED_FORM_IDENTITY_INVALID");
        var stored=JsonUtils.parseObject(form.getFormDataJson(),Map.class);if(!Objects.equals(proof.canonical(stored.get("dccExecutionPreparation")),proof.canonical(preparation)))throw invalid("ACTUAL_FORM_PROOF_REQUIRED");
        if(rounds.require("OBSOLETE",file.getId(),bpm)!=number)throw invalid("EXACT_RESERVED_ROUND_REQUIRED");
        var attrs=attributes.find("OBSOLETE",file.getId(),number);
        if(attrs==null||!Objects.equals(attrs.getTenantId(),tenant)||!Objects.equals(attrs.getProjectCodeId(),file.getDccProjectCodeId())||!Objects.equals(attrs.getApplicationId(),file.getId())||!"OBSOLETE".equals(attrs.getApplicationType())||!Integer.valueOf(number).equals(attrs.getApplicationRound())||!Boolean.TRUE.equals(attrs.getSubmitted())
                ||!(preparation.get("attributeRound") instanceof Number n)||n.intValue()!=number||!Objects.equals(attrs.getDefaultSourceJson(),preparation.get("defaultSourceJson"))||!Objects.equals(attrs.getActualAttributesJson(),preparation.get("actualAttributesJson")))throw invalid("FROZEN_ATTRIBUTE_PROOF_REQUIRED");
        proof.verifyRecorded(file,form,preparation,text(preparation,"preparedProcessDefinitionId"),text(preparation,"preparedDefinitionDigest"));requireActualBpm(form,file,bpm,preparation);
    }
    private void requireActualBpm(FormActionInstanceDO form,DccControlledFileDO file,String bpm,Map<String,Object> preparation){
        var runtime=processes.getProcessInstance(bpm);String id,tenant,business,definitionId,key,actor;Map<String,Object> variables;
        if(runtime!=null){id=runtime.getId();tenant=runtime.getTenantId();business=runtime.getBusinessKey();definitionId=runtime.getProcessDefinitionId();key=runtime.getProcessDefinitionKey();actor=runtime.getStartUserId();variables=runtime.getProcessVariables();}
        else{var history=processes.getHistoricProcessInstance(bpm);if(history==null)throw invalid("ACTUAL_BPM_REQUIRED");id=history.getId();tenant=history.getTenantId();business=history.getBusinessKey();definitionId=history.getProcessDefinitionId();key=history.getProcessDefinitionKey();actor=history.getStartUserId();variables=history.getProcessVariables();}
        if(!Objects.equals(id,bpm)||!Objects.equals(tenant,file.getTenantId().toString())||!Objects.equals(business,"FORM_ACTION:"+form.getInstanceCode())||!Objects.equals(actor,form.getApplicantUserId().toString())||!DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(key)||!Objects.equals(definitionId,preparation.get("preparedProcessDefinitionId"))||variables==null
                ||!Objects.equals(String.valueOf(form.getId()),String.valueOf(variables.get("formInstanceId")))||!Objects.equals(file.getId().toString(),String.valueOf(variables.get("controlledFileId")))||!Objects.equals(file.getId().toString(),String.valueOf(variables.get("objectId")))
                ||!Objects.equals(file.getVersionNo(),variables.get("objectVersion"))||!Objects.equals(file.getTenantId().toString(),String.valueOf(variables.get("tenantId")))||!"DCC".equals(variables.get("systemCode"))||!"CONTROLLED_FILE".equals(variables.get("objectType"))||!"OBSOLETE".equals(variables.get("actionCode")))throw invalid("ACTUAL_BPM_IDENTITY_INVALID");
        var definition=definitions.getProcessDefinition(definitionId);var model=definitions.getProcessDefinitionBpmnModel(definitionId);
        if(definition==null||!Objects.equals(definitionId,definition.getId())||!Objects.equals(tenant,definition.getTenantId())||!Objects.equals(key,definition.getKey())||definition.getDeploymentId()==null||model==null||model.getProcesses().size()!=1||!Objects.equals(key,model.getMainProcess().getId())
                ||!Objects.equals(DccLifecycleEffectiveContractResolver.definitionDigest(model),preparation.get("preparedDefinitionDigest")))throw invalid("ACTUAL_EXACT_GRAPH_INVALID");
    }
    private List<DccObsoleteFormRuntimeObligationDO> expectedObligations(DccObsoleteFormRuntimeSnapshotDO head,Map<String,Object> preparation){
        var runtime=JsonUtils.parseObject(text(preparation,"runtimeApprovalNodesJson"),Map.class);var nodes=JsonUtils.parseArray(JsonUtils.toJsonString(runtime.get("nodes")),DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode.class);
        if(nodes==null||!nodes.stream().map(DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode::stageCode).toList().equals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL")))throw invalid("STAGE_CONTRACT_INVALID");
        var result=new ArrayList<DccObsoleteFormRuntimeObligationDO>();for(var node:nodes){
            if(node.candidateSourceIds()==null||node.candidateSourceIds().isEmpty()||node.resolvedUserIds()==null||node.resolvedUserIds().isEmpty()||node.resolvedUserIds().stream().anyMatch(id->id==null||id<=0))throw invalid("FROZEN_CANDIDATES_REQUIRED");
            if("MATRIX_REVIEW".equals(node.stageCode())){
                if(!"DEPT".equals(node.candidateSourceType())||node.candidateSourceIds().size()!=node.resolvedUserIds().size())throw invalid("DEPARTMENT_LEADER_MAPPING_REQUIRED");
                for(int i=0;i<node.candidateSourceIds().size();i++){Long dept=node.candidateSourceIds().get(i);if(dept==null||dept<=0)throw invalid("DEPARTMENT_REQUIRED");result.add(member(head,node,dept,List.of(dept),List.of(node.resolvedUserIds().get(i))));}
            }else result.add(member(head,node,null,node.candidateSourceIds(),node.resolvedUserIds()));
        }return List.copyOf(result);
    }
    private DccObsoleteFormRuntimeObligationDO member(DccObsoleteFormRuntimeSnapshotDO head,DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node,Long dept,List<Long> candidates,List<Long> users){
        return new DccObsoleteFormRuntimeObligationDO(null,head.tenantId(),head.id(),head.formInstanceId(),head.controlledFileId(),head.bpmProcessInstanceId(),"form-"+head.formInstanceId()+":"+node.stageCode()+(dept==null?"":":"+dept),node.stageCode(),node.stageCode(),node.stageNo(),node.stageOrder(),dept,node.candidateSourceType(),proof.canonical(candidates),proof.canonical(users),node.approveMethod(),node.approveRatio(),node.requireAllApprovals(),head.createdAt());
    }
    private List<String> memberFacts(List<DccObsoleteFormRuntimeObligationDO> rows){return rows.stream().map(r->proof.canonical(Arrays.asList(r.tenantId(),r.headId(),r.formInstanceId(),r.controlledFileId(),r.bpmProcessInstanceId(),r.obligationId(),r.stageCode(),r.nodeInstanceId(),r.stageNo(),r.stageOrder(),r.departmentId(),r.candidateSourceType(),r.candidateSourceIdsJson(),r.frozenUserIdsJson(),r.approveMethod(),r.approveRatio(),r.requireAllApprovals()))).sorted().toList();}
    private String text(Map<?,?> data,String field){if(!(data.get(field) instanceof String value)||value.isBlank())throw invalid("REQUIRED_"+field);return value;}
    private String sha(String value){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(value);}
    private IllegalStateException invalid(String reason){return new IllegalStateException("DCC_OBSOLETE_RUNTIME_"+reason);}
}
