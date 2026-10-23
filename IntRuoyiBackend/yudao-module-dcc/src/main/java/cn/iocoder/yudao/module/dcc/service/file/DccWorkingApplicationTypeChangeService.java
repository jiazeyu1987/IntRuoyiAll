package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.service.category.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** One explicit conversion of an unreleased NEW rejected body; history and source ownership never move. */
@Service
public class DccWorkingApplicationTypeChangeService {
    @Resource(name="dccControlledFileMapper") private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccFileCategoryMapper categories;
    @Resource private DccCategoryDirectoryBindingMapper bindings;
    @Resource private DccFileDirectoryMapper directories;
    @Resource private DccFileTypeTaxonomyAdminService taxonomies;
    @Resource private DccProjectAccessService access;
    @Resource private DccControlledFileCategoryPermissionSupport permissions;
    @Resource private DccControlledFileSourceOwnershipMapper ownership;
    @Resource private DccControlledFileSourceOwnershipService sourceOwnership;
    @Resource private DccControlledFileCheckoutMapper checkouts;
    @Resource private DccPublicUploadPlacementService placements;
    @Resource private DccControlledFileNameClaimService names;
    @Resource private DccControlledFileMetadataChangeMapper typeChanges;
    @Resource private DccApplicationExecutionContractService contracts;
    @Resource private DccApplicationRoundService rounds;
    @Resource private DccApprovalMatrixSourceService matrices;
    @Resource private DccControlledFileApprovalRouteAssigneeResolver resolver;
    @Resource private DccControlledFileRouteReadinessService readiness;
    @Resource private cn.iocoder.yudao.module.system.api.dept.DeptApi departments;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFilePlacementMapper placementRows;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper storageMappings;

    private IllegalArgumentException invalid(String reason){return new IllegalArgumentException("WORKING_TYPE_CHANGE_"+reason);}
    private record Selection(DccControlledFileDO file,DccApplicationExecutionContractService.DraftFacts origin,int round,
            DccFileTypeTaxonomyPath path,Long targetCategory,DccApprovalMatrixSourceService.Source matrix,
            List<Long> signoff,List<Long> distribution,DccControlledFileRouteReadinessRespVO readiness){}
    private record RecoveryFacts(DccControlledFileMetadataChangeDO proof,DccApplicationExecutionContractService.TypeChangeState before,
            DccApplicationExecutionContractService.TypeChangeState after,Long folderId,String proofDigest,
            DccControlledFileMetadataChangeDO recovery,DccApplicationExecutionContractService.StorageRecoveryEnvelope priorEnvelope){}

    @Transactional(readOnly=true)
    public DccWorkingApplicationTypeChangePreviewVO preview(Long actor,Long id,DccWorkingApplicationTypeChangeReqVO request){
        var file=requireFile(actor,id,false);return previewOf(select(actor,file,request,false));
    }

    @Transactional(readOnly=true)
    public DccWorkingApplicationStorageRecoveryPreviewVO previewStorageRecovery(Long actor,Long id){
        return storageRecoveryPreview(actor,requireFile(actor,id,false,true));
    }

    @Transactional(rollbackFor=Exception.class)
    public DccWorkingApplicationStorageRecoveryPreviewVO confirmStorageRecovery(Long actor,Long id,DccWorkingApplicationStorageRecoveryReqVO request){
        if(request==null||!Boolean.TRUE.equals(request.confirmed())||request.expectedOriginalProofId()==null
                ||request.expectedOriginalProofSha256()==null||request.expectedObservedSha256()==null
                ||request.requestId()==null||!request.requestId().matches("[A-Za-z0-9_-]{1,64}")
                ||request.changeReason()==null||request.changeReason().isBlank()||request.changeReason().length()>512)
            throw invalid("STORAGE_RECOVERY_CONFIRMATION_REQUIRED");
        var file=requireFile(actor,id,true,true);var facts=storageRecoveryFacts(actor,file);
        var existing=storageRecoveryRows(file).stream().filter(r->Objects.equals(r.getRequestId(),request.requestId())).toList();
        if(!existing.isEmpty()){
            if(existing.size()!=1)throw invalid("STORAGE_RECOVERY_IDEMPOTENCY_CONFLICT");
            var row=existing.get(0);var prior=JsonUtils.parseObject(row.getBeforeSnapshotJson(),DccApplicationExecutionContractService.StorageRecoveryEnvelope.class);
            var priorObserved=prior==null?null:prior.observed();
            String observedHash=priorObserved==null?null:sha256(JsonUtils.toJsonString(priorObserved));
            if(!Objects.equals(row.getOperatorUserId(),actor)||!Objects.equals(row.getChangeReason(),request.changeReason().trim())
                    ||!Objects.equals(row.getControlledFileId(),id)||!Objects.equals(row.getMasterId(),file.getMasterId())
                    ||!Objects.equals(row.getProjectCodeId(),file.getDccProjectCodeId())
                    ||!Objects.equals(facts.proof().getId(),request.expectedOriginalProofId())
                    ||!Objects.equals(facts.proofDigest(),request.expectedOriginalProofSha256())
                    ||!Objects.equals(observedHash,request.expectedObservedSha256()))throw invalid("STORAGE_RECOVERY_IDEMPOTENCY_CONFLICT");
            return storageRecoveryPreview(actor,file);
        }
        if(!storageRecoveryRows(file).isEmpty())throw invalid("STORAGE_RECOVERY_ALREADY_RECORDED");
        var preview=storageRecoveryPreview(actor,file);
        if(!Objects.equals(preview.originalProofId(),request.expectedOriginalProofId().toString())
                ||!Objects.equals(preview.originalProofSha256(),request.expectedOriginalProofSha256())
                ||!Objects.equals(preview.observedSha256(),request.expectedObservedSha256()))throw invalid("STORAGE_RECOVERY_EXPECTATION_STALE");
        requirePermission(actor,facts.before().categoryId());requirePermission(actor,facts.after().categoryId());
        var targetStorage=placements.resolveWorkingStorageRecovery(actor,id,facts.after().sourceApplicationId(),facts.before().categoryId(),
                facts.after().categoryId(),facts.after().taxonomyId(),facts.folderId());
        Long oldDirectory=file.getDirectoryId();Long targetDirectory=targetStorage.storageDirectoryId;
        if(masters.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileMasterDO>()
                .eq(DccControlledFileMasterDO::getId,file.getMasterId()).eq(DccControlledFileMasterDO::getTenantId,file.getTenantId())
                .eq(DccControlledFileMasterDO::getDirectoryId,oldDirectory).eq(DccControlledFileMasterDO::getCategoryId,file.getCategoryId())
                .eq(DccControlledFileMasterDO::getFileTypeTaxonomyLeafId,file.getFileTypeTaxonomyId())
                .set(DccControlledFileMasterDO::getDirectoryId,targetDirectory))!=1)throw invalid("STORAGE_RECOVERY_MASTER_CAS_CONFLICT");
        if(files.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId,id).eq(DccControlledFileDO::getTenantId,file.getTenantId())
                .eq(DccControlledFileDO::getDirectoryId,oldDirectory).eq(DccControlledFileDO::getStatus,"WORKING")
                .set(DccControlledFileDO::getDirectoryId,targetDirectory))!=1)throw invalid("STORAGE_RECOVERY_FILE_CAS_CONFLICT");
        file.setDirectoryId(targetDirectory);
        var priorEnvelope=new DccApplicationExecutionContractService.StorageRecoveryEnvelope(
                DccApplicationExecutionContractService.STORAGE_RECOVERY_FORMAT,facts.proof().getId(),facts.proofDigest(),
                recoveryState(facts.after(),oldDirectory,facts.folderId()));
        var afterEnvelope=new DccApplicationExecutionContractService.StorageRecoveryEnvelope(
                DccApplicationExecutionContractService.STORAGE_RECOVERY_FORMAT,facts.proof().getId(),facts.proofDigest(),
                recoveryState(facts.after(),targetDirectory,facts.folderId()));
        var evidence=DccControlledFileMetadataChangeDO.builder().projectCodeId(file.getDccProjectCodeId()).controlledFileId(id).masterId(file.getMasterId())
                .operatorUserId(actor).source(DccApplicationExecutionContractService.STORAGE_RECOVERY_SOURCE).requestId(request.requestId())
                .changeReason(request.changeReason().trim()).changedFieldCount(3).beforeSnapshotJson(JsonUtils.toJsonString(priorEnvelope))
                .afterSnapshotJson(JsonUtils.toJsonString(afterEnvelope)).changedTime(LocalDateTime.now()).build();evidence.setTenantId(file.getTenantId());
        if(typeChanges.insert(evidence)!=1)throw invalid("STORAGE_RECOVERY_EVIDENCE_PERSIST_FAILED");
        placements.rebindWorkingStorageRecovery(actor,id,oldDirectory,targetStorage,facts.proof().getId(),evidence.getId());
        if(!Objects.equals(contracts.readConfirmedTypeChangeBaseline(actor,file),facts.after().approvalMatrixCanonical()))
            throw invalid("STORAGE_RECOVERY_READBACK_FAILED");
        return storageRecoveryPreview(actor,file);
    }

    @Transactional(rollbackFor=Exception.class)
    public DccWorkingApplicationTypeChangePreviewVO confirm(Long actor,Long id,DccWorkingApplicationTypeChangeReqVO request){
        if(request==null||!Boolean.TRUE.equals(request.getConfirmed())||request.getRequestId()==null||!request.getRequestId().matches("[A-Za-z0-9_-]{1,64}")
                ||request.getChangeReason()==null||request.getChangeReason().isBlank()||request.getChangeReason().length()>512)throw invalid("CONFIRMATION_REQUIRED");
        var file=requireFile(actor,id,true);
        var replays=typeChanges.selectListBySource(DccApplicationExecutionContractService.TYPE_CHANGE_SOURCE).stream()
                .filter(r->Objects.equals(r.getTenantId(),file.getTenantId())&&Objects.equals(r.getMasterId(),file.getMasterId())&&Objects.equals(r.getRequestId(),request.getRequestId())).toList();
        if(!replays.isEmpty()){
            if(replays.size()!=1)throw invalid("IDEMPOTENCY_CONFLICT");var row=replays.get(0);
            var before=JsonUtils.parseObject(row.getBeforeSnapshotJson(),DccApplicationExecutionContractService.TypeChangeState.class);
            var after=JsonUtils.parseObject(row.getAfterSnapshotJson(),DccApplicationExecutionContractService.TypeChangeState.class);
            if(before==null||after==null||!Objects.equals(row.getControlledFileId(),id)||!Objects.equals(row.getOperatorUserId(),actor)
                    ||!Objects.equals(row.getChangeReason(),request.getChangeReason().trim())||!Objects.equals(after.taxonomyId(),request.getTargetFileTypeTaxonomyId()))throw invalid("IDEMPOTENCY_CONFLICT");
            expectSource(request,before);expectMatrix(request,DccApprovalMatrixSourceService.readCanonical(after.approvalMatrixCanonical()));
            if(!Objects.equals(ids(after.selectedSignoffDepartmentIdsJson()),request.getSelectedSignoffDepartmentIds())
                    ||!Objects.equals(ids(after.distributionDepartmentIdsJson()),request.getSelectedDistributionDepartmentIds()))throw invalid("IDEMPOTENCY_CONFLICT");
            requirePermission(actor,before.categoryId());requirePermission(actor,after.categoryId());
            if(!Objects.equals(contracts.readConfirmedTypeChangeBaseline(actor,file),after.approvalMatrixCanonical()))throw invalid("IDEMPOTENCY_CONFLICT");
            var source=source(after.approvalMatrixCanonical());var route=resolver.resolvePublicMatrix(source,actor,"NEW",ids(after.selectedSignoffDepartmentIdsJson()));
            var check=readiness.evaluateFrozenRoute(route);check.requireReady();
            return new DccWorkingApplicationTypeChangePreviewVO(id.toString(),after.attributeRound(),before.categoryId().toString(),before.taxonomyId().toString(),
                    before.sourceSha256(),DccApprovalMatrixSourceService.readCanonical(before.approvalMatrixCanonical()).getMatrixDigest(),after.categoryId().toString(),after.taxonomyId().toString(),
                    String.join(" / ",List.of(taxonomies.resolveActivePath(after.taxonomyId()).level1(),taxonomies.resolveActivePath(after.taxonomyId()).level2(),taxonomies.resolveActivePath(after.taxonomyId()).level3(),taxonomies.resolveActivePath(after.taxonomyId()).level4(),taxonomies.resolveActivePath(after.taxonomyId()).level5())),
                    DccApprovalMatrixSourceService.readCanonical(before.approvalMatrixCanonical()),source.baseline(),strings(ids(after.selectedSignoffDepartmentIdsJson())),strings(ids(after.distributionDepartmentIdsJson())),check.response());
        }
        var selected=select(actor,file,request,true);
        var before=contracts.typeChangeState(actor,file,selected.round(),selected.origin(),request.getRequestId());expectSource(request,before);
        expectMatrix(request,selected.matrix().baseline());
        if(request.getSelectedSignoffDepartmentIds()==null||request.getSelectedDistributionDepartmentIds()==null)throw invalid("EXPLICIT_DEPARTMENTS_REQUIRED");
        var occupied=masters.selectByNewLogicalIdentity(file.getTenantId(),file.getDccProjectCodeId(),selected.path().id(),file.getFileNumber());
        if(occupied!=null&&!Objects.equals(occupied.getId(),file.getMasterId()))throw invalid("IDENTITY_CONFLICT");
        Long oldDirectory=file.getDirectoryId();
        var targetStorage=placements.resolveWorkingTypeChange(actor,id,selected.targetCategory(),selected.path().id());
        Long targetDirectory=targetStorage==null?oldDirectory:targetStorage.storageDirectoryId;
        names.rebindNewWorkingType(file,selected.path().id());
        if(masters.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileMasterDO>()
                .eq(DccControlledFileMasterDO::getId,file.getMasterId()).eq(DccControlledFileMasterDO::getTenantId,file.getTenantId())
                .eq(DccControlledFileMasterDO::getCategoryId,file.getCategoryId()).eq(DccControlledFileMasterDO::getFileTypeTaxonomyLeafId,file.getFileTypeTaxonomyId())
                .eq(DccControlledFileMasterDO::getDirectoryId,oldDirectory)
                .isNull(DccControlledFileMasterDO::getLatestControlledFileId).isNull(DccControlledFileMasterDO::getCurrentActiveControlledFileId)
                .set(DccControlledFileMasterDO::getCategoryId,selected.targetCategory()).set(DccControlledFileMasterDO::getFileTypeTaxonomyLeafId,selected.path().id())
                .set(DccControlledFileMasterDO::getDirectoryId,targetDirectory))!=1)
            throw invalid("MASTER_CAS_CONFLICT");
        file.setCategoryId(selected.targetCategory());file.setFileTypeTaxonomyId(selected.path().id());file.setDirectoryId(targetDirectory);
        file.setFileTypeLevel1(selected.path().level1());file.setFileTypeLevel2(selected.path().level2());file.setFileTypeLevel3(selected.path().level3());
        file.setFileTypeLevel4(selected.path().level4());file.setFileTypeLevel5(selected.path().level5());file.setDraftApprovalMatrixJson(selected.matrix().canonical());
        file.setSelectedSignoffDepartmentIdsJson(JsonUtils.toJsonString(strings(selected.signoff())));
        file.setDistributionDepartmentIdsJson(JsonUtils.toJsonString(strings(selected.distribution())));
        if(files.updateById(file)!=1)throw invalid("FILE_CAS_CONFLICT");
        var after=contracts.typeChangeState(actor,file,selected.round(),selected.origin(),request.getRequestId());
        var proof=DccControlledFileMetadataChangeDO.builder().projectCodeId(file.getDccProjectCodeId()).controlledFileId(id).masterId(file.getMasterId())
                .operatorUserId(actor).source(DccApplicationExecutionContractService.TYPE_CHANGE_SOURCE).requestId(request.getRequestId())
                .changeReason(request.getChangeReason().trim()).changedFieldCount(5).beforeSnapshotJson(JsonUtils.toJsonString(before))
                .afterSnapshotJson(JsonUtils.toJsonString(after)).changedTime(LocalDateTime.now()).build();proof.setTenantId(file.getTenantId());
        if(typeChanges.insert(proof)!=1)throw invalid("PROOF_PERSIST_FAILED");
        if(targetStorage!=null)placements.rebindWorkingTypeChange(actor,id,oldDirectory,targetStorage,proof.getId());
        if(!Objects.equals(contracts.readConfirmedTypeChangeBaseline(actor,file),selected.matrix().canonical()))throw invalid("PROOF_READBACK_FAILED");
        return previewOf(selected);
    }

    private DccWorkingApplicationStorageRecoveryPreviewVO storageRecoveryPreview(Long actor,DccControlledFileDO file){
        var facts=storageRecoveryFacts(actor,file);var after=facts.after();
        var path=taxonomies.resolveActivePath(after.taxonomyId());
        var baseline=DccApprovalMatrixSourceService.readCanonical(after.approvalMatrixCanonical());
        var bindingRows=bindings.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO::getCategoryId,after.categoryId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO::getActive,true));
        if(bindingRows.size()!=1)throw invalid("STORAGE_RECOVERY_TARGET_BINDING_INVALID");
        var base=directories.selectById(bindingRows.get(0).getDirectoryId());
        if(base==null||!Objects.equals(base.getTenantId(),file.getTenantId())||!Boolean.TRUE.equals(base.getActive()))throw invalid("STORAGE_RECOVERY_TARGET_BINDING_INVALID");
        var mapped=storageMappings.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderStorageMappingDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderStorageMappingDO::getTenantId,file.getTenantId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderStorageMappingDO::getProjectFolderId,facts.folderId())
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderStorageMappingDO::getCategoryId,after.categoryId()));
        if(mapped!=null&&(!Objects.equals(mapped.getBaseDirectoryId(),base.getId())||Boolean.TRUE.equals(mapped.getDeleted())))
            throw invalid("STORAGE_RECOVERY_TARGET_MAPPING_INVALID");
        var observed=recoveryState(after,file.getDirectoryId(),facts.folderId());
        String digest=sha256(JsonUtils.toJsonString(observed));
        boolean recovered=facts.recovery()!=null;
        if(recovered){
            contracts.readConfirmedTypeChangeBaseline(actor,file);
            observed=facts.priorEnvelope().observed();digest=sha256(JsonUtils.toJsonString(observed));
        }
        return new DccWorkingApplicationStorageRecoveryPreviewVO(file.getId().toString(),file.getMasterId().toString(),file.getFileNumber(),
                file.getDccProjectCodeId().toString(),facts.folderId().toString(),after.categoryId().toString(),after.taxonomyId().toString(),
                String.join(" / ",List.of(path.level1(),path.level2(),path.level3(),path.level4(),path.level5())),facts.proof().getId().toString(),
                facts.proofDigest(),digest,file.getSourceSha256(),baseline.getMatrixDigest(),facts.before().directoryId().toString(),base.getId().toString(),
                mapped==null?null:mapped.getStorageDirectoryId().toString(),recovered,recovered?facts.recovery().getId().toString():null);
    }

    private RecoveryFacts storageRecoveryFacts(Long actor,DccControlledFileDO file){
        var originals=typeChanges.selectListBySource(DccApplicationExecutionContractService.TYPE_CHANGE_SOURCE).stream()
                .filter(r->Objects.equals(r.getTenantId(),file.getTenantId())&&Objects.equals(r.getControlledFileId(),file.getId())).toList();
        if(originals.size()!=1)throw invalid("STORAGE_RECOVERY_ORIGINAL_PROOF_INVALID");
        var proof=originals.get(0);var before=JsonUtils.parseObject(proof.getBeforeSnapshotJson(),DccApplicationExecutionContractService.TypeChangeState.class);
        var after=JsonUtils.parseObject(proof.getAfterSnapshotJson(),DccApplicationExecutionContractService.TypeChangeState.class);
        var recoveries=storageRecoveryRows(file);
        if(recoveries.size()>1)throw invalid("STORAGE_RECOVERY_EVIDENCE_AMBIGUOUS");
        var recovery=recoveries.isEmpty()?null:recoveries.get(0);
        var recoveryAfter=recovery==null?null:JsonUtils.parseObject(recovery.getAfterSnapshotJson(),DccApplicationExecutionContractService.StorageRecoveryEnvelope.class);
        Long currentDirectory=recoveryAfter==null||recoveryAfter.observed()==null?after==null?null:after.directoryId():recoveryAfter.observed().directoryId();
        if(before==null||after==null||!Objects.equals(proof.getOperatorUserId(),actor)||!Objects.equals(proof.getMasterId(),file.getMasterId())
                ||!Objects.equals(proof.getProjectCodeId(),file.getDccProjectCodeId())||!Objects.equals(after.tenantId(),file.getTenantId())
                ||!Objects.equals(after.actorId(),actor)||!Objects.equals(after.masterId(),file.getMasterId())||!Objects.equals(after.controlledFileId(),file.getId())
                ||!Objects.equals(after.projectId(),file.getDccProjectCodeId())||!Objects.equals(after.fileNumber(),file.getFileNumber())
                ||!Objects.equals(after.categoryId(),file.getCategoryId())||!Objects.equals(after.taxonomyId(),file.getFileTypeTaxonomyId())
                ||!Objects.equals(currentDirectory,file.getDirectoryId())||!Objects.equals(after.sourceSha256(),file.getSourceSha256())
                ||!Objects.equals(after.sourceFileId(),file.getSourceFileId())||!Objects.equals(after.approvalMatrixCanonical(),file.getDraftApprovalMatrixJson())
                ||!Objects.equals(before.directoryId(),after.directoryId())||Objects.equals(before.categoryId(),after.categoryId())
                ||Objects.equals(before.taxonomyId(),after.taxonomyId()))throw invalid("STORAGE_RECOVERY_ORIGINAL_PROOF_INVALID");
        var source=files.selectById(after.sourceApplicationId());var currentLocation=placementRows.findFile(file.getId());
        var sourceLocation=source==null?null:placementRows.findFile(source.getId());
        if(source==null||currentLocation==null||sourceLocation==null||!Objects.equals(source.getCategoryId(),before.categoryId())
                ||!Objects.equals(source.getFileTypeTaxonomyId(),before.taxonomyId())||!Objects.equals(source.getDirectoryId(),before.directoryId())
                ||!Objects.equals(currentLocation.getProjectCodeId(),file.getDccProjectCodeId())
                ||!Objects.equals(currentLocation.getStorageDirectoryId(),file.getDirectoryId())
                ||!Objects.equals(sourceLocation.getProjectCodeId(),file.getDccProjectCodeId())
                ||!Objects.equals(sourceLocation.getStorageDirectoryId(),source.getDirectoryId())
                ||!Objects.equals(currentLocation.getProjectFolderId(),sourceLocation.getProjectFolderId()))throw invalid("STORAGE_RECOVERY_SOURCE_LOCATION_INVALID");
        Long folder=currentLocation.getProjectFolderId();var oldMapping=storageMappings.findLeaf(file.getTenantId(),before.directoryId());
        if(oldMapping==null||Boolean.TRUE.equals(oldMapping.getDeleted())||!Objects.equals(oldMapping.getProjectCodeId(),file.getDccProjectCodeId())
                ||!Objects.equals(oldMapping.getProjectFolderId(),folder)||!Objects.equals(oldMapping.getCategoryId(),before.categoryId()))
            throw invalid("STORAGE_RECOVERY_SOURCE_MAPPING_INVALID");
        requireActiveRecoveryCategory(before.categoryId(),before.taxonomyId());requireActiveRecoveryCategory(after.categoryId(),after.taxonomyId());
        if(!permissions.hasCategoryPermission(before.categoryId(),actor,DccFileCategoryPermissionActionEnum.UPLOAD))throw invalid("SOURCE_UPLOAD_PERMISSION_DENIED");
        DccApplicationExecutionContractService.StorageRecoveryEnvelope prior=null;
        if(recovery!=null){
            prior=JsonUtils.parseObject(recovery.getBeforeSnapshotJson(),DccApplicationExecutionContractService.StorageRecoveryEnvelope.class);
            var current=JsonUtils.parseObject(recovery.getAfterSnapshotJson(),DccApplicationExecutionContractService.StorageRecoveryEnvelope.class);
            if(prior==null||current==null||prior.observed()==null||current.observed()==null
                    ||!Objects.equals(prior.originalProofId(),proof.getId())||!Objects.equals(current.originalProofId(),proof.getId())
                    ||!Objects.equals(prior.originalProofSha256(),DccApplicationExecutionContractService.storageRecoveryDigest(proof))
                    ||!Objects.equals(current.originalProofSha256(),DccApplicationExecutionContractService.storageRecoveryDigest(proof)))
                throw invalid("STORAGE_RECOVERY_EVIDENCE_INVALID");
        }
        return new RecoveryFacts(proof,before,after,folder,DccApplicationExecutionContractService.storageRecoveryDigest(proof),recovery,prior);
    }

    private List<DccControlledFileMetadataChangeDO> storageRecoveryRows(DccControlledFileDO file){
        return typeChanges.selectListBySource(DccApplicationExecutionContractService.STORAGE_RECOVERY_SOURCE).stream()
                .filter(r->Objects.equals(r.getTenantId(),file.getTenantId())&&Objects.equals(r.getControlledFileId(),file.getId())).toList();
    }

    private void requireActiveRecoveryCategory(Long categoryId,Long taxonomyId){
        var category=categories.selectById(categoryId);
        if(category==null||!Boolean.TRUE.equals(category.getActive())||!Objects.equals(category.getFileTypeTaxonomyId(),taxonomyId)
                ||!Objects.equals(taxonomies.resolveActiveCategoryId(taxonomyId),categoryId))throw invalid("STORAGE_RECOVERY_CATEGORY_INVALID");
    }

    private DccApplicationExecutionContractService.TypeChangeState recoveryState(DccApplicationExecutionContractService.TypeChangeState s,Long directory,Long folder){
        return new DccApplicationExecutionContractService.TypeChangeState(s.formatVersion(),s.requestId(),s.tenantId(),s.actorId(),s.masterId(),s.controlledFileId(),
                s.projectId(),s.fileNumber(),s.sourceOriginalFileName(),s.changeType(),directory,folder,s.attributeRound(),s.sourceApplicationId(),s.sourceAttributeRound(),
                s.rejectedApplicationId(),s.rejectedAttributeRound(),s.rejectedBpm(),s.categoryId(),s.taxonomyId(),s.sourceFileId(),s.sourceSha256(),
                s.previousSourceSha256(),s.approvalMatrixCanonical(),s.preparedWorkflowContract(),s.preparedProcessDefinitionId(),s.preparedDefinitionDigest(),
                s.selectedSignoffDepartmentIdsJson(),s.distributionDepartmentIdsJson());
    }

    private String sha256(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}

    private DccControlledFileDO requireFile(Long actor,Long id,boolean lock){
        return requireFile(actor,id,lock,false);
    }
    private DccControlledFileDO requireFile(Long actor,Long id,boolean lock,boolean storageRecovery){
        var first=id==null?null:files.selectById(id);Long tenant=TenantContextHolder.getRequiredTenantId();
        if(first==null||!Objects.equals(first.getTenantId(),tenant)||first.getDccProjectCodeId()==null||first.getMasterId()==null)throw invalid("SOURCE_INVALID");
        access.assertProjectEditorOrOwner(actor,first.getDccProjectCodeId());
        var project=lock?projects.selectByIdForUpdate(first.getDccProjectCodeId()):projects.selectById(first.getDccProjectCodeId());
        if(project==null||!Objects.equals(project.getTenantId(),tenant)||!"ENABLE".equals(project.getStatus()))throw invalid("PROJECT_INVALID");
        if(lock)placements.lockSourceLocation(actor,id);
        var master=lock?masters.selectByIdForUpdate(first.getMasterId()):masters.selectById(first.getMasterId());
        var file=lock?files.selectByIdAndTenantForUpdate(tenant,id):first;
        if(file==null||master==null||!Objects.equals(file.getTenantId(),tenant)||!Objects.equals(file.getMasterId(),master.getId())
                ||!Objects.equals(master.getTenantId(),tenant)||!Objects.equals(master.getDccProjectCodeId(),file.getDccProjectCodeId())
                ||!Objects.equals(master.getCategoryId(),file.getCategoryId())||!Objects.equals(master.getFileTypeTaxonomyLeafId(),file.getFileTypeTaxonomyId())
                ||!Objects.equals(master.getNormalizedFileNumber(),file.getFileNumber())||!Objects.equals(file.getRequesterId(),actor)
                ||!"WORKING".equals(file.getStatus())||!"NEW".equals(file.getChangeType())||file.getProcessInstanceId()!=null||file.getCheckedOutBy()!=null
                ||master.getLatestControlledFileId()!=null||master.getCurrentActiveControlledFileId()!=null)throw invalid("SOURCE_INVALID");
        var chain=lock?files.selectListByMasterIdForUpdate(master.getId()):files.selectListByMasterId(master.getId());
        if(chain.stream().anyMatch(f->f.getControlledTime()!=null||!"NEW".equals(f.getChangeType())||!Set.of("WORKING","DRAFT","REJECTED","WITHDRAWN").contains(f.getStatus()))
                ||lock&&checkouts.selectActiveByMasterId(tenant,master.getId())!=null)throw invalid("UNRELEASED_NEW_REQUIRED");
        requirePermission(actor,file.getCategoryId());
        var sourceCategory=categories.selectById(file.getCategoryId());
        if(sourceCategory==null||!Boolean.TRUE.equals(sourceCategory.getActive())||!"PUBLIC_MATRIX".equals(sourceCategory.getApprovalSourceMode())
                ||!Objects.equals(taxonomies.resolveActiveCategoryId(file.getFileTypeTaxonomyId()),file.getCategoryId()))throw invalid("SOURCE_CATEGORY_INVALID");
        if(!storageRecovery)requireBinding(file,file.getCategoryId());
        var owned=ownership.selectByControlledFileId(tenant,id);
        if(owned==null||!Objects.equals(owned.getSourceFileId(),file.getSourceFileId())||!Objects.equals(owned.getSourceSha256(),file.getSourceSha256())
                ||!Objects.equals(owned.getClaimedBy(),actor)||!Objects.equals(sourceOwnership.inspectSource(file.getSourceFileId()).sourceSha256(),file.getSourceSha256()))throw invalid("SOURCE_HASH_OR_OWNERSHIP_INVALID");
        return file;
    }

    private Selection select(Long actor,DccControlledFileDO file,DccWorkingApplicationTypeChangeReqVO request,boolean lock){
        if(request==null||request.getTargetFileTypeTaxonomyId()==null||Objects.equals(request.getTargetFileTypeTaxonomyId(),file.getFileTypeTaxonomyId()))throw invalid("TARGET_REQUIRED");
        int round=rounds.requireDraft("UPLOAD",file.getId());var origin=contracts.readRejectedDraftFacts(actor,file);
        if(origin==null)throw invalid("ORIGINAL_REJECTED_SOURCE_REQUIRED");
        var rejected=files.selectById(origin.executedSourceApplicationId());
        if(rejected==null||!"REJECTED".equals(rejected.getStatus())||!"NEW".equals(rejected.getChangeType()))throw invalid("ORIGINAL_REJECTED_SOURCE_REQUIRED");
        contracts.requirePrepared(file,file.getPreparedWorkflowContract(),file.getPreparedProcessDefinitionId(),file.getPreparedDefinitionDigest(),DccControlledFileProcessDefinitionKeys.UPLOAD);
        var path=taxonomies.resolveActivePath(request.getTargetFileTypeTaxonomyId());Long categoryId=taxonomies.resolveActiveCategoryId(path.id());
        var category=lock?categories.selectMatrixCategoryForUpdate(file.getTenantId(),categoryId):categories.selectById(categoryId);
        if(category==null||!Boolean.TRUE.equals(category.getActive())||!Objects.equals(category.getFileTypeTaxonomyId(),path.id())
                ||!"PUBLIC_MATRIX".equals(category.getApprovalSourceMode()))throw invalid("PUBLIC_TARGET_INVALID");
        requirePermission(actor,categoryId);requireTargetBinding(file,categoryId);
        var matrix=lock?matrices.readForUpdate(categoryId):matrices.read(categoryId);
        List<Long> signoff=request.getSelectedSignoffDepartmentIds();
        if(signoff==null){var derived=cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.project(matrix.rawNodes(),"NEW",null);
            signoff=Arrays.stream(derived.nodes().get(0).getCandidateSourceIds().split(",")).map(Long::valueOf).toList();}
        var distribution=request.getSelectedDistributionDepartmentIds()==null?ids(file.getDistributionDepartmentIdsJson()):request.getSelectedDistributionDepartmentIds();
        if(distribution==null||distribution.stream().anyMatch(id->id==null||id<=0)||distribution.stream().distinct().count()!=distribution.size())throw invalid("EXPLICIT_DEPARTMENTS_REQUIRED");
        departments.validateDeptList(distribution);
        var route=resolver.resolvePublicMatrix(matrix,actor,"NEW",signoff);var checked=readiness.evaluateFrozenRoute(route);checked.requireReady();
        return new Selection(file,origin,round,path,categoryId,matrix,List.copyOf(signoff),List.copyOf(distribution),checked.response());
    }
    private void requirePermission(Long actor,Long category){if(!permissions.hasCategoryPermission(category,actor,DccFileCategoryPermissionActionEnum.UPLOAD))throw invalid("UPLOAD_PERMISSION_DENIED");}
    private void requireTargetBinding(DccControlledFileDO file,Long category){
        var location=placementRows.findFile(file.getId());
        if(location==null){requireBinding(file,category);return;}
        if(!Objects.equals(location.getProjectCodeId(),file.getDccProjectCodeId())||!Objects.equals(location.getStorageDirectoryId(),file.getDirectoryId()))throw invalid("SOURCE_PLACEMENT_INVALID");
        var configured=bindings.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO::getCategoryId,category)
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO::getActive,true));
        if(configured.size()!=1)throw invalid("TARGET_BINDING_INVALID");
        var base=directories.selectById(configured.get(0).getDirectoryId());
        if(base==null||!Objects.equals(base.getTenantId(),file.getTenantId())||!Boolean.TRUE.equals(base.getActive()))throw invalid("TARGET_BINDING_INVALID");
        // A registered logical folder will resolve its own target-category directory only on confirm.
    }
    private void requireBinding(DccControlledFileDO file,Long category){
        var binding=bindings.selectActiveByCategoryId(category);if(binding==null||binding.getDirectoryId()==null)throw invalid("TARGET_BINDING_INVALID");
        Long current=file.getDirectoryId();var seen=new HashSet<Long>();
        while(current!=null&&seen.add(current)){
            var directory=directories.selectById(current);
            if(directory==null||!Objects.equals(directory.getTenantId(),file.getTenantId())||!Boolean.TRUE.equals(directory.getActive()))throw invalid("TARGET_BINDING_INVALID");
            if(Objects.equals(current,binding.getDirectoryId()))return;current=directory.getParentId();
        }
        throw invalid("TARGET_BINDING_INVALID");
    }
    private void expectSource(DccWorkingApplicationTypeChangeReqVO r,DccApplicationExecutionContractService.TypeChangeState before){
        if(!Objects.equals(r.getExpectedSourceAttributeRound(),before.attributeRound())||!Objects.equals(r.getExpectedSourceCategoryId(),before.categoryId())
                ||!Objects.equals(r.getExpectedSourceTaxonomyId(),before.taxonomyId())||!Objects.equals(r.getExpectedSourceSha256(),before.sourceSha256())
                ||!Objects.equals(r.getExpectedSourceMatrixDigest(),DccApprovalMatrixSourceService.readCanonical(before.approvalMatrixCanonical()).getMatrixDigest()))throw invalid("SOURCE_EXPECTATION_STALE");
    }
    private void expectMatrix(DccWorkingApplicationTypeChangeReqVO r,DccApprovalMatrixBaselineVO m){
        if(!Objects.equals(r.getExpectedApprovalRouteId(),m.getRouteId())||!Objects.equals(r.getExpectedApprovalRouteVersionNo(),m.getRouteVersionNo())
                ||!Objects.equals(r.getExpectedApprovalMatrixDigest(),m.getMatrixDigest()))throw invalid("TARGET_MATRIX_EXPECTATION_STALE");
    }
    private DccWorkingApplicationTypeChangePreviewVO previewOf(Selection s){
        var before=DccApprovalMatrixSourceService.readCanonical(s.origin().approvalMatrixCanonical());var f=s.file();
        return new DccWorkingApplicationTypeChangePreviewVO(f.getId().toString(),s.round(),before.getCategoryId().toString(),before.getFileTypeTaxonomyId().toString(),f.getSourceSha256(),before.getMatrixDigest(),
                s.targetCategory().toString(),s.path().id().toString(),String.join(" / ",List.of(s.path().level1(),s.path().level2(),s.path().level3(),s.path().level4(),s.path().level5())),
                before,s.matrix().baseline(),strings(s.signoff()),strings(s.distribution()),s.readiness());
    }
    private List<Long> ids(String json){if(json==null)throw invalid("SAVED_DEPARTMENTS_REQUIRED");return JsonUtils.parseArray(json,String.class).stream().map(Long::valueOf).toList();}
    private List<String> strings(List<Long> values){return values.stream().map(String::valueOf).toList();}
    private DccApprovalMatrixSourceService.Source source(String canonical){var b=DccApprovalMatrixSourceService.readCanonical(canonical);
        return new DccApprovalMatrixSourceService.Source(cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(b.getRouteId()).versionNo(b.getRouteVersionNo()).actionType("LEGACY").build(),
                b.getNodes().stream().map(n->JsonUtils.parseObject(JsonUtils.toJsonString(n),cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.class)).toList(),b,canonical);}
}
