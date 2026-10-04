package cn.iocoder.yudao.module.dcc.service.projectcode.folder;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** 项目逻辑目录的显式维护；不改模板来源、NAS目录、文件位置或引用。 */
@Service
public class DccProjectFolderMaintenanceService {
    public record Save(Long id, Long parentId, String name, Integer sortOrder, String changeReason) {}
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProjectFolderMapper folders;
    @Resource private DccProjectAccessService access;
    @Resource private PermissionApi permissions;
    @Resource private DccProjectConfigurationAuditService reasons;
    @Resource private GxpAuditService audit;
    @Resource private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /** CC-2逻辑删除：D同一project→folder锁，历史身份与审计保留，无物理级联。 */
    @Transactional(rollbackFor=Exception.class)
    public void delete(Long userId,Long projectId,Long folderId,boolean confirmed,String reason) {
        if(userId==null || !permissions.hasAnyPermissions(userId,"dcc:project-code:update"))throw fail(TEMPLATE_FORBIDDEN);
        access.assertProjectEditorOrOwner(userId,projectId);reasons.validateReason(reason);
        if(!confirmed || folderId==null || folderId<=0)throw fail(FOLDER_INVALID);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var project=projectId==null?null:projects.selectByIdForUpdate(projectId);
        if(project==null || !Objects.equals(project.getTenantId(),tenant) || !"ENABLE".equals(project.getStatus()))throw fail(FOLDER_INVALID);
        var ids=jdbc.queryForList("SELECT id FROM dcc_project_folder WHERE tenant_id=? AND project_code_id=? AND id=? AND deleted=0 FOR UPDATE",
                Long.class,tenant,projectId,folderId);
        if(ids.size()!=1)throw fail(FOLDER_INVALID);
        var before=folders.selectById(folderId);
        if(before==null || !Boolean.TRUE.equals(before.getActive()))throw fail(FOLDER_INVALID);
        if(!jdbc.queryForList("SELECT id FROM dcc_project_folder WHERE tenant_id=? AND project_code_id=? AND parent_id=? AND deleted=0 FOR UPDATE",
                Long.class,tenant,projectId,folderId).isEmpty()
                || !jdbc.queryForList("SELECT id FROM dcc_project_file_placement WHERE tenant_id=? AND project_code_id=? AND project_folder_id=? FOR UPDATE",
                Long.class,tenant,projectId,folderId).isEmpty()
                || !jdbc.queryForList("SELECT id FROM dcc_project_file_reference WHERE tenant_id=? AND project_id=? AND folder_id=? FOR UPDATE",
                Long.class,tenant,projectId,folderId).isEmpty())throw fail(FOLDER_USED);
        if(jdbc.update("UPDATE dcc_project_folder SET deleted=1,active=0 WHERE tenant_id=? AND project_code_id=? AND id=? AND deleted=0",
                tenant,projectId,folderId)!=1)throw exception(WRITE_INCOMPLETE,"项目目录逻辑删除");
        var after=jdbc.queryForMap("SELECT * FROM dcc_project_folder WHERE tenant_id=? AND project_code_id=? AND id=?",tenant,projectId,folderId);
        String version=UUID.randomUUID().toString();
        audit.append(GxpAuditCommand.builder().operationId("dcc.project-folder.delete").subjectId("DCC_PROJECT_FOLDER:"+folderId)
                .subjectVersion(version).reason(reason.trim()).idempotencyKey("DCC:B:FOLDER:DELETE:"+version)
                .source("DccProjectFolderMaintenanceService.delete")
                .beforeState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion(version).canonicalJson(JsonUtils.toJsonString(before)).build())
                .afterState(GxpAuditStateEnvelope.builder().state("VOIDED").objectVersion(version).canonicalJson(JsonUtils.toJsonString(after)).build()).build());
    }

    @Transactional(rollbackFor=Exception.class)
    public DccProjectFolderDO save(Long userId, Long projectId, Save input) {
        if (userId==null || !permissions.hasAnyPermissions(userId,"dcc:project-code:update")) throw fail(TEMPLATE_FORBIDDEN);
        access.assertProjectEditorOrOwner(userId,projectId);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var project=projectId==null?null:projects.selectByIdForUpdate(projectId);
        if(project==null || !Objects.equals(project.getTenantId(),tenant) || !"ENABLE".equals(project.getStatus())) throw fail(FOLDER_INVALID);
        if(input==null || input.name()==null || input.name().isBlank() || input.name().length()>128
                || input.sortOrder()==null || input.sortOrder()<0 || (input.id()!=null && input.id()<=0)
                || (input.parentId()!=null && input.parentId()<0)) throw fail(FOLDER_INVALID);
        reasons.validateReason(input.changeReason());
        Long parent=input.parentId()==null?0L:input.parentId();
        var rows=folders.listByProject(projectId);
        Map<Long,DccProjectFolderDO> byId=new HashMap<>();
        for(var row:rows) {
            if(!Objects.equals(row.getTenantId(),tenant) || byId.put(row.getId(),row)!=null) throw fail(FOLDER_INVALID);
        }
        DccProjectFolderDO previous=input.id()==null?null:byId.get(input.id());
        if(input.id()!=null && (previous==null || !Boolean.TRUE.equals(previous.getActive()))) throw fail(FOLDER_INVALID);
        Set<Long> visited=new HashSet<>();
        if(input.id()!=null) visited.add(input.id());
        Long cursor=parent;
        while(!Long.valueOf(0).equals(cursor)) {
            if(cursor==null || !visited.add(cursor)) throw fail(FOLDER_INVALID);
            var ancestor=byId.get(cursor);
            if(ancestor==null || !Boolean.TRUE.equals(ancestor.getActive())) throw fail(FOLDER_INVALID);
            cursor=ancestor.getParentId();
        }
        String name=input.name().trim();
        if(rows.stream().anyMatch(row->!Objects.equals(row.getId(),input.id())
                && Objects.equals(row.getParentId(),parent) && name.equals(row.getName().trim()))) throw fail(FOLDER_INVALID);
        String before=previous==null?null:JsonUtils.toJsonString(previous);
        Long id;
        if(previous==null) {
            var row=new DccProjectFolderDO();row.setTenantId(tenant);row.setProjectCodeId(projectId);
            row.setParentId(parent);row.setName(name);row.setSortOrder(input.sortOrder());row.setActive(true);
            // 人工创建无模板来源；不伪造0、节点序号或UUID模板键。
            if(folders.insert(row)!=1 || row.getId()==null) throw exception(WRITE_INCOMPLETE,"项目目录新增");
            id=row.getId();
        } else {
            id=previous.getId();
            if(folders.update(null,new LambdaUpdateWrapper<DccProjectFolderDO>()
                    .eq(DccProjectFolderDO::getTenantId,tenant).eq(DccProjectFolderDO::getProjectCodeId,projectId)
                    .eq(DccProjectFolderDO::getId,id).set(DccProjectFolderDO::getParentId,parent)
                    .set(DccProjectFolderDO::getName,name).set(DccProjectFolderDO::getSortOrder,input.sortOrder()))!=1)
                throw exception(WRITE_INCOMPLETE,"项目目录修改");
        }
        var persisted=folders.selectById(id);
        if(persisted==null || !Objects.equals(persisted.getProjectCodeId(),projectId) || !Objects.equals(persisted.getTenantId(),tenant)) throw fail(FOLDER_INVALID);
        String version=UUID.randomUUID().toString();
        audit.append(GxpAuditCommand.builder().operationId(previous==null?"dcc.project-folder.create":"dcc.project-folder.update")
                .subjectId("DCC_PROJECT_FOLDER:"+id).subjectVersion(version).reason(input.changeReason().trim())
                .source("DccProjectFolderMaintenanceService.save").idempotencyKey("DCC:B:FOLDER:"+version)
                .beforeState(GxpAuditStateEnvelope.builder().state(before==null?"ABSENT":"PRESENT")
                        .canonicalJson(before==null?"{}":before).objectVersion(version).build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").canonicalJson(JsonUtils.toJsonString(persisted))
                        .objectVersion(version).build()).build());
        return persisted;
    }
}
