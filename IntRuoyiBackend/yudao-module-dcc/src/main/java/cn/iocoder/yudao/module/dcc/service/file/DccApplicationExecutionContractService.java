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
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeMapper typeChanges;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFilePlacementMapper typeChangePlacements;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper typeChangeMappings;
    public static final String TYPE_CHANGE_SOURCE="APPLICANT_TYPE_CHANGE",TYPE_CHANGE_FORMAT="DCC_NEW_REJECTED_TYPE_CHANGE_V1";
    public static final String STORAGE_RECOVERY_SOURCE="APPLICANT_TYPE_STORAGE_RECOVERY",STORAGE_RECOVERY_FORMAT="DCC_NEW_REJECTED_TYPE_CHANGE_STORAGE_RECOVERY_V1";
    public record StorageRecoveryEnvelope(String formatVersion,Long originalProofId,String originalProofSha256,TypeChangeState observed){}
    public record TypeChangeState(String formatVersion,String requestId,Long tenantId,Long actorId,Long masterId,Long controlledFileId,
            Long projectId,String fileNumber,String sourceOriginalFileName,String changeType,Long directoryId,Long projectFolderId,Integer attributeRound,
            Long sourceApplicationId,Integer sourceAttributeRound,Long rejectedApplicationId,Integer rejectedAttributeRound,String rejectedBpm,
            Long categoryId,Long taxonomyId,Long sourceFileId,String sourceSha256,String previousSourceSha256,String approvalMatrixCanonical,
            String preparedWorkflowContract,String preparedProcessDefinitionId,String preparedDefinitionDigest,
            String selectedSignoffDepartmentIdsJson,String distributionDepartmentIdsJson){
        private List<Object> immutableSource(){return Arrays.asList(formatVersion,requestId,tenantId,actorId,masterId,controlledFileId,projectId,
                fileNumber,sourceOriginalFileName,changeType,projectFolderId,attributeRound,sourceApplicationId,sourceAttributeRound,
                rejectedApplicationId,rejectedAttributeRound,rejectedBpm,sourceFileId,sourceSha256,previousSourceSha256,
                preparedWorkflowContract,preparedProcessDefinitionId,preparedDefinitionDigest);}
        public boolean sameSourceAs(TypeChangeState other){return other!=null&&immutableSource().equals(other.immutableSource());}
    }
    public TypeChangeState typeChangeState(Long actor,DccControlledFileDO file,int round,DraftFacts origin,String requestId){
        var saved=sourceAttributes.find("UPLOAD",file.getId(),round);
        if(saved==null||origin==null)throw typeChangeInvalid();
        var location=typeChangePlacements.findFile(file.getId());
        return new TypeChangeState(TYPE_CHANGE_FORMAT,requestId,file.getTenantId(),actor,file.getMasterId(),file.getId(),file.getDccProjectCodeId(),
                file.getFileNumber(),file.getSourceOriginalFileName(),file.getChangeType(),file.getDirectoryId(),location==null?null:location.getProjectFolderId(),round,
                saved.getSourceApplicationId(),saved.getSourceApplicationRound(),origin.executedSourceApplicationId(),origin.executedSourceAttributeRound(),origin.executedSourceBpm(),
                file.getCategoryId(),file.getFileTypeTaxonomyId(),file.getSourceFileId(),file.getSourceSha256(),file.getPreviousSourceSha256(),file.getDraftApprovalMatrixJson(),
                file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),
                file.getSelectedSignoffDepartmentIdsJson(),file.getDistributionDepartmentIdsJson());
    }
    private IllegalArgumentException typeChangeInvalid(){return new IllegalArgumentException("EXECUTION_CONTRACT_TYPE_CHANGE_PROOF_INVALID");}
    private boolean sameType(DccControlledFileDO a,DccControlledFileDO b){return Objects.equals(a.getCategoryId(),b.getCategoryId())&&Objects.equals(a.getFileTypeTaxonomyId(),b.getFileTypeTaxonomyId());}
    /** An explicit saved target is distinct from a matrix executed in the original rejected BPM. */
    public String readConfirmedTypeChangeBaseline(Long actor,DccControlledFileDO target){
        requireDraft(actor,target);if(!isPublicContract(target.getPreparedWorkflowContract()))return null;
        var cursor=target;var seen=new HashSet<Long>();
        while(seen.add(cursor.getId())){
            requireDraft(actor,cursor);requireSameFamily(target,cursor);
            int round=rounds.requireDraft(typeOf(cursor),cursor.getId());
            var saved=sourceAttributes.find(typeOf(cursor),cursor.getId(),round);
            if(saved==null)throw sourceInvalid();if(saved.getSourceApplicationId()==null)return null;
            var source=files.selectById(saved.getSourceApplicationId());if(source==null)throw sourceInvalid();
            if(!sameType(cursor,source)){
                var proof=readTypeChangeBoundary(actor,cursor,round);if(proof==null)return null;
                if(!Objects.equals(target.getDraftApprovalMatrixJson(),cursor.getDraftApprovalMatrixJson()))throw typeChangeInvalid();
                return cursor.getDraftApprovalMatrixJson();
            }
            readInheritedDraftFactsFromVerifiedTarget(actor,cursor,source.getId(),saved.getSourceApplicationRound());
            if(!"WORKING".equals(source.getStatus())||source.getProcessInstanceId()!=null)return null;
            cursor=source;
        }
        throw sourceInvalid();
    }
    private TypeChangeState readTypeChangeBoundary(Long actor,DccControlledFileDO file,int round){
        var rows=typeChanges.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO::getTenantId,file.getTenantId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO::getControlledFileId,file.getId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO::getSource,TYPE_CHANGE_SOURCE));
        if(rows.isEmpty())return null;
        if(rows.size()!=1)throw typeChangeInvalid();var row=rows.get(0);
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getBeforeSnapshotJson(),TypeChangeState.class);
        var after=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getAfterSnapshotJson(),TypeChangeState.class);
        var recoveryRows=typeChanges.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO::getTenantId,file.getTenantId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO::getControlledFileId,file.getId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO::getSource,STORAGE_RECOVERY_SOURCE));
        if(recoveryRows.size()>1)throw typeChangeInvalid();
        var recovery=recoveryRows.isEmpty()?null:recoveryRows.get(0);
        StorageRecoveryEnvelope recovered=null;
        if(recovery!=null){
            recovered=readStorageRecoveryEnvelope(row,recovery,actor,file);
            after=recovered.observed();
        }
        if(before==null||after==null||!TYPE_CHANGE_FORMAT.equals(after.formatVersion())||row.getRequestId()==null||row.getRequestId().isBlank()
                ||!Objects.equals(row.getRequestId(),(recovered==null?after:cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getAfterSnapshotJson(),TypeChangeState.class)).requestId())
                ||!before.immutableSource().equals(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(row.getAfterSnapshotJson(),TypeChangeState.class).immutableSource())
                ||!Objects.equals(actor,row.getOperatorUserId())||!Objects.equals(row.getMasterId(),file.getMasterId())
                ||!Objects.equals(row.getProjectCodeId(),file.getDccProjectCodeId())||!Objects.equals(after.tenantId(),file.getTenantId())
                ||!Objects.equals(after.actorId(),actor)||!Objects.equals(after.controlledFileId(),file.getId())
                ||!Objects.equals(after.masterId(),file.getMasterId())||!Objects.equals(after.projectId(),file.getDccProjectCodeId())
                ||!Objects.equals(after.attributeRound(),round)||!"NEW".equals(file.getChangeType())||!"NEW".equals(after.changeType())
                ||!Objects.equals(after.fileNumber(),file.getFileNumber())||!Objects.equals(after.sourceOriginalFileName(),file.getSourceOriginalFileName())
                ||!Objects.equals(after.directoryId(),file.getDirectoryId())||!Objects.equals(after.categoryId(),file.getCategoryId())
                ||!Objects.equals(after.taxonomyId(),file.getFileTypeTaxonomyId())||!Objects.equals(after.sourceFileId(),file.getSourceFileId())
                ||!Objects.equals(after.sourceSha256(),file.getSourceSha256())||!Objects.equals(after.previousSourceSha256(),file.getPreviousSourceSha256())
                ||!Objects.equals(after.approvalMatrixCanonical(),file.getDraftApprovalMatrixJson())
                ||!Objects.equals(after.preparedWorkflowContract(),file.getPreparedWorkflowContract())
                ||!Objects.equals(after.preparedProcessDefinitionId(),file.getPreparedProcessDefinitionId())
                ||!Objects.equals(after.preparedDefinitionDigest(),file.getPreparedDefinitionDigest())
                ||Objects.equals(before.taxonomyId(),after.taxonomyId())||before.categoryId()==null||before.taxonomyId()==null)throw typeChangeInvalid();
        var master=masters.selectById(file.getMasterId());
        if(master==null||!Objects.equals(master.getTenantId(),file.getTenantId())||master.getLatestControlledFileId()!=null||master.getCurrentActiveControlledFileId()!=null
                ||!Objects.equals(master.getFileTypeTaxonomyLeafId(),after.taxonomyId())||!Objects.equals(master.getCategoryId(),after.categoryId())||!Objects.equals(master.getDirectoryId(),after.directoryId())
                ||files.selectListByMasterId(master.getId()).stream().anyMatch(f->f.getControlledTime()!=null))throw typeChangeInvalid();
        var currentLocation=typeChangePlacements.findFile(file.getId());
        var beforeMapping=typeChangeMappings.findLeaf(file.getTenantId(),before.directoryId());
        var afterMapping=typeChangeMappings.findLeaf(file.getTenantId(),after.directoryId());
        if(recovered!=null){
            var sourceLocation=typeChangePlacements.findFile(before.sourceApplicationId());
            if(sourceLocation==null||!Objects.equals(recovered.observed().projectFolderId(),sourceLocation.getProjectFolderId())
                    ||!Objects.equals(sourceLocation.getProjectCodeId(),before.projectId())
                    ||!Objects.equals(sourceLocation.getStorageDirectoryId(),before.directoryId())
                    ||currentLocation==null||!Objects.equals(currentLocation.getProjectFolderId(),recovered.observed().projectFolderId())
                    ||!Objects.equals(currentLocation.getProjectCodeId(),after.projectId())
                    ||!Objects.equals(currentLocation.getStorageDirectoryId(),after.directoryId())
                    ||beforeMapping==null||afterMapping==null||Boolean.TRUE.equals(beforeMapping.getDeleted())||Boolean.TRUE.equals(afterMapping.getDeleted())
                    ||!Objects.equals(beforeMapping.getProjectFolderId(),recovered.observed().projectFolderId())
                    ||!Objects.equals(beforeMapping.getProjectCodeId(),before.projectId())||!Objects.equals(beforeMapping.getCategoryId(),before.categoryId())
                    ||!Objects.equals(afterMapping.getProjectFolderId(),recovered.observed().projectFolderId())
                    ||!Objects.equals(afterMapping.getProjectCodeId(),after.projectId())||!Objects.equals(afterMapping.getCategoryId(),after.categoryId()))throw typeChangeInvalid();
        }else if(after.projectFolderId()==null){
            if(currentLocation!=null||beforeMapping!=null||afterMapping!=null||!Objects.equals(before.directoryId(),after.directoryId()))throw typeChangeInvalid();
        }else{
            var sourceLocation=typeChangePlacements.findFile(before.sourceApplicationId());
            if(currentLocation==null||sourceLocation==null||beforeMapping==null||afterMapping==null
                    ||Boolean.TRUE.equals(beforeMapping.getDeleted())||Boolean.TRUE.equals(afterMapping.getDeleted())
                    ||!Objects.equals(currentLocation.getProjectCodeId(),after.projectId())||!Objects.equals(currentLocation.getProjectFolderId(),after.projectFolderId())
                    ||!Objects.equals(currentLocation.getStorageDirectoryId(),after.directoryId())||!Objects.equals(sourceLocation.getProjectCodeId(),before.projectId())
                    ||!Objects.equals(sourceLocation.getProjectFolderId(),before.projectFolderId())||!Objects.equals(sourceLocation.getStorageDirectoryId(),before.directoryId())
                    ||!Objects.equals(beforeMapping.getProjectCodeId(),before.projectId())||!Objects.equals(beforeMapping.getProjectFolderId(),before.projectFolderId())
                    ||!Objects.equals(beforeMapping.getCategoryId(),before.categoryId())||!Objects.equals(afterMapping.getProjectCodeId(),after.projectId())
                    ||!Objects.equals(afterMapping.getProjectFolderId(),after.projectFolderId())||!Objects.equals(afterMapping.getCategoryId(),after.categoryId()))throw typeChangeInvalid();
        }
        var saved=sourceAttributes.find("UPLOAD",file.getId(),round);
        if(saved==null||!Objects.equals(saved.getSourceApplicationId(),after.sourceApplicationId())
                ||!Objects.equals(saved.getSourceApplicationRound(),after.sourceAttributeRound()))throw typeChangeInvalid();
        var oldSource=files.selectById(before.sourceApplicationId());
        if(oldSource==null||!Objects.equals(oldSource.getCategoryId(),before.categoryId())||!Objects.equals(oldSource.getFileTypeTaxonomyId(),before.taxonomyId()))throw typeChangeInvalid();
        var original=new DccControlledFileDO();org.springframework.beans.BeanUtils.copyProperties(file,original);
        original.setCategoryId(before.categoryId());original.setFileTypeTaxonomyId(before.taxonomyId());original.setDirectoryId(before.directoryId());original.setDraftApprovalMatrixJson(before.approvalMatrixCanonical());
        var executed=readRejectedDraftFacts(actor,original);
        if(executed==null||!Objects.equals(executed.executedSourceApplicationId(),before.rejectedApplicationId())
                ||!Objects.equals(executed.executedSourceAttributeRound(),before.rejectedAttributeRound())
                ||!Objects.equals(executed.executedSourceBpm(),before.rejectedBpm())||!Objects.equals(executed.approvalMatrixCanonical(),before.approvalMatrixCanonical()))throw typeChangeInvalid();
        var target=DccApprovalMatrixSourceService.readCanonical(after.approvalMatrixCanonical());
        if(target==null||!Objects.equals(target.getTenantId(),after.tenantId())||!Objects.equals(target.getCategoryId(),after.categoryId())
                ||!Objects.equals(target.getFileTypeTaxonomyId(),after.taxonomyId()))throw typeChangeInvalid();
        return before;
    }
    private StorageRecoveryEnvelope readStorageRecoveryEnvelope(
            cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO original,
            cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO recovery,Long actor,DccControlledFileDO file){
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(original.getBeforeSnapshotJson(),TypeChangeState.class);
        var originalAfter=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(original.getAfterSnapshotJson(),TypeChangeState.class);
        var envelope=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(recovery.getAfterSnapshotJson(),StorageRecoveryEnvelope.class);
        var prior=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(recovery.getBeforeSnapshotJson(),StorageRecoveryEnvelope.class);
        if(before==null||originalAfter==null||envelope==null||prior==null||envelope.observed()==null||prior.observed()==null
                ||!STORAGE_RECOVERY_FORMAT.equals(envelope.formatVersion())||!STORAGE_RECOVERY_FORMAT.equals(prior.formatVersion())
                ||!Objects.equals(envelope.originalProofId(),original.getId())||!Objects.equals(prior.originalProofId(),original.getId())
                ||!Objects.equals(envelope.originalProofSha256(),storageRecoveryDigest(original))
                ||!Objects.equals(prior.originalProofSha256(),storageRecoveryDigest(original))
                ||!Objects.equals(recovery.getTenantId(),file.getTenantId())||!Objects.equals(recovery.getControlledFileId(),file.getId())
                ||!Objects.equals(recovery.getMasterId(),file.getMasterId())||!Objects.equals(recovery.getProjectCodeId(),file.getDccProjectCodeId())
                ||!Objects.equals(recovery.getOperatorUserId(),actor)||!Objects.equals(original.getOperatorUserId(),actor)
                ||recovery.getRequestId()==null||recovery.getRequestId().isBlank()||recovery.getChangeReason()==null||recovery.getChangeReason().isBlank())throw typeChangeInvalid();
        var observed=envelope.observed();var priorObserved=prior.observed();
        var anchored=new TypeChangeState(originalAfter.formatVersion(),originalAfter.requestId(),originalAfter.tenantId(),originalAfter.actorId(),originalAfter.masterId(),
                originalAfter.controlledFileId(),originalAfter.projectId(),originalAfter.fileNumber(),originalAfter.sourceOriginalFileName(),originalAfter.changeType(),
                originalAfter.directoryId(),observed.projectFolderId(),originalAfter.attributeRound(),originalAfter.sourceApplicationId(),originalAfter.sourceAttributeRound(),
                originalAfter.rejectedApplicationId(),originalAfter.rejectedAttributeRound(),originalAfter.rejectedBpm(),originalAfter.categoryId(),originalAfter.taxonomyId(),
                originalAfter.sourceFileId(),originalAfter.sourceSha256(),originalAfter.previousSourceSha256(),originalAfter.approvalMatrixCanonical(),
                originalAfter.preparedWorkflowContract(),originalAfter.preparedProcessDefinitionId(),originalAfter.preparedDefinitionDigest(),
                originalAfter.selectedSignoffDepartmentIdsJson(),originalAfter.distributionDepartmentIdsJson());
        var priorAnchored=new TypeChangeState(priorObserved.formatVersion(),priorObserved.requestId(),priorObserved.tenantId(),priorObserved.actorId(),priorObserved.masterId(),
                priorObserved.controlledFileId(),priorObserved.projectId(),priorObserved.fileNumber(),priorObserved.sourceOriginalFileName(),priorObserved.changeType(),
                priorObserved.directoryId(),observed.projectFolderId(),priorObserved.attributeRound(),priorObserved.sourceApplicationId(),priorObserved.sourceAttributeRound(),
                priorObserved.rejectedApplicationId(),priorObserved.rejectedAttributeRound(),priorObserved.rejectedBpm(),priorObserved.categoryId(),priorObserved.taxonomyId(),
                priorObserved.sourceFileId(),priorObserved.sourceSha256(),priorObserved.previousSourceSha256(),priorObserved.approvalMatrixCanonical(),
                priorObserved.preparedWorkflowContract(),priorObserved.preparedProcessDefinitionId(),priorObserved.preparedDefinitionDigest(),
                priorObserved.selectedSignoffDepartmentIdsJson(),priorObserved.distributionDepartmentIdsJson());
        if(!recoveryIdentity(originalAfter).equals(recoveryIdentity(anchored))||!observed.sameSourceAs(anchored)||!priorAnchored.sameSourceAs(anchored)
                ||!Objects.equals(originalAfter.directoryId(),before.directoryId())||Objects.equals(observed.directoryId(),before.directoryId())
                ||!Objects.equals(observed.projectFolderId(),priorObserved.projectFolderId())
                ||!Objects.equals(observed.directoryId(),file.getDirectoryId())||!Objects.equals(observed.categoryId(),file.getCategoryId())
                ||!Objects.equals(observed.taxonomyId(),file.getFileTypeTaxonomyId())||!Objects.equals(observed.sourceSha256(),file.getSourceSha256()))throw typeChangeInvalid();
        return envelope;
    }
    private List<Object> recoveryIdentity(TypeChangeState s){return Arrays.asList(s.formatVersion(),s.requestId(),s.tenantId(),s.actorId(),s.masterId(),s.controlledFileId(),
            s.projectId(),s.fileNumber(),s.sourceOriginalFileName(),s.changeType(),s.attributeRound(),s.sourceApplicationId(),s.sourceAttributeRound(),
            s.rejectedApplicationId(),s.rejectedAttributeRound(),s.rejectedBpm(),s.categoryId(),s.taxonomyId(),s.sourceFileId(),s.sourceSha256(),
            s.previousSourceSha256(),s.preparedWorkflowContract(),s.preparedProcessDefinitionId(),s.preparedDefinitionDigest());}
    public static String storageRecoveryDigest(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO proof){
        try{
            String input=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Arrays.asList(proof.getId(),proof.getTenantId(),proof.getControlledFileId(),
                    proof.getMasterId(),proof.getProjectCodeId(),proof.getOperatorUserId(),proof.getSource(),proof.getRequestId(),proof.getChangeReason(),
                    proof.getBeforeSnapshotJson(),proof.getAfterSnapshotJson()));
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    /** The only historical type exception is a verified NEW applicant conversion and its actual ancestors. */
    public boolean isRecordedNewTypeChangeHistory(Long actor,DccControlledFileDO selected,DccControlledFileDO historical){
        if(selected==null||historical==null||!"NEW".equals(selected.getChangeType())||!"WORKING".equals(selected.getStatus())
                ||!Objects.equals(selected.getTenantId(),historical.getTenantId())||!Objects.equals(selected.getMasterId(),historical.getMasterId())
                ||!Objects.equals(selected.getDccProjectCodeId(),historical.getDccProjectCodeId())||!Objects.equals(selected.getFileNumber(),historical.getFileNumber()))return false;
        var cursor=selected;var visited=new HashSet<Long>();
        while(cursor!=null&&visited.add(cursor.getId())){
            int round;
            if("WORKING".equals(cursor.getStatus())&&cursor.getProcessInstanceId()==null){
                requireDraft(actor,cursor);round=rounds.requireDraft("UPLOAD",cursor.getId());
            }else{
                if(!Objects.equals(actor,cursor.getRequesterId())||!Objects.equals(selected.getTenantId(),cursor.getTenantId())
                        ||!"NEW".equals(cursor.getChangeType())||!Set.of("REJECTED","WITHDRAWN").contains(cursor.getStatus())
                        ||cursor.getProcessInstanceId()==null)throw sourceInvalid();
                round=rounds.require("UPLOAD",cursor.getId(),cursor.getProcessInstanceId());
                requirePublicBoundSource(cursor,rounds.resolveBoundBpm("UPLOAD",cursor.getId(),round));
            }
            var saved=sourceAttributes.find("UPLOAD",cursor.getId(),round);if(saved==null||saved.getSourceApplicationId()==null)return false;
            var source=files.selectById(saved.getSourceApplicationId());if(source==null)return false;
            if(!sameType(cursor,source)){
                var proof=readTypeChangeBoundary(actor,cursor,round);if(proof==null)return false;
                var old=source;var oldIds=new HashSet<Long>();
                while(old!=null&&oldIds.add(old.getId())){
                    if(Objects.equals(old.getId(),historical.getId()))return Objects.equals(old.getCategoryId(),proof.categoryId())&&Objects.equals(old.getFileTypeTaxonomyId(),proof.taxonomyId());
                    if(!"WORKING".equals(old.getStatus()))return false;
                    old=old.getPredecessorControlledFileId()==null?null:files.selectById(old.getPredecessorControlledFileId());
                }
                return false;
            }
            verifyInheritedPrepared(actor,cursor.getId(),source.getId(),saved.getSourceApplicationRound());cursor=source;
        }
        return false;
    }
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
    /** Only saved same-application provenance can pin a failed execution, including repeated checkins. */
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public DraftFacts readRejectedDraftFacts(Long actor,DccControlledFileDO target){
        requireDraft(actor,target);
        if(!isPublicContract(target.getPreparedWorkflowContract()))return null;
        var cursor=target;String type=typeOf(target);int round=rounds.requireDraft(type,target.getId());
        var visited=new HashSet<Long>();var draftMatrices=new ArrayList<String>();
        while(true){
            requireDraft(actor,cursor);requireSameFamily(target,cursor);
            if(!visited.add(cursor.getId())||!Objects.equals(type,typeOf(cursor)))throw sourceInvalid();
            var saved=sourceAttributes.find(type,cursor.getId(),round);
            if(saved==null||!Objects.equals(saved.getTenantId(),target.getTenantId())
                    ||!Objects.equals(saved.getProjectCodeId(),target.getDccProjectCodeId())
                    ||!Objects.equals(saved.getApplicationId(),cursor.getId())||!Objects.equals(saved.getApplicationType(),type)
                    ||!Objects.equals(saved.getApplicationRound(),round)||!Boolean.FALSE.equals(saved.getSubmitted())
                    ||rounds.requireDraft(type,cursor.getId())!=round
                    ||(saved.getSourceApplicationId()==null)!=(saved.getSourceApplicationRound()==null))throw sourceInvalid();
            if(saved.getSourceApplicationId()==null){
                if(cursor.getPredecessorControlledFileId()!=null){
                    var controlled=files.selectById(cursor.getPredecessorControlledFileId());requireSameFamily(target,controlled);
                    if(!isNewControlledRevisionBody(cursor,controlled))throw sourceInvalid();
                    int controlledRound=rounds.require(typeOf(controlled),controlled.getId(),controlled.getProcessInstanceId());
                    verifyInheritedPrepared(actor,cursor.getId(),controlled.getId(),controlledRound);
                }
                return null; // An actual unsent root or a registered new controlled revision still compares latest.
            }
            var source=files.selectById(saved.getSourceApplicationId());
            if(source!=null&&!sameType(cursor,source)){
                var proof=readTypeChangeBoundary(actor,cursor,round);
                if(proof!=null){String confirmed=cursor.getDraftApprovalMatrixJson();if(draftMatrices.stream().anyMatch(matrix->!Objects.equals(matrix,confirmed)))throw typeChangeInvalid();return null;}
            }
            requireSameFamily(target,source);
            var facts=readInheritedDraftFactsFromVerifiedTarget(actor,cursor,source.getId(),saved.getSourceApplicationRound());
            if(!facts.sameApplicationType()||facts.newActionBaselineRequired())throw sourceInvalid();
            draftMatrices.add(cursor.getDraftApprovalMatrixJson());
            if("WORKING".equals(source.getStatus())&&source.getProcessInstanceId()==null){
                cursor=source;round=saved.getSourceApplicationRound();continue;
            }
            if(!Set.of("REJECTED","WITHDRAWN","PENDING_APPLICANT_REWORK").contains(source.getStatus())
                    ||!Objects.equals(source.getRequesterId(),actor)
                    ||!Objects.equals(facts.executedSourceApplicationId(),source.getId())
                    ||!Objects.equals(facts.executedSourceAttributeRound(),saved.getSourceApplicationRound())
                    ||facts.executedSourceBpm()==null||facts.approvalMatrixCanonical()==null||facts.runtimeApprovalNodesJson()==null
                    ||draftMatrices.stream().anyMatch(matrix->!Objects.equals(matrix,facts.approvalMatrixCanonical())))throw sourceInvalid();
            return facts;
        }
    }
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
        String targetContextKey="dcc-custom-v1:"+target.getTenantId()+":"+typeOf(target)+":"+targetId+":"+targetRound;
        var reserved=new DccApplicationCustomMatrixService.SourceContext(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,target.getTenantId(),target.getDccProjectCodeId(),target.getCategoryId(),target.getFileTypeTaxonomyId(),target.getRequesterId(),targetId,targetRound,typeOf(target),targetContextKey,target.getPreparedProcessDefinitionId(),target.getPreparedDefinitionDigest());
        return new CustomDraftInheritance("APPLICATION_CUSTOM",values.canonicalize(reserved,actual.applicationCustomMatrix(),actual.selectedSignoffDepartmentIds()).canonical(),targetContextKey);
    }
    /** Read exact saved facts for a newly created body; writes remain with its Query/Revision owner. */
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true,rollbackFor=Exception.class)
    public DraftFacts readInheritedDraftFacts(Long actor,Long targetFileId,Long sourceFileId,Integer sourceAttributeRound){
        if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("DRAFT_FACTS_OUTER_TRANSACTION_REQUIRED");
        var target=files.selectById(targetFileId);requireDraft(actor,target);
        return readInheritedDraftFactsFromVerifiedTarget(actor,target,sourceFileId,sourceAttributeRound);
    }
    private DraftFacts readInheritedDraftFactsFromVerifiedTarget(Long actor,DccControlledFileDO target,Long sourceFileId,Integer sourceAttributeRound){
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
