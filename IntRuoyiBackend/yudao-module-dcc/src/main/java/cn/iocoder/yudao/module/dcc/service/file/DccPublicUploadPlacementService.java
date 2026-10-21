package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.function.Supplier;
import java.util.Objects;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

/** Public upload orchestration; A owns file creation and B owns exact project placement. */
@Service
public class DccPublicUploadPlacementService {
    @Resource private DccProjectFolderStorageService storage;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProjectFolderMapper folders;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper files;
    @Resource private DccProjectAccessService access;
    @Resource private DccProjectFilePlacementService placements;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFilePlacementMapper placementRows;
    @Resource private DccProjectFolderStorageMappingMapper storageMappings;

    /** Resolve the target only from the source version's registered logical folder; never infer one. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccDerivedUploadStorage resolveWorkingTypeChange(Long actor,Long sourceId,Long targetCategory,Long targetType){
        Long tenant=TenantContextHolder.getRequiredTenantId();var source=files.selectById(sourceId);
        if(source==null||!Objects.equals(source.getTenantId(),tenant)||!Objects.equals(source.getRequesterId(),actor)
                ||!"NEW".equals(source.getChangeType())||!"WORKING".equals(source.getStatus())||source.getProcessInstanceId()!=null)throw fail(FOLDER_INVALID);
        var location=placementRows.findFile(sourceId);var oldMapping=storageMappings.findLeaf(tenant,source.getDirectoryId());
        if(location==null){if(oldMapping!=null)throw fail(FOLDER_INVALID);return null;} // Explicit physical history has no logical-folder fact to migrate.
        if(oldMapping==null||Boolean.TRUE.equals(oldMapping.getDeleted())||!Objects.equals(location.getProjectCodeId(),source.getDccProjectCodeId())
                ||!Objects.equals(location.getStorageDirectoryId(),source.getDirectoryId())||!Objects.equals(oldMapping.getCategoryId(),source.getCategoryId())
                ||!Objects.equals(oldMapping.getProjectCodeId(),location.getProjectCodeId())||!Objects.equals(oldMapping.getProjectFolderId(),location.getProjectFolderId()))throw fail(FOLDER_INVALID);
        access.assertProjectEditorOrOwner(actor,source.getDccProjectCodeId());var project=projects.selectByIdForUpdate(source.getDccProjectCodeId());
        var folder=folders.selectOne(new LambdaQueryWrapperX<DccProjectFolderDO>().eq(DccProjectFolderDO::getId,location.getProjectFolderId())
                .eq(DccProjectFolderDO::getProjectCodeId,source.getDccProjectCodeId()).eq(DccProjectFolderDO::getTenantId,tenant).last("FOR UPDATE"));
        if(project==null||folder==null)throw fail(FOLDER_INVALID);
        return storage.resolve(actor,project,folder,targetCategory,targetType);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccDerivedUploadStorage resolveWorkingStorageRecovery(Long actor,Long currentFileId,Long sourceFileId,
            Long sourceCategory,Long targetCategory,Long targetType,Long expectedFolder){
        Long tenant=TenantContextHolder.getRequiredTenantId();var current=files.selectById(currentFileId);var source=files.selectById(sourceFileId);
        var location=placementRows.findFile(currentFileId);var sourceLocation=placementRows.findFile(sourceFileId);
        var oldMapping=current==null?null:storageMappings.findLeaf(tenant,current.getDirectoryId());
        if(current==null||source==null||location==null||sourceLocation==null||oldMapping==null||Boolean.TRUE.equals(oldMapping.getDeleted())
                ||!Objects.equals(current.getTenantId(),tenant)||!Objects.equals(current.getRequesterId(),actor)
                ||!Objects.equals(source.getTenantId(),tenant)||!Objects.equals(source.getDccProjectCodeId(),current.getDccProjectCodeId())
                ||!Objects.equals(location.getProjectCodeId(),current.getDccProjectCodeId())||!Objects.equals(location.getProjectFolderId(),expectedFolder)
                ||!Objects.equals(location.getStorageDirectoryId(),current.getDirectoryId())
                ||!Objects.equals(sourceLocation.getProjectCodeId(),current.getDccProjectCodeId())
                ||!Objects.equals(sourceLocation.getProjectFolderId(),expectedFolder)||!Objects.equals(sourceLocation.getStorageDirectoryId(),source.getDirectoryId())
                ||!Objects.equals(source.getCategoryId(),sourceCategory)||!Objects.equals(oldMapping.getCategoryId(),sourceCategory)
                ||!Objects.equals(oldMapping.getProjectCodeId(),current.getDccProjectCodeId())
                ||!Objects.equals(oldMapping.getProjectFolderId(),expectedFolder))throw fail(FOLDER_INVALID);
        access.assertProjectEditorOrOwner(actor,current.getDccProjectCodeId());var project=projects.selectByIdForUpdate(current.getDccProjectCodeId());
        var folder=folders.selectOne(new LambdaQueryWrapperX<DccProjectFolderDO>().eq(DccProjectFolderDO::getId,expectedFolder)
                .eq(DccProjectFolderDO::getProjectCodeId,current.getDccProjectCodeId()).eq(DccProjectFolderDO::getTenantId,tenant).last("FOR UPDATE"));
        if(project==null||folder==null||!Boolean.TRUE.equals(folder.getActive()))throw fail(FOLDER_INVALID);
        return storage.resolve(actor,project,folder,targetCategory,targetType);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void rebindWorkingTypeChange(Long actor,Long fileId,Long beforeDirectory,DccDerivedUploadStorage target,Long typeChangeProofId){
        if(target==null)throw fail(FOLDER_INVALID);
        placements.rebindDerivedWorkingType(actor,fileId,beforeDirectory,target,typeChangeProofId);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void rebindWorkingStorageRecovery(Long actor,Long fileId,Long beforeDirectory,DccDerivedUploadStorage target,
            Long originalProofId,Long recoveryEvidenceId){
        placements.rebindRecoveredDerivedWorkingType(actor,fileId,beforeDirectory,target,originalProofId,recoveryEvidenceId);
    }

    /** Acquire project and exact registered folder before callers acquire Master/File locks. */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void lockSourceLocation(Long actorId,Long sourceFileId) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var source=sourceFileId==null?null:files.selectById(sourceFileId);
        if(source==null || !Objects.equals(source.getTenantId(),tenant) || source.getDccProjectCodeId()==null) throw fail(FOLDER_INVALID);
        access.assertProjectEditorOrOwner(actorId,source.getDccProjectCodeId());
        var project=projects.selectByIdForUpdate(source.getDccProjectCodeId());
        if(project==null || !Objects.equals(project.getTenantId(),tenant) || !"ENABLE".equals(project.getStatus())) throw fail(FOLDER_INVALID);
        var location=placementRows.findFile(sourceFileId);
        if(location==null) return; // Explicit historical absence; no mapping from NAS or names.
        if(!Objects.equals(location.getProjectCodeId(),source.getDccProjectCodeId())
                || !Objects.equals(location.getStorageDirectoryId(),source.getDirectoryId())) throw fail(FOLDER_INVALID);
        var folder=folders.selectOne(new LambdaQueryWrapperX<DccProjectFolderDO>()
                .eq(DccProjectFolderDO::getId,location.getProjectFolderId()).eq(DccProjectFolderDO::getTenantId,tenant)
                .eq(DccProjectFolderDO::getProjectCodeId,source.getDccProjectCodeId()).last("FOR UPDATE"));
        if(folder==null || !Boolean.TRUE.equals(folder.getActive())) throw fail(FOLDER_INVALID);
    }

    /** Inherit only the source version's recorded location; B remains the sole binding writer. */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void inherit(Long actorId,Long sourceFileId,Long targetFileId,String reason) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var source=files.selectById(sourceFileId);var target=files.selectById(targetFileId);
        if(source==null || target==null || !Objects.equals(source.getTenantId(),tenant) || !Objects.equals(target.getTenantId(),tenant)
                || Objects.equals(sourceFileId,targetFileId) || !Objects.equals(source.getMasterId(),target.getMasterId())
                || !Objects.equals(source.getDccProjectCodeId(),target.getDccProjectCodeId())
                || !Objects.equals(target.getPredecessorControlledFileId(),sourceFileId)) throw fail(FOLDER_INVALID);
        var location=placementRows.findFile(sourceFileId);
        if(location==null) {
            if(placementRows.findFile(targetFileId)!=null) throw fail(FOLDER_INVALID);
            return;
        }
        if(!Objects.equals(location.getProjectCodeId(),source.getDccProjectCodeId())
                || !Objects.equals(location.getStorageDirectoryId(),source.getDirectoryId())) throw fail(FOLDER_INVALID);
        placements.bind(actorId,target.getDccProjectCodeId(),location.getProjectFolderId(),targetFileId,target.getDirectoryId(),reason);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Long create(Long actorId, DccControlledFileSubmitReqVO request, Supplier<Long> createFile) {
        return create(actorId,request,context->createFile.get());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Long create(Long actorId, DccControlledFileSubmitReqVO request,
                       java.util.function.Function<DccDerivedUploadStorage,Long> createFile) {
        if (request == null || createFile == null || actorId == null || actorId <= 0) throw fail(FOLDER_INVALID);
        // External review keeps its own existing endpoint and does not fabricate a controlled-file placement.
        if ("EXTERNAL_REVIEW".equals(request.getProcessType())) return createFile.apply(null);
        if (request.getDirectoryId()!=null || request.isDirectoryIdProvided()
                || !"NEW".equals(request.getChangeType())
                || !(request.getProcessType()==null || request.getProcessType().isBlank() || "CONTROLLED_FILE".equals(request.getProcessType()))) throw fail(FOLDER_INVALID);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        Long projectId = request.getDccProjectCodeId();
        Long folderId = request.getProjectFolderId();
        if (projectId == null || projectId <= 0 || folderId == null || folderId <= 0) throw fail(FOLDER_INVALID);
        String reason = request.getProjectFolderChangeReason() == null ? "" : request.getProjectFolderChangeReason().trim();
        if (reason.length() > 500) throw new cn.iocoder.yudao.framework.common.exception.ServiceException(
                INVALID.getCode(), "文件夹登记说明不能超过500字");
        access.assertProjectViewerOrAbove(actorId, projectId);
        access.assertProjectEditorOrOwner(actorId, projectId);
        var project = projects.selectByIdForUpdate(projectId);
        if (project == null || !Objects.equals(project.getTenantId(), tenant) || !"ENABLE".equals(project.getStatus()))
            throw fail(FOLDER_INVALID);
        // Lock logical folder before A obtains Master/File, sharing D's project -> folder -> Master order.
        var folder = folders.selectOne(new LambdaQueryWrapperX<DccProjectFolderDO>()
                .eq(DccProjectFolderDO::getId, folderId).eq(DccProjectFolderDO::getTenantId, tenant)
                .eq(DccProjectFolderDO::getProjectCodeId, projectId).last("FOR UPDATE"));
        if (folder == null || !Boolean.TRUE.equals(folder.getActive())) throw fail(FOLDER_INVALID);
        var context=storage.resolve(actorId,project,folder,request.getCategoryId(),request.getFileTypeTaxonomyId());
        Long fileId = createFile.apply(context);
        var file = fileId == null ? null : files.selectById(fileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenant)
                || !Objects.equals(file.getDccProjectCodeId(), projectId) || !Objects.equals(file.getDirectoryId(),context.storageDirectoryId)
                || !Objects.equals(file.getCategoryId(),context.categoryId))
            throw fail(FOLDER_INVALID);
        placements.bindDerived(actorId, projectId, folderId, fileId, file.getDirectoryId(), reason,context);
        return fileId;
    }
}
