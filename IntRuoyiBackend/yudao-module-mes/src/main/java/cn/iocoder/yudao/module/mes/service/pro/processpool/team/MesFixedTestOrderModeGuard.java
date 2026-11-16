package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED;

/** Server-side switch for the single fixed test fixture; UI visibility is not authorization. */
@Component
@RequiredArgsConstructor
public class MesFixedTestOrderModeGuard {
    static final String TEST_MODE_KEY = "system.ui.test-mode-enabled";
    private final ConfigApi configApi;

    public void requireEnabled(Long operatorId) {
        Long tenantId = TenantContextHolder.getTenantId();
        var loginUser = SecurityFrameworkUtils.getLoginUser();
        if (operatorId == null || operatorId <= 0 || tenantId == null || tenantId <= 0
                || TenantContextHolder.isIgnore() || loginUser == null
                || !Objects.equals(operatorId, loginUser.getId())
                || !Objects.equals(tenantId, loginUser.getTenantId())) {
            throw exception(PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED, "重置测试订单需要登录用户及当前租户身份");
        }
        if (!"true".equals(configApi.getConfigValueByKey(TEST_MODE_KEY))) {
            throw exception(PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED,
                    "系统未开启测试模式；system.ui.test-mode-enabled 必须明确配置为 true");
        }
    }
}
