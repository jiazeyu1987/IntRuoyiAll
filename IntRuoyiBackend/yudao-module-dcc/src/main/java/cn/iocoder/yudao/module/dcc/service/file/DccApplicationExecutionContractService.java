package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Pins a prepared exact definition; no field absence, deployment number or mutable latest lookup implies legacy. */
@Service
public class DccApplicationExecutionContractService {
    public static final String PUBLIC="PUBLIC_MATRIX_NATIVE_V1",PUBLIC_V2="PUBLIC_MATRIX_NATIVE_V2",LEGACY="LEGACY_V4_ACTION";
    public static boolean isPublicContract(String contract){return PUBLIC.equals(contract)||PUBLIC_V2.equals(contract);}
    @Resource private BpmProcessDefinitionService definitions;
    @Resource(name="dccControlledFileMapper") private DccControlledFileMapper files;
    @Resource private DccApplicationRoundService rounds;
    @Resource private DccLifecycleEffectiveContractResolver effectiveContractResolver;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper sourceAttributes;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;
    @Resource private DccControlledFileCategoryPermissionSupport categoryPermissions;
    @Resource private DccExecutionDefinitionTransitions transitions;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masters;
    @Resource private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService processes;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper routeSnapshots;
    @Resource private DccControlledFileApprovalRouteAssigneeResolver routeResolver;
    public record Prepared(String executionContract,String processDefinitionId,String definitionDigest,String processKey){}
    @Resource private DccBoundWorkflowContractResolver boundContracts;
    public DccBoundWorkflowContractResolver.BoundExecution resolveBoundExecution(DccControlledFileDO file){return boundContracts.resolve(file);}
    public record BaselinePreparationEligibility(boolean required,boolean allowed) { }
    /** Discovery never reads current matrix. Only real registered cross-action ancestry grants preparation. */
    public BaselinePreparationEligibility discoverNewActionBaselinePreparation(Long actor,DccControlledFileDO file){
        if(file==null||file.getDraftApprovalMatrixJson()!=null||!isPublicContract(file.getPreparedWorkflowContract())||!"REVISION".equals(file.getChangeType()))
            return new BaselinePreparationEligibility(false,false);
        var source=file.getPredecessorControlledFileId()==null?null:files.selectById(file.getPredecessorControlledFileId());
        if(source==null)throw new IllegalStateException("NEW_ACTION_BASELINE_SOURCE_REQUIRED");
        if(!"NEW".equals(source.getChangeType())&&!"WORKING".equals(source.getStatus())
                &&!("REVISION".equals(source.getChangeType())&&source.getControlledTime()!=null))return new BaselinePreparationEligibility(false,false);
        requireNewActionBaselinePreparation(actor,file.getId());
        return new BaselinePreparationEligibility(true,file.getCheckedOutBy()==null);
    }
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public Prepared requireNewActionBaselinePreparation(Long actor,Long targetFileId){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("NEW_ACTION_BASELINE_OUTER_TRANSACTION_REQUIRED");
        var target=files.selectById(targetFileId);requireDraft(actor,target);
        if(!isPublicContract(target.getPreparedWorkflowContract())||!"REVISION".equals(target.getChangeType())
                ||target.getDraftApprovalMatrixJson()!=null||target.getControlledTime()!=null||target.getSelectedIterationControlledFileId()!=null)
            throw new IllegalArgumentException("NEW_ACTION_BASELINE_PREPARATION_NOT_ALLOWED");
        var cursor=target;var visited=new HashSet<Long>();Integer expectedRound=null;
        while(true){
            requireSameFamily(target,cursor);requireDraft(actor,cursor);
            if(!visited.add(cursor.getId())||!"REVISION".equals(cursor.getChangeType())||cursor.getDraftApprovalMatrixJson()!=null)throw sourceInvalid();
            int round=rounds.requireDraft("REVISION",cursor.getId());if(expectedRound!=null&&round!=expectedRound)throw sourceInvalid();
            var saved=sourceAttributes.find("REVISION",cursor.getId(),round);
            if(saved==null||Boolean.TRUE.equals(saved.getSubmitted())||!Objects.equals(saved.getProjectCodeId(),target.getDccProjectCodeId())
                    ||!Objects.equals(saved.getTenantId(),target.getTenantId()))throw sourceInvalid();
            var predecessor=cursor.getPredecessorControlledFileId()==null?null:files.selectById(cursor.getPredecessorControlledFileId());
            requireSameFamily(target,predecessor);
            if(predecessor.getControlledTime()!=null
                    &&Set.of("NEW","REVISION").contains(predecessor.getChangeType())){
                if(saved.getSourceApplicationId()!=null||saved.getSourceApplicationRound()!=null||predecessor.getProcessInstanceId()==null)throw sourceInvalid();
                String sourceType=typeOf(predecessor);
                int sourceRound=rounds.require(sourceType,predecessor.getId(),predecessor.getProcessInstanceId());
                var registered=resolveInheritedPrepared(actor,cursor,predecessor.getId(),sourceRound,true);
                requireControlledTransition(cursor,predecessor,sourceType,"REVISION",false);
                if(!isPublicContract(registered.executionContract()))throw sourceInvalid();
                requirePrepared(cursor,registered.executionContract(),registered.processDefinitionId(),registered.definitionDigest(),registered.processKey());
                return requirePrepared(target,registered.executionContract(),registered.processDefinitionId(),registered.definitionDigest(),registered.processKey());
            }
            if(!"REVISION".equals(predecessor.getChangeType())||!Objects.equals(saved.getSourceApplicationId(),predecessor.getId())
                    ||saved.getSourceApplicationRound()==null)throw new IllegalArgumentException("NEW_ACTION_BASELINE_EXACT_CROSS_ACTION_ANCESTRY_REQUIRED");
            var inherited=resolveInheritedPrepared(actor,cursor,predecessor.getId(),saved.getSourceApplicationRound(),true);
            requirePrepared(cursor,inherited.executionContract(),inherited.processDefinitionId(),inherited.definitionDigest(),inherited.processKey());
            cursor=predecessor;expectedRound=saved.getSourceApplicationRound();
        }
    }
    public record DraftFacts(boolean sameApplicationType,boolean newActionBaselineRequired,String approvalMatrixCanonical,String runtimeApprovalNodesJson,
            String effectiveMode,java.time.LocalDate effectiveDate,String selectedSignoffDepartmentIdsJson,String distributionDepartmentIdsJson,Boolean needTraining,
            Long executedSourceApplicationId,Integer executedSourceAttributeRound,String executedSourceBpm) { }
    public record CustomDraftInheritance(String draftApprovalSourceMode,String draftApplicationCustomMatrixJson,String draftApplicationContextKey) {}
    /** Typed facts for the Query/Revision owner after the target's real reserved round is initialized. */
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public CustomDraftInheritance readInheritedCustomDraft(Long actor,Long targetId,Long sourceId,Integer sourceRound){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("CUSTOM_DRAFT_INHERIT_OUTER_TRANSACTION_REQUIRED");
        var target=files.selectById(targetId);requireDraft(actor,target);var source=files.selectById(sourceId);requireSameFamily(target,source);
        if(!"APPLICATION_CUSTOM".equals(source.getDraftApprovalSourceMode()))return null;
        var prepared=resolveInheritedPrepared(actor,target,sourceId,sourceRound,true);requirePrepared(target,prepared.executionContract(),prepared.processDefinitionId(),prepared.definitionDigest(),prepared.processKey());
        var values=new DccApplicationCustomMatrixService();
        var original=new DccApplicationCustomMatrixService.SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,source.getTenantId(),source.getDccProjectCodeId(),source.getCategoryId(),source.getFileTypeTaxonomyId(),source.getRequesterId(),sourceId,sourceRound,typeOf(source),source.getDraftApplicationContextKey(),source.getPreparedProcessDefinitionId(),source.getPreparedDefinitionDigest());
        if(source.getDraftApplicationCustomMatrixJson()==null)throw new IllegalArgumentException("CUSTOM_DRAFT_INHERITED_CONFIGURATION_MISSING");
        var actual=values.readCanonical(source.getDraftApplicationCustomMatrixJson(),cn.hutool.crypto.digest.DigestUtil.sha256Hex(source.getDraftApplicationCustomMatrixJson()),original);
        if(source.getProcessInstanceId()!=null)routeResolver.restoreCustomFrozenRoute(source,routeSnapshots.selectListByControlledFileId(sourceId));
        int targetRound=rounds.requireDraft(typeOf(target),targetId);
        var reserved=new DccApplicationCustomMatrixService.SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,target.getTenantId(),target.getDccProjectCodeId(),target.getCategoryId(),target.getFileTypeTaxonomyId(),target.getRequesterId(),targetId,targetRound,typeOf(target),source.getDraftApplicationContextKey(),target.getPreparedProcessDefinitionId(),target.getPreparedDefinitionDigest());
        return new CustomDraftInheritance("APPLICATION_CUSTOM",values.canonicalize(reserved,actual.applicationCustomMatrix(),actual.selectedSignoffDepartmentIds()).canonical(),source.getDraftApplicationContextKey());
    }
    /** Read exact saved facts for a newly created body; writes remain with its Query/Revision owner. */
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public DraftFacts readInheritedDraftFacts(Long actor,Long targetFileId,Long sourceFileId,Integer sourceAttributeRound){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("DRAFT_FACTS_OUTER_TRANSACTION_REQUIRED");
        var target=files.selectById(targetFileId);requireDraft(actor,target);
        var prepared=resolveInheritedPrepared(actor,target,sourceFileId,sourceAttributeRound,true);
        requirePrepared(target,prepared.executionContract(),prepared.processDefinitionId(),prepared.definitionDigest(),prepared.processKey());
        var source=files.selectById(sourceFileId);
        if("APPLICATION_CUSTOM".equals(source.getDraftApprovalSourceMode())){
            if(source.getDraftApplicationCustomMatrixJson()==null)throw new IllegalArgumentException("CUSTOM_DRAFT_INHERITED_CONFIGURATION_MISSING");
            if(source.getProcessInstanceId()!=null)routeResolver.restoreCustomFrozenRoute(source,routeSnapshots.selectListByControlledFileId(sourceFileId));
            return new DraftFacts(Objects.equals(typeOf(target),typeOf(source)),false,null,null,source.getEffectiveMode(),source.getEffectiveDate(),source.getSelectedSignoffDepartmentIdsJson(),source.getDistributionDepartmentIdsJson(),source.getNeedTraining(),source.getProcessInstanceId()==null?null:sourceFileId,source.getProcessInstanceId()==null?null:sourceAttributeRound,source.getProcessInstanceId());
        }
        if(!Objects.equals(typeOf(target),typeOf(source))||isNewControlledRevisionBody(target,source))
            return new DraftFacts(false,isPublicContract(prepared.executionContract()),null,null,null,null,null,null,null,null,null,null);
        String canonical,runtime=null,bpm=null;Long executedSource=null;Integer executedRound=null;
        boolean pendingNewActionBaseline=false;
        if("WORKING".equals(source.getStatus())&&source.getProcessInstanceId()==null){
            canonical=source.getDraftApprovalMatrixJson();
            if(isPublicContract(prepared.executionContract())&&canonical==null){
                // A body checked in from a controlled upload starts a new revision application.
                // Further checkins may retain its unchosen baseline only after proving that
                // exact registered ancestry; never invent a matrix or read today's defaults.
                requireNewActionBaselinePreparation(actor,sourceFileId);
                pendingNewActionBaseline=true;
            }
        }else if(isPublicContract(prepared.executionContract())){
            bpm=rounds.resolveBoundBpm(typeOf(source),source.getId(),sourceAttributeRound);
            var exact=new DccControlledFileDO();org.springframework.beans.BeanUtils.copyProperties(source,exact);exact.setProcessInstanceId(bpm);
            var rows=routeSnapshots.selectListByControlledFileId(sourceFileId);
            var restored=routeResolver.restoreFrozenRoute(exact,rows);
            if(restored.sourceApprovalMatrix()==null||rows.isEmpty())throw new IllegalStateException("DRAFT_FACTS_EXECUTED_PUBLIC_SNAPSHOT_REQUIRED");
            canonical=rows.get(0).getSourceApprovalMatrixJson();runtime=rows.get(0).getRuntimeApprovalNodesJson();executedSource=sourceFileId;executedRound=sourceAttributeRound;
        }else{canonical=source.getDraftApprovalMatrixJson();}
        if(canonical!=null){var baseline=DccApprovalMatrixSourceService.readCanonical(canonical);
            if(!Objects.equals(baseline.getTenantId(),target.getTenantId())||!Objects.equals(baseline.getCategoryId(),target.getCategoryId())
                    ||!Objects.equals(baseline.getFileTypeTaxonomyId(),target.getFileTypeTaxonomyId())||!"LEGACY".equals(baseline.getActionType()))
                throw new IllegalStateException("DRAFT_FACTS_BASELINE_FAMILY_INVALID");}
        return new DraftFacts(true,pendingNewActionBaseline,canonical,runtime,source.getEffectiveMode(),source.getEffectiveDate(),source.getSelectedSignoffDepartmentIdsJson(),
                source.getDistributionDepartmentIdsJson(),source.getNeedTraining(),executedSource,executedRound,bpm);
    }
    /** Caller holds Project -> Master -> source/target File locks in its creation transaction.
     * The explicit source attribute round is authoritative even before target attribute initialization. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Prepared inheritPrepared(Long actor,Long targetFileId,Long sourceFileId,Integer sourceAttributeRound){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("EXECUTION_CONTRACT_INHERIT_OUTER_TRANSACTION_REQUIRED");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var target=files.selectByIdAndTenantForUpdate(tenant,targetFileId);requireDraft(actor,target);
        if(target.getControlledTime()!=null||target.getCheckedOutBy()!=null)throw sourceInvalid();
        return persistPreparedTarget(target,resolveInheritedPrepared(actor,target,sourceFileId,sourceAttributeRound,false));
    }
    /** Creation/submission replay verifier: no CAS, INSERT, binding or historical repair. */
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public Prepared verifyInheritedPrepared(Long actor,Long existingTargetId,Long sourceFileId,Integer sourceAttributeRound){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("EXECUTION_CONTRACT_REPLAY_OUTER_TRANSACTION_REQUIRED");
        var target=files.selectById(existingTargetId);
        if(target==null||actor==null||!Objects.equals(target.getId(),existingTargetId)||!Objects.equals(target.getRequesterId(),actor)
                ||!Objects.equals(target.getTenantId(),TenantContextHolder.getRequiredTenantId())||Boolean.TRUE.equals(target.getDeleted()))throw sourceInvalid();
        var prepared=resolveInheritedPrepared(actor,target,sourceFileId,sourceAttributeRound,true);
        requirePrepared(target,prepared.executionContract(),prepared.processDefinitionId(),prepared.definitionDigest(),prepared.processKey());
        return prepared;
    }
    private Prepared resolveInheritedPrepared(Long actor,DccControlledFileDO target,Long sourceFileId,Integer sourceAttributeRound,boolean readOnly){
        Long tenant=TenantContextHolder.getRequiredTenantId();Long targetFileId=target.getId();
        if(target.getMasterId()==null||target.getDccProjectCodeId()==null||sourceFileId==null||sourceAttributeRound==null
                ||sourceAttributeRound<=0||Objects.equals(targetFileId,sourceFileId)
                ||!Objects.equals(target.getPredecessorControlledFileId(),sourceFileId))throw sourceInvalid();
        projectAccess.assertProjectEditorOrOwner(actor,target.getDccProjectCodeId());
        if(!categoryPermissions.hasCategoryPermission(target.getCategoryId(),actor,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.UPLOAD))
            throw new IllegalArgumentException("EXECUTION_CONTRACT_INHERIT_PERMISSION_DENIED");
        var source=readSource(tenant,sourceFileId,readOnly);requireSameFamily(target,source);
        if(target.getSelectedIterationControlledFileId()==null
                &&(source.getSourceSha256()==null||!Objects.equals(target.getPreviousSourceSha256(),source.getSourceSha256())))throw sourceInvalid();
        String targetType=typeOf(target),sourceType=typeOf(source);
        boolean changedType=!targetType.equals(sourceType);
        boolean freshApplication=changedType||isNewControlledRevisionBody(target,source);
        if(freshApplication)requireControlledTransition(target,source,sourceType,targetType,readOnly);
        else if(target.getSelectedIterationControlledFileId()!=null
                &&(!Objects.equals(target.getSelectedIterationControlledFileId(),sourceFileId)
                    ||!Objects.equals(target.getSelectedIterationVersionNo(),source.getVersionNo())
                    ||!Objects.equals(target.getSourceSha256(),source.getSourceSha256())))throw sourceInvalid();
        // A legitimate newly inserted candidate may have no target attribute row yet. Any existing
        // draft row must prove the exact source; an absent row never supplies source facts.
        var targetRows=sourceAttributes.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO::getTenantId,tenant)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO::getApplicationType,targetType)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO::getApplicationId,targetFileId));
        if(targetRows.size()>1)throw sourceInvalid();
        if(!targetRows.isEmpty()){
            var row=targetRows.get(0);
            if(!readOnly&&Boolean.TRUE.equals(row.getSubmitted())||!Objects.equals(row.getProjectCodeId(),target.getDccProjectCodeId())
                    ||freshApplication&&(row.getSourceApplicationId()!=null||row.getSourceApplicationRound()!=null)
                    ||!freshApplication&&(!Objects.equals(row.getSourceApplicationId(),sourceFileId)||!Objects.equals(row.getSourceApplicationRound(),sourceAttributeRound)))
                throw sourceInvalid();
            if(Boolean.TRUE.equals(row.getSubmitted())&&!Objects.equals(row.getApplicationRound(),rounds.require(targetType,targetFileId,target.getProcessInstanceId())))throw sourceInvalid();
        }
        var visited=new HashSet<String>();visited.add(targetFileId+":"+targetType);
        Prepared origin=resolveSource(actor,target,source,sourceAttributeRound,sourceType,visited,readOnly);
        Prepared prepared=changedType || freshApplication && !PUBLIC_V2.equals(origin.executionContract()) ? resolveTransition(origin,keyOf(target)):origin;
        if(!Objects.equals(prepared.processKey(),keyOf(target)))throw sourceInvalid();
        return prepared;
    }
    private DccControlledFileDO readSource(Long tenant,Long sourceId,boolean readOnly){return readOnly?files.selectById(sourceId):files.selectByIdAndTenantForUpdate(tenant,sourceId);}
    private Prepared resolveSource(Long actor,DccControlledFileDO target,DccControlledFileDO source,Integer attributeRound,String type,Set<String> visited,boolean readOnly){
        requireSameFamily(target,source);
        if(attributeRound==null||attributeRound<=0||!Objects.equals(type,typeOf(source))||!visited.add(source.getId()+":"+type))
            throw new IllegalStateException("EXECUTION_CONTRACT_SOURCE_CYCLE_OR_TYPE_INVALID");
        var attributes=sourceAttributes.find(type,source.getId(),attributeRound);
        if(attributes==null||!Objects.equals(attributes.getTenantId(),target.getTenantId())
                ||!Objects.equals(attributes.getProjectCodeId(),target.getDccProjectCodeId()))throw sourceInvalid();
        boolean anyPrepared=source.getPreparedWorkflowContract()!=null||source.getPreparedProcessDefinitionId()!=null||source.getPreparedDefinitionDigest()!=null;
        if("WORKING".equals(source.getStatus())&&source.getProcessInstanceId()==null){
            if(Boolean.TRUE.equals(attributes.getSubmitted())||rounds.requireDraft(type,source.getId())!=attributeRound)throw sourceInvalid();
            if(anyPrepared)return requirePrepared(source,source.getPreparedWorkflowContract(),source.getPreparedProcessDefinitionId(),source.getPreparedDefinitionDigest(),keyOf(source));
            Long ancestorId=attributes.getSourceApplicationId();Integer ancestorRound=attributes.getSourceApplicationRound();
            if(ancestorId==null||ancestorRound==null||!Objects.equals(source.getPredecessorControlledFileId(),ancestorId))
                throw new IllegalStateException("EXECUTION_CONTRACT_AUTHORITATIVE_SOURCE_REQUIRED");
            var ancestor=readSource(target.getTenantId(),ancestorId,readOnly);
            return resolveSource(actor,target,ancestor,ancestorRound,type,visited,readOnly);
        }
        boolean eligibleBoundStatus=Set.of("REJECTED","WITHDRAWN","PENDING_APPLICANT_REWORK","ACTIVE","CONTROLLED_PENDING_EFFECTIVE","SUPERSEDED").contains(source.getStatus());
        if(!Boolean.TRUE.equals(attributes.getSubmitted())||!(eligibleBoundStatus||readOnly&&"OBSOLETE".equals(source.getStatus())))throw sourceInvalid();
        String bpm=rounds.resolveBoundBpm(type,source.getId(),attributeRound);
        if(isPublicContract(source.getPreparedWorkflowContract()))return requirePublicBoundSource(source,bpm);
        var exactSource=new DccControlledFileDO();org.springframework.beans.BeanUtils.copyProperties(source,exactSource);exactSource.setProcessInstanceId(bpm);
        Prepared result=verifyLegacySource(effectiveContractResolver.resolve(exactSource));
        if(anyPrepared)requireSame(source,result);
        return result;
    }
    private Prepared requirePublicBoundSource(DccControlledFileDO source,String bpm){
        var prepared=requirePrepared(source,source.getPreparedWorkflowContract(),source.getPreparedProcessDefinitionId(),source.getPreparedDefinitionDigest(),keyOf(source));
        var runtime=processes.getProcessInstance(bpm);
        if(runtime!=null){requireBoundIdentity(source,bpm,prepared,runtime.getId(),runtime.getTenantId(),runtime.getBusinessKey(),
                runtime.getProcessDefinitionId(),runtime.getProcessDefinitionKey(),runtime.getProcessVariables());}
        else{var history=processes.getHistoricProcessInstance(bpm);
            if(history==null)throw new IllegalStateException("EXECUTION_CONTRACT_BOUND_SOURCE_PROCESS_REQUIRED");
            requireBoundIdentity(source,bpm,prepared,history.getId(),history.getTenantId(),history.getBusinessKey(),
                    history.getProcessDefinitionId(),history.getProcessDefinitionKey(),history.getProcessVariables());}
        return prepared;
    }
    private void requireBoundIdentity(DccControlledFileDO source,String bpm,Prepared prepared,String actualId,String tenant,String businessKey,
            String definitionId,String definitionKey,Map<String,Object> variables){
        if(!Objects.equals(bpm,actualId)||!Objects.equals(source.getTenantId().toString(),tenant)
                ||!Objects.equals(source.getId().toString(),businessKey)||!Objects.equals(prepared.processDefinitionId(),definitionId)
                ||!Objects.equals(prepared.processKey(),definitionKey)||!Objects.equals(source.getProcessDefinitionKey(),definitionKey)
                ||variables==null||!Objects.equals(source.getId().toString(),String.valueOf(variables.get("controlledFileId"))))
            throw new IllegalStateException("EXECUTION_CONTRACT_BOUND_SOURCE_IDENTITY_INVALID");
    }
    private void requireControlledTransition(DccControlledFileDO target,DccControlledFileDO source,String sourceType,String targetType,boolean readOnly){
        if(!Set.of("UPLOAD","REVISION").contains(sourceType)||!"REVISION".equals(targetType)||source.getControlledTime()==null
                ||!(Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(source.getStatus())
                    ||readOnly&&Set.of("SUPERSEDED","OBSOLETE").contains(source.getStatus()))
                ||!Objects.equals(target.getRevisionBaseActiveControlledFileId(),source.getId())||target.getSelectedIterationControlledFileId()!=null)
            throw new IllegalStateException("EXECUTION_CONTRACT_UNREGISTERED_ACTION_TRANSITION");
        var master=readOnly?masters.selectById(target.getMasterId()):masters.selectByIdForUpdate(target.getMasterId());
        if(master==null||!Objects.equals(master.getTenantId(),target.getTenantId())
                ||!Objects.equals(master.getDccProjectCodeId(),target.getDccProjectCodeId())
                ||!readOnly&&!Objects.equals(master.getLatestControlledFileId(),source.getId()))throw sourceInvalid();
    }
    /** A controlled version and its next editable body are different applications even for REVISION -> REVISION. */
    private boolean isNewControlledRevisionBody(DccControlledFileDO target,DccControlledFileDO source){
        return "REVISION".equals(target.getChangeType())&&source.getControlledTime()!=null
                &&target.getSelectedIterationControlledFileId()==null;
    }
    private Prepared resolveTransition(Prepared origin,String targetKey){
        var registry=transitions.getDefinitionTransitions();
        if(registry==null)throw new IllegalStateException("EXECUTION_CONTRACT_TRANSITION_REGISTRY_INVALID");
        var matches=registry.stream().filter(row->row!=null&&Objects.equals(row.sourceProcessDefinitionId(),origin.processDefinitionId())
                &&Objects.equals(row.sourceDefinitionDigest(),origin.definitionDigest())).toList();
        if(matches.size()!=1)throw new IllegalStateException("EXECUTION_CONTRACT_EXACT_ACTION_TRANSITION_REQUIRED");
        var mapping=matches.get(0);
        if(mapping.targetDefinitionDigest()==null||!mapping.targetDefinitionDigest().matches("[a-f0-9]{64}"))throw sourceInvalid();
        var registered=DccWorkflowDefinitionCatalog.find(targetKey,mapping.targetDefinitionDigest());
        if(registered==null)throw new IllegalStateException("EXECUTION_CONTRACT_REGISTERED_TARGET_GRAPH_REQUIRED");
        return verify(registered.executionContract(),mapping.targetProcessDefinitionId(),targetKey,mapping.targetDefinitionDigest());
    }
    private Prepared persistPreparedTarget(DccControlledFileDO target,Prepared prepared){
        if(target.getPreparedWorkflowContract()!=null||target.getPreparedProcessDefinitionId()!=null||target.getPreparedDefinitionDigest()!=null){requireSame(target,prepared);return prepared;}
        if(files.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,target.getId()).eq(DccControlledFileDO::getTenantId,target.getTenantId())
                .eq(DccControlledFileDO::getStatus,"WORKING").isNull(DccControlledFileDO::getProcessInstanceId)
                .isNull(DccControlledFileDO::getPreparedWorkflowContract).isNull(DccControlledFileDO::getPreparedProcessDefinitionId).isNull(DccControlledFileDO::getPreparedDefinitionDigest)
                .set(DccControlledFileDO::getPreparedWorkflowContract,prepared.executionContract())
                .set(DccControlledFileDO::getPreparedProcessDefinitionId,prepared.processDefinitionId()).set(DccControlledFileDO::getPreparedDefinitionDigest,prepared.definitionDigest()))!=1)
            throw new IllegalStateException("EXECUTION_CONTRACT_INHERIT_CAS_CONFLICT");
        target.setPreparedWorkflowContract(prepared.executionContract());target.setPreparedProcessDefinitionId(prepared.processDefinitionId());target.setPreparedDefinitionDigest(prepared.definitionDigest());return prepared;
    }
    private void requireSameFamily(DccControlledFileDO target,DccControlledFileDO source){
        if(source==null||source.getId()==null||!Objects.equals(source.getTenantId(),target.getTenantId())||Boolean.TRUE.equals(source.getDeleted())
                ||!Objects.equals(source.getMasterId(),target.getMasterId())||!Objects.equals(source.getDccProjectCodeId(),target.getDccProjectCodeId())
                ||!Objects.equals(source.getCategoryId(),target.getCategoryId())||!Objects.equals(source.getFileTypeTaxonomyId(),target.getFileTypeTaxonomyId())
                ||!Objects.equals(source.getFileNumber(),target.getFileNumber()))throw sourceInvalid();
    }
    private String typeOf(DccControlledFileDO file){if(file!=null&&"NEW".equals(file.getChangeType()))return "UPLOAD";
        if(file!=null&&"REVISION".equals(file.getChangeType()))return "REVISION";throw sourceInvalid();}
    private String keyOf(DccControlledFileDO file){return "UPLOAD".equals(typeOf(file))?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION;}
    private IllegalArgumentException sourceInvalid(){return new IllegalArgumentException("EXECUTION_CONTRACT_SOURCE_IDENTITY_INVALID");}
    public Prepared preparePublicPreview(String key){
        var selected=definitions.getActiveProcessDefinition(key);
        if(selected==null)throw new IllegalStateException("EXECUTION_CONTRACT_NEW_DEFINITION_NOT_DEPLOYED");
        return verify(DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(key)?PUBLIC:PUBLIC_V2,selected.getId(),key,null);
    }
    public Prepared verifyLegacySource(DccLifecycleEffectiveContract source){
        if(source==null||source.rule()!=DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE
                ||!Objects.equals(source.tenantId(),TenantContextHolder.getRequiredTenantId()))
            throw new IllegalStateException("EXECUTION_CONTRACT_LEGACY_SOURCE_INVALID");
        return verify(LEGACY,source.processDefinitionId(),source.processKey(),source.definitionDigest());
    }
    /** Called only from the controlled internal legacy preparation entry, never ordinary new HTTP based on its request string. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Prepared prepareLegacy(Long actor,DccControlledFileDO file,String exactDefinitionId){
        requireDraft(actor,file);String key="NEW".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.UPLOAD:
                "REVISION".equals(file.getChangeType())?DccControlledFileProcessDefinitionKeys.REVISION:null;
        var prepared=verify(LEGACY,exactDefinitionId,key,null);
        if(file.getPreparedWorkflowContract()!=null){requireSame(file,prepared);return prepared;}
        if(files.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,file.getId()).eq(DccControlledFileDO::getTenantId,TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileDO::getStatus,"WORKING").isNull(DccControlledFileDO::getProcessInstanceId)
                .isNull(DccControlledFileDO::getPreparedWorkflowContract).isNull(DccControlledFileDO::getPreparedProcessDefinitionId)
                .isNull(DccControlledFileDO::getPreparedDefinitionDigest).set(DccControlledFileDO::getPreparedWorkflowContract,prepared.executionContract())
                .set(DccControlledFileDO::getPreparedProcessDefinitionId,prepared.processDefinitionId()).set(DccControlledFileDO::getPreparedDefinitionDigest,prepared.definitionDigest()))!=1)
            throw new IllegalStateException("EXECUTION_CONTRACT_PREPARE_CAS_CONFLICT");
        file.setPreparedWorkflowContract(prepared.executionContract());file.setPreparedProcessDefinitionId(prepared.processDefinitionId());
        file.setPreparedDefinitionDigest(prepared.definitionDigest());return prepared;
    }
    public Prepared requirePrepared(DccControlledFileDO file,String expectedContract,String expectedId,String expectedDigest,String key){
        if(file==null||file.getPreparedWorkflowContract()==null||file.getPreparedProcessDefinitionId()==null||file.getPreparedDefinitionDigest()==null)
            throw new IllegalStateException("EXECUTION_CONTRACT_PREPARED_IDENTITY_REQUIRED");
        if(!Objects.equals(expectedContract,file.getPreparedWorkflowContract())||!Objects.equals(expectedId,file.getPreparedProcessDefinitionId())
                ||!Objects.equals(expectedDigest,file.getPreparedDefinitionDigest()))throw new IllegalArgumentException("EXECUTION_CONTRACT_EXPECTATION_STALE");
        var result=verify(expectedContract,expectedId,key,expectedDigest);requireSame(file,result);return result;
    }
    public Prepared requireOrdinaryPublic(String contract,String id,String digest,String key){
        if(!isPublicContract(contract))throw new IllegalArgumentException("EXECUTION_CONTRACT_ORDINARY_REQUIRES_PUBLIC");
        if(digest==null||!digest.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("EXECUTION_CONTRACT_EXPECTED_DIGEST_REQUIRED");
        return verify(contract,id,key,digest);
    }
    private Prepared verify(String contract,String id,String key,String expectedDigest){
        if(id==null||id.isBlank()||key==null)throw new IllegalArgumentException("EXECUTION_CONTRACT_EXACT_DEFINITION_REQUIRED");
        var definition=definitions.getProcessDefinition(id);
        if(definition==null||!id.equals(definition.getId())||!TenantContextHolder.getRequiredTenantId().toString().equals(definition.getTenantId())
                ||!key.equals(definition.getKey())||definition.getDeploymentId()==null)throw new IllegalStateException("EXECUTION_CONTRACT_DEFINITION_IDENTITY_INVALID");
        var model=definitions.getProcessDefinitionBpmnModel(id);
        if(model==null||model.getProcesses().size()!=1||model.getMainProcess()==null||!key.equals(model.getMainProcess().getId()))throw new IllegalStateException("EXECUTION_CONTRACT_FULL_MODEL_INVALID");
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(model);
        DccWorkflowDefinitionCatalog.require(contract,key,digest);
        if(expectedDigest!=null&&!expectedDigest.equals(digest))throw new IllegalStateException("EXECUTION_CONTRACT_UNREGISTERED_DEFINITION");
        return new Prepared(contract,id,digest,key);
    }
    private void requireDraft(Long actor,DccControlledFileDO file){if(file==null||actor==null||!Objects.equals(file.getRequesterId(),actor)
            ||!Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())||!"WORKING".equals(file.getStatus())||file.getProcessInstanceId()!=null)
        throw new IllegalArgumentException("EXECUTION_CONTRACT_DRAFT_IDENTITY_INVALID");}
    private void requireSame(DccControlledFileDO file,Prepared prepared){if(!Objects.equals(file.getPreparedWorkflowContract(),prepared.executionContract())
            ||!Objects.equals(file.getPreparedProcessDefinitionId(),prepared.processDefinitionId())||!Objects.equals(file.getPreparedDefinitionDigest(),prepared.definitionDigest()))
        throw new IllegalStateException("EXECUTION_CONTRACT_PREPARED_IDENTITY_CONFLICT");}
}
