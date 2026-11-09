package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** A destination editor reads only the exact effective version persisted in its reference. */
@Component
class DccProjectReferenceReadAccess {
    @Resource private JdbcTemplate jdbc;
    @Resource private DccProjectAccessService projectAccessService;

    boolean canRead(Long actor, DccControlledFileDO file) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (actor == null || file == null || file.getId() == null || file.getMasterId() == null
                || Boolean.TRUE.equals(file.getDeleted()) || !Objects.equals(tenant, file.getTenantId())
                || !"ACTIVE".equals(file.getStatus())
                || file.getControlledTime() == null || file.getActivatedTime() == null
                || file.getPublishedFileId() == null || file.getPublishedFileId() <= 0) return false;
        var destinations = jdbc.queryForList("""
                SELECT DISTINCT r.project_id FROM dcc_project_file_reference r
                JOIN dcc_project_folder d ON d.id=r.folder_id AND d.project_code_id=r.project_id
                    AND d.tenant_id=r.tenant_id AND d.deleted=0 AND d.active=1
                JOIN dcc_project_code p ON p.id=r.project_id AND p.tenant_id=r.tenant_id
                    AND p.deleted=0 AND p.status='ENABLE'
                WHERE r.tenant_id=? AND r.selected_controlled_file_id=? AND r.master_id=?
                """, Long.class, tenant, file.getId(), file.getMasterId());
        if (destinations.isEmpty()) return false;
        var readable = projectAccessService.listReadableProjectIds(actor);
        return destinations.stream().anyMatch(project -> readable.contains(project)
                && projectAccessService.hasProjectEditorOrOwner(actor, project));
    }
}
