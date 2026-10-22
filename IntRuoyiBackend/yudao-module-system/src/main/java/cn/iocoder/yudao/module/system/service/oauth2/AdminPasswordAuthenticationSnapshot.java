package cn.iocoder.yudao.module.system.service.oauth2;

import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;

import java.time.LocalDateTime;
import java.util.Objects;

/** 密码认证版本，仅保留于调用栈；禁止记录或序列化密码哈希。 */
public final class AdminPasswordAuthenticationSnapshot {

    private final Long tenantId;
    private final Long userId;
    private final String passwordHash;
    private final LocalDateTime passwordUpdateTime;
    private final String passwordCredentialStatus;

    private AdminPasswordAuthenticationSnapshot(AdminUserDO user) {
        this.tenantId = Objects.requireNonNull(user.getTenantId(), "Authenticated user tenant is required");
        this.userId = Objects.requireNonNull(user.getId(), "Authenticated user ID is required");
        if (userId <= 0) {
            throw new IllegalArgumentException("Password authentication requires a positive user ID");
        }
        this.passwordHash = Objects.requireNonNull(user.getPassword(), "Authenticated password hash is required");
        this.passwordUpdateTime = user.getPasswordUpdateTime();
        this.passwordCredentialStatus = user.getPasswordCredentialStatus();
    }

    public static AdminPasswordAuthenticationSnapshot from(AdminUserDO authenticatedUser) {
        return new AdminPasswordAuthenticationSnapshot(Objects.requireNonNull(authenticatedUser,
                "Authenticated user is required"));
    }

    public Long getTenantId() { return tenantId; }
    public Long getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getPasswordUpdateTime() { return passwordUpdateTime; }
    public String getPasswordCredentialStatus() { return passwordCredentialStatus; }
}
