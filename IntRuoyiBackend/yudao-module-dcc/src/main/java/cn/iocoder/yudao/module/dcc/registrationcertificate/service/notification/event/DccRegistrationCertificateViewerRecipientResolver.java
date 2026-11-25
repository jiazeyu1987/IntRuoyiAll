package cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.access.DccRegistrationCertificateBusinessFileAccessProvider.QUERY_CURRENT_PERMISSION;

/** CE生效通知与正式证件查看使用同一权限口径，不另设公司或固定人员名单。 */
@Service
public class DccRegistrationCertificateViewerRecipientResolver {

    private final JdbcTemplate jdbc;
    private final PermissionApi permissionApi;

    public DccRegistrationCertificateViewerRecipientResolver(JdbcTemplate jdbc, PermissionApi permissionApi) {
        this.jdbc = jdbc;
        this.permissionApi = permissionApi;
    }

    public List<Long> resolve(Long tenantId) {
        if (tenantId == null || tenantId <= 0
                || !Objects.equals(tenantId, TenantContextHolder.getRequiredTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_GRANT_SCOPE_INVALID);
        }
        // 先限制租户和启用状态，再交给正式权限服务判断（包括临时权限等有效授权）。
        return jdbc.queryForList("""
                SELECT id FROM system_users
                 WHERE tenant_id = ? AND status = 0 AND deleted = 0
                 ORDER BY id
                """, Long.class, tenantId).stream()
                .filter(userId -> permissionApi.hasAnyPermissions(userId, QUERY_CURRENT_PERMISSION))
                .toList();
    }
}
