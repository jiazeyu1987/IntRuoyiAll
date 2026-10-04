package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;

/** Current formal account and existing project permissions; no route marker or user-id convention. */
@Service
public class DccProjectProductActorSupport {
    private static final cn.iocoder.yudao.framework.common.exception.ErrorCode QUERY_REQUIRED =
            new cn.iocoder.yudao.framework.common.exception.ErrorCode(1_080_090_019, "项目及产品申请办理账号缺少正式查询权限");
    @Resource private AdminUserApi users;
    @Resource private PermissionApi permissions;
    @Resource private javax.sql.DataSource dataSource;

    public AdminUserRespDTO requireAccount(Long id) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        AdminUserRespDTO user = id == null || id <= 0 ? null : users.getUser(id);
        if (TenantContextHolder.isIgnore() || user == null || !Objects.equals(user.getId(), id)
                || !Objects.equals(user.getTenantId(), tenant) || !Objects.equals(user.getStatus(), 0)
                || user.getUsername() == null || user.getUsername().isBlank()
                || user.getNickname() == null || user.getNickname().isBlank()) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        return user;
    }

    public boolean canUpdate(Long id) {
        requireAccount(id);
        return permissions.hasAnyPermissions(id, "dcc:project-code:update") && canQuery(id);
    }

    public boolean canQuery(Long id) {
        requireAccount(id);
        return permissions.hasAnyPermissions(id, "dcc:project-code:query");
    }

    public void requireReadableAccount(Long id) {
        if (!canQuery(id)) throw exception(QUERY_REQUIRED);
    }

    public boolean isApprover(Long id) {
        return "admin".equals(requireAccount(id).getUsername()) && canUpdate(id);
    }

    public Long requireApprover() {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var ids = new JdbcTemplate(dataSource).queryForList(
                "SELECT id FROM system_users WHERE tenant_id=? AND username=? AND status=0 AND deleted=0",
                Long.class, tenant, "admin");
        if (ids.size() != 1 || !isApprover(ids.get(0))) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_ADMIN_REQUIRED);
        }
        return ids.get(0);
    }

    public void assertReadable(Long actor, DccProjectProductCreateRequestDO request) {
        requireAccount(actor);
        if (request == null || !Objects.equals(request.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !permissions.hasAnyPermissions(actor, "dcc:project-code:query")
                || !(Objects.equals(actor, request.getApplicantUserId())
                || Objects.equals(actor, request.getConfiguredReviewerUserId())
                || Objects.equals(actor, request.getReviewerUserId()) || Objects.equals(actor, request.getApproverUserId())
                || isApprover(actor) || permissions.hasAnyRoles(actor, "doc_control", "approval_admin"))) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS);
        }
    }
}
