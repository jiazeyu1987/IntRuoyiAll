package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserDirectSupervisorRespDTO;
import cn.iocoder.yudao.module.system.service.user.AdminUserDirectSupervisorErrors;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/** Formal user-management relation shared by native upload, revision and obsolete preparation. */
@Service
public class DccUserManagementDirectSupervisorSource implements DccDirectSupervisorSource {
    /** A truthful domain outcome that public callers may expose separately from technical directory failures. */
    public static final class RelationUnavailableException extends IllegalStateException {
        private final String reasonCode;

        private RelationUnavailableException(String reasonCode, String message) {
            super(reasonCode + "：" + message);
            this.reasonCode = reasonCode;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public ServiceException toServiceException() {
            return new ServiceException(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants
                    .DIRECT_SUPERVISOR_SOURCE_UNAVAILABLE.getCode(), getMessage());
        }
    }

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Evidence require(Context context) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (TenantContextHolder.isIgnore() || context == null
                || !Objects.equals(tenantId, context.tenantId())
                || context.requesterId() == null || context.requesterId() <= 0
                || !Set.of("UPLOAD", "REVISION", "OBSOLETE").contains(
                        context.applicationType() == null ? "" : context.applicationType())) {
            throw new IllegalArgumentException("DIRECT_SUPERVISOR_CONTEXT_INVALID：直属主管来源租户或申请人身份无效");
        }
        requireEnabledAccount(adminUserApi.getUser(context.requesterId()), context.requesterId(), tenantId,
                "DIRECT_SUPERVISOR_REQUESTER_INVALID");
        AdminUserDirectSupervisorRespDTO relation;
        try {
            relation = adminUserApi.getDirectSupervisorRelation(context.requesterId());
        } catch (ServiceException failure) {
            if (!Objects.equals(failure.getCode(), AdminUserDirectSupervisorErrors.INVALID.getCode())
                    && !Objects.equals(failure.getCode(), AdminUserDirectSupervisorErrors.VERSION_INVALID.getCode())) {
                throw failure;
            }
            var unavailable = new RelationUnavailableException("DIRECT_SUPERVISOR_RELATION_INVALID", failure.getMessage());
            unavailable.initCause(failure);
            throw unavailable;
        }
        if (relation == null || !Objects.equals(relation.getTenantId(), tenantId)
                || !Objects.equals(relation.getUserId(), context.requesterId())
                || relation.getRelationVersion() == null || relation.getRelationVersion() < 0) {
            throw new RelationUnavailableException("DIRECT_SUPERVISOR_RELATION_INVALID", "用户管理直属主管关系身份或版本无效");
        }
        Long supervisorId = relation.getSupervisorUserId();
        if (supervisorId == null) {
            throw new RelationUnavailableException("DIRECT_SUPERVISOR_NOT_CONFIGURED", "请先在用户管理设置直属主管");
        }
        if (supervisorId <= 0 || Objects.equals(supervisorId, context.requesterId())) {
            throw new RelationUnavailableException("DIRECT_SUPERVISOR_RELATION_INVALID", "直属主管不能是本人或无效账号");
        }
        requireEnabledAccount(adminUserApi.getUser(supervisorId), supervisorId, tenantId,
                "DIRECT_SUPERVISOR_ACCOUNT_INVALID");
        return new Evidence(tenantId, context.requesterId(), supervisorId, "SYSTEM_USER_MANAGEMENT",
                "SYSTEM_USER_MANAGEMENT:DIRECT_SUPERVISOR:" + tenantId + ":" + context.requesterId(),
                relation.getRelationVersion().toString());
    }

    private void requireEnabledAccount(AdminUserRespDTO account, Long userId, Long tenantId, String errorCode) {
        if (account == null || !Objects.equals(account.getId(), userId)
                || !Objects.equals(account.getTenantId(), tenantId)
                || !CommonStatusEnum.ENABLE.getStatus().equals(account.getStatus())) {
            throw new RelationUnavailableException(errorCode, "账号不存在、停用或不属于当前租户");
        }
    }
}
