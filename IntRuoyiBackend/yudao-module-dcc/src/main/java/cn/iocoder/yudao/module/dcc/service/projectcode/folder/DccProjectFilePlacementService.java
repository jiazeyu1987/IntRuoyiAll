package cn.iocoder.yudao.module.dcc.service.projectcode.folder;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFilePlacementDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

/** A/Root在文件创建或正式新版本事务登记目录位置；无任意fileId公共写入口。 */
@Service
public class DccProjectFilePlacementService {
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccControlledFileMapper files;
    @Resource private DccProjectFilePlacementMapper placements;
    @Resource private DccFolderTemplateService folders;
    @Resource private DccProjectAccessService access;
    @Resource private JdbcTemplate jdbc;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService audit;
    @Resource private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService ledger;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeMapper typeChanges;
    @Resource private DccProjectFolderStorageMappingMapper storageMappings;

    /** PRESENT -> PRESENT is audited by the enclosing exact TYPE_CHANGE proof, never by CREATE bind policy. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void rebindDerivedWorkingType(Long actor,Long fileId,Long oldDirectory,
            cn.iocoder.yudao.module.dcc.service.file.DccDerivedUploadStorage target,Long proofId){
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(target==null||proofId==null||!Objects.equals(target.tenantId,tenant)||!Objects.equals(target.actorId,actor))throw fail(FOLDER_INVALID);
        access.assertProjectEditorOrOwner(actor,target.projectId);folders.requireProjectFolderForUpdate(target.projectId,target.folderId);
        var file=files.selectById(fileId);var current=placements.findFile(fileId);var proof=typeChanges.selectById(proofId);
        if(file==null||current==null||proof==null||!Objects.equals(file.getTenantId(),tenant)||!Objects.equals(file.getRequesterId(),actor)
                ||!"NEW".equals(file.getChangeType())||!"WORKING".equals(file.getStatus())||file.getProcessInstanceId()!=null
                ||!Objects.equals(file.getDccProjectCodeId(),target.projectId)||!Objects.equals(file.getCategoryId(),target.categoryId)
                ||!Objects.equals(file.getDirectoryId(),target.storageDirectoryId)||!Objects.equals(current.getProjectCodeId(),target.projectId)
                ||!Objects.equals(current.getProjectFolderId(),target.folderId)||!Objects.equals(current.getStorageDirectoryId(),oldDirectory)
                ||!Objects.equals(proof.getTenantId(),tenant)||!Objects.equals(proof.getControlledFileId(),fileId)||!Objects.equals(proof.getMasterId(),file.getMasterId())
                ||!Objects.equals(proof.getProjectCodeId(),target.projectId)||!Objects.equals(proof.getOperatorUserId(),actor)
                ||!cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TYPE_CHANGE_SOURCE.equals(proof.getSource())
                ||proof.getRequestId()==null||proof.getRequestId().isBlank()||proof.getChangeReason()==null||proof.getChangeReason().isBlank())throw fail(FOLDER_INVALID);
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(proof.getBeforeSnapshotJson(),cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TypeChangeState.class);
        var after=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(proof.getAfterSnapshotJson(),cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TypeChangeState.class);
        var sourceMapping=storageMappings.findLeaf(tenant,oldDirectory);var destination=storageMappings.findLeaf(tenant,target.storageDirectoryId);
        if(before==null||after==null||!before.sameSourceAs(after)
                ||!cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TYPE_CHANGE_FORMAT.equals(after.formatVersion())
                ||!Objects.equals(before.requestId(),proof.getRequestId())||!Objects.equals(after.requestId(),proof.getRequestId())
                ||!Objects.equals(after.tenantId(),tenant)||!Objects.equals(after.actorId(),actor)||!"NEW".equals(after.changeType())
                ||!Objects.equals(before.controlledFileId(),fileId)||!Objects.equals(after.controlledFileId(),fileId)
                ||!Objects.equals(before.masterId(),file.getMasterId())||!Objects.equals(after.masterId(),file.getMasterId())
                ||!Objects.equals(before.projectId(),target.projectId)||!Objects.equals(after.projectId(),target.projectId)
                ||!Objects.equals(before.projectFolderId(),target.folderId)||!Objects.equals(after.projectFolderId(),target.folderId)
                ||!Objects.equals(before.directoryId(),oldDirectory)||!Objects.equals(after.directoryId(),target.storageDirectoryId)
                ||!Objects.equals(after.categoryId(),target.categoryId)||!Objects.equals(before.sourceFileId(),after.sourceFileId())
                ||!Objects.equals(after.taxonomyId(),file.getFileTypeTaxonomyId())||!Objects.equals(after.approvalMatrixCanonical(),file.getDraftApprovalMatrixJson())
                ||!Objects.equals(after.fileNumber(),file.getFileNumber())||!Objects.equals(after.sourceOriginalFileName(),file.getSourceOriginalFileName())
                ||!Objects.equals(after.sourceFileId(),file.getSourceFileId())||!Objects.equals(before.sourceSha256(),after.sourceSha256())
                ||!Objects.equals(after.sourceSha256(),file.getSourceSha256())||sourceMapping==null||destination==null
                ||Boolean.TRUE.equals(sourceMapping.getDeleted())||Boolean.TRUE.equals(destination.getDeleted())
                ||!Objects.equals(sourceMapping.getCategoryId(),before.categoryId())||!Objects.equals(sourceMapping.getProjectCodeId(),target.projectId)
                ||!Objects.equals(sourceMapping.getProjectFolderId(),target.folderId)||!Objects.equals(destination.getId(),target.mappingId)
                ||!Objects.equals(destination.getCategoryId(),target.categoryId)||!Objects.equals(destination.getProjectCodeId(),target.projectId)
                ||!Objects.equals(destination.getProjectFolderId(),target.folderId)||!Objects.equals(destination.getBaseDirectoryId(),target.baseDirectoryId))throw fail(FOLDER_INVALID);
        if(placements.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccProjectFilePlacementDO>()
                .eq(DccProjectFilePlacementDO::getId,current.getId()).eq(DccProjectFilePlacementDO::getTenantId,tenant)
                .eq(DccProjectFilePlacementDO::getControlledFileId,fileId).eq(DccProjectFilePlacementDO::getProjectCodeId,target.projectId)
                .eq(DccProjectFilePlacementDO::getProjectFolderId,target.folderId).eq(DccProjectFilePlacementDO::getStorageDirectoryId,oldDirectory)
                .set(DccProjectFilePlacementDO::getStorageDirectoryId,target.storageDirectoryId))!=1)throw fail(FOLDER_INVALID);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void rebindRecoveredDerivedWorkingType(Long actor,Long fileId,Long oldDirectory,
            cn.iocoder.yudao.module.dcc.service.file.DccDerivedUploadStorage target,Long originalProofId,Long recoveryEvidenceId){
        Long tenant=TenantContextHolder.getRequiredTenantId();var file=files.selectById(fileId);var current=placements.findFile(fileId);
        var original=typeChanges.selectById(originalProofId);var recovery=typeChanges.selectById(recoveryEvidenceId);
        var originalBefore=original==null?null:cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(original.getBeforeSnapshotJson(),
                cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TypeChangeState.class);
        var rows=typeChanges.selectListBySource(cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.STORAGE_RECOVERY_SOURCE);
        var envelope=recovery==null?null:cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(recovery.getAfterSnapshotJson(),
                cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.StorageRecoveryEnvelope.class);
        var oldMapping=storageMappings.findLeaf(tenant,oldDirectory);var newMapping=target==null?null:storageMappings.findLeaf(tenant,target.storageDirectoryId);
        if(target==null||file==null||current==null||original==null||recovery==null||envelope==null||envelope.observed()==null
                ||!Objects.equals(target.tenantId,tenant)||!Objects.equals(target.actorId,actor)||!Objects.equals(file.getRequesterId(),actor)
                ||!Objects.equals(file.getDirectoryId(),target.storageDirectoryId)||!Objects.equals(current.getProjectCodeId(),target.projectId)
                ||!Objects.equals(current.getProjectFolderId(),target.folderId)||!Objects.equals(current.getStorageDirectoryId(),oldDirectory)
                ||!Objects.equals(original.getSource(),cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.TYPE_CHANGE_SOURCE)
                ||!Objects.equals(recovery.getSource(),cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.STORAGE_RECOVERY_SOURCE)
                ||!Objects.equals(recovery.getTenantId(),tenant)||!Objects.equals(recovery.getControlledFileId(),fileId)
                ||!Objects.equals(recovery.getOperatorUserId(),actor)||!Objects.equals(envelope.originalProofId(),originalProofId)
                ||!Objects.equals(envelope.originalProofSha256(),cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractService.storageRecoveryDigest(original))
                ||!Objects.equals(envelope.observed().directoryId(),target.storageDirectoryId)
                ||!Objects.equals(envelope.observed().projectFolderId(),target.folderId)
                ||rows.stream().filter(r->Objects.equals(r.getTenantId(),tenant)&&Objects.equals(r.getControlledFileId(),fileId)).count()!=1
                ||oldMapping==null||newMapping==null||Boolean.TRUE.equals(oldMapping.getDeleted())||Boolean.TRUE.equals(newMapping.getDeleted())
                ||originalBefore==null||!Objects.equals(oldMapping.getCategoryId(),originalBefore.categoryId())
                ||!Objects.equals(oldMapping.getProjectFolderId(),target.folderId)||!Objects.equals(newMapping.getId(),target.mappingId)
                ||!Objects.equals(newMapping.getCategoryId(),target.categoryId)||!Objects.equals(newMapping.getProjectFolderId(),target.folderId))throw fail(FOLDER_INVALID);
        if(placements.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccProjectFilePlacementDO>()
                .eq(DccProjectFilePlacementDO::getId,current.getId()).eq(DccProjectFilePlacementDO::getTenantId,tenant)
                .eq(DccProjectFilePlacementDO::getControlledFileId,fileId).eq(DccProjectFilePlacementDO::getProjectCodeId,target.projectId)
                .eq(DccProjectFilePlacementDO::getProjectFolderId,target.folderId).eq(DccProjectFilePlacementDO::getStorageDirectoryId,oldDirectory)
                .set(DccProjectFilePlacementDO::getStorageDirectoryId,target.storageDirectoryId))!=1)throw fail(FOLDER_INVALID);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectFilePlacementDO bindDerived(Long userId,Long projectId,Long folderId,Long fileId,Long directoryId,String reason,
            cn.iocoder.yudao.module.dcc.service.file.DccDerivedUploadStorage storage) {
        if(storage==null) throw fail(FOLDER_INVALID);
        return bind(userId,projectId,folderId,fileId,directoryId,reason,storage);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectFilePlacementDO bind(Long userId, Long projectId, Long projectFolderId,
                                          Long controlledFileId, Long storageDirectoryId, String reason) {
        return bind(userId,projectId,projectFolderId,controlledFileId,storageDirectoryId,reason,null);
    }
    private DccProjectFilePlacementDO bind(Long userId,Long projectId,Long projectFolderId,Long controlledFileId,Long storageDirectoryId,String reason,
            cn.iocoder.yudao.module.dcc.service.file.DccDerivedUploadStorage storage) {
        String actualReason;
        if(storage==null) {
            audit.validateReason(reason);
            actualReason=reason.trim();
        } else {
            actualReason=reason==null?"":reason.trim();
            if(actualReason.length()>500) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(
                    INVALID.getCode(),"文件夹登记说明不能超过500字");
        }
        access.assertProjectEditorOrOwner(userId, projectId);
        var project = projects.selectByIdForUpdate(projectId);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (project == null || !Objects.equals(project.getTenantId(), tenant)) throw fail(FOLDER_INVALID);
        folders.requireProjectFolderForUpdate(projectId, projectFolderId);
        var file = controlledFileId == null ? null : files.selectById(controlledFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenant)
                || !Objects.equals(file.getDccProjectCodeId(), projectId)
                || storageDirectoryId == null || !Objects.equals(file.getDirectoryId(), storageDirectoryId)) throw fail(FOLDER_INVALID);
        if(storage!=null && (!Objects.equals(storage.tenantId,tenant) || !Objects.equals(storage.actorId,userId)
                || !Objects.equals(storage.projectId,projectId) || !Objects.equals(storage.folderId,projectFolderId)
                || !Objects.equals(storage.storageDirectoryId,storageDirectoryId) || !Objects.equals(storage.categoryId,file.getCategoryId())))
            throw fail(FOLDER_INVALID);
        var existing = placements.findFile(controlledFileId);
        if (existing != null) {
            if (!Objects.equals(existing.getProjectCodeId(), projectId)
                    || !Objects.equals(existing.getProjectFolderId(), projectFolderId)
                    || !Objects.equals(existing.getStorageDirectoryId(), storageDirectoryId)) throw fail(FOLDER_INVALID);
            return existing;
        }
        // 原存储目录的真实ID及租户事实；目录名称/数字碰巧相等/模板节点不参与推断。
        var directory = jdbc.queryForList("SELECT id FROM dcc_file_directory WHERE tenant_id=? AND id=? AND active=1 AND deleted=0",
                Long.class, tenant, storageDirectoryId);
        if (directory.size() != 1) throw fail(FOLDER_INVALID);
        var saved = new DccProjectFilePlacementDO();
        saved.setTenantId(tenant); saved.setProjectCodeId(projectId); saved.setProjectFolderId(projectFolderId);
        saved.setControlledFileId(controlledFileId); saved.setStorageDirectoryId(storageDirectoryId);
        if (placements.insert(saved) != 1) throw fail(FOLDER_INVALID);
        var state=new LinkedHashMap<String,Object>();state.put("placement",placements.findFile(controlledFileId));
        if(storage!=null) {
            state.put("mappingId",storage.mappingId);state.put("baseDirectoryId",storage.baseDirectoryId);
            state.put("storageDirectoryId",storage.storageDirectoryId);state.put("mappingCreated",storage.created);
        }
        ledger.append(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.builder()
                .operationId(storage==null?"dcc.project-file-placement.bind":"dcc.project-file-placement.upload-bind")
                .subjectId("DCC_PROJECT_FILE_PLACEMENT:"+saved.getId())
                .subjectVersion("1").reason(actualReason)
                .beforeState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder()
                        .state("ABSENT").canonicalJson("{}").build())
                .afterState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder()
                        .state("PRESENT").objectVersion("1")
                        .canonicalJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(storage==null?placements.findFile(controlledFileId):state)).build())
                .idempotencyKey("DCC:PROJECT_FILE_PLACEMENT:"+saved.getId())
                .source(storage==null?"DccProjectFilePlacementService.bind":"DccProjectFilePlacementService.bindDerived").build());
        return saved;
    }

    public DccProjectFilePlacementDO require(Long userId, Long projectId, Long controlledFileId) {
        access.assertProjectEditorOrOwner(userId, projectId);
        var row = placements.findFile(controlledFileId);
        if (row == null || !Objects.equals(row.getProjectCodeId(), projectId)) throw fail(FOLDER_INVALID);
        return row; // 历史位置只取本版本记录，不能用当前项目/目录配置回填
    }
    public List<DccProjectFilePlacementDO> list(Long userId, Long projectId, Long folderId) {
        access.assertProjectEditorOrOwner(userId, projectId);
        folders.requireProjectFolder(projectId, folderId);
        return placements.listFolder(projectId, folderId);
    }
}
