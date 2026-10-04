package cn.iocoder.yudao.module.dcc.service.projectcode.folder;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class DccFolderTemplateService {
    @Resource private DccFolderTemplateMapper templateMapper;
    @Resource private DccProjectFolderMapper folderMapper;
    @Resource private DccProjectCodeMapper projectMapper;
    @Resource private PermissionApi permissionApi;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;
    @Resource private DccFolderTemplateHistoryMapper historyMapper;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService auditService;

    public record Save(Long id, String name, String description, Boolean active, DccFolderTemplateStructure structure, String changeReason) {}
    public List<DccFolderTemplateDO> list() { return templateMapper.selectList().stream().filter(row -> Objects.equals(row.getTenantId(), TenantContextHolder.getRequiredTenantId())).toList(); }
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long userId, Save input) {
        assertEditor(userId);
        if (input == null) throw fail(TEMPLATE_INVALID);
        auditService.validateReason(input.changeReason());
        if (input == null || input.name() == null || input.name().isBlank() || input.name().length() > 128
                || input.active() == null || input.structure() == null
                || (input.description() != null && input.description().length() > 2048)) throw fail(TEMPLATE_INVALID);
        input.structure().validated();
        DccFolderTemplateDO row;
        String beforeJson = null;
        if (input.id() == null) {
            row = new DccFolderTemplateDO(); row.setEverUsed(false); row.setTenantId(TenantContextHolder.getRequiredTenantId());
        } else {
            row = templateMapper.selectByIdForUpdate(input.id());
            if (row == null || !Objects.equals(row.getTenantId(), TenantContextHolder.getRequiredTenantId())) throw fail(TEMPLATE_INVALID);
            beforeJson = JsonUtils.toJsonString(row);
        }
        row.setName(input.name().trim()); row.setDescription(input.description());
        row.setActive(input.active()); row.setStructureJson(JsonUtils.toJsonString(input.structure()));
        row.setEditedByUserId(userId);
        int written = row.getId() == null ? templateMapper.insert(row) : templateMapper.updateById(row);
        if (written != 1) throw fail(TEMPLATE_INVALID);
        DccFolderTemplateDO persisted = templateMapper.selectById(row.getId());
        if (persisted == null) throw fail(TEMPLATE_INVALID);
        String afterJson = JsonUtils.toJsonString(persisted);
        Long historyId = recordHistory(userId, row.getId(), "SAVE", beforeJson, afterJson);
        auditService.append("dcc.folder-template.save", row.getId(), String.valueOf(historyId), input.changeReason(), beforeJson, afterJson);
        return row.getId();
    }
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id, String reason) {
        assertEditor(userId);
        auditService.validateReason(reason);
        var row = templateMapper.selectByIdForUpdate(id);
        if (row == null || !Objects.equals(row.getTenantId(), TenantContextHolder.getRequiredTenantId())) throw fail(TEMPLATE_INVALID);
        if (Boolean.TRUE.equals(row.getEverUsed())) throw fail(TEMPLATE_USED);
        if (templateMapper.deleteById(id) != 1) throw exception(WRITE_INCOMPLETE, "模板删除");
        String before = JsonUtils.toJsonString(row);
        Long historyId = recordHistory(userId, id, "DELETE", before, null);
        auditService.append("dcc.folder-template.delete", id, String.valueOf(historyId), reason, before, null);
    }
    /** 项目申请提交时冻结，不在批准时重新查当前模板；保留使用历史。 */
    @Transactional(rollbackFor = Exception.class)
    public String captureForRequest(Long templateId) {
        var row = templateMapper.selectByIdForUpdate(templateId);
        if (row == null || !Objects.equals(row.getTenantId(), TenantContextHolder.getRequiredTenantId()) || !Boolean.TRUE.equals(row.getActive())) throw fail(TEMPLATE_INVALID);
        parse(row.getStructureJson());
        if (!Boolean.TRUE.equals(row.getEverUsed())) {
            row.setEverUsed(true);
            if (templateMapper.updateById(row) != 1) throw exception(WRITE_INCOMPLETE, "模板使用历史");
        }
        return row.getStructureJson();
    }
    public DccFolderTemplateStructure parse(String json) {
        if (json == null || json.isBlank()) throw fail(TEMPLATE_INVALID);
        var structure = JsonUtils.parseObject(json, DccFolderTemplateStructure.class);
        if (structure == null) throw fail(TEMPLATE_INVALID);
        return structure.validated();
    }
    /** 必须加入项目创建事务；独立 ID，不自动同步模板、不级联操作公共文件。 */
    @Transactional(rollbackFor = Exception.class)
    public List<DccProjectFolderDO> generate(Long projectId, Long templateId, String snapshotJson) {
        var project = projectMapper.selectByIdForUpdate(projectId);
        if (project == null || !Objects.equals(project.getTenantId(), TenantContextHolder.getRequiredTenantId()) || templateId == null) throw fail(FOLDER_INVALID);
        var nodes = parse(snapshotJson).parentFirst();
        if (!folderMapper.listByProject(projectId).isEmpty()) throw fail(FOLDER_INVALID);
        Map<String, Long> generatedIds = new HashMap<>();
        List<DccProjectFolderDO> result = new ArrayList<>();
        for (var node : nodes) {
            var folder = new DccProjectFolderDO();
            folder.setTenantId(TenantContextHolder.getRequiredTenantId());
            folder.setProjectCodeId(projectId); folder.setName(node.name().trim());
            folder.setParentId(node.parentKey() == null ? 0L : generatedIds.get(node.parentKey()));
            folder.setSortOrder(node.sortOrder()); folder.setActive(true);
            folder.setSourceTemplateId(templateId); folder.setSourceNodeKey(node.key());
            if (folderMapper.insert(folder) != 1 || folder.getId() == null) throw exception(WRITE_INCOMPLETE, "项目目录");
            generatedIds.put(node.key(), folder.getId()); result.add(folder);
        }
        return result;
    }
    public List<DccProjectFolderDO> listProjectFolders(Long projectId) { return folderMapper.listByProject(projectId); }
    /** 返回真实项目目录，包括尚无文件的空目录；不从文件列表/NAS推断，也不写业务事实。 */
    @Transactional(readOnly = true)
    public List<DccProjectFolderDO> readProjectFolders(Long userId, Long projectId) {
        projectAccess.assertProjectViewerOrAbove(userId, projectId);
        return listProjectFolders(projectId);
    }
    public DccProjectFolderDO requireProjectFolder(Long projectId, Long folderId) {
        var folder = folderId == null ? null : folderMapper.selectById(folderId);
        if (folder == null || !Objects.equals(folder.getTenantId(), TenantContextHolder.getRequiredTenantId()) || !Objects.equals(folder.getProjectCodeId(), projectId)
                || !Boolean.TRUE.equals(folder.getActive())) throw fail(FOLDER_INVALID);
        return folder;
    }
    private void assertEditor(Long userId) {
        // 复用既有项目配置 update 权限，不自行猜新角色或创建审批。
        if (userId == null || !permissionApi.hasAnyPermissions(userId, "dcc:project-code:update")) throw fail(TEMPLATE_FORBIDDEN);
    }
    private Long recordHistory(Long userId, Long id, String operation, String before, String after) {
        var history = new DccFolderTemplateHistoryDO();
        history.setTenantId(TenantContextHolder.getRequiredTenantId());
        history.setTemplateId(id); history.setOperatorUserId(userId); history.setOperation(operation);
        history.setBeforeJson(before); history.setAfterJson(after);
        if (historyMapper.insert(history) != 1 || history.getId() == null) {
            throw exception(WRITE_INCOMPLETE, "模板变更历史");
        } // 与模板修改同一事务，失败必须回滚，不吞异常
        return history.getId();
    }
}
