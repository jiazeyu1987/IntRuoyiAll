package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
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
    @Resource private DccProjectConfigurationAuditService audit;
    @Resource private DccProjectFilePlacementService placements;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFilePlacementMapper placementRows;

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
        String reason = request.getProjectFolderChangeReason();
        audit.validateReason(reason);
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
