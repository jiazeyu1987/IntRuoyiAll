package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;

import java.util.Objects;

/** 用户明确授权的瑛泰集团管理员例外，不适用于新增用户或其他租户。 */
final class YintaiAdminAccountPolicy {
    static final Long TENANT_ID = 165L;
    static final Long ADMIN_USER_ID = 9908090372L;

    private YintaiAdminAccountPolicy() { }

    static boolean isAdministrator(AdminUserDO user, TenantDO tenant) {
        return user != null && tenant != null && ADMIN_USER_ID.equals(user.getId())
                && TENANT_ID.equals(user.getTenantId()) && TENANT_ID.equals(tenant.getId())
                && "瑛泰集团".equals(tenant.getName())
                && Objects.equals(user.getId(), tenant.getContactUserId())
                && !Boolean.TRUE.equals(user.getDeleted())
                && ("yintaigroupadmin".equals(user.getUsername()) || "admin".equals(user.getUsername()));
    }

}
