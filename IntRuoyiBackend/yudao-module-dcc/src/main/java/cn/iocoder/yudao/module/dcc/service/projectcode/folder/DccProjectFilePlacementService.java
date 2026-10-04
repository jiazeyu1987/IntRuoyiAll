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

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectFilePlacementDO bindDerived(Long userId,Long projectId,Long folderId,Long fileId,Long directoryId,String reason,
            cn.iocoder.yudao.module.dcc.service.file.DccDerivedUploadStorage storage) {
        return bind(userId,projectId,folderId,fileId,directoryId,reason,storage);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectFilePlacementDO bind(Long userId, Long projectId, Long projectFolderId,
                                          Long controlledFileId, Long storageDirectoryId, String reason) {
        return bind(userId,projectId,projectFolderId,controlledFileId,storageDirectoryId,reason,null);
    }
    private DccProjectFilePlacementDO bind(Long userId,Long projectId,Long projectFolderId,Long controlledFileId,Long storageDirectoryId,String reason,
            cn.iocoder.yudao.module.dcc.service.file.DccDerivedUploadStorage storage) {
        audit.validateReason(reason);
        access.assertProjectEditorOrOwner(userId, projectId);
        var project = projects.selectByIdForUpdate(projectId);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (project == null || !Objects.equals(project.getTenantId(), tenant)) throw fail(FOLDER_INVALID);
        folders.requireProjectFolder(projectId, projectFolderId);
        var file = controlledFileId == null ? null : files.selectById(controlledFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenant)
                || !Objects.equals(file.getDccProjectCodeId(), projectId)
                || storageDirectoryId == null || !Objects.equals(file.getDirectoryId(), storageDirectoryId)) throw fail(FOLDER_INVALID);
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
            if(!Objects.equals(storage.tenantId,tenant) || !Objects.equals(storage.actorId,userId)
              || !Objects.equals(storage.projectId,projectId) || !Objects.equals(storage.folderId,projectFolderId)
              || !Objects.equals(storage.storageDirectoryId,storageDirectoryId) || !Objects.equals(storage.categoryId,file.getCategoryId())) throw fail(FOLDER_INVALID);
            state.put("mappingId",storage.mappingId);state.put("baseDirectoryId",storage.baseDirectoryId);
            state.put("storageDirectoryId",storage.storageDirectoryId);state.put("mappingCreated",storage.created);
        }
        ledger.append(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.builder()
                .operationId("dcc.project-file-placement.bind").subjectId("DCC_PROJECT_FILE_PLACEMENT:"+saved.getId())
                .subjectVersion("1").reason(reason.trim())
                .beforeState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder()
                        .state("ABSENT").canonicalJson("{}").build())
                .afterState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder()
                        .state("PRESENT").objectVersion("1")
                        .canonicalJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(storage==null?placements.findFile(controlledFileId):state)).build())
                .idempotencyKey("DCC:PROJECT_FILE_PLACEMENT:"+saved.getId())
                .source("DccProjectFilePlacementService.bind").build());
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
