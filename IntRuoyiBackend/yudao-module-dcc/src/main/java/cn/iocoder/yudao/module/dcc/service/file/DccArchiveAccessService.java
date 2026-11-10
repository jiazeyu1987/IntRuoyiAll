package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.Set;

@Service
public class DccArchiveAccessService {
    @Resource private PermissionApi permissionApi;
    @Resource private AdminUserApi adminUserApi;
    public static boolean isArchived(DccControlledFileDO file) {
        return file != null && file.getStatus()!=null && Set.of("SUPERSEDED", "OBSOLETE").contains(file.getStatus());
    }
    public boolean canQuery(Long actor) {
        return actor != null && DccAccessUserIdentity.isEnabledTenantUser(actor, adminUserApi.getUser(actor))
                && permissionApi.hasAnyRoles(actor, "doc_control")
                && permissionApi.hasAnyPermissions(actor, "dcc:controlled-file:archive:query");
    }
    public boolean canDownload(Long actor, DccControlledFileDO file, Long attachmentId) {
        return canRead(actor, file);
    }
    public boolean canRead(Long actor, DccControlledFileDO file) {
        return isArchived(file) && !Boolean.TRUE.equals(file.getDeleted())
                && Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId()) && canQuery(actor);
    }
}
